import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
// ★ 重命名成 logoutApi，避免和下面这个组合式函数自己的 logout 撞名
import { logout as logoutApi } from '../api/user'

/**
 * 登录态 / 权限 / 身份 的统一入口
 *
 *   isLogin / isAdmin / username 都是 computed，读 localStorage
 *   需要"拦一下再跳登录"                 → requireLogin()
 *   需要"退出（清凭证 + 通知后端拉黑）"   → logout()
 *   需要"退出 + 跳登录页"                 → logoutAndRedirect()
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
   * 退出登录：清本地凭证 + 通知后端拉黑 token（不做跳转）
   *
   * 顺序很重要，别调换：
   *   1) 先把 token 抠下来 —— 下一步就把它删了
   *   2) 清本地凭证（定向删除，不用 localStorage.clear()）
   *   3) 再通知后端 POST /users/logout
   *
   * ★ 为什么不是"先通知后端、成功再清本地"？
   *   退出必须是**瞬时**的，不能因为网慢让用户卡在原地转圈。
   *   本地一清，用户视角就已经退出了；服务端拉黑属于"尽力而为"。
   *
   * ★ 为什么把 token 显式传给接口，而不是靠 axios 拦截器自动带？
   *   拦截器是**异步**跑的，轮到它读 localStorage 时第 2 步已经删完了，
   *   读出来是空串 → header 没有身份 → 后端 401 → 拉黑白做。
   *
   * ★ 拉黑失败为什么不弹框？
   *   这时用户已经退出了，弹个"退出失败"只会让人困惑。
   *   但也不能一声不吭 —— 所以留 console.warn，排查时不至于两眼一抹黑。
   *
   * ★ 顺带一个好处：清完本地再发请求，哪怕后端回 401，
   *   api/index.js 里的拦截器看到 localStorage 没 token 了就不会再跳一次登录页
   *   （它的判断是"本来登录着才跳"），不会和调用方的 router.push('/login') 打架。
   */
  const logout = () => {
    // 1) 先抠 token
    const token = localStorage.getItem('token')

    // 2) 清本地凭证
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('username')
    localStorage.removeItem('role')

    // 3) 通知后端拉黑（不 await：退出不能等网络）
    //    没登录时 token 是 null，直接跳过 —— 「游客浏览」那个按钮也走这里，不该发请求
    if (token) {
      logoutApi(token)
        .then((res) => {
          // ★★ 这里必须看 body.code，不能只看"没抛异常"！
          //    本项目约定：业务异常也返回 **HTTP 200** + body 里 code=500。
          //    也就是说后端明确告诉你"我没拉黑成功"，axios 也照样算成功、
          //    不会进 catch —— 只写 catch 的话这种失败会彻底静默。
          if (res.data?.code !== 200) {
            console.warn(
              `[logout] 后端没能拉黑 token（body.code=${res.data?.code}）：${res.data?.msg || ''}` +
                ' —— 本地已退出，旧 token 要等它自然过期'
            )
          }
        })
        .catch((error) => {
          // 这条管的是"请求根本没成功"：断网、超时、401 等
          console.warn(
            '[logout] 通知后端拉黑 token 失败（本地已退出，旧 token 需等它自然过期）:',
            error?.message || error
          )
        })
    }
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