/**
 * Vite Configuration File
 * Vite构建工具配置文件
 */
/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

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
      '/workorder': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        bypass: (req) => {
          if (req.headers.accept?.includes('text/html')) return '/'
        }
      },
      '/process': {
        target: 'http://localhost:8083',
        changeOrigin: true,
        bypass: (req) => {
          if (req.headers.accept?.includes('text/html')) return '/'
        }
      },
      '/quality': {
        target: 'http://localhost:8084',
        changeOrigin: true,
        bypass: (req) => {
          if (req.headers.accept?.includes('text/html')) return '/'
        }
      },
      '/dashboard': {
        target: 'http://localhost:8085',
        changeOrigin: true,
        bypass: (req) => {
          if (req.headers.accept?.includes('text/html')) return '/'
        }
      },
      '/api': {
        target: 'http://localhost:9090',
        changeOrigin: true,
        bypass: (req) => {
          if (req.headers.accept?.includes('text/html')) return '/'
        }
      },
      '/auth': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        bypass: (req) => {
          if (req.headers.accept?.includes('text/html')) return '/'
        }
      },
      '/ai': {
        target: 'http://localhost:8087',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/ai/, ''),
        bypass: (req) => {
          if (req.headers.accept?.includes('text/html')) return '/'
        }
      }
    }
  }
})