<template>
  <div class="upload-panel">
    <!-- Drop zone -->
    <div
      class="upload-dropzone"
      :class="{ 'is-dragover': isDragover }"
      @dragover.prevent="isDragover = true"
      @dragleave="isDragover = false"
      @drop.prevent="handleDrop"
      @click="triggerFileInput"
    >
      <icon-upload class="upload-icon" />
      <div class="upload-text">
        <span>拖拽文件到此处，或 <a-link>点击选择文件</a-link></span>
      </div>
      <div class="upload-hint">
        支持 MP4、FLV、AVI、MP3、TXT，单文件最大 2GB
      </div>
      <input
        ref="fileInputRef"
        type="file"
        :accept="acceptTypes"
        :multiple="multiple"
        style="display: none"
        @change="handleFileSelect"
      />
    </div>

    <!-- Queue list -->
    <div v-if="queueStore.items.length > 0" class="upload-queue">
      <div class="queue-header">
        <span>上传队列 ({{ queueStore.items.length }})</span>
        <a-button size="mini" type="text" @click="queueStore.clearCompleted()">
          清除已完成
        </a-button>
      </div>
      <div v-for="item in queueStore.items" :key="item.id" class="queue-item">
        <div class="item-info">
          <span class="item-name">{{ item.file.name }}</span>
          <span class="item-size">{{ formatSize(item.file.size) }}</span>
        </div>
        <a-progress
          v-if="item.status === 'uploading'"
          :percent="item.progress / 100"
          size="small"
        />
        <div class="item-status">
          <a-tag v-if="item.status === 'queued'" color="blue">排队中</a-tag>
          <a-tag v-else-if="item.status === 'uploading'" color="orangered">
            上传中 {{ item.progress }}%
          </a-tag>
          <a-tag v-else-if="item.status === 'completed'" color="green">已完成</a-tag>
          <a-tag v-else-if="item.status === 'failed'" color="red">
            失败: {{ item.error }}
          </a-tag>
          <a-tag v-else-if="item.status === 'cancelled'" color="gray">已取消</a-tag>

          <a-button
            v-if="item.status === 'uploading'"
            size="mini"
            type="text"
            status="danger"
            @click="queueStore.cancelItem(item.id)"
          >
            取消
          </a-button>
          <a-button
            v-if="item.status !== 'uploading'"
            size="mini"
            type="text"
            @click="queueStore.removeItem(item.id)"
          >
            移除
          </a-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useUploadQueueStore } from '@/stores/uploadQueue'
import { IconUpload } from '@arco-design/web-vue/es/icon'

const props = withDefaults(defineProps<{
  bucket?: string
  multiple?: boolean
  acceptTypes?: string
}>(), {
  multiple: true,
  acceptTypes: '.mp4,.flv,.avi,.mp3,.txt'
})

const emit = defineEmits<{
  (e: 'uploaded', storageKey: string, fileName: string): void
}>()

const queueStore = useUploadQueueStore()
const fileInputRef = ref<HTMLInputElement | null>(null)
const isDragover = ref(false)

function triggerFileInput() {
  fileInputRef.value?.click()
}

function handleFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  if (input.files) {
    addFiles(Array.from(input.files))
    input.value = ''
  }
}

function handleDrop(event: DragEvent) {
  isDragover.value = false
  if (event.dataTransfer?.files) {
    addFiles(Array.from(event.dataTransfer.files))
  }
}

function addFiles(files: File[]) {
  for (const file of files) {
    queueStore.addFile(file, props.bucket)
  }
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}
</script>

<style scoped>
.upload-dropzone {
  border: 2px dashed var(--color-border-3);
  border-radius: 8px;
  padding: 32px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
}

.upload-dropzone:hover,
.upload-dropzone.is-dragover {
  border-color: rgb(var(--primary-6));
  background: var(--color-primary-light-1);
}

.upload-icon {
  font-size: 32px;
  color: var(--color-text-3);
  margin-bottom: 8px;
}

.upload-text {
  color: var(--color-text-2);
  margin-bottom: 4px;
}

.upload-hint {
  color: var(--color-text-4);
  font-size: 12px;
}

.upload-queue {
  margin-top: 16px;
}

.queue-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--color-text-2);
}

.queue-item {
  padding: 8px 12px;
  border: 1px solid var(--color-border-2);
  border-radius: 6px;
  margin-bottom: 6px;
}

.item-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.item-name {
  font-size: 13px;
  color: var(--color-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 70%;
}

.item-size {
  font-size: 12px;
  color: var(--color-text-3);
}

.item-status {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}
</style>
