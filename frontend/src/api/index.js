import request from '@/utils/request'

// ─── 认证 ───
export const login = (data) => request.post('/api/auth/login', data)
export const register = (data) => request.post('/api/auth/register', data)

// ─── 食堂 ───
export const getCanteens = () => request.get('/api/canteens')
export const createCanteen = (data) => request.post('/api/canteens', data)
export const updateCanteen = (id, data) => request.put(`/api/canteens/${id}`, data)
export const deleteCanteen = (id) => request.delete(`/api/canteens/${id}`)

// ─── 档口 ───
export const rankWindows = (params) => request.get('/api/windows', { params })
export const adminWindows = (params) => request.get('/api/windows/all', { params })
export const getWindow = (id) => request.get(`/api/windows/${id}`)
export const submitWindow = (data) => request.post('/api/windows', data)
export const approveWindow = (id, status) =>
  request.put(`/api/windows/${id}/status`, null, { params: { status } })
export const deleteWindow = (id) => request.delete(`/api/windows/${id}`)

// ─── 菜品 ───
export const rankDishes = (params) => request.get('/api/dishes', { params })
export const adminDishes = (params) => request.get('/api/dishes/all', { params })
export const getDish = (id) => request.get(`/api/dishes/${id}`)
export const submitDish = (data) => request.post('/api/dishes', data)
export const approveDish = (id, status) =>
  request.put(`/api/dishes/${id}/status`, null, { params: { status } })
export const deleteDish = (id) => request.delete(`/api/dishes/${id}`)

// ─── 评分 ───
export const submitRating = (data) => request.post('/api/ratings', data)
export const getRatings = (params) => request.get('/api/ratings', { params })
export const myRatings = (params) => request.get('/api/ratings/mine', { params })
export const adminRatings = (params) => request.get('/api/ratings/all', { params })
export const deleteRating = (id) => request.delete(`/api/ratings/${id}`)
export const adminDeleteRating = (id) => request.delete(`/api/ratings/admin/${id}`)

// ─── 点赞 ───
export const toggleLike = (data) => request.post('/api/likes', data)
export const myLikes = (params) => request.get('/api/likes/mine', { params })

// ─── 收藏 ───
export const toggleFavorite = (data) => request.post('/api/favorites', data)
export const myFavorites = (params) => request.get('/api/favorites/mine', { params })

// ─── 上传 ───
export const uploadImage = (file) => {
  const form = new FormData()
  form.append('file', file)
  return request.post('/api/upload/image', form)
}

// ─── 统计（管理员） ───
export const getOverview = () => request.get('/api/stats/overview')
export const getCanteenStats = () => request.get('/api/stats/canteen')
export const getTopWindows = (limit = 10) =>
  request.get('/api/stats/top-windows', { params: { limit } })
export const getTopDishes = (limit = 10) =>
  request.get('/api/stats/top-dishes', { params: { limit } })

// ─── 用户（管理员） ───
export const getUsers = (params) => request.get('/api/users', { params })
export const updateUserStatus = (id, status) =>
  request.put(`/api/users/${id}/status`, null, { params: { status } })
export const updateUserRole = (id, role) =>
  request.put(`/api/users/${id}/role`, null, { params: { role } })
export const deleteUser = (id) => request.delete(`/api/users/${id}`)
