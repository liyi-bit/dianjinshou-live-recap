<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'

const status = ref<UpdateStatusPayload | null>(null)
const progress = ref<UpdateProgressPayload | null>(null)
const dismissedVersion = ref<string>(localStorage.getItem('djs_dismiss_update') || '')

let offStatus: (() => void) | null = null
let offProgress: (() => void) | null = null

onMounted(() => {
  const api = window.electronAPI as any
  if (!api?.onUpdateStatus) return // dev 模式或非 Electron 环境
  offStatus = api.onUpdateStatus((payload: UpdateStatusPayload) => {
    status.value = payload
    if (payload.state === 'available' || payload.state === 'downloaded') {
      progress.value = null
    }
  })
  offProgress = api.onUpdateProgress((payload: UpdateProgressPayload) => {
    progress.value = payload
  })
})

onBeforeUnmount(() => {
  offStatus?.()
  offProgress?.()
})

const visible = computed(() => {
  const s = status.value
  if (!s) return false
  // 用户主动忽略过这个版本 → 不显示
  if ((s.state === 'available' || s.state === 'downloaded') && s.version && s.version === dismissedVersion.value) {
    return false
  }
  return ['available', 'error', 'downloaded'].includes(s.state) || !!progress.value
})

const downloading = computed(() => !!progress.value && status.value?.state !== 'downloaded')
const fmtSpeed = computed(() => {
  const bps = progress.value?.bytesPerSecond || 0
  if (bps > 1024 * 1024) return (bps / 1024 / 1024).toFixed(1) + ' MB/s'
  if (bps > 1024) return (bps / 1024).toFixed(1) + ' KB/s'
  return bps + ' B/s'
})

async function onDownload() {
  const api = window.electronAPI as any
  await api?.downloadUpdate?.()
}
async function onInstall() {
  const api = window.electronAPI as any
  await api?.installUpdate?.()
}
function onDismiss() {
  const v = status.value?.version
  if (v) {
    dismissedVersion.value = v
    localStorage.setItem('djs_dismiss_update', v)
  }
  status.value = null
  progress.value = null
}
</script>

<template>
  <Transition name="slide">
    <div v-if="visible" class="update-banner">
      <!-- 下载中：进度条优先展示 -->
      <template v-if="downloading && progress">
        <span class="icon">⬇️</span>
        <span class="text">正在下载新版本 v{{ status?.version }} · {{ progress.percent }}% · {{ fmtSpeed }}</span>
        <div class="bar"><div class="bar-fill" :style="{ width: progress.percent + '%' }"></div></div>
      </template>

      <!-- 有新版本但还没下 -->
      <template v-else-if="status?.state === 'available'">
        <span class="icon">🎉</span>
        <span class="text">发现新版本 <b>v{{ status.version }}</b>，建议尽快更新</span>
        <button class="btn primary" @click="onDownload">立即更新</button>
        <button class="btn ghost" @click="onDismiss">稍后提醒</button>
      </template>

      <!-- 下载完成 -->
      <template v-else-if="status?.state === 'downloaded'">
        <span class="icon">✅</span>
        <span class="text">v{{ status.version }} 已下载完毕，重启即可生效</span>
        <button class="btn primary" @click="onInstall">重启并安装</button>
        <button class="btn ghost" @click="onDismiss">稍后</button>
      </template>

      <!-- 错误 -->
      <template v-else-if="status?.state === 'error'">
        <span class="icon">⚠️</span>
        <span class="text">更新检查失败：{{ status.message }}</span>
        <button class="btn ghost" @click="onDismiss">关闭</button>
      </template>
    </div>
  </Transition>
</template>

<style scoped>
.update-banner {
  position: fixed;
  top: 40px; /* 让出 TitleBar 30px + 10px 呼吸 */
  left: 50%;
  transform: translateX(-50%);
  z-index: 10000;
  min-width: 380px;
  max-width: 720px;
  padding: 10px 14px;
  display: flex;
  align-items: center;
  gap: 10px;
  background: linear-gradient(180deg, #FFFFFF, #FBF8F1);
  border: 1px solid var(--line, #E8E1D3);
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(36,30,24,.12), 0 2px 6px rgba(36,30,24,.06);
  font-size: 13px;
  color: var(--text-1, #2B2420);
}
.update-banner .icon { font-size: 16px; flex-shrink: 0; }
.update-banner .text { flex: 1; line-height: 1.4; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.update-banner .text b { color: var(--brand-dark, #8F6224); }
.update-banner .btn {
  height: 28px; padding: 0 14px; border-radius: 6px;
  font-size: 12.5px; font-weight: 600; cursor: pointer; border: 1px solid transparent;
  transition: background .15s, border-color .15s;
}
.update-banner .btn.primary {
  background: var(--brand, #B8823A);
  color: #fff;
}
.update-banner .btn.primary:hover { background: var(--brand-dark, #8F6224); }
.update-banner .btn.ghost {
  background: transparent;
  color: var(--text-2, #6B5F52);
  border-color: var(--line, #E8E1D3);
}
.update-banner .btn.ghost:hover {
  background: var(--card-hover, #FBF8F1);
  color: var(--text-1, #2B2420);
}
.update-banner .bar {
  position: absolute; left: 10px; right: 10px; bottom: 3px;
  height: 2px; background: rgba(184,130,58,.15); border-radius: 2px; overflow: hidden;
}
.update-banner .bar-fill {
  height: 100%;
  background: var(--brand, #B8823A);
  transition: width .3s;
}

.slide-enter-active, .slide-leave-active { transition: all .25s; }
.slide-enter-from, .slide-leave-to { opacity: 0; transform: translateX(-50%) translateY(-8px); }
</style>
