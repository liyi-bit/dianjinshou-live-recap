import request from './request'

// --- Types ---

export interface ComparisonDraft {
  id: number
  firstRecordingId: number
  listContext: string
  expiresAt: string
  createdAt: string
}

export interface ComparisonItem {
  id: number
  type: 'full' | 'clip'
  recordingIdOptimize: number
  recordingIdReference: number
  taskIdOptimize: number | null
  taskIdReference: number | null
  clipCategory: string | null
  aiComparisonResult: string | null
  aiModel: string
  status: string
  createdAt: string
  anchorNameOptimize?: string | null
  anchorNameReference?: string | null
  anchorAvatarOptimize?: string | null
  anchorAvatarReference?: string | null
  /** 整场对比时的录制文件名（recording 被清理后从 cloud_files 快照取） */
  localFileNameOptimize?: string | null
  localFileNameReference?: string | null
  clipFilenameOptimize?: string | null
  clipRemarkOptimize?: string | null
  clipFilenameReference?: string | null
  clipRemarkReference?: string | null
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
}

// --- Draft API ---

export function createDraft(data: { firstRecordingId: number; firstTaskId?: number; listContext: string }) {
  return request.post<ComparisonDraft>('/comparison-drafts', data)
}

export function getCurrentDraft() {
  return request.get<ComparisonDraft>('/comparison-drafts/current')
}

export function selectSecond(draftId: number, data: { secondRecordingId: number; secondTaskId?: number; listContext: string }) {
  return request.post<ComparisonItem>(`/comparison-drafts/${draftId}/select-second`, data)
}

export function cancelDraft() {
  return request.delete('/comparison-drafts/current')
}

// --- Comparison CRUD API ---

export function createComparison(data: {
  recordingIdOptimize: number
  recordingIdReference: number
  type: string
  clipCategory?: string
  aiModel?: string
}) {
  return request.post<ComparisonItem>('/comparisons', data)
}

export function getComparisons(params?: { type?: string; status?: string; startDate?: string; endDate?: string; page?: number; size?: number }) {
  return request.get<PageResult<ComparisonItem>>('/comparisons', { params })
}

export function getComparison(id: number) {
  return request.get<ComparisonItem>(`/comparisons/${id}`)
}

export function swapComparison(id: number) {
  return request.post<ComparisonItem>(`/comparisons/${id}/swap`)
}

export function batchDeleteComparisons(ids: number[]) {
  return request.delete('/comparisons', { data: ids })
}

export function getComparisonKeywords(id: number) {
  return request.get(`/comparisons/${id}/keywords`)
}
