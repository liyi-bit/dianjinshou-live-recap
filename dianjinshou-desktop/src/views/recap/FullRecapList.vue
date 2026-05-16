<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Message, Modal } from '@arco-design/web-vue'
import { useRecapStore } from '@/stores/recap'
import { useStreamerStore } from '@/stores/streamer'
import * as analysisApi from '@/api/analysis'
import * as comparisonApi from '@/api/comparison'
import type { Recording } from '@/api/recording'
import type { Streamer } from '@/api/streamer'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import Pagination from '@/components/common/Pagination.vue'
import ExportDialog from '@/components/recap/ExportDialog.vue'
import type { AnalysisTask, AsrParagraph } from '@/api/analysis'
import { openNativeDatePicker } from '@/utils/nativeControls'
import { formatDateTime } from '@/utils/format'
import { checkCloudUploadDuplicate } from '@/utils/cloudUploadStatus'

const router = useRouter()
const route = useRoute()
const recapStore = useRecapStore()
const streamerStore = useStreamerStore()

// State
const activeTab = ref('ALL')
const searchKeyword = ref('')
const selectedStreamerId = ref<number | undefined>(undefined)
const selectedIds = ref<number[]>([])

// 全选/部分选中状态（基于当前页数据）
const isAllChecked = computed(() => {
  const items = recapStore.list || []
  return items.length > 0 && items.every((r) => selectedIds.value.includes(r.id))
})
const isPartialChecked = computed(() => {
  const items = recapStore.list || []
  const checked = items.filter((r) => selectedIds.value.includes(r.id)).length
  return checked > 0 && checked < items.length
})
function toggleSelectAll(e: Event) {
  const checked = (e.target as HTMLInputElement).checked
  const items = recapStore.list || []
  if (checked) {
    // 并集：已选 + 当前页全部
    const merged = new Set<number>(selectedIds.value)
    items.forEach((r) => merged.add(r.id))
    selectedIds.value = Array.from(merged)
  } else {
    // 差集：从已选里去掉当前页
    const pageIds = new Set(items.map((r) => r.id))
    selectedIds.value = selectedIds.value.filter((id) => !pageIds.has(id))
  }
}
const filterAnalysisStatus = ref('')
const filterStartDate = ref('')
const filterEndDate = ref('')

// Export dialog
const showExport = ref(false)
const exportTaskId = ref(0)
const exportTask = ref<AnalysisTask | null>(null)
const exportParagraphs = ref<AsrParagraph[]>([])
const exportStreamerName = ref('')

// Rename
const renameDialogVisible = ref(false)
const renameTarget = ref<Recording | null>(null)
const renameName = ref('')

// Delete
const deleteDialogVisible = ref(false)
const deleteTargetIds = ref<number[]>([])

// Streamer search
const streamerSearch = ref('')
const asideCollapsed = ref(false)

const filteredStreamers = computed(() => {
  const kw = streamerSearch.value.trim()
  if (!kw) return streamerStore.allStreamers
  return streamerStore.allStreamers.filter((s) => s.anchorName.includes(kw))
})

const todayAllCount = computed(() =>
  streamerStore.allStreamers.reduce((sum, s) => sum + (s.todaySessions || 0), 0)
)

const tabs = [
  { key: 'ALL', label: '全部' },
  { key: 'COMPLETED', label: '已完成分析' },
  { key: 'DIAG_DONE', label: '已完成AI诊断' }
]

function getStatusLabel(status: string) {
  const map: Record<string, string> = {
    recording: '录制中',
    completed: '已完成',
    failed: '录制失败',
    interrupted: '录制中断',
    pending: '等待中',
    stopped: '已停止'
  }
  return map[status] || status
}

function getAnalysisStatusLabel(status: string) {
  const map: Record<string, string> = {
    none: '未分析',
    pending: '排队分析中',
    recording: '录制中',
    transcribing: '逐字稿生成中',
    transcribed: '未分析',
    asr_processing: '语音转写中',
    ai_processing: 'AI分析中',
    completed: '分析完成',
    failed: '分析失败',
    skipped: '录制中断'
  }
  return map[status] || status
}

function getAnalysisStatusColor(status: string) {
  const map: Record<string, string> = {
    none: '#8693A4',
    pending: '#ff7d00',
    recording: '#3491FA',
    transcribing: '#3491FA',
    transcribed: '#E07B00',
    asr_processing: '#ff7d00',
    ai_processing: '#ff7d00',
    completed: 'var(--green)',
    failed: '#f53f3f',
    skipped: '#8693A4'
  }
  return map[status] || '#8693A4'
}

function formatDuration(seconds: number | null) {
  if (!seconds) return '-'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  if (h > 0) return `${h}h${m}m${s}s`
  return `${m}m${s}s`
}

function formatFileSize(bytes: number | null) {
  if (!bytes) return '-'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}

function formatTime(datetime: string | null) {
  return datetime ? formatDateTime(datetime) : '-'
}

function fileNameOf(path: string) {
  return path.split(/[\\/]/).pop() || path
}

function getStreamerUploadMeta(streamerId?: number | null) {
  const streamer = streamerStore.allStreamers.find((item) => item.id === streamerId)
  return {
    streamerId: streamer?.cloudSyncEnabled ? streamer.id : undefined,
    anchorName: streamer?.anchorName,
    industryId: streamer?.industryId ?? undefined,
    accountType: streamer?.accountType,
  }
}

// Actions
function onTabChange(key: string) {
  activeTab.value = key
  recapStore.setTab(key)
}

function onStreamerSelect(streamerId: number | undefined) {
  selectedStreamerId.value = streamerId
  recapStore.setStreamerId(streamerId)
}

function onSearch() {
  // "录制中" 是 status 不是 analysisStatus，分开路由
  if (filterAnalysisStatus.value === 'recording') {
    recapStore.query.status = 'recording'
    recapStore.query.analysisStatus = undefined
  } else {
    recapStore.query.status = undefined
    recapStore.query.analysisStatus = filterAnalysisStatus.value || undefined
  }
  recapStore.query.startDate = filterStartDate.value || undefined
  recapStore.query.endDate = filterEndDate.value || undefined
  recapStore.query.page = 1
  recapStore.fetchList()
}

function onPageChange(page: number) {
  recapStore.setPage(page)
}

function goToDetail(record: Recording) {
  if (!record.latestTaskId) {
    Message.warning('该录制尚未进行分析，请先生成诊断')
    return
  }
  router.push({ name: 'RecapDetail', params: { id: record.latestTaskId } })
}

// v1.1.0：列表页直接触发 AI 复盘（仅 transcribed 状态可点）
const startingAiIds = ref<Set<number>>(new Set())
async function handleStartAiFromList(record: Recording) {
  if (!record.latestTaskId) {
    Message.warning('尚未生成逐字稿，无法进行 AI 复盘')
    return
  }
  const taskId = record.latestTaskId as number
  if (startingAiIds.value.has(taskId)) return
  startingAiIds.value.add(taskId)
  try {
    await analysisApi.startAiAnalysis(taskId)
    Message.success('已开始 AI 复盘，稍候刷新查看结果')
    recapStore.fetchList()
  } catch (err: any) {
    if (err?.code === 40006) {
      Modal.warning({
        title: '今日 AI 复盘额度已用完',
        content: '每个账号每天最多 10 次 AI 复盘。明天 0 点自动重置，届时可继续使用。',
        okText: '我知道了',
        hideCancel: true,
      })
    } else {
      Message.error(err?.message || 'AI 复盘触发失败')
    }
  } finally {
    startingAiIds.value.delete(taskId)
  }
}

async function startAnalysis(record: Recording) {
  if (!record.localFilePath) {
    Message.error('未找到本地录制文件，无法进行分析')
    return
  }
  const api = (window as any).electronAPI
  if (!api?.runAsr) {
    Message.error('当前环境不支持桌面端ASR')
    return
  }
  try {
    Message.info('正在进行语音识别，请耐心等待...')
    const asrResult = await api.runAsr(record.localFilePath)
    if (!asrResult.success || !asrResult.data?.length) {
      Message.error('语音识别失败: ' + (asrResult.error || '未识别到内容'))
      return
    }
    await analysisApi.submitAsrResult({
      recordingId: record.id,
      segments: asrResult.data,
    })
    Message.success('分析任务已提交')
    recapStore.fetchList()
  } catch {
    Message.error('分析失败')
  }
}

function goToOptimization(record: Recording) {
  if (!record.latestTaskId) {
    Message.warning('该录制尚未进行分析，请先生成诊断')
    return
  }
  router.push({ name: 'RecapDetail', params: { id: record.latestTaskId }, query: { tab: 'optimization' } })
}

async function openExportDialog(record: Recording) {
  if (!record.latestTaskId) {
    Message.warning('该录制尚未进行分析，请先生成诊断')
    return
  }
  try {
    const res = await analysisApi.getAnalysis(record.latestTaskId)
    const task = (res as any).data ?? res
    exportTaskId.value = record.latestTaskId
    exportTask.value = task
    exportStreamerName.value = record.anchorName || record.localFileName || '主播'
    // Fetch paragraphs
    try {
      const pRes = await analysisApi.getParagraphs(record.latestTaskId)
      exportParagraphs.value = ((pRes as any).data ?? pRes) || []
    } catch {
      exportParagraphs.value = []
    }
    showExport.value = true
  } catch {
    Message.error('获取分析数据失败')
  }
}

function viewSummary(record: Recording) {
  if (!record.latestTaskId) {
    Message.warning('该录制尚未进行分析，请先生成诊断')
    return
  }
  router.push({ name: 'RecapDetail', params: { id: record.latestTaskId }, query: { tab: 'summary' } })
}

function viewDiagnosis(record: Recording) {
  if (!record.latestTaskId) {
    Message.warning('该录制尚未进行分析')
    return
  }
  router.push({ name: 'RecapDetail', params: { id: record.latestTaskId }, query: { tab: 'diagnosis' } })
}

function openRename(record: Recording) {
  renameTarget.value = record
  renameName.value = record.localFileName
  renameDialogVisible.value = true
}

async function confirmRename() {
  if (!renameTarget.value) return
  const name = renameName.value.trim()
  if (!name) {
    Message.warning('名称不能为空')
    return
  }
  if (name.length > 15) {
    Message.warning('名称不能超过15个字符')
    return
  }
  try {
    await recapStore.renameRecording(renameTarget.value.id, name)
    Message.success('重命名成功')
    renameDialogVisible.value = false
  } catch {
    Message.error('重命名失败')
  }
}

function openBatchDelete() {
  if (selectedIds.value.length === 0) {
    Message.warning('请先选择要删除的记录')
    return
  }
  deleteTargetIds.value = [...selectedIds.value]
  deleteDialogVisible.value = true
}

async function confirmDelete() {
  try {
    await recapStore.batchDelete(deleteTargetIds.value)
    Message.success('删除成功')
    selectedIds.value = []
    deleteDialogVisible.value = false
  } catch {
    Message.error('删除失败')
  }
}

function onSelectionChange(rowKeys: (string | number)[]) {
  selectedIds.value = rowKeys.map(Number)
}

// More dropdown menu
const moreMenuId = ref<number | null>(null)
const moreMenuStyle = ref<Record<string, string>>({})

function toggleMoreMenu(id: number, event: MouseEvent) {
  if (moreMenuId.value === id) {
    moreMenuId.value = null
    return
  }
  moreMenuId.value = id
  const btn = event.currentTarget as HTMLElement
  const rect = btn.getBoundingClientRect()
  const spaceBelow = window.innerHeight - rect.bottom
  const menuHeight = 200 // 预估菜单高度
  if (spaceBelow < menuHeight) {
    // 向上弹出
    moreMenuStyle.value = { position: 'fixed', right: `${window.innerWidth - rect.right}px`, bottom: `${window.innerHeight - rect.top + 4}px` }
  } else {
    // 向下弹出
    moreMenuStyle.value = { position: 'fixed', right: `${window.innerWidth - rect.right}px`, top: `${rect.bottom + 4}px` }
  }
}
function closeMoreMenu() {
  moreMenuId.value = null
}

// --- Real button handlers ---

function openVideoFolder(record: Recording) {
  if (!record.localFilePath) {
    Message.warning('未找到本地视频文件路径')
    return
  }
  window.electronAPI.showItemInFolder(record.localFilePath)
}

function openOriginalVideo(record: Recording) {
  if (!record.localFilePath) {
    Message.warning('未找到本地视频文件路径')
    return
  }
  window.electronAPI.openFile(record.localFilePath).then((res) => {
    if (!res.success) Message.error(res.error || '打开视频失败')
  })
}

function goToAssistant(record: Recording) {
  if (!record.latestTaskId) {
    Message.warning('该录制尚未进行分析，无法打开AI助手')
    return
  }
  router.push({ name: 'AssistantOperation', query: { taskId: String(record.latestTaskId) } })
}

async function uploadFullRecapToCloud(record: Recording) {
  const api = (window as any).electronAPI
  if (!api?.enqueueCloudUpload) {
    Message.warning('当前环境不支持云空间上传')
    return
  }
  if (!record.localFilePath) {
    Message.warning('未找到本地视频文件路径')
    return
  }
  const streamerMeta = getStreamerUploadMeta(record.streamerId)
  try {
    const duplicate = await checkCloudUploadDuplicate({
      electronApi: api,
      businessType: 'full_recap',
      businessId: record.id,
      recordingId: record.id,
    })
    if (duplicate.duplicate) {
      Message.warning(duplicate.message || '该全场复盘已在云空间，无需重复上传')
      return
    }
    const result = await api.enqueueCloudUpload({
      id: `full_recap_${record.id}`,
      filePath: record.localFilePath,
      fileName: record.localFileName || fileNameOf(record.localFilePath),
      businessType: 'full_recap',
      businessId: record.id,
      recordingId: record.id,
      streamerId: streamerMeta.streamerId,
      anchorName: streamerMeta.anchorName || record.anchorName || undefined,
      industryId: streamerMeta.industryId,
      accountType: streamerMeta.accountType,
      recordedAt: record.startTime,
      durationSeconds: record.duration ?? undefined,
      manualUpload: true,
    })
    if (result?.success) Message.success('已加入云空间上传队列')
    else Message.error(result?.error || '加入云空间上传队列失败')
  } catch (err: any) {
    Message.error(err?.message || '加入云空间上传队列失败')
  }
}

/**
 * Re-analyze a recording. If the record has a completed/diagnosed task, trigger the
 * backend's fast AI-only re-run. Otherwise (no task, stuck, or failed) restart from
 * scratch: ensure MP4 → desktop ASR → submit ASR result to create a fresh task.
 */
async function doReAnalyze(record: Recording) {
  const api = (window as any).electronAPI
  const stuckStatuses = ['none', 'pending', 'asr_processing', 'ai_processing', 'failed']
  const canFastPath =
    record.latestTaskId && record.analysisStatus &&
    !stuckStatuses.includes(record.analysisStatus)

  if (canFastPath) {
    try {
      await analysisApi.reAnalyze(record.latestTaskId as number)
      Message.success('已重新提交分析任务')
      recapStore.fetchList()
    } catch {
      // request.ts already showed a toast
    }
    return
  }

  // From-scratch flow
  if (!record.localFilePath) {
    Message.error('未找到本地录制文件，无法进行分析')
    return
  }
  if (!api?.ensureMp4 || !api?.runAsr) {
    Message.error('当前环境不支持桌面端 ASR')
    return
  }

  try {
    // Step 1: make sure we have an MP4 (remux if still FLV).
    Message.info('正在检查视频格式...')
    const mp4Res = await api.ensureMp4(record.localFilePath)
    if (!mp4Res.success || !mp4Res.path) {
      Message.error(mp4Res.error || '视频格式转换失败')
      return
    }
    if (mp4Res.converted) Message.success('视频已转为 MP4')

    // Step 2: run desktop ASR.
    Message.info('正在进行语音识别，请耐心等待...')
    const asrResult = await api.runAsr(mp4Res.path)
    if (!asrResult.success || !asrResult.data?.length) {
      Message.error('语音识别失败：' + (asrResult.error || '未识别到内容'))
      return
    }

    // Step 3: submit ASR to backend; backend creates a fresh task at ai_processing state.
    await analysisApi.submitAsrResult({
      recordingId: record.id,
      segments: asrResult.data,
    })
    Message.success('分析任务已提交')
    recapStore.fetchList()
  } catch (err: any) {
    Message.error('重新分析失败：' + (err?.message || '未知错误'))
  }
}

async function doCancelAnalysis(record: Recording) {
  if (!record.latestTaskId) return
  try {
    await analysisApi.cancelAnalysis(record.latestTaskId)
    Message.success('已取消分析')
    recapStore.fetchList()
  } catch {
    Message.error('取消分析失败')
  }
}

// Comparison draft
const comparisonDraft = ref<{ id: number; firstRecordingId: number } | null>(null)

async function addToCompare(record: Recording) {
  try {
    if (!comparisonDraft.value) {
      // First pick: create draft
      const res = await comparisonApi.createDraft({
        firstRecordingId: record.id,
        listContext: 'AI_FULL_RECAP'
      })
      comparisonDraft.value = (res as any).data ?? res
      Message.success('已选为对比基准，请再选一场作为参照')
    } else {
      if (comparisonDraft.value.firstRecordingId === record.id) {
        Message.warning('不能与自己对比，请选择另一场录制')
        return
      }
      // Second pick: complete comparison
      const res = await comparisonApi.selectSecond(comparisonDraft.value.id, {
        secondRecordingId: record.id,
        listContext: 'AI_FULL_RECAP'
      })
      const comparison = (res as any).data ?? res
      comparisonDraft.value = null
      Message.success('对比已创建')
      router.push({ name: 'FullComparisonList' })
    }
  } catch {
    Message.error('对比操作失败')
  }
}

function cancelCompare() {
  comparisonDraft.value = null
  comparisonApi.cancelDraft().catch(() => {})
  Message.info('已取消对比选择')
}

onMounted(() => {
  // 每次重新进入页面，清空上次的筛选缓存
  recapStore.resetQuery()
  selectedStreamerId.value = undefined
  filterAnalysisStatus.value = ''
  filterStartDate.value = ''
  filterEndDate.value = ''
  streamerSearch.value = ''

  // 支持从直播间列表跳转时通过 query 参数筛选主播
  const qStreamerId = route.query.streamerId
  if (qStreamerId) {
    const sid = Number(qStreamerId)
    if (!isNaN(sid)) {
      selectedStreamerId.value = sid
      recapStore.setStreamerId(sid)
    }
  }
  recapStore.fetchList()
  recapStore.fetchTotalAll()
  streamerStore.fetchList()
  streamerStore.fetchAllStreamers()
})

const columns = [
  { title: '文件名', dataIndex: 'localFileName', width: 180 },
  { title: '录制状态', dataIndex: 'status', width: 100 },
  { title: '分析状态', dataIndex: 'analysisStatus', width: 120 },
  { title: '开始时间', dataIndex: 'startTime', width: 160 },
  { title: '时长', dataIndex: 'duration', width: 80 },
  { title: '文件大小', dataIndex: 'fileSize', width: 100 },
  { title: '分辨率', dataIndex: 'resolution', width: 100 },
  { title: '敏感词', dataIndex: 'sensitiveWordCount', width: 80 },
  { title: '运营词', dataIndex: 'operationKeywordCount', width: 80 },
  { title: '操作', slotName: 'actions', width: 220, fixed: 'right' }
]
</script>

<template>
  <div class="recap-layout" :class="{ 'aside-collapsed': asideCollapsed }">
    <!-- Left Panel: Streamer Sub-List -->
    <div class="streamer-panel">
      <span class="toggle" role="button" @click="asideCollapsed = !asideCollapsed">
        {{ asideCollapsed ? '≫ 展开菜单' : '≡ 收起菜单' }}
      </span>

      <input
        v-model="streamerSearch"
        class="djsinput sq"
        placeholder="搜索主播"
        style="font-size:12px"
      />

      <!-- 全部 summary card -->
      <div
        class="streamer-summary"
        :class="{ active: !selectedStreamerId }"
        @click="onStreamerSelect(undefined)"
      >
        <span class="title">全部</span>
        <div class="counts">
          <span class="count-chip">总 <b>{{ recapStore.totalAll }}</b></span>
          <span class="count-chip">今 <b>{{ todayAllCount }}</b></span>
        </div>
      </div>

      <!-- Individual streamer cards -->
      <div class="streamer-list">
        <div
          v-for="s in filteredStreamers"
          :key="s.id"
          class="streamer-item"
          :class="{ active: selectedStreamerId === s.id }"
          @click="onStreamerSelect(s.id)"
        >
          <span class="djsav grad-blue" style="width:30px;height:30px;font-size:12px;flex-shrink:0;overflow:hidden">
            <img v-if="s.anchorAvatar" :src="s.anchorAvatar" style="width:100%;height:100%;object-fit:cover" referrerpolicy="no-referrer" />
            <template v-else>{{ s.anchorName ? s.anchorName.charAt(0) : '?' }}</template>
          </span>
          <div style="min-width:0;flex:1">
            <div class="name">{{ s.anchorName }}</div>
            <div class="meta">总/<b>{{ s.totalSessions ?? 0 }}</b> 今/<b>{{ s.todaySessions ?? 0 }}</b></div>
          </div>
        </div>
      </div>
    </div>

    <!-- Main Area -->
    <div class="main-area">
      <!-- Tabs -->
      <div class="djstabs" style="padding:0 20px;border-bottom:1px solid var(--line)">
        <span
          v-for="tab in tabs"
          :key="tab.key"
          class="djstab"
          :class="{ active: activeTab === tab.key }"
          @click="onTabChange(tab.key)"
        >{{ tab.label }}</span>
      </div>

      <!-- Toolbar -->
      <div class="djstoolbar" style="padding:10px 20px;display:flex;align-items:center;gap:10px;border-bottom:1px solid var(--line)">
        <select v-model="filterAnalysisStatus" class="djsselect" style="font-size:12px" @change="onSearch">
          <option value="">分析状态</option>
          <option value="recording">录制中</option>
          <option value="none">未分析</option>
          <option value="pending">排队分析中</option>
          <option value="asr_processing">语音转写中</option>
          <option value="transcribed">转写完成（待 AI 分析）</option>
          <option value="ai_processing">AI 分析中</option>
          <option value="completed">分析完成</option>
          <option value="failed">分析失败</option>
          <option value="skipped">录制中断</option>
        </select>
        <input v-model="filterStartDate" type="date" class="djsinput" style="font-size:12px" placeholder="录制时间起" @click="openNativeDatePicker" @change="onSearch" />
        <input v-model="filterEndDate" type="date" class="djsinput" style="font-size:12px" placeholder="录制时间止" @click="openNativeDatePicker" @change="onSearch" />
        <button class="djsbtn primary sm" @click="onSearch">查找</button>
        <a class="djslink danger sm" style="margin-left:auto;font-size:12px" @click="openBatchDelete">批量删除</a>
        <a class="djslink sm" style="font-size:12px" @click="recapStore.fetchList()">刷新</a>
      </div>

      <!-- Comparison draft hint -->
      <div v-if="comparisonDraft" style="padding:8px 20px;background:#FFF7E6;border-bottom:1px solid var(--line);display:flex;align-items:center;gap:8px;font-size:12px">
        <span style="color:#ff7d00">📊 已选择基准录制，请点击另一场录制的「选为参照」完成对比</span>
        <a class="djslink sm" style="color:var(--text-3)" @click="cancelCompare">取消</a>
      </div>

      <!-- Table -->
      <div class="table-wrap">
        <table class="djstbl" style="width:100%">
          <thead>
            <tr>
              <th style="width:24px">
                <input
                  type="checkbox"
                  :checked="isAllChecked"
                  :indeterminate.prop="isPartialChecked"
                  @change="toggleSelectAll"
                />
              </th>
              <th style="width:140px">本地文件名</th>
              <th style="width:72px">录制时间</th>
              <th style="width:52px">分析状态</th>
              <th style="width:32px">话术</th>
              <th style="width:130px;text-align:center">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="recapStore.loading">
              <td colspan="6" style="text-align:center;padding:32px;color:var(--text-3);font-size:13px">加载中…</td>
            </tr>
            <tr v-else-if="!recapStore.list || recapStore.list.length === 0">
              <td colspan="6" style="text-align:center;padding:32px;color:var(--text-3);font-size:13px">暂无数据</td>
            </tr>
            <template v-else>
              <tr v-for="record in recapStore.list" :key="record.id">
                <!-- Checkbox -->
                <td>
                  <input
                    type="checkbox"
                    :checked="selectedIds.includes(record.id)"
                    @change="(e) => {
                      const checked = (e.target as HTMLInputElement).checked
                      if (checked) selectedIds.push(record.id)
                      else { const idx = selectedIds.indexOf(record.id); if (idx > -1) selectedIds.splice(idx, 1) }
                    }"
                  />
                </td>

                <!-- 本地文件名 -->
                <td>
                  <div style="display:flex;align-items:center;gap:8px">
                    <span class="djsav grad-purple" style="width:28px;height:28px;font-size:11px;flex-shrink:0;overflow:hidden">
                      <img v-if="record.anchorAvatar" :src="record.anchorAvatar" style="width:100%;height:100%;object-fit:cover" referrerpolicy="no-referrer" />
                      <template v-else>{{ record.anchorName ? record.anchorName.charAt(0) : (record.localFileName ? record.localFileName.charAt(0).toUpperCase() : 'R') }}</template>
                    </span>
                    <span
                      style="font-size:13px;color:var(--text-1);cursor:pointer;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;min-width:0"
                      @dblclick="openRename(record)"
                      :title="record.localFileName"
                    >{{ record.localFileName || '-' }}</span>
                  </div>
                </td>

                <!-- 录制时间 -->
                <td style="font-size:12px;color:var(--text-2b);white-space:nowrap">
                  <div>{{ record.startTime ? formatTime(record.startTime).substring(0,10) : '-' }}</div>
                  <div style="color:var(--text-3)">{{ record.startTime ? formatTime(record.startTime).substring(11,19) : '' }}</div>
                </td>

                <!-- 分析状态：录制中优先，录制完了走分析状态（none=未分析）-->
                <td>
                  <span
                    v-if="record.status === 'recording'"
                    style="font-size:12px;font-weight:500;color:#4080ff"
                  >
                    录制中
                  </span>
                  <span
                    v-else-if="['pending','asr_processing','ai_processing'].includes(record.analysisStatus)"
                    style="font-size:12px;font-weight:500;color:#ff7d00"
                  >
                    {{ getAnalysisStatusLabel(record.analysisStatus) }}
                  </span>
                  <span v-else :style="{ fontSize:'12px', fontWeight:500, color: getAnalysisStatusColor(record.analysisStatus) }">
                    {{ getAnalysisStatusLabel(record.analysisStatus) }}
                  </span>
                </td>

                <!-- 话术 -->
                <td>
                  <a class="djslink sm" @click="openExportDialog(record)">导出</a>
                </td>

                <!-- 操作 -->
                <td>
                  <div class="ops-cell">
                    <div class="ops-row">
                      <button
                        v-if="record.analysisStatus === 'transcribed'"
                        class="djsbtn primary sm"
                        :disabled="startingAiIds.has(record.latestTaskId as number)"
                        @click="handleStartAiFromList(record)"
                      >
                        {{ startingAiIds.has(record.latestTaskId as number) ? '提交中...' : 'AI 复盘' }}
                      </button>
                      <button class="djsbtn primary sm" @click="goToDetail(record)">查看整场分析</button>
                      <a class="djslink sm" @click="addToCompare(record)">
                        {{ comparisonDraft ? (comparisonDraft.firstRecordingId === record.id ? '已选为基准' : '选为参照') : '加对比' }}
                      </a>
                    </div>
                    <div class="ops-row">
                      <a class="djslink sm" @click="openVideoFolder(record)">视频文件夹</a>
                      <span style="color:var(--line)">|</span>
                      <div class="more-dropdown">
                        <a class="djslink sm" style="color:var(--text-3)" @click="toggleMoreMenu(record.id, $event)">更多 ▾</a>
                        <Teleport to="body">
                        <div v-if="moreMenuId === record.id" class="more-menu-overlay" @click="closeMoreMenu">
                          <div class="more-menu" :style="moreMenuStyle" @click.stop>
                            <div class="more-item" @click="() => { closeMoreMenu(); goToAssistant(record) }">AI助手</div>
                            <div class="more-item" @click="() => { closeMoreMenu(); openOriginalVideo(record) }">原视频</div>
                            <div class="more-item" @click="() => { closeMoreMenu(); doReAnalyze(record) }">再分析</div>
                            <div class="more-item" @click="() => { closeMoreMenu(); openRename(record) }">修改文件名</div>
                            <div class="more-item" @click="() => { closeMoreMenu(); uploadFullRecapToCloud(record) }">上传云端</div>
                            <div
                              :class="['more-item', 'danger', { disabled: ['pending','asr_processing','ai_processing'].includes(record.analysisStatus) }]"
                              @click="() => { if (!['pending','asr_processing','ai_processing'].includes(record.analysisStatus)) { closeMoreMenu(); deleteTargetIds = [record.id]; deleteDialogVisible = true } }"
                            >删除</div>
                          </div>
                        </div>
                        </Teleport>
                      </div>
                    </div>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
        <Pagination
          :page="recapStore.query.page"
          :size="recapStore.query.size"
          :total="recapStore.total"
          @change="(p) => { recapStore.query.page = p; recapStore.fetchList() }"
        />
      </div>
    </div>
  </div>

  <!-- Rename dialog -->
  <a-modal v-model:visible="renameDialogVisible" title="重命名" @ok="confirmRename" ok-text="确定" cancel-text="取消">
    <a-input v-model="renameName" placeholder="请输入文件名（最多15字）" :max-length="15" />
  </a-modal>

  <!-- Delete dialog -->
  <ConfirmDialog
    :visible="deleteDialogVisible"
    title="确认删除"
    content="确定要删除选中的录制记录吗？删除后无法恢复。"
    @confirm="confirmDelete"
    @cancel="deleteDialogVisible = false"
  />

  <ExportDialog
    v-model:visible="showExport"
    :task-id="exportTaskId"
    :task="exportTask"
    :paragraphs="exportParagraphs"
    :streamer-name="exportStreamerName"
  />
</template>

<style scoped>
.recap-layout { display:flex; height:100% }
.streamer-panel { width:220px; background:var(--card); border-right:1px solid var(--line); padding:14px 12px; flex-shrink:0; display:flex; flex-direction:column; gap:10px; transition:width .2s ease }
.streamer-panel .toggle { font-size:12px; color:var(--text-2b); cursor:pointer; padding:4px 6px; user-select:none; align-self:flex-start; border-radius:4px }
.streamer-panel .toggle:hover { background:var(--bg-2); color:var(--text-1) }
.recap-layout.aside-collapsed .streamer-panel { width:44px; padding:14px 6px; gap:6px }
.recap-layout.aside-collapsed .streamer-panel > :not(.toggle) { display:none }
.streamer-summary { background:var(--brand-soft); border-radius:var(--radius-lg); padding:12px 14px; display:flex; align-items:center; justify-content:space-between; cursor:pointer }
.streamer-summary.active { box-shadow:inset 0 0 0 2px var(--brand) }
.streamer-summary .title { font-size:13px; color:var(--text-1); font-weight:600 }
.streamer-summary .counts { display:flex; gap:6px }
.streamer-summary .count-chip { background:var(--card); border-radius:8px; padding:3px 9px; font-size:11px; font-family:var(--fm); color:var(--text-2b) }
.streamer-summary .count-chip b { color:var(--brand) }
.streamer-list { display:flex; flex-direction:column; gap:8px; overflow-y:auto; flex:1 }
.streamer-item { padding:9px 10px; border-radius:var(--radius-lg); cursor:pointer; display:flex; align-items:center; gap:10px }
.streamer-item:hover { background:var(--bg) }
.streamer-item.active { background:var(--brand-soft); box-shadow:inset 0 0 0 1.5px rgba(184,130,58,.3) }
.streamer-item .name { font-size:12px; font-weight:500; color:var(--text-1); white-space:nowrap; overflow:hidden; text-overflow:ellipsis }
.streamer-item .meta { font-size:10px; color:var(--text-3); margin-top:2px }
.streamer-item .meta b { color:var(--brand); font-family:var(--fm) }
.main-area { flex:1; display:flex; flex-direction:column; overflow:hidden }
.table-wrap { flex:1; overflow-y:auto; overflow-x:hidden; padding:0 14px 14px }
.table-wrap .djstbl { table-layout:fixed; width:100% }
.table-wrap .djstbl thead th { padding:10px 8px; font-size:10px }
.table-wrap .djstbl tbody td { padding:12px 8px; font-size:12.5px; overflow:hidden; text-overflow:ellipsis }
.core-data { font-size:11px; line-height:1.7; white-space:nowrap }
.core-data .dot { display:inline-block; width:6px; height:6px; border-radius:50%; margin-right:5px }
.core-data .pink { background:#EC4899 }
.core-data .red { background:var(--red) }
.core-data .yellow { background:#F59E0B }
.ops-cell { display:flex; flex-direction:column; align-items:flex-end; gap:4px }
.ops-row { display:flex; gap:8px; align-items:center }
.more-dropdown { position:relative }
</style>
