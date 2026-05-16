import request from './request'

export interface FileAnalysisTask {
  id: number
  fileName: string
  fileSize: number
  duration: number | null
  aiModel: string
  status: string
  errorMsg: string | null
  createdAt: string
}

export interface CreateFileAnalysisParams {
  fileName: string
  storageKey: string
  industryId?: number
  aiModel?: string
}

export function createFileAnalysis(params: CreateFileAnalysisParams) {
  return request.post<FileAnalysisTask>('/file-analysis', params)
}

export function listFileAnalyses(page: number, size: number, status?: string, keyword?: string) {
  return request.get<any>('/file-analysis', { params: { page, size, status, keyword } })
}

export function getFileAnalysis(id: number) {
  return request.get<FileAnalysisTask>(`/file-analysis/${id}`)
}

export function deleteFileAnalysis(id: number) {
  return request.delete(`/file-analysis/${id}`)
}

export function createFileClip(fileAnalysisId: number, params: {
  clipStart: number
  clipEnd: number
  clipCategory: string
  clipFilename?: string
  clipRemark?: string
}) {
  return request.post<FileAnalysisTask>(`/file-analysis/${fileAnalysisId}/clips`, params)
}

export function listFileClips(fileAnalysisId: number) {
  return request.get<FileAnalysisTask[]>(`/file-analysis/${fileAnalysisId}/clips`)
}

export interface CopywritingReviewResult {
  id: number
  textContent: string
  result: string | null
  riskScore: number | null
  status: string
  createdAt: string
}

export function submitCopywritingReview(params: {
  textContent: string
  industryId?: number
  checkSensitive?: boolean
  checkCompliance?: boolean
}) {
  return request.post<CopywritingReviewResult>('/file-analysis/copywriting-review', params)
}

export function listCopywritingReviews(page: number, size: number) {
  return request.get<any>('/file-analysis/copywriting-reviews', { params: { page, size } })
}
