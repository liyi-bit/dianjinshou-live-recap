<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { IconRefresh, IconMinus, IconExpand, IconShrink, IconClose } from '@arco-design/web-vue/es/icon'

const isMaximized = ref(false)

async function checkMaximized() {
  if (window.electronAPI) {
    isMaximized.value = await window.electronAPI.isMaximized()
  }
}

function handleRefresh() {
  window.location.reload()
}

async function handleMinimize() {
  await window.electronAPI?.minimizeWindow()
}

async function handleMaximize() {
  await window.electronAPI?.maximizeWindow()
  await checkMaximized()
}

async function handleClose() {
  await window.electronAPI?.closeWindow()
}

let resizeHandler: (() => void) | null = null

onMounted(() => {
  checkMaximized()
  resizeHandler = () => checkMaximized()
  window.addEventListener('resize', resizeHandler)
})

onUnmounted(() => {
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
  }
})
</script>

<template>
  <div class="title-bar">
    <div class="title-bar__drag">
      <div class="title-bar__info">
        <span class="title-bar__icon">💰</span>
        <span class="title-bar__title">点金手</span>
      </div>
    </div>
    <div class="title-bar__controls">
      <button class="title-bar__btn" title="刷新" @click="handleRefresh">
        <IconRefresh />
      </button>
      <button class="title-bar__btn" title="最小化" @click="handleMinimize">
        <IconMinus />
      </button>
      <button class="title-bar__btn" title="最大化/还原" @click="handleMaximize">
        <IconExpand v-if="!isMaximized" />
        <IconShrink v-else />
      </button>
      <button class="title-bar__btn title-bar__btn--close" title="关闭" @click="handleClose">
        <IconClose />
      </button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.title-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 30px;
  background: var(--text-1, #0F1726);
  user-select: none;
  -webkit-app-region: drag;
  // 保证窗口按钮栏永远在右侧抽屉/弹窗之上（arco drawer 默认 z-index=1001）
  position: relative;
  z-index: 10001;

  &__drag {
    flex: 1;
    height: 100%;
    display: flex;
    align-items: center;
    -webkit-app-region: drag;
    padding-left: 12px;
  }

  &__info {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__icon {
    font-size: 14px;
  }

  &__title {
    font-size: 13px;
    font-weight: 600;
    color: rgba(255, 255, 255, 0.85);
  }

  &__controls {
    display: flex;
    height: 100%;
    -webkit-app-region: no-drag;
  }

  &__btn {
    width: 46px;
    height: 100%;
    border: none;
    background: transparent;
    color: rgba(255, 255, 255, 0.65);
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    transition: background 0.15s, color 0.15s;

    &:hover {
      background: rgba(255, 255, 255, 0.1);
      color: rgba(255, 255, 255, 0.95);
    }

    &--close:hover {
      background: #e81123;
      color: #fff;
    }
  }
}
</style>
