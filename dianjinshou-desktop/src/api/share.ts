import request from './request'

export interface ShareLinkItem {
  id: number
  cloudFileId: number
  fileName: string
  shareCode: string
  shareUrl: string
  hasPassword: boolean
  expiresAt: string | null
  maxDownloads: number | null
  downloadCount: number
  viewCount: number
  status: string
  createdAt: string
}

export function createShareLink(fileId: number, password?: string, expireHours?: number, maxDownloads?: number) {
  return request.post<ShareLinkItem>(`/cloud/files/${fileId}/share`, {
    password,
    expireHours,
    maxDownloads
  })
}

export function listMyShares() {
  return request.get<ShareLinkItem[]>('/cloud/shares')
}

export function cancelShare(id: number) {
  return request.delete(`/cloud/shares/${id}`)
}
