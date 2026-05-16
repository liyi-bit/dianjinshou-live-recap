import request from './request'

export interface FeishuBotItem {
  id: number
  appId: string
  botName: string | null
  status: number
  lastConnectedAt: string | null
  lastError: string | null
  appSecret: string
}

export interface CreateFeishuBotPayload {
  appId: string
  appSecret: string
  botName?: string
}

export function listFeishuBots() {
  return request.get<FeishuBotItem[]>('/feishu/bots') as unknown as Promise<FeishuBotItem[]>
}

export function createFeishuBot(payload: CreateFeishuBotPayload) {
  return request.post<{ id: number; appId: string; botName: string | null }>('/feishu/bots', payload) as unknown as Promise<{
    id: number
    appId: string
    botName: string | null
  }>
}

export function deleteFeishuBot(id: number) {
  return request.delete(`/feishu/bots/${id}`)
}
