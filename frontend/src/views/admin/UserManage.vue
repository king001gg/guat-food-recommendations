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
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUsers, updateUserStatus, updateUserRole, deleteUser } from '@/api'

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

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 16px; }
.pager { display: flex; justify-content: center; margin-top: 16px; }
</style>
