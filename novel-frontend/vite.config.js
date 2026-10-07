import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// ========== 后端地址：从环境变量读，不写死 ==========
// 优先级：命令行环境变量 > .env 文件 > 默认值
//
//   npm run dev                                       → 8080（IDEA 里跑的后端）
//   VITE_API_TARGET=http://localhost:8081 npm run dev  → 8081（Docker 里的后端）
//
// ★ 这个 proxy 只在 dev（Vite dev server）生效。
//   生产的 dist/ 由 Nginx 反代 /api，走的不是这里。
const DEFAULT_API_TARGET = 'http://localhost:8080'

export default defineConfig(({ mode }) => {
  // 第三个参数 '' 表示不过滤前缀，这样 .env 文件里的 key 也能读到
  const env = loadEnv(mode, process.cwd(), '')
  const apiTarget = env.VITE_API_TARGET || DEFAULT_API_TARGET

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    server: {
      proxy: {
        // 浏览器请求 /api/xxx → 转发到 {apiTarget}/xxx（rewrite 去掉 /api 前缀）
        '/api': {
          target: apiTarget,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, '')
        }
      }
    }
  }
})