
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  const backendTarget = env.VITE_API_BASE_URL || 'http://localhost:8080'

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    build: {
      // 提高单 chunk 体积告警阈值，避免 element-plus / echarts 这类大库正常拆分后仍持续报警。
      chunkSizeWarningLimit: 1200,
      rollupOptions: {
        output: {
          // 把第三方大库拆成独立 chunk：浏览器可单独缓存，业务代码改动时无需重新下载依赖，
          // 首屏也不再被打成一个巨大的 index.js。echarts 已在 utils/echarts.js 按需引入，
          // 这里再单独成块，进一步缩短首屏关键路径。
          // Vite 8（Rolldown）要求 manualChunks 为函数形式，按依赖路径归类。
          manualChunks(id) {
            if (!id.includes('node_modules')) return
            if (id.includes('echarts') || id.includes('zrender')) return 'echarts'
            if (id.includes('element-plus') || id.includes('@element-plus')) return 'element-plus'
            if (id.includes('/vue/') || id.includes('/vue-router/') || id.includes('@vue')) return 'vue'
          }
        }
      }
    },
    server: {
      // 固定开发端口，避免 5173 被占用时自动跳到 5174 导致跨域来源变化。
      host: 'localhost',
      port: 5173,
      strictPort: true,
      proxy: {
        '/api': {
          target: backendTarget,
          changeOrigin: true
        },
        '/uploads': {
          target: backendTarget,
          changeOrigin: true
        }
      }
    }
  }
})
