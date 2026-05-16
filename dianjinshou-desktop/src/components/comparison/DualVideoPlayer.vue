<script setup lang="ts">
import { useDualSync } from '@/composables/useDualSync'

const { video1, video2, isPlaying, play, pause } = useDualSync()
</script>

<template>
  <div class="dual-video">
    <div class="video-panel">
      <div class="video-placeholder">
        <video ref="video1" style="display: none" />
        <icon-play-circle :size="36" />
        <p>视频1（优化场次）</p>
        <p class="hint">待集成 video.js</p>
      </div>
    </div>
    <div class="video-panel">
      <div class="video-placeholder">
        <video ref="video2" style="display: none" />
        <icon-play-circle :size="36" />
        <p>视频2（参考场次）</p>
        <p class="hint">待集成 video.js</p>
      </div>
    </div>
    <div class="sync-controls">
      <a-button v-if="!isPlaying" size="small" type="primary" @click="play">同步播放</a-button>
      <a-button v-else size="small" @click="pause">暂停</a-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.dual-video {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;

  .video-panel {
    flex: 1;
    min-width: 200px;
  }

  .video-placeholder {
    background: #000;
    border-radius: 8px;
    aspect-ratio: 9/16;
    max-height: 300px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #666;
    p { margin: 4px 0; font-size: 12px; }
    .hint { font-size: 11px; color: #444; }
  }

  .sync-controls {
    width: 100%;
    display: flex;
    justify-content: center;
    padding-top: 8px;
  }
}
</style>
