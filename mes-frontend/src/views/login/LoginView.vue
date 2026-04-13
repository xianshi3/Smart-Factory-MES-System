<template>
  <div class="login-container">
    <div class="login-bg">
      <div class="bg-gradient"></div>
      <div class="bg-particles">
        <div class="particle" v-for="i in 20" :key="i" :style="{ '--delay': i * 0.5 + 's', '--x': Math.random() * 100 + '%' }"></div>
      </div>
      <div class="bg-lines">
        <div class="line" v-for="i in 5" :key="i" :style="{ '--delay': i * 0.3 + 's' }"></div>
      </div>
    </div>
    
    <div class="login-card">
      <div class="card-header">
        <div class="company-logo">
          <div class="logo-inner">
            <svg viewBox="0 0 24 24" fill="none">
              <path d="M12 2L2 7V17L12 22L22 17V7L12 2Z" stroke="currentColor" stroke-width="1.5"/>
              <path d="M12 22V12"/>
              <path d="M22 7L12 12L2 7"/>
              <circle cx="12" cy="12" r="2"/>
            </svg>
          </div>
        </div>
        <h1>Smart MES</h1>
        <p>智能工厂执行系统</p>
      </div>
      
      <div class="card-body">
        <div class="welcome-text">
          <h2>欢迎回来</h2>
          <span>请使用您的账号登录系统</span>
        </div>
        
        <el-form ref="loginFormRef" :model="loginForm" :rules="rules" @keyup.enter="handleLogin">
          <el-form-item prop="username">
            <el-input 
              v-model="loginForm.username" 
              placeholder="用户名 / 邮箱 / 手机号"
              prefix-icon="User"
              size="large"
              class="custom-input"
            />
          </el-form-item>
          
          <el-form-item prop="password">
            <el-input 
              v-model="loginForm.password" 
              type="password" 
              placeholder="请输入登录密码"
              prefix-icon="Lock"
              size="large"
              class="custom-input"
              show-password
            />
          </el-form-item>
          
          <div class="form-options">
            <el-checkbox v-model="rememberMe">下次自动登录</el-checkbox>
            <a href="javascript:;" class="forgot-link">忘记密码？</a>
          </div>
          
          <el-button 
            type="primary" 
            size="large" 
            :loading="loading" 
            class="login-btn"
            @click="handleLogin"
          >
            <span v-if="!loading">
              <span>立即登录</span>
              <el-icon class="arrow-icon"><ArrowRight /></el-icon>
            </span>
            <span v-else>正在登录...</span>
          </el-button>
        </el-form>
      </div>
      
      <div class="card-footer">
        <div class="security-tips">
          <el-icon><Lock /></el-icon>
          <span>您的数据安全由我们保障</span>
        </div>
      </div>
    </div>
    
    <div class="login-footer">
      <p>© 2026 Smart Factory MES · 智能制造解决方案提供商</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, FormInstance } from 'element-plus'
import { User, Lock, ArrowRight } from '@element-plus/icons-vue'
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
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background: #0a0a0f;
  padding: 20px;
}

.login-bg {
  position: fixed;
  inset: 0;
  overflow: hidden;
}

.bg-gradient {
  position: absolute;
  inset: 0;
  background: 
    radial-gradient(ellipse at 20% 20%, rgba(99, 102, 241, 0.15) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 80%, rgba(139, 92, 246, 0.1) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 50%, rgba(59, 130, 246, 0.08) 0%, transparent 70%);
}

.bg-particles {
  position: absolute;
  inset: 0;
}

.particle {
  position: absolute;
  width: 4px;
  height: 4px;
  background: rgba(99, 102, 241, 0.4);
  border-radius: 50%;
  left: var(--x);
  top: 50%;
  animation: particle-float 15s ease-in-out infinite;
  animation-delay: var(--delay);
}

@keyframes particle-float {
  0%, 100% { 
    transform: translateY(0) translateX(0); 
    opacity: 0;
  }
  10% { opacity: 1; }
  90% { opacity: 1; }
  50% { 
    transform: translateY(-100vh) translateX(50px); 
  }
}

.bg-lines {
  position: absolute;
  inset: 0;
  perspective: 1000px;
}

.line {
  position: absolute;
  width: 1px;
  height: 100%;
  background: linear-gradient(to bottom, transparent, rgba(99, 102, 241, 0.1), transparent);
  left: calc(var(--delay) * 20%);
  animation: line-scan 8s linear infinite;
}

@keyframes line-scan {
  0% { transform: translateY(-100%); }
  100% { transform: translateY(100%); }
}

.login-card {
  position: relative;
  width: 100%;
  max-width: 440px;
  background: rgba(18, 18, 24, 0.8);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 
    0 25px 50px -12px rgba(0, 0, 0, 0.5),
    0 0 0 1px rgba(255, 255, 255, 0.03) inset;
}

.login-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(99, 102, 241, 0.3), transparent);
}

.card-header {
  text-align: center;
  padding: 48px 40px 32px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
}

.company-logo {
  margin-bottom: 24px;
}

.logo-inner {
  width: 64px;
  height: 64px;
  margin: 0 auto;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 8px 32px rgba(99, 102, 241, 0.3);
}

.logo-inner svg {
  width: 32px;
  height: 32px;
}

.card-header h1 {
  font-size: 28px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 8px;
  letter-spacing: 2px;
}

.card-header p {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
  letter-spacing: 4px;
}

.card-body {
  padding: 40px;
}

.welcome-text {
  margin-bottom: 32px;
}

.welcome-text h2 {
  font-size: 20px;
  font-weight: 500;
  color: #fff;
  margin-bottom: 8px;
}

.welcome-text span {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-form :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  box-shadow: none;
  padding: 4px 12px;
}

.login-form :deep(.el-input__wrapper:hover) {
  border-color: rgba(99, 102, 241, 0.4);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15);
}

.login-form :deep(.el-input__inner) {
  height: 44px;
  color: #fff;
}

.login-form :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.3);
}

.login-form :deep(.el-input__prefix) {
  color: rgba(255, 255, 255, 0.3);
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
}

.form-options :deep(.el-checkbox__label) {
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
}

.form-options :deep(.el-checkbox__inner) {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.15);
}

.forgot-link {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
  text-decoration: none;
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
  font-size: 15px;
  font-weight: 500;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s;
}

.login-btn:hover {
  background: linear-gradient(135deg, #5558e3, #7c4de4);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.4);
}

.login-btn:active {
  transform: translateY(0);
}

.arrow-icon {
  transition: transform 0.3s;
}

.login-btn:hover .arrow-icon {
  transform: translateX(4px);
}

.card-footer {
  padding: 0 40px 32px;
}

.security-tips {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  background: rgba(99, 102, 241, 0.08);
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
}

.security-tips .el-icon {
  color: #6366f1;
}

.login-footer {
  position: fixed;
  bottom: 20px;
  left: 0;
  right: 0;
  text-align: center;
}

.login-footer p {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.2);
}

html.light .login-container {
  background: #f8f9fc;
}

html.light .login-card {
  background: rgba(255, 255, 255, 0.95);
  border-color: rgba(0, 0, 0, 0.06);
}

html.light .card-header h1 {
  color: #1a1a2e;
}

html.light .card-header p {
  color: rgba(0, 0, 0, 0.4);
}

html.light .welcome-text h2 {
  color: #1a1a2e;
}

html.light .welcome-text span {
  color: rgba(0, 0, 0, 0.4);
}

html.light .login-form :deep(.el-input__wrapper) {
  background: rgba(0, 0, 0, 0.02);
  border-color: rgba(0, 0, 0, 0.08);
}

html.light .login-form :deep(.el-input__inner) {
  color: #1a1a2e;
}

html.light .login-form :deep(.el-input__inner::placeholder) {
  color: rgba(0, 0, 0, 0.3);
}

html.light .form-options :deep(.el-checkbox__label) {
  color: rgba(0, 0, 0, 0.5);
}

html.light .forgot-link {
  color: rgba(0, 0, 0, 0.4);
}

html.light .security-tips {
  background: rgba(99, 102, 241, 0.08);
  color: rgba(0, 0, 0, 0.5);
}

html.light .login-footer p {
  color: rgba(0, 0, 0, 0.2);
}
</style>