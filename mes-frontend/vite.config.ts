/**
 * Vite Configuration File
 * Vite构建工具配置文件
 */
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
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
      '/process/template': {
        target: 'http://localhost:8083',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/process/, ''),
        bypass: (req) => {
          if (req.headers.accept?.includes('text/html')) return '/'
        }
      },
      '/process/parameter': {
        target: 'http://localhost:8083',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/process/, ''),
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
      '/api': {
        target: 'http://localhost:8085',
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
      }
    }
  }
})