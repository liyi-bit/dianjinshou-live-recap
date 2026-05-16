<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import * as aiApi from '@/api/ai'
import * as analysisApi from '@/api/analysis'
import * as recordingApi from '@/api/recording'
import * as comparisonApi from '@/api/comparison'
import { formatDateTime, nowLocalIso } from '@/utils/format'
import CustomVideoPlayer from '@/components/common/CustomVideoPlayer.vue'
import type { ChatMessage, PresetQuestion } from '@/api/ai'
import type { AnalysisTask, AsrParagraph } from '@/api/analysis'
import type { RecordingDetail } from '@/api/recording'
import type { ComparisonItem } from '@/api/comparison'

const route = useRoute()
const router = useRouter()

const CLIP_CAT_MAP: Record<string, string> = {
  RETENTION: '留人切片', QUALITY_SPEECH: '优质话术', MARKETING: '营销塑品',
  INTERACTION: '互动切片', FAN_CLUB: '粉团切片', EXPRESSION: '表现力切片',
  COMPLIANCE: '规避违规', OTHER: '其他'
}
function clipCatLabel(code: string | null | undefined): string {
  return code ? (CLIP_CAT_MAP[code] || code) : '--'
}

// Mode detection
const taskId = computed(() => route.query.taskId ? Number(route.query.taskId) : null)
const comparisonId = computed(() => route.query.comparisonId ? Number(route.query.comparisonId) : null)
const isComparison = computed(() => !!comparisonId.value)

// Data: single recap mode
const task = ref<AnalysisTask | null>(null)
const recording = ref<RecordingDetail | null>(null)
const paragraphs = ref<AsrParagraph[]>([])
const videoUrl = ref('')

// Data: comparison mode
const comparison = ref<ComparisonItem | null>(null)
const recordingOpt = ref<RecordingDetail | null>(null)
const recordingRef = ref<RecordingDetail | null>(null)
const clipTaskOpt = ref<AnalysisTask | null>(null)
const clipTaskRef = ref<AnalysisTask | null>(null)
const videoUrlOpt = ref('')
const videoUrlRef = ref('')
const paragraphsOpt = ref<AsrParagraph[]>([])
const paragraphsRef = ref<AsrParagraph[]>([])
const videoElOpt = ref<InstanceType<typeof CustomVideoPlayer> | null>(null)
const videoElRef = ref<InstanceType<typeof CustomVideoPlayer> | null>(null)

// AI chat
const aiMessages = ref<ChatMessage[]>([])
const aiInput = ref('')
const aiLoading = ref(false)
const presets = ref<PresetQuestion[]>([])
const aiModel = ref('glm4')
const messagesRef = ref<HTMLElement | null>(null)

// Computed helpers
const streamerName = computed(() => recording.value?.streamerInfo?.anchorName ?? '主播')
const streamerAvatar = computed(() => recording.value?.streamerInfo?.anchorAvatar ?? '')
const streamerNameOpt = computed(() => recordingOpt.value?.streamerInfo?.anchorName ?? '主播A')
const streamerAvatarOpt = computed(() => recordingOpt.value?.streamerInfo?.anchorAvatar ?? '')
const streamerNameRef = computed(() => recordingRef.value?.streamerInfo?.anchorName ?? '主播B')
const streamerAvatarRef = computed(() => recordingRef.value?.streamerInfo?.anchorAvatar ?? '')
const isClipComparison = computed(() => comparison.value?.type === 'clip')
const clipDurationOpt = computed(() => {
  if (!clipTaskOpt.value) return null
  return (clipTaskOpt.value.clipEnd ?? 0) - (clipTaskOpt.value.clipStart ?? 0)
})
const clipDurationRef = computed(() => {
  if (!clipTaskRef.value) return null
  return (clipTaskRef.value.clipEnd ?? 0) - (clipTaskRef.value.clipStart ?? 0)
})

function formatTime(t: string | null | undefined): string {
  return formatDateTime(t)
}

function formatDuration(d: number | null | undefined): string {
  if (!d) return '--'
  const h = Math.floor(d / 3600)
  const m = Math.floor((d % 3600) / 60)
  const s = d % 60
  if (h > 0) return `${h}时${m}分${s}秒`
  if (m > 0) return `${m}分${s}秒`
  return `${s}秒`
}

function formatFileSize(bytes: number | null | undefined): string {
  if (!bytes) return '--'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}

function parseTimeToSeconds(t: string | null | undefined): number {
  if (!t) return 0
  const parts = t.split(':').map(Number)
  if (parts.length === 3) return parts[0] * 3600 + parts[1] * 60 + parts[2]
  if (parts.length === 2) return parts[0] * 60 + parts[1]
  return parts[0] || 0
}

function seekComparisonVideo(side: 'opt' | 'ref', paragraph: AsrParagraph) {
  const el = side === 'opt' ? videoElOpt.value : videoElRef.value
  el?.seekTo(parseTimeToSeconds(paragraph.startTime))
}

async function getVideoUrl(localPath: string | null | undefined): Promise<string> {
  if (!localPath) return ''
  const api = (window as any).electronAPI
  if (api?.getLocalVideoUrl) {
    return await api.getLocalVideoUrl(localPath)
  }
  return `local-video://video?path=${encodeURIComponent(localPath)}`
}

// Fetch: single mode
async function fetchSingleData() {
  if (!taskId.value) return
  try {
    const res = await analysisApi.getAnalysis(taskId.value)
    task.value = (res as any).data ?? res
    if (task.value?.recordingId) {
      const rRes = await recordingApi.getRecording(task.value.recordingId)
      recording.value = (rRes as any).data ?? rRes
      videoUrl.value = await getVideoUrl(recording.value?.localFilePath)
    }
  } catch { Message.error('加载录制信息失败') }
  try {
    const res = await analysisApi.getParagraphs(taskId.value, 1, 500)
    const data = (res as any).data ?? res
    paragraphs.value = data.items || []
  } catch { paragraphs.value = [] }
}

async function resolveTaskId(taskId: number | null | undefined, recordingId: number, type: string): Promise<number | null> {
  if (taskId) return taskId
  const statusFallback = ['completed', 'transcribed', 'ai_processing', 'failed']
  try {
    for (const status of statusFallback) {
      const res = await analysisApi.listAnalysisTasks({ type, status, page: 1, size: 50 })
      const data = (res as any).data ?? res
      const items = data.items || []
      const match = items.find((t: any) => t.recordingId === recordingId)
      if (match?.id) return match.id
    }
  } catch {}
  return null
}

async function fetchParagraphs(taskIdValue: number | null, target: { value: AsrParagraph[] }) {
  if (!taskIdValue) {
    target.value = []
    return
  }
  try {
    const res = await analysisApi.getParagraphs(taskIdValue, 1, 500)
    const data = (res as any).data ?? res
    target.value = data.items || []
  } catch {
    target.value = []
  }
}

// Fetch: comparison mode
async function fetchComparisonData() {
  if (!comparisonId.value) return
  try {
    const res = await comparisonApi.getComparison(comparisonId.value)
    comparison.value = (res as any).data ?? res
    if (comparison.value) {
      const isClip = comparison.value.type === 'clip'
      const [taskIdOpt, taskIdRef] = await Promise.all([
        resolveTaskId(comparison.value.taskIdOptimize, comparison.value.recordingIdOptimize, comparison.value.type || 'full'),
        resolveTaskId(comparison.value.taskIdReference, comparison.value.recordingIdReference, comparison.value.type || 'full')
      ])
      comparison.value.taskIdOptimize = taskIdOpt as any
      comparison.value.taskIdReference = taskIdRef as any

      const [rOpt, rRef] = await Promise.all([
        recordingApi.getRecording(comparison.value.recordingIdOptimize),
        recordingApi.getRecording(comparison.value.recordingIdReference)
      ])
      recordingOpt.value = (rOpt as any).data ?? rOpt
      recordingRef.value = (rRef as any).data ?? rRef

      if (isClip) {
        const [tOpt, tRef] = await Promise.all([
          taskIdOpt ? analysisApi.getAnalysis(taskIdOpt) : null,
          taskIdRef ? analysisApi.getAnalysis(taskIdRef) : null
        ])
        const taskOptData = tOpt ? ((tOpt as any).data ?? tOpt) : null
        const taskRefData = tRef ? ((tRef as any).data ?? tRef) : null
        clipTaskOpt.value = taskOptData
        clipTaskRef.value = taskRefData
        videoUrlOpt.value = await getVideoUrl(taskOptData?.clipFilePath) || await getVideoUrl(recordingOpt.value?.localFilePath)
        videoUrlRef.value = await getVideoUrl(taskRefData?.clipFilePath) || await getVideoUrl(recordingRef.value?.localFilePath)
      } else {
        videoUrlOpt.value = await getVideoUrl(recordingOpt.value?.localFilePath)
        videoUrlRef.value = await getVideoUrl(recordingRef.value?.localFilePath)
      }

      await Promise.all([
        fetchParagraphs(taskIdOpt, paragraphsOpt),
        fetchParagraphs(taskIdRef, paragraphsRef)
      ])
    }
  } catch { Message.error('加载对比信息失败') }
}

// AI
async function fetchPresets() {
  try {
    const res = await aiApi.getPresets('script')
    presets.value = (res as any).data ?? res ?? []
  } catch { presets.value = [] }
}

async function fetchChatHistory() {
  try {
    const params: any = { assistantType: 'script' }
    if (taskId.value) params.taskId = taskId.value
    if (comparisonId.value) params.comparisonId = comparisonId.value
    const res = await aiApi.getChatHistory(params)
    const data = (res as any).data ?? res
    aiMessages.value = data.items || data.messages || []
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
  scrollToBottom()
  try {
    const req: any = {
      assistantType: 'script',
      message: text,
      aiModel: aiModel.value
    }
    if (taskId.value) req.taskId = taskId.value
    if (comparisonId.value) req.comparisonId = comparisonId.value
    const res = await aiApi.sendChat(req)
    const msg = (res as any).data ?? res
    aiMessages.value.push(msg)
  } catch (err: any) {
    aiMessages.value.pop()
    if (err?.code === 40006) {
      Message.warning('今日 AI 额度已用完（每天 10 次，含录制复盘、切片、AI 助手提问），明天 0 点自动重置')
    } else {
      Message.error(err?.message || 'AI 回复失败')
    }
  }
  finally { aiLoading.value = false; scrollToBottom() }
}

function onPresetClick(preset: PresetQuestion) {
  sendMessage(preset.title)
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

function goBack() {
  router.back()
}

// Default presets
const defaultPresets: PresetQuestion[] = [
  { id: 1, title: '优化开场白话术', desc: '', color: '#FF7D00' },
  { id: 2, title: '提炼产品卖点话术', desc: '', color: '#F5364B' },
  { id: 3, title: '优化促单催单话术', desc: '', color: '#F5A623' },
  { id: 4, title: '生成互动留人话术', desc: '', color: '#722ED1' },
  { id: 5, title: '优化福利引导话术', desc: '', color: '#00B42A' },
  { id: 6, title: '生成下播预告话术', desc: '', color: '#3491FA' },
  { id: 7, title: '拆解话术节奏和结构', desc: '', color: '#0FC6C2' },
  { id: 8, title: '生成痛点引导话术', desc: '', color: '#EB0AA4' },
  { id: 9, title: '优化逼单连环话术', desc: '', color: '#F76560' },
  { id: 10, title: '生成粉丝感谢话术', desc: '', color: '#A78BFA' }
]

const defaultComparisonPresets: PresetQuestion[] = [
  { id: 1, title: '对比两场开场白话术', desc: '', color: '#FF7D00' },
  { id: 2, title: '对比两场产品介绍技巧', desc: '', color: '#F5364B' },
  { id: 3, title: '对比两场促单话术差异', desc: '', color: '#F5A623' },
  { id: 4, title: '对比两场互动话术质量', desc: '', color: '#722ED1' },
  { id: 5, title: '对比两场逼单节奏差异', desc: '', color: '#00B42A' },
  { id: 6, title: '对比两场痛点引导方式', desc: '', color: '#3491FA' }
]

const displayPresets = computed(() => {
  if (presets.value.length > 0) return presets.value
  return isComparison.value ? defaultComparisonPresets : defaultPresets
})

// Simple markdown renderer
function renderMarkdown(text: string): string {
  if (!text) return ''
  return text
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/^### (.+)$/gm, '<h4>$1</h4>')
    .replace(/^## (.+)$/gm, '<h3>$1</h3>')
    .replace(/^# (.+)$/gm, '<h2>$1</h2>')
    .replace(/^\d+\.\s+(.+)$/gm, '<div class="md-li">$&</div>')
    .replace(/^- (.+)$/gm, '<div class="md-li">• $1</div>')
    .replace(/\n/g, '<br/>')
}

onMounted(async () => {
  if (isComparison.value) {
    await fetchComparisonData()
  } else {
    await fetchSingleData()
  }
  await Promise.all([fetchPresets(), fetchChatHistory()])
})
</script>

<template>
  <div class="assistant-page">

    <!-- ===== 顶部导航栏 ===== -->
    <div class="top-nav">
      <span class="back-link" @click="goBack">&lsaquo; AI话术助手</span>
    </div>

    <!-- ===== 主体区域 ===== -->
    <div class="main-body">

      <!-- == 左栏：视频+主播信息 == -->
      <div class="left-panel">

        <!-- 单场模式 -->
        <template v-if="!isComparison">
          <div class="streamer-card">
            <div class="sc-avatar"><img v-if="streamerAvatar" :src="streamerAvatar" class="sc-avatar-img" /><template v-else>{{ streamerName.charAt(0) }}</template></div>
            <div class="sc-info">
              <div class="sc-name">{{ streamerName }}</div>
              <a-tag size="small" color="arcoblue">直播专号</a-tag>
            </div>
          </div>

          <div class="video-section">
            <div class="video-label">直播画面</div>
            <div class="video-wrapper">
              <CustomVideoPlayer v-if="videoUrl" :src="videoUrl" preload="metadata" class="video-el" />
              <div v-else class="video-empty">视频加载中...</div>
            </div>
          </div>

          <div class="recording-info">
            <div class="ri-row"><span class="ri-label">直播时间</span><span class="ri-val">{{ formatTime(recording?.startTime) }}</span></div>
            <div class="ri-row"><span class="ri-label">录制时长</span><span class="ri-val">{{ formatDuration(recording?.duration) }}</span></div>
            <div class="ri-row"><span class="ri-label">文件大小</span><span class="ri-val">{{ formatFileSize(recording?.fileSize) }}</span></div>
          </div>

          <div class="transcript-section" v-if="paragraphs.length > 0">
            <div class="ts-header">
              <span>转录文本</span>
              <span class="ts-count">{{ paragraphs.length }}段</span>
            </div>
            <div class="ts-body">
              <div v-for="p in paragraphs" :key="p.id" class="ts-line">
                <span class="ts-time">{{ p.startTime }}</span>
                <span class="ts-text">{{ p.textContent }}</span>
              </div>
            </div>
          </div>
        </template>

        <!-- 对比模式 -->
        <template v-else>
          <div class="dual-streamers">
            <div class="streamer-card compact">
              <div class="sc-avatar grad-blue"><img v-if="streamerAvatarOpt" :src="streamerAvatarOpt" class="sc-avatar-img" /><template v-else>{{ streamerNameOpt.charAt(0) }}</template></div>
              <div class="sc-info">
                <div class="sc-name">{{ streamerNameOpt }}</div>
                <div class="sc-meta">优化场次</div>
              </div>
            </div>
            <div class="streamer-card compact">
              <div class="sc-avatar grad-orange"><img v-if="streamerAvatarRef" :src="streamerAvatarRef" class="sc-avatar-img" /><template v-else>{{ streamerNameRef.charAt(0) }}</template></div>
              <div class="sc-info">
                <div class="sc-name">{{ streamerNameRef }}</div>
                <div class="sc-meta">参考场次</div>
              </div>
            </div>
          </div>

          <div class="dual-videos">
            <div class="video-half">
              <div class="video-label">直播画面</div>
              <div class="video-wrapper">
                <CustomVideoPlayer v-if="videoUrlOpt" ref="videoElOpt" :src="videoUrlOpt" preload="metadata" class="video-el" />
                <div v-else class="video-empty">视频加载中...</div>
              </div>
              <div class="recording-info compact">
                <div class="ri-row"><span class="ri-label">{{ isClipComparison ? '切片分类' : '直播时间' }}</span><span class="ri-val">{{ isClipComparison ? clipCatLabel(clipTaskOpt?.clipCategory) : formatTime(recordingOpt?.startTime) }}</span></div>
                <div class="ri-row"><span class="ri-label">{{ isClipComparison ? '切片时长' : '录制时长' }}</span><span class="ri-val">{{ formatDuration(isClipComparison ? clipDurationOpt : recordingOpt?.duration) }}</span></div>
              </div>
              <div class="transcript-section comparison-transcript">
                <div class="ts-header">
                  <span>优化场次逐字稿</span>
                  <span class="ts-count">{{ paragraphsOpt.length }}段</span>
                </div>
                <div class="ts-body">
                  <div v-if="paragraphsOpt.length === 0" class="transcript-empty">暂无逐字稿</div>
                  <div
                    v-for="p in paragraphsOpt"
                    :key="'opt-' + p.id"
                    class="ts-line"
                    @click="seekComparisonVideo('opt', p)"
                  >
                    <span class="ts-time">{{ p.startTime }}</span>
                    <span class="ts-text">{{ p.textContent }}</span>
                  </div>
                </div>
              </div>
            </div>
            <div class="video-half">
              <div class="video-label">直播画面</div>
              <div class="video-wrapper">
                <CustomVideoPlayer v-if="videoUrlRef" ref="videoElRef" :src="videoUrlRef" preload="metadata" class="video-el" />
                <div v-else class="video-empty">视频加载中...</div>
              </div>
              <div class="recording-info compact">
                <div class="ri-row"><span class="ri-label">{{ isClipComparison ? '切片分类' : '直播时间' }}</span><span class="ri-val">{{ isClipComparison ? clipCatLabel(clipTaskRef?.clipCategory) : formatTime(recordingRef?.startTime) }}</span></div>
                <div class="ri-row"><span class="ri-label">{{ isClipComparison ? '切片时长' : '录制时长' }}</span><span class="ri-val">{{ formatDuration(isClipComparison ? clipDurationRef : recordingRef?.duration) }}</span></div>
              </div>
              <div class="transcript-section comparison-transcript">
                <div class="ts-header">
                  <span>参考场次逐字稿</span>
                  <span class="ts-count">{{ paragraphsRef.length }}段</span>
                </div>
                <div class="ts-body">
                  <div v-if="paragraphsRef.length === 0" class="transcript-empty">暂无逐字稿</div>
                  <div
                    v-for="p in paragraphsRef"
                    :key="'ref-' + p.id"
                    class="ts-line"
                    @click="seekComparisonVideo('ref', p)"
                  >
                    <span class="ts-time">{{ p.startTime }}</span>
                    <span class="ts-text">{{ p.textContent }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>

      <!-- == 右栏：AI对话 == -->
      <div class="chat-panel">
        <div class="chat-header">
          <span class="ch-title">话术助手</span>
          <div class="ch-right">
            <span class="model-static">Opus 4.7</span>
          </div>
        </div>

        <div class="chat-messages" ref="messagesRef">
          <div v-if="aiMessages.length === 0" class="welcome-area">
            <div class="welcome-text">
              <b>你好，{{ isComparison ? '朋友' : streamerName }}</b><br/>
              我是你的AI话术助手，我能帮你：优化直播话术、生成开场白、提炼卖点、设计互动话术、打造逼单连环话术。
            </div>

            <div class="preset-grid">
              <div
                v-for="p in displayPresets"
                :key="p.id"
                class="preset-chip"
                :style="{ borderColor: p.color, color: p.color }"
                @click="onPresetClick(p)"
              >
                <span class="pc-dot" :style="{ background: p.color }">{{ p.id }}</span>
                {{ p.title }}
              </div>
            </div>
          </div>

          <template v-for="msg in aiMessages" :key="msg.id">
            <div class="msg-row" :class="msg.role">
              <div v-if="msg.role === 'user'" class="msg-bubble user">
                {{ msg.content }}
              </div>
              <div v-else class="msg-bubble ai">
                <details v-if="msg.thinking" class="thinking-block">
                  <summary>深度思考过程</summary>
                  <div class="thinking-content">{{ msg.thinking }}</div>
                </details>
                <div class="ai-content" v-html="renderMarkdown(msg.content)"></div>
              </div>
            </div>
          </template>

          <div v-if="aiLoading" class="msg-row assistant">
            <div class="msg-bubble ai loading-bubble">
              <span class="loading-dots">AI正在思考<span>...</span></span>
            </div>
          </div>
        </div>

        <div v-if="aiMessages.length > 0" class="bottom-presets">
          <div
            v-for="p in displayPresets.slice(0, 6)"
            :key="p.id"
            class="bp-chip"
            @click="onPresetClick(p)"
          >{{ p.title }}</div>
        </div>

        <div class="chat-input">
          <input
            class="djsinput"
            v-model="aiInput"
            placeholder="输入你想优化的话术问题..."
            style="flex:1;border-radius:20px"
            @keydown.enter.exact.prevent="sendMessage()"
          />
          <button class="djsbtn primary sm" :disabled="aiLoading" @click="sendMessage()">
            发送
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.assistant-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: var(--bg);
}

.top-nav {
  height: 44px;
  padding: 0 20px;
  background: var(--card);
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.back-link {
  font-size: 14px;
  color: var(--text-2b);
  cursor: pointer;
  font-weight: 500;
  &:hover { color: var(--brand); }
}

.nav-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
}

.nav-expire {
  font-size: 12px;
  color: var(--text-3);
}

.main-body {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.left-panel {
  width: 420px;
  flex-shrink: 0;
  background: var(--card);
  border-right: 1px solid var(--line);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.streamer-card {
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid var(--line);
}

.streamer-card.compact {
  padding: 12px 16px;
  flex: 1;
}

.sc-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--gold), var(--orange));
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  overflow: hidden;
}

.sc-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.sc-avatar.grad-blue {
  background: linear-gradient(135deg, var(--brand-light), var(--brand));
}

.sc-avatar.grad-orange {
  background: linear-gradient(135deg, #FF9A6E, #FF5E3A);
}

.sc-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sc-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
}

.sc-meta {
  font-size: 11px;
  color: var(--text-3);
}

.video-section { padding: 0; }

.video-label {
  height: 28px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  background: var(--text-1);
  font-size: 12px;
  color: #fff;
  font-weight: 500;
}

.video-wrapper {
  background: #0B0F1A;
  aspect-ratio: 16/9;
}

.video-el {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: contain;
}

.video-empty {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  color: #5A6377;
  font-size: 13px;
  aspect-ratio: 16/9;
  background: #0B0F1A;
}

.recording-info {
  padding: 14px 20px;
  border-bottom: 1px solid var(--line);
}

.recording-info.compact { padding: 10px 12px; }

.ri-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
}

.ri-label { font-size: 12px; color: var(--text-3); }
.ri-val { font-size: 12px; color: var(--text-1); font-weight: 500; }

.transcript-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.ts-header {
  padding: 12px 20px;
  border-bottom: 1px solid var(--line);
  font-size: 13px;
  font-weight: 600;
  color: var(--text-1);
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.ts-count { font-size: 12px; color: var(--text-3); font-weight: 400; }

.ts-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px 12px;
}

.transcript-empty {
  padding: 18px 8px;
  text-align: center;
  color: var(--text-4);
  font-size: 12px;
}

.ts-line {
  padding: 8px 10px;
  border-radius: 6px;
  margin-bottom: 4px;
  font-size: 12px;
  line-height: 1.6;
  cursor: pointer;
  &:hover { background: var(--bg); }
}

.ts-time {
  color: var(--orange);
  font-family: var(--fm);
  font-weight: 600;
  margin-right: 8px;
  font-size: 11px;
}

.ts-text { color: var(--text-2b); }

.comparison-transcript {
  border-top: 1px solid var(--line);
  background: var(--card);
}

.comparison-transcript .ts-header {
  padding: 10px 12px;
}

.comparison-transcript .ts-body {
  padding: 6px 8px 10px;
}

.dual-streamers {
  display: flex;
  border-bottom: 1px solid var(--line);
}

.dual-videos {
  display: flex;
  flex: 1;
  min-height: 0;
}

.video-half {
  flex: 1;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--line);
  &:last-child { border-right: none; }
}

.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--card);
  min-width: 0;
}

.chat-header {
  height: 48px;
  padding: 0 20px;
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.ch-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--orange);
}

.ch-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 10px;
}

.model-static {
  height: 28px;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  padding: 0 12px;
  border: 1px solid var(--line);
  border-radius: 4px;
  color: var(--text-1);
  background: var(--bg-2, #fafafa);
  user-select: none;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.welcome-area {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.welcome-text {
  font-size: 15px;
  color: var(--text-1);
  line-height: 1.7;
  padding: 16px 20px;
  background: var(--bg);
  border-radius: var(--radius);
}

.preset-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.preset-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border: 1px solid;
  border-radius: 20px;
  font-size: 12px;
  cursor: pointer;
  transition: all .15s;
  background: var(--card);
  &:hover { background: var(--bg); }
}

.pc-dot {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.msg-row {
  display: flex;
  &.user { justify-content: flex-end; }
  &.assistant { justify-content: flex-start; }
}

.msg-bubble {
  max-width: 80%;
  padding: 12px 16px;
  border-radius: var(--radius);
  font-size: 13px;
  line-height: 1.7;
}

.msg-bubble.user {
  background: linear-gradient(135deg, var(--gold), var(--orange));
  color: #fff;
  border-bottom-right-radius: 4px;
}

.msg-bubble.ai {
  background: var(--bg);
  color: var(--text-1);
  border: 1px solid var(--line);
  border-bottom-left-radius: 4px;
}

.thinking-block {
  margin-bottom: 10px;
  summary {
    font-size: 12px;
    color: var(--text-3);
    cursor: pointer;
    &:hover { color: var(--orange); }
  }
}

.thinking-content {
  margin-top: 8px;
  padding: 10px 14px;
  background: var(--card);
  border-radius: 8px;
  font-size: 12px;
  color: var(--text-3);
  line-height: 1.6;
  border: 1px solid var(--line);
}

.ai-content {
  :deep(strong) { color: var(--orange); }
  :deep(.md-li) { padding: 2px 0; }
}

.loading-bubble { color: var(--text-3); }

.loading-dots span {
  animation: dots 1.4s infinite;
}

@keyframes dots {
  0%, 20% { opacity: 0; }
  50% { opacity: 1; }
  100% { opacity: 0; }
}

.bottom-presets {
  padding: 8px 20px;
  border-top: 1px solid var(--line);
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  flex-shrink: 0;
}

.bp-chip {
  padding: 5px 12px;
  border-radius: 14px;
  border: 1px solid var(--line);
  font-size: 11px;
  color: var(--text-2b);
  cursor: pointer;
  transition: all .12s;
  &:hover {
    border-color: var(--orange);
    color: var(--orange);
    background: var(--bg);
  }
}

.chat-input {
  padding: 12px 20px;
  border-top: 1px solid var(--line);
  display: flex;
  gap: 10px;
  align-items: center;
  flex-shrink: 0;
  background: var(--card);
}
</style>
