<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import * as analysisApi from '@/api/analysis'
import NotesEditor from '@/components/notes/NotesEditor.vue'

const route = useRoute()
const router = useRouter()

const taskId = computed(() => Number(route.params.id))

const TAB_TYPES = [
  { key: 'MINUTE_SEGMENTS', label: '分钟段落笔记' },
  { key: 'AI_SCRIPT', label: 'AI脚本拆解笔记' },
  { key: 'RECAP_SUMMARY', label: '复盘小结' },
  { key: 'CORRECTIONS', label: '纠正和批注汇总' }
]

const activeTab = ref('MINUTE_SEGMENTS')
const contents = ref<Record<string, string>>({
  MINUTE_SEGMENTS: '',
  AI_SCRIPT: '',
  RECAP_SUMMARY: '',
  CORRECTIONS: ''
})
const saving = ref(false)
const loading = ref(false)

async function fetchNotes() {
  loading.value = true
  try {
    for (const tab of TAB_TYPES) {
      try {
        const res = await analysisApi.getNotes(taskId.value, tab.key)
        const data = (res as any).data ?? res
        if (data && data.contentHtml) {
          contents.value[tab.key] = data.contentHtml
        }
      } catch {
        // tab may not have saved notes yet
      }
    }
  } finally {
    loading.value = false
  }
}

async function saveNote() {
  saving.value = true
  try {
    await analysisApi.saveNotes(taskId.value, {
      tabType: activeTab.value,
      contentHtml: contents.value[activeTab.value]
    })
    Message.success('笔记已保存')
  } catch {
    Message.error('保存失败')
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.push({ name: 'RecapDetail', params: { id: taskId.value } })
}

onMounted(() => {
  fetchNotes()
})
</script>

<template>
  <div class="notes-panel">
    <!-- Top bar -->
    <div class="notes-top-bar">
      <div class="top-left">
        <a-button type="text" @click="goBack">
          <icon-left /> 返回复盘
        </a-button>
        <span class="title">笔记 + 批注</span>
      </div>
      <div class="top-right">
        <a-button type="primary" :loading="saving" @click="saveNote">保存笔记</a-button>
        <a-button>导出</a-button>
      </div>
    </div>

    <!-- Three-column layout -->
    <div class="notes-layout">
      <!-- Left placeholder: video -->
      <div class="notes-left">
        <div class="video-placeholder">
          <icon-play-circle :size="36" />
          <p>视频播放器</p>
        </div>
      </div>

      <!-- Middle: transcript reference -->
      <div class="notes-middle">
        <div class="reference-header">原文参考</div>
        <div class="reference-content">
          <a-empty description="选中段落后在此显示参考原文" />
        </div>
      </div>

      <!-- Right: notes tabs + editor -->
      <div class="notes-right">
        <a-tabs v-model:active-key="activeTab" type="rounded" size="small">
          <a-tab-pane v-for="tab in TAB_TYPES" :key="tab.key" :title="tab.label">
            <NotesEditor v-model="contents[tab.key]" />
          </a-tab-pane>
        </a-tabs>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.notes-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.notes-top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border);
  margin-bottom: 12px;

  .top-left {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .title {
    font-size: 16px;
    font-weight: 600;
  }

  .top-right {
    display: flex;
    gap: 8px;
  }
}

.notes-layout {
  flex: 1;
  display: flex;
  gap: 12px;
  min-height: 0;
}

.notes-left {
  width: 200px;
  min-width: 200px;

  .video-placeholder {
    background: #000;
    border-radius: 8px;
    aspect-ratio: 9/16;
    max-height: 360px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #666;
    p { margin-top: 6px; font-size: 12px; }
  }
}

.notes-middle {
  width: 300px;
  min-width: 300px;
  background: var(--color-bg-2);
  border-radius: 8px;
  padding: 12px;
  display: flex;
  flex-direction: column;

  .reference-header {
    font-weight: 600;
    margin-bottom: 12px;
  }

  .reference-content {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.notes-right {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-2);
  border-radius: 8px;
  padding: 12px;

  :deep(.arco-tabs-content) {
    flex: 1;
    display: flex;
    flex-direction: column;
  }

  :deep(.arco-tabs-pane) {
    flex: 1;
    display: flex;
    flex-direction: column;
  }
}
</style>
