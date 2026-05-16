<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { userDetail, userRelatedCounts, type AdminUserDetail, type UserRelatedCounts } from '../api';
import { formatDateTime } from '../utils/format';

const route = useRoute();
const router = useRouter();

const id = Number(route.params.id);
const loading = ref(false);
const detail = ref<AdminUserDetail | null>(null);
const counts = ref<UserRelatedCounts | null>(null);

async function load() {
  loading.value = true;
  try {
    const [d, c] = await Promise.all([userDetail(id), userRelatedCounts(id)]);
    detail.value = d;
    counts.value = c;
  } finally {
    loading.value = false;
  }
}

function roleLabel(r: string) {
  return { super_admin: '超管', admin: '管理员', operator: '运营', anchor: '主播' }[r] || r;
}
function roleBadgeClass(r: string) {
  return { super_admin: 'red', admin: 'amber', operator: 'blue', anchor: 'green' }[r] || 'blue';
}
function vipLabel(l: number) { return l ? `VIP${l}` : '免费' }

function formatDate(s: string | null) {
  return formatDateTime(s);
}

function goRecordings() {
  router.push({ path: '/recordings', query: { userId: String(id) } });
}
function goTasks() {
  router.push({ path: '/tasks', query: { userId: String(id) } });
}

function quotaPct(used: number, total: number) {
  if (!total || total <= 0) return 0;
  return Math.min(100, Math.round((used / total) * 100));
}

onMounted(load);
</script>

<template>
  <div>
    <!-- 返回 -->
    <div style="margin-bottom:14px">
      <span class="back-link" @click="router.push('/users')">&lsaquo; 用户管理</span>
    </div>

    <div v-if="loading" style="padding:60px;text-align:center;color:var(--text-3)">加载中…</div>

    <template v-else-if="detail">
      <!-- Header -->
      <div class="djscard detail-header" style="margin-bottom:16px">
        <div class="avatar-big">{{ (detail.username || '?')[0].toUpperCase() }}</div>
        <div style="flex:1; min-width:0">
          <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap">
            <h2 class="big-name">{{ detail.username }}</h2>
            <span class="djsbadge" :class="roleBadgeClass(detail.role)">{{ roleLabel(detail.role) }}</span>
            <span class="djsbadge" :class="detail.status === 1 ? 'green' : 'red'">
              {{ detail.status === 1 ? '正常' : '禁用' }}
            </span>
            <span class="djsbadge amber">{{ vipLabel(detail.vipLevel) }}</span>
          </div>
          <div class="meta-row">
            <span class="meta-item">
              <span class="mono" style="color:var(--text-3)">#{{ detail.id }}</span>
            </span>
            <span class="meta-sep"></span>
            <span class="meta-item">
              <span style="color:var(--text-3);font-size:12px">手机</span>
              <span class="mono" style="color:var(--text-2);font-size:13px">{{ detail.phone || '—' }}</span>
            </span>
            <span class="meta-sep"></span>
            <span class="meta-item">
              <span style="color:var(--text-3);font-size:12px">组织</span>
              <span class="mono" style="color:var(--text-2);font-size:13px">{{ detail.orgId ?? '—' }}</span>
            </span>
            <span class="meta-sep"></span>
            <span class="meta-item">
              <span style="color:var(--text-3);font-size:12px">最后登录</span>
              <span class="mono" style="color:var(--text-2);font-size:12px">{{ formatDate(detail.lastLoginAt) }}</span>
            </span>
          </div>
        </div>
      </div>

      <!-- 关联统计 -->
      <div class="djsgrid-4" style="margin-bottom:16px">
        <div class="djsstat clickable" @click="goRecordings">
          <div class="ico blue">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="2" y="6" width="20" height="12" rx="2"/><path d="M10 10l5 3-5 3z"/></svg>
          </div>
          <div style="flex:1">
            <div class="lbl">录制数</div>
            <div class="val">{{ counts?.recordingCount ?? '—' }}</div>
          </div>
        </div>
        <div class="djsstat clickable" @click="goTasks">
          <div class="ico green">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z"/></svg>
          </div>
          <div style="flex:1">
            <div class="lbl">分析任务</div>
            <div class="val">{{ counts?.analysisTaskCount ?? '—' }}</div>
          </div>
        </div>
        <div class="djsstat clickable" @click="goTasks">
          <div class="ico amber">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><path d="M14 2v6h6"/></svg>
          </div>
          <div style="flex:1">
            <div class="lbl">文件分析</div>
            <div class="val">{{ counts?.fileAnalysisCount ?? '—' }}</div>
          </div>
        </div>
        <div class="djsstat clickable" @click="goTasks">
          <div class="ico amber">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
          </div>
          <div style="flex:1">
            <div class="lbl">上传任务</div>
            <div class="val">{{ counts?.uploadCount ?? '—' }}</div>
          </div>
        </div>
      </div>

      <!-- 详细信息 -->
      <div class="djscard" style="margin-bottom:16px">
        <div class="sec-title">账号信息</div>
        <div class="kv-grid">
          <div class="kv"><span class="k">用户 ID</span><span class="v mono">#{{ detail.id }}</span></div>
          <div class="kv"><span class="k">用户名</span><span class="v">{{ detail.username }}</span></div>
          <div class="kv"><span class="k">手机号</span><span class="v mono">{{ detail.phone || '—' }}</span></div>
          <div class="kv"><span class="k">邮箱</span><span class="v">{{ detail.email || '—' }}</span></div>
          <div class="kv"><span class="k">角色</span><span class="v">{{ roleLabel(detail.role) }}</span></div>
          <div class="kv"><span class="k">状态</span><span class="v">{{ detail.status === 1 ? '正常' : '禁用' }}</span></div>
          <div class="kv"><span class="k">组织 ID</span><span class="v mono">{{ detail.orgId ?? '—' }}</span></div>
          <div class="kv"><span class="k">微信 OpenId</span><span class="v mono" style="font-size:11px">{{ detail.wechatOpenId || '—' }}</span></div>
          <div class="kv"><span class="k">QQ OpenId</span><span class="v mono" style="font-size:11px">{{ detail.qqOpenId || '—' }}</span></div>
          <div class="kv"><span class="k">注册时间</span><span class="v mono" style="font-size:12px">{{ formatDate(detail.createdAt) }}</span></div>
          <div class="kv"><span class="k">最后登录</span><span class="v mono" style="font-size:12px">{{ formatDate(detail.lastLoginAt) }}</span></div>
          <div class="kv"><span class="k">更新时间</span><span class="v mono" style="font-size:12px">{{ formatDate(detail.updatedAt) }}</span></div>
        </div>
      </div>

      <!-- VIP 与额度 -->
      <div class="djscard" style="margin-bottom:16px">
        <div class="sec-title">VIP 与额度</div>
        <div class="kv-grid">
          <div class="kv"><span class="k">VIP 等级</span><span class="v">{{ vipLabel(detail.vipLevel) }}</span></div>
          <div class="kv"><span class="k">VIP 到期</span><span class="v mono" style="font-size:12px">{{ formatDate(detail.vipExpireAt) }}</span></div>
        </div>

        <div class="quota-block">
          <div class="quota-row">
            <span class="quota-label">AI 字数额度</span>
            <span class="quota-value mono">{{ (detail.aiQuotaUsed ?? 0).toLocaleString() }} / {{ (detail.aiQuotaTotal ?? 0).toLocaleString() }}</span>
          </div>
          <div class="quota-bar">
            <div class="quota-fill" :style="{ width: quotaPct(detail.aiQuotaUsed, detail.aiQuotaTotal) + '%' }"></div>
          </div>
        </div>

        <div class="quota-block">
          <div class="quota-row">
            <span class="quota-label">录制时长额度（秒）</span>
            <span class="quota-value mono">{{ (detail.durationQuotaUsed ?? 0).toLocaleString() }} / {{ (detail.durationQuotaTotal ?? 0).toLocaleString() }}</span>
          </div>
          <div class="quota-bar">
            <div class="quota-fill green" :style="{ width: quotaPct(detail.durationQuotaUsed, detail.durationQuotaTotal) + '%' }"></div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.back-link {
  color:var(--text-3); font-size:13px; cursor:pointer;
  transition:color .15s;
}
.back-link:hover { color:var(--brand) }

.detail-header {
  display:flex; align-items:center; gap:20px; padding:22px 24px;
}
.avatar-big {
  width:68px; height:68px; border-radius:50%;
  background:
    radial-gradient(circle at 30% 25%, #E8BE74 0%, transparent 45%),
    linear-gradient(140deg, #D09E4E 0%, #8F6224 100%);
  color:#fff; display:flex; align-items:center; justify-content:center;
  font-family:var(--fd); font-style:italic; font-size:30px; font-weight:700;
  box-shadow:
    inset 0 1px 2px rgba(255,255,255,.35),
    inset 0 -2px 2px rgba(74,49,15,.25),
    0 3px 8px rgba(143,98,36,.35),
    0 0 0 3px rgba(184,130,58,.08);
  flex-shrink:0; letter-spacing:-.02em;
}
.big-name {
  margin:0; font-family:var(--fd); font-size:24px; font-weight:400;
  color:var(--text-1); letter-spacing:-.015em;
}
.meta-row {
  display:flex; align-items:center; gap:10px; margin-top:10px;
  flex-wrap:wrap;
}
.meta-item { display:flex; align-items:center; gap:6px; white-space:nowrap }
.meta-sep {
  width:1px; height:14px; background:var(--line-3); opacity:.5;
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

.kv-grid {
  padding:16px 20px;
  display:grid; grid-template-columns:repeat(2, 1fr); gap:14px 40px;
}
.kv {
  display:flex; align-items:flex-start; gap:12px;
  min-height:24px;
}
.kv .k {
  width:110px; flex-shrink:0;
  font-size:12px; color:var(--text-3);
  letter-spacing:.05em;
}
.kv .v {
  flex:1; font-size:13.5px; color:var(--text-1);
  word-break:break-all; font-weight:500;
}

.mono { font-family:var(--fm) }

.quota-block { padding:14px 20px; border-top:1px dashed var(--line) }
.quota-row {
  display:flex; justify-content:space-between; align-items:baseline;
  margin-bottom:8px;
}
.quota-label { font-size:12.5px; color:var(--text-2); font-weight:500 }
.quota-value { font-size:13px; color:var(--text-1); font-weight:600 }
.quota-bar {
  height:6px; background:var(--bg-2); border-radius:var(--radius-pill);
  overflow:hidden;
}
.quota-fill {
  height:100%;
  background:linear-gradient(90deg, var(--brand-lighter), var(--brand));
  border-radius:var(--radius-pill);
  transition:width .4s var(--ease);
}
.quota-fill.green {
  background:linear-gradient(90deg, var(--green-light), var(--green));
}

.djsstat.clickable { cursor:pointer }
</style>
