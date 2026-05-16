<template>
  <div class="file-analysis">
    <!-- Tabs -->
    <div class="djstabs">
      <div class="djstab" :class="{ on: activeTab === 'video' }" @click="activeTab = 'video'">视频分析</div>
      <div class="djstab" :class="{ on: activeTab === 'copywriting' }" @click="activeTab = 'copywriting'">文案预审</div>
      <div class="djstab" :class="{ on: activeTab === 'clip' }" @click="activeTab = 'clip'">切片复盘</div>
      <div class="djstab" :class="{ on: activeTab === 'short-clip' }" @click="activeTab = 'short-clip'">短视频切片</div>
    </div>

    <!-- Toolbar -->
    <div class="djstoolbar" style="margin:16px 20px 0">
      <input
        class="djsinput"
        :value="searchKeyword"
        @input="(e) => { searchKeyword = (e.target as HTMLInputElement).value }"
        placeholder="搜索文件名..."
        style="width:220px"
        @keyup.enter="loadList"
      />
      <select
        class="djsselect"
        :value="filterStatus"
        @change="(e) => { filterStatus = (e.target as HTMLSelectElement).value || undefined; loadList() }"
      >
        <option value="">全部状态</option>
        <option value="pending">待处理</option>
        <option value="asr_processing">ASR处理中</option>
        <option value="ai_processing">AI分析中</option>
        <option value="completed">已完成</option>
        <option value="failed">失败</option>
      </select>
      <div class="spacer"></div>
      <UploadPanel bucket="files" @uploaded="handleUploaded" />
    </div>

    <!-- Table -->
    <div class="table-wrap" style="padding:16px 20px 20px">
      <table class="djstbl">
        <thead>
          <tr>
            <th style="width:36px">
              <input
                type="checkbox"
                :checked="isAllChecked"
                :indeterminate.prop="isPartialChecked"
                @change="toggleSelectAll"
              />
            </th>
            <th>文件名称</th>
            <th>文件大小</th>
            <th>视频时长</th>
            <th>话术</th>
            <th>上传时间</th>
            <th>分析时间</th>
            <th>分析状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="store.loading">
            <td colspan="9" style="text-align:center;padding:40px 0;color:var(--text-3)">加载中...</td>
          </tr>
          <tr v-else-if="!store.list.length">
            <td colspan="9">
              <div class="djsempty">
                <div class="ic">🎬</div>
                <div class="ti">暂无文件分析任务</div>
                <div>上传视频文件开始 AI 分析</div>
              </div>
            </td>
          </tr>
          <tr v-for="record in store.list" :key="record.id" v-else>
            <td>
              <input
                type="checkbox"
                :checked="selectedKeys.includes(record.id)"
                @change="(e: any) => {
                  if (e.target.checked) { if (!selectedKeys.includes(record.id)) selectedKeys.push(record.id) }
                  else { const i = selectedKeys.indexOf(record.id); if (i >= 0) selectedKeys.splice(i, 1) }
                }"
              />
            </td>
            <td>
              <div style="display:flex;align-items:center;gap:10px">
                <div class="file-icon">🎬</div>
                <span style="font-size:13px;color:var(--text-1);font-weight:500">{{ record.fileName }}</span>
              </div>
            </td>
            <td>{{ formatSize(record.fileSize) }}</td>
            <td>{{ record.duration ? formatDuration(record.duration) : '-' }}</td>
            <td>{{ record.aiModel || '-' }}</td>
            <td>{{ formatDateTime(record.createdAt) }}</td>
            <td>{{ formatDateTime((record as any).updatedAt) }}</td>
            <td>
              <span v-if="record.status === 'pending'" class="djsbadge peer">待处理</span>
              <span v-else-if="record.status === 'asr_processing'" class="djsbadge" style="background:#FFF1E1;color:#E07B00">ASR处理中</span>
              <span v-else-if="record.status === 'ai_processing'" class="djsbadge" style="background:#FFF1E1;color:#E07B00">AI分析中</span>
              <span v-else-if="record.status === 'completed'" class="djsbadge live">已完成</span>
              <span v-else-if="record.status === 'failed'" class="djsbadge own">失败</span>
            </td>
            <td>
              <div style="display:flex;align-items:center;gap:8px;justify-content:flex-end">
                <span
                  v-if="record.status === 'completed'"
                  class="djslink"
                  @click="goDetail(record.id)"
                >查看</span>
                <span
                  class="djslink"
                  style="color:var(--red)"
                  @click="handleDelete(record.id)"
                >删除</span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <Pagination :page="currentPage" :size="20" :total="store.total" @change="onPageChange" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useFileAnalysisStore } from '@/stores/fileAnalysis'
import UploadPanel from '@/components/upload/UploadPanel.vue'
import Pagination from '@/components/common/Pagination.vue'
import { formatDateTime } from '@/utils/format'

const store = useFileAnalysisStore()
const router = useRouter()
const route = useRoute()
const filterStatus = ref<string | undefined>()
const searchKeyword = ref('')
const activeTab = ref((route.query.tab as string) || 'video')
const currentPage = ref(1)
const selectedKeys = ref<number[]>([])

const isAllChecked = computed(() => {
  const items = store.list || []
  return items.length > 0 && items.every((r: any) => selectedKeys.value.includes(r.id))
})
const isPartialChecked = computed(() => {
  const items = store.list || []
  const checked = items.filter((r: any) => selectedKeys.value.includes(r.id)).length
  return checked > 0 && checked < items.length
})
function toggleSelectAll(e: Event) {
  const checked = (e.target as HTMLInputElement).checked
  const items = store.list || []
  if (checked) {
    const merged = new Set<number>(selectedKeys.value)
    items.forEach((r: any) => merged.add(r.id))
    selectedKeys.value = Array.from(merged)
  } else {
    const pageIds = new Set(items.map((r: any) => r.id))
    selectedKeys.value = selectedKeys.value.filter((id) => !pageIds.has(id))
  }
}

watch(() => route.query.tab, (tab) => {
  if (tab && typeof tab === 'string') activeTab.value = tab
})

onMounted(() => {
  loadList()
})

function loadList() {
  currentPage.value = 1
  store.fetchList(1, 20, filterStatus.value, searchKeyword.value || undefined)
}

function onPageChange(page: number) {
  currentPage.value = page
  store.fetchList(page, 20, filterStatus.value, searchKeyword.value || undefined)
}

function handleUploaded(storageKey: string, fileName: string) {
  store.create({ fileName, storageKey })
  setTimeout(() => loadList(), 1000)
}

async function handleDelete(id: number) {
  const ok = await store.remove(id)
  if (ok) loadList()
}

function goDetail(id: number) {
  router.push({ name: 'FileAnalysisDetail', params: { id } })
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
</script>

<style scoped>
.file-analysis {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.table-wrap {
  flex: 1;
  overflow: auto;
  padding: 0 0 20px;
}

.file-icon {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: var(--bg);
  display: grid;
  place-items: center;
  font-size: 16px;
  flex-shrink: 0;
}

.spacer {
  flex: 1;
}
</style>
