import request from './request'

export type CloudView = 'full' | 'clip' | 'comparison'
export type CloudCompareMode = 'full' | 'clip'

export interface CloudFileItem {
  id: number
  fileName: string
  displayName?: string | null
  fileSize: number
  contentType: string
  fileType: string
  businessType: string
  businessId?: number | null
  recordingId?: number | null
  clipId?: number | null
  comparisonId?: number | null
  streamerId?: number | null
  anchorName?: string | null
  anchorAvatar?: string | null
  anchorNameOptimize?: string | null
  anchorNameReference?: string | null
  anchorAvatarOptimize?: string | null
  anchorAvatarReference?: string | null
  industryId?: number | null
  accountType?: string | null
  uploadAccount?: string | null
  recordedAt?: string | null
  durationSeconds?: number | null
  localExists?: boolean
  readonlyRestored?: boolean
  uploadProgress?: number
  status: 'queued' | 'uploading' | 'active' | string
  createdAt: string
}

export interface CloudUsage {
  usedBytes: number
  totalQuotaBytes: number
  remainingBytes: number
  fileCount: number
  usagePercent: number
}

export interface CloudListParams {
  page: number
  size: number
  keyword?: string
  industryId?: number | null
  anchorName?: string
  uploadAccount?: string
  accountType?: string
  startTime?: string
  endTime?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

export interface CloudReadonlyDetail {
  file: CloudFileItem
  signedUrl?: { url: string; method: string; expiresAt: string } | null
  recapDetail?: any
  comparisonDetail?: any
  readonly: boolean
  allowDownload: boolean
  allowDownloadToLocal: boolean
}

export interface CloudComparisonSource {
  role: 'optimize' | 'reference' | string
  recordingId?: number | null
  taskId?: number | null
  streamerId?: number | null
  anchorName?: string | null
  industryId?: number | null
  accountType?: string | null
  recordedAt?: string | null
  durationSeconds?: number | null
  fileName?: string | null
  localFilePath?: string | null
  businessType: 'full_recap' | 'clip_recap'
  businessId?: number | null
  clipId?: number | null
  cloudFileId?: number | null
  cloudStatus?: string | null
  uploaded?: boolean
  uploading?: boolean
}

export interface CloudComparisonSourceStatus {
  comparisonId: number
  mode: CloudCompareMode
  optimize?: CloudComparisonSource | null
  reference?: CloudComparisonSource | null
}

export interface CloudUploadStatus {
  exists: boolean
  fileId?: number | null
  businessType?: string | null
  status?: string | null
  message?: string | null
}

export function listFullRecaps(params: CloudListParams) {
  return request.get<PageResult<CloudFileItem>>('/cloud-space/full-recaps', { params })
}

export function listRecordings(page: number, size: number, keyword?: string) {
  return listFullRecaps({ page, size, keyword })
}

export function listClipRecaps(params: CloudListParams) {
  return request.get<PageResult<CloudFileItem>>('/cloud-space/clip-recaps', { params })
}

export function listClips(page: number, size: number, keyword?: string) {
  return listClipRecaps({ page, size, keyword })
}

export function listComparisons(mode: CloudCompareMode, params: Omit<CloudListParams, 'industryId' | 'anchorName' | 'accountType'>) {
  return request.get<PageResult<CloudFileItem>>('/cloud-space/comparisons', { params: { ...params, mode } })
}

export function listDocuments(page: number, size: number, keyword?: string) {
  return listComparisons('full', { page, size, keyword })
}

export function listComparisonCandidates(mode: CloudCompareMode, params: CloudListParams) {
  return request.get<PageResult<CloudFileItem>>('/cloud-space/comparison-candidates', { params: { ...params, mode } })
}

export function getCloudUsage() {
  return request.get<CloudUsage>('/cloud-space/usage')
}

export function renameCloudFile(id: number, displayName: string) {
  return request.patch<CloudFileItem>(`/cloud-space/files/${id}/display-name`, { displayName })
}

export function deleteCloudFile(id: number) {
  return request.delete(`/cloud-space/files/${id}`)
}

export async function batchDeleteFiles(ids: number[]) {
  await Promise.all(ids.map((id) => deleteCloudFile(id)))
}

export function getSignedUrl(id: number) {
  return request.post<{ url: string; method: string; expiresAt: string }>(`/cloud-space/files/${id}/signed-url`)
}

export async function batchDownloadFiles(ids: number[]) {
  const signedUrls = await Promise.all(ids.map((id) => getSignedUrl(id) as any))
  return signedUrls.map((item: any) => (item?.data ?? item)?.url).filter(Boolean)
}

export function getCloudReadonlyDetail(id: number) {
  return request.get<CloudReadonlyDetail>(`/cloud-space/files/${id}/readonly-detail`)
}

export function requestDownloadToLocal(id: number) {
  return request.post<{ url: string; method: string; expiresAt: string }>(`/cloud-space/files/${id}/download-to-local-request`)
}

export function markDownloadToLocalComplete(id: number, localFilePath: string) {
  return request.post<CloudFileItem>(`/cloud-space/files/${id}/download-to-local-complete`, { localFilePath })
}

export function getOpenTarget(id: number) {
  return request.get<{ target: string; routeName: string; readonly: boolean; params: Record<string, any> }>(`/cloud-space/files/${id}/open-target`)
}

export function createCloudComparison(mode: CloudCompareMode, fileIds: number[]) {
  return request.post('/cloud-space/comparisons', { mode, fileIds })
}

export function getComparisonSourceStatus(comparisonId: number) {
  return request.get<CloudComparisonSourceStatus>(`/cloud-space/comparisons/${comparisonId}/source-status`)
}

export function getUploadStatus(params: {
  businessType: 'full_recap' | 'clip_recap' | 'full_comparison' | 'clip_comparison'
  businessId?: number | null
  recordingId?: number | null
  clipId?: number | null
  comparisonId?: number | null
}) {
  return request.get<CloudUploadStatus>('/cloud-space/upload-status', { params })
}
