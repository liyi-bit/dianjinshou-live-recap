<template>
  <div class="search-creators">
    <!-- Search toolbar -->
    <div class="djstoolbar">
      <span class="lbl">搜索达人</span>
      <input
        class="djsinput"
        style="width:200px"
        v-model="keyword"
        placeholder="请输入达人昵称"
        @keyup.enter="doSearch"
      />
      <select class="djsselect" style="width:110px" v-model="platform" @change="doSearch">
        <option value="">全部平台</option>
        <option value="douyin">抖音</option>
        <option value="kuaishou">快手</option>
        <option value="shipinhao">视频号</option>
      </select>
      <input
        class="djsinput"
        style="width:120px"
        v-model="industry"
        placeholder="行业"
      />
      <button class="djsbtn primary" @click="doSearch">搜索</button>
    </div>

    <!-- Loading -->
    <div v-if="store.creatorLoading" class="djsempty">
      <div class="ti">搜索中…</div>
    </div>

    <!-- Empty state -->
    <div v-else-if="!store.creatorList.length" class="djsempty">
      <div class="ic">🔍</div>
      <div class="ti">输入关键词搜索达人</div>
      <p>支持抖音、快手、视频号平台达人搜索</p>
    </div>

    <!-- Creator grid -->
    <div v-else class="creator-grid">
      <div
        v-for="creator in store.creatorList"
        :key="creator.id"
        class="djscard creator-card"
        @click="openDetail(creator)"
      >
        <div class="cc-body">
          <div class="djsav" :class="platformGradient(creator.platform)">
            {{ creator.nickname?.charAt(0) || '?' }}
            <span class="pf" :style="{ background: platformColor(creator.platform) }">
              {{ platformLabel(creator.platform).charAt(0) }}
            </span>
          </div>
          <div class="cc-info">
            <div class="cc-name">{{ creator.nickname }}</div>
            <div class="cc-meta">
              <span class="djschip" style="padding:2px 8px;font-size:11px">{{ platformLabel(creator.platform) }}</span>
              <span v-if="creator.industry" class="cc-industry">{{ creator.industry }}</span>
            </div>
            <div class="cc-stats">
              <span>粉丝 <b>{{ formatCount(creator.followerCount) }}</b></span>
              <span>视频 <b>{{ creator.videoCount ?? 0 }}</b></span>
            </div>
          </div>
          <button class="djsbtn sm" @click.stop="openDetail(creator)">详情</button>
        </div>
      </div>
    </div>

    <!-- Detail overlay -->
    <div v-if="drawerVisible" class="sc-drawer-mask" @click.self="drawerVisible = false">
      <div class="sc-drawer">
        <div class="sc-drawer__head">
          <span class="sc-drawer__title">{{ selectedCreator?.nickname || '达人详情' }}</span>
          <span class="sc-drawer__close" @click="drawerVisible = false">✕</span>
        </div>
        <div class="sc-drawer__body" v-if="selectedCreator">
          <table class="djstbl" style="margin-bottom:20px">
            <tbody>
              <tr><td class="sc-label">平台</td><td>{{ platformLabel(selectedCreator.platform) }}</td></tr>
              <tr><td class="sc-label">粉丝数</td><td>{{ formatCount(selectedCreator.followerCount) }}</td></tr>
              <tr><td class="sc-label">视频数</td><td>{{ selectedCreator.videoCount ?? 0 }}</td></tr>
              <tr><td class="sc-label">行业</td><td>{{ selectedCreator.industry || '-' }}</td></tr>
              <tr><td class="sc-label">简介</td><td>{{ selectedCreator.description || '-' }}</td></tr>
            </tbody>
          </table>
          <button class="djsbtn primary" @click="handleSubscribe">订阅达人</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useShortVideoStore } from '@/stores/shortVideo'
import { subscribeCreator, type CreatorItem } from '@/api/shortVideo'
import { Message } from '@arco-design/web-vue'

const store = useShortVideoStore()
const keyword = ref('')
const platform = ref<string | undefined>()
const industry = ref('')
const drawerVisible = ref(false)
const selectedCreator = ref<CreatorItem | null>(null)

function doSearch() {
  store.fetchCreators({
    keyword: keyword.value || undefined,
    platform: platform.value,
    industry: industry.value || undefined
  })
}

function openDetail(creator: CreatorItem) {
  selectedCreator.value = creator
  drawerVisible.value = true
}

async function handleSubscribe() {
  if (!selectedCreator.value?.id) return
  try {
    await subscribeCreator(selectedCreator.value.id)
    Message.success('已订阅')
  } catch {
    Message.error('订阅失败')
  }
}

function formatCount(n: number): string {
  if (!n) return '0'
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return String(n)
}

function platformLabel(p: string): string {
  const map: Record<string, string> = { douyin: '抖音', kuaishou: '快手', shipinhao: '视频号' }
  return map[p] || p
}

function platformColor(p: string): string {
  const map: Record<string, string> = { douyin: '#000', kuaishou: '#FF4906', shipinhao: '#07C160' }
  return map[p] || '#B8823A'
}

function platformGradient(p: string): string {
  const map: Record<string, string> = { douyin: 'g1', kuaishou: 'g2', shipinhao: 'g3' }
  return map[p] || 'g5'
}
</script>

<style scoped>
.search-creators { padding: 0 24px; }

.creator-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 12px;
  margin-top: 4px;
}

.creator-card { padding: 16px; cursor: pointer; transition: box-shadow .15s; }
.creator-card:hover { box-shadow: 0 6px 20px -6px rgba(43,107,255,.18); }

.cc-body { display: flex; align-items: center; gap: 12px; }
.cc-info { flex: 1; min-width: 0; }
.cc-name { font-size: 14px; font-weight: 600; color: var(--text-1); margin-bottom: 5px; }
.cc-meta { display: flex; align-items: center; gap: 6px; margin-bottom: 5px; }
.cc-industry { font-size: 11px; color: var(--text-3); }
.cc-stats { font-size: 12px; color: var(--text-3); display: flex; gap: 14px; }
.cc-stats b { color: var(--text-1); }

/* Detail overlay */
.sc-drawer-mask {
  position: fixed; inset: 0; background: rgba(26,29,41,.35);
  z-index: 1000; display: flex; justify-content: flex-end;
}
.sc-drawer {
  width: 440px; height: 100%; background: var(--card);
  box-shadow: -8px 0 32px rgba(36,30,24,.12); display: flex; flex-direction: column;
}
.sc-drawer__head {
  padding: 18px 24px; border-bottom: 1px solid var(--line);
  display: flex; align-items: center; justify-content: space-between;
}
.sc-drawer__title { font-size: 15px; font-weight: 600; }
.sc-drawer__close { cursor: pointer; color: var(--text-3); font-size: 16px; }
.sc-drawer__body { padding: 20px 24px; flex: 1; overflow: auto; }
.sc-label { color: var(--text-3); font-size: 12px; width: 80px; }
</style>
