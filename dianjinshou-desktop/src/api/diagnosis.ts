import request from './request'

export interface DimensionScore {
  name: string
  score: number
  suggestion: string
  historicalAvg: number | null
}

export interface DiagnosisReport {
  taskId: number
  overallScore: number
  overallComment: string
  dimensions: DimensionScore[]
  radarData: number[]
  radarLabels: string[]
  status: string
}

export function generateDiagnosis(taskId: number) {
  return request.post<DiagnosisReport>(`/analysis/${taskId}/diagnosis`)
}

export function getDiagnosisReport(taskId: number) {
  return request.get<DiagnosisReport>(`/analysis/${taskId}/diagnosis-report`)
}
