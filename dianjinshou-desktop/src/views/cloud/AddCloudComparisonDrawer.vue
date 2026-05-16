<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { Message } from '@arco-design/web-vue'
import * as cloudApi from '@/api/cloudSpace'
import type { CloudCompareMode, CloudFileItem } from '@/api/cloudSpace'
import { useStreamerStore } from '@/stores/streamer'
import { formatDateTime } from '@/utils/format'

const props = defineProps<{
  visible: boolean
  mode: CloudCompareMode
}>()

const emit = defineEmits<{
  close: []
  selectedTwo: [items: CloudFileItem[]]
}>()

const loading = ref(false)
const streamerStore = useStreamerStore()
const page = ref(1)
const size = ref(10)
const total = ref(0)
const keyword = ref('')
const items = ref<CloudFileItem[]>([])
const selected = ref<CloudFileItem[]>([])

const selectedIds = computed(() => selected.value.map((item) => item.id))

watch(() => props.visible, (visible) => {
  if (visible) {
    selected.value = []
    page.value = 1
    streamerStore.fetchAllStreamers()
    fetchCandidates()
  }
})

onMounted(() => {
  streamerStore.fetchAllStreamers()
})

watch(() => props.mode, () => {
  if (props.visible) {
    selected.value = []
    page.value = 1
    fetchCandidates()
  }
})

async function fetchCandidates() {
  loading.value = true
  try {
    const res: any = await cloudApi.listComparisonCandidates(props.mode, {
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
    })
    const data = res?.data ?? res
    items.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function choose(item: CloudFileItem) {
  if (selectedIds.value.includes(item.id)) {
    selected.value = selected.value.filter((candidate) => candidate.id !== item.id)
    return
  }
  if (selected.value.length >= 2) {
    Message.warning('最多选择两条数据')
    return
  }
  selected.value = [...selected.value, item]
  if (selected.value.length === 2) {
    emit('selectedTwo', [...selected.value])
  }
}

function streamerInitial(name?: string | null) {
  return (name || '?').trim().charAt(0) || '?'
}

function avatarFor(item: CloudFileItem) {
  if (item.anchorAvatar) return item.anchorAvatar
  const anchorName = item.anchorName?.trim()
  const streamer = streamerStore.allStreamers.find((s) =>
    (item.streamerId != null && String(s.id) === String(item.streamerId)) ||
    (!!anchorName && s.anchorName?.trim() === anchorName)
  )
  return streamer?.anchorAvatar || null
}

function formatCloudTime(item: CloudFileItem) {
  return item.recordedAt || item.createdAt ? formatDateTime(item.recordedAt || item.createdAt) : '-'
}

function formatDuration(seconds?: number | null) {
  if (!seconds) return '-'
  const min = Math.floor(seconds / 60)
  const sec = seconds % 60
  return `${min}分${String(sec).padStart(2, '0')}秒`
}

function displayName(item: CloudFileItem) {
  return item.displayName || item.fileName || '未命名文件'
}
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="drawer-mask">
      <aside class="cloud-drawer">
        <div class="dw-head cloud-drawer-head">
          <div class="dw-title">新增对比分析</div>
          <button class="dw-close" @click="emit('close')">×</button>
        </div>
        <div class="drawer-toolbar djsfilter">
          <input v-model="keyword" class="djsinput search-input" placeholder="输入主播或文件名" @keyup.enter="fetchCandidates" />
          <button class="djsbtn primary sm" @click="fetchCandidates">查找</button>
          <a class="djslink" @click="fetchCandidates">刷新</a>
          <span class="spacer"></span>
          <div class="selected-count">已选 {{ selected.length }}/2</div>
        </div>
        <div class="selected-strip" v-if="selected.length">
          <span v-for="item in selected" :key="item.id" class="djsbadge amber">{{ displayName(item) }} <button @click="choose(item)">×</button></span>
        </div>
        <div class="table-scroll">
          <table class="djstbl drawer-table">
            <thead>
              <tr>
                <th>主播</th>
                <th>录制时间</th>
                <th>视频时长</th>
                <th style="text-align:right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading"><td colspan="4" class="empty">加载中...</td></tr>
              <tr v-else-if="items.length === 0"><td colspan="4" class="empty">暂无可对比数据</td></tr>
              <tr v-for="item in items" :key="item.id" :class="{ selected: selectedIds.includes(item.id) }">
                <td>
                  <div class="file">
                    <div class="djsav avatar-box" :class="mode === 'clip' ? 'g1' : 'g3'">
                      <img v-if="avatarFor(item)" :src="avatarFor(item)!" referrerpolicy="no-referrer" />
                      <template v-else>{{ streamerInitial(item.anchorName || displayName(item)) }}</template>
                    </div>
                    <div>
                      <div class="name">{{ displayName(item) }}</div>
                      <div class="meta"><span class="djsbadge" :class="mode === 'clip' ? 'blue' : 'amber'">{{ mode === 'clip' ? '切片' : '全场' }}</span><span>{{ item.accountType || '-' }}</span></div>
                    </div>
                  </div>
                </td>
                <td class="mono">{{ formatCloudTime(item) }}</td>
                <td class="mono">{{ formatDuration(item.durationSeconds) }}</td>
                <td style="text-align:right">
                  <button class="djsbtn sm" :class="selectedIds.includes(item.id) ? 'primary' : 'ghost'" @click="choose(item)">
                    {{ selectedIds.includes(item.id) ? '已选择' : '加入对比' }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="drawer-foot">
          <span>共 {{ total }} 条</span>
          <button class="page-btn" :disabled="page <= 1" @click="page--; fetchCandidates()">‹</button>
          <span>{{ page }}</span>
          <button class="page-btn" :disabled="page * size >= total" @click="page++; fetchCandidates()">›</button>
        </div>
      </aside>
    </div>
  </Teleport>
</template>

<style scoped>
.drawer-mask{position:fixed;inset:0;background:rgba(26,22,18,.45);z-index:1000;display:flex;justify-content:flex-end;backdrop-filter:blur(4px)}
.cloud-drawer{width:min(920px,52vw);min-width:760px;height:100%;background:linear-gradient(180deg,var(--card-hover),var(--card));box-shadow:var(--sh-4);display:flex;flex-direction:column;border-left:1px solid var(--line)}
.cloud-drawer-head{height:auto;padding:20px 26px}
.cloud-drawer-head .dw-title{font-size:22px}
.cloud-drawer-head .dw-close{border:0;background:transparent;font-size:24px;line-height:1}
.drawer-toolbar{padding:14px 24px}
.search-input{width:260px}
.selected-count{height:28px;display:inline-flex;align-items:center;padding:0 10px;border-radius:var(--radius-pill);background:var(--brand-soft-06);color:var(--text-3);font-size:12px;font-weight:650}
.selected-strip{display:flex;gap:8px;flex-wrap:wrap;padding:0 24px 12px}
.selected-strip .djsbadge{height:26px;gap:6px;max-width:260px;text-transform:none;letter-spacing:0}
.selected-strip button{border:0;background:transparent;color:var(--brand);cursor:pointer;font-weight:700}
.table-scroll{flex:1;overflow:auto;padding:0 24px;background:var(--card)}
.drawer-table{min-width:760px}
.drawer-table :deep(th){height:44px}
.drawer-table :deep(td){height:72px}
tr.selected td{background:var(--brand-soft-03)}
.file{display:flex;align-items:center;gap:10px}
.file .djsav{width:38px;height:38px;border-radius:10px}
.avatar-box{overflow:hidden;flex-shrink:0;color:#fff;font-size:13px;font-weight:650}
.avatar-box img{width:100%;height:100%;display:block;object-fit:cover}
.name{max-width:390px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;font-weight:650;color:var(--text-1)}
.meta{display:flex;align-items:center;gap:8px;margin-top:4px;color:var(--text-3);font-size:12px}
.meta .djsbadge{height:20px;padding:0 8px;font-size:10px}
.mono{font-family:var(--fm);color:var(--text-2b)}
.empty{text-align:center;color:var(--text-3)}
.drawer-foot{height:52px;display:flex;align-items:center;justify-content:center;gap:14px;border-top:1px solid var(--line);color:var(--text-3);font-size:13px;background:var(--card)}
.page-btn{width:30px;height:30px;border:1px solid var(--line);background:var(--card);border-radius:var(--radius-sm);cursor:pointer}
.page-btn:disabled{opacity:.45;cursor:not-allowed}
</style>
