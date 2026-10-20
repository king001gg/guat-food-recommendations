<template>
  <el-container class="admin-layout">
    <el-aside width="210px" class="aside">
      <div class="aside-logo">
        <el-icon><Promotion /></el-icon>
        <span>桂航美食管理</span>
      </div>
      <el-menu :default-active="$route.path" router background-color="#1f2d3d" text-color="#bfcbd9" active-text-color="#409eff">
        <el-menu-item index="/admin/dashboard"><el-icon><Odometer /></el-icon><span>数据概览</span></el-menu-item>
        <el-menu-item index="/admin/windows"><el-icon><Shop /></el-icon><span>档口管理</span></el-menu-item>
        <el-menu-item index="/admin/dishes"><el-icon><Food /></el-icon><span>菜品管理</span></el-menu-item>
        <el-menu-item index="/admin/canteens"><el-icon><School /></el-icon><span>食堂管理</span></el-menu-item>
        <el-menu-item index="/admin/ratings"><el-icon><Star /></el-icon><span>评价管理</span></el-menu-item>
        <el-menu-item index="/admin/users"><el-icon><User /></el-icon><span>用户管理</span></el-menu-item>
        <el-menu-item index="/admin/audit-logs"><el-icon><Document /></el-icon><span>审计日志</span></el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <span class="title">管理后台</span>
        <div class="right">
          <el-button link @click="$router.push('/')"><el-icon><House /></el-icon>&nbsp;返回前台</el-button>
          <el-dropdown @command="onCommand">
            <span class="user-chip">
              <el-avatar :size="28" :src="userStore.user?.avatar || ''">{{ (userStore.user?.nickname || 'A').slice(0, 1) }}</el-avatar>
              <span>{{ userStore.user?.nickname }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="security">修改密码</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const onCommand = async (cmd) => {
  if (cmd === 'security') router.push('/profile?tab=security')
  if (cmd === 'logout') {
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<style scoped>
.admin-layout { min-height: 100vh; }
.aside { background: #1f2d3d; }
.aside-logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.aside :deep(.el-menu) { border-right: none; }
.admin-header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.admin-header .title { font-size: 16px; font-weight: 600; }
.admin-header .right { display: flex; align-items: center; gap: 12px; }
.user-chip { display: flex; align-items: center; gap: 8px; cursor: pointer; font-size: 14px; }
.admin-main { background: #f0f2f5; }
</style>
