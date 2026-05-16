<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import CustomVideoPlayer from '@/components/common/CustomVideoPlayer.vue'
import * as cloudApi from '@/api/cloudSpace'
import type { CloudReadonlyDetail } from '@/api/cloudSpace'
import { formatDateTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref<CloudReadonlyDetail | null>(null)

const fileId = computed(() => Number(route.params.id))
const file = computed(() => detail.value?.file)
const videoUrl = computed(() => detail.value?.signedUrl?.url || '')
const recap = computed<any>(() => detail.value?.recapDetail || null)
const comparison = computed<any>(() => detail.value?.comparisonDetail || null)
const isComparison = computed(() => {
  const businessType = file.value?.businessType || ''
  return businessType === 'full_comparison' || businessType === 'clip_comparison'
})

onMounted(load)

async function load() {
  if (!fileId.value) return
  loading.value = true
  try {
    const res: any = await cloudApi.getCloudReadonlyDetail(fileId.value)
    detail.value = res?.data ?? res
  } finally {
    loading.value = false
  }
}

function nameOf() {
  const current = file.value
  return current?.displayName || current?.fileName || '未命名文件'
}

function formatSize(bytes?: number | null) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let value = bytes
  let idx = 0
  while (value >= 1024 && idx < units.length - 1) {
    value /= 1024
    idx++
  }
  return `${value.toFixed(idx === 0 ? 0 : 1)} ${units[idx]}`
}

function formatDuration(seconds?: number | null) {
  if (!seconds) return '-'
  const min = Math.floor(seconds / 60)
  const sec = seconds % 60
  return `${min}分${String(sec).padStart(2, '0')}秒`
}

function formatCloudTime() {
  const current = file.value
  return current?.recordedAt || current?.createdAt ? formatDateTime(current.recordedAt || current.createdAt) : '-'
}

function textOf(value: unknown) {
  if (value == null || value === '') return '暂无内容'
  if (typeof value === 'string') return value
  return JSON.stringify(value, null, 2)
}

async function download() {
  const url = detail.value?.signedUrl?.url
  if (!url) {
    Message.warning('暂无可下载文件')
    return
  }
  window.open(url, '_blank')
}

async function downloadToLocal() {
  const signed = await cloudApi.requestDownloadToLocal(fileId.value) as any
  const data = signed?.data ?? signed
  if (!data?.url) {
    Message.warning('暂无可下载文件')
    return
  }
  if (!window.electronAPI?.downloadCloudFile) {
    window.open(data.url, '_blank')
    return
  }
  const result = await window.electronAPI.downloadCloudFile({ url: data.url, fileName: nameOf() })
  if (result.success && result.data?.path) {
    await cloudApi.markDownloadToLocalComplete(fileId.value, result.data.path)
    Message.success('已下载到本地')
    await load()
  } else if (!result.canceled) {
    Message.error(result.error || '下载失败')
  }
}
</script>

<template>
  <div class="readonly-page">
    <header class="detail-head pg-title">
      <button class="djsbtn ghost sm back-action" @click="router.back()">返回</button>
      <div class="pt-main">
        <div class="pt-info">
        <div class="eyebrow">云端只读</div>
        <h1 class="pt-name">{{ nameOf() }}</h1>
        </div>
      </div>
      <div class="pt-actions">
        <button class="djsbtn ghost sm" :disabled="!detail?.allowDownload" @click="download">下载</button>
        <button class="djsbtn primary sm" :disabled="!detail?.allowDownloadToLocal" @click="downloadToLocal">下载到本地</button>
      </div>
    </header>

    <div v-if="loading" class="empty djscard">加载中...</div>
    <div v-else-if="!detail" class="empty djscard">暂无云端详情</div>

    <div v-else class="detail-grid">
      <section class="video-panel djscard">
        <CustomVideoPlayer v-if="videoUrl" :src="videoUrl" preload="metadata" class="video" />
        <div v-else class="video-empty">该记录暂无云端视频文件</div>
      </section>

      <aside class="info-panel djscard">
        <div class="info-row"><span>类型</span><strong>{{ isComparison ? '对比复盘' : (file?.businessType === 'clip_recap' ? '切片复盘' : '全场复盘') }}</strong></div>
        <div class="info-row"><span>主播</span><strong>{{ file?.anchorName || '-' }}</strong></div>
        <div class="info-row"><span>录制时间</span><strong>{{ formatCloudTime() }}</strong></div>
        <div class="info-row"><span>视频时长</span><strong>{{ formatDuration(file?.durationSeconds) }}</strong></div>
        <div class="info-row"><span>文件大小</span><strong>{{ formatSize(file?.fileSize) }}</strong></div>
        <div class="info-row"><span>上传账号</span><strong>{{ file?.uploadAccount || '-' }}</strong></div>
      </aside>

      <section v-if="!isComparison" class="content-panel djscard">
        <div class="section-title">逐字稿</div>
        <pre>{{ textOf(recap?.asrText) }}</pre>
      </section>

      <section v-if="!isComparison" class="content-panel djscard">
        <div class="section-title">AI诊断报告</div>
        <pre>{{ textOf(recap?.aiDiagnosis || recap?.aiResult || recap?.summary) }}</pre>
      </section>

      <section v-if="isComparison" class="content-panel wide djscard">
        <div class="section-title">对比分析</div>
        <pre>{{ textOf(comparison?.aiComparisonResult || comparison) }}</pre>
      </section>
    </div>
  </div>
</template>

<style scoped>
.readonly-page{height:100%;overflow:auto;color:var(--text-2);min-width:0}
.detail-head{display:flex;align-items:center;gap:14px;margin-bottom:14px;padding:16px 20px}
.back-action{margin-right:2px}
.eyebrow{font-size:12px;color:var(--text-3);font-weight:650;letter-spacing:.04em;margin-bottom:2px}
.pt-name{max-width:780px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.djsbtn:disabled{opacity:.45;cursor:not-allowed;transform:none}
.detail-grid{display:grid;grid-template-columns:minmax(0,1fr) 300px;gap:14px}
.video-panel{height:430px;overflow:hidden}
.video{width:100%;height:100%}
.video-empty{height:100%;display:flex;align-items:center;justify-content:center;color:var(--text-3)}
.info-panel{padding:16px}
.info-row{display:flex;justify-content:space-between;gap:14px;padding:12px 0;border-bottom:1px solid var(--line);font-size:13px}
.info-row:last-child{border-bottom:0}
.info-row span{color:var(--text-3)}
.info-row strong{font-weight:650;text-align:right;word-break:break-all}
.content-panel{min-height:220px;padding:16px;overflow:hidden}
.content-panel.wide{grid-column:1 / -1}
.section-title{font-weight:700;margin-bottom:12px;color:var(--text-1);font-size:14px}
pre{white-space:pre-wrap;word-break:break-word;font-family:var(--ff);font-size:13px;line-height:1.8;color:var(--text-2b);margin:0;max-height:360px;overflow:auto}
.empty{height:260px;display:flex;align-items:center;justify-content:center;color:var(--text-3)}
@media (max-width: 1180px){
  .detail-grid{grid-template-columns:1fr}
  .video-panel{height:360px}
  .detail-head{align-items:flex-start}
}
</style>
