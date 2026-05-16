<script setup lang="ts">
import type { DimensionScore } from '@/api/diagnosis'

defineProps<{
  dimension: DimensionScore
}>()

function getScoreColor(score: number): string {
  if (score >= 80) return 'var(--green-6, #00b42a)'
  if (score >= 60) return 'var(--orange-6, #ff7d00)'
  return 'var(--red-6, #f53f3f)'
}
</script>

<template>
  <div class="dimension-card">
    <div class="dimension-card__header">
      <span class="dimension-card__name">{{ dimension.name }}</span>
      <span class="dimension-card__score" :style="{ color: getScoreColor(dimension.score) }">
        {{ dimension.score }}
      </span>
    </div>
    <a-progress
      :percent="dimension.score / 100"
      :show-text="false"
      :stroke-width="6"
      :color="getScoreColor(dimension.score)"
      style="margin: 8px 0"
    />
    <div class="dimension-card__suggestion">{{ dimension.suggestion }}</div>
    <div v-if="dimension.historicalAvg != null" class="dimension-card__compare">
      <span>历史平均: {{ dimension.historicalAvg }}</span>
      <span v-if="dimension.score > dimension.historicalAvg" style="color: var(--green-6, #00b42a)">
        (+{{ dimension.score - dimension.historicalAvg }})
      </span>
      <span v-else-if="dimension.score < dimension.historicalAvg" style="color: var(--red-6, #f53f3f)">
        ({{ dimension.score - dimension.historicalAvg }})
      </span>
    </div>
  </div>
</template>

<style scoped lang="scss">
.dimension-card {
  padding: 12px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  margin-bottom: 12px;

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  &__name {
    font-weight: 500;
  }

  &__score {
    font-size: 20px;
    font-weight: 700;
  }

  &__suggestion {
    font-size: 13px;
    color: var(--color-text-2);
    line-height: 1.5;
  }

  &__compare {
    margin-top: 6px;
    font-size: 12px;
    color: var(--color-text-3);
    display: flex;
    gap: 8px;
  }
}
</style>
