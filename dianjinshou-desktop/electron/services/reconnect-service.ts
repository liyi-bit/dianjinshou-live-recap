import { RecordingQueue, QueuedRecording } from './recording-queue'

interface RetryState {
  count: number
  lastAttemptAt: number
}

export interface RetryStatus {
  attempts: number
  maxAttempts: number
  nextRetryMs: number
}

export class ReconnectService {
  private maxAttempts = 5
  private retryStates: Map<string, RetryState> = new Map()
  private pendingTimers: Map<string, ReturnType<typeof setTimeout>> = new Map()
  private recordingQueue: RecordingQueue

  constructor(recordingQueue: RecordingQueue) {
    this.recordingQueue = recordingQueue
  }

  /**
   * Handle an unexpected disconnect for a recording.
   * Implements exponential backoff: 1s, 2s, 4s, 8s, 16s
   */
  handleDisconnect(recordingId: string, config: QueuedRecording): void {
    const state = this.retryStates.get(recordingId) || { count: 0, lastAttemptAt: 0 }

    if (state.count >= this.maxAttempts) {
      console.error(
        `[Reconnect] Recording ${recordingId} (${config.anchorName}) failed after ${this.maxAttempts} attempts. Giving up.`
      )
      this.retryStates.delete(recordingId)
      this.recordingQueue.onRecordingFinished(recordingId)
      return
    }

    const delay = 1000 * Math.pow(2, state.count)
    state.count++
    state.lastAttemptAt = Date.now()
    this.retryStates.set(recordingId, state)

    console.log(
      `[Reconnect] Scheduling retry ${state.count}/${this.maxAttempts} for ${config.anchorName} (${recordingId}) in ${delay}ms`
    )

    // Clear any existing timer for this recording
    const existingTimer = this.pendingTimers.get(recordingId)
    if (existingTimer) {
      clearTimeout(existingTimer)
    }

    const timer = setTimeout(() => {
      this.pendingTimers.delete(recordingId)
      console.log(
        `[Reconnect] Retrying recording ${config.anchorName} (${recordingId}), attempt ${state.count}/${this.maxAttempts}`
      )

      // Create a new queued recording with a fresh recordingId
      const retryConfig: QueuedRecording = {
        ...config,
        recordingId: `${config.streamerId}_${Date.now()}`,
        queuedAt: Date.now(),
      }

      // Transfer retry state to the new recordingId
      this.retryStates.set(retryConfig.recordingId, state)
      this.retryStates.delete(recordingId)

      this.recordingQueue.enqueue(retryConfig)
    }, delay)

    this.pendingTimers.set(recordingId, timer)
  }

  /**
   * Reset retry counter for a recording (called on successful start)
   */
  resetAttempts(recordingId: string): void {
    this.retryStates.delete(recordingId)
    const timer = this.pendingTimers.get(recordingId)
    if (timer) {
      clearTimeout(timer)
      this.pendingTimers.delete(recordingId)
    }
  }

  /**
   * Get the retry status for a recording
   */
  getRetryStatus(recordingId: string): RetryStatus {
    const state = this.retryStates.get(recordingId)
    if (!state) {
      return { attempts: 0, maxAttempts: this.maxAttempts, nextRetryMs: 0 }
    }

    const nextDelay = state.count < this.maxAttempts ? 1000 * Math.pow(2, state.count) : 0
    return {
      attempts: state.count,
      maxAttempts: this.maxAttempts,
      nextRetryMs: nextDelay,
    }
  }

  /**
   * Cancel all pending retries (called on shutdown)
   */
  cancelAll(): void {
    for (const [recordingId, timer] of this.pendingTimers) {
      clearTimeout(timer)
      console.log(`[Reconnect] Cancelled pending retry for ${recordingId}`)
    }
    this.pendingTimers.clear()
    this.retryStates.clear()
  }
}
