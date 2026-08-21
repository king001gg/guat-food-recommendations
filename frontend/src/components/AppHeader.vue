<template>
  <header class="app-header">
    <div class="header-inner">
      <router-link to="/" class="logo">
        <span class="logo-badge">🚀</span>
        <span class="logo-text">桂航美食榜</span>
      </router-link>

      <nav class="nav">
        <router-link to="/" :class="{ active: isActive('/') }">排行榜</router-link>
        <router-link to="/submit" :class="{ active: isActive('/submit') }">我要投稿</router-link>
        <router-link to="/profile" :class="{ active: isActive('/profile') }">个人中心</router-link>
        <router-link v-if="userStore.isAdmin()" to="/admin" :class="{ active: isActive('/admin') }">管理后台</router-link>
      </nav>

      <div class="actions">
        <template v-if="userStore.isLogin()">
          <el-dropdown @command="onCommand">
            <span class="user-chip">
              <el-avatar :size="28" :src="userStore.user?.avatar || ''">
                {{ (userStore.user?.nickname || 'U').slice(0, 1) }}
              </el-avatar>
              <span class="nickname">{{ userStore.user?.nickname }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button link @click="$router.push('/login')">登录</el-button>
          <el-button type="primary" round @click="$router.push('/register')">注册</el-button>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const isActive = (path) => router.currentRoute.value.path === path

const onCommand = (cmd) => {
  if (cmd === 'profile') router.push('/profile')
  if (cmd === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  }
}
</script>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(11, 30, 58, 0.78);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.header-inner {
  max-width: 1100px;
  margin: 0 auto;
  height: 60px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  gap: 24px;
}
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 19px;
  font-weight: 800;
}
.logo-badge {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: linear-gradient(135deg, #1e5aa8, #4da3ff);
  font-size: 18px;
  box-shadow: 0 4px 10px rgba(77, 163, 255, 0.35);
}
.logo-text {
  background: linear-gradient(120deg, #ffffff, #9ecbff 60%, #4da3ff);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: 0.5px;
}
.nav { display: flex; gap: 20px; flex: 1; }
.nav a {
  font-size: 15px;
  color: #b9c7dd;
  padding: 4px 2px;
  border-bottom: 2px solid transparent;
}
.nav a.active,
.nav a:hover { color: #ffffff; border-bottom-color: var(--guat-sky); }
.actions { display: flex; align-items: center; gap: 8px; }
.actions .el-button.is-link { color: #a9c2e4; }
.actions .el-button.is-link:hover { color: #ffffff; }
.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #e6eefb;
  font-size: 14px;
}
.nickname { max-width: 100px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
