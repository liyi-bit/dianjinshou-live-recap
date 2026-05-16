import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'
import * as recordingApi from '@/api/recording'
import type { Recording, RecordingQuery } from '@/api/recording'

export const useRecapStore = defineStore('recap', () => {
  const list = ref<Recording[]>([])
  const total = ref(0)
  const totalAll = ref(0)
  const loading = ref(false)
  const query = reactive<RecordingQuery>({
    page: 1,
    size: 10,
    tab: 'ALL',
    analysisStatus: undefined,
    startDate: undefined,
    endDate: undefined,
  })

  async function fetchList() {
    loading.value = true
    try {
      const res = await recordingApi.getRecordings(query)
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

  async function fetchTotalAll() {
    try {
      const res = await recordingApi.getRecordings({ page: 1, size: 1, tab: 'ALL' })
      const data = (res as any).data ?? res
      totalAll.value = data.total || 0
    } catch {
      totalAll.value = 0
    }
  }

  async function renameRecording(id: number, name: string) {
    await recordingApi.renameRecording(id, name)
    await fetchList()
  }

  async function batchDelete(ids: number[]) {
    await recordingApi.batchDeleteRecordings(ids)
    await fetchList()
  }

  function setStreamerId(streamerId: number | undefined) {
    query.streamerId = streamerId
    query.page = 1
    fetchList()
  }

  function setTab(tab: string) {
    query.tab = tab
    query.page = 1
    fetchList()
  }

  function setFilters(filters: { analysisStatus?: string; startDate?: string; endDate?: string }) {
    query.analysisStatus = filters.analysisStatus || undefined
    query.startDate = filters.startDate || undefined
    query.endDate = filters.endDate || undefined
    query.page = 1
    fetchList()
  }

  function setPage(page: number) {
    query.page = page
    fetchList()
  }

  /** 重置筛选条件到默认（用户重新进入列表页时调用） */
  function resetQuery() {
    query.page = 1
    query.size = 10
    query.tab = 'ALL'
    query.analysisStatus = undefined
    query.status = undefined
    query.startDate = undefined
    query.endDate = undefined
    query.streamerId = undefined
  }

  return {
    list,
    total,
    totalAll,
    loading,
    query,
    fetchList,
    fetchTotalAll,
    renameRecording,
    batchDelete,
    setStreamerId,
    setTab,
    setFilters,
    setPage,
    resetQuery
  }
})
