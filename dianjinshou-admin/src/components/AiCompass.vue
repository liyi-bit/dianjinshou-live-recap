<script setup lang="ts">
import { computed } from 'vue';

interface CompassItem { name: string; percent: number }
interface Props { json: string | null | undefined }
const props = defineProps<Props>();

const parsed = computed(() => {
  if (!props.json) return null;
  try {
    const obj = JSON.parse(props.json);
    return {
      contentRatio: {
        score: typeof obj?.contentRatio?.score === 'number' ? obj.contentRatio.score : null,
        items: Array.isArray(obj?.contentRatio?.items) ? obj.contentRatio.items as CompassItem[] : []
      },
      liveRhythm: {
        score: typeof obj?.liveRhythm?.score === 'number' ? obj.liveRhythm.score : null,
        items: Array.isArray(obj?.liveRhythm?.items) ? obj.liveRhythm.items as CompassItem[] : []
      }
    };
  } catch (_) { return null; }
});

function scoreColor(n: number) {
  if (n >= 85) return 'var(--green)';
  if (n >= 70) return 'var(--brand)';
  if (n >= 60) return 'var(--gold)';
  return 'var(--red)';
}
</script>

<template>
  <div v-if="!parsed" class="empty-json">无内容罗盘数据</div>
  <div v-else class="compass-wrap">
    <!-- 内容占比 -->
    <div v-if="parsed.contentRatio.items.length" class="compass-block">
      <div class="compass-head">
        <span class="compass-title">内容占比分布</span>
        <span v-if="parsed.contentRatio.score != null" class="compass-score mono" :style="{ color: scoreColor(parsed.contentRatio.score) }">
          {{ parsed.contentRatio.score }}<span class="unit">分</span>
        </span>
      </div>
      <div class="bars">
        <div v-for="item in parsed.contentRatio.items" :key="item.name" class="bar-item">
          <div class="bar-row">
            <span class="bar-name">{{ item.name }}</span>
            <span class="bar-percent mono">{{ item.percent.toFixed(1) }}%</span>
          </div>
          <div class="bar-track">
            <div class="bar-fill content" :style="{ width: Math.min(100, item.percent) + '%' }"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 直播节奏 -->
    <div v-if="parsed.liveRhythm.items.length" class="compass-block">
      <div class="compass-head">
        <span class="compass-title">直播节奏</span>
        <span v-if="parsed.liveRhythm.score != null" class="compass-score mono" :style="{ color: scoreColor(parsed.liveRhythm.score) }">
          {{ parsed.liveRhythm.score }}<span class="unit">分</span>
        </span>
      </div>
      <div class="bars">
        <div v-for="item in parsed.liveRhythm.items" :key="item.name" class="bar-item">
          <div class="bar-row">
            <span class="bar-name">{{ item.name }}</span>
            <span class="bar-percent mono">{{ item.percent.toFixed(1) }}%</span>
          </div>
          <div class="bar-track">
            <div class="bar-fill rhythm" :style="{ width: Math.min(100, item.percent) + '%' }"></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.empty-json { padding:24px; text-align:center; color:var(--text-3); font-size:13px }

.compass-wrap { display:flex; flex-direction:column; gap: 16px }
.compass-block {
  padding: 14px 16px;
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
}
.compass-head {
  display:flex; align-items:baseline; justify-content:space-between;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px dashed var(--line);
}
.compass-title {
  font-size: 12px; font-weight: 700;
  letter-spacing: .12em; text-transform: uppercase;
  color: var(--text-2);
}
.compass-score {
  font-size: 24px; font-weight: 700;
  font-family: var(--fm);
}
.compass-score .unit {
  font-size: 11px; color: var(--text-3);
  margin-left: 2px; font-weight: 500;
}

.bars { display: flex; flex-direction: column; gap: 8px }
.bar-item { }
.bar-row {
  display: flex; justify-content: space-between; align-items: baseline;
  margin-bottom: 4px;
}
.bar-name {
  font-size: 12.5px; color: var(--text-1);
  font-weight: 500;
}
.bar-percent {
  font-size: 12px; color: var(--text-2);
  font-weight: 600;
}
.bar-track {
  height: 5px;
  background: var(--bg-2);
  border-radius: 3px;
  overflow: hidden;
}
.bar-fill {
  height: 100%; border-radius: 3px;
  transition: width .5s var(--ease);
}
.bar-fill.content { background: linear-gradient(90deg, var(--brand-lighter), var(--brand)) }
.bar-fill.rhythm { background: linear-gradient(90deg, var(--blue2-light), var(--blue2)) }

.mono { font-family: var(--fm) }
</style>
