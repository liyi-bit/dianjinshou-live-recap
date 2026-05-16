import { RecordingService } from './recording-service'
import { refreshStreamUrlByPlatform } from './live-platform-service'

export interface QueuedRecording {
  recordingId: string
  streamerId: string
  streamUrl: string
  anchorName: string
  platform?: string
  webRid?: string
  resolution: '480p' | '720p' | '1080p' | 'source'
  segmentDuration: number
  queuedAt: number
}

export interface QueueStatus {
  active: number
  queued: number
  max: number
}

export class RecordingQueue {
  private maxConcurrent: number
  private activeRecordings: Map<string, QueuedRecording> = new Map()
  private queue: QueuedRecording[] = []
  private recordingService: RecordingService

  constructor(recordingService: RecordingService, maxConcurrent = 30) {
    this.recordingService = recordingService
    this.maxConcurrent = maxConcurrent
  }

  /**
   * Add a recording to the queue. Starts immediately if under the concurrent limit.
   */
  enqueue(config: QueuedRecording): void {
    if (this.activeRecordings.has(config.recordingId)) {
      console.log(`[Queue] Recording ${config.recordingId} is already active, skipping`)
      return
    }

    // Check if already queued
    const alreadyQueued = this.queue.some((q) => q.recordingId === config.recordingId)
    if (alreadyQueued) {
      console.log(`[Queue] Recording ${config.recordingId} is already queued, skipping`)
      return
    }

    if (this.activeRecordings.size < this.maxConcurrent) {
      // Start immediately
      this.startRecording(config)
    } else {
      // Add to wait queue, sorted by queuedAt (FIFO)
      this.queue.push(config)
      this.queue.sort((a, b) => a.queuedAt - b.queuedAt)
      console.log(
        `[Queue] Queued ${config.anchorName} (${config.recordingId}), position: ${this.queue.length}`
      )
    }
  }

  /**
   * Remove a recording from the queue or stop it if active
   */
  async dequeue(recordingId: string): Promise<void> {
    // Check if it's in the waiting queue
    const queueIndex = this.queue.findIndex((q) => q.recordingId === recordingId)
    if (queueIndex !== -1) {
      const removed = this.queue.splice(queueIndex, 1)[0]
      console.log(`[Queue] Removed ${removed.anchorName} (${recordingId}) from wait queue`)
      return
    }

    // Check if it's active
    if (this.activeRecordings.has(recordingId)) {
      await this.recordingService.stopRecording(recordingId)
      this.activeRecordings.delete(recordingId)
      console.log(`[Queue] Stopped active recording ${recordingId}`)
      this.processQueue()
    }
  }

  /**
   * Called when a recording finishes (either normally or with error).
   * Removes from active set and starts the next queued recording.
   */
  onRecordingFinished(recordingId: string): void {
    if (this.activeRecordings.has(recordingId)) {
      const config = this.activeRecordings.get(recordingId)
      this.activeRecordings.delete(recordingId)
      console.log(
        `[Queue] Recording finished: ${config?.anchorName} (${recordingId}). Active: ${this.activeRecordings.size}/${this.maxConcurrent}`
      )
      this.processQueue()
    }
  }

  /**
   * Process the wait queue: start recordings to fill available slots
   */
  processQueue(): void {
    while (this.activeRecordings.size < this.maxConcurrent && this.queue.length > 0) {
      const next = this.queue.shift()!
      this.startRecording(next)
    }
  }

  /**
   * Get current queue status
   */
  getQueueStatus(): QueueStatus {
    return {
      active: this.activeRecordings.size,
      queued: this.queue.length,
      max: this.maxConcurrent,
    }
  }

  /**
   * Get list of active recording configs
   */
  getActiveConfigs(): QueuedRecording[] {
    return Array.from(this.activeRecordings.values())
  }

  /**
   * Get list of queued recording configs
   */
  getQueuedConfigs(): QueuedRecording[] {
    return [...this.queue]
  }

  /**
   * Start a recording and track it as active
   */
  private async startRecording(config: QueuedRecording): Promise<void> {
    try {
      // 录制前刷新流地址（平台 CDN URL 会过期），按用户选择的画质拉
      if (config.webRid && config.platform) {
        const freshUrl = await refreshStreamUrlByPlatform(config.platform, config.webRid, config.resolution)
        if (freshUrl) {
          config.streamUrl = freshUrl
        } else {
          console.warn(`[Queue] ${config.anchorName}: failed to refresh stream URL, using cached`)
        }
      }

      console.log(
        `[Queue] Starting recording: ${config.anchorName} (${config.recordingId}). Active: ${this.activeRecordings.size + 1}/${this.maxConcurrent}`
      )
      this.activeRecordings.set(config.recordingId, config)

      await this.recordingService.startRecording(
        config.streamerId,
        config.streamUrl,
        config.anchorName,
        {
          platform: config.platform as 'douyin' | 'kuaishou' | undefined,
          resolution: config.resolution,
          segmentDuration: config.segmentDuration,
          // 关键：把 Queue 预分配的 recordingId 传下去，否则 RecordingService 会重新生成一个
          // 新 id，导致 FFmpeg 完成回调带来的 id 跟 Queue 的 activeRecordings key 对不上，
          // onRecordingFinished 静默失败，active 永不递减
          recordingId: config.recordingId,
        }
      )
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      console.error(`[Queue] Failed to start recording ${config.recordingId}:`, message)
      this.activeRecordings.delete(config.recordingId)
      // Don't re-queue on start failure; reconnect service will handle retries
    }
  }
}
