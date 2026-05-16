<script setup lang="ts">
/**
 * 强制升级阻塞窗：当本地版本 < 服务端 minVersion 时显示。
 * 全屏遮罩 + 不可关闭 + 只能"立即更新"或"退出"。
 *
 * 触发方式：App.vue 挂载后调用 /public/client-version 比较版本，
 * 命中强制升级条件则 <ForceUpdateModal v-if="forceUpdateNeeded" />。
 */
import { ref, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps<{
  currentVersion: string
  minVersion: string
  latestVersion: string
}>()

type State = 'idle' | 'downloading' | 'downloaded' | 'error'
const state = ref<State>('idle')
const progress = ref<{ percent: number; transferred: number; total: number }>({
  percent: 0, transferred: 0, total: 0,
})
const errorMsg = ref<string>('')

let offStatus: (() => void) | null = null
let offProgress: (() => void) | null = null

onMounted(() => {
  const api = (window as any).electronAPI
  if (!api) return

  offStatus = api.onUpdateStatus?.((payload: any) => {
    console.log('[ForceUpdate] status', payload)
    if (payload?.state === 'downloaded') {
      state.value = 'downloaded'
    } else if (payload?.state === 'error') {
      state.value = 'error'
      errorMsg.value = payload?.message || '下载失败,请重试或手动下载安装包'
    } else if (payload?.state === 'not-available') {
      // 服务端 feed 没更新就显示此对话框是异常，通常是服务端 minVersion 配错
      state.value = 'error'
      errorMsg.value = '服务端暂无新版本,请联系管理员'
    }
  })
  offProgress = api.onUpdateProgress?.((p: any) => {
    progress.value = {
      percent: Math.round(p?.percent || 0),
      transferred: p?.transferred || 0,
      total: p?.total || 0,
    }
  })
})

onBeforeUnmount(() => {
  offStatus?.()
  offProgress?.()
})

async function startDownload() {
  state.value = 'downloading'
  errorMsg.value = ''
  progress.value = { percent: 0, transferred: 0, total: 0 }
  try {
    const api = (window as any).electronAPI
    const res = await api?.downloadUpdate?.()
    if (res && !res.success) {
      state.value = 'error'
      errorMsg.value = res.error || '启动下载失败'
    }
  } catch (e: any) {
    state.value = 'error'
    errorMsg.value = e?.message || String(e)
  }
}

function installNow() {
  const api = (window as any).electronAPI
  api?.installUpdate?.()
}

function exitApp() {
  const api = (window as any).electronAPI
  api?.closeWindow?.()
}

function openManualDownload() {
  const url = `http://localhost:18081/desktop/dianjinshou-desktop-latest-setup.exe`
  // Electron 渲染进程里 window.open 对外部 URL 在默认配置下会被屏蔽，
  // 通过 <a> 点击让主进程走 shell.openExternal（项目现有处理）
  const a = document.createElement('a')
  a.href = url
  a.target = '_blank'
  a.rel = 'noopener noreferrer'
  document.body.appendChild(a)
  a.click()
  a.remove()
}

function formatMB(bytes: number) {
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}
</script>

<template>
  <div class="force-update-overlay" @click.stop @keydown.esc.stop.prevent>
    <div class="force-update-card">
      <div class="force-update-icon">⚠</div>
      <h2 class="force-update-title">需要升级到新版本</h2>
      <p class="force-update-desc">
        当前版本 <code>v{{ currentVersion }}</code> 已不再支持，
        必须升级到 <code>v{{ latestVersion }}</code> 或更高版本才能继续使用。
      </p>

      <!-- idle: 初始状态，显示按钮 -->
      <div v-if="state === 'idle'" class="force-update-actions">
        <button class="btn-primary" @click="startDownload">立即更新</button>
        <button class="btn-secondary" @click="exitApp">退出</button>
      </div>

      <!-- downloading: 显示进度条 -->
      <div v-else-if="state === 'downloading'" class="force-update-progress-wrap">
        <div class="progress-bar">
          <div class="progress-bar-fill" :style="{ width: progress.percent + '%' }"></div>
        </div>
        <div class="progress-text">
          下载中 {{ progress.percent }}%
          <span v-if="progress.total > 0" class="progress-size">
            ({{ formatMB(progress.transferred) }} / {{ formatMB(progress.total) }})
          </span>
        </div>
      </div>

      <!-- downloaded: 下载完成，显示安装按钮 -->
      <div v-else-if="state === 'downloaded'" class="force-update-actions">
        <p class="force-update-hint">下载完成，点击安装后会自动重启。</p>
        <button class="btn-primary" @click="installNow">立即安装并重启</button>
      </div>

      <!-- error: 下载出错 -->
      <div v-else-if="state === 'error'" class="force-update-error-wrap">
        <p class="force-update-error">{{ errorMsg }}</p>
        <div class="force-update-actions">
          <button class="btn-primary" @click="startDownload">重试下载</button>
          <button class="btn-secondary" @click="openManualDownload">手动下载安装包</button>
          <button class="btn-text" @click="exitApp">退出</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.force-update-overlay {
  position: fixed;
  inset: 0;
  background: rgba(20, 18, 14, 0.88);
  backdrop-filter: blur(8px);
  z-index: 99999;
  display: flex;
  align-items: center;
  justify-content: center;
  -webkit-app-region: no-drag;
}

.force-update-card {
  background: #fff;
  border-radius: 12px;
  padding: 36px 40px;
  width: min(480px, 90vw);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.35);
  text-align: center;
}

.force-update-icon {
  font-size: 48px;
  color: #e68a00;
  margin-bottom: 12px;
  line-height: 1;
}

.force-update-title {
  margin: 0 0 12px;
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
}

.force-update-desc {
  margin: 0 0 24px;
  font-size: 14px;
  line-height: 1.7;
  color: #444;
}

.force-update-desc code {
  background: #f5f0e4;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 13px;
  color: #8b5a00;
  border: 1px solid #e8ddc0;
}

.force-update-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 16px;
}

.force-update-hint {
  margin: 0 0 16px;
  font-size: 13px;
  color: #666;
}

.btn-primary {
  background: #c9a054;
  color: #fff;
  border: none;
  padding: 12px 24px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-primary:hover { background: #b58e46; }

.btn-secondary {
  background: #f3f0e8;
  color: #3a3027;
  border: 1px solid #d8cfb9;
  padding: 10px 24px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-secondary:hover { background: #ebe6d7; }

.btn-text {
  background: none;
  border: none;
  color: #888;
  font-size: 12px;
  cursor: pointer;
  text-decoration: underline;
  padding: 6px;
}
.btn-text:hover { color: #555; }

.force-update-progress-wrap {
  margin-top: 8px;
}

.progress-bar {
  height: 10px;
  background: #eee7d4;
  border-radius: 5px;
  overflow: hidden;
  margin-bottom: 8px;
}

.progress-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #c9a054, #dfb86b);
  transition: width 0.2s;
}

.progress-text {
  font-size: 13px;
  color: #555;
}

.progress-size {
  font-size: 12px;
  color: #888;
  margin-left: 4px;
}

.force-update-error-wrap { margin-top: 8px; }

.force-update-error {
  color: #c0392b;
  font-size: 13px;
  margin: 0 0 16px;
  padding: 10px 14px;
  background: #fdecea;
  border-radius: 6px;
  border: 1px solid #f5c9c2;
}
</style>
