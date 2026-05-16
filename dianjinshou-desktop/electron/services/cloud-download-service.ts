import axios from 'axios'
import { app, dialog } from 'electron'
import { createWriteStream, mkdirSync } from 'fs'
import { basename, dirname, join } from 'path'

export interface CloudDownloadResult {
  success: boolean
  canceled?: boolean
  path?: string
  error?: string
}

export async function downloadCloudFile(url: string, fileName?: string): Promise<CloudDownloadResult> {
  if (!url) {
    return { success: false, error: 'Missing download url' }
  }

  const defaultName = sanitizeFileName(fileName || basename(new URL(url).pathname) || 'cloud-file.mp4')
  const saveResult = await dialog.showSaveDialog({
    defaultPath: join(app.getPath('downloads'), defaultName),
    properties: ['createDirectory']
  })

  if (saveResult.canceled || !saveResult.filePath) {
    return { success: false, canceled: true }
  }

  mkdirSync(dirname(saveResult.filePath), { recursive: true })
  const response = await axios.get(url, { responseType: 'stream', timeout: 0 })

  await new Promise<void>((resolve, reject) => {
    const writer = createWriteStream(saveResult.filePath!)
    response.data.pipe(writer)
    response.data.on('error', reject)
    writer.on('finish', resolve)
    writer.on('error', reject)
  })

  return { success: true, path: saveResult.filePath }
}

function sanitizeFileName(value: string) {
  const safe = value.replace(/[\\/:*?"<>|]+/g, '_').trim()
  return safe || 'cloud-file.mp4'
}
