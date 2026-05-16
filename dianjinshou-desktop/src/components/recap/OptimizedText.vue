<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Message } from '@arco-design/web-vue'
import * as analysisApi from '@/api/analysis'
import type { AsrParagraph } from '@/api/analysis'

const props = defineProps<{
  taskId: number
  paragraphs: AsrParagraph[]
  /** Pre-loaded optimized text from task (if available) */
  optimizedText?: string | null
}>()

// 子 tab：优化原文 / 原文
const subTab = ref<'optimized' | 'original'>('optimized')
const loading = ref(false)
const generatedText = ref<string | null>(props.optimizedText ?? null)
const hasRequested = ref(false)

// 原文：拼接所有段落
const originalText = computed(() => {
  if (!props.paragraphs || props.paragraphs.length === 0) return ''
  return props.paragraphs.map(p => p.textContent).join('')
})

// 当切换到优化原文 tab 且尚未生成时，自动请求
watch(subTab, (val) => {
  if (val === 'optimized' && !generatedText.value && !hasRequested.value) {
    generateOptimized()
  }
}, { immediate: true })

// 如果 props 传入了已有的优化文本，直接使用
watch(() => props.optimizedText, (val) => {
  if (val) generatedText.value = val
})

async function generateOptimized() {
  if (loading.value) return
  hasRequested.value = true
  loading.value = true
  try {
    const res = await analysisApi.generateOptimizedText(props.taskId)
    const data = (res as any).data ?? res
    generatedText.value = typeof data === 'string' ? data : (data?.data ?? data?.toString() ?? '')
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '生成优化原文失败'
    Message.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="optimized-text">
    <!-- 无数据 -->
    <div v-if="!paragraphs || paragraphs.length === 0" class="empty-state">
      <div class="empty-icon">✨</div>
      <div class="empty-text">暂无文本数据，请等待分析完成</div>
    </div>

    <!-- 有数据 -->
    <template v-else>
      <!-- 子 Tab 切换 -->
      <div class="ot-tabs">
        <span
          class="ot-tab"
          :class="{ active: subTab === 'optimized' }"
          @click="subTab = 'optimized'"
        >优化原文</span>
        <span
          class="ot-tab"
          :class="{ active: subTab === 'original' }"
          @click="subTab = 'original'"
        >原文</span>
      </div>

      <!-- 优化原文 -->
      <div v-if="subTab === 'optimized'" class="ot-content">
        <!-- Loading -->
        <div v-if="loading" class="ot-loading">
          <a-spin dot />
          <span class="ot-loading-text">AI正在优化原文，请稍候...</span>
        </div>
        <!-- 已生成 -->
        <div v-else-if="generatedText" class="ot-text">{{ generatedText }}</div>
        <!-- 未生成 -->
        <div v-else class="ot-generate">
          <div class="ot-generate-desc">点击下方按钮，AI将对原文进行优化整理：去除语气词、修正标点、优化句子结构。</div>
          <a-button type="primary" @click="generateOptimized">生成优化原文</a-button>
        </div>
      </div>

      <!-- 原文 -->
      <div v-if="subTab === 'original'" class="ot-content">
        <div class="ot-text">{{ originalText }}</div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.optimized-text {
  padding: 4px 0;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--text-3);
}

.empty-icon {
  font-size: 40px;
  margin-bottom: 12px;
}

.empty-text {
  font-size: 14px;
}

.ot-tabs {
  display: flex;
  gap: 0;
  padding: 0 16px;
  border-bottom: 1px solid var(--line);
  margin-bottom: 4px;
}

.ot-tab {
  padding: 8px 16px;
  font-size: 13px;
  color: var(--text-3);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  font-weight: 500;
  transition: color 0.2s;
}

.ot-tab:hover {
  color: var(--text-2b);
}

.ot-tab.active {
  color: var(--brand);
  border-bottom-color: var(--brand);
  font-weight: 600;
}

.ot-content {
  padding: 16px;
}

.ot-text {
  font-size: 14px;
  line-height: 2;
  color: var(--text-1);
  white-space: pre-wrap;
  word-break: break-all;
}

.ot-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 16px;
}

.ot-loading-text {
  font-size: 13px;
  color: var(--text-3);
}

.ot-generate {
  text-align: center;
  padding: 40px 20px;
}

.ot-generate-desc {
  font-size: 13px;
  color: var(--text-3);
  margin-bottom: 16px;
  line-height: 1.6;
}
</style>
