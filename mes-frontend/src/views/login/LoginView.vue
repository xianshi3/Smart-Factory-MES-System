<template>
  <div class="login-wrapper">
    <div class="login-main">
      <div class="login-brand">
        <div class="brand-icon">
          <Monitor :size="32" />
        </div>
        <div class="brand-info">
          <h1>Smart MES</h1>
          <p>智能工厂执行系统</p>
        </div>
      </div>
      
      <div class="login-box">
        <div class="login-header">
          <h2>账号登录</h2>
          <p>请输入您的账号信息</p>
        </div>
        
        <el-form ref="loginFormRef" :model="loginForm" :rules="rules" @keyup.enter="handleLogin">
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
          
          <div class="login-options">
            <el-checkbox v-model="rememberMe">记住登录</el-checkbox>
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
      
      <div class="login-footer">
        <p>© 2026 Smart Factory MES</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, FormInstance } from 'element-plus'
import { Monitor, User, Lock } from '@element-plus/icons-vue'
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
.login-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-main {
  width: 100%;
  max-width: 420px;
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 32px;
  color: #fff;
}

.brand-icon {
  width: 56px;
  height: 56px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-info h1 {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 4px;
}

.brand-info p {
  font-size: 13px;
  opacity: 0.8;
}

.login-box {
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.login-header {
  margin-bottom: 32px;
}

.login-header h2 {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.login-header p {
  font-size: 14px;
  color: #909399;
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

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.login-options :deep(.el-checkbox__label) {
  color: #606266;
  font-size: 14px;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  border-radius: 8px;
}

.login-footer {
  text-align: center;
  margin-top: 24px;
}

.login-footer p {
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
}

html.dark .login-wrapper {
  background: linear-gradient(135deg, #1e3a5f 0%, #0d2137 100%);
}

html.dark .login-box {
  background: #1a1a2e;
}

html.dark .login-header h2 {
  color: #fff;
}

html.dark .login-header p {
  color: #909399;
}

html.dark .login-options :deep(.el-checkbox__label) {
  color: #909399;
}
</style>