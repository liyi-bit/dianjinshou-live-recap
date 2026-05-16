<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { useComparisonStore } from '@/stores/comparison'
import ComparisonLockedHeader from '@/components/comparison/ComparisonLockedHeader.vue'
import Pagination from '@/components/common/Pagination.vue'
import { formatDateTime } from '@/utils/format'
import { openNativeDatePicker } from '@/utils/nativeControls'
import { ensureComparisonSourceVideosUploaded } from '@/utils/cloudComparisonUpload'
import { checkCloudUploadDuplicate } from '@/utils/cloudUploadStatus'

const router = useRouter()
const store = useComparisonStore()

const page = ref(1)
const selectedKeys = ref<number[]>([])
const isAllChecked = computed(() => (store.list?.length ?? 0) > 0 && store.list.every((r: any) => selectedKeys.value.includes(r.id)))
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
const filterStatus = ref('')
const filterStartDate = ref('')
const filterEndDate = ref('')

const CLIP_COLORS: Record<string, string> = {
  '开场话术': '#4A6896', '逼单话术': '#ff7d00', '促单话术': '#0fc6c2',
  '互动话术': '#722ed1', '憋单话术': '#eb0aa4', '过款话术': '#8693A4',
  '福利话术': '#00b42a', '塑品话术': '#f53f3f', '售后话术': '#AEB9C8',
  '人设话术': '#3491fa', '低俗话术': '#ff5722', '客服话术': '#fadc19',
  '违禁词话术': '#f53f3f', '其他': '#AEB9C8'
}

const columns = [
  { title: '对比1（优化场次）', dataIndex: 'recordingIdOptimize' },
  { title: '对比2（参考场次）', dataIndex: 'recordingIdReference' },
  { title: '切片类型', dataIndex: 'clipCategory', slotName: 'clipCategory' },
  { title: '对比时间', dataIndex: 'createdAt' },
  { title: '状态', dataIndex: 'status', slotName: 'status' },
  { title: '操作', slotName: 'actions', width: 320 }
]

function fetchData() {
  store.fetchList('clip', page.value, 10, {
    status: filterStatus.value || undefined,
    startDate: filterStartDate.value || undefined,
    endDate: filterEndDate.value || undefined,
  })
}

function onPageChange(p: number) {
  page.value = p
  fetchData()
}

function viewDetail(id: number) {
  router.push({ name: 'ComparisonDetail', params: { id } })
}

function goToAssistant(id: number) {
  router.push({ name: 'AssistantOperation', query: { comparisonId: String(id) } })
}

async function uploadComparisonToCloud(record: any) {
  const api = (window as any).electronAPI
  if (!api?.enqueueGeneratedCloudUpload) {
    Message.warning('当前环境不支持云空间上传')
    return
  }
  const optimizeName = record.clipRemarkOptimize || record.clipFilenameOptimize || record.anchorNameOptimize || `录制 #${record.recordingIdOptimize}`
  const referenceName = record.clipRemarkReference || record.clipFilenameReference || record.anchorNameReference || `录制 #${record.recordingIdReference}`
  try {
    const duplicate = await checkCloudUploadDuplicate({
      electronApi: api,
      businessType: 'clip_comparison',
      businessId: record.id,
      comparisonId: record.id,
    })
    if (duplicate.duplicate) {
      Message.warning(duplicate.message || '该对比复盘已在云空间，无需重复上传')
      return
    }
    const sourceResult = await ensureComparisonSourceVideosUploaded(record.id, api)
    if (!sourceResult.success) {
      Message.error(sourceResult.error || '原视频上传检查失败')
      return
    }
    const result = await api.enqueueGeneratedCloudUpload({
      id: `clip_comparison_${record.id}`,
      fileName: `切片对比_${record.id}.json`,
      contentType: 'application/json',
      businessType: 'clip_comparison',
      businessId: record.id,
      comparisonId: record.id,
      recordingId: record.recordingIdOptimize,
      clipId: record.taskIdOptimize,
      anchorName: referenceName,
      recordedAt: record.createdAt,
      manualUpload: true,
      content: {
        type: 'clip',
        comparisonId: record.id,
        optimizeName,
        referenceName,
        recordingIdOptimize: record.recordingIdOptimize,
        recordingIdReference: record.recordingIdReference,
        taskIdOptimize: record.taskIdOptimize,
        taskIdReference: record.taskIdReference,
        clipCategory: record.clipCategory,
        status: record.status,
        aiModel: record.aiModel,
        aiComparisonResult: record.aiComparisonResult,
        createdAt: record.createdAt,
      },
    })
    if (result?.success) {
      Message.success(sourceResult.queued > 0
        ? `已检查原视频，${sourceResult.queued} 个缺失视频已加入上传队列，对比复盘已加入云空间`
        : '两个原视频已在云空间，对比复盘已加入上传队列')
    }
    else Message.error(result?.error || '加入云空间上传队列失败')
  } catch (err: any) {
    Message.error(err?.message || '加入云空间上传队列失败')
  }
}

async function doSwap(id: number) {
  const ok = await store.swap(id)
  if (ok) fetchData()
}

async function onBatchDelete() {
  if (selectedKeys.value.length === 0) return
  const ok = await store.batchDelete(selectedKeys.value)
  if (ok) {
    selectedKeys.value = []
    fetchData()
  }
}

function onAddComparison() {
  Message.info('请先在"AI切片复盘"列表中选择两条切片记录进行对比')
}

onMounted(() => {
  store.fetchDraft()
  fetchData()
})
</script>

<template>
  <div class="comparison-list">
    <ComparisonLockedHeader />

    <!-- Toolbar -->
    <div class="djstoolbar">
      <select v-model="filterStatus" class="djsselect" @change="page = 1; fetchData()">
        <option value="">全部</option>
        <option value="completed">已完成</option>
        <option value="pending">处理中</option>
        <option value="failed">失败</option>
      </select>
      <span style="font-size:12px;color:#8693A4">对比时间</span>
      <input v-model="filterStartDate" type="date" class="djsinput" style="font-size:12px;width:130px" @click="openNativeDatePicker" @change="page = 1; fetchData()" />
      <span style="font-size:12px;color:#8693A4">~</span>
      <input v-model="filterEndDate" type="date" class="djsinput" style="font-size:12px;width:130px" @click="openNativeDatePicker" @change="page = 1; fetchData()" />
      <button class="djsbtn primary sm" @click="page = 1; fetchData()">查找</button>
      <button
        v-if="selectedKeys.length > 0"
        class="djsbtn danger sm"
        @click="onBatchDelete"
      >
        批量删除 ({{ selectedKeys.length }})
      </button>
      <div style="flex:1"></div>
      <a class="djslink sm" @click="fetchData">刷新</a>
    </div>

    <!-- Table -->
    <div class="table-wrap">
      <table class="djstbl">
        <thead>
          <tr>
            <th style="width:40px">
              <input
                type="checkbox"
                :checked="isAllChecked"
                :indeterminate.prop="isPartialChecked"
                @change="toggleSelectAll"
              />
            </th>
            <th>对比1（优化场次）</th>
            <th>对比2（参考场次）</th>
            <th>切片类型</th>
            <th>对比时间</th>
            <th style="width:320px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="store.loading">
            <td colspan="6" style="text-align:center;padding:32px">加载中...</td>
          </tr>
          <tr v-else-if="!store.list || store.list.length === 0">
            <td colspan="6" style="text-align:center;padding:32px;color:#8693A4">暂无数据</td>
          </tr>
          <tr v-for="record in store.list" :key="record.id">
            <td>
              <input
                type="checkbox"
                :checked="selectedKeys.includes(record.id)"
                @change="(e: Event) => {
                  const checked = (e.target as HTMLInputElement).checked
                  if (checked) selectedKeys = [...selectedKeys, record.id]
                  else selectedKeys = selectedKeys.filter(k => k !== record.id)
                }"
              />
            </td>
            <td>
              <div class="streamer-cell">
                <span v-if="record.anchorAvatarOptimize" class="djsav">
                  <img :src="record.anchorAvatarOptimize" alt="" />
                </span>
                <span v-else class="djsav djsav-initial">{{ (record.anchorNameOptimize || '?')[0] }}</span>
                <div class="streamer-info">
                  <div class="streamer-name">{{ record.anchorNameOptimize || '录制 #' + record.recordingIdOptimize }}</div>
                  <div class="clip-name" v-if="record.clipRemarkOptimize || record.clipFilenameOptimize">
                    {{ record.clipRemarkOptimize || record.clipFilenameOptimize }}
                  </div>
                  <div class="streamer-time">{{ formatDateTime(record.createdAt) }}</div>
                </div>
              </div>
            </td>
            <td>
              <div class="streamer-cell">
                <span v-if="record.anchorAvatarReference" class="djsav">
                  <img :src="record.anchorAvatarReference" alt="" />
                </span>
                <span v-else class="djsav djsav-initial">{{ (record.anchorNameReference || '?')[0] }}</span>
                <div class="streamer-info">
                  <div class="streamer-name">{{ record.anchorNameReference || '录制 #' + record.recordingIdReference }}</div>
                  <div class="clip-name" v-if="record.clipRemarkReference || record.clipFilenameReference">
                    {{ record.clipRemarkReference || record.clipFilenameReference }}
                  </div>
                  <div class="streamer-time">{{ formatDateTime(record.createdAt) }}</div>
                </div>
              </div>
            </td>
            <td>
              <span
                v-if="record.clipCategory"
                class="djsbadge"
                :style="{ background: CLIP_COLORS[record.clipCategory] || '#8693A4', color: '#fff' }"
              >
                {{ record.clipCategory }}
              </span>
              <span v-else style="color:#8693A4">-</span>
            </td>
            <td>{{ formatDateTime(record.createdAt) }}</td>
            <td>
              <div class="action-cell">
                <button class="djsbtn primary sm" @click="viewDetail(record.id)">查看智能对比</button>
                <a class="djslink" href="#" @click.prevent="goToAssistant(record.id)">AI助手</a>
                <a class="djslink" href="#" @click.prevent="doSwap(record.id)">变换定位</a>
                <a class="djslink" href="#" @click.prevent="uploadComparisonToCloud(record)">上传云端</a>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <Pagination :page="page" :size="10" :total="store.total" @change="onPageChange" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.comparison-list {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.table-wrap {
  flex: 1;
  overflow: auto;
}

.streamer-cell {
  display: flex;
  align-items: center;
  gap: 8px;

  .djsav {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    flex-shrink: 0;
    overflow: hidden;
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    &.djsav-initial {
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--brand);
      color: #fff;
      font-size: 14px;
      font-weight: 500;
    }
  }

  .streamer-info {
    .streamer-name {
      font-size: 13px;
      font-weight: 500;
      color: var(--text-1);
    }
    .clip-name {
      font-size: 12px;
      color: var(--brand);
      margin-top: 2px;
      max-width: 160px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .streamer-time {
      font-size: 12px;
      color: #8693A4;
      margin-top: 2px;
    }
  }
}

.action-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

</style>
