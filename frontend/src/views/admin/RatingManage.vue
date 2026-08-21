<template>
  <div>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索评价内容" clearable style="width: 240px" @keyup.enter="load" @clear="load" />
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="160" />
      <el-table-column label="用户" width="120">
        <template #default="{ row }">{{ row.user?.nickname || '-' }}</template>
      </el-table-column>
      <el-table-column label="对象" min-width="140">
        <template #default="{ row }">
          <el-tag size="small" :type="row.targetType === 'WINDOW' ? 'primary' : 'warning'" effect="plain">
            {{ row.targetType === 'WINDOW' ? '档口' : '菜品' }}
          </el-tag>
          {{ row.targetName || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="评分" width="130">
        <template #default="{ row }">口味{{ row.taste }} 性价比{{ row.valueScore }} 分量{{ row.portion }}</template>
      </el-table-column>
      <el-table-column prop="comment" label="评语" min-width="180" show-overflow-tooltip />
      <el-table-column label="时间" width="160">
        <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
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
import { adminRatings, adminDeleteRating } from '@/api'

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
    const res = await adminRatings({ keyword: keyword.value || null, page: page.value, size })
    list.value = res.data.records || []
    total.value = Number(res.data.total || 0)
  } finally {
    loading.value = false
  }
}
const onPage = (p) => { page.value = p; load() }

const remove = async (row) => {
  await ElMessageBox.confirm('确定删除这条评价吗？', '提示', { type: 'warning' })
  await adminDeleteRating(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 16px; }
.pager { display: flex; justify-content: center; margin-top: 16px; }
</style>
