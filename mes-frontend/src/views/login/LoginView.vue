<template>
  <div class="login-page">
    <div class="login-left">
      <div class="left-bg">
        <div class="bg-circle circle-1"></div>
        <div class="bg-circle circle-2"></div>
        <div class="bg-circle circle-3"></div>
      </div>
      
      <div class="brand-content">
        <div class="brand-logo">
          <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect x="6" y="30" width="36" height="14" rx="2" stroke="currentColor" stroke-width="2"/>
            <rect x="10" y="34" width="8" height="6" stroke="currentColor" stroke-width="2"/>
            <rect x="22" y="34" width="8" height="6" stroke="currentColor" stroke-width="2"/>
            <path d="M12 30V18L24 10L36 18V30" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <circle cx="24" cy="22" r="4" stroke="currentColor" stroke-width="2"/>
            <circle cx="24" cy="22" r="2" fill="currentColor"/>
          </svg>
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
import { Check } from '@element-plus/icons-vue'
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
        await userStore.login(loginForm.username, loginForm.password, rememberMe.value)
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
  flex: 0 0 48%;
  width: 48%;
  background: linear-gradient(135deg, #1e3a5f 0%, #0f1d2e 50%, #0a1422 100%);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 60px;
  position: relative;
  overflow: hidden;
}

.left-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
}

.circle-1 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.15) 0%, transparent 70%);
  top: -150px;
  right: -100px;
}

.circle-2 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(139, 92, 246, 0.1) 0%, transparent 70%);
  bottom: -100px;
  left: -80px;
}

.circle-3 {
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.1) 0%, transparent 70%);
  top: 40%;
  left: 20%;
}

.brand-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.brand-logo {
  display: flex;
  justify-content: center;
  color: #fff;
}

.brand-logo svg {
  width: 52px;
  height: 52px;
  margin-bottom: 24px;
}

.login-left h1 {
  font-size: 32px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 8px;
  letter-spacing: 1px;
  text-align: center;
}

.login-left > .brand-content > p {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 28px;
  text-align: center;
}

.feature-list {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  color: rgba(255, 255, 255, 0.75);
  font-size: 14px;
}

.feature-item .el-icon {
  width: 24px;
  height: 24px;
  background: rgba(99, 102, 241, 0.25);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #818cf8;
}

.brand-footer p {
  color: rgba(255, 255, 255, 0.25);
  font-size: 13px;
  text-align: center;
}

.login-right {
  flex: 0 0 52%;
  width: 52%;
  background: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.login-box {
  width: 360px;
}

.login-header {
  margin-bottom: 32px;
  text-align: center;
}

.login-header h2 {
  font-size: 24px;
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
  padding: 4px 12px;
}

.login-form :deep(.el-input__wrapper:hover) {
  border-color: #a5b4fc;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
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
  font-size: 13px;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 500;
  border-radius: 8px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border: none;
  transition: all 0.3s;
}

.login-btn:hover {
  background: linear-gradient(135deg, #5558e3, #7c4de4);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.35);
}

html.dark .login-left {
  background: linear-gradient(135deg, #1e3a5f 0%, #0f1d2e 50%, #0a1422 100%);
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
  
  .login-left,
  .login-right {
    flex: none;
    width: 100%;
  }
  
  .login-left {
    min-height: 260px;
  }
  
  .feature-list {
    display: none;
  }
}
</style>
