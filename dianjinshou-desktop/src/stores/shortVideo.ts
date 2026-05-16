import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  listCopywriting,
  extractCopywriting,
  deleteCopywriting,
  recordCopy,
  searchCreators,
  listSubscriptions,
  type VideoCopywritingItem,
  type CreatorItem
} from '@/api/shortVideo'
import { Message } from '@arco-design/web-vue'

export const useShortVideoStore = defineStore('shortVideo', () => {
  // Copywriting
  const copywritingList = ref<VideoCopywritingItem[]>([])
  const copywritingTotal = ref(0)
  const copywritingLoading = ref(false)

  // Creators
  const creatorList = ref<CreatorItem[]>([])
  const creatorLoading = ref(false)

  // Subscriptions
  const subscriptions = ref<any>(null)

  async function fetchCopywriting(page = 1, size = 20, status?: string) {
    copywritingLoading.value = true
    try {
      const res = await listCopywriting(page, size, status)
      const data = (res as any).data ?? res
      copywritingList.value = data.records || []
      copywritingTotal.value = data.total || 0
    } catch {
      copywritingList.value = []
      copywritingTotal.value = 0
    } finally {
      copywritingLoading.value = false
    }
  }

  async function createExtract(params: { sourceType: string; sourceUrl?: string; storageKey?: string; title?: string }) {
    try {
      const res = await extractCopywriting(params)
      const data = (res as any).data ?? res
      Message.success('文案提取任务已创建')
      return data
    } catch {
      Message.error('创建提取任务失败')
      return null
    }
  }

  async function removeCopywriting(id: number) {
    try {
      await deleteCopywriting(id)
      Message.success('已删除')
      return true
    } catch {
      Message.error('删除失败')
      return false
    }
  }

  async function doCopy(id: number) {
    try {
      await recordCopy(id)
      Message.success('已复制')
    } catch {
      // silent
    }
  }

  async function fetchCreators(params: { keyword?: string; platform?: string; industry?: string }) {
    creatorLoading.value = true
    try {
      const res = await searchCreators({ ...params, page: 1, size: 20 })
      creatorList.value = (res as any).data ?? res
    } catch {
      creatorList.value = []
    } finally {
      creatorLoading.value = false
    }
  }

  async function fetchSubscriptions() {
    try {
      const res = await listSubscriptions()
      subscriptions.value = (res as any).data ?? res
    } catch {
      subscriptions.value = null
    }
  }

  return {
    copywritingList,
    copywritingTotal,
    copywritingLoading,
    creatorList,
    creatorLoading,
    subscriptions,
    fetchCopywriting,
    createExtract,
    removeCopywriting,
    doCopy,
    fetchCreators,
    fetchSubscriptions
  }
})
