<template>
  <div class="login-wrapper">
    <div class="particles" ref="particlesContainer"></div>
    <div class="glow glow-1"></div>
    <div class="glow glow-2"></div>

    <div class="login-card">
      <div class="card-header">
        <div class="book-icon">
          <div class="book-cover"></div>
          <div class="book-spine"></div>
          <div class="book-page"></div>
          <div class="book-glow"></div>
        </div>
        <h1>夏辰义的书城</h1>
        <p>写故事的人，终将被故事铭记</p>
      </div>

      <el-form @keyup.enter="handleLogin" class="login-form">
        <el-form-item>
          <el-input
            v-model="username"
            placeholder="用户名"
            prefix-icon="User"
            size="large"
            class="custom-input"
          />
        </el-form-item>

        <el-form-item>
          <el-input
            v-model="password"
            type="password"
            placeholder="密码"
            prefix-icon="Lock"
            size="large"
            show-password
            class="custom-input"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleLogin"
            class="login-btn"
          >
            {{ loading ? '登录中...' : '立即体验' }}
          </el-button>
        </el-form-item>

        <!-- ====== 游客入口 ====== -->
        <div class="guest-entry">
          <el-button
            type="text"
            size="small"
            @click="goGuest"
            class="guest-btn"
          >
            🚶 以游客身份浏览
          </el-button>
        </div>

        <div class="card-footer">
          <span>还没有账号？</span>
          <a href="#" @click.prevent="$router.push('/register')">立即注册</a>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { login } from '../api/user'

const router = useRouter()
const username = ref('')
const password = ref('')
const loading = ref(false)
const particlesContainer = ref(null)
let animationId = null

// ========== 登录方法 ==========
const handleLogin = async () => {
  if (!username.value || !password.value) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  loading.value = true
  try {
    const res = await login(username.value, password.value)
    const body = res.data

    // ① 打印一下方便排查后端到底返回了什么
    console.log('登录接口返回:', body)

    // ② 业务码判断
    if (body.code !== 200) {
      ElMessage.error(body.msg || `登录失败（code: ${body.code}）`)
      return
    }

    // ③ data 判空
    if (!body.data) {
      ElMessage.error('登录返回数据为空，请检查后端接口')
      return
    }

    // ④ 安全解构，兼容不同字段名
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
    router.push('/')
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

// ========== 游客模式 ==========
const goGuest = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('username')
  localStorage.removeItem('role')
  router.push('/')
}

// ========== 粒子背景 ==========
onMounted(() => {
  // 已登录跳转首页
  const token = localStorage.getItem('token')
  if (token) {
    router.push('/')
    return
  }

  const container = particlesContainer.value
  if (!container) return

  const canvas = document.createElement('canvas')
  container.appendChild(canvas)
  const ctx = canvas.getContext('2d')

  const resize = () => {
    canvas.width = container.clientWidth
    canvas.height = container.clientHeight
  }
  resize()
  window.addEventListener('resize', resize)

  const particles = []
  const count = 120
  for (let i = 0; i < count; i++) {
    particles.push({
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height,
      radius: Math.random() * 2 + 1,
      dx: (Math.random() - 0.5) * 0.5,
      dy: (Math.random() - 0.5) * 0.5,
    })
  }

  function animate() {
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    for (const p of particles) {
      p.x += p.dx
      p.y += p.dy
      if (p.x < 0 || p.x > canvas.width) p.dx *= -1
      if (p.y < 0 || p.y > canvas.height) p.dy *= -1
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2)
      ctx.fillStyle = 'rgba(255,255,255,0.6)'
      ctx.fill()
    }
    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x
        const dy = particles[i].y - particles[j].y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < 120) {
          ctx.beginPath()
          ctx.moveTo(particles[i].x, particles[i].y)
          ctx.lineTo(particles[j].x, particles[j].y)
          ctx.strokeStyle = `rgba(255,255,255,${0.15 * (1 - dist / 120)})`
          ctx.lineWidth = 0.5
          ctx.stroke()
        }
      }
    }
    animationId = requestAnimationFrame(animate)
  }
  animate()
})

onUnmounted(() => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
})
</script>

<style scoped>
.login-wrapper {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: linear-gradient(135deg, #0c0e1a 0%, #1a1040 40%, #2d1b69 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  font-family: 'Segoe UI', sans-serif;
}

.particles {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}

.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.3;
  pointer-events: none;
  z-index: 0;
}
.glow-1 {
  width: 400px;
  height: 400px;
  background: #7c3aed;
  top: -100px;
  right: -100px;
  animation: floatGlow 8s ease-in-out infinite alternate;
}
.glow-2 {
  width: 350px;
  height: 350px;
  background: #06b6d4;
  bottom: -80px;
  left: -80px;
  animation: floatGlow 10s ease-in-out infinite alternate-reverse;
}

@keyframes floatGlow {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(40px, 40px) scale(1.2); }
}

.login-card {
  position: relative;
  z-index: 1;
  width: 420px;
  padding: 48px 40px 36px;
  background: rgba(255, 255, 255, 0.07);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-radius: 32px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: 0 40px 80px rgba(0, 0, 0, 0.6);
  animation: cardEntry 0.8s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  opacity: 0;
  transform: translateY(30px);
}

@keyframes cardEntry {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.card-header {
  text-align: center;
  margin-bottom: 32px;
}

.card-header h1 {
  font-size: 28px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 2px;
  margin: 0 0 6px 0;
  background: linear-gradient(135deg, #e0d7ff, #a78bfa);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.card-header p {
  color: rgba(255, 255, 255, 0.5);
  font-size: 14px;
  letter-spacing: 2px;
  margin: 0;
}

.login-form {
  margin-top: 8px;
}
.custom-input :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06) !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  border-radius: 14px !important;
  box-shadow: none !important;
  transition: all 0.3s ease;
}
.custom-input :deep(.el-input__wrapper:hover) {
  border-color: rgba(255, 255, 255, 0.2) !important;
}
.custom-input :deep(.el-input__wrapper.is-focus) {
  border-color: #8b5cf6 !important;
  box-shadow: 0 0 0 4px rgba(139, 92, 246, 0.15) !important;
}
.custom-input :deep(.el-input__inner) {
  color: #fff !important;
  font-size: 15px;
}
.custom-input :deep(.el-input__prefix) {
  color: rgba(255, 255, 255, 0.3) !important;
}
.custom-input :deep(.el-input__suffix) {
  color: rgba(255, 255, 255, 0.3) !important;
}

.login-btn {
  width: 100%;
  height: 50px;
  border-radius: 14px !important;
  background: linear-gradient(135deg, #7c3aed, #4f46e5) !important;
  border: none !important;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
  transition: all 0.3s ease !important;
  box-shadow: 0 8px 24px rgba(79, 70, 229, 0.35) !important;
  margin-top: 4px;
}
.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(79, 70, 229, 0.5) !important;
}
.login-btn:active {
  transform: scale(0.97);
}

.guest-entry {
  text-align: center;
  margin-top: 4px;
}
.guest-btn {
  color: rgba(255, 255, 255, 0.4) !important;
  font-size: 14px;
  transition: color 0.3s;
}
.guest-btn:hover {
  color: rgba(255, 255, 255, 0.7) !important;
}

.card-footer {
  text-align: center;
  margin-top: 12px;
  color: rgba(255, 255, 255, 0.35);
  font-size: 14px;
}
.card-footer a {
  color: #a78bfa;
  text-decoration: none;
  font-weight: 500;
  margin-left: 6px;
  transition: color 0.2s;
}
.card-footer a:hover {
  color: #c4b5fd;
}

.book-icon {
  position: relative;
  width: 64px;
  height: 64px;
  margin: 0 auto 12px;
  display: flex;
  justify-content: center;
  align-items: center;
}
.book-cover {
  position: absolute;
  width: 48px;
  height: 56px;
  background: linear-gradient(145deg, #a78bfa, #6d28d9);
  border-radius: 4px 8px 8px 4px;
  box-shadow: 0 8px 30px rgba(124, 58, 237, 0.4), inset -2px 0 8px rgba(255,255,255,0.1);
  transform: rotate(-4deg);
  z-index: 2;
}
.book-spine {
  position: absolute;
  left: 4px;
  width: 6px;
  height: 56px;
  background: linear-gradient(180deg, #8b5cf6, #4c1d95);
  border-radius: 3px 0 0 3px;
  transform: rotate(-4deg);
  z-index: 3;
  box-shadow: inset -2px 0 6px rgba(0,0,0,0.2);
}
.book-page {
  position: absolute;
  right: -4px;
  width: 8px;
  height: 52px;
  background: linear-gradient(90deg, rgba(255,255,255,0.05), rgba(255,255,255,0.15));
  border-radius: 0 3px 3px 0;
  transform: rotate(-4deg) translateY(2px);
  z-index: 1;
  box-shadow: inset -1px 0 4px rgba(255,255,255,0.05);
}
.book-glow {
  position: absolute;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(124,58,237,0.25), transparent 70%);
  filter: blur(16px);
  z-index: 0;
  animation: glowPulse 3s ease-in-out infinite alternate;
}
@keyframes glowPulse {
  0% { opacity: 0.6; transform: scale(0.9); }
  100% { opacity: 1; transform: scale(1.2); }
}
.book-icon {
  animation: floatBook 4s ease-in-out infinite;
}
@keyframes floatBook {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}
</style>