/** 解析 JWT payload（仅用于本地判断是否过期，不校验签名） */
export function parseTokenPayload(token) {
  if (!token || typeof token !== 'string') return null
  const parts = token.split('.')
  if (parts.length !== 3) return null
  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')
    const binary = atob(padded)
    const json = decodeURIComponent(
      Array.from(binary)
        .map((c) => `%${c.charCodeAt(0).toString(16).padStart(2, '0')}`)
        .join('')
    )
    return JSON.parse(json)
  } catch {
    return null
  }
}

/** Token 缺失、格式错误或已过期时返回 true */
export function isTokenExpired(token) {
  const payload = parseTokenPayload(token)
  if (!payload) return true
  if (!payload.exp) return false
  return payload.exp * 1000 <= Date.now()
}

/** 读取本地登录态，顺带清理无效/过期 Token */
export function readAuth() {
  const token = localStorage.getItem('token') || ''
  let user = null
  try {
    user = JSON.parse(localStorage.getItem('user') || 'null')
  } catch {
    user = null
  }
  if (!token || isTokenExpired(token)) {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    return { token: '', user: null }
  }
  return { token, user }
}
