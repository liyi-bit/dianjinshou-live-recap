<script setup lang="ts">
import { computed } from 'vue';

interface Props {
  status: string | null | undefined;
}
const props = defineProps<Props>();

/** 桌面端 FullRecapList.vue:102-115 同款映射 */
const label = computed(() => {
  if (!props.status) return '—';
  const s = props.status.toLowerCase();
  const map: Record<string, string> = {
    none: '未开始',
    pending: '待处理',
    recording: '录制中',
    transcribing: '转写中',
    transcribed: '已转写',
    asr_processing: 'ASR中',
    ai_processing: 'AI中',
    completed: '已完成',
    failed: '失败',
    skipped: '跳过',
  };
  return map[s] || props.status;
});

const color = computed(() => {
  if (!props.status) return '#8A8174';
  const s = props.status.toLowerCase();
  if (s === 'completed') return '#00b42a';
  if (s === 'failed') return '#f53f3f';
  if (s === 'pending' || s === 'transcribed' || s === 'asr_processing' || s === 'ai_processing') return '#ff7d00';
  if (s === 'recording' || s === 'transcribing') return '#3491fa';
  return '#8A8174';
});

const bg = computed(() => {
  const c = color.value;
  if (c === '#8A8174') return 'rgba(138,129,116,0.12)';
  if (c === '#00b42a') return 'rgba(0,180,42,0.12)';
  if (c === '#f53f3f') return 'rgba(245,63,63,0.12)';
  if (c === '#ff7d00') return 'rgba(255,125,0,0.12)';
  if (c === '#3491fa') return 'rgba(52,145,250,0.12)';
  return 'rgba(138,129,116,0.12)';
});
</script>

<template>
  <span class="a-st" :style="{ color, background: bg }">
    <span class="dot" :style="{ background: color }"></span>
    {{ label }}
  </span>
</template>

<style scoped>
.a-st {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 9px;
  border-radius: var(--radius-pill);
  font-size: 12px;
  font-weight: 600;
  line-height: 18px;
  white-space: nowrap;
}
.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
</style>
