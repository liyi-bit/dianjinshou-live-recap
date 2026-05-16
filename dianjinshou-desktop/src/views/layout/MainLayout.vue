<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useThemeStore } from '@/stores/theme'
import { useUserStore } from '@/stores/user'
import { Message } from '@arco-design/web-vue'
import TitleBar from '@/components/TitleBar.vue'
import DiskUsageBar from '@/components/common/DiskUsageBar.vue'

const router = useRouter()
const route = useRoute()
const appStore = useAppStore()
const themeStore = useThemeStore()
const userStore = useUserStore()

onMounted(async () => {
  // placeholder for future onMounted logic
})

const openKeys = ref<string[]>(['add', 'recap', 'comparison', 'cloud'])

const activeKey = computed(() => {
  const path = route.path
  if (path.startsWith('/streamers/add')) return 'add'
  if (path.startsWith('/streamers')) return 'streamers'
  if (path.startsWith('/recap')) return 'recap'
  if (path.startsWith('/comparison')) return 'comparison'
  if (path.startsWith('/assistant/operation')) return 'assistant-operation'
  if (path.startsWith('/assistant/compliance')) return 'assistant-compliance'
  if (path.startsWith('/cloud')) return 'cloud'
  if (path.startsWith('/settings')) return 'settings'
  return 'streamers'
})

// Keep selectedKeys for backward compat (not used in new template but harmless)
const selectedKeys = computed(() => [activeKey.value])

const breadcrumb = computed(() => {
  const path = route.path
  if (path.startsWith('/streamers/add')) return '添加直播间'
  if (path.startsWith('/streamers')) return '直播间总览'
  if (path.startsWith('/recap/full')) return 'AI 全域复盘 / <b>全场复盘</b>'
  if (path.startsWith('/recap/clip')) return 'AI 全域复盘 / <b>切片复盘</b>'
  if (path.startsWith('/recap')) return 'AI 全域复盘'
  if (path.startsWith('/comparison/full')) return 'AI 对比复盘 / <b>整场对比</b>'
  if (path.startsWith('/comparison/clip')) return 'AI 对比复盘 / <b>切片对比</b>'
  if (path.startsWith('/comparison')) return 'AI 对比复盘'
  if (path.startsWith('/assistant/operation')) return 'AI 助手 / <b>AI 运营助手</b>'
  if (path.startsWith('/assistant/compliance')) return 'AI 助手 / <b>AI 违规助手</b>'
  if (path.startsWith('/cloud')) {
    if (path.startsWith('/cloud/readonly')) return '云空间 / <b>云端只读详情</b>'
    if (route.query.tab === 'clip') return '云空间 / <b>切片复盘</b>'
    if (route.query.tab === 'comparison') return '云空间 / <b>对比复盘</b>'
    return '云空间 / <b>全场复盘</b>'
  }
  if (path.startsWith('/settings')) return '系统设置'
  return '直播间总览'
})

function navigateTo(path: string) {
  if (route.fullPath === path) return
  router.push(path).catch((err) => {
    const message = String(err?.message || err || '')
    if (
      message.includes('Failed to fetch dynamically imported module') ||
      message.includes('Importing a module script failed') ||
      message.includes('Unable to preload CSS')
    ) {
      Message.warning('页面资源已更新，正在刷新后重新打开')
      window.location.reload()
      return
    }
    if (err?.name !== 'NavigationDuplicated') {
      Message.error('页面打开失败，请重试')
    }
  })
}

function openSubmenuAndNavigate(key: string, path: string) {
  if (!openKeys.value.includes(key)) {
    openKeys.value.push(key)
  }
  navigateTo(path)
}

function handleDiskCritical() {
  console.warn('[MainLayout] Disk critically full, stopping all recordings')
  window.electronAPI?.getAllRecordings().then((res) => {
    if (res.success && res.data) {
      res.data
        .filter((r) => r.status === 'recording')
        .forEach((r) => window.electronAPI?.stopRecording(r.recordingId))
    }
  })
}
</script>

<template>
  <div class="main-layout">
    <TitleBar />
    <div class="main-layout__body">
      <!-- Sidebar -->
      <div class="sidebar">
        <!-- Logo -->
        <div class="logo">
          <div class="logo-mark">金</div>
          <div class="logo-text">
            <div class="name">点金手直播中控台</div>
          </div>
        </div>

        <!-- Nav -->
        <nav class="nav">
          <!-- Group: 监控 -->
          <div class="nav-group">监控</div>

          <!-- 直播间列表 -->
          <div
            class="nav-item"
            :class="{ active: activeKey === 'streamers' }"
            @click="navigateTo('/streamers')"
          >
            <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 5a2 2 0 012-2h12a2 2 0 012 2v11a2 2 0 01-2 2H6a2 2 0 01-2-2V5z"/>
              <path d="M8 21h8M12 19v2"/>
            </svg>
            直播间总览
          </div>

          <!-- 添加直播间 -->
          <div
            class="nav-item"
            :class="{ active: activeKey === 'add' }"
            @click="openSubmenuAndNavigate('add', '/streamers/add/douyin')"
          >
            <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 4a4 4 0 100 8 4 4 0 000-8z"/>
              <path d="M4 20c0-4 4-6 8-6s8 2 8 6"/>
            </svg>
            添加直播间
            <span class="arrow" :class="{ open: openKeys.includes('add') }">▾</span>
          </div>
          <div class="nav-sub" v-show="openKeys.includes('add')">
            <div class="nav-sub-item" :class="{ active: route.path === '/streamers/add/douyin' }" @click.stop="navigateTo('/streamers/add/douyin')">添加抖音</div>
            <div class="nav-sub-item" :class="{ active: route.path === '/streamers/add/kuaishou' }" @click.stop="navigateTo('/streamers/add/kuaishou')">添加快手</div>
            <!-- 视频号移至V2开发 -->
            <!-- <div class="nav-sub-item" :class="{ active: route.path === '/streamers/add/shipinhao' }" @click="navigateTo('/streamers/add/shipinhao')">添加视频号</div> -->
          </div>

          <!-- Group: AI 复盘 -->
          <div class="nav-group">AI 复盘</div>

          <!-- AI复盘 -->
          <div
            class="nav-item"
            :class="{ active: activeKey === 'recap' }"
            @click="openSubmenuAndNavigate('recap', '/recap/full')"
          >
            <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 4h16v16H4z"/>
              <path d="M9 9h6v6H9z"/>
            </svg>
            AI全域复盘
            <span class="arrow" :class="{ open: openKeys.includes('recap') }">▾</span>
          </div>
          <div class="nav-sub" v-show="openKeys.includes('recap')">
            <div class="nav-sub-item" :class="{ active: route.path === '/recap/full' }" @click.stop="navigateTo('/recap/full')">AI全场复盘</div>
            <div class="nav-sub-item" :class="{ active: route.path === '/recap/clip' }" @click.stop="navigateTo('/recap/clip')">AI切片复盘</div>
          </div>

          <!-- Group: AI 对比复盘 -->
          <div class="nav-group">AI 对比复盘</div>

          <!-- AI对比复盘 -->
          <div
            class="nav-item"
            :class="{ active: activeKey === 'comparison' }"
            @click="openSubmenuAndNavigate('comparison', '/comparison/full')"
          >
            <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 19V5M20 19V5M4 12h16"/>
            </svg>
            AI对比复盘
            <span class="arrow" :class="{ open: openKeys.includes('comparison') }">▾</span>
          </div>
          <div class="nav-sub" v-show="openKeys.includes('comparison')">
            <div class="nav-sub-item" :class="{ active: route.path === '/comparison/full' }" @click.stop="navigateTo('/comparison/full')">整场对比</div>
            <div class="nav-sub-item" :class="{ active: route.path === '/comparison/clip' }" @click.stop="navigateTo('/comparison/clip')">切片对比</div>
          </div>

          <!-- Group: AI 助手 -->
          <div class="nav-group">AI 助手</div>

          <!-- AI 运营助手 -->
          <div
            class="nav-item"
            :class="{ active: activeKey === 'assistant-operation' }"
            @click="navigateTo('/assistant/operation/list')"
          >
            <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="3" width="7" height="7"/>
              <rect x="14" y="3" width="7" height="7"/>
              <rect x="14" y="14" width="7" height="7"/>
              <rect x="3" y="14" width="7" height="7"/>
            </svg>
            AI 运营助手
          </div>

          <!-- AI 违规助手 -->
          <div
            class="nav-item"
            :class="{ active: activeKey === 'assistant-compliance' }"
            @click="navigateTo('/assistant/compliance/list')"
          >
            <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
              <path d="M12 8v4M12 16h.01"/>
            </svg>
            AI 违规助手
          </div>

          <!-- Group: 云空间 -->
          <div class="nav-group">云空间</div>
          <div
            class="nav-item"
            :class="{ active: activeKey === 'cloud' }"
            @click="openSubmenuAndNavigate('cloud', '/cloud?tab=full')"
          >
            <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 16.5A4.5 4.5 0 0016.5 12h-.7A6 6 0 104.5 16.5H21z"/>
            </svg>
            云空间
            <span class="arrow" :class="{ open: openKeys.includes('cloud') }">▾</span>
          </div>
          <div class="nav-sub" v-show="openKeys.includes('cloud')">
            <div class="nav-sub-item" :class="{ active: route.path === '/cloud' && route.query.tab !== 'clip' && route.query.tab !== 'comparison' }" @click.stop="navigateTo('/cloud?tab=full')">全场复盘</div>
            <div class="nav-sub-item" :class="{ active: route.path === '/cloud' && route.query.tab === 'clip' }" @click.stop="navigateTo('/cloud?tab=clip')">切片复盘</div>
            <div class="nav-sub-item" :class="{ active: route.path === '/cloud' && route.query.tab === 'comparison' }" @click.stop="navigateTo('/cloud?tab=comparison')">对比复盘</div>
          </div>

          <!-- Group: 系统 -->
          <div class="nav-group">系统</div>

          <!-- 系统设置 -->
          <div
            class="nav-item"
            :class="{ active: activeKey === 'settings' }"
            @click="navigateTo('/settings')"
          >
            <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 9v6m0 0a3 3 0 100-6 3 3 0 000 6zM19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 11-2.83 2.83l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 11-4 0v-.09A1.65 1.65 0 009 19.4"/>
            </svg>
            系统设置
          </div>
        </nav>

        <!-- Sidebar Footer -->
        <div class="sidebar-footer">
          <div class="sb-ft-row">
            <span class="sb-ft-lbl">存储空间</span>
          </div>
          <div class="disk">
            <DiskUsageBar @critical="handleDiskCritical" />
          </div>
          <div class="ver-info">
            <span class="ver-dot">{{ appStore.appVersion }}</span>
          </div>
        </div>
      </div>

      <!-- Main content -->
      <div class="main-content">
        <!-- Topbar -->
        <div class="topbar">
          <div class="breadcrumb" v-html="breadcrumb" />
          <div class="spacer" />
          <div class="hd-user">
            <div class="avatar">{{ (appStore.username || '用')[0] }}</div>
            <span class="username">{{ appStore.username || '用户' }}</span>
          </div>
          <span class="logout-btn" @click="userStore.logout()" title="退出登录">退出</span>
        </div>

        <!-- Page content -->
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

/* ═══ SIDEBAR ═══ */
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

/* Logo */
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

/* Nav */
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
.nav-item .arrow { margin-left:auto; font-size:11px; color:var(--text-3); transition:transform .2s var(--ease) }
.nav-item .arrow.open { transform:rotate(180deg) }
.nav-item .tag { margin-left:auto; font-size:9px; color:var(--red); background:var(--red-soft); padding:1px 5px; border-radius:var(--radius-xs); font-weight:600 }

.nav-sub { padding-left:42px; font-size:12.75px; color:var(--text-3); margin-top:4px }
.nav-sub-item { padding:7px 12px; font-size:12.75px; color:var(--text-3); cursor:pointer; border-radius:var(--radius-sm); margin-bottom:2px; transition:all .15s }
.nav-sub-item:hover { background:var(--hov); color:var(--text-2) }
.nav-sub-item.active { color:var(--brand); font-weight:600 }

/* Sidebar Footer */
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
.disk { margin-bottom:8px }
.ver-info {
  font-size:9px; color:var(--text-4); font-family:var(--fm);
  margin-top:10px; letter-spacing:.1em; text-transform:uppercase; font-weight:700;
  padding-top:9px; border-top:1px dashed var(--line);
  display:flex; align-items:center; justify-content:space-between;
}
.ver-dot {
  display:inline-flex; align-items:center; gap:5px;
}
.ver-dot::before {
  content:''; width:5px; height:5px;
  background:var(--green); border-radius:50%;
  box-shadow:0 0 4px rgba(62,122,92,.5);
  animation:pulseSm 2.5s var(--ease) infinite;
}
@keyframes pulseSm { 0%,100%{opacity:1} 50%{opacity:.5} }

/* ═══ TOPBAR ═══ */
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

/* Header badge */
.hd-badge {
  display:inline-flex; align-items:center; gap:6px;
  padding:5px 13px 5px 11px; border-radius:var(--radius-pill);
  background:linear-gradient(180deg,rgba(168,118,14,.13),rgba(168,118,14,.07));
  font-size:12px; color:#6B4800; font-weight:650;
  border:1px solid rgba(168,118,14,.22);
  box-shadow:inset 0 1px 0 rgba(255,255,255,.3),0 1px 2px rgba(168,118,14,.06);
  letter-spacing:-.005em;
}
.hd-badge svg { opacity:.85 }

/* Vertical divider */
.hd-vd {
  width:1px; height:22px;
  background:linear-gradient(180deg,transparent,var(--line-3) 20%,var(--line-3) 80%,transparent);
  margin:0 4px; flex-shrink:0;
}

/* Notification bell */
.hd-bell {
  width:34px; height:34px;
  display:flex; align-items:center; justify-content:center;
  border-radius:var(--radius-md); background:transparent;
  border:1px solid transparent; cursor:pointer; color:var(--text-2);
  position:relative; transition:all .22s var(--ease); outline:none;
}
.hd-bell:hover {
  background:linear-gradient(180deg,var(--card-hover),var(--card));
  border-color:var(--line);
  color:var(--text-1);
  box-shadow:
    inset 0 1px 0 rgba(255,255,255,.5),
    0 1px 2px rgba(36,30,24,.04),
    0 2px 8px rgba(36,30,24,.04);
  transform:translateY(-1px);
}
.hd-bell:hover svg { animation:bellShake .5s var(--ease) }
@keyframes bellShake {
  0%,100%{transform:rotate(0)} 20%{transform:rotate(-8deg)}
  40%{transform:rotate(7deg)} 60%{transform:rotate(-5deg)} 80%{transform:rotate(3deg)}
}
.hd-bell svg { width:16px; height:16px; transition:transform .25s var(--ease) }

/* User area */
.hd-user {
  display:flex; align-items:center; gap:10px;
  padding:4px 14px 4px 4px; border-radius:var(--radius-pill);
  cursor:pointer; transition:all .22s var(--ease);
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
  letter-spacing:-.02em; position:relative;
}
.topbar .username { font-size:13px; font-weight:600; color:var(--text-1); letter-spacing:-.005em }
.logout-btn {
  font-size:12.5px; color:var(--text-3); cursor:pointer;
  transition:all .2s; padding:6px 12px; border-radius:var(--radius-sm); font-weight:500;
}
.logout-btn:hover { color:var(--red); background:var(--red-soft) }

/* ═══ CONTENT ═══ */
.main-content { flex:1; display:flex; flex-direction:column; overflow:hidden }
.main-content__page { flex:1; overflow-y:auto; overflow-x:hidden; padding:18px 18px; background:var(--bg); position:relative; z-index:1 }
</style>
