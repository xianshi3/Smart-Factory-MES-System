<template>
  <div class="login-page">
    <div class="login-left">
      <div class="brand-content">
        <div class="brand-logo">
          <Monitor :size="36" />
        </div>
        <h1>Smart MES</h1>
        <p>智能工厂执行系统</p>
        
        <div class="feature-list">
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>生产管理数字化</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>设备监控智能化</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>质量追溯全程化</span>
          </div>
        </div>
      </div>
      
      <div class="brand-footer">
        <p>© 2026 Smart Factory MES</p>
      </div>
    </div>
    
    <div class="login-right">
      <div class="login-box">
        <div class="login-header">
          <h2>欢迎登录</h2>
          <p>请输入账号密码登录系统</p>
        </div>
        
        <el-form 
          ref="loginFormRef" 
          :model="loginForm" 
          :rules="rules" 
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input 
              v-model="loginForm.username" 
              placeholder="请输入用户名"
              prefix-icon="User"
              size="large"
            />
          </el-form-item>
          
          <el-form-item prop="password">
            <el-input 
              v-model="loginForm.password" 
              type="password" 
              placeholder="请输入密码"
              prefix-icon="Lock"
              size="large"
              show-password
            />
          </el-form-item>
          
          <div class="form-options">
            <el-checkbox v-model="rememberMe">记住我</el-checkbox>
          </div>
          
          <el-button 
            type="primary" 
            size="large" 
            :loading="loading" 
            class="login-btn"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, FormInstance } from 'element-plus'
import { Monitor, Check, User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loginFormRef = ref<FormInstance>()
const loading = ref(false)
const rememberMe = ref(false)
const loginForm = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await userStore.login(loginForm.username, loginForm.password)
        ElMessage.success('登录成功')
        router.push('/')
      } catch (error: any) {
        ElMessage.error(error?.response?.data?.message || error?.message || '登录失败')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
}

.login-left {
  width: 45%;
  background: linear-gradient(135deg, #1e3a5f 0%, #0d2137 100%);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 60px;
}

.brand-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.brand-logo {
  width: 72px;
  height: 72px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-bottom: 24px;
}

.login-left h1 {
  font-size: 32px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 8px;
}

.login-left > .brand-content > p {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 48px;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  color: rgba(255, 255, 255, 0.85);
  font-size: 15px;
}

.feature-item .el-icon {
  width: 24px;
  height: 24px;
  background: rgba(99, 102, 241, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6366f1;
}

.brand-footer p {
  color: rgba(255, 255, 255, 0.3);
  font-size: 13px;
}

.login-right {
  width: 55%;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.login-box {
  width: 380px;
}

.login-header {
  margin-bottom: 36px;
}

.login-header h2 {
  font-size: 26px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
}

.login-header p {
  font-size: 14px;
  color: #64748b;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 8px;
}

.login-form :deep(.el-input__inner) {
  height: 44px;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.form-options :deep(.el-checkbox__label) {
  color: #64748b;
  font-size: 14px;
}

.login-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  border-radius: 8px;
}

html.dark .login-left {
  background: linear-gradient(135deg, #1e3a5f 0%, #0d2137 100%);
}

html.dark .login-right {
  background: #0f172a;
}

html.dark .login-header h2 {
  color: #f1f5f9;
}

html.dark .login-header p {
  color: #94a3b8;
}

html.dark .login-form :deep(.el-input__wrapper) {
  background: #1e293b;
  border-color: #334155;
}

html.dark .login-form :deep(.el-input__inner) {
  color: #f1f5f9;
}

html.dark .form-options :deep(.el-checkbox__label) {
  color: #94a3b8;
}

@media (max-width: 1024px) {
  .login-page {
    flex-direction: column;
  }
  
  .login-left {
    width: 100%;
    padding: 40px;
    min-height: 280px;
  }
  
  .login-right {
    width: 100%;
    padding: 40px;
  }
  
  .feature-list {
    display: none;
  }
}
</style>