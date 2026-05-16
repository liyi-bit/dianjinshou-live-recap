import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import { Message } from '@arco-design/web-vue'
import { useUserStore } from '@/stores/user'
import { resolveErrorMessage } from '@/utils/errorMessages'
import { normalizeDateTime } from '@/utils/format'

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Flag to prevent multiple simultaneous refresh attempts
let isRefreshing = false
let failedQueue: Array<{
  resolve: (value: unknown) => void
  reject: (reason?: unknown) => void
}> = []

const ISO_DATE_TIME_RE = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/

function normalizeDateTimes<T>(value: T): T {
  if (typeof value === 'string') {
    return (ISO_DATE_TIME_RE.test(value) ? normalizeDateTime(value) : value) as T
  }
  if (Array.isArray(value)) {
    return value.map((item) => normalizeDateTimes(item)) as T
  }
  if (value && typeof value === 'object') {
    Object.keys(value as Record<string, unknown>).forEach((key) => {
      ;(value as Record<string, unknown>)[key] = normalizeDateTimes((value as Record<string, unknown>)[key])
    })
  }
  return value
}

function processQueue(error: unknown, token: string | null = null) {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error)
    } else {
      prom.resolve(token)
    }
  })
  failedQueue = []
}

// Request interceptor: inject JWT
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 以下路径不能带过期的 access token，否则会被后端 JwtAuthFilter 先拦下抛 40101：
    // - /auth/refresh : refresh 接口本身只读 body 里的 refreshToken
    // - /public/*     : public endpoint 不应该校验 token（比如启动时的 /public/client-version）
    const url = typeof config.url === 'string' ? config.url : ''
    if (url.includes('/auth/refresh') || url.includes('/public/')) {
      if (config.headers && 'Authorization' in config.headers) {
        delete (config.headers as any).Authorization
      }
      return config
    }
    const userStore = useUserStore()
    if (userStore.accessToken) {
      config.headers.Authorization = `Bearer ${userStore.accessToken}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Response interceptor: unwrap ApiResponse then handle 401 auto-refresh
request.interceptors.response.use(
  (response) => {
    // Unwrap backend ApiResponse: { code, message, data } → return data directly
    const body = normalizeDateTimes(response.data)
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 200) {
        return body.data
      }
      // 非 200 业务码是错误，不能当成功返回 —— 否则调用方会拿到没有 data 的壳对象
      const err: any = new Error(body.message || `业务错误 code=${body.code}`)
      err.code = body.code
      err.response = response
      return Promise.reject(err)
    }
    return response.data
  },
  async (error) => {
    const originalRequest = error.config
    const userStore = useUserStore()
    const url: string = typeof originalRequest?.url === 'string' ? originalRequest.url : ''
    const isRefreshUrl = url.includes('/auth/refresh')
    // 登录 / 注册 / 发短信等 auth 接口的 401 不是"token 过期"，是用户凭据错误或权限问题
    // 不能去触发 refresh（用户还没登录），必须直接报错给用户看
    const isAuthEntryUrl = url.includes('/auth/login') || url.includes('/auth/register') || url.includes('/auth/sms/send')

    // refresh 接口本身返回 401 = refresh token 已过期，不能再触发刷新流程（会死锁）
    // 直接清 token + 跳登录页
    if (error.response?.status === 401 && isRefreshUrl) {
      Message.error('登录已过期，请重新登录')
      userStore.logout()
      return Promise.reject(error)
    }

    // 登录类接口 401 = 凭据错误，直接显示后端消息（手机号或密码错误等）
    if (error.response?.status === 401 && isAuthEntryUrl) {
      Message.error(resolveErrorMessage(error))
      return Promise.reject(error)
    }

    // Show error message for all non-401 errors. 401 is handled below by the refresh flow.
    if (error.response?.status !== 401) {
      Message.error(resolveErrorMessage(error))
    }

    // If 401 and not already retried
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // Queue the request until token is refreshed
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then((token) => {
          originalRequest.headers.Authorization = `Bearer ${token}`
          return request(originalRequest)
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      const userStore = useUserStore()
      try {
        const newToken = await userStore.refreshAccessToken()
        if (newToken) {
          originalRequest.headers.Authorization = `Bearer ${newToken}`
          processQueue(null, newToken)
          return request(originalRequest)
        } else {
          // refreshAccessToken 内部已经根据错误类型决定是否 logout（真 401 才清）
          // 这里不再强制 logout，让网络错误 / 5xx 等临时故障的用户保留登录状态
          processQueue(new Error('Refresh failed'), null)
          return Promise.reject(error)
        }
      } catch (refreshError) {
        processQueue(refreshError, null)
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    // 401 but already retried — 到这里说明 refresh 也没救回来，通常是真过期了；
    // refreshAccessToken 内部应已 logout，这里不再重复


    return Promise.reject(error)
  }
)

export default request
