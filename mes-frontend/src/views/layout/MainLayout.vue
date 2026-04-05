<!-- Main Layout Component - Application shell with sidebar and header -->
<template>
  <el-container class="layout-container">
    <el-aside width="200px">
      <div class="logo">
        <h2>MES系统</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="el-menu-vertical"
        background-color="#0f3460"
        text-color="#ffffff"
        active-text-color="#e94560"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-menu-item index="/workorder">
          <el-icon><Document /></el-icon>
          <span>工单管理</span>
        </el-menu-item>
        <el-menu-item index="/process">
          <el-icon><Setting /></el-icon>
          <span>工艺管理</span>
        </el-menu-item>
        <el-menu-item index="/quality">
          <el-icon><CircleCheck /></el-icon>
          <span>质量管理</span>
        </el-menu-item>
        <el-menu-item index="/device">
          <el-icon><Monitor /></el-icon>
          <span>设备监控</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header>
        <div class="header-left">
          <span class="page-title">{{ pageTitle }}</span>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><User /></el-icon>
              <span>管理员</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
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

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const menuTitles: Record<string, string> = {
  '/dashboard': '首页',
  '/workorder': '工单管理',
  '/process': '工艺管理',
  '/quality': '质量管理',
  '/device': '设备监控'
}

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => menuTitles[route.path] || 'MES系统')

const handleCommand = (command: string) => {
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      userStore.logout()
      router.push('/login')
    }).catch(() => {})
  } else if (command === 'profile') {
    ElMessageBox.alert('个人中心功能开发中...', '提示')
  }
}
</script>

<style scoped lang="scss">
.layout-container {
  width: 100%;
  height: 100vh;
  
  .el-aside {
    background-color: #0f3460;
    
    .logo {
      height: 60px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);
      
      h2 {
        color: #ffffff;
        font-size: 18px;
      }
    }
    
    .el-menu-vertical {
      border-right: none;
    }
  }
  
  .el-header {
    background-color: #1a1a2e;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 20px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    
    .page-title {
      color: #ffffff;
      font-size: 16px;
    }
    
    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #ffffff;
      cursor: pointer;
    }
  }
  
  .el-main {
    background-color: #1a1a2e;
    padding: 20px;
    min-height: calc(100vh - 60px);
  }
}
</style>