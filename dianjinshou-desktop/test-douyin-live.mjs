/**
 * 抖音直播检测+录制端到端测试
 * 用法: node test-douyin-live.mjs <直播间ID/抖音号/URL>
 */

import axios from 'axios'
import dns from 'dns'
import http from 'http'
import https from 'https'
import { spawn, execSync } from 'child_process'
import { existsSync, statSync, unlinkSync, mkdirSync, createWriteStream } from 'fs'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'
import { createRequire } from 'module'

const __dirname = dirname(fileURLToPath(import.meta.url))
const require = createRequire(import.meta.url)
const FFMPEG = require('ffmpeg-static')

const QUALITY_KEYS = ['FULL_HD1', 'HD1', 'SD1', 'SD2', 'LD']
const RECORD_DURATION = 60
const OUTPUT_DIR = join(__dirname, 'test-output')

// Force IPv4 DNS + agent to avoid CDN IPv6 issues
function ipv4Lookup(hostname, options, callback) {
  dns.lookup(hostname, { ...options, family: 4 }, callback)
}
const httpAgent = new http.Agent({ lookup: ipv4Lookup })
const httpsAgent = new https.Agent({ lookup: ipv4Lookup })
const axiosIPv4 = axios.create({ httpAgent, httpsAgent })

// ── API Detection ──

async function getLiveInfo(input) {
  const webRid = input.trim().match(/live\.douyin\.com\/([A-Za-z0-9_]+)/)?.[1]
    || input.trim().match(/^[A-Za-z0-9_]{3,}$/)?.[0]
  if (!webRid) return { error: '无法解析输入' }

  console.log(`\n[Step 1] web_rid: ${webRid}`)

  // 获取 ttwid cookie (先从根路径，再从主播页)
  console.log(`[Step 2] 获取 ttwid...`)
  let cookies = ''
  try {
    const root = await axiosIPv4.get(`https://live.douyin.com/`, {
      headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36' },
      timeout: 10000, validateStatus: () => true,
    })
    const rootCookies = (root.headers['set-cookie'] || []).map(c => c.split(';')[0])
    cookies = rootCookies.join('; ')
    console.log(`  ├─ root: ${cookies ? 'OK (' + rootCookies.length + ')' : 'EMPTY'}`)
    const init = await axiosIPv4.get(`https://live.douyin.com/${webRid}`, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        Cookie: cookies,
      },
      timeout: 10000, validateStatus: () => true,
    })
    const extra = (init.headers['set-cookie'] || []).map(c => c.split(';')[0]).filter(Boolean)
    if (extra.length) cookies = cookies + '; ' + extra.join('; ')
    console.log(`  └─ final: ${cookies.length} chars, ttwid=${/ttwid=/.test(cookies) ? 'YES' : 'NO'}`)
  } catch(e) {
    console.log(`  └─ 失败: ${e.message}`)
  }

  // 多次尝试API (有时第一次返回HTML)
  console.log(`[Step 3] 调用API...`)
  for (let attempt = 1; attempt <= 3; attempt++) {
    try {
      const apiUrl = `https://live.douyin.com/webcast/room/web/enter/?web_rid=${webRid}` +
        `&aid=6383&app_name=douyin_web&live_id=1&device_platform=web` +
        `&language=zh-CN&browser_language=zh-CN&browser_platform=Win32` +
        `&browser_name=Chrome&browser_version=120.0.0.0`

      const resp = await axiosIPv4.get(apiUrl, {
        headers: {
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
          'Referer': `https://live.douyin.com/${webRid}`,
          'Accept': 'application/json, text/plain, */*',
          'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
          'Cookie': cookies,
        },
        timeout: 10000, validateStatus: () => true,
      })

      let data = resp.data
      if (typeof data === 'string') {
        try { data = JSON.parse(data) } catch {
          // 收集新cookie重试
          const newCookies = (resp.headers['set-cookie'] || []).map(c => c.split(';')[0])
          if (newCookies.length) cookies = cookies + '; ' + newCookies.join('; ')
          console.log(`  attempt ${attempt}: HTML, retrying...`)
          await new Promise(r => setTimeout(r, 1000))
          continue
        }
      }

      if (data?.status_code !== 0) {
        console.log(`  attempt ${attempt}: status_code=${data?.status_code}`)
        continue
      }

      const rooms = data?.data?.data
      if (!Array.isArray(rooms) || !rooms.length) {
        console.log(`  attempt ${attempt}: no rooms`)
        continue
      }

      const room = rooms[0]
      const streamerName = room.owner?.nickname || `直播间${webRid}`
      console.log(`  ├─ 主播: ${streamerName}`)
      console.log(`  ├─ status: ${room.status} (2=直播中)`)
      console.log(`  └─ ${room.status === 2 ? '✓ 正在直播' : '✗ 未开播'}`)

      if (room.status !== 2) return { isLive: false, streamerName, webRid }

      // Extract streams
      const streams = {}
      const flv = room.stream_url?.flv_pull_url || {}
      for (const q of QUALITY_KEYS) if (flv[q]) streams[q] = flv[q]
      if (!Object.keys(streams).length) {
        const hls = room.stream_url?.hls_pull_url_map || room.stream_url?.hls_pull_url || {}
        for (const q of QUALITY_KEYS) if (hls[q]) streams[q] = hls[q]
      }

      console.log(`\n[Step 4] 画质: ${Object.keys(streams).join(', ')}`)
      let streamUrl = null
      for (const q of QUALITY_KEYS) {
        if (streams[q]) { streamUrl = streams[q]; console.log(`  └─ 选择: ${q}`); break }
      }

      return { isLive: true, streamerName, webRid, streams, streamUrl }
    } catch(e) {
      console.log(`  attempt ${attempt} error: ${e.message}`)
    }
    await new Promise(r => setTimeout(r, 1000))
  }

  return { error: '3次尝试后仍无法获取直播间数据' }
}

// ── Record using Node.js HTTP pipe → FFmpeg stdin ──
// 这避免了 FFmpeg 自己连接 CDN 时的 IPv6 问题

function recordStream(streamUrl, flvPath, duration) {
  return new Promise((resolve, reject) => {
    console.log(`\n[Step 5] 开始录制 ${duration}秒`)
    console.log(`  ├─ 输出: ${flvPath}`)
    console.log(`  ├─ 模式: Node.js HTTP → pipe → FFmpeg`)

    // FFmpeg 从 stdin 读取流
    const ffmpeg = spawn(FFMPEG, [
      '-y',
      '-i', 'pipe:0',       // 从stdin读取
      '-c', 'copy',
      '-t', String(duration),
      '-f', 'flv',
      flvPath,
    ], { stdio: ['pipe', 'pipe', 'pipe'] })

    let lastLine = ''
    ffmpeg.stderr.on('data', (data) => {
      const line = data.toString().trim()
      if (line) lastLine = line
      const tm = line.match(/time=(\d{2}:\d{2}:\d{2}\.\d{2})/)
      if (tm) process.stdout.write(`\r  ├─ 进度: ${tm[1]}`)
    })

    ffmpeg.on('exit', (code) => {
      console.log(`\n  └─ FFmpeg code=${code}`)
      if (existsSync(flvPath) && statSync(flvPath).size > 0) {
        console.log(`  ✓ FLV: ${(statSync(flvPath).size / 1024 / 1024).toFixed(2)} MB`)
        resolve(flvPath)
      } else {
        reject(new Error(`录制失败: ${lastLine}`))
      }
    })
    ffmpeg.on('error', (err) => reject(new Error(`FFmpeg: ${err.message}`)))

    // 用 axios 连接 CDN (自动跟随重定向, 强制 IPv4)
    axiosIPv4.get(streamUrl, {
      responseType: 'stream',
      timeout: 15000,
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Referer': 'https://live.douyin.com/',
      },
      maxRedirects: 5,
    }).then((res) => {
      console.log(`  ├─ CDN status: ${res.status}`)
      res.data.pipe(ffmpeg.stdin)
      res.data.on('error', (err) => {
        console.log(`\n  ├─ 流中断: ${err.message}`)
        try { ffmpeg.stdin.end() } catch {}
      })
      res.data.on('end', () => {
        try { ffmpeg.stdin.end() } catch {}
      })
    }).catch((err) => {
      reject(new Error(`CDN连接失败: ${err.message}`))
    })
  })
}

// ── Remux ──

function remux(flvPath, mp4Path) {
  return new Promise((resolve) => {
    console.log(`\n[Step 6] FLV→MP4 转封装`)
    const proc = spawn(FFMPEG, ['-y', '-i', flvPath, '-c', 'copy', '-movflags', '+faststart', mp4Path],
      { stdio: ['pipe', 'pipe', 'pipe'] })
    proc.on('exit', (code) => {
      if (code === 0 && existsSync(mp4Path) && statSync(mp4Path).size > 0) {
        console.log(`  ✓ MP4: ${(statSync(mp4Path).size / 1024 / 1024).toFixed(2)} MB`)
        try { unlinkSync(flvPath) } catch {}
        resolve(mp4Path)
      } else {
        console.log(`  ✗ 转封装失败, 保留FLV`)
        resolve(flvPath)
      }
    })
    proc.on('error', () => resolve(flvPath))
  })
}

// ── Main ──

async function main() {
  const input = process.argv[2] || '88459800212'

  console.log('═══════════════════════════════════════════')
  console.log('  抖音直播 检测+录制 端到端测试')
  console.log('═══════════════════════════════════════════')
  console.log(`输入: ${input} | 录制: ${RECORD_DURATION}秒`)

  const info = await getLiveInfo(input)
  if (info.error) { console.error(`\n✗ ${info.error}`); process.exit(1) }
  if (!info.isLive) { console.log('\n✗ 未开播'); process.exit(0) }
  if (!info.streamUrl) { console.error('\n✗ 无流地址'); process.exit(1) }

  if (!existsSync(OUTPUT_DIR)) mkdirSync(OUTPUT_DIR, { recursive: true })
  const dt = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19)
  const base = `${info.streamerName.replace(/[<>:"/\\|?*]/g, '_')}_${dt}`
  const flvPath = join(OUTPUT_DIR, `${base}.flv`)
  const mp4Path = join(OUTPUT_DIR, `${base}.mp4`)

  await recordStream(info.streamUrl, flvPath, RECORD_DURATION)
  const finalPath = await remux(flvPath, mp4Path)

  console.log('\n═══════════════════════════════════════════')
  console.log('  ✓ 端到端测试完成!')
  console.log('═══════════════════════════════════════════')
  console.log(`  主播: ${info.streamerName}`)
  console.log(`  输出: ${finalPath}`)
  try {
    const ffprobe = FFMPEG.replace('ffmpeg.exe', 'ffprobe.exe').replace('ffmpeg', 'ffprobe')
    const probe = execSync(`"${ffprobe}" -v quiet -print_format json -show_format "${finalPath}"`, { encoding: 'utf-8' })
    const m = JSON.parse(probe)
    console.log(`  时长: ${parseFloat(m.format.duration).toFixed(1)}s`)
    console.log(`  大小: ${(parseInt(m.format.size) / 1024 / 1024).toFixed(2)} MB`)
    console.log(`  格式: ${m.format.format_long_name}`)
  } catch {}
}

main().catch(e => { console.error('Fatal:', e.message); process.exit(1) })
