import request from './request'

export interface HitWord {
  word: string
  position: number
  category: string
  riskLevel: number
  replacement: string | null
}

export interface ComplianceCheckResult {
  hitWords: HitWord[]
  aiAnalysis: string
  riskScore: number
  riskLevel: string
  suggestions: string[]
}

export interface SensitiveWordItem {
  id: number
  word: string
  category: string
  riskLevel: number
  replacementSuggestion: string | null
  platform: string
  industry: string | null
  source: string
  isActive: number
  createdAt: string
}

export function checkCompliance(
  scenario: string,
  textContent: string,
  platform?: string,
  industry?: string
) {
  return request.post<ComplianceCheckResult>('/ai/compliance/check', {
    scenario,
    textContent,
    platform,
    industry
  })
}

export function listSensitiveWords(
  page: number,
  size: number,
  category?: string,
  keyword?: string
) {
  return request.get<any>('/ai/compliance/library', {
    params: { page, size, category, keyword }
  })
}

export function addSensitiveWord(word: Partial<SensitiveWordItem>) {
  return request.post<SensitiveWordItem>('/ai/compliance/library', word)
}

export function updateSensitiveWord(id: number, word: Partial<SensitiveWordItem>) {
  return request.put(`/ai/compliance/library/${id}`, word)
}

export function deleteSensitiveWord(id: number) {
  return request.delete(`/ai/compliance/library/${id}`)
}
