import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

/**
 * Router guard before each route navigation
 */
NProgress.configure({ showSpinner: false })

const pathMenuCodeMap: Record<string, string> = {
  '/': 'dashboard',
  '/dashboard': 'dashboard',
  '/workorder': 'workorder',
  '/process': 'process',
  '/quality': 'quality',
  '/device': 'device',
  '/report': 'report',
  '/alarm': 'alarm',
  '/role': 'role',
  '/menu': 'menu',
  '/permission': 'permission'
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue')
  },
  {
    path: '/',
    component: () => import('@/views/layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue')
      },
      {
        path: 'workorder',
        name: 'WorkOrder',
        component: () => import('@/views/workorder/WorkOrderView.vue')
      },
      {
        path: 'process',
        name: 'Process',
        component: () => import('@/views/process/ProcessView.vue')
      },
      {
        path: 'quality',
        name: 'Quality',
        component: () => import('@/views/quality/QualityView.vue')
      },
      {
        path: 'device',
        name: 'Device',
        component: () => import('@/views/device/DeviceView.vue')
      },
      {
        path: 'report',
        name: 'Report',
        component: () => import('@/views/report/ReportView.vue')
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/ProfileView.vue')
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/settings/SettingsView.vue')
      },
      {
        path: 'role',
        name: 'Role',
        component: () => import('@/views/role/RoleView.vue')
      },
      {
        path: 'alarm',
        name: 'Alarm',
        component: () => import('@/views/alarm/AlarmView.vue')
      },
      {
        path: 'menu',
        name: 'Menu',
        component: () => import('@/views/menu/MenuView.vue')
      },
      {
        path: 'permission',
        name: 'Permission',
        component: () => import('@/views/permission/PermissionView.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * Route navigation guard - check authentication before each route
 */
router.beforeEach((to, from, next) => {
  NProgress.start()
  const userStore = useUserStore()
  
  if (to.path === '/login') {
    if (userStore.token) {
      next('/dashboard')
    } else {
      next()
    }
    return
  }
  
  if (!userStore.token) {
    next('/login')
    return
  }
  
  next()
})

/**
 * Route navigation complete handler
 */
router.afterEach(() => {
  NProgress.done()
})

export default router