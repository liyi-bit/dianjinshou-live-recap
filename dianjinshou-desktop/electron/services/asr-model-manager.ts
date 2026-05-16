/**
 * sherpa-onnx Paraformer 模型管理器
 *
 * 职责：
 *   - 优先使用安装包内置的 Paraformer 中文模型（model.int8.onnx + tokens.txt）
 *   - 兼容旧版本已下载到 userData 的模型
 *   - 状态查询供 UI 展示
 *
 * 目录布局（Windows 默认）：
 *   安装包 resources/sherpa-models/paraformer-zh/
 *     ├── model.int8.onnx   (~227 MB)
 *     └── tokens.txt        (~75 KB)
 */
import { app, BrowserWindow } from 'electron'
import { join } from 'path'
import {
  existsSync,
  statSync,
  unlinkSync,
} from 'fs'

export interface ModelStatus {
  ready: boolean
  downloading: boolean
  /** 0-100 */
  percent: number
  downloadedBytes: number
  totalBytes: number
  lastError?: string
  modelDir: string
}

/** 解压后期望的两个文件（缺一则认为未就绪） */
const REQUIRED_FILES = ['model.int8.onnx', 'tokens.txt']
/** model.int8.onnx 最小体积 sanity check（防截断文件被误认已就绪） */
const MIN_MODEL_SIZE = 200 * 1024 * 1024 // 200 MB


export class AsrModelManager {
  private status: ModelStatus

  constructor() {
    this.status = {
      ready: false,
      downloading: false,
      percent: 0,
      downloadedBytes: 0,
      totalBytes: 0,
      modelDir: this.getModelDir(),
    }
    this.refreshReady()
  }

  getModelDir(): string {
    const bundled = this.getBundledModelDir()
    if (this.isDirReady(bundled)) return bundled

    const legacy = this.getLegacyModelDir()
    if (this.isDirReady(legacy)) return legacy

    return bundled
  }

  getModelPath(): string {
    return join(this.getModelDir(), 'model.int8.onnx')
  }

  getTokensPath(): string {
    return join(this.getModelDir(), 'tokens.txt')
  }

  isReady(): boolean {
    return this.refreshReady()
  }

  getStatus(): ModelStatus {
    this.refreshReady()
    return { ...this.status }
  }

  /** 重新检查本地文件是否齐全；更新 status.ready */
  private refreshReady(): boolean {
    const dir = this.getModelDir()
    const ready = this.isDirReady(dir)
    this.status.ready = ready
    this.status.downloading = false
    this.status.percent = ready ? 100 : 0
    this.status.downloadedBytes = ready ? this.getModelSize(dir) : 0
    this.status.totalBytes = ready ? this.getModelSize(dir) : 0
    this.status.modelDir = dir
    if (ready) delete this.status.lastError
    return ready
  }

  /**
   * 模型随安装包内置，不再联网下载。
   * 保留此方法是为了兼容旧 IPC 调用；缺模型时返回 false 并给出重装提示。
   */
  async ensureModelReady(): Promise<boolean> {
    const ready = this.refreshReady()
    if (!ready) {
      this.status.lastError = '安装包缺少本地 ASR 模型，请重新安装最新版客户端'
      this.broadcast()
    }
    return ready
  }

  /** 仅删除旧版本 userData 中的模型；安装包内置模型不可删除。 */
  async removeModel(): Promise<void> {
    const dir = this.getLegacyModelDir()
    REQUIRED_FILES.forEach((f) => {
      const p = join(dir, f)
      if (existsSync(p)) { try { unlinkSync(p) } catch {} }
    })
    this.refreshReady()
    this.broadcast()
  }

  private getBundledModelDir(): string {
    if (app.isPackaged) {
      return join(process.resourcesPath, 'sherpa-models', 'paraformer-zh')
    }
    return join(app.getAppPath(), 'build', 'sherpa-models', 'paraformer-zh')
  }

  private getLegacyModelDir(): string {
    return join(app.getPath('userData'), 'sherpa-models', 'paraformer-zh')
  }

  private isDirReady(dir: string): boolean {
    if (!existsSync(dir)) return false
    try {
      return (
        REQUIRED_FILES.every((f) => existsSync(join(dir, f))) &&
        existsSync(join(dir, 'model.int8.onnx')) &&
        statSync(join(dir, 'model.int8.onnx')).size > MIN_MODEL_SIZE
      )
    } catch {
      return false
    }
  }

  private getModelSize(dir: string): number {
    try {
      return REQUIRED_FILES.reduce((sum, file) => sum + statSync(join(dir, file)).size, 0)
    } catch {
      return 0
    }
  }

  private broadcast(): void {
    const payload = this.getStatus()
    for (const w of BrowserWindow.getAllWindows()) {
      try {
        w.webContents.send('asr:model-status', payload)
      } catch {}
    }
  }
}

/** 单例 */
let _instance: AsrModelManager | null = null
export function getAsrModelManager(): AsrModelManager {
  if (!_instance) _instance = new AsrModelManager()
  return _instance
}
