/**
 * Local HTTP server for serving video files with proper Range request support.
 * This replaces the custom local-video:// protocol which doesn't handle seeking correctly.
 */

import { createServer, Server, IncomingMessage, ServerResponse } from 'http'
import { createReadStream, statSync } from 'fs'
import { extname } from 'path'

let server: Server | null = null
let serverPort = 0

const MIME_TYPES: Record<string, string> = {
  '.mp4': 'video/mp4',
  '.flv': 'video/x-flv',
  '.webm': 'video/webm',
  '.mkv': 'video/x-matroska',
}

function handleRequest(req: IncomingMessage, res: ServerResponse): void {
  try {
    const url = new URL(req.url || '/', `http://localhost:${serverPort}`)
    const filePath = decodeURIComponent(url.searchParams.get('path') || '')

    if (!filePath) {
      res.writeHead(400, { 'Content-Type': 'text/plain' })
      res.end('Missing path parameter')
      return
    }

    let stat
    try {
      stat = statSync(filePath)
    } catch {
      res.writeHead(404, { 'Content-Type': 'text/plain' })
      res.end('File not found')
      return
    }

    const fileSize = stat.size
    const ext = extname(filePath).toLowerCase()
    const contentType = MIME_TYPES[ext] || 'application/octet-stream'
    const rangeHeader = req.headers.range

    if (rangeHeader) {
      // Parse Range header: "bytes=start-end"
      const parts = rangeHeader.replace(/bytes=/, '').split('-')
      const start = parseInt(parts[0], 10)
      const end = parts[1] ? parseInt(parts[1], 10) : fileSize - 1

      if (start >= fileSize || end >= fileSize || start > end) {
        res.writeHead(416, {
          'Content-Range': `bytes */${fileSize}`,
        })
        res.end()
        return
      }

      const chunkSize = end - start + 1
      res.writeHead(206, {
        'Content-Range': `bytes ${start}-${end}/${fileSize}`,
        'Accept-Ranges': 'bytes',
        'Content-Length': chunkSize,
        'Content-Type': contentType,
        'Access-Control-Allow-Origin': '*',
      })

      const stream = createReadStream(filePath, { start, end })
      stream.pipe(res)
      stream.on('error', () => {
        if (!res.writableEnded) res.end()
      })
    } else {
      // No Range — serve full file
      res.writeHead(200, {
        'Content-Length': fileSize,
        'Content-Type': contentType,
        'Accept-Ranges': 'bytes',
        'Access-Control-Allow-Origin': '*',
      })

      const stream = createReadStream(filePath)
      stream.pipe(res)
      stream.on('error', () => {
        if (!res.writableEnded) res.end()
      })
    }
  } catch (err) {
    console.error('[VideoServer] Request error:', err)
    if (!res.writableEnded) {
      res.writeHead(500, { 'Content-Type': 'text/plain' })
      res.end('Internal server error')
    }
  }
}

/**
 * Start the local video server. Returns the port number.
 * Uses port 0 to let the OS assign a free port.
 */
export function startVideoServer(): Promise<number> {
  return new Promise((resolve, reject) => {
    if (server && serverPort > 0) {
      resolve(serverPort)
      return
    }

    server = createServer(handleRequest)

    server.on('error', (err) => {
      console.error('[VideoServer] Server error:', err)
      reject(err)
    })

    // Listen on localhost only (security: not accessible from network)
    server.listen(0, '127.0.0.1', () => {
      const addr = server!.address()
      if (addr && typeof addr === 'object') {
        serverPort = addr.port
        console.log(`[VideoServer] Listening on http://127.0.0.1:${serverPort}`)
        resolve(serverPort)
      } else {
        reject(new Error('Failed to get server address'))
      }
    })
  })
}

/** Get the current server port (0 if not started) */
export function getVideoServerPort(): number {
  return serverPort
}

/** Stop the video server */
export function stopVideoServer(): void {
  if (server) {
    server.close()
    server = null
    serverPort = 0
  }
}
