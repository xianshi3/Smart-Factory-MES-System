<template>
  <el-container class="layout-container">
    <el-aside width="240">
      <div class="logo">
        <div class="logo-icon">
          <el-icon size="24"><Cpu /></el-icon>
        </div>
        <div class="logo-text">
          <span class="logo-title">Smart MES</span>
          <span class="logo-subtitle">智能工厂</span>
        </div>
      </div>
      
      <el-menu 
        :default-active="activeMenu" 
        class="el-menu-vertical" 
        router
        :ellipsis="false"
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
      
      <div class="sidebar-footer">
        <div class="version-tag">v1.0.0</div>
      </div>
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
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { usePermissionStore } from '@/stores/permission'
import { Cpu, User, Sunny, Moon, ArrowDown, Setting, SwitchButton } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()
const permissionStore = usePermissionStore()

const menuItems = computed(() => {
  const menuList = permissionStore.menus
  if (!menuList || !Array.isArray(menuList) || menuList.length === 0) {
    return [
      { path: '/dashboard', title: '首页', icon: 'Odometer' },
      { path: '/workorder', title: '工单管理', icon: 'Document' },
      { path: '/process', title: '工艺管理', icon: 'Setting' },
      { path: '/quality', title: '质量管理', icon: 'CircleCheck' },
      { path: '/device', title: '设备监控', icon: 'Monitor' },
      { path: '/report', title: '生产报表', icon: 'DataAnalysis' },
      { path: '/role', title: '角色管理', icon: 'Key' }
    ]
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
  '/report': '数据分析 / 生产报表'
}

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => menuTitles.value[route.path] || 'MES系统')
const breadcrumb = computed(() => menuBreadcrumbs[route.path] || '')

onMounted(() => {
  permissionStore.loadMenus()
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
}

.logo {
  height: 70px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
  border-bottom: 1px solid var(--border-color);
  background: linear-gradient(135deg, var(--accent-light), transparent);
}

.logo-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--gradient-primary);
  border-radius: 10px;
  color: #fff;
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

.el-menu-vertical {
  flex: 1;
  background: transparent;
  border: none;
  padding: 8px;
}

.el-menu-item {
  color: var(--text-secondary);
  margin: 4px 0;
  border-radius: var(--radius-md);
  height: 44px;
  transition: all var(--transition-fast);
}

.el-menu-item:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.el-menu-item.is-active {
  background: var(--accent-light);
  color: var(--accent);
}

.el-menu-item.is-active .el-icon {
  color: var(--accent);
}

.el-menu-item .el-icon {
  margin-right: 10px;
  font-size: 18px;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid var(--border-color);
  text-align: center;
}

.version-tag {
  display: inline-block;
  padding: 4px 12px;
  background: var(--bg-hover);
  border-radius: 20px;
  color: var(--text-muted);
  font-size: 11px;
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

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
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