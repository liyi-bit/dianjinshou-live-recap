<script setup lang="ts">
import { ref } from 'vue'
import { Message } from '@arco-design/web-vue'
import type { AnalysisTask, AsrParagraph } from '@/api/analysis'
import { nowLocalDateTime, todayLocalDate } from '@/utils/format'

const props = defineProps<{
  visible: boolean
  taskId: number
  task?: AnalysisTask | null
  paragraphs?: AsrParagraph[]
  streamerName?: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
}>()

const contentSelection = ref<string[]>(['paragraphs'])

const CONTENT_OPTIONS = [
  { value: 'paragraphs', label: '分钟段落' },
  { value: 'ai_script', label: 'AI脚本拆解' },
  { value: 'optimized', label: '优化原文' }
]

function buildExportText(): string {
  const parts: string[] = []
  const name = props.streamerName || '主播'
  const sel = contentSelection.value

  parts.push(`【${name} - 直播话术导出】`)
  parts.push(`导出时间：${nowLocalDateTime()}`)
  parts.push('')

  if (sel.includes('paragraphs') && props.paragraphs?.length) {
    parts.push('═══════════════════════════════')
    parts.push('【分钟段落】')
    parts.push('═══════════════════════════════')
    parts.push('')
    for (const p of props.paragraphs) {
      parts.push(`[${p.startTime}] ${p.textContent}`)
    }
    parts.push('')
  }

  if (sel.includes('ai_script')) {
    parts.push('═══════════════════════════════')
    parts.push('【AI脚本拆解】')
    parts.push('═══════════════════════════════')
    parts.push('')
    if (props.paragraphs?.length) {
      // Group by scriptCategory
      const groups: Record<string, AsrParagraph[]> = {}
      for (const p of props.paragraphs) {
        const cat = p.scriptCategory || '其他'
        if (!groups[cat]) groups[cat] = []
        groups[cat].push(p)
      }
      for (const [cat, items] of Object.entries(groups)) {
        parts.push(`▸ ${cat}（${items.length}段）`)
        for (const p of items) {
          parts.push(`  [${p.startTime}] ${p.textContent}`)
        }
        parts.push('')
      }
    }
    if (props.task?.aiResult) {
      parts.push(props.task.aiResult)
      parts.push('')
    }
  }

  if (sel.includes('optimized') && props.task?.optimizedText) {
    parts.push('═══════════════════════════════')
    parts.push('【优化原文】')
    parts.push('═══════════════════════════════')
    parts.push('')
    parts.push(props.task.optimizedText)
    parts.push('')
  }

  return parts.join('\n')
}

function doExport() {
  if (contentSelection.value.length === 0) {
    Message.warning('请至少选择一项导出内容')
    return
  }

  const text = buildExportText()
  if (text.split('\n').length <= 4) {
    Message.warning('所选内容暂无数据')
    return
  }

  // Download as TXT via Blob
  const blob = new Blob(['\uFEFF' + text], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  const name = props.streamerName || '话术'
  const date = todayLocalDate()
  a.download = `${name}_话术导出_${date}.txt`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)

  Message.success('导出成功')
  emit('update:visible', false)
}
</script>

<template>
  <a-modal
    :visible="visible"
    title="导出话术"
    :width="440"
    @cancel="emit('update:visible', false)"
    :footer="false"
  >
    <a-form layout="vertical">
      <a-form-item label="选择导出内容">
        <a-checkbox-group v-model="contentSelection" direction="vertical">
          <a-checkbox v-for="opt in CONTENT_OPTIONS" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </a-checkbox>
        </a-checkbox-group>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="doExport" long>
          确认导出
        </a-button>
      </a-form-item>
    </a-form>
  </a-modal>
</template>
