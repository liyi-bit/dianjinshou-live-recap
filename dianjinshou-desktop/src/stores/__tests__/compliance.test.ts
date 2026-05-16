import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useComplianceStore } from '../compliance'

vi.mock('@/api/compliance', () => ({
  checkCompliance: vi.fn().mockResolvedValue({
    data: {
      hitWords: [
        { word: '全网最低价', position: 5, category: '虚假宣传', riskLevel: 2, replacement: '优惠价格' }
      ],
      aiAnalysis: '检测完成',
      riskScore: 30,
      riskLevel: 'medium',
      suggestions: ['将「全网最低价」替换为「优惠价格」']
    }
  }),
  listSensitiveWords: vi.fn().mockResolvedValue({
    data: {
      records: [{ id: 1, word: '全网最低价', category: '虚假宣传', riskLevel: 2 }],
      total: 1
    }
  })
}))

describe('compliance store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('doCheck populates checkResult', async () => {
    const store = useComplianceStore()
    await store.doCheck('live_speech', '这是全网最低价的商品')

    expect(store.checkResult).not.toBeNull()
    expect(store.checkResult!.riskScore).toBe(30)
    expect(store.checkResult!.hitWords.length).toBe(1)
    expect(store.checkResult!.hitWords[0].word).toBe('全网最低价')
  })

  it('fetchWords populates words list', async () => {
    const store = useComplianceStore()
    await store.fetchWords(1, 20)

    expect(store.words.length).toBe(1)
    expect(store.wordTotal).toBe(1)
  })

  it('clearResult resets checkResult', async () => {
    const store = useComplianceStore()
    await store.doCheck('ad_copy', 'test')
    expect(store.checkResult).not.toBeNull()

    store.clearResult()
    expect(store.checkResult).toBeNull()
  })

  it('doCheck populates suggestions', async () => {
    const store = useComplianceStore()
    await store.doCheck('product_desc', '全网最低价商品描述')

    expect(store.checkResult).not.toBeNull()
    expect(store.checkResult!.suggestions.length).toBeGreaterThan(0)
    expect(store.checkResult!.riskLevel).toBe('medium')
  })
})
