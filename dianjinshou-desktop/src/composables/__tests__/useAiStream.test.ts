import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useAiStream } from '../useAiStream'

// Mock fetch
const mockFetch = vi.fn()
vi.stubGlobal('fetch', mockFetch)

function createMockResponse(chunks: string[]) {
  let idx = 0
  const reader = {
    read: vi.fn().mockImplementation(() => {
      if (idx >= chunks.length) {
        return Promise.resolve({ done: true, value: undefined })
      }
      const value = new TextEncoder().encode(chunks[idx++])
      return Promise.resolve({ done: false, value })
    })
  }
  return {
    ok: true,
    body: { getReader: () => reader }
  }
}

describe('useAiStream', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('accumulates content from SSE chunks', async () => {
    mockFetch.mockResolvedValue(
      createMockResponse([
        'data: {"content":"Hello "}\n\n',
        'data: {"content":"World"}\n\n',
        'data: [DONE]\n\n'
      ])
    )

    const { content, start, loading } = useAiStream()
    await start('http://test/api')

    expect(content.value).toBe('Hello World')
    expect(loading.value).toBe(false)
  })

  it('handles thinking blocks', async () => {
    mockFetch.mockResolvedValue(
      createMockResponse([
        'data: {"thinking":"Let me think..."}\n\n',
        'data: {"content":"Answer"}\n\n'
      ])
    )

    const { content, thinking, start } = useAiStream()
    await start('http://test/api')

    expect(thinking.value).toBe('Let me think...')
    expect(content.value).toBe('Answer')
  })

  it('handles errors', async () => {
    mockFetch.mockResolvedValue({ ok: false, status: 500, statusText: 'Internal Error' })

    const onError = vi.fn()
    const { error, start } = useAiStream({ onError })
    await start('http://test/api')

    expect(error.value).toContain('500')
    expect(onError).toHaveBeenCalled()
  })

  it('calls onComplete callback', async () => {
    mockFetch.mockResolvedValue(
      createMockResponse([
        'data: {"content":"Done"}\n\n'
      ])
    )

    const onComplete = vi.fn()
    const { start } = useAiStream({ onComplete })
    await start('http://test/api')

    expect(onComplete).toHaveBeenCalledWith('Done')
  })
})
