import apiClient from './index.js'

export const login = (username, password) => {
  return apiClient.post('/users/login', { username, password })
}
// ====== 新增注册接口 ======
export const register = (username, password) => {
  return apiClient.post('/users/register', { username, password })
}

// ====== 退出登录（后端拉黑 token）======
// ★ 为什么要把 token 当参数传进来、还要自己塞请求头？
//   因为 useAuth.logout() 是"先清本地、再通知后端"（退出必须是瞬时的）。
//   axios 的请求拦截器是**异步**跑的 —— 等它去 localStorage 读 token 时，
//   本地凭证已经被删干净了，读出来是空字符串，header 就带不上身份。
//   后端不认识这个请求 → 401 → 拉黑等于没做，而且界面上还看不出来。
export const logout = (token) => {
  return apiClient.post('/users/logout', null, {
    headers: { Authorization: `Bearer ${token}` }
  })
}