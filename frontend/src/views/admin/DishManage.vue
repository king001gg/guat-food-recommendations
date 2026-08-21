<template>
  <div>
    <div class="toolbar">
      <el-select v-model="status" placeholder="全部状态" clearable style="width: 140px" @change="load">
        <el-option label="待审核" value="PENDING" />
        <el-option label="已发布" value="PUBLISHED" />
      </el-select>
      <el-input v-model="keyword" placeholder="搜索菜品名称" clearable style="width: 220px" @keyup.enter="load" @clear="load" />
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="160" />
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column label="档口" width="140">
        <template #default="{ row }">{{ row.windowName || '-' }}</template>
      </el-table-column>
      <el-table-column label="价格" width="90">
        <template #default="{ row }">¥{{ Number(row.price ?? 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'warning'" size="small">
            {{ row.status === 'PUBLISHED' ? '已发布' : '待审核' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="160">
        <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING'" size="small" type="success" @click="approve(row, 'PUBLISHED')">通过</el-button>
          <el-button v-else size="small" type="warning" @click="approve(row, 'PENDING')">驳回</el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
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
import { adminDishes, approveDish, deleteDish } from '@/api'

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const status = ref(null)
const keyword = ref('')
const loading = ref(false)

const fmt = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '')

const load = async () => {
  loading.value = true
  try {
    const res = await adminDishes({ status: status.value || null, keyword: keyword.value || null, page: page.value, size })
    list.value = res.data.records || []
    total.value = Number(res.data.total || 0)
  } finally {
    loading.value = false
  }
}
const onPage = (p) => { page.value = p; load() }

const approve = async (row, s) => {
  await approveDish(row.id, s)
  ElMessage.success(s === 'PUBLISHED' ? '已通过' : '已驳回')
  load()
}
const remove = async (row) => {
  await ElMessageBox.confirm(`确定删除菜品「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteDish(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 16px; }
.pager { display: flex; justify-content: center; margin-top: 16px; }
</style>
