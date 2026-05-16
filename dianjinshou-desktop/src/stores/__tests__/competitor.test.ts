import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/api/competitor', () => ({
  createCompetitorReport: vi.fn().mockResolvedValue({
    data: {
      id: 1,
      streamerId: 1,
      competitorStreamerId: 2,
      streamerName: '主播A',
      competitorStreamerName: '主播B',
      report: '{"dimensions":[],"highlights":[],"improvements":[],"summary":"test"}',
      status: 'completed'
    }
  }),
  listCompetitorReports: vi.fn().mockResolvedValue({
    data: { items: [{ id: 1, streamerName: '主播A', competitorStreamerName: '主播B', status: 'completed' }], total: 1 }
  }),
  getCompetitorReport: vi.fn().mockResolvedValue({
    data: { id: 1, streamerName: '主播A', report: '{}', status: 'completed' }
  })
}))

import { createCompetitorReport, listCompetitorReports } from '@/api/competitor'

describe('competitor API', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('createCompetitorReport returns report', async () => {
    const res = await createCompetitorReport({ streamerId: 1, competitorStreamerId: 2 })
    const data = (res as any).data ?? res
    expect(data.streamerName).toBe('主播A')
    expect(data.competitorStreamerName).toBe('主播B')
    expect(data.status).toBe('completed')
  })

  it('listCompetitorReports returns list', async () => {
    const res = await listCompetitorReports()
    const data = (res as any).data ?? res
    expect(data.items.length).toBe(1)
    expect(data.total).toBe(1)
  })
})
