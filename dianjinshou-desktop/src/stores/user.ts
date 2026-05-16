import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as authApi from '@/api/auth'
import type { UserInfo } from '@/api/auth'
import router from '@/router'

export type { UserInfo }

export const useUserStore = defineStore('user', () => {
  const accessToken = ref<string>(localStorage.getItem('accessToken') || '')
  const refreshToken = ref<string>(localStorage.getItem('refreshToken') || '')
  const user = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!accessToken.value)

  function syncTokensToMain(access = accessToken.value, refresh = refreshToken.value) {
    const api = typeof window !== 'undefined' ? (window as any).electronAPI : null
    if (api?.setAuthTokens && access && refresh) {
      api.setAuthTokens(access, refresh).catch(() => {})
    } else if (api?.setAuthToken && access) {
      api.setAuthToken(access).catch(() => {})
    }
  }

  if (accessToken.value) {
    syncTokensToMain()
  }

  function setTokens(access: string, refresh: string) {
    accessToken.value = access
    refreshToken.value = refresh
    localStorage.setItem('accessToken', access)
    localStorage.setItem('refreshToken', refresh)
    // 同步两个 token 给主进程：主进程需要 refresh token 才能在 access 过期时自己续期
    syncTokensToMain(access, refresh)
  }

  function setUser(info: UserInfo) {
    user.value = info
  }

  async function login(params: authApi.LoginParams) {
    const res = await authApi.login(params)
    const data = (res as any).data ?? res
    setTokens(data.accessToken, data.refreshToken)
    setUser(data.user)
    // Remember me: persist phone
    if (params.rememberMe) {
      localStorage.setItem('rememberedPhone', params.phone)
    } else {
      localStorage.removeItem('rememberedPhone')
    }
    router.push('/')
  }

  async function smsLogin(params: authApi.SmsLoginParams) {
    const res = await authApi.smsLogin(params)
    const data = (res as any).data ?? res
    setTokens(data.accessToken, data.refreshToken)
    setUser(data.user)
    router.push('/')
  }

  async function register(params: authApi.RegisterParams) {
    const res = await authApi.register(params)
    const data = (res as any).data ?? res
    setTokens(data.accessToken, data.refreshToken)
    setUser(data.user)
    router.push('/')
  }

  function logout(silent = false) {
    accessToken.value = ''
    refreshToken.value = ''
    user.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    if (!silent) {
      router.push('/login')
    }
  }

  /** 本地解码 JWT 的 exp，看 token 自己是否已过期（容忍 60s clock skew）*/
  function jwtExpiredLocally(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
      if (!payload.exp) return true
      return payload.exp * 1000 < Date.now() - 60_000
    } catch {
      return true // 解不开的 token 当作过期
    }
  }

  async function refreshAccessToken(): Promise<string | null> {
    if (!refreshToken.value) {
      logout()
      return null
    }

    // 带一次内部重试，应对后端偶发"误判过期"
    for (let attempt = 1; attempt <= 2; attempt++) {
      try {
        const res = await authApi.refreshToken(refreshToken.value)
        const data = (res as any).data ?? res
        setTokens(data.accessToken, data.refreshToken)
        return data.accessToken
      } catch (err: any) {
        const status = err?.response?.status
        const bizCode = err?.code ?? err?.response?.data?.code
        const isExpiredResp = status === 401 || bizCode === 40101 || bizCode === 401

        // 只有"后端说过期"且"本地解码也确实过期"才 logout；
        // 本地 token 还在有效期的话 → 大概率后端误判，保留登录、返回 null 让调用方报错重试
        if (isExpiredResp) {
          const reallyExpired = jwtExpiredLocally(refreshToken.value)
          if (reallyExpired) {
            logout()
            return null
          }
          if (attempt === 1) {
            console.warn('[auth] refresh 返回 40101 但本地 token 还在有效期，500ms 后重试')
            await new Promise(r => setTimeout(r, 500))
            continue
          }
          // 重试后仍失败，但 token 本地看起来没过期 —— 不 logout，保留登录状态
          console.warn('[auth] refresh 连续 2 次失败但本地 token 未过期，暂不清登录')
          return null
        }

        // 网络错误 / 5xx 等：也保留 token 让下次请求重试
        return null
      }
    }
    return null
  }

  async function fetchMe() {
    try {
      const res = await authApi.getMe()
      const data = (res as any).data ?? res
      setUser(data.user)
    } catch {
      // ignore
    }
  }

  return {
    accessToken,
    refreshToken,
    user,
    isLoggedIn,
    setTokens,
    setUser,
    login,
    smsLogin,
    register,
    logout,
    refreshAccessToken,
    fetchMe,
    syncTokensToMain
  }
})
