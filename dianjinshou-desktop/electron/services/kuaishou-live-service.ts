import dns from 'dns'
import http from 'http'
import https from 'https'
import axios from 'axios'

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

export interface KuaishouLiveInfo {
  isLive: boolean
  streamUrl: string | null
  streamerName: string | null
  streamerAvatar: string | null
  accountId: string | null
  roomId: string | null
  webRid: string | null
  streams: Record<string, string>
  error: string | null
}

interface StreamCandidate {
  url: string
  bitrate: number
  kind: 'flv' | 'hls'
}

const PC_HEADERS = {
  'User-Agent':
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/115.0',
  'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
  Referer: 'https://live.kuaishou.com/',
}

const MOBILE_HEADERS = {
  'User-Agent':
    'Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1',
  'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
  Referer: 'https://m.gifshow.com/',
}

const QUALITY_BITRATE: Record<string, number> = {
  OD: Number.POSITIVE_INFINITY,
  BD: 4000,
  UHD: 2000,
  HD: 1000,
  SD: 800,
  LD: 600,
  FULL_HD1: Number.POSITIVE_INFINITY,
  HD1: 2000,
  SD1: 1000,
  SD2: 800,
  source: Number.POSITIVE_INFINITY,
  '1080p': 2000,
  '720p': 1000,
  '480p': 800,
}

function cleanStreamUrl(url: string): string {
  return url
    .replace(/\\u0026/g, '&')
    .replace(/&amp;/g, '&')
    .trim()
}

function extractUrlFromText(text: string): string {
  const match = text.match(/https?:\/\/[^\s<>"']+/)
  return (match ? match[0] : text).trim().replace(/[),.;]+$/, '')
}

function parseKuaishouIdFromUrl(input: string): string | null {
  const patterns = [
    /live\.kuaishou\.com\/u\/([^/?#\s]+)/,
    /m\.gifshow\.com\/fw\/live\/([^/?#\s]+)/,
    /kuaishou\.com\/profile\/([^/?#\s]+)/,
  ]
  for (const pattern of patterns) {
    const match = input.match(pattern)
    if (match?.[1]) return decodeURIComponent(match[1])
  }
  return null
}

async function resolveShortUrl(url: string): Promise<string | null> {
  try {
    const resp = await axiosIPv4.get(url, {
      headers: PC_HEADERS,
      maxRedirects: 8,
      timeout: 10000,
      responseType: 'text',
      transformResponse: (data) => data,
      validateStatus: () => true,
      maxContentLength: 512 * 1024,
    })
    return resp.request?.res?.responseUrl || resp.config?.url || null
  } catch {
    return null
  }
}

export async function extractKuaishouAccountId(input: string): Promise<string | null> {
  const clean = extractUrlFromText(input)
  const fromUrl = parseKuaishouIdFromUrl(clean)
  if (fromUrl) return fromUrl

  if (/^https?:\/\//.test(clean)) {
    const resolved = await resolveShortUrl(clean)
    if (resolved) {
      const fromResolved = parseKuaishouIdFromUrl(resolved)
      if (fromResolved) return fromResolved
    }
    return null
  }

  const direct = clean.trim()
  if (/^[A-Za-z0-9_.-]{2,80}$/.test(direct)) return direct
  return null
}

function findLivePayload(node: unknown, seen = new Set<unknown>()): any | null {
  if (!node || typeof node !== 'object' || seen.has(node)) return null
  seen.add(node)

  const obj = node as Record<string, unknown>
  if ('liveStream' in obj) return obj

  if (Array.isArray(node)) {
    for (const item of node) {
      const found = findLivePayload(item, seen)
      if (found) return found
    }
    return null
  }

  for (const value of Object.values(obj)) {
    const found = findLivePayload(value, seen)
    if (found) return found
  }
  return null
}

export function parseKuaishouInitialState(html: string): any | null {
  const match =
    html.match(/<script>\s*window\.__INITIAL_STATE__=([\s\S]*?);\(function\(\)\{var s;/) ||
    html.match(/window\.__INITIAL_STATE__=([\s\S]*?);<\/script>/)
  if (!match?.[1]) return null

  try {
    return findLivePayload(JSON.parse(normalizeInitialStateLiteral(match[1])))
  } catch {
    return null
  }
}

function normalizeInitialStateLiteral(input: string): string {
  let output = ''
  let quote: '"' | "'" | null = null
  let escaped = false

  for (let index = 0; index < input.length; index++) {
    const char = input[index]

    if (quote) {
      output += char
      if (escaped) {
        escaped = false
      } else if (char === '\\') {
        escaped = true
      } else if (char === quote) {
        quote = null
      }
      continue
    }

    if (char === '"' || char === "'") {
      quote = char
      output += char
      continue
    }

    if (input.startsWith('undefined', index)) {
      const prev = input[index - 1]
      const next = input[index + 'undefined'.length]
      const beforeBoundary = !prev || !/[A-Za-z0-9_$]/.test(prev)
      const afterBoundary = !next || !/[A-Za-z0-9_$]/.test(next)
      if (beforeBoundary && afterBoundary) {
        output += 'null'
        index += 'undefined'.length - 1
        continue
      }
    }

    output += char
  }

  return output
}

function normalizeBitrate(value: unknown): number {
  const bitrate = Number(value)
  if (!Number.isFinite(bitrate) || bitrate <= 0) return 0
  return bitrate > 100000 ? Math.round(bitrate / 1000) : bitrate
}

function asArray(value: unknown): any[] {
  if (!value) return []
  return Array.isArray(value) ? value : [value]
}

function readUrl(value: any): string | null {
  if (!value) return null
  if (typeof value === 'string') return cleanStreamUrl(value)
  if (typeof value.url === 'string') return cleanStreamUrl(value.url)
  if (typeof value.playUrl === 'string') return cleanStreamUrl(value.playUrl)
  if (typeof value.href === 'string') return cleanStreamUrl(value.href)
  const nested = asArray(value.urls || value.urlList || value.playUrls)
  for (const item of nested) {
    const url = readUrl(item)
    if (url) return url
  }
  return null
}

function toCandidate(value: any, kind: 'flv' | 'hls'): StreamCandidate | null {
  const url = readUrl(value)
  if (!url || url.includes('only_audio=1')) return null
  return {
    url,
    kind,
    bitrate: normalizeBitrate(value?.bitrate || value?.bandwidth || value?.videoBitrate),
  }
}

function collectRepresentationCandidates(liveStream: any): StreamCandidate[] {
  const playUrls = liveStream?.playUrls
  if (!playUrls) return []

  const candidates: StreamCandidate[] = []
  const preferred = playUrls?.h264?.adaptationSet?.representation
  for (const rep of asArray(preferred)) {
    const item = toCandidate(rep, 'flv')
    if (item) candidates.push(item)
  }

  if (candidates.length > 0) return candidates

  const pools = Array.isArray(playUrls) ? playUrls : Object.values(playUrls)
  for (const pool of pools) {
    const reps = (pool as any)?.adaptationSet?.representation
    for (const rep of asArray(reps)) {
      const item = toCandidate(rep, 'flv')
      if (item) candidates.push(item)
    }
  }
  return candidates
}

function collectHlsCandidates(liveStream: any): StreamCandidate[] {
  const candidates: StreamCandidate[] = []
  const hlsPools = [
    liveStream?.multiResolutionHlsPlayUrls,
    liveStream?.hlsPlayUrls,
    liveStream?.hlsPlayUrl,
  ]

  for (const pool of hlsPools) {
    for (const group of asArray(pool)) {
      const urls = asArray(group?.urls || group)
      for (const entry of urls) {
        const item = toCandidate(entry, 'hls')
        if (item) candidates.push(item)
      }
    }
  }
  return candidates
}

export function pickKuaishouStreamUrl(
  candidates: StreamCandidate[],
  quality = 'source'
): StreamCandidate | null {
  if (candidates.length === 0) return null

  const sorted = [...candidates].sort((a, b) => {
    if (b.bitrate !== a.bitrate) return b.bitrate - a.bitrate
    if (a.kind !== b.kind) return a.kind === 'flv' ? -1 : 1
    return 0
  })

  const target = QUALITY_BITRATE[String(quality)] ?? QUALITY_BITRATE.source
  if (!Number.isFinite(target)) return sorted[0]

  const byBitrate = sorted.find((item) => item.bitrate > 0 && item.bitrate <= target)
  return byBitrate || sorted[sorted.length - 1] || sorted[0]
}

function extractAvatar(author: any): string | null {
  return readUrl(author?.avatar || author?.avatarUrl || author?.headUrl || author?.headerUrl)
}

function buildResult(accountId: string | null): KuaishouLiveInfo {
  return {
    isLive: false,
    streamUrl: null,
    streamerName: null,
    streamerAvatar: null,
    accountId,
    roomId: accountId,
    webRid: accountId,
    streams: {},
    error: null,
  }
}

function parsePcLiveInfo(html: string, accountId: string, quality: string): KuaishouLiveInfo {
  const result = buildResult(accountId)
  const payload = parseKuaishouInitialState(html)
  if (!payload) {
    result.error = 'Unable to parse Kuaishou live page'
    return result
  }

  if (payload.errorType) {
    result.error = [payload.errorType.title, payload.errorType.content].filter(Boolean).join(' ')
    return result
  }

  const author = payload.author || payload.user || {}
  result.streamerName = author.name || author.username || author.userName || accountId
  result.streamerAvatar = extractAvatar(author)

  const liveStream = payload.liveStream
  if (!liveStream) return result

  const flvCandidates = collectRepresentationCandidates(liveStream)
  const hlsCandidates = collectHlsCandidates(liveStream)
  const candidates = flvCandidates.length > 0 ? flvCandidates : hlsCandidates
  const picked = pickKuaishouStreamUrl(candidates, quality)
  if (!picked) return result

  result.isLive = true
  result.streamUrl = picked.url
  result.streams = Object.fromEntries(candidates.map((item, index) => [`${item.kind}_${index}`, item.url]))
  return result
}

function randomDid(): string {
  const suffix = Array.from({ length: 32 }, () => Math.floor(Math.random() * 16).toString(16)).join('')
  return `web_${suffix}`
}

async function tryMobileFallback(accountId: string, quality: string): Promise<KuaishouLiveInfo | null> {
  try {
    const resp = await axiosIPv4.get(`https://m.gifshow.com/fw/live/${encodeURIComponent(accountId)}`, {
      headers: {
        ...MOBILE_HEADERS,
        Cookie: `did=${randomDid()}`,
      },
      timeout: 10000,
      responseType: 'text',
      transformResponse: (data) => data,
      validateStatus: () => true,
      maxContentLength: 1024 * 1024,
    })
    const html = typeof resp.data === 'string' ? resp.data : ''
    const match = html.match(/liveStream":(.*?),"obfuseData/)
    if (!match?.[1]) return null
    const liveStream = JSON.parse(match[1])
    const result = buildResult(accountId)
    const candidates = collectHlsCandidates(liveStream)
    const picked = pickKuaishouStreamUrl(candidates, quality)
    if (!picked) return result
    result.isLive = true
    result.streamUrl = picked.url
    result.streams = Object.fromEntries(candidates.map((item, index) => [`${item.kind}_${index}`, item.url]))
    return result
  } catch {
    return null
  }
}

export async function getKuaishouLiveInfo(input: string, quality = 'source'): Promise<KuaishouLiveInfo> {
  const accountId = await extractKuaishouAccountId(input)
  const result = buildResult(accountId)
  if (!accountId) {
    result.error = 'Unable to extract Kuaishou account id from input'
    return result
  }

  const url = `https://live.kuaishou.com/u/${encodeURIComponent(accountId)}`
  try {
    const resp = await axiosIPv4.get(url, {
      headers: PC_HEADERS,
      timeout: 12000,
      responseType: 'text',
      transformResponse: (data) => data,
      validateStatus: () => true,
      maxContentLength: 2 * 1024 * 1024,
    })
    const html = typeof resp.data === 'string' ? resp.data : ''
    const pcInfo = parsePcLiveInfo(html, accountId, quality)
    if (pcInfo.streamerName || pcInfo.isLive) return pcInfo
  } catch (err) {
    console.log('[KuaishouLive] PC page fetch failed:', (err as Error).message)
  }

  const fallback = await tryMobileFallback(accountId, quality)
  if (fallback) return fallback

  return result
}

export async function refreshKuaishouStreamUrl(input: string, quality = 'source'): Promise<string | null> {
  const info = await getKuaishouLiveInfo(input, quality)
  return info.isLive ? info.streamUrl : null
}
