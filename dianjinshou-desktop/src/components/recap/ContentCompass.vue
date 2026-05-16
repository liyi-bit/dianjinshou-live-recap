<script setup lang="ts">
import { ref, computed, onMounted, watch, shallowRef } from 'vue'
import * as echarts from 'echarts/core'
import { RadarChart, BarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  RadarComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import * as analysisApi from '@/api/analysis'
import type { AnalysisTask, AsrParagraph } from '@/api/analysis'

echarts.use([
  RadarChart, BarChart,
  TitleComponent, TooltipComponent, LegendComponent, GridComponent, RadarComponent,
  CanvasRenderer
])

const props = defineProps<{
  taskId: number
  task: AnalysisTask | null
  paragraphs: AsrParagraph[]
}>()

// ── 内容占比：从段落 scriptCategory 统计 ──
const CONTENT_CATEGORIES = [
  { key: '话题话术', label: '话题话术', color: '#4A6896' },
  { key: '行动指令', label: '行动指令', color: '#ff7d00' },
  { key: '留人话术', label: '留人话术', color: '#0fc6c2' },
  { key: '塑品话术', label: '塑品话术', color: '#f53f3f' },
  { key: '互动话术', label: '互动话术', color: '#722ed1' },
  { key: '促单话术', label: '促单话术', color: '#00b42a' },
  { key: '逼单话术', label: '逼单话术', color: '#eb0aa4' },
  { key: '福利话术', label: '福利话术', color: '#fadc19' },
  { key: '开场话术', label: '开场话术', color: '#3491fa' },
  { key: '人设话术', label: '人设话术', color: '#8693A4' },
]

// ── 直播节奏分类 ──
const RHYTHM_CATEGORIES = [
  { key: '开场暖场', label: '开场暖场', color: '#4A6896' },
  { key: '产品讲解', label: '产品讲解', color: '#ff7d00' },
  { key: '互动环节', label: '互动环节', color: '#722ed1' },
  { key: '促单逼单', label: '促单逼单', color: '#00b42a' },
  { key: '福利发放', label: '福利发放', color: '#fadc19' },
  { key: '过渡衔接', label: '过渡衔接', color: '#0fc6c2' },
  { key: '闲聊休息', label: '闲聊休息', color: '#8693A4' },
]

// ── 雷达图维度 ──
const RADAR_DIMENSIONS = [
  { key: 'opening', label: '开场力' },
  { key: 'retention', label: '留人力' },
  { key: 'product', label: '塑品力' },
  { key: 'promotion', label: '促单力' },
  { key: 'interaction', label: '互动力' },
  { key: 'expression', label: '表达力' },
]

// ── 解析内容罗盘数据 ──
interface CompassData {
  contentRatio: { score: number; items: { name: string; percent: number }[] }
  liveRhythm: { score: number; items: { name: string; percent: number }[] }
}

const compassData = computed<CompassData | null>(() => {
  // 优先使用 AI 生成的 contentCompass 数据
  if (props.task?.contentCompass) {
    try {
      const parsed = JSON.parse(props.task.contentCompass)
      if (parsed.contentRatio && parsed.liveRhythm) {
        return parsed as CompassData
      }
    } catch { /* fallback to paragraph-based computation */ }
  }

  // 从段落 scriptCategory 计算内容占比（仅在有分类数据时）
  const paras = props.paragraphs
  if (!paras || paras.length === 0) return null

  // 检查是否有真实的 scriptCategory 数据
  const hasCategory = paras.some(p => p.scriptCategory)
  if (!hasCategory) return null

  const total = paras.length
  const catCount: Record<string, number> = {}
  for (const p of paras) {
    const cat = p.scriptCategory || '其他'
    catCount[cat] = (catCount[cat] || 0) + 1
  }

  const contentItems = CONTENT_CATEGORIES.map(c => ({
    name: c.label,
    percent: Math.round(((catCount[c.key] || 0) / total) * 1000) / 10
  }))
  const otherCount = catCount['其他'] || 0
  if (otherCount > 0) {
    contentItems.push({ name: '其他', percent: Math.round((otherCount / total) * 1000) / 10 })
  }

  const totalContentPercent = contentItems.reduce((s, i) => s + i.percent, 0)
  const contentScore = totalContentPercent > 0 ? Math.round(totalContentPercent) : 0

  return {
    contentRatio: { score: contentScore, items: contentItems },
    liveRhythm: null as any // 直播节奏需要AI生成，无数据时不显示
  }
})

// ── 雷达图数据：从 aiResult dimensions 提取 ──
const radarScores = computed(() => {
  if (!props.task?.aiResult) return null
  try {
    const parsed = JSON.parse(props.task.aiResult)
    const dims = parsed.dimensions
    if (!dims) return null
    return RADAR_DIMENSIONS.map(d => ({
      label: d.label,
      score: dims[d.key]?.score ?? 0
    }))
  } catch { return null }
})

// ── ECharts 实例 ──
const radarChartRef = ref<HTMLElement>()
const barChartRef = ref<HTMLElement>()
let radarChart: echarts.ECharts | null = null
let barChart: echarts.ECharts | null = null

function renderRadarChart() {
  if (!radarChartRef.value || !radarScores.value) return
  if (!radarChart) {
    radarChart = echarts.init(radarChartRef.value)
  }

  const scores = radarScores.value
  radarChart.setOption({
    radar: {
      indicator: scores.map(s => ({ name: s.label, max: 100 })),
      shape: 'polygon',
      radius: '65%',
      axisName: {
        color: getComputedStyle(document.documentElement).getPropertyValue('--text-2b').trim() || '#556270',
        fontSize: 11
      },
      splitArea: {
        areaStyle: { color: ['rgba(22,93,255,0.02)', 'rgba(22,93,255,0.05)'] }
      },
      splitLine: {
        lineStyle: { color: getComputedStyle(document.documentElement).getPropertyValue('--line').trim() || '#E2E7EF' }
      },
      axisLine: {
        lineStyle: { color: getComputedStyle(document.documentElement).getPropertyValue('--line').trim() || '#E2E7EF' }
      }
    },
    series: [{
      type: 'radar',
      data: [{
        value: scores.map(s => s.score),
        name: '本场直播',
        areaStyle: {
          color: 'rgba(22,93,255,0.15)'
        },
        lineStyle: {
          color: getComputedStyle(document.documentElement).getPropertyValue('--brand').trim() || '#B8823A',
          width: 2
        },
        itemStyle: {
          color: getComputedStyle(document.documentElement).getPropertyValue('--brand').trim() || '#B8823A'
        }
      }]
    }],
    tooltip: {
      trigger: 'item'
    }
  })
}

function renderBarChart() {
  if (!barChartRef.value || !compassData.value) return
  if (!barChart) {
    barChart = echarts.init(barChartRef.value)
  }

  const items = compassData.value.contentRatio.items.filter(i => i.percent > 0)
  const colors = ['#4A6896', '#F76560', '#0FC6C2', '#FF7D00', '#722ED1', '#00B42A', '#EB0AA4']

  barChart.setOption({
    grid: {
      left: 60,
      right: 20,
      top: 30,
      bottom: 40
    },
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const p = Array.isArray(params) ? params[0] : params
        return `${p.name}: ${p.value}%`
      }
    },
    xAxis: {
      type: 'category',
      data: items.map(i => i.name),
      axisLabel: {
        fontSize: 11,
        color: getComputedStyle(document.documentElement).getPropertyValue('--text-3').trim() || '#8693A4',
        rotate: items.length > 6 ? 30 : 0
      },
      axisLine: { lineStyle: { color: getComputedStyle(document.documentElement).getPropertyValue('--line').trim() || '#E2E7EF' } },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        fontSize: 11,
        color: getComputedStyle(document.documentElement).getPropertyValue('--text-3').trim() || '#8693A4',
        formatter: '{value}%'
      },
      splitLine: { lineStyle: { color: getComputedStyle(document.documentElement).getPropertyValue('--line').trim() || '#E2E7EF' } }
    },
    series: [{
      type: 'bar',
      data: items.map((item, idx) => ({
        value: item.percent,
        itemStyle: { color: colors[idx % colors.length], borderRadius: [3, 3, 0, 0] }
      })),
      barWidth: 28,
      label: {
        show: true,
        position: 'top',
        formatter: '{c}%',
        fontSize: 11,
        color: getComputedStyle(document.documentElement).getPropertyValue('--text-2b').trim() || '#556270'
      }
    }]
  })
}

function handleResize() {
  radarChart?.resize()
  barChart?.resize()
}

watch([compassData, radarScores], () => {
  setTimeout(() => {
    renderRadarChart()
    renderBarChart()
  }, 100)
})

onMounted(() => {
  setTimeout(() => {
    renderRadarChart()
    renderBarChart()
  }, 200)
  window.addEventListener('resize', handleResize)
})

// Cleanup
import { onUnmounted } from 'vue'
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  radarChart?.dispose()
  barChart?.dispose()
})
</script>

<template>
  <div class="content-compass">
    <!-- 无数据 -->
    <div v-if="!compassData && !radarScores" class="compass-empty">
      <div class="empty-icon">📊</div>
      <div class="empty-text">暂无内容罗盘数据，请等待分析完成</div>
    </div>

    <template v-else>
      <!-- ===== 上部：占比 + 雷达图 ===== -->
      <div class="compass-top">
        <!-- 左侧：内容占比 + 直播节奏 -->
        <div class="ratio-area">
          <!-- 内容占比 -->
          <div v-if="compassData" class="ratio-block">
            <div class="ratio-header">
              <span class="ratio-title">内容占比</span>
              <span class="ratio-score">{{ compassData.contentRatio.score }}%</span>
            </div>
            <div class="ratio-grid">
              <div
                v-for="item in compassData.contentRatio.items"
                :key="item.name"
                class="ratio-item"
              >
                <span class="ratio-label">{{ item.name }}</span>
                <span class="ratio-value">{{ item.percent }}%</span>
              </div>
            </div>
          </div>

          <!-- 直播节奏 -->
          <div v-if="compassData?.liveRhythm" class="ratio-block">
            <div class="ratio-header">
              <span class="ratio-title">直播节奏</span>
              <span class="ratio-score">{{ compassData.liveRhythm.score }}%</span>
            </div>
            <div class="ratio-grid">
              <div
                v-for="item in compassData.liveRhythm.items"
                :key="item.name"
                class="ratio-item"
              >
                <span class="ratio-label">{{ item.name }}</span>
                <span class="ratio-value">{{ item.percent }}%</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：雷达图 -->
        <div class="radar-area">
          <div ref="radarChartRef" class="radar-chart" />
        </div>
      </div>

      <!-- ===== 下部：柱状图 ===== -->
      <div class="compass-bottom">
        <div class="bar-title">话术占比分布</div>
        <div ref="barChartRef" class="bar-chart" />
      </div>
    </template>
  </div>
</template>

<style scoped>
.content-compass {
  padding: 16px;
}

.compass-empty {
  text-align: center;
  padding: 40px 20px;
  color: var(--text-3);
}

.empty-icon {
  font-size: 36px;
  margin-bottom: 8px;
}

.empty-text {
  font-size: 13px;
}

/* ===== 上部布局 ===== */
.compass-top {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.ratio-area {
  flex: 1;
  display: flex;
  gap: 12px;
  min-width: 0;
}

.radar-area {
  width: 240px;
  flex-shrink: 0;
}

.radar-chart {
  width: 240px;
  height: 200px;
}

/* ===== 占比块 ===== */
.ratio-block {
  flex: 1;
  background: var(--bg);
  border-radius: 8px;
  padding: 12px;
  min-width: 0;
}

.ratio-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--line);
}

.ratio-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-1);
}

.ratio-score {
  font-size: 14px;
  font-weight: 700;
  color: var(--brand);
}

.ratio-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px 12px;
}

.ratio-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 3px 0;
}

.ratio-label {
  font-size: 12px;
  color: var(--text-2b);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ratio-value {
  font-size: 12px;
  color: var(--text-1);
  font-weight: 500;
  margin-left: 4px;
  white-space: nowrap;
}

/* ===== 下部柱状图 ===== */
.compass-bottom {
  border-top: 1px solid var(--line);
  padding-top: 12px;
}

.bar-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-1);
  margin-bottom: 8px;
}

.bar-chart {
  width: 100%;
  height: 220px;
}
</style>
