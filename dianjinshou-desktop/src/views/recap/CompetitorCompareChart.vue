<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import type { CompetitorDimension } from '@/api/competitor'

const props = defineProps<{
  dimensions: CompetitorDimension[]
  myName: string
  competitorName: string
}>()

const canvasRef = ref<HTMLCanvasElement | null>(null)

function drawChart() {
  const canvas = canvasRef.value
  if (!canvas || !props.dimensions.length) return

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const w = canvas.width
  const h = canvas.height
  const padding = { top: 40, right: 20, bottom: 40, left: 100 }
  const chartW = w - padding.left - padding.right
  const chartH = h - padding.top - padding.bottom
  const n = props.dimensions.length
  const barGroupH = chartH / n
  const barH = barGroupH * 0.3

  ctx.clearRect(0, 0, w, h)

  // Draw bars
  for (let i = 0; i < n; i++) {
    const dim = props.dimensions[i]
    const y = padding.top + i * barGroupH

    // Label
    ctx.fillStyle = '#0F1726'
    ctx.font = '12px sans-serif'
    ctx.textAlign = 'right'
    ctx.fillText(dim.name, padding.left - 8, y + barGroupH / 2 + 4)

    // My bar
    const myW = (dim.myScore / 100) * chartW
    ctx.fillStyle = 'rgba(75, 143, 226, 0.8)'
    ctx.fillRect(padding.left, y + (barGroupH / 2 - barH - 2), myW, barH)
    ctx.fillStyle = '#B8823A'
    ctx.textAlign = 'left'
    ctx.font = '11px sans-serif'
    ctx.fillText(String(dim.myScore), padding.left + myW + 4, y + (barGroupH / 2 - barH / 2 + 2))

    // Competitor bar
    const compW = (dim.competitorScore / 100) * chartW
    ctx.fillStyle = 'rgba(255, 125, 0, 0.8)'
    ctx.fillRect(padding.left, y + barGroupH / 2 + 2, compW, barH)
    ctx.fillStyle = '#ff7d00'
    ctx.fillText(String(dim.competitorScore), padding.left + compW + 4, y + barGroupH / 2 + barH / 2 + 6)
  }

  // Legend
  ctx.fillStyle = 'rgba(75, 143, 226, 0.8)'
  ctx.fillRect(padding.left, 8, 14, 14)
  ctx.fillStyle = '#0F1726'
  ctx.font = '12px sans-serif'
  ctx.textAlign = 'left'
  ctx.fillText(props.myName, padding.left + 20, 20)

  ctx.fillStyle = 'rgba(255, 125, 0, 0.8)'
  ctx.fillRect(padding.left + 120, 8, 14, 14)
  ctx.fillStyle = '#0F1726'
  ctx.fillText(props.competitorName, padding.left + 140, 20)
}

onMounted(drawChart)
watch(() => props.dimensions, drawChart, { deep: true })
</script>

<template>
  <div class="compare-chart">
    <canvas ref="canvasRef" :width="560" :height="Math.max(300, dimensions.length * 60 + 80)" />
  </div>
</template>

<style scoped lang="scss">
.compare-chart {
  display: flex;
  justify-content: center;
  canvas { max-width: 100%; }
}
</style>
