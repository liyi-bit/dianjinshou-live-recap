import { ref, onUnmounted } from 'vue'

/**
 * Synchronizes two video elements using requestAnimationFrame.
 * Keeps them within 100ms tolerance.
 */
export function useDualSync() {
  const video1 = ref<HTMLVideoElement | null>(null)
  const video2 = ref<HTMLVideoElement | null>(null)
  const isPlaying = ref(false)
  let animFrameId: number | null = null

  const TOLERANCE = 0.1 // 100ms

  function syncLoop() {
    if (video1.value && video2.value && isPlaying.value) {
      const diff = Math.abs(video1.value.currentTime - video2.value.currentTime)
      if (diff > TOLERANCE) {
        video2.value.currentTime = video1.value.currentTime
      }
    }
    animFrameId = requestAnimationFrame(syncLoop)
  }

  function play() {
    if (video1.value && video2.value) {
      video1.value.play()
      video2.value.play()
      isPlaying.value = true
      if (!animFrameId) {
        syncLoop()
      }
    }
  }

  function pause() {
    if (video1.value) video1.value.pause()
    if (video2.value) video2.value.pause()
    isPlaying.value = false
  }

  function seekTo(time: number) {
    if (video1.value) video1.value.currentTime = time
    if (video2.value) video2.value.currentTime = time
  }

  function stop() {
    pause()
    if (animFrameId) {
      cancelAnimationFrame(animFrameId)
      animFrameId = null
    }
  }

  onUnmounted(() => {
    stop()
  })

  return { video1, video2, isPlaying, play, pause, seekTo, stop }
}
