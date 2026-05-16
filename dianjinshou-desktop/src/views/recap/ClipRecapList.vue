<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { useStreamerStore } from '@/stores/streamer'
import * as analysisApi from '@/api/analysis'
import * as comparisonApi from '@/api/comparison'
import * as recordingApi from '@/api/recording'
import type { AnalysisTask } from '@/api/analysis'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import Pagination from '@/components/common/Pagination.vue'
import { formatDateTime } from '@/utils/format'
import { checkCloudUploadDuplicate } from '@/utils/cloudUploadStatus'

const router = useRouter()
const streamerStore = useStreamerStore()

const list = ref<AnalysisTask[]>([])
const total = ref(0)
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)

const selectedStreamerId = ref<number | undefined>(undefined)
const streamerSearch = ref('')
const categoryFilter = ref('')
const selectedIds = ref<number[]>([])

const isAllChecked = computed(() => list.value.length > 0 && list.value.every((t) => selectedIds.value.includes(t.id)))
const isPartialChecked = computed(() => {
  const checked = list.value.filter((t) => selectedIds.value.includes(t.id)).length
  return checked > 0 && checked < list.value.length
})
function toggleSelectAll(e: Event) {
  const checked = (e.target as HTMLInputElement).checked
  if (checked) {
    const merged = new Set<number>(selectedIds.value)
    list.value.forEach((t) => merged.add(t.id))
    selectedIds.value = Array.from(merged)
  } else {
    const pageIds = new Set(list.value.map((t) => t.id))
    selectedIds.value = selectedIds.value.filter((id) => !pageIds.has(id))
  }
}

const CLIP_CATEGORIES = [
  { value: '', label: '全部类型' },
  { value: 'RETENTION', label: '留人切片' },
  { value: 'QUALITY_SPEECH', label: '优质话术' },
  { value: 'MARKETING', label: '营销塑品' },
  { value: 'INTERACTION', label: '互动切片' },
  { value: 'FAN_CLUB', label: '粉团切片' },
  { value: 'EXPRESSION', label: '表现力切片' },
  { value: 'COMPLIANCE', label: '规避违规' },
  { value: 'OTHER', label: '其他' }
]

const filteredStreamers = computed(() => {
  const kw = streamerSearch.value.trim()
  if (!kw) return streamerStore.allStreamers
  return streamerStore.allStreamers.filter((s) => s.anchorName.includes(kw))
})

function getCategoryLabel(code: string | null): string {
  if (!code) return '-'
  const cat = CLIP_CATEGORIES.find(c => c.value === code)
  return cat?.label || code
}

function getStatusLabel(status: string) {
  const map: Record<string, string> = {
    pending: '排队中', asr_processing: '语音转写中',
    transcribing: '逐字稿生成中', transcribed: '未分析',
    ai_processing: 'AI分析中', completed: '已完成', failed: '失败'
  }
  return map[status] || status
}

function getStatusColor(status: string) {
  const map: Record<string, string> = {
    pending: '#ff7d00', asr_processing: '#ff7d00',
    transcribing: '#3491FA', transcribed: '#E07B00',
    ai_processing: '#ff7d00', completed: 'var(--green)', failed: '#f53f3f'
  }
  return map[status] || '#8693A4'
}

function onCategoryChange() {
  page.value = 1
  fetchList()
}

async function fetchList() {
  loading.value = true
  try {
    const res = await analysisApi.listAnalysisTasks({
      type: 'clip',
      streamerId: selectedStreamerId.value,
      clipCategory: categoryFilter.value || undefined,
      page: page.value,
      size: pageSize.value
    })
    const data = (res as any).data ?? res
    list.value = data.items || []
    total.value = data.total || 0
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onStreamerSelect(id: number | undefined) {
  selectedStreamerId.value = id
  page.value = 1
  fetchList()
}

function onPageChange(p: number) {
  page.value = p
  fetchList()
}

function goToDetail(task: AnalysisTask) {
  router.push({ name: 'RecapDetail', params: { id: task.id } })
}

function toggleSelect(id: number) {
  const idx = selectedIds.value.indexOf(id)
  if (idx === -1) selectedIds.value.push(id)
  else selectedIds.value.splice(idx, 1)
}

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
  const menuHeight = 160
  if (spaceBelow < menuHeight) {
    moreMenuStyle.value = { position: 'fixed', right: `${window.innerWidth - rect.right}px`, bottom: `${window.innerHeight - rect.top + 4}px` }
  } else {
    moreMenuStyle.value = { position: 'fixed', right: `${window.innerWidth - rect.right}px`, top: `${rect.bottom + 4}px` }
  }
}
function closeMoreMenu() {
  moreMenuId.value = null
}

// Delete
const deleteDialogVisible = ref(false)
const deleteTargetIds = ref<number[]>([])

async function confirmDelete() {
  try {
    await analysisApi.batchDeleteAnalysis(deleteTargetIds.value)
    Message.success('删除成功')
    selectedIds.value = []
    deleteDialogVisible.value = false
    fetchList()
  } catch {
    Message.error('删除失败')
  }
}

// Comparison draft
const comparisonDraft = ref<{ id: number; firstRecordingId: number; firstTaskId: number } | null>(null)

async function addToCompare(task: AnalysisTask) {
  try {
    if (!comparisonDraft.value) {
      const res = await comparisonApi.createDraft({
        firstRecordingId: task.recordingId,
        firstTaskId: task.id,
        listContext: 'AI_CLIP_RECAP'
      })
      const data = (res as any).data ?? res
      comparisonDraft.value = { ...data, firstTaskId: task.id }
      Message.success('已选为对比基准，请再选一条切片作为参照')
    } else {
      if (comparisonDraft.value.firstTaskId === task.id) {
        Message.warning('不能与自己对比，请选择另一条切片')
        return
      }
      await comparisonApi.selectSecond(comparisonDraft.value.id, {
        secondRecordingId: task.recordingId,
        secondTaskId: task.id,
        listContext: 'AI_CLIP_RECAP'
      })
      comparisonDraft.value = null
      Message.success('对比已创建')
      router.push({ name: 'ClipComparisonList' })
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

// --- Real button handlers ---

function openVideoFolder(task: AnalysisTask) {
  if (!task.clipFilePath) {
    Message.warning('未找到本地切片文件路径')
    return
  }
  window.electronAPI.showItemInFolder(task.clipFilePath)
}

function openOriginalVideo(task: AnalysisTask) {
  if (!task.clipFilePath) {
    Message.warning('未找到本地切片文件路径')
    return
  }
  window.electronAPI.openFile(task.clipFilePath).then((res) => {
    if (!res.success) Message.error(res.error || '打开视频失败')
  })
}

function goToAssistant(task: AnalysisTask) {
  router.push({ name: 'AssistantOperation', query: { taskId: String(task.id) } })
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

async function uploadClipRecapToCloud(task: AnalysisTask) {
  const api = (window as any).electronAPI
  if (!api?.enqueueCloudUpload) {
    Message.warning('当前环境不支持云空间上传')
    return
  }
  if (!task.clipFilePath) {
    Message.warning('未找到本地切片文件路径')
    return
  }
  try {
    const recordingRes: any = await recordingApi.getRecording(task.recordingId)
    const recording = recordingRes?.data ?? recordingRes
    const streamerMeta = getStreamerUploadMeta(recording?.streamerId)
    const durationSeconds = task.clipStart != null && task.clipEnd != null
      ? Math.max(0, task.clipEnd - task.clipStart)
      : undefined
    const duplicate = await checkCloudUploadDuplicate({
      electronApi: api,
      businessType: 'clip_recap',
      businessId: task.id,
      recordingId: task.recordingId,
      clipId: task.id,
    })
    if (duplicate.duplicate) {
      Message.warning(duplicate.message || '该切片复盘已在云空间，无需重复上传')
      return
    }
    const result = await api.enqueueCloudUpload({
      id: `clip_recap_${task.id}`,
      filePath: task.clipFilePath,
      fileName: task.clipFilename || fileNameOf(task.clipFilePath),
      businessType: 'clip_recap',
      businessId: task.id,
      recordingId: task.recordingId,
      clipId: task.id,
      streamerId: streamerMeta.streamerId,
      anchorName: streamerMeta.anchorName || recording?.streamerInfo?.anchorName || recording?.anchorName || undefined,
      industryId: streamerMeta.industryId,
      accountType: streamerMeta.accountType || recording?.streamerInfo?.accountType,
      recordedAt: recording?.startTime || task.createdAt,
      durationSeconds,
      manualUpload: true,
    })
    if (result?.success) Message.success('已加入云空间上传队列')
    else Message.error(result?.error || '加入云空间上传队列失败')
  } catch (err: any) {
    Message.error(err?.message || '加入云空间上传队列失败')
  }
}

async function doReAnalyze(task: AnalysisTask) {
  try {
    await analysisApi.reAnalyze(task.id)
    Message.success('已重新提交分析任务')
    fetchList()
  } catch {
    Message.error('重新分析失败')
  }
}

function formatTime(dt: string | null) {
  return dt ? formatDateTime(dt) : '-'
}

onMounted(() => {
  fetchList()
  streamerStore.fetchList()
  streamerStore.fetchAllStreamers()
})
</script>

<template>
  <div class="clip-recap-list">
    <div class="recap-layout">
      <!-- Left: Streamer sidebar -->
      <div class="streamer-sidebar djscard">
        <div class="sidebar-search">
          <input
            v-model="streamerSearch"
            class="djsinput sq"
            placeholder="搜索主播"
            style="width: 100%"
          />
        </div>
        <div class="streamer-list">
          <div
            class="streamer-item"
            :class="{ active: !selectedStreamerId }"
            @click="onStreamerSelect(undefined)"
          >
            全部主播
          </div>
          <div
            v-for="s in filteredStreamers"
            :key="s.id"
            class="streamer-item"
            :class="{ active: selectedStreamerId === s.id }"
            @click="onStreamerSelect(s.id)"
          >
            {{ s.anchorName }}
          </div>
        </div>
      </div>

      <!-- Right: Main content -->
      <div class="main-content">
        <div class="djstoolbar">
          <select v-model="categoryFilter" class="djsselect" @change="onCategoryChange">
            <option v-for="cat in CLIP_CATEGORIES" :key="cat.value" :value="cat.value">
              {{ cat.label }}
            </option>
          </select>
          <div class="spacer"></div>
          <a class="djslink sm" style="font-size:12px" @click="fetchList">刷新</a>
        </div>

        <!-- Comparison draft hint -->
        <div v-if="comparisonDraft" style="padding:8px 16px;background:#FFF7E6;border-bottom:1px solid var(--line);display:flex;align-items:center;gap:8px;font-size:12px">
          <span style="color:#ff7d00">已选择基准切片，请点击另一条切片的「选为参照」完成对比</span>
          <a class="djslink sm" style="color:var(--text-3)" @click="cancelCompare">取消</a>
        </div>

        <div class="djscard">
          <table class="djstbl">
            <thead>
              <tr>
                <th style="width: 40px">
                  <input
                    type="checkbox"
                    :checked="isAllChecked"
                    :indeterminate.prop="isPartialChecked"
                    @change="toggleSelectAll"
                  />
                </th>
                <th>切片名称</th>
                <th style="width: 120px">切片分类</th>
                <th style="width: 100px">分析状态</th>
                <th style="width: 160px">创建时间</th>
                <th style="width: 80px">字数</th>
                <th style="width: 150px">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="7">
                  <div class="djsempty"><div class="ti">加载中...</div></div>
                </td>
              </tr>
              <tr v-else-if="list.length === 0">
                <td colspan="7">
                  <div class="djsempty">
                    <div class="ic">📋</div>
                    <div class="ti">暂无切片数据</div>
                    <div class="desc">在整场复盘详情中点击「切片」按钮创建</div>
                  </div>
                </td>
              </tr>
              <tr v-for="task in list" :key="task.id">
                <td>
                  <input
                    type="checkbox"
                    :checked="selectedIds.includes(task.id)"
                    @change="toggleSelect(task.id)"
                  />
                </td>
                <td>{{ task.clipFilename || `切片_${task.id}` }}</td>
                <td>
                  <span class="djskw">{{ getCategoryLabel(task.clipCategory ?? null) }}</span>
                </td>
                <td>
                  <span :style="{ fontSize: '12px', fontWeight: 500, color: getStatusColor(task.status) }">
                    {{ getStatusLabel(task.status) }}
                  </span>
                </td>
                <td style="font-size: 12px; color: var(--text-2b)">{{ formatTime(task.createdAt) }}</td>
                <td style="font-size: 12px">{{ task.asrWordCount ?? 0 }}</td>
                <td>
                  <div class="ops-cell">
                    <span class="djslink" @click="goToDetail(task)">查看分析</span>
                    <span class="djslink" @click="addToCompare(task)">
                      {{ comparisonDraft ? (comparisonDraft.firstTaskId === task.id ? '已选为基准' : '选为参照') : '加对比' }}
                    </span>
                    <span class="djslink" @click="openVideoFolder(task)">视频文件夹</span>
                    <span class="djslink" style="color:var(--text-3)" @click="toggleMoreMenu(task.id, $event)">更多 ▾</span>
                    <Teleport to="body">
                    <div v-if="moreMenuId === task.id" class="more-menu-overlay" @click="closeMoreMenu">
                      <div class="more-menu" :style="moreMenuStyle" @click.stop>
                        <div class="more-item" @click="() => { closeMoreMenu(); goToAssistant(task) }">AI助手</div>
                        <div class="more-item" @click="() => { closeMoreMenu(); openOriginalVideo(task) }">原视频</div>
                        <div class="more-item" @click="() => { closeMoreMenu(); doReAnalyze(task) }">再分析</div>
                        <div class="more-item" @click="() => { closeMoreMenu(); uploadClipRecapToCloud(task) }">上传云端</div>
                        <div class="more-item danger" @click="() => { closeMoreMenu(); deleteTargetIds = [task.id]; deleteDialogVisible = true }">删除</div>
                      </div>
                    </div>
                    </Teleport>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination -->
        <Pagination :page="page" :size="pageSize" :total="total" @change="onPageChange" />
      </div>
    </div>
  </div>

  <!-- Delete dialog -->
  <ConfirmDialog
    :visible="deleteDialogVisible"
    title="确认删除"
    content="确定要删除选中的切片记录吗？删除后无法恢复。"
    @confirm="confirmDelete"
    @cancel="deleteDialogVisible = false"
  />
</template>

<style scoped>
.clip-recap-list { height: 100% }
.recap-layout { display: flex; height: 100%; gap: 16px }
.streamer-sidebar { width: 220px; min-width: 220px; padding: 12px; display: flex; flex-direction: column; gap: 0 }
.sidebar-search { margin-bottom: 12px }
.streamer-list { flex: 1; overflow-y: auto }
.streamer-item { padding: 8px 12px; border-radius: 6px; cursor: pointer; font-size: 13px; color: var(--text-2) }
.streamer-item:hover { background: var(--line-2) }
.streamer-item.active { background: var(--brand); color: #fff; font-weight: 500 }
.main-content { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 0; overflow: hidden }
.main-content > .djscard { flex: 1; overflow: auto }
.desc { font-size: 12px; color: var(--text-3); margin-top: 4px }
.ops-cell { display:flex; align-items:center; gap:8px; flex-wrap:wrap; position:relative }
</style>
