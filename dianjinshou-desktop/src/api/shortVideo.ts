import request from './request'

export interface VideoCopywritingItem {
  id: number
  sourceType: string
  sourceUrl: string | null
  title: string | null
  extractedText: string | null
  polishedText: string | null
  wordCount: number
  tags: string | null
  status: string
  errorMsg: string | null
  copyCount: number
  createdAt: string
}

export interface CreatorItem {
  id: number
  platform: string
  creatorId: string
  nickname: string
  avatarUrl: string | null
  followerCount: number
  videoCount: number
  industry: string | null
  description: string | null
}

export interface CreatorVideoItem {
  id: number
  title: string
  coverUrl: string | null
  playCount: number
  likeCount: number
  commentCount: number
  shareCount: number
  duration: number
  url: string | null
}

export interface CreatorDetailItem extends CreatorItem {
  recentVideos: CreatorVideoItem[]
}

// Copywriting extract
export function extractCopywriting(params: { sourceType: string; sourceUrl?: string; storageKey?: string; title?: string }) {
  return request.post<VideoCopywritingItem>('/short-video/extract-copywriting', params)
}

export function listCopywriting(page: number, size: number, status?: string) {
  return request.get<any>('/short-video/copywriting', { params: { page, size, status } })
}

export function getCopywriting(id: number) {
  return request.get<VideoCopywritingItem>(`/short-video/copywriting/${id}`)
}

export function recordCopy(id: number) {
  return request.post(`/short-video/copywriting/${id}/copy`)
}

export function deleteCopywriting(id: number) {
  return request.delete(`/short-video/copywriting/${id}`)
}

// Creator search
export function searchCreators(params: {
  keyword?: string; platform?: string; industry?: string;
  minFollowers?: number; maxFollowers?: number; page?: number; size?: number
}) {
  return request.get<CreatorItem[]>('/short-video/creators/search', { params })
}

export function getCreatorDetail(id: number) {
  return request.get<CreatorDetailItem>(`/short-video/creators/${id}`)
}

// Subscriptions
export function subscribeCreator(creatorId: number) {
  return request.post('/short-video/subscriptions/creator', { creatorId })
}

export function subscribeTrending(params: {
  platform?: string; industry?: string; minPlayCount?: number; minLikeCount?: number; keywords?: string
}) {
  return request.post('/short-video/subscriptions/trending', params)
}

export function listSubscriptions() {
  return request.get<any>('/short-video/subscriptions')
}

export function cancelSubscription(id: number, type: string) {
  return request.delete(`/short-video/subscriptions/${id}`, { params: { type } })
}

// Alerts
export function listAlerts(page: number, size: number) {
  return request.get<any>('/short-video/alerts', { params: { page, size } })
}

export function markAlertRead(id: number) {
  return request.put(`/short-video/alerts/${id}/read`)
}
