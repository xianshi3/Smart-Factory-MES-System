<template>
  <div class="login-page">
    <div class="login-left">
      <div class="brand-section">
        <div class="brand-logo">
          <el-icon :size="40"><Monitor /></el-icon>
        </div>
        <h1 class="brand-title">Smart MES</h1>
        <p class="brand-subtitle">智能工厂执行系统</p>
        
        <div class="brand-features">
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
        <div class="login-title">
          <h2>欢迎登录</h2>
          <p>请输入账号密码登录系统</p>
        </div>
        
        <el-form 
          ref="loginFormRef" 
          :model="loginForm" 
          :rules="rules" 
          @keyup.enter="handleLogin"
          class="login-form"
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
          
          <div class="form-extra">
            <el-checkbox v-model="rememberMe">记住我</el-checkbox>
          </div>
          
          <el-button 
            type="primary" 
            size="large" 
            :loading="loading" 
            class="submit-btn"
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
  position: relative;
  overflow: hidden;
}

.login-left::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.1) 0%, transparent 50%);
  animation: rotate 20s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.brand-section {
  position: relative;
  z-index: 1;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.brand-logo {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-bottom: 24px;
  box-shadow: 0 8px 32px rgba(99, 102, 241, 0.3);
}

.brand-title {
  font-size: 36px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 8px;
}

.brand-subtitle {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 48px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  color: rgba(255, 255, 255, 0.8);
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

.brand-footer {
  position: relative;
  z-index: 1;
}

.brand-footer p {
  color: rgba(255, 255, 255, 0.3);
  font-size: 13px;
}

.login-right {
  width: 55%;
  background: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.login-box {
  width: 400px;
}

.login-title {
  margin-bottom: 40px;
}

.login-title h2 {
  font-size: 28px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
}

.login-title p {
  font-size: 14px;
  color: #64748b;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 24px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: none;
  border: 1px solid #e2e8f0;
  padding: 4px 12px;
}

.login-form :deep(.el-input__wrapper:hover) {
  border-color: #6366f1;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.login-form :deep(.el-input__inner) {
  height: 44px;
}

.form-extra {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.form-extra :deep(.el-checkbox__label) {
  color: #64748b;
  font-size: 13px;
}

.submit-btn {
  width: 100%;
  height: 48px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
}

.submit-btn:hover {
  background: linear-gradient(135deg, #5558e3, #7c4de4);
}

.login-tips {
  margin-top: 24px;
  padding: 12px 16px;
  background: #f1f5f9;
  border-radius: 8px;
}

.login-tips p {
  font-size: 13px;
  color: #64748b;
}

html.dark .login-left {
  background: linear-gradient(135deg, #1e3a5f 0%, #0d2137 100%);
}

html.dark .login-right {
  background: #0f172a;
}

html.dark .login-title h2 {
  color: #f1f5f9;
}

html.dark .login-title p {
  color: #94a3b8;
}

html.dark .login-form :deep(.el-input__wrapper) {
  background: #1e293b;
  border-color: #334155;
}

html.dark .login-form :deep(.el-input__inner) {
  color: #f1f5f9;
}

html.dark .login-form :deep(.el-input__inner::placeholder) {
  color: #64748b;
}

html.dark .login-tips {
  background: #1e293b;
}

html.dark .login-tips p {
  color: #94a3b8;
}

@media (max-width: 1024px) {
  .login-page {
    flex-direction: column;
  }
  
  .login-left {
    width: 100%;
    padding: 40px;
    min-height: 300px;
  }
  
  .login-right {
    width: 100%;
    padding: 40px;
  }
  
  .brand-features {
    display: none;
  }
}
</style>