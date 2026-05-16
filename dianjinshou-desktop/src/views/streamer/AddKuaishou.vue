<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { createStreamer, getIndustries } from '@/api/streamer'
import type { IndustryTree } from '@/api/streamer'
import { useAppStore } from '@/stores/app'
import { storeToRefs } from 'pinia'

const router = useRouter()
const appStore = useAppStore()
const { resolution, segmentDuration } = storeToRefs(appStore)
const loading = ref(false)
const resolving = ref(false)
const resolvedName = ref('')
const resolvedAvatar = ref('')
const resolvedLive = ref<boolean | null>(null)
const industries = ref<IndustryTree[]>([])

const form = reactive({
  accountId: '',
  industryId: undefined as number | undefined,
  accountType: 'own',
  liveRoomMode: '',
  accountStage: '',
  accountLevel: '',
  trafficStructure: '',
  broadcastTimeStart: '',
  broadcastTimeEnd: '',
  accountIssue: '',
  defaultLanguage: '中文通用'
})

const liveRoomModeOptions = [
  { label: '多人间隔', value: '多人间隔' },
  { label: '多人日不落', value: '多人日不落' },
  { label: '单人日不落', value: '单人日不落' },
  { label: '单人间隔', value: '单人间隔' }
]

const accountStageOptions = [
  { label: '起号期', value: '起号期' },
  { label: '上升期', value: '上升期' },
  { label: '打标签期', value: '打标签期' },
  { label: '变现期', value: '变现期' },
  { label: '平稳期', value: '平稳期' },
  { label: '瓶颈期', value: '瓶颈期' },
  { label: '衰退期', value: '衰退期' }
]

const trafficStructureOptions = [
  { label: '纯自然流', value: '纯自然流' },
  { label: '微付费', value: '微付费' },
  { label: '重付费', value: '重付费' },
  { label: '纯付费', value: '纯付费' }
]

const languageOptions = [
  { label: '中文通用', value: '中文通用' },
  { label: '英语', value: '英语' },
  { label: '粤语', value: '粤语' },
  { label: '日语', value: '日语' },
  { label: '韩语', value: '韩语' }
]

const optionalExpanded = ref(false)

type RecordingResolution = '480p' | '720p' | '1080p' | 'source'
let resolveTimer: ReturnType<typeof setTimeout> | null = null

async function syncDesktopSettingsIfAvailable() {
  const sync = (appStore as any).syncDesktopSettings
  if (typeof sync === 'function') {
    await sync.call(appStore).catch(() => {})
  }
}

function getRecordingSegmentSeconds(): number {
  const minutes = Math.max(10, Math.min(180, Number(segmentDuration.value) || 30))
  return minutes * 60
}

function buildKuaishouRoomUrl(accountId: string): string {
  return `https://live.kuaishou.com/u/${accountId}`
}

function onAccountIdInput() {
  if (resolveTimer) clearTimeout(resolveTimer)
  resolvedName.value = ''
  resolvedAvatar.value = ''
  resolvedLive.value = null
  if (!form.accountId.trim()) return
  resolveTimer = setTimeout(() => resolveKuaishouRoom(false), 800)
}

async function resolveKuaishouRoom(showError = false): Promise<boolean> {
  const input = form.accountId.trim()
  if (!input) return false
  const api = (window as any).electronAPI
  if (!api?.resolveKuaishouRoom) return false

  resolving.value = true
  try {
    const res = await api.resolveKuaishouRoom(input)
    if (res.success && res.data) {
      form.accountId = res.data.accountId || res.data.webRid || res.data.roomId || form.accountId
      resolvedName.value = res.data.streamerName || ''
      resolvedAvatar.value = res.data.streamerAvatar || ''
      resolvedLive.value = res.data.isLive ?? null
      return true
    }
    if (showError) {
      Message.error(res.error || '无法识别快手直播间，请检查快手号或直播间链接')
    }
    return false
  } catch {
    if (showError) Message.error('快手直播间解析失败，请重试')
    return false
  } finally {
    resolving.value = false
  }
}

const selectedParentId = ref<number | undefined>(undefined)
const industryOptions = ref<any[]>([])

const subIndustryOptions = computed(() => {
  if (!selectedParentId.value) return []
  const parent = industryOptions.value.find((o: any) => o.value === selectedParentId.value)
  return parent?.children || []
})

function handleParentChange(val: string) {
  const id = val ? Number(val) : undefined
  selectedParentId.value = id
  const parent = industryOptions.value.find((o: any) => o.value === id)
  if (parent?.children?.length) {
    form.industryId = undefined
  } else {
    form.industryId = id
  }
}

function handleSubChange(val: string) {
  form.industryId = val ? Number(val) : selectedParentId.value
}

onMounted(async () => {
  await syncDesktopSettingsIfAvailable()
  try {
    const res = await getIndustries()
    const data = (res as any).data ?? res
    industries.value = Array.isArray(data) ? data : []
    industryOptions.value = industries.value.map((item) => ({
      value: item.id,
      label: item.name,
      children: item.children?.length
        ? item.children.map((c) => ({ value: c.id, label: c.name }))
        : []
    }))
  } catch {
    // ignore
  }
})

async function handleSubmit() {
  if (!form.accountId.trim()) {
    Message.error('请输入快手号或直播间链接')
    return
  }

  const resolved = await resolveKuaishouRoom(true)
  if (!resolved && /^https?:\/\//.test(form.accountId.trim())) {
    return
  }
  form.accountId = form.accountId.trim()

  loading.value = true
  try {
    const result = await createStreamer({
      platform: 'kuaishou',
      accountId: form.accountId,
      anchorName: resolvedName.value || undefined,
      anchorAvatar: resolvedAvatar.value || undefined,
      industryId: form.industryId,
      accountType: form.accountType,
      liveRoomMode: form.liveRoomMode || undefined,
      accountStage: form.accountStage || undefined,
      accountLevel: form.accountLevel || undefined,
      trafficStructure: form.trafficStructure || undefined,
      broadcastTimeStart: form.broadcastTimeStart || undefined,
      broadcastTimeEnd: form.broadcastTimeEnd || undefined,
      accountIssue: form.accountIssue || undefined,
      defaultLanguage: form.defaultLanguage || undefined
    })
    const streamerId = (result as any)?.id
    if (streamerId) {
      const api = (window as any).electronAPI
      if (api?.addMonitoredStreamer) {
        await syncDesktopSettingsIfAvailable()
        await api.addMonitoredStreamer({
          streamerId: String(streamerId),
          platform: 'kuaishou',
          roomId: form.accountId || '',
          roomUrl: buildKuaishouRoomUrl(form.accountId),
          anchorName: resolvedName.value || form.accountId,
          resolution: resolution.value as RecordingResolution,
          segmentDuration: getRecordingSegmentSeconds(),
          autoRecord: true,
        }).catch(() => { /* best effort */ })
      }
    }
    Message.success('添加成功，已自动开启录制')
    try { await (window as any).electronAPI?.pollLiveStatusNow?.() } catch { /* best effort */ }
    router.push('/streamers')
  } catch (err: any) {
    const msg = err?.response?.data?.message || err?.message || '添加失败'
    Message.error(msg)
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push('/streamers')
}
</script>

<template>
  <div class="djscard">
    <!-- Blue info box -->
    <div class="info-box">
      <strong>添加快手直播间说明：</strong>请输入快手号、直播间链接或分享文案，系统会自动识别主播信息并接入直播录制。
    </div>

    <!-- 快手号 / 直播间链接 -->
    <div class="djsform-row">
      <div class="djsform-label req">快手号</div>
      <div class="djsform-ctrl">
        <div class="ks-input-line">
          <input
            class="djsinput sq"
            v-model="form.accountId"
            placeholder="请输入快手号，或粘贴 https://live.kuaishou.com/u/..."
            @input="onAccountIdInput"
          />
        </div>
        <div class="share-hint">
          <span>支持：快手号、直播间链接、分享文案，输入后自动识别主播</span>
        </div>
        <div v-if="resolving" class="form-hint" style="margin-top:6px;color:var(--brand);">正在查询主播信息...</div>
        <div v-else-if="resolvedName" class="form-hint" style="margin-top:6px;">
          <span style="color:var(--text-1);">主播：<strong>{{ resolvedName }}</strong></span>
          <span v-if="resolvedLive === true" style="color:var(--green);margin-left:8px;">直播中</span>
          <span v-else-if="resolvedLive === false" style="color:var(--text-3);margin-left:8px;">未开播</span>
        </div>
      </div>
    </div>

    <!-- 行业选择 -->
    <div class="djsform-row">
      <div class="djsform-label req">行业选择</div>
      <div class="djsform-ctrl">
        <select class="djsselect" @change="handleParentChange(($event.target as HTMLSelectElement).value)">
          <option value="">请选择一级行业</option>
          <option v-for="opt in industryOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
        </select>
        <select v-if="subIndustryOptions.length" class="djsselect" style="margin-left:10px;" @change="handleSubChange(($event.target as HTMLSelectElement).value)">
          <option value="">请选择二级行业</option>
          <option v-for="opt in subIndustryOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
        </select>
      </div>
    </div>

    <!-- 账号归属 -->
    <div class="djsform-row">
      <div class="djsform-label req">账号归属</div>
      <div class="djsform-ctrl">
        <div class="djsradio-group">
          <div
            class="djsradio"
            :class="{ on: form.accountType === 'own' }"
            @click="form.accountType = 'own'"
          >自有账号</div>
          <div
            class="djsradio"
            :class="{ on: form.accountType === 'industry' }"
            @click="form.accountType = 'industry'"
          >同行业账号</div>
        </div>
      </div>
    </div>

    <!-- 直播间模式 -->
    <div class="djsform-row">
      <div class="djsform-label">直播间模式</div>
      <div class="djsform-ctrl">
        <div class="djsradio-group">
          <div
            v-for="opt in liveRoomModeOptions"
            :key="opt.value"
            class="djsradio"
            :class="{ on: form.liveRoomMode === opt.value }"
            @click="form.liveRoomMode = opt.value"
          >{{ opt.label }}直播</div>
        </div>
      </div>
    </div>

    <!-- Optional fields divider -->
    <div class="opt-divider" @click="optionalExpanded = !optionalExpanded">
      <span>以下内容非必填，填写后可提升 AI 分析准确度</span>
      <span class="opt-toggle">{{ optionalExpanded ? '收起' : '展开填写' }}
        <svg class="opt-chevron" :class="{ open: optionalExpanded }" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="4 6 8 10 12 6"/></svg>
      </span>
    </div>

    <div v-show="optionalExpanded">
    <!-- 账号阶段 -->
    <div class="djsform-row">
      <div class="djsform-label">账号阶段</div>
      <div class="djsform-ctrl">
        <div class="djsradio-group">
          <div
            v-for="opt in accountStageOptions"
            :key="opt.value"
            class="djsradio"
            :class="{ on: form.accountStage === opt.value }"
            @click="form.accountStage = opt.value"
          >{{ opt.label }}</div>
        </div>
      </div>
    </div>

    <!-- 账号问题 -->
    <div class="djsform-row top">
      <div class="djsform-label" style="padding-top:8px;">账号问题</div>
      <div class="djsform-ctrl">
        <textarea
          class="djsinput sq"
          v-model="form.accountIssue"
          placeholder="请描述账号当前存在的问题（选填）"
          rows="3"
          style="width:100%;resize:vertical;"
        ></textarea>
      </div>
    </div>

    <!-- 账号水平 -->
    <div class="djsform-row">
      <div class="djsform-label">账号水平</div>
      <div class="djsform-ctrl">
        <div class="djsradio-group">
          <div
            class="djsradio"
            :class="{ on: form.accountLevel === '头部' }"
            @click="form.accountLevel = '头部'"
          >头部</div>
          <div
            class="djsradio"
            :class="{ on: form.accountLevel === '中腰部' }"
            @click="form.accountLevel = '中腰部'"
          >中腰部</div>
          <div
            class="djsradio"
            :class="{ on: form.accountLevel === '尾部' }"
            @click="form.accountLevel = '尾部'"
          >尾部</div>
        </div>
      </div>
    </div>

    <!-- 流量结构 -->
    <div class="djsform-row">
      <div class="djsform-label">流量结构</div>
      <div class="djsform-ctrl">
        <div class="djsradio-group">
          <div
            v-for="opt in trafficStructureOptions"
            :key="opt.value"
            class="djsradio"
            :class="{ on: form.trafficStructure === opt.value }"
            @click="form.trafficStructure = opt.value"
          >{{ opt.label }}</div>
        </div>
      </div>
    </div>

    <!-- 开播时间 -->
    <div class="djsform-row">
      <div class="djsform-label">开播时间</div>
      <div class="djsform-ctrl" style="display:flex;gap:8px;">
        <input class="djsinput sq" type="time" v-model="form.broadcastTimeStart" style="width:120px;" />
        <span style="color:var(--text-3);">至</span>
        <input class="djsinput sq" type="time" v-model="form.broadcastTimeEnd" style="width:120px;" />
      </div>
    </div>

    <!-- 默认识别语言 -->
    <div class="djsform-row">
      <div class="djsform-label">默认识别语言</div>
      <div class="djsform-ctrl">
        <select class="djsselect" v-model="form.defaultLanguage">
          <option v-for="opt in languageOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
        </select>
      </div>
    </div>
    </div><!-- end v-show optionalExpanded -->

    <!-- Bottom buttons -->
    <div class="form-actions">
      <button class="djsbtn primary lg" :disabled="loading" @click="handleSubmit">
        {{ loading ? '提交中...' : '添加到直播间列表' }}
      </button>
      <button class="djsbtn ghost lg" @click="goBack">取消</button>
    </div>
  </div>
</template>

<style scoped>
.info-box { background:var(--brand-soft); border-radius:var(--radius-lg); padding:14px 18px; margin-bottom:22px; font-size:13px; color:var(--text-2b); line-height:1.7 }
.warn-box { background:var(--gold-soft); border-radius:var(--radius-lg); padding:10px 16px; margin-bottom:22px; font-size:12px; color:#8B5A00; display:flex; align-items:center; justify-content:space-between }
.opt-divider { background:var(--brand-soft); border-left:3px solid var(--brand); padding:10px 14px; margin:20px 0 18px 0; border-radius:var(--radius-sm); font-size:12px; color:var(--brand); cursor:pointer; display:flex; align-items:center; justify-content:space-between; transition:background .15s; user-select:none }
.opt-divider:hover { background:var(--brand-soft-06) }
.opt-toggle { display:inline-flex; align-items:center; gap:4px; font-weight:600; font-size:12px; color:var(--brand) }
.opt-chevron { width:14px; height:14px; transition:transform .25s var(--ease) }
.opt-chevron.open { transform:rotate(180deg) }
.ks-input-line { width:min(100%, 480px) }
.ks-input-line .djsinput { width:100% }
.form-hint { font-size:11px; color:var(--text-3); margin-top:4px }
.form-hint.warn { color:var(--orange) }
.form-actions { padding:24px 0 40px 146px; display:flex; gap:12px }
.share-hint { display:flex; align-items:center; justify-content:space-between; margin-top:6px; font-size:11px; color:var(--text-3) }
</style>
