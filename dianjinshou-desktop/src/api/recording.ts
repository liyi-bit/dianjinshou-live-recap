import request from './request'

// --- Types ---

export interface Recording {
  id: number
  streamerId: number
  localFileName: string
  localFilePath: string
  startTime: string
  endTime: string | null
  duration: number | null
  fileSize: number | null
  resolution: string | null
  status: string
  analysisStatus: string
  sensitiveWordCount: number | null
  operationKeywordCount: number | null
  latestTaskId: number | null
  coreData: string | null
  anchorName: string | null
  anchorAvatar: string | null
  createdAt: string
}

export interface RecordingDetail extends Recording {
  streamerInfo: {
    id: number
    anchorName: string
    accountType: string | null
    anchorAvatar: string | null
  } | null
  /** 顶级 latestTaskId，与 list 接口保持一致；可能为 null（录制未分析） */
  latestTaskId?: number | null
}

export interface RecordingQuery {
  page?: number
  size?: number
  streamerId?: number
  status?: string
  analysisStatus?: string
  startDate?: string
  endDate?: string
  tab?: string
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
}

// --- API functions ---

export function getRecordings(params: RecordingQuery) {
  return request.get<PageResult<Recording>>('/recordings', { params })
}

export function getRecording(id: number) {
  return request.get<RecordingDetail>(`/recordings/${id}`)
}

export function renameRecording(id: number, name: string) {
  return request.put(`/recordings/${id}/name`, { name })
}

export function exportRecording(id: number) {
  return request.post(`/recordings/${id}/export`)
}

export function batchDeleteRecordings(ids: number[]) {
  return request.delete('/recordings', { data: { ids } })
}
