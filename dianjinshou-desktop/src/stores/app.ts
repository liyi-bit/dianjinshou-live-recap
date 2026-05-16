import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

const SETTINGS_KEY = 'djs_settings'
const LEGACY_DEFAULT_RECORDING_PATH = 'D:\\录制文件'
const RESOLUTION_VALUES = new Set(['480p', '720p', '1080p', 'source'])

type RecordingResolution = '480p' | '720p' | '1080p' | 'source'

function loadSettings() {
  try {
    const raw = localStorage.getItem(SETTINGS_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

function normalizeResolution(value: unknown): RecordingResolution {
  if (value === '原始' || value === '原画') return 'source'
  return RESOLUTION_VALUES.has(String(value))
    ? (String(value) as RecordingResolution)
    : '1080p'
}

function normalizeSegmentDuration(value: unknown): number {
  const n = Math.floor(Number(value))
  if (!Number.isFinite(n)) return 30
  return Math.max(10, Math.min(180, n))
}

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const appVersion = ref('')

  const api = (window as any).electronAPI
  api?.getAppVersion?.()
    .then((v: string) => { appVersion.value = v ? `v${v}` : '' })
    .catch(() => {})

  const saved = loadSettings()

  // Basic settings
  const storagePath = ref<string>(saved.storagePath ?? LEGACY_DEFAULT_RECORDING_PATH)
  const resolution = ref<RecordingResolution>(normalizeResolution(saved.resolution))
  const checkInterval = ref<number>(saved.checkInterval ?? 60)
  const segmentDuration = ref<number>(normalizeSegmentDuration(saved.segmentDuration))
  let syncingRecordingPath = false

  // Account settings
  const username = ref<string>(saved.username ?? '')
  const phone = ref<string>(saved.phone ?? '')
  const avatarUrl = ref<string>(saved.avatarUrl ?? '')

  function persistSettings() {
    localStorage.setItem(SETTINGS_KEY, JSON.stringify({
      storagePath: storagePath.value,
      resolution: resolution.value,
      checkInterval: checkInterval.value,
      segmentDuration: segmentDuration.value,
      username: username.value,
      phone: phone.value,
      avatarUrl: avatarUrl.value
    }))
  }

  watch([storagePath, resolution, checkInterval, segmentDuration, username, phone, avatarUrl], persistSettings, { deep: true })

  watch(checkInterval, (seconds) => {
    api?.setMonitorInterval?.(seconds)?.catch(() => {})
  })

  watch(storagePath, (path) => {
    if (syncingRecordingPath) return
    if (!path || path === LEGACY_DEFAULT_RECORDING_PATH) return
    api?.setRecordingsPath?.(path)?.then((res: any) => {
      if (res?.success && res.data && res.data !== storagePath.value) {
        syncingRecordingPath = true
        storagePath.value = res.data
        syncingRecordingPath = false
      }
    }).catch(() => {})
  })

  async function syncDesktopSettings() {
    if (!api) return
    try {
      if (storagePath.value && storagePath.value !== LEGACY_DEFAULT_RECORDING_PATH) {
        const res = await api.setRecordingsPath?.(storagePath.value)
        if (res?.success && res.data) {
          syncingRecordingPath = true
          storagePath.value = res.data
          syncingRecordingPath = false
        } else {
          const cur = await api.getRecordingsPath?.()
          if (cur?.success && cur.data) {
            syncingRecordingPath = true
            storagePath.value = cur.data
            syncingRecordingPath = false
          }
        }
      } else {
        const cur = await api.getRecordingsPath?.()
        if (cur?.success && cur.data) {
          syncingRecordingPath = true
          storagePath.value = cur.data
          syncingRecordingPath = false
        }
      }
    } finally {
      await api.setMonitorInterval?.(checkInterval.value)?.catch(() => {})
    }
  }

  syncDesktopSettings().catch(() => {})

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setSidebarCollapsed(collapsed: boolean) {
    sidebarCollapsed.value = collapsed
  }

  return {
    sidebarCollapsed,
    appVersion,
    storagePath,
    resolution,
    checkInterval,
    segmentDuration,
    username,
    phone,
    avatarUrl,
    syncDesktopSettings,
    toggleSidebar,
    setSidebarCollapsed
  }
})
