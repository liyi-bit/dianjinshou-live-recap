import { spawn, ChildProcess } from 'child_process'
import { join } from 'path'
import { app } from 'electron'
import { statfsSync } from 'fs'
import { remuxFlvToMp4 } from './remux-service'

export interface RecordingConfig {
  recordingId: string
  streamUrl: string
  outputDir: string
  anchorName: string
  platform?: 'douyin' | 'kuaishou'
  resolution: '480p' | '720p' | '1080p' | 'source'
  segmentDuration: number // seconds, default 1800 (30 min)
}

export interface RecordingStatus {
  recordingId: string
  status: 'recording' | 'stopped' | 'error' | 'remuxing'
  startTime: number
  filePath: string | null
  flvPath: string | null
  mp4Path: string | null
  segmentIndex: number
  error?: string
  bytesReceived: number
  reconnectCount: number
}

const GRACEFUL_STOP_TIMEOUT = 5000
const HEALTH_CHECK_INTERVAL = 15000 // 15s
/**
 * 如果 FFmpeg 连续 N 毫秒没有新输出（time=... 停止推进），判定直播结束。
 * CDN 节点切换 / 主播短暂断流常常 1-2 分钟内会恢复，阈值放宽到 3 分钟更稳。
 */
const PROGRESS_STALL_THRESHOLD = 180000 // 3 分钟
/**
 * 低码率保护：避免直播结束后 CDN 持续发空 keep-alive 但不推实际数据时，FFmpeg 无限挂住。
 * 主播短暂暂停、网络抖动、CDN 节点切换都能触发 0 KB/s，阈值放宽到 5 分钟能容忍大部分场景。
 * 参考成熟方案 ihmily/DouyinLiveRecorder 的做法是完全依赖 -rw_timeout + reconnect 不主动杀，
 * 我们保留保护但明显放宽。
 */
const LOW_BITRATE_THRESHOLD = 30 * 1024 // 30 KB/s
const LOW_BITRATE_DURATION = 300000 // 5 分钟
const DISK_FREE_WARN_BYTES = 500 * 1024 * 1024 // 500 MB
const DISK_FREE_STOP_BYTES = 200 * 1024 * 1024 // 200 MB
const DISK_CHECK_INTERVAL = 60000

/** 流 URL 刷新回调（外部注入，当 FFmpeg 启动失败 / 彻底退出时用来换新 URL 再试一次） */
type StreamUrlRefresher = (recordingId: string) => Promise<string | null>

/**
 * 解析 FFmpeg 进度行。兼容两种格式：
 * 1) 传统 stderr 格式："size= 1024kB time=00:01:10.54 bitrate=..."
 * 2) `-progress pipe:1` 的 KV 格式：每行 "key=value"，关键行有 total_size=..., out_time_ms=..., out_time_us=...
 */
function parseProgress(raw: string): { timeMs: number | null; sizeBytes: number | null } {
  let timeMs: number | null = null
  let sizeBytes: number | null = null

  // 格式 2（KV）：逐行找 total_size / out_time_us
  for (const line of raw.split(/\r?\n/)) {
    const kv = line.match(/^([a-z_]+)=(.+)$/)
    if (!kv) continue
    const [, k, v] = kv
    if (k === 'total_size') {
      const n = parseInt(v.trim(), 10)
      if (Number.isFinite(n)) sizeBytes = n
    } else if (k === 'out_time_us' || k === 'out_time_ms') {
      // 注意：FFmpeg 里 out_time_ms 其实是微秒（历史遗留），out_time_us 也是微秒
      const n = parseInt(v.trim(), 10)
      if (Number.isFinite(n)) timeMs = Math.floor(n / 1000)
    }
  }

  // 格式 1（旧 stderr 格式）兜底
  if (timeMs === null) {
    const tm = raw.match(/time=\s*(\d+):(\d+):(\d+(?:\.\d+)?)/)
    if (tm) timeMs = (parseInt(tm[1]) * 3600 + parseInt(tm[2]) * 60 + parseFloat(tm[3])) * 1000
  }
  if (sizeBytes === null) {
    const sz = raw.match(/size=\s*(\d+)kB/)
    if (sz) sizeBytes = parseInt(sz[1]) * 1024
  }

  return { timeMs, sizeBytes }
}

export class FFmpegManager {
  private processes: Map<string, ChildProcess> = new Map()
  private statuses: Map<string, RecordingStatus> = new Map()
  private configs: Map<string, RecordingConfig> = new Map()
  private onRecordingComplete?: (recordingId: string, finalPath: string) => void
  private streamUrlRefresher?: StreamUrlRefresher

  /** 每个录制的运行时状态 */
  private runtimeState: Map<string, {
    healthTimer: ReturnType<typeof setInterval> | null
    diskTimer: ReturnType<typeof setInterval> | null
    segmentTimer: ReturnType<typeof setTimeout> | null
    stopping: boolean
    lastProgressTime: number        // 最近一次 FFmpeg 打印 time= 的时间
    lastBytesSnapshot: number
    lastBytesSnapshotTime: number
    lowBitrateSince: number | null
  }> = new Map()

  constructor() {
    app.on('before-quit', () => {
      this.stopAll()
    })
  }

  setOnRecordingComplete(callback: (recordingId: string, finalPath: string) => void): void {
    this.onRecordingComplete = callback
  }

  setStreamUrlRefresher(refresher: StreamUrlRefresher): void {
    this.streamUrlRefresher = refresher
  }

  private buildPaths(config: RecordingConfig): { flvPath: string; mp4Path: string } {
    const d = new Date()
    const datetime = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}-${String(d.getMinutes()).padStart(2, '0')}-${String(d.getSeconds()).padStart(2, '0')}`
    const safeName = config.anchorName.replace(/[<>:"/\\|?*]/g, '_')
    const baseName = `${safeName}_${datetime}`
    return {
      flvPath: join(config.outputDir, `${baseName}.flv`),
      mp4Path: join(config.outputDir, `${baseName}.mp4`),
    }
  }

  private getFFmpegBin(): string {
    if (app.isPackaged) {
      return join(process.resourcesPath, 'ffmpeg', process.platform === 'win32' ? 'ffmpeg.exe' : 'ffmpeg')
    }
    try {
      // eslint-disable-next-line @typescript-eslint/no-var-requires
      const ffmpegStatic = require('ffmpeg-static') as string
      if (ffmpegStatic) return ffmpegStatic
    } catch { /* ignore */ }
    return 'ffmpeg'
  }

  private checkDiskSpace(outputDir: string): { free: number; ok: boolean; warning: boolean } {
    try {
      const stats = statfsSync(outputDir)
      const free = Number(stats.bfree) * Number(stats.bsize)
      return { free, ok: free > DISK_FREE_STOP_BYTES, warning: free < DISK_FREE_WARN_BYTES }
    } catch {
      return { free: Infinity, ok: true, warning: false }
    }
  }

  private getRefererForPlatform(platform: RecordingConfig['platform']): string {
    if (platform === 'kuaishou') return 'https://live.kuaishou.com/'
    return 'https://live.douyin.com/'
  }

  private buildFfmpegArgs(config: RecordingConfig, flvPath: string): string[] {
    const referer = this.getRefererForPlatform(config.platform)
    return [
      '-y',
      '-hide_banner',
      // 静默 stderr —— 默认每秒十几行进度刷 stderr，Node.js pipe 缓冲满后 FFmpeg 写 stderr
      // 阻塞 → 读 socket 变慢 → TCP window 收缩 → CDN 认为接收慢而节流。只保留 error 级。
      '-nostats',
      '-loglevel', 'error',
      // HTTP 请求头
      '-user_agent',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
      '-headers', `Referer: ${referer}\r\n`,
      // HTTP keep-alive —— 复用同一 TCP，避免 reconnect 时重新握手 + TCP slow-start
      '-multiple_requests', '1',
      // 明确声明流不可 seek —— FFmpeg 不会做 HEAD 探测
      '-seekable', '0',
      // 加快探测、快速开始录制
      '-analyzeduration', '5000000',
      '-probesize', '5000000',
      // 输入线程队列 —— 默认只有 8 个包，网络抖动时队列满直接丢包
      '-thread_queue_size', '1024',
      // FFmpeg 内置 HTTP 重连（字节流层）
      '-reconnect', '1',
      '-reconnect_streamed', '1',
      '-reconnect_at_eof', '1',
      '-reconnect_on_network_error', '1',
      '-reconnect_on_http_error', '4xx,5xx',
      '-reconnect_delay_max', '30',
      // 30s 无读写就触发重连
      '-rw_timeout', '30000000',
      '-timeout', '30000000',
      // 只丢掉坏包，不重算时间戳（之前的 +genpts+igndts 会搞乱时间戳导致周期性卡顿）
      '-fflags', '+discardcorrupt',
      '-i', config.streamUrl,
      // 禁用字幕/数据流 —— 抖音 FLV 偶尔带脏 data 轨会让 mux 错乱
      '-sn',
      '-dn',
      '-c', 'copy',
      // 修正时间戳溢出 + 避免负时间戳（长时间录制 / CDN 切换时关键）
      '-correct_ts_overflow', '1',
      '-avoid_negative_ts', '1',
      // mux 队列 —— 默认 1024，磁盘偶发慢时不够用，写包被丢 → 卡顿
      '-max_muxing_queue_size', '2048',
      '-f', 'flv',
      flvPath,
      // 进度单独走 stdout（机器可读 KV 格式，比 stderr 噪声小、解析稳）
      '-progress', 'pipe:1',
    ]
  }

  /** Start a new FFmpeg recording process. */
  startRecording(config: RecordingConfig): RecordingStatus {
    if (this.processes.has(config.recordingId)) {
      const existing = this.statuses.get(config.recordingId)
      if (existing && existing.status === 'recording') {
        throw new Error(`Recording ${config.recordingId} is already active`)
      }
    }

    const disk = this.checkDiskSpace(config.outputDir)
    if (!disk.ok) {
      const status: RecordingStatus = {
        recordingId: config.recordingId,
        status: 'error',
        startTime: Date.now(),
        filePath: null,
        flvPath: null,
        mp4Path: null,
        segmentIndex: 0,
        error: `磁盘空间不足（剩余 ${Math.round(disk.free / 1024 / 1024)}MB），无法开始录制`,
        bytesReceived: 0,
        reconnectCount: 0,
      }
      this.statuses.set(config.recordingId, status)
      return status
    }

    const { flvPath, mp4Path } = this.buildPaths(config)

    const proc = spawn(this.getFFmpegBin(), this.buildFfmpegArgs(config, flvPath))

    const status: RecordingStatus = {
      recordingId: config.recordingId,
      status: 'recording',
      startTime: Date.now(),
      filePath: config.outputDir,
      flvPath,
      mp4Path,
      segmentIndex: 0,
      bytesReceived: 0,
      reconnectCount: 0,
    }

    this.processes.set(config.recordingId, proc)
    this.statuses.set(config.recordingId, status)
    this.configs.set(config.recordingId, config)

    const now = Date.now()
    this.runtimeState.set(config.recordingId, {
      healthTimer: null,
      diskTimer: null,
      segmentTimer: null,
      stopping: false,
      lastProgressTime: now,
      lastBytesSnapshot: 0,
      lastBytesSnapshotTime: now,
      lowBitrateSince: null,
    })

    this.startHealthCheck(config.recordingId)
    this.startDiskCheck(config.recordingId, config.outputDir)
    this.startSegmentTimer(config)

    // stdout：`-progress pipe:1` 的 KV 格式进度，每秒一批
    proc.stdout?.on('data', (data: Buffer) => {
      const raw = data.toString()
      const { timeMs, sizeBytes } = parseProgress(raw)
      const rt = this.runtimeState.get(config.recordingId)
      if (rt && (timeMs !== null || sizeBytes !== null)) {
        rt.lastProgressTime = Date.now()
        if (sizeBytes !== null) status.bytesReceived = sizeBytes
      }
      // 不打 progress KV 到控制台（量大、无用信息）
    })

    // stderr：只剩 error 级日志（`-loglevel error -nostats` 压制了噪声），直接打出来
    proc.stderr?.on('data', (data: Buffer) => {
      const line = data.toString().trim()
      if (line) console.log(`[FFmpeg ${config.recordingId}] ${line}`)
    })

    proc.on('error', (err: Error) => {
      console.error(`[FFmpeg ${config.recordingId}] Process error:`, err.message)
      this.cleanupRuntimeState(config.recordingId)
      const currentStatus = this.statuses.get(config.recordingId)
      if (currentStatus) {
        currentStatus.status = 'error'
        currentStatus.error = err.message.includes('ENOENT')
          ? 'FFmpeg not found. Please install FFmpeg and ensure it is in your PATH.'
          : err.message
      }
      this.processes.delete(config.recordingId)
      this.configs.delete(config.recordingId)
    })

    proc.on('exit', (code: number | null, signal: string | null) => {
      console.log(`[FFmpeg ${config.recordingId}] Exited with code=${code}, signal=${signal}`)
      this.cleanupRuntimeState(config.recordingId)
      this.processes.delete(config.recordingId)
      this.configs.delete(config.recordingId)

      const currentStatus = this.statuses.get(config.recordingId)
      if (!currentStatus) return

      if (code !== 0 && code !== null && !signal) {
        console.warn(`[FFmpeg ${config.recordingId}] Non-zero exit code ${code}, attempting remux of partial FLV`)
      }

      currentStatus.status = 'remuxing'
      remuxFlvToMp4(flvPath, mp4Path)
        .then((finalPath) => {
          currentStatus.status = 'stopped'
          currentStatus.filePath = finalPath
          if (finalPath === mp4Path) currentStatus.mp4Path = mp4Path
          console.log(`[FFmpeg ${config.recordingId}] Final output: ${finalPath}`)
          this.onRecordingComplete?.(config.recordingId, finalPath)
        })
        .catch((err) => {
          currentStatus.status = 'stopped'
          currentStatus.filePath = flvPath
          console.error(`[FFmpeg ${config.recordingId}] Remux error:`, err)
          this.onRecordingComplete?.(config.recordingId, flvPath)
        })
    })

    if (disk.warning) {
      console.warn(`[FFmpeg ${config.recordingId}] Warning: disk space low (${Math.round(disk.free / 1024 / 1024)}MB), recording started but may stop soon`)
    }

    return status
  }

  private startHealthCheck(recordingId: string): void {
    const rt = this.runtimeState.get(recordingId)
    if (!rt) return
    const status = this.statuses.get(recordingId)

    rt.healthTimer = setInterval(() => {
      if (rt.stopping) return

      // 1. FFmpeg 进度停滞 —— 可能 reconnect 全部失败了
      const elapsed = Date.now() - rt.lastProgressTime
      if (elapsed > PROGRESS_STALL_THRESHOLD) {
        console.warn(`[FFmpeg ${recordingId}] No progress for ${Math.round(elapsed / 1000)}s (FFmpeg's internal reconnects likely exhausted). Stopping.`)
        this.stopRecording(recordingId).catch(() => { /* swallow */ })
        return
      }

      // 2. 低码率检测：直播结束后 CDN 可能持续发空帧
      if (status) {
        const now = Date.now()
        const dtMs = now - rt.lastBytesSnapshotTime
        if (dtMs > 0) {
          const bytesPerSec = ((status.bytesReceived - rt.lastBytesSnapshot) / dtMs) * 1000
          rt.lastBytesSnapshot = status.bytesReceived
          rt.lastBytesSnapshotTime = now

          if (bytesPerSec < LOW_BITRATE_THRESHOLD && status.bytesReceived > 1024 * 1024) {
            if (!rt.lowBitrateSince) {
              rt.lowBitrateSince = now
              console.warn(`[FFmpeg ${recordingId}] Low bitrate detected: ${Math.round(bytesPerSec / 1024)}KB/s`)
            } else if (now - rt.lowBitrateSince > LOW_BITRATE_DURATION) {
              console.warn(`[FFmpeg ${recordingId}] Low bitrate persisted, live stream ended. Stopping.`)
              this.stopRecording(recordingId).catch(() => { /* swallow */ })
              return
            }
          } else {
            if (rt.lowBitrateSince) {
              console.log(`[FFmpeg ${recordingId}] Bitrate recovered: ${Math.round(bytesPerSec / 1024)}KB/s`)
            }
            rt.lowBitrateSince = null
          }
        }
      }
    }, HEALTH_CHECK_INTERVAL)
  }

  private startDiskCheck(recordingId: string, outputDir: string): void {
    const rt = this.runtimeState.get(recordingId)
    if (!rt) return
    rt.diskTimer = setInterval(() => {
      const disk = this.checkDiskSpace(outputDir)
      if (!disk.ok) {
        console.error(`[FFmpeg ${recordingId}] Disk space critically low (${Math.round(disk.free / 1024 / 1024)}MB), stopping recording`)
        this.stopRecording(recordingId).catch(() => { /* swallow */ })
      } else if (disk.warning) {
        console.warn(`[FFmpeg ${recordingId}] Disk space low: ${Math.round(disk.free / 1024 / 1024)}MB remaining`)
      }
    }, DISK_CHECK_INTERVAL)
  }

  private startSegmentTimer(config: RecordingConfig): void {
    const rt = this.runtimeState.get(config.recordingId)
    if (!rt) return
    const seconds = Math.floor(Number(config.segmentDuration) || 0)
    if (seconds <= 0) return

    rt.segmentTimer = setTimeout(() => {
      const status = this.statuses.get(config.recordingId)
      if (!status || status.status !== 'recording') return
      console.log(`[FFmpeg ${config.recordingId}] Segment duration reached (${seconds}s), rotating recording`)
      this.stopRecording(config.recordingId).catch((err) => {
        console.error(`[FFmpeg ${config.recordingId}] Segment stop failed:`, (err as Error).message)
      })
    }, seconds * 1000)
  }

  private cleanupRuntimeState(recordingId: string): void {
    const rt = this.runtimeState.get(recordingId)
    if (!rt) return
    rt.stopping = true
    if (rt.healthTimer) { clearInterval(rt.healthTimer); rt.healthTimer = null }
    if (rt.diskTimer) { clearInterval(rt.diskTimer); rt.diskTimer = null }
    if (rt.segmentTimer) { clearTimeout(rt.segmentTimer); rt.segmentTimer = null }
    this.runtimeState.delete(recordingId)
  }

  async stopRecording(recordingId: string): Promise<RecordingStatus | null> {
    const proc = this.processes.get(recordingId)
    const status = this.statuses.get(recordingId)
    if (!proc || !status) return status || null

    const rt = this.runtimeState.get(recordingId)
    if (rt) rt.stopping = true

    return new Promise<RecordingStatus>((resolve) => {
      const onExit = (): void => resolve(status)
      if (proc.exitCode !== null) { onExit(); return }
      proc.once('exit', onExit)

      this.cleanupRuntimeState(recordingId)
      try { proc.stdin?.write('q') } catch { /* ignore */ }

      const killTimer = setTimeout(() => {
        if (proc.exitCode === null) {
          console.warn(`[FFmpeg ${recordingId}] Graceful stop timed out, sending SIGTERM`)
          proc.kill('SIGTERM')
        }
      }, GRACEFUL_STOP_TIMEOUT)

      proc.once('exit', () => clearTimeout(killTimer))
    })
  }

  getStatus(recordingId: string): RecordingStatus | null {
    return this.statuses.get(recordingId) || null
  }

  getActiveRecordings(): RecordingStatus[] {
    return Array.from(this.statuses.values()).filter((s) => s.status === 'recording')
  }

  getAllStatuses(): RecordingStatus[] {
    return Array.from(this.statuses.values())
  }

  stopAll(): void {
    for (const [recordingId, proc] of Array.from(this.processes.entries())) {
      console.log(`[FFmpeg] Stopping recording ${recordingId} on app quit`)
      this.cleanupRuntimeState(recordingId)
      try { proc.stdin?.write('q') } catch { /* ignore */ }
      setTimeout(() => {
        if (proc.exitCode === null) proc.kill('SIGTERM')
      }, 2000)
      const status = this.statuses.get(recordingId)
      if (status) status.status = 'stopped'
    }
  }
}
