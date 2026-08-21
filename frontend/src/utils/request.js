import axios from 'axios'
import { ElMessage } from 'element-plus'

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

// 响应拦截：统一处理 Result 结构
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res
    }
    if (res.code === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      ElMessage.error(res.message || '请先登录')
      window.location.href = '/login'
      return Promise.reject(new Error(res.message || '请先登录'))
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
