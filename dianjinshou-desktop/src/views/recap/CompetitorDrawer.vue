<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import {
  createCompetitorReport,
  getCompetitorReport,
  type CompetitorReport,
  type CompetitorReportData
} from '@/api/competitor'
import { Message } from '@arco-design/web-vue'
import CompetitorSelectDialog from './CompetitorSelectDialog.vue'
import CompetitorCompareChart from './CompetitorCompareChart.vue'

const props = defineProps<{
  visible: boolean
  taskId: number
  streamerId: number
  industryId: number | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const report = ref<CompetitorReport | null>(null)
const reportData = ref<CompetitorReportData | null>(null)
const loading = ref(false)
const showSelectDialog = ref(false)

const parsedReport = computed(() => {
  if (!report.value?.report) return null
  try {
    return JSON.parse(report.value.report) as CompetitorReportData
  } catch {
    return null
  }
})

async function handleSelectCompetitor(competitorId: number) {
  loading.value = true
  try {
    const res = await createCompetitorReport({
      streamerId: props.streamerId,
      competitorStreamerId: competitorId
    })
    report.value = (res as any).data ?? res
    Message.success('竞品分析报告已生成')
  } catch {
    Message.error('生成竞品分析失败')
  } finally {
    loading.value = false
  }
}

function handleStartAnalysis() {
  showSelectDialog.value = true
}

function handleClose() {
  report.value = null
  emit('update:visible', false)
}
</script>

<template>
  <a-drawer
    :visible="visible"
    title="竞品分析报告"
    :width="640"
    @cancel="handleClose"
    :footer="false"
    unmount-on-close
  >
    <a-spin :loading="loading" style="width: 100%">
      <!-- No report yet -->
      <div v-if="!report" class="competitor-empty">
        <a-empty description="选择竞品主播生成对比报告" />
        <a-button type="primary" @click="handleStartAnalysis" style="margin-top: 16px">
          选择竞品主播
        </a-button>
      </div>

      <!-- Report content -->
      <div v-else class="competitor-report">
        <!-- Header: streamer vs competitor -->
        <div class="vs-header">
          <div class="vs-card">
            <a-avatar :size="48" :image-url="report.streamerAvatar || undefined">{{ report.streamerAvatar ? '' : (report.streamerName || '').charAt(0) }}</a-avatar>
            <div class="vs-name">{{ report.streamerName }}</div>
          </div>
          <div class="vs-label">VS</div>
          <div class="vs-card">
            <a-avatar :size="48" :image-url="report.competitorStreamerAvatar || undefined">{{ report.competitorStreamerAvatar ? '' : (report.competitorStreamerName || '').charAt(0) }}</a-avatar>
            <div class="vs-name">{{ report.competitorStreamerName }}</div>
          </div>
        </div>

        <!-- Compare chart -->
        <div v-if="parsedReport?.dimensions" class="chart-section">
          <h4>8维度对比</h4>
          <CompetitorCompareChart
            :dimensions="parsedReport.dimensions"
            :my-name="report.streamerName"
            :competitor-name="report.competitorStreamerName"
          />
        </div>

        <!-- Highlights -->
        <div v-if="parsedReport?.highlights?.length" class="section">
          <h4>学习亮点</h4>
          <ul>
            <li v-for="(h, i) in parsedReport.highlights" :key="i">{{ h }}</li>
          </ul>
        </div>

        <!-- Improvements -->
        <div v-if="parsedReport?.improvements?.length" class="section">
          <h4>改进方向</h4>
          <ul>
            <li v-for="(imp, i) in parsedReport.improvements" :key="i">{{ imp }}</li>
          </ul>
        </div>

        <!-- AI Summary -->
        <div v-if="parsedReport?.summary" class="section">
          <h4>AI 总结</h4>
          <p class="summary-text">{{ parsedReport.summary }}</p>
        </div>
      </div>
    </a-spin>

    <CompetitorSelectDialog
      v-model:visible="showSelectDialog"
      :streamer-id="streamerId"
      :industry-id="industryId"
      @select="handleSelectCompetitor"
    />
  </a-drawer>
</template>

<style scoped lang="scss">
.competitor-empty {
  text-align: center;
  padding: 60px 0;
}

.competitor-report {
  .vs-header {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 24px;
    margin-bottom: 24px;
    padding: 20px;
    background: var(--color-fill-1);
    border-radius: 12px;
  }

  .vs-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
  }

  .vs-name {
    font-weight: 600;
    font-size: 16px;
  }

  .vs-label {
    font-size: 24px;
    font-weight: 700;
    color: var(--color-text-3);
  }

  .chart-section {
    margin-bottom: 24px;
    h4 { margin-bottom: 12px; }
  }

  .section {
    margin-bottom: 20px;
    h4 { margin-bottom: 8px; }
    ul { padding-left: 20px; }
    li { margin-bottom: 4px; color: var(--color-text-2); }
  }

  .summary-text {
    color: var(--color-text-2);
    line-height: 1.6;
  }
}
</style>
