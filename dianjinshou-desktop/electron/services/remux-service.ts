/**
 * FLV → MP4 转封装服务
 *
 * 直播录制先存为 FLV（流式写入，崩溃不丢文件），结束后转封装为 MP4。
 * 使用 -c copy 模式，不重编码。
 * 超时时间根据文件大小动态计算，避免大文件被误杀。
 */

import { spawn } from 'child_process'
import { join } from 'path'
import { app } from 'electron'
import { existsSync, statSync, unlinkSync } from 'fs'

/** 基础超时 5 分钟 + 每 GB 额外 3 分钟 */
const BASE_TIMEOUT = 5 * 60 * 1000
const TIMEOUT_PER_GB = 3 * 60 * 1000

function getFFmpegBin(): string {
  if (app.isPackaged) {
    return join(process.resourcesPath, 'ffmpeg', process.platform === 'win32' ? 'ffmpeg.exe' : 'ffmpeg')
  }
  try {
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const ffmpegStatic = require('ffmpeg-static') as string
    if (ffmpegStatic) return ffmpegStatic
  } catch {
    // ignore
  }
  return 'ffmpeg'
}

/** 根据文件大小计算 remux 超时时间 */
function calculateTimeout(fileSizeBytes: number): number {
  const sizeGB = fileSizeBytes / (1024 * 1024 * 1024)
  return BASE_TIMEOUT + Math.ceil(sizeGB) * TIMEOUT_PER_GB
}

/**
 * 将 FLV 文件转封装为 MP4
 *
 * @param flvPath FLV 源文件路径
 * @param mp4Path MP4 目标文件路径
 * @returns 成功返回 mp4Path，失败返回 flvPath（保留 FLV 作为降级）
 */
export async function remuxFlvToMp4(flvPath: string, mp4Path: string): Promise<string> {
  // 检查 FLV 文件是否有效
  if (!existsSync(flvPath)) {
    console.error(`[Remux] FLV file not found: ${flvPath}`)
    return flvPath
  }

  const flvSize = statSync(flvPath).size
  if (flvSize === 0) {
    console.warn(`[Remux] FLV file is empty, skipping remux: ${flvPath}`)
    return flvPath
  }

  const timeout = calculateTimeout(flvSize)
  console.log(`[Remux] Starting: ${flvPath} → ${mp4Path} (${(flvSize / 1024 / 1024).toFixed(1)} MB, timeout=${Math.round(timeout / 1000)}s)`)

  return new Promise<string>((resolve) => {
    const ffmpegBin = getFFmpegBin()
    const args = [
      '-y',
      '-i', flvPath,
      '-c', 'copy',
      '-movflags', '+faststart',
      mp4Path,
    ]

    const proc = spawn(ffmpegBin, args, { stdio: ['pipe', 'pipe', 'pipe'] })

    const timer = setTimeout(() => {
      console.warn(`[Remux] Timeout after ${Math.round(timeout / 1000)}s, killing process`)
      proc.kill('SIGTERM')
    }, timeout)

    proc.stderr?.on('data', (data: Buffer) => {
      const line = data.toString().trim()
      if (line) {
        console.log(`[Remux] ${line}`)
      }
    })

    proc.on('exit', (code) => {
      clearTimeout(timer)

      if (code === 0 && existsSync(mp4Path) && statSync(mp4Path).size > 0) {
        console.log(`[Remux] Success: ${mp4Path}`)
        // 删除 FLV 源文件
        try {
          unlinkSync(flvPath)
          console.log(`[Remux] Deleted FLV: ${flvPath}`)
        } catch {
          console.warn(`[Remux] Failed to delete FLV: ${flvPath}`)
        }
        resolve(mp4Path)
      } else {
        console.error(`[Remux] Failed (code=${code}), keeping FLV as output`)
        resolve(flvPath)
      }
    })

    proc.on('error', (err) => {
      clearTimeout(timer)
      console.error(`[Remux] Process error:`, err.message)
      resolve(flvPath)
    })
  })
}
