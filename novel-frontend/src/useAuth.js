import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'

export function useAuth() {
  const router = useRouter()

  const isLogin = computed(() => !!localStorage.getItem('token'))
  const isAdmin = computed(() => localStorage.getItem('role') === 'ADMIN')
  const username = computed(() => localStorage.getItem('username') || '读者')

  // 需要登录的操作：没登录就弹框，确认后带 redirect 跳登录
  const requireLogin = (actionName = '该操作') => {
    if (isLogin.value) return true

    ElMessageBox.confirm(
      `登录后才能${actionName}，是否前往登录？`,
      '提示',
      { confirmButtonText: '去登录', cancelButtonText: '继续浏览', type: 'info' }
    ).then(() => {
      router.push({
        path: '/login',
        query: { redirect: router.currentRoute.value.fullPath }
      })
    }).catch(() => {})

    return false
  }

  const logout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
  }

  return { isLogin, isAdmin, username, requireLogin, logout }
}