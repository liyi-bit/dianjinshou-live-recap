import request from './request'

export interface ShortClipItem {
  id: number
  recordingId: number
  sourceType: string
  clipName: string
  startTime: number
  endTime: number
  duration: number
  resolution: string
  watermarkText: string | null
  fileSize: number
  status: string
  storageKey: string | null
  createdAt: string
}

export interface CreateShortClipParams {
  recordingId: number
  startTime: number
  endTime: number
  clipName: string
  resolution?: string
  watermarkText?: string
}

export function createShortClip(params: CreateShortClipParams) {
  return request.post<ShortClipItem>('/short-clips', params)
}

export function listShortClips(page: number, size: number, recordingId?: number, status?: string) {
  return request.get<any>('/short-clips', { params: { page, size, recordingId, status } })
}

export function getShortClip(id: number) {
  return request.get<ShortClipItem>(`/short-clips/${id}`)
}

export function deleteShortClip(id: number) {
  return request.delete(`/short-clips/${id}`)
}

export function batchExportClips(clipIds: number[]) {
  return request.post<{ exportKey: string }>('/short-clips/batch-export', clipIds)
}

export function uploadClipToCloud(id: number) {
  return request.post(`/short-clips/${id}/upload-cloud`)
}
