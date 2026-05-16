import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  createSession,
  listSessions,
  getSessionDetail,
  deleteSession,
  sendMessage,
  type AiSessionItem,
  type AiSessionDetail,
  type ChatMessage
} from '@/api/aiSession'
import { Message } from '@arco-design/web-vue'

export const useAiSessionStore = defineStore('aiSession', () => {
  const sessions = ref<AiSessionItem[]>([])
  const sessionsTotal = ref(0)
  const sessionsLoading = ref(false)
  const currentSession = ref<AiSessionDetail | null>(null)
  const sending = ref(false)

  async function fetchSessions(page = 1, size = 50, assistantType?: string) {
    sessionsLoading.value = true
    try {
      const res = await listSessions(page, size, assistantType)
      const data = (res as any).data ?? res
      sessions.value = data.records || []
      sessionsTotal.value = data.total || 0
    } catch {
      sessions.value = []
    } finally {
      sessionsLoading.value = false
    }
  }

  async function create(assistantType: string, initialMessage?: string) {
    try {
      const res = await createSession(assistantType, initialMessage)
      const data = (res as any).data ?? res
      return data
    } catch {
      Message.error('创建会话失败')
      return null
    }
  }

  async function loadSession(id: number) {
    try {
      const res = await getSessionDetail(id)
      currentSession.value = (res as any).data ?? res
    } catch {
      currentSession.value = null
    }
  }

  async function remove(id: number) {
    try {
      await deleteSession(id)
      Message.success('已删除')
      return true
    } catch {
      Message.error('删除失败')
      return false
    }
  }

  async function send(content: string, presetQuestionId?: number) {
    if (!currentSession.value) return null
    sending.value = true
    try {
      const res = await sendMessage(currentSession.value.id, content, presetQuestionId)
      const data = (res as any).data ?? res
      // Reload session to get updated messages
      await loadSession(currentSession.value.id)
      return data
    } catch {
      Message.error('发送失败')
      return null
    } finally {
      sending.value = false
    }
  }

  return {
    sessions,
    sessionsTotal,
    sessionsLoading,
    currentSession,
    sending,
    fetchSessions,
    create,
    loadSession,
    remove,
    send
  }
})
