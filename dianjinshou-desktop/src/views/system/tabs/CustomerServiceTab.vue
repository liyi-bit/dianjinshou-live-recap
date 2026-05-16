<script setup lang="ts">
import { ref } from 'vue'
import { Message } from '@arco-design/web-vue'
import request from '@/api/request'

const form = ref({
  title: '',
  description: '',
  contact: ''
})
const submitting = ref(false)

async function submitTicket() {
  if (!form.value.title.trim() || !form.value.description.trim()) {
    Message.warning('请填写标题和描述')
    return
  }
  submitting.value = true
  try {
    await request.post('/settings/customer-service-tickets', form.value)
    Message.success('工单已提交')
    form.value = { title: '', description: '', contact: '' }
  } catch {
    Message.error('提交失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="cs-wrap">
    <!-- QR section centered -->
    <div class="qr-section djscard">
      <div class="qr-box">
        <span>📱</span>
      </div>
      <div class="qr-title">扫码添加客服微信</div>
      <div class="qr-sub">微信扫一扫，获得专属客服服务</div>
    </div>

    <!-- Ticket form -->
    <div class="ticket-section djscard">
      <div class="ticket-title">提交工单</div>
      <div class="djsform-row">
        <span class="djsform-label">问题标题</span>
        <input
          class="djsinput djsform-ctrl"
          v-model="form.title"
          placeholder="简要描述您的问题"
          maxlength="200"
        />
      </div>
      <div class="djsform-row align-top">
        <span class="djsform-label">问题描述</span>
        <textarea
          class="djsinput djsform-ctrl"
          v-model="form.description"
          placeholder="详细描述您遇到的问题"
          rows="4"
          style="resize:vertical;"
        ></textarea>
      </div>
      <div class="djsform-row">
        <span class="djsform-label">联系方式</span>
        <input
          class="djsinput djsform-ctrl"
          v-model="form.contact"
          placeholder="手机号/微信号（选填）"
        />
      </div>
      <div style="margin-top:20px;">
        <button class="djsbtn" :disabled="submitting" @click="submitTicket">
          {{ submitting ? '提交中…' : '提交工单' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cs-wrap { display: flex; flex-direction: column; gap: 20px; max-width: 560px; }
.qr-section {
  text-align: center; padding: 40px;
  background: var(--card); border-radius: 14px; border: 1px solid var(--line);
}
.qr-box {
  width: 200px; height: 200px; background: var(--bg); border-radius: 14px;
  display: grid; place-items: center; margin: 0 auto 16px; font-size: 48px;
}
.qr-title { font-size: 16px; font-weight: 700; color: var(--text-1); margin-bottom: 6px; }
.qr-sub { font-size: 13px; color: var(--text-3); }
.ticket-section {
  background: var(--card); border-radius: 14px; border: 1px solid var(--line); padding: 28px 32px;
}
.ticket-title { font-size: 15px; font-weight: 700; margin-bottom: 16px; }
</style>
