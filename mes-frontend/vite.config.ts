/**
 * Vite Configuration File
 * Vite构建工具配置文件
 */
/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import type { ProxyOptions } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

/** 代理目标不可达（后端未启动）时仅告警，防止 ECONNRESET 导致 dev server 崩溃退出 */
function safeProxy(target: string, extra?: Partial<ProxyOptions>): ProxyOptions {
  return {
    target,
    changeOrigin: true,
    bypass: (req) => {
      if (req.headers.accept?.includes('text/html')) return '/'
    },
    configure: (proxy) => {
      proxy.on('error', (err) => {
        console.warn(`[vite-proxy] ${target} 不可达: ${err.message}（后端服务未启动？）`)
      })
    },
    ...extra,
  }
}

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'node',
    include: ['src/**/*.{test,spec}.{ts,js}'],
    coverage: {
      reporter: ['text', 'lcov'],
    },
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor-vue': ['vue', 'vue-router', 'pinia'],
          'vendor-ui': ['element-plus', '@element-plus/icons-vue'],
          'vendor-chart': ['echarts', 'vue-echarts'],
          'vendor-three': ['three'],
        },
      },
    },
    chunkSizeWarningLimit: 800,
  },
  server: {
    port: 3000,
    proxy: {
      '/workorder': safeProxy('http://localhost:8082'),
      '/process': safeProxy('http://localhost:8083'),
      '/quality': safeProxy('http://localhost:8084'),
      '/dashboard': safeProxy('http://localhost:8085'),
      '/alarm': safeProxy('http://localhost:8085'),
      '/api': safeProxy('http://localhost:9090'),
      '/auth': safeProxy('http://localhost:8081'),
      '/ai': safeProxy('http://localhost:8087', {
        rewrite: (path) => path.replace(/^\/ai/, ''),
      }),
    }
  }
})
