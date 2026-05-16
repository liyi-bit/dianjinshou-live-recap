import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/views/layout/MainLayout.vue'),
    meta: { requiresAuth: true },
    redirect: '/streamers',
    children: [
      // 直播间管理
      {
        path: 'streamers',
        name: 'StreamerList',
        component: () => import('@/views/streamer/StreamerList.vue')
      },
      {
        path: 'streamers/add/douyin',
        name: 'AddDouyin',
        component: () => import('@/views/streamer/AddDouyin.vue')
      },
      {
        path: 'streamers/add/kuaishou',
        name: 'AddKuaishou',
        component: () => import('@/views/streamer/AddKuaishou.vue')
      },
      // 视频号：v2 开发
      // {
      //   path: 'streamers/add/shipinhao',
      //   name: 'AddShipinhao',
      //   component: () => import('@/views/streamer/AddShipinhao.vue')
      // },
      {
        path: 'streamers/:id/recap-table',
        name: 'StreamerRecapTable',
        component: () => import('@/views/streamer/RecapTable.vue')
      },
      // AI 复盘
      {
        path: 'recap/full',
        name: 'FullRecapList',
        component: () => import('@/views/recap/FullRecapList.vue')
      },
      {
        path: 'recap/clip',
        name: 'ClipRecapList',
        component: () => import('@/views/recap/ClipRecapList.vue')
      },
      {
        path: 'recap/:id',
        name: 'RecapDetail',
        component: () => import('@/views/recap/RecapDetail.vue')
      },
      {
        path: 'recap/:id/notes',
        name: 'RecapNotes',
        component: () => import('@/views/recap/NotesPanel.vue')
      },
      // AI 对比复盘
      {
        path: 'comparison/full',
        name: 'FullComparisonList',
        component: () => import('@/views/comparison/FullComparisonList.vue')
      },
      {
        path: 'comparison/clip',
        name: 'ClipComparisonList',
        component: () => import('@/views/comparison/ClipComparisonList.vue')
      },
      {
        path: 'comparison/:id',
        name: 'ComparisonDetail',
        component: () => import('@/views/comparison/ComparisonDetail.vue')
      },
      // v2 features
      {
        path: 'short-clip',
        name: 'ShortClip',
        component: () => import('@/views/short-clip/ShortClip.vue')
      },
      {
        path: 'file-analysis',
        name: 'FileAnalysis',
        component: () => import('@/views/file-analysis/FileAnalysis.vue')
      },
      {
        path: 'file-analysis/:id',
        name: 'FileAnalysisDetail',
        component: () => import('@/views/file-analysis/FileAnalysisDetail.vue')
      },
      {
        path: 'short-video',
        name: 'ShortVideo',
        component: () => import('@/views/short-video/ShortVideo.vue')
      },
      {
        path: 'assistant/operation/list',
        name: 'AssistantOperationList',
        component: () => import('@/views/assistant/AssistantHub.vue'),
        props: { mode: 'operation' }
      },
      {
        path: 'assistant/operation',
        name: 'AssistantOperation',
        component: () => import('@/views/assistant/OperationAssistant.vue')
      },
      {
        path: 'assistant/compliance/list',
        name: 'AssistantComplianceList',
        component: () => import('@/views/assistant/AssistantHub.vue'),
        props: { mode: 'compliance' }
      },
      {
        path: 'assistant/compliance',
        name: 'AssistantCompliance',
        component: () => import('@/views/assistant/ComplianceAssistant.vue')
      },
      {
        path: 'assistant/script',
        name: 'AssistantScript',
        component: () => import('@/views/assistant/ScriptAssistant.vue')
      },
      {
        path: 'cloud',
        name: 'Cloud',
        component: () => import('@/views/cloud/CloudSpace.vue')
      },
      {
        path: 'cloud/readonly/:id',
        name: 'CloudReadonlyDetail',
        component: () => import('@/views/cloud/CloudReadonlyDetailAdapter.vue')
      },
      // 系统设置
      {
        path: 'settings/:tab?',
        name: 'SystemSettings',
        component: () => import('@/views/system/SystemSettings.vue')
      }
    ]
  },
  // Catch-all redirect
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// Check if a JWT token is expired by decoding its payload
function isTokenExpired(token: string): boolean {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.exp * 1000 < Date.now()
  } catch {
    return true
  }
}

// Navigation guard
router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()
  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth !== false)

  if (requiresAuth) {
    // 完全没登录过 → 直接跳登录
    if (!userStore.accessToken) {
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
    // access token 过期但有 refresh token → 先试着刷新，刷不回来才踢到登录页
    if (isTokenExpired(userStore.accessToken)) {
      if (userStore.refreshToken) {
        try {
          const newToken = await userStore.refreshAccessToken()
          if (newToken) { next(); return }
        } catch { /* fall through to logout */ }
      }
      userStore.logout(true)
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
    next()
  } else if (
    (to.path === '/login' || to.path === '/register') &&
    userStore.accessToken &&
    !isTokenExpired(userStore.accessToken)
  ) {
    next('/')
  } else {
    next()
  }
})

export default router
