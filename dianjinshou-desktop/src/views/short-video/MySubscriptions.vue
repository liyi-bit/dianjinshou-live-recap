<template>
  <div class="my-subscriptions">
    <!-- Toolbar -->
    <div class="djstoolbar">
      <span class="lbl">订阅达人</span>
      <div class="spacer"></div>
      <button class="djsbtn ghost" @click="loadSubscriptions">刷新</button>
    </div>

    <!-- Empty state -->
    <div v-if="!creators.length" class="djsempty">
      <div class="ic">👥</div>
      <div class="ti">暂无订阅达人</div>
      <p>在「搜达人」页找到感兴趣的达人并订阅</p>
    </div>

    <!-- Subscription table -->
    <div v-else class="djscard">
      <table class="djstbl">
        <thead>
          <tr>
            <th>#</th>
            <th>达人 ID</th>
            <th>订阅时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(sub, idx) in creators" :key="sub.id">
            <td>{{ idx + 1 }}</td>
            <td>{{ sub.creatorId }}</td>
            <td>{{ formatDateTime(sub.createdAt) }}</td>
            <td>
              <span
                class="djslink"
                style="color:#E0341E"
                @click="confirmCancel(sub)"
              >取消订阅</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Confirm overlay -->
    <div v-if="cancelTarget" class="ms-confirm-mask" @click.self="cancelTarget = null">
      <div class="ms-confirm">
        <div class="ms-confirm__title">取消订阅</div>
        <p class="ms-confirm__body">确认取消对达人 <b>{{ cancelTarget.creatorId }}</b> 的订阅？</p>
        <div class="ms-confirm__foot">
          <button class="djsbtn ghost" @click="cancelTarget = null">取消</button>
          <button class="djsbtn danger" @click="handleCancel(cancelTarget.id, 'creator')">确认取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listSubscriptions, cancelSubscription } from '@/api/shortVideo'
import { Message } from '@arco-design/web-vue'
import { formatDateTime } from '@/utils/format'

const creators = ref<any[]>([])
const cancelTarget = ref<any | null>(null)

onMounted(() => {
  loadSubscriptions()
})

async function loadSubscriptions() {
  try {
    const res = await listSubscriptions()
    const data = (res as any).data ?? res
    creators.value = data.creators || []
  } catch {
    creators.value = []
  }
}

function confirmCancel(sub: any) {
  cancelTarget.value = sub
}

async function handleCancel(id: number, type: string) {
  try {
    await cancelSubscription(id, type)
    Message.success('已取消订阅')
    cancelTarget.value = null
    loadSubscriptions()
  } catch {
    Message.error('操作失败')
  }
}
</script>

<style scoped>
.my-subscriptions { padding: 0 24px; }

/* confirm modal */
.ms-confirm-mask {
  position: fixed; inset: 0; background: rgba(26,29,41,.35);
  z-index: 1000; display: flex; align-items: center; justify-content: center;
}
.ms-confirm {
  background: var(--card); border-radius: 14px; padding: 28px 30px;
  min-width: 320px; box-shadow: 0 20px 50px -16px rgba(36,30,24,.25);
}
.ms-confirm__title { font-size: 16px; font-weight: 600; margin-bottom: 12px; }
.ms-confirm__body { font-size: 13px; color: var(--text-2); margin-bottom: 22px; }
.ms-confirm__foot { display: flex; justify-content: flex-end; gap: 10px; }
</style>
