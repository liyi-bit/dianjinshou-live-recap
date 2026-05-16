<script setup lang="ts">
import { IconClockCircle } from '@arco-design/web-vue/es/icon'
</script>

<template>
  <div class="coming-soon">
    <div class="coming-soon__content">
      <IconClockCircle class="coming-soon__icon" />
      <h2 class="coming-soon__title">即将上线</h2>
      <p class="coming-soon__desc">该功能正在开发中，敬请期待</p>
    </div>
  </div>
</template>

<style scoped lang="scss">
.coming-soon {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;

  &__content {
    text-align: center;
  }

  &__icon {
    font-size: 64px;
    color: var(--color-text-4);
    margin-bottom: 16px;
  }

  &__title {
    font-size: 24px;
    font-weight: 600;
    color: var(--color-text-1);
    margin: 0 0 8px;
  }

  &__desc {
    font-size: 14px;
    color: var(--color-text-3);
    margin: 0;
  }
}
</style>
