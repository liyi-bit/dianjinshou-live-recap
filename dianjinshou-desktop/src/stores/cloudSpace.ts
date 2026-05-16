import { ref } from 'vue'
import { defineStore } from 'pinia'
import * as api from '@/api/cloudSpace'
import type { CloudCompareMode, CloudFileItem, CloudListParams, CloudUsage, CloudView } from '@/api/cloudSpace'

export const useCloudSpaceStore = defineStore('cloudSpace', () => {
  const files = ref<CloudFileItem[]>([])
  const total = ref(0)
  const loading = ref(false)
  const usage = ref<CloudUsage | null>(null)
  const selectedIds = ref<number[]>([])

  async function fetchList(view: CloudView, mode: CloudCompareMode, params: CloudListParams) {
    loading.value = true
    try {
      let res: any
      if (view === 'full') {
        res = await api.listFullRecaps(params)
      } else if (view === 'clip') {
        res = await api.listClipRecaps(params)
      } else {
        res = await api.listComparisons(mode, params)
      }
      const data = (res as any).data ?? res
      files.value = data.records || []
      total.value = data.total || 0
    } finally {
      loading.value = false
    }
  }

  async function fetchUsage() {
    const res = await api.getCloudUsage()
    usage.value = (res as any).data ?? res
  }

  async function fetchFiles(fileType: 'recording' | 'clip' | 'document' | string, page = 1, size = 20) {
    loading.value = true
    try {
      let res: any
      if (fileType === 'clip') {
        res = await api.listClips(page, size)
      } else if (fileType === 'document') {
        res = await api.listDocuments(page, size)
      } else {
        res = await api.listRecordings(page, size)
      }
      const data = res?.data ?? res
      files.value = data.records || []
      total.value = data.total || 0
    } finally {
      loading.value = false
    }
  }

  async function renameFile(id: number, displayName: string) {
    await api.renameCloudFile(id, displayName)
  }

  async function deleteFile(id: number) {
    await api.deleteCloudFile(id)
  }

  async function batchDelete(ids: number[]) {
    await api.batchDeleteFiles(ids)
    selectedIds.value = []
  }

  async function signedUrl(id: number) {
    const res = await api.getSignedUrl(id)
    return (res as any).data ?? res
  }

  async function batchDownload(ids: number[]) {
    const res = await api.batchDownloadFiles(ids)
    return (res as any).data ?? res
  }

  async function openTarget(id: number) {
    const res = await api.getOpenTarget(id)
    return (res as any).data ?? res
  }

  return {
    files,
    total,
    loading,
    usage,
    selectedIds,
    fetchList,
    fetchFiles,
    fetchUsage,
    renameFile,
    deleteFile,
    batchDelete,
    signedUrl,
    batchDownload,
    openTarget,
  }
})
