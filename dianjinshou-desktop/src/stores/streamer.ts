import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'
import * as streamerApi from '@/api/streamer'
import type { Streamer, StreamerStats, StreamerQuery } from '@/api/streamer'

export const useStreamerStore = defineStore('streamer', () => {
  const list = ref<Streamer[]>([])
  // 不分页的全量主播列表 —— 用于"筛选主播"下拉，避免被分页限制到只能选前 20 个
  const allStreamers = ref<Streamer[]>([])
  const total = ref(0)
  const liveStateMap = ref<Record<string, boolean>>({})
  const stats = ref<StreamerStats>({
    total: 0,
    monitoring: 0,
    recording: 0,
    ownCount: 0,
    competitorCount: 0,
    industryCount: 0
  })
  const loading = ref(false)
  const query = reactive<StreamerQuery>({
    page: 1,
    size: 20,
    keyword: '',
    accountType: '',
    platform: ''
  })

  async function fetchList() {
    loading.value = true
    try {
      const res = await streamerApi.getStreamers(query)
      const data = (res as any).data ?? res
      list.value = data.items || []
      total.value = data.total || 0
    } catch {
      list.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
  }

  /** 拉全量主播（size=1000）用于筛选下拉，不修改 query/list */
  async function fetchAllStreamers() {
    try {
      const res = await streamerApi.getStreamers({
        page: 1,
        size: 1000,
        keyword: '',
        accountType: '',
        platform: '',
      } as StreamerQuery)
      const data = (res as any).data ?? res
      allStreamers.value = data.items || []
    } catch {
      allStreamers.value = []
    }
  }

  async function fetchStats() {
    try {
      const res = await streamerApi.getStreamerStats()
      const data = (res as any).data ?? res
      stats.value = data
    } catch {
      // keep defaults
    }
  }

  function updateLiveStates(streamers: Array<{ streamerId: string | number; isLive: boolean }> | undefined) {
    if (!streamers) return
    const map: Record<string, boolean> = { ...liveStateMap.value }
    for (const s of streamers) {
      map[String(s.streamerId)] = s.isLive
    }
    liveStateMap.value = map
  }

  async function removeStreamer(id: number) {
    await streamerApi.deleteStreamer(id)
    await fetchList()
    await fetchAllStreamers()
    await fetchStats()
  }

  /** 重置筛选条件到默认（用户重新进入列表页时调用，避免上次筛选缓存） */
  function resetQuery() {
    query.page = 1
    query.size = 20
    query.keyword = ''
    query.accountType = ''
    query.platform = ''
    query.isMonitoring = undefined
  }

  async function toggleMonitor(id: number, start: boolean) {
    if (start) {
      await streamerApi.startMonitor(id)
    } else {
      await streamerApi.stopMonitor(id)
    }
    await fetchList()
    await fetchAllStreamers()
    await fetchStats()
  }

  async function toggleCloudSync(id: number, enabled: boolean) {
    await streamerApi.updateStreamer(id, { cloudSyncEnabled: enabled })
    await fetchList()
    await fetchAllStreamers()
  }

  return {
    list,
    allStreamers,
    total,
    liveStateMap,
    stats,
    loading,
    query,
    fetchList,
    fetchAllStreamers,
    fetchStats,
    updateLiveStates,
    removeStreamer,
    toggleMonitor,
    toggleCloudSync,
    resetQuery
  }
})
