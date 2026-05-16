<script setup lang="ts">
import { ref, watch } from 'vue'
import { Message } from '@arco-design/web-vue'

const props = defineProps<{
  visible: boolean
  currentRecordingId: number
  previousRecordingId: number | null
  currentName: string
  previousName: string | null
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'confirm'): void
}>()

const hasPrevious = ref(false)

watch(() => props.previousRecordingId, (val) => {
  hasPrevious.value = val != null && val > 0
}, { immediate: true })

function doConfirm() {
  if (!hasPrevious.value) return
  emit('confirm')
  emit('update:visible', false)
}
</script>

<template>
  <a-modal
    :visible="visible"
    title="对比上一场"
    :width="500"
    @cancel="emit('update:visible', false)"
    :footer="false"
  >
    <div class="compare-info">
      <div class="session-card">
        <span class="label">本场</span>
        <span class="name">{{ currentName }}</span>
      </div>
      <span class="vs">VS</span>
      <div class="session-card">
        <span class="label">上一场</span>
        <span class="name">{{ previousName || '无' }}</span>
      </div>
    </div>

    <a-alert
      v-if="!hasPrevious"
      type="warning"
      style="margin-bottom: 16px"
    >
      目前只有一场直播, 暂时不能创建对比复盘
    </a-alert>

    <div class="actions">
      <a-button @click="emit('update:visible', false)">取消</a-button>
      <a-button
        type="primary"
        :disabled="!hasPrevious"
        @click="doConfirm"
      >
        确认生成对比
      </a-button>
    </div>
  </a-modal>
</template>

<style scoped lang="scss">
.compare-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 20px 0;

  .session-card {
    flex: 1;
    text-align: center;
    padding: 12px;
    background: var(--color-fill-1);
    border-radius: 8px;

    .label {
      display: block;
      font-size: 12px;
      color: var(--color-text-3);
      margin-bottom: 4px;
    }
    .name {
      font-size: 14px;
      font-weight: 600;
    }
  }

  .vs {
    font-size: 18px;
    font-weight: 700;
    color: var(--color-text-3);
  }
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
