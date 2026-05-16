/**
 * 本地 ASR 服务（sherpa-onnx WASM + Paraformer 中文模型）。
 *
 * v1.1.0 架构改动：**不再在主进程跑 WASM 推理**。
 *   原因：sherpa-onnx 的 decode 是同步 WASM 调用，单 chunk ~5s。30 分钟视频 60 chunk
 *        主进程阻塞 5-6 分钟，用户点"停止录制"后 UI 整个冻住。
 *   方案：把 transcribe 全流程（ffmpeg 转 WAV + 切片推理）搬到 Electron `utilityProcess.fork`
 *        子进程里跑。主进程只负责拉起子进程 + 等 IPC 返回 segments，0 阻塞。
 *
 * 主进程只保留本机 ASR 路径；不再接入云端 ASR。
 */
import { app, utilityProcess, UtilityProcess } from 'electron'
import { join } from 'path'
import { statSync } from 'fs'

import type { AsrSegment } from './asr-types'
import { getAsrModelManager } from './asr-model-manager'
import { getPunctModelManager } from './punct-model-manager'

/**
 * 动态超时策略（防 worker 假死）：
 *   - 已知视频时长：BASE + PER_MIN * 时长，上限 CAP
 *   - 未知时长：按文件大小估算（PER_GB），上限 CAP
 *   - 极小文件下限 BASE，避免 30s 视频给 30s 超时反而误杀
 *
 * 旧固定 40 分钟在长直播录像（>2h）会误杀；改成动态后既兼容长视频又避免短视频卡死太久。
 */
const ASR_TIMEOUT_BASE_MS = 5 * 60 * 1000
const ASR_TIMEOUT_PER_MIN_MS = 30 * 1000
const ASR_TIMEOUT_PER_GB_MS = 8 * 60 * 1000
const ASR_TIMEOUT_CAP_MS = 90 * 60 * 1000

function computeAsrTimeoutMs(videoPath: string, durationSec?: number): number {
  let ms = ASR_TIMEOUT_BASE_MS
  if (durationSec && durationSec > 0) {
    ms += Math.ceil(durationSec / 60) * ASR_TIMEOUT_PER_MIN_MS
  } else {
    try {
      const sizeGB = statSync(videoPath).size / (1024 ** 3)
      ms += Math.ceil(sizeGB) * ASR_TIMEOUT_PER_GB_MS
    } catch {
      // 文件不存在或读不到，退化到固定 40 分钟（与历史行为一致）
      ms = 40 * 60 * 1000
    }
  }
  return Math.min(ASR_TIMEOUT_CAP_MS, Math.max(ASR_TIMEOUT_BASE_MS, ms))
}

/** 仅保留这一层：用于测试/卸载模型时重置状态（子进程每次 transcribe 自己 fork 了，这里空壳） */
export function disposeLocalAsr(): void {
  /* no-op: worker 进程每次 transcribe 结束后会 exit */
}

export class LocalAsrService {
  async transcribe(videoPath: string, options?: { durationSec?: number }): Promise<AsrSegment[]> {
    const mgr = getAsrModelManager()
    if (!mgr.isReady()) {
      throw new Error('本地 ASR 模型未就绪，请重新安装最新版客户端')
    }

    // sherpa-onnx WASM 的 native abort 不会被 JS catch 抓到（直接 SIGABRT 退出），
    // 但实际中相当部分 abort 是模型/线程偶发问题，重新 fork 一个干净进程就能恢复。
    // 因此父端做 1 次自动重试，attempt=1 时仍然失败再向上抛。
    let lastErr: any = null
    for (let attempt = 0; attempt <= 1; attempt++) {
      try {
        return await this.runOnce(videoPath, options, attempt)
      } catch (err: any) {
        lastErr = err
        // 用户主动取消（kill）或模型未就绪不重试
        const msg = String(err?.message || err)
        if (/未就绪|模型|cancel/i.test(msg)) break
        if (attempt === 0) {
          console.warn(`[LocalAsr] attempt#0 失败，自动重试一次：${msg}`)
        }
      }
    }
    throw lastErr
  }

  private runOnce(videoPath: string, options: { durationSec?: number } | undefined, attempt: number): Promise<AsrSegment[]> {
    const workerPath = this.getWorkerPath()
    const ffmpegBin = this.getFFmpegBin()
    const timeoutMs = computeAsrTimeoutMs(videoPath, options?.durationSec)
    console.log(`[LocalAsr] timeout budget: ${Math.round(timeoutMs / 60000)} 分钟 (durationSec=${options?.durationSec ?? 'unknown'}, attempt=${attempt})`)
    const mgr = getAsrModelManager()

    return new Promise<AsrSegment[]>((resolve, reject) => {
      let child: UtilityProcess | null = null
      let settled = false
      const finish = (ok: boolean, payload: any) => {
        if (settled) return
        settled = true
        clearTimeout(timer)
        try { child?.kill() } catch {}
        ok ? resolve(payload) : reject(payload)
      }

      try {
        child = utilityProcess.fork(workerPath, [], {
          serviceName: 'djsh-asr-worker',
          stdio: 'pipe',
        })
      } catch (err: any) {
        return reject(new Error(`ASR 子进程启动失败: ${err?.message || err}`))
      }

      // 子进程 stdout/stderr 透传到主进程 console，方便观察 sherpa-onnx 日志
      child.stdout?.on('data', (buf) => process.stdout.write(`[AsrWorker] ${buf}`))
      child.stderr?.on('data', (buf) => process.stderr.write(`[AsrWorker] ${buf}`))

      const timer = setTimeout(() => {
        finish(false, new Error(`ASR 超时（>${Math.round(timeoutMs / 60000)} 分钟）`))
      }, timeoutMs)

      child.on('message', (msg: any) => {
        if (msg?.type === 'result') {
          finish(true, msg.segments as AsrSegment[])
        } else if (msg?.type === 'error') {
          finish(false, new Error(msg.message || 'ASR 子进程报错'))
        } else if (msg?.type === 'progress') {
          // 目前不向 UI 广播，只打日志；如需进度条可在此 send IPC 给渲染
          if (msg.chunkIdx % 10 === 0 || msg.chunkIdx === msg.totalChunks) {
            console.log(`[LocalAsr] progress ${msg.chunkIdx}/${msg.totalChunks}`)
          }
        }
      })

      child.on('exit', (code) => {
        if (!settled) {
          // sherpa-onnx WASM SIGABRT 时 code 可能为 null/134/3221225477；统一给上层一个更友好的提示
          const hint = (code === null || code === 134 || code === 6 || code === 3221225477)
            ? '（疑似 native abort，可能是模型或音频边界异常）'
            : ''
          finish(false, new Error(`ASR 子进程意外退出，code=${code}${hint}`))
        }
      })

      // 标点模型就绪就带上（没就绪也 OK，worker 会跳过 punct、输出裸文字）
      const punctMgr = getPunctModelManager()
      const punctModelPath = punctMgr.isReady() ? punctMgr.getModelPath() : undefined

      child.postMessage({
        type: 'transcribe',
        videoPath,
        modelPath: mgr.getModelPath(),
        tokensPath: mgr.getTokensPath(),
        ffmpegBin,
        punctModelPath,
      })
    })
  }

  private getWorkerPath(): string {
    // 打包后：app.asar/out/main/workers/asr-worker.js
    // 开发期：out/main/workers/asr-worker.js（electron-vite 产物）
    return join(__dirname, 'workers', 'asr-worker.js')
  }

  private getFFmpegBin(): string {
    if (app.isPackaged) {
      return join(
        process.resourcesPath,
        'ffmpeg',
        process.platform === 'win32' ? 'ffmpeg.exe' : 'ffmpeg'
      )
    }
    try {
      // eslint-disable-next-line @typescript-eslint/no-var-requires
      const f = require('ffmpeg-static') as string
      if (f) return f
    } catch {}
    return 'ffmpeg'
  }
}
