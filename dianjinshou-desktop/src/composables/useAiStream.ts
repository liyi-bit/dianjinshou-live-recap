import { ref, type Ref } from 'vue'

export interface UseAiStreamOptions {
  onChunk?: (chunk: string) => void
  onComplete?: (fullText: string) => void
  onError?: (error: Error) => void
}

export interface UseAiStreamReturn {
  content: Ref<string>
  thinking: Ref<string>
  loading: Ref<boolean>
  error: Ref<string | null>
  start: (url: string, body?: Record<string, unknown>, headers?: Record<string, string>) => void
  stop: () => void
}

export function useAiStream(options: UseAiStreamOptions = {}): UseAiStreamReturn {
  const content = ref('')
  const thinking = ref('')
  const loading = ref(false)
  const error = ref<string | null>(null)
  let abortController: AbortController | null = null

  function stop() {
    if (abortController) {
      abortController.abort()
      abortController = null
    }
    loading.value = false
  }

  async function start(
    url: string,
    body?: Record<string, unknown>,
    headers?: Record<string, string>
  ) {
    stop()
    content.value = ''
    thinking.value = ''
    error.value = null
    loading.value = true

    abortController = new AbortController()

    try {
      const response = await fetch(url, {
        method: body ? 'POST' : 'GET',
        headers: {
          'Content-Type': 'application/json',
          ...headers
        },
        body: body ? JSON.stringify(body) : undefined,
        signal: abortController.signal
      })

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }

      const reader = response.body?.getReader()
      if (!reader) {
        throw new Error('No readable stream')
      }

      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const data = line.slice(6)
            if (data === '[DONE]') continue

            try {
              const parsed = JSON.parse(data)
              if (parsed.thinking) {
                thinking.value += parsed.thinking
              } else if (parsed.content) {
                content.value += parsed.content
                options.onChunk?.(parsed.content)
              }
            } catch {
              // Plain text chunk
              content.value += data
              options.onChunk?.(data)
            }
          }
        }
      }

      options.onComplete?.(content.value)
    } catch (err: unknown) {
      if (err instanceof DOMException && err.name === 'AbortError') {
        return
      }
      const e = err instanceof Error ? err : new Error(String(err))
      error.value = e.message
      options.onError?.(e)
    } finally {
      loading.value = false
      abortController = null
    }
  }

  return { content, thinking, loading, error, start, stop }
}
