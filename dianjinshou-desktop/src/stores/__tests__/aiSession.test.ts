import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAiSessionStore } from '../aiSession'

vi.mock('@/api/aiSession', () => ({
  createSession: vi.fn().mockResolvedValue({
    data: { id: 1, assistantType: 'operation', title: '新会话', messageCount: 0, status: 'active' }
  }),
  listSessions: vi.fn().mockResolvedValue({
    data: { records: [{ id: 1, title: '测试会话', messageCount: 3, assistantType: 'operation' }], total: 1 }
  }),
  getSessionDetail: vi.fn().mockResolvedValue({
    data: { id: 1, title: '测试会话', messages: [{ id: 1, role: 'user', content: '你好' }] }
  }),
  deleteSession: vi.fn().mockResolvedValue({}),
  sendMessage: vi.fn().mockResolvedValue({
    data: { id: 2, role: 'user', content: '测试消息' }
  })
}))

vi.mock('@arco-design/web-vue', () => ({
  Message: { success: vi.fn(), error: vi.fn() }
}))

describe('aiSession store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('fetchSessions populates list', async () => {
    const store = useAiSessionStore()
    await store.fetchSessions(1, 50, 'operation')

    expect(store.sessions.length).toBe(1)
    expect(store.sessions[0].title).toBe('测试会话')
  })

  it('create returns session', async () => {
    const store = useAiSessionStore()
    const result = await store.create('operation')

    expect(result).not.toBeNull()
    expect(result.id).toBe(1)
  })

  it('loadSession populates currentSession', async () => {
    const store = useAiSessionStore()
    await store.loadSession(1)

    expect(store.currentSession).not.toBeNull()
    expect(store.currentSession!.title).toBe('测试会话')
  })

  it('remove returns true', async () => {
    const store = useAiSessionStore()
    const ok = await store.remove(1)
    expect(ok).toBe(true)
  })

  it('send appends message to currentSession', async () => {
    const store = useAiSessionStore()
    await store.loadSession(1)
    expect(store.currentSession).not.toBeNull()

    const msg = await store.send('测试消息')
    expect(msg).not.toBeNull()
  })
})
