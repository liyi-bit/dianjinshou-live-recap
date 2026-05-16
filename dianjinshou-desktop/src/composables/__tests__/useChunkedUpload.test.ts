import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useChunkedUpload } from '../useChunkedUpload'

const mockInitUpload = vi.fn()
const mockUploadPart = vi.fn()
const mockCompleteUpload = vi.fn()
const mockCancelUpload = vi.fn()

vi.mock('@/api/upload', () => ({
  initUpload: (...args: any[]) => mockInitUpload(...args),
  uploadPart: (...args: any[]) => mockUploadPart(...args),
  completeUpload: (...args: any[]) => mockCompleteUpload(...args),
  cancelUpload: (...args: any[]) => mockCancelUpload(...args)
}))

function createMockFile(name: string, size: number): File {
  const blob = new Blob(['x'.repeat(Math.min(size, 100))], { type: 'video/mp4' })
  Object.defineProperty(blob, 'name', { value: name })
  Object.defineProperty(blob, 'size', { value: size })
  Object.defineProperty(blob, 'type', { value: 'video/mp4' })
  return blob as unknown as File
}

describe('useChunkedUpload', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('initializes with idle status', () => {
    const { status, progress } = useChunkedUpload()
    expect(status.value).toBe('idle')
    expect(progress.value).toBe(0)
  })

  it('startUpload completes successfully', async () => {
    mockInitUpload.mockResolvedValue({
      data: { uploadId: 1, storageKey: 'org1/test.mp4', totalParts: 2, partSize: 5242880 }
    })
    mockUploadPart.mockResolvedValue({})
    mockCompleteUpload.mockResolvedValue({ data: { storageKey: 'org1/test.mp4' } })

    const { startUpload, status, storageKey } = useChunkedUpload()
    const file = createMockFile('test.mp4', 10 * 1024 * 1024)

    const result = await startUpload(file)

    expect(result).toBe('org1/test.mp4')
    expect(status.value).toBe('completed')
    expect(storageKey.value).toBe('org1/test.mp4')
    expect(mockInitUpload).toHaveBeenCalledTimes(1)
    expect(mockUploadPart).toHaveBeenCalledTimes(2)
    expect(mockCompleteUpload).toHaveBeenCalledTimes(1)
  })

  it('handles upload failure', async () => {
    mockInitUpload.mockRejectedValue(new Error('Network error'))

    const { startUpload, status, error } = useChunkedUpload()
    const file = createMockFile('test.mp4', 1024)

    const result = await startUpload(file)

    expect(result).toBeNull()
    expect(status.value).toBe('failed')
    expect(error.value).toBe('Network error')
  })

  it('reset clears all state', async () => {
    mockInitUpload.mockResolvedValue({
      data: { uploadId: 1, storageKey: 'key', totalParts: 1, partSize: 5242880 }
    })
    mockUploadPart.mockResolvedValue({})
    mockCompleteUpload.mockResolvedValue({ data: { storageKey: 'key' } })

    const upload = useChunkedUpload()
    const file = createMockFile('test.mp4', 1024)
    await upload.startUpload(file)

    upload.reset()

    expect(upload.status.value).toBe('idle')
    expect(upload.uploadId.value).toBeNull()
    expect(upload.storageKey.value).toBe('')
    expect(upload.progress.value).toBe(0)
    expect(upload.error.value).toBeNull()
  })
})
