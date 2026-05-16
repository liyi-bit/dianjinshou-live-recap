import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'

/**
 * AI 助手已添加的场次本地存储。
 *
 * 与 AI 全域复盘数据独立——用户主动添加才出现在助手列表。
 *
 * 多实例同步：写入用 setTimeout(300) 防抖；
 *   监听 `window.storage` 事件实现跨窗口同步。
 */

export type AssistantType = 'operation' | 'compliance'

interface State {
  /** key: assistantType -> recordingId 列表 */
  recordings: Record<AssistantType, number[]>
  /** key: assistantType -> comparisonId 列表 */
  comparisons: Record<AssistantType, number[]>
}

const STORAGE_KEY = 'djsh.assistant.added.v1'
const PERSIST_DEBOUNCE_MS = 300

function loadState(): State {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw) as Partial<State>
      return {
        recordings: {
          operation: Array.isArray(parsed.recordings?.operation) ? parsed.recordings!.operation : [],
          compliance: Array.isArray(parsed.recordings?.compliance) ? parsed.recordings!.compliance : []
        },
        comparisons: {
          operation: Array.isArray(parsed.comparisons?.operation) ? parsed.comparisons!.operation : [],
          compliance: Array.isArray(parsed.comparisons?.compliance) ? parsed.comparisons!.compliance : []
        }
      }
    }
  } catch (e) {
    // 数据损坏 / quota 超限：保留默认空集，但留下日志便于排查
    if (typeof console !== 'undefined') {
      console.warn('[assistant store] failed to load state from localStorage', e)
    }
  }
  return {
    recordings: { operation: [], compliance: [] },
    comparisons: { operation: [], compliance: [] }
  }
}

export const useAssistantStore = defineStore('assistant', () => {
  const initial = loadState()
  const recordingIds = ref<Record<AssistantType, number[]>>(initial.recordings)
  const comparisonIds = ref<Record<AssistantType, number[]>>(initial.comparisons)

  // === 持久化（防抖） ===
  let persistTimer: any = null
  let suppressNextPersist = false // 由 storage 事件回写时设置，避免反弹写
  function schedulePersist() {
    if (suppressNextPersist) {
      suppressNextPersist = false
      return
    }
    if (persistTimer) clearTimeout(persistTimer)
    persistTimer = setTimeout(() => {
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify({
          recordings: recordingIds.value,
          comparisons: comparisonIds.value
        }))
      } catch (e) {
        if (typeof console !== 'undefined') {
          console.warn('[assistant store] failed to persist state', e)
        }
      }
    }, PERSIST_DEBOUNCE_MS)
  }
  watch([recordingIds, comparisonIds], schedulePersist, { deep: true })

  // === 跨窗口同步：监听 storage 事件 ===
  if (typeof window !== 'undefined') {
    window.addEventListener('storage', (ev: StorageEvent) => {
      if (ev.key !== STORAGE_KEY || !ev.newValue) return
      try {
        const parsed = JSON.parse(ev.newValue) as Partial<State>
        suppressNextPersist = true
        recordingIds.value = {
          operation: Array.isArray(parsed.recordings?.operation) ? parsed.recordings!.operation : [],
          compliance: Array.isArray(parsed.recordings?.compliance) ? parsed.recordings!.compliance : []
        }
        suppressNextPersist = true
        comparisonIds.value = {
          operation: Array.isArray(parsed.comparisons?.operation) ? parsed.comparisons!.operation : [],
          compliance: Array.isArray(parsed.comparisons?.compliance) ? parsed.comparisons!.compliance : []
        }
      } catch (_) { /* ignore */ }
    })
  }

  function getRecordingIds(type: AssistantType): number[] {
    return recordingIds.value[type] || []
  }
  function getComparisonIds(type: AssistantType): number[] {
    return comparisonIds.value[type] || []
  }
  function hasRecording(type: AssistantType, id: number): boolean {
    return getRecordingIds(type).includes(id)
  }
  function hasComparison(type: AssistantType, id: number): boolean {
    return getComparisonIds(type).includes(id)
  }

  function addRecordings(type: AssistantType, ids: number[]) {
    const current = new Set(recordingIds.value[type] || [])
    for (const id of ids) current.add(id)
    recordingIds.value = { ...recordingIds.value, [type]: Array.from(current) }
  }
  function addComparisons(type: AssistantType, ids: number[]) {
    const current = new Set(comparisonIds.value[type] || [])
    for (const id of ids) current.add(id)
    comparisonIds.value = { ...comparisonIds.value, [type]: Array.from(current) }
  }
  function removeRecording(type: AssistantType, id: number) {
    recordingIds.value = {
      ...recordingIds.value,
      [type]: (recordingIds.value[type] || []).filter(x => x !== id)
    }
  }
  function removeComparison(type: AssistantType, id: number) {
    comparisonIds.value = {
      ...comparisonIds.value,
      [type]: (comparisonIds.value[type] || []).filter(x => x !== id)
    }
  }

  const operationRecordingCount = computed(() => recordingIds.value.operation?.length || 0)
  const complianceRecordingCount = computed(() => recordingIds.value.compliance?.length || 0)

  return {
    recordingIds,
    comparisonIds,
    getRecordingIds,
    getComparisonIds,
    hasRecording,
    hasComparison,
    addRecordings,
    addComparisons,
    removeRecording,
    removeComparison,
    operationRecordingCount,
    complianceRecordingCount
  }
})
