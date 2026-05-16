import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  checkCompliance,
  listSensitiveWords,
  type ComplianceCheckResult,
  type SensitiveWordItem
} from '@/api/compliance'

export const useComplianceStore = defineStore('compliance', () => {
  const checkResult = ref<ComplianceCheckResult | null>(null)
  const checking = ref(false)
  const words = ref<SensitiveWordItem[]>([])
  const wordTotal = ref(0)
  const wordLoading = ref(false)

  async function doCheck(
    scenario: string,
    textContent: string,
    platform?: string,
    industry?: string
  ) {
    checking.value = true
    try {
      const res = await checkCompliance(scenario, textContent, platform, industry)
      const data = (res as any).data ?? res
      checkResult.value = data
    } finally {
      checking.value = false
    }
  }

  async function fetchWords(page: number, size: number, category?: string, keyword?: string) {
    wordLoading.value = true
    try {
      const res = await listSensitiveWords(page, size, category, keyword)
      const data = (res as any).data ?? res
      words.value = data.records || []
      wordTotal.value = data.total || 0
    } finally {
      wordLoading.value = false
    }
  }

  function clearResult() {
    checkResult.value = null
  }

  return {
    checkResult,
    checking,
    words,
    wordTotal,
    wordLoading,
    doCheck,
    fetchWords,
    clearResult
  }
})
