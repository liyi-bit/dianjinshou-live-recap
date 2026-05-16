import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useShortClipStore } from '../shortClip'

vi.mock('@/api/shortClip', () => ({
  listShortClips: vi.fn().mockResolvedValue({
    data: { records: [{ id: 1, clipName: '精彩片段', status: 'completed', duration: 60 }], total: 1 }
  }),
  createShortClip: vi.fn().mockResolvedValue({
    data: { id: 2, clipName: '新切片', status: 'pending', duration: 30 }
  }),
  deleteShortClip: vi.fn().mockResolvedValue({}),
  batchExportClips: vi.fn().mockResolvedValue({
    data: { exportKey: 'exports/100/123_abc.zip' }
  }),
  uploadClipToCloud: vi.fn().mockResolvedValue({})
}))

vi.mock('@arco-design/web-vue', () => ({
  Message: { success: vi.fn(), error: vi.fn(), warning: vi.fn() }
}))

describe('shortClip store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('fetchList populates list and total', async () => {
    const store = useShortClipStore()
    await store.fetchList(1, 20)

    expect(store.list.length).toBe(1)
    expect(store.list[0].clipName).toBe('精彩片段')
    expect(store.total).toBe(1)
  })

  it('create returns new clip', async () => {
    const store = useShortClipStore()
    const result = await store.create({
      recordingId: 10,
      startTime: 60,
      endTime: 90,
      clipName: '新切片'
    })

    expect(result).not.toBeNull()
    expect(result.id).toBe(2)
  })

  it('remove returns true on success', async () => {
    const store = useShortClipStore()
    const ok = await store.remove(1)

    expect(ok).toBe(true)
  })

  it('batchExport returns exportKey', async () => {
    const store = useShortClipStore()
    store.selectedIds = [1]
    const key = await store.batchExport()

    expect(key).toContain('exports/')
  })

  it('uploadToCloud returns true on success', async () => {
    const store = useShortClipStore()
    const ok = await store.uploadToCloud(1)

    expect(ok).toBe(true)
  })
})
