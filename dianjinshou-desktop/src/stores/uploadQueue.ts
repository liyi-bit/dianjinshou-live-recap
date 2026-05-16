import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { initUpload, uploadPart, completeUpload, cancelUpload } from '@/api/upload'
import type { InitUploadParams, UploadInitResult } from '@/api/upload'

export type QueueItemStatus = 'queued' | 'uploading' | 'completed' | 'failed' | 'cancelled'

export interface QueueItem {
  id: string
  file: File
  bucket?: string
  status: QueueItemStatus
  progress: number
  storageKey: string
  uploadId: number | null
  totalParts: number
  uploadedParts: number
  error: string | null
  /** cancel 接口报错的次要信息（不覆盖主 error） */
  cancelWarn?: string | null
}

const MAX_CONCURRENT_FILES = 2
const PART_SIZE = 5 * 1024 * 1024 // 5MB
const CONCURRENT_PARTS = 3
const PART_RETRY = 3

let nextId = 1

export const useUploadQueueStore = defineStore('uploadQueue', () => {
  const items = ref<QueueItem[]>([])

  const activeCount = computed(() =>
    items.value.filter(i => i.status === 'uploading').length
  )

  const queuedCount = computed(() =>
    items.value.filter(i => i.status === 'queued').length
  )

  function addFile(file: File, bucket?: string): string {
    const id = `upload-${nextId++}`
    items.value.push({
      id,
      file,
      bucket,
      status: 'queued',
      progress: 0,
      storageKey: '',
      uploadId: null,
      totalParts: 0,
      uploadedParts: 0,
      error: null,
      cancelWarn: null
    })
    processQueue()
    return id
  }

  function removeItem(id: string) {
    const idx = items.value.findIndex(i => i.id === id)
    if (idx < 0) return
    const item = items.value[idx]
    // 先把状态切到 cancelled，让 uploadFile 闭包中的 status 检查能短路退出
    if (item.status === 'uploading' || item.status === 'queued') {
      item.status = 'cancelled'
    }
    if (item.uploadId) {
      cancelUpload(item.uploadId).catch(() => { /* best effort */ })
    }
    items.value.splice(idx, 1)
  }

  function clearCompleted() {
    items.value = items.value.filter(i => i.status !== 'completed' && i.status !== 'failed' && i.status !== 'cancelled')
  }

  function processQueue() {
    // 同步循环里把每个待启动 item 直接置为 'uploading' 占位，
    // 再异步启动 uploadFile，避免同帧内 activeCount 滞后导致重复启动。
    while (activeCount.value < MAX_CONCURRENT_FILES) {
      const next = items.value.find(i => i.status === 'queued')
      if (!next) break
      next.status = 'uploading'
      next.error = null
      void runUpload(next)
    }
  }

  async function runUpload(item: QueueItem) {
    try {
      const params: InitUploadParams = {
        fileName: item.file.name,
        fileSize: item.file.size,
        contentType: item.file.type || 'application/octet-stream',
        bucket: item.bucket
      }

      // 启动前最后一次取消检查
      if (item.status === 'cancelled') return

      const res = await initUpload(params)
      const data: UploadInitResult = (res as any).data ?? res

      // init 期间用户可能已 cancel
      if (item.status === 'cancelled') {
        if (data?.uploadId) {
          cancelUpload(data.uploadId).catch(() => {})
        }
        return
      }

      item.uploadId = data.uploadId
      item.storageKey = data.storageKey
      item.totalParts = data.totalParts
      item.uploadedParts = 0

      const partNumbers = Array.from({ length: item.totalParts }, (_, i) => i + 1)
      let index = 0

      async function uploadOnePart(partNum: number): Promise<void> {
        const start = (partNum - 1) * PART_SIZE
        const end = Math.min(start + PART_SIZE, item.file.size)
        const blob = item.file.slice(start, end)
        let lastErr: any = null
        for (let attempt = 1; attempt <= PART_RETRY; attempt++) {
          if (item.status === 'cancelled') return
          try {
            await uploadPart(item.uploadId!, partNum, blob)
            return
          } catch (e) {
            lastErr = e
            // 指数退避：1s / 2s / 4s
            const wait = Math.pow(2, attempt - 1) * 1000
            await new Promise(r => setTimeout(r, wait))
          }
        }
        throw lastErr
      }

      async function uploadNext(): Promise<void> {
        while (index < partNumbers.length) {
          if (item.status === 'cancelled') return
          const partNum = partNumbers[index++]
          await uploadOnePart(partNum)
          if (item.status === 'cancelled') return
          item.uploadedParts++
          item.progress = Math.round((item.uploadedParts / item.totalParts) * 100)
        }
      }

      const workers = Array.from(
        { length: Math.min(CONCURRENT_PARTS, item.totalParts) },
        () => uploadNext()
      )
      await Promise.all(workers)

      if (item.status === 'cancelled') return

      const completeRes = await completeUpload(item.uploadId!)
      const completeData = (completeRes as any).data ?? completeRes
      item.storageKey = completeData.storageKey || item.storageKey
      item.status = 'completed'
      item.progress = 100
    } catch (e: any) {
      // cancelled 状态下 throw 不应转 failed
      if (item.status !== 'cancelled') {
        item.status = 'failed'
        item.error = e?.message || 'Upload failed'
      }
      // 通知服务端取消任务，避免幽灵 UploadTask（cancel 失败仅记录到 cancelWarn）
      if (item.uploadId) {
        try {
          await cancelUpload(item.uploadId)
        } catch (ce: any) {
          item.cancelWarn = ce?.message || '取消请求失败'
        }
      }
    } finally {
      processQueue()
    }
  }

  async function cancelItem(id: string) {
    const item = items.value.find(i => i.id === id)
    if (!item) return
    if (item.status !== 'uploading' && item.status !== 'queued') return
    item.status = 'cancelled'
    if (item.uploadId) {
      try {
        await cancelUpload(item.uploadId)
      } catch (e: any) {
        item.cancelWarn = '取消请求失败：' + (e?.message || '网络错误')
      }
    }
    processQueue()
  }

  return {
    items,
    activeCount,
    queuedCount,
    addFile,
    removeItem,
    clearCompleted,
    cancelItem
  }
})
