<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Message, Modal } from '@arco-design/web-vue'
import * as analysisApi from '@/api/analysis'
import * as aiApi from '@/api/ai'
import * as recordingApi from '@/api/recording'
import * as comparisonApi from '@/api/comparison'
import type { AnalysisTask, AsrParagraph } from '@/api/analysis'
import type { ChatMessage, PresetQuestion } from '@/api/ai'
import type { RecordingDetail } from '@/api/recording'
import { formatDateTime, nowLocalIso } from '@/utils/format'
import ScriptBreakdown from '@/components/recap/ScriptBreakdown.vue'
import OptimizedText from '@/components/recap/OptimizedText.vue'
import AnalysisPanel from '@/components/recap/AnalysisPanel.vue'
import OptimizationDialog from '@/components/recap/OptimizationDialog.vue'
import ExportDialog from '@/components/recap/ExportDialog.vue'
import ClipDrawer from '@/components/clip/ClipDrawer.vue'
import DiagnosisDrawer from '@/views/recap/DiagnosisDrawer.vue'
import CustomVideoPlayer from '@/components/common/CustomVideoPlayer.vue'

const route = useRoute()
const router = useRouter()
const taskId = computed(() => Number(route.params.id))

// ---- v1.1.0：手动 AI 复盘 ----
const dailyQuota = ref<analysisApi.DailyQuotaStatus | null>(null)
const startingAi = ref(false)
let aiPollTimer: ReturnType<typeof setInterval> | null = null
let quotaRefreshTimer: ReturnType<typeof setTimeout> | null = null
const MAX_TIMER_DELAY = 2147483647

async function loadDailyQuota() {
  try {
    const q = (await analysisApi.getDailyQuota()) as any
    dailyQuota.value = q
  } catch {
    dailyQuota.value = null
  } finally {
    scheduleDailyQuotaRefresh()
  }
}

function parseQuotaResetTime(resetAt?: string | null): number {
  if (!resetAt) return 0
  const value = String(resetAt).trim()
  if (!value) return 0
  const normalized = value.includes('T') ? value : value.replace(' ', 'T')
  const time = new Date(normalized).getTime()
  return Number.isFinite(time) ? time : 0
}

function stopQuotaRefreshTimer() {
  if (quotaRefreshTimer) {
    clearTimeout(quotaRefreshTimer)
    quotaRefreshTimer = null
  }
}

function scheduleDailyQuotaRefresh() {
  stopQuotaRefreshTimer()
  const resetTime = parseQuotaResetTime(dailyQuota.value?.resetAt)
  if (!resetTime) return
  const delay = resetTime <= Date.now() ? 60000 : resetTime - Date.now() + 3000
  quotaRefreshTimer = setTimeout(() => {
    loadDailyQuota()
  }, Math.min(delay, MAX_TIMER_DELAY))
}

function refreshDailyQuotaOnFocus() {
  loadDailyQuota()
}

function refreshDailyQuotaWhenVisible() {
  if (document.visibilityState === 'visible') {
    loadDailyQuota()
  }
}

async function handleStartAiAnalysis() {
  if (!taskId.value) return
  startingAi.value = true
  try {
    await analysisApi.startAiAnalysis(taskId.value)
    Message.success('已开始 AI 复盘，请稍候')
    await loadDailyQuota()
    await fetchTask()
    startAiPolling()
  } catch (err: any) {
    const code = err?.code
    if (code === 40006) {
      Modal.warning({
        title: '今日 AI 复盘额度已用完',
        content: `每个账号每天最多 ${dailyQuota.value?.limit || 10} 次 AI 复盘。明天 0 点自动重置，届时可继续使用。`,
        okText: '我知道了',
        hideCancel: true,
      })
    } else {
      Message.error(err?.message || 'AI 复盘触发失败')
    }
  } finally {
    startingAi.value = false
  }
}

function startAiPolling() {
  if (aiPollTimer) return
  aiPollTimer = setInterval(async () => {
    await fetchTask()
    const s = task.value?.status
    if (s === 'completed' || s === 'failed') {
      stopAiPolling()
      if (s === 'completed') {
        Message.success('AI 复盘完成')
        fetchParagraphs()
      }
    }
  }, 5000)
}
function stopAiPolling() {
  if (aiPollTimer) { clearInterval(aiPollTimer); aiPollTimer = null }
}

const CLIP_CAT_MAP: Record<string, string> = {
  RETENTION: '留人切片', QUALITY_SPEECH: '优质话术', MARKETING: '营销塑品',
  INTERACTION: '互动切片', FAN_CLUB: '粉团切片', EXPRESSION: '表现力切片',
  COMPLIANCE: '规避违规', OTHER: '其他'
}
function clipCatLabel(code: string | null | undefined): string {
  return code ? (CLIP_CAT_MAP[code] || code) : ''
}

// Data
const task = ref<AnalysisTask | null>(null)
const recording = ref<RecordingDetail | null>(null)
const paragraphs = ref<AsrParagraph[]>([])
const loading = ref(false)

// Video
const videoUrl = ref('')
const videoRef = ref<InstanceType<typeof CustomVideoPlayer> | null>(null)

// Tabs
const contentTab = ref('paragraphs')   // 分钟段落 | AI脚本拆解 | 优化原文

// 是否处于"AI 还没分析"的前置状态（录制中/逐字稿生成中/未分析）
const isPreAiStatus = computed(() => {
  const s = task.value?.status
  return s === 'recording' || s === 'transcribing' || s === 'transcribed'
})

// AI assistant
const aiMessages = ref<ChatMessage[]>([])
const aiInput = ref('')
const aiLoading = ref(false)
const presets = ref<PresetQuestion[]>([])
const aiModel = ref('doubao')

// Dialogs
const showOptimization = ref(false)
const showExport = ref(false)
const showClipDrawer = ref(false)
const showDiagnosis = ref(false)
const comparingPrev = ref(false)
const showNoteDrawer = ref(false)
const noteContent = ref('')
const noteSaving = ref(false)
const noteLoaded = ref(false)

// Quick locate slider
const locateValue = ref(0)
const sliderDragging = ref(false) // prevent feedback loop

// Video ↔ paragraph sync
const currentTime = ref(0)
const currentParagraphId = ref<number | null>(null)
const paragraphsViewRef = ref<HTMLElement | null>(null)

// Computed
const isClip = computed(() => task.value?.type === 'clip')
const streamerName = computed(() => recording.value?.streamerInfo?.anchorName ?? '主播')
const streamerAvatar = computed(() => recording.value?.streamerInfo?.anchorAvatar ?? '')

const liveTimeRange = computed(() => {
  if (!recording.value) return { start: '', end: '' }
  return {
    start: recording.value.startTime ? formatDateTime(recording.value.startTime) : '',
    end: recording.value.endTime ? formatDateTime(recording.value.endTime) : ''
  }
})

const liveDuration = computed(() => {
  if (!recording.value?.duration) return ''
  const d = recording.value.duration
  const h = Math.floor(d / 3600)
  const m = Math.floor((d % 3600) / 60)
  const s = d % 60
  if (h > 0) return `${h}时${m}分${s}秒`
  if (m > 0) return `${m}分${s}秒`
  return `${s}秒`
})

const clipTimeRange = computed(() => {
  if (!isClip.value || !task.value) return ''
  const fmt = (s: number) => {
    const m = Math.floor(s / 60)
    const sec = s % 60
    return `${m}分${sec}秒`
  }
  return `${fmt(task.value.clipStart ?? 0)} ~ ${fmt(task.value.clipEnd ?? 0)}`
})

const clipDuration = computed(() => {
  if (!isClip.value || !task.value) return ''
  const d = (task.value.clipEnd ?? 0) - (task.value.clipStart ?? 0)
  const m = Math.floor(d / 60)
  const s = d % 60
  if (m > 0) return `${m}分${s}秒`
  return `${s}秒`
})

const durationMinutes = computed(() => {
  if (!recording.value?.duration) return 30
  return Math.ceil(recording.value.duration / 60)
})

// Functions
async function fetchTask() {
  loading.value = true
  try {
    const res = await analysisApi.getAnalysis(taskId.value)
    task.value = (res as any).data ?? res
    if (task.value?.recordingId) fetchRecording(task.value.recordingId)
  } catch { Message.error('加载分析任务失败') }
  finally { loading.value = false }
}

async function fetchRecording(recordingId: number) {
  try {
    const res = await recordingApi.getRecording(recordingId)
    recording.value = (res as any).data ?? res
    // For clip tasks with a dedicated clip file, use that; otherwise use original recording
    const clipPath = isClip.value ? task.value?.clipFilePath : null
    const localPath = clipPath || recording.value?.localFilePath
    if (localPath) {
      const api = (window as any).electronAPI
      if (api?.getLocalVideoUrl) {
        const url = await api.getLocalVideoUrl(localPath)
        videoUrl.value = url
      } else {
        videoUrl.value = `local-video://video?path=${encodeURIComponent(localPath)}`
      }
    }
  } catch (e) { console.warn('[RecapDetail] fetch recording failed:', e) }
}

async function fetchParagraphs() {
  try {
    const res = await analysisApi.getParagraphs(taskId.value, 1, 500)
    const data = (res as any).data ?? res
    paragraphs.value = data.items || []
  } catch { paragraphs.value = [] }
}

async function fetchPresets() {
  try {
    const res = await aiApi.getPresets('operation')
    presets.value = (res as any).data ?? res ?? []
  } catch { presets.value = [] }
}

async function fetchChatHistory() {
  try {
    const res = await aiApi.getChatHistory({ taskId: taskId.value, assistantType: 'operation' })
    const data = (res as any).data ?? res
    aiMessages.value = data.items || []
  } catch { aiMessages.value = [] }
}

async function sendMessage(message?: string) {
  const text = message || aiInput.value.trim()
  if (!text) return
  aiLoading.value = true
  aiInput.value = ''
  aiMessages.value.push({
    id: Date.now(), role: 'user', content: text,
    thinking: null, tokensUsed: null, presetQuestionId: null,
    createdAt: nowLocalIso()
  })
  try {
    const res = await aiApi.sendChat({ taskId: taskId.value, assistantType: 'operation', message: text, aiModel: aiModel.value })
    aiMessages.value.push((res as any).data ?? res)
  } catch (err: any) {
    aiMessages.value.pop()
    if (err?.code === 40006) {
      Message.warning('今日 AI 额度已用完（每天 10 次，含录制复盘、切片、AI 助手提问），明天 0 点自动重置')
    } else {
      Message.error(err?.message || 'AI 回复失败')
    }
  }
  finally { aiLoading.value = false }
}

function onPresetClick(preset: PresetQuestion) { sendMessage(preset.title) }
function goBack() { router.push({ name: isClip.value ? 'ClipRecapList' : 'FullRecapList' }) }

function timeToSeconds(timeStr: string): number {
  const parts = timeStr.split(':').map(Number)
  if (parts.length === 3) return parts[0] * 3600 + parts[1] * 60 + parts[2]
  if (parts.length === 2) return parts[0] * 60 + parts[1]
  return parts[0] || 0
}

function seekTo(timeStr: string) {
  videoRef.value?.seekTo(timeToSeconds(timeStr))
}

/** Video timeupdate → sync slider + highlight paragraph */
function onTimeUpdate(t: number) {
  currentTime.value = t

  // Update slider (in minutes) unless user is dragging
  if (!sliderDragging.value) {
    locateValue.value = Math.round(t / 60)
  }

  // Find current paragraph
  let found: number | null = null
  for (const p of paragraphs.value) {
    const start = timeToSeconds(p.startTime)
    const end = timeToSeconds(p.endTime)
    if (t >= start && t < end) { found = p.id; break }
  }
  if (found !== currentParagraphId.value) {
    currentParagraphId.value = found
    // Auto-scroll to active paragraph
    if (found !== null) {
      nextTick(() => {
        const el = paragraphsViewRef.value?.querySelector('.para-item.active') as HTMLElement
        if (el) el.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
      })
    }
  }
}

/** Slider drag → seek video */
watch(locateValue, (val) => {
  if (!sliderDragging.value) return
  videoRef.value?.seekTo(val * 60, false)
})

function goToAssistant(type: 'operation' | 'compliance' | 'script') {
  const nameMap = { operation: 'AssistantOperation', compliance: 'AssistantCompliance', script: 'AssistantScript' }
  router.push({ name: nameMap[type], query: { taskId: taskId.value } })
}

/** Mark slider as being dragged (mousedown on slider area) */
function onSliderMousedown() { sliderDragging.value = true }
function onSliderMouseup() {
  if (sliderDragging.value) {
    videoRef.value?.seekTo(locateValue.value * 60, false)
  }
  sliderDragging.value = false
}

/** Compare with previous recording of same streamer */
async function compareWithPrevious() {
  const streamerId = recording.value?.streamerInfo?.id
  const currentRecordingId = task.value?.recordingId
  if (!streamerId || !currentRecordingId) {
    Message.warning('缺少主播或录制信息')
    return
  }

  comparingPrev.value = true
  try {
    // Fetch recent completed recordings for this streamer
    const res = await recordingApi.getRecordings({
      streamerId,
      status: 'completed',
      page: 1,
      size: 10,
    })
    const data = (res as any).data ?? res
    const items = data.items || []

    // Find the previous recording (exclude current)
    const prev = items.find((r: any) => r.id !== currentRecordingId)
    if (!prev) {
      Message.warning('未找到同主播的上一场录制记录')
      return
    }

    // Create comparison: current = optimize, previous = reference
    const compRes = await comparisonApi.createComparison({
      recordingIdOptimize: currentRecordingId,
      recordingIdReference: prev.id,
      type: 'full',
    })
    const comp = (compRes as any).data ?? compRes
    if (comp?.id) {
      Message.success('对比创建成功，正在跳转...')
      router.push({ name: 'ComparisonDetail', params: { id: comp.id } })
    } else {
      Message.error('创建对比失败')
    }
  } catch (e: any) {
    Message.error(e?.response?.data?.message || '创建对比失败')
  } finally {
    comparingPrev.value = false
  }
}

/** Copy current tab's text to clipboard */
function copyOriginalText() {
  let text = ''
  if (contentTab.value === 'paragraphs') {
    text = paragraphs.value.map(p => `[${p.startTime}] ${p.textContent}`).join('\n')
  } else if (contentTab.value === 'script') {
    // Group by scriptCategory
    const groups: Record<string, typeof paragraphs.value> = {}
    for (const p of paragraphs.value) {
      const cat = p.scriptCategory || '其他'
      if (!groups[cat]) groups[cat] = []
      groups[cat].push(p)
    }
    const parts: string[] = []
    for (const [cat, items] of Object.entries(groups)) {
      parts.push(`【${cat}】(${items.length}段)`)
      for (const p of items) {
        parts.push(`  [${p.startTime}] ${p.textContent}`)
      }
      parts.push('')
    }
    text = parts.join('\n')
  } else if (contentTab.value === 'optimized') {
    text = task.value?.optimizedText || paragraphs.value.map(p => p.textContent).join('\n')
  }
  if (!text) {
    Message.warning('当前无内容可复制')
    return
  }
  navigator.clipboard.writeText(text).then(() => {
    Message.success('已复制到剪贴板')
  }).catch(() => {
    Message.error('复制失败')
  })
}

/** Open note drawer and load existing notes */
async function openNoteDrawer() {
  showNoteDrawer.value = true
  if (!noteLoaded.value) {
    try {
      const res = await analysisApi.getNotes(taskId.value, contentTab.value)
      const data = (res as any).data ?? res
      noteContent.value = data?.contentHtml || ''
      noteLoaded.value = true
    } catch { noteContent.value = '' }
  }
}

/** Save note */
async function saveNote() {
  noteSaving.value = true
  try {
    await analysisApi.saveNotes(taskId.value, {
      tabType: contentTab.value,
      contentHtml: noteContent.value
    })
    Message.success('笔记已保存')
  } catch {
    Message.error('保存失败')
  } finally {
    noteSaving.value = false
  }
}

// Reset note when switching tabs
watch(contentTab, () => {
  noteLoaded.value = false
  noteContent.value = ''
})

onMounted(async () => {
  await fetchTask(); fetchParagraphs(); fetchPresets(); fetchChatHistory()
  loadDailyQuota()
  window.addEventListener('focus', refreshDailyQuotaOnFocus)
  document.addEventListener('visibilitychange', refreshDailyQuotaWhenVisible)
  // 如果已经在 ai_processing 状态，自动开始轮询等结果
  if (task.value?.status === 'ai_processing') startAiPolling()
  document.addEventListener('mouseup', onSliderMouseup)
})
onUnmounted(() => {
  stopAiPolling()
  stopQuotaRefreshTimer()
  window.removeEventListener('focus', refreshDailyQuotaOnFocus)
  document.removeEventListener('visibilitychange', refreshDailyQuotaWhenVisible)
  document.removeEventListener('mouseup', onSliderMouseup)
})
</script>

<template>
  <div class="recap-detail">

    <!-- ===== 1. 顶部导航栏 ===== -->
    <div class="top-nav">
      <span class="back-link" @click="goBack">&lsaquo; {{ isClip ? '切片复盘' : '复盘分析' }}</span>
    </div>

    <!-- ===== 2. 主播信息栏 ===== -->
    <div class="pg-title">
      <div class="pt-main">
        <div class="av av-lg av-1" v-if="!streamerAvatar">{{ streamerName.charAt(0) }}</div>
        <img v-else :src="streamerAvatar" class="av av-lg" style="object-fit:cover" />
        <div class="pt-info">
          <div class="pt-name">{{ streamerName }}</div>
          <div class="pt-tags"><span class="tg tg-b">直播专号</span></div>
        </div>
      </div>
      <div class="pt-actions">
        <button class="djsbtn ghost" @click="showExport = true">导出话术</button>
        <template v-if="!isClip">
          <button class="djsbtn ghost" :disabled="comparingPrev" @click="compareWithPrevious">{{ comparingPrev ? '对比中...' : '对比上一场' }}</button>
          <button class="djsbtn ghost" @click="showClipDrawer = true">切片</button>
        </template>
      </div>
    </div>

    <!-- ===== 2.5 AI 复盘 Banner（v1.1.0） ===== -->
    <div v-if="task && (task.status === 'transcribed' || task.status === 'transcribing' || task.status === 'ai_processing' || task.status === 'failed')"
         class="ai-analyze-banner"
         :class="{
           'banner-transcribed': task.status === 'transcribed',
           'banner-processing': task.status === 'ai_processing' || task.status === 'transcribing',
           'banner-failed': task.status === 'failed'
         }">
      <div class="ab-left">
        <template v-if="task.status === 'transcribing'">
          <span class="ab-icon">⏳</span>
          <div class="ab-text">
            <div class="ab-title">逐字稿生成中...</div>
            <div class="ab-sub">桌面端正在本地转写，完成后可点击"AI 复盘"</div>
          </div>
        </template>
        <template v-else-if="task.status === 'transcribed'">
          <span class="ab-icon">📝</span>
          <div class="ab-text">
            <div class="ab-title">逐字稿已生成（未分析），可点右侧按钮触发 AI 复盘</div>
            <div class="ab-sub" v-if="dailyQuota && !dailyQuota.unlimited">
              今日额度：<strong>{{ dailyQuota.used }} / {{ dailyQuota.limit }}</strong>
              <span v-if="dailyQuota.used >= dailyQuota.limit" style="color:#f53f3f;margin-left:8px">已用完，明日 0 点重置</span>
            </div>
            <div class="ab-sub" v-else-if="dailyQuota?.unlimited">您的账号不受每日限额</div>
          </div>
        </template>
        <template v-else-if="task.status === 'ai_processing'">
          <span class="ab-icon">🤖</span>
          <div class="ab-text">
            <div class="ab-title">AI 分析中，请稍候...</div>
            <div class="ab-sub">通常需要 30 秒到 2 分钟，完成后其他 Tab 会自动显示</div>
          </div>
        </template>
        <template v-else-if="task.status === 'failed'">
          <span class="ab-icon">⚠️</span>
          <div class="ab-text">
            <div class="ab-title">AI 分析失败</div>
            <div class="ab-sub">{{ task.errorMsg || '请稍后重试' }}</div>
          </div>
        </template>
      </div>
      <div class="ab-right">
        <button
          v-if="task.status === 'transcribed' || task.status === 'failed'"
          class="djsbtn"
          :disabled="startingAi || (dailyQuota && !dailyQuota.unlimited && dailyQuota.used >= dailyQuota.limit)"
          @click="handleStartAiAnalysis"
        >{{ startingAi ? '提交中...' : (task.status === 'failed' ? '重试 AI 复盘' : '立即 AI 复盘') }}</button>
      </div>
    </div>

    <!-- ===== 3. 时间 & 敏感词信息栏 ===== -->
    <div class="info-bar">
      <template v-if="isClip">
        <div class="ib-item"><span class="lbl">切片</span><span class="val">{{ task?.clipFilename || '未命名切片' }}</span></div>
        <div class="ib-item"><span class="lbl">时段</span><span class="val">{{ clipTimeRange }}</span></div>
        <div class="ib-item dur"><span class="lbl">时长</span><span class="val">{{ clipDuration }}</span></div>
        <span v-if="task?.clipCategory" class="tg tg-or">{{ clipCatLabel(task.clipCategory) }}</span>
      </template>
      <template v-else>
        <div class="ib-item"><span class="lbl">开始</span><span class="val">{{ liveTimeRange.start }}</span></div>
        <div class="ib-item"><span class="lbl">结束</span><span class="val">{{ liveTimeRange.end }}</span></div>
        <div class="ib-item dur"><span class="lbl">时长</span><span class="val">{{ liveDuration }}</span></div>
      </template>
      <div class="ib-sens"><span class="d" /><span>敏感词 {{ task?.sensitiveCount ?? 0 }}次 · 点击标注</span></div>
    </div>

    <!-- ===== 4. 主体区域 ===== -->
    <div class="main-body">

      <!-- == 左列：视频播放器 == -->
      <div class="video-col">
        <div class="video-title-bar">
          <span class="vt-label">{{ isClip ? '切片回放' : '直播画面' }}</span>
          <span v-if="isClip" style="font-size:12px;color:var(--text-3);margin-left:8px">{{ clipTimeRange }}</span>
          <a-link size="small">打开视频/点击刷新</a-link>
        </div>
        <div class="video-wrapper">
          <CustomVideoPlayer
            v-if="videoUrl"
            ref="videoRef"
            :src="videoUrl"
            preload="metadata"
            @timeupdate="onTimeUpdate"
          />
          <div v-else class="video-empty">
            <icon-play-circle :size="48" />
          </div>
        </div>
      </div>

      <!-- == 右列：内容区域 == -->
      <div class="content-col">

        <!-- 快速定位 -->
        <div class="quick-locate">
          <span class="ql-icon">&#9679;</span>
          <span class="ql-label">快速定位</span>
          <div class="ql-slider" @mousedown="onSliderMousedown">
            <a-slider
              v-model="locateValue"
              :min="0"
              :max="durationMinutes"
              :step="1"
              :style="{ flex: 1 }"
            />
          </div>
          <span class="ql-time-start">00:00</span>
          <span class="ql-time-end">{{ durationMinutes }}min</span>
        </div>

        <!-- 内容Tab栏 -->
        <div class="content-tab-bar">
          <div class="ct-left">
            <span class="ct-tab" :class="{ active: contentTab === 'paragraphs' }" @click="contentTab = 'paragraphs'">分钟段落</span>
            <span class="ct-tab" :class="{ active: contentTab === 'script' }" @click="contentTab = 'script'">
              AI脚本拆解
            </span>
            <span class="ct-tab" :class="{ active: contentTab === 'optimized' }" @click="contentTab = 'optimized'">优化原文</span>
          </div>
          <div class="ct-right">
            <a-button size="mini" type="outline" @click="copyOriginalText">复制原文</a-button>
            <a-button size="mini" type="outline" @click="openNoteDrawer">笔记+批注</a-button>
          </div>
        </div>

        <!-- 内容区域 -->
        <div class="content-body" :class="{ 'content-body--full': contentTab !== 'paragraphs' }">
          <!-- 分钟段落 -->
          <div v-if="contentTab === 'paragraphs'" ref="paragraphsViewRef" class="paragraphs-view">
            <div v-if="paragraphs.length === 0" class="empty-state">
              <a-empty description="暂无段落数据" />
            </div>
            <div v-for="p in paragraphs" :key="p.id" class="para-item" :class="{ active: currentParagraphId === p.id }" @click="seekTo(p.startTime)">
              <a-avatar :size="28" class="para-avatar" :image-url="streamerAvatar || undefined">{{ streamerAvatar ? '' : streamerName.charAt(0) }}</a-avatar>
              <div class="para-content">
                <span class="para-time">{{ p.startTime }}</span>
                <div class="para-text">{{ p.textContent }}</div>
              </div>
            </div>
          </div>

          <!-- AI脚本拆解 -->
          <div v-if="contentTab === 'script'" class="tab-content">
            <div v-if="isPreAiStatus" class="ai-empty-state">
              <a-empty description="AI 尚未分析，暂无脚本拆解内容">
                <a-button type="primary" :loading="startingAi" :disabled="task?.status !== 'transcribed'" @click="handleStartAiAnalysis">
                  {{ task?.status === 'transcribed' ? '立即 AI 复盘' : '等待逐字稿生成' }}
                </a-button>
              </a-empty>
            </div>
            <template v-else>
              <div class="tab-notice">注意：经过AI整理，基于千万级场次数据训练，按照一线直播间优质话术类型归类，对直播稿进行功能性话术拆解，方便运营和主播快速复盘。</div>
              <ScriptBreakdown :task-id="taskId" :paragraphs="paragraphs" @refresh="fetchParagraphs" />
            </template>
          </div>

          <!-- 优化原文 -->
          <div v-if="contentTab === 'optimized'" class="tab-content">
            <div v-if="isPreAiStatus" class="ai-empty-state">
              <a-empty description="AI 尚未分析，暂无优化原文">
                <a-button type="primary" :loading="startingAi" :disabled="task?.status !== 'transcribed'" @click="handleStartAiAnalysis">
                  {{ task?.status === 'transcribed' ? '立即 AI 复盘' : '等待逐字稿生成' }}
                </a-button>
              </a-empty>
            </div>
            <template v-else>
              <div class="tab-notice">注意：优化原文是经过AI整理，在原文基础上进行了一定的话术优化，作为您优化稿件的参考，推荐自有账号使用。</div>
              <OptimizedText :task-id="taskId" :paragraphs="paragraphs" :optimized-text="task?.optimizedText" />
            </template>
          </div>
        </div>

        <!-- 底部分析区（仅分钟段落 tab 下显示） -->
        <div v-if="contentTab === 'paragraphs'" class="bottom-area">
          <AnalysisPanel v-if="task" :task-id="taskId" :task="task" :paragraphs="paragraphs" />
          <a-empty v-if="!task && !loading" description="暂无数据" />
        </div>
      </div>

      <!-- == 右侧悬浮AI助手按钮 == -->
      <div class="ai-float-btns">
        <div class="ai-float-btn purple" @click="goToAssistant('operation')">
          <span class="afb-icon">AI</span>
          <span class="afb-text">AI运营助手</span>
        </div>
        <div class="ai-float-btn red" @click="goToAssistant('compliance')">
          <span class="afb-icon">AI</span>
          <span class="afb-text">AI违规助手</span>
        </div>
        <div class="ai-float-btn orange" @click="goToAssistant('script')">
          <span class="afb-icon">AI</span>
          <span class="afb-text">AI话术助手</span>
        </div>
      </div>
    </div>

    <!-- ===== Dialogs & Drawers ===== -->
    <OptimizationDialog v-model:visible="showOptimization" :task-id="taskId" />
    <ExportDialog
      v-model:visible="showExport"
      :task-id="taskId"
      :task="task"
      :paragraphs="paragraphs"
      :streamer-name="streamerName"
    />
    <ClipDrawer
      v-model:visible="showClipDrawer"
      :recording-id="task?.recordingId ?? 0"
      :duration="recording?.duration ?? 3600"
      :video-file-path="recording?.localFilePath ?? ''"
      @created="fetchTask()"
    />
    <DiagnosisDrawer v-model:visible="showDiagnosis" :task-id="taskId" />

    <!-- 笔记+批注抽屉 -->
    <a-drawer
      :visible="showNoteDrawer"
      title="笔记 + 批注"
      :width="480"
      @cancel="showNoteDrawer = false"
      :footer="false"
      unmount-on-close
    >
      <div class="note-drawer-body">
        <div class="note-tab-hint">
          当前标签：<a-tag size="small" color="arcoblue">{{ contentTab === 'paragraphs' ? '分钟段落' : contentTab === 'script' ? 'AI脚本拆解' : '优化原文' }}</a-tag>
        </div>
        <a-textarea
          v-model="noteContent"
          placeholder="在此输入笔记或批注内容..."
          :auto-size="{ minRows: 12, maxRows: 24 }"
          allow-clear
          class="note-textarea"
        />
        <div class="note-actions">
          <a-button type="primary" :loading="noteSaving" @click="saveNote">保存笔记</a-button>
          <a-button @click="showNoteDrawer = false">关闭</a-button>
        </div>
      </div>
    </a-drawer>
  </div>
</template>

<style scoped lang="scss">
/* ===== 容器 ===== */
.recap-detail {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: var(--card);
}

/* ===== 1. 顶部导航栏 ===== */
.ai-analyze-banner {
  margin: 0 0 12px; padding: 14px 18px;
  border-radius: 8px; display: flex; justify-content: space-between; align-items: center;
  gap: 16px; border: 1px solid transparent;
}
.ai-analyze-banner.banner-transcribed {
  background: rgba(224, 123, 0, 0.08); border-color: rgba(224, 123, 0, 0.3);
}
.ai-analyze-banner.banner-processing {
  background: rgba(52, 145, 250, 0.08); border-color: rgba(52, 145, 250, 0.3);
}
.ai-analyze-banner.banner-failed {
  background: rgba(245, 63, 63, 0.08); border-color: rgba(245, 63, 63, 0.3);
}
.ai-analyze-banner .ab-left { display: flex; align-items: center; gap: 14px; flex: 1; }
.ai-analyze-banner .ab-icon { font-size: 28px; line-height: 1; }
.ai-analyze-banner .ab-title { font-size: 14px; font-weight: 700; margin-bottom: 4px; }
.ai-analyze-banner .ab-sub { font-size: 12.5px; color: var(--text-2b); }
.ai-analyze-banner .ab-right .djsbtn { min-width: 120px; }

.top-nav {
  height: 40px;
  padding: 0 20px;
  background: var(--card);
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.back-link {
  font-size: 14px;
  color: var(--text-2);
  cursor: pointer;
  &:hover { color: var(--brand); }
}

/* ===== 2. 主播信息栏（pg-title 卡片） ===== */
.pg-title {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  margin-bottom: 14px;
  flex-wrap: wrap;
}
.pg-title .pt-main {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 200px;
}
.pg-title .pt-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.pg-title .pt-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-1);
  letter-spacing: -.01em;
  line-height: 1.2;
}
.pg-title .pt-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  align-items: center;
}
.pg-title .pt-actions {
  display: flex;
  gap: 7px;
  margin-left: auto;
}

/* Avatar variants */
.av { width: 36px; height: 36px; border-radius: 9px; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 700; color: #fff; flex-shrink: 0; position: relative; overflow: hidden; }
.av::after { content: ''; position: absolute; inset: 0; background: linear-gradient(180deg,rgba(255,255,255,.22),transparent 55%); border-radius: inherit; }
.av-lg { width: 46px; height: 46px; font-size: 16px; border-radius: 12px; }
.av-1 { background: linear-gradient(140deg,#6B8DD6,#5570B8); }

/* Tags */
.tg { display: inline-flex; align-items: center; height: 20px; padding: 0 9px; border-radius: var(--radius-pill); font-size: 11px; font-weight: 650; letter-spacing: .02em; }
.tg-b { background: var(--blue2-soft, rgba(91,123,181,.07)); color: var(--blue2, #5B7BB5); }
.tg-or { background: var(--orange-soft); color: var(--orange); }

/* ===== 3. 时间 & 敏感词信息栏 ===== */
.info-bar {
  display: flex;
  align-items: center;
  gap: 0;
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  padding: 10px 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  box-shadow: 0 1px 2px rgba(100,116,145,.04);
}
.info-bar .ib-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: var(--text-3);
  padding: 0 14px;
  border-right: 1px solid var(--line-2);
  font-family: var(--fm);
}
.info-bar .ib-item:first-child { padding-left: 0; }
.info-bar .ib-item .lbl { color: var(--text-3); font-family: var(--ff); }
.info-bar .ib-item .val { color: var(--text-2); font-weight: 600; }
.info-bar .ib-item.dur .val { color: var(--brand); font-weight: 700; }
.info-bar .ib-sens {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 10px;
  background: var(--red-soft);
  border-radius: var(--radius-pill);
  font-size: 12px;
  color: var(--red);
  margin-left: auto;
  cursor: pointer;
  transition: all .15s ease;
}
.info-bar .ib-sens:hover { background: rgba(229,62,62,.1); }
.info-bar .ib-sens .d { width: 6px; height: 6px; background: var(--red); border-radius: 50%; }

.sens-link {
  font-size: 12px;
  margin-left: 8px;
}

.sens-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--brand);
  cursor: pointer;
}

/* ===== 4. 主体区域 ===== */
.main-body {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
  position: relative;
}

/* -- 视频列 -- */
.video-col {
  width: 300px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--line);
  background: var(--card);
}

.video-title-bar {
  height: 32px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--text-1);
  flex-shrink: 0;
}

.vt-label {
  font-size: 12px;
  color: #fff;
  font-weight: 500;
}

.video-wrapper {
  flex: 1;
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 0;
}

.video-el {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.video-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.3);
  width: 100%;
  height: 100%;
}

/* -- 内容列 -- */
.content-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

/* 快速定位 */
.quick-locate {
  height: 42px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid var(--line);
  flex-shrink: 0;
}

.ql-icon {
  color: var(--brand);
  font-size: 10px;
}

.ql-label {
  font-size: 13px;
  color: var(--text-1);
  font-weight: 500;
  white-space: nowrap;
}

.ql-slider {
  flex: 1;
  padding: 0 8px;
}

.ql-time-start, .ql-time-end {
  font-size: 11px;
  color: var(--text-3);
  white-space: nowrap;
}

/* 内容Tab栏 */
.content-tab-bar {
  height: 38px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid var(--line);
  flex-shrink: 0;
}

.ct-left {
  display: flex;
  gap: 0;
}

.ct-tab {
  padding: 0 14px;
  height: 38px;
  line-height: 36px;
  font-size: 13px;
  color: var(--text-3);
  cursor: pointer;
  white-space: nowrap;
  border-bottom: 2px solid transparent;
  border: 1px solid var(--line);
  border-bottom: none;
  border-radius: 4px 4px 0 0;
  margin-right: -1px;
  background: var(--hov);

  &.active {
    color: var(--text-1);
    font-weight: 500;
    background: var(--card);
    border-bottom: 2px solid var(--brand);
  }

  &:hover:not(.active) { color: var(--brand); }
}

.ct-right {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

/* 内容区域 */
.content-body {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.content-body.content-body--full {
  /* AI脚本拆解/优化原文时，占满剩余空间（无底部分析区） */
  flex: 1;
}

.tab-notice {
  padding: 12px 16px;
  font-size: 12px;
  color: var(--text-3);
  line-height: 1.5;
  background: var(--bg);
  border-bottom: 1px solid var(--line);
}

.tab-content {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow-y: auto;

  > :deep(*:not(.tab-notice)) {
    padding: 16px;
  }
}

/* AI 未分析时的占位（脚本拆解 / 优化原文 tab） */
.ai-empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 16px;
}

/* 分钟段落视图 */
.paragraphs-view {
  padding: 12px 16px;
}

.para-item {
  display: flex;
  gap: 10px;
  padding: 8px 0;
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.15s;

  &:hover { background: var(--hov); }

  &.active {
    background: var(--brand-soft-12);
    .para-text { color: var(--brand); }
    .para-time { color: var(--brand); font-weight: 500; }
  }
}

.para-avatar {
  flex-shrink: 0;
  background: linear-gradient(135deg, var(--brand), var(--brand-dark));
  color: #fff;
  font-size: 11px;
  margin-top: 2px;
}

.para-content {
  flex: 1;
  min-width: 0;
}

.para-time {
  font-size: 11px;
  color: var(--text-3);
  font-family: var(--fm);
}

.para-text {
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-1);
  margin-top: 4px;
}

/* -- 右侧悬浮AI助手 -- */
.ai-float-btns {
  position: fixed;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  gap: 0;
  z-index: 80;
  background: none;
  border: none;
  box-shadow: none;
}
.ai-float-btns:hover .ai-float-btn { gap: 6px; padding: 6px 12px 6px 8px; }
.ai-float-btns:hover .afb-text { max-width: 80px; opacity: 1; }

.ai-float-btn {
  display: flex;
  align-items: center;
  gap: 0;
  padding: 6px 8px;
  border-radius: var(--radius-pill, 100px) 0 0 var(--radius-pill, 100px);
  cursor: pointer;
  box-shadow: -2px 2px 10px rgba(36,30,24,.12);
  font-size: 12.5px;
  font-weight: 600;
  color: #fff;
  overflow: hidden;


  &.purple { background: linear-gradient(135deg, var(--brand), #6B8DD6); }
  &.red { background: linear-gradient(135deg, var(--red, #E53E3E), var(--orange, #E07C24)); }
  &.orange { background: linear-gradient(135deg, var(--green, #1A9955), #4BA0E0); }
}

.afb-icon {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(255,255,255,0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  flex-shrink: 0;
}

.afb-text {
  white-space: nowrap;
  font-size: 11px;
  max-width: 0;
  opacity: 0;
  overflow: hidden;
}

/* ===== 5. 底部分析区（在右侧 content-col 内部） ===== */
.bottom-area {
  border-top: 1px solid var(--line);
  background: var(--card);
  overflow-y: auto;
  height: 260px;
  flex-shrink: 0;
  padding: 0 16px;
}

/* ===== 共用 ===== */
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 0;
}

/* ===== 笔记抽屉 ===== */
.note-drawer-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
}

.note-tab-hint {
  font-size: 13px;
  color: var(--text-2b);
  display: flex;
  align-items: center;
  gap: 8px;
}

.note-textarea {
  flex: 1;
}

.note-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
</style>
