<script setup lang="ts">
import { ref, watch } from 'vue'
import request from '@/api/request'

const props = defineProps<{
  visible: boolean
  streamerId: number
  industryId: number | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  select: [competitorId: number]
}>()

const streamers = ref<any[]>([])
const loading = ref(false)
const selected = ref<number | null>(null)

async function fetchStreamers() {
  loading.value = true
  try {
    const res = await request.get('/streamers', {
      params: { page: 1, size: 100, industryId: props.industryId }
    })
    const data = (res as any).data ?? res
    const items = data.items || data.records || []
    // Filter out current streamer
    streamers.value = items.filter((s: any) => s.id !== props.streamerId)
  } catch {
    streamers.value = []
  } finally {
    loading.value = false
  }
}

function handleConfirm() {
  if (selected.value) {
    emit('select', selected.value)
    emit('update:visible', false)
  }
}

function handleClose() {
  selected.value = null
  emit('update:visible', false)
}

watch(() => props.visible, (val) => {
  if (val) fetchStreamers()
})
</script>

<template>
  <a-modal
    :visible="visible"
    title="选择竞品主播"
    @cancel="handleClose"
    @ok="handleConfirm"
    :ok-button-props="{ disabled: !selected }"
    :width="500"
  >
    <a-spin :loading="loading">
      <div v-if="streamers.length === 0 && !loading" style="text-align: center; padding: 20px">
        <a-empty description="暂无同行业主播" />
      </div>
      <a-radio-group v-else v-model="selected" direction="vertical" style="width: 100%">
        <a-radio v-for="s in streamers" :key="s.id" :value="s.id" style="margin-bottom: 8px">
          <div style="display: flex; align-items: center; gap: 8px">
            <a-avatar :size="32" :image-url="s.anchorAvatar || undefined">{{ s.anchorAvatar ? '' : (s.anchorName || s.roomId || '').charAt(0) }}</a-avatar>
            <div>
              <div style="font-weight: 500">{{ s.anchorName || s.roomId }}</div>
              <div style="font-size: 12px; color: var(--color-text-3)">{{ s.platform }}</div>
            </div>
          </div>
        </a-radio>
      </a-radio-group>
    </a-spin>
  </a-modal>
</template>
