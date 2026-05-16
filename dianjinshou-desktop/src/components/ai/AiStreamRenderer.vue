<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  content: string
  thinking?: string
  loading?: boolean
}>()

const showThinking = defineModel<boolean>('showThinking', { default: false })

const displayContent = computed(() => {
  if (!props.content && props.loading) {
    return '正在思考...'
  }
  return props.content
})
</script>

<template>
  <div class="ai-stream">
    <!-- Thinking section -->
    <div v-if="thinking" class="ai-stream__thinking-section">
      <span class="djslink" @click="showThinking = !showThinking">
        {{ showThinking ? '收起思考过程' : '查看思考过程' }}
      </span>
      <div v-if="showThinking" class="ai-stream__thinking-content">
        {{ thinking }}
      </div>
    </div>

    <!-- Main content -->
    <div class="ai-stream__content">
      {{ displayContent }}
      <span v-if="loading" class="ai-stream__cursor">|</span>
    </div>
  </div>
</template>

<style scoped>
.ai-stream {
  line-height: 1.8;
}

.ai-stream__thinking-section {
  margin-bottom: 8px;
}

.ai-stream__thinking-content {
  padding: 8px 12px;
  background: var(--line-2);
  border-radius: 6px;
  font-size: 12px;
  color: var(--text-3);
  margin-top: 4px;
  white-space: pre-wrap;
}

.ai-stream__content {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  color: var(--text-1);
}

.ai-stream__cursor {
  animation: blink 1s step-end infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
</style>
