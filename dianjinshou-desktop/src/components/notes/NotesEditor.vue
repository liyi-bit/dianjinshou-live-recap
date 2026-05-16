<script setup lang="ts">
import { onBeforeUnmount, watch } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import TextAlign from '@tiptap/extension-text-align'

const props = defineProps<{
  modelValue: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: string): void
}>()

const editor = useEditor({
  content: props.modelValue,
  extensions: [
    StarterKit,
    Underline,
    TextAlign.configure({ types: ['heading', 'paragraph'] })
  ],
  onUpdate({ editor: ed }) {
    emit('update:modelValue', ed.getHTML())
  }
})

watch(() => props.modelValue, (val) => {
  if (editor.value && editor.value.getHTML() !== val) {
    editor.value.commands.setContent(val, false)
  }
})

onBeforeUnmount(() => {
  editor.value?.destroy()
})

function isActive(name: string, attrs?: Record<string, unknown>): boolean {
  return editor.value?.isActive(name, attrs) ?? false
}
</script>

<template>
  <div class="notes-editor" v-if="editor">
    <div class="toolbar">
      <button :class="{ active: isActive('heading', { level: 2 }) }" @click="editor!.chain().focus().toggleHeading({ level: 2 }).run()">H</button>
      <button :class="{ active: isActive('bold') }" @click="editor!.chain().focus().toggleBold().run()">B</button>
      <button :class="{ active: isActive('italic') }" @click="editor!.chain().focus().toggleItalic().run()">I</button>
      <button :class="{ active: isActive('underline') }" @click="editor!.chain().focus().toggleUnderline().run()">U</button>
      <button :class="{ active: isActive('strike') }" @click="editor!.chain().focus().toggleStrike().run()">S</button>
      <span class="divider" />
      <button @click="editor!.chain().focus().setTextAlign('left').run()">左</button>
      <button @click="editor!.chain().focus().setTextAlign('center').run()">中</button>
      <button @click="editor!.chain().focus().setTextAlign('right').run()">右</button>
      <span class="divider" />
      <button :class="{ active: isActive('bulletList') }" @click="editor!.chain().focus().toggleBulletList().run()">列</button>
      <button :class="{ active: isActive('orderedList') }" @click="editor!.chain().focus().toggleOrderedList().run()">序</button>
      <button :class="{ active: isActive('blockquote') }" @click="editor!.chain().focus().toggleBlockquote().run()">引</button>
      <button @click="editor!.chain().focus().setHorizontalRule().run()">线</button>
      <span class="divider" />
      <button @click="editor!.chain().focus().undo().run()">撤</button>
      <button @click="editor!.chain().focus().redo().run()">重</button>
    </div>
    <EditorContent :editor="editor" class="editor-content" />
  </div>
</template>

<style scoped lang="scss">
.notes-editor {
  display: flex;
  flex-direction: column;
  height: 100%;
  border: 1px solid var(--color-border);
  border-radius: 6px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 6px 8px;
  border-bottom: 1px solid var(--color-border);
  flex-wrap: wrap;

  button {
    width: 28px;
    height: 28px;
    border: none;
    border-radius: 4px;
    background: transparent;
    cursor: pointer;
    font-size: 13px;
    font-weight: 600;
    color: var(--color-text-2);

    &:hover { background: var(--color-fill-2); }
    &.active { background: var(--color-fill-3); color: rgb(var(--primary-6)); }
  }

  .divider {
    width: 1px;
    height: 20px;
    background: var(--color-border);
    margin: 0 4px;
  }
}

.editor-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;

  :deep(.tiptap) {
    outline: none;
    min-height: 200px;
    font-size: 14px;
    line-height: 1.7;

    h2 { font-size: 18px; margin: 16px 0 8px; }
    p { margin: 4px 0; }
    blockquote {
      border-left: 3px solid var(--color-border);
      padding-left: 12px;
      color: var(--color-text-3);
    }
    hr { border: none; border-top: 1px solid var(--color-border); margin: 12px 0; }
    ul, ol { padding-left: 24px; }
  }
}
</style>
