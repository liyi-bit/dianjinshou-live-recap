<template>
  <div class="detail-layout">
    <!-- Header -->
    <div class="detail-header">
      <span class="back-btn" @click="router.back()">&#8592;</span>
      <span style="font-size:15px;font-weight:600;color:var(--text-1);flex:1">
        {{ task?.fileName || '分析详情' }}
      </span>
      <span v-if="task" style="font-size:12px;color:var(--text-3);margin-right:8px">
        {{ formatSize(task.fileSize) }} &nbsp;·&nbsp; {{ task.duration ? formatDuration(task.duration) : '-' }}
      </span>
      <button class="djsbtn sm ghost">导出报告</button>
      <button class="djsbtn sm">切片管理</button>
    </div>

    <!-- Loading state -->
    <div v-if="loading" style="flex:1;display:flex;align-items:center;justify-content:center;color:var(--text-3)">
      加载中...
    </div>

    <!-- Main content -->
    <template v-else-if="task">
      <div class="two-col">
        <!-- Left: video player -->
        <div class="video-col">
          <div class="djsplayer" style="aspect-ratio:16/9;margin-bottom:14px">
            <div class="play-btn"></div>
          </div>
          <!-- File meta -->
          <div style="display:flex;flex-direction:column;gap:10px">
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span style="font-size:12px;color:var(--text-3)">文件大小</span>
              <span style="font-size:13px;color:var(--text-1);font-weight:500">{{ formatSize(task.fileSize) }}</span>
            </div>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span style="font-size:12px;color:var(--text-3)">视频时长</span>
              <span style="font-size:13px;color:var(--text-1);font-weight:500">{{ task.duration ? formatDuration(task.duration) : '-' }}</span>
            </div>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span style="font-size:12px;color:var(--text-3)">AI模型</span>
              <span style="font-size:13px;color:var(--text-1);font-weight:500">{{ task.aiModel || '-' }}</span>
            </div>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span style="font-size:12px;color:var(--text-3)">分析状态</span>
              <span v-if="task.status === 'completed'" class="djsbadge live">已完成</span>
              <span v-else-if="task.status === 'failed'" class="djsbadge own">失败</span>
              <span v-else class="djsbadge peer">{{ task.status }}</span>
            </div>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span style="font-size:12px;color:var(--text-3)">上传时间</span>
              <span style="font-size:12px;color:var(--text-3)">{{ formatDateTime(task.createdAt) }}</span>
            </div>
          </div>
          <!-- Error -->
          <div v-if="task.errorMsg" style="margin-top:14px;padding:12px;background:#FFF1F0;border-radius:8px;border:1px solid #FFCCC7;font-size:12px;color:var(--red)">
            {{ task.errorMsg }}
          </div>
        </div>

        <!-- Right: transcript panel -->
        <div class="transcript-col">
          <!-- Tabs -->
          <div class="djstabs" style="padding:0 16px">
            <div class="djstab on">话术转写</div>
            <div class="djstab">文案预审</div>
            <div class="djstab">AI 分析</div>
          </div>

          <!-- Transcript body -->
          <div class="transcript-body">
            <div v-if="task.status !== 'completed'" class="djsempty">
              <div class="ic">⏳</div>
              <div class="ti">分析进行中</div>
              <div>文件正在处理，请稍候</div>
            </div>
            <template v-else>
              <!-- Copywriting review section embedded -->
              <div style="margin-bottom:18px">
                <div style="font-size:13px;font-weight:600;color:var(--text-1);margin-bottom:10px">文案预审</div>
                <textarea
                  v-model="copywritingText"
                  class="djsinput sq"
                  placeholder="粘贴直播话术文案，AI 检测敏感词和合规风险（最多10000字）"
                  maxlength="10000"
                  rows="5"
                  style="width:100%;height:120px;resize:vertical;padding:10px 14px;font-size:13px;line-height:1.6"
                ></textarea>
                <div style="display:flex;justify-content:flex-end;margin-top:8px">
                  <button class="djsbtn primary sm" :disabled="reviewLoading" @click="submitReview">
                    {{ reviewLoading ? '检测中...' : '开始检测' }}
                  </button>
                </div>
                <div v-if="reviewResult" style="margin-top:12px;padding:14px;background:var(--bg);border-radius:8px">
                  <div style="display:flex;gap:20px;align-items:center">
                    <div style="text-align:center">
                      <div style="font-family:var(--fm);font-size:28px;font-weight:700"
                           :style="{ color: (reviewResult.riskScore || 0) > 50 ? 'var(--red)' : 'var(--green)' }">
                        {{ reviewResult.riskScore ?? '-' }}
                      </div>
                      <div style="font-size:11px;color:var(--text-3)">风险评分</div>
                    </div>
                    <div>
                      <div style="font-size:13px;color:var(--text-1);font-weight:500">{{ reviewResult.status }}</div>
                      <div style="font-size:12px;color:var(--text-3);margin-top:4px">{{ formatDateTime(reviewResult.createdAt) }}</div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Transcript placeholder lines -->
              <div class="djsempty" v-if="!copywritingText">
                <div class="ic">📝</div>
                <div class="ti">暂无转写内容</div>
                <div>在上方输入文案进行预审，或等待 ASR 转写完成</div>
              </div>
            </template>
          </div>
        </div>
      </div>
    </template>

    <!-- Empty state -->
    <div v-else class="djsempty" style="flex:1">
      <div class="ic">🎬</div>
      <div class="ti">未找到分析任务</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useFileAnalysisStore } from '@/stores/fileAnalysis'
import { submitCopywritingReview, type CopywritingReviewResult } from '@/api/fileAnalysis'
import { IconLeft } from '@arco-design/web-vue/es/icon'
import { formatDateTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const store = useFileAnalysisStore()
const loading = ref(false)
const task = ref(store.currentTask)
const copywritingText = ref('')
const reviewLoading = ref(false)
const reviewResult = ref<CopywritingReviewResult | null>(null)

onMounted(async () => {
  loading.value = true
  const id = Number(route.params.id)
  task.value = await store.fetchDetail(id)
  loading.value = false
})

async function submitReview() {
  if (!copywritingText.value.trim()) return
  reviewLoading.value = true
  try {
    const res = await submitCopywritingReview({ textContent: copywritingText.value })
    reviewResult.value = (res as any).data ?? res
  } catch {
    // handled
  } finally {
    reviewLoading.value = false
  }
}

function formatSize(bytes: number): string {
  if (!bytes) return '-'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function formatDuration(seconds: number): string {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${s.toString().padStart(2, '0')}`
}
</script>

<style scoped>
.detail-layout {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.detail-header {
  padding: 16px 20px;
  background: var(--card);
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}

.back-btn {
  font-size: 20px;
  color: var(--text-3);
  cursor: pointer;
}

.two-col {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.video-col {
  width: 340px;
  flex-shrink: 0;
  padding: 16px;
  border-right: 1px solid var(--line);
  overflow-y: auto;
}

.transcript-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.transcript-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
}
</style>
