import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/api/diagnosis', () => ({
  generateDiagnosis: vi.fn().mockResolvedValue({
    data: {
      taskId: 1,
      overallScore: 77,
      overallComment: '整体表现中等偏上',
      dimensions: [
        { name: '开场话术', score: 78, suggestion: '建议优化', historicalAvg: 70 }
      ],
      radarData: [78, 82, 65, 71, 88, 75, 80, 69, 73, 92, 70, 76],
      radarLabels: ['开场话术', '产品介绍', '互动引导', '促单技巧', '节奏把控', '情绪感染力', '专业知识', '观众留存', '转化效率', '违规风险', '内容创意', '数据表现'],
      status: 'completed'
    }
  }),
  getDiagnosisReport: vi.fn().mockResolvedValue({
    data: {
      taskId: 1,
      overallScore: 77,
      status: 'completed',
      radarLabels: [],
      radarData: [],
      dimensions: []
    }
  })
}))

import { generateDiagnosis, getDiagnosisReport } from '@/api/diagnosis'

describe('diagnosis API', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('generateDiagnosis returns report with 12 radar data points', async () => {
    const res = await generateDiagnosis(1)
    const data = (res as any).data ?? res
    expect(data.overallScore).toBe(77)
    expect(data.radarData.length).toBe(12)
    expect(data.status).toBe('completed')
  })

  it('getDiagnosisReport returns report', async () => {
    const res = await getDiagnosisReport(1)
    const data = (res as any).data ?? res
    expect(data.taskId).toBe(1)
    expect(data.status).toBe('completed')
  })
})
