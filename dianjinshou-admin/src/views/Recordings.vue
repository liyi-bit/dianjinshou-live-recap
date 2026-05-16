<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import {
  listRecordings,
  recordingStats,
  type AdminRecording,
  type AdminRecordingStats,
  type PageResult
} from '../api';
import { formatBytes, formatDateTime, formatDuration, formatDurationCompact } from '../utils/format';
import Pagination from '../components/common/Pagination.vue';
import AnalysisStatus from '../components/AnalysisStatus.vue';

const router = useRouter();

const loading = ref(false);
const data = ref<PageResult<AdminRecording>>({ items: [], total: 0, page: 1, size: 20 });
const stats = ref<AdminRecordingStats | null>(null);

const filter = reactive({
  userPhone: '',
  analysisStatus: '',
  startDate: '',
  endDate: '',
  page: 1,
  size: 20
});

async function loadStats() {
  try { stats.value = await recordingStats(); } catch (_) { /* ignore */ }
}

async function load() {
  loading.value = true;
  try {
    data.value = await listRecordings({
      page: filter.page,
      size: filter.size,
      userPhone: filter.userPhone || undefined,
      analysisStatus: filter.analysisStatus || undefined,
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
  filter.analysisStatus = '';
  filter.startDate = '';
  filter.endDate = '';
  filter.page = 1;
  load();
}

function onPageChange(p: number) { filter.page = p; load(); }

function openDetail(r: AdminRecording) {
  router.push(`/recordings/${r.id}`);
}

function formatDate(s: string | null) {
  return formatDateTime(s);
}

onMounted(() => { load(); loadStats(); });
</script>

<template>
  <div>
    <!-- 统计卡 -->
    <div class="djsgrid-4" style="margin-bottom:16px">
      <div class="djsstat">
        <div class="ico blue">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="2" y="6" width="20" height="12" rx="2" /><path d="M10 10l5 3-5 3z" /></svg>
        </div>
        <div style="flex:1">
          <div class="lbl">录制总数</div>
          <div class="val">{{ stats?.total ?? '—' }}</div>
        </div>
      </div>
      <div class="djsstat">
        <div class="ico green">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" /></svg>
        </div>
        <div style="flex:1">
          <div class="lbl">今日 / 近 7 日</div>
          <div class="val">{{ stats?.todayCount ?? 0 }} <span class="u">/ {{ stats?.weekCount ?? 0 }}</span></div>
        </div>
      </div>
      <div class="djsstat">
        <div class="ico amber">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" /></svg>
        </div>
        <div style="flex:1">
          <div class="lbl">总时长</div>
          <div class="val" style="font-size:18px">{{ formatDurationCompact(stats?.totalDurationSec) }}</div>
        </div>
      </div>
      <div class="djsstat">
        <div class="ico amber">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4" /><polyline points="17 8 12 3 7 8" /><line x1="12" y1="3" x2="12" y2="15" /></svg>
        </div>
        <div style="flex:1">
          <div class="lbl">总文件大小</div>
          <div class="val" style="font-size:18px">{{ formatBytes(stats?.totalFileBytes) }}</div>
        </div>
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
        <select class="djsselect" style="width:130px" v-model="filter.analysisStatus">
          <option value="">全部</option>
          <option value="none">未开始</option>
          <option value="pending">待处理</option>
          <option value="recording">录制中</option>
          <option value="transcribing">转写中</option>
          <option value="transcribed">已转写</option>
          <option value="asr_processing">ASR 中</option>
          <option value="ai_processing">AI 中</option>
          <option value="completed">已完成</option>
          <option value="failed">失败</option>
          <option value="skipped">跳过</option>
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
            <th style="width:70px">ID</th>
            <th>主播</th>
            <th>用户</th>
            <th>手机号</th>
            <th>开始时间</th>
            <th>时长</th>
            <th>大小</th>
            <th>分析状态</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in data.items" :key="r.id" class="row-clickable" @click="openDetail(r)">
            <td><span class="mono">#{{ r.id }}</span></td>
            <td>
              <div v-if="r.streamerName || r.streamerAvatar" style="display:flex;align-items:center;gap:10px">
                <img v-if="r.streamerAvatar" :src="r.streamerAvatar" class="st-avatar" referrerpolicy="no-referrer" />
                <div v-else class="st-avatar-fallback">{{ (r.streamerName || '?')[0] }}</div>
                <div style="min-width:0">
                  <div style="font-size:13.5px;font-weight:600;color:var(--text-1);line-height:1.2">{{ r.streamerName || '—' }}</div>
                  <div class="mono" style="font-size:11px;color:var(--text-3)">#{{ r.streamerId ?? '—' }}</div>
                </div>
              </div>
              <span v-else style="color:var(--text-3)">—</span>
            </td>
            <td>
              <div style="display:flex;flex-direction:column">
                <span style="font-weight:500;color:var(--text-1);font-size:13px">{{ r.username || '—' }}</span>
                <span class="mono" style="font-size:11px;color:var(--text-3)">#{{ r.userId }}</span>
              </div>
            </td>
            <td><span class="mono" style="color:var(--text-2)">{{ r.userPhone || '—' }}</span></td>
            <td><span class="mono" style="font-size:12px;color:var(--text-2)">{{ formatDate(r.startTime) }}</span></td>
            <td><span class="mono" style="font-weight:650">{{ formatDuration(r.duration) }}</span></td>
            <td><span class="mono" style="color:var(--text-2)">{{ formatBytes(r.fileSize) }}</span></td>
            <td><AnalysisStatus :status="r.analysisStatus" /></td>
          </tr>
        </tbody>
      </table>

      <div v-else-if="!loading" class="empty-row">
        <div class="empty-ico">📹</div>
        <div class="empty-txt">暂无录制</div>
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
</style>
