<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="aside-container">
      <div class="logo" :class="{ collapsed: isCollapse }" @click="toggleCollapse">
        <div class="logo-icon">
          <el-icon size="24"><Cpu /></el-icon>
        </div>
        <transition name="fade">
          <div class="logo-text" v-show="!isCollapse">
            <span class="logo-title">Smart MES</span>
            <span class="logo-subtitle">智能工厂</span>
          </div>
        </transition>
        <div class="collapse-btn" :class="{ collapsed: isCollapse }">
          <el-icon><ArrowLeft v-if="!isCollapse" /><ArrowRight v-else /></el-icon>
        </div>
      </div>
      
      <el-menu 
        :default-active="activeMenu" 
        class="el-menu-vertical" 
        router
        :ellipsis="false"
        :collapse="isCollapse"
        :collapse-transition="true"
      >
        <template v-for="group in menuGroups" :key="group.title">
          <el-sub-menu v-if="!isCollapse" :index="group.title">
            <template #title>
              <el-icon><component :is="group.icon" /></el-icon>
              <span>{{ group.title }}</span>
            </template>
            <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
              <el-icon><component :is="item.icon" /></el-icon>
              <template #title>{{ item.title }}</template>
            </el-menu-item>
          </el-sub-menu>
          <template v-else>
            <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
              <el-icon><component :is="item.icon" /></el-icon>
              <template #title>{{ item.title }}</template>
            </el-menu-item>
          </template>
        </template>
      </el-menu>
    </el-aside>
    
    <el-container class="main-container">
      <el-header>
        <div class="header-left">
          <div class="page-title">{{ pageTitle }}</div>
          <div class="breadcrumb">{{ breadcrumb }}</div>
        </div>
        
        <div class="header-right">
          <div class="header-actions">
            <div class="action-btn" @click="themeStore.toggleTheme()" :title="themeStore.isDark ? '切换到亮色模式' : '切换到暗色模式'">
              <el-icon><Sunny v-if="themeStore.isDark" /><Moon v-else /></el-icon>
            </div>
          </div>
          
          <el-dropdown @command="handleCommand" trigger="click">
            <div class="user-info">
              <div class="user-avatar">
                <el-icon><User /></el-icon>
              </div>
              <span class="user-name">{{ userStore.user?.username || '管理员' }}</span>
              <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu class="user-dropdown">
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item command="settings">
                  <el-icon><Setting /></el-icon>
                  系统设置
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <el-main>
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { usePermissionStore } from '@/stores/permission'
import { Cpu, User, Sunny, Moon, ArrowDown, ArrowLeft, ArrowRight, Setting, SwitchButton } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()
const permissionStore = usePermissionStore()

const isCollapse = ref(false)

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

interface MenuItem {
  path: string
  title: string
  icon: string
}

interface MenuGroup {
  title: string
  icon: string
  items: MenuItem[]
}

const menuGroups = computed<MenuGroup[]>(() => {
  return [
    { 
      title: '生产监控', 
      icon: 'Odometer',
      items: [
        { path: '/dashboard', title: '工作台', icon: 'Odometer' },
        { path: '/device', title: '设备监控', icon: 'Monitor' },
        { path: '/alarm', title: '报警中心', icon: 'Warning' }
      ]
    },
    { 
      title: '生产管理', 
      icon: 'Document',
      items: [
        { path: '/workorder', title: '工单管理', icon: 'Document' },
        { path: '/process', title: '工艺管理', icon: 'Setting' },
        { path: '/quality', title: '质量管理', icon: 'CircleCheck' },
        { path: '/report', title: '生产报表', icon: 'DataAnalysis' }
      ]
    },
    { 
      title: '基础数据', 
      icon: 'Grid',
      items: [
        { path: '/production-line', title: '生产线', icon: 'Connection' },
        { path: '/workstation', title: '工位管理', icon: 'Location' }
      ]
    },
    { 
      title: '系统管理', 
      icon: 'Setting',
      items: [
        { path: '/role', title: '角色管理', icon: 'Key' },
        { path: '/menu', title: '菜单管理', icon: 'Menu' },
        { path: '/permission', title: '权限管理', icon: 'Lock' }
      ]
    }
  ]
})

const menuItems = computed(() => {
  const menuList = permissionStore.menus
  if (!menuList || !Array.isArray(menuList) || menuList.length === 0) {
    return menuGroups.value.flatMap(g => g.items)
  }
  return menuList.map((menu: any) => ({
    path: menu.path,
    title: menu.menuName,
    icon: menu.icon || 'Menu'
  }))
})

const menuTitles = computed(() => {
  const titles: Record<string, string> = {}
  const menuList = permissionStore.menus
  if (menuList && Array.isArray(menuList)) {
    menuList.forEach((menu: any) => {
      titles[menu.path] = menu.menuName
    })
  }
  return titles
})

const menuBreadcrumbs: Record<string, string> = {
  '/dashboard': '概览 / Dashboard',
  '/workorder': '生产管理 / 工单管理',
  '/process': '生产管理 / 工艺管理',
  '/quality': '质量管理 / 质检管理',
  '/device': '设备管理 / 监控中心',
  '/alarm': '设备管理 / 报警管理',
  '/report': '数据分析 / 生产报表'
}

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => menuTitles.value[route.path] || 'MES系统')
const breadcrumb = computed(() => menuBreadcrumbs[route.path] || '')

onMounted(async () => {
  await userStore.getUserInfo()
  const userRoles = userStore.userInfo?.role ? [userStore.userInfo.role] : ['ADMIN']
  permissionStore.loadMenus(userRoles)
})

const handleCommand = (command: string) => {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'settings') {
    router.push('/settings')
  } else if (command === 'logout') {
    ElMessageBox.confirm('确定退出登录?', '提示', { type: 'warning' })
      .then(() => { userStore.logout(); router.push('/login') })
      .catch(() => {})
  }
}
</script>

<style scoped>
.layout-container {
  width: 100%;
  height: 100vh;
  background: var(--bg-app);
}

.el-aside {
  background: var(--bg-sidebar);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  overflow: hidden;
}

.aside-container {
  position: relative;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
  border-bottom: 1px solid var(--border-color);
  background: linear-gradient(135deg, var(--accent-light), transparent);
  transition: all 0.3s ease;
  cursor: pointer;
  position: relative;
}

.logo.collapsed {
  padding: 0;
  justify-content: center;
}

.logo-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--accent), var(--accent-dark, #3b82f6));
  border-radius: 10px;
  color: #fff;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.logo-text {
  display: flex;
  flex-direction: column;
}

.logo-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.5px;
}

.logo-subtitle {
  font-size: 10px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 1px;
}

.collapse-btn {
  position: absolute;
  right: -12px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.3s ease;
  color: var(--text-secondary);
  z-index: 10;
}

.collapse-btn:hover {
  background: var(--accent-light);
  border-color: var(--accent);
  color: var(--accent);
}

.collapse-btn.collapsed {
  transform: rotate(180deg);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.el-menu-vertical {
  flex: 1;
  background: transparent;
  border: none;
  padding: 8px 0;
  overflow-y: auto;
}

.el-menu-vertical:not(.el-menu--collapse) {
  width: 100%;
}

.el-menu--collapse {
  padding: 8px 0;
  width: 64px !important;
}

.el-menu--collapse .el-menu-item {
  justify-content: center;
  padding: 0 !important;
  min-width: 0;
  margin: 2px 4px;
}

.el-menu--collapse .el-menu-item .el-icon {
  margin: 0;
  font-size: 20px;
}

.el-menu--collapse .el-menu-item span {
  display: none;
}

/* 子菜单样式 - 主题兼容 */
:deep(.el-sub-menu__title) {
  color: var(--text-secondary) !important;
  border-radius: 8px;
  margin: 4px 8px;
  transition: all 0.2s ease;
}

:deep(.el-sub-menu__title:hover) {
  background: var(--bg-hover) !important;
  color: var(--text-primary) !important;
}

:deep(.el-sub-menu .el-menu) {
  background: transparent !important;
}

:deep(.el-sub-menu .el-menu-item) {
  color: var(--text-secondary) !important;
  border-radius: 8px;
  margin: 2px 0;
  min-height: 40px;
}

:deep(.el-sub-menu .el-menu-item:hover) {
  background: var(--bg-hover) !important;
  color: var(--text-primary) !important;
}

:deep(.el-sub-menu .el-menu-item.is-active) {
  background: var(--accent-light) !important;
  color: var(--accent) !important;
}

/* 折叠时隐藏子菜单 */
.el-menu--collapse .el-sub-menu {
  display: none;
}

.el-menu-item {
  color: var(--text-secondary);
  margin: 2px 8px;
  border-radius: 8px;
  height: 42px;
  transition: all 0.2s ease;
}

.el-menu-item:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.el-menu-item.is-active {
  background: linear-gradient(90deg, var(--accent-light), transparent);
  color: var(--accent);
  font-weight: 500;
}

.el-menu-item.is-active .el-icon {
  color: var(--accent);
}

.el-menu-item .el-icon {
  margin-right: 10px;
  font-size: 18px;
}

.main-container {
  display: flex;
  flex-direction: column;
}

.el-header {
  background: var(--bg-header);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 64px;
  border-bottom: 1px solid var(--border-color);
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.page-title {
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 600;
}

.breadcrumb {
  color: var(--text-muted);
  font-size: 12px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-hover);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.action-btn:hover {
  background: var(--accent-light);
  color: var(--accent);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: var(--radius-md);
  transition: background var(--transition-fast);
}

.user-info:hover {
  background: var(--bg-hover);
}

.user-avatar {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--accent-light);
  border-radius: 50%;
  color: var(--accent);
}

.user-name {
  color: var(--text-primary);
  font-weight: 500;
  font-size: 14px;
}

.dropdown-arrow {
  color: var(--text-muted);
  font-size: 12px;
}

.el-main {
  background: var(--bg-app);
  padding: 24px;
  overflow-y: auto;
}

html.light .logo-title { color: #1a1a2e; }
html.light .page-title { color: #1a1a2e; }
</style>

<style>
/* Dropdown menu styling */
.user-dropdown {
  background: var(--bg-card) !important;
  border: 1px solid var(--border-color) !important;
  border-radius: var(--radius-md) !important;
  padding: 6px !important;
}

.user-dropdown .el-dropdown-menu__item {
  color: var(--text-secondary) !important;
  border-radius: var(--radius-sm) !important;
  padding: 8px 12px !important;
}

.user-dropdown .el-dropdown-menu__item:hover {
  background: var(--bg-hover) !important;
  color: var(--text-primary) !important;
}

.user-dropdown .el-dropdown-menu__item .el-icon {
  margin-right: 8px;
}
</style>