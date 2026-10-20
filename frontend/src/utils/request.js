import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const request = axios.create({
  baseURL: '',
  timeout: 15000,
})

// 请求拦截：携带 Token
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

const urlOf = (config) => config?.url || ''
// 登录/注册接口的 401 是表单错误，不应触发全局跳转
const isAuthRequest = (config) => {
  const url = urlOf(config)
  return url.includes('/auth/login') || url.includes('/auth/register')
}
// 注销接口即使返回 401 也只清理本地状态，不再跳转
const isLogoutRequest = (config) => urlOf(config).includes('/auth/logout')

let redirecting = false

function clearLocalAuth() {
  try {
    useUserStore().clearLocal()
  } catch {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }
}

function handleUnauthorized(message) {
  clearLocalAuth()
  ElMessage.error(message || '登录状态已失效，请重新登录')
  if (redirecting || window.location.pathname === '/login') return
  redirecting = true
  const current = window.location.pathname + window.location.search
  window.location.href = `/login?redirect=${encodeURIComponent(current)}`
}

// 响应拦截：统一处理 Result 结构（后端已按业务码返回真实 HTTP 状态）
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res && res.code === 200) {
      return res
    }
    // 兼容个别未设置 HTTP 状态的旧接口
    if (res && res.code === 401) {
      if (isAuthRequest(response.config)) {
        ElMessage.error(res.message || '用户名或密码错误')
      } else {
        handleUnauthorized(res.message)
      }
      return Promise.reject(new Error(res.message || '请先登录'))
    }
    ElMessage.error(res?.message || '请求失败')
    return Promise.reject(new Error(res?.message || '请求失败'))
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message

    if (status === 401) {
      if (isLogoutRequest(error.config)) {
        clearLocalAuth()
      } else if (isAuthRequest(error.config)) {
        ElMessage.error(message || '用户名或密码错误')
      } else {
        handleUnauthorized(message)
      }
    } else if (status === 403) {
      // 已登录但无权限：只提示，不能清除 Token
      ElMessage.error(message || '没有权限执行此操作')
    } else if (status === 429) {
      const retryAfter = error.response.headers?.['retry-after']
      ElMessage.warning(
        retryAfter
          ? `${message || '操作过于频繁'}（${retryAfter} 秒后可重试）`
          : message || '操作过于频繁，请稍后再试'
      )
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else if (!error.response) {
      ElMessage.error('网络异常，请检查网络连接')
    } else {
      ElMessage.error(message || `请求失败（${status}）`)
    }
    return Promise.reject(error)
  }
)

export default request
