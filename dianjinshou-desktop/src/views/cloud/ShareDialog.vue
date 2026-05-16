<script setup lang="ts">
import { ref } from 'vue'
import { createShareLink, type ShareLinkItem } from '@/api/share'
import { Message } from '@arco-design/web-vue'

const props = defineProps<{
  visible: boolean
  fileId: number
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  created: [link: ShareLinkItem]
}>()

const password = ref('')
const expireHours = ref<number | undefined>(24)
const maxDownloads = ref<number | undefined>()
const loading = ref(false)
const createdLink = ref<ShareLinkItem | null>(null)

const expireOptions = [
  { value: 1, label: '1小时' },
  { value: 24, label: '24小时' },
  { value: 72, label: '3天' },
  { value: 168, label: '7天' },
  { value: undefined, label: '永久' }
]

async function handleCreate() {
  loading.value = true
  try {
    const res = await createShareLink(
      props.fileId,
      password.value || undefined,
      expireHours.value,
      maxDownloads.value
    )
    createdLink.value = (res as any).data ?? res
    emit('created', createdLink.value!)
    Message.success('分享链接已创建')
  } finally {
    loading.value = false
  }
}

function handleCopy() {
  if (!createdLink.value) return
  const text = createdLink.value.shareUrl + (password.value ? `\n密码: ${password.value}` : '')
  navigator.clipboard.writeText(text)
  Message.success('已复制到剪贴板')
}

function handleClose() {
  createdLink.value = null
  password.value = ''
  expireHours.value = 24
  maxDownloads.value = undefined
  emit('update:visible', false)
}
</script>

<template>
  <a-modal
    :visible="visible"
    title="创建分享链接"
    @cancel="handleClose"
    :footer="false"
    :width="440"
  >
    <template v-if="!createdLink">
      <a-form layout="vertical">
        <a-form-item label="密码（可选）">
          <a-input v-model="password" placeholder="4位密码，留空则无密码" :max-length="10" />
        </a-form-item>
        <a-form-item label="有效期">
          <a-radio-group v-model="expireHours">
            <a-radio v-for="opt in expireOptions" :key="String(opt.value)" :value="opt.value">
              {{ opt.label }}
            </a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="最大下载次数（可选）">
          <a-input-number v-model="maxDownloads" placeholder="不限" :min="1" style="width: 100%" />
        </a-form-item>
      </a-form>
      <a-button type="primary" long :loading="loading" @click="handleCreate">
        创建分享链接
      </a-button>
    </template>

    <template v-else>
      <div class="share-result">
        <div class="share-result__url">
          <a-input :model-value="createdLink.shareUrl" readonly>
            <template #append>
              <a-button type="text" @click="handleCopy">复制</a-button>
            </template>
          </a-input>
        </div>
        <div v-if="password" class="share-result__password">
          密码: <strong>{{ password }}</strong>
        </div>
        <a-button long @click="handleClose" style="margin-top: 16px">关闭</a-button>
      </div>
    </template>
  </a-modal>
</template>

<style scoped lang="scss">
.share-result {
  text-align: center;

  &__url {
    margin-bottom: 12px;
  }

  &__password {
    color: var(--color-text-2);
  }
}
</style>
