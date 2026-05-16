<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import request from '@/api/request'

const accounts = ref<any[]>([])
const loading = ref(false)
const showAddModal = ref(false)
const form = ref({ username: '', phone: '', password: '', role: 'operator' })

const ROLE_OPTIONS = [
  { value: 'operator', label: '运营' },
  { value: 'anchor', label: '主播' }
]

const columns = [
  { title: '昵称', dataIndex: 'username' },
  { title: '手机号', dataIndex: 'phone' },
  { title: '角色', dataIndex: 'role' },
  { title: '状态', dataIndex: 'status', slotName: 'status' },
  { title: '操作', slotName: 'actions', width: 100 }
]

async function fetchList() {
  loading.value = true
  try {
    const res = await request.get('/settings/sub-accounts')
    accounts.value = (res as any).data ?? res ?? []
  } catch {
    accounts.value = []
  } finally {
    loading.value = false
  }
}

async function addAccount() {
  try {
    await request.post('/settings/sub-accounts', form.value)
    Message.success('子账号已创建')
    showAddModal.value = false
    form.value = { username: '', phone: '', password: '', role: 'operator' }
    fetchList()
  } catch {
    Message.error('创建失败')
  }
}

async function deleteAccount(id: number) {
  try {
    await request.delete(`/settings/sub-accounts/${id}`)
    Message.success('已删除')
    fetchList()
  } catch {
    Message.error('删除失败')
  }
}

onMounted(fetchList)
</script>

<template>
  <div class="sub-accounts-wrap">
    <!-- Toolbar -->
    <div class="sa-toolbar">
      <span class="sa-count">子账号 <b>{{ accounts.length }}</b> / 20</span>
      <button
        class="djsbtn"
        @click="showAddModal = true"
        :disabled="accounts.length >= 20"
      >+ 添加子账号</button>
    </div>

    <!-- Table -->
    <div class="djscard sa-table-card">
      <table class="djstbl">
        <thead>
          <tr>
            <th>昵称</th>
            <th>手机号</th>
            <th>角色</th>
            <th>录制主播数</th>
            <th>昨日录制场次</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="7" style="text-align:center;padding:24px;color:var(--text-3);">加载中…</td>
          </tr>
          <tr v-else-if="!accounts.length">
            <td colspan="7" style="text-align:center;padding:24px;color:var(--text-3);">暂无子账号</td>
          </tr>
          <tr v-for="row in accounts" :key="row.id">
            <td>{{ row.username }}</td>
            <td>{{ row.phone }}</td>
            <td>{{ ROLE_OPTIONS.find(r => r.value === row.role)?.label || row.role }}</td>
            <td>{{ row.anchorCount ?? '—' }}</td>
            <td>{{ row.recordCount ?? '—' }}</td>
            <td>
              <span class="status-tag" :class="row.status === 1 ? 'status-ok' : 'status-off'">
                {{ row.status === 1 ? '正常' : '禁用' }}
              </span>
            </td>
            <td>
              <button
                class="djslink text-danger"
                @click="deleteAccount(row.id)"
              >删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Add modal -->
    <div v-if="showAddModal" class="modal-overlay" @click.self="showAddModal = false">
      <div class="modal-box">
        <div class="modal-title">添加子账号</div>
        <div class="djsform-row">
          <span class="djsform-label">昵称</span>
          <input class="djsinput djsform-ctrl" v-model="form.username" placeholder="请输入昵称" />
        </div>
        <div class="djsform-row">
          <span class="djsform-label">手机号</span>
          <input class="djsinput djsform-ctrl" v-model="form.phone" placeholder="请输入手机号" />
        </div>
        <div class="djsform-row">
          <span class="djsform-label">密码</span>
          <input class="djsinput djsform-ctrl" type="password" v-model="form.password" placeholder="请设置密码" />
        </div>
        <div class="djsform-row">
          <span class="djsform-label">角色</span>
          <select class="djsselect djsform-ctrl" v-model="form.role">
            <option v-for="r in ROLE_OPTIONS" :key="r.value" :value="r.value">{{ r.label }}</option>
          </select>
        </div>
        <div style="margin-top:20px;display:flex;gap:10px;justify-content:flex-end;">
          <button class="djsbtn ghost" @click="showAddModal = false">取消</button>
          <button class="djsbtn" @click="addAccount">确认添加</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.sub-accounts-wrap { max-width: 860px; }
.sa-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 14px;
}
.sa-count { font-size: 14px; color: var(--text-2b); }
.sa-table-card { border-radius: 14px; border: 1px solid var(--line); overflow: hidden; padding: 0; }
.status-tag { display: inline-block; padding: 2px 10px; border-radius: 20px; font-size: 12px; }
.status-ok { background: #E8F8EE; color: #18A750; }
.status-off { background: #FEE; color: #D03030; }
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
