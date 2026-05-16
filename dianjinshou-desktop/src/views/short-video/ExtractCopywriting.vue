<template>
  <div class="extract-copywriting">
    <!-- Input area -->
    <div class="djstoolbar" style="margin-bottom:16px">
      <!-- Source type selector -->
      <div class="djsradio-group">
        <div class="djsradio" :class="{ on: form.sourceType === 'url' }" @click="form.sourceType = 'url'">视频链接</div>
        <div class="djsradio" :class="{ on: form.sourceType === 'local' }" @click="form.sourceType = 'local'">本地文件</div>
      </div>
      <span class="div"></span>
      <input
        v-if="form.sourceType === 'url'"
        class="djsinput"
        style="flex:1;min-width:260px;border-radius:8px"
        v-model="form.sourceUrl"
        placeholder="粘贴短视频链接（抖音 / 快手 / 视频号）"
      />
      <input
        v-else
        class="djsinput"
        style="flex:1;min-width:260px;border-radius:8px"
        v-model="form.storageKey"
        placeholder="已上传文件的存储 Key"
      />
      <input
        class="djsinput"
        style="width:180px;border-radius:8px"
        v-model="form.title"
        placeholder="标题（可选）"
      />
      <button class="djsbtn primary" :disabled="extracting" @click="handleExtract">
        {{ extracting ? '提取中…' : '提取' }}
      </button>
    </div>

    <!-- Results table -->
    <div class="djscard">
      <div class="djscard-title">提取记录</div>
      <div v-if="store.copywritingLoading" class="djsempty">
        <div class="ti">加载中…</div>
      </div>
      <div v-else-if="!store.copywritingList.length" class="djsempty">
        <div class="ic">📝</div>
        <div class="ti">暂无提取记录</div>
        <p>粘贴视频链接后点击「提取」开始</p>
      </div>
      <table v-else class="djstbl">
        <thead>
          <tr>
            <th>标题 / 链接</th>
            <th>来源</th>
            <th>字数</th>
            <th>复制次数</th>
            <th>状态</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="record in store.copywritingList" :key="record.id">
            <td class="ec-title">{{ record.title || record.sourceUrl || '-' }}</td>
            <td>
              <span class="djschip" :class="record.sourceType === 'url' ? 'chip-blue' : 'chip-green'">
                {{ record.sourceType === 'url' ? '链接' : '本地' }}
              </span>
            </td>
            <td>{{ record.wordCount ?? '-' }}</td>
            <td>{{ record.copyCount ?? 0 }}</td>
            <td>
              <span class="ec-status" :class="record.status">
                {{ statusLabel(record.status) }}
              </span>
            </td>
            <td>{{ formatDateTime(record.createdAt) }}</td>
            <td>
              <span
                v-if="record.status === 'completed' && record.polishedText"
                class="djslink"
                style="margin-right:12px"
                @click="handleCopy(record)"
              >复制文案</span>
              <span class="djslink danger" @click="handleDelete(record.id)">删除</span>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div v-if="store.copywritingTotal > 20" class="djspagination">
        <span>共 {{ store.copywritingTotal }} 条</span>
        <button
          class="page-btn"
          :disabled="currentPage === 1"
          @click="onPageChange(currentPage - 1)"
        >‹</button>
        <button
          v-for="p in pageCount"
          :key="p"
          class="page-btn"
          :class="{ active: p === currentPage }"
          @click="onPageChange(p)"
        >{{ p }}</button>
        <button
          class="page-btn"
          :disabled="currentPage === pageCount"
          @click="onPageChange(currentPage + 1)"
        >›</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useShortVideoStore } from '@/stores/shortVideo'
import { formatDateTime } from '@/utils/format'

const store = useShortVideoStore()
const extracting = ref(false)
const currentPage = ref(1)

const pageCount = computed(() => Math.ceil(store.copywritingTotal / 20) || 1)

function statusLabel(status: string): string {
  const map: Record<string, string> = {
    pending: '待处理', asr: 'ASR中', polishing: '润色中', completed: '已完成', failed: '失败'
  }
  return map[status] ?? status
}

const form = reactive({
  sourceType: 'url',
  sourceUrl: '',
  storageKey: '',
  title: ''
})

onMounted(() => {
  store.fetchCopywriting(1, 20)
})

async function handleExtract() {
  extracting.value = true
  const result = await store.createExtract({
    sourceType: form.sourceType,
    sourceUrl: form.sourceType === 'url' ? form.sourceUrl : undefined,
    storageKey: form.sourceType !== 'url' ? form.storageKey : undefined,
    title: form.title || undefined
  })
  extracting.value = false
  if (result) {
    form.sourceUrl = ''
    form.storageKey = ''
    form.title = ''
    store.fetchCopywriting(1, 20)
  }
}

function onPageChange(page: number) {
  currentPage.value = page
  store.fetchCopywriting(page, 20)
}

async function handleCopy(record: any) {
  if (record.polishedText) {
    await navigator.clipboard.writeText(record.polishedText)
    store.doCopy(record.id)
  }
}

async function handleDelete(id: number) {
  const ok = await store.removeCopywriting(id)
  if (ok) store.fetchCopywriting(currentPage.value, 20)
}
</script>

<style scoped>
.extract-copywriting { padding: 0 24px; }

.ec-title { max-width: 240px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* status chips */
.ec-status {
  display: inline-flex; align-items: center;
  padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 500;
}
.ec-status.pending  { background: var(--brand-soft); color: var(--brand); }
.ec-status.asr      { background: #FFF1E1; color: #D25A00; }
.ec-status.polishing{ background: #FFF1E1; color: #D25A00; }
.ec-status.completed{ background: #E8F6EF; color: #00855A; }
.ec-status.failed   { background: #FFF0F0; color: #E0341E; }

/* source chips */
.chip-blue { border-color: #DCE6FF; color: var(--brand); background: var(--brand-soft); }
.chip-green{ border-color: #B7E5D0; color: #00855A; background: #E8F6EF; }

.djslink.danger { color: #E0341E; }
.djslink.danger:hover { text-decoration: underline; }
</style>
