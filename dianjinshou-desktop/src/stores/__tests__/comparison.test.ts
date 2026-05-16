import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useComparisonStore } from '../comparison'

// Mock comparison API
vi.mock('@/api/comparison', () => ({
  getCurrentDraft: vi.fn(),
  createDraft: vi.fn(),
  selectSecond: vi.fn(),
  cancelDraft: vi.fn(),
  getComparisons: vi.fn(),
  swapComparison: vi.fn(),
  batchDeleteComparisons: vi.fn()
}))

// Mock arco message
vi.mock('@arco-design/web-vue', () => ({
  Message: { success: vi.fn(), error: vi.fn() }
}))

import * as comparisonApi from '@/api/comparison'

describe('comparison store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchDraft sets draft on success', async () => {
    vi.mocked(comparisonApi.getCurrentDraft).mockResolvedValue({
      data: { id: 1, firstRecordingId: 10, listContext: 'AI_FULL_RECAP', expiresAt: '2026-01-01' }
    } as any)

    const store = useComparisonStore()
    await store.fetchDraft()

    expect(store.draft).toBeTruthy()
    expect(store.draft?.id).toBe(1)
  })

  it('fetchDraft sets null on error', async () => {
    vi.mocked(comparisonApi.getCurrentDraft).mockRejectedValue(new Error('fail'))

    const store = useComparisonStore()
    await store.fetchDraft()

    expect(store.draft).toBeNull()
  })

  it('lockFirst creates draft and returns it', async () => {
    vi.mocked(comparisonApi.createDraft).mockResolvedValue({
      data: { id: 2, firstRecordingId: 20, listContext: 'AI_FULL_RECAP' }
    } as any)

    const store = useComparisonStore()
    const result = await store.lockFirst(20, 'AI_FULL_RECAP')

    expect(result).toBeTruthy()
    expect(store.draft?.firstRecordingId).toBe(20)
  })

  it('lockFirst returns null on error', async () => {
    vi.mocked(comparisonApi.createDraft).mockRejectedValue(new Error('fail'))

    const store = useComparisonStore()
    const result = await store.lockFirst(20, 'AI_FULL_RECAP')

    expect(result).toBeNull()
  })

  it('selectSecondAndConfirm creates comparison and clears draft', async () => {
    vi.mocked(comparisonApi.selectSecond).mockResolvedValue({
      data: { id: 100, type: 'full', status: 'pending' }
    } as any)

    const store = useComparisonStore()
    store.draft = { id: 1, firstRecordingId: 10, listContext: 'AI_FULL_RECAP', expiresAt: '', createdAt: '' }

    const result = await store.selectSecondAndConfirm(20, 'AI_FULL_RECAP')

    expect(result).toBeTruthy()
    expect(result?.id).toBe(100)
    expect(store.draft).toBeNull()
  })

  it('selectSecondAndConfirm returns null when no draft', async () => {
    const store = useComparisonStore()
    const result = await store.selectSecondAndConfirm(20, 'AI_FULL_RECAP')
    expect(result).toBeNull()
  })

  it('cancel clears draft', async () => {
    vi.mocked(comparisonApi.cancelDraft).mockResolvedValue(undefined as any)

    const store = useComparisonStore()
    store.draft = { id: 1, firstRecordingId: 10, listContext: 'AI_FULL_RECAP', expiresAt: '', createdAt: '' }

    await store.cancel()

    expect(store.draft).toBeNull()
  })

  it('fetchList populates list and total', async () => {
    vi.mocked(comparisonApi.getComparisons).mockResolvedValue({
      data: {
        items: [{ id: 1 }, { id: 2 }],
        total: 2
      }
    } as any)

    const store = useComparisonStore()
    await store.fetchList('full', 1, 10)

    expect(store.list.length).toBe(2)
    expect(store.total).toBe(2)
    expect(store.loading).toBe(false)
  })

  it('fetchList resets on error', async () => {
    vi.mocked(comparisonApi.getComparisons).mockRejectedValue(new Error('fail'))

    const store = useComparisonStore()
    await store.fetchList()

    expect(store.list).toEqual([])
    expect(store.total).toBe(0)
  })

  it('swap returns true on success', async () => {
    vi.mocked(comparisonApi.swapComparison).mockResolvedValue(undefined as any)

    const store = useComparisonStore()
    const result = await store.swap(1)

    expect(result).toBe(true)
  })

  it('batchDelete returns true on success', async () => {
    vi.mocked(comparisonApi.batchDeleteComparisons).mockResolvedValue(undefined as any)

    const store = useComparisonStore()
    const result = await store.batchDelete([1, 2])

    expect(result).toBe(true)
  })
})
