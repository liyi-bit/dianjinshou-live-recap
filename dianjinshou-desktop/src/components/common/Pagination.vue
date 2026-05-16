<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  page: number
  size: number
  total: number
  /** 显示翻页中间的数字按钮数量，默认 5 */
  window?: number
}>(), {
  window: 5,
})

const emit = defineEmits<{
  (e: 'update:page', page: number): void
  (e: 'change', page: number): void
}>()

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / Math.max(1, props.size))))

const visiblePages = computed(() => {
  const tp = totalPages.value
  const cur = Math.min(Math.max(1, props.page), tp)
  const win = Math.max(1, props.window)
  let from = Math.max(1, cur - Math.floor(win / 2))
  let to = Math.min(tp, from + win - 1)
  from = Math.max(1, to - win + 1)
  const out: number[] = []
  for (let p = from; p <= to; p++) out.push(p)
  return out
})

function go(p: number) {
  const clamped = Math.min(Math.max(1, p), totalPages.value)
  if (clamped === props.page) return
  emit('update:page', clamped)
  emit('change', clamped)
}
</script>

<template>
  <div v-if="total > 0" class="djspager">
    <span class="info">共 {{ total }} 条</span>
    <button class="btn" :disabled="page <= 1" @click="go(page - 1)">‹ 上一页</button>
    <button
      v-if="visiblePages[0] > 1"
      class="btn num"
      @click="go(1)"
    >1</button>
    <span v-if="visiblePages[0] > 2" class="ellipsis">…</span>
    <button
      v-for="p in visiblePages"
      :key="p"
      class="btn num"
      :class="{ active: p === page }"
      @click="go(p)"
    >{{ p }}</button>
    <span v-if="visiblePages[visiblePages.length - 1] < totalPages - 1" class="ellipsis">…</span>
    <button
      v-if="visiblePages[visiblePages.length - 1] < totalPages"
      class="btn num"
      @click="go(totalPages)"
    >{{ totalPages }}</button>
    <button class="btn" :disabled="page >= totalPages" @click="go(page + 1)">下一页 ›</button>
  </div>
</template>

<style scoped>
.djspager {
  display: flex; align-items: center; gap: 6px;
  padding: 14px 16px;
  font-size: 12.5px;
  flex-wrap: wrap;
}
.djspager .info {
  color: var(--text-3);
  margin-right: 8px;
}
.djspager .btn {
  min-width: 32px;
  height: 28px; padding: 0 10px;
  border: 1px solid var(--line);
  background: var(--card);
  color: var(--text-2);
  border-radius: 6px;
  cursor: pointer;
  font-size: 12.5px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background .15s, color .15s, border-color .15s;
}
.djspager .btn:hover:not(:disabled) {
  background: var(--card-hover, #FBF8F1);
  color: var(--text-1);
  border-color: var(--line-3, #D8CFB8);
}
.djspager .btn.num.active {
  background: var(--brand, #B8823A);
  color: #fff;
  border-color: var(--brand, #B8823A);
  font-weight: 600;
}
.djspager .btn:disabled {
  opacity: .4;
  cursor: not-allowed;
}
.djspager .ellipsis {
  color: var(--text-3);
  padding: 0 2px;
}
</style>
