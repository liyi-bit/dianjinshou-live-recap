<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import {
  listTasks,
  taskStats,
  type AdminTask,
  type AdminTaskStats,
  type PageResult
} from '../api';
import Pagination from '../components/common/Pagination.vue';
import AnalysisStatus from '../components/AnalysisStatus.vue';
import { formatDateTime } from '../utils/format';

const router = useRouter();

const loading = ref(false);
const data = ref<PageResult<AdminTask>>({ items: [], total: 0, page: 1, size: 20 });
const stats = ref<AdminTaskStats | null>(null);

const filter = reactive({
  userPhone: '',
  status: '',
  startDate: '',
  endDate: '',
  page: 1,
  size: 20
});

const statusBreakdown = computed(() => {
  if (!stats.value?.byStatus) return [];
  return Object.entries(stats.value.byStatus).sort((a, b) => b[1] - a[1]);
});

async function loadStats() {
  try { stats.value = await taskStats(); } catch (_) { /* ignore */ }
}

async function load() {
  loading.value = true;
  try {
    data.value = await listTasks({
      page: filter.page,
      size: filter.size,
      userPhone: filter.userPhone || undefined,
      status: filter.status || undefined,
      start: filter.startDate ? `${filter.startDate} 00:00:00` : undefined,
      end: filter.endDate ? `${filter.endDate} 23:59:59` : undefined
    });
  } finally {
    loading.value = false;
  }
}

function doSearch() { filter.page = 1; load(); }

function reset() {
  filter.userPhone = '';
  filter.status = '';
  filter.startDate = '';
  filter.endDate = '';
  filter.page = 1;
  load();
}

function onPageChange(p: number) { filter.page = p; load(); }

function openDetail(t: AdminTask) {
  router.push(`/tasks/${t.taskType}/${t.id}`);
}

function formatDate(s: string | null) {
  return formatDateTime(s);
}

onMounted(() => { load(); loadStats(); });
</script>

<template>
  <div>
    <!-- 统计卡 -->
    <div class="djsgrid-3" style="margin-bottom:16px">
      <div class="djsstat">
        <div class="ico blue">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z" /></svg>
        </div>
        <div style="flex:1">
          <div class="lbl">任务总数</div>
          <div class="val">
            {{ (stats?.byType?.analysis ?? 0) + (stats?.byType?.file_analysis ?? 0) + (stats?.byType?.upload ?? 0) }}
          </div>
        </div>
      </div>
      <div class="djsstat">
        <div class="ico green">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" /><polyline points="22 4 12 14.01 9 11.01" /></svg>
        </div>
        <div style="flex:1">
          <div class="lbl">已完成 / 今日</div>
          <div class="val">{{ stats?.completedCount ?? 0 }} <span class="u">/ {{ stats?.todayCount ?? 0 }}</span></div>
        </div>
      </div>
      <div class="djsstat">
        <div class="ico amber">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" /></svg>
        </div>
        <div style="flex:1">
          <div class="lbl">失败 / 待处理</div>
          <div class="val">{{ stats?.failedCount ?? 0 }} <span class="u">/ {{ stats?.pendingCount ?? 0 }}</span></div>
        </div>
      </div>
    </div>

    <!-- 状态分布 -->
    <div v-if="statusBreakdown.length" class="djscard" style="margin-bottom:16px; padding:14px 16px">
      <div style="font-size:11px;font-weight:700;letter-spacing:.15em;text-transform:uppercase;color:var(--text-4);margin-bottom:10px">各状态分布</div>
      <div style="display:flex;flex-wrap:wrap;gap:8px">
        <span v-for="[s, n] in statusBreakdown" :key="s" class="count-chip">
          <AnalysisStatus :status="s" /> <span class="count-num mono">{{ n }}</span>
        </span>
      </div>
    </div>

    <!-- 列表 -->
    <div class="djscard" style="margin-bottom:16px">
      <div class="djsfilter">
        <div class="sch" style="width:200px">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" /></svg>
          <input type="text" placeholder="用户手机号" v-model="filter.userPhone" @keyup.enter="doSearch">
        </div>
        <span class="fl-label">分析状态</span>
        <select class="djsselect" style="width:150px" v-model="filter.status">
          <option value="">全部</option>
          <option value="pending">待处理</option>
          <option value="PENDING">待处理 (大写)</option>
          <option value="processing">处理中</option>
          <option value="asr_processing">ASR 中</option>
          <option value="transcribing">转写中</option>
          <option value="transcribed">已转写</option>
          <option value="ai_processing">AI 中</option>
          <option value="completed">已完成</option>
          <option value="COMPLETED">已完成 (大写)</option>
          <option value="failed">失败</option>
          <option value="FAILED">失败 (大写)</option>
        </select>
        <span class="fl-label">开始</span>
        <input type="date" class="djsselect" style="width:140px" v-model="filter.startDate">
        <span class="fl-label">结束</span>
        <input type="date" class="djsselect" style="width:140px" v-model="filter.endDate">
        <button class="djsbtn primary" style="height:30px;padding:0 16px;font-size:12px" @click="doSearch">查询</button>
        <div class="spacer"></div>
        <button class="djsbtn text" @click="reset">重置</button>
      </div>

      <table class="djstbl" v-if="data.items.length > 0">
        <thead>
          <tr>
            <th style="width:80px">ID</th>
            <th>主播</th>
            <th>用户</th>
            <th>手机号</th>
            <th>分析状态</th>
            <th>AI 模型</th>
            <th>提交时间</th>
            <th>完成时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in data.items" :key="`${t.taskType}-${t.id}`" class="row-clickable" @click="openDetail(t)">
            <td><span class="mono">#{{ t.id }}</span></td>
            <td>
              <div v-if="t.streamerName || t.streamerAvatar" style="display:flex;align-items:center;gap:10px">
                <img v-if="t.streamerAvatar" :src="t.streamerAvatar" class="st-avatar" referrerpolicy="no-referrer" />
                <div v-else class="st-avatar-fallback">{{ (t.streamerName || '?')[0] }}</div>
                <div style="min-width:0">
                  <div style="font-size:13.5px;font-weight:600;color:var(--text-1);line-height:1.2">{{ t.streamerName || '—' }}</div>
                  <div class="mono" style="font-size:11px;color:var(--text-3)">#{{ t.streamerId ?? '—' }}</div>
                </div>
              </div>
              <span v-else style="color:var(--text-3)">—</span>
            </td>
            <td>
              <div style="display:flex;flex-direction:column">
                <span style="font-weight:500;color:var(--text-1);font-size:13px">{{ t.username || '—' }}</span>
                <span class="mono" style="font-size:11px;color:var(--text-3)">#{{ t.userId }}</span>
              </div>
            </td>
            <td><span class="mono" style="color:var(--text-2)">{{ t.userPhone || '—' }}</span></td>
            <td><AnalysisStatus :status="t.status" /></td>
            <td><span class="mono" style="color:var(--text-3);font-size:12px">{{ t.aiModel || '—' }}</span></td>
            <td><span class="mono" style="font-size:12px;color:var(--text-2)">{{ formatDate(t.createdAt) }}</span></td>
            <td><span class="mono" style="font-size:12px;color:var(--text-3)">{{ formatDate(t.completedAt) }}</span></td>
          </tr>
        </tbody>
      </table>

      <div v-else-if="!loading" class="empty-row">
        <div class="empty-ico">🤖</div>
        <div class="empty-txt">暂无任务</div>
      </div>

      <div v-if="loading" style="padding:30px;text-align:center;color:var(--text-3)">加载中…</div>

      <Pagination
        v-if="data.total > 0"
        :page="data.page"
        :size="data.size"
        :total="data.total"
        @change="onPageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.row-clickable { cursor:pointer }
.row-clickable:hover td { background:var(--hov) }
.empty-row { padding:60px 20px; text-align:center }
.empty-ico { font-size:40px; opacity:.5; margin-bottom:8px }
.empty-txt { color:var(--text-3); font-size:13px }
.mono { font-family:var(--fm); font-size:13px }

.st-avatar {
  width:32px; height:32px; border-radius:8px;
  object-fit:cover; flex-shrink:0;
  background: var(--bg-2);
  border: 1px solid var(--line);
}
.st-avatar-fallback {
  width:32px; height:32px; border-radius:8px;
  background: linear-gradient(140deg, #D09E4E 0%, #8F6224 100%);
  color:#fff; font-weight:700; font-size:14px;
  display:flex; align-items:center; justify-content:center;
  flex-shrink:0;
}

.count-chip {
  display:inline-flex; align-items:center; gap:6px;
  padding:3px 10px; background:var(--bg-2);
  border:1px solid var(--line); border-radius:var(--radius-pill);
}
.count-num {
  font-weight:700; color:var(--text-1); font-size:12px;
}
</style>
