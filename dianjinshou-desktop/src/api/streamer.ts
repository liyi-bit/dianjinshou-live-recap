import request from './request'

// --- Types ---

export interface Streamer {
  id: number
  platform: 'douyin' | 'kuaishou'
  accountId: string
  anchorName: string
  anchorAvatar: string | null
  secUid: string | null
  accountType: 'own' | 'industry' | 'competitor'
  industryId: number | null
  industryName: string | null
  isMonitoring: boolean
  autoAiAnalysis: boolean
  cloudSyncEnabled: boolean
  totalSessions: number
  todaySessions: number
  lastLiveAt: string | null
}

export interface StreamerDetail extends Streamer {
  roomId: string | null
  roomUrl: string | null
  accountId: string
  liveRoomMode: string | null
  accountStage: string | null
  accountLevel: string | null
  trafficStructure: string | null
  broadcastTimeStart: string | null
  broadcastTimeEnd: string | null
  accountIssue: string | null
  defaultLanguage: string | null
}

export interface StreamerStats {
  total: number
  monitoring: number
  recording: number
  ownCount: number
  competitorCount: number
  industryCount: number
}

export interface IndustryTree {
  id: number
  name: string
  code: string
  children: IndustryTree[]
}

export interface CreateStreamerParams {
  platform: string
  accountId: string
  anchorName?: string
  anchorAvatar?: string
  secUid?: string
  industryId?: number
  accountType: string
  liveRoomMode?: string
  accountStage?: string
  accountLevel?: string
  trafficStructure?: string
  broadcastTimeStart?: string
  broadcastTimeEnd?: string
  accountIssue?: string
  defaultLanguage?: string
  autoAiAnalysis?: boolean
  cloudSyncEnabled?: boolean
}

export interface StreamerQuery {
  page: number
  size: number
  keyword?: string
  accountType?: string
  platform?: string
  /** true 只返回开启自动录制的主播 */
  isMonitoring?: boolean | number
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
}

// --- API functions ---

export function getStreamers(params: StreamerQuery) {
  return request.get<PageResult<Streamer>>('/streamers', { params })
}

export function createStreamer(data: CreateStreamerParams) {
  return request.post<Streamer>('/streamers', data)
}

export function getStreamer(id: number) {
  return request.get<StreamerDetail>(`/streamers/${id}`)
}

export function updateStreamer(id: number, data: Partial<CreateStreamerParams>) {
  return request.put<Streamer>(`/streamers/${id}`, data)
}

export function deleteStreamer(id: number) {
  return request.delete(`/streamers/${id}`)
}

export function startMonitor(id: number) {
  return request.post(`/streamers/${id}/monitor/start`)
}

export function stopMonitor(id: number) {
  return request.post(`/streamers/${id}/monitor/stop`)
}

export function getStreamerStats() {
  return request.get<StreamerStats>('/streamers/stats')
}

export function getIndustries() {
  return request.get<IndustryTree[]>('/industries')
}
