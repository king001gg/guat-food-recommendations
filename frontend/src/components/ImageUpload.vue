<template>
  <div class="image-upload">
    <el-upload
      drag
      accept="image/*"
      :show-file-list="false"
      :http-request="doUpload"
    >
      <div v-if="modelValue" class="preview">
        <img :src="modelValue" alt="预览" />
        <div class="mask">点击更换</div>
      </div>
      <div v-else class="placeholder">
        <el-icon class="icon"><UploadFilled /></el-icon>
        <div class="text">点击或拖拽上传图片</div>
      </div>
    </el-upload>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadImage } from '@/api'

const modelValue = defineModel({ type: String, default: '' })
const loading = ref(false)

const doUpload = async (options) => {
  loading.value = true
  try {
    const res = await uploadImage(options.file)
    modelValue.value = res.data
    ElMessage.success('上传成功')
  } catch (e) {
    ElMessage.error('上传失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.image-upload { width: 200px; }
.preview { position: relative; }
.preview img { width: 100%; height: 140px; object-fit: cover; border-radius: 6px; }
.preview .mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  opacity: 0;
  transition: opacity 0.2s;
}
.preview:hover .mask { opacity: 1; }
.placeholder { padding: 20px 0; color: #8c939d; }
.placeholder .icon { font-size: 34px; }
.placeholder .text { font-size: 13px; margin-top: 6px; }
</style>
