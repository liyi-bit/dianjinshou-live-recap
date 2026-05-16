<template>
  <div class="short-clip-page">
    <div class="page-header">
      <h2 class="page-title">短视频切片</h2>
      <span class="page-desc">从录制视频中截取精彩片段，支持自定义时间、水印和分辨率</span>
    </div>

    <!-- Create clip form -->
    <div class="djscard clip-form-card">
      <div class="djscard-title">创建切片</div>
      <form class="clip-form" @submit.prevent="handleCreate">
        <div class="form-fields">
          <label class="field-group">
            <span class="field-label">录制ID <span class="req">*</span></span>
            <input v-model.number="form.recordingId" type="number" min="1" placeholder="录制ID" class="djsinput sq" style="width: 120px" required />
          </label>
          <label class="field-group">
            <span class="field-label">开始时间(秒) <span class="req">*</span></span>
            <input v-model.number="form.startTime" type="number" min="0" placeholder="起始秒" class="djsinput sq" style="width: 120px" required />
          </label>
          <label class="field-group">
            <span class="field-label">结束时间(秒) <span class="req">*</span></span>
            <input v-model.number="form.endTime" type="number" min="0" placeholder="结束秒" class="djsinput sq" style="width: 120px" required />
          </label>
          <label class="field-group">
            <span class="field-label">切片名称 <span class="req">*</span></span>
            <input v-model="form.clipName" type="text" maxlength="15" placeholder="最多15字" class="djsinput sq" style="width: 160px" required />
          </label>
          <label class="field-group">
            <span class="field-label">分辨率</span>
            <select v-model="form.resolution" class="djsselect">
              <option value="original">原始</option>
              <option value="1080p">1080p</option>
              <option value="720p">720p</option>
            </select>
          </label>
          <label class="field-group">
            <span class="field-label">水印</span>
            <input v-model="form.watermarkText" type="text" placeholder="可选" class="djsinput sq" style="width: 140px" />
          </label>
          <button type="submit" class="djsbtn primary" :disabled="creating">
            <span v-if="creating" class="spin-dot"></span>
            开始切片
          </button>
        </div>
      </form>
    </div>

    <!-- Filter & batch actions -->
    <div class="djstoolbar">
      <select v-model="filterStatus" class="djsselect" @change="loadList">
        <option value="">状态筛选</option>
        <option value="pending">待处理</option>
        <option value="processing">处理中</option>
        <option value="completed">已完成</option>
        <option value="exported">已导出</option>
        <option value="failed">失败</option>
      </select>
      <input
        v-model.number="filterRecordingId"
        type="number"
        min="1"
        placeholder="按录制ID筛选"
        class="djsinput sq"
        style="width: 160px"
        @input="loadList"
      />
      <div class="spacer"></div>
      <button
        class="djsbtn"
        :disabled="store.selectedIds.length === 0"
        @click="handleBatchExport"
      >
        批量导出 ({{ store.selectedIds.length }})
      </button>
    </div>

    <!-- Clip list -->
    <div class="djscard">
      <table class="djstbl">
        <thead>
          <tr>
            <th>切片名称</th>
            <th style="width: 80px">时长</th>
            <th style="width: 90px">分辨率</th>
            <th style="width: 100px">大小</th>
            <th style="width: 100px">状态</th>
            <th style="width: 170px">创建时间</th>
            <th style="width: 200px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="store.loading">
            <td colspan="7">
              <div class="djsempty"><div class="ti">加载中...</div></div>
            </td>
          </tr>
          <tr v-else-if="store.list.length === 0">
            <td colspan="7">
              <div class="djsempty">
                <div class="ic">🎬</div>
                <div class="ti">暂无切片记录</div>
              </div>
            </td>
          </tr>
          <tr v-for="record in store.list" :key="record.id">
            <td style="max-width: 160px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap">{{ record.clipName }}</td>
            <td>{{ formatDuration(record.duration) }}</td>
            <td>{{ record.resolution }}</td>
            <td>{{ formatSize(record.fileSize) }}</td>
            <td>
              <span class="djskw" :class="statusClass(record.status)">{{ statusLabel(record.status) }}</span>
            </td>
            <td>{{ formatDateTime(record.createdAt) }}</td>
            <td>
              <span
                v-if="record.status === 'completed' && !record.storageKey"
                class="djslink"
                style="margin-right: 8px"
                @click="handleUploadCloud(record.id)"
              >上传云端</span>
              <span v-if="record.storageKey" class="djskw green" style="margin-right: 8px">已上云</span>
              <span class="djslink" style="color: var(--red)" @click="handleDelete(record.id)">删除</span>
            </td>
          </tr>
        </tbody>
      </table>

      <Pagination :page="store.currentPage" :size="20" :total="store.total" @change="onPageChange" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useShortClipStore } from '@/stores/shortClip'
import Pagination from '@/components/common/Pagination.vue'
import { formatDateTime } from '@/utils/format'

const store = useShortClipStore()
const filterStatus = ref<string>('')
const filterRecordingId = ref<number | undefined>()
const creating = ref(false)

const form = reactive({
  recordingId: undefined as number | undefined,
  startTime: 0,
  endTime: 60,
  clipName: '',
  resolution: 'original',
  watermarkText: ''
})

onMounted(() => {
  loadList()
})

function loadList() {
  store.fetchList(1, 20, filterRecordingId.value, filterStatus.value || undefined)
}

function onPageChange(page: number) {
  store.fetchList(page, 20, filterRecordingId.value, filterStatus.value || undefined)
}

function onSelectionChange(rowKeys: (string | number)[]) {
  store.selectedIds = rowKeys as number[]
}

async function handleCreate() {
  if (!form.recordingId || !form.clipName) return
  creating.value = true
  const result = await store.create({
    recordingId: form.recordingId,
    startTime: form.startTime,
    endTime: form.endTime,
    clipName: form.clipName,
    resolution: form.resolution,
    watermarkText: form.watermarkText || undefined
  })
  creating.value = false
  if (result) {
    form.clipName = ''
    loadList()
  }
}

async function handleDelete(id: number) {
  const ok = await store.remove(id)
  if (ok) loadList()
}

async function handleBatchExport() {
  await store.batchExport()
}

async function handleUploadCloud(id: number) {
  const ok = await store.uploadToCloud(id)
  if (ok) loadList()
}

function formatSize(bytes: number): string {
  if (!bytes) return '-'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}

function formatDuration(seconds: number): string {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${s.toString().padStart(2, '0')}`
}

function statusLabel(status: string): string {
  const map: Record<string, string> = {
    pending: '待处理',
    processing: '处理中',
    completed: '已完成',
    exported: '已导出',
    failed: '失败'
  }
  return map[status] || status
}

function statusClass(status: string): string {
  const map: Record<string, string> = {
    pending: 'blue',
    processing: '',
    completed: 'green',
    exported: '',
    failed: 'hot'
  }
  return map[status] || ''
}
</script>

<style scoped>
.short-clip-page {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-1);
}

.page-desc {
  color: var(--text-3);
  font-size: 13px;
}

.clip-form-card {
  padding: 0;
}

.clip-form {
  padding: 16px 22px 20px;
}

.form-fields {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: flex-end;
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: 12px;
  color: var(--text-2);
  font-weight: 500;
}

.req {
  color: var(--red);
}

.spin-dot {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255,255,255,.6);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  display: inline-block;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
