<template>
  <div class="login-container">
    <div class="login-bg">
      <div class="bg-shape shape-1"></div>
      <div class="bg-shape shape-2"></div>
      <div class="bg-shape shape-3"></div>
      <div class="bg-grid"></div>
    </div>
    
    <div class="login-card">
      <div class="card-glow"></div>
      
      <div class="login-header">
        <div class="logo-wrapper">
          <div class="logo-icon">
            <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 2L2 7V17L12 22L22 17V7L12 2Z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M12 22V12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
              <path d="M22 7L12 12L2 7" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M7 4.5L17 9.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
              <circle cx="12" cy="12" r="2" fill="currentColor"/>
            </svg>
          </div>
          <div class="logo-pulse"></div>
        </div>
        
        <h1>Smart MES</h1>
        <p>智能工厂执行系统</p>
        <div class="header-line"></div>
      </div>
      
      <el-form ref="loginFormRef" :model="loginForm" :rules="rules" @keyup.enter="handleLogin" class="login-form">
        <el-form-item prop="username">
          <div class="input-wrapper">
            <div class="input-icon">
              <User />
            </div>
            <el-input 
              v-model="loginForm.username" 
              placeholder="请输入用户名" 
              size="large"
              class="custom-input"
            />
          </div>
        </el-form-item>
        
        <el-form-item prop="password">
          <div class="input-wrapper">
            <div class="input-icon">
              <Lock />
            </div>
            <el-input 
              v-model="loginForm.password" 
              type="password" 
              placeholder="请输入密码" 
              size="large"
              class="custom-input"
              show-password
            />
          </div>
        </el-form-item>
        
        <div class="form-options">
          <el-checkbox v-model="rememberMe">记住密码</el-checkbox>
          <span class="forgot-link">忘记密码?</span>
        </div>
        
        <el-button 
          type="primary" 
          size="large" 
          :loading="loading" 
          class="login-btn" 
          @click="handleLogin"
        >
          <span v-if="!loading" class="btn-content">
            <span class="btn-text">登 录</span>
            <span class="btn-arrow">→</span>
          </span>
          <span v-else>登录中...</span>
        </el-button>
      </el-form>
      
      <div class="login-footer">
        <div class="tech-tags">
          <span class="tag">工业4.0</span>
          <span class="tag">智能制造</span>
          <span class="tag">数字化工厂</span>
        </div>
        <p class="copyright">© 2026 Smart Factory MES</p>
      </div>
    </div>
    
    <div class="decoration">
      <div class="deco-line"></div>
      <div class="deco-dot"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, FormInstance } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
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
.login-container {
  width: 100%;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #0a0e27 0%, #1a1f3a 50%, #0d1321 100%);
}

.login-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.bg-shape {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
}

.shape-1 {
  width: 600px;
  height: 600px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  top: -200px;
  right: -100px;
  animation: float 8s ease-in-out infinite;
}

.shape-2 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #06b6d4, #3b82f6);
  bottom: -100px;
  left: -100px;
  animation: float 10s ease-in-out infinite reverse;
}

.shape-3 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, #ec4899, #f43f5e);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation: pulse 4s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(30px, -30px); }
}

@keyframes pulse {
  0%, 100% { transform: translate(-50%, -50%) scale(1); opacity: 0.3; }
  50% { transform: translate(-50%, -50%) scale(1.2); opacity: 0.5; }
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image: 
    linear-gradient(rgba(99, 102, 241, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(99, 102, 241, 0.03) 1px, transparent 1px);
  background-size: 60px 60px;
}

.login-card {
  position: relative;
  width: 420px;
  padding: 48px 40px;
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 24px;
  box-shadow: 
    0 25px 50px -12px rgba(0, 0, 0, 0.5),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
  z-index: 10;
}

.card-glow {
  position: absolute;
  inset: -1px;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.3), rgba(236, 72, 153, 0.3));
  opacity: 0.5;
  z-index: -1;
  filter: blur(20px);
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.logo-wrapper {
  position: relative;
  width: 72px;
  height: 72px;
  margin: 0 auto 20px;
}

.logo-icon {
  width: 72px;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border-radius: 20px;
  color: #fff;
  box-shadow: 0 8px 32px rgba(99, 102, 241, 0.4);
}

.logo-icon svg {
  width: 36px;
  height: 36px;
}

.logo-pulse {
  position: absolute;
  inset: -4px;
  border-radius: 24px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  opacity: 0.4;
  animation: pulse 2s ease-in-out infinite;
}

.login-header h1 {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 8px;
  letter-spacing: 2px;
}

.login-header p {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 4px;
  text-transform: uppercase;
}

.header-line {
  width: 60px;
  height: 3px;
  background: linear-gradient(90deg, transparent, #6366f1, transparent);
  margin: 20px auto 0;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.input-wrapper {
  position: relative;
}

.input-icon {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 10;
  color: rgba(255, 255, 255, 0.4);
  font-size: 18px;
}

.input-wrapper :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 4px 16px;
  box-shadow: none;
}

.input-wrapper :deep(.el-input__wrapper:hover) {
  border-color: rgba(99, 102, 241, 0.5);
}

.input-wrapper :deep(.el-input__wrapper.is-focus) {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.2);
}

.input-wrapper :deep(.el-input__inner) {
  color: #fff;
  height: 44px;
}

.input-wrapper :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.3);
}

.input-wrapper :deep(.el-input__inner) {
  padding-left: 40px;
}

.input-wrapper :deep(.el-input__password) {
  color: rgba(255, 255, 255, 0.4);
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.form-options :deep(.el-checkbox__label) {
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
}

.form-options :deep(.el-checkbox__inner) {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
}

.forgot-link {
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
  cursor: pointer;
  transition: color 0.3s;
}

.forgot-link:hover {
  color: #6366f1;
}

.login-btn {
  width: 100%;
  height: 50px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 2px;
  color: #fff;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 20px rgba(99, 102, 241, 0.4);
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(99, 102, 241, 0.6);
}

.login-btn:active {
  transform: translateY(0);
}

.btn-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn-arrow {
  transition: transform 0.3s;
}

.login-btn:hover .btn-arrow {
  transform: translateX(4px);
}

.login-footer {
  margin-top: 32px;
  text-align: center;
}

.tech-tags {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 16px;
}

.tag {
  padding: 4px 12px;
  background: rgba(99, 102, 241, 0.1);
  border: 1px solid rgba(99, 102, 241, 0.2);
  border-radius: 20px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 1px;
}

.copyright {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.2);
}

.decoration {
  position: absolute;
  bottom: 40px;
  right: 40px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.deco-line {
  width: 60px;
  height: 1px;
  background: linear-gradient(90deg, #6366f1, transparent);
}

.deco-dot {
  width: 8px;
  height: 8px;
  background: #6366f1;
  border-radius: 50%;
  animation: blink 2s ease-in-out infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

html.light .login-container {
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 50%, #dfe4ec 100%);
}

html.light .shape-1,
html.light .shape-2,
html.light .shape-3 {
  opacity: 0.2;
}

html.light .login-card {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
}

html.light .login-header h1 {
  color: #1a1a2e;
}

html.light .login-header p {
  color: rgba(0, 0, 0, 0.5);
}

html.light .input-wrapper :deep(.el-input__wrapper) {
  background: rgba(0, 0, 0, 0.03);
  border-color: rgba(0, 0, 0, 0.1);
}

html.light .input-wrapper :deep(.el-input__inner) {
  color: #1a1a2e;
}

html.light .input-wrapper :deep(.el-input__inner::placeholder) {
  color: rgba(0, 0, 0, 0.3);
}

html.light .input-icon {
  color: rgba(0, 0, 0, 0.3);
}

html.light .form-options :deep(.el-checkbox__label) {
  color: rgba(0, 0, 0, 0.5);
}

html.light .forgot-link {
  color: rgba(0, 0, 0, 0.5);
}

html.light .tag {
  background: rgba(99, 102, 241, 0.1);
  color: rgba(0, 0, 0, 0.5);
}

html.light .copyright {
  color: rgba(0, 0, 0, 0.2);
}

html.light .bg-grid {
  background-image: 
    linear-gradient(rgba(99, 102, 241, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(99, 102, 241, 0.05) 1px, transparent 1px);
}
</style>