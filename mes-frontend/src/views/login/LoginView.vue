<!-- Login Page Component - User authentication view -->
<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h1>智能工厂MES系统</h1>
        <p>Smart Factory Manufacturing Execution System</p>
      </div>
      <el-form ref="loginFormRef" :model="loginForm" :rules="rules" class="login-form">
        <el-form-item prop="username">
          <el-input 
            v-model="loginForm.username" 
            placeholder="用户名" 
            :prefix-icon="User"
            size="large"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input 
            v-model="loginForm.password" 
            type="password" 
            placeholder="密码" 
            :prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button 
            type="primary" 
            size="large" 
            :loading="loading" 
            class="login-button"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>
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

const loginForm = reactive({
  username: '',
  password: ''
})

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
        const msg = error?.response?.data?.message || error?.message || '登录失败'
        ElMessage.error(msg)
        console.error('Login error:', error)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped lang="scss">
.login-container {
  width: 100%;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #0f3460 0%, #1a1a2e 100%);
  
  .login-box {
    width: 400px;
    padding: 40px;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 12px;
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.1);
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
    
    .login-header {
      text-align: center;
      margin-bottom: 40px;
      
      h1 {
        font-size: 24px;
        color: #ffffff;
        margin-bottom: 8px;
      }
      
      p {
        font-size: 12px;
        color: rgba(255, 255, 255, 0.5);
      }
    }
    
    .login-form {
      :deep(.el-input__wrapper) {
        background: rgba(255, 255, 255, 0.05);
        border: 1px solid rgba(255, 255, 255, 0.1);
        box-shadow: none;
        
        &:hover, &:focus {
          border-color: #e94560;
        }
      }
      
      :deep(.el-input__inner) {
        color: #ffffff;
        
        &::placeholder {
          color: rgba(255, 255, 255, 0.4);
        }
      }
      
      .login-button {
        width: 100%;
        background: linear-gradient(135deg, #e94560 0%, #0f3460 100%);
        border: none;
        font-size: 16px;
        font-weight: bold;
        
        &:hover {
          opacity: 0.9;
        }
      }
    }
  }
}
</style>