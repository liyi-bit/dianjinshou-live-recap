<script setup lang="ts">
defineProps<{
  questions: Array<{
    id?: number
    label: string
    value: string
  }>
}>()

const emit = defineEmits<{
  select: [value: string, id?: number]
}>()
</script>

<template>
  <div class="preset-bar">
    <span
      v-for="(q, idx) in questions"
      :key="idx"
      class="djschip"
      @click="emit('select', q.value, q.id)"
    >
      {{ q.label }}
    </span>
  </div>
</template>

<style scoped>
.preset-bar {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding: 8px 0;
  flex-wrap: nowrap;
}

.preset-bar::-webkit-scrollbar {
  height: 4px;
}

.preset-bar::-webkit-scrollbar-thumb {
  background: var(--line);
  border-radius: 2px;
}
</style>
