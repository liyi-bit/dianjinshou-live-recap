import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as comparisonApi from '@/api/comparison'
import type { ComparisonDraft, ComparisonItem } from '@/api/comparison'
import { Message } from '@arco-design/web-vue'

export const useComparisonStore = defineStore('comparison', () => {
  const draft = ref<ComparisonDraft | null>(null)
  const list = ref<ComparisonItem[]>([])
  const total = ref(0)
  const loading = ref(false)

  async function fetchDraft() {
    try {
      const res = await comparisonApi.getCurrentDraft()
      draft.value = (res as any).data ?? res ?? null
    } catch {
      draft.value = null
    }
  }

  async function lockFirst(recordingId: number, listContext: string) {
    try {
      const res = await comparisonApi.createDraft({ firstRecordingId: recordingId, listContext })
      draft.value = (res as any).data ?? res
      return draft.value
    } catch {
      Message.error('锁定第一方失败')
      return null
    }
  }

  async function selectSecondAndConfirm(secondRecordingId: number, listContext: string) {
    if (!draft.value) return null
    try {
      const res = await comparisonApi.selectSecond(draft.value.id, { secondRecordingId, listContext })
      draft.value = null
      const data = (res as any).data ?? res
      return data as ComparisonItem
    } catch {
      Message.error('创建对比失败')
      return null
    }
  }

  async function cancel() {
    try {
      await comparisonApi.cancelDraft()
      draft.value = null
    } catch {
      Message.error('取消失败')
    }
  }

  async function fetchList(type?: string, page = 1, size = 10, filters?: { status?: string; startDate?: string; endDate?: string }) {
    loading.value = true
    try {
      const res = await comparisonApi.getComparisons({ type, page, size, ...filters })
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

  async function swap(id: number) {
    try {
      await comparisonApi.swapComparison(id)
      Message.success('已变换定位')
      return true
    } catch {
      Message.error('变换定位失败')
      return false
    }
  }

  async function batchDelete(ids: number[]) {
    try {
      await comparisonApi.batchDeleteComparisons(ids)
      Message.success('删除成功')
      return true
    } catch {
      Message.error('删除失败')
      return false
    }
  }

  return {
    draft, list, total, loading,
    fetchDraft, lockFirst, selectSecondAndConfirm, cancel,
    fetchList, swap, batchDelete
  }
})
