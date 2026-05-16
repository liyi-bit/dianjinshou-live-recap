import request from './request'

export interface ScriptTemplate {
  id: number
  name: string
  description: string
  category: string
  icon: string
  promptTemplate: string
  inputFields: string
  sortOrder: number
}

export interface InputField {
  name: string
  label: string
  type: 'text' | 'textarea' | 'select'
  options?: string[]
}

export interface ScriptGenerationItem {
  id: number
  templateId: number
  inputParams: string
  generatedText: string
  aiModel: string
  tokensUsed: number
  rating: number | null
  createdAt: string
}

export function listTemplates() {
  return request.get<ScriptTemplate[]>('/ai/script/templates')
}

export function generateScript(templateId: number, inputParams: string) {
  return request.post<ScriptGenerationItem>('/ai/script/generate', { templateId, inputParams })
}

export function listHistory(page: number, size: number) {
  return request.get<any>('/ai/script/history', { params: { page, size } })
}

export function rateScript(id: number, rating: number) {
  return request.post(`/ai/script/${id}/rate`, null, { params: { rating } })
}
