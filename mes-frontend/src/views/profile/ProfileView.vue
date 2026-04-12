<template>
  <div class="profile-container">
    <div class="page-header">
      <div class="header-title">
        <el-icon size="24"><User /></el-icon>
        <h1>个人中心</h1>
      </div>
    </div>

    <div class="profile-content">
      <!-- 用户信息卡片 -->
      <div class="profile-card">
        <div class="profile-bg"></div>
        <div class="profile-main">
          <div class="avatar-section">
            <div class="avatar-wrapper">
              <el-icon size="40"><User /></el-icon>
            </div>
            <div class="avatar-edit">
              <el-icon><Camera /></el-icon>
            </div>
          </div>
          <div class="user-info">
            <h2>{{ userInfo.realName || userInfo.username || '管理员' }}</h2>
            <p class="user-role">{{ userInfo.role === 'ADMIN' ? '系统管理员' : userInfo.role === 'MANAGER' ? '生产主管' : '生产员工' }}</p>
            <div class="user-tags">
              <el-tag type="primary" size="small">{{ userInfo.status === 1 ? '在职' : '离职' }}</el-tag>
              <el-tag size="small">{{ userInfo.department || '生产部' }}</el-tag>
            </div>
          </div>
          <div class="profile-stats">
            <div class="stat-item">
              <span class="stat-value">156</span>
              <span class="stat-label">累计工单</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">98%</span>
              <span class="stat-label">完成率</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ userInfo.hireDate || '2025-01-15' }}</span>
              <span class="stat-label">入职时间</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 基本信息 -->
      <div class="info-section">
        <div class="section-header">
          <span class="section-title">
            <el-icon><Document /></el-icon>
            基本信息
          </span>
          <el-button type="primary" link @click="editBasicInfo">
            <el-icon><Edit /></el-icon>
            编辑
          </el-button>
        </div>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">用户名</span>
            <span class="info-value">{{ userInfo.username || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">员工编号</span>
            <span class="info-value">{{ userInfo.employeeNo || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">所属部门</span>
            <span class="info-value">{{ userInfo.department || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">岗位</span>
            <span class="info-value">{{ userInfo.position || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">手机号码</span>
            <span class="info-value">{{ userInfo.phone || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">电子邮箱</span>
            <span class="info-value">{{ userInfo.email || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">办公地点</span>
            <span class="info-value">总部工厂 - A栋3楼</span>
          </div>
          <div class="info-item">
            <span class="info-label">直接上级</span>
            <span class="info-value">{{ userInfo.managerId ? '张总监' : '-' }}</span>
          </div>
        </div>
      </div>

      <!-- 账号安全 -->
      <div class="info-section">
        <div class="section-header">
          <span class="section-title">
            <el-icon><Lock /></el-icon>
            账号安全
          </span>
        </div>
        <div class="security-list">
          <div class="security-item">
            <div class="security-icon password">
              <el-icon><Lock /></el-icon>
            </div>
            <div class="security-info">
              <span class="security-title">登录密码</span>
              <span class="security-desc">定期修改密码有助于账户安全</span>
            </div>
            <el-button type="primary" size="small" @click="showPasswordDialog = true">修改</el-button>
          </div>
          <div class="security-item">
            <div class="security-icon phone">
              <el-icon><Iphone /></el-icon>
            </div>
            <div class="security-info">
              <span class="security-title">手机绑定</span>
              <span class="security-desc">已绑定 138****8888</span>
            </div>
            <el-button type="primary" size="small">更换</el-button>
          </div>
          <div class="security-item">
            <div class="security-icon email">
              <el-icon><Message /></el-icon>
            </div>
            <div class="security-info">
              <span class="security-title">邮箱绑定</span>
              <span class="security-desc">已绑定 admin@smartmes.com</span>
            </div>
            <el-button type="primary" size="small">更换</el-button>
          </div>
          <div class="security-item">
            <div class="security-icon token">
              <el-icon><Key /></el-icon>
            </div>
            <div class="security-info">
              <span class="security-title">登录令牌</span>
              <span class="security-desc">当前登录状态，有效期至 2026-04-13</span>
            </div>
            <el-button type="danger" size="small" @click="handleLogoutAll">强制下线</el-button>
          </div>
        </div>
      </div>

      <!-- 快捷操作 -->
      <div class="info-section">
        <div class="section-header">
          <span class="section-title">
            <el-icon><Operation /></el-icon>
            快捷操作
          </span>
        </div>
        <div class="quick-actions">
          <div class="action-item" @click="router.push('/workorder')">
            <div class="action-icon">
              <el-icon><Document /></el-icon>
            </div>
            <span class="action-text">我的工单</span>
          </div>
          <div class="action-item" @click="router.push('/device')">
            <div class="action-icon">
              <el-icon><Monitor /></el-icon>
            </div>
            <span class="action-text">设备监控</span>
          </div>
          <div class="action-item" @click="router.push('/settings')">
            <div class="action-icon">
              <el-icon><Setting /></el-icon>
            </div>
            <span class="action-text">系统设置</span>
          </div>
          <div class="action-item" @click="handleExportData">
            <div class="action-icon">
              <el-icon><Download /></el-icon>
            </div>
            <span class="action-text">导出数据</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="showPasswordDialog" title="修改密码" width="450px">
      <el-form :model="passwordForm" label-width="100px" :rules="passwordRules" ref="passwordFormRef">
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请确认新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" @click="handleChangePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getUserInfo, updateProfile, changePassword } from '@/api/auth'
import { User, Camera, Document, Edit, Lock, Iphone, Message, Key, Operation, Monitor, Setting, Download } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const userInfo = ref<any>({
  username: '',
  realName: '',
  nickname: '',
  phone: '',
  email: '',
  employeeNo: '',
  department: '',
  position: '',
  managerId: null,
  hireDate: '',
  role: ''
})

const loading = ref(false)
const showPasswordDialog = ref(false)
const passwordFormRef = ref()

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule: any, value: string, callback: any) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const loadUserInfo = async () => {
  try {
    loading.value = true
    const res = await getUserInfo()
    userInfo.value = res.data || res
  } catch (e) {
    console.error('获取用户信息失败', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadUserInfo()
})

const editBasicInfo = () => {
  ElMessage.info('编辑功能开发中')
}

const handleChangePassword = async () => {
  try {
    await passwordFormRef.value?.validate()
    await changePassword(passwordForm)
    ElMessage.success('密码修改成功')
    showPasswordDialog.value = false
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (e: any) {
    ElMessage.error(e?.message || '修改失败')
  }
}

const handleLogoutAll = () => {
  ElMessageBox.confirm('确定要强制下线当前登录的所有设备吗？', '提示', { type: 'warning' })
    .then(() => {
      ElMessage.success('已强制下线其他设备')
    })
    .catch(() => {})
}

const handleExportData = () => {
  ElMessage.info('数据导出功能开发中')
}

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
  max-width: 1000px;
}

.profile-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 16px;
  overflow: hidden;
  position: relative;
}

.profile-bg {
  height: 120px;
  background: linear-gradient(135deg, var(--accent) 0%, var(--accent-secondary) 100%);
  opacity: 0.8;
}

.profile-main {
  display: flex;
  align-items: flex-start;
  gap: 24px;
  padding: 0 24px 24px;
  margin-top: -60px;
  position: relative;
}

.avatar-section {
  position: relative;
}

.avatar-wrapper {
  width: 100px;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-card);
  border: 4px solid var(--bg-card);
  border-radius: 50%;
  color: var(--accent);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.avatar-edit {
  position: absolute;
  bottom: 4px;
  right: 4px;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--accent);
  border-radius: 50%;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
}

.user-info {
  flex: 1;
  padding-top: 50px;
}

.user-info h2 {
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.user-role {
  color: var(--text-muted);
  font-size: 14px;
  margin-bottom: 12px;
}

.user-tags {
  display: flex;
  gap: 8px;
}

.profile-stats {
  display: flex;
  gap: 32px;
  padding-top: 60px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: 12px;
  color: var(--text-muted);
}

.info-section {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 12px;
  padding: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-light);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.section-title .el-icon {
  color: var(--accent);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.info-label {
  font-size: 12px;
  color: var(--text-muted);
}

.info-value {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.security-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.security-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: var(--bg-hover);
  border-radius: 10px;
}

.security-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  font-size: 20px;
}

.security-icon.password {
  background: var(--accent-light);
  color: var(--accent);
}

.security-icon.phone {
  background: var(--success-light);
  color: var(--success);
}

.security-icon.email {
  background: var(--info-light);
  color: var(--info);
}

.security-icon.token {
  background: var(--warning-light);
  color: var(--warning);
}

.security-info {
  flex: 1;
}

.security-title {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.security-desc {
  font-size: 12px;
  color: var(--text-muted);
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 20px;
  background: var(--bg-hover);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-item:hover {
  background: var(--accent-light);
  transform: translateY(-2px);
}

.action-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-card);
  border-radius: 12px;
  font-size: 24px;
  color: var(--accent);
}

.action-text {
  font-size: 13px;
  color: var(--text-secondary);
}

html.light .profile-card,
html.light .info-section {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

@media (max-width: 900px) {
  .info-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .quick-actions {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .profile-main {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
  .profile-stats {
    padding-top: 20px;
  }
  .user-tags {
    justify-content: center;
  }
}
</style>