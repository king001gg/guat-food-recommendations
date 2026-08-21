<template>
  <div>
    <AppHeader />
    <div class="page-container submit-page">
      <div class="guat-card form-card">
        <h2 class="form-title"><el-icon><EditPen /></el-icon>&nbsp;我要投稿</h2>
        <p class="form-sub">分享你心中的桂航美食，投稿经管理员审核后即可上榜</p>

        <el-radio-group v-model="target" class="target-switch">
          <el-radio-button value="window">投稿档口</el-radio-button>
          <el-radio-button value="dish">投稿菜品</el-radio-button>
        </el-radio-group>

        <el-form :model="form" :rules="rules" ref="formRef" label-position="top" class="form-body">
          <template v-if="target === 'window'">
            <el-form-item label="所属食堂" prop="canteenId">
              <el-select v-model="form.canteenId" placeholder="请选择食堂" style="width: 100%">
                <el-option v-for="c in canteens" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="档口名称" prop="name">
              <el-input v-model="form.name" placeholder="例如：桂林米粉" />
            </el-form-item>
            <el-form-item label="档口位置">
              <el-input v-model="form.location" placeholder="例如：一楼东侧" />
            </el-form-item>
            <el-form-item label="简介">
              <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="介绍一下这个档口" />
            </el-form-item>
            <el-form-item label="封面图片">
              <ImageUpload v-model="form.coverImage" />
            </el-form-item>
          </template>

          <template v-else>
            <el-form-item label="所属档口" prop="windowId">
              <el-select v-model="form.windowId" placeholder="请选择档口" filterable style="width: 100%">
                <el-option v-for="w in windows" :key="w.id" :label="w.name" :value="w.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="菜品名称" prop="name">
              <el-input v-model="form.name" placeholder="例如：招牌螺蛳粉" />
            </el-form-item>
            <el-form-item label="价格（元）" prop="price">
              <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" style="width: 200px" />
            </el-form-item>
            <el-form-item label="简介">
              <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="介绍一下这道菜" />
            </el-form-item>
            <el-form-item label="菜品图片">
              <ImageUpload v-model="form.image" />
            </el-form-item>
          </template>

          <el-form-item>
            <el-button type="primary" size="large" :loading="loading" @click="submit">提交投稿</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
    <AppFooter />
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import { getCanteens, rankWindows, submitWindow, submitDish } from '@/api'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const target = ref('window')
const canteens = ref([])
const windows = ref([])

const form = reactive({
  canteenId: null,
  windowId: null,
  name: '',
  location: '',
  description: '',
  price: null,
  coverImage: '',
  image: '',
})

const rules = {
  name: [{ required: true, message: '请填写名称', trigger: 'blur' }],
  canteenId: [{ required: true, message: '请选择食堂', trigger: 'change' }],
  windowId: [{ required: true, message: '请选择档口', trigger: 'change' }],
  price: [{ required: true, message: '请填写价格', trigger: 'blur' }],
}

const submit = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    if (target.value === 'window') {
      await submitWindow({
        canteenId: form.canteenId,
        name: form.name,
        location: form.location,
        description: form.description,
        coverImage: form.coverImage || null,
      })
    } else {
      await submitDish({
        windowId: form.windowId,
        name: form.name,
        price: form.price,
        description: form.description,
        image: form.image || null,
      })
    }
    ElMessage.success('投稿成功，等待管理员审核')
    router.push('/')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  const res = await getCanteens()
  canteens.value = res.data || []
  const wr = await rankWindows({ type: 'overall', page: 1, size: 200 })
  windows.value = wr.data.list || []
})
</script>

<style scoped>
.submit-page { max-width: 720px; }
.form-card { padding: 28px; }
.form-title { display: flex; align-items: center; margin: 0 0 6px; }
.form-sub { color: #909399; font-size: 13px; margin: 0 0 20px; }
.target-switch { margin-bottom: 20px; }
.form-body :deep(.el-form-item) { margin-bottom: 18px; }
</style>
