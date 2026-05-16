<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { Message } from '@arco-design/web-vue';
import { adminLogin } from '../api/adminAuth';
import { useAuthStore } from '../stores/auth';

const auth = useAuthStore();
const router = useRouter();
const loading = ref(false);

const form = reactive({
  username: '',
  password: ''
});

async function handleLogin() {
  if (!form.username || !form.password) {
    Message.warning('请输入用户名和密码');
    return;
  }
  loading.value = true;
  try {
    const result = await adminLogin(form.username, form.password);
    auth.setAuth(result.accessToken, result.user);
    Message.success('登录成功');
    router.push('/users');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-bg">
      <div class="auth-card">
        <!-- Left panel -->
        <div class="auth-left">
          <div class="auth-logo">
            <span class="logo-mark">金</span>
            <span>点金手后台</span>
          </div>
          <h1>掌握每一条<br />业务运行的脉搏</h1>
          <p class="sub">集中查看所有业务用户、录制文件与 AI 分析任务的处理情况。仅限授权管理员登录。</p>
          <div class="auth-tags">
            <span class="auth-tag">👥 用户</span>
            <span class="auth-tag">🎥 录制</span>
            <span class="auth-tag">🤖 AI 任务</span>
            <span class="auth-tag">📊 全局统计</span>
            <span class="auth-tag">🛡 只读视图</span>
          </div>
          <p class="auth-slogan">所有操作受权限控制 · 所有访问将被记录 · 管理员只读模式 ·</p>
        </div>

        <!-- Right panel -->
        <div class="auth-right">
          <h2>欢迎回来</h2>
          <p class="hint">请登录你的管理员账号</p>

          <div class="auth-tabs">
            <span class="auth-tab on">登录</span>
          </div>

          <form @submit.prevent="handleLogin">
            <div class="auth-field">
              <input
                class="input"
                type="text"
                placeholder="请输入用户名"
                maxlength="64"
                v-model="form.username"
                autocomplete="username"
              />
            </div>
            <div class="auth-field">
              <input
                class="input"
                type="password"
                placeholder="请输入密码"
                maxlength="64"
                v-model="form.password"
                autocomplete="current-password"
                @keyup.enter="handleLogin"
              />
            </div>
            <button
              type="submit"
              class="auth-btn"
              :disabled="loading"
            >
              {{ loading ? '登录中...' : '登录' }}
            </button>
          </form>

          <p class="foot-note">仅限授权管理员 · 如需账号请联系管理员</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page { display:flex; flex-direction:column; height:100vh; width:100vw }

.auth-bg {
  flex:1;
  background:
    radial-gradient(ellipse at top left, rgba(184,130,58,.08) 0%, transparent 60%),
    radial-gradient(ellipse at bottom right, var(--gold-soft) 0%, transparent 60%),
    var(--bg);
  display:flex; align-items:center; justify-content:center;
  padding:40px; position:relative; overflow:hidden;
}
.auth-bg::before {
  content:""; position:absolute; top:-200px; right:-200px;
  width:500px; height:500px; border-radius:50%;
  background: radial-gradient(circle, rgba(184,130,58,.1), transparent 70%);
}
.auth-bg::after {
  content:""; position:absolute; bottom:-100px; left:-100px;
  width:420px; height:420px; border-radius:50%;
  background: radial-gradient(circle, rgba(233,185,73,.1), transparent 70%);
}

.auth-card {
  display:flex; width:880px; min-height:540px;
  border-radius:var(--radius-xl); overflow:hidden;
  background:var(--card);
  box-shadow: var(--sh-3);
  position:relative; z-index:1;
}

.auth-left {
  flex:1;
  padding:46px 40px;
  background:linear-gradient(135deg, var(--brand) 0%, var(--brand-dark) 70%, var(--brand-deep) 100%);
  color:#fff;
  position:relative; overflow:hidden; min-width:0;
}
.auth-left::before {
  content:""; position:absolute; top:-80px; right:-80px;
  width:260px; height:260px; border-radius:50%;
  background: radial-gradient(circle, rgba(233,185,73,.3), transparent 70%);
}
.auth-left::after {
  content:""; position:absolute; bottom:-120px; left:-120px;
  width:320px; height:320px; border-radius:50%;
  background: radial-gradient(circle, rgba(255,255,255,.08), transparent 70%);
}

.auth-logo {
  display:flex; align-items:center; gap:12px;
  font-size:22px; font-weight:700;
  margin-bottom:22px; position:relative;
}
.auth-logo .logo-mark {
  width:46px; height:46px; border-radius:var(--radius);
  background:linear-gradient(135deg, #fff, var(--gold));
  color:var(--brand);
  display:grid; place-items:center;
  font-size:22px; font-weight:700;
  font-family:var(--fd); font-style:italic;
}

.auth-left h1 {
  font-size:26px; font-weight:700; line-height:1.4;
  margin-bottom:12px; position:relative;
  font-family:var(--fd); letter-spacing:-.01em;
}

.auth-left .sub {
  font-size:13px; opacity:.85; line-height:1.7;
  margin-bottom:26px; position:relative;
}

.auth-tags {
  display:flex; flex-wrap:wrap; gap:8px;
  margin-bottom:20px; position:relative;
}
.auth-tag {
  padding:6px 12px; border-radius:16px;
  background:rgba(255,255,255,.12);
  backdrop-filter:blur(6px);
  font-size:11px; border:1px solid rgba(255,255,255,.2);
}

.auth-slogan {
  position:relative;
  font-size:11px; opacity:.7; line-height:1.7;
  border-top:1px solid rgba(255,255,255,.15);
  padding-top:18px; margin-top:10px;
}

.auth-right {
  width:380px; padding:46px 40px; flex-shrink:0;
  display:flex; flex-direction:column;
}
.auth-right h2 {
  font-size:22px; font-weight:700; color:var(--text-1); margin-bottom:6px;
  font-family:var(--fd); letter-spacing:-.01em;
}
.auth-right .hint { font-size:12px; color:var(--text-3); margin-bottom:26px }

.auth-tabs {
  display:flex; gap:28px; margin-bottom:22px;
  border-bottom:1px solid var(--line);
}
.auth-tab {
  padding:10px 0; font-size:14px; color:var(--text-3);
  cursor:pointer; border-bottom:2px solid transparent; font-weight:500;
}
.auth-tab.on {
  color:var(--brand); border-bottom-color:var(--brand); font-weight:600;
}

.auth-field { margin-bottom:16px }
.auth-field .input {
  width:100%; height:42px; border-radius:var(--radius-lg);
  background:var(--bg-2); border:1px solid var(--line);
  padding:0 14px; font-size:13px; color:var(--text-1);
  outline:none; font-family:inherit; box-sizing:border-box;
  transition:all .18s var(--ease);
}
.auth-field .input:focus {
  border-color:var(--brand); background:var(--card);
  box-shadow:0 0 0 3px var(--brand-soft);
}

.auth-btn {
  width:100%; height:44px; border-radius:var(--radius-pill);
  background:linear-gradient(135deg, var(--brand-light) 0%, var(--brand) 100%);
  color:#fff; border:none; font-size:14px; font-weight:600;
  cursor:pointer; margin-top:6px;
  box-shadow: 0 8px 20px -6px rgba(184,130,58,.5), inset 0 1px 0 rgba(255,255,255,.3);
  font-family:inherit;
  transition:all .2s var(--ease);
}
.auth-btn:hover:not(:disabled) {
  box-shadow: 0 10px 22px -4px rgba(184,130,58,.55), inset 0 1px 0 rgba(255,255,255,.35);
  transform:translateY(-1px);
}
.auth-btn:disabled { opacity:0.7; cursor:not-allowed }

.foot-note {
  margin-top:auto; padding-top:20px;
  font-size:11px; color:var(--text-4);
  text-align:center; letter-spacing:.02em;
}
</style>
