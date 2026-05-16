<script setup lang="ts">
const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'confirm'): void
  (e: 'cancel'): void
}>()

const TIPS = [
  '对比分析会使用AI额度，请确认额度充足',
  '对比分析需要两场均已完成AI分析',
  '对比结果将包含8大维度的详细对比',
  '对比生成后可随时变换优化/参考定位',
  '对比分析通常需要1-3分钟完成'
]
</script>

<template>
  <a-modal
    :visible="visible"
    title="温馨提示"
    :width="440"
    @cancel="emit('cancel')"
    :footer="false"
  >
    <div class="tips-content">
      <div v-for="(tip, idx) in TIPS" :key="idx" class="tip-item">
        <icon-info-circle style="color: var(--orange); margin-right: 8px; flex-shrink: 0" />
        <span>{{ tip }}</span>
      </div>
    </div>
    <div class="tips-actions">
      <a-button @click="emit('cancel')">取消生成</a-button>
      <a-button type="primary" @click="emit('confirm')">点我生成</a-button>
    </div>
  </a-modal>
</template>

<style scoped lang="scss">
.tips-content {
  margin-bottom: 20px;

  .tip-item {
    display: flex;
    align-items: flex-start;
    padding: 8px 0;
    font-size: 14px;
    line-height: 1.5;
  }
}

.tips-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
