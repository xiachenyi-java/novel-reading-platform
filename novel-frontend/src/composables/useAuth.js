import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'

/**
 * 登录态 / 权限 / 身份 的统一入口
 *
 *   isLogin / isAdmin / username 都是 computed，读 localStorage
 *   需要"拦一下再跳登录" → requireLogin()
 *   需要"只清理凭证"     → logout()
 *   需要"清理 + 跳登录"   → logoutAndRedirect()
 */
export function useAuth() {
  const router = useRouter()

  const isLogin = computed(() => !!localStorage.getItem('token'))
  const isAdmin = computed(() => localStorage.getItem('role') === 'ADMIN')
  const username = computed(() => localStorage.getItem('username') || '读者')

  /**
   * 需要登录的操作：没登录就弹框，确认后带 redirect 跳登录
   * @returns {boolean} 已登录返回 true（调用方继续执行），未登录返回 false
   */
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

  /**
   * 只清理本地凭证，不做跳转
   * ★ 用定向删除而不是 localStorage.clear()：
   *   以后如果加了主题偏好、语言设置等其他 key，clear() 会一并清掉
   */
  const logout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
  }

  /**
   * 清理凭证 + 跳登录页
   */
  const logoutAndRedirect = () => {
    logout()
    router.push('/login')
  }

  return { isLogin, isAdmin, username, requireLogin, logout, logoutAndRedirect }
}