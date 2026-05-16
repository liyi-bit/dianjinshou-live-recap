<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Message } from '@arco-design/web-vue'
import { classifyParagraphs as classifyParagraphsApi } from '@/api/analysis'
import type { AsrParagraph } from '@/api/analysis'

const props = defineProps<{
  taskId: number
  paragraphs: AsrParagraph[]
}>()

const emit = defineEmits<{
  (e: 'refresh'): void
}>()

const classifying = ref(false)

async function triggerClassify() {
  if (classifying.value) return
  classifying.value = true
  try {
    await classifyParagraphsApi(props.taskId)
    Message.success('话术分类完成')
    emit('refresh')
  } catch (e: any) {
    Message.error(e?.message || '分类失败')
  } finally {
    classifying.value = false
  }
}

// Auto-classify if paragraphs exist but none have scriptCategory
const hasCategory = computed(() => props.paragraphs?.some(p => p.scriptCategory))
const autoTriggered = ref(false)

watch(() => [props.paragraphs, props.taskId], () => {
  if (props.paragraphs?.length && !hasCategory.value && !autoTriggered.value && !classifying.value) {
    autoTriggered.value = true
    triggerClassify()
  }
}, { immediate: true })

// 话术分类定义（与爱复盘一致）
const SCRIPT_CATEGORIES = [
  { key: '话题话术', label: '话题话术', color: '#4A6896', desc: '用于发起新话题和转换话题，通过一些趋势话题、周边故事拓展讨论范围和深度' },
  { key: '行动指令', label: '行动指令', color: '#ff7d00', desc: '引导观众执行特定操作，如关注、点赞、加入粉丝团等' },
  { key: '留人话术', label: '留人话术', color: '#0fc6c2', desc: '延长观众停留时间的话术，重点营造好奇和悬念氛围' },
  { key: '塑品话术', label: '塑品话术', color: '#f53f3f', desc: '产品价值塑造与卖点提炼，突出产品优势和使用体验' },
  { key: '互动话术', label: '互动话术', color: '#722ed1', desc: '提升直播间互动率的话术，引导弹幕、评论和互动' },
  { key: '促单话术', label: '促单话术', color: '#00b42a', desc: '促进下单转化的话术，营造紧迫感和限时优惠' },
  { key: '逼单话术', label: '逼单话术', color: '#eb0aa4', desc: '更强力的促单话术，利用限量、倒计时等方式逼单' },
  { key: '福利话术', label: '福利话术', color: '#fadc19', desc: '福利发放和活动预告，吸引观众互动' },
  { key: '开场话术', label: '开场话术', color: '#3491fa', desc: '直播间开场引入话术，快速吸引停留' },
  { key: '人设话术', label: '人设话术', color: '#8693A4', desc: '主播个人人设塑造，增加信任感和亲和力' },
]

// 将段落按 scriptCategory 分组（如果没有分类则自动 mock 分配）
const categorizedParagraphs = computed(() => {
  const paras = props.paragraphs
  if (!paras || paras.length === 0) return []

  // 检查是否已有 scriptCategory 数据
  const hasCategory = paras.some(p => p.scriptCategory)

  let grouped: Record<string, AsrParagraph[]>

  if (hasCategory) {
    grouped = {}
    for (const p of paras) {
      const cat = p.scriptCategory || '其他'
      if (!grouped[cat]) grouped[cat] = []
      grouped[cat].push(p)
    }
  } else {
    // 没有分类数据，全部归为"其他"
    grouped = { '其他': [...paras] }
  }

  // 按 SCRIPT_CATEGORIES 顺序排列
  const result: Array<{ category: typeof SCRIPT_CATEGORIES[0]; paragraphs: AsrParagraph[] }> = []
  for (const cat of SCRIPT_CATEGORIES) {
    if (grouped[cat.key]) {
      result.push({ category: cat, paragraphs: grouped[cat.key] })
    }
  }
  // 其他未分类
  if (grouped['其他']) {
    result.push({
      category: { key: '其他', label: '其他话术', color: '#8693A4', desc: '未归类的话术内容' },
      paragraphs: grouped['其他']
    })
  }
  return result
})

// 汇总分析文本
const summaryText = computed(() => {
  const total = props.paragraphs?.length || 0
  if (total === 0) return ''
  const cats = categorizedParagraphs.value
  const parts = cats.map(c => `【${c.category.label}】${c.paragraphs.length}段`)
  return `本场直播共分析${total}个段落，其中包含：${parts.join('、')}。整体话术覆盖了直播间运营的主要场景。`
})
</script>

<template>
  <div class="script-breakdown">
    <!-- 无数据 -->
    <div v-if="!paragraphs || paragraphs.length === 0" class="empty-state">
      <div class="empty-icon">📝</div>
      <div class="empty-text">暂无脚本数据，请等待分析完成</div>
    </div>

    <!-- 分类中 -->
    <div v-else-if="classifying" class="empty-state">
      <a-spin :size="32" />
      <div class="empty-text" style="margin-top: 12px">正在进行话术分类...</div>
    </div>

    <!-- 有数据 -->
    <template v-else>
      <!-- 总结 -->
      <div class="sb-summary">
        <div class="sb-summary-title">话术分析</div>
        <div class="sb-summary-text">{{ summaryText }}</div>
      </div>

      <!-- 分类段落 -->
      <div v-for="(group, idx) in categorizedParagraphs" :key="group.category.key" class="sb-section">
        <div class="sb-section-header">
          <span class="sb-section-num">({{ idx + 1 }})</span>
          <span class="sb-section-badge" :style="{ background: group.category.color }">{{ group.category.label }}</span>
          <span class="sb-section-desc">{{ group.category.desc }}</span>
        </div>
        <div class="sb-section-body">
          <div v-for="p in group.paragraphs" :key="p.id" class="sb-para">
            <span class="sb-para-time">{{ p.startTime }}</span>
            <span class="sb-para-text">{{ p.textContent }}</span>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.script-breakdown {
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

.sb-summary {
  padding: 12px 16px;
  margin-bottom: 8px;
}

.sb-summary-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-1);
  margin-bottom: 8px;
}

.sb-summary-text {
  font-size: 13px;
  line-height: 1.8;
  color: var(--text-2b);
}

.sb-section {
  padding: 0 16px;
  margin-bottom: 20px;
}

.sb-section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.sb-section-num {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
}

.sb-section-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: var(--radius-lg);
  color: #fff;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.sb-section-desc {
  font-size: 12px;
  color: var(--text-3);
}

.sb-section-body {
  padding-left: 4px;
}

.sb-para {
  display: flex;
  gap: 10px;
  padding: 6px 0;
  font-size: 13px;
  line-height: 1.7;
  border-bottom: 1px solid var(--line);
}

.sb-para:last-child {
  border-bottom: none;
}

.sb-para-time {
  color: var(--brand);
  font-size: 12px;
  white-space: nowrap;
  flex-shrink: 0;
  margin-top: 2px;
}

.sb-para-text {
  color: var(--text-1);
}
</style>
