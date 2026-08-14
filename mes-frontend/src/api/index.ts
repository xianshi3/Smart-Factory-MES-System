import axios from 'axios'
import { getToken, removeToken } from '@/utils/auth'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000
})

request.interceptors.request.use(
  config => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

request.interceptors.response.use(
  response => {
    const res = response.data
    if (!res) {
      return response.data
    }
    const code = res.code ?? res.status
    if (code === undefined || code === null || isNaN(code)) {
      return res
    }
    if (code !== 200) {
      if (code === 401) {
        removeToken()
        if (window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
      }
      // 构造带响应结构的错误，便于业务层读取后端消息
      const err = new Error(res.message || `请求失败 (code: ${code})`) as Error & { response?: unknown }
      err.response = { data: res }
      return Promise.reject(err)
    }
    return res
  },
  error => {
    const response = error.response
    if (response) {
      if (response.status === 401) {
        removeToken()
        if (window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
        return Promise.reject(new Error('未登录'))
      }
      // 后端/代理返回了内容：优先取 JSON 里的 message，否则给出友好提示
      let message: string
      const data = response.data
      if (data && typeof data === 'object') {
        message = data.message || data.msg || `请求失败 (HTTP ${response.status})`
      } else if (typeof data === 'string' && data.trim().startsWith('{')) {
        try {
          const parsed = JSON.parse(data)
          message = parsed?.message || `请求失败 (HTTP ${response.status})`
        } catch {
          message = `服务暂时不可用 (HTTP ${response.status})，请检查后端服务是否已启动`
        }
      } else {
        // HTML 错误页（如 vite 代理 500 / nginx 502），不向用户展示原始内容
        message = `服务暂时不可用 (HTTP ${response.status})，请检查后端服务是否已启动`
      }
      console.error('API Error:', message)
      const err = new Error(message) as Error & { response?: unknown }
      err.response = { data }
      return Promise.reject(err)
    }
    // 网络层错误：后端未启动 / 代理不通 / 跨域 / 超时
    const isTimeout = error.code === 'ECONNABORTED' || /timeout/i.test(error.message || '')
    const message = isTimeout
      ? '请求超时，请检查后端服务是否已启动'
      : '无法连接到服务器，请检查后端服务是否已启动'
    console.error('API Error:', error.message, error.code)
    return Promise.reject(new Error(message))
  }
)

export default request
