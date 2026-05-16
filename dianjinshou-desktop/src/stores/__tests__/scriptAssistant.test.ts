import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useScriptAssistantStore } from '../scriptAssistant'

vi.mock('@/api/scriptAssistant', () => ({
  listTemplates: vi.fn().mockResolvedValue({
    data: [
      { id: 1, name: '开场白话术', description: '直播开场白', category: '开场', icon: 'icon-play-arrow', inputFields: '[]', sortOrder: 1 },
      { id: 2, name: '促单话术', description: '成交话术', category: '成交', icon: 'icon-thunderbolt', inputFields: '[]', sortOrder: 4 }
    ]
  }),
  generateScript: vi.fn().mockResolvedValue({
    data: { id: 10, templateId: 1, generatedText: '生成的话术内容', rating: null, createdAt: '2026-04-11' }
  }),
  listHistory: vi.fn().mockResolvedValue({
    data: {
      records: [{ id: 10, templateId: 1, generatedText: '历史话术', rating: 5, createdAt: '2026-04-10' }],
      total: 1
    }
  }),
  rateScript: vi.fn().mockResolvedValue({})
}))

describe('scriptAssistant store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('fetchTemplates populates templates', async () => {
    const store = useScriptAssistantStore()
    await store.fetchTemplates()

    expect(store.templates.length).toBe(2)
    expect(store.templates[0].name).toBe('开场白话术')
  })

  it('doGenerate returns generated result', async () => {
    const store = useScriptAssistantStore()
    const result = await store.doGenerate(1, '{}')

    expect(result).not.toBeNull()
    expect(result.id).toBe(10)
    expect(store.lastGenerated).not.toBeNull()
  })

  it('fetchHistory populates history', async () => {
    const store = useScriptAssistantStore()
    await store.fetchHistory(1, 20)

    expect(store.history.length).toBe(1)
    expect(store.historyTotal).toBe(1)
  })

  it('doRate updates local state', async () => {
    const store = useScriptAssistantStore()
    await store.fetchHistory(1, 20)
    await store.doRate(10, 4)

    expect(store.history[0].rating).toBe(4)
  })
})
