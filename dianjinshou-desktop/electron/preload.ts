import { contextBridge, ipcRenderer } from 'electron'

const electronAPI = {
  minimizeWindow: () => ipcRenderer.invoke('window:minimize'),
  maximizeWindow: () => ipcRenderer.invoke('window:maximize'),
  closeWindow: () => ipcRenderer.invoke('window:close'),
  isMaximized: () => ipcRenderer.invoke('window:isMaximized') as Promise<boolean>,

  // Recording
  startRecording: (config: {
    streamerId: string
    streamUrl: string
    anchorName: string
    options?: {
      resolution?: '480p' | '720p' | '1080p' | 'source'
      segmentDuration?: number
      outputDir?: string
    }
  }) => ipcRenderer.invoke('ipc:recording:start', config),
  stopRecording: (recordingId: string) =>
    ipcRenderer.invoke('ipc:recording:stop', recordingId),
  getRecordingStatus: (recordingId: string) =>
    ipcRenderer.invoke('ipc:recording:getStatus', recordingId),
  getAllRecordings: () => ipcRenderer.invoke('ipc:recording:getAll'),

  // Storage & File operations
  getDiskUsage: () => ipcRenderer.invoke('ipc:storage:getDiskUsage'),
  getRecordingsPath: () => ipcRenderer.invoke('ipc:storage:getRecordingsPath'),
  setRecordingsPath: (newPath: string) => ipcRenderer.invoke('ipc:storage:setRecordingsPath', newPath),
  openFolder: (path: string) => ipcRenderer.invoke('ipc:storage:openFolder', path),
  showItemInFolder: (filePath: string) => ipcRenderer.invoke('ipc:storage:showItemInFolder', filePath),
  openFile: (filePath: string) => ipcRenderer.invoke('ipc:storage:openFile', filePath),
  selectDirectory: () => ipcRenderer.invoke('dialog:selectDirectory'),

  // Monitor
  startMonitoring: () => ipcRenderer.invoke('ipc:monitor:start'),
  stopMonitoring: () => ipcRenderer.invoke('ipc:monitor:stop'),
  startLiveStatusPolling: () => ipcRenderer.invoke('ipc:monitor:startStatusPolling'),
  stopLiveStatusPolling: () => ipcRenderer.invoke('ipc:monitor:stopStatusPolling'),
  pollLiveStatusNow: () => ipcRenderer.invoke('ipc:monitor:pollNow'),
  addMonitoredStreamer: (config: {
    streamerId: string
    platform: string
    roomId: string
    roomUrl: string
    anchorName: string
    resolution: '480p' | '720p' | '1080p' | 'source'
    segmentDuration: number
    autoRecord?: boolean
  }) => ipcRenderer.invoke('ipc:monitor:addStreamer', config),
  removeMonitoredStreamer: (streamerId: string) =>
    ipcRenderer.invoke('ipc:monitor:removeStreamer', streamerId),
  getMonitorStatus: () => ipcRenderer.invoke('ipc:monitor:getStatus'),
  setMonitorInterval: (seconds: number) => ipcRenderer.invoke('ipc:monitor:setInterval', seconds),

  // Queue
  getQueueStatus: () => ipcRenderer.invoke('ipc:recording:getQueueStatus'),
  getCloudUploadQueue: () => ipcRenderer.invoke('ipc:cloud-upload:getQueue'),
  enqueueCloudUpload: (item: any) => ipcRenderer.invoke('ipc:cloud-upload:enqueue', item),
  enqueueGeneratedCloudUpload: (payload: any) => ipcRenderer.invoke('ipc:cloud-upload:enqueueGenerated', payload),
  downloadCloudFile: (payload: { url: string; fileName?: string }) =>
    ipcRenderer.invoke('ipc:cloud-download:file', payload),

  // Douyin
  resolveDouyinRoom: (input: string) => ipcRenderer.invoke('ipc:douyin:resolve', input),
  resolveKuaishouRoom: (input: string) => ipcRenderer.invoke('ipc:kuaishou:resolve', input),

  // Auth token sync (for main process to call backend API)
  setAuthToken: (token: string) => ipcRenderer.invoke('ipc:auth:setToken', token),
  setAuthTokens: (access: string, refresh: string) =>
    ipcRenderer.invoke('ipc:auth:setTokens', { access, refresh }),
  // 主进程自己 refresh 后把新 token 推回来，渲染进程需要更新 store/localStorage
  onAuthTokenRefreshed: (cb: (payload: { accessToken: string; refreshToken: string }) => void) => {
    const listener = (_e: unknown, payload: any) => cb(payload)
    ipcRenderer.on('auth:token-refreshed', listener)
    return () => ipcRenderer.removeListener('auth:token-refreshed', listener)
  },

  // App info
  getAppVersion: () => ipcRenderer.invoke('ipc:app:version') as Promise<string>,

  // Auto updater
  checkForUpdate: () => ipcRenderer.invoke('ipc:update:check'),
  downloadUpdate: () => ipcRenderer.invoke('ipc:update:download'),
  installUpdate: () => ipcRenderer.invoke('ipc:update:install'),
  onUpdateStatus: (cb: (payload: any) => void) => {
    const listener = (_e: unknown, payload: any) => cb(payload)
    ipcRenderer.on('update:status', listener)
    return () => ipcRenderer.removeListener('update:status', listener)
  },
  onUpdateProgress: (cb: (payload: any) => void) => {
    const listener = (_e: unknown, payload: any) => cb(payload)
    ipcRenderer.on('update:progress', listener)
    return () => ipcRenderer.removeListener('update:progress', listener)
  },

  // Local ASR status. Provider/download APIs are kept as compatibility no-ops.
  getAsrProvider: () => ipcRenderer.invoke('ipc:asr:getProvider'),
  setAsrProviderIpc: (provider: 'local') =>
    ipcRenderer.invoke('ipc:asr:setProvider', provider),
  getAsrModelStatus: () => ipcRenderer.invoke('ipc:asr:getModelStatus'),
  downloadAsrModel: () => ipcRenderer.invoke('ipc:asr:downloadModel'),
  removeAsrModel: () => ipcRenderer.invoke('ipc:asr:removeModel'),
  onAsrModelStatus: (cb: (payload: any) => void) => {
    const listener = (_e: unknown, payload: any) => cb(payload)
    ipcRenderer.on('asr:model-status', listener)
    return () => ipcRenderer.removeListener('asr:model-status', listener)
  },

  // ASR — run transcription on a local file, returns segments
  ensureMp4: (filePath: string) =>
    ipcRenderer.invoke('ipc:video:ensureMp4', filePath) as Promise<{
      success: boolean
      path?: string
      converted?: boolean
      error?: string
    }>,
  runAsr: (filePath: string) =>
    ipcRenderer.invoke('ipc:asr:run', filePath) as Promise<{
      success: boolean
      data?: Array<{ segmentIndex: number; startTime: string; endTime: string; text: string }>
      error?: string
    }>,

  // Clip extraction
  extractClip: (config: { sourcePath: string; clipStart: number; clipEnd: number; clipFilename?: string }) =>
    ipcRenderer.invoke('ipc:clip:extract', config) as Promise<{ success: boolean; clipPath?: string; error?: string }>,

  // Local video playback — use local HTTP server for proper Range/seeking support
  getLocalVideoUrl: async (filePath: string) => {
    try {
      const port = await ipcRenderer.invoke('video:getServerPort')
      if (port > 0) {
        return `http://127.0.0.1:${port}/video?path=${encodeURIComponent(filePath)}`
      }
    } catch { /* fallback */ }
    return `local-video://video?path=${encodeURIComponent(filePath)}`
  },
}

contextBridge.exposeInMainWorld('electronAPI', electronAPI)
