<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { useUserStore } from '@/stores/user'
import { sendSms } from '@/api/auth'
import TitleBar from '@/components/TitleBar.vue'

const router = useRouter()
const userStore = useUserStore()

// Tab: 'password' | 'sms'
const activeTab = ref('password')

// Password login form
const passwordForm = reactive({
  phone: '',
  password: '',
  rememberMe: false
})

// SMS login form
const smsForm = reactive({
  phone: '',
  code: ''
})

const loading = ref(false)
const smsLoading = ref(false)
const smsCooldown = ref(0)
let cooldownTimer: ReturnType<typeof setInterval> | null = null

// Restore remembered phone
onMounted(() => {
  const saved = localStorage.getItem('rememberedPhone')
  if (saved) {
    passwordForm.phone = saved
    passwordForm.rememberMe = true
  }
})

function isValidPhone(phone: string): boolean {
  return /^1[3-9]\d{9}$/.test(phone)
}

// Password login
async function handlePasswordLogin() {
  if (!isValidPhone(passwordForm.phone)) {
    Message.error('请输入正确的手机号')
    return
  }
  if (!passwordForm.password || passwordForm.password.length < 8) {
    Message.error('密码至少8位')
    return
  }
  loading.value = true
  try {
    await userStore.login({
      phone: passwordForm.phone,
      password: passwordForm.password,
      rememberMe: passwordForm.rememberMe
    })
    Message.success('登录成功')
  } catch {
    // error already shown by request interceptor
  } finally {
    loading.value = false
  }
}

// SMS login
async function handleSmsLogin() {
  if (!isValidPhone(smsForm.phone)) {
    Message.error('请输入正确的手机号')
    return
  }
  if (!smsForm.code) {
    Message.error('请输入验证码')
    return
  }
  loading.value = true
  try {
    await userStore.smsLogin({
      phone: smsForm.phone,
      code: smsForm.code
    })
    Message.success('登录成功')
  } catch {
    // error already shown by request interceptor
  } finally {
    loading.value = false
  }
}

// Send SMS code
async function handleSendSms() {
  const phone = smsForm.phone
  if (!isValidPhone(phone)) {
    Message.error('请输入正确的手机号')
    return
  }
  smsLoading.value = true
  try {
    await sendSms({ phone, type: 'login' })
    Message.success('验证码已发送')
    startCooldown()
  } catch (err: any) {
    Message.error(err?.response?.data?.message || '发送失败')
  } finally {
    smsLoading.value = false
  }
}

function startCooldown() {
  smsCooldown.value = 60
  if (cooldownTimer) clearInterval(cooldownTimer)
  cooldownTimer = setInterval(() => {
    smsCooldown.value--
    if (smsCooldown.value <= 0) {
      clearInterval(cooldownTimer!)
      cooldownTimer = null
    }
  }, 1000)
}

function goRegister() {
  router.push('/register')
}
</script>

<template>
  <div class="auth-page">
  <TitleBar />
  <div class="auth-bg">
    <div class="auth-card">
      <!-- Left panel -->
      <div class="auth-left">
        <div class="auth-logo">
          <span class="logo-mark">金</span>
          <span>点金手</span>
        </div>
        <h1>让每一场直播<br>都能被 AI 点石成金</h1>
        <p class="sub">覆盖抖音 / 快手 / 视频号的一站式直播 AI 复盘平台，帮助主播与 MCN 机构快速定位问题、优化话术、提升转化。</p>
        <div class="auth-tags">
          <span class="auth-tag">🎯 整场复盘</span>
          <span class="auth-tag">✂ 智能切片</span>
          <span class="auth-tag">🤖 AI 助手</span>
          <span class="auth-tag">📊 竞品分析</span>
          <span class="auth-tag">🛡 违规检测</span>
        </div>
        <p class="auth-slogan">服务 20,000+ 主播 · 累计分析 500,000+ 场直播 · 平均转化率提升 32% ·</p>
      </div>

      <!-- Right panel -->
      <div class="auth-right">
        <h2>欢迎回来</h2>
        <p class="hint">请登录你的点金手账号</p>

        <!-- Top tabs: 登录 / 注册 -->
        <div class="auth-tabs">
          <span class="auth-tab on">登录</span>
          <span class="auth-tab" @click="goRegister">注册</span>
        </div>

        <!-- Inner tabs: 密码登录 / 验证码登录 -->
        <div class="auth-tabs auth-tabs--inner">
          <span
            class="auth-tab"
            :class="{ on: activeTab === 'password' }"
            @click="activeTab = 'password'"
          >密码登录</span>
          <span
            class="auth-tab"
            :class="{ on: activeTab === 'sms' }"
            @click="activeTab = 'sms'"
          >验证码登录</span>
        </div>

        <!-- Password form -->
        <div v-if="activeTab === 'password'">
          <div class="auth-field">
            <input
              class="input"
              type="tel"
              placeholder="请输入手机号"
              maxlength="11"
              v-model="passwordForm.phone"
            />
          </div>
          <div class="auth-field">
            <input
              class="input"
              type="password"
              placeholder="请输入密码"
              maxlength="32"
              v-model="passwordForm.password"
              @keyup.enter="handlePasswordLogin"
            />
          </div>
          <div class="auth-field auth-field--row">
            <label class="remember-label">
              <input type="checkbox" v-model="passwordForm.rememberMe" />
              <span>记住我</span>
            </label>
            <a class="forgot-link" href="#">忘记密码？</a>
          </div>
          <button
            class="auth-btn"
            :disabled="loading"
            @click="handlePasswordLogin"
          >
            {{ loading ? '登录中...' : '登录' }}
          </button>
        </div>

        <!-- SMS form -->
        <div v-else>
          <div class="auth-field">
            <input
              class="input"
              type="tel"
              placeholder="请输入手机号"
              maxlength="11"
              v-model="smsForm.phone"
            />
          </div>
          <div class="auth-field auth-field--sms">
            <input
              class="input sms-input"
              type="text"
              placeholder="请输入验证码"
              maxlength="6"
              v-model="smsForm.code"
              @keyup.enter="handleSmsLogin"
            />
            <button
              class="sms-btn"
              :disabled="smsCooldown > 0 || smsLoading"
              @click="handleSendSms"
            >
              {{ smsCooldown > 0 ? `${smsCooldown}s 后重发` : (smsLoading ? '发送中...' : '获取验证码') }}
            </button>
          </div>
          <button
            class="auth-btn"
            :disabled="loading"
            @click="handleSmsLogin"
          >
            {{ loading ? '登录中...' : '登录' }}
          </button>
        </div>

        <!-- Divider -->
        <div class="auth-divider">
          <span>其他登录方式</span>
        </div>

        <!-- Social icons -->
        <div class="auth-social">
          <button class="social-btn">💬</button>
          <button class="social-btn">D</button>
          <button class="social-btn">K</button>
        </div>
      </div>
    </div>
  </div>
  </div>
</template>

<style scoped>
.auth-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100vw;
}

.auth-bg {
  flex: 1;
  background: radial-gradient(ellipse at top left, #E8F0FF 0%, transparent 60%),
              radial-gradient(ellipse at bottom right, var(--gold-soft) 0%, transparent 60%),
              var(--bg);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  position: relative;
  overflow: hidden;
}

.auth-bg::before {
  content: "";
  position: absolute;
  top: -200px;
  right: -200px;
  width: 500px;
  height: 500px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(43,107,255,.08), transparent 70%);
}

.auth-card {
  display: flex;
  max-width: 960px;
  width: 100%;
  background: var(--card);
  border-radius: 20px;
  box-shadow: 0 30px 80px -30px rgba(26,29,41,.3);
  overflow: hidden;
  position: relative;
  z-index: 1;
}

.auth-left {
  flex: 1;
  padding: 46px 40px;
  background: linear-gradient(135deg, var(--brand) 0%, var(--brand-dark) 70%, var(--brand-deep) 100%);
  color: #fff;
  position: relative;
  overflow: hidden;
  min-width: 0;
}

.auth-left::before {
  content: "";
  position: absolute;
  top: -80px;
  right: -80px;
  width: 260px;
  height: 260px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(233,185,73,.3), transparent 70%);
}

.auth-left::after {
  content: "";
  position: absolute;
  bottom: -120px;
  left: -120px;
  width: 320px;
  height: 320px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255,255,255,.08), transparent 70%);
}

.auth-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 22px;
  position: relative;
}

.auth-logo .logo-mark {
  width: 46px;
  height: 46px;
  border-radius: var(--radius);
  background: linear-gradient(135deg, #fff, var(--gold));
  color: var(--brand);
  display: grid;
  place-items: center;
  font-size: 22px;
  font-weight: 700;
}

.auth-left h1 {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.4;
  margin-bottom: 12px;
  position: relative;
}

.auth-left .sub {
  font-size: 13px;
  opacity: .85;
  line-height: 1.7;
  margin-bottom: 26px;
  position: relative;
}

.auth-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
  position: relative;
}

.auth-tag {
  padding: 6px 12px;
  border-radius: 16px;
  background: rgba(255,255,255,.12);
  backdrop-filter: blur(6px);
  font-size: 11px;
  border: 1px solid rgba(255,255,255,.2);
}

.auth-slogan {
  position: relative;
  font-size: 11px;
  opacity: .7;
  line-height: 1.7;
  border-top: 1px solid rgba(255,255,255,.15);
  padding-top: 18px;
  margin-top: 10px;
}

.auth-right {
  width: 380px;
  padding: 46px 40px;
  flex-shrink: 0;
}

.auth-right h2 {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-1);
  margin-bottom: 6px;
}

.auth-right .hint {
  font-size: 12px;
  color: var(--text-3);
  margin-bottom: 26px;
}

.auth-tabs {
  display: flex;
  gap: 28px;
  margin-bottom: 22px;
  border-bottom: 1px solid var(--line);
}

.auth-tabs--inner {
  margin-top: 4px;
}

.auth-tab {
  padding: 10px 0;
  font-size: 14px;
  color: var(--text-3);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  font-weight: 500;
}

.auth-tab.on {
  color: var(--brand);
  border-bottom-color: var(--brand);
  font-weight: 600;
}

.auth-field {
  margin-bottom: 16px;
}

.auth-field .input {
  width: 100%;
  height: 42px;
  border-radius: var(--radius-lg);
  background: #F7F9FF;
  border: 1px solid var(--line);
  padding: 0 14px;
  font-size: 13px;
  color: var(--text-1);
  outline: none;
  font-family: inherit;
  box-sizing: border-box;
}

.auth-field .input:focus {
  border-color: var(--brand);
  background: var(--card);
  box-shadow: 0 0 0 3px rgba(43,107,255,.1);
}

.auth-field--row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.remember-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-1);
  cursor: pointer;
}

.forgot-link {
  font-size: 13px;
  color: var(--brand);
  text-decoration: none;
}

.forgot-link:hover {
  text-decoration: underline;
}

.auth-field--sms {
  display: flex;
  gap: 8px;
}

.auth-field--sms .sms-input {
  flex: 1;
}

.sms-btn {
  height: 42px;
  padding: 0 14px;
  border-radius: var(--radius-lg);
  background: #F7F9FF;
  border: 1px solid var(--line);
  font-size: 12px;
  color: var(--brand);
  cursor: pointer;
  white-space: nowrap;
  font-family: inherit;
}

.sms-btn:disabled {
  color: var(--text-3);
  cursor: not-allowed;
}

.auth-btn {
  width: 100%;
  height: 44px;
  border-radius: 22px;
  background: linear-gradient(90deg, var(--brand), #5B8BFF);
  color: #fff;
  border: none;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 6px;
  box-shadow: 0 8px 20px -6px rgba(43,107,255,.5);
  font-family: inherit;
}

.auth-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.auth-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 24px 0 16px;
  font-size: 12px;
  color: var(--text-3);
}

.auth-divider::before,
.auth-divider::after {
  content: "";
  flex: 1;
  height: 1px;
  background: var(--line);
}

.auth-social {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.social-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 1px solid var(--line);
  background: var(--card);
  font-size: 16px;
  cursor: pointer;
  display: grid;
  place-items: center;
}

.social-btn:hover {
  border-color: var(--brand);
}
</style>
