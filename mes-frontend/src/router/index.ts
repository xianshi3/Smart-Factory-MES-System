import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

/**
 * Router guard before each route navigation
 */
NProgress.configure({ showSpinner: false })

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
  if (to.path !== '/login' && !userStore.token) {
    next('/login')
  } else if (to.path === '/login' && userStore.token) {
    next('/')
  } else {
    next()
  }
})

/**
 * Route navigation complete handler
 */
router.afterEach(() => {
  NProgress.done()
})

export default router