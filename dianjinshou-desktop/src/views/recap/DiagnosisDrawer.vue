<script setup lang="ts">
import { ref, watch } from 'vue'
import { generateDiagnosis, getDiagnosisReport, type DiagnosisReport } from '@/api/diagnosis'
import { Message } from '@arco-design/web-vue'
import DiagnosisRadarChart from './DiagnosisRadarChart.vue'
import DiagnosisDimensionCard from './DiagnosisDimensionCard.vue'

const props = defineProps<{
  visible: boolean
  taskId: number
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const report = ref<DiagnosisReport | null>(null)
const loading = ref(false)

async function loadReport() {
  loading.value = true
  try {
    const res = await getDiagnosisReport(props.taskId)
    report.value = (res as any).data ?? res
  } catch {
    report.value = null
  } finally {
    loading.value = false
  }
}

async function handleGenerate() {
  loading.value = true
  try {
    const res = await generateDiagnosis(props.taskId)
    report.value = (res as any).data ?? res
    Message.success('诊断报告已生成')
  } catch {
    Message.error('生成诊断报告失败')
  } finally {
    loading.value = false
  }
}

function handleClose() {
  emit('update:visible', false)
}

function getScoreColor(score: number): string {
  if (score >= 80) return 'var(--green-6, #00b42a)'
  if (score >= 60) return 'var(--orange-6, #ff7d00)'
  return 'var(--red-6, #f53f3f)'
}

watch(() => props.visible, (val) => {
  if (val) loadReport()
})
</script>

<template>
  <a-drawer
    :visible="visible"
    title="AI 数据诊断"
    :width="640"
    @cancel="handleClose"
    :footer="false"
    unmount-on-close
  >
    <a-spin :loading="loading" style="width: 100%">
      <!-- Not generated yet -->
      <div v-if="!report || report.status === 'not_generated'" class="diagnosis-empty">
        <a-empty description="暂未生成诊断报告" />
        <a-button type="primary" :loading="loading" @click="handleGenerate" style="margin-top: 16px">
          生成数据诊断
        </a-button>
      </div>

      <!-- Report content -->
      <div v-else class="diagnosis-report">
        <!-- Overall score -->
        <div class="overall-section">
          <div class="overall-score" :style="{ color: getScoreColor(report.overallScore) }">
            {{ report.overallScore }}
          </div>
          <div class="overall-label">综合评分</div>
          <div class="overall-comment">{{ report.overallComment }}</div>
        </div>

        <!-- Radar chart -->
        <div class="radar-section">
          <h4>12维度雷达图</h4>
          <DiagnosisRadarChart
            v-if="report.radarLabels && report.radarData"
            :labels="report.radarLabels"
            :data="report.radarData"
          />
        </div>

        <!-- Dimension details -->
        <div class="dimensions-section">
          <h4>维度详情</h4>
          <DiagnosisDimensionCard
            v-for="dim in report.dimensions"
            :key="dim.name"
            :dimension="dim"
          />
        </div>
      </div>
    </a-spin>
  </a-drawer>
</template>

<style scoped lang="scss">
.diagnosis-empty {
  text-align: center;
  padding: 60px 0;
}

.diagnosis-report {
  .overall-section {
    text-align: center;
    margin-bottom: 24px;
    padding: 20px;
    background: var(--color-fill-1);
    border-radius: 12px;
  }

  .overall-score {
    font-size: 56px;
    font-weight: 700;
    line-height: 1.2;
  }

  .overall-label {
    font-size: 14px;
    color: var(--color-text-3);
    margin-top: 4px;
  }

  .overall-comment {
    font-size: 14px;
    color: var(--color-text-2);
    margin-top: 8px;
  }

  .radar-section {
    margin-bottom: 24px;
    h4 { margin-bottom: 12px; }
  }

  .dimensions-section {
    h4 { margin-bottom: 12px; }
  }
}
</style>
