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
  const defaultMenus: Menu[] = [
    { menuName: '首页', menuCode: 'dashboard', path: '/dashboard', icon: 'Odometer', sort: 1 },
    { menuName: '工单管理', menuCode: 'workorder', path: '/workorder', icon: 'Document', sort: 2 },
    { menuName: '工艺管理', menuCode: 'process', path: '/process', icon: 'Setting', sort: 3 },
    { menuName: '质量管理', menuCode: 'quality', path: '/quality', icon: 'CircleCheck', sort: 4 },
    { menuName: '设备监控', menuCode: 'device', path: '/device', icon: 'Monitor', sort: 5 },
    { menuName: '生产报表', menuCode: 'report', path: '/report', icon: 'DataAnalysis', sort: 6 },
    { menuName: '报警管理', menuCode: 'alarm', path: '/alarm', icon: 'Warning', sort: 7 },
    { menuName: '角色管理', menuCode: 'role', path: '/role', icon: 'Key', sort: 8 }
  ]

  const roleMenus: Record<string, string[]> = {
    ADMIN: ['dashboard', 'workorder', 'process', 'quality', 'device', 'report', 'alarm', 'role'],
    OPERATOR: ['dashboard', 'workorder', 'device', 'report'],
    TECHNICIAN: ['dashboard', 'process', 'quality', 'device'],
    VIEWER: ['dashboard']
  }

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

  const filterMenusByRole = (userRoles: string[]): Menu[] => {
    let allowedCodes: string[] = []
    for (const role of userRoles) {
      const roleCodeList = roleMenus[role.toUpperCase()]
      if (roleCodeList) {
        allowedCodes = [...new Set([...allowedCodes, ...roleCodeList])]
      }
    }
    if (allowedCodes.length === 0) {
      allowedCodes = roleMenus.VIEWER
    }
    return defaultMenus
      .filter(m => allowedCodes.includes(m.menuCode))
      .sort((a, b) => a.sort - b.sort)
  }

  const loadMenus = async (userRoles?: string[]) => {
    if (userRoles && userRoles.length > 0) {
      menus.value = filterMenusByRole(userRoles)
    } else {
      menus.value = defaultMenus
    }
    loading.value = false
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