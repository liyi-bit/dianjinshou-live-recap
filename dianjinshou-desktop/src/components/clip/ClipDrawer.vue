<script setup lang="ts">
import { ref, watch } from 'vue'
import { Message, Modal } from '@arco-design/web-vue'
import * as analysisApi from '@/api/analysis'
import * as recordingApi from '@/api/recording'
import { nowLocalDateTime } from '@/utils/format'

const props = defineProps<{
  visible: boolean
  recordingId: number
  duration: number
  videoFilePath: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'created'): void
}>()

const CLIP_CATEGORIES = [
  { value: 'RETENTION', label: '留人切片' },
  { value: 'QUALITY_SPEECH', label: '优质话术' },
  { value: 'MARKETING', label: '营销塑品' },
  { value: 'INTERACTION', label: '互动切片' },
  { value: 'FAN_CLUB', label: '粉团切片' },
  { value: 'EXPRESSION', label: '表现力切片' },
  { value: 'COMPLIANCE', label: '规避违规' },
  { value: 'VIEWPOINT', label: '观点切片' },
  { value: 'EXAMPLE', label: '举例切片' },
  { value: 'PRIVATE_DOMAIN', label: '引导私域' },
  { value: 'PERSONA', label: '人设切片' },
  { value: 'LOOP_SPEECH', label: '循环话术' },
  { value: 'BIE_DAN', label: '憋单切片' },
  { value: 'OTHER', label: '其他切片' }
]

const form = ref({
  clipCategory: '',
  clipFilename: '',
  clipRemark: ''
})

const submitting = ref(false)

// Range thumbs live in a stable ref — arco's v-model owns it, we never write back during drag.
const timeRange = ref<[number, number]>([0, 60])

// Reset the range whenever the drawer opens for a new recording or duration finishes loading.
watch(
  () => [props.visible, props.duration] as const,
  ([vis, d]) => {
    if (vis && d && d > 0) timeRange.value = [0, d]
  },
  { immediate: true }
)

// arco clamps a dragging thumb to the slider's min/max. Dragging the start thumb past
// the end leaves both at the far edge (merged). Auto-swap on update so thumbs pass through
// cleanly instead of sticking together.
function onRangeUpdate(val: unknown) {
  if (!Array.isArray(val) || val.length !== 2) return
  let [s, e] = val as [number, number]
  if (s > e) [s, e] = [e, s]
  timeRange.value = [s, e]
}

function formatSeconds(s: number): string {
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
}

async function submit() {
  if (!form.value.clipCategory) {
    Message.warning('请选择切片分类')
    return
  }
  let [clipStart, clipEnd] = timeRange.value
  if (clipStart > clipEnd) [clipStart, clipEnd] = [clipEnd, clipStart]
  if (clipStart === clipEnd) {
    Message.warning('起止时间相同，无法切片')
    return
  }

  submitting.value = true
  // Step 0: 创建占位 task（同步快返），让"切片复盘列表"立即出现一条 transcribing 状态行
  let draftTaskId: number | null = null
  try {
    const draftRes: any = await analysisApi.createClipDraft({
      recordingId: props.recordingId,
      clipStart,
      clipEnd,
      clipCategory: form.value.clipCategory,
      clipFilename: form.value.clipFilename || undefined,
      clipRemark: form.value.clipRemark || undefined,
    })
    const data = draftRes?.data ?? draftRes
    draftTaskId = data?.taskId ?? data?.id ?? null
  } catch (err: any) {
    submitting.value = false
    if (err?.code === 40006) {
      Modal.warning({
        title: '今日 AI 复盘额度已用完',
        content: '每个账号每天最多 10 次 AI 复盘（含切片）。明天 0 点自动重置，届时可继续使用。',
        okText: '我知道了',
        hideCancel: true,
      })
    } else {
      Message.error(err?.message || '切片创建失败')
    }
    return
  }

  // 立即关闭抽屉 + 通知父组件刷新列表（用户可以离开页面，后续 ASR 在后台进行）
  Message.success('切片已创建，逐字稿生成中…')
  emit('created')
  emit('update:visible', false)
  submitting.value = false

  // 后台异步：截取 + ASR + 回填。失败时把 task 标记为 failed，但不阻塞用户。
  void runClipPipelineInBackground(draftTaskId, clipStart, clipEnd, form.value.clipCategory,
    form.value.clipFilename || undefined, form.value.clipRemark || undefined)
}

/**
 * 后台流水线（detached promise）：ffmpeg 截取 → 桌面端 ASR → 调 submit-clip-asr 携带 taskId 完成。
 * 即便用户切走路由，promise 会继续在 SPA 内存中执行；切实失败时通过 markClipFailed 反馈到列表的 failed 状态。
 */
async function runClipPipelineInBackground(
  draftTaskId: number | null,
  clipStart: number,
  clipEnd: number,
  clipCategory: string,
  clipFilename: string | undefined,
  clipRemark: string | undefined,
) {
  const api = (window as any).electronAPI
  let clipFilePath: string | undefined
  try {
    if (api?.extractClip && props.videoFilePath) {
      const clipName = clipFilename || `clip_${clipStart}-${clipEnd}`
      const result = await api.extractClip({
        sourcePath: props.videoFilePath,
        clipStart,
        clipEnd,
        clipFilename: clipName,
      })
      if (!result.success) throw new Error('视频截取失败: ' + (result.error || '未知错误'))
      clipFilePath = result.clipPath
    }

    const asrTargetPath = clipFilePath || props.videoFilePath
    if (!api?.runAsr || !asrTargetPath) throw new Error('当前环境不支持桌面端 ASR')
    const asrResult = await api.runAsr(asrTargetPath)
    if (!asrResult.success || !asrResult.data?.length) {
      throw new Error('语音识别失败: ' + (asrResult.error || '未识别到内容'))
    }

    const analysisResult: any = await analysisApi.submitClipAsrResult({
      taskId: draftTaskId ?? undefined,
      recordingId: props.recordingId,
      clipStart,
      clipEnd,
      clipCategory,
      clipFilename,
      clipFilePath,
      clipRemark,
      segments: asrResult.data,
    })
    if (clipFilePath && api?.enqueueCloudUpload) {
      enqueueClipCloudUpload(clipFilePath, analysisResult?.data ?? analysisResult, clipStart, clipEnd).catch((err) => {
        console.warn('[ClipDrawer] enqueue clip cloud upload failed:', err)
      })
    }
    Message.success('切片逐字稿已就绪，AI 分析进行中')
  } catch (err: any) {
    console.error('[ClipDrawer] background clip pipeline failed:', err)
    Message.error(err?.message || '切片处理失败')
    if (draftTaskId && (analysisApi as any).markClipFailed) {
      try { await (analysisApi as any).markClipFailed(draftTaskId, err?.message || '切片失败') } catch { /* ignore */ }
    }
  }
}

async function enqueueClipCloudUpload(clipFilePath: string, analysisResult: any, clipStart: number, clipEnd: number) {
  const api = (window as any).electronAPI
  const recordingRes: any = await recordingApi.getRecording(props.recordingId)
  const recording = recordingRes?.data ?? recordingRes
  const taskId = analysisResult?.taskId || analysisResult?.id
  await api.enqueueCloudUpload({
    filePath: clipFilePath,
    fileName: fileNameOf(clipFilePath),
    contentType: 'video/mp4',
    businessType: 'clip_recap',
    businessId: taskId,
    recordingId: props.recordingId,
    clipId: taskId,
    streamerId: recording?.streamerId,
    anchorName: recording?.streamerInfo?.anchorName || recording?.anchorName,
    accountType: recording?.streamerInfo?.accountType,
    recordedAt: recording?.startTime || nowLocalDateTime(),
    durationSeconds: Math.max(0, clipEnd - clipStart),
  })
}

function fileNameOf(path: string) {
  return path.split(/[\\/]/).pop() || path
}
</script>

<template>
  <a-drawer
    :visible="visible"
    title="切片"
    :width="720"
    @cancel="emit('update:visible', false)"
    :footer="false"
  >
    <a-form :model="form" layout="vertical">
      <a-form-item label="切片时间范围">
        <div style="display:flex; flex-direction:column; gap:6px; width:100%;">
          <a-slider
            :model-value="timeRange"
            range
            :min="0"
            :max="duration || 3600"
            :step="1"
            @update:model-value="onRangeUpdate"
          />
          <div style="display:flex; justify-content:space-between; font-size:12px; color:var(--color-text-3)">
            <span>{{ formatSeconds(timeRange[0]) }}</span>
            <span>{{ formatSeconds(timeRange[1]) }}</span>
          </div>
        </div>
      </a-form-item>

      <a-form-item label="切片分类" required>
        <a-select v-model="form.clipCategory" placeholder="请选择切片分类">
          <a-option v-for="cat in CLIP_CATEGORIES" :key="cat.value" :value="cat.value">
            {{ cat.label }}
          </a-option>
        </a-select>
      </a-form-item>

      <a-form-item label="文件名">
        <a-input v-model="form.clipFilename" placeholder="自动生成或手动输入" />
      </a-form-item>

      <a-form-item label="备注（≤100字）">
        <a-textarea
          v-model="form.clipRemark"
          placeholder="添加备注"
          :max-length="100"
          show-word-limit
          :auto-size="{ minRows: 2, maxRows: 4 }"
        />
      </a-form-item>

      <a-form-item>
        <a-button type="primary" :loading="submitting" @click="submit" long>
          确认切片
        </a-button>
      </a-form-item>
    </a-form>
  </a-drawer>
</template>
