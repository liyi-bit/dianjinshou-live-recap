/**
 * sherpa-onnx 中英混合标点模型管理器（ct-transformer-zh-en-vocab272727-2024-04-12）
 *
 * Paraformer 只出纯文字没有标点，再套一层 OfflinePunctuation 把逗号/句号补齐。
 *
 * 目录布局：
 *   安装包 resources/sherpa-models/punct-zh-en/
 *     └── model.onnx   (~278 MB)
 */
import { app, BrowserWindow } from 'electron'
import { join } from 'path'
import {
  existsSync,
  statSync,
} from 'fs'

export interface PunctModelStatus {
  ready: boolean
  downloading: boolean
  percent: number
  downloadedBytes: number
  totalBytes: number
  lastError?: string
  modelDir: string
}

const REQUIRED_FILES = ['model.onnx']
/** 278MB 真值，200MB 做下限 sanity check */
const MIN_MODEL_SIZE = 200 * 1024 * 1024

export class PunctModelManager {
  private status: PunctModelStatus

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
    return join(this.getModelDir(), 'model.onnx')
  }

  isReady(): boolean {
    return this.refreshReady()
  }

  getStatus(): PunctModelStatus {
    this.refreshReady()
    return { ...this.status }
  }

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

  async ensureModelReady(): Promise<boolean> {
    const ready = this.refreshReady()
    if (!ready) {
      this.status.lastError = '安装包缺少标点模型，逐字稿将降级为无标点文本'
      this.broadcast()
    }
    return ready
  }

  private getBundledModelDir(): string {
    if (app.isPackaged) {
      return join(process.resourcesPath, 'sherpa-models', 'punct-zh-en')
    }
    return join(app.getAppPath(), 'build', 'sherpa-models', 'punct-zh-en')
  }

  private getLegacyModelDir(): string {
    return join(app.getPath('userData'), 'sherpa-models', 'punct-zh-en')
  }

  private isDirReady(dir: string): boolean {
    const p = join(dir, 'model.onnx')
    try {
      return existsSync(p) && statSync(p).size > MIN_MODEL_SIZE
    } catch {
      return false
    }
  }

  private getModelSize(dir: string): number {
    try {
      return statSync(join(dir, 'model.onnx')).size
    } catch {
      return 0
    }
  }

  private broadcast(): void {
    const payload = this.getStatus()
    for (const w of BrowserWindow.getAllWindows()) {
      try { w.webContents.send('punct:model-status', payload) } catch {}
    }
  }
}

let _instance: PunctModelManager | null = null
export function getPunctModelManager(): PunctModelManager {
  if (!_instance) _instance = new PunctModelManager()
  return _instance
}
