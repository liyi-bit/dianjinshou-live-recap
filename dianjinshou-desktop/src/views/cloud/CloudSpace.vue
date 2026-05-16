<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Message, Modal } from '@arco-design/web-vue'
import { useCloudSpaceStore } from '@/stores/cloudSpace'
import { useStreamerStore } from '@/stores/streamer'
import type { CloudCompareMode, CloudFileItem, CloudView } from '@/api/cloudSpace'
import * as cloudApi from '@/api/cloudSpace'
import Pagination from '@/components/common/Pagination.vue'
import AddCloudComparisonDrawer from './AddCloudComparisonDrawer.vue'
import ConfirmCloudComparisonDialog from './ConfirmCloudComparisonDialog.vue'
import { openNativeDatePicker } from '@/utils/nativeControls'
import { formatDateTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const store = useCloudSpaceStore()
const streamerStore = useStreamerStore()

const view = ref<CloudView>(normalizeView(route.query.tab))
const compareMode = ref<CloudCompareMode>('full')
const page = ref(1)
const size = ref(10)
const drawerVisible = ref(false)
const confirmVisible = ref(false)
const selectedForCompare = ref<CloudFileItem[]>([])
const editingId = ref<number | null>(null)
const editingName = ref('')

const filters = reactive({
  keyword: '',
  anchorName: '',
  uploadAccount: '',
  accountType: '',
  startTime: '',
  endTime: '',
})

const title = computed(() => {
  if (view.value === 'full') return '全场复盘'
  if (view.value === 'clip') return '切片复盘'
  return '对比复盘'
})

const usageText = computed(() => {
  const usage = store.usage
  if (!usage) return '0 / 20 GB'
  return `${formatGb(usage.usedBytes)} / ${formatGb(usage.totalQuotaBytes)} GB`
})

watch(() => route.query.tab, (tab) => {
  view.value = normalizeView(tab)
  page.value = 1
  loadAll()
})

watch(compareMode, () => {
  if (view.value === 'comparison') {
    page.value = 1
    loadList()
  }
})

function normalizeView(tab: unknown): CloudView {
  if (tab === 'clip') return 'clip'
  if (tab === 'comparison') return 'comparison'
  return 'full'
}

function loadList() {
  return store.fetchList(view.value, compareMode.value, {
    page: page.value,
    size: size.value,
    keyword: filters.keyword || undefined,
    anchorName: filters.anchorName || undefined,
    uploadAccount: filters.uploadAccount || undefined,
    accountType: view.value === 'clip' ? filters.accountType || undefined : undefined,
    startTime: filters.startTime || undefined,
    endTime: filters.endTime || undefined,
  })
}

function loadAll() {
  return Promise.allSettled([loadList(), store.fetchUsage()])
}

function search() {
  page.value = 1
  loadList()
}

function setPage(next: number) {
  page.value = next
  loadList()
}

function displayName(item: CloudFileItem) {
  return item.displayName || item.fileName || '未命名文件'
}

function streamerInitial(name?: string | null) {
  return (name || '?').trim().charAt(0) || '?'
}

function textMatchAvatar(...texts: Array<string | null | undefined>) {
  const candidates = texts
    .map((text) => text?.trim())
    .filter((text): text is string => !!text)
  const streamer = streamerStore.allStreamers.find((s) => {
    const name = s.anchorName?.trim()
    if (!name || !s.anchorAvatar) return false
    return candidates.some((text) => text === name || (name.length >= 2 && text.includes(name)))
  })
  return streamer?.anchorAvatar || null
}

function avatarFor(item: CloudFileItem) {
  if (item.anchorAvatar) return item.anchorAvatar
  if (item.anchorAvatarOptimize) return item.anchorAvatarOptimize
  const streamer = streamerStore.allStreamers.find((s) =>
    item.streamerId != null && String(s.id) === String(item.streamerId)
  )
  return streamer?.anchorAvatar || textMatchAvatar(item.anchorName, item.displayName, item.fileName)
}

function avatarForName(name?: string | null) {
  if (!name) return null
  const normalized = name.trim()
  return streamerStore.allStreamers.find((s) => s.anchorName?.trim() === normalized)?.anchorAvatar || textMatchAvatar(normalized)
}

function avatarForCompareFirst(item: CloudFileItem) {
  return item.anchorAvatarOptimize || avatarFor(item)
}

function avatarForCompareSecond(item: CloudFileItem) {
  return item.anchorAvatarReference || textMatchAvatar(item.anchorNameReference, item.anchorName, item.displayName, item.fileName)
}

function isPending(item: CloudFileItem) {
  return item.status === 'queued' || item.status === 'uploading' || item.status === 'failed'
}

function formatSize(bytes?: number | null) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let value = bytes
  let idx = 0
  while (value >= 1024 && idx < units.length - 1) {
    value /= 1024
    idx++
  }
  return `${value.toFixed(idx === 0 ? 0 : 1)} ${units[idx]}`
}

function formatGb(bytes?: number | null) {
  return ((bytes || 0) / 1024 / 1024 / 1024).toFixed(2).replace(/\.00$/, '')
}

function formatDuration(seconds?: number | null) {
  if (!seconds) return '-'
  const min = Math.floor(seconds / 60)
  const sec = seconds % 60
  return `${min}分${String(sec).padStart(2, '0')}秒`
}

function formatCloudTime(item: CloudFileItem) {
  return item.recordedAt || item.createdAt ? formatDateTime(item.recordedAt || item.createdAt) : '-'
}

function beginRename(item: CloudFileItem) {
  if (isPending(item)) return
  editingId.value = item.id
  editingName.value = displayName(item)
}

async function commitRename(item: CloudFileItem) {
  const name = editingName.value.trim()
  const original = displayName(item)
  editingId.value = null
  if (!name || name === original) return
  try {
    await store.renameFile(item.id, name)
    Message.success('文件名已更新')
    loadList()
  } catch (e: any) {
    Message.error(e?.message || '重命名失败，请重试')
  }
}

function confirmDelete(item: CloudFileItem) {
  Modal.confirm({
    title: '删除云端文件',
    content: '仅删除云端，不影响本地数据',
    okText: '删除',
    cancelText: '取消',
    async onOk() {
      await store.deleteFile(item.id)
      Message.success('已删除云端文件')
      loadAll()
    },
  })
}

async function download(item: CloudFileItem) {
  const signed = await store.signedUrl(item.id)
  if (signed?.url) window.open(signed.url, '_blank')
}

async function openItem(item: CloudFileItem) {
  const target = await store.openTarget(item.id)
  const params = target?.params || {}
  // 0 / null / undefined / 空串 都视为无效 ID，避免拼成 /recap/0 这类无意义路由
  const isValidId = (v: unknown) => {
    if (v == null) return false
    const n = Number(v)
    return Number.isFinite(n) && n > 0
  }
  if (target?.target === 'local') {
    if (isValidId(params.comparisonId)) {
      router.push({ name: 'ComparisonDetail', params: { id: String(params.comparisonId) } })
      return
    }
    const recapId = isValidId(params.businessId) ? params.businessId : (isValidId(params.recordingId) ? params.recordingId : null)
    if (recapId != null) {
      router.push({ name: 'RecapDetail', params: { id: String(recapId) } })
      return
    }
    Message.warning('该云文件未关联可打开的源记录')
    return
  }
  router.push({ name: 'CloudReadonlyDetail', params: { id: String(item.id) } })
}

function onSelectedTwo(items: CloudFileItem[]) {
  selectedForCompare.value = items
  confirmVisible.value = true
}

async function startComparison() {
  if (selectedForCompare.value.length !== 2) return
  await cloudApi.createCloudComparison(compareMode.value, selectedForCompare.value.map((item) => item.id))
  Message.success('对比任务已创建')
  confirmVisible.value = false
  drawerVisible.value = false
  selectedForCompare.value = []
  loadList()
}

onMounted(() => {
  loadAll()
  streamerStore.fetchAllStreamers()
})
</script>

<template>
  <div class="cloud-page">
    <div class="compare-tabs djstabs" v-if="view === 'comparison'">
      <span class="djstab" :class="{ on: compareMode === 'full' }" @click="compareMode = 'full'">全场复盘</span>
      <span class="djstab" :class="{ on: compareMode === 'clip' }" @click="compareMode = 'clip'">切片复盘</span>
    </div>

    <section class="panel djscard">
      <div class="toolbar djstoolbar">
        <div class="filter-strip">
          <span v-if="view === 'clip'" class="lbl">切片类型</span>
          <select v-if="view === 'clip'" v-model="filters.accountType" class="djsselect filter-select">
            <option value="">全部</option>
            <option value="own">自有账号</option>
            <option value="industry">同行账号</option>
            <option value="competitor">竞品账号</option>
          </select>
          <input v-if="view !== 'comparison'" v-model="filters.anchorName" class="djsinput filter-input" placeholder="输入主播名称" @keyup.enter="search" />
          <input v-model="filters.uploadAccount" class="djsinput filter-input" placeholder="输入上传账号" @keyup.enter="search" />
          <span class="lbl">{{ view === 'comparison' ? '对比时间' : '录制时间' }}</span>
          <input v-model="filters.startTime" type="date" class="djsinput date-input" placeholder="开始时间" @click="openNativeDatePicker" />
          <span class="range-mark">~</span>
          <input v-model="filters.endTime" type="date" class="djsinput date-input" placeholder="结束时间" @click="openNativeDatePicker" />
          <button class="djsbtn primary sm" @click="search">查找</button>
          <button v-if="view === 'comparison'" class="djsbtn ghost sm" @click="drawerVisible = true">新增对比分析</button>
        </div>
        <div class="toolbar-side">
          <div class="quota-card">
            <div class="quota-row">
              <span>云空间容量</span>
              <strong>{{ usageText }}</strong>
              <a class="djslink" @click="store.fetchUsage()">刷新</a>
            </div>
            <div class="bar"><span :style="{ width: Math.min(100, store.usage?.usagePercent || 0) + '%' }"></span></div>
          </div>
        </div>
      </div>

      <div class="table-wrap">
        <table class="djstbl cloud-table">
          <thead>
            <tr v-if="view === 'full'">
              <th style="width:42%">文件名（双击修改）</th>
              <th>录制时间</th>
              <th>文件大小</th>
              <th>上传账号</th>
              <th style="width:210px;text-align:right">操作</th>
            </tr>
            <tr v-else-if="view === 'clip'">
              <th style="width:38%">文件名（双击修改）</th>
              <th>切片类型</th>
              <th>录制时间</th>
              <th>文件大小</th>
              <th>上传账号</th>
              <th style="width:210px;text-align:right">操作</th>
            </tr>
            <tr v-else>
              <th>{{ compareMode === 'clip' ? '切片1' : '对比1' }}</th>
              <th>{{ compareMode === 'clip' ? '切片2' : '对比2' }}</th>
              <th>对比时间</th>
              <th>上传账号</th>
              <th style="width:230px;text-align:right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="store.loading"><td :colspan="view === 'clip' ? 6 : 5" class="empty">加载中...</td></tr>
            <tr v-else-if="store.files.length === 0"><td :colspan="view === 'clip' ? 6 : 5" class="empty">暂无{{ title }}数据</td></tr>
            <tr v-for="item in store.files" :key="item.id" :class="{ pending: isPending(item) }">
              <template v-if="view === 'comparison'">
                <td><div class="file compare-file"><div class="djsav g3 avatar-box"><img v-if="avatarForCompareFirst(item)" :src="avatarForCompareFirst(item)!" referrerpolicy="no-referrer" /><template v-else>{{ streamerInitial(displayName(item)) }}</template></div><div><div class="file-name">{{ displayName(item) }}</div><div class="meta"><span>{{ compareMode === 'clip' ? '切片对比' : '整场对比' }}</span></div></div></div></td>
                <td><div class="file compare-file"><div class="djsav g1 avatar-box"><img v-if="avatarForCompareSecond(item)" :src="avatarForCompareSecond(item)!" referrerpolicy="no-referrer" /><template v-else>{{ streamerInitial(item.anchorName || item.anchorNameReference) }}</template></div><div><div class="file-name">{{ item.anchorName || item.anchorNameReference || '-' }}</div><div class="meta"><span>{{ compareMode === 'clip' ? '参考切片' : '参考场次' }}</span></div></div></div></td>
              <td class="mono">{{ formatCloudTime(item) }}</td>
                <td>{{ item.uploadAccount || '-' }}</td>
                <td class="actions-cell">
                  <button v-if="!isPending(item)" class="djsbtn primary sm" @click="openItem(item)">查看对比分析</button>
                  <div v-if="!isPending(item)" class="sub-actions"><a class="djslink danger" @click="confirmDelete(item)">删除</a></div>
                  <div v-else class="progress"><span :style="{ width: (item.uploadProgress || 0) + '%' }"></span></div>
                </td>
              </template>

              <template v-else>
                <td>
                  <div class="file">
                    <div class="djsav avatar-box" :class="view === 'clip' ? 'g1' : 'g3'">
                      <img v-if="avatarFor(item)" :src="avatarFor(item)!" referrerpolicy="no-referrer" />
                      <template v-else>{{ streamerInitial(item.anchorName || displayName(item)) }}</template>
                    </div>
                    <div>
                      <input
                        v-if="editingId === item.id"
                        v-model="editingName"
                        class="rename-input"
                        autofocus
                        @blur="commitRename(item)"
                        @keyup.enter="commitRename(item)"
                      />
                      <div v-else class="file-name" @dblclick="beginRename(item)">{{ displayName(item) }}</div>
                      <div class="meta">
                        <span class="djsbadge" :class="view === 'clip' ? 'blue' : 'amber'">{{ view === 'clip' ? '切片' : '全场' }}</span>
                        <span v-if="isPending(item)" :class="item.status === 'failed' ? 'upload-failed' : ''">
                          {{
                            item.status === 'queued' ? '等待上传'
                              : item.status === 'failed' ? `上传失败：${item.errorMsg || '请重试'}`
                              : `上传中 ${item.uploadProgress || 0}%`
                          }}
                        </span>
                        <span v-else>{{ formatDuration(item.durationSeconds) }}</span>
                      </div>
                    </div>
                  </div>
                </td>
                <td v-if="view === 'clip'"><span class="djsbadge blue">片段</span></td>
              <td class="mono">{{ formatCloudTime(item) }}</td>
                <td>{{ formatSize(item.fileSize) }}</td>
                <td>{{ item.uploadAccount || '-' }}</td>
                <td class="actions-cell">
                  <button v-if="!isPending(item)" class="djsbtn primary sm" @click="openItem(item)">{{ view === 'clip' ? '查看切片' : '查看复盘' }}</button>
                  <div v-if="!isPending(item)" class="sub-actions">
                    <a class="djslink" @click="download(item)">下载</a>
                    <a class="djslink danger" @click="confirmDelete(item)">删除</a>
                  </div>
                  <div v-else class="progress"><span :style="{ width: (item.uploadProgress || 0) + '%' }"></span></div>
                </td>
              </template>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pager">
        <Pagination :page="page" :size="size" :total="store.total" @change="setPage" />
      </div>
    </section>

    <AddCloudComparisonDrawer
      :visible="drawerVisible"
      :mode="compareMode"
      @close="drawerVisible = false"
      @selected-two="onSelectedTwo"
    />
    <ConfirmCloudComparisonDialog
      :visible="confirmVisible"
      :items="selectedForCompare"
      @close="confirmVisible = false"
      @back="confirmVisible = false"
      @start="startComparison"
    />
  </div>
</template>

<style scoped>
.cloud-page{height:100%;display:flex;flex-direction:column;gap:0;background:transparent;min-width:0}
.compare-tabs{height:42px;align-items:flex-end;margin-bottom:14px;flex:0 0 auto}
.panel{flex:1;min-height:0;display:flex;flex-direction:column}
.toolbar{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:14px;align-items:center;min-height:58px;padding:10px 16px}
.filter-strip{display:flex;align-items:center;gap:10px;min-width:0;flex-wrap:wrap}
.filter-select{width:118px}
.filter-input{width:160px}
.date-input{width:128px}
.range-mark{font-size:12px;color:var(--text-3);margin:0 -3px}
.djslink.danger{color:var(--red)}
.toolbar-side{display:flex;align-items:center;justify-content:flex-end;min-width:246px}
.quota-card{width:246px;height:38px;border:1px solid var(--line);background:linear-gradient(180deg,var(--card-hover),var(--card));border-radius:var(--radius-md);padding:6px 10px;box-shadow:var(--sh-in),0 1px 2px rgba(36,30,24,.025)}
.quota-row{display:flex;align-items:center;gap:8px;font-size:12px;color:var(--text-3);line-height:14px}
.quota-row span::before{content:"";display:inline-block;width:6px;height:6px;border-radius:50%;background:var(--green);margin-right:6px;box-shadow:0 0 0 3px var(--green-soft)}
.quota-row strong{font-family:var(--fm);color:var(--text-1);font-weight:700}
.quota-row .djslink{margin-left:auto;font-size:12px}
.bar{height:3px;background:var(--press);border-radius:999px;margin-top:5px;overflow:hidden}
.bar span{display:block;height:100%;background:linear-gradient(90deg,var(--brand-lighter),var(--brand-light) 50%,var(--brand))}
.table-wrap{flex:1;min-height:0;overflow:auto;background:var(--card)}
.cloud-table{min-width:1040px}
.cloud-table :deep(th){height:44px}
.cloud-table :deep(td){height:76px}
tr.pending td{background:var(--brand-soft-03)}
.file{display:flex;align-items:center;gap:12px;min-width:0}
.compare-file{max-width:520px}
.file .djsav{width:40px;height:40px;border-radius:10px}
.avatar-box{overflow:hidden;flex-shrink:0;color:#fff;font-size:14px;font-weight:650}
.avatar-box img{width:100%;height:100%;display:block;object-fit:cover}
.file-name{max-width:500px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;font-size:13.5px;font-weight:650;color:var(--text-1);cursor:text}
.rename-input{width:360px;height:30px;border:1px solid var(--brand-light);border-radius:var(--radius-sm);padding:0 8px;outline:0;background:var(--card);font-family:var(--ff)}
.rename-input:focus{border-color:var(--brand);box-shadow:0 0 0 3px var(--brand-soft-12)}
.meta{display:flex;align-items:center;gap:8px;margin-top:6px;color:var(--text-3);font-size:12px;min-height:20px}
.mono{font-family:var(--fm);color:var(--text-2b);line-height:1.65}
.actions-cell{text-align:right}
.sub-actions{display:flex;gap:14px;justify-content:flex-end;margin-top:8px}
.progress{height:6px;background:var(--press);border-radius:999px;overflow:hidden;margin-left:auto;width:150px}
.progress span{display:block;height:100%;background:linear-gradient(90deg,var(--brand-lighter),var(--brand))}
.empty{text-align:center;color:var(--text-3);height:170px}
.pager{height:48px;display:flex;align-items:center;justify-content:center;border-top:1px solid var(--line);background:var(--card);flex:0 0 auto}

@media (max-width: 1280px){
  .toolbar{grid-template-columns:1fr}
  .toolbar-side{justify-content:flex-start;min-width:0}
}
</style>
