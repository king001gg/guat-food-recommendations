<template>
  <div class="auth-page">
    <div class="auth-card guat-card">
      <div class="auth-head">
        <el-icon class="rocket"><Promotion /></el-icon>
        <h2>登录</h2>
        <p>欢迎回到桂航美食排行榜</p>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" size="large" placeholder="请输入用户名" :prefix-icon="'User'" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" size="large" show-password placeholder="请输入密码" :prefix-icon="'Lock'" @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="submit">登 录</el-button>
      </el-form>
      <div class="auth-foot">
        还没有账号？<router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const submit = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await login(form)
    userStore.setLogin(res.data)
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e5aa8 0%, #3b7fd1 100%);
  padding: 20px;
}
.auth-card { width: 400px; padding: 32px; }
.auth-head { text-align: center; margin-bottom: 20px; }
.auth-head .rocket { font-size: 40px; color: var(--guat-blue); }
.auth-head h2 { margin: 8px 0 4px; }
.auth-head p { margin: 0; color: #909399; font-size: 13px; }
.submit-btn { width: 100%; }
.auth-foot { margin-top: 16px; text-align: center; font-size: 14px; color: #606266; }
.auth-foot a { color: var(--guat-blue); font-weight: 600; }
</style>
