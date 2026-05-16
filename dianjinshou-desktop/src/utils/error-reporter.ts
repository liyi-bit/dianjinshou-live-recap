/**
 * 渲染进程错误上报器（v1.1.0）。
 *
 * 功能：
 *   - 捕获 window.onerror / unhandledrejection / Vue errorHandler
 *   - 维护最多 50 条环形面包屑（route 切换 / 用户点击 / API 调用自动埋点）
 *   - 5 秒或 10 条批量 flush；失败存 localStorage 下次启动重传
 *   - 同一 stack 1 分钟内只报 1 次（去重 + 防循环）
 *
 * 用法（main.ts）：
 *   import { initErrorReporter, reportError, breadcrumb } from '@/utils/error-reporter'
 *   initErrorReporter(app, router)
 *   // 业务代码里：
 *   reportError('asr', err, { recordingId: 123 })
 *   breadcrumb('click', 'downloadBtn')
 */
import type { App } from 'vue'
import type { Router } from 'vue-router'
import { nowLocalDateTime } from '@/utils/format'

type Level = 'error' | 'warn' | 'fatal'

interface Breadcrumb {
  ts: number
  type: 'route' | 'click' | 'api' | 'log'
  message: string
  data?: Record<string, any>
}

interface ErrorItem {
  level: Level
  scope: string
  source: 'desktop-renderer'
  clientVersion: string
  platform: string
  userAgent: string
  userId?: number | string
  message: string
  stack?: string
  recordingId?: number
  taskId?: number
  modelVersion?: string
  details?: Record<string, any>
  breadcrumbs: Breadcrumb[]
  occurredAt: string
}

/** 从当前 access token 解出 userId（仅本地解析 payload，不验签）。 */
function getCurrentUserId(): number | string | undefined {
  try {
    const token = localStorage.getItem('accessToken') || ''
    if (!token) return undefined
    const parts = token.split('.')
    if (parts.length < 2) return undefined
    const json = atob(parts[1].replace(/-/g, '+').replace(/_/g, '/'))
    const payload = JSON.parse(json)
    return payload?.userId ?? payload?.uid ?? payload?.sub ?? undefined
  } catch {
    return undefined
  }
}

const MAX_CRUMBS = 50
const FLUSH_INTERVAL_MS = 5000
const FLUSH_SIZE_TRIGGER = 10
const DEDUP_WINDOW_MS = 60_000
const STORAGE_KEY = 'errorQueueV1'
const MAX_QUEUE = 200 // 防止 localStorage 无限膨胀

let _inited = false
const crumbs: Breadcrumb[] = []
let queue: ErrorItem[] = []
const recentStacks = new Map<string, number>()  // stack → lastTs
let flushTimer: ReturnType<typeof setInterval> | null = null
let _clientVersion = '0.0.0'
let _platform = ''

function getApiBase(): string {
  return (import.meta.env?.VITE_API_BASE as string) || 'http://localhost:18081/api/v1'
}

export function breadcrumb(type: Breadcrumb['type'], message: string, data?: Record<string, any>) {
  if (crumbs.length >= MAX_CRUMBS) crumbs.shift()
  crumbs.push({ ts: Date.now(), type, message, data })
}

/** 业务代码主动上报（捕获 catch 时用）。 */
export function reportError(scope: string, err: unknown, details?: Record<string, any>, level: Level = 'error') {
  try {
    const errObj = toErr(err)
    const key = (errObj.stack || errObj.message || '').slice(0, 500)
    if (dedup(key)) return
    enqueue({
      level,
      scope,
      source: 'desktop-renderer',
      clientVersion: _clientVersion,
      platform: _platform,
      userAgent: navigator.userAgent || '',
      userId: getCurrentUserId(),
      message: (errObj.message || String(err)).slice(0, 1024),
      stack: errObj.stack,
      details,
      breadcrumbs: crumbs.slice(-MAX_CRUMBS),
      occurredAt: nowLocalDateTime(),
    })
  } catch {
    // 静默 —— 错误上报自身绝不能再抛错
  }
}

export function initErrorReporter(app: App, router?: Router) {
  if (_inited) return
  _inited = true

  // 平台信息
  try {
    _clientVersion = ((window as any).electronAPI?.getAppVersion?.() as any) || '0.0.0'
    // getAppVersion 返回 Promise；先给一个同步占位，异步再覆盖
    Promise.resolve(_clientVersion).then((v) => { if (typeof v === 'string') _clientVersion = v })
    _platform = navigator.platform || ''
  } catch {}

  // window.onerror
  window.addEventListener('error', (e) => {
    reportError('renderer.window.error', e.error || e.message, {
      filename: (e as any).filename,
      lineno: (e as any).lineno,
      colno: (e as any).colno,
    })
  })
  // unhandledrejection
  window.addEventListener('unhandledrejection', (e) => {
    reportError('renderer.unhandledrejection', e.reason)
  })
  // Vue app errorHandler
  app.config.errorHandler = (err, _instance, info) => {
    reportError('renderer.vue', err, { info })
    // 原 Vue 行为：打 console
    // eslint-disable-next-line no-console
    console.error('[Vue errorHandler]', err, info)
  }

  // Router 切换埋点
  if (router) {
    router.afterEach((to, from) => {
      breadcrumb('route', `${from.fullPath || '-'} → ${to.fullPath}`, { name: to.name })
    })
  }

  // 用户点击埋点（捕获阶段，减少业务代码干扰）
  document.addEventListener('click', (e) => {
    const el = e.target as HTMLElement | null
    if (!el) return
    const tag = el.tagName?.toLowerCase()
    if (tag === 'button' || tag === 'a' || el.getAttribute('role') === 'button') {
      const txt = (el.textContent || '').trim().slice(0, 32)
      breadcrumb('click', `${tag}: ${txt}`, { id: el.id || undefined })
    }
  }, { capture: true, passive: true })

  // 恢复上次未 flush 的队列
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const saved: ErrorItem[] = JSON.parse(raw)
      if (Array.isArray(saved) && saved.length) {
        queue.push(...saved.slice(-MAX_QUEUE))
      }
      localStorage.removeItem(STORAGE_KEY)
    }
  } catch {}

  // 定时 flush
  flushTimer = setInterval(flushQueue, FLUSH_INTERVAL_MS)

  // 页面关闭前持久化
  window.addEventListener('beforeunload', () => persistQueue())
}

function enqueue(item: ErrorItem) {
  queue.push(item)
  if (queue.length > MAX_QUEUE) queue.splice(0, queue.length - MAX_QUEUE)
  if (queue.length >= FLUSH_SIZE_TRIGGER) flushQueue()
}

function persistQueue() {
  if (queue.length === 0) return
  try { localStorage.setItem(STORAGE_KEY, JSON.stringify(queue.slice(-MAX_QUEUE))) } catch {}
}

function dedup(key: string): boolean {
  const now = Date.now()
  // 清理旧记录
  for (const [k, ts] of recentStacks) {
    if (now - ts > DEDUP_WINDOW_MS) recentStacks.delete(k)
  }
  if (recentStacks.has(key)) return true
  recentStacks.set(key, now)
  return false
}

async function flushQueue() {
  if (queue.length === 0) return
  const payload = queue.splice(0)
  try {
    const res = await fetch(`${getApiBase()}/obs/error`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ items: payload }),
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
  } catch (e) {
    // 失败：放回队首，下次 flush 再试；避免无限增长，只保留最后 MAX_QUEUE
    queue.unshift(...payload)
    if (queue.length > MAX_QUEUE) queue = queue.slice(-MAX_QUEUE)
    persistQueue()
  }
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
