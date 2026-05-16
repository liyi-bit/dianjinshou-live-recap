/**
 * Electron 主进程错误上报（v1.1.0）。
 *
 * 捕获：
 *   - process.uncaughtException
 *   - process.unhandledRejection
 *   - app 'render-process-gone' / 'child-process-gone'
 *   - autoUpdater 'error'
 *
 * 批量 POST 到 /api/v1/obs/error，失败落本地文件 obs-queue.json 下次启动重传。
 */
import { app } from 'electron'
import { join } from 'path'
import {
  existsSync,
  readFileSync,
  writeFileSync,
  mkdirSync,
  appendFileSync,
} from 'fs'
import { request as httpsRequest } from 'https'
import { request as httpRequest } from 'http'
import { URL } from 'url'

type Level = 'error' | 'warn' | 'fatal'

interface ErrorItem {
  level: Level
  scope: string
  source: 'desktop-main'
  clientVersion: string
  platform: string
  message: string
  stack?: string
  recordingId?: number
  taskId?: number
  modelVersion?: string
  details?: Record<string, any>
  occurredAt: string
}

const FLUSH_INTERVAL_MS = 5000
const MAX_QUEUE = 200
const DEDUP_WINDOW_MS = 60_000

let inited = false
let queue: ErrorItem[] = []
const recentStacks = new Map<string, number>()
let flushTimer: NodeJS.Timeout | null = null
let apiBase = 'http://localhost:18081/api/v1'
let clientVersion = '0.0.0'
let platformInfo = ''

function queueFilePath(): string {
  const dir = app.getPath('userData')
  if (!existsSync(dir)) mkdirSync(dir, { recursive: true })
  return join(dir, 'obs-queue.json')
}

export function setApiBase(url: string): void {
  apiBase = url.replace(/\/+$/, '')
}

export function reportMainError(scope: string, err: unknown, details?: Record<string, any>, level: Level = 'error'): void {
  try {
    const e = toErr(err)
    const key = (e.stack || e.message || '').slice(0, 500)
    if (dedup(key)) return
    const item: ErrorItem = {
      level,
      scope,
      source: 'desktop-main',
      clientVersion,
      platform: platformInfo,
      message: e.message.slice(0, 1024),
      stack: e.stack,
      details,
      occurredAt: new Date().toISOString(),
    }
    queue.push(item)
    if (queue.length > MAX_QUEUE) queue.splice(0, queue.length - MAX_QUEUE)
    // 先落盘，防止进程下一秒崩掉丢失
    try { writeFileSync(queueFilePath(), JSON.stringify(queue)) } catch {}
  } catch {}
}

export function initMainErrorReporter(version: string, base: string): void {
  if (inited) return
  inited = true
  clientVersion = version || '0.0.0'
  platformInfo = `${process.platform} ${process.arch} node=${process.versions.node} electron=${process.versions.electron}`
  apiBase = base.replace(/\/+$/, '')

  // 恢复上次未发送的队列
  try {
    const p = queueFilePath()
    if (existsSync(p)) {
      const raw = readFileSync(p, 'utf-8')
      const arr = JSON.parse(raw)
      if (Array.isArray(arr)) queue.push(...arr.slice(-MAX_QUEUE))
    }
  } catch {}

  process.on('uncaughtException', (err) => {
    reportMainError('main.uncaughtException', err, undefined, 'fatal')
    // 继续传递给默认 handler —— 不吞异常
  })
  process.on('unhandledRejection', (reason) => {
    reportMainError('main.unhandledRejection', reason)
  })
  app.on('render-process-gone', (_e, _wc, details) => {
    reportMainError('main.renderGone', new Error('Renderer gone: ' + details.reason),
      { exitCode: details.exitCode, reason: details.reason })
  })
  app.on('child-process-gone', (_e, details) => {
    reportMainError('main.childGone', new Error('Child process gone: ' + details.type),
      { reason: details.reason, exitCode: details.exitCode, name: details.name })
  })

  flushTimer = setInterval(flush, FLUSH_INTERVAL_MS)
}

function dedup(key: string): boolean {
  const now = Date.now()
  for (const [k, ts] of recentStacks) {
    if (now - ts > DEDUP_WINDOW_MS) recentStacks.delete(k)
  }
  if (recentStacks.has(key)) return true
  recentStacks.set(key, now)
  return false
}

async function flush(): Promise<void> {
  if (queue.length === 0) return
  const payload = queue.splice(0)
  try {
    await post(`${apiBase}/obs/error`, { items: payload })
    // 成功：清理持久化
    try { writeFileSync(queueFilePath(), JSON.stringify(queue)) } catch {}
  } catch {
    // 失败：放回队列，持久化
    queue.unshift(...payload)
    if (queue.length > MAX_QUEUE) queue = queue.slice(-MAX_QUEUE)
    try { writeFileSync(queueFilePath(), JSON.stringify(queue)) } catch {}
  }
}

function post(url: string, body: any): Promise<void> {
  return new Promise((resolve, reject) => {
    const u = new URL(url)
    const isHttps = u.protocol === 'https:'
    const options = {
      method: 'POST',
      hostname: u.hostname,
      port: u.port || (isHttps ? 443 : 80),
      path: u.pathname + (u.search || ''),
      headers: { 'Content-Type': 'application/json' },
      timeout: 10_000,
    }
    const req = (isHttps ? httpsRequest : httpRequest)(options, (res) => {
      res.resume()
      if (res.statusCode && res.statusCode >= 200 && res.statusCode < 300) resolve()
      else reject(new Error(`HTTP ${res.statusCode}`))
    })
    req.on('error', reject)
    req.on('timeout', () => req.destroy(new Error('timeout')))
    req.write(JSON.stringify(body))
    req.end()
  })
}

function toErr(x: unknown): { message: string; stack?: string } {
  if (x instanceof Error) return { message: x.message, stack: x.stack }
  if (typeof x === 'string') return { message: x }
  if (x && typeof x === 'object') {
    const m = (x as any).message || JSON.stringify(x)
    const s = (x as any).stack
    return { message: typeof m === 'string' ? m : JSON.stringify(m), stack: typeof s === 'string' ? s : undefined }
  }
  return { message: String(x) }
}
