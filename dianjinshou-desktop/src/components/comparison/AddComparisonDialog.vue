<script setup lang="ts">
import { ref } from 'vue'
import WarmTipsDialog from './WarmTipsDialog.vue'

const props = defineProps<{
  visible: boolean
  firstName: string
  secondName: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'confirm'): void
}>()

const showTips = ref(false)

function onStartCompare() {
  showTips.value = true
}

function onTipsConfirm() {
  showTips.value = false
  emit('update:visible', false)
  emit('confirm')
}

function onTipsCancel() {
  showTips.value = false
}
</script>

<template>
  <a-modal
    :visible="visible"
    title="添加对比分析"
    :width="640"
    @cancel="emit('update:visible', false)"
    :footer="false"
  >
    <div class="comparison-cards">
      <div class="card optimize">
        <div class="card-label">优化场次</div>
        <div class="card-name">{{ firstName }}</div>
      </div>
      <div class="vs-badge">VS</div>
      <div class="card reference">
        <div class="card-label">参考场次</div>
        <div class="card-name">{{ secondName }}</div>
      </div>
    </div>
    <div class="dialog-actions">
      <a-button @click="emit('update:visible', false)">取消</a-button>
      <a-button type="primary" @click="onStartCompare">开始对比</a-button>
    </div>

    <WarmTipsDialog
      :visible="showTips"
      @confirm="onTipsConfirm"
      @cancel="onTipsCancel"
    />
  </a-modal>
</template>

<style scoped lang="scss">
.comparison-cards {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 24px 0;

  .card {
    flex: 1;
    padding: 20px;
    border-radius: var(--radius);
    text-align: center;

    &.optimize {
      background: linear-gradient(135deg, var(--brand-soft) 0%, #d6e4ff 100%);
      border: 1px solid #bedaff;
    }

    &.reference {
      background: linear-gradient(135deg, var(--gold-soft) 0%, #ffe4ba 100%);
      border: 1px solid #ffd6a0;
    }

    .card-label {
      font-size: 12px;
      color: var(--color-text-3);
      margin-bottom: 8px;
    }

    .card-name {
      font-size: 16px;
      font-weight: 600;
    }
  }

  .vs-badge {
    font-size: 20px;
    font-weight: 700;
    color: var(--color-text-3);
  }
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 12px;
}
</style>
