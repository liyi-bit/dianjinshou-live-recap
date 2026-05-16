import request from './request'

export interface InitUploadParams {
  fileName: string
  fileSize: number
  contentType: string
  bucket?: string
}

export interface UploadInitResult {
  uploadId: number
  storageKey: string
  totalParts: number
  partSize: number
  partUploadUrls: string[]
}

export function initUpload(params: InitUploadParams) {
  return request.post<UploadInitResult>('/upload/init', params)
}

export function uploadPart(uploadId: number, partNumber: number, file: Blob) {
  const formData = new FormData()
  formData.append('file', file)
  return request.put(`/upload/${uploadId}/part/${partNumber}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 300000 // 5 min for large parts
  })
}

export function completeUpload(uploadId: number) {
  return request.post<{ storageKey: string }>(`/upload/${uploadId}/complete`)
}

export function cancelUpload(uploadId: number) {
  return request.delete(`/upload/${uploadId}`)
}
