<script setup lang="ts">
import { onMounted } from 'vue'
import type { CloudFileItem } from '@/api/cloudSpace'
import { useStreamerStore } from '@/stores/streamer'
import { formatDateTime } from '@/utils/format'

defineProps<{
  visible: boolean
  items: CloudFileItem[]
}>()

const streamerStore = useStreamerStore()

onMounted(() => {
  streamerStore.fetchAllStreamers()
})

const emit = defineEmits<{
  close: []
  back: []
  start: []
}>()

function nameOf(item: CloudFileItem) {
  return item.displayName || item.fileName || '未命名文件'
}

function streamerInitial(name?: string | null) {
  return (name || '?').trim().charAt(0) || '?'
}

function avatarFor(item?: CloudFileItem) {
  if (!item) return null
  if (item.anchorAvatar) return item.anchorAvatar
  const anchorName = item.anchorName?.trim()
  const streamer = streamerStore.allStreamers.find((s) =>
    (item.streamerId != null && String(s.id) === String(item.streamerId)) ||
    (!!anchorName && s.anchorName?.trim() === anchorName)
  )
  return streamer?.anchorAvatar || null
}

function formatCloudTime(item?: CloudFileItem) {
  return item?.recordedAt || item?.createdAt ? formatDateTime(item.recordedAt || item.createdAt) : '-'
}

function formatDuration(seconds?: number | null) {
  if (!seconds) return '-'
  const min = Math.floor(seconds / 60)
  const sec = seconds % 60
  return `${min}分${String(sec).padStart(2, '0')}秒`
}
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="confirm-mask">
      <div class="confirm-card djsmodal">
        <div class="confirm-head djsmodal-head">
          <span class="djsmodal-title">添加对比分析</span>
          <button class="close-btn" @click="emit('close')">×</button>
        </div>
        <div class="pair djsmodal-body">
          <div class="slot optimize">
            <div class="slot-title">优化场次</div>
            <div v-if="items[0]" class="mini-file">
              <div class="djsav g3 avatar-box">
                <img v-if="avatarFor(items[0])" :src="avatarFor(items[0])!" referrerpolicy="no-referrer" />
                <template v-else>{{ streamerInitial(items[0]?.anchorName || nameOf(items[0])) }}</template>
              </div>
              <div>
                <div class="name">{{ nameOf(items[0]) }}</div>
                <div class="meta">{{ items[0].accountType || '账号' }}</div>
              </div>
            </div>
            <div class="info">录制时间：{{ formatCloudTime(items[0]) }}</div>
            <div class="info">录制时长：{{ formatDuration(items[0]?.durationSeconds) }}</div>
          </div>
          <div class="swap">→</div>
          <div class="slot reference">
            <div class="slot-title">参考场次</div>
            <div v-if="items[1]" class="mini-file">
              <div class="djsav g1 avatar-box">
                <img v-if="avatarFor(items[1])" :src="avatarFor(items[1])!" referrerpolicy="no-referrer" />
                <template v-else>{{ streamerInitial(items[1]?.anchorName || nameOf(items[1])) }}</template>
              </div>
              <div>
                <div class="name">{{ nameOf(items[1]) }}</div>
                <div class="meta">{{ items[1].accountType || '账号' }}</div>
              </div>
            </div>
            <div class="info">录制时间：{{ formatCloudTime(items[1]) }}</div>
            <div class="info">录制时长：{{ formatDuration(items[1]?.durationSeconds) }}</div>
          </div>
        </div>
        <div class="actions djsmodal-foot">
          <button class="djsbtn ghost" @click="emit('back')">重新选择</button>
          <button class="djsbtn primary" @click="emit('start')">开始对比</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.confirm-mask{position:fixed;inset:0;background:rgba(26,22,18,.45);display:flex;align-items:center;justify-content:center;z-index:1100;backdrop-filter:blur(4px)}
.confirm-card{width:680px}
.confirm-head{height:auto}
.close-btn{width:30px;height:30px;border:1px solid transparent;border-radius:var(--radius-sm);background:transparent;color:var(--text-3);font-size:22px;line-height:1;cursor:pointer}
.close-btn:hover{background:var(--hov);color:var(--text-1);border-color:var(--line)}
.pair{display:grid;grid-template-columns:1fr 42px 1fr;align-items:center;gap:0;padding:24px 22px}
.slot{height:196px;border-radius:var(--radius-md);padding:14px;border:1px solid var(--line);background:linear-gradient(180deg,var(--card-hover),var(--card));box-shadow:var(--sh-in),0 1px 2px rgba(36,30,24,.025)}
.slot.optimize{border-color:rgba(184,130,58,.24);background:linear-gradient(180deg,var(--brand-soft-06),var(--card))}
.slot.reference{border-color:rgba(74,104,150,.22);background:linear-gradient(180deg,var(--blue2-soft),var(--card))}
.slot-title{text-align:center;font-size:13px;font-weight:700;color:var(--text-2);margin-bottom:12px}
.mini-file{display:flex;align-items:center;gap:10px;background:rgba(253,252,248,.82);border:1px solid var(--line);border-radius:var(--radius-sm);padding:10px;margin-bottom:12px}
.mini-file .djsav{width:40px;height:40px}
.avatar-box{overflow:hidden;flex-shrink:0;color:#fff;font-size:13px;font-weight:650}
.avatar-box img{width:100%;height:100%;display:block;object-fit:cover}
.name{max-width:210px;font-size:13.5px;font-weight:650;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;color:var(--text-1)}
.meta,.info{font-size:12px;color:var(--text-3);line-height:1.9}
.swap{text-align:center;color:var(--brand);font-size:24px;font-family:var(--fm);font-weight:700}
.actions{justify-content:center}
</style>
