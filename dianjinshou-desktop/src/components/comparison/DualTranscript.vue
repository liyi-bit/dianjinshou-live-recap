<script setup lang="ts">
import { ref } from 'vue'
import type { AsrParagraph } from '@/api/analysis'

defineProps<{
  paragraphs1: AsrParagraph[]
  paragraphs2: AsrParagraph[]
}>()

const scrollLocked = ref(true)

function onScroll1(e: Event) {
  if (!scrollLocked.value) return
  const target = e.target as HTMLElement
  const col2 = document.querySelector('.transcript-col-2') as HTMLElement
  if (col2) col2.scrollTop = target.scrollTop
}

function onScroll2(e: Event) {
  if (!scrollLocked.value) return
  const target = e.target as HTMLElement
  const col1 = document.querySelector('.transcript-col-1') as HTMLElement
  if (col1) col1.scrollTop = target.scrollTop
}
</script>

<template>
  <div class="dual-transcript">
    <div class="transcript-header">
      <span>双文本对齐</span>
      <a-checkbox v-model="scrollLocked">滚动锁定</a-checkbox>
    </div>
    <div class="transcript-columns">
      <div class="transcript-col transcript-col-1" @scroll="onScroll1">
        <div v-for="p in paragraphs1" :key="p.id" class="para-item">
          <span class="time">{{ p.startTime }}</span>
          <span class="text">{{ p.textContent }}</span>
        </div>
        <a-empty v-if="paragraphs1.length === 0" description="暂无段落" />
      </div>
      <div class="transcript-col transcript-col-2" @scroll="onScroll2">
        <div v-for="p in paragraphs2" :key="p.id" class="para-item">
          <span class="time">{{ p.startTime }}</span>
          <span class="text">{{ p.textContent }}</span>
        </div>
        <a-empty v-if="paragraphs2.length === 0" description="暂无段落" />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.dual-transcript {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;

  .transcript-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 0;
    font-weight: 600;
  }

  .transcript-columns {
    flex: 1;
    display: flex;
    gap: 8px;
    min-height: 0;
  }

  .transcript-col {
    flex: 1;
    overflow-y: auto;
    background: var(--color-bg-2);
    border-radius: 6px;
    padding: 8px;
  }

  .para-item {
    padding: 6px 0;
    border-bottom: 1px solid var(--color-border);
    font-size: 13px;

    .time {
      display: inline-block;
      background: rgb(var(--primary-6));
      color: #fff;
      padding: 1px 6px;
      border-radius: 3px;
      font-size: 11px;
      font-family: monospace;
      margin-right: 6px;
    }

    .text {
      line-height: 1.5;
    }
  }
}
</style>
