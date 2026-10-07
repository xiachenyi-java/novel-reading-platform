import apiClient from './index.js'

export const login = (username, password) => {
  return apiClient.post('/users/login', { username, password })
}
// ====== 新增注册接口 ======
export const register = (username, password) => {
  return apiClient.post('/users/register', { username, password })
}