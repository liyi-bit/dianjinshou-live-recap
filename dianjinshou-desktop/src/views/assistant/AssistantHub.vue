<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Message, Modal } from '@arco-design/web-vue'
import * as recordingApi from '@/api/recording'
import * as comparisonApi from '@/api/comparison'
import type { Recording } from '@/api/recording'
import type { ComparisonItem } from '@/api/comparison'
import { useAssistantStore, type AssistantType } from '@/stores/assistant'
import RecordingPickerDialog from '@/components/assistant/RecordingPickerDialog.vue'
import ComparisonPickerDialog from '@/components/assistant/ComparisonPickerDialog.vue'
import Pagination from '@/components/common/Pagination.vue'
import { formatDateTime } from '@/utils/format'

interface Props {
  /** operation: 单场+对比 2 tab；compliance: 单场 1 tab */
  mode: AssistantType
}
const props = defineProps<Props>()

const router = useRouter()
const route = useRoute()
const store = useAssistantStore()

type TabKey = 'recap' | 'comparison'

const tabs = computed<{ key: TabKey; label: string }[]>(() => {
  if (props.mode === 'operation') {
    return [
      { key: 'recap', label: '单场复盘' },
      { key: 'comparison', label: '对比复盘' }
    ]
  }
  return [{ key: 'recap', label: '单场复盘' }]
})
const activeTab = ref<TabKey>(((route.query.tab as TabKey) || 'recap') as TabKey)

watch(activeTab, (k) => {
  router.replace({ query: { ...route.query, tab: k } })
  loadCurrent()
})

// 两条菜单路由复用同一组件实例，仅 mode prop 改变 —— onMounted 不会再触发，
// 必须在 mode 切换时手动重置 tab/筛选并重新拉数据。
watch(() => props.mode, () => {
  keyword.value = ''
  const validKeys = tabs.value.map(t => t.key)
  if (!validKeys.includes(activeTab.value)) {
    // 切到 compliance 但当前停在 'comparison' 这种无效 tab —— 改回 recap，
    // activeTab 的 watcher 会顺带触发 loadCurrent()
    activeTab.value = 'recap'
  } else {
    loadCurrent()
  }
})

// === 数据缓存（按 ID 反查后的明细） ===
const loading = ref(false)
const recordingMap = ref<Map<number, Recording>>(new Map())
const comparisonMap = ref<Map<number, ComparisonItem>>(new Map())
/** 反查失败的 ID（已被删除或无权访问）— 仅用于 UI 显示"录制不可用"，不主动从 store 删 */
const missingRecordingIds = ref<Set<number>>(new Set())
const missingComparisonIds = ref<Set<number>>(new Set())
const keyword = ref('')

// 当前 tab 在 store 中的 ID 列表
const activeRecordingIds = computed(() => store.getRecordingIds(props.mode))
const activeComparisonIds = computed(() => store.getComparisonIds(props.mode))

// 分页：纯前端切片（数据已经全部反查到 recordingMap/comparisonMap）
const PAGE_SIZE = 10
const recapPage = ref(1)
const compPage = ref(1)

// 反查后展示用的全集（保持添加顺序倒序，最新在上）—— 后续基于这个做分页切片
const recapAll = computed<Recording[]>(() => {
  const list: Recording[] = []
  for (let i = activeRecordingIds.value.length - 1; i >= 0; i--) {
    const id = activeRecordingIds.value[i]
    const r = recordingMap.value.get(id)
    if (r) list.push(r)
  }
  if (!keyword.value.trim()) return list
  const kw = keyword.value.trim().toLowerCase()
  return list.filter(r =>
    (r.anchorName || '').toLowerCase().includes(kw) ||
    (r.localFileName || '').toLowerCase().includes(kw)
  )
})
const compAll = computed<ComparisonItem[]>(() => {
  const list: ComparisonItem[] = []
  for (let i = activeComparisonIds.value.length - 1; i >= 0; i--) {
    const id = activeComparisonIds.value[i]
    const c = comparisonMap.value.get(id)
    if (c) list.push(c)
  }
  if (!keyword.value.trim()) return list
  const kw = keyword.value.trim().toLowerCase()
  return list.filter(c =>
    (c.anchorNameOptimize || '').toLowerCase().includes(kw) ||
    (c.anchorNameReference || '').toLowerCase().includes(kw)
  )
})

// 分页后的当前页
const recapList = computed<Recording[]>(() => {
  const start = (recapPage.value - 1) * PAGE_SIZE
  return recapAll.value.slice(start, start + PAGE_SIZE)
})
const compList = computed<ComparisonItem[]>(() => {
  const start = (compPage.value - 1) * PAGE_SIZE
  return compAll.value.slice(start, start + PAGE_SIZE)
})

// 总数变化时自动回退当前页（如删除最后一项导致越界）
watch(() => recapAll.value.length, (n) => {
  const max = Math.max(1, Math.ceil(n / PAGE_SIZE))
  if (recapPage.value > max) recapPage.value = max
})
watch(() => compAll.value.length, (n) => {
  const max = Math.max(1, Math.ceil(n / PAGE_SIZE))
  if (compPage.value > max) compPage.value = max
})

// 切 tab 或换关键词：回到第一页
watch([activeTab, keyword], () => {
  recapPage.value = 1
  compPage.value = 1
})

/** 当前进行中的加载请求 token，用于丢弃过期响应 */
let loadToken = 0

async function loadRecordings() {
  const ids = [...activeRecordingIds.value]
  const token = ++loadToken
  if (ids.length === 0) {
    recordingMap.value = new Map()
    missingRecordingIds.value = new Set()
    return
  }
  loading.value = true
  try {
    // 精准并发反查每个 ID（用 allSettled 避免单条失败拖累整批）
    const results = await Promise.allSettled(
      ids.map(id => recordingApi.getRecording(id))
    )
    if (token !== loadToken) return // 过期响应丢弃
    const m = new Map<number, Recording>()
    const missing = new Set<number>()
    results.forEach((r, idx) => {
      const id = ids[idx]
      if (r.status === 'fulfilled') {
        const data = (r.value as any).data ?? r.value
        // detail 接口把主播头像/名放在 streamerInfo 嵌套里；列表组件期望顶级字段，这里做扁平化兜底
        const flat: Recording = {
          ...data,
          anchorName: data?.anchorName ?? data?.streamerInfo?.anchorName ?? null,
          anchorAvatar: data?.anchorAvatar ?? data?.streamerInfo?.anchorAvatar ?? null,
        }
        m.set(id, flat)
      } else {
        missing.add(id)
      }
    })
    recordingMap.value = m
    missingRecordingIds.value = missing
  } catch (e: any) {
    if (token === loadToken) Message.error(e?.message || '加载失败')
  } finally {
    if (token === loadToken) loading.value = false
  }
}

async function loadComparisons() {
  const ids = [...activeComparisonIds.value]
  const token = ++loadToken
  if (ids.length === 0) {
    comparisonMap.value = new Map()
    missingComparisonIds.value = new Set()
    return
  }
  loading.value = true
  try {
    const results = await Promise.allSettled(
      ids.map(id => comparisonApi.getComparison(id))
    )
    if (token !== loadToken) return
    const m = new Map<number, ComparisonItem>()
    const missing = new Set<number>()
    results.forEach((r, idx) => {
      const id = ids[idx]
      if (r.status === 'fulfilled') {
        const data = (r.value as any).data ?? r.value
        m.set(id, data as ComparisonItem)
      } else {
        missing.add(id)
      }
    })
    comparisonMap.value = m
    missingComparisonIds.value = missing
  } catch (e: any) {
    if (token === loadToken) Message.error(e?.message || '加载失败')
  } finally {
    if (token === loadToken) loading.value = false
  }
}

function loadCurrent() {
  if (activeTab.value === 'recap') loadRecordings()
  else loadComparisons()
}

// === 添加（弹 Picker） ===
const recordingPickerVisible = ref(false)
const comparisonPickerVisible = ref(false)

function openAdd() {
  if (activeTab.value === 'recap') recordingPickerVisible.value = true
  else comparisonPickerVisible.value = true
}

function onRecordingsConfirm(ids: number[]) {
  store.addRecordings(props.mode, ids)
  Message.success(`已添加 ${ids.length} 个场次`)
  loadRecordings()
}
function onComparisonsConfirm(ids: number[]) {
  store.addComparisons(props.mode, ids)
  Message.success(`已添加 ${ids.length} 个对比`)
  loadComparisons()
}

// === 移除（不删原数据） ===
function removeRecap(r: Recording) {
  Modal.confirm({
    title: '从助手中移除',
    content: `从 AI ${assistantLabel.value}中移除「${r.anchorName || r.localFileName}」？（不会删除原录制）`,
    okText: '移除',
    cancelText: '取消',
    onOk: () => {
      store.removeRecording(props.mode, r.id)
      Message.success('已移除')
    }
  })
}
function removeComparison(c: ComparisonItem) {
  Modal.confirm({
    title: '从助手中移除',
    content: `从 AI ${assistantLabel.value}中移除该对比？（不会删除原对比）`,
    okText: '移除',
    cancelText: '取消',
    onOk: () => {
      store.removeComparison(props.mode, c.id)
      Message.success('已移除')
    }
  })
}

// === 跳转详情 ===
// 即使 latestTaskId 为空也照样跳到对应的 AI 助手首页 —— 由助手页面自行决定空状态展示与引导，
// 这里不再用 toast 拦住用户。
function goRecapDetail(r: Recording) {
  const name = props.mode === 'operation' ? 'AssistantOperation' : 'AssistantCompliance'
  const query: Record<string, string | number> = { recordingId: r.id }
  if (r.latestTaskId) query.taskId = r.latestTaskId
  router.push({ name, query })
}
function goComparisonDetail(c: ComparisonItem) {
  const name = props.mode === 'operation' ? 'AssistantOperation' : 'AssistantCompliance'
  router.push({ name, query: { comparisonId: c.id } })
}

// === 工具 ===
const STATUS_MAP: Record<string, { label: string; tone: 'green' | 'red' | 'amber' | 'blue' | 'gray' }> = {
  pending:        { label: '待处理',  tone: 'amber' },
  processing:     { label: '处理中',  tone: 'blue' },
  recording:      { label: '录制中',  tone: 'blue' },
  asr_processing: { label: 'ASR 中',   tone: 'blue' },
  transcribing:   { label: '转写中',  tone: 'blue' },
  transcribed:    { label: '已转写',  tone: 'amber' },
  ai_processing:  { label: 'AI 中',    tone: 'blue' },
  completed:      { label: '已完成',  tone: 'green' },
  failed:         { label: '失败',     tone: 'red' },
  none:           { label: '未开始',  tone: 'gray' }
}
function statusInfo(s: string | null | undefined) {
  if (!s) return STATUS_MAP['none']
  return STATUS_MAP[s] || { label: s, tone: 'gray' as const }
}

const assistantLabel = computed(() => props.mode === 'operation' ? '运营助手' : '违规助手')
const addBtnLabel = computed(() => activeTab.value === 'recap' ? '添加场次' : '添加对比')

onMounted(() => loadCurrent())
</script>

<template>
  <div class="hub">
    <!-- ===== 顶部 Tab ===== -->
    <div class="hub-tab-bar">
      <div class="hub-tabs">
        <span
          v-for="t in tabs"
          :key="t.key"
          class="hub-tab"
          :class="{ on: activeTab === t.key }"
          @click="activeTab = t.key"
        >
          {{ t.label }}
          <span class="hub-count" v-if="t.key === 'recap'">{{ activeRecordingIds.length }}</span>
          <span class="hub-count" v-else>{{ activeComparisonIds.length }}</span>
        </span>
      </div>
    </div>

    <!-- ===== 筛选栏（含添加按钮） ===== -->
    <div class="djscard hub-filter-card">
      <div class="djsfilter">
        <div class="sch" style="width:240px">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input type="text" placeholder="按主播 / 文件名筛选" v-model="keyword" />
        </div>
        <div class="spacer"></div>
        <a class="djslink sm hub-refresh-link" @click="loadCurrent">
          <svg style="width:13px;height:13px;vertical-align:-2px;margin-right:3px" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M23 4v6h-6"/><path d="M1 20v-6h6"/><path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"/></svg>
          刷新
        </a>
        <button class="djsbtn primary hub-add-btn" @click="openAdd">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          {{ addBtnLabel }}
        </button>
      </div>
    </div>

    <!-- ===== 表格区 ===== -->
    <div class="djscard hub-table-card">
      <!-- 单场复盘 -->
      <table v-if="activeTab === 'recap' && recapList.length > 0" class="djstbl hub-tbl">
        <colgroup>
          <col style="width:24%" />
          <col style="width:14%" />
          <col style="width:11%" />
          <col style="width:11%" />
          <col style="width:14%" />
          <col style="width:26%" />
        </colgroup>
        <thead>
          <tr>
            <th>主播</th>
            <th>录制时间</th>
            <th style="text-align:right">智能敏感词</th>
            <th style="text-align:right">运营关键词</th>
            <th>分析状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in recapList" :key="r.id" class="row-clickable" @click="goRecapDetail(r)">
            <td>
              <div class="cell-streamer">
                <div class="hub-av">
                  <img v-if="r.anchorAvatar" :src="r.anchorAvatar" referrerpolicy="no-referrer" />
                  <span v-else>{{ (r.anchorName || r.localFileName || '?').charAt(0) }}</span>
                </div>
                <div style="min-width:0">
                  <div class="hub-name">{{ r.anchorName || r.localFileName }}</div>
                  <div class="hub-sub mono">{{ r.localFileName }}</div>
                </div>
              </div>
            </td>
            <td>{{ formatDateTime(r.startTime) }}</td>
            <td style="text-align:right"><span class="mono num-cell" :class="{ 'is-zero': r.sensitiveWordCount == null || r.sensitiveWordCount === 0 }">{{ r.sensitiveWordCount == null ? '—' : r.sensitiveWordCount }}</span></td>
            <td style="text-align:right"><span class="mono num-cell" :class="{ 'is-zero': r.operationKeywordCount == null || r.operationKeywordCount === 0 }">{{ r.operationKeywordCount == null ? '—' : r.operationKeywordCount }}</span></td>
            <td class="col-status"><span class="djsbadge" :class="statusInfo(r.analysisStatus).tone">{{ statusInfo(r.analysisStatus).label }}</span></td>
            <td class="col-action" @click.stop>
              <button class="djsbtn ghost row-action" @click="goRecapDetail(r)">查看 AI 分析</button>
              <a class="djslink sm danger" @click="removeRecap(r)">移除</a>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 对比复盘 -->
      <table v-else-if="activeTab === 'comparison' && compList.length > 0" class="djstbl hub-tbl">
        <colgroup>
          <col style="width:30%" />
          <col style="width:30%" />
          <col style="width:18%" />
          <col style="width:22%" />
        </colgroup>
        <thead>
          <tr>
            <th>对比 1（优化场）</th>
            <th>对比 2（参考场）</th>
            <th>对比时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in compList" :key="c.id" class="row-clickable" @click="goComparisonDetail(c)">
            <td>
              <div class="cell-streamer">
                <div class="hub-av">
                  <img v-if="c.anchorAvatarOptimize" :src="c.anchorAvatarOptimize" referrerpolicy="no-referrer" />
                  <span v-else>{{ (c.anchorNameOptimize || '?').charAt(0) }}</span>
                </div>
                <div style="min-width:0">
                  <div class="hub-name">{{ c.anchorNameOptimize || '—' }}</div>
                  <div class="hub-sub mono">{{ c.clipFilenameOptimize || c.localFileNameOptimize || '—' }}</div>
                </div>
              </div>
            </td>
            <td>
              <div class="cell-streamer">
                <div class="hub-av av-ref">
                  <img v-if="c.anchorAvatarReference" :src="c.anchorAvatarReference" referrerpolicy="no-referrer" />
                  <span v-else>{{ (c.anchorNameReference || '?').charAt(0) }}</span>
                </div>
                <div style="min-width:0">
                  <div class="hub-name">{{ c.anchorNameReference || '—' }}</div>
                  <div class="hub-sub mono">{{ c.clipFilenameReference || c.localFileNameReference || '—' }}</div>
                </div>
              </div>
            </td>
            <td>{{ formatDateTime(c.createdAt) }}</td>
            <td class="col-action" @click.stop>
              <button class="djsbtn ghost row-action" @click="goComparisonDetail(c)">查看 AI 分析</button>
              <a class="djslink sm danger" @click="removeComparison(c)">移除</a>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Loading / 空 -->
      <div v-else-if="loading" class="state-msg">加载中…</div>
      <div v-else class="empty-tab">
        <div class="empty-illust">
          <svg viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
            <rect x="10" y="14" width="44" height="36" rx="4"/>
            <line x1="20" y1="26" x2="44" y2="26"/>
            <line x1="20" y1="34" x2="44" y2="34"/>
            <line x1="20" y1="42" x2="34" y2="42"/>
          </svg>
        </div>
        <div class="empty-title">尚未添加任何{{ activeTab === 'recap' ? '场次' : '对比' }}</div>
        <div class="empty-desc">点击右上角「{{ addBtnLabel }}」选择需要 AI {{ assistantLabel }}分析的内容</div>
        <button class="djsbtn primary" style="margin-top:18px" @click="openAdd">
          <svg style="width:14px;height:14px;vertical-align:-2px;margin-right:4px" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          {{ addBtnLabel }}
        </button>
      </div>

      <!-- 分页（在 .hub-table-card 内、表格之后）-->
      <Pagination
        v-if="activeTab === 'recap' && recapAll.length > 0"
        :page="recapPage"
        :size="PAGE_SIZE"
        :total="recapAll.length"
        @update:page="(p) => recapPage = p"
      />
      <Pagination
        v-if="activeTab === 'comparison' && compAll.length > 0"
        :page="compPage"
        :size="PAGE_SIZE"
        :total="compAll.length"
        @update:page="(p) => compPage = p"
      />
    </div>

    <!-- 添加弹窗 -->
    <RecordingPickerDialog
      v-model:visible="recordingPickerVisible"
      :added-ids="activeRecordingIds"
      :title="`添加场次到 AI ${assistantLabel}`"
      @confirm="onRecordingsConfirm"
    />
    <ComparisonPickerDialog
      v-model:visible="comparisonPickerVisible"
      :added-ids="activeComparisonIds"
      :title="`添加对比到 AI ${assistantLabel}`"
      @confirm="onComparisonsConfirm"
    />
  </div>
</template>

<style scoped>
.hub { padding: 0; }

/* ===== 顶部 Tab 栏 ===== */
.hub-tab-bar {
  display: flex;
  align-items: center;
  height: 42px;
  padding: 0 12px 0 4px;
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  margin-bottom: 10px;
  box-shadow: 0 1px 2px rgba(36,30,24,.025);
}
.hub-tabs {
  display: flex;
  align-items: stretch;
  height: 100%;
}
.hub-tab {
  font-size: 14px; font-weight: 500;
  color: var(--text-3);
  cursor: pointer;
  padding: 0 22px;
  display: inline-flex; align-items: center;
  gap: 8px;
  position: relative;
  letter-spacing: -.005em;
  transition: color .18s var(--ease);
}
.hub-tab:not(.on):hover { color: var(--text-2); }
.hub-tab.on {
  color: var(--brand);
  font-weight: 650;
}
.hub-tab.on::after {
  content: '';
  position: absolute;
  left: 22px; right: 22px; bottom: 8px;
  height: 2.5px; border-radius: 2px;
  background: linear-gradient(90deg, var(--brand-lighter), var(--brand));
  box-shadow: 0 1px 3px rgba(184,130,58,.3);
}
.hub-tab + .hub-tab::before {
  content: '';
  position: absolute; left: 0; top: 11px; bottom: 11px;
  width: 1px; background: var(--line-2);
}
.hub-count {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 18px; height: 18px; padding: 0 6px;
  font-size: 11px; font-weight: 700;
  background: var(--bg-2);
  color: var(--text-3);
  border-radius: var(--radius-pill, 100px);
  font-family: var(--fm);
}
.hub-tab.on .hub-count {
  background: var(--brand-soft);
  color: var(--brand);
}

.spacer { flex: 1; }

.hub-add-btn {
  height: 30px; padding: 0 14px;
  font-size: 12px; font-weight: 600;
  display: inline-flex; align-items: center; gap: 5px;
  margin-left: 4px;
}
.hub-add-btn svg { width: 12px; height: 12px; }

.hub-refresh-link {
  display: inline-flex;
  align-items: center;
  height: 30px;
  padding: 0 10px;
  border-radius: 6px;
  font-size: 12.5px;
  color: var(--text-3);
  cursor: pointer;
  transition: color .15s var(--ease), background .15s var(--ease);
}
.hub-refresh-link:hover {
  color: var(--brand);
  background: var(--brand-soft-06);
}

/* ===== 筛选卡 ===== */
.hub-filter-card {
  padding: 10px 16px;
  margin-bottom: 10px;
}

/* ===== 表格卡 ===== */
.hub-table-card {
  padding: 0;
  overflow: hidden;
}
/*
 * .hub-tbl 仅做"固定列宽 + 行 hover 颜色"两项个性化，
 * 字号/字重/字间距/padding 一律继承全局 .djstbl，避免 AI 助手列表与系统其他列表观感不一致。
 */
.hub-tbl {
  table-layout: fixed;
  width: 100%;
}
.hub-tbl tbody tr.row-clickable {
  cursor: pointer;
}
.hub-tbl .col-status,
.hub-tbl .col-action { white-space: nowrap; }
.hub-tbl tbody tr.row-clickable:hover {
  background: var(--brand-soft-06);
}

/* 主播单元 */
.cell-streamer {
  display: flex; align-items: center; gap: 12px;
  min-width: 0;
}
.hub-av {
  width: 38px; height: 38px;
  border-radius: 9px;
  overflow: hidden;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, var(--brand-lighter), var(--brand-dark));
  color: #fff; font-weight: 700; font-size: 14px;
  flex-shrink: 0;
  box-shadow:
    inset 0 1px 0 rgba(255,255,255,.25),
    0 1px 2px rgba(36,30,24,.08);
}
.hub-av img { width:100%; height:100%; object-fit:cover; }
.hub-av.av-ref {
  background: linear-gradient(135deg, var(--orange-light), var(--orange));
}

/* 主播单元继承全局 .djstbl tbody td 字号；不覆盖 font-size 以与其他列表保持一致 */
.hub-name {
  color: var(--text-1);
  font-weight: 600;
  letter-spacing: -.005em;
  line-height: 1.25;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  max-width: 280px;
}
.hub-sub {
  font-size: 12px; color: var(--text-3);
  margin-top: 2px; line-height: 1.2;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  max-width: 280px;
}

/* 数字列 */
.num-cell {
  color: var(--text-1);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.num-cell.is-zero {
  color: var(--text-4);
  font-weight: 500;
}
.dim { color: var(--text-3); }
.dim.sm { font-size: 11.5px; }

/* 操作列按钮 */
.row-action {
  height: 28px;
  padding: 0 14px;
  font-size: 12px;
  border-radius: 6px;
}
.djslink.sm { font-size: 12.5px; cursor: pointer; }
.djslink.danger { color: var(--red); }
.djslink.danger:hover { color: var(--red-dark); text-decoration: underline; }
td .djslink.sm.danger { margin-left: 12px; }

/* ===== 状态徽章 ===== */
.djsbadge {
  display: inline-flex; align-items: center;
  height: 22px;
  padding: 0 9px 0 18px;
  border-radius: var(--radius-pill, 100px);
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: .02em;
  position: relative;
}
.djsbadge::before {
  content: '';
  position: absolute;
  left: 8px; top: 50%;
  width: 6px; height: 6px;
  border-radius: 50%;
  transform: translateY(-50%);
}
.djsbadge.green { background: var(--green-soft); color: var(--green-dark); border: 1px solid rgba(62,122,92,.18); }
.djsbadge.green::before { background: var(--green); }
.djsbadge.red { background: var(--red-soft); color: var(--red-dark); border: 1px solid rgba(184,68,60,.2); }
.djsbadge.red::before { background: var(--red); }
.djsbadge.amber { background: var(--gold-soft); color: #6B4800; border: 1px solid rgba(168,118,14,.22); }
.djsbadge.amber::before { background: var(--gold); }
.djsbadge.blue { background: var(--blue2-soft); color: var(--blue2); border: 1px solid rgba(74,104,150,.2); }
.djsbadge.blue::before { background: var(--blue2); }
.djsbadge.gray { background: var(--bg-2); color: var(--text-3); border: 1px solid var(--line); }
.djsbadge.gray::before { background: var(--text-4); }

/* ===== 状态/空 ===== */
.state-msg {
  padding: 80px 20px;
  text-align: center;
  color: var(--text-3);
  font-size: 13px;
}

.empty-tab {
  padding: 70px 20px 90px;
  text-align: center;
  color: var(--text-3);
}
.empty-illust {
  width: 84px; height: 84px;
  border-radius: 50%;
  margin: 0 auto 16px;
  display: flex; align-items: center; justify-content: center;
  background: var(--brand-soft-06);
  color: var(--brand);
}
.empty-illust svg { width: 42px; height: 42px; }
.empty-title {
  font-size: 16px;
  font-weight: 650;
  color: var(--text-1);
  margin-bottom: 6px;
  letter-spacing: -.01em;
}
.empty-desc {
  font-size: 12.5px;
  color: var(--text-3);
}

.mono { font-family: var(--fm); }
</style>
