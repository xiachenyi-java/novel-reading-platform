import axios from 'axios'

const apiClient = axios.create({
  baseURL: '/api',        // 所有请求以 /api 开头，由 Vite 代理转发
  timeout: 10000,
})

// ============================================================
// 请求拦截器：自动带 Token
// ------------------------------------------------------------
// 从 localStorage 读 token，塞进 Authorization 头。
// 后端约定 header 格式：Bearer <token>
//   - el-upload 那种不走 axios 实例的请求，需要自己带（见 WriteNovel.vue）
// ============================================================
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ============================================================
// 响应拦截器：统一处理 401 / 403
// ------------------------------------------------------------
// 401（未登录 / token 失效）：
//   - 清掉本地凭证
//   - 如果"本来登录着"（hadToken），跳登录页并带 redirect
//   - 如果本来就是游客，不跳，交给页面自己处理
//
// 403（已登录但无权限）：
//   - 不清凭证、不跳登录页
//   - 直接 reject，由调用方决定怎么提示
//   - ★ 不能把 403 当 401 处理，否则管理员权限不足会被误踢下线
// ============================================================
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status

    if (status === 401) {
      const hadToken = !!localStorage.getItem('token')

      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('username')
      localStorage.removeItem('role')

      if (hadToken) {
        const current = window.location.pathname + window.location.search
        if (!current.startsWith('/login')) {
          window.location.href = `/login?redirect=${encodeURIComponent(current)}`
        }
      }
    }

    // 403 不需要额外动作，直接 reject 即可

    return Promise.reject(error)
  }
)

export default apiClient