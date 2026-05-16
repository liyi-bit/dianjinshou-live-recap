/**
 * 抖音直播间检测服务
 *
 * 移植自 dianjinshou-live/src/douyin.py，使用抖音 Webcast API 检测开播状态和获取流地址。
 * 无需 Cookie，只需直播间 URL 或 web_rid。
 */

import dns from 'dns'
import http from 'http'
import https from 'https'
import axios from 'axios'

// Force IPv4 DNS resolution to avoid CDN IPv6 issues on some networks
function ipv4Lookup(
  hostname: string,
  options: dns.LookupOptions,
  callback: (err: NodeJS.ErrnoException | null, address: string, family: number) => void
): void {
  dns.lookup(hostname, { ...options, family: 4 }, callback)
}
const httpAgent = new http.Agent({ lookup: ipv4Lookup as any })
const httpsAgent = new https.Agent({ lookup: ipv4Lookup as any })
const axiosIPv4 = axios.create({ httpAgent, httpsAgent })

// ── Types ──

export interface LiveInfo {
  isLive: boolean
  streamUrl: string | null
  streamerName: string | null
  streamerAvatar: string | null
  secUid: string | null
  roomId: string | null
  webRid: string | null
  streams: Record<string, string>
  error: string | null
}

// ── Constants ──

const HEADERS = {
  'User-Agent':
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
  Referer: 'https://live.douyin.com/',
  Accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
  'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
}

const MOBILE_HEADERS = {
  'User-Agent':
    'Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1',
  Accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
}

const QUALITY_KEYS = ['FULL_HD1', 'HD1', 'SD1', 'SD2', 'LD'] as const

// ── Internal helpers ──

/** 从分享文案中提取 URL */
function extractUrlFromText(text: string): string {
  const match = text.match(/https?:\/\/[^\s<>"']+/)
  return match ? match[0].replace(/\/+$/, '') : text.trim()
}

/** 解析 v.douyin.com 短链接，跟随重定向获取最终 URL */
async function resolveShortUrl(shortUrl: string): Promise<string | null> {
  try {
    const resp = await axiosIPv4.get(shortUrl, {
      headers: HEADERS,
      maxRedirects: 5,
      timeout: 10000,
      // 不需要响应体，只关心最终 URL
      maxContentLength: 1024 * 100,
    })
    return resp.request?.res?.responseUrl || resp.config?.url || null
  } catch {
    return null
  }
}

/**
 * 从各种格式的输入中提取 web_rid
 *
 * 支持：
 * - https://live.douyin.com/24771065580
 * - https://live.douyin.com/DNX833 (别名)
 * - https://v.douyin.com/xxxx/ (短链接)
 * - 分享文案中包含的链接
 * - 直接输入数字ID或别名
 */
export async function extractWebRid(text: string): Promise<string | null> {
  const clean = extractUrlFromText(text)

  // live.douyin.com/{id} — id 允许含句点（某些抖音号真实形式如 "lvlaoshi985.."）
  let match = clean.match(/live\.douyin\.com\/([A-Za-z0-9_.]+)/)
  if (match) return match[1]

  // webcast reflow URL
  match = clean.match(/webcast\.amemv\.com\/(?:douyin\/)?webcast\/reflow\/(\d+)/)
  if (match) return match[1]

  // 短链接 -> 跳转解析
  if (/v\.douyin\.com\//.test(clean)) {
    const resolved = await resolveShortUrl(clean)
    if (resolved) {
      match = resolved.match(/live\.douyin\.com\/([A-Za-z0-9_.]+)/)
      if (match) return match[1]
      match = resolved.match(/webcast\.amemv\.com\/(?:douyin\/)?webcast\/reflow\/(\d+)/)
      if (match) return match[1]
    }
  }

  // 直接输入别名/ID（抖音号允许含句点，如 "lvlaoshi985.."）
  const stripped = text.trim()
  if (/^[A-Za-z0-9_.]{3,}$/.test(stripped)) {
    return stripped
  }

  return null
}

/** 获取 ttwid cookie + 从 HTML 页面中提取 sec_uid、主播昵称和头像 */
async function fetchTtwidAndSecUid(webRid: string): Promise<{
  cookies: string
  secUid: string | null
  nickname: string | null
  avatar: string | null
}> {
  try {
    // 抖音把 ttwid 下发逻辑改了：只有根路径 / 才返回 ttwid，/webRid 不再下发
    // 必须先访问根路径拿 ttwid，否则后续 webcast API 会返回 HTML 而非 JSON（反爬拦截）
    let rootCookie = ''
    try {
      const root = await axiosIPv4.get('https://live.douyin.com/', {
        headers: { 'User-Agent': HEADERS['User-Agent'] },
        timeout: 10000,
        validateStatus: () => true,
      })
      const rootSetCookies: string[] = root.headers['set-cookie'] || []
      rootCookie = rootSetCookies.map((c: string) => c.split(';')[0]).filter(Boolean).join('; ')
    } catch {
      // 根路径失败不致命，继续尝试主播页
    }

    // 主播页 HTML 请求故意不带 ttwid：带上后抖音会走 CSR（HTML 里不嵌入 nickname/avatar），
    // 不带时抖音给 SSR 完整 HTML 含主播信息 —— 这是之前 96 个主播能正常拿到昵称的路径。
    // ttwid 只用于后续的 webcast API 调用（调 API 时才会加上）。
    const resp = await axiosIPv4.get(`https://live.douyin.com/${webRid}`, {
      headers: {
        'User-Agent': HEADERS['User-Agent'],
      },
      timeout: 10000,
      validateStatus: () => true,
    })
    const setCookies: string[] = resp.headers['set-cookie'] || []
    const extraCookies = setCookies.map((c: string) => c.split(';')[0]).filter(Boolean).join('; ')
    // 最终返回的 cookies 合并了根路径 ttwid + 主播页响应里下发的任何 cookie，供 webcast API 用
    const cookies = [rootCookie, extraCookies].filter(Boolean).join('; ')

    const html = typeof resp.data === 'string' ? resp.data : ''

    // 从 HTML 中提取 sec_uid（嵌在 RENDER_DATA 或脚本中）
    let secUid: string | null = null
    const secMatch = html.match(/sec_uid[^A-Za-z0-9]*([A-Za-z0-9_-]{20,})/)
    if (secMatch) {
      secUid = secMatch[1]
    }

    // 从 RENDER_DATA 中提取主播昵称和头像（即使未开播也有）
    // RENDER_DATA 是 URL-encoded 的 JSON，嵌在 <script id="RENDER_DATA"> 标签中
    let nickname: string | null = null
    let avatar: string | null = null

    // 尝试解析 RENDER_DATA
    const renderDataMatch = html.match(/<script\s+id="RENDER_DATA"[^>]*>([^<]+)<\/script>/)
    if (renderDataMatch) {
      try {
        const decoded = decodeURIComponent(renderDataMatch[1])
        const renderData = JSON.parse(decoded)
        // RENDER_DATA 结构: { "/path": { "initialState": { "roomStore": { "roomInfo": { "room": { "owner": ... } } } } } }
        for (const key of Object.keys(renderData)) {
          const state = renderData[key]?.initialState || renderData[key]
          const roomInfo = state?.roomStore?.roomInfo || state?.roomInfo
          const owner = roomInfo?.room?.owner || roomInfo?.anchor || state?.anchor
          if (owner?.nickname) {
            nickname = owner.nickname
            const avatarObj = owner.avatar_thumb || owner.avatar_medium || owner.avatar_large || {}
            const urls = avatarObj?.url_list || []
            avatar = urls.length > 0 ? urls[0] : null
            break
          }
          // 也尝试 user 字段
          const user = state?.userStore?.user || state?.user
          if (!nickname && user?.nickname) {
            nickname = user.nickname
            const avatarObj = user.avatar_thumb || user.avatar_medium || user.avatar_large || {}
            const urls = avatarObj?.url_list || []
            avatar = urls.length > 0 ? urls[0] : null
            break
          }
        }
      } catch {
        // RENDER_DATA 解析失败，用正则兜底
      }
    }

    // 正则兜底：从 HTML 或 JSON 片段中提取 nickname
    if (!nickname) {
      // 匹配 "nickname":"xxx" 或 \"nickname\":\"xxx\" (URL-encoded 或转义格式)
      // HTML 中可能有多个 nickname 字段，第一个可能是 $undefined 占位符，需要遍历找到有效值
      const nickRe = /\\?"nickname\\?"\s*:\s*\\?"([^"\\]{1,50})\\?"/g
      let nickMatch
      while ((nickMatch = nickRe.exec(html)) !== null) {
        const val = nickMatch[1]
        if (val && !val.includes('undefined') && !val.startsWith('$')) {
          nickname = val
          break
        }
      }
    }

    // 正则兜底：头像
    if (!avatar) {
      const avatarMatch = html.match(/avatar_thumb[^}]*url_list[^[]*\[\\?"(https?:\/\/[^"\\]+)/) ||
                          html.match(/avatarThumb[^}]*urlList[^[]*\[\\?"(https?:\/\/[^"\\]+)/)
      if (avatarMatch) {
        avatar = avatarMatch[1]
      }
    }

    return { cookies, secUid, nickname, avatar }
  } catch {
    return { cookies: '', secUid: null, nickname: null, avatar: null }
  }
}

/** 单次调用 Webcast API */
async function callWebcastApi(webRid: string, cookies: string): Promise<any | null> {
  const apiUrl =
    `https://live.douyin.com/webcast/room/web/enter/?web_rid=${webRid}` +
    `&aid=6383&app_name=douyin_web&live_id=1&device_platform=web` +
    `&language=zh-CN&browser_language=zh-CN&browser_platform=Win32` +
    `&browser_name=Chrome&browser_version=120.0.0.0`

  const resp = await axiosIPv4.get(apiUrl, {
    headers: {
      ...HEADERS,
      Accept: 'application/json, text/plain, */*',
      Cookie: cookies,
    },
    timeout: 10000,
    validateStatus: () => true,
  })

  let data = resp.data
  if (typeof data === 'string') {
    try {
      data = JSON.parse(data)
    } catch {
      // 返回了 HTML — 收集新 cookie 用于重试
      const newCookies = (resp.headers?.['set-cookie'] || []) as string[]
      if (newCookies.length > 0) {
        return { _needRetry: true, cookies: newCookies.map((c: string) => c.split(';')[0]).join('; ') }
      }
      return null
    }
  }

  if (data?.status_code !== 0) return null
  const rooms = data?.data?.data
  if (!Array.isArray(rooms) || rooms.length === 0) return null
  return rooms[0]
}

/** 调用 Webcast API 获取直播间原始数据（自动重试最多3次） */
async function tryWebcastApi(webRid: string): Promise<{
  room: any
  secUidFromHtml: string | null
  nicknameFromHtml: string | null
  avatarFromHtml: string | null
} | null> {
  const ttwidResult = await fetchTtwidAndSecUid(webRid)
  let cookies = ttwidResult.cookies
  const secUidFromHtml = ttwidResult.secUid
  const nicknameFromHtml = ttwidResult.nickname
  const avatarFromHtml = ttwidResult.avatar

  for (let attempt = 0; attempt < 3; attempt++) {
    try {
      const result = await callWebcastApi(webRid, cookies)
      if (!result) return null
      if (result._needRetry) {
        // API 返回 HTML，用新 cookie 重试
        cookies = cookies + '; ' + result.cookies
        console.log(`[DouyinLive] API returned HTML, retrying (${attempt + 1}/3)...`)
        await new Promise((r) => setTimeout(r, 1000))
        continue
      }
      return { room: result, secUidFromHtml, nicknameFromHtml, avatarFromHtml }
    } catch (e) {
      console.log(`[DouyinLive] webcast API error (${attempt + 1}/3):`, (e as Error).message)
      if (attempt < 2) await new Promise((r) => setTimeout(r, 1000))
    }
  }
  // API 全部失败，但仍返回从 HTML 提取的信息
  if (secUidFromHtml || nicknameFromHtml) {
    return { room: null, secUidFromHtml, nicknameFromHtml, avatarFromHtml }
  }
  return null
}

/** 从移动端页面提取主播信息、流地址和真实 web_rid（仅支持数字 room_id） */
async function tryMobilePage(
  roomId: string
): Promise<{ streamUrl: string | null; streamerName: string | null; streamerAvatar: string | null; webRid: string | null } | null> {
  if (!/^\d+$/.test(roomId)) return null

  const url = `https://webcast.amemv.com/douyin/webcast/reflow/${roomId}`
  try {
    const resp = await axiosIPv4.get(url, {
      headers: MOBILE_HEADERS,
      timeout: 15000,
    })
    const page: string = resp.data

    // 规范化转义
    const normalized = page.replace(/\\u0026/g, '&')

    // 提取 FLV 流地址
    const stop = `"'<>\\s\\\\`
    const flvRe = new RegExp(`(https?://pull-flv[^${stop}]+\\.flv[^${stop}]*)`, 'g')
    const hlsRe = new RegExp(`(https?://pull-hls[^${stop}]+\\.m3u8[^${stop}]*)`, 'g')

    let flvMatches = Array.from(normalized.matchAll(flvRe), (m) => m[1])
    let hlsMatches = Array.from(normalized.matchAll(hlsRe), (m) => m[1])

    // 过滤无效URL
    const filter = (u: string) => !u.includes('only_audio=1') && !u.includes('-admin.')
    flvMatches = flvMatches.filter(filter)
    hlsMatches = hlsMatches.filter(filter)

    // 优选带鉴权参数的URL
    const authKeys = ['wsSecret', 'keeptime', 'unique_id', 'sign=', 'expire=']
    const best = (urls: string[]): string | null => {
      const auth = urls.filter((u) => authKeys.some((k) => u.includes(k)))
      const pool = (auth.length > 0 ? auth : urls).sort((a, b) => b.length - a.length)
      return pool[0] || null
    }

    const streamUrl = flvMatches.length > 0 ? best(flvMatches) : best(hlsMatches)

    // 提取主播名（页面可能用 "nickname" 或 \"nickname\"）
    const nickMatch = page.match(/\\?"nickname\\?"\s*:\s*\\?"([^"\\]+)\\?"/) ||
                      page.match(/"nickname"\s*:\s*"([^"]+)"/)
    const streamerName = nickMatch ? nickMatch[1] : null

    // 提取主播头像（avatarThumb.urlList 中的第一个 URL）
    // 页面中引号可能是 \" 或 "，匹配两种格式
    const avatarMatch = page.match(/avatarThumb[^}]*urlList[^[]*\[\\?"(https?:\/\/[^"\\]+)/) ||
                        page.match(/avatar_thumb[^}]*url_list[^[]*\[\\?"(https?:\/\/[^"\\]+)/)
    const streamerAvatar = avatarMatch ? avatarMatch[1] : null

    // 提取真实 webRid（永久直播间标识，用于 Webcast API 查询）
    const webRidMatch = page.match(/webRid[\\?"':]*\s*[\\?"':]*\s*(\d+)/)
    const webRid = webRidMatch ? webRidMatch[1] : null

    // 即使没有流地址（未开播），只要有主播信息就返回
    if (!streamUrl && !streamerName) return null
    return { streamUrl, streamerName, streamerAvatar, webRid }
  } catch (e) {
    console.log(`[DouyinLive] mobile page error for ${roomId}:`, (e as Error).message)
    return null
  }
}

/** 从 room 对象中提取各画质的流地址 */
function extractStreamsFromRoom(room: any): Record<string, string> {
  const streams: Record<string, string> = {}
  const su = room?.stream_url || room?.StreamUrl
  if (!su || typeof su !== 'object') return streams

  // 优先 FLV
  const flv = su.flv_pull_url
  if (flv && typeof flv === 'object') {
    for (const quality of QUALITY_KEYS) {
      if (flv[quality]) streams[quality] = flv[quality]
    }
  }

  // FLV 没有则用 HLS
  if (Object.keys(streams).length === 0) {
    const hls = su.hls_pull_url_map || su.hls_pull_url
    if (hls && typeof hls === 'object') {
      for (const quality of QUALITY_KEYS) {
        if (hls[quality]) streams[quality] = hls[quality]
      }
    }
  }

  return streams
}

/** 根据画质偏好从 streams 中选择最佳流地址 */
function pickStreamUrl(streams: Record<string, string>, quality: string): string | null {
  if (streams[quality]) return streams[quality]
  for (const fallback of QUALITY_KEYS) {
    if (streams[fallback]) return streams[fallback]
  }
  return null
}

// ── Public API ──

/**
 * 获取抖音直播间信息
 *
 * @param input 直播间链接、短链接、分享文案或 web_rid
 * @param quality 画质偏好，默认 FULL_HD1（蓝光）
 */
export async function getLiveInfo(input: string, quality = 'FULL_HD1'): Promise<LiveInfo> {
  const result: LiveInfo = {
    isLive: false,
    streamUrl: null,
    streamerName: null,
    streamerAvatar: null,
    secUid: null,
    roomId: null,
    webRid: null,
    streams: {},
    error: null,
  }

  const webRid = await extractWebRid(input)
  if (!webRid) {
    result.error =
      '无法从输入中提取直播间标识。支持的格式：\n' +
      '  1. https://live.douyin.com/123456789\n' +
      '  2. https://live.douyin.com/DNX833 (别名)\n' +
      '  3. https://v.douyin.com/xxxx/ (抖音短链)\n' +
      '  4. 直接输入数字 ID 或别名'
    return result
  }

  result.webRid = webRid

  // 方式1（优先）：Webcast API
  const apiResult = await tryWebcastApi(webRid)
  if (apiResult) {
    // 始终记录从 HTML 提取的信息
    result.secUid = apiResult.secUidFromHtml || null

    if (apiResult.room) {
      const room = apiResult.room
      result.roomId = room.id_str || String(room.id || '')
      // 从 API 返回中提取真实 web_rid（确保用永久标识符，而不是自定义抖音号）
      if (room.web_rid && room.web_rid !== webRid) {
        result.webRid = room.web_rid
      }
      const owner = room.owner || {}
      // 主播名：API owner > HTML 页面 > 默认值
      result.streamerName = owner.nickname || apiResult.nicknameFromHtml || `直播间${webRid}`
      // sec_uid: 优先从 API owner 中取，降级从 HTML 页面中取
      result.secUid = owner.sec_uid || result.secUid
      // 提取主播头像：API owner > HTML 页面
      const avatarObj = owner.avatar_thumb || owner.avatar_medium || owner.avatar_large || {}
      const avatarUrls = avatarObj.url_list || []
      result.streamerAvatar = avatarUrls.length > 0 ? avatarUrls[0] : (apiResult.avatarFromHtml || null)

      if (room.status === 2) {
        result.isLive = true
        const streams = extractStreamsFromRoom(room)
        result.streams = streams
        result.streamUrl = pickStreamUrl(streams, quality)
      }
      return result
    }

    // API 没有返回 room，但 HTML 页面有主播信息（未开播场景）
    if (apiResult.nicknameFromHtml) {
      result.streamerName = apiResult.nicknameFromHtml
      result.streamerAvatar = apiResult.avatarFromHtml || null
      return result
    }
    // API 失败但有 secUid — 继续降级尝试
  }

  // 方式2（降级）：移动端页面（仅数字 room_id）
  if (/^\d+$/.test(webRid)) {
    result.roomId = webRid
    const mobileInfo = await tryMobilePage(webRid)
    if (mobileInfo) {
      // 用从 reflow 页面提取的真实 webRid 替换临时 room_id
      const realWebRid = (mobileInfo.webRid && mobileInfo.webRid !== webRid) ? mobileInfo.webRid : null
      if (realWebRid) {
        console.log(`[DouyinLive] Resolved real webRid: ${realWebRid} (from reflow room_id: ${webRid})`)
        result.webRid = realWebRid
      }
      result.streamerName = mobileInfo.streamerName || result.streamerName || `直播间${webRid}`
      result.streamerAvatar = mobileInfo.streamerAvatar || result.streamerAvatar || null
      if (mobileInfo.streamUrl) {
        result.isLive = true
        result.streamUrl = mobileInfo.streamUrl
      }
      // 用真实 webRid 再调一次 Webcast API，获取 secUid 和精确的开播状态
      if (realWebRid) {
        const apiResult2 = await tryWebcastApi(realWebRid)
        if (apiResult2?.room) {
          const room2 = apiResult2.room
          const owner2 = room2.owner || {}
          result.secUid = owner2.sec_uid || result.secUid
          result.streamerName = owner2.nickname || apiResult2.nicknameFromHtml || result.streamerName
          const av2 = owner2.avatar_thumb || owner2.avatar_medium || owner2.avatar_large || {}
          result.streamerAvatar = (av2.url_list || [])[0] || apiResult2.avatarFromHtml || result.streamerAvatar
          if (room2.status === 2) {
            result.isLive = true
            const streams = extractStreamsFromRoom(room2)
            result.streams = streams
            result.streamUrl = pickStreamUrl(streams, quality)
          }
        } else if (apiResult2) {
          result.secUid = apiResult2.secUidFromHtml || result.secUid
          result.streamerName = apiResult2.nicknameFromHtml || result.streamerName
          result.streamerAvatar = apiResult2.avatarFromHtml || result.streamerAvatar
        }
      }
      return result
    }
  }

  // 都失败了
  if (!result.streamerName) {
    result.error = '无法获取直播间数据，请确认直播间链接正确'
  }
  return result
}

/**
 * 录制前刷新流地址（URL 会过期，必须在开始录制前重新获取）
 */
export async function refreshStreamUrl(
  webRid: string,
  quality = 'FULL_HD1'
): Promise<string | null> {
  const apiResult = await tryWebcastApi(webRid)
  if (apiResult?.room && apiResult.room.status === 2) {
    const streams = extractStreamsFromRoom(apiResult.room)
    return pickStreamUrl(streams, quality)
  }
  return null
}
