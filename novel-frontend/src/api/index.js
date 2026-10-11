import axios from 'axios'

const apiClient = axios.create({
  baseURL: '/api',        // 所有请求以 /api 开头，由 Vite 代理转发
  timeout: 10000,
})

// 请求拦截器：自动带 Token
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const hadToken = !!localStorage.getItem('token')
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('username')
      localStorage.removeItem('role')

      // 只有"本来登录着"的用户被踢下线才跳登录页
      // 游客访问需要登录的接口，让页面自己处理，不要强跳
      if (hadToken) {
        const current = window.location.pathname + window.location.search
        if (!current.startsWith('/login')) {
          window.location.href = `/login?redirect=${encodeURIComponent(current)}`
        }
      }
    }
    return Promise.reject(error)
  }
)

// 响应拦截器：统一处理 401
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // ★ 改了：保留当前路径，登录后能跳回来，不再无脑跳到 /login
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('username')
      localStorage.removeItem('role')

      const current = window.location.pathname + window.location.search
      if (!current.startsWith('/login')) {
        window.location.href = `/login?redirect=${encodeURIComponent(current)}`
      }
    }
    return Promise.reject(error)
  }
)

export default apiClient