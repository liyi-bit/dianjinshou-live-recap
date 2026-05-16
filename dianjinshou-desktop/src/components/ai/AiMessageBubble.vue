<script setup lang="ts">
defineProps<{
  role: 'user' | 'assistant' | 'system'
  content: string
  thinking?: string | null
  timestamp?: string | null
}>()

const showThinking = defineModel<boolean>('showThinking', { default: false })
</script>

<template>
  <div class="ai-bubble" :class="'ai-bubble--' + role">
    <div class="ai-bubble__avatar" :class="'avatar--' + role">
      <span v-if="role === 'user'">U</span>
      <span v-else-if="role === 'assistant'">AI</span>
      <span v-else>S</span>
    </div>
    <div class="ai-bubble__body">
      <!-- Thinking toggle -->
      <div v-if="thinking" class="ai-bubble__thinking-toggle">
        <span class="djslink" @click="showThinking = !showThinking">
          {{ showThinking ? '收起思考过程' : '查看思考过程' }}
        </span>
      </div>
      <div v-if="thinking && showThinking" class="ai-bubble__thinking">
        {{ thinking }}
      </div>
      <div class="djsbubble" :class="role === 'user' ? 'me' : role === 'assistant' ? 'ai' : 'system'" v-html="content" />
      <div v-if="timestamp" class="ai-bubble__time">{{ timestamp }}</div>
    </div>
  </div>
</template>

<style scoped>
.ai-bubble {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.ai-bubble--user {
  flex-direction: row-reverse;
}

.ai-bubble--user .ai-bubble__body {
  align-items: flex-end;
}

.ai-bubble--system {
  justify-content: center;
}

.ai-bubble--system .ai-bubble__avatar {
  display: none;
}

.ai-bubble__avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  font-weight: bold;
  flex-shrink: 0;
}

.avatar--user {
  background: var(--brand);
}

.avatar--assistant {
  background: var(--green);
}

.avatar--system {
  background: var(--text-3);
}

.ai-bubble__body {
  display: flex;
  flex-direction: column;
}

.ai-bubble__thinking-toggle {
  margin-bottom: 4px;
}

.ai-bubble__thinking {
  padding: 8px 12px;
  background: var(--line-2);
  border-radius: 8px;
  font-size: 12px;
  color: var(--text-3);
  margin-bottom: 4px;
  white-space: pre-wrap;
  max-width: 70%;
}

.ai-bubble__time {
  font-size: 11px;
  color: var(--text-4);
  margin-top: 4px;
}

/* system bubble override */
.djsbubble.system {
  background: var(--line-2);
  font-size: 12px;
  color: var(--text-3);
  border: 1px solid var(--line);
  border-radius: 12px;
}
</style>
