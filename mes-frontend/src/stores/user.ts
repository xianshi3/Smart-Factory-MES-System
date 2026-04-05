import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { login as loginApi, getUserInfo as getUserInfoApi } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken() || '')
  const userInfo = ref<any>(null)
  const roles = ref<string[]>([])

  const isLoggedIn = computed(() => !!token.value)

  /**
 * User login function
 */
async function login(username: string, password: string) {
    const res = await loginApi({ username, password })
    token.value = res.data.token
    setToken(res.data.token)
    return res.data
  }

  /**
 * Get current user information
 */
async function getUserInfo() {
    if (!token.value) return
    const res = await getUserInfoApi()
    userInfo.value = res.data
    roles.value = [res.data.role]
    return res.data
  }

  /**
 * User logout function
 */
function logout() {
    token.value = ''
    userInfo.value = null
    roles.value = []
    removeToken()
  }

  return { token, userInfo, roles, isLoggedIn, login, getUserInfo, logout }
})