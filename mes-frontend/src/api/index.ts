import axios from 'axios'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000
})

request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
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
        window.location.href = '/login'
      }
      return Promise.reject(new Error(res.message || `请求失败 (code: ${code})`))
    }
    return res
  },
  error => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

export default request
