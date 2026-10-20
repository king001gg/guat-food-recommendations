<template>
  <div>
    <div class="toolbar">
      <el-select v-model="filters.action" placeholder="全部操作" clearable style="width: 170px">
        <el-option v-for="(label, value) in actionLabels" :key="value" :label="label" :value="value" />
      </el-select>
      <el-select v-model="filters.success" placeholder="全部结果" clearable style="width: 120px">
        <el-option label="成功" :value="true" />
        <el-option label="失败" :value="false" />
      </el-select>
      <el-input v-model="filters.actorId" placeholder="操作人 ID" clearable style="width: 130px" />
      <el-date-picker
        v-model="dateRange"
        type="datetimerange"
        range-separator="至"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        value-format="YYYY-MM-DD HH:mm:ss"
        style="width: 360px"
      />
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border>
      <el-table-column label="时间" width="170">
        <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">{{ actionLabels[row.action] || row.action }}</template>
      </el-table-column>
      <el-table-column label="操作人" width="160">
        <template #default="{ row }">
          <span>{{ row.actorUsername || '-' }}</span>
          <span v-if="row.actorId" class="muted">#{{ row.actorId }}</span>
        </template>
      </el-table-column>
      <el-table-column label="角色" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.actorRole" size="small" :type="row.actorRole === 'ADMIN' ? 'danger' : 'info'">
            {{ row.actorRole === 'ADMIN' ? '管理员' : '用户' }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="请求" min-width="230" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="method">{{ row.method }}</span>&nbsp;{{ row.path }}
          <span v-if="row.queryString" class="muted">?{{ row.queryString }}</span>
        </template>
      </el-table-column>
      <el-table-column label="结果" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.success ? 'success' : 'danger'">{{ row.resultCode }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="ip" label="IP" width="140" show-overflow-tooltip />
      <el-table-column label="耗时" width="90" align="right">
        <template #default="{ row }">{{ row.durationMs ?? 0 }} ms</template>
      </el-table-column>
      <el-table-column label="详情" min-width="170" show-overflow-tooltip>
        <template #default="{ row }">{{ row.errorMessage || row.userAgent || '-' }}</template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        background
        layout="prev, pager, next, total"
        :total="total"
        :page-size="size"
        :current-page="page"
        @current-change="onPage"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAuditLogs } from '@/api'

const actionLabels = {
  LOGIN: '登录',
  LOGOUT: '注销',
  REGISTER: '注册',
  PASSWORD_CHANGE: '修改密码',
  PASSWORD_RESET: '重置密码',
  USER_STATUS: '用户启停',
  USER_ROLE: '变更角色',
  USER_DELETE: '删除用户',
  CANTEEN_CREATE: '新增食堂',
  CANTEEN_UPDATE: '修改食堂',
  CANTEEN_DELETE: '删除食堂',
  WINDOW_SUBMIT: '提交档口',
  WINDOW_APPROVE: '审核档口',
  WINDOW_DELETE: '删除档口',
  DISH_SUBMIT: '提交菜品',
  DISH_APPROVE: '审核菜品',
  DISH_DELETE: '删除菜品',
  RATING_SUBMIT: '提交评价',
  RATING_DELETE: '删除评价',
  RATING_ADMIN_DELETE: '管理员删除评价',
  UPLOAD_IMAGE: '上传图片',
}

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = 20
const loading = ref(false)
const dateRange = ref([])

const filters = reactive({ action: '', success: null, actorId: '' })

const fmt = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '')

const load = async () => {
  if (filters.actorId && !/^\d+$/.test(String(filters.actorId).trim())) {
    ElMessage.warning('操作人 ID 必须是数字')
    return
  }
  const params = { page: page.value, size }
  if (filters.action) params.action = filters.action
  if (typeof filters.success === 'boolean') params.success = filters.success
  if (filters.actorId) params.actorId = Number(filters.actorId)
  if (dateRange.value?.length === 2) {
    params.start = dateRange.value[0]
    params.end = dateRange.value[1]
  }

  loading.value = true
  try {
    const res = await getAuditLogs(params)
    list.value = res.data.records || []
    total.value = Number(res.data.total || 0)
  } finally {
    loading.value = false
  }
}

const search = () => {
  page.value = 1
  load()
}

const resetFilters = () => {
  filters.action = ''
  filters.success = null
  filters.actorId = ''
  dateRange.value = []
  search()
}

const onPage = (p) => {
  page.value = p
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 16px; }
.pager { display: flex; justify-content: center; margin-top: 16px; }
.muted { color: #909399; font-size: 12px; margin-left: 4px; }
.method { color: var(--guat-orange, #ff9f43); font-weight: 600; font-size: 12px; }
</style>
