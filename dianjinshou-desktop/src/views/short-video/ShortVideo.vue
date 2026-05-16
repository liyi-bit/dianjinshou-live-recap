<template>
  <div class="short-video">
    <!-- Sub-nav tabs -->
    <div class="djstabs">
      <div class="djstab" :class="{ on: activeTab === 'extract' }" @click="activeTab = 'extract'">提取文案</div>
      <div class="djstab" :class="{ on: activeTab === 'creators' }" @click="activeTab = 'creators'">搜达人</div>
      <div class="djstab" :class="{ on: activeTab === 'subscriptions' }" @click="activeTab = 'subscriptions'">订阅达人</div>
      <div class="djstab" :class="{ on: activeTab === 'groups' }" @click="activeTab = 'groups'">管理分组</div>
      <div class="djstab" :class="{ on: activeTab === 'trending' }" @click="activeTab = 'trending'">订阅爆款</div>
    </div>

    <!-- Tab panels -->
    <div class="sv-content">
      <ExtractCopywriting v-if="activeTab === 'extract'" />
      <SearchCreators v-else-if="activeTab === 'creators'" />
      <MySubscriptions v-else-if="activeTab === 'subscriptions'" />
      <div v-else-if="activeTab === 'groups'" class="djsempty">
        <div class="ic">📂</div>
        <div class="ti">管理分组</div>
        <p>分组管理功能即将上线</p>
      </div>
      <TrendingRules v-else-if="activeTab === 'trending'" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import ExtractCopywriting from './ExtractCopywriting.vue'
import SearchCreators from './SearchCreators.vue'
import MySubscriptions from './MySubscriptions.vue'
import TrendingRules from './TrendingRules.vue'

const route = useRoute()
const activeTab = ref((route.query.tab as string) || 'extract')

watch(() => route.query.tab, (tab) => {
  if (tab && typeof tab === 'string') activeTab.value = tab
})
</script>

<style scoped>
.short-video { display: flex; flex-direction: column; height: 100%; }
.sv-content { flex: 1; overflow: auto; padding: 16px 0; }
</style>
