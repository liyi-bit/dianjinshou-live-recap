<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as analysisApi from '@/api/analysis'
import type { KeywordItem, AnalysisTask, AsrParagraph } from '@/api/analysis'
import ContentCompass from './ContentCompass.vue'

const props = defineProps<{
  taskId: number
  task?: AnalysisTask | null
  paragraphs?: AsrParagraph[]
}>()

const activeTab = ref('keywords')
const keywords = ref<KeywordItem[]>([])
const keywordStats = ref({ totalOperational: 0, totalSensitive: 0 })
const keywordType = ref('')

const tabs = [
  { key: 'keywords', label: '运营关键词汇总' },
  { key: 'compass', label: '内容罗盘' },
]

async function fetchKeywords() {
  try {
    const res = await analysisApi.getKeywords(props.taskId, {
      type: keywordType.value || undefined,
      page: 1,
      size: 50
    })
    const data = (res as any).data ?? res
    keywords.value = data.items || []
    keywordStats.value = data.stats || { totalOperational: 0, totalSensitive: 0 }
  } catch {
    keywords.value = []
  }
}

function onTypeChange(type: string) {
  keywordType.value = type
  fetchKeywords()
}

onMounted(() => {
  fetchKeywords()
})
</script>

<template>
  <div class="analysis-panel">
    <!-- Tabs -->
    <div class="djstabs">
      <div
        v-for="tab in tabs"
        :key="tab.key"
        class="djstab"
        :class="{ on: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
      </div>
    </div>

    <!-- Keywords tab -->
    <div v-if="activeTab === 'keywords'" class="tab-body">
      <div class="keyword-header">
        <div class="djsradio-group">
          <span
            class="djsradio"
            :class="{ on: keywordType === '' }"
            @click="onTypeChange('')"
          >全部</span>
          <span
            class="djsradio"
            :class="{ on: keywordType === 'operational' }"
            @click="onTypeChange('operational')"
          >运营词 ({{ keywordStats.totalOperational }})</span>
          <span
            class="djsradio"
            :class="{ on: keywordType === 'sensitive' }"
            @click="onTypeChange('sensitive')"
          >敏感词 ({{ keywordStats.totalSensitive }})</span>
        </div>
      </div>
      <table class="djstbl">
        <thead>
          <tr>
            <th>关键词</th>
            <th>类型</th>
            <th>分类</th>
            <th>出现次数</th>
            <th>来源</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="keywords.length === 0">
            <td colspan="5">
              <div class="djsempty">
                <div class="ti">暂无数据</div>
              </div>
            </td>
          </tr>
          <tr v-for="kw in keywords" :key="kw.word">
            <td>{{ kw.word }}</td>
            <td>
              <span class="djskw" :class="kw.type === 'sensitive' ? 'hot' : ''">
                {{ kw.type === 'sensitive' ? '敏感词' : '运营词' }}
              </span>
            </td>
            <td>{{ kw.category }}</td>
            <td>{{ kw.totalCount }}</td>
            <td>{{ kw.source }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 内容罗盘 -->
    <div v-if="activeTab === 'compass'" class="tab-body">
      <ContentCompass
        :task-id="props.taskId"
        :task="props.task ?? null"
        :paragraphs="props.paragraphs ?? []"
      />
    </div>

  </div>
</template>

<style scoped>
.analysis-panel {
  border-top: 1px solid var(--line);
}

.tab-body {
  padding: 16px;
}

.keyword-header {
  margin-bottom: 14px;
}

</style>
