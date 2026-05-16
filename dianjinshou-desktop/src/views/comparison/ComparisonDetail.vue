<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import * as comparisonApi from '@/api/comparison'
import * as analysisApi from '@/api/analysis'
import * as recordingApi from '@/api/recording'
import type { ComparisonItem } from '@/api/comparison'
import type { AsrParagraph, KeywordItem } from '@/api/analysis'
import type { RecordingDetail } from '@/api/recording'
import { formatDateTime } from '@/utils/format'
import CustomVideoPlayer from '@/components/common/CustomVideoPlayer.vue'

const route = useRoute()
const router = useRouter()
const comparisonId = computed(() => Number(route.params.id))

const CLIP_CAT_MAP: Record<string, string> = {
  RETENTION: '留人切片', QUALITY_SPEECH: '优质话术', MARKETING: '营销塑品',
  INTERACTION: '互动切片', FAN_CLUB: '粉团切片', EXPRESSION: '表现力切片',
  COMPLIANCE: '规避违规', OTHER: '其他'
}
function clipCatLabel(code: string | null | undefined): string {
  return code ? (CLIP_CAT_MAP[code] || code) : ''
}

// Core data
const comparison = ref<ComparisonItem | null>(null)
const recordingOpt = ref<RecordingDetail | null>(null)
const recordingRef = ref<RecordingDetail | null>(null)
const taskOpt = ref<any>(null)
const taskRef = ref<any>(null)
const paragraphsOpt = ref<AsrParagraph[]>([])
const paragraphsRef = ref<AsrParagraph[]>([])
const loading = ref(false)

// Video
const videoUrlOpt = ref('')
const videoUrlRef = ref('')
const videoElOpt = ref<InstanceType<typeof CustomVideoPlayer> | null>(null)
const videoElRef = ref<InstanceType<typeof CustomVideoPlayer> | null>(null)
const currentTimeOpt = ref(0)
const currentTimeRef = ref(0)

// Transcript scroll containers
const scrollOptEl = ref<HTMLElement | null>(null)
const scrollRefEl = ref<HTMLElement | null>(null)

// Bottom analysis tabs
const bottomTab = ref('keywords')
const keywords = ref<KeywordItem[]>([])
const allKeywords = ref<KeywordItem[]>([])
const keywordStats = ref({ totalOperational: 0, totalSensitive: 0 })
const keywordType = ref('')
const keywordCategory = ref('')

// Keyword category breakdown stats
interface CategoryCount { name: string; count: number }
const keywordCategoryStats = computed<CategoryCount[]>(() => {
  const map: Record<string, number> = {}
  for (const k of allKeywords.value) {
    const cat = k.category || '未分类'
    map[cat] = (map[cat] || 0) + 1
  }
  return Object.entries(map)
    .map(([name, count]) => ({ name, count }))
    .sort((a, b) => b.count - a.count)
})

// Computed
const isClipComparison = computed(() => comparison.value?.type === 'clip')
const streamerNameOpt = computed(() => recordingOpt.value?.streamerInfo?.anchorName ?? '主播A')
const streamerAvatarOpt = computed(() => recordingOpt.value?.streamerInfo?.anchorAvatar ?? '')
const streamerNameRef = computed(() => recordingRef.value?.streamerInfo?.anchorName ?? '主播B')
const streamerAvatarRef = computed(() => recordingRef.value?.streamerInfo?.anchorAvatar ?? '')
const clipInfoOpt = computed(() => {
  if (!taskOpt.value) return ''
  const cat = clipCatLabel(taskOpt.value.clipCategory)
  const start = taskOpt.value.clipStart ?? 0
  const end = taskOpt.value.clipEnd ?? 0
  return cat ? `${cat} (${formatDuration(start)}~${formatDuration(end)})` : `${formatDuration(start)}~${formatDuration(end)}`
})
const clipInfoRef = computed(() => {
  if (!taskRef.value) return ''
  const cat = clipCatLabel(taskRef.value.clipCategory)
  const start = taskRef.value.clipStart ?? 0
  const end = taskRef.value.clipEnd ?? 0
  return cat ? `${cat} (${formatDuration(start)}~${formatDuration(end)})` : `${formatDuration(start)}~${formatDuration(end)}`
})

// Parse HH:MM:SS to seconds
function parseTimeToSeconds(t: string | null | undefined): number {
  if (!t) return 0
  const parts = t.split(':').map(Number)
  if (parts.length === 3) return parts[0] * 3600 + parts[1] * 60 + parts[2]
  if (parts.length === 2) return parts[0] * 60 + parts[1]
  return parts[0] || 0
}

// Find active paragraph index based on current video time
function findActiveParagraphIndex(paragraphs: AsrParagraph[], currentTime: number): number {
  if (paragraphs.length === 0) return -1
  for (let i = paragraphs.length - 1; i >= 0; i--) {
    const start = parseTimeToSeconds(paragraphs[i].startTime)
    if (currentTime >= start) return i
  }
  return 0
}

const activeOptIndex = computed(() => findActiveParagraphIndex(paragraphsOpt.value, currentTimeOpt.value))
const activeRefIndex = computed(() => findActiveParagraphIndex(paragraphsRef.value, currentTimeRef.value))

// Auto-scroll to active paragraph
function scrollToActive(containerEl: HTMLElement | null, index: number) {
  if (!containerEl || index < 0) return
  const lines = containerEl.querySelectorAll('.cmp2-tri')
  if (lines[index]) {
    lines[index].scrollIntoView({ block: 'center', behavior: 'smooth' })
  }
}

watch(activeOptIndex, (idx) => nextTick(() => scrollToActive(scrollOptEl.value, idx)))
watch(activeRefIndex, (idx) => nextTick(() => scrollToActive(scrollRefEl.value, idx)))

function onTimeUpdateOpt(t: number) { currentTimeOpt.value = t }
function onTimeUpdateRef(t: number) { currentTimeRef.value = t }

// Click on a transcript line to seek video
function seekOpt(p: AsrParagraph) {
  videoElOpt.value?.seekTo(parseTimeToSeconds(p.startTime))
}
function seekRef(p: AsrParagraph) {
  videoElRef.value?.seekTo(parseTimeToSeconds(p.startTime))
}

// Content compass (内容罗盘) - category distribution
const CATEGORY_COLORS: Record<string, string> = {
  '开场话术': '#4A6896', '逼单话术': '#ff7d00', '促单话术': '#0fc6c2',
  '互动话术': '#722ed1', '憋单话术': '#eb0aa4', '过款话术': '#8693A4',
  '福利话术': '#00b42a', '塑品话术': '#f53f3f', '售后话术': '#AEB9C8',
  '人设话术': '#3491fa', '低俗话术': '#ff5722', '客服话术': '#fadc19',
  '违禁词话术': '#f53f3f', '话题话术': '#4A6896', '行动指令': '#ff7d00',
  '留人话术': '#0fc6c2', '其他': '#AEB9C8'
}

interface CategoryStat { name: string; count: number; percent: number; color: string }

function computeCategoryStats(paragraphs: AsrParagraph[]): CategoryStat[] {
  const map: Record<string, number> = {}
  for (const p of paragraphs) {
    const cat = p.scriptCategory || '其他'
    map[cat] = (map[cat] || 0) + 1
  }
  const total = paragraphs.length || 1
  return Object.entries(map)
    .map(([name, count]) => ({
      name,
      count,
      percent: Math.round((count / total) * 1000) / 10,
      color: CATEGORY_COLORS[name] || '#8693A4'
    }))
    .sort((a, b) => b.count - a.count)
}

const categoryStatsOpt = computed(() => computeCategoryStats(paragraphsOpt.value))
const categoryStatsRef = computed(() => computeCategoryStats(paragraphsRef.value))

function buildConicGradient(stats: CategoryStat[]): string {
  if (stats.length === 0) return 'conic-gradient(#E2E7EF 0% 100%)'
  const segments: string[] = []
  let acc = 0
  for (const s of stats) {
    segments.push(`${s.color} ${acc}% ${acc + s.percent}%`)
    acc += s.percent
  }
  if (acc < 100) segments.push(`#E2E7EF ${acc}% 100%`)
  return `conic-gradient(${segments.join(', ')})`
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

function formatTime(t: string | null | undefined): string {
  return formatDateTime(t)
}

// Fetch functions
// Resolve taskId: use comparison's taskId, or fallback to finding latest completed task for the recording
async function resolveTaskId(taskId: number | null | undefined, recordingId: number, type: string): Promise<number | null> {
  if (taskId) return taskId
  // v1.1.0：兼容 transcribed / ai_processing / completed / failed —— 只要有 ASR 就能看逐字稿
  const statusFallback = ['completed', 'transcribed', 'ai_processing', 'failed']
  try {
    for (const status of statusFallback) {
      const res = await analysisApi.listAnalysisTasks({ type, status, page: 1, size: 50 })
      const data = (res as any).data ?? res
      const items = data.items || []
      const match = items.find((t: any) => t.recordingId === recordingId)
      if (match?.id) return match.id
    }
    return null
  } catch { return null }
}

async function fetchComparison() {
  loading.value = true
  try {
    const res = await comparisonApi.getComparison(comparisonId.value)
    comparison.value = (res as any).data ?? res
    if (comparison.value) {
      const c = comparison.value
      const isClip = c.type === 'clip'

      // Resolve taskIds (fallback for legacy comparisons without taskId)
      const [taskIdOpt, taskIdRef] = await Promise.all([
        resolveTaskId(c.taskIdOptimize, c.recordingIdOptimize, c.type || 'full'),
        resolveTaskId(c.taskIdReference, c.recordingIdReference, c.type || 'full')
      ])
      // Update comparison object so fetchKeywords can use them
      c.taskIdOptimize = taskIdOpt as any
      c.taskIdReference = taskIdRef as any

      await Promise.all([
        fetchRecording(c.recordingIdOptimize, 'opt'),
        fetchRecording(c.recordingIdReference, 'ref'),
        fetchParagraphs(taskIdOpt, paragraphsOpt),
        fetchParagraphs(taskIdRef, paragraphsRef),
        isClip ? fetchClipVideo(taskIdOpt, 'opt') : Promise.resolve(),
        isClip ? fetchClipVideo(taskIdRef, 'ref') : Promise.resolve()
      ])
    }
  } catch { Message.error('加载对比详情失败') }
  finally { loading.value = false }
}

async function fetchRecording(recordingId: number, side: 'opt' | 'ref') {
  try {
    const res = await recordingApi.getRecording(recordingId)
    const rec = (res as any).data ?? res
    if (side === 'opt') recordingOpt.value = rec
    else recordingRef.value = rec
    const isClip = comparison.value?.type === 'clip'
    if (!isClip && rec?.localFilePath) {
      const api = (window as any).electronAPI
      let url: string
      if (api?.getLocalVideoUrl) {
        url = await api.getLocalVideoUrl(rec.localFilePath)
      } else {
        url = `local-video://video?path=${encodeURIComponent(rec.localFilePath)}`
      }
      if (side === 'opt') videoUrlOpt.value = url
      else videoUrlRef.value = url
    }
  } catch (e) { console.warn('[ComparisonDetail] fetch recording failed:', e) }
}

async function fetchParagraphs(taskId: number | null, target: typeof paragraphsOpt) {
  if (!taskId) return
  try {
    const res = await analysisApi.getParagraphs(taskId, 1, 500)
    const data = (res as any).data ?? res
    target.value = data.items || []
  } catch { target.value = [] }
}

async function fetchClipVideo(taskId: number | null, side: 'opt' | 'ref') {
  if (!taskId) return
  try {
    const res = await analysisApi.getAnalysis(taskId)
    const task = (res as any).data ?? res
    if (side === 'opt') taskOpt.value = task
    else taskRef.value = task
    if (task?.clipFilePath) {
      const api = (window as any).electronAPI
      let url: string
      if (api?.getLocalVideoUrl) {
        url = await api.getLocalVideoUrl(task.clipFilePath)
      } else {
        url = `local-video://video?path=${encodeURIComponent(task.clipFilePath)}`
      }
      if (side === 'opt') videoUrlOpt.value = url
      else videoUrlRef.value = url
    }
  } catch (e) { console.warn('[ComparisonDetail] fetch clip video failed:', e) }
}

async function fetchKeywords() {
  try {
    // Try comparison-level keywords first
    const res = await comparisonApi.getComparisonKeywords(comparisonId.value)
    const data = (res as any).data ?? res
    let kwList: KeywordItem[] = Array.isArray(data) ? data : (data.items || [])

    // If no comparison-level keywords, merge from both tasks
    if (kwList.length === 0 && comparison.value) {
      const [optKw, refKw] = await Promise.all([
        comparison.value.taskIdOptimize ? fetchTaskKeywords(comparison.value.taskIdOptimize) : Promise.resolve([]),
        comparison.value.taskIdReference ? fetchTaskKeywords(comparison.value.taskIdReference) : Promise.resolve([])
      ])
      // Merge: group by word, use opt as video1 and ref as video2
      const map = new Map<string, KeywordItem>()
      for (const k of optKw) {
        map.set(k.word, { ...k, hitCountVideo1: k.totalCount || k.hitCountVideo1, hitCountVideo2: 0 })
      }
      for (const k of refKw) {
        const existing = map.get(k.word)
        if (existing) {
          existing.hitCountVideo2 = k.totalCount || k.hitCountVideo2
        } else {
          map.set(k.word, { ...k, hitCountVideo1: 0, hitCountVideo2: k.totalCount || k.hitCountVideo2 })
        }
      }
      kwList = Array.from(map.values()).map(k => ({
        ...k,
        totalCount: (k.hitCountVideo1 || 0) + (k.hitCountVideo2 || 0)
      })).sort((a, b) => b.totalCount - a.totalCount)
    }

    allKeywords.value = kwList
    keywordStats.value = {
      totalOperational: allKeywords.value.filter((k: KeywordItem) => k.type === 'operational').length,
      totalSensitive: allKeywords.value.filter((k: KeywordItem) => k.type === 'sensitive').length
    }
    applyKeywordFilter()
  } catch { keywords.value = []; allKeywords.value = [] }
}

async function fetchTaskKeywords(taskId: number): Promise<KeywordItem[]> {
  try {
    const res = await analysisApi.getKeywords(taskId)
    const data = (res as any).data ?? res
    return data.items || (Array.isArray(data) ? data : [])
  } catch { return [] }
}

function applyKeywordFilter() {
  let filtered = allKeywords.value
  if (keywordType.value) {
    filtered = filtered.filter((k: KeywordItem) => k.type === keywordType.value)
  }
  if (keywordCategory.value) {
    filtered = filtered.filter((k: KeywordItem) => (k.category || '未分类') === keywordCategory.value)
  }
  keywords.value = filtered
}

function onKeywordTypeChange(type: string) {
  keywordType.value = keywordType.value === type ? '' : type
  keywordCategory.value = ''
  applyKeywordFilter()
}

function onKeywordCategoryChange(cat: string) {
  keywordCategory.value = keywordCategory.value === cat ? '' : cat
  keywordType.value = ''
  applyKeywordFilter()
}

function goToAssistant(type: 'operation' | 'compliance' | 'script') {
  const nameMap = { operation: 'AssistantOperation', compliance: 'AssistantCompliance', script: 'AssistantScript' }
  router.push({ name: nameMap[type], query: { comparisonId: comparisonId.value } })
}

function goBack() {
  const type = comparison.value?.type
  router.push({ name: type === 'clip' ? 'ClipComparisonList' : 'FullComparisonList' })
}

onMounted(async () => {
  await fetchComparison()
  fetchKeywords()
})
</script>

<template>
  <div class="comparison-detail">

    <!-- Back link -->
    <div class="back" @click="goBack">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
      智能对比分析
    </div>

    <div v-if="loading" class="loading-state">加载中...</div>

    <template v-else>
      <!-- Info bar -->
      <div class="info-bar">
        <div class="ib-item"><span class="lbl">类型</span><span class="val">{{ isClipComparison ? '切片对比' : '整场对比' }}</span></div>
        <div class="ib-item"><span class="lbl">对比时间</span><span class="val">{{ formatDateTime(comparison?.createdAt) }}</span></div>
        <div class="ib-item" v-if="isClipComparison && (clipInfoOpt || clipInfoRef)"><span class="lbl">切片类型</span><span class="val" style="color:var(--brand)">{{ clipInfoOpt }} vs {{ clipInfoRef }}</span></div>
        <div class="ib-item" v-else><span class="lbl">共计</span><span class="val" style="color:var(--brand)">2 场</span></div>
      </div>

      <!-- 4-column grid: video1 | transcript1 | video2 | transcript2 -->
      <div class="cmp2">
        <!-- Stream 1: video -->
        <div class="cmp2-stream">
          <div class="cmp2-stream-hd">
            <span v-if="streamerAvatarOpt" class="av av-4"><img :src="streamerAvatarOpt" alt="" referrerpolicy="no-referrer" /></span>
            <span v-else class="av av-4">{{ streamerNameOpt.charAt(0) }}</span>
            <div class="cmp2-stream-info">
              <div class="nm">{{ streamerNameOpt }}</div>
              <div class="tm">{{ isClipComparison && clipInfoOpt ? clipInfoOpt : formatTime(recordingOpt?.startTime) }}</div>
            </div>
            <span class="cmp2-tag opt">优化场次</span>
          </div>
          <div class="cmp2-vid">
            <div class="cmp2-vid-head">直播画面</div>
            <CustomVideoPlayer
              v-if="videoUrlOpt"
              ref="videoElOpt"
              :src="videoUrlOpt"
              preload="metadata"
              class="cmp2-video-el"
              @timeupdate="onTimeUpdateOpt"
            />
            <div v-else class="play"><svg viewBox="0 0 24 24"><polygon points="5 3 19 12 5 21 5 3" fill="#fff"/></svg></div>
          </div>
        </div>

        <!-- Stream 1: transcript -->
        <div class="cmp2-trans">
          <div class="cmp2-trans-hd">
            <div class="tl opt">优化场次字幕</div>
            <span class="cnt">{{ paragraphsOpt.length }}句</span>
          </div>
          <div ref="scrollOptEl" class="cmp2-trans-body">
            <div v-if="paragraphsOpt.length === 0" class="transcript-empty">暂无字幕数据</div>
            <div
              v-for="(p, i) in paragraphsOpt"
              :key="'opt-' + p.id"
              class="cmp2-tri"
              :class="{ hl: i === activeOptIndex }"
              @click="seekOpt(p)"
            >
              <span class="t">{{ p.startTime }}</span>
              <span class="tx">{{ p.textContent }}</span>
            </div>
          </div>
        </div>

        <!-- Stream 2: video -->
        <div class="cmp2-stream">
          <div class="cmp2-stream-hd">
            <span v-if="streamerAvatarRef" class="av av-3"><img :src="streamerAvatarRef" alt="" referrerpolicy="no-referrer" /></span>
            <span v-else class="av av-3">{{ streamerNameRef.charAt(0) }}</span>
            <div class="cmp2-stream-info">
              <div class="nm">{{ streamerNameRef }}</div>
              <div class="tm">{{ isClipComparison && clipInfoRef ? clipInfoRef : formatTime(recordingRef?.startTime) }}</div>
            </div>
            <span class="cmp2-tag ref">参考场次</span>
          </div>
          <div class="cmp2-vid">
            <div class="cmp2-vid-head">直播画面</div>
            <CustomVideoPlayer
              v-if="videoUrlRef"
              ref="videoElRef"
              :src="videoUrlRef"
              preload="metadata"
              class="cmp2-video-el"
              @timeupdate="onTimeUpdateRef"
            />
            <div v-else class="play"><svg viewBox="0 0 24 24"><polygon points="5 3 19 12 5 21 5 3" fill="#fff"/></svg></div>
          </div>
        </div>

        <!-- Stream 2: transcript -->
        <div class="cmp2-trans">
          <div class="cmp2-trans-hd">
            <div class="tl ref">参考场次字幕</div>
            <span class="cnt">{{ paragraphsRef.length }}句</span>
          </div>
          <div ref="scrollRefEl" class="cmp2-trans-body">
            <div v-if="paragraphsRef.length === 0" class="transcript-empty">暂无字幕数据</div>
            <div
              v-for="(p, i) in paragraphsRef"
              :key="'ref-' + p.id"
              class="cmp2-tri"
              :class="{ hl: i === activeRefIndex, or: i === activeRefIndex }"
              @click="seekRef(p)"
            >
              <span class="t">{{ p.startTime }}</span>
              <span class="tx">{{ p.textContent }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- AI dock (右侧悬浮) -->
      <div class="ai-float-btns">
        <div class="ai-float-btn purple" @click="goToAssistant('operation')">
          <span class="afb-icon">AI</span>
          <span class="afb-text">运营助手</span>
        </div>
        <div class="ai-float-btn red" @click="goToAssistant('compliance')">
          <span class="afb-icon">AI</span>
          <span class="afb-text">违规助手</span>
        </div>
        <div class="ai-float-btn orange" @click="goToAssistant('script')">
          <span class="afb-icon">AI</span>
          <span class="afb-text">话术助手</span>
        </div>
      </div>

      <!-- Bottom analysis -->
      <div class="bottom-card">
        <div class="bottom-tabs">
          <div class="tabs"><div class="tab" :class="{ on: bottomTab === 'keywords' }" @click="bottomTab = 'keywords'">运营关键词汇总</div><div class="tab" :class="{ on: bottomTab === 'compass' }" @click="bottomTab = 'compass'">内容罗盘</div></div>
        </div>

        <!-- Keywords tab -->
        <div v-if="bottomTab === 'keywords'">
          <div class="pills-bar">
            <div class="pills">
              <div class="pill" :class="{ on: !keywordType && !keywordCategory }" @click="keywordType = ''; keywordCategory = ''; applyKeywordFilter()">全部 {{ allKeywords.length }}</div>
              <div class="pill" :class="{ on: keywordType === 'operational' }" @click="onKeywordTypeChange('operational')">运营关键词 {{ keywordStats.totalOperational }}</div>
              <div class="pill" :class="{ on: keywordType === 'sensitive' }" @click="onKeywordTypeChange('sensitive')">敏感词 {{ keywordStats.totalSensitive }}</div>
            </div>
            <div class="pills" style="margin-left:8px">
              <div
                v-for="cs in keywordCategoryStats"
                :key="cs.name"
                class="pill"
                :class="{ on: keywordCategory === cs.name }"
                @click="onKeywordCategoryChange(cs.name)"
              >{{ cs.name }} {{ cs.count }}</div>
            </div>
          </div>
          <table class="djstbl">
            <thead>
              <tr>
                <th>关键词</th>
                <th>类型</th>
                <th>分类</th>
                <th>对比1次数</th>
                <th>对比2次数</th>
                <th>差异</th>
                <th>来源</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="keywords.length === 0">
                <td colspan="7">
                  <div class="djsempty"><div class="ti">暂无数据</div></div>
                </td>
              </tr>
              <tr v-for="kw in keywords" :key="kw.word">
                <td style="color:var(--text-1)">{{ kw.word }}</td>
                <td>
                  <span class="tg" :class="kw.type === 'sensitive' ? 'tg-r' : 'tg-b'">
                    {{ kw.type === 'sensitive' ? '敏感词' : '运营词' }}
                  </span>
                </td>
                <td>{{ kw.category }}</td>
                <td style="font-family:var(--fm)">{{ kw.hitCountVideo1 }}</td>
                <td style="font-family:var(--fm)">{{ kw.hitCountVideo2 }}</td>
                <td>
                  <span :class="{ 'diff-p': kw.hitCountVideo1 > kw.hitCountVideo2, 'diff-m': kw.hitCountVideo1 < kw.hitCountVideo2 }">
                    {{ kw.hitCountVideo1 - kw.hitCountVideo2 > 0 ? '+' : '' }}{{ kw.hitCountVideo1 - kw.hitCountVideo2 }}
                  </span>
                </td>
                <td style="color:var(--text-4);font-family:var(--fm)">{{ kw.source }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Compass tab -->
        <div v-if="bottomTab === 'compass'" class="compass-body">
          <div class="compass-side">
            <div class="compass-title">
              <span class="dot opt-dot"></span>
              优化方 · {{ streamerNameOpt }}
            </div>
            <div class="compass-chart-row">
              <div class="donut-chart" :style="{ background: buildConicGradient(categoryStatsOpt) }">
                <div class="donut-hole">
                  <div class="donut-total">{{ paragraphsOpt.length }}</div>
                  <div class="donut-label">总句数</div>
                </div>
              </div>
              <div class="compass-legend">
                <div v-for="s in categoryStatsOpt" :key="s.name" class="legend-item">
                  <span class="legend-dot" :style="{ background: s.color }"></span>
                  <span class="legend-name">{{ s.name }}</span>
                  <span class="legend-pct">{{ s.percent }}%</span>
                </div>
              </div>
            </div>
          </div>
          <div class="compass-divider"></div>
          <div class="compass-side">
            <div class="compass-title">
              <span class="dot ref-dot"></span>
              参考方 · {{ streamerNameRef }}
            </div>
            <div class="compass-chart-row">
              <div class="donut-chart" :style="{ background: buildConicGradient(categoryStatsRef) }">
                <div class="donut-hole">
                  <div class="donut-total">{{ paragraphsRef.length }}</div>
                  <div class="donut-label">总句数</div>
                </div>
              </div>
              <div class="compass-legend">
                <div v-for="s in categoryStatsRef" :key="s.name" class="legend-item">
                  <span class="legend-dot" :style="{ background: s.color }"></span>
                  <span class="legend-name">{{ s.name }}</span>
                  <span class="legend-pct">{{ s.percent }}%</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.comparison-detail { padding:0; overflow-y:auto; height:100% }
.loading-state { display:flex; align-items:center; justify-content:center; padding:60px 0; color:var(--text-3); font-size:14px }

/* Back link */
.back { display:flex; align-items:center; gap:6px; font-size:13.5px; font-weight:600; color:var(--text-2); cursor:pointer; margin-bottom:14px; padding:2px 0 }
.back:hover { color:var(--brand) }
.back svg { width:16px; height:16px; flex-shrink:0 }

/* Info bar */
.info-bar { display:flex; align-items:center; gap:0; background:#fff; border:1px solid var(--line); border-radius:var(--radius-md); padding:0; margin-bottom:16px; overflow:hidden }
.info-bar .ib-item { display:flex; align-items:center; gap:8px; padding:11px 22px; border-right:1px solid var(--line-2) }
.info-bar .ib-item:last-child { border-right:none }
.info-bar .lbl { font-size:11.5px; color:var(--text-3); font-weight:550 }
.info-bar .val { font-size:13px; color:var(--text-1); font-weight:650; font-family:var(--fm) }

/* 4-column grid — fixed height so bottom sections are visible */
.cmp2 { display:grid; grid-template-columns:220px 1fr 220px 1fr; gap:12px; margin-bottom:20px; background:#fff; border:1px solid var(--line); border-radius:var(--radius-md); padding:14px; align-items:stretch; height:520px }

/* Stream column (video) */
.cmp2-stream { display:flex; flex-direction:column; min-height:0 }
.cmp2-stream-hd { display:flex; align-items:center; gap:8px; padding:4px 2px 10px; flex-shrink:0 }
.cmp2-stream-info { min-width:0; flex:1 }
.cmp2-stream-info .nm { font-size:13px; font-weight:650; color:var(--text-1); line-height:1.2; overflow:hidden; text-overflow:ellipsis; white-space:nowrap }
.cmp2-stream-info .tm { font-size:10.5px; color:var(--text-4); font-family:var(--fm); margin-top:2px }

/* Avatar */
.av { width:30px; height:30px; border-radius:8px; display:flex; align-items:center; justify-content:center; color:#fff; font-size:10px; font-weight:700; flex-shrink:0; overflow:hidden; position:relative }
.av::after { content:''; position:absolute; inset:0; background:linear-gradient(180deg,rgba(255,255,255,.25),transparent 55%); border-radius:inherit; pointer-events:none }
.av img { width:100%; height:100%; object-fit:cover; border-radius:inherit }
.av-1 { background:linear-gradient(140deg,var(--brand),var(--brand-light)) }
.av-2 { background:linear-gradient(140deg,#1A9955,#2DB872) }
.av-3 { background:linear-gradient(140deg,#E07C24,#F0A030) }
.av-4 { background:linear-gradient(140deg,#7C5DD6,#9B7AEA) }

/* Tags */
.cmp2-tag { font-size:9.5px; padding:2px 8px; border-radius:var(--radius-xs); font-weight:700; letter-spacing:.03em; flex-shrink:0 }
.cmp2-tag.opt { background:var(--brand-soft-12); color:var(--brand) }
.cmp2-tag.ref { background:rgba(224,124,36,.08); color:#E07C24 }

/* Video area */
.cmp2-vid { flex:1; background:linear-gradient(135deg,#1A1E2E,#141828); border-radius:var(--radius-md); position:relative; display:flex; align-items:center; justify-content:center; overflow:hidden; min-height:0 }
.cmp2-vid-head { position:absolute; top:0; left:0; right:0; padding:9px 12px; font-size:11.5px; color:#fff; font-weight:600; background:linear-gradient(180deg,rgba(36,30,24,.6),transparent); z-index:2 }
.cmp2-video-el { width:100%; height:100%; object-fit:contain; position:relative; z-index:1 }
.play { width:42px; height:42px; background:rgba(255,255,255,.15); backdrop-filter:blur(8px); border-radius:50%; display:flex; align-items:center; justify-content:center }
.play svg { width:16px; height:16px }

/* Transcript column */
.cmp2-trans { display:flex; flex-direction:column; background:linear-gradient(180deg,#FAFBFD,#F5F7FB); border-radius:var(--radius-md); border:1px solid var(--line-2); overflow:hidden; min-height:0 }
.cmp2-trans-hd { padding:10px 14px; display:flex; align-items:center; justify-content:space-between; border-bottom:1px solid var(--line-2); background:#fff }
.cmp2-trans-hd .tl { font-size:12.5px; font-weight:650; color:var(--text-1); display:flex; align-items:center; gap:6px }
.cmp2-trans-hd .tl.opt::before { content:''; width:3px; height:12px; background:var(--brand); border-radius:2px }
.cmp2-trans-hd .tl.ref::before { content:''; width:3px; height:12px; background:#E07C24; border-radius:2px }
.cmp2-trans-hd .cnt { font-size:11px; color:var(--text-4); font-family:var(--fm); font-weight:550 }
.cmp2-trans-body { flex:1; overflow-y:auto; padding:4px 2px }
.transcript-empty { color:var(--text-4); font-size:12px; text-align:center; padding:24px 0 }

/* Transcript row */
.cmp2-tri { display:flex; gap:10px; padding:8px 12px; border-radius:6px; transition:background .12s ease; cursor:pointer }
.cmp2-tri:hover { background:rgba(255,255,255,.6) }
.cmp2-tri.hl { background:var(--brand-soft-12) }
.cmp2-tri.hl.or { background:rgba(224,124,36,.08) }
.cmp2-tri .t { font-size:10.5px; color:var(--text-4); font-family:var(--fm); white-space:nowrap; min-width:52px; flex-shrink:0; padding-top:2px }
.cmp2-tri .tx { font-size:12.5px; color:var(--text-2); line-height:1.65; flex:1 }
.cmp2-tri.hl .t { color:var(--brand); font-weight:600 }
.cmp2-tri.hl.or .t { color:#E07C24; font-weight:600 }
.cmp2-tri.hl .tx { color:var(--text-1) }

/* AI 右侧悬浮按钮 */
.ai-float-btns {
  position:fixed; right:0; top:50%; transform:translateY(-50%);
  display:flex; flex-direction:column; gap:0; z-index:80;
}
.ai-float-btn {
  display:flex; align-items:center; gap:0;
  padding:6px 8px;
  border-radius:var(--radius-pill,100px) 0 0 var(--radius-pill,100px);
  cursor:pointer; box-shadow:-2px 2px 10px rgba(36,30,24,.12);
  font-size:12.5px; font-weight:600; color:#fff; overflow:hidden;
}
.ai-float-btns:hover .ai-float-btn { gap:6px; padding:6px 12px 6px 8px }
.ai-float-btns:hover .afb-text { max-width:80px; opacity:1 }
.ai-float-btn.purple { background:linear-gradient(135deg,var(--brand),#6B8DD6) }
.ai-float-btn.red { background:linear-gradient(135deg,var(--red,#E53E3E),var(--orange,#E07C24)) }
.ai-float-btn.orange { background:linear-gradient(135deg,var(--green,#1A9955),#4BA0E0) }
.afb-icon {
  width:22px; height:22px; border-radius:50%; background:rgba(255,255,255,.2);
  display:flex; align-items:center; justify-content:center;
  font-size:10px; font-weight:700; flex-shrink:0;
}
.afb-text { white-space:nowrap; font-size:11px; max-width:0; opacity:0; overflow:hidden }

/* Bottom card */
.bottom-card { background:#fff; border:1px solid var(--line); border-radius:var(--radius-md); overflow:hidden }
.bottom-tabs { padding:0 18px; border-bottom:1px solid var(--line-2) }
.tabs { display:flex; gap:0; border:none; margin:0 }
.tab { padding:12px 18px; font-size:13px; font-weight:600; color:var(--text-3); cursor:pointer; border-bottom:2px solid transparent; transition:all .15s }
.tab:hover { color:var(--text-1) }
.tab.on { color:var(--brand); border-bottom-color:var(--brand) }

/* Pills */
.pills-bar { padding:6px 18px; border-bottom:1px solid var(--line-2); display:flex; align-items:center; flex-wrap:wrap }
.pills { display:flex; gap:4px; flex-wrap:wrap }
.pill { padding:4px 12px; font-size:11.5px; font-weight:600; color:var(--text-2b,#556270); background:var(--hov,#ECF0F6); border-radius:var(--radius-pill); cursor:pointer; transition:all .15s; white-space:nowrap }
.pill:hover { background:var(--press,#E2E7EF) }
.pill.on { background:var(--brand); color:#fff }

/* Keyword tag */
.tg { font-size:10.5px; padding:2px 8px; border-radius:var(--radius-xs); font-weight:600 }
.tg-b { background:var(--brand-soft-12); color:var(--brand) }
.tg-r { background:rgba(229,62,62,.08); color:var(--red,#E53E3E) }

/* Diff */
.diff-p { color:var(--green,#1A9955); font-weight:650; font-family:var(--fm) }
.diff-m { color:var(--red,#E53E3E); font-weight:650; font-family:var(--fm) }

/* Compass */
.compass-body { display:flex; gap:0; padding:12px 0 }
.compass-side { flex:1; padding:0 16px }
.compass-divider { width:1px; background:var(--line); margin:8px 0 }
.compass-title { font-size:13px; font-weight:600; color:var(--text-1); margin-bottom:12px; display:flex; align-items:center; gap:6px }
.dot { width:8px; height:8px; border-radius:50% }
.opt-dot { background:var(--brand) }
.ref-dot { background:#E07C24 }
.compass-chart-row { display:flex; align-items:flex-start; gap:20px }
.donut-chart { width:120px; height:120px; border-radius:50%; position:relative; flex-shrink:0 }
.donut-hole { position:absolute; top:50%; left:50%; transform:translate(-50%,-50%); width:70px; height:70px; border-radius:50%; background:#fff; display:flex; flex-direction:column; align-items:center; justify-content:center }
.donut-total { font-family:var(--fm); font-size:18px; font-weight:700; color:var(--text-1); line-height:1 }
.donut-label { font-size:10px; color:var(--text-3); margin-top:2px }
.compass-legend { flex:1; display:flex; flex-direction:column; gap:4px }
.legend-item { display:flex; align-items:center; gap:6px; font-size:11px }
.legend-dot { width:8px; height:8px; border-radius:2px; flex-shrink:0 }
.legend-name { color:var(--text-2); flex:1 }
.legend-pct { color:var(--text-1); font-weight:500; font-family:var(--fm) }
</style>
