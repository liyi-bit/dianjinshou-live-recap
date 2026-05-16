/**
 * 自动更新服务
 *
 * 分发方式：electron-builder 配置的 `publish.url`（当前指向 http://localhost:18081/desktop/）
 *   客户端会 GET `${url}/latest.yml` 对比版本，有新版本则下载 Setup.exe 并 quitAndInstall。
 *
 * 事件通过 IPC 推给渲染进程：
 *   update:status   — 状态变化（checking / available / not-available / downloaded / error）
 *   update:progress — 下载进度 { percent, bytesPerSecond, transferred, total }
 *
 * 渲染端 IPC 可调用：
 *   ipc:update:check    — 主动检查（通常用户点「检查更新」按钮）
 *   ipc:update:download — 确认下载（update-available 后用户点「立即更新」触发）
 *   ipc:update:install  — 下载完成后「重启安装」
 */
import { BrowserWindow, ipcMain } from 'electron'
import { autoUpdater } from 'electron-updater'

const CHECK_INTERVAL_MS = 60 * 60 * 1000 // 1 小时周期性检查

export function setupAutoUpdater(mainWindow: BrowserWindow): void {
  // 用户点「立即更新」后再下载 —— 默认不自动下载避免偷流量
  autoUpdater.autoDownload = false
  // 用户点「重启安装」才装；退出时不自动装（避免无意中触发）
  autoUpdater.autoInstallOnAppQuit = false

  const send = (channel: string, payload: unknown) => {
    if (!mainWindow || mainWindow.isDestroyed()) return
    mainWindow.webContents.send(channel, payload)
  }

  autoUpdater.on('checking-for-update', () => {
    send('update:status', { state: 'checking' })
  })

  autoUpdater.on('update-available', (info) => {
    send('update:status', {
      state: 'available',
      version: info.version,
      releaseDate: info.releaseDate,
      releaseNotes: info.releaseNotes,
    })
  })

  autoUpdater.on('update-not-available', () => {
    send('update:status', { state: 'not-available' })
  })

  autoUpdater.on('download-progress', (p) => {
    send('update:progress', {
      percent: Math.round(p.percent * 10) / 10,
      bytesPerSecond: p.bytesPerSecond,
      transferred: p.transferred,
      total: p.total,
    })
  })

  autoUpdater.on('update-downloaded', (info) => {
    send('update:status', { state: 'downloaded', version: info.version })
  })

  autoUpdater.on('error', (err) => {
    console.error('[AutoUpdater]', err)
    send('update:status', { state: 'error', message: err?.message || String(err) })
  })

  // IPC: 主动检查
  ipcMain.handle('ipc:update:check', async () => {
    try {
      await autoUpdater.checkForUpdates()
      return { success: true }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      return { success: false, error: message }
    }
  })

  // IPC: 确认下载
  //   v1.1.0 修复：强制升级弹窗可能比自动 check 先一步打开，用户点「立即更新」时
  //   electron-updater 内部 isUpdateAvailable 还是 false，downloadUpdate 直接抛
  //   "Please check update first. Event 'update-available' has not been emitted yet."
  //   这里先同步等一次 check，让内部状态就位，再触发下载。
  ipcMain.handle('ipc:update:download', async () => {
    try {
      const result = await autoUpdater.checkForUpdates()
      if (!result || !result.updateInfo) {
        return { success: false, error: '没有可用更新（后端未发布新版本）' }
      }
      await autoUpdater.downloadUpdate()
      return { success: true }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      return { success: false, error: message }
    }
  })

  // IPC: 下载后重启安装
  ipcMain.handle('ipc:update:install', () => {
    // (isSilent=false, isForceRunAfter=true) 允许安装界面 + 装完启动新版
    autoUpdater.quitAndInstall(false, true)
    return { success: true }
  })

  // 启动 5 秒后做第一次检查（不阻塞启动、给窗口时间准备好接收事件）
  setTimeout(() => {
    autoUpdater.checkForUpdates().catch((e) => console.warn('[AutoUpdater] initial check failed:', e?.message))
  }, 5000)

  // 周期性检查
  setInterval(() => {
    autoUpdater.checkForUpdates().catch(() => { /* silent */ })
  }, CHECK_INTERVAL_MS)
}
