<template>
  <div class="register-page">
    <!-- ===== 顶部品牌栏 ===== -->
    <header class="register-header">
      <div class="header-inner">
        <div class="brand" @click="$router.push('/')">
          <span class="brand-icon">📚</span>
          <span class="brand-name">草莓</span>
        </div>
        <div class="header-right">
          <span class="tip">已有账号？</span>
          <a class="link" @click.prevent="$router.push('/login')">立即登录</a>
        </div>
      </div>
    </header>

    <!-- ===== 主体 ===== -->
    <main class="register-main">
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
            <div class="figure">✍️</div>
          </div>
          <h2 class="slogan">加入我们，开始你的阅读之旅</h2>
          <p class="sub-slogan">收藏好书 · 分享书单 · 找到同好</p>
        </div>

        <!-- 右：注册卡片 -->
        <div class="register-card">
          <h1 class="card-title">注册新账号</h1>
          <p class="card-subtitle">只需几秒，即可开启你的书城之旅</p>

          <el-form
            @keyup.enter="handleRegister"
            class="register-form"
            label-position="top"
          >
            <el-form-item>
              <el-input
                v-model="username"
                placeholder="用户名（3-20位）"
                size="large"
                class="custom-input"
                maxlength="20"
                show-word-limit
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
                placeholder="密码（至少6位）"
                size="large"
                show-password
                class="custom-input"
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item>
              <el-input
                v-model="confirmPassword"
                type="password"
                placeholder="确认密码"
                size="large"
                show-password
                class="custom-input"
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <!-- 密码强度提示（可选，实时显示） -->
            <div v-if="password" class="strength-bar">
              <div class="strength-label">密码强度：</div>
              <div class="strength-track">
                <div
                  class="strength-fill"
                  :class="strengthClass"
                  :style="{ width: strengthPercent + '%' }"
                ></div>
              </div>
              <span class="strength-text" :class="strengthClass">{{ strengthText }}</span>
            </div>

            <div class="agreement">
              <el-checkbox v-model="agreed" size="small">
                我已阅读并同意
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

            <el-button
              type="primary"
              size="large"
              :loading="loading"
              @click="handleRegister"
              class="submit-btn"
            >
              {{ loading ? '注册中...' : '立即注册' }}
            </el-button>

            <div class="form-footer">
              <span>已有账号？</span>
              <a class="link" @click.prevent="$router.push('/login')">立即登录</a>
            </div>
          </el-form>
        </div>
      </div>
    </main>

    <!-- ===== 底部 Footer（已抽成组件）===== -->
    <SiteFooter />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { register } from '../api/user'
import SiteFooter from '../components/SiteFooter.vue'

const router = useRouter()

const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const agreed = ref(false)
const loading = ref(false)

// ===== 密码强度计算 =====
const strengthScore = computed(() => {
  const pwd = password.value
  if (!pwd) return 0
  let score = 0
  if (pwd.length >= 6) score++
  if (pwd.length >= 10) score++
  if (/[a-z]/.test(pwd) && /[A-Z]/.test(pwd)) score++
  if (/\d/.test(pwd)) score++
  if (/[^a-zA-Z0-9]/.test(pwd)) score++
  return Math.min(score, 4)
})

const strengthPercent = computed(() => {
  if (!password.value) return 0
  return (strengthScore.value / 4) * 100
})

const strengthClass = computed(() => {
  const s = strengthScore.value
  if (s <= 1) return 'weak'
  if (s === 2) return 'medium'
  if (s === 3) return 'good'
  return 'strong'
})

const strengthText = computed(() => {
  const s = strengthScore.value
  if (s <= 1) return '弱'
  if (s === 2) return '中'
  if (s === 3) return '良好'
  return '强'
})

// ===== 注册 =====
const handleRegister = async () => {
  if (!username.value || username.value.length < 3 || username.value.length > 20) {
    ElMessage.warning('用户名长度需为 3-20 位')
    return
  }
  if (!password.value || password.value.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  if (password.value !== confirmPassword.value) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  if (!agreed.value) {
    ElMessage.warning('请先阅读并同意用户服务协议')
    return
  }

  loading.value = true
  try {
    const res = await register(username.value, password.value)
    if (res.data.code === 200) {
      ElMessage.success('注册成功！请登录')
      router.push('/login')
    } else {
      ElMessage.error(res.data.msg || '注册失败')
    }
  } catch (error) {
    console.error('注册失败:', error)
    const msg = error.response?.data?.msg || '注册失败，请检查网络'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
* { box-sizing: border-box; }

.register-page {
  min-height: 100vh;
  background: #faf9f7;
  display: flex;
  flex-direction: column;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* ===== 顶部品牌栏 ===== */
.register-header {
  background: #fff;
  border-bottom: 1px solid #ececea;
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
  background: linear-gradient(135deg, #6d4bd0, #6d4bd0);
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
  color: #6d4bd0;
  text-decoration: none;
  cursor: pointer;
  transition: color 0.2s;
}
.link:hover { color: #6d4bd0; }

/* ===== 主体 ===== */
.register-main {
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

/* ===== 左侧插画 ===== */
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
  background: linear-gradient(145deg, #a78bfa, #5b3cb8);
  border-radius: 8px 14px 14px 8px;
  box-shadow: 0 20px 40px rgba(109, 75, 208, 0.35), inset -3px 0 12px rgba(255,255,255,0.15);
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
  background: linear-gradient(180deg, #8b5cf6, #4a2f9e);
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
  color: #4a2f9e;
  letter-spacing: 2px;
  margin: 30px 0 10px;
}
.sub-slogan {
  font-size: 14px;
  color: #888;
  letter-spacing: 1px;
}

/* ===== 右侧注册卡 ===== */
.register-card {
  flex-shrink: 0;
  width: 400px;
  background: #fff;
  border-radius: 12px;
  padding: 36px 40px 28px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08);
  border: 1px solid #ececea;
}
.card-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 6px;
  text-align: center;
}
.card-subtitle {
  font-size: 13px;
  color: #999;
  text-align: center;
  margin: 0 0 24px;
}

/* 输入框 */
.register-form :deep(.el-form-item) {
  margin-bottom: 16px;
}
.custom-input :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #ececea inset;
  padding: 4px 12px;
  transition: all 0.2s;
}
.custom-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #5b6b8c inset;
}
.custom-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #6d4bd0 inset, 0 0 0 3px rgba(109, 75, 208, 0.1);
}
.custom-input :deep(.el-input__prefix) {
  color: #9ca3af;
}
.custom-input :deep(.el-input__inner) {
  height: 40px;
  font-size: 14px;
}

/* 密码强度 */
.strength-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: -6px 0 14px;
  font-size: 12px;
}
.strength-label {
  color: #999;
  flex-shrink: 0;
}
.strength-track {
  flex: 1;
  height: 4px;
  background: #ececea;
  border-radius: 2px;
  overflow: hidden;
}
.strength-fill {
  height: 100%;
  transition: width 0.3s, background 0.3s;
  border-radius: 2px;
}
.strength-fill.weak { background: #ef4444; }
.strength-fill.medium { background: #b0702a; }
.strength-fill.good { background: #10b981; }
.strength-fill.strong { background: #6d4bd0; }
.strength-text { flex-shrink: 0; font-weight: 500; }
.strength-text.weak { color: #ef4444; }
.strength-text.medium { color: #b0702a; }
.strength-text.good { color: #10b981; }
.strength-text.strong { color: #6d4bd0; }

/* 协议 */
.agreement {
  margin: 6px 0 20px;
}
.agreement :deep(.el-checkbox__label) {
  font-size: 12px;
  color: #999;
}
.agreement .link {
  font-size: 12px;
}

/* 提交按钮 */
.submit-btn {
  width: 100%;
  height: 48px;
  border-radius: 8px !important;
  background: linear-gradient(135deg, #6d4bd0, #6d4bd0) !important;
  border: none !important;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  box-shadow: 0 6px 20px rgba(109, 75, 208, 0.3) !important;
  transition: all 0.2s !important;
}
.submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 24px rgba(109, 75, 208, 0.4) !important;
}
.submit-btn:active {
  transform: translateY(0);
}

/* 底部 */
.form-footer {
  text-align: center;
  margin-top: 18px;
  font-size: 14px;
  color: #999;
}
.form-footer .link {
  margin-left: 6px;
  font-weight: 500;
}

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .illustration { display: none; }
  .main-inner { justify-content: center; gap: 0; }
}
@media (max-width: 480px) {
  .register-card {
    width: 100%;
    padding: 28px 24px 20px;
  }
  .register-main { padding: 20px 16px; }
}
</style>