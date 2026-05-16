import request from './request'

export interface ThirdPartyField {
  key: string
  label: string
  type: 'text' | 'secret'
  value: string
  masked: boolean
  placeholder: string
}

export interface ThirdPartyGroup {
  key: string
  label: string
  fields: ThirdPartyField[]
}

export interface ThirdPartyView {
  groups: ThirdPartyGroup[]
}

export function getAdminConfig() {
  return request.get<ThirdPartyView>('/admin/config') as unknown as Promise<ThirdPartyView>
}

export function updateAdminConfig(payload: Record<string, string>) {
  return request.put('/admin/config', payload)
}

export interface QuotaStatus {
  used: number
  limit: number
  hasOwnConfig: boolean
}

/** 查询当前用户的默认密钥免费配额（用完后需配置自己的密钥才能继续用 AI）。 */
export function getAdminQuota() {
  return request.get<QuotaStatus>('/admin/config/quota') as unknown as Promise<QuotaStatus>
}
