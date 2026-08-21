<template>
  <div>
    <div class="toolbar">
      <el-button type="primary" @click="openAdd"><el-icon><Plus /></el-icon>&nbsp;新增食堂</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="160" />
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column prop="location" label="位置" width="140" />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑食堂' : '新增食堂'" width="420px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="例如：第一食堂" />
        </el-form-item>
        <el-form-item label="位置">
          <el-input v-model="form.location" placeholder="例如：南校区" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCanteens, createCanteen, updateCanteen, deleteCanteen } from '@/api'

const list = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref(false)
const form = reactive({ id: null, name: '', location: '', sortOrder: 0 })

const load = async () => {
  loading.value = true
  try {
    const res = await getCanteens()
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

const openAdd = () => {
  editing.value = false
  form.id = null
  form.name = ''
  form.location = ''
  form.sortOrder = 0
  dialogVisible.value = true
}
const openEdit = (row) => {
  editing.value = true
  form.id = row.id
  form.name = row.name
  form.location = row.location
  form.sortOrder = row.sortOrder || 0
  dialogVisible.value = true
}

const save = async () => {
  if (!form.name) return ElMessage.warning('请填写名称')
  const payload = { name: form.name, location: form.location, sortOrder: form.sortOrder }
  if (editing.value) await updateCanteen(form.id, payload)
  else await createCanteen(payload)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确定删除食堂「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteCanteen(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { margin-bottom: 16px; }
</style>
