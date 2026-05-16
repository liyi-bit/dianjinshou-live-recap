<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listMyShares, cancelShare, type ShareLinkItem } from '@/api/share'
import { Message, Modal } from '@arco-design/web-vue'

const shares = ref<ShareLinkItem[]>([])
const loading = ref(false)

const columns = [
  { title: '文件名', dataIndex: 'fileName', ellipsis: true },
  { title: '分享码', dataIndex: 'shareCode', width: 100 },
  { title: '浏览', dataIndex: 'viewCount', width: 70 },
  { title: '下载', dataIndex: 'downloadCount', width: 70 },
  { title: '状态', dataIndex: 'status', width: 90 },
  { title: '过期时间', dataIndex: 'expiresAt', width: 170 },
  { title: '操作', slotName: 'action', width: 100 }
]

async function fetchShares() {
  loading.value = true
  try {
    const res = await listMyShares()
    shares.value = (res as any).data ?? res
    if (!Array.isArray(shares.value)) shares.value = []
  } finally {
    loading.value = false
  }
}

function handleCancel(id: number) {
  Modal.confirm({
    title: '取消分享',
    content: '确定取消此分享链接？',
    async onOk() {
      await cancelShare(id)
      Message.success('已取消分享')
      fetchShares()
    }
  })
}

function getStatusTag(status: string) {
  if (status === 'active') return { color: 'green', text: '有效' }
  if (status === 'expired') return { color: 'gray', text: '已过期' }
  return { color: 'red', text: '已禁用' }
}

onMounted(fetchShares)
</script>

<template>
  <div class="my-shares">
    <h4 style="margin-bottom: 16px">我的分享</h4>
    <a-table
      :columns="columns"
      :data="shares"
      :loading="loading"
      :pagination="false"
      row-key="id"
    >
      <template #action="{ record }">
        <a-button
          v-if="record.status === 'active'"
          type="text"
          size="mini"
          status="danger"
          @click="handleCancel(record.id)"
        >
          取消
        </a-button>
      </template>
    </a-table>
  </div>
</template>

<style scoped lang="scss">
.my-shares {
  padding: 16px;
}
</style>
