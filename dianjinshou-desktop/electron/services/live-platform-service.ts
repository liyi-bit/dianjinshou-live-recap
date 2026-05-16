import { getLiveInfo as getDouyinLiveInfo, refreshStreamUrl as refreshDouyinStreamUrl } from './douyin-live-service'
import { getKuaishouLiveInfo, refreshKuaishouStreamUrl } from './kuaishou-live-service'

export type RecordingResolution = '480p' | '720p' | '1080p' | 'source'

export interface PlatformLiveInfo {
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

export function buildRoomUrl(platform: string | undefined, accountId: string | null | undefined): string {
  const id = (accountId || '').trim()
  if (!id) return ''
  if (platform === 'douyin') return `https://live.douyin.com/${id}`
  if (platform === 'kuaishou') return `https://live.kuaishou.com/u/${id}`
  return ''
}

function resolutionToDouyinQuality(resolution: RecordingResolution | string | undefined): string {
  switch (resolution) {
    case '480p':
      return 'SD2'
    case '720p':
      return 'SD1'
    case '1080p':
      return 'HD1'
    case 'source':
    default:
      return 'FULL_HD1'
  }
}

function resolutionToKuaishouQuality(resolution: RecordingResolution | string | undefined): string {
  switch (resolution) {
    case '480p':
      return '480p'
    case '720p':
      return '720p'
    case '1080p':
      return '1080p'
    case 'source':
    default:
      return 'source'
  }
}

export function resolutionToPlatformQuality(
  platform: string | undefined,
  resolution: RecordingResolution | string | undefined
): string {
  if (platform === 'kuaishou') return resolutionToKuaishouQuality(resolution)
  return resolutionToDouyinQuality(resolution)
}

export async function getLiveInfoByPlatform(
  platform: string | undefined,
  input: string,
  resolution: RecordingResolution | string = 'source'
): Promise<PlatformLiveInfo> {
  if (platform === 'kuaishou') {
    const info = await getKuaishouLiveInfo(input, resolutionToKuaishouQuality(resolution))
    return {
      isLive: info.isLive,
      streamUrl: info.streamUrl,
      streamerName: info.streamerName,
      streamerAvatar: info.streamerAvatar,
      secUid: null,
      roomId: info.roomId,
      webRid: info.webRid,
      streams: info.streams,
      error: info.error,
    }
  }

  if (platform === 'douyin' || !platform) {
    const info = await getDouyinLiveInfo(input, resolutionToDouyinQuality(resolution))
    return {
      isLive: info.isLive,
      streamUrl: info.streamUrl,
      streamerName: info.streamerName,
      streamerAvatar: info.streamerAvatar,
      secUid: info.secUid,
      roomId: info.roomId,
      webRid: info.webRid,
      streams: info.streams,
      error: info.error,
    }
  }

  return {
    isLive: false,
    streamUrl: null,
    streamerName: null,
    streamerAvatar: null,
    secUid: null,
    roomId: null,
    webRid: null,
    streams: {},
    error: `Unsupported live platform: ${platform}`,
  }
}

export async function refreshStreamUrlByPlatform(
  platform: string | undefined,
  input: string,
  resolution: RecordingResolution | string = 'source'
): Promise<string | null> {
  if (platform === 'kuaishou') {
    return refreshKuaishouStreamUrl(input, resolutionToKuaishouQuality(resolution))
  }
  return refreshDouyinStreamUrl(input, resolutionToDouyinQuality(resolution))
}
