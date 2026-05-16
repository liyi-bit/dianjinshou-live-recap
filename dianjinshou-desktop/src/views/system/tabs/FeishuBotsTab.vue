<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Message, Modal } from '@arco-design/web-vue'
import {
  listFeishuBots,
  createFeishuBot,
  deleteFeishuBot,
  type FeishuBotItem,
} from '@/api/feishuBots'
import { formatDateTime } from '@/utils/format'

const bots = ref<FeishuBotItem[]>([])
const loading = ref(false)
const adding = ref(false)

const form = ref({ appId: '', appSecret: '', botName: '' })
const formOpen = ref(false)

async function load() {
  loading.value = true
  try {
    bots.value = (await listFeishuBots()) || []
  } finally {
    loading.value = false
  }
}

function openForm() {
  form.value = { appId: '', appSecret: '', botName: '' }
  formOpen.value = true
}

async function submit() {
  const appId = form.value.appId.trim()
  const appSecret = form.value.appSecret.trim()
  if (!appId || !appSecret) {
    Message.warning('AppId 和 AppSecret 均为必填')
    return
  }
  adding.value = true
  try {
    await createFeishuBot({
      appId,
      appSecret,
      botName: form.value.botName.trim() || undefined,
    })
    Message.success('已添加并启动长连接')
    formOpen.value = false
    await load()
  } catch {
    // request interceptor already toasts
  } finally {
    adding.value = false
  }
}

function confirmDelete(bot: FeishuBotItem) {
  Modal.confirm({
    title: '删除飞书机器人',
    content: `确认删除「${bot.botName || bot.appId}」？将断开其长连接。`,
    okText: '删除',
    cancelText: '取消',
    onOk: async () => {
      try {
        await deleteFeishuBot(bot.id)
        Message.success('已删除')
        await load()
      } catch {
        /* toasted */
      }
    },
  })
}

function statusLabel(b: FeishuBotItem): { text: string; cls: string } {
  if (b.status === 0) return { text: '已禁用', cls: 'st-off' }
  if (b.lastError) return { text: '连接异常', cls: 'st-err' }
  if (b.lastConnectedAt) return { text: '已连接', cls: 'st-ok' }
  return { text: '待连接', cls: 'st-wait' }
}

function fmtTime(ts: string | null): string {
  return formatDateTime(ts)
}

onMounted(load)
</script>

<template>
  <div class="feishu-bots-page">
    <div class="page-head">
      <div>
        <div class="page-title">飞书机器人</div>
        <div class="page-desc">
          在飞书群中 @机器人 发送抖音直播间链接，将自动添加到你的直播间列表。
        </div>
      </div>
      <button class="djsbtn" @click="openForm">+ 新增机器人</button>
    </div>

    <div v-if="loading" class="empty">加载中…</div>
    <div v-else-if="bots.length === 0" class="empty">
      还没有机器人。点击右上角「新增机器人」绑定你在
      <a href="https://open.feishu.cn" target="_blank">飞书开放平台</a>
      创建的应用。
    </div>
    <div v-else class="bot-list">
      <div v-for="b in bots" :key="b.id" class="djscard bot-card">
        <div class="bot-main">
          <div class="bot-name">
            {{ b.botName || b.appId }}
            <span class="bot-status" :class="statusLabel(b).cls">{{ statusLabel(b).text }}</span>
          </div>
          <div class="bot-meta">
            <span>AppId：<code>{{ b.appId }}</code></span>
            <span>最近连接：{{ fmtTime(b.lastConnectedAt) }}</span>
          </div>
          <div v-if="b.lastError" class="bot-error" :title="b.lastError">
            错误：{{ b.lastError }}
          </div>
        </div>
        <div class="bot-actions">
          <button class="djsbtn ghost btn-delete" @click="confirmDelete(b)">删除</button>
        </div>
      </div>
    </div>

    <div class="footnote djscard">
      <div class="note-title">使用说明</div>
      <p>1. 在 <a href="https://open.feishu.cn" target="_blank">飞书开放平台</a> 创建企业自建应用，拿到 AppId / AppSecret。</p>
      <p>2. 在「权限管理」中勾选：<code>im:message.group_at_msg:readonly</code>、<code>im:message:send_as_bot</code>。</p>
      <p>3. 在「事件订阅」中订阅 <code>im.message.receive_v1</code>（群聊接收消息）。<strong>无需配置请求地址</strong>，本系统以长连接方式接收事件。</p>
      <p>4. 发布应用版本，并将机器人拉进目标群聊。</p>
      <p>5. 在本页面填入 AppId / AppSecret 完成绑定。支持同一账号绑定多个机器人。</p>
    </div>

    <a-modal
      v-model:visible="formOpen"
      title="新增飞书机器人"
      :mask-closable="false"
      :unmount-on-close="true"
      :ok-loading="adding"
      :on-before-ok="(async () => { await submit(); return false; }) as any"
      ok-text="保存"
      cancel-text="取消"
    >
      <div class="form-grid">
        <label>AppId <span class="req">*</span></label>
        <input
          class="djsinput"
          v-model="form.appId"
          placeholder="cli_xxxxxxxxxxxxxxxx"
          autocomplete="off"
        />
        <label>AppSecret <span class="req">*</span></label>
        <input
          class="djsinput"
          v-model="form.appSecret"
          placeholder="飞书应用后台生成的 Secret"
          autocomplete="off"
        />
        <label>展示名</label>
        <input
          class="djsinput"
          v-model="form.botName"
          placeholder="方便自己辨认（可选）"
          autocomplete="off"
        />
      </div>
      <div class="form-hint">
        保存后将立刻尝试建立长连接。若 AppSecret 填错，状态会显示「连接异常」。
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
.feishu-bots-page { max-width: 860px; }
.page-head {
  display: flex; justify-content: space-between; align-items: flex-start;
  margin-bottom: 18px;
}
.page-title { font-size: 16px; font-weight: 700; color: var(--text-1); }
.page-desc { margin-top: 4px; color: var(--text-2b); font-size: 13px; }

.empty {
  padding: 40px; text-align: center; color: var(--text-3);
  border: 1px dashed var(--line); border-radius: 10px;
}
.empty a { color: var(--brand, #b8823a); text-decoration: underline; }

.bot-list { display: flex; flex-direction: column; gap: 12px; }
.bot-card {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px;
}
.bot-main { flex: 1; min-width: 0; }
.bot-name {
  font-size: 14.5px; font-weight: 600; color: var(--text-1);
  display: flex; align-items: center; gap: 10px;
}
.bot-status {
  font-size: 11.5px; font-weight: 500; padding: 2px 8px;
  border-radius: 10px; letter-spacing: .02em;
}
.st-ok { background: rgba(75,180,110,.12); color: #2e8a4d; }
.st-err { background: rgba(220,92,92,.12); color: #c24747; }
.st-wait { background: rgba(184,130,58,.12); color: #b8823a; }
.st-off { background: rgba(120,120,120,.12); color: #6b6b6b; }
.bot-meta {
  margin-top: 6px; display: flex; gap: 18px;
  color: var(--text-2b); font-size: 12.5px;
}
.bot-meta code {
  font-family: 'Consolas','Menlo',monospace;
  background: rgba(0,0,0,.04); padding: 1px 6px; border-radius: 4px;
}
.bot-error {
  margin-top: 6px; font-size: 12px; color: #c24747;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 560px;
}
.bot-actions { flex-shrink: 0; }
.djsbtn.ghost.btn-delete { color: #c24747; }
.djsbtn.ghost.btn-delete:hover {
  color: #c24747;
  background: linear-gradient(180deg, #FFF7F7, #FDEEEE);
  box-shadow: var(--sh-in), 0 0 0 1px rgba(194,71,71,.35), 0 2px 6px rgba(194,71,71,.08);
}

.form-grid {
  display: grid; grid-template-columns: 92px 1fr; gap: 12px 14px; align-items: center;
}
.form-grid label { color: var(--text-2); font-size: 13px; }
.form-grid .req { color: #c24747; margin-left: 2px; }
.form-hint {
  margin-top: 14px; font-size: 12.5px; color: var(--text-3); line-height: 1.6;
}

.footnote { padding: 18px 22px; margin-top: 18px; font-size: 12.5px; line-height: 1.85; color: var(--text-2b); }
.footnote .note-title { font-weight: 700; color: var(--text-1); margin-bottom: 6px; font-size: 13.5px; }
.footnote p { margin: 0; }
.footnote code {
  font-family: 'Consolas','Menlo',monospace;
  background: rgba(0,0,0,.04); padding: 1px 6px; border-radius: 4px;
}
.footnote a { color: var(--brand, #b8823a); text-decoration: underline; }
</style>
