<template>
  <div class="auth-page">
    <div class="auth-card guat-card">
      <div class="auth-head">
        <el-icon class="rocket"><Promotion /></el-icon>
        <h2>注册</h2>
        <p>加入桂航美食推荐社区</p>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" size="large" placeholder="3-50位用户名" :prefix-icon="'User'" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" size="large" placeholder="你的昵称" :prefix-icon="'Postcard'" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" size="large" show-password placeholder="至少6位密码" :prefix-icon="'Lock'" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirm">
          <el-input v-model="form.confirm" type="password" size="large" show-password placeholder="再次输入密码" :prefix-icon="'Lock'" @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="submit">注 册</el-button>
      </el-form>
      <div class="auth-foot">
        已有账号？<router-link to="/login">直接登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', nickname: '', password: '', confirm: '' })

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度为3-50位', trigger: 'blur' },
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度为6-100位', trigger: 'blur' },
  ],
  confirm: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

const submit = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    await register({ username: form.username, password: form.password, nickname: form.nickname })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
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
