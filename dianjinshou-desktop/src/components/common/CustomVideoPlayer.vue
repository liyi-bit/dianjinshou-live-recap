<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

interface Props {
  src: string
  /** 默认 metadata，避免一次性加载整段视频 */
  preload?: 'none' | 'metadata' | 'auto'
  /** 静音默认 false */
  muted?: boolean
}
const props = withDefaults(defineProps<Props>(), {
  preload: 'metadata',
  muted: false,
})

const emit = defineEmits<{
  (e: 'timeupdate', seconds: number): void
  (e: 'loadedmetadata', duration: number): void
  (e: 'ended'): void
  (e: 'play'): void
  (e: 'pause'): void
}>()

const videoEl = ref<HTMLVideoElement | null>(null)
const wrapperEl = ref<HTMLDivElement | null>(null)

const playing = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const buffered = ref(0)
const muted = ref(props.muted)
// 父组件传入的 muted 变化时同步到内部 ref
watch(() => props.muted, (v) => { muted.value = v })
const volume = ref(1)
const showVolumePanel = ref(false)
const isFullscreen = ref(false)
const isHovering = ref(false)
const isControlsVisible = ref(true)
let hideTimer: number | undefined

const progressPct = computed(() => duration.value > 0 ? (currentTime.value / duration.value) * 100 : 0)
const bufferedPct = computed(() => duration.value > 0 ? (buffered.value / duration.value) * 100 : 0)

function format(t: number): string {
  if (!isFinite(t) || t < 0) return '00:00'
  const h = Math.floor(t / 3600)
  const m = Math.floor((t % 3600) / 60)
  const s = Math.floor(t % 60)
  const mm = String(m).padStart(2, '0')
  const ss = String(s).padStart(2, '0')
  return h > 0 ? `${h}:${mm}:${ss}` : `${mm}:${ss}`
}

function togglePlay() {
  const v = videoEl.value
  if (!v) return
  if (v.paused) v.play().catch(() => {})
  else v.pause()
}

function onTimeUpdate() {
  const v = videoEl.value
  if (!v) return
  currentTime.value = v.currentTime
  // buffered
  if (v.buffered.length > 0) {
    buffered.value = v.buffered.end(v.buffered.length - 1)
  }
  emit('timeupdate', v.currentTime)
}

function onLoadedMeta() {
  const v = videoEl.value
  if (!v) return
  duration.value = v.duration || 0
  emit('loadedmetadata', duration.value)
}

function onPlay() { playing.value = true; emit('play') }
function onPause() { playing.value = false; emit('pause') }
function onEnded() { playing.value = false; emit('ended') }

const isDraggingProgress = ref(false)
const progressTrack = ref<HTMLDivElement | null>(null)

function pctFromMouse(ev: MouseEvent | PointerEvent): number {
  const track = progressTrack.value
  if (!track) return 0
  const rect = track.getBoundingClientRect()
  return Math.max(0, Math.min(1, (ev.clientX - rect.left) / rect.width))
}

function onProgressDown(ev: PointerEvent) {
  isDraggingProgress.value = true
  applyProgress(pctFromMouse(ev))
  window.addEventListener('pointermove', onProgressMove)
  window.addEventListener('pointerup', onProgressUp, { once: true })
}

function onProgressMove(ev: PointerEvent) {
  if (!isDraggingProgress.value) return
  applyProgress(pctFromMouse(ev))
}

function onProgressUp() {
  isDraggingProgress.value = false
  window.removeEventListener('pointermove', onProgressMove)
}

function applyProgress(pct: number) {
  const v = videoEl.value
  if (!v || !duration.value) return
  v.currentTime = pct * duration.value
}

function toggleMute() {
  const v = videoEl.value
  if (!v) return
  v.muted = !v.muted
  muted.value = v.muted
}

function changeVolume(ev: Event) {
  const v = videoEl.value
  if (!v) return
  const val = Number((ev.target as HTMLInputElement).value)
  v.volume = val
  volume.value = val
  if (val > 0 && v.muted) {
    v.muted = false
    muted.value = false
  }
}

function toggleFullscreen() {
  const w = wrapperEl.value
  if (!w) return
  if (!document.fullscreenElement) {
    w.requestFullscreen?.().catch(() => {})
  } else {
    document.exitFullscreen?.().catch(() => {})
  }
}

function onFullscreenChange() {
  isFullscreen.value = !!document.fullscreenElement
}

function showControls() {
  isControlsVisible.value = true
  if (hideTimer) window.clearTimeout(hideTimer)
  if (playing.value && !isHovering.value) {
    hideTimer = window.setTimeout(() => { isControlsVisible.value = false }, 3000)
  }
}

function onMouseEnter() { isHovering.value = true; showControls() }
function onMouseMove() { showControls() }
function onMouseLeave() {
  isHovering.value = false
  if (playing.value) {
    if (hideTimer) window.clearTimeout(hideTimer)
    hideTimer = window.setTimeout(() => { isControlsVisible.value = false }, 1500)
  }
}

watch(playing, (v) => {
  if (!v) {
    isControlsVisible.value = true
    if (hideTimer) window.clearTimeout(hideTimer)
  } else {
    showControls()
  }
})

onMounted(() => {
  document.addEventListener('fullscreenchange', onFullscreenChange)
})
onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', onFullscreenChange)
  if (hideTimer) window.clearTimeout(hideTimer)
})

// 暴露给父组件
function seekTo(seconds: number, autoPlay = true) {
  const v = videoEl.value
  if (!v) return
  if (!isFinite(seconds) || seconds < 0) return
  v.currentTime = seconds
  if (autoPlay) v.play().catch(() => {})
}

function play() { videoEl.value?.play().catch(() => {}) }
function pause() { videoEl.value?.pause() }

/** 实时获取当前时间（getter 在 .value 二次访问时返回死值，故改为函数） */
function getCurrentTime() { return videoEl.value?.currentTime ?? 0 }
function getDuration() { return videoEl.value?.duration ?? 0 }

defineExpose({
  seekTo,
  play,
  pause,
  getCurrentTime,
  getDuration,
  /** 直接获取 video 元素以便父组件做更细粒度操作（不可解构使用） */
  videoEl,
})
</script>

<template>
  <div
    ref="wrapperEl"
    class="cvp-wrapper"
    :class="{ 'cvp-fullscreen': isFullscreen }"
    @mouseenter="onMouseEnter"
    @mousemove="onMouseMove"
    @mouseleave="onMouseLeave"
  >
    <video
      ref="videoEl"
      class="cvp-video"
      :src="src"
      :preload="preload"
      :muted="muted"
      @timeupdate="onTimeUpdate"
      @loadedmetadata="onLoadedMeta"
      @play="onPlay"
      @pause="onPause"
      @ended="onEnded"
      @click="togglePlay"
    />

    <!-- 中央播放按钮（暂停时显示） -->
    <div v-if="!playing" class="cvp-center-btn" @click="togglePlay">
      <svg viewBox="0 0 24 24" fill="currentColor" width="28" height="28">
        <path d="M8 5v14l11-7z"/>
      </svg>
    </div>

    <!-- 控制条 -->
    <div class="cvp-controls" :class="{ 'cvp-controls--hidden': !isControlsVisible }">
      <!-- 进度条 -->
      <div
        ref="progressTrack"
        class="cvp-progress"
        @pointerdown="onProgressDown"
      >
        <div class="cvp-progress__bg"></div>
        <div class="cvp-progress__buffered" :style="{ width: bufferedPct + '%' }"></div>
        <div class="cvp-progress__fill" :style="{ width: progressPct + '%' }"></div>
        <div class="cvp-progress__handle" :style="{ left: progressPct + '%' }"></div>
      </div>

      <!-- 控制按钮行 -->
      <div class="cvp-bar">
        <button class="cvp-btn" @click="togglePlay" :title="playing ? '暂停' : '播放'">
          <svg v-if="playing" viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
            <path d="M6 5h4v14H6zm8 0h4v14h-4z"/>
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
            <path d="M8 5v14l11-7z"/>
          </svg>
        </button>

        <div class="cvp-time">
          <span class="cvp-time__cur">{{ format(currentTime) }}</span>
          <span class="cvp-time__sep">/</span>
          <span class="cvp-time__dur">{{ format(duration) }}</span>
        </div>

        <div class="cvp-spacer"></div>

        <div
          class="cvp-vol"
          @mouseenter="showVolumePanel = true"
          @mouseleave="showVolumePanel = false"
        >
          <button class="cvp-btn" @click="toggleMute" :title="muted ? '取消静音' : '静音'">
            <svg v-if="muted || volume === 0" viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
              <path d="M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51A8.796 8.796 0 0021 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06a8.99 8.99 0 003.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z"/>
            </svg>
            <svg v-else viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
              <path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z"/>
            </svg>
          </button>
          <div v-show="showVolumePanel" class="cvp-vol-panel">
            <input
              type="range"
              min="0"
              max="1"
              step="0.05"
              :value="muted ? 0 : volume"
              @input="changeVolume"
              class="cvp-vol-slider"
            />
          </div>
        </div>

        <button class="cvp-btn" @click="toggleFullscreen" :title="isFullscreen ? '退出全屏' : '全屏'">
          <svg v-if="!isFullscreen" viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
            <path d="M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z"/>
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
            <path d="M5 16h3v3h2v-5H5v2zm3-8H5v2h5V5H8v3zm6 11h2v-3h3v-2h-5v5zm2-11V5h-2v5h5V8h-3z"/>
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cvp-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  background: #000;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.cvp-fullscreen { background: #000; }

.cvp-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
  cursor: pointer;
  background: #000;
}

/* 中央播放按钮 */
.cvp-center-btn {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: auto;
  cursor: pointer;
  background: linear-gradient(to bottom, transparent 50%, rgba(0,0,0,0.2));
}
.cvp-center-btn::before {
  content: '';
  width: 64px; height: 64px;
  border-radius: 50%;
  background:
    radial-gradient(circle at 30% 30%, rgba(255,255,255,0.18), transparent 60%),
    linear-gradient(135deg, var(--brand-light, #D09E4E), var(--brand-dark, #8F6224));
  border: 2px solid rgba(255,255,255,0.45);
  box-shadow:
    0 8px 24px rgba(0,0,0,0.45),
    0 0 0 6px rgba(184,130,58,0.16),
    inset 0 1px 0 rgba(255,255,255,0.3);
  position: absolute;
  transition: transform .2s var(--ease, ease-out);
}
.cvp-center-btn:hover::before { transform: scale(1.08); }
.cvp-center-btn svg {
  position: relative;
  color: #fff;
  margin-left: 4px;
  filter: drop-shadow(0 1px 2px rgba(0,0,0,0.4));
}

/* 控制条 */
.cvp-controls {
  position: absolute;
  left: 0; right: 0; bottom: 0;
  padding: 24px 12px 8px;
  background: linear-gradient(to top, rgba(20, 14, 8, 0.95) 0%, rgba(20, 14, 8, 0.65) 60%, transparent 100%);
  transition: opacity .25s ease, transform .25s ease;
  user-select: none;
  pointer-events: auto;
  z-index: 2;
}
.cvp-controls--hidden {
  opacity: 0;
  transform: translateY(8px);
  pointer-events: none;
}

/* 进度条 */
.cvp-progress {
  position: relative;
  height: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  margin-bottom: 6px;
}
.cvp-progress__bg {
  position: absolute;
  left: 0; right: 0;
  height: 3px;
  background: rgba(255,255,255,0.18);
  border-radius: 2px;
  transition: height .15s ease;
}
.cvp-progress:hover .cvp-progress__bg,
.cvp-progress:hover .cvp-progress__buffered,
.cvp-progress:hover .cvp-progress__fill { height: 5px; }
.cvp-progress__buffered {
  position: absolute;
  left: 0; height: 3px;
  background: rgba(255,255,255,0.30);
  border-radius: 2px;
  transition: width .15s linear, height .15s ease;
}
.cvp-progress__fill {
  position: absolute;
  left: 0; height: 3px;
  background: linear-gradient(90deg, var(--brand-light, #D09E4E), var(--brand, #B8823A));
  border-radius: 2px;
  transition: width .1s linear, height .15s ease;
  box-shadow: 0 0 8px rgba(184,130,58,0.5);
}
.cvp-progress__handle {
  position: absolute;
  top: 50%;
  width: 12px; height: 12px;
  border-radius: 50%;
  background:
    radial-gradient(circle at 30% 30%, #F4D99B, var(--brand, #B8823A));
  border: 2px solid #FBE8B8;
  transform: translate(-50%, -50%) scale(0);
  box-shadow: 0 2px 6px rgba(0,0,0,0.45);
  transition: transform .15s var(--ease, ease-out);
}
.cvp-progress:hover .cvp-progress__handle { transform: translate(-50%, -50%) scale(1); }

/* 控制条按钮行 */
.cvp-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 32px;
}
.cvp-btn {
  width: 30px; height: 30px;
  border-radius: 6px;
  background: transparent;
  border: none;
  color: #F4D99B;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  transition: background .15s ease, color .15s ease;
}
.cvp-btn:hover {
  background: rgba(184,130,58,0.22);
  color: #FFFAEC;
}

.cvp-time {
  font-family: 'JetBrains Mono', 'SF Mono', monospace;
  font-size: 12px;
  color: rgba(244,217,155,0.85);
  display: flex;
  align-items: center;
  gap: 4px;
  letter-spacing: .02em;
}
.cvp-time__cur { color: #FFFAEC; font-weight: 600; }
.cvp-time__sep { color: rgba(244,217,155,0.4); }

.cvp-spacer { flex: 1; }

.cvp-vol { position: relative; }
.cvp-vol-panel {
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%) translateY(-4px);
  padding: 8px 6px;
  background: rgba(20, 14, 8, 0.95);
  border: 1px solid rgba(184,130,58,0.4);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.5);
}
.cvp-vol-slider {
  -webkit-appearance: slider-vertical;
  appearance: slider-vertical;
  width: 18px;
  height: 80px;
  outline: none;
  cursor: pointer;
  accent-color: var(--brand-light, #D09E4E);
}
</style>
