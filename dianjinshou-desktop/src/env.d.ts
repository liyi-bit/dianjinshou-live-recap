/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export default component
}

interface RecordingStartConfig {
  streamerId: string
  streamUrl: string
  anchorName: string
  options?: {
    resolution?: '480p' | '720p' | '1080p' | 'source'
    segmentDuration?: number
    outputDir?: string
  }
}

interface RecordingStatusData {
  recordingId: string
  status: 'recording' | 'stopped' | 'error'
  startTime: number
  filePath: string | null
  segmentIndex: number
  error?: string
}

interface DiskUsageData {
  used: number
  total: number
  free: number
  percentUsed: number
}

interface MonitorConfigData {
  streamerId: string
  platform: string
  roomId: string
  roomUrl: string
  anchorName: string
  resolution: '480p' | '720p' | '1080p' | 'source'
  segmentDuration: number
  autoRecord?: boolean
}

interface MonitorStatusData {
  isRunning: boolean
  streamerCount: number
  liveCount: number
  statusPolling?: boolean
  streamers: Array<MonitorConfigData & { isLive: boolean; recordingId: string | null }>
}

interface QueueStatusData {
  active: number
  queued: number
  max: number
}

interface AsrModelStatusData {
  ready: boolean
  downloading: boolean
  percent: number
  downloadedBytes: number
  totalBytes: number
  lastError?: string
  modelDir: string
}

interface AsrSegmentData {
  segmentIndex: number
  startTime: string
  endTime: string
  text: string
}

interface UpdateStatusPayload {
  state: 'checking' | 'available' | 'not-available' | 'downloaded' | 'error'
  version?: string
  releaseDate?: string
  releaseNotes?: string | unknown
  message?: string
}
interface UpdateProgressPayload {
  percent: number
  bytesPerSecond: number
  transferred: number
  total: number
}

interface IPCResponse<T = unknown> {
  success: boolean
  data?: T
  error?: string
}

interface ElectronAPI {
  minimizeWindow: () => Promise<void>
  maximizeWindow: () => Promise<void>
  closeWindow: () => Promise<void>
  isMaximized: () => Promise<boolean>

  // Recording
  startRecording: (config: RecordingStartConfig) => Promise<IPCResponse<RecordingStatusData>>
  stopRecording: (recordingId: string) => Promise<IPCResponse<RecordingStatusData>>
  getRecordingStatus: (recordingId: string) => Promise<IPCResponse<RecordingStatusData>>
  getAllRecordings: () => Promise<IPCResponse<RecordingStatusData[]>>

  // Storage & File operations
  getDiskUsage: () => Promise<IPCResponse<DiskUsageData>>
  getRecordingsPath: () => Promise<IPCResponse<string>>
  setRecordingsPath: (newPath: string) => Promise<IPCResponse<string>>
  openFolder: (path: string) => Promise<IPCResponse>
  showItemInFolder: (filePath: string) => Promise<IPCResponse>
  openFile: (filePath: string) => Promise<IPCResponse>
  selectDirectory: () => Promise<IPCResponse<string>>

  // Clip extraction
  extractClip: (config: { sourcePath: string; clipStart: number; clipEnd: number; clipFilename?: string }) =>
    Promise<{ success: boolean; clipPath?: string; error?: string }>

  // Douyin
  resolveDouyinRoom: (input: string) => Promise<IPCResponse>
  resolveKuaishouRoom: (input: string) => Promise<IPCResponse>

  // Auth
  setAuthToken: (token: string) => Promise<IPCResponse>
  setAuthTokens: (access: string, refresh: string) => Promise<IPCResponse>
  onAuthTokenRefreshed: (cb: (payload: { accessToken: string; refreshToken: string }) => void) => () => void

  // App info
  getAppVersion: () => Promise<string>

  // Auto updater
  checkForUpdate: () => Promise<IPCResponse>
  downloadUpdate: () => Promise<IPCResponse>
  installUpdate: () => Promise<IPCResponse>
  onUpdateStatus: (cb: (payload: UpdateStatusPayload) => void) => () => void
  onUpdateProgress: (cb: (payload: UpdateProgressPayload) => void) => () => void

  // Local video
  getLocalVideoUrl: (filePath: string) => Promise<string>

  // Monitor
  startMonitoring: () => Promise<IPCResponse>
  stopMonitoring: () => Promise<IPCResponse>
  startLiveStatusPolling: () => Promise<IPCResponse>
  stopLiveStatusPolling: () => Promise<IPCResponse>
  pollLiveStatusNow: () => Promise<IPCResponse>
  addMonitoredStreamer: (config: MonitorConfigData) => Promise<IPCResponse>
  removeMonitoredStreamer: (streamerId: string) => Promise<IPCResponse>
  getMonitorStatus: () => Promise<IPCResponse<MonitorStatusData>>
  setMonitorInterval: (seconds: number) => Promise<IPCResponse>

  // Queue
  getQueueStatus: () => Promise<IPCResponse<QueueStatusData>>
  getCloudUploadQueue: () => Promise<IPCResponse<unknown[]>>
  enqueueCloudUpload: (item: {
    filePath: string
    businessType: 'full_recap' | 'clip_recap' | 'full_comparison' | 'clip_comparison'
    businessId?: number
    recordingId?: number
    clipId?: number
    comparisonId?: number
    streamerId?: number
    anchorName?: string
    industryId?: number
    accountType?: string
    uploadAccount?: string
    recordedAt?: string
    durationSeconds?: number
    manualUpload?: boolean
    contentType?: string
    fileName?: string
    id?: string
  }) => Promise<IPCResponse>
  enqueueGeneratedCloudUpload: (payload: {
    businessType: 'full_recap' | 'clip_recap' | 'full_comparison' | 'clip_comparison'
    businessId?: number
    recordingId?: number
    clipId?: number
    comparisonId?: number
    streamerId?: number
    anchorName?: string
    industryId?: number
    accountType?: string
    uploadAccount?: string
    recordedAt?: string
    durationSeconds?: number
    manualUpload?: boolean
    contentType?: string
    fileName?: string
    id?: string
    content?: unknown
  }) => Promise<IPCResponse>
  downloadCloudFile: (payload: { url: string; fileName?: string }) => Promise<IPCResponse<{ path: string }>>

  // ASR
  getAsrProvider: () => Promise<{ success: boolean; provider: 'local'; error?: string }>
  setAsrProviderIpc: (provider: 'local') => Promise<{ success: boolean; provider?: 'local'; error?: string }>
  getAsrModelStatus: () => Promise<IPCResponse<AsrModelStatusData>>
  downloadAsrModel: () => Promise<IPCResponse<AsrModelStatusData>>
  removeAsrModel: () => Promise<IPCResponse>
  onAsrModelStatus: (cb: (payload: AsrModelStatusData) => void) => () => void
  ensureMp4: (filePath: string) => Promise<{ success: boolean; path?: string; converted?: boolean; error?: string }>
  runAsr: (filePath: string) => Promise<{ success: boolean; data?: AsrSegmentData[]; error?: string }>
}

interface Window {
  electronAPI: ElectronAPI
}
