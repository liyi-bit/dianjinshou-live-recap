import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  listTemplates,
  generateScript,
  listHistory,
  rateScript,
  type ScriptTemplate,
  type ScriptGenerationItem
} from '@/api/scriptAssistant'

export const useScriptAssistantStore = defineStore('scriptAssistant', () => {
  const templates = ref<ScriptTemplate[]>([])
  const templatesLoading = ref(false)
  const history = ref<ScriptGenerationItem[]>([])
  const historyTotal = ref(0)
  const generating = ref(false)
  const lastGenerated = ref<ScriptGenerationItem | null>(null)

  async function fetchTemplates() {
    templatesLoading.value = true
    try {
      const res = await listTemplates()
      const data = (res as any).data ?? res
      templates.value = Array.isArray(data) ? data : []
    } finally {
      templatesLoading.value = false
    }
  }

  async function doGenerate(templateId: number, inputParams: string) {
    generating.value = true
    try {
      const res = await generateScript(templateId, inputParams)
      const data = (res as any).data ?? res
      lastGenerated.value = data
      return data
    } finally {
      generating.value = false
    }
  }

  async function fetchHistory(page: number, size: number) {
    const res = await listHistory(page, size)
    const data = (res as any).data ?? res
    history.value = data.records || []
    historyTotal.value = data.total || 0
  }

  async function doRate(id: number, rating: number) {
    await rateScript(id, rating)
    // Update local state
    const item = history.value.find((h) => h.id === id)
    if (item) item.rating = rating
    if (lastGenerated.value && lastGenerated.value.id === id) {
      lastGenerated.value.rating = rating
    }
  }

  return {
    templates,
    templatesLoading,
    history,
    historyTotal,
    generating,
    lastGenerated,
    fetchTemplates,
    doGenerate,
    fetchHistory,
    doRate
  }
})
