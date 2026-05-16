import axios from 'axios'
import { createReadStream, statSync } from 'fs'

export class CosUploadService {
  async uploadFile(
    uploadUrl: string,
    filePath: string,
    contentType: string,
    onProgress?: (progress: number) => Promise<void> | void
  ): Promise<void> {
    const stat = statSync(filePath)
    let lastProgress = -1
    await axios.put(uploadUrl, createReadStream(filePath), {
      headers: {
        'Content-Type': contentType || 'application/octet-stream',
        'Content-Length': stat.size,
      },
      maxBodyLength: Infinity,
      maxContentLength: Infinity,
      timeout: 0,
      onUploadProgress: async (event) => {
        if (!event.total) return
        const progress = Math.max(0, Math.min(99, Math.floor((event.loaded / event.total) * 100)))
        if (progress !== lastProgress && progress % 5 === 0) {
          lastProgress = progress
          await onProgress?.(progress)
        }
      },
    })
    await onProgress?.(100)
  }
}
