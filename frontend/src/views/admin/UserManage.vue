<template>
  <div>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索用户名/昵称" clearable style="width: 240px" @keyup.enter="load" @clear="load" />
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="160" />
      <el-table-column prop="username" label="用户名" width="140" />
      <el-table-column prop="nickname" label="昵称" width="140" />
      <el-table-column label="角色" width="110">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" size="small">{{ row.role === 'ADMIN' ? '管理员' : '用户' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'warning'" size="small">{{ row.status === 'ACTIVE' ? '正常' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" width="160">
        <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="320" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openReset(row)">重置密码</el-button>
          <el-button v-if="row.role !== 'ADMIN'" size="small" type="warning" @click="setRole(row)">设为管理员</el-button>
          <el-button v-if="row.role !== 'ADMIN'" size="small" :type="row.status === 'ACTIVE' ? 'info' : 'success'" @click="setStatus(row)">
            {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
          </el-button>
          <el-button v-if="row.role !== 'ADMIN'" size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination background layout="prev, pager, next, total" :total="total" :page-size="size" :current-page="page" @current-change="onPage" />
    </div>

    <el-dialog v-model="resetVisible" title="重置密码" width="440px" :close-on-click-modal="false">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        :title="`重置「${resetTarget?.nickname || resetTarget?.username || ''}」的密码后，该用户所有登录状态会立即失效。`"
      />
      <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-position="top" class="reset-form">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetForm.newPassword" type="password" show-password autocomplete="new-password" placeholder="8-100位" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="resetForm.confirmPassword" type="password" show-password autocomplete="new-password" placeholder="再次输入新密码" @keyup.enter="submitReset" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetLoading" @click="submitReset">确定重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUsers, updateUserStatus, updateUserRole, deleteUser, resetUserPassword } from '@/api'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const keyword = ref('')
const loading = ref(false)

const fmt = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '')

const load = async () => {
  loading.value = true
  try {
    const res = await getUsers({ keyword: keyword.value || null, page: page.value, size })
    list.value = res.data.records || []
    total.value = Number(res.data.total || 0)
  } finally {
    loading.value = false
  }
}
const onPage = (p) => { page.value = p; load() }

const setStatus = async (row) => {
  const status = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await updateUserStatus(row.id, status)
  ElMessage.success('操作成功')
  load()
}
const setRole = async (row) => {
  await updateUserRole(row.id, 'ADMIN')
  ElMessage.success('已设为管理员')
  load()
}
const remove = async (row) => {
  await ElMessageBox.confirm(`确定删除用户「${row.nickname}」吗？`, '提示', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('已删除')
  load()
}

// ─── 重置密码 ───
const resetVisible = ref(false)
const resetLoading = ref(false)
const resetTarget = ref(null)
const resetFormRef = ref()
const resetForm = reactive({ newPassword: '', confirmPassword: '' })
const resetRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 100, message: '新密码长度为8-100位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== resetForm.newPassword) callback(new Error('两次输入的新密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

const openReset = async (row) => {
  resetTarget.value = row
  resetForm.newPassword = ''
  resetForm.confirmPassword = ''
  resetVisible.value = true
  await nextTick()
  resetFormRef.value?.clearValidate()
}

const submitReset = async () => {
  try {
    await resetFormRef.value.validate()
  } catch {
    return
  }
  resetLoading.value = true
  try {
    await resetUserPassword(resetTarget.value.id, resetForm.newPassword)
    resetVisible.value = false
    if (String(resetTarget.value.id) === String(userStore.user?.id)) {
      userStore.clearLocal()
      ElMessage.success('密码已重置，请重新登录')
      router.push('/login')
    } else {
      ElMessage.success('密码已重置，该用户需重新登录')
    }
  } finally {
    resetLoading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 16px; }
.pager { display: flex; justify-content: center; margin-top: 16px; }
.reset-form { margin-top: 16px; }
</style>
