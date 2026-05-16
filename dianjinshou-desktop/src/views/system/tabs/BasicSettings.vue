<script setup lang="ts">
import { onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import { useAppStore } from '@/stores/app'
import { storeToRefs } from 'pinia'

const appStore = useAppStore()
const { storagePath, resolution, segmentDuration, checkInterval } = storeToRefs(appStore)

const CHECK_INTERVALS = [
  { value: 30, label: '30秒' },
  { value: 60, label: '1分钟' },
  { value: 120, label: '2分钟' },
  { value: 300, label: '5分钟' },
  { value: 600, label: '10分钟' }
]

onMounted(async () => {
  const sync = (appStore as any).syncDesktopSettings
  if (typeof sync === 'function') {
    await sync.call(appStore).catch(() => {})
  }
})

async function browsePath() {
  const res = await window.electronAPI?.selectDirectory()
  if (!res?.success || !res.data) return
  const apply = await window.electronAPI?.setRecordingsPath(res.data)
  if (apply?.success && apply.data) {
    storagePath.value = apply.data
    Message.success('保存位置已更新')
  } else {
    Message.error(apply?.error || '保存位置更新失败')
  }
}
</script>

<template>
  <div class="form-card djscard">
    <div class="djsform-row">
      <span class="djsform-label">检测频率</span>
      <div class="djsform-ctrl">
        <select class="djsselect" v-model.number="checkInterval">
          <option v-for="opt in CHECK_INTERVALS" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
        </select>
      </div>
    </div>
    <div class="djsform-row">
      <span class="djsform-label">保存位置</span>
      <div class="djsform-ctrl" style="display:flex;gap:8px;">
        <input class="djsinput" style="flex:1;" v-model="storagePath" readonly />
        <button class="djsbtn ghost" @click="browsePath">浏览</button>
      </div>
    </div>
    <div class="djsform-row">
      <span class="djsform-label">直播源</span>
      <div class="djsform-ctrl djsradio-group resolution-group">
        <label class="djsradio resolution-option" :class="{ on: resolution === '480p' }">
          <input type="radio" v-model="resolution" value="480p" />
          <span>标清</span>
        </label>
        <label class="djsradio resolution-option" :class="{ on: resolution === '720p' }">
          <input type="radio" v-model="resolution" value="720p" />
          <span>高清</span>
        </label>
        <label class="djsradio resolution-option" :class="{ on: resolution === '1080p' }">
          <input type="radio" v-model="resolution" value="1080p" />
          <span>超清</span>
        </label>
        <label class="djsradio resolution-option" :class="{ on: resolution === 'source' }">
          <input type="radio" v-model="resolution" value="source" />
          <span>原始</span>
        </label>
      </div>
    </div>
    <div class="djsform-row">
      <span class="djsform-label">时长分段</span>
      <div class="djsform-ctrl segment-duration-ctrl">
        <input
          class="slider segment-slider"
          type="range"
          :min="10"
          :max="180"
          :step="10"
          v-model.number="segmentDuration"
        />
        <span class="segment-duration-value">{{ segmentDuration }} 分钟</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.form-card { background: var(--card); border-radius: 14px; border: 1px solid var(--line); padding: 28px 32px; max-width: 700px; }

.resolution-group {
  align-items: center;
  gap: 10px;
}

.resolution-option {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-width: 74px;
  height: 32px;
  padding: 0 14px;
  line-height: 1;
  box-sizing: border-box;
}

.resolution-option input {
  flex: 0 0 auto;
  width: 14px;
  height: 14px;
  margin: 0;
}

.resolution-option span {
  display: inline-flex;
  align-items: center;
  line-height: 14px;
}

.segment-duration-ctrl {
  display: flex;
  align-items: center;
  gap: 12px;
}

.segment-slider {
  width: 260px;
  flex: 0 0 260px;
}

.segment-duration-value {
  min-width: 72px;
  color: var(--text-2b);
  font-size: 13px;
  line-height: 20px;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
</style>
