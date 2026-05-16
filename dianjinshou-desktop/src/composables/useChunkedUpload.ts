import { ref, computed } from 'vue'
import { initUpload, uploadPart, completeUpload, cancelUpload } from '@/api/upload'
import type { InitUploadParams, UploadInitResult } from '@/api/upload'

export type UploadStatus = 'idle' | 'initializing' | 'uploading' | 'completing' | 'completed' | 'failed' | 'cancelled'

const CONCURRENT_PARTS = 3

export function useChunkedUpload() {
  const status = ref<UploadStatus>('idle')
  const uploadId = ref<number | null>(null)
  const storageKey = ref<string>('')
  const totalParts = ref(0)
  const uploadedParts = ref(0)
  const partSize = ref(0)
  const error = ref<string | null>(null)

  const progress = computed(() => {
    if (totalParts.value === 0) return 0
    return Math.round((uploadedParts.value / totalParts.value) * 100)
  })

  async function startUpload(file: File, bucket?: string): Promise<string | null> {
    try {
      status.value = 'initializing'
      error.value = null
      uploadedParts.value = 0

      const params: InitUploadParams = {
        fileName: file.name,
        fileSize: file.size,
        contentType: file.type || 'application/octet-stream',
        bucket
      }

      const res = await initUpload(params)
      const data: UploadInitResult = (res as any).data ?? res

      uploadId.value = data.uploadId
      storageKey.value = data.storageKey
      totalParts.value = data.totalParts
      partSize.value = data.partSize

      status.value = 'uploading'

      // Upload parts with concurrency limit + per-part retry
      const partNumbers = Array.from({ length: totalParts.value }, (_, i) => i + 1)
      let index = 0
      const PART_RETRY = 3

      async function uploadOnePart(partNum: number): Promise<void> {
        const start = (partNum - 1) * partSize.value
        const end = Math.min(start + partSize.value, file.size)
        const blob = file.slice(start, end)
        let lastErr: any = null
        for (let attempt = 1; attempt <= PART_RETRY; attempt++) {
          if (status.value === 'cancelled') return
          try {
            await uploadPart(uploadId.value!, partNum, blob)
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
          if (status.value === 'cancelled') return
          const partNum = partNumbers[index++]
          await uploadOnePart(partNum)
          uploadedParts.value++
        }
      }

      const workers = Array.from(
        { length: Math.min(CONCURRENT_PARTS, totalParts.value) },
        () => uploadNext()
      )
      await Promise.all(workers)

      if (status.value === 'cancelled') return null

      status.value = 'completing'
      const completeRes = await completeUpload(uploadId.value!)
      const completeData = (completeRes as any).data ?? completeRes

      status.value = 'completed'
      storageKey.value = completeData.storageKey || storageKey.value
      return storageKey.value
    } catch (e: any) {
      status.value = 'failed'
      error.value = e?.message || 'Upload failed'
      // 通知服务端取消任务，避免幽灵 UploadTask
      if (uploadId.value) {
        try { await cancelUpload(uploadId.value) } catch { /* best effort */ }
      }
      return null
    }
  }

  async function cancel() {
    if (uploadId.value && status.value === 'uploading') {
      status.value = 'cancelled'
      try {
        await cancelUpload(uploadId.value)
      } catch {
        // best effort
      }
    }
  }

  function reset() {
    status.value = 'idle'
    uploadId.value = null
    storageKey.value = ''
    totalParts.value = 0
    uploadedParts.value = 0
    partSize.value = 0
    error.value = null
  }

  return {
    status,
    uploadId,
    storageKey,
    totalParts,
    uploadedParts,
    partSize,
    progress,
    error,
    startUpload,
    cancel,
    reset
  }
}
