import * as cloudSpaceApi from '@/api/cloudSpace'

type BusinessType = 'full_recap' | 'clip_recap' | 'full_comparison' | 'clip_comparison'

interface CheckParams {
  electronApi?: any
  businessType: BusinessType
  businessId?: number | null
  recordingId?: number | null
  clipId?: number | null
  comparisonId?: number | null
}

interface DuplicateResult {
  duplicate: boolean
  message?: string
  status?: string | null
}

function unpack<T>(response: T | { data?: T }): T {
  return ((response as any)?.data ?? response) as T
}

function subjectOf(type: BusinessType) {
  if (type === 'clip_recap') return '该切片复盘'
  if (type === 'full_comparison' || type === 'clip_comparison') return '该对比复盘'
  return '该全场复盘'
}

function localQueueMessage(type: BusinessType) {
  return `${subjectOf(type)}已在上传队列，无需重复添加`
}

function matchesQueueItem(item: any, params: CheckParams) {
  if (!item || item.businessType !== params.businessType) return false
  if (item.status === 'completed' || item.status === 'dead') return false
  if (params.comparisonId != null && item.comparisonId === params.comparisonId) return true
  if (params.clipId != null && item.clipId === params.clipId) return true
  if (params.recordingId != null && item.recordingId === params.recordingId) return true
  if (params.businessId != null && item.businessId === params.businessId) return true
  return false
}

export async function checkCloudUploadDuplicate(params: CheckParams): Promise<DuplicateResult> {
  if (params.electronApi?.getCloudUploadQueue) {
    const queueResult = await params.electronApi.getCloudUploadQueue()
    const queueItems = queueResult?.data ?? []
    if (Array.isArray(queueItems) && queueItems.some((item) => matchesQueueItem(item, params))) {
      return { duplicate: true, message: localQueueMessage(params.businessType), status: 'queued' }
    }
  }

  const remote = unpack(await cloudSpaceApi.getUploadStatus({
    businessType: params.businessType,
    businessId: params.businessId,
    recordingId: params.recordingId,
    clipId: params.clipId,
    comparisonId: params.comparisonId,
  }))
  if (remote?.exists) {
    return {
      duplicate: true,
      message: remote.message || localQueueMessage(params.businessType),
      status: remote.status,
    }
  }
  return { duplicate: false }
}
