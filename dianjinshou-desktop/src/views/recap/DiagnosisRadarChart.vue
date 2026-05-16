<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'

const props = defineProps<{
  labels: string[]
  data: number[]
}>()

const canvasRef = ref<HTMLCanvasElement | null>(null)

function drawRadar() {
  const canvas = canvasRef.value
  if (!canvas || !props.labels.length) return

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const w = canvas.width
  const h = canvas.height
  const cx = w / 2
  const cy = h / 2
  const r = Math.min(cx, cy) - 40
  const n = props.labels.length
  const angleStep = (2 * Math.PI) / n

  ctx.clearRect(0, 0, w, h)

  // Draw background circles
  ctx.strokeStyle = 'var(--color-border, #E2E7EF)'
  for (let ring = 1; ring <= 5; ring++) {
    const rr = (r * ring) / 5
    ctx.beginPath()
    for (let i = 0; i <= n; i++) {
      const angle = i * angleStep - Math.PI / 2
      const x = cx + rr * Math.cos(angle)
      const y = cy + rr * Math.sin(angle)
      if (i === 0) ctx.moveTo(x, y)
      else ctx.lineTo(x, y)
    }
    ctx.stroke()
  }

  // Draw axis lines
  ctx.strokeStyle = '#ddd'
  for (let i = 0; i < n; i++) {
    const angle = i * angleStep - Math.PI / 2
    ctx.beginPath()
    ctx.moveTo(cx, cy)
    ctx.lineTo(cx + r * Math.cos(angle), cy + r * Math.sin(angle))
    ctx.stroke()
  }

  // Draw data polygon
  ctx.fillStyle = 'rgba(75, 143, 226, 0.15)'
  ctx.strokeStyle = 'rgb(75, 143, 226)'
  ctx.lineWidth = 2
  ctx.beginPath()
  for (let i = 0; i < n; i++) {
    const angle = i * angleStep - Math.PI / 2
    const val = (props.data[i] || 0) / 100
    const x = cx + r * val * Math.cos(angle)
    const y = cy + r * val * Math.sin(angle)
    if (i === 0) ctx.moveTo(x, y)
    else ctx.lineTo(x, y)
  }
  ctx.closePath()
  ctx.fill()
  ctx.stroke()

  // Draw data points
  ctx.fillStyle = 'rgb(75, 143, 226)'
  for (let i = 0; i < n; i++) {
    const angle = i * angleStep - Math.PI / 2
    const val = (props.data[i] || 0) / 100
    const x = cx + r * val * Math.cos(angle)
    const y = cy + r * val * Math.sin(angle)
    ctx.beginPath()
    ctx.arc(x, y, 3, 0, 2 * Math.PI)
    ctx.fill()
  }

  // Draw labels
  ctx.fillStyle = 'var(--color-text-1, #0F1726)'
  ctx.font = '12px sans-serif'
  ctx.textAlign = 'center'
  for (let i = 0; i < n; i++) {
    const angle = i * angleStep - Math.PI / 2
    const x = cx + (r + 24) * Math.cos(angle)
    const y = cy + (r + 24) * Math.sin(angle)
    ctx.fillText(props.labels[i], x, y + 4)
  }
}

onMounted(drawRadar)
watch(() => [props.data, props.labels], drawRadar, { deep: true })
</script>

<template>
  <div class="radar-chart">
    <canvas ref="canvasRef" width="400" height="400" />
  </div>
</template>

<style scoped lang="scss">
.radar-chart {
  display: flex;
  justify-content: center;
  canvas {
    max-width: 100%;
  }
}
</style>
