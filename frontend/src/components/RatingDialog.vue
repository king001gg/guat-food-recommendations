<template>
  <el-dialog v-model="visible" :title="`评价：${targetName}`" width="440px" destroy-on-close>
    <div class="rate-form">
      <div class="rate-row">
        <span class="label">口味</span>
        <el-rate v-model="form.taste" :max="5" />
      </div>
      <div class="rate-row">
        <span class="label">性价比</span>
        <el-rate v-model="form.valueScore" :max="5" />
      </div>
      <div class="rate-row">
        <span class="label">分量</span>
        <el-rate v-model="form.portion" :max="5" />
      </div>
      <el-input
        v-model="form.comment"
        type="textarea"
        :rows="3"
        maxlength="500"
        show-word-limit
        placeholder="写下你的评价（选填）"
      />
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="submit">提交评价</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { submitRating } from '@/api'

const visible = defineModel({ type: Boolean, default: false })

const props = defineProps({
  targetType: { type: String, required: true },
  targetId: { type: [String, Number], required: true },
  targetName: { type: String, default: '' },
})
const emit = defineEmits(['rated'])

const loading = ref(false)
const form = reactive({ taste: 5, valueScore: 5, portion: 5, comment: '' })

const reset = () => {
  form.taste = 5
  form.valueScore = 5
  form.portion = 5
  form.comment = ''
}

const submit = async () => {
  if (!form.taste || !form.valueScore || !form.portion) {
    ElMessage.warning('请完成三项评分')
    return
  }
  loading.value = true
  try {
    await submitRating({
      targetType: props.targetType,
      targetId: props.targetId,
      taste: form.taste,
      valueScore: form.valueScore,
      portion: form.portion,
      comment: form.comment,
    })
    ElMessage.success('评价成功')
    visible.value = false
    reset()
    emit('rated')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.rate-form { display: flex; flex-direction: column; gap: 16px; }
.rate-row { display: flex; align-items: center; gap: 16px; }
.rate-row .label { width: 48px; color: #606266; }
</style>
