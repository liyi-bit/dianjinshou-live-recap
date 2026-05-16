import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '../user'

// Mock auth API
vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  smsLogin: vi.fn(),
  register: vi.fn(),
  logout: vi.fn().mockResolvedValue(undefined),
  refreshToken: vi.fn(),
  getMe: vi.fn()
}))

// Mock router
vi.mock('@/router', () => ({
  default: { push: vi.fn() }
}))

import * as authApi from '@/api/auth'
import router from '@/router'

describe('user store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('isLoggedIn is false when no token', () => {
    const store = useUserStore()
    expect(store.isLoggedIn).toBe(false)
  })

  it('login sets tokens and user, navigates to /', async () => {
    const mockResponse = {
      data: {
        accessToken: 'at_123',
        refreshToken: 'rt_123',
        user: { id: 1, username: 'test', role: 'operator' }
      }
    }
    vi.mocked(authApi.login).mockResolvedValue(mockResponse as any)

    const store = useUserStore()
    await store.login({ phone: '13800138000', password: 'pass123', rememberMe: true })

    expect(store.accessToken).toBe('at_123')
    expect(store.refreshToken).toBe('rt_123')
    expect(store.user).toEqual({ id: 1, username: 'test', role: 'operator' })
    expect(store.isLoggedIn).toBe(true)
    expect(localStorage.getItem('accessToken')).toBe('at_123')
    expect(localStorage.getItem('rememberedPhone')).toBe('13800138000')
    expect(router.push).toHaveBeenCalledWith('/')
  })

  it('logout clears tokens and navigates to /login', () => {
    const store = useUserStore()
    store.setTokens('at', 'rt')
    store.setUser({ id: 1, username: 'test' } as any)

    store.logout()

    expect(store.accessToken).toBe('')
    expect(store.refreshToken).toBe('')
    expect(store.user).toBeNull()
    expect(store.isLoggedIn).toBe(false)
    expect(localStorage.getItem('accessToken')).toBeNull()
    expect(router.push).toHaveBeenCalledWith('/login')
  })

  it('refreshAccessToken updates tokens', async () => {
    const mockResponse = {
      data: {
        accessToken: 'new_at',
        refreshToken: 'new_rt'
      }
    }
    vi.mocked(authApi.refreshToken).mockResolvedValue(mockResponse as any)

    const store = useUserStore()
    store.setTokens('old_at', 'old_rt')

    const result = await store.refreshAccessToken()

    expect(result).toBe('new_at')
    expect(store.accessToken).toBe('new_at')
    expect(store.refreshToken).toBe('new_rt')
  })

  it('refreshAccessToken with no token calls logout', async () => {
    const store = useUserStore()
    const result = await store.refreshAccessToken()
    expect(result).toBeNull()
    expect(router.push).toHaveBeenCalledWith('/login')
  })

  it('refreshAccessToken failure calls logout', async () => {
    vi.mocked(authApi.refreshToken).mockRejectedValue(new Error('fail'))

    const store = useUserStore()
    store.setTokens('at', 'rt')

    const result = await store.refreshAccessToken()

    expect(result).toBeNull()
    expect(store.accessToken).toBe('')
  })

  it('register sets tokens and navigates', async () => {
    const mockResponse = {
      data: {
        accessToken: 'reg_at',
        refreshToken: 'reg_rt',
        user: { id: 2, username: 'newuser' }
      }
    }
    vi.mocked(authApi.register).mockResolvedValue(mockResponse as any)

    const store = useUserStore()
    await store.register({ username: 'newuser', phone: '13900139000', password: 'pass', code: '123456' })

    expect(store.accessToken).toBe('reg_at')
    expect(store.user?.username).toBe('newuser')
    expect(router.push).toHaveBeenCalledWith('/')
  })
})
