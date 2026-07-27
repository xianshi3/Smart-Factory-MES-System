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

      <el-menu :default-active="activeMenu" class="el-menu-vertical" router :ellipsis="false" :collapse="isCollapse" :collapse-transition="true">
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
        <div class="header-bar">
          <div class="header-left">
            <span class="header-title">{{ pageTitle }}</span>
            <el-icon v-if="tabs.length" class="header-sep"><ArrowRight /></el-icon>
            <span class="header-sub">{{ currentTabLabel }}</span>
          </div>
          <div class="header-right">
            <div class="action-btn" @click="themeStore.toggleTheme()" :title="themeStore.isDark ? '亮色' : '暗色'">
              <el-icon><Sunny v-if="themeStore.isDark" /><Moon v-else /></el-icon>
            </div>
            <el-dropdown @command="handleCommand" trigger="click">
              <div class="user-info">
                <div class="user-avatar"><el-icon><User /></el-icon></div>
                <span class="user-name">{{ userStore.userInfo?.username || '管理员' }}</span>
                <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu class="user-dropdown">
                  <el-dropdown-item command="profile"><el-icon><User /></el-icon>个人中心</el-dropdown-item>
                  <el-dropdown-item command="settings"><el-icon><Setting /></el-icon>系统设置</el-dropdown-item>
                  <el-dropdown-item divided command="logout"><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
        <div class="tab-bar" v-if="tabs.length">
          <div class="tab-scroll" ref="tabScrollRef">
            <div v-for="tab in tabs" :key="tab.path" class="tab-item" :class="{ active: tab.path === route.path }" @click="switchTab(tab)" @mouseup.middle="closeTab(tab)">
              <component :is="tab.icon" class="tab-icon" />
              <span>{{ tab.label }}</span>
              <el-icon class="tab-close" @click.stop="closeTab(tab)" v-if="tabs.length > 1"><Close /></el-icon>
            </div>
          </div>
          <div class="tab-actions">
            <el-dropdown trigger="click" @command="handleTabCommand">
              <el-button text size="small"><el-icon><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="closeOther">关闭其他</el-dropdown-item>
                  <el-dropdown-item command="closeAll">关闭所有</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
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
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { usePermissionStore } from '@/stores/permission'
import { Close, Cpu, User, Sunny, Moon, ArrowDown, ArrowLeft, ArrowRight, Setting, SwitchButton } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()
const permissionStore = usePermissionStore()

const isCollapse = ref(false)
const tabs = ref<{ path: string; label: string; icon: any }[]>([])
const tabScrollRef = ref<HTMLElement>()

interface MenuItem { path: string; title: string; icon: string }
interface MenuGroup { title: string; icon: string; items: MenuItem[] }

const menuGroups = computed<MenuGroup[]>(() => [
  { title: '生产监控', icon: 'Odometer', items: [
    { path: '/dashboard', title: '工作台', icon: 'Odometer' },
    { path: '/device', title: '设备监控', icon: 'Monitor' },
    { path: '/alarm', title: '报警中心', icon: 'Warning' }] },
  { title: '生产管理', icon: 'Document', items: [
    { path: '/workorder', title: '工单管理', icon: 'Document' },
    { path: '/process', title: '工艺管理', icon: 'Setting' },
    { path: '/quality', title: '质量管理', icon: 'CircleCheck' },
    { path: '/report', title: '生产报表', icon: 'DataAnalysis' }] },
  { title: '基础数据', icon: 'Grid', items: [
    { path: '/production-line', title: '生产线', icon: 'Connection' },
    { path: '/workstation', title: '工位管理', icon: 'Location' },
    { path: '/material', title: '物料管理', icon: 'Box' },
    { path: '/bom', title: 'BOM管理', icon: 'List' }] },
  { title: '系统管理', icon: 'Setting', items: [
    { path: '/role', title: '角色管理', icon: 'Key' },
    { path: '/menu', title: '菜单管理', icon: 'Menu' },
    { path: '/permission', title: '权限管理', icon: 'Lock' },
    { path: '/settings', title: '系统设置', icon: 'Setting' },
    { path: '/profile', title: '个人中心', icon: 'User' }] }
])

const flatMenuMap = computed(() => {
  const map: Record<string, MenuItem> = {}
  for (const g of menuGroups.value)
    for (const i of g.items) map[i.path] = i
  return map
})

const toggleCollapse = () => { isCollapse.value = !isCollapse.value }

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => {
  const m = flatMenuMap.value[route.path]
  if (m) {
    const g = menuGroups.value.find(g => g.items.some(i => i.path === route.path))
    return g ? g.title : ''
  }
  return ''
})
const currentTabLabel = computed(() => flatMenuMap.value[route.path]?.title || route.path.slice(1) || '首页')

const switchTab = (tab: { path: string }) => { router.push(tab.path) }

const closeTab = (tab: { path: string }) => {
  if (tabs.value.length <= 1) return
  const idx = tabs.value.findIndex(t => t.path === tab.path)
  tabs.value = tabs.value.filter(t => t.path !== tab.path)
  if (route.path === tab.path && tabs.value.length) {
    const next = tabs.value[Math.min(idx, tabs.value.length - 1)]
    router.push(next.path)
  }
}

const handleTabCommand = (cmd: string) => {
  if (cmd === 'closeOther') {
    tabs.value = tabs.value.filter(t => t.path === route.path)
  } else if (cmd === 'closeAll') {
    tabs.value = []
    router.push('/dashboard')
  }
}

watch(() => route.path, (path) => {
  const item = flatMenuMap.value[path]
  if (!item) return
  if (!tabs.value.some(t => t.path === path)) {
    tabs.value.push({ path: item.path, label: item.title, icon: item.icon })
  }
  setTimeout(() => {
    const el = tabScrollRef.value
    if (el) {
      const active = el.querySelector('.tab-item.active') as HTMLElement
      if (active) active.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'nearest' })
    }
  }, 50)
}, { immediate: true })

onMounted(async () => {
  await userStore.getUserInfo()
  const userRoles = userStore.userInfo?.role ? [userStore.userInfo.role] : ['ADMIN']
  permissionStore.loadMenus(userRoles)
})

const handleCommand = (command: string) => {
  if (command === 'profile') router.push('/profile')
  else if (command === 'settings') router.push('/settings')
  else if (command === 'logout') {
    ElMessageBox.confirm('确定退出登录?', '提示', { type: 'warning' })
      .then(() => { userStore.logout(); router.push('/login') }).catch(() => {})
  }
}
</script>

<style scoped>
.layout-container { width: 100%; height: 100vh; background: var(--bg-app); }

.el-aside { background: var(--bg-sidebar); border-right: 1px solid var(--border-color); display: flex; flex-direction: column; transition: width 0.3s ease; overflow: hidden; position: relative; z-index: 100; }

.logo { height: 64px; display: flex; align-items: center; gap: 12px; padding: 0 20px; border-bottom: 1px solid var(--border-color); background: linear-gradient(135deg, var(--accent-light), transparent); transition: all 0.3s ease; cursor: pointer; position: relative; flex-shrink: 0; }
.logo.collapsed { padding: 0; justify-content: center; }
.logo-icon { flex-shrink: 0; width: 40px; height: 40px; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, var(--accent), var(--accent-dark)); border-radius: 10px; color: #fff; box-shadow: 0 4px 12px rgba(59,130,246,0.3); }
.logo-text { display: flex; flex-direction: column; }
.logo-title { font-size: 16px; font-weight: 700; color: var(--text-primary); letter-spacing: 0.5px; }
.logo-subtitle { font-size: 10px; color: var(--text-muted); letter-spacing: 1px; }

.collapse-btn { position: absolute; right: -12px; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 50%; cursor: pointer; transition: all 0.3s ease; color: var(--text-secondary); z-index: 10; }
.collapse-btn:hover { background: var(--accent-light); border-color: var(--accent); color: var(--accent); }
.collapse-btn.collapsed { transform: rotate(180deg); }

.el-menu-vertical { flex: 1; background: transparent; border: none; padding: 8px 0; overflow-y: auto; }
.el-menu-vertical:not(.el-menu--collapse) { width: 100%; }
.el-menu--collapse { padding: 8px 4px; width: 64px !important; }
.el-menu--collapse .el-menu-item { justify-content: center; padding: 0 !important; min-width: 0; margin: 2px 4px; border-radius: 8px; }
.el-menu--collapse .el-menu-item .el-icon { margin: 0; font-size: 20px; }

:deep(.el-sub-menu__title) { color: var(--text-secondary) !important; border-radius: 8px; margin: 4px 8px; height: 42px; }
:deep(.el-sub-menu__title:hover) { background: var(--bg-hover) !important; color: var(--text-primary) !important; }
:deep(.el-sub-menu .el-menu) { background: transparent !important; }
:deep(.el-sub-menu .el-menu-item) { color: var(--text-secondary) !important; border-radius: 8px; margin: 2px 8px; min-height: 38px; }
:deep(.el-sub-menu .el-menu-item:hover) { background: var(--bg-hover) !important; color: var(--text-primary) !important; }
:deep(.el-sub-menu .el-menu-item.is-active) { background: var(--accent-light) !important; color: var(--accent) !important; }
.el-menu--collapse .el-sub-menu { display: none; }

.el-menu-item { color: var(--text-secondary); height: 42px; margin: 2px 8px; border-radius: 8px; }
.el-menu-item:hover { background: var(--bg-hover); color: var(--text-primary); }
.el-menu-item.is-active { background: linear-gradient(90deg, var(--accent-light), transparent); color: var(--accent); font-weight: 500; }
.el-menu-item.is-active .el-icon { color: var(--accent); }

/* ===== Header ===== */
.el-header { background: var(--bg-header); padding: 0; height: auto; border-bottom: 1px solid var(--border-color); display: flex; flex-direction: column; }
.header-bar { display: flex; justify-content: space-between; align-items: center; padding: 0 24px; height: 56px; }
.header-left { display: flex; align-items: center; gap: 8px; }
.header-title { font-size: 14px; color: var(--text-muted); font-weight: 500; }
.header-sep { color: var(--text-muted); font-size: 12px; }
.header-sub { font-size: 16px; color: var(--text-primary); font-weight: 600; }
.header-right { display: flex; align-items: center; gap: 12px; }
.action-btn { width: 34px; height: 34px; display: flex; align-items: center; justify-content: center; background: var(--bg-hover); border-radius: var(--radius-md); color: var(--text-secondary); cursor: pointer; }
.action-btn:hover { background: var(--accent-light); color: var(--accent); }

.user-info { display: flex; align-items: center; gap: 8px; cursor: pointer; padding: 4px 10px; border-radius: var(--radius-md); }
.user-info:hover { background: var(--bg-hover); }
.user-avatar { width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; background: var(--accent-light); border-radius: 50%; color: var(--accent); }
.user-name { color: var(--text-primary); font-weight: 500; font-size: 14px; }
.dropdown-arrow { color: var(--text-muted); font-size: 12px; }

/* ===== Tab Bar ===== */
.tab-bar { display: flex; align-items: center; padding: 0 8px; height: 36px; background: var(--bg-hover); border-top: 1px solid var(--border-light); }
.tab-scroll { flex: 1; display: flex; gap: 2px; overflow-x: auto; scrollbar-width: none; }
.tab-scroll::-webkit-scrollbar { display: none; }
.tab-item { display: flex; align-items: center; gap: 5px; padding: 4px 12px; font-size: 13px; color: var(--text-secondary); cursor: pointer; border-radius: 6px; white-space: nowrap; transition: all 0.15s; user-select: none; }
.tab-item:hover { background: var(--bg-card); color: var(--text-primary); }
.tab-item.active { background: var(--bg-card); color: var(--accent); font-weight: 500; }
.tab-icon { font-size: 14px; }
.tab-close { font-size: 12px; margin-left: 4px; opacity: 0; border-radius: 4px; padding: 1px; }
.tab-item:hover .tab-close { opacity: 0.6; }
.tab-item .tab-close:hover { opacity: 1; background: var(--danger-light); color: var(--danger); }
.tab-actions { flex-shrink: 0; padding-left: 8px; border-left: 1px solid var(--border-light); margin-left: 4px; }

.el-main { background: var(--bg-app); padding: 24px; overflow-y: auto; }

.fade-enter-active, .fade-leave-active { transition: opacity 0.15s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>

<style>
.user-dropdown { background: var(--bg-card) !important; border: 1px solid var(--border-color) !important; border-radius: var(--radius-md) !important; padding: 6px !important; }
.user-dropdown .el-dropdown-menu__item { color: var(--text-secondary) !important; border-radius: var(--radius-sm) !important; padding: 8px 12px !important; }
.user-dropdown .el-dropdown-menu__item:hover { background: var(--bg-hover) !important; color: var(--text-primary) !important; }
.user-dropdown .el-dropdown-menu__item .el-icon { margin-right: 8px; }
</style>
