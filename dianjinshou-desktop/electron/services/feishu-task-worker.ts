/**
 * Background long-poll worker that drains Feishu tasks dispatched by the backend.
 *
 * The server-side {@link FeishuTaskDispatcher} holds per-user task queues. Whenever
 * a Feishu message arrives for a bot owned by this user, the message handler enqueues
 * a task (e.g. `resolve_douyin`) and blocks on a CompletableFuture waiting for the
 * desktop to return the parsed result. This worker is the other side of that channel.
 *
 * Loop: GET /feishu/tasks/next?wait=25 → dispatch by type → POST result back.
 * On auth loss the loop idles until a new token arrives; on network errors it backs off.
 */
import { backendApi } from './backend-api'
import { getLiveInfo } from './douyin-live-service'

type Task = {
  taskId: string
  type: string
  payload: Record<string, unknown>
}

const LONG_POLL_WAIT_SEC = 25
const NETWORK_BACKOFF_MS = 5000
const AUTH_WAIT_MS = 10000

class FeishuTaskWorker {
  private running = false
  private loopPromise: Promise<void> | null = null

  start(): void {
    if (this.running) return
    this.running = true
    console.log('[FeishuWorker] Starting long-poll loop')
    this.loopPromise = this.loop().catch((e) => {
      console.error('[FeishuWorker] Loop crashed:', (e as Error).message)
    })
  }

  stop(): void {
    this.running = false
    console.log('[FeishuWorker] Stop requested')
  }

  private async loop(): Promise<void> {
    while (this.running) {
      if (!backendApi.getAuthToken()) {
        await sleep(AUTH_WAIT_MS)
        continue
      }

      let task: Task | null = null
      try {
        task = await backendApi.pollFeishuTask(LONG_POLL_WAIT_SEC)
      } catch (err: any) {
        const msg = err?.message || String(err)
        if (msg === 'unauthorized') {
          console.warn('[FeishuWorker] Auth token rejected, idling until next login')
          await sleep(AUTH_WAIT_MS)
          continue
        }
        console.warn('[FeishuWorker] Poll failed:', msg)
        await sleep(NETWORK_BACKOFF_MS)
        continue
      }

      if (!task) continue

      // Don't await — process tasks concurrently so one slow resolve doesn't block the queue.
      this.handleTask(task).catch((e) =>
        console.error('[FeishuWorker] handleTask error:', (e as Error).message)
      )
    }
  }

  private async handleTask(task: Task): Promise<void> {
    console.log(`[FeishuWorker] Received task ${task.taskId} type=${task.type}`)
    try {
      if (task.type === 'resolve_douyin') {
        await this.handleResolveDouyin(task)
        return
      }
      await backendApi.deliverFeishuTaskResult(task.taskId, {
        ok: false,
        error: `unknown task type: ${task.type}`,
      })
    } catch (err) {
      const msg = (err as Error).message
      console.error(`[FeishuWorker] Task ${task.taskId} failed:`, msg)
      await backendApi.deliverFeishuTaskResult(task.taskId, { ok: false, error: msg })
    }
  }

  private async handleResolveDouyin(task: Task): Promise<void> {
    const value = String(task.payload?.value ?? '').trim()
    if (!value) {
      await backendApi.deliverFeishuTaskResult(task.taskId, { ok: false, error: '空的抖音输入' })
      return
    }

    const info = await getLiveInfo(value)
    if (info.error && !info.webRid) {
      await backendApi.deliverFeishuTaskResult(task.taskId, {
        ok: false,
        error: info.error,
      })
      return
    }

    const accountId = info.webRid || info.roomId || ''
    if (!accountId) {
      await backendApi.deliverFeishuTaskResult(task.taskId, {
        ok: false,
        error: '无法识别抖音账号',
      })
      return
    }

    await backendApi.deliverFeishuTaskResult(task.taskId, {
      ok: true,
      data: {
        accountId,
        secUid: info.secUid || '',
        anchorName: info.streamerName || '',
        anchorAvatar: info.streamerAvatar || '',
        isLive: info.isLive,
      },
    })
    console.log(`[FeishuWorker] Task ${task.taskId} resolved: accountId=${accountId}`)
  }
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

export const feishuTaskWorker = new FeishuTaskWorker()
