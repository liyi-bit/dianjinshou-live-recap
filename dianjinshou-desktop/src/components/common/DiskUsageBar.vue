<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'

const emit = defineEmits<{
  critical: []
}>()

const used = ref(0)
const total = ref(0)
const free = ref(0)
const percentUsed = ref(0)

const isWarning = computed(() => percentUsed.value >= 90)
const isCritical = computed(() => percentUsed.value >= 95)

const progressColor = computed(() => {
  if (isCritical.value) return 'var(--red)'
  if (isWarning.value) return 'var(--orange)'
  return 'var(--brand)'
})

function formatSize(bytes: number): string {
  if (bytes === 0) return '0GB'
  const gb = bytes / (1024 * 1024 * 1024)
  return `${gb.toFixed(1)}GB`
}

let timer: ReturnType<typeof setInterval> | null = null
let hasFiredCritical = false

async function fetchDiskUsage() {
  try {
    if (window.electronAPI?.getDiskUsage) {
      const res = await window.electronAPI.getDiskUsage()
      if (res.success && res.data) {
        used.value = res.data.used
        total.value = res.data.total
        free.value = res.data.free
        percentUsed.value = res.data.percentUsed
        if (isCritical.value && !hasFiredCritical) {
          hasFiredCritical = true
          emit('critical')
        }
        if (!isCritical.value) {
          hasFiredCritical = false
        }
      }
    } else {
      // Web mode fallback: show placeholder
      used.value = 2.3 * 1024 * 1024 * 1024
      total.value = 10 * 1024 * 1024 * 1024
      free.value = total.value - used.value
      percentUsed.value = Math.round((used.value / total.value) * 100)
    }
  } catch (err) {
    console.error('[DiskUsageBar] Failed to fetch disk usage:', err)
  }
}

onMounted(() => {
  fetchDiskUsage()
  timer = setInterval(fetchDiskUsage, 30_000)
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})
</script>

<template>
  <div class="disk-usage-bar">
    <div class="disk-usage-bar__label">
      <span>存储空间</span>
      <span>{{ formatSize(used) }} / {{ formatSize(total) }}</span>
    </div>
    <a-progress
      :percent="percentUsed / 100"
      :show-text="false"
      size="small"
      :color="progressColor"
    />
    <div v-if="isWarning" class="disk-usage-bar__warning">
      {{ isCritical ? '磁盘空间严重不足，已强制停止录制' : '磁盘空间不足' }}
    </div>
  </div>
</template>

<style scoped lang="scss">
.disk-usage-bar {
  &__label {
    display: flex;
    justify-content: space-between;
    font-size: 12px;
    color: var(--color-text-3);
    margin-bottom: 4px;
  }

  &__warning {
    margin-top: 4px;
    font-size: 11px;
    color: var(--orange);
    line-height: 1.4;
  }
}
</style>
