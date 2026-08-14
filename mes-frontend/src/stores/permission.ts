import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/api'
import { getUserPermissions } from '@/api/auth'
import { useUserStore } from '@/stores/user'

interface Menu {
  id?: number
  menuName: string
  menuCode: string
  path: string
  icon?: string
  sort: number
  children?: Menu[]
}

interface Permission {
  id?: number
  permissionName: string
  permissionCode: string
  permissionType: string
}

export const usePermissionStore = defineStore('permission', () => {
  const menus = ref<Menu[]>([])
  const permissions = ref<Permission[]>([])
  const codes = ref<string[]>([])
  const loaded = ref(false)
  const loading = ref(false)

  const menuCodes = computed(() => {
    if (!menus.value || !Array.isArray(menus.value)) return []
    return menus.value.map(m => m.menuCode)
  })

  const permissionCodes = computed(() => {
    if (!permissions.value || !Array.isArray(permissions.value)) return []
    return permissions.value.map(p => p.permissionCode)
  })

  const hasPermission = (code: string): boolean => {
    if (!code) return true
    const userStore = useUserStore()
    if (userStore.roles.includes('ADMIN')) return true
    return codes.value.includes(code)
  }

  const hasMenu = (code: string): boolean => {
    return menuCodes.value.includes(code)
  }

  const loadMenus = async () => {
    loading.value = true
    try {
      const res = await request({ url: '/auth/menu/user', method: 'get' })
      menus.value = res?.data || []
    } catch {
      console.error('获取菜单失败')
      menus.value = []
    } finally {
      loading.value = false
    }
  }

  /**
   * 加载当前用户权限码（v-permission 指令 / 路由守卫使用）
   */
  const loadCurrentPermissions = async () => {
    if (loaded.value) return
    loading.value = true
    try {
      const res = await getUserPermissions()
      codes.value = res?.data || []
      loaded.value = true
    } catch (e) {
      // 首次加载失败不置 loaded，下次路由跳转会重试，避免权限码永久缺失
      console.error('获取当前用户权限失败', e)
      codes.value = []
    } finally {
      loading.value = false
    }
  }

  const loadPermissions = async () => {
    try {
      const res = await request({ url: '/auth/role/permissions', method: 'get' })
      permissions.value = res?.data || res || []
    } catch (e) {
      console.error('获取权限失败', e)
    }
  }

  const reset = () => {
    codes.value = []
    loaded.value = false
    menus.value = []
    permissions.value = []
  }

  return {
    menus,
    permissions,
    codes,
    loaded,
    loading,
    menuCodes,
    permissionCodes,
    hasPermission,
    hasMenu,
    loadMenus,
    loadCurrentPermissions,
    loadPermissions,
    reset
  }
})