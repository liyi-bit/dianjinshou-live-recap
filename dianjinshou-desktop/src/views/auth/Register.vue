<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { useUserStore } from '@/stores/user'
import { sendSms } from '@/api/auth'
import TitleBar from '@/components/TitleBar.vue'

const router = useRouter()
const userStore = useUserStore()

const form = reactive({
  username: '',
  phone: '',
  password: '',
  confirmPassword: '',
  code: ''
})

const loading = ref(false)
const smsLoading = ref(false)
const smsCooldown = ref(0)
let cooldownTimer: ReturnType<typeof setInterval> | null = null

function isValidPhone(phone: string): boolean {
  return /^1[3-9]\d{9}$/.test(phone)
}

function isValidPassword(password: string): boolean {
  return /^(?=.*[A-Za-z])(?=.*\d).{8,32}$/.test(password)
}

async function handleRegister() {
  // Auto-generate username if empty
  if (!form.username.trim()) {
    form.username = '用户' + Math.random().toString(36).substring(2, 8)
  }
  if (!isValidPhone(form.phone)) {
    Message.error('请输入正确的手机号')
    return
  }
  if (!isValidPassword(form.password)) {
    Message.error('密码需8-32位，包含字母和数字')
    return
  }
  if (!form.code) {
    Message.error('请输入验证码')
    return
  }

  loading.value = true
  try {
    await userStore.register({
      username: form.username.trim(),
      phone: form.phone,
      password: form.password,
      code: form.code
    })
    Message.success('注册成功')
  } catch {
    // error already shown by request interceptor
  } finally {
    loading.value = false
  }
}

async function handleSendSms() {
  if (!isValidPhone(form.phone)) {
    Message.error('请输入正确的手机号')
    return
  }
  smsLoading.value = true
  try {
    await sendSms({ phone: form.phone, type: 'register' })
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

function goLogin() {
  router.push('/login')
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
          <div class="logo-mark">金</div>
          <span>点金手</span>
        </div>
        <h1>让每一场直播<br>都能被 AI 点石成金</h1>
        <p class="sub">
          点金手是专为直播电商打造的 AI 复盘平台，<br>
          帮助主播和运营团队从每一场直播中提炼增长洞察。
        </p>
        <div class="auth-tags">
          <span class="auth-tag">🎯 整场复盘</span>
          <span class="auth-tag">✂ 智能切片</span>
          <span class="auth-tag">🤖 AI 助手</span>
          <span class="auth-tag">📊 竞品分析</span>
          <span class="auth-tag">🛡 违规检测</span>
        </div>
        <p class="auth-slogan">
          已帮助超过 10,000+ 场直播完成智能复盘<br>
          平均提升转化率 23%，退货率降低 18%
        </p>
      </div>

      <!-- Right panel -->
      <div class="auth-right">
        <h2>创建账号</h2>
        <p class="hint">几分钟即可开始你的 AI 复盘之旅</p>

        <div class="auth-tabs">
          <div class="auth-tab" @click="goLogin">登录</div>
          <div class="auth-tab on">注册</div>
        </div>

        <!-- Hidden username field to satisfy script validation -->
        <input type="hidden" v-model="form.username" />

        <div class="auth-field">
          <input
            class="input"
            type="tel"
            v-model="form.phone"
            placeholder="请输入手机号"
            maxlength="11"
          />
        </div>

        <div class="auth-field">
          <div class="sms-row">
            <input
              class="input"
              type="text"
              v-model="form.code"
              placeholder="请输入验证码"
              maxlength="6"
            />
            <button
              class="sms-btn"
              :disabled="smsCooldown > 0 || smsLoading"
              @click="handleSendSms"
            >
              {{ smsCooldown > 0 ? `${smsCooldown}s 后重发` : '获取验证码' }}
            </button>
          </div>
        </div>

        <div class="auth-field">
          <input
            class="input"
            type="password"
            v-model="form.password"
            placeholder="设置密码（8-16 位）"
            maxlength="16"
          />
        </div>

        <div class="agreement">
          <input type="checkbox" id="agree" />
          <label for="agree">
            我已阅读并同意
            <span class="link">用户协议</span>
            和
            <span class="link">隐私政策</span>
          </label>
        </div>

        <button class="auth-btn" :disabled="loading" @click="handleRegister">
          {{ loading ? '注册中…' : '立即注册' }}
        </button>

        <p class="register-footer">注册成功后立即可用</p>
      </div>
    </div>
  </div>
  </div>
</template>

<style scoped>
.auth-page { display:flex; flex-direction:column; height:100vh; width:100vw }
.auth-bg { flex:1; background:radial-gradient(ellipse at top left,#E8F0FF 0%,transparent 60%),radial-gradient(ellipse at bottom right,var(--gold-soft) 0%,transparent 60%),var(--bg); display:flex; align-items:center; justify-content:center; padding:40px; position:relative; overflow:hidden }
.auth-bg::before { content:""; position:absolute; top:-200px; right:-200px; width:500px; height:500px; border-radius:50%; background:radial-gradient(circle,rgba(43,107,255,.08),transparent 70%) }
.auth-card { display:flex; max-width:960px; width:100%; background:var(--card); border-radius:20px; box-shadow:0 30px 80px -30px rgba(26,29,41,.3); overflow:hidden; position:relative; z-index:1 }
.auth-left { flex:1; padding:46px 40px; background:linear-gradient(135deg,var(--brand) 0%,var(--brand-dark) 70%,var(--brand-deep) 100%); color:#fff; position:relative; overflow:hidden; min-width:0 }
.auth-left::before { content:""; position:absolute; top:-80px; right:-80px; width:260px; height:260px; border-radius:50%; background:radial-gradient(circle,rgba(233,185,73,.3),transparent 70%) }
.auth-left::after { content:""; position:absolute; bottom:-120px; left:-120px; width:320px; height:320px; border-radius:50%; background:radial-gradient(circle,rgba(255,255,255,.08),transparent 70%) }
.auth-logo { display:flex; align-items:center; gap:12px; font-size:22px; font-weight:700; margin-bottom:22px; position:relative }
.logo-mark { width:46px; height:46px; border-radius:var(--radius); background:linear-gradient(135deg,#fff,var(--gold)); color:var(--brand); display:grid; place-items:center; font-size:22px; font-weight:700 }
.auth-left h1 { font-size:24px; font-weight:700; line-height:1.4; margin-bottom:12px; position:relative }
.auth-left .sub { font-size:13px; opacity:.85; line-height:1.7; margin-bottom:26px; position:relative }
.auth-tags { display:flex; flex-wrap:wrap; gap:8px; margin-bottom:20px; position:relative }
.auth-tag { padding:6px 12px; border-radius:16px; background:rgba(255,255,255,.12); backdrop-filter:blur(6px); font-size:11px; border:1px solid rgba(255,255,255,.2) }
.auth-slogan { position:relative; font-size:11px; opacity:.7; line-height:1.7; border-top:1px solid rgba(255,255,255,.15); padding-top:18px; margin-top:10px }
.auth-right { width:380px; padding:46px 40px; flex-shrink:0 }
.auth-right h2 { font-size:20px; font-weight:700; color:var(--text-1); margin-bottom:6px }
.auth-right .hint { font-size:12px; color:var(--text-3); margin-bottom:26px }
.auth-tabs { display:flex; gap:28px; margin-bottom:22px; border-bottom:1px solid var(--line) }
.auth-tab { padding:10px 0; font-size:14px; color:var(--text-3); cursor:pointer; border-bottom:2px solid transparent; font-weight:500 }
.auth-tab.on { color:var(--brand); border-bottom-color:var(--brand); font-weight:600 }
.auth-field { margin-bottom:16px }
.auth-field .input { width:100%; height:42px; border-radius:var(--radius-lg); background:#F7F9FF; border:1px solid var(--line); padding:0 14px; font-size:13px; color:var(--text-1); outline:none; font-family:inherit }
.auth-field .input:focus { border-color:var(--brand); background:var(--card); box-shadow:0 0 0 3px rgba(43,107,255,.1) }
.auth-btn { width:100%; height:44px; border-radius:22px; background:linear-gradient(90deg,var(--brand),#5B8BFF); color:#fff; border:none; font-size:14px; font-weight:600; cursor:pointer; margin-top:6px; box-shadow:0 8px 20px -6px rgba(43,107,255,.5) }
.auth-btn:disabled { opacity:0.6; cursor:not-allowed }
.sms-row { display:flex; gap:8px }
.sms-row .input { flex:1 }
.sms-btn { height:42px; border-radius:var(--radius-lg); border:1px solid var(--brand); background:var(--card); color:var(--brand); padding:0 14px; font-size:12px; cursor:pointer; white-space:nowrap; font-family:inherit }
.sms-btn:disabled { opacity:0.5; cursor:not-allowed; border-color:var(--line); color:var(--text-3) }
.agreement { font-size:11px; color:var(--text-3); margin-bottom:6px; display:flex; align-items:center; gap:6px }
.agreement input { accent-color:var(--brand) }
.agreement .link { color:var(--brand); cursor:pointer }
.register-footer { text-align:center; margin-top:16px; font-size:12px; color:var(--text-3) }
.register-footer b { color:var(--brand) }
</style>
