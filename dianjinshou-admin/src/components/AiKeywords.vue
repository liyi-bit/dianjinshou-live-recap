<script setup lang="ts">
import { computed } from 'vue';

interface Props { json: string | null | undefined }
const props = defineProps<Props>();

/** 结构：{ operational: { 类别: [词...] }, sensitive: { 类别: [词...] } } */
const parsed = computed(() => {
  if (!props.json) return null;
  try {
    const obj = JSON.parse(props.json);
    const flatten = (src: any) => {
      if (!src || typeof src !== 'object') return [] as Array<{ category: string; words: string[] }>;
      return Object.entries(src).map(([cat, words]: any) => ({
        category: cat,
        words: Array.isArray(words) ? words : []
      }));
    };
    return {
      operational: flatten(obj.operational),
      sensitive: flatten(obj.sensitive)
    };
  } catch (_) { return null; }
});

const hasAny = computed(() => parsed.value && (parsed.value.operational.length > 0 || parsed.value.sensitive.length > 0));
</script>

<template>
  <div v-if="!hasAny" class="empty-json">无关键词数据</div>
  <div v-else class="kw-wrap">
    <div v-if="parsed!.operational.length" class="kw-section">
      <div class="kw-section-title" style="color: var(--brand-dark)">运营关键词</div>
      <div v-for="g in parsed!.operational" :key="'op-'+g.category" class="kw-group">
        <div class="kw-cat">{{ g.category }}</div>
        <div class="kw-tags">
          <span v-for="w in g.words" :key="'op-'+g.category+'-'+w" class="kw-tag op">{{ w }}</span>
        </div>
      </div>
    </div>

    <div v-if="parsed!.sensitive.length" class="kw-section">
      <div class="kw-section-title" style="color: var(--red-dark)">敏感词</div>
      <div v-for="g in parsed!.sensitive" :key="'se-'+g.category" class="kw-group">
        <div class="kw-cat">{{ g.category }}</div>
        <div class="kw-tags">
          <span v-for="w in g.words" :key="'se-'+g.category+'-'+w" class="kw-tag se">{{ w }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.empty-json { padding:24px; text-align:center; color:var(--text-3); font-size:13px }

.kw-wrap { display:flex; flex-direction:column; gap: 18px }
.kw-section { }
.kw-section-title {
  font-size: 11px; font-weight: 700;
  letter-spacing: .15em; text-transform: uppercase;
  margin-bottom: 10px;
  display: flex; align-items: center; gap: 6px;
}
.kw-section-title::after {
  content: ''; flex: 1; height: 1px;
  background: linear-gradient(90deg, var(--line), transparent);
}

.kw-group {
  display: grid;
  grid-template-columns: 110px 1fr;
  gap: 12px;
  margin-bottom: 10px;
  align-items: start;
}
.kw-cat {
  font-size: 12px; color: var(--text-2b);
  font-weight: 600; padding-top: 4px;
}
.kw-tags {
  display: flex; flex-wrap: wrap; gap: 6px;
}
.kw-tag {
  display: inline-flex;
  padding: 3px 10px; border-radius: var(--radius-sm);
  font-size: 12px; font-weight: 500;
  line-height: 1.4;
}
.kw-tag.op {
  background: var(--brand-soft);
  color: var(--brand-dark);
  border: 1px solid rgba(184,130,58,.2);
}
.kw-tag.se {
  background: var(--red-soft);
  color: var(--red-dark);
  border: 1px solid rgba(184,68,60,.2);
}
</style>
