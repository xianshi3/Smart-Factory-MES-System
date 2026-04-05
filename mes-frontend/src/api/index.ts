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
      import('element-plus').then(({ ElMessage }) => {
        ElMessage.error(res.message || `请求失败 (code: ${code})`)
      })
      if (code === 401) {
        window.location.href = '/login'
      }
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  error => {
    let message = '网络错误'
    if (error.response) {
      const status = error.response.status
      const data = error.response.data
      if (status === 0 || status === 504) {
        message = '无法连接服务器，请检查后端服务是否运行'
      } else if (status === 404) {
        message = 'API接口不存在'
      } else if (status === 500) {
        message = data?.message || '服务器内部错误'
      } else if (status === 502) {
        message = '网关错误，服务不可用'
      } else if (data?.message) {
        message = data.message
      }
    } else if (error.message?.includes('Network Error')) {
      message = '网络错误：无法连接到服务器'
    }
    import('element-plus').then(({ ElMessage }) => {
      ElMessage.error(message)
    })
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

export default request
