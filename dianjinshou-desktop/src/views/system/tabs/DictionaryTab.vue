<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { Message } from '@arco-design/web-vue'
import request from '@/api/request'

interface Dict { id: number; name: string; description: string; isSystem: number }
interface Keyword { id: number; category: string; subCategory: string; keyword: string; description: string }

const dictionaries = ref<Dict[]>([])
const selectedDictId = ref<number | null>(null)
const keywords = ref<Keyword[]>([])
const loadingKw = ref(false)

const showAddDict = ref(false)
const newDictName = ref('')
const newDictDesc = ref('')

const showAddKeyword = ref(false)
const kwForm = ref({ category: '', subCategory: '', keyword: '', description: '', replacementSuggestion: '' })

async function fetchDicts() {
  try {
    const res = await request.get('/settings/dictionaries')
    dictionaries.value = (res as any).data ?? res ?? []
    if (dictionaries.value.length > 0 && !selectedDictId.value) {
      selectedDictId.value = dictionaries.value[0].id
    }
  } catch {
    dictionaries.value = []
  }
}

async function fetchKeywords() {
  if (!selectedDictId.value) return
  loadingKw.value = true
  try {
    const res = await request.get(`/settings/dictionaries/${selectedDictId.value}/keywords`)
    keywords.value = (res as any).data ?? res ?? []
  } catch {
    keywords.value = []
  } finally {
    loadingKw.value = false
  }
}

async function createDict() {
  try {
    await request.post('/settings/dictionaries', { name: newDictName.value, description: newDictDesc.value })
    Message.success('词库已创建')
    showAddDict.value = false
    newDictName.value = ''
    newDictDesc.value = ''
    fetchDicts()
  } catch {
    Message.error('创建失败')
  }
}

async function deleteDict(id: number) {
  try {
    await request.delete(`/settings/dictionaries/${id}`)
    Message.success('已删除')
    if (selectedDictId.value === id) selectedDictId.value = null
    fetchDicts()
  } catch {
    Message.error('删除失败')
  }
}

async function addKeyword() {
  if (!selectedDictId.value) return
  try {
    await request.post(`/settings/dictionaries/${selectedDictId.value}/keywords`, kwForm.value)
    Message.success('关键词已添加')
    showAddKeyword.value = false
    kwForm.value = { category: '', subCategory: '', keyword: '', description: '', replacementSuggestion: '' }
    fetchKeywords()
  } catch {
    Message.error('添加失败')
  }
}

async function deleteKeyword(id: number) {
  try {
    await request.delete(`/settings/dictionaries/keywords/${id}`)
    Message.success('已删除')
    fetchKeywords()
  } catch {
    Message.error('删除失败')
  }
}

watch(selectedDictId, () => fetchKeywords())
onMounted(fetchDicts)
</script>

<template>
  <div class="dict-wrap">
    <!-- Top add bar -->
    <div class="dict-addbar djscard">
      <input
        class="djsinput"
        v-model="newDictName"
        placeholder="输入词库名称…"
        style="flex:1;"
        @keydown.enter="createDict"
      />
      <button class="djsbtn" @click="createDict">添加词库</button>
    </div>

    <!-- Layout: left list + right keywords -->
    <div class="dict-layout">
      <!-- Left: dictionary list -->
      <div class="dict-list djscard">
        <div class="dict-list-header">
          <span>词库列表</span>
        </div>
        <div
          v-for="d in dictionaries"
          :key="d.id"
          class="dict-item"
          :class="{ active: selectedDictId === d.id }"
          @click="selectedDictId = d.id"
        >
          <span class="dict-item-name">{{ d.name }}</span>
          <span v-if="d.isSystem" class="dict-badge-sys">系统</span>
          <button
            v-else
            class="djslink text-danger dict-del"
            @click.stop="deleteDict(d.id)"
          >删</button>
        </div>
        <div v-if="!dictionaries.length" class="dict-empty">暂无词库</div>
      </div>

      <!-- Right: keyword panel -->
      <div class="kw-panel">
        <div class="kw-toolbar">
          <span class="kw-title">关键词管理</span>
          <button
            class="djsbtn"
            @click="showAddKeyword = true"
            :disabled="!selectedDictId"
          >+ 新增关键词</button>
        </div>

        <div class="djscard kw-table-card">
          <table class="djstbl">
            <thead>
              <tr>
                <th>分类</th>
                <th>子分类</th>
                <th>关键词</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loadingKw">
                <td colspan="4" style="text-align:center;padding:20px;color:var(--text-3);">加载中…</td>
              </tr>
              <tr v-else-if="!keywords.length">
                <td colspan="4" style="text-align:center;padding:20px;color:var(--text-3);">暂无关键词</td>
              </tr>
              <tr v-for="kw in keywords" :key="kw.id">
                <td>{{ kw.category }}</td>
                <td>{{ kw.subCategory }}</td>
                <td>{{ kw.keyword }}</td>
                <td>
                  <button class="djslink text-danger" @click="deleteKeyword(kw.id)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Add keyword modal -->
    <div v-if="showAddKeyword" class="modal-overlay" @click.self="showAddKeyword = false">
      <div class="modal-box">
        <div class="modal-title">新增关键词</div>
        <div class="djsform-row">
          <span class="djsform-label">分类</span>
          <input class="djsinput djsform-ctrl" v-model="kwForm.category" placeholder="如：运营关键词" />
        </div>
        <div class="djsform-row">
          <span class="djsform-label">子分类</span>
          <input class="djsinput djsform-ctrl" v-model="kwForm.subCategory" placeholder="如：促单" />
        </div>
        <div class="djsform-row">
          <span class="djsform-label">关键词</span>
          <input class="djsinput djsform-ctrl" v-model="kwForm.keyword" placeholder="请输入关键词" />
        </div>
        <div class="djsform-row">
          <span class="djsform-label">说明</span>
          <input class="djsinput djsform-ctrl" v-model="kwForm.description" placeholder="说明（选填）" />
        </div>
        <div style="margin-top:20px;display:flex;gap:10px;justify-content:flex-end;">
          <button class="djsbtn ghost" @click="showAddKeyword = false">取消</button>
          <button class="djsbtn" @click="addKeyword">确认添加</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dict-wrap { max-width: 900px; }
.dict-addbar {
  display: flex; gap: 10px; align-items: center;
  padding: 14px 18px; margin-bottom: 16px;
  border-radius: 14px; border: 1px solid var(--line); background: var(--card);
}
.dict-layout { display: flex; gap: 16px; min-height: 400px; }
.dict-list {
  width: 200px; min-width: 200px; border-radius: 14px;
  border: 1px solid var(--line); overflow: hidden; padding: 0;
}
.dict-list-header {
  padding: 12px 14px; font-weight: 700; font-size: 13px;
  border-bottom: 1px solid var(--line); background: var(--bg);
}
.dict-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 9px 14px; cursor: pointer; font-size: 13px;
}
.dict-item:hover { background: var(--bg); }
.dict-item.active { background: var(--brand-soft); color: var(--brand); }
.dict-item-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dict-badge-sys {
  background: var(--brand-soft); color: var(--brand);
  font-size: 11px; padding: 1px 7px; border-radius: 10px;
}
.dict-del { font-size: 12px; }
.dict-empty { text-align: center; padding: 20px; color: var(--text-3); font-size: 13px; }
.kw-panel { flex: 1; display: flex; flex-direction: column; }
.kw-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 12px;
}
.kw-title { font-size: 15px; font-weight: 700; }
.kw-table-card { border-radius: 14px; border: 1px solid var(--line); overflow: hidden; padding: 0; flex: 1; }
.text-danger { color: #E03030; }
.modal-overlay {
  position: fixed; inset: 0; background: rgba(36,30,24,.45);
  display: flex; align-items: center; justify-content: center; z-index: 9999;
}
.modal-box {
  background: var(--card); border-radius: 14px; padding: 28px 32px; min-width: 380px;
  box-shadow: 0 8px 40px -8px rgba(36,30,24,.22);
}
.modal-title { font-size: 16px; font-weight: 700; margin-bottom: 20px; }
</style>
