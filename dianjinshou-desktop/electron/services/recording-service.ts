import { FFmpegManager, RecordingConfig, RecordingStatus } from './ffmpeg-manager'
import { StorageService } from './storage-service'
import { backendApi } from './backend-api'
import { LocalAsrService } from './local-asr-service'
import { getAsrModelManager } from './asr-model-manager'
import { reportMainError } from './main-error-reporter'
import { getCloudUploadQueue } from './cloud-upload-queue'
import { join, basename } from 'path'
import { statSync } from 'fs'
import { spawnSync } from 'child_process'
import { app } from 'electron'

export interface StartRecordingOptions {
  platform?: 'douyin' | 'kuaishou'
  resolution?: '480p' | '720p' | '1080p' | 'source'
  segmentDuration?: number
  outputDir?: string
  /** 可选：由调用方预先分配的 recordingId。若不传则本服务生成。Queue 会传入，保证两侧 key 一致。 */
  recordingId?: string
}

export class RecordingService {
  private ffmpegManager: FFmpegManager
  private storageService: StorageService
  // Maps local recordingId → Promise that resolves to backend recording DB id
  private backendIdPromises: Map<string, Promise<number | null>> = new Map()
  private cloudMetaByRecordingId: Map<string, { streamerId: number; anchorName: string; recordedAt: string }> = new Map()
  private externalCompleteCallback?: (recordingId: string, finalPath: string) => void

  constructor(storageService?: StorageService) {
    this.ffmpegManager = new FFmpegManager()
    // Share the same StorageService instance as the IPC handlers so updates from the UI
    // (basic-settings save-location picker → ipc:storage:setRecordingsPath) take effect for new recordings.
    this.storageService = storageService ?? new StorageService()

    // When FFmpeg finishes recording (after remux), notify backend + external listeners
    this.ffmpegManager.setOnRecordingComplete((recordingId, finalPath) => {
      this.notifyRecordingComplete(recordingId, finalPath)
      this.externalCompleteCallback?.(recordingId, finalPath)
    })
  }

  /**
   * Set external callback for when any recording finishes.
   * Used by MonitorService/RecordingQueue to clean up state.
   */
  setOnComplete(callback: (recordingId: string, finalPath: string) => void): void {
    this.externalCompleteCallback = callback
  }

  /**
   * Set stream URL refresher for CDN reconnection.
   * Called by FFmpegManager when a stream disconnects and needs a fresh URL.
   */
  setStreamUrlRefresher(refresher: (recordingId: string) => Promise<string | null>): void {
    this.ffmpegManager.setStreamUrlRefresher(refresher)
  }

  /**
   * Start recording a livestream
   *
   * 1. Prepare output directory
   * 2. Start FFmpeg via FFmpegManager
   * 3. Return recording status
   */
  async startRecording(
    streamerId: string,
    streamUrl: string,
    anchorName: string,
    options: StartRecordingOptions = {}
  ): Promise<RecordingStatus> {
    const recordingId = options.recordingId || `${streamerId}_${Date.now()}`

    // Determine output directory: <baseDir>/<YYYY-MM-DD>/<anchorName>
    const baseDir = options.outputDir || this.storageService.getRecordingDir()
    const today = new Date()
    const dateDir = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
    const outputDir = join(baseDir, dateDir, anchorName)
    this.storageService.ensureDir(outputDir)

    const config: RecordingConfig = {
      recordingId,
      streamUrl,
      outputDir,
      anchorName,
      platform: options.platform,
      resolution: options.resolution || 'source',
      segmentDuration: options.segmentDuration || 1800,
    }

    // Start FFmpeg process
    const status = this.ffmpegManager.startRecording(config)

    // 首字节门控：CDN 链接不通 / 主播下播的瞬间碰上 poll，FFmpeg 会拿不到任何数据。
    // 等最多 20s 看是否有字节到达 —— 没有就直接放弃，不落 DB 记录，避免"幽灵失败记录"堆积。
    // 20s 给 CDN 冷启动 / 流刚切换的场景留余地；FFmpeg 自身 rw_timeout 是 30s 兜底
    const FIRST_BYTE_TIMEOUT_MS = 20000
    const deadline = Date.now() + FIRST_BYTE_TIMEOUT_MS
    while (Date.now() < deadline) {
      if (status.bytesReceived > 0) break
      // 如果 ffmpeg-manager 内部已经把状态切成非 recording（比如立刻 error），也别继续等
      if (status.status !== 'recording') break
      await new Promise((r) => setTimeout(r, 200))
    }
    if (status.bytesReceived === 0) {
      console.warn(`[RecordingService] ${recordingId} received 0 bytes within ${FIRST_BYTE_TIMEOUT_MS}ms, aborting (no DB record created)`)
      try { await this.ffmpegManager.stopRecording(recordingId) } catch { /* ignore */ }
      status.status = 'error'
      status.error = '20 秒内未收到任何数据（CDN 不可达或流已结束）'
      return status
    }

    // Notify backend that recording started (store as Promise so completion callback can await it)
    const streamerIdNum = parseInt(streamerId, 10)
    if (!isNaN(streamerIdNum)) {
      this.cloudMetaByRecordingId.set(recordingId, {
        streamerId: streamerIdNum,
        anchorName,
        recordedAt: formatLocalDateTime(new Date()),
      })
      // 用实际磁盘文件名（ffmpeg-manager.buildPaths 生成，本地时区），
      // 不要自己再用 toISOString() 拼 — 那会出 UTC 和磁盘名不一致，分析时找不到文件。
      const diskFileName = status.mp4Path ? basename(status.mp4Path) : `${anchorName}_${Date.now()}.mp4`
      const backendIdPromise = backendApi
        .createRecording({
          streamerId: streamerIdNum,
          localFilePath: config.outputDir,
          localFileName: diskFileName,
          streamUrl,
          resolution: options.resolution || 'source',
          sessionId: recordingId,
        })
        .then((result) => {
          if (result?.id) {
            console.log(`[RecordingService] Backend recording created: id=${result.id}`)
            return result.id
          }
          return null
        })
        .catch((err) => {
          console.error('[RecordingService] Failed to create backend recording:', err)
          return null
        })
      this.backendIdPromises.set(recordingId, backendIdPromise)
    }

    return status
  }

  /**
   * Stop an active recording
   *
   * 1. Stop FFmpeg
   * 2. Update backend recording status
   * 3. Return final recording info
   */
  async stopRecording(recordingId: string): Promise<RecordingStatus | null> {
    const status = await this.ffmpegManager.stopRecording(recordingId)
    // Backend notification happens via onRecordingComplete callback after remux
    return status
  }

  /**
   * Called after FFmpeg exits and remux completes.
   * Notifies the backend with final file info.
   */
  private async notifyRecordingComplete(recordingId: string, finalPath: string): Promise<void> {
    const backendIdPromise = this.backendIdPromises.get(recordingId)
    if (!backendIdPromise) return

    try {
      // Await the backend ID — this fixes the race condition where FFmpeg exits
      // before createRecording API call completes
      const backendId = await backendIdPromise
      if (!backendId) return

      // Check if file actually exists and has content
      let fileSize = 0
      let fileExists = false
      try {
        const stats = statSync(finalPath)
        fileSize = stats.size
        fileExists = fileSize > 0
      } catch {
        // File doesn't exist
      }

      const status = this.ffmpegManager.getStatus(recordingId)
      const startTime = status?.startTime || Date.now()
      // Wall-clock duration is over-counted when CDN stalls and FFmpeg keeps reconnecting
      // for ~60-150s producing no new bytes. Probe the actual MP4 length so the DB / quota
      // reflect real content, not idle waiting time. Falls back to wall-clock on probe failure.
      const wallClockDuration = Math.round((Date.now() - startTime) / 1000)
      const probedDuration = probeMediaDuration(finalPath)
      const duration = probedDuration > 0 ? probedDuration : wallClockDuration
      if (probedDuration > 0 && Math.abs(probedDuration - wallClockDuration) > 5) {
        console.log(`[RecordingService] Duration: probed=${probedDuration}s wallClock=${wallClockDuration}s (using probed)`)
      }

      if (fileExists) {
        await backendApi.completeRecording(backendId, {
          localFilePath: finalPath,
          localFileName: basename(finalPath),
          duration,
          fileSize,
          status: 'completed',
        })
        console.log(`[RecordingService] Backend recording completed: id=${backendId}`)
        await this.enqueueCloudRecordingIfEnabled(recordingId, backendId, finalPath, fileSize, duration)

        // v1.1.0：录制完成后只自动生成逐字稿；是否触发 AI 复盘由用户在详情页手动决定。
        this.runAsrAndSubmit(backendId, finalPath).catch((err) => {
          reportMainError('asr.autoPipeline', err, { recordingId: backendId, filePath: finalPath })
          console.error('[RecordingService] ASR failed:', err.message || err)
        })
      } else {
        // No file produced — mark as failed in backend
        await backendApi.completeRecording(backendId, {
          duration,
          status: 'failed',
          errorMsg: 'CDN连接失败或流中断，未生成录制文件',
        })
        console.warn(`[RecordingService] Backend recording marked failed (no file): id=${backendId}`)
      }
    } catch (err) {
      console.error('[RecordingService] Failed to complete backend recording:', err)
    } finally {
      this.backendIdPromises.delete(recordingId)
      this.cloudMetaByRecordingId.delete(recordingId)
    }
  }

  private async enqueueCloudRecordingIfEnabled(
    localRecordingId: string,
    backendRecordingId: number,
    finalPath: string,
    fileSize: number,
    durationSeconds: number
  ): Promise<void> {
    const meta = this.cloudMetaByRecordingId.get(localRecordingId)
    if (!meta) return
    const streamer = await backendApi.getStreamer(meta.streamerId)
    if (!streamer?.cloudSyncEnabled) return

    getCloudUploadQueue().enqueue({
      id: `full_recap_${backendRecordingId}`,
      filePath: finalPath,
      fileName: basename(finalPath),
      contentType: 'video/mp4',
      businessType: 'full_recap',
      businessId: backendRecordingId,
      recordingId: backendRecordingId,
      streamerId: meta.streamerId,
      anchorName: streamer.anchorName || meta.anchorName,
      industryId: streamer.industryId,
      accountType: streamer.accountType,
      recordedAt: meta.recordedAt,
      durationSeconds,
    })
    console.log(`[RecordingService] Cloud upload queued: recordingId=${backendRecordingId}, bytes=${fileSize}`)
  }

  /**
   * Run bundled local ASR on the recording file, and submit results to server.
   * Called in background after recording completion.
   */
  private async runAsrAndSubmit(backendId: number, finalPath: string): Promise<void> {
    const mgr = getAsrModelManager()
    if (!mgr.isReady()) {
      const ok = await mgr.ensureModelReady()
      if (!ok) {
        console.warn('[RecordingService] Bundled local ASR model missing, skipping ASR:', mgr.getStatus().lastError)
        return
      }
    }

    const localAsr = new LocalAsrService()
    console.log('[RecordingService] Starting LOCAL ASR for recording: %s', finalPath)
    const segments = await localAsr.transcribe(finalPath)
    console.log('[RecordingService] ASR completed: %d segments', segments.length)

    // 3. Submit transcript to server — v1.1.0 起不自动触发 AI 分析
    //    用户在复盘详情页手动点"AI 复盘"按钮才走 /analysis/{id}/start-ai
    if (segments.length > 0) {
      const result = await backendApi.submitAsrResult({
        recordingId: backendId,
        segments,
        autoAnalyze: false,   // 关键开关：仅写逐字稿，状态停在 transcribed
      })
      if (result) {
        console.log('[RecordingService] Transcript submitted (manual AI), taskId=%s status=%s',
          result.taskId, result.status)
      }
    } else {
      console.warn('[RecordingService] ASR produced no segments, skipping analysis')
    }
  }

  /**
   * Get status of a specific recording
   */
  getRecordingStatus(recordingId: string): RecordingStatus | null {
    return this.ffmpegManager.getStatus(recordingId)
  }

  /**
   * Get all active recordings
   */
  getActiveRecordings(): RecordingStatus[] {
    return this.ffmpegManager.getActiveRecordings()
  }

  /**
   * Get all recording statuses (including stopped/error)
   */
  getAllStatuses(): RecordingStatus[] {
    return this.ffmpegManager.getAllStatuses()
  }

  /**
   * Stop all active recordings
   */
  stopAll(): void {
    this.ffmpegManager.stopAll()
  }
}

/** Locate the FFmpeg binary (mirrors the logic in ffmpeg-manager). */
function getFFmpegBin(): string {
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

/**
 * Read the actual playable duration from a media file by parsing the FFmpeg
 * "Duration: HH:MM:SS.ss" line on stderr. Returns whole seconds, or 0 on failure.
 */
function probeMediaDuration(filePath: string): number {
  try {
    const proc = spawnSync(getFFmpegBin(), ['-hide_banner', '-i', filePath], {
      encoding: 'utf8',
      timeout: 15000,
      windowsHide: true,
    })
    // FFmpeg exits with code 1 when no output is requested — that's expected.
    const stderr = (proc.stderr || '') + (proc.stdout || '')
    const m = stderr.match(/Duration:\s*(\d+):(\d+):(\d+(?:\.\d+)?)/)
    if (!m) return 0
    const h = parseInt(m[1], 10)
    const min = parseInt(m[2], 10)
    const sec = parseFloat(m[3])
    return Math.round(h * 3600 + min * 60 + sec)
  } catch {
    return 0
  }
}

function formatLocalDateTime(date: Date): string {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}
