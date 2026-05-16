<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { useStreamerStore } from '@/stores/streamer'
import { useRecapStore } from '@/stores/recap'
import { useAppStore } from '@/stores/app'
import { storeToRefs } from 'pinia'
import * as streamerApi from '@/api/streamer'
import type { Streamer, StreamerDetail } from '@/api/streamer'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import Pagination from '@/components/common/Pagination.vue'

const router = useRouter()
const store = useStreamerStore()
const recapStore = useRecapStore()
const appStore = useAppStore()
const { liveStateMap } = storeToRefs(store)
const { resolution, segmentDuration } = storeToRefs(appStore)

// More dropdown state
const showMoreId = ref<string | number | null>(null)

// Settings dialog
const settingsDialogVisible = ref(false)
const settingsTarget = ref<Streamer | null>(null)
const settingsForm = ref({
  accountType: 'own' as string,
  accountStage: '' as string,
  accountIssue: '' as string,
  accountLevel: '' as string,
  trafficStructure: '' as string,
  broadcastTimeStart: '' as string,
  broadcastTimeEnd: '' as string,
  liveRoomMode: '' as string,
  defaultLanguage: '中文通用' as string
})
const settingsLoading = ref(false)

// Homepage dialog
const homepageDialogVisible = ref(false)
const homepageUrl = ref('')
const homepageStreamerName = ref('')

// Filter state
const activeTab = ref('all')
const searchKeyword = ref('')
const filterAccountType = ref('')
const filterPlatform = ref('')

// Delete dialog
const deleteDialogVisible = ref(false)
const deleteTarget = ref<Streamer | null>(null)

// Add dropdown
const addDropdownVisible = ref(false)

// Recording monitor state
const isMonitorRunning = ref(false)
const monitorStatusTimer = ref<ReturnType<typeof setInterval> | null>(null)

// Recording quality selector（format 固定 flv→remux 成 mp4，UI 不暴露）
type RecordingResolution = '480p' | '720p' | '1080p' | 'source'
type RecordQuality = 'sd' | 'hd' | 'uhd' | 'source'

// UI 画质 → main 端识别的 resolution 值（monitor-service 会再映射到抖音 FULL_HD1/HD1/SD1/SD2）
const qualityToResolution: Record<RecordQuality, RecordingResolution> = {
  sd: '480p',
  hd: '720p',
  uhd: '1080p',
  source: 'source'
}
const resolutionToQuality: Record<RecordingResolution, RecordQuality> = {
  '480p': 'sd',
  '720p': 'hd',
  '1080p': 'uhd',
  source: 'source'
}

const recordQuality = computed<RecordQuality>({
  get: () => resolutionToQuality[resolution.value as RecordingResolution] || 'source',
  set: (value) => { resolution.value = qualityToResolution[value] }
})

async function syncDesktopSettingsIfAvailable() {
  const sync = (appStore as any).syncDesktopSettings
  if (typeof sync === 'function') {
    await sync.call(appStore).catch(() => {})
  }
}

const platformOptions = [
  { label: '全部平台', value: '' },
  { label: '抖音', value: 'douyin' },
  { label: '快手', value: 'kuaishou' }
]

const accountTypeOptions = [
  { label: '全部', value: '' },
  { label: '自有', value: 'own' },
  { label: '同行业', value: 'industry' },
  { label: '竞品', value: 'competitor' }
]

// 列表数据来源：
//   - 「所有直播间」tab → 走后端分页的 store.list
//   - 「录制中」tab → 前端基于 store.allStreamers 过滤（isMonitoring 开 + 主播确实直播中），
//     与顶部"录制中"卡片语义保持一致；通常数量较少，不需后端分页
const filteredList = computed(() => {
  if (activeTab.value === 'recording') {
    const map = liveStateMap.value || {}
    return store.allStreamers.filter((s) => s.isMonitoring && map[String(s.id)] === true)
  }
  return store.list
})

watch(activeTab, () => {
  // 切 tab 时回到第 1 页；不再把 isMonitoring 传给后端 —— 录制中走前端过滤
  store.query.page = 1
  delete (store.query as any).isMonitoring
  store.fetchList()
})

const activeMoreStreamer = computed(() => {
  if (showMoreId.value === null) return null
  return store.list.find(s => s.id === showMoreId.value) || null
})

// allCount / recordingCount / monitoringCount 必须反映全量主播，不受 tab 切换 / 后端分页影响
// （store.total 会随后端 query 的过滤条件变化，所以这里不能用它）
const allCount = computed(() => store.allStreamers.length)
// 监测中：打开了自动录制开关的（不论主播是否在直播）
const monitoringCount = computed(() => store.allStreamers.filter((s) => s.isMonitoring).length)
// 录制中：自动录制开关开 + 当前主播确实在直播 ——这才是真正在录视频的主播数
const recordingCount = computed(() => store.allStreamers.filter((s) => {
  if (!s.isMonitoring) return false
  return liveStateMap.value?.[String(s.id)] === true
}).length)

function getPlatformLabel(platform: string) {
  const map: Record<string, string> = { douyin: '抖音', kuaishou: '快手' }
  return map[platform] || platform
}

function getPlatformColor(platform: string) {
  const map: Record<string, string> = { douyin: '#000000', kuaishou: '#ff4906' }
  return map[platform] || '#B8823A'
}

function getAccountTypeLabel(type: string) {
  const map: Record<string, string> = { own: '自有', industry: '同行业', competitor: '竞品' }
  return map[type] || type
}

function getAccountTypeColor(type: string) {
  const map: Record<string, string> = { own: 'arcoblue', industry: 'green', competitor: 'orangered' }
  return map[type] || 'arcoblue'
}

// Search / filter
function handleSearch(value: string) {
  store.query.keyword = value
  store.query.page = 1
  store.fetchList()
}

function handleAccountTypeChange(value: string) {
  filterAccountType.value = value
  store.query.accountType = value
  store.query.page = 1
  store.fetchList()
}

function handlePlatformChange(value: string) {
  filterPlatform.value = value
  store.query.platform = value
  store.query.page = 1
  store.fetchList()
}

// Recording analysis toggle
async function toggleRecordingAnalysis() {
  const api = window.electronAPI
  if (!api) {
    Message.error('录制功能仅在桌面客户端中可用')
    return
  }

  if (isMonitorRunning.value) {
    // Stop monitoring + stop all active recordings
    const res = await api.stopMonitoring()
    if (res.success) {
      isMonitorRunning.value = false
      Message.success('已停止录制分析，所有录制已停止')
      // Refresh list to update monitoring status
      store.fetchList()
    } else {
      Message.error(res.error || '停止失败')
    }
  } else {
    // 从"全量主播"里筛出开启自动录制的 —— 不能用 store.list（那只有当前页 20 条，会漏录第 21 条后的）
    // 每次启动前刷一次 allStreamers，防止用户刚切换 isMonitoring 开关后数据过时
    await store.fetchAllStreamers()
    const enabledStreamers = store.allStreamers.filter((s) => s.isMonitoring)
    if (enabledStreamers.length === 0) {
      Message.warning('没有开启自动录制的直播间，请先在「更多」中开启自动录制')
      return
    }

    // 并发启动每个主播的监控；失败的不阻塞其他成功项
    const addResults = await Promise.allSettled(
      enabledStreamers.map(s => api.addMonitoredStreamer(buildMonitorConfig(s, true)))
    )
    const failed = addResults.filter(r => r.status === 'rejected').length

    const res = await api.startMonitoring()
    if (res.success) {
      isMonitorRunning.value = true
      const ok = enabledStreamers.length - failed
      if (failed > 0) {
        Message.warning(`已启动 ${ok}/${enabledStreamers.length} 个直播间，${failed} 个失败`)
      } else {
        Message.success(`开始录制分析，正在监控 ${ok} 个直播间`)
      }
      startMonitorPolling()
    } else {
      Message.error(res.error || '启动失败')
    }
  }
}

function startMonitorPolling() {
  if (monitorStatusTimer.value) return
  monitorStatusTimer.value = setInterval(async () => {
    const api = window.electronAPI
    if (!api) return
    const res = await api.getMonitorStatus()
    if (res.success && res.data) {
      isMonitorRunning.value = res.data.isRunning
      updateLiveStates(res.data.streamers)
    }
  }, 5000)
}

function stopMonitorPolling() {
  if (monitorStatusTimer.value) {
    clearInterval(monitorStatusTimer.value)
    monitorStatusTimer.value = null
  }
}

function updateLiveStates(streamers: Array<{ streamerId: string; isLive: boolean }> | undefined) {
  store.updateLiveStates(streamers)
}

/** 头像加载失败时显示首字母占位 */
function onAvatarError(ev: Event) {
  const img = ev.target as HTMLImageElement | null
  if (!img) return
  img.style.display = 'none'
  const fallback = img.nextElementSibling as HTMLElement | null
  if (fallback) fallback.style.display = ''
}

function getLiveStatus(streamer: Streamer | null | undefined): { text: string; cls: string } {
  if (!streamer || streamer.id == null) {
    return { text: '检测中', cls: 'idle' }
  }
  const map = liveStateMap.value || {}
  const key = String(streamer.id)
  if (!Object.prototype.hasOwnProperty.call(map, key)) {
    return { text: '检测中', cls: 'idle' }
  }
  const isLive = map[key]
  if (isLive) return { text: '直播中', cls: 'live' }
  return { text: '未直播', cls: 'offline' }
}

function getRecordingSegmentSeconds(): number {
  const minutes = Math.max(10, Math.min(180, Number(segmentDuration.value) || 30))
  return minutes * 60
}

function buildMonitorConfig(s: Streamer, autoRecord = true) {
  const accountId = s.accountId || ''
  const roomUrl = s.platform === 'douyin'
    ? `https://live.douyin.com/${accountId}`
    : s.platform === 'kuaishou'
      ? `https://live.kuaishou.com/u/${accountId}`
      : ''
  return {
    streamerId: String(s.id),
    platform: s.platform || '',
    roomId: accountId,
    roomUrl,
    anchorName: s.anchorName || '',
    resolution: resolution.value as RecordingResolution,
    segmentDuration: getRecordingSegmentSeconds(),
    autoRecord,
  }
}

// Toggle auto-recording for individual streamer
async function handleToggleAutoRecord(streamer: Streamer) {
  const enabling = !streamer.isMonitoring
  try {
    await store.toggleMonitor(streamer.id, enabling)
    // Keep the main-process monitor in sync so adds/removes after the Monitor session
    // already started still take effect without needing to restart the session.
    await syncStreamerWithMonitor(streamer, enabling)
    Message.success(enabling ? '已开启自动录制' : '已关闭自动录制')
  } catch {
    Message.error('操作失败')
  }
}

async function handleToggleCloudSync(streamer: Streamer) {
  const enabling = !streamer.cloudSyncEnabled
  try {
    await store.toggleCloudSync(streamer.id, enabling)
    Message.success(enabling ? '已开启云空间同步' : '已关闭云空间同步')
  } catch {
    Message.error('云空间同步设置失败')
  }
}

/** Push a streamer into (or out of) the live Monitor. Safe to call whether the monitor is running or not. */
async function syncStreamerWithMonitor(s: Streamer, enabled: boolean) {
  const api = window.electronAPI
  if (!api) return
  if (enabled) {
    await api.addMonitoredStreamer(buildMonitorConfig(s, true) as any).catch(() => { /* best effort */ })
  } else {
    await api.removeMonitoredStreamer(String(s.id)).catch(() => { /* best effort */ })
  }
}

async function syncEnabledStreamersWithMonitor() {
  const api = window.electronAPI
  if (!api) return
  await syncDesktopSettingsIfAvailable()
  const enabledStreamers = store.allStreamers.filter((s) => s.isMonitoring)
  for (const s of enabledStreamers) {
    await api.addMonitoredStreamer(buildMonitorConfig(s, true) as any).catch(() => { /* best effort */ })
  }
}

watch([resolution, segmentDuration], () => {
  syncEnabledStreamersWithMonitor().catch(() => {})
})

function handleMoreAction(action: string, streamer: Streamer) {
  switch (action) {
    case 'recap':
      // 跳转到AI整场复盘列表并筛选该主播
      recapStore.setStreamerId(streamer.id)
      router.push({ path: '/recap/full', query: { streamerId: String(streamer.id) } })
      break
    case 'homepage':
      openHomepage(streamer)
      break
    case 'pause': {
      const enabling = !streamer.isMonitoring
      store.toggleMonitor(streamer.id, enabling)
        .then(() => syncStreamerWithMonitor(streamer, enabling))
        .then(() => {
          Message.success(enabling ? '已开启自动录制' : '已暂停自动录制')
        })
        .catch(() => {
          Message.error('操作失败')
        })
      break
    }
    case 'settings':
      openSettings(streamer)
      break
    case 'delete':
      deleteTarget.value = streamer
      deleteDialogVisible.value = true
      break
  }
}

// Issue 2: 主页弹窗
function openHomepage(streamer: Streamer) {
  const accountId = streamer.accountId
  if (!accountId) {
    Message.warning('未找到主播账号ID')
    return
  }
  homepageStreamerName.value = streamer.anchorName
  if (streamer.platform === 'douyin') {
    if (streamer.secUid) {
      homepageUrl.value = `https://www.douyin.com/user/${streamer.secUid}`
    } else {
      // 没有 secUid 时尝试实时获取
      fetchSecUidAndOpenHomepage(streamer)
      return
    }
  } else if (streamer.platform === 'kuaishou') {
    homepageUrl.value = `https://live.kuaishou.com/u/${accountId}`
  } else {
    homepageUrl.value = ''
  }
  homepageDialogVisible.value = true
}

async function fetchSecUidAndOpenHomepage(streamer: Streamer) {
  const api = window.electronAPI as any
  if (!api?.resolveDouyinRoom) {
    homepageUrl.value = `https://live.douyin.com/${streamer.accountId}`
    homepageDialogVisible.value = true
    return
  }
  Message.info('正在获取主播主页信息...')
  try {
    const res = await api.resolveDouyinRoom(streamer.accountId)
    if (res.success && res.data?.secUid) {
      homepageUrl.value = `https://www.douyin.com/user/${res.data.secUid}`
      // 后台更新 secUid 到数据库
      streamerApi.updateStreamer(streamer.id, { secUid: res.data.secUid }).catch(() => {})
    } else {
      homepageUrl.value = `https://live.douyin.com/${streamer.accountId}`
    }
  } catch {
    homepageUrl.value = `https://live.douyin.com/${streamer.accountId}`
  }
  homepageDialogVisible.value = true
}

function goToHomepage() {
  if (homepageUrl.value) {
    window.open(homepageUrl.value, '_blank')
  }
  homepageDialogVisible.value = false
}

// Issue 4: 基础设置弹窗
async function openSettings(streamer: Streamer) {
  settingsTarget.value = streamer
  settingsLoading.value = true
  settingsDialogVisible.value = true
  try {
    const res = await streamerApi.getStreamer(streamer.id)
    const detail: StreamerDetail = (res as any).data ?? res
    settingsForm.value = {
      accountType: detail.accountType || 'own',
      accountStage: detail.accountStage || '',
      accountIssue: detail.accountIssue || '',
      accountLevel: detail.accountLevel || '',
      trafficStructure: detail.trafficStructure || '',
      broadcastTimeStart: detail.broadcastTimeStart || '',
      broadcastTimeEnd: detail.broadcastTimeEnd || '',
      liveRoomMode: detail.liveRoomMode || '',
      defaultLanguage: detail.defaultLanguage || '中文通用'
    }
  } catch {
    Message.error('获取主播信息失败')
  }
  settingsLoading.value = false
}

async function saveSettings() {
  if (!settingsTarget.value) return
  try {
    await streamerApi.updateStreamer(settingsTarget.value.id, settingsForm.value)
    Message.success('保存成功')
    settingsDialogVisible.value = false
    store.fetchList()
  } catch {
    Message.error('保存失败')
  }
}

// Issue 5: 视频文件夹
async function openStreamerVideoFolder(_streamer: Streamer) {
  const api = window.electronAPI
  if (!api) {
    Message.warning('仅桌面客户端可用')
    return
  }
  try {
    const res = await api.getRecordingsPath()
    if (res?.success && res.data) {
      await api.openFolder(res.data)
    } else {
      Message.warning('未找到录制文件夹')
    }
  } catch {
    Message.warning('未找到录制文件夹')
  }
}

// More menu with fixed positioning to avoid overflow clipping
const moreMenuStyle = ref<Record<string, string>>({})

function toggleMoreMenu(id: number, event: MouseEvent) {
  if (showMoreId.value === id) {
    showMoreId.value = null
    return
  }
  showMoreId.value = id
  const btn = event.currentTarget as HTMLElement
  const rect = btn.getBoundingClientRect()
  const spaceBelow = window.innerHeight - rect.bottom
  const menuHeight = 180
  if (spaceBelow < menuHeight) {
    moreMenuStyle.value = { position: 'fixed', right: `${window.innerWidth - rect.right}px`, bottom: `${window.innerHeight - rect.top + 4}px` }
  } else {
    moreMenuStyle.value = { position: 'fixed', right: `${window.innerWidth - rect.right}px`, top: `${rect.bottom + 4}px` }
  }
}

async function confirmDelete() {
  if (!deleteTarget.value) return
  try {
    await store.removeStreamer(deleteTarget.value.id)
    Message.success('删除成功')
  } catch {
    Message.error('删除失败')
  }
  deleteTarget.value = null
}

// v1.1.0：每个账号最多 20 个主播
const MAX_STREAMERS = 20

function toggleAddDropdown() {
  addDropdownVisible.value = !addDropdownVisible.value
}

function goAddStreamer(platform: 'douyin' | 'kuaishou') {
  const current = store.total || store.allStreamers.length
  if (current >= MAX_STREAMERS) {
    Message.warning(`已达到主播数量上限（${MAX_STREAMERS} 个），请先删除不再需要的再添加`)
    return
  }
  addDropdownVisible.value = false
  router.push(`/streamers/add/${platform}`)
}

// Grouped streamers by industryName
const groupedStreamers = computed(() => {
  const groups: Record<string, Streamer[]> = {}
  for (const s of filteredList.value) {
    const key = s.industryName || '未分类'
    if (!groups[key]) groups[key] = []
    groups[key].push(s)
  }
  return Object.entries(groups).map(([name, items]) => ({ name, items }))
})

onMounted(async () => {
  await syncDesktopSettingsIfAvailable()
  // 每次重新进入页面，清空上次的筛选缓存（store 是 pinia 单例不会自动重置）
  store.resetQuery()
  activeTab.value = 'all'
  searchKeyword.value = ''
  filterAccountType.value = ''
  filterPlatform.value = ''

  await Promise.allSettled([
    store.fetchList(),
    store.fetchAllStreamers(),
    store.fetchStats(),
  ])

  // 主播直播状态与录制监控共用主进程同一条轮询队列，避免重复请求抖音。
  const api = window.electronAPI
  if (api) {
    try {
      await api.startLiveStatusPolling?.()
      const res = await api.getMonitorStatus()
      if (res.success && res.data) {
        isMonitorRunning.value = res.data.isRunning
        updateLiveStates(res.data.streamers)
      }
      startMonitorPolling()
    } catch {
      // ignore
    }
  }
})

onUnmounted(() => {
  stopMonitorPolling()
})
</script>

<template>
  <div class="streamer-list" @click="showMoreId = null">

    <!-- 1. Stats Row -->
    <div class="djsgrid-3" style="margin-bottom:16px">
      <div class="djsstat">
        <div class="ico blue">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="2" y="6" width="20" height="12" rx="2"/><path d="M10 10l5 3-5 3z"/></svg>
        </div>
        <div style="flex:1">
          <div class="lbl">直播间总数</div>
          <div class="val">{{ store.stats?.total ?? 0 }}</div>
        </div>
      </div>
      <div class="djsstat">
        <div class="ico green">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="3" fill="currentColor"/></svg>
        </div>
        <div style="flex:1">
          <div class="lbl">录制中</div>
          <div class="val">{{ recordingCount }}</div>
        </div>
      </div>
      <div class="djsstat">
        <div class="ico amber">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
        </div>
        <div style="flex:1">
          <div class="lbl">监测中</div>
          <div class="val">{{ monitoringCount }}</div>
        </div>
      </div>
    </div>

    <!-- 2. Card with toolbar + filter + table -->
    <div class="djscard" style="margin-bottom:16px">
      <!-- Toolbar: Pills + Add button -->
      <div class="djstoolbar">
        <div class="pills">
          <div class="pill" :class="{ on: activeTab === 'all' }" @click="activeTab = 'all'">
            所有直播间<span class="num">{{ allCount }}</span>
          </div>
          <div class="pill" :class="{ on: activeTab === 'recording' }" @click="activeTab = 'recording'">
            录制中<span class="num">{{ recordingCount }}</span>
          </div>
        </div>
        <div class="spacer"></div>
        <div class="add-wrap">
          <button class="djsbtn primary" @click.stop="toggleAddDropdown">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" width="12" height="12"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            添加直播间
          </button>
          <div v-show="addDropdownVisible" class="add-menu" @click.stop>
            <div @click="goAddStreamer('douyin')">添加抖音</div>
            <div @click="goAddStreamer('kuaishou')">添加快手</div>
          </div>
        </div>
      </div>

      <!-- Filter bar -->
      <div class="djsfilter">
        <div class="sch">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input
            type="text"
            placeholder="请输入主播名称"
            v-model="searchKeyword"
            @keyup.enter="handleSearch(searchKeyword)"
          >
        </div>
        <button class="djsbtn primary" style="height:30px;padding:0 16px;font-size:12px" @click="handleSearch(searchKeyword)">查询</button>
        <span class="div"></span>
        <span class="fl-label">账号筛选</span>
        <div class="rds">
          <div class="rd" :class="{ on: filterAccountType === '' }" @click="handleAccountTypeChange('')">
            <div class="rd-c"></div>全部账号
          </div>
          <div class="rd" :class="{ on: filterAccountType === 'own' }" @click="handleAccountTypeChange('own')">
            <div class="rd-c"></div>自有账号
          </div>
          <div class="rd" :class="{ on: filterAccountType === 'industry' }" @click="handleAccountTypeChange('industry')">
            <div class="rd-c"></div>同行业账号
          </div>
        </div>
        <div class="spacer"></div>
        <button
          class="djsbtn"
          :class="isMonitorRunning ? 'stop-rec' : 'danger pulse'"
          @click="toggleRecordingAnalysis"
        >
          <span class="dot"></span>
          {{ isMonitorRunning ? '停止录制分析' : '点我录制分析' }}
        </button>
        <select class="djsselect" style="width:76px;font-family:var(--fm);font-size:12px" v-model="recordQuality" :disabled="isMonitorRunning">
          <option value="sd">标清</option>
          <option value="hd">高清</option>
          <option value="uhd">超清</option>
          <option value="source">原画</option>
        </select>
      </div>

    <!-- 4. Table grouped by industry -->
    <template v-if="filteredList.length > 0">
      <template
        v-for="group in groupedStreamers"
        :key="group.name"
      >
        <div class="sdv">{{ group.name }}</div>
        <table class="djstbl">
          <thead>
            <tr>
              <th>主播</th>
              <th>平台授权</th>
              <th>自动录制</th>
              <th>云空间</th>
              <th>昨日录制</th>
              <th>视频文件夹</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="streamer in group.items" :key="streamer.id">
              <!-- 主播 cell -->
              <td>
                <div style="display:flex;align-items:center;gap:12px">
                  <div
                    class="djsav"
                    :class="streamer.platform === 'douyin' ? 'g1' : streamer.platform === 'kuaishou' ? 'g2' : 'g3'"
                  >
                    <img
                      v-if="streamer.anchorAvatar"
                      :src="streamer.anchorAvatar"
                      class="djsav-img"
                      referrerpolicy="no-referrer"
                      @error="onAvatarError"
                    />
                    <span v-if="!streamer.anchorAvatar" class="djsav-text">{{ streamer.anchorName?.charAt(0) || '?' }}</span>
                    <span v-else class="djsav-text" style="display:none">{{ streamer.anchorName?.charAt(0) || '?' }}</span>
                    <span class="pf" :style="{ background: getPlatformColor(streamer.platform) }">
                      {{ getPlatformLabel(streamer.platform).charAt(0) }}
                    </span>
                  </div>
                  <div>
                    <div style="font-size:14px;font-weight:650;color:var(--text-1);line-height:1.2;letter-spacing:-.008em">{{ streamer.anchorName }}</div>
                    <div style="display:flex;align-items:center;gap:4px;margin-top:5px">
                      <span
                        class="djsbadge"
                        :class="streamer.accountType === 'own' ? 'own' : 'peer'"
                      >{{ getAccountTypeLabel(streamer.accountType) }}</span>
                      <span class="s-status" :class="getLiveStatus(streamer).cls">
                        {{ getLiveStatus(streamer).text }}
                      </span>
                    </div>
                  </div>
                </div>
              </td>
              <!-- 平台授权 -->
              <td style="color:var(--text-3)">{{ getPlatformLabel(streamer.platform) }}</td>
              <!-- 数据看板（自动录制开关） -->
              <td>
                <label class="djsswitch">
                  <input type="checkbox" :checked="streamer.isMonitoring" @change="handleToggleAutoRecord(streamer)">
                  <span></span>
                </label>
              </td>
              <!-- 云空间同步开关 -->
              <td>
                <label class="djsswitch" title="开启后，未来录制完成的数据会同步到云空间">
                  <input type="checkbox" :checked="streamer.cloudSyncEnabled" @change="handleToggleCloudSync(streamer)">
                  <span></span>
                </label>
              </td>
              <!-- 昨日录制 -->
              <td><span style="font-family:var(--fm);font-weight:700;color:var(--text-1);font-size:14px;letter-spacing:-.02em">{{ streamer.totalSessions ?? 0 }}</span></td>
              <!-- 视频文件夹 -->
              <td><button class="djsbtn text" @click="openStreamerVideoFolder(streamer)">查看</button></td>
              <!-- 操作 -->
              <td>
                <div style="display:flex;align-items:center;gap:4px">
                  <button class="djsbtn icon" @click="handleMoreAction('recap', streamer)">复盘表</button>
                  <button class="djsbtn icon" @click.stop="toggleMoreMenu(streamer.id, $event)">更多 ▾</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </template>
    </template>

      <!-- Pagination -->
      <Pagination
        v-if="store.total > 0"
        :page="store.query.page"
        :size="store.query.size"
        :total="store.total"
        @change="(p) => { store.query.page = p; store.fetchList() }"
      />

      <!-- 5. Empty State (inside card) -->
      <div v-if="filteredList.length === 0" class="djsempty">
        <div class="ic">📋</div>
        <div class="ti">暂无直播间</div>
        <p>请先添加直播间，再点击"点我录制分析"开始监控</p>
        <div style="display:flex;gap:10px;margin-top:12px;justify-content:center">
          <button class="djsbtn primary" @click="goAddStreamer('douyin')">添加抖音</button>
          <button class="djsbtn" @click="goAddStreamer('kuaishou')">添加快手</button>
        </div>
      </div>
    </div><!-- end .djscard -->

    <!-- Fixed-position More Menu -->
    <Teleport to="body">
      <div v-show="showMoreId !== null && activeMoreStreamer" class="more-menu-overlay" @click="showMoreId = null">
        <div class="more-menu-fixed" :style="moreMenuStyle" @click.stop>
          <div @click="handleMoreAction('homepage', activeMoreStreamer!); showMoreId = null">主页</div>
          <div @click="handleMoreAction('pause', activeMoreStreamer!); showMoreId = null">
            {{ activeMoreStreamer?.isMonitoring ? '暂停自动录制' : '开启自动录制' }}
          </div>
          <div @click="handleMoreAction('settings', activeMoreStreamer!); showMoreId = null">基础设置</div>
          <div class="danger" @click="handleMoreAction('delete', activeMoreStreamer!); showMoreId = null">删除</div>
        </div>
      </div>
    </Teleport>

    <!-- 6. Delete Dialog -->
    <ConfirmDialog
      v-model:visible="deleteDialogVisible"
      title="确认删除"
      content="确定删除该直播间吗？删除后该主播所有的复盘数据、切片数据、笔记、AI 对话历史都将被清除，此操作不可恢复！"
      confirm-text="确定删除"
      cancel-text="取消"
      @confirm="confirmDelete"
    />

    <!-- 7. Homepage Dialog -->
    <Teleport to="body">
      <div v-if="homepageDialogVisible" class="djsmodal-overlay" @click.self="homepageDialogVisible = false">
        <div class="djsmodal" style="width:480px">
          <div class="djsmodal-header">
            <span>主播主页</span>
            <span class="djsmodal-close" @click="homepageDialogVisible = false">&times;</span>
          </div>
          <div class="djsmodal-body" style="text-align:center;padding:24px">
            <div style="font-size:15px;font-weight:600;margin-bottom:12px">{{ homepageStreamerName }}</div>
            <div style="font-size:13px;color:var(--text-3);margin-bottom:20px;word-break:break-all">{{ homepageUrl }}</div>
            <button class="djsbtn primary" @click="goToHomepage">打开主页</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 8. Settings Dialog -->
    <Teleport to="body">
      <div v-if="settingsDialogVisible" class="djsmodal-overlay" @click.self="settingsDialogVisible = false">
        <div class="djsmodal" style="width:600px;max-height:80vh;overflow-y:auto">
          <div class="djsmodal-header">
            <span>基础设置</span>
            <span class="djsmodal-close" @click="settingsDialogVisible = false">&times;</span>
          </div>
          <div class="djsmodal-body" v-if="!settingsLoading">
            <div class="settings-form">
              <div class="form-row">
                <label class="form-label required">账号归属</label>
                <div class="form-value">
                  <label><input type="radio" v-model="settingsForm.accountType" value="own"> 自有账号</label>
                  <label><input type="radio" v-model="settingsForm.accountType" value="industry"> 同行业账号</label>
                  <label><input type="radio" v-model="settingsForm.accountType" value="competitor"> 竞品账号</label>
                </div>
              </div>

              <div class="form-section-title">以下内容非必填：</div>

              <div class="form-row">
                <label class="form-label">A.账号阶段</label>
                <div class="form-value tags">
                  <span v-for="opt in ['起号期','上升期','打标签期','变现期','平稳期','瓶颈期','衰退期']" :key="opt"
                    class="form-tag" :class="{ active: settingsForm.accountStage === opt }"
                    @click="settingsForm.accountStage = settingsForm.accountStage === opt ? '' : opt">{{ opt }}</span>
                </div>
              </div>

              <div class="form-row">
                <label class="form-label">B.账号问题</label>
                <div class="form-value">
                  <textarea class="djsinput" rows="3" v-model="settingsForm.accountIssue"
                    placeholder="为了方便AI分析更准确，提供更有效、更具有建议有效，请按账号目前遇到的问题和您的需求详细的描述出来"
                    maxlength="200" style="width:100%;resize:vertical"></textarea>
                  <div style="text-align:right;font-size:11px;color:var(--text-3)">{{ settingsForm.accountIssue.length }}/200</div>
                </div>
              </div>

              <div class="form-row">
                <label class="form-label">账号水平</label>
                <div class="form-value tags">
                  <span v-for="opt in ['头部','中腰部','尾部']" :key="opt"
                    class="form-tag" :class="{ active: settingsForm.accountLevel === opt }"
                    @click="settingsForm.accountLevel = settingsForm.accountLevel === opt ? '' : opt">{{ opt }}</span>
                </div>
              </div>

              <div class="form-row">
                <label class="form-label">流量结构</label>
                <div class="form-value tags">
                  <span v-for="opt in ['纯自然流','微付费','重付费','纯付费']" :key="opt"
                    class="form-tag" :class="{ active: settingsForm.trafficStructure === opt }"
                    @click="settingsForm.trafficStructure = settingsForm.trafficStructure === opt ? '' : opt">{{ opt }}</span>
                </div>
              </div>

              <div class="form-row">
                <label class="form-label">开播时间</label>
                <div class="form-value" style="display:flex;align-items:center;gap:8px">
                  <input type="time" class="djsinput" style="width:120px" v-model="settingsForm.broadcastTimeStart">
                  <span>至</span>
                  <input type="time" class="djsinput" style="width:120px" v-model="settingsForm.broadcastTimeEnd">
                </div>
              </div>

              <div class="form-row">
                <label class="form-label">直播间模式</label>
                <div class="form-value tags">
                  <span v-for="opt in ['多人间隔','多人日不落','单人日不落','单人间隔']" :key="opt"
                    class="form-tag" :class="{ active: settingsForm.liveRoomMode === opt }"
                    @click="settingsForm.liveRoomMode = settingsForm.liveRoomMode === opt ? '' : opt">{{ opt }}</span>
                </div>
              </div>

              <div class="form-row">
                <label class="form-label">默认识别语言</label>
                <div class="form-value">
                  <select class="djsselect" v-model="settingsForm.defaultLanguage" style="width:160px">
                    <option value="中文通用">中文通用</option>
                    <option value="英语">英语</option>
                    <option value="粤语">粤语</option>
                    <option value="日语">日语</option>
                    <option value="韩语">韩语</option>
                  </select>
                </div>
              </div>
            </div>
          </div>
          <div class="djsmodal-body" v-else style="text-align:center;padding:40px">
            <span style="color:var(--text-3)">加载中...</span>
          </div>
          <div class="djsmodal-footer">
            <button class="djsbtn" @click="settingsDialogVisible = false">取消</button>
            <button class="djsbtn primary" @click="saveSettings" :disabled="settingsLoading">保存</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.streamer-list {
  position: relative;
  min-height: 100%;
  padding-bottom: 40px;
}

/* Stop recording button — same pill shape as .danger but white */
.djsbtn.stop-rec {
  height: 40px; padding: 0 22px;
  font-size: 13.5px; font-weight: 700;
  color: var(--text-1); border-radius: var(--radius-pill);
  background: linear-gradient(180deg, #FFFFFF 0%, var(--card) 100%);
  box-shadow:
    inset 0 1px 0 rgba(255,255,255,.8),
    0 0 0 1px var(--line),
    0 1px 3px rgba(36,30,24,.08),
    0 4px 14px rgba(36,30,24,.06);
  letter-spacing: .012em;
}
.djsbtn.stop-rec:hover {
  box-shadow:
    inset 0 1px 0 rgba(255,255,255,.8),
    0 0 0 1px var(--line-3),
    0 2px 6px rgba(36,30,24,.1),
    0 8px 22px rgba(36,30,24,.08);
  transform: translateY(-1px);
}
.djsbtn.stop-rec .dot {
  width: 7px; height: 7px; background: var(--red); border-radius: 2px;
  box-shadow: 0 0 6px rgba(184,68,60,.4);
  animation: none;
}

.s-status {
  display: inline-flex; align-items: center;
  height: 22px; padding: 0 8px;
  font-size: 10.5px; font-weight: 600; line-height: 1;
  color: var(--text-3);
  border-radius: var(--radius-pill);
  background: rgba(36,30,24,.04);
  border: 1px solid transparent;
}

.s-status.live {
  color: var(--green);
  font-weight: 500;
}

.s-status.offline {
  color: var(--gold);
}

.s-status.idle {
  color: var(--text-3);
}

.more-wrap {
  position: relative;
}

.more-trigger {
  font-size: 11px;
  color: var(--text-3);
  cursor: pointer;
}


.add-wrap {
  position: relative;
}

.add-menu {
  position: absolute;
  right: 0;
  top: calc(100% + 6px);
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  box-shadow: 0 12px 32px -8px rgba(36,30,24,.18);
  padding: 6px 0;
  min-width: 120px;
  z-index: 20;
}

.add-menu div {
  padding: 8px 18px;
  font-size: 12px;
  color: var(--text-1);
  cursor: pointer;
}

.add-menu div:hover {
  background: var(--bg);
}

</style>

<style>
/* Unscoped styles for Teleported elements */
.more-menu-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  z-index: 9998;
}

.more-menu-fixed {
  position: fixed;
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  box-shadow: 0 12px 32px -8px rgba(36,30,24,.18);
  padding: 6px 0;
  min-width: 140px;
  z-index: 9999;
}

.more-menu-fixed div {
  padding: 8px 18px;
  font-size: 12px;
  color: var(--text-1);
  cursor: pointer;
}

.more-menu-fixed div:hover {
  background: var(--bg);
}

.more-menu-fixed .danger {
  color: var(--red);
}

.djsmodal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(36,30,24,.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
}

.djsmodal {
  background: var(--card);
  border-radius: var(--radius);
  box-shadow: 0 16px 48px rgba(36,30,24,.22);
}

.djsmodal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--line);
  font-size: 15px;
  font-weight: 600;
}

.djsmodal-close {
  font-size: 20px;
  cursor: pointer;
  color: var(--text-3);
  line-height: 1;
}

.djsmodal-body {
  padding: 16px 20px;
}

.djsmodal-footer {
  display: flex;
  justify-content: center;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid var(--line);
}

.settings-form .form-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 14px;
  gap: 12px;
}

.settings-form .form-label {
  width: 90px;
  flex-shrink: 0;
  font-size: 13px;
  color: var(--text-1);
  line-height: 32px;
}

.settings-form .form-label.required::before {
  content: '* ';
  color: var(--red);
}

.settings-form .form-value {
  flex: 1;
}

.settings-form .form-value label {
  margin-right: 16px;
  font-size: 13px;
  cursor: pointer;
}

.settings-form .form-value.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.settings-form .form-tag {
  padding: 4px 12px;
  border: 1px solid var(--line);
  border-radius: var(--radius-xs);
  font-size: 12px;
  color: var(--text-1);
  cursor: pointer;
  transition: all .2s;
}

.settings-form .form-tag:hover {
  border-color: var(--brand);
  color: var(--brand);
}

.settings-form .form-tag.active {
  border-color: var(--brand);
  background: var(--brand-soft);
  color: var(--brand);
}

.settings-form .form-section-title {
  font-size: 13px;
  color: var(--orange);
  margin: 8px 0 14px;
  padding: 6px 10px;
  background: var(--gold-soft);
  border: 1px solid rgba(184,134,11,.18);
  border-radius: var(--radius-xs);
}
</style>
