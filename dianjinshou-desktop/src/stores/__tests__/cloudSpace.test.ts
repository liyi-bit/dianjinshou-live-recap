import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useCloudSpaceStore } from '../cloudSpace'

vi.mock('@/api/cloudSpace', () => ({
  listRecordings: vi.fn().mockResolvedValue({
    data: {
      records: [{ id: 1, fileName: 'test.mp4', fileSize: 1000, fileType: 'recording', status: 'active' }],
      total: 1
    }
  }),
  listClips: vi.fn().mockResolvedValue({ data: { records: [], total: 0 } }),
  listDocuments: vi.fn().mockResolvedValue({ data: { records: [], total: 0 } }),
  batchDeleteFiles: vi.fn().mockResolvedValue({}),
  batchDownloadFiles: vi.fn().mockResolvedValue({ data: ['url1', 'url2'] }),
  getCloudUsage: vi.fn().mockResolvedValue({
    data: { usedBytes: 500000, totalQuotaBytes: 1073741824, fileCount: 5, usagePercent: 0.05 }
  })
}))

describe('cloudSpace store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('fetchFiles populates recordings', async () => {
    const store = useCloudSpaceStore()
    await store.fetchFiles('recording', 1, 20)

    expect(store.files.length).toBe(1)
    expect(store.total).toBe(1)
    expect(store.files[0].fileName).toBe('test.mp4')
  })

  it('fetchUsage populates usage data', async () => {
    const store = useCloudSpaceStore()
    await store.fetchUsage()

    expect(store.usage).not.toBeNull()
    expect(store.usage!.fileCount).toBe(5)
  })

  it('batchDownload returns URLs', async () => {
    const store = useCloudSpaceStore()
    const urls = await store.batchDownload([1, 2])

    expect(urls).toEqual(['url1', 'url2'])
  })

  it('batchDelete clears selectedIds', async () => {
    const store = useCloudSpaceStore()
    store.selectedIds = [1, 2]
    await store.batchDelete([1, 2])

    expect(store.selectedIds).toEqual([])
  })
})
