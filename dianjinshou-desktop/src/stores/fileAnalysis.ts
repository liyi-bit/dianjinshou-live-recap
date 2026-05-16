import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  listFileAnalyses,
  getFileAnalysis,
  deleteFileAnalysis,
  createFileAnalysis,
  type FileAnalysisTask,
  type CreateFileAnalysisParams
} from '@/api/fileAnalysis'
import { Message } from '@arco-design/web-vue'

export const useFileAnalysisStore = defineStore('fileAnalysis', () => {
  const list = ref<FileAnalysisTask[]>([])
  const total = ref(0)
  const currentPage = ref(1)
  const loading = ref(false)
  const currentTask = ref<FileAnalysisTask | null>(null)

  async function fetchList(page = 1, size = 20, status?: string, keyword?: string) {
    loading.value = true
    try {
      const res = await listFileAnalyses(page, size, status, keyword)
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

  async function fetchDetail(id: number) {
    try {
      const res = await getFileAnalysis(id)
      currentTask.value = (res as any).data ?? res
      return currentTask.value
    } catch {
      currentTask.value = null
      return null
    }
  }

  async function create(params: CreateFileAnalysisParams) {
    try {
      const res = await createFileAnalysis(params)
      const data = (res as any).data ?? res
      Message.success('分析任务创建成功')
      return data
    } catch {
      Message.error('创建分析任务失败')
      return null
    }
  }

  async function remove(id: number) {
    try {
      await deleteFileAnalysis(id)
      Message.success('已删除')
      return true
    } catch {
      Message.error('删除失败')
      return false
    }
  }

  return {
    list,
    total,
    currentPage,
    loading,
    currentTask,
    fetchList,
    fetchDetail,
    create,
    remove
  }
})
