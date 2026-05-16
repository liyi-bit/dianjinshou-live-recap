<script setup lang="ts">
import { computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { adminLogout } from '../api/adminAuth';
import { Message } from '@arco-design/web-vue';

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();

const activeKey = computed(() => {
  const p = route.path;
  if (p.startsWith('/users')) return 'users';
  if (p.startsWith('/recordings')) return 'recordings';
  if (p.startsWith('/tasks')) return 'tasks';
  return 'users';
});

const breadcrumb = computed(() => {
  const p = route.path;
  if (p.startsWith('/users/')) return '用户管理 / <b>用户详情</b>';
  if (p === '/users') return '用户管理';
  if (p.startsWith('/recordings/')) return '录制情况 / <b>录制详情</b>';
  if (p === '/recordings') return '录制情况';
  if (p.startsWith('/tasks/')) return '任务情况 / <b>任务详情</b>';
  if (p === '/tasks') return '任务情况';
  return '';
});

function go(path: string) { router.push(path); }

async function logout() {
  try { await adminLogout(); } catch (_) { /* ignore */ }
  auth.clear();
  Message.success('已退出');
  router.push('/login');
}

const roleLabel = computed(() => {
  if (auth.user?.role === 'admin_super') return '超级管理员';
  if (auth.user?.role === 'admin_normal') return '管理员';
  return '';
});
</script>

<template>
  <div class="main-layout">
    <div class="main-layout__body">
      <!-- Sidebar -->
      <div class="sidebar">
        <div class="logo">
          <div class="logo-mark">金</div>
          <div class="logo-text">
            <div class="name">点金手后台</div>
            <span class="ver">ADMIN · 控制台</span>
          </div>
        </div>

        <nav class="nav">
          <div class="nav-group">数据管理</div>

          <div class="nav-item" :class="{ active: activeKey === 'users' }" @click="go('/users')">
            <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 4a4 4 0 100 8 4 4 0 000-8z" />
              <path d="M4 20c0-4 4-6 8-6s8 2 8 6" />
            </svg>
            用户管理
          </div>

          <div class="nav-item" :class="{ active: activeKey === 'recordings' }" @click="go('/recordings')">
            <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 5a2 2 0 012-2h12a2 2 0 012 2v11a2 2 0 01-2 2H6a2 2 0 01-2-2V5z" />
              <path d="M10 9l5 3-5 3V9z" />
            </svg>
            录制情况
          </div>

          <div class="nav-item" :class="{ active: activeKey === 'tasks' }" @click="go('/tasks')">
            <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 4h16v16H4z" />
              <path d="M9 9h6v6H9z" />
            </svg>
            任务情况
          </div>
        </nav>

        <div class="sidebar-footer">
          <div class="sb-ft-row">
            <span class="sb-ft-lbl">只读模式</span>
          </div>
          <div class="ver-info">
            <span class="ver-dot">管理员只读视图</span>
          </div>
        </div>
      </div>

      <!-- Main -->
      <div class="main-content">
        <div class="topbar">
          <div class="breadcrumb" v-html="breadcrumb" />
          <div class="spacer" />
          <div class="hd-user">
            <div class="avatar">{{ (auth.user?.displayName || auth.user?.username || 'A')[0].toUpperCase() }}</div>
            <div class="user-meta">
              <span class="username">{{ auth.user?.displayName || auth.user?.username }}</span>
              <span class="role">{{ roleLabel }}</span>
            </div>
          </div>
          <span class="logout-btn" @click="logout">退出</span>
        </div>

        <div class="main-content__page">
          <router-view />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.main-layout { display:flex; flex-direction:column; height:100vh; background:var(--bg) }
.main-layout__body { display:flex; flex:1; overflow:hidden }

.sidebar {
  width:var(--sidebar-width); height:100%;
  background:linear-gradient(180deg,#FDFCF8 0%,#F9F6EE 60%,#F4F0E3 100%);
  border-right:1px solid var(--line);
  display:flex; flex-direction:column; flex-shrink:0;
  box-shadow:inset -1px 0 0 rgba(253,252,248,.6);
  position:relative;
}
.sidebar::before {
  content:''; position:absolute; top:0; right:0; bottom:0; width:1px;
  background:linear-gradient(180deg,transparent,rgba(184,130,58,.08) 20%,rgba(184,130,58,.08) 80%,transparent);
  pointer-events:none;
}

.logo {
  padding:20px 16px 18px; display:flex; align-items:center; gap:11px;
  border-bottom:1px solid var(--line); position:relative;
}
.logo::after {
  content:''; position:absolute; left:18px; right:18px; bottom:-1px; height:1px;
  background:linear-gradient(90deg,transparent,var(--line) 20%,var(--line) 80%,transparent);
}
.logo-mark {
  width:40px; height:40px; border-radius:11px;
  background:
    radial-gradient(circle at 30% 25%,#F4D99B 0%,transparent 45%),
    linear-gradient(140deg,#E8BE74 0%,#B8823A 55%,#8F6224 100%);
  color:#FDFCF8; display:flex; align-items:center; justify-content:center;
  font-family:var(--fd); font-style:italic; font-size:24px;
  box-shadow:
    inset 0 1px 1.5px rgba(255,255,255,.55),
    inset 0 -1.5px 1.5px rgba(74,49,15,.35),
    inset 0 0 0 .5px rgba(74,49,15,.2),
    0 1px 2px rgba(74,49,15,.25),
    0 4px 12px rgba(184,130,58,.32),
    0 0 0 3px rgba(184,130,58,.06);
  position:relative; overflow:hidden; flex-shrink:0;
  letter-spacing:-.02em; text-shadow:0 1px 0 rgba(74,49,15,.25);
}
.logo-mark::after {
  content:''; position:absolute; inset:0;
  background:
    linear-gradient(155deg,rgba(255,255,255,.4) 0%,transparent 45%),
    linear-gradient(25deg,transparent 60%,rgba(255,255,255,.15) 85%);
  border-radius:inherit;
}
.logo-text { display:flex; flex-direction:column; min-width:0 }
.logo-text .name {
  font-family:var(--fd); font-size:19px; font-weight:400;
  color:var(--text-1); letter-spacing:-.015em; line-height:1;
  text-shadow:0 1px 0 rgba(253,252,248,.6);
}
.logo-text .ver {
  font-size:8px; font-weight:700; color:var(--brand-dark);
  letter-spacing:.22em; text-transform:uppercase;
  font-family:var(--ff); margin-top:6px;
  display:flex; align-items:center; gap:6px; line-height:1;
}
.logo-text .ver::before {
  content:''; width:3px; height:3px;
  background:linear-gradient(140deg,var(--brand-light),var(--brand));
  border-radius:50%; box-shadow:0 0 3px rgba(184,130,58,.5);
}
.logo-text .ver::after {
  content:''; flex:1; height:1px;
  background:linear-gradient(90deg,rgba(143,98,36,.22),transparent);
}

.nav { flex:1; overflow-y:auto; padding:10px 10px 14px }
.nav-group {
  padding:16px 12px 6px; font-size:10px; font-weight:700;
  color:var(--text-4); text-transform:uppercase;
  letter-spacing:.18em; display:flex; align-items:center; gap:8px;
}
.nav-group::after {
  content:''; flex:1; height:1px;
  background:linear-gradient(90deg,var(--line),transparent);
  margin-left:2px;
}
.nav-item {
  display:flex; align-items:center; gap:11px;
  padding:9px 12px; border-radius:var(--radius-md);
  color:var(--text-2b); cursor:pointer;
  font-size:13.5px; font-weight:450;
  transition:all .22s var(--ease);
  margin:1px 0; position:relative; letter-spacing:-.002em;
}
.nav-item:hover { background:var(--hov); color:var(--text-1) }
.nav-item:hover .ico { opacity:.78; color:var(--brand) }
.nav-item.active {
  background:linear-gradient(180deg,#FFFEFA,#F7F2E4);
  color:var(--text-1); font-weight:600;
  box-shadow:
    inset 0 0 0 1px rgba(184,130,58,.22),
    inset 0 1px 0 rgba(255,255,255,.8),
    0 1px 2px rgba(36,30,24,.035),
    0 6px 14px rgba(184,130,58,.1);
}
.nav-item.active::before {
  content:''; position:absolute; left:-10px; top:50%;
  transform:translateY(-50%);
  width:3px; height:20px;
  background:linear-gradient(180deg,var(--brand-lighter),var(--brand) 50%,var(--brand-dark));
  border-radius:0 3px 3px 0;
  box-shadow:0 0 8px rgba(184,130,58,.45),0 0 2px rgba(184,130,58,.6);
}
.nav-item .ico { width:17px; height:17px; flex-shrink:0; opacity:.52; transition:all .22s var(--ease); stroke-width:1.7 }
.nav-item.active .ico { opacity:1; color:var(--brand) }

.sidebar-footer {
  padding:16px 20px 18px; border-top:1px solid var(--line);
  background:linear-gradient(0deg,rgba(36,30,24,.025),transparent);
  position:relative; margin-top:8px;
}
.sidebar-footer::before {
  content:''; position:absolute; top:-1px; left:18px; right:18px; height:1px;
  background:linear-gradient(90deg,transparent,var(--line) 20%,var(--line) 80%,transparent);
}
.sb-ft-row { display:flex; align-items:center; justify-content:space-between; margin-bottom:10px }
.sb-ft-lbl {
  font-size:10px; color:var(--text-3); font-weight:700;
  letter-spacing:.12em; text-transform:uppercase;
  display:flex; align-items:center; gap:6px;
}
.sb-ft-lbl::before {
  content:''; width:3px; height:3px;
  background:var(--brand); border-radius:50%;
  box-shadow:0 0 3px rgba(184,130,58,.5);
}
.ver-info {
  font-size:9px; color:var(--text-4); font-family:var(--fm);
  margin-top:10px; letter-spacing:.1em; text-transform:uppercase; font-weight:700;
  padding-top:9px; border-top:1px dashed var(--line);
  display:flex; align-items:center; justify-content:space-between;
}
.ver-dot { display:inline-flex; align-items:center; gap:5px }
.ver-dot::before {
  content:''; width:5px; height:5px;
  background:var(--green); border-radius:50%;
  box-shadow:0 0 4px rgba(62,122,92,.5);
}

.topbar {
  height:var(--header-height);
  background:rgba(244,242,236,.7);
  backdrop-filter:blur(32px) saturate(1.7);
  -webkit-backdrop-filter:blur(32px) saturate(1.7);
  border-bottom:1px solid var(--line);
  display:flex; align-items:center; padding:0 32px; gap:12px; flex-shrink:0;
  box-shadow:0 1px 0 rgba(253,252,248,.4);
  position:relative;
}
.topbar::after {
  content:''; position:absolute; bottom:-1px; left:0; right:0; height:1px;
  background:linear-gradient(90deg,transparent,rgba(184,130,58,.12) 15%,rgba(184,130,58,.12) 85%,transparent);
}
.breadcrumb { font-size:14px; color:var(--text-2); font-weight:500; letter-spacing:-.005em }
.breadcrumb :deep(b) { color:var(--text-1); font-weight:650; letter-spacing:-.008em }
.topbar .spacer { flex:1 }

.hd-user {
  display:flex; align-items:center; gap:10px;
  padding:4px 14px 4px 4px; border-radius:var(--radius-pill);
  transition:all .22s var(--ease);
}
.hd-user:hover {
  background:var(--hov);
  box-shadow:inset 0 0 0 1px var(--line);
}
.avatar {
  width:30px; height:30px; border-radius:50%;
  background:
    radial-gradient(circle at 30% 25%,#E8BE74 0%,transparent 45%),
    linear-gradient(140deg,#D09E4E 0%,#8F6224 100%);
  color:#fff; display:flex; align-items:center; justify-content:center;
  font-size:11.5px; font-weight:700; font-family:var(--fm);
  box-shadow:
    inset 0 1px 0 rgba(255,255,255,.35),
    inset 0 -1px 0 rgba(74,49,15,.25),
    0 1px 3px rgba(143,98,36,.35),
    0 0 0 2px rgba(184,130,58,.08);
  letter-spacing:-.02em;
}
.user-meta { display:flex; flex-direction:column; line-height:1.2 }
.topbar .username { font-size:13px; font-weight:600; color:var(--text-1); letter-spacing:-.005em }
.topbar .role { font-size:10px; color:var(--text-3); letter-spacing:.05em }
.logout-btn {
  font-size:12.5px; color:var(--text-3); cursor:pointer;
  transition:all .2s; padding:6px 12px; border-radius:var(--radius-sm); font-weight:500;
}
.logout-btn:hover { color:var(--red); background:var(--red-soft) }

.main-content { flex:1; display:flex; flex-direction:column; overflow:hidden }
.main-content__page { flex:1; overflow-y:auto; overflow-x:hidden; padding:18px 18px; background:var(--bg); position:relative; z-index:1 }
</style>
