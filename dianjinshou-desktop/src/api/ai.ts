import request from './request'

// --- Types ---

export interface ChatRequest {
  taskId?: number
  comparisonId?: number
  assistantType: string
  message: string
  aiModel?: string
  presetQuestionId?: number
}

export interface ChatMessage {
  id: number
  role: 'user' | 'assistant' | 'system'
  content: string
  thinking: string | null
  tokensUsed: number | null
  presetQuestionId: number | null
  createdAt: string
}

export interface PresetQuestion {
  id: number
  title: string
  desc: string
  color: string
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
}

// --- API functions ---

export function sendChat(data: ChatRequest) {
  return request.post<ChatMessage>('/ai/chat', data, { timeout: 120000 })
}

export function getPresets(type: string) {
  return request.get<PresetQuestion[]>(`/ai/presets/${type}`)
}

export function switchModel(aiModel: string) {
  return request.post<{ aiModel: string; message: string }>('/ai/model/switch', { aiModel })
}

export function getChatHistory(params: {
  taskId?: number
  comparisonId?: number
  assistantType?: string
  page?: number
  size?: number
}) {
  return request.get<PageResult<ChatMessage>>('/ai/history', { params })
}
