import request from './request'

export interface AiSessionItem {
  id: number
  assistantType: string
  title: string
  messageCount: number
  lastMessageAt: string | null
  status: string
  createdAt: string
}

export interface ChatMessage {
  id: number
  role: string
  content: string
  thinking: string | null
  tokensUsed: number | null
  presetQuestionId: number | null
  createdAt: string
}

export interface AiSessionDetail extends AiSessionItem {
  messages: ChatMessage[]
}

export function createSession(assistantType: string, initialMessage?: string) {
  return request.post<AiSessionItem>('/ai/sessions', { assistantType, initialMessage })
}

export function listSessions(page: number, size: number, assistantType?: string) {
  return request.get<any>('/ai/sessions', { params: { page, size, assistantType } })
}

export function getSessionDetail(id: number) {
  return request.get<AiSessionDetail>(`/ai/sessions/${id}`)
}

export function updateSessionTitle(id: number, title: string) {
  return request.put(`/ai/sessions/${id}`, { title })
}

export function deleteSession(id: number) {
  return request.delete(`/ai/sessions/${id}`)
}

export function sendMessage(sessionId: number, content: string, presetQuestionId?: number) {
  return request.post<ChatMessage>(`/ai/sessions/${sessionId}/messages`, { content, presetQuestionId })
}
