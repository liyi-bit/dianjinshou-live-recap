import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  listShortClips,
  createShortClip,
  deleteShortClip,
  batchExportClips,
  uploadClipToCloud,
  type ShortClipItem,
  type CreateShortClipParams
} from '@/api/shortClip'
import { Message } from '@arco-design/web-vue'

export const useShortClipStore = defineStore('shortClip', () => {
  const list = ref<ShortClipItem[]>([])
  const total = ref(0)
  const currentPage = ref(1)
  const loading = ref(false)
  const selectedIds = ref<number[]>([])

  async function fetchList(page = 1, size = 20, recordingId?: number, status?: string) {
    loading.value = true
    try {
      const res = await listShortClips(page, size, recordingId, status)
      const data = (res as any).data ?? res
      list.value = data.records || []
      total.value = data.total || 0
      currentPage.value = page
    } catch {
      list.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
  }

  async function create(params: CreateShortClipParams) {
    try {
      const res = await createShortClip(params)
      const data = (res as any).data ?? res
      Message.success('切片任务已创建')
      return data
    } catch {
      Message.error('创建切片失败')
      return null
    }
  }

  async function remove(id: number) {
    try {
      await deleteShortClip(id)
      Message.success('已删除')
      return true
    } catch {
      Message.error('删除失败')
      return false
    }
  }

  async function batchExport() {
    if (selectedIds.value.length === 0) {
      Message.warning('请先选择切片')
      return null
    }
    try {
      const res = await batchExportClips(selectedIds.value)
      const data = (res as any).data ?? res
      Message.success('批量导出已发起')
      return data.exportKey
    } catch {
      Message.error('批量导出失败')
      return null
    }
  }

  async function uploadToCloud(id: number) {
    try {
      await uploadClipToCloud(id)
      Message.success('已上传至云端')
      return true
    } catch {
      Message.error('上传云端失败')
      return false
    }
  }

  return {
    list,
    total,
    currentPage,
    loading,
    selectedIds,
    fetchList,
    create,
    remove,
    batchExport,
    uploadToCloud
  }
})
