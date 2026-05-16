<script setup lang="ts">
import { computed, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BasicSettings from './tabs/BasicSettings.vue'
import AccountSettings from './tabs/AccountSettings.vue'
// v1.1.0：第三方接入 tab 不再暴露给用户；文件保留仅供 admin 后台将来复用
// import ThirdPartyTab from './tabs/ThirdPartyTab.vue'
import AsrProviderTab from './tabs/AsrProviderTab.vue'
import FeishuBotsTab from './tabs/FeishuBotsTab.vue'
import SubAccountsTab from './tabs/SubAccountsTab.vue'
import DictionaryTab from './tabs/DictionaryTab.vue'
import CustomerServiceTab from './tabs/CustomerServiceTab.vue'

const route = useRoute()
const router = useRouter()
const activeTab = computed(() => (route.params.tab as string) || 'basic')

const TABS = computed<{ key: string; label: string; comp: Component }[]>(() => [
  { key: 'basic', label: '基本设置', comp: BasicSettings },
  { key: 'account', label: '账号设置', comp: AccountSettings },
  // v1.1.0：桌面端不再需要配置第三方密钥，全部走系统默认
  // { key: 'third-party', label: '第三方接入', comp: ThirdPartyTab },
  { key: 'asr', label: '语音识别', comp: AsrProviderTab },
  { key: 'feishu-bots', label: '飞书机器人', comp: FeishuBotsTab },
])

const currentComponent = computed(() => {
  return TABS.value.find(t => t.key === activeTab.value)?.comp ?? BasicSettings
})

function switchTab(key: string) {
  router.push({ params: { tab: key } })
}
</script>

<template>
  <div class="settings-page">
    <div class="djstabs">
      <span
        v-for="tab in TABS"
        :key="tab.key"
        class="djstab"
        :class="{ on: activeTab === tab.key }"
        @click="switchTab(tab.key)"
      >{{ tab.label }}</span>
    </div>
    <div class="settings-content">
      <keep-alive>
        <component :is="currentComponent" :key="activeTab" />
      </keep-alive>
    </div>
  </div>
</template>

<style scoped>
.settings-page { display: flex; flex-direction: column; height: 100%; }
.settings-content { flex: 1; overflow-y: auto; padding: 20px 24px; }
</style>
