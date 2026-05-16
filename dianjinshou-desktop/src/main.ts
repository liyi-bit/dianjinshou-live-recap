import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ArcoVue from '@arco-design/web-vue'
import '@arco-design/web-vue/dist/arco.css'
import './styles/design-system.css'
import './styles/components.css'
import './styles/arco-overrides.css'
import App from './App.vue'
import router from './router'
import './assets/styles/variables.scss'
import { useUserStore } from './stores/user'
import { initErrorReporter } from '@/utils/error-reporter'

const app = createApp(App)

const pinia = createPinia()
app.use(pinia)
app.use(router)
app.use(ArcoVue)

app.mount('#app')

// 错误上报（window.onerror / unhandledrejection / Vue errorHandler / 面包屑）
initErrorReporter(app, router)

// 主进程自己 refresh access token 成功后推回来，渲染进程更新 store + localStorage，
// 保持两端 token 一致；app 重启后也能从 localStorage 读到最新的。
// 这里不走 userStore.setTokens（避免再反向 IPC 回主进程），直接内联写 state + localStorage
const api = (window as any).electronAPI
if (api?.onAuthTokenRefreshed) {
  api.onAuthTokenRefreshed((payload: { accessToken: string; refreshToken: string }) => {
    if (!payload?.accessToken || !payload?.refreshToken) return
    const userStore = useUserStore()
    userStore.accessToken = payload.accessToken
    userStore.refreshToken = payload.refreshToken
    localStorage.setItem('accessToken', payload.accessToken)
    localStorage.setItem('refreshToken', payload.refreshToken)
  })
}

// 启动时如果本地有 token，立即把两个 token 同步给主进程（否则主进程在用户打开 app 后、
// 还没做第一次需要登录的操作之前，不知道 refresh token，遇到 401 会失败）
const userStore = useUserStore()
if (userStore.accessToken && userStore.refreshToken && api?.setAuthTokens) {
  api.setAuthTokens(userStore.accessToken, userStore.refreshToken).catch(() => {})
}
