import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useShortVideoStore } from '../shortVideo'

vi.mock('@/api/shortVideo', () => ({
  listCopywriting: vi.fn().mockResolvedValue({
    data: { records: [{ id: 1, title: '测试', status: 'completed', wordCount: 100 }], total: 1 }
  }),
  extractCopywriting: vi.fn().mockResolvedValue({
    data: { id: 2, status: 'pending', sourceType: 'url' }
  }),
  deleteCopywriting: vi.fn().mockResolvedValue({}),
  recordCopy: vi.fn().mockResolvedValue({}),
  searchCreators: vi.fn().mockResolvedValue({
    data: [{ id: 1, nickname: '达人A', platform: 'douyin', followerCount: 500000 }]
  }),
  listSubscriptions: vi.fn().mockResolvedValue({
    data: { creators: [], trending: [] }
  })
}))

vi.mock('@arco-design/web-vue', () => ({
  Message: { success: vi.fn(), error: vi.fn() }
}))

describe('shortVideo store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('fetchCopywriting populates list', async () => {
    const store = useShortVideoStore()
    await store.fetchCopywriting(1, 20)

    expect(store.copywritingList.length).toBe(1)
    expect(store.copywritingTotal).toBe(1)
  })

  it('createExtract returns new task', async () => {
    const store = useShortVideoStore()
    const result = await store.createExtract({ sourceType: 'url', sourceUrl: 'https://example.com/v.mp4' })

    expect(result).not.toBeNull()
    expect(result.id).toBe(2)
  })

  it('removeCopywriting returns true', async () => {
    const store = useShortVideoStore()
    const ok = await store.removeCopywriting(1)
    expect(ok).toBe(true)
  })

  it('fetchCreators populates list', async () => {
    const store = useShortVideoStore()
    await store.fetchCreators({ keyword: '达人' })

    expect(store.creatorList.length).toBe(1)
    expect(store.creatorList[0].nickname).toBe('达人A')
  })

  it('fetchSubscriptions loads data', async () => {
    const store = useShortVideoStore()
    await store.fetchSubscriptions()

    expect(store.subscriptions).not.toBeNull()
  })
})
