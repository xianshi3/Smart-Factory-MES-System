import axios from 'axios'

/**
 * Axios request instance configuration
 * 创建axios请求实例
 */
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000
})

export default request