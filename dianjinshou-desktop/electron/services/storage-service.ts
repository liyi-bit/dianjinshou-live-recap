import { app } from 'electron'
import { join } from 'path'
import { mkdirSync, existsSync, statfsSync } from 'fs'

export interface DiskUsage {
  used: number
  total: number
  free: number
  percentUsed: number
}

const MIN_FREE_BYTES_FOR_AUTO_PICK = 5 * 1024 * 1024 * 1024 // 5GB
const FOLDER_NAME = 'dianjinshou-recordings'

export class StorageService {
  private baseDir: string

  constructor() {
    this.baseDir = StorageService.resolveBaseDir()
    this.ensureDir(this.baseDir)
    console.log(`[StorageService] Recording dir: ${this.baseDir}`)
  }

  /**
   * Pick the best directory for recordings. Priority:
   *   1. DIANJINSHOU_RECORDING_DIR env var
   *   2. On Windows, the non-C drive with the most free space (>= 5GB)
   *   3. Electron userData (typically on C:)
   */
  private static resolveBaseDir(): string {
    const fromEnv = process.env.DIANJINSHOU_RECORDING_DIR
    if (fromEnv && fromEnv.trim()) return fromEnv.trim()

    if (process.platform === 'win32') {
      const candidates: { root: string; free: number }[] = []
      for (const letter of ['D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N']) {
        const root = `${letter}:\\`
        if (!existsSync(root)) continue
        try {
          const s = statfsSync(root)
          const free = Number(s.bfree) * Number(s.bsize)
          if (free >= MIN_FREE_BYTES_FOR_AUTO_PICK) {
            candidates.push({ root, free })
          }
        } catch {
          // drive not readable (e.g. unmounted CD-ROM); skip
        }
      }
      if (candidates.length > 0) {
        candidates.sort((a, b) => b.free - a.free)
        return join(candidates[0].root, FOLDER_NAME)
      }
    }

    return join(app.getPath('userData'), 'recordings')
  }

  /**
   * Get the default recording directory path
   */
  getRecordingDir(): string {
    return this.baseDir
  }

  /**
   * Switch the recording base directory. Validates the target is creatable and writable.
   * Returns the resolved path actually in use. Throws on failure.
   */
  setBaseDir(newPath: string): string {
    const trimmed = (newPath || '').trim()
    if (!trimmed) throw new Error('路径不能为空')
    this.ensureDir(trimmed)
    // Probe writability with a temp file
    const probe = join(trimmed, `.djs-write-probe-${Date.now()}`)
    try {
      require('fs').writeFileSync(probe, 'ok')
      require('fs').unlinkSync(probe)
    } catch (err) {
      throw new Error(`目录不可写: ${err instanceof Error ? err.message : String(err)}`)
    }
    this.baseDir = trimmed
    console.log(`[StorageService] Recording dir updated: ${this.baseDir}`)
    return this.baseDir
  }

  /**
   * Ensure a directory exists, creating it recursively if needed
   */
  ensureDir(dirPath: string): void {
    if (!existsSync(dirPath)) {
      mkdirSync(dirPath, { recursive: true })
    }
  }

  /**
   * Get disk usage information for the recording directory's drive
   */
  getDiskUsage(): DiskUsage {
    try {
      const stats = statfsSync(this.baseDir)
      const blockSize = stats.bsize
      const total = stats.blocks * blockSize
      const free = stats.bfree * blockSize
      const used = total - free
      const percentUsed = total > 0 ? Math.round((used / total) * 10000) / 100 : 0

      return { used, total, free, percentUsed }
    } catch (err) {
      console.error('[StorageService] Failed to get disk usage:', err)
      return { used: 0, total: 0, free: 0, percentUsed: 0 }
    }
  }

  /**
   * Format bytes to a human-readable string
   */
  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 B'

    const units = ['B', 'KB', 'MB', 'GB', 'TB']
    const k = 1024
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    const size = bytes / Math.pow(k, i)

    return `${size.toFixed(i === 0 ? 0 : 2)} ${units[i]}`
  }
}
