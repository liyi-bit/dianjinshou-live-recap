import request from './request'

export interface CompetitorDimension {
  name: string
  myScore: number
  competitorScore: number
}

export interface CompetitorReportData {
  dimensions: CompetitorDimension[]
  highlights: string[]
  improvements: string[]
  summary: string
}

export interface CompetitorReport {
  id: number
  streamerId: number
  competitorStreamerId: number
  streamerName: string
  competitorStreamerName: string
  streamerAvatar: string | null
  competitorStreamerAvatar: string | null
  report: string
  aiModel: string
  status: string
  createdAt: string
}

export function createCompetitorReport(data: {
  streamerId: number
  competitorStreamerId: number
  recordingId?: number
  competitorRecordingId?: number
}) {
  return request.post<CompetitorReport>('/analysis/competitor-report', data)
}

export function listCompetitorReports(page = 1, size = 20) {
  return request.get<{ items: CompetitorReport[]; total: number }>('/analysis/competitor-reports', {
    params: { page, size }
  })
}

export function getCompetitorReport(id: number) {
  return request.get<CompetitorReport>(`/analysis/competitor-reports/${id}`)
}
