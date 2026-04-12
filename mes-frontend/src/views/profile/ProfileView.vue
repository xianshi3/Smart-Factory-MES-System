<template>
  <div class="profile-container">
    <div class="page-header">
      <div class="header-title">
        <el-icon size="24"><User /></el-icon>
        <h1>个人中心</h1>
      </div>
    </div>

    <div class="profile-content">
      <div class="profile-card">
        <div class="avatar-section">
          <div class="avatar-wrapper">
            <el-icon size="48"><User /></el-icon>
          </div>
          <div class="user-info">
            <h2>{{ userStore.user?.username || '管理员' }}</h2>
            <p>{{ userStore.user?.role || '系统管理员' }}</p>
          </div>
        </div>
      </div>

      <div class="info-card">
        <div class="card-header">
          <span class="card-title">基本信息</span>
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="用户名">{{ userStore.user?.username || '-' }}</el-descriptions-item>
          <el-descriptions-item label="角色">{{ userStore.user?.role || '管理员' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ userStore.user?.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ userStore.user?.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最后登录">{{ userStore.user?.lastLoginTime || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="info-card">
        <div class="card-header">
          <span class="card-title">修改密码</span>
        </div>
        <el-form :model="passwordForm" label-width="100px" class="password-form">
          <el-form-item label="当前密码">
            <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
          </el-form-item>
          <el-form-item label="确认密码">
            <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请确认新密码" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleChangePassword">修改密码</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { User } from '@element-plus/icons-vue'

const userStore = useUserStore()

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const handleChangePassword = () => {
  if (!passwordForm.oldPassword) {
    ElMessage.warning('请输入当前密码')
    return
  }
  if (!passwordForm.newPassword) {
    ElMessage.warning('请输入新密码')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  ElMessage.success('密码修改成功')
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}
</script>

<style scoped>
.profile-container {
  padding: 24px;
  background: var(--bg-app);
  min-height: 100%;
}

.page-header {
  margin-bottom: 24px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--text-primary);
}

.header-title h1 {
  font-size: 24px;
  font-weight: 600;
}

.header-title .el-icon {
  color: var(--accent);
}

.profile-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 12px;
  padding: 24px;
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 20px;
}

.avatar-wrapper {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--accent-light);
  border-radius: 50%;
  color: var(--accent);
}

.user-info h2 {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.user-info p {
  color: var(--text-muted);
  font-size: 14px;
}

.info-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 12px;
  padding: 20px;
}

.card-header {
  margin-bottom: 16px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.password-form {
  max-width: 400px;
}

html.light .profile-card,
html.light .info-card {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}
</style>