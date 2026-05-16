<script setup lang="ts">
import { computed } from 'vue';

interface Props { json: string | null | undefined }
const props = defineProps<Props>();

const parsed = computed(() => {
  if (!props.json) return null;
  try {
    const obj = JSON.parse(props.json);
    return {
      strengths: Array.isArray(obj.strengths) ? obj.strengths : [],
      weaknesses: Array.isArray(obj.weaknesses) ? obj.weaknesses : [],
      suggestions: Array.isArray(obj.suggestions) ? obj.suggestions : []
    };
  } catch (_) {
    return null;
  }
});
</script>

<template>
  <div v-if="!parsed" class="empty-json">无诊断数据</div>
  <div v-else class="diag-grid">
    <div class="diag-col diag-col--strength">
      <div class="diag-head">
        <span class="diag-icon">✓</span>
        <span>优势</span>
        <span class="diag-count mono">{{ parsed.strengths.length }}</span>
      </div>
      <ul class="diag-list">
        <li v-for="(s, i) in parsed.strengths" :key="'s'+i">{{ s }}</li>
      </ul>
    </div>

    <div class="diag-col diag-col--weakness">
      <div class="diag-head">
        <span class="diag-icon">!</span>
        <span>劣势</span>
        <span class="diag-count mono">{{ parsed.weaknesses.length }}</span>
      </div>
      <ul class="diag-list">
        <li v-for="(s, i) in parsed.weaknesses" :key="'w'+i">{{ s }}</li>
      </ul>
    </div>

    <div class="diag-col diag-col--suggest">
      <div class="diag-head">
        <span class="diag-icon">★</span>
        <span>建议</span>
        <span class="diag-count mono">{{ parsed.suggestions.length }}</span>
      </div>
      <ul class="diag-list">
        <li v-for="(s, i) in parsed.suggestions" :key="'g'+i">{{ s }}</li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.empty-json { padding:24px; text-align:center; color:var(--text-3); font-size:13px }

.diag-grid {
  display:grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.diag-col {
  border:1px solid var(--line);
  border-radius: var(--radius-md);
  padding: 14px 16px;
  min-height: 120px;
}
.diag-col--strength {
  background: linear-gradient(135deg, rgba(62,122,92,.06), rgba(62,122,92,.02));
  border-color: rgba(62,122,92,.22);
}
.diag-col--weakness {
  background: linear-gradient(135deg, rgba(184,68,60,.06), rgba(184,68,60,.02));
  border-color: rgba(184,68,60,.22);
}
.diag-col--suggest {
  background: linear-gradient(135deg, rgba(184,130,58,.06), rgba(184,130,58,.02));
  border-color: rgba(184,130,58,.22);
}
.diag-head {
  display:flex; align-items:center; gap:6px;
  font-size: 12px; font-weight: 700;
  letter-spacing: .1em;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px dashed var(--line);
}
.diag-col--strength .diag-head { color: var(--green-dark) }
.diag-col--weakness .diag-head { color: var(--red-dark) }
.diag-col--suggest .diag-head { color: var(--brand-dark) }
.diag-icon {
  width: 18px; height: 18px; border-radius: 50%;
  display:flex; align-items:center; justify-content:center;
  font-size: 10px; color: #fff; font-weight: 700;
  flex-shrink: 0;
}
.diag-col--strength .diag-icon { background: var(--green) }
.diag-col--weakness .diag-icon { background: var(--red) }
.diag-col--suggest .diag-icon { background: var(--brand) }
.diag-count {
  margin-left: auto;
  font-size: 11px;
  color: var(--text-3);
  font-weight: 600;
}
.diag-list {
  list-style: none; padding: 0; margin: 0;
  display: flex; flex-direction: column; gap: 6px;
}
.diag-list li {
  font-size: 13px;
  color: var(--text-1);
  line-height: 1.5;
  padding-left: 16px;
  position: relative;
}
.diag-list li::before {
  content: ''; position: absolute;
  left: 4px; top: 8px;
  width: 4px; height: 4px;
  border-radius: 50%;
  background: var(--text-3);
}
.mono { font-family: var(--fm) }
</style>
