<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { Message } from '@arco-design/web-vue'
import { getAdminConfig, updateAdminConfig, getAdminQuota, type ThirdPartyGroup, type QuotaStatus } from '@/api/admin'

const groups = ref<ThirdPartyGroup[]>([])
const quota = ref<QuotaStatus | null>(null)
const loading = ref(false)
const saving = ref(false)
const revealed = ref<Set<string>>(new Set())
const dirty = ref<Map<string, string>>(new Map())

const quotaPercent = computed(() => {
  if (!quota.value || quota.value.limit === 0) return 0
  return Math.min(100, Math.round((quota.value.used / quota.value.limit) * 100))
})
const quotaExhausted = computed(() =>
  !!quota.value && !quota.value.hasOwnConfig && quota.value.used >= quota.value.limit
)

async function load() {
  loading.value = true
  try {
    const [cfg, q] = await Promise.all([
      getAdminConfig(),
      getAdminQuota().catch(() => null),
    ])
    groups.value = cfg?.groups || []
    quota.value = q
    dirty.value = new Map()
    revealed.value = new Set()
  } catch (err: any) {
    // 错误提示由全局 axios 拦截器显示
  } finally {
    loading.value = false
  }
}

function onInput(key: string, value: string) {
  dirty.value.set(key, value)
}

function onReveal(key: string) {
  // First click clears the masked placeholder so the user can type the new secret
  if (!revealed.value.has(key)) {
    revealed.value.add(key)
    dirty.value.set(key, '')
  }
}

function getDisplayValue(field: { key: string; value: string }) {
  if (dirty.value.has(field.key)) return dirty.value.get(field.key) ?? ''
  return field.value
}

function isReadonly(field: { key: string; type: string }) {
  return field.type === 'secret' && !revealed.value.has(field.key)
}

async function save() {
  if (dirty.value.size === 0) {
    Message.info('没有改动')
    return
  }
  saving.value = true
  try {
    const payload: Record<string, string> = {}
    dirty.value.forEach((v, k) => { payload[k] = v })
    await updateAdminConfig(payload)
    Message.success(`已保存 ${dirty.value.size} 项配置`)
    await load()
  } catch (err: any) {
    // request.ts already shows toast for non-401
  } finally {
    saving.value = false
  }
}

function reset() {
  dirty.value = new Map()
  revealed.value = new Set()
  Message.info('已撤销未保存的改动')
}

onMounted(load)
</script>

<template>
  <div class="third-party-page">
    <div v-if="loading" style="padding:60px;text-align:center;color:var(--text-3);">加载中…</div>
    <template v-else>
      <div v-if="quota && !quota.hasOwnConfig" class="quota-card" :class="{ exhausted: quotaExhausted }">
        <div class="quota-title">
          <span v-if="quotaExhausted" style="color:#d64545">⚠ 免费额度已用完</span>
          <span v-else>免费 AI 解析额度</span>
        </div>
        <div class="quota-bar">
          <div class="quota-bar-fill" :style="{ width: quotaPercent + '%' }"></div>
        </div>
        <div class="quota-text">
          已用 <strong>{{ quota.used }}</strong> / {{ quota.limit }} 个视频
          <span v-if="quotaExhausted">— 需要配置下方的云雾 API Key 才能继续使用</span>
          <span v-else>— 用完后需要配置下方的云雾 API Key</span>
        </div>
      </div>
      <div v-else-if="quota && quota.hasOwnConfig" class="quota-card configured">
        <div class="quota-title">✓ 已配置自己的密钥</div>
        <div class="quota-text">您使用自己的云雾 API Key，不受免费额度限制。</div>
      </div>

      <div v-for="g in groups.filter((item) => item.key !== 'asr')" :key="g.key" class="djscard group-card">
        <div class="group-title">{{ g.label }}</div>
        <div v-for="f in g.fields" :key="f.key" class="djsform-row">
          <span class="djsform-label">{{ f.label }}</span>
          <div class="djsform-ctrl" style="display:flex;gap:8px;align-items:center;">
            <input
              class="djsinput"
              style="flex:1;max-width:480px;"
              :value="getDisplayValue(f)"
              :placeholder="f.placeholder || f.label"
              :readonly="isReadonly(f)"
              @input="onInput(f.key, ($event.target as HTMLInputElement).value)"
            />
            <button
              v-if="f.type === 'secret' && !revealed.has(f.key)"
              class="djsbtn ghost"
              style="font-size:12px;"
              @click="onReveal(f.key)"
            >修改</button>
            <span class="key-hint">{{ f.key }}</span>
          </div>
        </div>
      </div>

      <div class="actions">
        <button class="djsbtn ghost" :disabled="dirty.size === 0" @click="reset">撤销</button>
        <button class="djsbtn" :disabled="saving || dirty.size === 0" @click="save">
          {{ saving ? '保存中…' : `保存（${dirty.size} 项）` }}
        </button>
      </div>
      <div class="footnote">
        <p>● <strong>每个账号独立配置</strong>：这里保存的是 <strong>当前登录账号</strong> 的密钥，其他账号互不可见。</p>
        <p>● <strong>免费额度</strong>：不配置密钥可使用系统默认密钥，但最多只能完成 <strong>5 个视频</strong> 的 AI 解析，超过后必须配置自己的密钥才能继续使用。</p>
        <p>● <strong>配置自己的密钥</strong>：不受 5 次额度限制，AI 费用由您自己的云雾账号承担。</p>
        <p>● <strong>密钥字段默认掩码</strong>：需要修改时点【修改】会清空字段，再录入新值。</p>
        <p>● <strong>修改立即生效</strong>，下次调用即使用新值。</p>
      </div>
    </template>
  </div>
</template>

<style scoped>
.third-party-page { max-width: 860px; }
.group-card { padding: 24px 28px; margin-bottom: 18px; }
.group-title {
  font-size: 14.5px; font-weight: 700; color: var(--text-1);
  padding-bottom: 14px; margin-bottom: 18px;
  border-bottom: 1px solid var(--line);
}
.key-hint { color: var(--text-3); font-size: 11px; font-family: 'Consolas','Menlo',monospace; }
.actions {
  display: flex; gap: 10px; justify-content: flex-end;
  position: sticky; bottom: 0; padding: 16px 0;
  background: linear-gradient(180deg, transparent 0%, var(--bg) 30%);
}
.footnote {
  margin-top: 18px; padding: 14px 18px;
  background: rgba(184,130,58,.06); border-radius: 8px;
  color: var(--text-2b); font-size: 12.5px; line-height: 1.7;
}
.footnote p { margin: 0 0 4px; }
.footnote p:last-child { margin-bottom: 0; }

.quota-card {
  padding: 14px 20px 16px; margin-bottom: 18px;
  background: rgba(184, 130, 58, .08);
  border: 1px solid rgba(184, 130, 58, .28);
  border-radius: 8px;
}
.quota-card.exhausted {
  background: rgba(214, 69, 69, .08);
  border-color: rgba(214, 69, 69, .35);
}
.quota-card.configured {
  background: rgba(62, 143, 87, .08);
  border-color: rgba(62, 143, 87, .3);
}
.quota-title { font-weight: 700; font-size: 13.5px; margin-bottom: 8px; }
.quota-bar {
  height: 8px; background: rgba(0,0,0,.06); border-radius: 4px;
  overflow: hidden; margin-bottom: 8px;
}
.quota-bar-fill {
  height: 100%; background: linear-gradient(90deg, #d9a84b, #b8863c);
  transition: width .3s;
}
.quota-card.exhausted .quota-bar-fill { background: #d64545; }
.quota-text { font-size: 12.5px; color: var(--text-2b); }
</style>
