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
        window.location.href = '/login'
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
        window.location.href = '/login'
        return Promise.reject(new Error('未登录'))
      }
      const message = response.data?.message || response.data?.msg || JSON.stringify(response.data)
      console.error('API Error:', message)
      // 保留响应结构
      const err = new Error(message) as Error & { response?: unknown }
      err.response = { data: response.data }
      return Promise.reject(err)
    }
    console.error('API Error:', error.message)
    return Promise.reject(error)
  }
)

export default request
