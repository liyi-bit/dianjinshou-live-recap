<script setup lang="ts">
import { ref } from 'vue'
import { Message } from '@arco-design/web-vue'
import * as analysisApi from '@/api/analysis'

const props = defineProps<{
  visible: boolean
  taskId: number
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'saved'): void
}>()

const form = ref({
  action: '',
  goal: ''
})
const submitting = ref(false)

async function submit() {
  if (!form.value.action.trim()) {
    Message.warning('请填写优化动作')
    return
  }
  submitting.value = true
  try {
    await analysisApi.saveOptimization(props.taskId, {
      action: form.value.action,
      goal: form.value.goal || undefined
    })
    Message.success('优化动作已保存')
    emit('saved')
    emit('update:visible', false)
    form.value = { action: '', goal: '' }
  } catch {
    Message.error('保存失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <a-modal
    :visible="visible"
    title="优化动作"
    :width="520"
    @cancel="emit('update:visible', false)"
    :footer="false"
  >
    <a-form :model="form" layout="vertical">
      <a-form-item label="优化动作" required>
        <a-textarea
          v-model="form.action"
          placeholder="描述具体的优化动作（≤500字）"
          :max-length="500"
          show-word-limit
          :auto-size="{ minRows: 3, maxRows: 6 }"
        />
      </a-form-item>
      <a-form-item label="优化目的">
        <a-textarea
          v-model="form.goal"
          placeholder="描述优化目的（≤500字）"
          :max-length="500"
          show-word-limit
          :auto-size="{ minRows: 2, maxRows: 4 }"
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" :loading="submitting" @click="submit" long>
          确认保存
        </a-button>
      </a-form-item>
    </a-form>
  </a-modal>
</template>
