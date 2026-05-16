import request from './request'

// --- Types ---

export interface OptimizedTextResult {
  data: string
}

export interface AnalysisTask {
  id: number
  recordingId: number
  type: 'full' | 'clip'
  status: 'pending' | 'asr_processing' | 'recording' | 'transcribing' | 'transcribed' | 'ai_processing' | 'completed' | 'failed'
  aiModel: string
  industry: string | null
  clipStart: number | null
  clipEnd: number | null
  clipCategory: string | null
  clipFilename: string | null
  clipFilePath: string | null
  clipRemark: string | null
  asrWordCount: number
  aiResult: string | null
  aiDiagnosis: string | null
  keywordSummary: string | null
  sensitiveWords: string | null
  sensitiveCount: number
  contentCompass: string | null
  optimizedText: string | null
  optimizationAction: string | null
  optimizationGoal: string | null
  summary: string | null
  consumedChars: number
  errorMsg: string | null
  startedAt: string | null
  completedAt: string | null
  createdAt: string
}

export interface AnalysisTaskCreate {
  taskId: number
  status: string
}

export interface AsrParagraph {
  id: number
  paragraphIndex: number
  startTime: string
  endTime: string | null
  naturalTime: string | null
  textContent: string
  wordCount: number
  wordsPerMin: number
  onlineCount: number | null
  barrageCount: number | null
  transactionCount: number | null
  interactionRate: number | null
  transactionRate: number | null
  salesAmount: number | null
  uvValue: number | null
  speakerId: string | null
  scriptCategory: string | null
  isHighlighted: number
}

export interface KeywordItem {
  id: number
  type: string
  category: string | null
  subCategory: string | null
  word: string
  hitCountVideo1: number
  hitCountVideo2: number
  totalCount: number
  source: string
  sceneDesc: string | null
  riskLevel: number | null
  sentenceRefs: string | null
}

export interface KeywordList {
  items: KeywordItem[]
  stats: { totalOperational: number; totalSensitive: number }
  total: number
}

export interface NoteData {
  id: number | null
  tabType: string
  contentHtml: string
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
}

// --- API functions ---

export function listAnalysisTasks(params: {
  type?: string
  status?: string
  streamerId?: number
  clipCategory?: string
  page?: number
  size?: number
}) {
  return request.get<PageResult<AnalysisTask>>('/analysis/list', { params })
}

export function createFullAnalysis(data: { recordingId: number; industry?: string; aiModel?: string }) {
  return request.post<AnalysisTaskCreate>('/analysis/full', data)
}

export function createClipAnalysis(data: {
  recordingId: number
  clipStart: number
  clipEnd: number
  clipCategory: string
  clipFilename?: string
  clipFilePath?: string
  clipRemark?: string
  aiModel?: string
}) {
  return request.post<AnalysisTaskCreate>('/analysis/clip', data)
}

export function submitAsrResult(data: {
  recordingId: number
  segments: Array<{ segmentIndex: number; startTime: string; endTime: string; text: string }>
  industry?: string
  aiModel?: string
}) {
  return request.post<AnalysisTaskCreate>('/analysis/submit-asr', data)
}

export function submitClipAsrResult(data: {
  /** 可选 — 提供后走 update 模式（前端 draft → 后台 ASR 的链路）*/
  taskId?: number
  recordingId: number
  clipStart: number
  clipEnd: number
  clipCategory: string
  clipFilename?: string
  clipFilePath?: string
  clipRemark?: string
  aiModel?: string
  segments: Array<{ segmentIndex: number; startTime: string; endTime: string; text: string }>
}) {
  return request.post<AnalysisTaskCreate>('/analysis/submit-clip-asr', data)
}

/** 切片占位：立即返回 taskId（status=transcribing），列表里马上能看到一条"逐字稿生成中" */
export function createClipDraft(data: {
  recordingId: number
  clipStart: number
  clipEnd: number
  clipCategory: string
  clipFilename?: string
  clipRemark?: string
  aiModel?: string
}) {
  return request.post<AnalysisTaskCreate>('/analysis/clip/draft', data)
}

export function getAnalysis(id: number) {
  return request.get<AnalysisTask>(`/analysis/${id}`)
}

export function getParagraphs(id: number, page = 1, size = 100) {
  return request.get<PageResult<AsrParagraph>>(`/analysis/${id}/paragraphs`, {
    params: { page, size }
  })
}

export function getKeywords(id: number, params?: { type?: string; category?: string; page?: number; size?: number }) {
  return request.get<KeywordList>(`/analysis/${id}/keywords`, { params })
}

export function getDiagnosis(id: number) {
  return request.get<AnalysisTask>(`/analysis/${id}/diagnosis`)
}

export function saveOptimization(id: number, data: { action: string; goal?: string }) {
  return request.post(`/analysis/${id}/optimization`, data)
}

export function getNotes(id: number, tabType: string) {
  return request.get<NoteData>(`/analysis/${id}/notes`, { params: { tabType } })
}

export function saveNotes(id: number, data: { tabType: string; contentHtml: string }) {
  return request.put<NoteData>(`/analysis/${id}/notes`, data)
}

/**
 * v1.1.0：用户在逐字稿详情页手动触发 AI 复盘（消耗一次每日额度）。
 * 超限时后端返回 code=40006 (DAILY_QUOTA_EXHAUSTED)，前端拦截该 code 弹提示。
 */
export function startAiAnalysis(id: number) {
  return request.post<AnalysisTaskCreate>(`/analysis/${id}/start-ai`)
}

export interface DailyQuotaStatus {
  used: number
  limit: number
  unlimited: boolean
  resetAt: string
}
/** 查询当前用户今日 AI 复盘额度。 */
export function getDailyQuota() {
  return request.get<DailyQuotaStatus>('/quota/daily')
}

export function reAnalyze(id: number) {
  return request.post<AnalysisTaskCreate>(`/analysis/${id}/re-analyze`)
}

export function cancelAnalysis(id: number) {
  return request.post(`/analysis/${id}/cancel`)
}

export function batchDeleteAnalysis(ids: number[]) {
  return request.delete('/analysis', { data: { ids } })
}

export function generateOptimizedText(id: number) {
  return request.post<string>(`/analysis/${id}/optimized-text`)
}

export function classifyParagraphs(id: number) {
  return request.post(`/analysis/${id}/classify-paragraphs`)
}
