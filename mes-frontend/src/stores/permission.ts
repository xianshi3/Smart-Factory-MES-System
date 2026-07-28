import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/api'

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
    return permissionCodes.value.includes(code)
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

  const loadPermissions = async () => {
    try {
      const res = await request({ url: '/auth/role/permissions', method: 'get' })
      permissions.value = res?.data || res || []
    } catch (e) {
      console.error('获取权限失败', e)
    }
  }

  return {
    menus,
    permissions,
    loading,
    menuCodes,
    permissionCodes,
    hasPermission,
    hasMenu,
    loadMenus,
    loadPermissions
  }
})
