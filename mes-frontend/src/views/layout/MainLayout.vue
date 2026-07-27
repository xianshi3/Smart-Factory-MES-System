<template>
  <el-container class="layout-root">
    <!-- ===== 左侧菜单 ===== -->
    <el-aside :width="isCollapse ? '56px' : '210px'" class="sidebar">
      <div class="sidebar-header">
        <div class="brand-icon">
          <el-icon :size="18"><Cpu /></el-icon>
        </div>
        <transition name="fade-slide">
          <div v-show="!isCollapse" class="brand-text">
            <span class="brand-name">Smart MES</span>
            <span class="brand-ver">v1.0</span>
          </div>
        </transition>
        <button class="collapse-btn-fixed" @click="toggleCollapse" :title="isCollapse ? '展开菜单' : '收起菜单'">
          <el-icon><ArrowLeft v-if="!isCollapse" /><ArrowRight v-else /></el-icon>
        </button>
      </div>

      <div class="menu-scroll">
        <el-menu
          :default-active="activeMenu"
          router
          :collapse="isCollapse"
          :collapse-transition="true"
          class="sidebar-menu"
          :default-openeds="openedMenus"
          @open="onMenuOpen"
          @close="onMenuClose"
        >
          <template v-for="group in menuGroups" :key="group.title">
            <el-sub-menu v-if="!isCollapse" :index="group.title">
              <template #title>
                <el-icon><component :is="group.icon" /></el-icon>
                <span>{{ group.title }}</span>
              </template>
              <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
                <el-icon><component :is="item.icon" /></el-icon>
                <span>{{ item.title }}</span>
                <span class="item-badge" v-if="item.badge">{{ item.badge }}</span>
              </el-menu-item>
            </el-sub-menu>
            <template v-else>
              <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
                <el-icon><component :is="item.icon" /></el-icon>
              </el-menu-item>
            </template>
          </template>
        </el-menu>
      </div>

      <div v-show="!isCollapse" class="sidebar-footer">
        <div class="collapse-btn-simple" @click="toggleCollapse">
          <el-icon><ArrowLeft /></el-icon>
          <span>收起菜单</span>
        </div>
      </div>
    </el-aside>

    <!-- ===== 主区域 ===== -->
    <el-container class="main-area">
      <!-- 顶部栏 -->
      <header class="topbar">
        <div class="topbar-left">
          <span class="breadcrumb-parent">{{ pageGroup }}</span>
          <el-icon class="breadcrumb-sep"><ArrowRight /></el-icon>
          <span class="breadcrumb-current">{{ currentTabLabel }}</span>
        </div>
        <div class="topbar-right">
          <el-tooltip :content="themeStore.isDark ? '亮色模式' : '暗色模式'" placement="bottom">
            <button class="topbar-btn" @click="themeStore.toggleTheme()">
              <el-icon><Sunny v-if="themeStore.isDark" /><Moon v-else /></el-icon>
            </button>
          </el-tooltip>
          <el-dropdown @command="handleCommand" trigger="click">
            <div class="user-card">
              <div class="user-avatar-small"><el-icon :size="14"><User /></el-icon></div>
              <span class="user-name">{{ userStore.userInfo?.username || '管理员' }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu class="dd-menu">
                <el-dropdown-item command="profile"><el-icon><User /></el-icon>个人中心</el-dropdown-item>
                <el-dropdown-item command="settings"><el-icon><Setting /></el-icon>系统设置</el-dropdown-item>
                <el-dropdown-item divided command="logout"><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 标签栏 -->
      <div v-if="tabs.length" class="tabbar">
        <div class="tab-scroll" ref="tabScrollRef">
          <div
            v-for="tab in tabs"
            :key="tab.path"
            class="tab-item"
            :class="{ active: tab.path === route.path }"
            @click="switchTab(tab)"
            @mouseup.middle="closeTab(tab)"
          >
            <span class="tab-dot" :class="{ on: tab.path === route.path }"></span>
            <span class="tab-label">{{ tab.label }}</span>
            <span v-if="tabs.length > 1" class="tab-close" @click.stop="closeTab(tab)">
              <el-icon><Close /></el-icon>
            </span>
          </div>
        </div>
        <div class="tab-dropdown">
          <el-dropdown trigger="click" @command="handleTabCommand">
            <button class="topbar-btn" style="width:28px;height:28px">
              <el-icon><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu class="dd-menu">
                <el-dropdown-item command="closeOther">关闭其他</el-dropdown-item>
                <el-dropdown-item command="closeAll">关闭所有</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 内容区 -->
      <el-main class="content-area">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
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
import { Close, Cpu, User, Sunny, Moon, ArrowDown, ArrowRight, Setting, SwitchButton } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()
const permissionStore = usePermissionStore()

const isCollapse = ref(false)
const openedMenus = ref<string[]>(['生产监控', '生产管理', '基础数据', '系统管理'])
const tabs = ref<{ path: string; label: string; group: string }[]>([])
const tabScrollRef = ref<HTMLElement>()

interface MI { path: string; title: string; icon: string; badge?: string }
interface MG { title: string; icon: string; items: MI[] }

const menuGroups = computed<MG[]>(() => [
  { title: '生产监控', icon: 'Odometer', items: [
    { path: '/dashboard', title: '工作台', icon: 'Odometer' },
    { path: '/device', title: '设备监控', icon: 'Monitor' },
    { path: '/alarm', title: '报警中心', icon: 'Warning' },
  ]},
  { title: '生产管理', icon: 'Document', items: [
    { path: '/workorder', title: '工单管理', icon: 'Document' },
    { path: '/process', title: '工艺管理', icon: 'Setting' },
    { path: '/quality', title: '质量管理', icon: 'CircleCheck' },
    { path: '/report', title: '生产报表', icon: 'DataAnalysis' },
  ]},
  { title: '基础数据', icon: 'Grid', items: [
    { path: '/production-line', title: '生产线', icon: 'Connection' },
    { path: '/workstation', title: '工位管理', icon: 'Location' },
    { path: '/material', title: '物料管理', icon: 'Box' },
    { path: '/bom', title: 'BOM管理', icon: 'List' },
  ]},
  { title: '系统管理', icon: 'Setting', items: [
    { path: '/role', title: '角色管理', icon: 'Key' },
    { path: '/menu', title: '菜单管理', icon: 'Menu' },
    { path: '/permission', title: '权限管理', icon: 'Lock' },
    { path: '/settings', title: '系统设置', icon: 'Setting' },
    { path: '/profile', title: '个人中心', icon: 'User' },
  ]},
])

const pageMap = computed(() => {
  const m: Record<string, { title: string; group: string }> = {}
  for (const g of menuGroups.value)
    for (const i of g.items) m[i.path] = { title: i.title, group: g.title }
  return m
})

const activeMenu = computed(() => route.path)
const pageGroup = computed(() => pageMap.value[route.path]?.group || '')
const currentTabLabel = computed(() => pageMap.value[route.path]?.title || route.path.slice(1) || '首页')

const switchTab = (tab: { path: string }) => router.push(tab.path)

const closeTab = (tab: { path: string }) => {
  if (tabs.value.length <= 1) return
  const idx = tabs.value.findIndex(t => t.path === tab.path)
  tabs.value = tabs.value.filter(t => t.path !== tab.path)
  if (route.path === tab.path && tabs.value.length) {
    router.push(tabs.value[Math.min(idx, tabs.value.length - 1)].path)
  }
}

const handleTabCommand = (cmd: string) => {
  if (cmd === 'closeOther') tabs.value = tabs.value.filter(t => t.path === route.path)
  else if (cmd === 'closeAll') { tabs.value = []; router.push('/dashboard') }
}

watch(() => route.path, (p) => {
  const info = pageMap.value[p]
  if (!info) return
  if (!tabs.value.some(t => t.path === p)) {
    tabs.value.push({ path: p, label: info.title, group: info.group })
  }
  setTimeout(() => {
    const el = tabScrollRef.value?.querySelector('.tab-item.active') as HTMLElement
    el?.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'nearest' })
  }, 50)
}, { immediate: true })

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
  if (!isCollapse.value && openedMenus.value.length === 0) {
    openedMenus.value = ['生产监控', '生产管理', '基础数据', '系统管理']
  }
}

const onMenuOpen = (idx: string) => {
  if (!openedMenus.value.includes(idx)) openedMenus.value.push(idx)
}

const onMenuClose = (idx: string) => {
  openedMenus.value = openedMenus.value.filter(i => i !== idx)
}

onMounted(async () => {
  await userStore.getUserInfo()
  const r = userStore.userInfo?.role ? [userStore.userInfo.role] : ['ADMIN']
  permissionStore.loadMenus(r)
})

const handleCommand = (cmd: string) => {
  if (cmd === 'profile') router.push('/profile')
  else if (cmd === 'settings') router.push('/settings')
  else if (cmd === 'logout') {
    ElMessageBox.confirm('确定退出登录?', '提示', { type: 'warning' })
      .then(() => { userStore.logout(); router.push('/login') }).catch(() => {})
  }
}
</script>

<style scoped>
.layout-root { width: 100%; height: 100vh; background: var(--bg-app); }

/* ===== SIDEBAR ===== */
.sidebar { background: var(--bg-sidebar); border-right: 1px solid var(--border-color); display: flex; flex-direction: column; transition: width 0.2s ease; overflow: hidden; z-index: 10; }

.sidebar-header { height: 48px; display: flex; align-items: center; gap: 10px; padding: 0 14px; flex-shrink: 0; border-bottom: 1px solid var(--border-color); position: relative; }
.brand-icon { width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; background: var(--accent); border-radius: 6px; color: #fff; flex-shrink: 0; }
.brand-text { display: flex; flex-direction: column; line-height: 1.2; overflow: hidden; }
.brand-name { font-size: 13px; font-weight: 700; color: var(--text-primary); letter-spacing: 0.5px; }
.brand-ver { font-size: 9px; color: var(--text-muted); }

.collapse-btn-fixed { position: absolute; right: -14px; top: 50%; transform: translateY(-50%); width: 20px; height: 20px; display: flex; align-items: center; justify-content: center; background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 50%; color: var(--text-muted); cursor: pointer; z-index: 11; font-size: 10px; transition: all 0.2s; }
.collapse-btn-fixed:hover { background: var(--accent-light); border-color: var(--accent); color: var(--accent); }
.el-aside[style*="56px"] + * .collapse-btn-fixed { right: -14px; }

.menu-scroll { flex: 1; overflow-y: auto; overflow-x: hidden; padding: 2px 0; }

.sidebar-menu { background: transparent; border: none; }
.sidebar-menu:not(.el-menu--collapse) { width: 100%; }
:deep(.el-sub-menu__title) { height: 36px; line-height: 36px; color: var(--text-secondary) !important; margin: 0 6px; border-radius: 6px; font-size: 14px; padding-left: 10px !important; }
:deep(.el-sub-menu__title:hover) { background: var(--bg-hover) !important; color: var(--text-primary) !important; }
:deep(.el-sub-menu__title .el-icon) { font-size: 16px; }
:deep(.el-sub-menu .el-menu) { background: transparent !important; }
:deep(.el-sub-menu .el-menu-item) { height: 32px; line-height: 32px; color: var(--text-secondary) !important; margin: 0 6px 0 14px; border-radius: 6px; font-size: 14px; padding-left: 14px !important; }
:deep(.el-sub-menu .el-menu-item:hover) { background: var(--bg-hover) !important; color: var(--text-primary) !important; }
:deep(.el-sub-menu .el-menu-item.is-active) { background: var(--accent-light) !important; color: var(--accent) !important; font-weight: 500; }
.el-menu--collapse .el-sub-menu { display: none; }

.el-menu-item { height: 36px; line-height: 36px; color: var(--text-secondary); margin: 0 6px; border-radius: 6px; padding-left: 10px !important; font-size: 14px; }
.el-menu-item:hover { background: var(--bg-hover); color: var(--text-primary); }
.el-menu-item.is-active { background: var(--accent-light); color: var(--accent); font-weight: 500; }
.el-menu-item.is-active .el-icon { color: var(--accent); }

.el-menu--collapse .el-menu-item { display: flex; align-items: center; justify-content: center; padding: 0 !important; margin: 2px auto; width: 42px; height: 42px; border-radius: 10px; }
.el-menu--collapse .el-menu-item .el-icon { margin: 0; font-size: 20px; }
.el-menu--collapse .el-menu-item span { display: none; }

.item-badge { margin-left: auto; background: var(--danger); color: #fff; font-size: 10px; padding: 1px 6px; border-radius: 8px; font-weight: 600; }

.sidebar-footer { flex-shrink: 0; border-top: 1px solid var(--border-color); padding: 4px 6px; }
.collapse-btn-simple { display: flex; align-items: center; gap: 6px; padding: 5px 8px; border-radius: 6px; color: var(--text-muted); font-size: 12px; cursor: pointer; }
.collapse-btn-simple:hover { background: var(--bg-hover); color: var(--text-primary); }

.fade-slide-enter-active, .fade-slide-leave-active { transition: all 0.15s ease; }
.fade-slide-enter-from, .fade-slide-leave-to { opacity: 0; transform: translateX(-6px); }

/* ===== MAIN ===== */
.main-area { display: flex; flex-direction: column; overflow: hidden; }
.topbar { height: 42px; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; background: var(--bg-header); border-bottom: 1px solid var(--border-color); flex-shrink: 0; }
.topbar-left { display: flex; align-items: center; gap: 6px; }
.breadcrumb-parent { font-size: 12px; color: var(--text-muted); }
.breadcrumb-sep { font-size: 10px; color: var(--text-muted); }
.breadcrumb-current { font-size: 14px; color: var(--text-primary); font-weight: 600; }
.topbar-right { display: flex; align-items: center; gap: 10px; }
.topbar-btn { width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; background: transparent; border: none; border-radius: 6px; color: var(--text-secondary); cursor: pointer; font-size: 15px; }
.topbar-btn:hover { background: var(--bg-hover); color: var(--text-primary); }
.user-card { display: flex; align-items: center; gap: 6px; padding: 3px 8px; border-radius: 6px; cursor: pointer; }
.user-card:hover { background: var(--bg-hover); }
.user-avatar-small { width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; background: var(--accent-light); border-radius: 50%; color: var(--accent); }
.user-name { font-size: 13px; color: var(--text-primary); font-weight: 500; }

.tabbar { display: flex; align-items: center; height: 30px; padding: 0 8px; background: var(--bg-app); border-bottom: 1px solid var(--border-color); flex-shrink: 0; }
.tab-scroll { flex: 1; display: flex; align-items: center; gap: 1px; overflow-x: auto; scrollbar-width: none; }
.tab-scroll::-webkit-scrollbar { display: none; }
.tab-item { display: flex; align-items: center; gap: 4px; padding: 2px 8px; font-size: 12px; color: var(--text-secondary); cursor: pointer; border-radius: 4px; white-space: nowrap; user-select: none; }
.tab-item:hover { background: var(--bg-hover); color: var(--text-primary); }
.tab-item.active { background: var(--bg-card); color: var(--accent); font-weight: 500; box-shadow: 0 0 0 1px var(--border-color); }
.tab-dot { width: 4px; height: 4px; border-radius: 50%; background: var(--border-color); flex-shrink: 0; }
.tab-dot.on { background: var(--accent); }
.tab-close { display: flex; align-items: center; font-size: 10px; opacity: 0; border-radius: 3px; padding: 1px; }
.tab-item:hover .tab-close { opacity: 0.4; }
.tab-item .tab-close:hover { opacity: 1; background: var(--danger-light); color: var(--danger); }
.tab-dropdown { flex-shrink: 0; padding-left: 4px; margin-left: 4px; border-left: 1px solid var(--border-light); }

.content-area { background: var(--bg-app); padding: 20px; overflow-y: auto; font-size: 14px; }
.page-fade-enter-active, .page-fade-leave-active { transition: opacity 0.1s ease; }
.page-fade-enter-from, .page-fade-leave-to { opacity: 0; }
</style>

<style>
.dd-menu { background: var(--bg-card) !important; border: 1px solid var(--border-color) !important; border-radius: 8px !important; padding: 4px !important; min-width: 130px; }
.dd-menu .el-dropdown-menu__item { color: var(--text-secondary) !important; border-radius: 6px !important; padding: 6px 12px !important; font-size: 13px; }
.dd-menu .el-dropdown-menu__item:hover { background: var(--bg-hover) !important; color: var(--text-primary) !important; }
.dd-menu .el-dropdown-menu__item .el-icon { margin-right: 6px; font-size: 14px; }
</style>
