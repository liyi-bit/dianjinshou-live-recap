<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

interface ModelStatus {
  ready: boolean
  downloading: boolean
  percent: number
  downloadedBytes: number
  totalBytes: number
  lastError?: string
  modelDir: string
}

const status = ref<ModelStatus | null>(null)
let offStatus: (() => void) | null = null

const api: any = (window as any).electronAPI

async function loadStatus() {
  const res = await api?.getAsrModelStatus?.()
  status.value = res?.data || null
}

function formatMB(bytes: number): string {
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

const stateText = computed(() => {
  if (!status.value) return '检测中'
  if (status.value.ready) return '已随安装包内置'
  return status.value.lastError || '未检测到内置 ASR 模型'
})

const stateClass = computed(() => {
  if (!status.value) return 'checking'
  return status.value.ready ? 'ready' : 'missing'
})

const modelSizeText = computed(() => {
  if (!status.value?.ready) return '—'
  return formatMB(status.value.totalBytes || status.value.downloadedBytes)
})

onMounted(() => {
  loadStatus()
  if (api?.onAsrModelStatus) {
    offStatus = api.onAsrModelStatus((payload: ModelStatus) => {
      status.value = payload
    })
  }
})

onBeforeUnmount(() => {
  offStatus?.()
})
</script>

<template>
  <div class="asr-tab">
    <div class="djscard section-card">
      <h3>语音识别方式</h3>
      <div class="engine-line">
        <div class="radio-dot"></div>
        <div class="engine-copy">
          <div class="engine-title">本机 ASR</div>
          <div class="engine-desc">内置 Paraformer 中文模型，离线生成逐字稿</div>
        </div>
        <div class="engine-badges">
          <span>无需密钥</span>
          <span>无需下载</span>
          <span>仅本机</span>
        </div>
      </div>
    </div>

    <div class="djscard section-card">
      <h3>内置模型状态</h3>

      <div class="status-grid">
        <div class="status-label">识别模型</div>
        <div class="status-value" :class="stateClass">{{ stateText }}</div>

        <div class="status-label">标点模型</div>
        <div class="status-value ready">已随安装包内置</div>

        <div class="status-label">模型大小</div>
        <div class="status-value">{{ modelSizeText }}</div>
      </div>

      <p class="hint">模型路径：<code>{{ status?.modelDir || '—' }}</code></p>
    </div>
  </div>
</template>

<style scoped>
.asr-tab { max-width: 860px; }
.section-card { padding: 22px 26px; margin-bottom: 18px; }
.section-card h3 {
  font-size: 15px; font-weight: 700; margin: 0 0 16px;
  padding-bottom: 12px; border-bottom: 1px solid var(--line);
}

.engine-line {
  display: flex; align-items: flex-start; gap: 14px;
  min-height: 70px; padding: 16px 0 4px;
}
.radio-dot {
  width: 14px; height: 14px; border-radius: 50%;
  border: 4px solid var(--gold-500, #b8863c);
  box-shadow: 0 0 0 1px rgba(184, 134, 60, .22);
  margin-top: 3px; flex: 0 0 auto;
}
.engine-copy { flex: 1; min-width: 0; }
.engine-title { font-size: 14px; font-weight: 700; margin-bottom: 7px; }
.engine-desc { font-size: 12.5px; color: var(--text-3); line-height: 1.7; }
.engine-badges {
  display: flex; flex-wrap: wrap; justify-content: flex-end;
  gap: 8px; max-width: 300px;
}
.engine-badges span {
  height: 26px; padding: 0 10px; display: inline-flex; align-items: center;
  border: 1px solid rgba(184, 134, 60, .28); border-radius: 999px;
  color: var(--gold-700, #8b5e1f); background: rgba(184, 134, 60, .06);
  font-size: 12px; white-space: nowrap;
}

.status-grid {
  display: grid; grid-template-columns: 96px minmax(0, 1fr);
  gap: 14px 18px; align-items: center;
}
.status-label { font-size: 12.5px; color: var(--text-3); }
.status-value { font-size: 13px; color: var(--text-2); min-width: 0; overflow-wrap: anywhere; }
.status-value.ready { color: #1f8f4d; font-weight: 600; }
.status-value.missing { color: #b42318; font-weight: 600; }
.status-value.checking { color: var(--text-3); }

.hint {
  margin: 18px 0 0; padding: 12px 14px;
  font-size: 11.5px; color: var(--text-3);
  background: var(--card-soft); border-left: 3px solid rgba(184, 134, 60, .7);
}
.hint code {
  font-size: 11px; font-family: 'Consolas', 'Menlo', monospace;
  overflow-wrap: anywhere;
}

@media (max-width: 720px) {
  .engine-line { flex-direction: column; gap: 10px; }
  .engine-badges { justify-content: flex-start; max-width: none; }
  .status-grid { grid-template-columns: 82px minmax(0, 1fr); }
}
</style>
