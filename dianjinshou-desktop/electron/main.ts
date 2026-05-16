import { app, BrowserWindow, dialog, ipcMain, net, protocol, shell } from 'electron'
import { join } from 'path'
import { is } from '@electron-toolkit/utils'
import { registerRecordingHandlers, getRecordingService, getMonitorService, getReconnectService } from './ipc/recording-handlers'
import { initMainErrorReporter, reportMainError } from './services/main-error-reporter'
import { feishuTaskWorker } from './services/feishu-task-worker'
import { setupAutoUpdater } from './services/auto-updater-service'
import { startVideoServer, getVideoServerPort } from './services/video-server'
import { getPunctModelManager } from './services/punct-model-manager'
import { getAsrModelManager } from './services/asr-model-manager'

// Dev-only: expose Chrome DevTools Protocol so the renderer DOM can be inspected externally.
if (!app.isPackaged) {
  app.commandLine.appendSwitch('remote-debugging-port', '9222')
}

// Prevent EPIPE crashes when stdout/stderr pipe is broken (e.g. parent process closed)
process.stdout?.on('error', () => {})
process.stderr?.on('error', () => {})

let mainWindow: BrowserWindow | null = null

function getWindowIconPath(): string | undefined {
  if (process.platform === 'darwin') return undefined
  const iconFile = process.platform === 'win32' ? 'icon.ico' : 'icon.png'
  if (app.isPackaged) return join(process.resourcesPath, iconFile)
  const devIconFile = process.platform === 'win32' ? 'app-icon.ico' : 'app-icon.png'
  return join(app.getAppPath(), 'resources', 'icons', devIconFile)
}

function createWindow(): void {
  mainWindow = new BrowserWindow({
    width: 1440,
    height: 900,
    minWidth: 1200,
    minHeight: 800,
    icon: getWindowIconPath(),
    show: false,
    frame: false, // frameless on Windows
    titleBarStyle: process.platform === 'darwin' ? 'hidden' : undefined,
    ...(process.platform === 'darwin'
      ? { trafficLightPosition: { x: 15, y: 10 } }
      : {}),
    webPreferences: {
      preload: join(__dirname, '../preload/index.js'),
      sandbox: false,
      contextIsolation: true,
      nodeIntegration: false
    }
  })

  mainWindow.on('ready-to-show', () => {
    mainWindow?.show()
  })

  mainWindow.webContents.setWindowOpenHandler((details) => {
    shell.openExternal(details.url)
    return { action: 'deny' }
  })

  // HMR for renderer in dev, load built files in production
  if (is.dev && process.env['ELECTRON_RENDERER_URL']) {
    mainWindow.loadURL(process.env['ELECTRON_RENDERER_URL'])
  } else {
    mainWindow.loadFile(join(__dirname, '../renderer/index.html'))
  }
}

// IPC handlers for frameless window controls
ipcMain.handle('window:minimize', () => {
  mainWindow?.minimize()
})

ipcMain.handle('window:maximize', () => {
  if (mainWindow?.isMaximized()) {
    mainWindow.unmaximize()
  } else {
    mainWindow?.maximize()
  }
})

ipcMain.handle('window:close', () => {
  mainWindow?.close()
})

ipcMain.handle('window:isMaximized', () => {
  return mainWindow?.isMaximized() ?? false
})

ipcMain.handle('video:getServerPort', () => {
  return getVideoServerPort()
})

ipcMain.handle('ipc:app:version', () => app.getVersion())

ipcMain.handle('dialog:selectDirectory', async () => {
  if (!mainWindow) return { success: false, error: 'No window' }
  const result = await dialog.showOpenDialog(mainWindow, {
    properties: ['openDirectory']
  })
  if (result.canceled || result.filePaths.length === 0) {
    return { success: false }
  }
  return { success: true, data: result.filePaths[0] }
})

// Register custom protocol for serving local video files
protocol.registerSchemesAsPrivileged([
  {
    scheme: 'local-video',
    privileges: {
      stream: true,
      bypassCSP: true,
      corsEnabled: true,
      supportFetchAPI: true
    }
  }
])

app.whenReady().then(async () => {
  // Start local HTTP server for video playback with Range support (seeking)
  try {
    const port = await startVideoServer()
    console.log(`[Main] Video server started on port ${port}`)
  } catch (err) {
    console.error('[Main] Failed to start video server:', err)
  }

  // Keep local-video:// as fallback protocol
  protocol.handle('local-video', (request) => {
    const url = new URL(request.url)
    const filePath = decodeURIComponent(url.searchParams.get('path') || '')
    if (!filePath) {
      return new Response('Missing path', { status: 400 })
    }
    return net.fetch(`file://${filePath}`)
  })

  // Strip referrer for Douyin avatar images to bypass hotlink protection
  const { session } = require('electron')
  session.defaultSession.webRequest.onBeforeSendHeaders(
    { urls: ['https://*.douyinpic.com/*', 'https://*.byteimg.com/*'] },
    (details: any, callback: any) => {
      delete details.requestHeaders['Referer']
      delete details.requestHeaders['referer']
      callback({ requestHeaders: details.requestHeaders })
    }
  )

  // 错误上报：越早初始化越能捕获启动早期异常
  const API_BASE =
    (process.env.VITE_API_BASE as string | undefined)
    || ((import.meta as any).env?.VITE_API_BASE as string | undefined)
    || 'http://localhost:18081/api/v1'
  initMainErrorReporter(app.getVersion(), API_BASE)

  // Register recording IPC handlers
  registerRecordingHandlers()

  // 标点模型随安装包内置。缺失时只降级为无标点文本，不再联网下载。
  try {
    const punctMgr = getPunctModelManager()
    if (!punctMgr.isReady()) {
      console.warn('[Main] Bundled punct model missing; transcripts will omit punctuation')
    }
  } catch (err) {
    console.warn('[Main] punct model manager init failed:', err)
  }

  // 渲染进程可通过 IPC 让主进程代上报（离线时主进程有更强持久化能力）
  const { ipcMain } = require('electron')
  ipcMain.handle('ipc:obs:reportError', (_e: unknown, scope: string, err: any, details?: any) => {
    reportMainError(scope, err, details)
    return { success: true }
  })

  createWindow()

  // 自动更新（dev 模式不跑，避免 electron-updater 尝试访问 dev feed）
  if (!is.dev && mainWindow) {
    setupAutoUpdater(mainWindow)
  }

  // ASR 模型随安装包内置。启动后做一次非阻塞健康检查，缺失时提示重装客户端。
  setTimeout(() => {
    try {
      const mgr = getAsrModelManager()
      if (!mgr.isReady()) {
        mgr.ensureModelReady().catch((e: Error) =>
          console.warn('[Main] Bundled ASR model missing:', e.message)
        )
      }
    } catch (e) {
      console.warn('[Main] ASR model check skipped:', (e as Error).message)
    }
  }, 5000)

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow()
    }
  })
})

app.on('before-quit', () => {
  feishuTaskWorker.stop()

  const monitorService = getMonitorService()
  if (monitorService) {
    monitorService.stopMonitoring()
  }

  const reconnectService = getReconnectService()
  if (reconnectService) {
    reconnectService.cancelAll()
  }

  const recordingService = getRecordingService()
  if (recordingService) {
    recordingService.stopAll()
  }
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})
