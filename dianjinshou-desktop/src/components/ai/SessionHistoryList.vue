<script setup lang="ts">
import { formatDateTime } from '@/utils/format'

defineProps<{
  sessions: Array<{
    id: number
    title: string
    messageCount: number
    lastMessageAt?: string | null
    createdAt: string
  }>
  activeId?: number | null
}>()

const emit = defineEmits<{
  select: [id: number]
  delete: [id: number]
}>()
</script>

<template>
  <div class="session-list">
    <div
      v-for="session in sessions"
      :key="session.id"
      class="session-item"
      :class="{ 'session-item--active': activeId === session.id }"
      @click="emit('select', session.id)"
    >
      <div class="session-item__title">{{ session.title || '未命名会话' }}</div>
      <div class="session-item__meta">
        <span>{{ session.messageCount }} 条消息</span>
        <span>{{ formatDateTime(session.createdAt) }}</span>
      </div>
      <button
        class="session-item__delete djsbtn ghost sm"
        @click.stop="emit('delete', session.id)"
      >
        删除
      </button>
    </div>
    <div v-if="sessions.length === 0" class="djsempty">
      <div class="ic">💬</div>
      <div class="ti">暂无会话记录</div>
    </div>
  </div>
</template>

<style scoped>
.session-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.session-item {
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  transition: background 0.15s;
  border: 1px solid transparent;
}

.session-item:hover {
  background: var(--line-2);
}

.session-item--active {
  background: var(--brand-soft);
  border-color: var(--brand);
}

.session-item__title {
  font-weight: 500;
  font-size: 13px;
  color: var(--text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding-right: 44px;
}

.session-item__meta {
  font-size: 12px;
  color: var(--text-3);
  display: flex;
  gap: 12px;
  margin-top: 4px;
}

.session-item__delete {
  position: absolute;
  top: 50%;
  right: 8px;
  transform: translateY(-50%);
  opacity: 0;
  transition: opacity 0.15s;
  border-color: var(--red);
  color: var(--red);
}

.session-item:hover .session-item__delete {
  opacity: 1;
}
</style>
