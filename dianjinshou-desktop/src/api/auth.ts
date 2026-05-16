import request from './request'

export interface LoginParams {
  phone: string
  password: string
  rememberMe?: boolean
}

export interface SmsLoginParams {
  phone: string
  code: string
}

export interface RegisterParams {
  username: string
  phone: string
  password: string
  code: string
}

export interface SmsSendParams {
  phone: string
  type: 'login' | 'register'
}

export interface UserInfo {
  id: number
  username: string
  phone: string
  avatarUrl: string | null
  role: string
  orgId: number | null
  vipLevel: number
  vipExpireAt: string | null
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: UserInfo
}

export interface RefreshResult {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

export interface SmsSendResult {
  success: boolean
  expireSeconds: number
}

export function login(data: LoginParams) {
  return request.post<LoginResult>('/auth/login', data)
}

export function smsLogin(data: SmsLoginParams) {
  return request.post<LoginResult>('/auth/login/sms', data)
}

export function register(data: RegisterParams) {
  return request.post<LoginResult>('/auth/register', data)
}

export function sendSms(data: SmsSendParams) {
  return request.post<SmsSendResult>('/auth/sms/send', data)
}

export function refreshToken(refreshTokenStr: string) {
  return request.post<RefreshResult>('/auth/refresh', { refreshToken: refreshTokenStr })
}

export function logout() {
  return request.post('/auth/logout')
}

export function getMe() {
  return request.get('/auth/me')
}
