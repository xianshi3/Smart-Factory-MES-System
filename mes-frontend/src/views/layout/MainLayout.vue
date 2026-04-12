<template>
  <el-container class="layout-container">
    <el-aside width="220">
      <div class="logo">
        <div class="logo-icon"><el-icon size="28"><Odometer /></el-icon></div>
        <h2>Smart MES</h2>
      </div>
      <el-menu :default-active="activeMenu" class="el-menu-vertical" router>
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
      <div class="sidebar-footer">v1.0.0</div>
    </el-aside>
    <el-container>
      <el-header>
        <div class="header-left">
          <span class="page-title">{{ pageTitle }}</span>
        </div>
        <div class="header-right">
          <div class="action-btn" @click="themeStore.toggleTheme()">
            <el-icon><Sunny v-if="themeStore.isDark" /><Moon v-else /></el-icon>
          </div>
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-icon><User /></el-icon>
              <span>{{ userStore.user?.username || '管理员' }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()

const menuItems = [
  { path: '/dashboard', title: '首页', icon: 'Odometer' },
  { path: '/workorder', title: '工单管理', icon: 'Document' },
  { path: '/process', title: '工艺管理', icon: 'Setting' },
  { path: '/quality', title: '质量管理', icon: 'CircleCheck' },
  { path: '/device', title: '设备监控', icon: 'Monitor' },
  { path: '/report', title: '生产报表', icon: 'DataAnalysis' }
]

const menuTitles: Record<string, string> = {
  '/dashboard': '首页', '/workorder': '工单管理', '/process': '工艺管理',
  '/quality': '质量管理', '/device': '设备监控', '/report': '生产报表'
}

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => menuTitles[route.path] || 'MES系统')

const handleCommand = (command: string) => {
  if (command === 'logout') {
    ElMessageBox.confirm('确定退出登录?', '提示', { type: 'warning' })
      .then(() => { userStore.logout(); router.push('/login') })
      .catch(() => {})
  }
}
</script>

<style scoped>
.layout-container { width: 100%; height: 100vh; background: var(--bg-app); }
.el-aside { background: var(--bg-sidebar); border-right: 1px solid var(--border-color); }
.logo { height: 70px; display: flex; align-items: center; gap: 12px; padding: 0 20px; border-bottom: 1px solid var(--border-color); }
.logo-icon { width: 40px; height: 40px; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #e94560, #0f3460); border-radius: 10px; color: #fff; }
.logo h2 { color: var(--text-primary); font-size: 18px; }
.el-menu-vertical { background: transparent; border: none; }
.el-menu-item { color: var(--text-secondary); margin: 4px 10px; border-radius: 10px; }
.el-menu-item:hover { background: var(--hover-bg); }
.el-menu-item.is-active { background: var(--accent); color: #fff; }
.sidebar-footer { padding: 16px; text-align: center; border-top: 1px solid var(--border-color); color: var(--text-muted); font-size: 12px; }
.el-header { background: var(--bg-header); display: flex; justify-content: space-between; align-items: center; padding: 0 24px; border-bottom: 1px solid var(--border-color); }
.page-title { color: var(--text-primary); font-weight: 500; }
.header-right { display: flex; align-items: center; gap: 16px; }
.action-btn { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; background: var(--bg-card); border-radius: 8px; color: var(--text-secondary); cursor: pointer; }
.action-btn:hover { background: var(--accent); color: #fff; }
.user-info { display: flex; align-items: center; gap: 8px; cursor: pointer; color: var(--text-primary); }
.el-main { background: var(--bg-app); padding: 24px; }
</style>