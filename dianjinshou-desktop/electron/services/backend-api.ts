/**
 * Backend API client for Electron main process.
 * Calls the dianjinshou-server REST API to create/update recording records.
 */

import axios, { AxiosInstance, AxiosRequestConfig } from 'axios'
import { BrowserWindow } from 'electron'

// Resolution order (highest → lowest):
//   1. CLI arg --api-base=...   ← lets a second local instance talk to localhost while
//                                  the packaged bundle defaults to the remote server
//   2. env VITE_API_BASE        ← OS-level override
//   3. import.meta.env.VITE_API_BASE   ← baked in at Vite build time from .env.{mode}
//   4. remote default
function parseApiBaseFromCli(): string | undefined {
  const argv = process.argv || []
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i]
    if (a === '--api-base' && i + 1 < argv.length) return argv[i + 1]
    if (a.startsWith('--api-base=')) return a.slice('--api-base='.length)
  }
  return undefined
}
const API_BASE =
  parseApiBaseFromCli() ||
  process.env.VITE_API_BASE ||
  (import.meta.env?.VITE_API_BASE as string | undefined) ||
  'http://localhost:18081/api/v1'
console.log('[BackendApi] API_BASE =', API_BASE)

class BackendApi {
  private client: AxiosInstance
  private authToken: string | null = null
  private refreshToken: string | null = null
  // 单例 refresh：多路并发 401 时只发一次 /auth/refresh，其他请求复用结果
  private refreshPromise: Promise<string | null> | null = null

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE,
      timeout: 10000,
      headers: { 'Content-Type': 'application/json' },
    })
  }

  setAuthToken(token: string): void {
    this.authToken = token
  }

  setRefreshToken(token: string): void {
    this.refreshToken = token
  }

  setTokens(access: string, refresh: string): void {
    this.authToken = access
    this.refreshToken = refresh
  }

  private getHeaders(): Record<string, string> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' }
    if (this.authToken) {
      headers.Authorization = `Bearer ${this.authToken}`
    }
    return headers
  }

  /**
   * 主进程自己刷新 access token，不依赖渲染进程。
   * 关键点：
   *   1. 不带 Authorization header（后端 JwtAuthFilter 对 /auth/refresh 放行，为保险也不带）
   *   2. 成功后通过 IPC 广播新 token 给所有渲染进程，让渲染进程也能同步到 localStorage
   *   3. 失败（refresh token 真过期）→ 清空内存 token，调用方负责抛
   */
  private refreshTokens(): Promise<string | null> {
    if (!this.refreshToken) return Promise.resolve(null)
    if (this.refreshPromise) return this.refreshPromise
    const rt = this.refreshToken
    this.refreshPromise = (async () => {
      try {
        const resp = await axios.post(
          `${API_BASE}/auth/refresh`,
          { refreshToken: rt },
          { timeout: 15000, headers: { 'Content-Type': 'application/json' } }
        )
        const data = resp.data?.data || resp.data
        const newAccess: string | undefined = data?.accessToken
        const newRefresh: string | undefined = data?.refreshToken
        if (!newAccess || !newRefresh) {
          console.warn('[BackendApi] refresh returned empty tokens, treating as failure')
          return null
        }
        this.authToken = newAccess
        this.refreshToken = newRefresh
        console.log('[BackendApi] access token refreshed in main process')
        for (const w of BrowserWindow.getAllWindows()) {
          try {
            w.webContents.send('auth:token-refreshed', {
              accessToken: newAccess,
              refreshToken: newRefresh,
            })
          } catch {}
        }
        return newAccess
      } catch (err: any) {
        const status = err?.response?.status
        const bizCode = err?.response?.data?.code
        console.warn('[BackendApi] refresh failed:',
          status ? `HTTP ${status} code=${bizCode}` : err?.message)
        // 只有后端明确说 refresh token 过期才清空，其他情况（网络错误、5xx）保留 token 等下次重试
        if (status === 401 || bizCode === 40101) {
          this.authToken = null
          this.refreshToken = null
        }
        return null
      } finally {
        this.refreshPromise = null
      }
    })()
    return this.refreshPromise
  }

  /**
   * 统一的请求包装器：遇到 401 自动 refresh 一次再重试。
   * 所有业务方法应通过这个 wrapper 发请求，而不是直接 this.client.xxx。
   */
  private async call<T>(
    method: 'get' | 'post',
    url: string,
    dataOrConfig?: any,
    config?: AxiosRequestConfig
  ): Promise<T> {
    const doRequest = async (): Promise<T> => {
      const cfg: AxiosRequestConfig = {
        ...(config || {}),
        headers: { ...(config?.headers || {}), ...this.getHeaders() },
      }
      if (method === 'get') {
        const r = await this.client.get(url, { ...cfg, params: dataOrConfig })
        return this.unwrapApiResponse<T>(r.data)
      }
      const r = await this.client.post(url, dataOrConfig, cfg)
      return this.unwrapApiResponse<T>(r.data)
    }
    try {
      return await doRequest()
    } catch (err: any) {
      const status = err?.response?.status
      if (status !== 401) throw err
      const newToken = await this.refreshTokens()
      if (!newToken) throw err
      return doRequest()
    }
  }

  private unwrapApiResponse<T>(body: any): T {
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 200) {
        return body.data as T
      }
      const err: any = new Error(body.message || `业务错误 code=${body.code}`)
      err.response = { status: 200, data: body }
      throw err
    }
    return body as T
  }

  /**
   * Create a recording record in the backend when recording starts.
   * Returns the backend recording ID.
   */
  async createRecording(params: {
    streamerId: number
    localFilePath?: string
    localFileName?: string
    streamUrl?: string
    resolution?: string
    sessionId?: string
  }): Promise<{ id: number } | null> {
    if (!this.authToken) {
      console.warn('[BackendApi] No auth token, skipping createRecording')
      return null
    }
    try {
      const resp = await this.call<any>('post', '/recordings', params)
      const data = resp?.data || resp
      return data?.id ? { id: data.id } : null
    } catch (err) {
      console.error('[BackendApi] createRecording failed:', (err as Error).message)
      return null
    }
  }

  /**
   * Notify the backend that a recording has completed (or failed).
   */
  async completeRecording(
    recordingId: number,
    params: {
      localFilePath?: string
      localFileName?: string
      duration?: number
      fileSize?: number
      status?: string
      errorMsg?: string
    }
  ): Promise<boolean> {
    if (!this.authToken) {
      console.warn('[BackendApi] No auth token, skipping completeRecording')
      return false
    }
    try {
      await this.call<any>('post', `/recordings/${recordingId}/complete`, params)
      return true
    } catch (err) {
      console.error('[BackendApi] completeRecording failed:', (err as Error).message)
      return false
    }
  }

  /**
   * v1.1.0：拉当前用户所有卡在 transcribing 状态的录制（软件意外关闭 / 崩溃导致 ASR 没跑完的）。
   */
  async listPendingAsr(): Promise<Array<{ id: number; localFilePath?: string; localFileName?: string }> | null> {
    if (!this.authToken) return null
    try {
      const resp = await this.call<any>('get', '/recordings/pending-asr')
      return resp?.data || resp || []
    } catch (err) {
      console.error('[BackendApi] listPendingAsr failed:', (err as Error).message)
      return null
    }
  }

  /**
   * v1.1.0：本地 MP4 已丢失 / 无法续跑 ASR，把记录标为 failed。
   */
  async markAsrFailed(recordingId: number, reason: string): Promise<boolean> {
    if (!this.authToken) return false
    try {
      await this.call<any>('post', `/recordings/${recordingId}/mark-asr-failed`, { reason })
      return true
    } catch (err) {
      console.error('[BackendApi] markAsrFailed failed:', (err as Error).message)
      return false
    }
  }

  /**
   * Submit ASR transcription result to the server for analysis.
   */
  async submitAsrResult(data: {
    recordingId: number
    segments: Array<{
      segmentIndex: number
      startTime: string
      endTime: string
      text: string
    }>
    industry?: string
    aiModel?: string
    /** v1.1.0：默认 false 只写逐字稿，等用户手动触发 AI；老客户端不传 → 后端兼容老行为 */
    autoAnalyze?: boolean
  }): Promise<{ taskId: number; status: string } | null> {
    if (!this.authToken) return null
    try {
      const resp = await this.call<any>('post', '/analysis/submit-asr', data, { timeout: 30000 })
      return resp?.data || resp || null
    } catch (err: any) {
      const status = err?.response?.status
      const body = err?.response?.data
      console.error('[BackendApi] submitAsrResult failed: status=%s body=%s message=%s',
        status, typeof body === 'string' ? body : JSON.stringify(body), err?.message)
      return null
    }
  }

  /**
   * Submit clip ASR transcription result to the server for analysis.
   */
  async submitClipAsrResult(data: {
    recordingId: number
    clipStart: number
    clipEnd: number
    clipCategory: string
    clipFilename?: string
    clipFilePath?: string
    clipRemark?: string
    aiModel?: string
    segments: Array<{
      segmentIndex: number
      startTime: string
      endTime: string
      text: string
    }>
  }): Promise<{ taskId: number; status: string } | null> {
    if (!this.authToken) return null
    try {
      const resp = await this.call<any>('post', '/analysis/submit-clip-asr', data, { timeout: 30000 })
      return resp?.data || resp || null
    } catch (err) {
      console.error('[BackendApi] submitClipAsrResult failed:', (err as Error).message)
      return null
    }
  }

  /**
   * Query orphaned recordings (status="recording") that were interrupted by app restart.
   */
  async getOrphanedRecordings(): Promise<Array<{ id: number; localFilePath: string | null; localFileName: string | null; sessionId: string | null }>> {
    if (!this.authToken) return []
    try {
      const resp = await this.call<any>('get', '/recordings', { status: 'recording', size: 100 })
      const items = resp?.items || resp?.data?.items || []
      return items.map((item: any) => ({
        id: item.id,
        localFilePath: item.localFilePath || null,
        localFileName: item.localFileName || null,
        sessionId: item.sessionId || null,
      }))
    } catch (err: any) {
      const msg = err?.response?.status
        ? `HTTP ${err.response.status}: ${err.response.data?.message || err.message}`
        : (err?.message || String(err))
      console.error('[BackendApi] getOrphanedRecordings failed:', msg)
      return []
    }
  }

  /** Expose the current auth token (null when logged out). Read by background workers. */
  getAuthToken(): string | null {
    return this.authToken
  }

  async getStreamer(id: number): Promise<{
    id: number
    anchorName: string
    accountType?: string
    industryId?: number
    cloudSyncEnabled: boolean
  } | null> {
    if (!this.authToken) return null
    try {
      const resp = await this.call<any>('get', `/streamers/${id}`)
      const data = resp?.data || resp
      return data ? {
        id: data.id,
        anchorName: data.anchorName || '',
        accountType: data.accountType,
        industryId: data.industryId,
        cloudSyncEnabled: data.cloudSyncEnabled === true || data.cloudSyncEnabled === 1,
      } : null
    } catch (err) {
      console.error('[BackendApi] getStreamer failed:', (err as Error).message)
      return null
    }
  }

  async initCloudUpload(data: {
    fileName: string
    fileSize: number
    contentType?: string
    businessType: string
    businessId?: number
    recordingId?: number
    clipId?: number
    comparisonId?: number
    streamerId?: number
    anchorName?: string
    industryId?: number
    accountType?: string
    uploadAccount?: string
    recordedAt?: string
    durationSeconds?: number
    manualUpload?: boolean
    localFilePath?: string
    clientTaskId?: string
  }): Promise<{
    uploadId: number
    fileId: number
    bucket: string
    storageKey: string
    uploadUrl: string
    uploadMethod: 'PUT'
  }> {
    const resp = await this.call<any>('post', '/cloud-space/uploads/init', {
      ...data,
      recordedAt: normalizeBackendDateTime(data.recordedAt),
    }, { timeout: 30000 })
    return resp?.data || resp
  }

  async updateCloudUploadProgress(uploadId: number, progress: number): Promise<boolean> {
    if (!this.authToken) return false
    try {
      await this.call<any>('post', `/cloud-space/uploads/${uploadId}/progress`, { progress })
      return true
    } catch (err) {
      console.warn('[BackendApi] updateCloudUploadProgress failed:', (err as Error).message)
      return false
    }
  }

  async completeCloudUpload(uploadId: number, data: { checksum?: string; fileSize?: number } = {}): Promise<boolean> {
    if (!this.authToken) return false
    try {
      await this.call<any>('post', `/cloud-space/uploads/${uploadId}/complete`, data, { timeout: 30000 })
      return true
    } catch (err) {
      console.error('[BackendApi] completeCloudUpload failed:', (err as Error).message)
      return false
    }
  }

  async failCloudUpload(uploadId: number, errorMessage: string): Promise<boolean> {
    if (!this.authToken) return false
    try {
      await this.call<any>('post', `/cloud-space/uploads/${uploadId}/fail`, { errorMessage })
      return true
    } catch (err) {
      console.warn('[BackendApi] failCloudUpload failed:', (err as Error).message)
      return false
    }
  }

  /**
   * Long-poll the backend for the next Feishu task dispatched to this user.
   * Returns null on timeout/empty, the task on hit.
   * The HTTP timeout must exceed the server-side wait window.
   */
  async pollFeishuTask(waitSec: number = 25): Promise<{
    taskId: string
    type: string
    payload: Record<string, unknown>
  } | null> {
    if (!this.authToken) return null
    try {
      const resp = await this.call<any>('get', '/feishu/tasks/next',
        { wait: waitSec },
        { timeout: (waitSec + 10) * 1000 })
      const data = resp?.data || resp
      if (!data || !data.taskId) return null
      return {
        taskId: String(data.taskId),
        type: String(data.type || ''),
        payload: (data.payload || {}) as Record<string, unknown>,
      }
    } catch (err: any) {
      // 401 走到这里说明 refresh 也失败了（refresh token 真过期），抛 unauthorized 让 worker backoff
      if (err?.response?.status === 401) {
        throw new Error('unauthorized')
      }
      throw err
    }
  }

  /** Deliver a Feishu task result back to the backend. */
  async deliverFeishuTaskResult(
    taskId: string,
    body: { ok: boolean; data?: Record<string, unknown>; error?: string }
  ): Promise<boolean> {
    if (!this.authToken) return false
    try {
      await this.call<any>('post', `/feishu/tasks/${encodeURIComponent(taskId)}/result`, body, { timeout: 10000 })
      return true
    } catch (err) {
      console.error('[BackendApi] deliverFeishuTaskResult failed:', (err as Error).message)
      return false
    }
  }

  /**
   * Fetch all streamers from the backend.
   * Used by MonitorService as the single source for live-state polling and auto-record flags.
   */
  async listAllStreamers(): Promise<Array<{ id: number; platform: string; accountId: string | null; anchorName: string; isMonitoring: boolean; cloudSyncEnabled: boolean }>> {
    if (!this.authToken) return []
    try {
      const resp = await this.call<any>('get', '/streamers', { size: 500 })
      const items = resp?.items || resp?.data?.items || []
      return items.map((item: any) => ({
        id: item.id,
        platform: item.platform || '',
        accountId: item.accountId || null,
        anchorName: item.anchorName || '',
        isMonitoring: item.isMonitoring === true || item.isMonitoring === 1,
        cloudSyncEnabled: item.cloudSyncEnabled === true || item.cloudSyncEnabled === 1,
      }))
    } catch (err: any) {
      const msg = err?.response?.status
        ? `HTTP ${err.response.status}: ${err.response.data?.message || err.message}`
        : (err?.message || String(err))
      console.error('[BackendApi] listAllStreamers failed:', msg)
      return []
    }
  }

  /**
   * Fetch all streamers with auto-recording enabled from the backend.
   * Kept for compatibility with older call sites.
   */
  async listMonitoringStreamers(): Promise<Array<{ id: number; platform: string; accountId: string | null; anchorName: string }>> {
    const streamers = await this.listAllStreamers()
    return streamers
      .filter((item) => item.isMonitoring)
      .map(({ id, platform, accountId, anchorName }) => ({ id, platform, accountId, anchorName }))
  }
}

function normalizeBackendDateTime(value?: string): string | undefined {
  if (!value) return undefined
  const trimmed = value.trim()
  if (!trimmed) return undefined

  const direct = trimmed.match(/^(\d{4}-\d{2}-\d{2})[T\s](\d{2}:\d{2}:\d{2})/)
  if (direct) return `${direct[1]} ${direct[2]}`
  if (/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) return `${trimmed} 00:00:00`

  const parsed = new Date(trimmed)
  if (Number.isNaN(parsed.getTime())) return trimmed.replace('T', ' ')

  const pad = (num: number) => String(num).padStart(2, '0')
  return [
    parsed.getFullYear(),
    pad(parsed.getMonth() + 1),
    pad(parsed.getDate()),
  ].join('-') + ' ' + [
    pad(parsed.getHours()),
    pad(parsed.getMinutes()),
    pad(parsed.getSeconds()),
  ].join(':')
}

export const backendApi = new BackendApi()
