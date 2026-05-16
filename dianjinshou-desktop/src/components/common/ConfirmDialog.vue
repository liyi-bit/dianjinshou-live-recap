<script setup lang="ts">
defineProps<{
  visible: boolean
  title?: string
  content?: string
  confirmText?: string
  cancelText?: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'confirm'): void
  (e: 'cancel'): void
}>()

function handleConfirm() {
  emit('confirm')
  emit('update:visible', false)
}

function handleCancel() {
  emit('cancel')
  emit('update:visible', false)
}
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="djsmodal-wrap" @click.self="handleCancel">
      <div class="djsmodal">
        <div class="djsmodal-head">
          <span class="djsmodal-title">{{ title || '确认' }}</span>
          <button class="close-btn" @click="handleCancel">✕</button>
        </div>
        <div class="djsmodal-body">
          <p class="modal-content">{{ content }}</p>
        </div>
        <div class="djsmodal-foot">
          <button class="djsbtn ghost" @click="handleCancel">{{ cancelText || '取消' }}</button>
          <button class="djsbtn primary" @click="handleConfirm">{{ confirmText || '确定' }}</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.djsmodal-wrap {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(26, 29, 41, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-content {
  margin: 0;
  line-height: 1.6;
  color: var(--text-2);
  font-size: 14px;
}

.close-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--text-3);
  font-size: 16px;
  padding: 0;
  line-height: 1;
}

.close-btn:hover {
  color: var(--text-1);
}
</style>
