<template>
  <div class="trending-rules">
    <!-- Create rule toolbar -->
    <div class="djstoolbar" style="margin-bottom:16px">
      <span class="lbl">新建爆款规则</span>
      <span class="div"></span>
      <span class="lbl" style="font-weight:400">平台</span>
      <select class="djsselect" style="width:110px" v-model="form.platform">
        <option value="douyin">抖音</option>
        <option value="kuaishou">快手</option>
        <option value="shipinhao">视频号</option>
      </select>
      <span class="lbl" style="font-weight:400">行业</span>
      <input class="djsinput" style="width:120px" v-model="form.industry" placeholder="行业（可选）" />
      <span class="lbl" style="font-weight:400">最低播放量</span>
      <input
        class="djsinput"
        style="width:130px"
        type="number"
        v-model.number="form.minPlayCount"
        placeholder="≥100000"
        min="100000"
        step="100000"
      />
      <span class="lbl" style="font-weight:400">关键词</span>
      <input class="djsinput" style="width:120px" v-model="form.keywords" placeholder="可选" />
      <div class="spacer"></div>
      <button class="djsbtn primary" :disabled="creating" @click="handleCreate">
        {{ creating ? '创建中…' : '创建规则' }}
      </button>
    </div>

    <!-- Rules list header -->
    <div class="djstoolbar" style="margin-bottom:0;border-bottom:none;border-radius:var(--radius) var(--radius) 0 0">
      <span class="lbl">已有规则</span>
      <div class="spacer"></div>
      <button class="djsbtn ghost" style="height:28px;padding:0 12px;font-size:12px" @click="loadRules">刷新</button>
    </div>

    <!-- Empty state -->
    <div v-if="!rules.length" class="djsempty djscard" style="border-top:none;border-radius:0 0 var(--radius) var(--radius)">
      <div class="ic">🔥</div>
      <div class="ti">暂无爆款规则</div>
      <p>配置规则后系统将自动推送符合条件的爆款视频</p>
    </div>

    <!-- Rules table -->
    <div v-else class="djscard" style="border-top:none;border-radius:0 0 var(--radius) var(--radius)">
      <table class="djstbl">
        <thead>
          <tr>
            <th>#</th>
            <th>平台</th>
            <th>行业</th>
            <th>最低播放量</th>
            <th>关键词</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(rule, idx) in rules" :key="rule.id">
            <td>{{ idx + 1 }}</td>
            <td>
              <span class="djschip" style="padding:2px 8px;font-size:11px">{{ platformLabel(rule.platform) }}</span>
            </td>
            <td>{{ rule.industry || '-' }}</td>
            <td>{{ formatCount(rule.minPlayCount) }}</td>
            <td>{{ rule.keywords || '-' }}</td>
            <td>{{ formatDateTime(rule.createdAt) }}</td>
            <td>
              <span class="djslink" style="color:#E0341E" @click="confirmDelete(rule)">删除</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Delete confirm -->
    <div v-if="deleteTarget" class="tr-confirm-mask" @click.self="deleteTarget = null">
      <div class="tr-confirm">
        <div class="tr-confirm__title">删除规则</div>
        <p class="tr-confirm__body">确认删除该爆款规则？删除后将停止推送相关视频。</p>
        <div class="tr-confirm__foot">
          <button class="djsbtn ghost" @click="deleteTarget = null">取消</button>
          <button class="djsbtn danger" @click="handleDelete(deleteTarget.id)">确认删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { subscribeTrending, listSubscriptions, cancelSubscription } from '@/api/shortVideo'
import { Message } from '@arco-design/web-vue'
import { formatDateTime } from '@/utils/format'

const rules = ref<any[]>([])
const creating = ref(false)
const deleteTarget = ref<any | null>(null)

const form = reactive({
  platform: 'douyin',
  industry: '',
  minPlayCount: 100000,
  keywords: ''
})

onMounted(() => {
  loadRules()
})

async function loadRules() {
  try {
    const res = await listSubscriptions()
    const data = (res as any).data ?? res
    rules.value = data.trending || []
  } catch {
    rules.value = []
  }
}

async function handleCreate() {
  creating.value = true
  try {
    await subscribeTrending({
      platform: form.platform,
      industry: form.industry || undefined,
      minPlayCount: form.minPlayCount,
      keywords: form.keywords || undefined
    })
    Message.success('规则创建成功')
    form.industry = ''
    form.keywords = ''
    loadRules()
  } catch {
    Message.error('创建失败')
  } finally {
    creating.value = false
  }
}

function confirmDelete(rule: any) {
  deleteTarget.value = rule
}

async function handleDelete(id: number) {
  try {
    await cancelSubscription(id, 'trending')
    Message.success('已删除')
    deleteTarget.value = null
    loadRules()
  } catch {
    Message.error('操作失败')
  }
}

function formatCount(n: number): string {
  if (!n) return '0'
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return String(n)
}

function platformLabel(p: string): string {
  const map: Record<string, string> = { douyin: '抖音', kuaishou: '快手', shipinhao: '视频号' }
  return map[p] || p
}
</script>

<style scoped>
.trending-rules { padding: 0 24px; }

/* confirm modal */
.tr-confirm-mask {
  position: fixed; inset: 0; background: rgba(26,29,41,.35);
  z-index: 1000; display: flex; align-items: center; justify-content: center;
}
.tr-confirm {
  background: var(--card); border-radius: 14px; padding: 28px 30px;
  min-width: 320px; box-shadow: 0 20px 50px -16px rgba(36,30,24,.25);
}
.tr-confirm__title { font-size: 16px; font-weight: 600; margin-bottom: 12px; }
.tr-confirm__body { font-size: 13px; color: var(--text-2); margin-bottom: 22px; }
.tr-confirm__foot { display: flex; justify-content: flex-end; gap: 10px; }
</style>
