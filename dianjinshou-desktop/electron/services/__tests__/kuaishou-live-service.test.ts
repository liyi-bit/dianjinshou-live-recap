import { describe, expect, it } from 'vitest'
import {
  extractKuaishouAccountId,
  parseKuaishouInitialState,
  pickKuaishouStreamUrl,
} from '../kuaishou-live-service'

describe('kuaishou-live-service', () => {
  it('extracts account ids from direct ids and live URLs', async () => {
    await expect(extractKuaishouAccountId('KPL704668133')).resolves.toBe('KPL704668133')
    await expect(extractKuaishouAccountId('https://live.kuaishou.com/u/yall1102')).resolves.toBe('yall1102')
    await expect(
      extractKuaishouAccountId('watch this https://live.kuaishou.com/u/demo_id?fid=share')
    ).resolves.toBe('demo_id')
  })

  it('parses the live payload from window initial state', () => {
    const payload = {
      route: {
        author: { name: 'demo-anchor' },
        liveStream: {
          playUrls: {
            h264: {
              adaptationSet: {
                representation: [
                  { url: 'https://ali.hlspull.yximgs.com/live/demo_low.flv', bitrate: 600 },
                  { url: 'https://ali.hlspull.yximgs.com/live/demo_hd.flv', bitrate: 1000 },
                ],
              },
            },
          },
        },
      },
    }
    const html = `<script>window.__INITIAL_STATE__=${JSON.stringify(payload)};(function(){var s;</script>`

    const parsed = parseKuaishouInitialState(html)

    expect(parsed?.author?.name).toBe('demo-anchor')
    expect(parsed?.liveStream?.playUrls?.h264?.adaptationSet?.representation).toHaveLength(2)
  })

  it('parses initial state with undefined fields from Kuaishou pages', () => {
    const html = `<script>window.__INITIAL_STATE__={"route":{"author":{"name":"三角洲行动赛事","avatar":"https://p.example.com/avatar.jpg"},"liveStream":{"id":"live-demo"},"authToken":undefined}};(function(){var s;</script>`

    const parsed = parseKuaishouInitialState(html)

    expect(parsed?.author?.name).toBe('三角洲行动赛事')
    expect(parsed?.author?.avatar).toBe('https://p.example.com/avatar.jpg')
    expect(parsed?.liveStream?.id).toBe('live-demo')
  })

  it('selects the closest stream by requested quality', () => {
    const candidates = [
      { url: 'source.flv', bitrate: 2500, kind: 'flv' as const },
      { url: 'hd.flv', bitrate: 1000, kind: 'flv' as const },
      { url: 'sd.flv', bitrate: 600, kind: 'flv' as const },
    ]

    expect(pickKuaishouStreamUrl(candidates, 'source')?.url).toBe('source.flv')
    expect(pickKuaishouStreamUrl(candidates, '720p')?.url).toBe('hd.flv')
    expect(pickKuaishouStreamUrl(candidates, '480p')?.url).toBe('sd.flv')
  })
})
