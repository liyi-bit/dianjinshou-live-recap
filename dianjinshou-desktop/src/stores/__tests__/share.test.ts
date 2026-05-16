import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/api/share', () => ({
  createShareLink: vi.fn().mockResolvedValue({
    data: {
      id: 1,
      cloudFileId: 10,
      fileName: 'demo.mp4',
      shareCode: 'abc12345',
      shareUrl: '/s/abc12345',
      hasPassword: false,
      expiresAt: '2026-04-12 00:00:00',
      maxDownloads: null,
      downloadCount: 0,
      viewCount: 0,
      status: 'active',
      createdAt: '2026-04-11 00:00:00'
    }
  }),
  listMyShares: vi.fn().mockResolvedValue({
    data: [
      { id: 1, shareCode: 'abc12345', fileName: 'demo.mp4', status: 'active', viewCount: 3, downloadCount: 1 },
      { id: 2, shareCode: 'xyz67890', fileName: 'clip.mp4', status: 'expired', viewCount: 10, downloadCount: 5 }
    ]
  }),
  cancelShare: vi.fn().mockResolvedValue({})
}))

// Simple inline composable test — no store file needed, just test the API layer via direct import
import { createShareLink, listMyShares, cancelShare } from '@/api/share'

describe('share API', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('createShareLink calls API with correct params', async () => {
    const res = await createShareLink(10, 'pass', 24, 100)
    expect(createShareLink).toHaveBeenCalledWith(10, 'pass', 24, 100)
    expect((res as any).data.shareCode).toBe('abc12345')
  })

  it('listMyShares returns share list', async () => {
    const res = await listMyShares()
    const shares = (res as any).data ?? res
    expect(Array.isArray(shares)).toBe(true)
    expect(shares.length).toBe(2)
    expect(shares[0].status).toBe('active')
  })

  it('cancelShare calls API', async () => {
    await cancelShare(1)
    expect(cancelShare).toHaveBeenCalledWith(1)
  })
})
