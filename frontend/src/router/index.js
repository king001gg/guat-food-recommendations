import { createRouter, createWebHistory } from 'vue-router'
import { readAuth } from '@/utils/auth'

const routes = [
  { path: '/', name: 'Home', component: () => import('@/views/Home.vue') },
  { path: '/windows/:id', name: 'WindowDetail', component: () => import('@/views/WindowDetail.vue') },
  { path: '/dishes/:id', name: 'DishDetail', component: () => import('@/views/DishDetail.vue') },
  { path: '/submit', name: 'Submit', component: () => import('@/views/Submit.vue'), meta: { requiresAuth: true } },
  { path: '/profile', name: 'Profile', component: () => import('@/views/Profile.vue'), meta: { requiresAuth: true } },
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('@/views/Register.vue') },
  {
    path: '/admin',
    component: () => import('@/components/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/admin/Dashboard.vue') },
      { path: 'windows', name: 'WindowManage', component: () => import('@/views/admin/WindowManage.vue') },
      { path: 'dishes', name: 'DishManage', component: () => import('@/views/admin/DishManage.vue') },
      { path: 'canteens', name: 'CanteenManage', component: () => import('@/views/admin/CanteenManage.vue') },
      { path: 'ratings', name: 'RatingManage', component: () => import('@/views/admin/RatingManage.vue') },
      { path: 'users', name: 'UserManage', component: () => import('@/views/admin/UserManage.vue') },
      { path: 'audit-logs', name: 'AuditLogManage', component: () => import('@/views/admin/AuditLogManage.vue') },
    ],
  },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/NotFound.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach((to) => {
  // readAuth 会顺带清理格式错误/已过期的 Token
  const { token, user } = readAuth()

  if (to.meta.requiresAuth && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin) {
    if (!token) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
    // 角色仅作前端兜底，最终权限仍由后端校验
    if (!user || user.role !== 'ADMIN') {
      return { path: '/' }
    }
  }
  if ((to.path === '/login' || to.path === '/register') && token) {
    return { path: '/' }
  }
  return true
})

export default router
