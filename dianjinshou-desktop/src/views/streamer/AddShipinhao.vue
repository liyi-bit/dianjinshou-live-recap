<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'

const router = useRouter()
const currentStep = ref(1)
const localServiceStarted = ref(false)

const steps = [
  { title: '了解流程', description: '添加视频号直播间的授权流程说明' },
  { title: '授权步骤', description: '按照以下步骤完成授权' },
  { title: '启动服务', description: '开启本地服务以接收授权回调' },
  { title: '完成授权', description: '前往微信完成授权操作' }
]

function goBack() {
  if (currentStep.value > 1) {
    currentStep.value--
  } else {
    router.push('/streamers')
  }
}

function nextStep() {
  if (currentStep.value < 4) {
    currentStep.value++
  }
}

function startLocalService() {
  localServiceStarted.value = true
  Message.success('本地服务已启动，请继续下一步')
  // Placeholder: actual service start logic
}

function openAuthUrl() {
  Message.info('正在打开授权页面...')
  // Placeholder: open auth URL in system browser
  // window.open('https://channels.weixin.qq.com/...', '_blank')
}
</script>

<template>
  <div class="djscard">
    <!-- Blue info box -->
    <div class="info-box">
      <strong>添加视频号直播间说明：</strong>视频号直播间需要通过微信授权方式添加。完成授权后系统将自动监测直播状态并录制分析。请按照以下步骤完成授权流程。
    </div>

    <!-- Shared recording quota warning -->
    <div class="quota-warn">
      注意：视频号直播录制名额与其他平台共享，请合理分配名额资源。
    </div>

    <!-- Step wizard -->
    <div class="step-wizard">
      <div
        v-for="(step, index) in steps"
        :key="index"
        class="step-item"
        :class="{ active: currentStep === index + 1, done: currentStep > index + 1 }"
      >
        <div class="step-num">{{ index + 1 }}</div>
        <div class="step-info">
          <div class="step-title">{{ step.title }}</div>
          <div class="step-desc-text">{{ step.description }}</div>
        </div>
        <div v-if="index < steps.length - 1" class="step-arrow">›</div>
      </div>
    </div>

    <!-- Step 1: Introduction -->
    <div v-if="currentStep === 1" class="step-panel">
      <div class="step-panel-icon">&#9432;</div>
      <h3>添加视频号</h3>
      <p class="step-body">
        视频号直播间需要通过微信授权的方式添加。整个流程分为 4 个步骤，
        请按照指引依次完成。授权完成后系统将自动监测直播状态并录制分析。
      </p>
      <div class="step-panel-actions">
        <button class="djsbtn primary lg" @click="nextStep">前往授权 ›</button>
      </div>
    </div>

    <!-- Step 2: Flow chart -->
    <div v-if="currentStep === 2" class="step-panel">
      <h3>授权步骤说明</h3>
      <div class="flow-steps">
        <div class="flow-step">
          <div class="flow-num">1</div>
          <div class="flow-content">
            <strong>启动本地服务</strong>
            <p>点击"开启本地服务"按钮，系统将在本地启动一个临时服务用于接收微信授权回调。</p>
          </div>
        </div>
        <div class="flow-connector">↓</div>
        <div class="flow-step">
          <div class="flow-num">2</div>
          <div class="flow-content">
            <strong>扫码授权</strong>
            <p>使用微信扫描授权二维码，在微信中确认授权视频号直播间的访问权限。</p>
          </div>
        </div>
        <div class="flow-connector">↓</div>
        <div class="flow-step">
          <div class="flow-num">3</div>
          <div class="flow-content">
            <strong>选择视频号</strong>
            <p>在微信授权页面中选择需要添加的视频号账号。</p>
          </div>
        </div>
        <div class="flow-connector">↓</div>
        <div class="flow-step">
          <div class="flow-num">4</div>
          <div class="flow-content">
            <strong>完成添加</strong>
            <p>授权成功后，系统将自动获取视频号信息并添加到直播间列表中。</p>
          </div>
        </div>
      </div>
      <div class="step-panel-actions">
        <button class="djsbtn primary lg" @click="nextStep">下一步 ›</button>
      </div>
    </div>

    <!-- Step 3: Start local service -->
    <div v-if="currentStep === 3" class="step-panel">
      <div class="step-panel-icon">&#128421;</div>
      <h3>启动本地服务</h3>
      <p class="step-body">
        点击下方按钮启动本地服务，用于接收微信视频号的授权回调。请确保未被防火墙拦截。
      </p>
      <div class="step-panel-actions">
        <button
          class="djsbtn primary lg"
          :disabled="localServiceStarted"
          @click="startLocalService"
        >
          {{ localServiceStarted ? '服务已启动' : '点我开启本地服务' }}
        </button>
        <button
          v-if="localServiceStarted"
          class="djsbtn primary lg"
          @click="nextStep"
        >下一步 ›</button>
      </div>
    </div>

    <!-- Step 4: Auth -->
    <div v-if="currentStep === 4" class="step-panel">
      <div class="step-panel-icon">&#128279;</div>
      <h3>完成授权</h3>
      <p class="step-body">
        点击下方按钮将打开微信授权页面。请在微信中完成扫码授权操作。授权完成后页面将自动跳转回直播间列表。
      </p>
      <div class="step-panel-actions">
        <button class="djsbtn primary lg" @click="openAuthUrl">去授权</button>
        <button class="djsbtn ghost lg" @click="router.push('/streamers')">稍后授权</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.info-box { background:var(--brand-soft); border-radius:var(--radius-lg); padding:14px 18px; margin-bottom:22px; font-size:13px; color:var(--text-2b); line-height:1.7 }
.warn-box { background:var(--gold-soft); border-radius:var(--radius-lg); padding:10px 16px; margin-bottom:16px; font-size:12px; color:#8B5A00; display:flex; align-items:center; justify-content:space-between }
.quota-warn { background:#FFF0F0; border-left:3px solid var(--red); border-radius:6px; padding:10px 14px; margin-bottom:22px; font-size:12px; color:#CF1322 }

.step-wizard { display:flex; align-items:center; gap:0; margin-bottom:32px; padding:16px 20px; background:#F8FAFF; border-radius:var(--radius-lg) }
.step-item { display:flex; align-items:center; gap:10px; flex:1 }
.step-num { width:28px; height:28px; border-radius:50%; background:#D0D7E8; color:#fff; font-size:13px; font-weight:600; display:flex; align-items:center; justify-content:center; flex-shrink:0 }
.step-item.active .step-num { background:var(--brand) }
.step-item.done .step-num { background:var(--green) }
.step-info { flex:1 }
.step-title { font-size:13px; font-weight:600; color:var(--text-2b) }
.step-item.active .step-title { color:var(--brand) }
.step-desc-text { font-size:11px; color:var(--text-3); margin-top:1px }
.step-arrow { font-size:18px; color:#C9D0DF; padding:0 4px; flex-shrink:0 }

.step-panel { max-width:520px; margin:0 auto; text-align:center; padding:24px 0 8px }
.step-panel-icon { font-size:44px; margin-bottom:14px }
.step-panel h3 { font-size:20px; font-weight:600; color:var(--text-1); margin-bottom:12px }
.step-body { font-size:13px; color:#6B7080; line-height:1.7; margin-bottom:24px }
.step-panel-actions { display:flex; gap:12px; justify-content:center; margin-top:4px }

.flow-steps { text-align:left; margin-bottom:24px; width:100% }
.flow-step { display:flex; gap:14px; align-items:flex-start; padding:12px 16px; background:#F8FAFF; border-radius:8px }
.flow-num { width:26px; height:26px; border-radius:50%; background:var(--brand); color:#fff; font-size:12px; font-weight:700; display:flex; align-items:center; justify-content:center; flex-shrink:0; margin-top:2px }
.flow-content strong { display:block; margin-bottom:4px; font-size:13px; color:var(--text-1) }
.flow-content p { margin:0; font-size:12px; color:var(--text-3); line-height:1.5 }
.flow-connector { text-align:center; padding:4px 0; font-size:18px; color:#C9D0DF }
</style>
