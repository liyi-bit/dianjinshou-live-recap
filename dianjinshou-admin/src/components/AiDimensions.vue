<script setup lang="ts">
import { computed } from 'vue';

interface DimItem { key: string; label: string; score: number; comment?: string }
interface Props { json: string | null | undefined }
const props = defineProps<Props>();

// 桌面端维度中文映射
const DIM_LABEL: Record<string, string> = {
  opening: '开场话术',
  retention: '留人话术',
  product: '产品塑造',
  promotion: '促单节奏',
  interaction: '互动话术',
  expression: '表达力',
  compliance: '合规意识',
  fanClub: '粉丝团引导',
  privateDomain: '私域引导',
  persona: '人设建立',
  closing: '收尾话术',
  overall: '整体评分'
};

const parsed = computed<DimItem[]>(() => {
  if (!props.json) return [];
  try {
    const obj = JSON.parse(props.json);
    const dim = obj?.dimensions;
    if (!dim || typeof dim !== 'object') return [];
    return Object.entries(dim).map(([k, v]: any) => ({
      key: k,
      label: DIM_LABEL[k] || k,
      score: typeof v?.score === 'number' ? v.score : 0,
      comment: v?.comment || ''
    }));
  } catch (_) {
    return [];
  }
});

const overall = computed(() => parsed.value.find(d => d.key === 'overall'));
const otherDims = computed(() => parsed.value.filter(d => d.key !== 'overall'));

function scoreColor(n: number) {
  if (n >= 85) return 'var(--green)';
  if (n >= 70) return 'var(--brand)';
  if (n >= 60) return 'var(--gold)';
  return 'var(--red)';
}
</script>

<template>
  <div v-if="parsed.length === 0" class="empty-json">
    <span>无维度数据</span>
  </div>
  <div v-else class="dim-wrap">
    <!-- 整体评分大卡 -->
    <div v-if="overall" class="overall-card">
      <div class="overall-label">整体评分</div>
      <div class="overall-score mono" :style="{ color: scoreColor(overall.score) }">
        {{ overall.score }}<span class="unit">/100</span>
      </div>
      <div class="overall-comment">{{ overall.comment }}</div>
    </div>

    <!-- 各维度格子 -->
    <div class="dim-grid">
      <div v-for="d in otherDims" :key="d.key" class="dim-item">
        <div class="dim-head">
          <span class="dim-name">{{ d.label }}</span>
          <span class="dim-score mono" :style="{ color: scoreColor(d.score) }">{{ d.score }}</span>
        </div>
        <div class="dim-bar">
          <div class="dim-fill" :style="{ width: d.score + '%', background: scoreColor(d.score) }"></div>
        </div>
        <div class="dim-comment" v-if="d.comment">{{ d.comment }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.empty-json {
  padding: 24px; text-align: center;
  color: var(--text-3); font-size: 13px;
}

.dim-wrap { padding: 0 }

.overall-card {
  margin: 0 0 16px;
  padding: 18px 20px;
  background: linear-gradient(135deg, rgba(184,130,58,.08), rgba(184,130,58,.02));
  border: 1px solid rgba(184,130,58,.2);
  border-radius: var(--radius-md);
  display: grid;
  grid-template-columns: 1fr auto;
  grid-template-areas: 'label score' 'comment comment';
  gap: 8px 16px;
  align-items: center;
}
.overall-label {
  grid-area: label;
  font-size: 11px; font-weight: 700;
  letter-spacing: .15em; text-transform: uppercase;
  color: var(--text-3);
}
.overall-score {
  grid-area: score;
  font-size: 42px; font-weight: 700;
  line-height: 1; font-family: var(--fm);
  font-variant-numeric: tabular-nums;
}
.overall-score .unit {
  font-size: 14px; color: var(--text-3); font-weight: 500;
  margin-left: 4px;
}
.overall-comment {
  grid-area: comment;
  font-size: 13px; color: var(--text-1);
  line-height: 1.5;
}

.dim-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.dim-item {
  padding: 12px 14px;
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
}
.dim-head {
  display: flex; justify-content: space-between; align-items: baseline;
  margin-bottom: 8px;
}
.dim-name {
  font-size: 13px; font-weight: 600; color: var(--text-1);
}
.dim-score {
  font-size: 18px; font-weight: 700;
  font-family: var(--fm);
}
.dim-bar {
  height: 4px;
  background: var(--bg-2);
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 8px;
}
.dim-fill {
  height: 100%;
  border-radius: 2px;
  transition: width .4s var(--ease);
}
.dim-comment {
  font-size: 12px; color: var(--text-2b);
  line-height: 1.5;
}
</style>
