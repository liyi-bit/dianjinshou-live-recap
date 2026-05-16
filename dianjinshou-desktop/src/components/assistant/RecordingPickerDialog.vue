<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Message } from '@arco-design/web-vue'
import * as recordingApi from '@/api/recording'
import type { Recording } from '@/api/recording'
import { formatDateTime } from '@/utils/format'

interface Props {
  visible: boolean
  /** 已添加的 ID 集合（用于禁用） */
  addedIds: number[]
  title?: string
}
const props = withDefaults(defineProps<Props>(), {
  title: '添加场次到 AI 助手'
})
const emit = defineEmits<{
  (e: 'update:visible', v: boolean): void
  (e: 'confirm', ids: number[]): void
}>()

const loading = ref(false)
const list = ref<Recording[]>([])
const total = ref(0)
const page = ref(1)
const size = 20
const keyword = ref('')
const selected = ref<Set<number>>(new Set())
let loadToken = 0 // 防止快速开关时旧响应覆盖新数据

const addedSet = computed(() => new Set(props.addedIds))

async function load() {
  const token = ++loadToken
  loading.value = true
  try {
    const res = await recordingApi.getRecordings({
      page: page.value, size, tab: 'ALL',
      analysisStatus: 'completed'
    })
    if (token !== loadToken) return
    const data = (res as any).data ?? res
    list.value = (data.items || []) as Recording[]
    total.value = data.total || 0
  } catch (e: any) {
    if (token === loadToken) Message.error(e?.message || '加载失败')
  } finally {
    if (token === loadToken) loading.value = false
  }
}

watch(() => props.visible, (v) => {
  if (v) {
    selected.value = new Set()
    page.value = 1
    keyword.value = ''
    load()
  } else {
    loadToken++ // 关闭时让在飞请求作废
  }
})

function toggleOne(id: number) {
  if (addedSet.value.has(id)) return
  const s = new Set(selected.value)
  if (s.has(id)) s.delete(id)
  else s.add(id)
  selected.value = s
}

function close() { emit('update:visible', false) }
function confirm() {
  if (selected.value.size === 0) { Message.warning('请至少选择一项'); return }
  emit('confirm', Array.from(selected.value))
  close()
}

const filteredList = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return list.value
  return list.value.filter(r =>
    (r.anchorName || '').toLowerCase().includes(kw) ||
    (r.localFileName || '').toLowerCase().includes(kw)
  )
})

function totalPages() { return Math.max(1, Math.ceil(total.value / size)) }
function changePage(delta: number) {
  const next = Math.min(Math.max(1, page.value + delta), totalPages())
  if (next === page.value) return
  page.value = next
  load()
}
</script>

<template>
  <a-modal
    :visible="visible"
    :title="title"
    :width="780"
    :footer="false"
    :mask-closable="false"
    @cancel="close"
  >
    <div class="picker">
      <div class="picker-toolbar">
        <div class="sch" style="flex:1">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input type="text" placeholder="按主播 / 文件名（仅过滤当前页）" v-model="keyword" />
        </div>
        <span class="picker-hint">已选 <b class="mono">{{ selected.size }}</b> 项</span>
      </div>

      <div class="picker-body">
        <div v-if="loading" class="picker-empty">加载中…</div>
        <div v-else-if="filteredList.length === 0" class="picker-empty">
          <div class="empty-ico">📋</div>
          <div v-if="keyword.trim()">当前页未匹配「{{ keyword }}」<br /><span class="dim sm">提示：搜索仅过滤当前页，可翻下一页查看</span></div>
          <div v-else>暂无可添加的录制</div>
        </div>
        <div v-else class="picker-list">
          <label
            v-for="r in filteredList"
            :key="r.id"
            class="picker-row"
            :class="{
              checked: selected.has(r.id),
              disabled: addedSet.has(r.id)
            }"
            @click.prevent="toggleOne(r.id)"
          >
            <span class="picker-checkbox">
              <span class="check-mark" v-if="selected.has(r.id) || addedSet.has(r.id)">✓</span>
            </span>
            <div class="picker-av">
              <img v-if="r.anchorAvatar" :src="r.anchorAvatar" referrerpolicy="no-referrer" />
              <span v-else>{{ (r.anchorName || r.localFileName || '?').charAt(0) }}</span>
            </div>
            <div class="picker-info">
              <div class="row-name">{{ r.anchorName || r.localFileName }}</div>
              <div class="row-sub mono">{{ r.localFileName }}</div>
            </div>
            <div class="picker-meta">
              <div class="mono">{{ formatDateTime(r.startTime) }}</div>
              <div class="mono dim">敏感词 {{ r.sensitiveWordCount ?? 0 }} · 关键词 {{ r.operationKeywordCount ?? 0 }}</div>
            </div>
            <span v-if="addedSet.has(r.id)" class="added-tag">已添加</span>
          </label>
        </div>
      </div>

      <div class="picker-footer">
        <div class="pager">
          <button class="djsbtn ghost sm" :disabled="page <= 1" @click="changePage(-1)">‹ 上一页</button>
          <span class="page-num">{{ page }} / {{ totalPages() }}</span>
          <button class="djsbtn ghost sm" :disabled="page >= totalPages()" @click="changePage(1)">下一页 ›</button>
        </div>
        <div class="actions">
          <button class="djsbtn" @click="close">取消</button>
          <button class="djsbtn primary" @click="confirm">添加 {{ selected.size > 0 ? `(${selected.size})` : '' }}</button>
        </div>
      </div>
    </div>
  </a-modal>
</template>

<style scoped>
.picker { display: flex; flex-direction: column; height: 540px; }

.picker-toolbar {
  display: flex; align-items: center; gap: 12px;
  margin-bottom: 12px;
}
.picker-hint {
  font-size: 12.5px; color: var(--text-3);
  white-space: nowrap;
}
.picker-hint b { color: var(--brand); }

.picker-body {
  flex: 1; overflow-y: auto;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--bg);
}
.picker-empty {
  padding: 80px 20px; text-align: center;
  color: var(--text-3); font-size: 13px;
}
.empty-ico { font-size: 32px; opacity: .4; margin-bottom: 6px; }

.picker-list { display: flex; flex-direction: column; }
.picker-row {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--line-2);
  cursor: pointer;
  transition: background .15s var(--ease);
  user-select: none;
  position: relative;
}
.picker-row:last-child { border-bottom: none; }
.picker-row:hover:not(.disabled) { background: var(--brand-soft-06); }
.picker-row.checked { background: var(--brand-soft-12); }
.picker-row.disabled { cursor: not-allowed; opacity: .55; }

.picker-checkbox {
  width: 18px; height: 18px;
  border: 1.5px solid var(--line-3);
  border-radius: 4px;
  background: var(--card);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  transition: all .15s var(--ease);
}
.picker-row.checked .picker-checkbox,
.picker-row.disabled .picker-checkbox {
  background: var(--brand);
  border-color: var(--brand);
}
.check-mark {
  color: #fff; font-size: 12px; font-weight: 700;
  line-height: 1;
}

.picker-av {
  width: 36px; height: 36px;
  border-radius: 8px;
  overflow: hidden;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, var(--brand-lighter), var(--brand-dark));
  color: #fff; font-weight: 700; font-size: 13px;
  flex-shrink: 0;
  box-shadow: 0 1px 2px rgba(36,30,24,.08);
}
.picker-av img { width:100%; height:100%; object-fit:cover; }

.picker-info { flex: 1; min-width: 0; }
.row-name {
  font-size: 13.5px; font-weight: 600; color: var(--text-1);
  letter-spacing: -.005em;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.row-sub {
  font-size: 11.5px; color: var(--text-3);
  margin-top: 3px; line-height: 1.2;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

.picker-meta {
  text-align: right; flex-shrink: 0;
}
.picker-meta .mono { font-size: 12px; color: var(--text-2); display: block; }
.picker-meta .dim { color: var(--text-3); font-size: 11px; margin-top: 3px; }

.added-tag {
  position: absolute; right: 14px; top: 12px;
  font-size: 10.5px;
  padding: 2px 8px;
  background: var(--green-soft);
  color: var(--green-dark);
  border: 1px solid rgba(62,122,92,.22);
  border-radius: var(--radius-pill, 100px);
  font-weight: 600;
}

.picker-footer {
  display: flex; align-items: center; justify-content: space-between;
  padding-top: 14px; margin-top: 12px;
  border-top: 1px solid var(--line);
}
.pager { display: flex; align-items: center; gap: 12px; }
.page-num { font-size: 12.5px; color: var(--text-2); font-family: var(--fm); }
.actions { display: flex; gap: 8px; }
.djsbtn.sm { height: 28px; padding: 0 12px; font-size: 12px; }

.mono { font-family: var(--fm); }
.dim { color: var(--text-3); }
</style>
