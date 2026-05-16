import * as cloudSpaceApi from '@/api/cloudSpace'
import type { CloudComparisonSource, CloudComparisonSourceStatus } from '@/api/cloudSpace'

interface EnsureResult {
  success: boolean
  queued: number
  skipped: number
  error?: string
  status?: CloudComparisonSourceStatus
}

function unpack<T>(response: T | { data?: T }): T {
  return ((response as any)?.data ?? response) as T
}

function hasCloudFile(source: CloudComparisonSource) {
  return Boolean(source.uploaded || source.uploading || source.cloudFileId)
}

function fileNameOf(path?: string | null) {
  if (!path) return ''
  const slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'))
  return slash >= 0 ? path.slice(slash + 1) : path
}

function sourceLabel(source: CloudComparisonSource) {
  return source.fileName || source.anchorName || `录制 #${source.recordingId || source.taskId || '-'}`
}

function queueIdOf(source: CloudComparisonSource) {
  const id = source.businessId || source.clipId || source.recordingId || Date.now()
  return `${source.businessType}_${id}`
}

export async function ensureComparisonSourceVideosUploaded(
  comparisonId: number,
  electronApi: any
): Promise<EnsureResult> {
  const status = unpack(await cloudSpaceApi.getComparisonSourceStatus(comparisonId))
  const sources = [status.optimize, status.reference].filter(Boolean) as CloudComparisonSource[]
  if (sources.length !== 2) {
    return { success: false, queued: 0, skipped: 0, error: '对比复盘缺少原视频信息，无法上传云空间' }
  }

  const needUpload = sources.filter((source) => !hasCloudFile(source))
  const skipped = sources.length - needUpload.length
  if (needUpload.length === 0) {
    return { success: true, queued: 0, skipped, status }
  }

  if (!electronApi?.enqueueCloudUpload) {
    return { success: false, queued: 0, skipped, error: '当前环境不支持云空间视频上传' }
  }

  const missingLocal = needUpload.filter((source) => !source.localFilePath)
  if (missingLocal.length > 0) {
    return {
      success: false,
      queued: 0,
      skipped,
      error: `原视频本地文件缺失，无法上传云空间：${missingLocal.map(sourceLabel).join('、')}`,
    }
  }

  let queued = 0
  for (const source of needUpload) {
    const result = await electronApi.enqueueCloudUpload({
      id: queueIdOf(source),
      filePath: source.localFilePath,
      fileName: source.fileName || fileNameOf(source.localFilePath),
      businessType: source.businessType,
      businessId: source.businessId ?? undefined,
      recordingId: source.recordingId ?? undefined,
      clipId: source.clipId ?? undefined,
      streamerId: source.streamerId ?? undefined,
      anchorName: source.anchorName ?? undefined,
      industryId: source.industryId ?? undefined,
      accountType: source.accountType ?? undefined,
      recordedAt: source.recordedAt ?? undefined,
      durationSeconds: source.durationSeconds ?? undefined,
      manualUpload: true,
    })
    if (!result?.success) {
      return {
        success: false,
        queued,
        skipped,
        error: result?.error || `${sourceLabel(source)} 加入云空间上传队列失败`,
      }
    }
    queued += 1
  }

  return { success: true, queued, skipped, status }
}
