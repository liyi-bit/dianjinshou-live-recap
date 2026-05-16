<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { recordingDetail, taskDetail, type AdminRecordingDetail, type AdminTaskDetail, type AdminTask } from '../api';
import RecapDetailView from '../components/RecapDetailView.vue';
import { formatDateTime } from '../utils/format';

const route = useRoute();
const router = useRouter();

const id = Number(route.params.id);
const loading = ref(false);
const recording = ref<AdminRecordingDetail | null>(null);
const task = ref<AdminTaskDetail | null>(null);
const taskLoadError = ref('');

/** 在关联任务中选一个主任务：优先 status=COMPLETED 且 type 非 clip，否则最新那条 */
function pickPrimaryTask(tasks: AdminTask[]): AdminTask | null {
  if (!tasks || tasks.length === 0) return null;
  const completed = tasks.find(t => (t.status || '').toUpperCase() === 'COMPLETED' && t.subType !== 'clip');
  if (completed) return completed;
  const anyCompleted = tasks.find(t => (t.status || '').toUpperCase() === 'COMPLETED');
  if (anyCompleted) return anyCompleted;
  return tasks[0];
}

async function load() {
  loading.value = true;
  task.value = null;
  taskLoadError.value = '';
  try {
    recording.value = await recordingDetail(id);
    const primary = pickPrimaryTask(recording.value.relatedTasks || []);
    if (primary) {
      try {
        task.value = await taskDetail(primary.taskType, primary.id);
      } catch (e: any) {
        taskLoadError.value = '关联任务加载失败';
      }
    }
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="recording-detail-page">
    <!-- 返回导航（放在 page card 外） -->
    <div class="top-nav-wrap">
      <span class="back-link" @click="router.push('/recordings')">&lsaquo; 录制情况</span>
    </div>

    <div v-if="loading" style="padding:60px;text-align:center;color:var(--text-3)">加载中…</div>

    <template v-else-if="recording">
      <!-- 有关联 analysis 任务：整套 RecapDetailView -->
      <RecapDetailView
        v-if="task"
        :task="task"
        :display-name="recording.username || recording.localFileName || `录制 #${recording.id}`"
        :start-time="recording.startTime"
        :end-time="recording.endTime"
        :duration="recording.duration"
        :streamer-id="recording.streamerId"
      />

      <!-- 无关联任务：只展示录制基础信息 -->
      <div v-else class="no-task-fallback">
        <div class="fallback-notice">
          <span class="fb-icon">ℹ</span>
          此录制暂无关联的 AI 分析任务，仅展示录制基础信息。
          <span v-if="taskLoadError" style="color:var(--red);margin-left:8px">{{ taskLoadError }}</span>
        </div>

        <div class="djscard" style="margin-bottom:14px">
          <div class="sec-title">录制基础信息</div>
          <div class="kv-grid">
            <div class="kv"><span class="k">ID</span><span class="v mono">#{{ recording.id }}</span></div>
            <div class="kv"><span class="k">用户</span><span class="v">{{ recording.username || '—' }} <span class="mono" style="color:var(--text-3)">#{{ recording.userId }}</span></span></div>
            <div class="kv"><span class="k">主播 ID</span><span class="v mono">{{ recording.streamerId ?? '—' }}</span></div>
            <div class="kv"><span class="k">组织 ID</span><span class="v mono">{{ recording.orgId ?? '—' }}</span></div>
            <div class="kv"><span class="k">开始时间</span><span class="v mono" style="font-size:12px">{{ formatDateTime(recording.startTime) }}</span></div>
            <div class="kv"><span class="k">结束时间</span><span class="v mono" style="font-size:12px">{{ formatDateTime(recording.endTime) }}</span></div>
            <div class="kv"><span class="k">录制状态</span><span class="v">{{ recording.status || '—' }}</span></div>
            <div class="kv"><span class="k">分析状态</span><span class="v">{{ recording.analysisStatus || '—' }}</span></div>
          </div>
        </div>

        <div v-if="recording.errorMsg" class="djscard" style="margin-bottom:14px">
          <div class="sec-title" style="color:var(--red)">错误信息</div>
          <div class="err-block">{{ recording.errorMsg }}</div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.recording-detail-page {
  display: flex; flex-direction: column;
  gap: 0;
}

.top-nav-wrap {
  padding: 0 4px 12px;
}
.back-link {
  color:var(--text-3); font-size:13px; cursor:pointer;
  transition:color .15s;
}
.back-link:hover { color:var(--brand) }

.no-task-fallback { padding: 0 }
.fallback-notice {
  display:flex; align-items:center; gap:8px;
  padding: 12px 16px;
  background: var(--blue2-soft);
  border: 1px solid rgba(74,104,150,.22);
  border-radius: var(--radius-md);
  color: var(--text-2);
  font-size: 13px;
  margin-bottom: 14px;
}
.fb-icon {
  width:22px; height:22px; border-radius:50%;
  background: var(--blue2); color:#fff;
  display:flex; align-items:center; justify-content:center;
  font-size:13px; font-weight:700; flex-shrink:0;
}

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
.mono { font-family:var(--fm) }

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
