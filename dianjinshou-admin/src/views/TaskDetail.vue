<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { taskDetail, type AdminTaskDetail } from '../api';
import { formatBytes, formatDateTime, formatDuration } from '../utils/format';
import RecapDetailView from '../components/RecapDetailView.vue';

const route = useRoute();
const router = useRouter();

const taskType = String(route.params.type);
const id = Number(route.params.id);
const loading = ref(false);
const detail = ref<AdminTaskDetail | null>(null);

const useRecapView = computed(() => detail.value?.taskType === 'analysis');

async function load() {
  loading.value = true;
  try { detail.value = await taskDetail(taskType, id); } finally { loading.value = false; }
}

function typeLabel(t: string | null | undefined) {
  return { analysis: '录制分析', file_analysis: '文件分析', upload: '分块上传' }[t || ''] || t || '—';
}
function typeBadgeClass(t: string | null | undefined) {
  return { analysis: 'purple', file_analysis: 'blue', upload: 'amber' }[t || ''] || 'blue';
}

function statusClass(s: string | null | undefined) {
  if (!s) return '';
  const u = s.toUpperCase();
  if (u === 'COMPLETED') return 'green';
  if (u === 'FAILED') return 'red';
  if (u === 'PENDING') return 'amber';
  return 'blue';
}
function statusLabel(s: string | null | undefined) {
  if (!s) return '—';
  const m: Record<string, string> = {
    PENDING: '待处理', pending: '待处理', processing: '处理中',
    ASR_PROCESSING: 'ASR 中', TRANSCRIBING: '转写中',
    TRANSCRIBED: '已转写', AI_PROCESSING: 'AI 中',
    COMPLETED: '已完成', completed: '已完成',
    uploading: '上传中', FAILED: '失败', failed: '失败'
  };
  return m[s] || s;
}

function formatDate(s: string | null | undefined) {
  return formatDateTime(s);
}

function goRecordingDetail(rid: number) {
  router.push(`/recordings/${rid}`);
}

onMounted(load);
</script>

<template>
  <div>
    <div class="top-nav-wrap">
      <span class="back-link" @click="router.push('/tasks')">&lsaquo; 任务情况</span>
    </div>

    <div v-if="loading" style="padding:60px;text-align:center;color:var(--text-3)">加载中…</div>

    <template v-else-if="detail">
      <!-- analysis 任务：RecapDetail 同款布局 -->
      <RecapDetailView
        v-if="useRecapView"
        :task="detail"
        :display-name="detail.recordingName || detail.username || `任务 #${detail.id}`"
        :start-time="detail.startedAt"
        :end-time="detail.completedAt"
        :duration="detail.duration"
      />

      <!-- file_analysis / upload：简化版 -->
      <template v-else>
        <div class="djscard detail-header" style="margin-bottom:14px">
          <div class="task-icon">
            <svg v-if="detail.taskType === 'file_analysis'" width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><path d="M14 2v6h6"/></svg>
            <svg v-else width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
          </div>
          <div style="flex:1; min-width:0">
            <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap">
              <h2 class="big-name">任务 #{{ detail.id }}</h2>
              <span class="djsbadge" :class="typeBadgeClass(detail.taskType)">{{ typeLabel(detail.taskType) }}</span>
              <span class="djsbadge" :class="statusClass(detail.status)">{{ statusLabel(detail.status) }}</span>
              <span class="admin-readonly">管理员 · 只读视图</span>
            </div>
            <div class="meta-row">
              <span class="meta-item">
                <span style="color:var(--text-3);font-size:12px">用户</span>
                <span style="color:var(--text-1);font-weight:600">{{ detail.username || '—' }}</span>
                <span class="mono" style="font-size:11px;color:var(--text-3)">#{{ detail.userId }}</span>
              </span>
              <span class="meta-sep"></span>
              <span class="meta-item">
                <span style="color:var(--text-3);font-size:12px">提交</span>
                <span class="mono" style="color:var(--text-2);font-size:12px">{{ formatDate(detail.createdAt) }}</span>
              </span>
              <span v-if="detail.completedAt" class="meta-sep"></span>
              <span v-if="detail.completedAt" class="meta-item">
                <span style="color:var(--text-3);font-size:12px">完成</span>
                <span class="mono" style="color:var(--text-2);font-size:12px">{{ formatDate(detail.completedAt) }}</span>
              </span>
            </div>
          </div>
        </div>

        <div class="djscard" style="margin-bottom:14px">
          <div class="sec-title">任务信息</div>
          <div class="kv-grid">
            <div class="kv"><span class="k">任务 ID</span><span class="v mono">#{{ detail.id }}</span></div>
            <div class="kv"><span class="k">类型</span><span class="v">{{ typeLabel(detail.taskType) }}</span></div>
            <div class="kv"><span class="k">状态</span><span class="v">{{ statusLabel(detail.status) }}</span></div>
            <div class="kv"><span class="k">AI 模型</span><span class="v mono" style="font-size:12px">{{ detail.aiModel || '—' }}</span></div>
            <div class="kv"><span class="k">组织 ID</span><span class="v mono">{{ detail.orgId ?? '—' }}</span></div>
            <div class="kv"><span class="k">用户</span><span class="v">{{ detail.username || '—' }} <span class="mono" style="color:var(--text-3);font-size:11px">#{{ detail.userId }}</span></span></div>

            <template v-if="detail.taskType === 'file_analysis'">
              <div class="kv"><span class="k">文件名</span><span class="v">{{ detail.fileName || '—' }}</span></div>
              <div class="kv"><span class="k">文件大小</span><span class="v mono">{{ formatBytes(detail.fileSize) }}</span></div>
              <div class="kv"><span class="k">文件时长</span><span class="v mono">{{ formatDuration(detail.duration) }}</span></div>
            </template>

            <template v-if="detail.taskType === 'upload'">
              <div class="kv"><span class="k">文件名</span><span class="v">{{ detail.fileName || '—' }}</span></div>
              <div class="kv"><span class="k">文件大小</span><span class="v mono">{{ formatBytes(detail.fileSize) }}</span></div>
              <div class="kv"><span class="k">分块进度</span><span class="v mono">{{ detail.uploadedParts ?? 0 }} / {{ detail.totalParts ?? '?' }}</span></div>
            </template>

            <div class="kv"><span class="k">创建时间</span><span class="v mono" style="font-size:12px">{{ formatDate(detail.createdAt) }}</span></div>
            <div class="kv"><span class="k">更新时间</span><span class="v mono" style="font-size:12px">{{ formatDate(detail.updatedAt) }}</span></div>
          </div>

          <div v-if="detail.storageKey" class="kv-wide-row">
            <span class="k">存储 Key</span>
            <span class="v mono break">{{ detail.storageKey }}</span>
          </div>
        </div>

        <div v-if="detail.errorMsg" class="djscard" style="margin-bottom:14px">
          <div class="sec-title" style="color:var(--red)">错误信息</div>
          <div class="err-block">{{ detail.errorMsg }}</div>
        </div>

        <div v-if="detail.asrText" class="djscard" style="margin-bottom:14px">
          <div class="sec-title">ASR 转写文本</div>
          <pre class="code-block">{{ detail.asrText }}</pre>
        </div>
      </template>
    </template>
  </div>
</template>

<style scoped>
.top-nav-wrap { padding: 0 4px 12px }
.back-link { color:var(--text-3); font-size:13px; cursor:pointer; transition:color .15s }
.back-link:hover { color:var(--brand) }

.detail-header { display:flex; align-items:center; gap:18px; padding:18px 22px }
.task-icon {
  width:56px; height:56px; border-radius:var(--radius-lg);
  background:linear-gradient(140deg, var(--brand-lighter) 0%, var(--brand) 100%);
  color:#fff; display:flex; align-items:center; justify-content:center;
  box-shadow: 0 3px 10px rgba(184,130,58,.35), inset 0 1px 0 rgba(255,255,255,.35);
  flex-shrink:0;
}
.big-name { margin:0; font-family:var(--fd); font-size:20px; font-weight:400; color:var(--text-1); letter-spacing:-.015em }
.admin-readonly {
  font-size: 10px; letter-spacing: .1em;
  color: var(--text-3); text-transform: uppercase;
  background: var(--bg-2); padding: 3px 10px;
  border-radius: var(--radius-pill); font-weight: 600;
}

.meta-row { display:flex; align-items:center; gap:10px; margin-top:8px; flex-wrap:wrap }
.meta-item { display:flex; align-items:center; gap:6px; white-space:nowrap }
.meta-sep { width:1px; height:14px; background:var(--line-3); opacity:.5 }

.sec-title {
  padding:14px 18px;
  font-size:11px; font-weight:700; letter-spacing:.15em;
  text-transform:uppercase; color:var(--text-3);
  border-bottom:1px solid var(--line);
  display:flex; align-items:center; gap:8px;
}
.sec-title::before {
  content:''; width:3px; height:3px; border-radius:50%;
  background:var(--brand); box-shadow:0 0 3px rgba(184,130,58,.5);
}

.kv-grid { padding:16px 20px; display:grid; grid-template-columns:repeat(2, 1fr); gap:14px 40px }
.kv { display:flex; align-items:flex-start; gap:12px; min-height:24px }
.kv .k { width:110px; flex-shrink:0; font-size:12px; color:var(--text-3); letter-spacing:.05em }
.kv .v { flex:1; font-size:13.5px; color:var(--text-1); font-weight:500 }
.kv .v.break { word-break:break-all }

.kv-wide-row {
  padding:10px 20px 16px;
  border-top:1px dashed var(--line);
  display:flex; gap:12px; align-items:flex-start;
}
.kv-wide-row .k { width:110px; flex-shrink:0; font-size:12px; color:var(--text-3); letter-spacing:.05em; padding-top:2px }
.kv-wide-row .v { flex:1; font-size:13px; color:var(--text-1); word-break:break-all }

.mono { font-family:var(--fm) }

.code-block {
  margin:16px 20px;
  background:var(--bg-2);
  border:1px solid var(--line);
  border-radius:var(--radius-md);
  padding:14px;
  font-size:12.5px;
  font-family:var(--fm);
  color:var(--text-1);
  white-space:pre-wrap;
  word-break:break-word;
  max-height:420px; overflow:auto;
  line-height:1.65;
}

.err-block {
  margin:14px 20px;
  padding:14px 16px;
  background:var(--red-soft);
  border:1px solid rgba(184,68,60,.22);
  border-radius:var(--radius-md);
  color:var(--red-dark);
  font-size:13px;
  font-family:var(--fm);
  line-height:1.55;
  word-break:break-word;
}
</style>
