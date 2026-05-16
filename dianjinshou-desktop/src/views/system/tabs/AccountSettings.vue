<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import { useAppStore } from '@/stores/app'
import { storeToRefs } from 'pinia'
import request from '@/api/request'

const appStore = useAppStore()
const { username, phone, avatarUrl } = storeToRefs(appStore)

const showPasswordModal = ref(false)
const showPhoneModal = ref(false)
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const newPhone = ref('')
const phoneCode = ref('')

async function fetchAccount() {
  try {
    const res = await request.get('/settings/account')
    const data = (res as any).data ?? res
    if (data.username) username.value = data.username
    if (data.phone) phone.value = data.phone
    if (data.avatarUrl) avatarUrl.value = data.avatarUrl
  } catch {
    // API unavailable, use locally persisted values
  }
}

async function saveUsername() {
  if (!username.value.trim()) {
    Message.warning('昵称不能为空')
    return
  }
  try {
    await request.put('/settings/account', { username: username.value })
    Message.success('昵称已更新')
  } catch {
    // Save locally even if API fails
    Message.success('昵称已保存（本地）')
  }
}

async function changePassword() {
  if (newPassword.value !== confirmPassword.value) {
    Message.warning('两次密码不一致')
    return
  }
  try {
    await request.put('/settings/account', {
      oldPassword: oldPassword.value,
      newPassword: newPassword.value
    })
    Message.success('密码已修改')
    showPasswordModal.value = false
    oldPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
  } catch {
    Message.error('密码修改失败')
  }
}

async function changeAvatar() {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = async () => {
    const file = input.files?.[0]
    if (!file) return
    if (file.size > 2 * 1024 * 1024) {
      Message.warning('头像文件不能超过2MB')
      return
    }
    const formData = new FormData()
    formData.append('file', file)
    try {
      const res = await request.post('/settings/avatar', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      const url = (res as any).data?.url ?? (res as any).data ?? ''
      if (url) {
        avatarUrl.value = url
        Message.success('头像已更新')
      }
    } catch {
      Message.error('头像上传失败')
    }
  }
  input.click()
}

async function changePhone() {
  if (!newPhone.value.trim()) {
    Message.warning('请输入新手机号')
    return
  }
  try {
    await request.put('/settings/account', { phone: newPhone.value })
    phone.value = newPhone.value
    Message.success('手机号已更新')
    showPhoneModal.value = false
    newPhone.value = ''
    phoneCode.value = ''
  } catch {
    Message.error('手机号更换失败')
  }
}

onMounted(fetchAccount)
</script>

<template>
  <div class="form-card djscard">
    <!-- Avatar -->
    <div class="djsform-row">
      <span class="djsform-label">头像</span>
      <div class="djsform-ctrl">
        <div class="avatar-upload">
          <div class="avatar-circle">{{ (username || '?').charAt(0) }}</div>
          <button class="djsbtn ghost" style="font-size:12px;" @click="changeAvatar">更换头像</button>
        </div>
      </div>
    </div>
    <!-- 昵称 -->
    <div class="djsform-row">
      <span class="djsform-label">昵称</span>
      <div class="djsform-ctrl" style="display:flex;gap:8px;">
        <input class="djsinput" v-model="username" placeholder="请输入昵称" style="width:220px;" />
        <button class="djsbtn" @click="saveUsername">保存</button>
      </div>
    </div>
    <!-- 手机号 -->
    <div class="djsform-row">
      <span class="djsform-label">手机号码</span>
      <div class="djsform-ctrl" style="display:flex;gap:12px;">
        <span style="font-size:14px;color:var(--text-1);">{{ phone || '未绑定' }}</span>
        <a class="djslink" href="#" @click.prevent="showPhoneModal = true">更换</a>
      </div>
    </div>
    <!-- 登录密码 -->
    <div class="djsform-row">
      <span class="djsform-label">登录密码</span>
      <div class="djsform-ctrl" style="display:flex;gap:12px;">
        <span style="font-size:18px;letter-spacing:4px;color:var(--text-1);">••••••••</span>
        <a class="djslink" href="#" @click.prevent="showPasswordModal = true">修改</a>
      </div>
    </div>
    <!-- 当前版本 -->
    <div class="djsform-row">
      <span class="djsform-label">当前版本</span>
      <div class="djsform-ctrl">
        <span style="font-size:14px;color:var(--text-2b);">{{ appStore.appVersion }}</span>
      </div>
    </div>
    <!-- 有效期 -->
    <div class="djsform-row">
      <span class="djsform-label">有效期</span>
      <div class="djsform-ctrl">
        <span style="font-size:14px;color:var(--text-2b);">长期有效</span>
      </div>
    </div>
    <!-- 资源更新时间 -->
    <div class="djsform-row">
      <span class="djsform-label">资源更新时间</span>
      <div class="djsform-ctrl">
        <span style="font-size:14px;color:var(--text-2b);">2024-01-01</span>
      </div>
    </div>

    <!-- Phone Modal -->
    <div v-if="showPhoneModal" class="modal-overlay" @click.self="showPhoneModal = false">
      <div class="modal-box">
        <div class="modal-title">更换手机号</div>
        <div class="djsform-row">
          <span class="djsform-label">新手机号</span>
          <input class="djsinput djsform-ctrl" v-model="newPhone" placeholder="请输入新手机号" />
        </div>
        <div style="margin-top:20px;display:flex;gap:10px;justify-content:flex-end;">
          <button class="djsbtn ghost" @click="showPhoneModal = false">取消</button>
          <button class="djsbtn" @click="changePhone">确认更换</button>
        </div>
      </div>
    </div>

    <!-- Password Modal -->
    <div v-if="showPasswordModal" class="modal-overlay" @click.self="showPasswordModal = false">
      <div class="modal-box">
        <div class="modal-title">修改密码</div>
        <div class="djsform-row">
          <span class="djsform-label">原密码</span>
          <input class="djsinput djsform-ctrl" type="password" v-model="oldPassword" />
        </div>
        <div class="djsform-row">
          <span class="djsform-label">新密码</span>
          <input class="djsinput djsform-ctrl" type="password" v-model="newPassword" />
        </div>
        <div class="djsform-row">
          <span class="djsform-label">确认新密码</span>
          <input class="djsinput djsform-ctrl" type="password" v-model="confirmPassword" />
        </div>
        <div style="margin-top:20px;display:flex;gap:10px;justify-content:flex-end;">
          <button class="djsbtn ghost" @click="showPasswordModal = false">取消</button>
          <button class="djsbtn" @click="changePassword">确认修改</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.form-card { background: var(--card); border-radius: 14px; border: 1px solid var(--line); padding: 28px 32px; max-width: 700px; }
.avatar-upload { display: flex; align-items: center; gap: 14px; }
.avatar-circle {
  width: 56px; height: 56px; border-radius: 50%;
  background: linear-gradient(135deg, var(--brand), var(--brand-light));
  color: #fff; font-size: 22px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
}
.modal-overlay {
  position: fixed; inset: 0; background: rgba(36,30,24,.45);
  display: flex; align-items: center; justify-content: center; z-index: 9999;
}
.modal-box {
  background: var(--card); border-radius: 14px; padding: 28px 32px; min-width: 380px;
  box-shadow: 0 8px 40px -8px rgba(36,30,24,.22);
}
.modal-title { font-size: 16px; font-weight: 700; margin-bottom: 20px; }
</style>
