import { RecordingQueue, QueuedRecording } from './recording-queue'
import { buildRoomUrl, getLiveInfoByPlatform, refreshStreamUrlByPlatform } from './live-platform-service'
import { backendApi } from './backend-api'

export interface MonitorConfig {
  streamerId: string
  platform: string
  roomId: string
  roomUrl: string
  anchorName: string
  resolution: '480p' | '720p' | '1080p' | 'source'
  segmentDuration: number
  /** Whether this streamer should start recording when recording mode is enabled. */
  autoRecord?: boolean
}

interface StreamerLiveState {
  isLive: boolean
  streamUrl: string | null
  recordingId: string | null
  webRid: string | null
}

/** 每个主播之间的检测间隔（ms），防止请求过于密集触发风控（抖音 IP 风控阈值约 2-3s/req） */
const PER_STREAMER_DELAY = 4000

export class MonitorService {
  private pollingInterval = 60000 // 60秒，避免触发抖音风控
  private monitoredStreamers: Map<string, MonitorConfig> = new Map()
  private liveStates: Map<string, StreamerLiveState> = new Map()
  private isPolling = false
  private statusPollingRequested = false
  private recordingEnabled = false
  private timer: ReturnType<typeof setTimeout> | null = null
  private recordingQueue: RecordingQueue
  private pollInProgress = false // 防止轮询重入
  private pollAgainAfterCurrent = false

  constructor(recordingQueue: RecordingQueue) {
    this.recordingQueue = recordingQueue
  }

  /**
   * 录制完成后清理状态（由外部调用，如 FFmpeg 录制结束后）
   * 这样下次轮询时不会因为 recordingId 存在而跳过检测
   */
  onRecordingFinished(recordingId: string): void {
    for (const [streamerId, state] of this.liveStates) {
      if (state.recordingId === recordingId) {
        console.log(`[Monitor] Recording ${recordingId} finished, clearing state for streamer ${streamerId}`)
        state.recordingId = null
        state.isLive = false
        state.streamUrl = null
        this.requestPollSoon()
        break
      }
    }
  }

  startMonitoring(): void {
    this.recordingEnabled = true
    console.log('[Monitor] Recording mode enabled')

    this.ensurePolling()
    this.requestPollSoon()
  }

  startLiveStatusPolling(): void {
    this.statusPollingRequested = true
    console.log('[Monitor] Live status polling requested')
    this.ensurePolling()
  }

  /**
   * 立刻触发一次 reconcile + poll，用于：用户刚添加新主播后无需等下一个 interval。
   * 已在 polling 时插入抢占式调度；未启动时同时开启 polling。
   */
  pollNow(): void {
    if (!this.isPolling) {
      this.statusPollingRequested = true
      this.ensurePolling()
      return
    }
    this.requestPollSoon()
  }

  stopLiveStatusPolling(): void {
    this.statusPollingRequested = false
    if (!this.recordingEnabled) {
      this.stopPolling()
    }
  }

  /**
   * 调整轮询间隔（秒）。下一次调度开始生效 —— 不中断当前进行中的 poll。
   * 合法范围：10-3600 秒，越界会 clamp。
   */
  setPollingInterval(seconds: number): void {
    const sec = Math.max(10, Math.min(3600, Math.floor(seconds)))
    this.pollingInterval = sec * 1000
    console.log('[Monitor] polling interval updated to', sec, 's')
  }

  stopMonitoring(): void {
    if (!this.recordingEnabled) {
      console.log('[Monitor] Recording mode is not running')
    }

    this.recordingEnabled = false

    // 停掉所有正在录的 ffmpeg（每个 ffmpeg 退出会触发 notifyRecordingComplete
    // → 后端 recording.status 从 'recording' 变为 'completed'）
    const activeIds: string[] = []
    for (const state of this.liveStates.values()) {
      if (state.recordingId) {
        activeIds.push(state.recordingId)
      }
    }
    console.log(`[Monitor] Stopping ${activeIds.length} active recording(s)...`)
    for (const rid of activeIds) {
      this.recordingQueue.dequeue(rid).catch((err) =>
        console.error(`[Monitor] Failed to stop recording ${rid}:`, (err as Error).message)
      )
    }
    // 只清空录制状态，保留 live 状态给主播列表展示。
    for (const state of this.liveStates.values()) {
      state.streamUrl = null
      state.recordingId = null
    }

    if (!this.statusPollingRequested) {
      this.stopPolling()
    }

    console.log('[Monitor] Recording mode stopped')
  }

  addStreamer(config: MonitorConfig): void {
    const existing = this.monitoredStreamers.get(config.streamerId)
    this.monitoredStreamers.set(config.streamerId, {
      ...existing,
      ...config,
      autoRecord: config.autoRecord ?? true,
    })
    if (!this.liveStates.has(config.streamerId)) {
      this.liveStates.set(config.streamerId, {
        isLive: false,
        streamUrl: null,
        recordingId: null,
        webRid: null,
      })
    }
    console.log(`[Monitor] Upserted streamer ${config.anchorName} (${config.streamerId}), autoRecord=${config.autoRecord ?? true}`)
  }

  removeStreamer(streamerId: string): void {
    const config = this.monitoredStreamers.get(streamerId)
    if (config) {
      config.autoRecord = false
      console.log(`[Monitor] Disabled auto-record for ${config.anchorName} (${streamerId})`)
    }
  }

  getMonitoredStreamers(): Array<MonitorConfig & { isLive: boolean; recordingId: string | null }> {
    const result: Array<MonitorConfig & { isLive: boolean; recordingId: string | null }> = []
    for (const [streamerId, config] of this.monitoredStreamers) {
      const state = this.liveStates.get(streamerId)
      result.push({
        ...config,
        isLive: state?.isLive ?? false,
        recordingId: state?.recordingId ?? null,
      })
    }
    return result
  }

  getStatus(): { isRunning: boolean; streamerCount: number; liveCount: number; statusPolling: boolean } {
    let liveCount = 0
    for (const state of this.liveStates.values()) {
      if (state.isLive) liveCount++
    }
    return {
      // isRunning keeps the old renderer meaning: recording mode is active.
      isRunning: this.recordingEnabled,
      streamerCount: this.monitoredStreamers.size,
      liveCount,
      statusPolling: this.isPolling,
    }
  }

  private ensurePolling(): void {
    if (this.isPolling) return
    this.isPolling = true
    console.log('[Monitor] Started unified live-status polling with interval', this.pollingInterval, 'ms')
    this.schedulePoll()
  }

  private stopPolling(): void {
    this.isPolling = false
    this.pollAgainAfterCurrent = false
    if (this.timer) {
      clearTimeout(this.timer)
      this.timer = null
    }
    console.log('[Monitor] Stopped unified live-status polling')
  }

  private requestPollSoon(): void {
    if (!this.isPolling) return
    if (this.pollInProgress) {
      this.pollAgainAfterCurrent = true
      return
    }
    if (this.timer) {
      clearTimeout(this.timer)
      this.timer = null
    }
    this.schedulePoll()
  }

  /** 调度下一次轮询 */
  private schedulePoll(): void {
    if (!this.isPolling) return
    // 先执行一次，然后等待间隔再调度下一次
    this.poll().finally(() => {
      if (this.isPolling) {
        if (this.pollAgainAfterCurrent) {
          this.pollAgainAfterCurrent = false
          this.schedulePoll()
        } else {
          this.timer = setTimeout(() => this.schedulePoll(), this.pollingInterval)
        }
      }
    })
  }

  /**
   * 轮询所有主播的开播状态
   * 每个主播之间间隔 PER_STREAMER_DELAY 防止限流
   */
  /**
   * Reconcile the local streamer map with the backend's current list.
   * The same polling pass updates live status for every streamer and only
   * starts recording for streamers whose autoRecord flag is enabled.
   */
  private async reconcileFromBackend(): Promise<void> {
    const remote = await backendApi.listAllStreamers()
    if (!remote || remote.length === 0) {
      // Empty response likely means auth issue; don't wipe our local map.
      return
    }
    const remoteIds = new Set(remote.map((s) => String(s.id)))

    // Add or update streamers.
    for (const s of remote) {
      const id = String(s.id)
      const cfg: MonitorConfig = {
        streamerId: id,
        platform: s.platform,
        roomId: s.accountId || '',
        roomUrl: buildRoomUrl(s.platform, s.accountId),
        anchorName: s.anchorName || s.accountId || id,
        resolution: 'source',
        segmentDuration: 1800,
        autoRecord: s.isMonitoring,
      }
      if (this.monitoredStreamers.has(id)) {
        const existing = this.monitoredStreamers.get(id)!
        this.monitoredStreamers.set(id, {
          ...existing,
          ...cfg,
          resolution: existing.resolution,
          segmentDuration: existing.segmentDuration,
        })
      } else {
        this.addStreamer(cfg)
        console.log(`[Monitor] Reconciled from backend: +${cfg.anchorName}`)
      }
    }

    // Remove ones deleted from the backend.
    for (const id of Array.from(this.monitoredStreamers.keys())) {
      if (!remoteIds.has(id)) {
        const cfg = this.monitoredStreamers.get(id)
        this.monitoredStreamers.delete(id)
        this.liveStates.delete(id)
        console.log(`[Monitor] Reconciled from backend: -${cfg?.anchorName ?? id}`)
      }
    }
  }

  private async poll(): Promise<void> {
    if (this.pollInProgress) return
    this.pollInProgress = true

    try {
      // Pull latest streamer list from backend before polling live state.
      // Best-effort — network/auth failures just skip reconcile and we poll what we have.
      try {
        await this.reconcileFromBackend()
      } catch (e) {
        console.warn('[Monitor] reconcileFromBackend failed:', (e as Error).message)
      }

      for (const [streamerId, config] of this.monitoredStreamers) {
        if (!this.isPolling) break

        const state = this.liveStates.get(streamerId)
        if (!state) continue

        try {
          const liveCheck = await this.checkIfLive(config)

          if (liveCheck.anchorName) {
            // 用 API 返回的主播名更新 config（自动发现）
            config.anchorName = liveCheck.anchorName
          }
          if (liveCheck.webRid) {
            state.webRid = liveCheck.webRid
          }

          if (liveCheck.isLive) {
            if (!state.isLive) {
              console.log(`[Monitor] ${config.anchorName} is now LIVE`)
            }
            state.isLive = true
            state.streamUrl = liveCheck.streamUrl || state.streamUrl

            if (this.recordingEnabled && config.autoRecord !== false && !state.recordingId) {
              // 主播已开播，且录制模式开启、该主播允许自动录制 — 刷新流地址并入队录制
              // 录制前刷新流地址（URL 会过期），按主播配置的画质拉
              let streamUrl = liveCheck.streamUrl
              if (state.webRid) {
                const fresh = await refreshStreamUrlByPlatform(config.platform, state.webRid, config.resolution)
                if (fresh) streamUrl = fresh
              }

              if (!streamUrl) {
                console.warn(`[Monitor] ${config.anchorName}: live but no stream URL`)
              } else {
                const recordingId = `${streamerId}_${Date.now()}`
                const queuedRecording: QueuedRecording = {
                  recordingId,
                  streamerId,
                  streamUrl,
                  anchorName: config.anchorName,
                  platform: config.platform,
                  webRid: state.webRid || undefined,
                  resolution: config.resolution,
                  segmentDuration: config.segmentDuration,
                  queuedAt: Date.now(),
                }

                state.streamUrl = streamUrl
                state.recordingId = recordingId

                this.recordingQueue.enqueue(queuedRecording)
              }
            }
          } else if (!liveCheck.isLive && state.isLive) {
            // 主播下播 — 停止录制
            console.log(`[Monitor] ${config.anchorName} is now OFFLINE, stopping recording...`)
            if (state.recordingId) {
              this.recordingQueue.dequeue(state.recordingId).catch((err) =>
                console.error(`[Monitor] Failed to stop recording ${state.recordingId}:`, (err as Error).message)
              )
            }
            state.isLive = false
            state.streamUrl = null
            state.recordingId = null
          }
        } catch (err) {
          console.error(`[Monitor] Error checking ${config.anchorName}:`, (err as Error).message)
        }

        // 多主播之间增加延迟，防止请求过密
        if (this.isPolling && this.monitoredStreamers.size > 1) {
          await new Promise((r) => setTimeout(r, PER_STREAMER_DELAY))
        }
      }
    } finally {
      this.pollInProgress = false
    }
  }

  /**
   * 检测主播是否正在直播
   */
  private async checkIfLive(
    config: MonitorConfig
  ): Promise<{ isLive: boolean; streamUrl: string | null; anchorName?: string; webRid?: string }> {
    const input = config.roomUrl || config.roomId
    const info = await getLiveInfoByPlatform(config.platform, input, config.resolution)
    if (info.error) {
      console.warn(`[Monitor] ${config.anchorName}: ${info.error}`)
    }
    return {
      isLive: info.isLive,
      streamUrl: info.streamUrl,
      anchorName: info.streamerName || undefined,
      webRid: info.webRid || info.roomId || undefined,
    }
  }
}
