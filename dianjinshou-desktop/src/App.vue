<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useThemeStore } from '@/stores/theme'
import { useUserStore } from '@/stores/user'
import UpdateBanner from '@/components/common/UpdateBanner.vue'
import ForceUpdateModal from '@/components/common/ForceUpdateModal.vue'
import { getClientVersion } from '@/api/config'

const themeStore = useThemeStore()
const userStore = useUserStore()

// 强制升级状态
const forceUpdateInfo = ref<{
  currentVersion: string
  minVersion: string
  latestVersion: string
} | null>(null)

/**
 * 返回 -1 / 0 / 1，按 "x.y.z" 或 "x.y.z-suffix" 比较。
 * 非数字段按字符串 localeCompare 兜底。
 */
function compareVersion(a: string, b: string): number {
  const [aMain] = a.split('-')
  const [bMain] = b.split('-')
  const as = aMain.split('.').map((x) => parseInt(x, 10) || 0)
  const bs = bMain.split('.').map((x) => parseInt(x, 10) || 0)
  const len = Math.max(as.length, bs.length)
  for (let i = 0; i < len; i++) {
    const av = as[i] || 0
    const bv = bs[i] || 0
    if (av < bv) return -1
    if (av > bv) return 1
  }
  return 0
}

async function checkForceUpdate() {
  const api = (window as any).electronAPI
  if (!api?.getAppVersion) {
    // 不在 Electron 里（比如 web 预览）—— 跳过
    return
  }
  try {
    const current = await api.getAppVersion()
    const info = (await getClientVersion()) as any
    const minVersion: string = info?.minVersion || '0.0.0'
    const latestVersion: string = info?.latestVersion || current
    if (compareVersion(current, minVersion) < 0) {
      console.warn(`[djs] force update: current=${current} < min=${minVersion}`)
      forceUpdateInfo.value = {
        currentVersion: current,
        minVersion,
        latestVersion,
      }
    } else {
      console.log(`[djs] version OK: current=${current} min=${minVersion}`)
    }
  } catch (e) {
    console.warn('[djs] checkForceUpdate failed (network / server):', e)
    // 后端不通时不强制 —— 避免把用户锁死
  }
}

onMounted(() => {
  console.log('[djs] client ready')
  themeStore.initTheme()
  // Sync existing auth token to Electron main process on app init
  const api = (window as any).electronAPI
  if (api?.setAuthToken && userStore.accessToken) {
    api.setAuthToken(userStore.accessToken).catch(() => {})
  }
  checkForceUpdate()
})
</script>

<template>
  <router-view />
  <UpdateBanner />
  <ForceUpdateModal
    v-if="forceUpdateInfo"
    :current-version="forceUpdateInfo.currentVersion"
    :min-version="forceUpdateInfo.minVersion"
    :latest-version="forceUpdateInfo.latestVersion"
  />
</template>

<style>
html { font-size: 15px }
html, body, #app {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  font-family: var(--ff, 'Noto Sans SC','Inter Tight',system-ui,sans-serif);
  background: var(--bg, #F4F2EC);
  color: var(--text-2, #2A241E);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-rendering: optimizeLegibility;
  letter-spacing: -.005em;
  font-feature-settings: "ss01","cv11","cv02","cv09";
  font-variant-numeric: tabular-nums;
}

/* Subtle warm paper texture + ambient highlights */
body::before {
  content: '';
  position: fixed; inset: 0;
  pointer-events: none; z-index: 0;
  background-image:
    radial-gradient(ellipse 800px 600px at 15% 10%, rgba(184,130,58,.045), transparent 55%),
    radial-gradient(ellipse 700px 500px at 88% 90%, rgba(74,104,150,.028), transparent 55%),
    radial-gradient(ellipse 500px 400px at 95% 15%, rgba(168,72,120,.018), transparent 60%);
}
body::after {
  content: '';
  position: fixed; inset: 0;
  pointer-events: none; z-index: 0; opacity: .35;
  background-image:
    repeating-linear-gradient(45deg, rgba(36,30,24,.006) 0, rgba(36,30,24,.006) 1px, transparent 1px, transparent 3px),
    repeating-linear-gradient(-45deg, rgba(36,30,24,.004) 0, rgba(36,30,24,.004) 1px, transparent 1px, transparent 3px);
}
</style>
