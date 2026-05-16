import { app } from 'electron'
import { existsSync, readFileSync, writeFileSync, statSync, mkdirSync } from 'fs'
import { basename, dirname, join } from 'path'
import { backendApi } from './backend-api'
import { CosUploadService } from './cos-upload-service'

export type CloudBusinessType = 'full_recap' | 'clip_recap' | 'full_comparison' | 'clip_comparison'

export interface CloudUploadQueueItem {
  id: string
  filePath: string
  fileName?: string
  contentType?: string
  businessType: CloudBusinessType
  businessId?: number
  recordingId?: number
  clipId?: number
  comparisonId?: number
  streamerId?: number
  anchorName?: string
  industryId?: number
  accountType?: string
  uploadAccount?: string
  recordedAt?: string
  durationSeconds?: number
  manualUpload?: boolean
  status: 'queued' | 'uploading' | 'retry_scheduled' | 'completed' | 'dead'
  retryCount: number
  nextRunAt: number
  uploadId?: number
  fileId?: number
  lastError?: string
}

class CloudUploadQueue {
  private items: CloudUploadQueueItem[] = []
  private processing = false
  private loaded = false
  private readonly uploader = new CosUploadService()
  private readonly queuePath = join(app.getPath('userData'), 'cloud-upload-queue.json')
  private timer: NodeJS.Timeout | null = null

  start(): void {
    this.load()
    if (!this.timer) {
      this.timer = setInterval(() => this.process().catch((err) => {
        console.error('[CloudUploadQueue] background process failed:', err)
      }), 5000)
    }
    this.process().catch(() => {})
  }

  enqueue(input: Omit<CloudUploadQueueItem, 'id' | 'status' | 'retryCount' | 'nextRunAt'> & { id?: string }): CloudUploadQueueItem {
    this.load()
    const id = input.id || `${input.businessType}_${input.businessId || Date.now()}_${Math.random().toString(36).slice(2, 8)}`
    const existing = this.items.find((item) => item.id === id)
    if (existing) {
      Object.assign(existing, {
        ...input,
        id,
        fileName: input.fileName || basename(input.filePath),
        contentType: input.contentType || resolveContentType(input.filePath),
        status: 'queued',
        retryCount: 0,
        nextRunAt: Date.now(),
        lastError: undefined,
      })
      this.save()
      this.process().catch(() => {})
      return existing
    }
    const item: CloudUploadQueueItem = {
      ...input,
      id,
      fileName: input.fileName || basename(input.filePath),
      contentType: input.contentType || resolveContentType(input.filePath),
      status: 'queued',
      retryCount: 0,
      nextRunAt: Date.now(),
    }
    this.items.push(item)
    this.save()
    this.process().catch(() => {})
    return item
  }

  getItems(): CloudUploadQueueItem[] {
    this.load()
    return [...this.items]
  }

  private async process(): Promise<void> {
    this.load()
    if (this.processing) return
    const now = Date.now()
    const item = this.items.find((candidate) =>
      (candidate.status === 'queued' || candidate.status === 'retry_scheduled') &&
      candidate.nextRunAt <= now
    )
    if (!item) return

    this.processing = true
    try {
      await this.processItem(item)
    } finally {
      this.processing = false
      this.save()
    }
  }

  private async processItem(item: CloudUploadQueueItem): Promise<void> {
    try {
      const stat = statSync(item.filePath)
      item.status = 'uploading'
      this.save()

      const init = await backendApi.initCloudUpload({
        fileName: item.fileName || basename(item.filePath),
        fileSize: stat.size,
        contentType: item.contentType || resolveContentType(item.filePath),
        businessType: item.businessType,
        businessId: item.businessId,
        recordingId: item.recordingId,
        clipId: item.clipId,
        comparisonId: item.comparisonId,
        streamerId: item.streamerId,
        anchorName: item.anchorName,
        industryId: item.industryId,
        accountType: item.accountType,
        uploadAccount: item.uploadAccount,
        recordedAt: item.recordedAt,
        durationSeconds: item.durationSeconds,
        manualUpload: item.manualUpload,
        localFilePath: item.filePath,
        clientTaskId: item.id,
      })
      item.uploadId = init.uploadId
      item.fileId = init.fileId

      await this.uploader.uploadFile(init.uploadUrl, item.filePath, item.contentType || 'application/octet-stream', async (progress) => {
        if (item.uploadId) {
          await backendApi.updateCloudUploadProgress(item.uploadId, progress).catch(() => {})
        }
      })

      if (item.uploadId) {
        await backendApi.completeCloudUpload(item.uploadId, { fileSize: stat.size })
      }
      item.status = 'completed'
      this.items = this.items.filter((candidate) => candidate.id !== item.id)
      this.save()
    } catch (err: any) {
      item.lastError = describeUploadError(err)
      if (item.uploadId) {
        await backendApi.failCloudUpload(item.uploadId, item.lastError).catch(() => {})
      }
      if (isNonRetryable(err) || item.retryCount >= 3) {
        item.status = 'dead'
        this.items = this.items.filter((candidate) => candidate.id !== item.id)
        console.warn('[CloudUploadQueue] upload dropped:', item.lastError)
        return
      }
      item.retryCount += 1
      item.status = 'retry_scheduled'
      item.nextRunAt = Date.now() + retryDelayMs(item.retryCount)
      console.warn('[CloudUploadQueue] upload retry scheduled:', item.id, item.lastError)
    }
  }

  private load(): void {
    if (this.loaded) return
    this.loaded = true
    try {
      if (!existsSync(this.queuePath)) {
        this.items = []
        return
      }
      const raw = readFileSync(this.queuePath, 'utf8')
      const parsed = JSON.parse(raw)
      this.items = Array.isArray(parsed) ? parsed : []
      for (const item of this.items) {
        if (item.status === 'uploading') {
          item.status = 'retry_scheduled'
          item.nextRunAt = Date.now()
        }
        if (item.status === 'retry_scheduled' && item.retryCount >= 3) {
          item.status = 'queued'
          item.retryCount = 0
          item.nextRunAt = Date.now()
          item.lastError = undefined
        }
      }
      this.save()
    } catch (err) {
      console.warn('[CloudUploadQueue] failed to load queue:', err)
      this.items = []
    }
  }

  private save(): void {
    try {
      mkdirSync(dirname(this.queuePath), { recursive: true })
      const pending = this.items.filter((item) => item.status !== 'completed' && item.status !== 'dead')
      writeFileSync(this.queuePath, JSON.stringify(pending, null, 2), 'utf8')
    } catch (err) {
      console.warn('[CloudUploadQueue] failed to save queue:', err)
    }
  }
}

function retryDelayMs(retryCount: number): number {
  if (retryCount <= 1) return 60_000
  if (retryCount === 2) return 5 * 60_000
  return 30 * 60_000
}

function isNonRetryable(err: any): boolean {
  const message = `${err?.response?.data?.message || err?.message || ''}`
  const code = err?.response?.data?.code
  return code === 40002 ||
    code === 40900 ||
    message.includes('容量不足') ||
    message.includes('未开启云空间同步') ||
    message.includes('已在云空间') ||
    message.includes('已在上传队列') ||
    message.includes('无需重复') ||
    message.includes('无权') ||
    message.includes('未配置')
}

function describeUploadError(err: any): string {
  const status = err?.response?.status
  const body = err?.response?.data
  const serverMessage = body?.message || body?.error || body?.msg
  if (status && serverMessage) return `HTTP ${status}: ${serverMessage}`
  if (status && typeof body === 'string' && body.trim()) return `HTTP ${status}: ${body.slice(0, 300)}`
  if (status) return `HTTP ${status}: ${err?.message || '请求失败'}`
  return err?.message || String(err)
}

function resolveContentType(filePath: string): string {
  const lower = filePath.toLowerCase()
  if (lower.endsWith('.mp4')) return 'video/mp4'
  if (lower.endsWith('.flv')) return 'video/x-flv'
  if (lower.endsWith('.m4a')) return 'audio/mp4'
  if (lower.endsWith('.mp3')) return 'audio/mpeg'
  return 'application/octet-stream'
}

const queue = new CloudUploadQueue()

export function getCloudUploadQueue(): CloudUploadQueue {
  return queue
}
