import request from './request'

export interface ClientVersionInfo {
  latestVersion: string
  minVersion: string
  downloadUrl: string
}

/**
 * 拉取服务端对客户端版本的要求。本地版本 < minVersion 时强制升级。
 * Public endpoint —— 不需要登录，启动时就可以调。
 */
export function getClientVersion() {
  return request.get<ClientVersionInfo>('/public/client-version')
}
