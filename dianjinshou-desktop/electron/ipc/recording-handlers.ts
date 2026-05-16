import { ipcMain, shell } from 'electron'
import { spawn } from 'child_process'
import { join, dirname, basename, extname } from 'path'
import { app } from 'electron'
import { RecordingService, StartRecordingOptions } from '../services/recording-service'
import { StorageService } from '../services/storage-service'
import { MonitorService, MonitorConfig } from '../services/monitor-service'
import { RecordingQueue } from '../services/recording-queue'
import { ReconnectService } from '../services/reconnect-service'
import { getLiveInfoByPlatform, refreshStreamUrlByPlatform } from '../services/live-platform-service'
import { LocalAsrService, disposeLocalAsr } from '../services/local-asr-service'
import { getAsrModelManager } from '../services/asr-model-manager'
import { backendApi } from '../services/backend-api'
import { feishuTaskWorker } from '../services/feishu-task-worker'
import { recoverOrphanedRecordings, recoverTranscribingRecordings } from '../services/recovery-service'
import { remuxFlvToMp4 } from '../services/remux-service'
import { getCloudUploadQueue, CloudUploadQueueItem } from '../services/cloud-upload-queue'
import { downloadCloudFile } from '../services/cloud-download-service'
import { existsSync, mkdirSync, statSync, writeFileSync } from 'fs'

let recordingService: RecordingService
let storageService: StorageService
let monitorService: MonitorService
let recordingQueue: RecordingQueue
let reconnectService: ReconnectService

function sanitizeGeneratedUploadName(name: string): string {
  const normalized = name.replace(/[\\/:*?"<>|]/g, '_').trim()
  return normalized || 'cloud-upload.json'
}

export function registerRecordingHandlers(): void {
  storageService = new StorageService()
  recordingService = new RecordingService(storageService)
  recordingQueue = new RecordingQueue(recordingService)
  reconnectService = new ReconnectService(recordingQueue)
  monitorService = new MonitorService(recordingQueue)

  // When any recording finishes (stream ended / manual stop / error),
  // clean up MonitorService state + RecordingQueue active set
  recordingService.setOnComplete((recordingId) => {
    monitorService.onRecordingFinished(recordingId)
    recordingQueue.onRecordingFinished(recordingId)
  })

  // Inject stream URL refresher for CDN reconnection
  // When FFmpeg detects stream stall, it calls this to get a fresh CDN URL
  recordingService.setStreamUrlRefresher(async (recordingId: string) => {
    // Extract webRid from the active recording config in the queue
    const activeConfigs = recordingQueue.getActiveConfigs()
    const config = activeConfigs.find((c) => c.recordingId === recordingId)
    if (config?.webRid && config.platform) {
      return refreshStreamUrlByPlatform(config.platform, config.webRid, config.resolution)
    }
    return null
  })

  /**
   * Start a new recording
   * Expects: { streamerId, streamUrl, anchorName, options? }
   */
  ipcMain.handle(
    'ipc:recording:start',
    async (
      _event,
      params: {
        streamerId: string
        streamUrl: string
        anchorName: string
        options?: StartRecordingOptions
      }
    ) => {
      try {
        const status = await recordingService.startRecording(
          params.streamerId,
          params.streamUrl,
          params.anchorName,
          params.options
        )
        return { success: true, data: status }
      } catch (err) {
        const message = err instanceof Error ? err.message : String(err)
        console.error('[IPC recording:start] Error:', message)
        return { success: false, error: message }
      }
    }
  )

  /**
   * Stop an active recording
   * Expects: recordingId (string)
   */
  ipcMain.handle('ipc:recording:stop', async (_event, recordingId: string) => {
    try {
      const status = await recordingService.stopRecording(recordingId)
      return { success: true, data: status }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      console.error('[IPC recording:stop] Error:', message)
      return { success: false, error: message }
    }
  })

  /**
   * Get status of a specific recording
   * Expects: recordingId (string)
   */
  ipcMain.handle('ipc:recording:getStatus', (_event, recordingId: string) => {
    try {
      const status = recordingService.getRecordingStatus(recordingId)
      return { success: true, data: status }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      console.error('[IPC recording:getStatus] Error:', message)
      return { success: false, error: message }
    }
  })

  /**
   * Get all recording statuses
   */
  ipcMain.handle('ipc:recording:getAll', () => {
    try {
      const statuses = recordingService.getAllStatuses()
      return { success: true, data: statuses }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      console.error('[IPC recording:getAll] Error:', message)
      return { success: false, error: message }
    }
  })

  /**
   * Get the recordings base directory path
   */
  ipcMain.handle('ipc:storage:getRecordingsPath', async () => {
    return { success: true, data: storageService.getRecordingDir() }
  })

  /**
   * Update the recordings base directory path. The new path must be creatable and writable.
   * Returns the resolved path on success.
   */
  ipcMain.handle('ipc:storage:setRecordingsPath', async (_event, newPath: string) => {
    try {
      const resolved = storageService.setBaseDir(newPath)
      return { success: true, data: resolved }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      console.error('[IPC storage:setRecordingsPath] Error:', message)
      return { success: false, error: message }
    }
  })

  /**
   * Open a folder in the system file explorer
   * Expects: folderPath (string)
   */
  ipcMain.handle('ipc:storage:openFolder', async (_event, folderPath: string) => {
    try {
      await shell.openPath(folderPath)
      return { success: true }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      console.error('[IPC storage:openFolder] Error:', message)
      return { success: false, error: message }
    }
  })

  /**
   * Show a file in its parent folder (highlight the file in file explorer)
   */
  ipcMain.handle('ipc:storage:showItemInFolder', async (_event, filePath: string) => {
    try {
      shell.showItemInFolder(filePath)
      return { success: true }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      console.error('[IPC storage:showItemInFolder] Error:', message)
      return { success: false, error: message }
    }
  })

  /**
   * Open a file with the system default application
   */
  ipcMain.handle('ipc:storage:openFile', async (_event, filePath: string) => {
    try {
      const errMsg = await shell.openPath(filePath)
      if (errMsg) return { success: false, error: errMsg }
      return { success: true }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      console.error('[IPC storage:openFile] Error:', message)
      return { success: false, error: message }
    }
  })

  /**
   * Get disk usage for the recording storage drive
   */
  ipcMain.handle('ipc:storage:getDiskUsage', () => {
    try {
      const usage = storageService.getDiskUsage()
      return { success: true, data: usage }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      console.error('[IPC storage:getDiskUsage] Error:', message)
      return { success: false, error: message }
    }
  })

  // ── Monitor IPC handlers ──

  /**
   * Start monitoring all configured streamers
   */
  ipcMain.handle('ipc:monitor:start', () => {
    try {
      monitorService.startMonitoring()
      return { success: true }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      console.error('[IPC monitor:start] Error:', message)
      return { success: false, error: message }
    }
  })

  /**
   * Stop monitoring
   */
  ipcMain.handle('ipc:monitor:stop', () => {
    try {
      monitorService.stopMonitoring()
      // Also stop all active FFmpeg recordings
      recordingService.stopAll()
      return { success: true }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      console.error('[IPC monitor:stop] Error:', message)
      return { success: false, error: message }
    }
  })

  /**
   * Start live-status polling without enabling recording.
   * It uses the same MonitorService queue as recording mode, so Douyin checks
   * stay single-threaded and rate-limited.
   */
  ipcMain.handle('ipc:monitor:startStatusPolling', () => {
    try {
      monitorService.startLiveStatusPolling()
      return { success: true }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      console.error('[IPC monitor:startStatusPolling] Error:', message)
      return { success: false, error: message }
    }
  })

  /**
   * 立即触发一次直播状态轮询（用于：刚添加新主播后立刻刷新状态，无需等下一个间隔）。
   */
  ipcMain.handle('ipc:monitor:pollNow', () => {
    try {
      monitorService.pollNow()
      return { success: true }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      console.error('[IPC monitor:pollNow] Error:', message)
      return { success: false, error: message }
    }
  })

  /**
   * Stop live-status polling when no recording monitor is active.
   */
  ipcMain.handle('ipc:monitor:stopStatusPolling', () => {
    try {
      monitorService.stopLiveStatusPolling()
      return { success: true }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      console.error('[IPC monitor:stopStatusPolling] Error:', message)
      return { success: false, error: message }
    }
  })

  /**
   * Add a streamer to monitor
   * Expects: MonitorConfig
   */
  ipcMain.handle('ipc:monitor:addStreamer', (_event, config: MonitorConfig) => {
    try {
      monitorService.addStreamer(config)
      return { success: true }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      console.error('[IPC monitor:addStreamer] Error:', message)
      return { success: false, error: message }
    }
  })

  /**
   * Remove a streamer from monitoring
   * Expects: streamerId (string)
   */
  ipcMain.handle('ipc:monitor:removeStreamer', (_event, streamerId: string) => {
    try {
      monitorService.removeStreamer(streamerId)
      return { success: true }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      console.error('[IPC monitor:removeStreamer] Error:', message)
      return { success: false, error: message }
    }
  })

  /**
   * Get monitoring status (isRunning, streamerCount, liveCount) plus streamer list
   */
  /**
   * 设置监测轮询间隔（秒）
   */
  ipcMain.handle('ipc:monitor:setInterval', (_event, seconds: number) => {
    try {
      monitorService.setPollingInterval(Number(seconds) || 60)
      return { success: true }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      console.error('[IPC monitor:setInterval] Error:', message)
      return { success: false, error: message }
    }
  })

  ipcMain.handle('ipc:monitor:getStatus', () => {
    try {
      const status = monitorService.getStatus()
      const streamers = monitorService.getMonitoredStreamers()
      return { success: true, data: { ...status, streamers } }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      console.error('[IPC monitor:getStatus] Error:', message)
      return { success: false, error: message }
    }
  })

  // ── Queue IPC handler ──

  /**
   * Get recording queue status
   */
  ipcMain.handle('ipc:recording:getQueueStatus', () => {
    try {
      const status = recordingQueue.getQueueStatus()
      return { success: true, data: status }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      console.error('[IPC recording:getQueueStatus] Error:', message)
      return { success: false, error: message }
    }
  })

  ipcMain.handle('ipc:cloud-upload:getQueue', () => {
    try {
      return { success: true, data: getCloudUploadQueue().getItems() }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      return { success: false, error: message }
    }
  })

  ipcMain.handle('ipc:cloud-upload:enqueue', (_event, item: Partial<CloudUploadQueueItem> & { filePath: string; businessType: CloudUploadQueueItem['businessType'] }) => {
    try {
      const queued = getCloudUploadQueue().enqueue(item as any)
      getCloudUploadQueue().start()
      return { success: true, data: queued }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      return { success: false, error: message }
    }
  })

  ipcMain.handle('ipc:cloud-upload:enqueueGenerated', (_event, payload: Partial<CloudUploadQueueItem> & {
    businessType: CloudUploadQueueItem['businessType']
    fileName?: string
    content?: unknown
  }) => {
    try {
      const safeName = sanitizeGeneratedUploadName(payload.fileName || `${payload.businessType}_${payload.businessId || Date.now()}.json`)
      const dir = join(app.getPath('userData'), 'cloud-generated-uploads')
      mkdirSync(dir, { recursive: true })
      const filePath = join(dir, `${Date.now()}_${safeName}`)
      const content = typeof payload.content === 'string'
        ? payload.content
        : JSON.stringify(payload.content ?? {}, null, 2)
      writeFileSync(filePath, content, 'utf8')
      const { content: _content, ...queuePayload } = payload
      const queued = getCloudUploadQueue().enqueue({
        ...queuePayload,
        filePath,
        fileName: safeName,
        contentType: payload.contentType || 'application/json',
      } as any)
      getCloudUploadQueue().start()
      return { success: true, data: queued }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      return { success: false, error: message }
    }
  })

  ipcMain.handle('ipc:cloud-download:file', async (_event, payload: { url: string; fileName?: string }) => {
    try {
      const result = await downloadCloudFile(payload.url, payload.fileName)
      return result.success
        ? { success: true, data: { path: result.path } }
        : { success: false, canceled: result.canceled, error: result.error }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      console.error('[IPC cloud-download:file] Error:', message)
      return { success: false, error: message }
    }
  })

  // ── Auth token sync ──

  let recoveryDone = false
  const onTokenSet = () => {
    if (!recoveryDone) {
      recoveryDone = true
      recoverOrphanedRecordings().catch((err) =>
        console.error('[Recovery] Background recovery failed:', err)
      )
      // v1.1.0：录制类恢复完了，再处理 ASR 跑一半卡住的
      recoverTranscribingRecordings().catch((err) =>
        console.error('[AsrRecovery] Background recovery failed:', err)
      )
    }
    feishuTaskWorker.start()
    getCloudUploadQueue().start()
  }

  ipcMain.handle('ipc:auth:setToken', (_event, token: string) => {
    try {
      backendApi.setAuthToken(token)
      onTokenSet()
      return { success: true }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      return { success: false, error: message }
    }
  })

  // 两端同步 refresh token —— 主进程需要 refresh token 才能在 401 时自己 refresh access token
  ipcMain.handle('ipc:auth:setTokens', (_event, payload: { access: string; refresh: string }) => {
    try {
      backendApi.setTokens(payload.access, payload.refresh)
      onTokenSet()
      return { success: true }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      return { success: false, error: message }
    }
  })

  // ── Douyin IPC handler ──

  /**
   * Resolve Douyin live room from URL/ID input
   * Returns: streamer name, live status, room ID, web_rid
   */
  ipcMain.handle('ipc:douyin:resolve', async (_event, input: string) => {
    try {
      const info = await getLiveInfoByPlatform('douyin', input)
      return {
        success: !info.error,
        data: {
          webRid: info.webRid,
          roomId: info.roomId,
          streamerName: info.streamerName,
          streamerAvatar: info.streamerAvatar,
          secUid: info.secUid,
          isLive: info.isLive,
          streamUrl: info.streamUrl,
        },
        error: info.error || undefined,
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      console.error('[IPC douyin:resolve] Error:', message)
      return { success: false, error: message }
    }
  })

  // Kuaishou IPC handler
  ipcMain.handle('ipc:kuaishou:resolve', async (_event, input: string) => {
    try {
      const info = await getLiveInfoByPlatform('kuaishou', input)
      return {
        success: !info.error || Boolean(info.webRid || info.roomId),
        data: {
          webRid: info.webRid,
          roomId: info.roomId,
          accountId: info.webRid || info.roomId,
          streamerName: info.streamerName,
          streamerAvatar: info.streamerAvatar,
          isLive: info.isLive,
          streamUrl: info.streamUrl,
        },
        error: info.error || undefined,
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      console.error('[IPC kuaishou:resolve] Error:', message)
      return { success: false, error: message }
    }
  })

  // ── ASR IPC handler ──

  /**
   * Run ASR transcription on a local audio/video file.
   * Returns ASR segments for the frontend to submit to the server.
   */
  ipcMain.handle(
    'ipc:asr:run',
    async (_event, filePath: string) => {
      try {
        const mgr = getAsrModelManager()
        if (!mgr.isReady()) {
          const ok = await mgr.ensureModelReady()
          if (!ok) {
            const lastError = mgr.getStatus().lastError
            return {
              success: false,
              error: '本地 ASR 模型未就绪，请重新安装最新版客户端' + (lastError ? `：${lastError}` : ''),
            }
          }
        }
        const localAsr = new LocalAsrService()
        const segments = await localAsr.transcribe(filePath)
        return {
          success: true,
          data: segments.map((s) => ({
            segmentIndex: s.segmentIndex,
            startTime: s.startTime,
            endTime: s.endTime,
            text: s.text,
          })),
        }
      } catch (err) {
        const message = err instanceof Error ? err.message : String(err)
        console.error('[IPC asr:run] Error:', message)
        return { success: false, error: message }
      }
    }
  )

  // ---- ASR provider 兼容接口 + 内置模型状态 ----

  ipcMain.handle('ipc:asr:getProvider', () => {
    return { success: true, provider: 'local' }
  })

  ipcMain.handle('ipc:asr:setProvider', (_e, provider: string) => {
    if (provider !== 'local') {
      return { success: false, error: '当前版本仅支持本机 ASR 语音识别' }
    }
    return { success: true, provider: 'local' }
  })

  ipcMain.handle('ipc:asr:getModelStatus', () => {
    return { success: true, data: getAsrModelManager().getStatus() }
  })

  ipcMain.handle('ipc:asr:downloadModel', async () => {
    try {
      const ok = await getAsrModelManager().ensureModelReady()
      return { success: ok, data: getAsrModelManager().getStatus() }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      return { success: false, error: message }
    }
  })

  ipcMain.handle('ipc:asr:removeModel', async () => {
    try {
      disposeLocalAsr()
      await getAsrModelManager().removeModel()
      return { success: true }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      return { success: false, error: message }
    }
  })

  /**
   * Ensure the recording file is in MP4 format. FLV inputs are remuxed in place
   * (original FLV is deleted on success). Returns the effective MP4 path.
   */
  ipcMain.handle('ipc:video:ensureMp4', async (_event, filePath: string) => {
    try {
      if (!filePath) return { success: false, error: '路径为空' }
      if (!existsSync(filePath)) return { success: false, error: '文件不存在：' + filePath }
      const ext = extname(filePath).toLowerCase()
      if (ext === '.mp4') return { success: true, path: filePath, converted: false }
      if (ext !== '.flv') {
        return { success: false, error: '不支持的视频格式：' + ext }
      }
      const mp4Path = filePath.replace(/\.flv$/i, '.mp4')
      // statSync just to fail early if file is empty/unreadable
      statSync(filePath)
      await remuxFlvToMp4(filePath, mp4Path)
      return { success: true, path: mp4Path, converted: true }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      console.error('[IPC video:ensureMp4] Error:', message)
      return { success: false, error: message }
    }
  })

  /**
   * Extract a clip from a video file using FFmpeg
   * Input: { sourcePath, clipStart (seconds), clipEnd (seconds) }
   * Returns: { success, clipPath, error? }
   */
  ipcMain.handle(
    'ipc:clip:extract',
    async (
      _event,
      config: { sourcePath: string; clipStart: number; clipEnd: number; clipFilename?: string }
    ) => {
      try {
        const { sourcePath, clipStart, clipEnd, clipFilename } = config
        const dir = dirname(sourcePath)
        const ext = extname(sourcePath) || '.mp4'
        const name = clipFilename || `clip_${clipStart}-${clipEnd}`
        const clipPath = join(dir, `${name}${ext}`)

        const ffmpegBin = getFFmpegBin()
        const duration = clipEnd - clipStart

        const args = [
          '-y',
          '-ss', String(clipStart),
          '-i', sourcePath,
          '-t', String(duration),
          '-c', 'copy',
          '-movflags', '+faststart',
          clipPath,
        ]

        console.log(`[Clip] Extracting: ${ffmpegBin} ${args.join(' ')}`)

        await new Promise<void>((resolve, reject) => {
          const proc = spawn(ffmpegBin, args, { stdio: ['ignore', 'pipe', 'pipe'] })
          let stderr = ''
          proc.stderr?.on('data', (d: Buffer) => { stderr += d.toString() })
          proc.on('close', (code) => {
            if (code === 0) resolve()
            else reject(new Error(`FFmpeg exited with code ${code}: ${stderr.slice(-500)}`))
          })
          proc.on('error', reject)
        })

        console.log(`[Clip] Success: ${clipPath}`)
        return { success: true, clipPath }
      } catch (err) {
        const message = err instanceof Error ? err.message : String(err)
        console.error('[IPC clip:extract] Error:', message)
        return { success: false, error: message }
      }
    }
  )
}

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
 * Get the recording service instance (for use in main.ts before-quit handler)
 */
export function getRecordingService(): RecordingService {
  return recordingService
}

/**
 * Get the monitor service instance (for use in main.ts before-quit handler)
 */
export function getMonitorService(): MonitorService {
  return monitorService
}

/**
 * Get the reconnect service instance (for use in main.ts before-quit handler)
 */
export function getReconnectService(): ReconnectService {
  return reconnectService
}
