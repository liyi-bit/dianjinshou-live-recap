import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useFileAnalysisStore } from '../fileAnalysis'

vi.mock('@/api/fileAnalysis', () => ({
  listFileAnalyses: vi.fn().mockResolvedValue({
    data: { records: [{ id: 1, fileName: 'test.mp4', status: 'completed' }], total: 1 }
  }),
  getFileAnalysis: vi.fn().mockResolvedValue({
    data: { id: 1, fileName: 'test.mp4', status: 'completed', aiModel: 'doubao' }
  }),
  createFileAnalysis: vi.fn().mockResolvedValue({
    data: { id: 2, fileName: 'new.mp4', status: 'pending' }
  }),
  deleteFileAnalysis: vi.fn().mockResolvedValue({})
}))

vi.mock('@arco-design/web-vue', () => ({
  Message: { success: vi.fn(), error: vi.fn() }
}))

describe('fileAnalysis store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('fetchList populates list and total', async () => {
    const store = useFileAnalysisStore()
    await store.fetchList(1, 20)

    expect(store.list.length).toBe(1)
    expect(store.list[0].fileName).toBe('test.mp4')
    expect(store.total).toBe(1)
  })

  it('fetchList with filters', async () => {
    const store = useFileAnalysisStore()
    await store.fetchList(1, 20, 'completed', 'test')

    expect(store.list.length).toBe(1)
  })

  it('fetchDetail returns task', async () => {
    const store = useFileAnalysisStore()
    const result = await store.fetchDetail(1)

    expect(result).not.toBeNull()
    expect(result!.id).toBe(1)
    expect(store.currentTask).not.toBeNull()
  })

  it('create returns new task', async () => {
    const store = useFileAnalysisStore()
    const result = await store.create({ fileName: 'new.mp4', storageKey: 'key' })

    expect(result).not.toBeNull()
    expect(result.id).toBe(2)
  })

  it('remove returns true on success', async () => {
    const store = useFileAnalysisStore()
    const ok = await store.remove(1)

    expect(ok).toBe(true)
  })
})
