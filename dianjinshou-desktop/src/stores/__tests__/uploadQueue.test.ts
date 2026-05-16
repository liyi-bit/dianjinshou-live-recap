import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUploadQueueStore } from '../uploadQueue'

vi.mock('@/api/upload', () => ({
  initUpload: vi.fn().mockResolvedValue({
    data: { uploadId: 1, storageKey: 'org1/test.mp4', totalParts: 2, partSize: 5242880, partUploadUrls: [] }
  }),
  uploadPart: vi.fn().mockResolvedValue({}),
  completeUpload: vi.fn().mockResolvedValue({ data: { storageKey: 'org1/test.mp4' } }),
  cancelUpload: vi.fn().mockResolvedValue({})
}))

function createMockFile(name: string, size: number): File {
  const blob = new Blob(['x'.repeat(Math.min(size, 100))], { type: 'video/mp4' })
  Object.defineProperty(blob, 'name', { value: name })
  Object.defineProperty(blob, 'size', { value: size })
  return blob as unknown as File
}

describe('uploadQueue store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('addFile creates an item and starts processing', () => {
    const store = useUploadQueueStore()
    const file = createMockFile('test.mp4', 10 * 1024 * 1024)
    const id = store.addFile(file, 'files')

    expect(id).toBeTruthy()
    expect(store.items.length).toBe(1)
    // processQueue runs immediately, so status transitions to uploading
    expect(['queued', 'uploading']).toContain(store.items[0].status)
    expect(store.items[0].file.name).toBe('test.mp4')
  })

  it('removeItem removes item from queue', () => {
    const store = useUploadQueueStore()
    const file = createMockFile('test.mp4', 1024)
    const id = store.addFile(file)
    store.items[0].status = 'completed'

    store.removeItem(id)
    expect(store.items.length).toBe(0)
  })

  it('clearCompleted removes only completed/failed items', () => {
    const store = useUploadQueueStore()
    const f1 = createMockFile('a.mp4', 1024)
    const f2 = createMockFile('b.mp4', 1024)
    const f3 = createMockFile('c.mp4', 1024)

    store.addFile(f1)
    store.addFile(f2)
    store.addFile(f3)

    // Manually set statuses to avoid real upload
    store.items[0].status = 'completed'
    store.items[1].status = 'failed'
    store.items[2].status = 'queued'

    store.clearCompleted()
    expect(store.items.length).toBe(1)
    expect(store.items[0].file.name).toBe('c.mp4')
  })

  it('queuedCount computed returns correct count', () => {
    const store = useUploadQueueStore()
    const f1 = createMockFile('a.mp4', 1024)
    const f2 = createMockFile('b.mp4', 1024)
    store.addFile(f1)
    store.addFile(f2)

    // Both start as queued (processQueue is async, won't run in sync test)
    // Manually keep one queued
    store.items[0].status = 'uploading'
    store.items[1].status = 'queued'

    expect(store.queuedCount).toBe(1)
    expect(store.activeCount).toBe(1)
  })
})
