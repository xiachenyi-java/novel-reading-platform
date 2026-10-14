<template>
  <div class="login-page">
    <!-- ===== 顶部品牌栏 ===== -->
    <header class="login-header">
      <div class="header-inner">
        <div class="brand" @click="$router.push('/')">
          <span class="brand-icon">📚</span>
          <span class="brand-name">夏辰义的书城</span>
        </div>
        <div class="header-right">
          <span class="tip">还没有账号？</span>
          <a class="link" @click.prevent="$router.push('/register')">免费注册</a>
        </div>
      </div>
    </header>

    <!-- ===== 主体 ===== -->
    <main class="login-main">
      <div class="main-inner">
        <!-- 左：插画 + 标语 -->
        <div class="illustration">
          <div class="scene">
            <div class="cloud cloud-1"></div>
            <div class="cloud cloud-2"></div>
            <div class="cloud cloud-3"></div>
            <div class="book">
              <div class="book-cover"></div>
              <div class="book-page"></div>
              <div class="book-spine"></div>
            </div>
            <div class="figure">📖</div>
          </div>
          <h2 class="slogan">阅读，让生活更美好！</h2>
          <p class="sub-slogan">发现好书 · 分享书单 · 记录你的阅读时光</p>
        </div>

        <!-- 右：登录卡片 -->
        <div class="login-card">
          <!-- Tab 切换 -->
          <div class="tabs">
            <div
              class="tab"
              :class="{ active: activeTab === 'account' }"
              @click="activeTab = 'account'"
            >
              账号登录
            </div>
            <div
              class="tab"
              :class="{ active: activeTab === 'phone' }"
              @click="activeTab = 'phone'"
            >
              手机登录
            </div>
          </div>

          <!-- 账号登录 -->
          <el-form
            v-if="activeTab === 'account'"
            @keyup.enter="handleLogin"
            class="login-form"
          >
            <el-form-item>
              <el-input
                v-model="username"
                placeholder="手机 / 邮箱 / 用户名"
                size="large"
                class="custom-input"
              >
                <template #prefix>
                  <el-icon><User /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item>
              <el-input
                v-model="password"
                type="password"
                placeholder="密码"
                size="large"
                show-password
                class="custom-input"
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <div class="form-row">
              <el-checkbox v-model="remember" class="remember">自动登录</el-checkbox>
              <a class="link forget" @click.prevent="handleForget">忘记密码？</a>
            </div>

            <el-button
              type="primary"
              size="large"
              :loading="loading"
              @click="handleLogin"
              class="submit-btn"
            >
              {{ loading ? '登录中...' : '登 录' }}
            </el-button>

            <div class="form-footer">
              <span>还没有账号？</span>
              <a class="link" @click.prevent="$router.push('/register')">免费注册</a>
            </div>

            <div class="agreement">
              <el-checkbox v-model="agreed" size="small">
                登录即代表同意
                <router-link
                  class="link"
                  to="/terms"
                  target="_blank"
                  @click.stop
                >《用户服务协议》</router-link>
                和
                <router-link
                  class="link"
                  to="/privacy"
                  target="_blank"
                  @click.stop
                >《隐私政策》</router-link>
              </el-checkbox>
            </div>
          </el-form>

          <!-- 手机登录（占位） -->
          <div v-else class="phone-placeholder">
            <div class="placeholder-icon">📱</div>
            <p>手机验证码登录功能开发中</p>
            <el-button type="text" @click="activeTab = 'account'">返回账号登录</el-button>
          </div>

          <!-- 游客入口 -->
          <div class="guest-entry">
            <el-button type="text" size="small" @click="goGuest" class="guest-btn">
              🚶 以游客身份浏览
            </el-button>
          </div>
        </div>
      </div>
    </main>

    <SiteFooter />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { login } from '../api/user'
import SiteFooter from '../components/SiteFooter.vue'
import { useAuth } from '../composables/useAuth'

const route = useRoute()
const router = useRouter()

// ★ 从 useAuth 拿 isLogin / logout
const { isLogin, logout } = useAuth()

const username = ref('')
const password = ref('')
const remember = ref(true)
const agreed = ref(false)
const loading = ref(false)
const activeTab = ref('account')

// ========== 登录 ==========
const handleLogin = async () => {
  if (!username.value || !password.value) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  if (!agreed.value) {
    ElMessage.warning('请先阅读并同意用户服务协议')
    return
  }

  loading.value = true
  try {
    const res = await login(username.value, password.value)
    const body = res.data

    if (body.code !== 200) {
      ElMessage.error(body.msg || `登录失败（code: ${body.code}）`)
      return
    }
    if (!body.data) {
      ElMessage.error('登录返回数据为空')
      return
    }

    const token = body.data.token || body.data.accessToken
    const refreshToken = body.data.refreshToken || ''
    const userInfo = body.data.userInfo || body.data.user || {}

    if (!token) {
      ElMessage.error('登录失败：未获取到 token')
      return
    }

    localStorage.setItem('token', token)
    if (refreshToken) localStorage.setItem('refreshToken', refreshToken)
    localStorage.setItem('username', userInfo.username || username.value)
    localStorage.setItem('role', userInfo.role || 'USER')

    ElMessage.success(`欢迎回来，${userInfo.username || username.value}！`)

    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (error) {
    console.error('登录失败:', error)
    const msg =
      error.response?.data?.msg ||
      error.response?.data?.message ||
      error.message ||
      '登录失败，请检查网络或账号密码'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

// ========== 忘记密码 ==========
const handleForget = () => {
  ElMessage.info('请联系管理员重置密码')
}

// ========== 游客 ==========
const goGuest = () => {
  // ★ 4 行 removeItem → 1 行 logout()
  logout()
  router.push('/')
}

// ========== 已登录直接跳 ==========
onMounted(() => {
  if (isLogin.value) {
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  }
})
</script>

<style scoped>
* { box-sizing: border-box; }

.login-page {
  min-height: 100vh;
  background: #f7f8fa;
  display: flex;
  flex-direction: column;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.login-header {
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  height: 60px;
  flex-shrink: 0;
}
.header-inner {
  max-width: 1200px;
  height: 100%;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}
.brand-icon { font-size: 24px; }
.brand-name {
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #7c3aed, #4f46e5);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}
.header-right .tip { color: #999; }
.link {
  color: #7c3aed;
  text-decoration: none;
  cursor: pointer;
  transition: color 0.2s;
}
.link:hover { color: #4f46e5; }

.login-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
}
.main-inner {
  width: 100%;
  max-width: 1080px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 60px;
}

.illustration {
  flex: 1;
  text-align: center;
  padding: 20px;
}
.scene {
  position: relative;
  height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.cloud {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 30%, #e0e7ff, #c7d2fe);
  filter: blur(1px);
  opacity: 0.85;
}
.cloud-1 { width: 180px; height: 90px; left: 10%; top: 20%; }
.cloud-2 { width: 220px; height: 110px; right: 5%; top: 55%; }
.cloud-3 { width: 140px; height: 70px; left: 20%; bottom: 8%; }

.book {
  position: relative;
  width: 140px;
  height: 170px;
  z-index: 2;
  animation: floatBook 4s ease-in-out infinite;
}
.book-cover {
  position: absolute;
  inset: 0;
  background: linear-gradient(145deg, #a78bfa, #6d28d9);
  border-radius: 8px 14px 14px 8px;
  box-shadow: 0 20px 40px rgba(124, 58, 237, 0.35), inset -3px 0 12px rgba(255,255,255,0.15);
  transform: rotate(-6deg);
}
.book-page {
  position: absolute;
  right: -8px;
  top: 6px;
  bottom: 6px;
  width: 14px;
  background: linear-gradient(90deg, rgba(255,255,255,0.1), rgba(255,255,255,0.4));
  border-radius: 0 6px 6px 0;
  transform: rotate(-6deg);
}
.book-spine {
  position: absolute;
  left: 6px;
  top: 6px;
  bottom: 6px;
  width: 10px;
  background: linear-gradient(180deg, #8b5cf6, #4c1d95);
  border-radius: 4px;
  transform: rotate(-6deg);
  box-shadow: inset -2px 0 8px rgba(0,0,0,0.2);
}
.figure {
  position: absolute;
  right: 20%;
  top: 18%;
  font-size: 56px;
  z-index: 3;
  animation: floatFigure 4.5s ease-in-out infinite;
}
@keyframes floatBook {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}
@keyframes floatFigure {
  0%, 100% { transform: translateY(0) rotate(-4deg); }
  50% { transform: translateY(-10px) rotate(4deg); }
}

.slogan {
  font-size: 26px;
  font-weight: 700;
  color: #4c1d95;
  letter-spacing: 2px;
  margin: 30px 0 10px;
}
.sub-slogan {
  font-size: 14px;
  color: #888;
  letter-spacing: 1px;
}

.login-card {
  flex-shrink: 0;
  width: 400px;
  background: #fff;
  border-radius: 12px;
  padding: 36px 40px 28px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08);
  border: 1px solid #f0f0f0;
}

.tabs {
  display: flex;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 28px;
}
.tab {
  flex: 1;
  text-align: center;
  padding: 12px 0;
  font-size: 16px;
  color: #666;
  cursor: pointer;
  position: relative;
  transition: color 0.2s;
}
.tab:hover { color: #7c3aed; }
.tab.active {
  color: #1a1a2e;
  font-weight: 600;
}
.tab.active::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: -1px;
  transform: translateX(-50%);
  width: 40px;
  height: 3px;
  background: linear-gradient(135deg, #7c3aed, #4f46e5);
  border-radius: 2px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 18px;
}
.custom-input :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #e5e7eb inset;
  padding: 4px 12px;
  transition: all 0.2s;
}
.custom-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #c4b5fd inset;
}
.custom-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #7c3aed inset, 0 0 0 3px rgba(124, 58, 237, 0.1);
}
.custom-input :deep(.el-input__prefix) {
  color: #9ca3af;
}
.custom-input :deep(.el-input__inner) {
  height: 40px;
  font-size: 14px;
}

.form-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  font-size: 13px;
}
.remember :deep(.el-checkbox__label) {
  font-size: 13px;
  color: #666;
}
.forget { font-size: 13px; }

.submit-btn {
  width: 100%;
  height: 48px;
  border-radius: 8px !important;
  background: linear-gradient(135deg, #7c3aed, #4f46e5) !important;
  border: none !important;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  box-shadow: 0 6px 20px rgba(79, 70, 229, 0.3) !important;
  transition: all 0.2s !important;
}
.submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 24px rgba(79, 70, 229, 0.4) !important;
}
.submit-btn:active {
  transform: translateY(0);
}

.form-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #999;
}
.form-footer .link {
  margin-left: 6px;
  font-weight: 500;
}

.agreement {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f5f5f5;
}
.agreement :deep(.el-checkbox__label) {
  font-size: 12px;
  color: #999;
}
.agreement .link {
  font-size: 12px;
}

.phone-placeholder {
  text-align: center;
  padding: 40px 0;
  color: #999;
}
.placeholder-icon {
  font-size: 56px;
  opacity: 0.6;
  margin-bottom: 16px;
}
.phone-placeholder p {
  margin-bottom: 12px;
  font-size: 14px;
}

.guest-entry {
  text-align: center;
  margin-top: 8px;
  padding-top: 8px;
}
.guest-btn {
  color: #999 !important;
  font-size: 13px;
}
.guest-btn:hover {
  color: #7c3aed !important;
}

@media (max-width: 900px) {
  .illustration { display: none; }
  .main-inner { justify-content: center; gap: 0; }
}
@media (max-width: 480px) {
  .login-card {
    width: 100%;
    padding: 28px 24px 20px;
  }
  .login-main { padding: 20px 16px; }
}
</style>