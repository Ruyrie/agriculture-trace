
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    // 固定开发端口，避免 5173 被占用时自动跳到 5174 导致跨域来源变化。
    host: 'localhost',
    port: 5173,
    strictPort: true
  }
})
