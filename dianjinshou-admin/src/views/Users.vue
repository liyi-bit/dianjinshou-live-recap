<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { listUsers, dashboardStats, type AdminUser, type DashboardStats, type PageResult } from '../api';
import Pagination from '../components/common/Pagination.vue';
import { formatDateTime } from '../utils/format';

const router = useRouter();

const loading = ref(false);
const data = ref<PageResult<AdminUser>>({ items: [], total: 0, page: 1, size: 20 });
const stats = ref<DashboardStats | null>(null);
const statsLoading = ref(false);

const filter = reactive({
  keyword: '',
  status: undefined as number | undefined,
  page: 1,
  size: 20
});

async function loadStats() {
  statsLoading.value = true;
  try { stats.value = await dashboardStats(); } finally { statsLoading.value = false; }
}

async function load() {
  loading.value = true;
  try {
    data.value = await listUsers({
      page: filter.page,
      size: filter.size,
      keyword: filter.keyword || undefined,
      status: filter.status
    });
  } finally {
    loading.value = false;
  }
}

function doSearch() { filter.page = 1; load(); }

function reset() {
  filter.keyword = '';
  filter.status = undefined;
  filter.page = 1;
  load();
}

function onPageChange(p: number) { filter.page = p; load(); }

function openDetail(u: AdminUser) {
  router.push(`/users/${u.id}`);
}

function formatDate(s: string | null) {
  return formatDateTime(s);
}

onMounted(() => { load(); loadStats(); });
</script>

<template>
  <div>
    <!-- 1. 顶部统计 -->
    <div class="djsgrid-3" style="margin-bottom:16px">
      <div class="djsstat">
        <div class="ico blue">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75"/></svg>
        </div>
        <div style="flex:1">
          <div class="lbl">注册用户总数</div>
          <div class="val">{{ stats?.totalUsers ?? '—' }}</div>
        </div>
      </div>
      <div class="djsstat">
        <div class="ico green">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M20 21v-2a4 4 0 00-3-3.87"/><path d="M4 21v-2a4 4 0 013-3.87"/><circle cx="12" cy="7" r="4"/><line x1="18" y1="8" x2="23" y2="8"/><line x1="20.5" y1="5.5" x2="20.5" y2="10.5"/></svg>
        </div>
        <div style="flex:1">
          <div class="lbl">近 24 小时新增</div>
          <div class="val">{{ stats?.todayNewUsers ?? '—' }}</div>
        </div>
      </div>
      <div class="djsstat">
        <div class="ico amber">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
        </div>
        <div style="flex:1">
          <div class="lbl">近 24 小时活跃</div>
          <div class="val">{{ stats?.todayActive ?? '—' }}</div>
        </div>
      </div>
    </div>

    <!-- 2. 列表 -->
    <div class="djscard" style="margin-bottom:16px">
      <div class="djsfilter">
        <div class="sch">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input type="text" placeholder="用户名 / 手机号" v-model="filter.keyword" @keyup.enter="doSearch">
        </div>
        <button class="djsbtn primary" style="height:30px;padding:0 16px;font-size:12px" @click="doSearch">查询</button>
        <span class="div"></span>
        <span class="fl-label">状态</span>
        <select class="djsselect" style="width:100px" v-model="filter.status">
          <option :value="undefined">全部</option>
          <option :value="1">正常</option>
          <option :value="0">禁用</option>
        </select>
        <div class="spacer"></div>
        <button class="djsbtn text" @click="reset">重置</button>
      </div>

      <table class="djstbl" v-if="data.items.length > 0">
        <thead>
          <tr>
            <th style="width:80px">ID</th>
            <th>用户</th>
            <th>手机号</th>
            <th style="text-align:right">主播数</th>
            <th style="text-align:right">录制数</th>
            <th style="text-align:right">今日主播</th>
            <th style="text-align:right">今日录制</th>
            <th style="text-align:right">今日 AI</th>
            <th>状态</th>
            <th>注册时间</th>
            <th>最后登录</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in data.items" :key="u.id" class="row-clickable" @click="openDetail(u)">
            <td><span class="mono">#{{ u.id }}</span></td>
            <td>
              <div style="display:flex;align-items:center;gap:10px">
                <div class="avatar-mini">{{ (u.username || '?')[0].toUpperCase() }}</div>
                <div style="font-size:14px;font-weight:600;color:var(--text-1);letter-spacing:-.008em">{{ u.username || '—' }}</div>
              </div>
            </td>
            <td><span class="mono" style="color:var(--text-2)">{{ u.phone || '—' }}</span></td>
            <td style="text-align:right"><span class="mono num-cell">{{ u.streamerCount ?? 0 }}</span></td>
            <td style="text-align:right"><span class="mono num-cell">{{ u.recordingCount ?? 0 }}</span></td>
            <td style="text-align:right"><span class="mono num-cell" :class="{ today: (u.todayStreamerCount ?? 0) > 0 }">{{ u.todayStreamerCount ?? 0 }}</span></td>
            <td style="text-align:right"><span class="mono num-cell" :class="{ today: (u.todayRecordingCount ?? 0) > 0 }">{{ u.todayRecordingCount ?? 0 }}</span></td>
            <td style="text-align:right">
              <span v-if="u.dailyAiUnlimited" class="mono num-cell" style="color:var(--brand);font-weight:700">∞</span>
              <span v-else class="mono num-cell" :class="{ today: (u.dailyAiUsed ?? 0) > 0 }" :title="`今日已使用 ${u.dailyAiUsed ?? 0} 次，每日限额 ${u.dailyAiLimit ?? 10} 次`">
                {{ u.dailyAiUsed ?? 0 }}<span style="color:var(--text-3);font-weight:500">/{{ u.dailyAiLimit ?? 10 }}</span>
              </span>
            </td>
            <td>
              <span class="djsbadge" :class="u.status === 1 ? 'green' : 'red'">
                {{ u.status === 1 ? '正常' : '禁用' }}
              </span>
            </td>
            <td><span class="mono" style="font-size:12px;color:var(--text-3)">{{ formatDate(u.createdAt) }}</span></td>
            <td><span class="mono" style="font-size:12px;color:var(--text-3)">{{ formatDate(u.lastLoginAt) }}</span></td>
          </tr>
        </tbody>
      </table>

      <div v-else-if="!loading" class="empty-row">
        <div class="empty-ico">📋</div>
        <div class="empty-txt">暂无用户</div>
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

.avatar-mini {
  width:30px; height:30px; border-radius:50%;
  background:
    radial-gradient(circle at 30% 25%, #E8BE74 0%, transparent 45%),
    linear-gradient(140deg, #D09E4E 0%, #8F6224 100%);
  color:#fff; display:flex; align-items:center; justify-content:center;
  font-family:var(--fm); font-size:12px; font-weight:700;
  box-shadow: 0 1px 3px rgba(143,98,36,.35), inset 0 1px 0 rgba(255,255,255,.35);
  flex-shrink:0;
}

.empty-row { padding:60px 20px; text-align:center }
.empty-ico { font-size:40px; opacity:.5; margin-bottom:8px }
.empty-txt { color:var(--text-3); font-size:13px }
.mono { font-family:var(--fm); font-size:13px }

.num-cell {
  font-weight: 600; color: var(--text-1);
}
.num-cell.today {
  color: var(--brand);
}
</style>
