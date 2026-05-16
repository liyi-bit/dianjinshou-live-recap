<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as recordingApi from '@/api/recording'
import type { Recording } from '@/api/recording'
import { formatDateTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const streamerId = computed(() => Number(route.params.id))
const recordings = ref<Recording[]>([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)

async function fetchData() {
  loading.value = true
  try {
    const res = await recordingApi.getRecordings({
      streamerId: streamerId.value,
      page: page.value,
      size: 20
    })
    const data = (res as any).data ?? res
    recordings.value = data.items || []
    total.value = data.total || 0
  } catch {
    recordings.value = []
  } finally {
    loading.value = false
  }
}

function onPageChange(p: number) {
  page.value = p
  fetchData()
}

function viewRecap(record: Recording) {
  if (!record.latestTaskId) {
    return
  }
  router.push({ name: 'RecapDetail', params: { id: record.latestTaskId } })
}

function goBack() {
  router.push({ name: 'StreamerList' })
}

function statusClass(status: string): string {
  const map: Record<string, string> = { completed: 'green', recording: 'blue' }
  return map[status] || 'gold'
}

onMounted(fetchData)
</script>

<template>
  <div class="recap-table-page">
    <div class="page-header">
      <div class="header-left">
        <button class="djsbtn ghost sm" @click="goBack">‹ 返回</button>
        <h2 class="page-title">复盘表</h2>
      </div>
    </div>

    <div class="djscard">
      <table class="djstbl">
        <thead>
          <tr>
            <th style="width: 60px">序号</th>
            <th>文件名</th>
            <th style="width: 160px">录制时间</th>
            <th style="width: 100px">时长(分钟)</th>
            <th style="width: 100px">录制状态</th>
            <th style="width: 100px">分析状态</th>
            <th style="width: 80px">敏感词</th>
            <th style="width: 100px">运营关键词</th>
            <th style="width: 140px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="9">
              <div class="djsempty"><div class="ti">加载中...</div></div>
            </td>
          </tr>
          <tr v-else-if="recordings.length === 0">
            <td colspan="9">
              <div class="djsempty">
                <div class="ic">📋</div>
                <div class="ti">暂无录制记录</div>
              </div>
            </td>
          </tr>
          <tr v-for="(record, index) in recordings" :key="record.id">
            <td>{{ (page - 1) * 20 + index + 1 }}</td>
            <td>{{ record.localFileName }}</td>
            <td>{{ formatDateTime(record.startTime) }}</td>
            <td>{{ record.duration ? Math.round(record.duration / 60) : '-' }}</td>
            <td>
              <span class="djskw" :class="statusClass(record.status)">{{ record.status }}</span>
            </td>
            <td>
              <span
                v-if="record.analysisStatus"
                class="djskw"
                :class="record.analysisStatus === 'completed' ? 'green' : 'gold'"
              >{{ record.analysisStatus }}</span>
              <span v-else>-</span>
            </td>
            <td>{{ record.sensitiveWordCount }}</td>
            <td>{{ record.operationKeywordCount }}</td>
            <td>
              <span class="djslink" @click="viewRecap(record)">查看复盘</span>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div v-if="total > 20" class="djspagination">
        <button
          class="page-btn"
          :disabled="page <= 1"
          @click="onPageChange(page - 1)"
        >‹</button>
        <span>第 {{ page }} 页 / 共 {{ Math.ceil(total / 20) }} 页</span>
        <button
          class="page-btn"
          :disabled="page >= Math.ceil(total / 20)"
          @click="onPageChange(page + 1)"
        >›</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.recap-table-page {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-1);
}

/* gold class override for djskw since design-system doesn't define it by name */
:deep(.djskw.gold) {
  background: var(--gold-soft);
  color: #B27400;
}
</style>
