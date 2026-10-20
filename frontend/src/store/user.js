import { defineStore } from 'pinia'
import { ref } from 'vue'

const readStoredUser = () => {
  try {
    return JSON.parse(localStorage.getItem('user') || 'null')
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(readStoredUser())

  const isLogin = () => !!token.value
  const isAdmin = () => user.value?.role === 'ADMIN'

  function setLogin(data) {
    token.value = data.token
    user.value = data.user
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(data.user))
  }

  /** 只清理本地登录态，不发起请求 */
  function clearLocal() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  /** 服务端注销会立即使 Token 失效；请求失败时仍然清理本地状态 */
  async function logout() {
    try {
      const { logout: logoutApi } = await import('@/api')
      await logoutApi()
    } catch {
      // 网络异常或 Token 已过期时无需阻塞退出
    } finally {
      clearLocal()
    }
  }

  return { token, user, isLogin, isAdmin, setLogin, clearLocal, logout }
})
