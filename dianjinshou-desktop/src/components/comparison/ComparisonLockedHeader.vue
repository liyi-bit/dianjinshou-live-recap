<script setup lang="ts">
import { useComparisonStore } from '@/stores/comparison'

const store = useComparisonStore()
</script>

<template>
  <div v-if="store.draft" class="locked-header">
    <div class="locked-info">
      <span style="color: var(--brand); margin-right: 6px; font-size: 16px">✓</span>
      <span>已选第一方，请在当前列表中选择第二方进行对比分析</span>
    </div>
    <button class="djsbtn djsbtn--text djsbtn--sm" @click="store.cancel()">取消</button>
  </div>
</template>

<style scoped lang="scss">
.locked-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: linear-gradient(90deg, var(--brand-soft) 0%, var(--line) 100%);
  border-radius: 6px;
  margin-bottom: 12px;

  .locked-info {
    display: flex;
    align-items: center;
    font-size: 13px;
    color: var(--color-text-1);
  }
}
</style>
