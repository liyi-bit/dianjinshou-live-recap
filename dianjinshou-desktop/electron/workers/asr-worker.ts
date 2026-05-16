/**
 * ASR 子进程（Electron utilityProcess）
 *
 * 为什么要分到子进程：
 *   sherpa-onnx 的 Node 绑定是 WASM + 同步 API，recognizer.decode(stream) 单次调用
 *   阻塞 JS 线程 ~5s。跑在主进程里会把 Electron 的 IPC / 窗口事件循环整个冻住，
 *   导致用户点击「停止录制」后 UI 卡几分钟。搬到子进程后，主进程完全不被拖慢。
 *
 * 协议：
 *   父 → 子：{ type:'transcribe', videoPath, modelPath, tokensPath, ffmpegBin }
 *   子 → 父：{ type:'progress', chunkIdx, totalChunks }
 *            { type:'result', segments }
 *            { type:'error', message }
 */
import { spawn } from 'child_process'
import { join, basename } from 'path'
import { existsSync, statSync, unlinkSync, mkdtempSync, rmSync } from 'fs'
import { tmpdir } from 'os'

type AsrSegment = {
  segmentIndex: number
  startTime: string
  endTime: string
  text: string
}

const CHUNK_SEC = 30
const SAMPLE_RATE = 16000
const OVERLAP_SEC = 0.5

let _recognizer: any = null
let _punct: any = null

function loadPunct(punctModelPath: string): any {
  if (_punct) return _punct
  if (!punctModelPath) return null
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  const sherpa = require('sherpa-onnx')
  try {
    console.log('[AsrWorker] Loading punct model:', punctModelPath)
    const t0 = Date.now()
    _punct = sherpa.createOfflinePunctuation({
      model: { ctTransformer: punctModelPath, numThreads: 1, debug: 0, provider: 'cpu' },
    })
    console.log('[AsrWorker] Punct loaded in', Date.now() - t0, 'ms')
    return _punct
  } catch (err: any) {
    console.warn('[AsrWorker] Punct load failed, fallback to plain text:', err?.message || err)
    _punct = null
    return null
  }
}

function loadRecognizer(modelPath: string, tokensPath: string): any {
  if (_recognizer) return _recognizer
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  const sherpa = require('sherpa-onnx')
  const config = {
    modelConfig: {
      paraformer: { model: modelPath },
      tokens: tokensPath,
      debug: 1,
    },
  }
  console.log('[AsrWorker] Loading Paraformer model...')
  const t0 = Date.now()
  _recognizer = sherpa.createOfflineRecognizer(config)
  console.log('[AsrWorker] Model loaded in', Date.now() - t0, 'ms')
  return _recognizer
}

function convertToWav(ffmpegBin: string, input: string, output: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const args = [
      '-y',
      '-fflags', '+genpts+igndts',
      '-i', input,
      '-vn',
      '-af', 'aresample=async=1',
      '-acodec', 'pcm_s16le',
      '-ar', String(SAMPLE_RATE),
      '-ac', '1',
      output,
    ]
    const proc = spawn(ffmpegBin, args)
    proc.on('error', (err) => reject(new Error(`FFmpeg 启动失败: ${err.message}`)))
    proc.stderr?.on('data', () => {})
    proc.stdout?.on('data', () => {})
    proc.on('exit', (code) => {
      if (code === 0 && existsSync(output) && statSync(output).size > 44) {
        resolve()
      } else {
        reject(new Error(`FFmpeg 转 WAV 失败，退出码 ${code}`))
      }
    })
  })
}

function secToTime(sec: number): string {
  const s = Math.floor(sec)
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const ss = s % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(ss).padStart(2, '0')}`
}

function post(msg: any): void {
  // utilityProcess 子进程拿到的 parentPort 挂在 process 上
  const pp = (process as any).parentPort
  if (pp) pp.postMessage(msg)
}

async function transcribe(
  videoPath: string,
  modelPath: string,
  tokensPath: string,
  ffmpegBin: string,
  punctModelPath?: string,
): Promise<AsrSegment[]> {
  console.log('[AsrWorker] Starting transcription:', basename(videoPath))

  const tmpDir = mkdtempSync(join(tmpdir(), 'djsh-asr-'))
  const wavPath = join(tmpDir, 'audio.wav')

  try {
    await convertToWav(ffmpegBin, videoPath, wavPath)
    console.log('[AsrWorker] WAV ready:', wavPath, statSync(wavPath).size, 'bytes')

    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const sherpa = require('sherpa-onnx')
    const recognizer = loadRecognizer(modelPath, tokensPath)
    const punct = punctModelPath ? loadPunct(punctModelPath) : null
    const wave = sherpa.readWave(wavPath)
    const samples: Float32Array = wave.samples
    const sr: number = wave.sampleRate
    const totalSamples = samples.length
    console.log('[AsrWorker] Audio:', (totalSamples / sr).toFixed(1), 's', sr, 'Hz')

    const samplesPerChunk = Math.floor(CHUNK_SEC * sr)
    const samplesOverlap = Math.floor(OVERLAP_SEC * sr)
    const step = samplesPerChunk - samplesOverlap

    const segments: AsrSegment[] = []
    let segIdx = 0
    const totalChunks = Math.max(1, Math.ceil(totalSamples / step))
    let chunkIdx = 0

    let skippedChunks = 0
    for (let start = 0; start < totalSamples; start += step) {
      const end = Math.min(start + samplesPerChunk, totalSamples)
      const chunk = samples.slice(start, end)

      let stream: any = null
      try {
        stream = recognizer.createStream()
        stream.acceptWaveform(sr, chunk)
        recognizer.decode(stream)
        let text = (recognizer.getResult(stream).text || '').trim()
        if (text && punct) {
          try {
            const punctuated = punct.addPunct(text)
            if (punctuated && punctuated.trim()) text = punctuated.trim()
          } catch (err: any) {
            console.warn('[AsrWorker] addPunct failed, keep plain:', err?.message || err)
          }
        }
        if (text) {
          segments.push({
            segmentIndex: segIdx++,
            startTime: secToTime(start / sr),
            endTime: secToTime(end / sr),
            text,
          })
        }
      } catch (err: any) {
        // JS 层可见的解码错误：跳过此 chunk，继续。
        // （native abort 在这里抓不到，只能交给父端重试）
        skippedChunks += 1
        console.warn(`[AsrWorker] chunk ${chunkIdx} decode failed, skip:`, err?.message || err)
      } finally {
        if (stream) { try { stream.free() } catch {} }
      }

      chunkIdx += 1
      post({ type: 'progress', chunkIdx, totalChunks })

      if (end >= totalSamples) break
      // 让事件循环喘口气，能响应父进程的 cancel / kill
      await new Promise((r) => setImmediate(r))
    }
    if (skippedChunks > 0) {
      console.warn(`[AsrWorker] 完成但跳过 ${skippedChunks}/${totalChunks} 个 chunk`)
    }

    console.log('[AsrWorker] Done:', segments.length, 'segments')
    return segments
  } finally {
    try { if (existsSync(wavPath)) unlinkSync(wavPath) } catch {}
    try { rmSync(tmpDir, { recursive: true, force: true }) } catch {}
  }
}

// utilityProcess 固定入口：process.parentPort 上收消息
const pp = (process as any).parentPort
if (!pp) {
  console.error('[AsrWorker] process.parentPort missing — not running as Electron utilityProcess')
  process.exit(1)
}

// JS 层异常兜底：sherpa-onnx 的 native abort 会直接 SIGABRT，捕不到；
// 但所有 JS 抛出的同步/异步异常这里都能尽量送回父端，避免父端只看到 exit code。
process.on('uncaughtException', (err: any) => {
  try { post({ type: 'error', message: `worker uncaught: ${err?.message || err}`, stack: err?.stack }) } catch {}
  // 留 50ms 让 IPC 发出去再退出
  setTimeout(() => process.exit(2), 50)
})
process.on('unhandledRejection', (reason: any) => {
  try { post({ type: 'error', message: `worker unhandled: ${reason?.message || String(reason)}`, stack: reason?.stack }) } catch {}
  setTimeout(() => process.exit(3), 50)
})

pp.on('message', async (e: any) => {
  const msg = e.data ?? e
  if (msg?.type === 'transcribe') {
    try {
      const segments = await transcribe(
        msg.videoPath,
        msg.modelPath,
        msg.tokensPath,
        msg.ffmpegBin,
        msg.punctModelPath,
      )
      post({ type: 'result', segments })
    } catch (err: any) {
      post({ type: 'error', message: String(err?.message || err) })
    }
  }
})
