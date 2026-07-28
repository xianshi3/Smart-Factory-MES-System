<template>
  <div class="page-wrapper">
    <div class="page-header">
      <div class="header-left">
        <div class="page-title">
          <el-icon><User /></el-icon>
          <h1>用户管理</h1>
        </div>
        <p class="page-desc">系统用户与权限分配</p>
      </div>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        新增用户
      </el-button>
    </div>

    <div class="user-list">
      <div v-for="user in users" :key="user.id" class="user-card">
        <div class="user-header">
          <div class="user-avatar" :class="getAvatarClass(user.username)">
            {{ user.username?.charAt(0)?.toUpperCase() || 'U' }}
          </div>
          <div class="user-info">
            <span class="user-name">{{ user.nickname || user.username }}</span>
            <span class="user-username">@{{ user.username }}</span>
          </div>
          <el-tag :type="user.status === 1 ? 'success' : 'danger'" size="small">
            {{ user.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </div>
        <div class="user-details">
          <div class="detail-item">
            <span class="label">部门</span>
            <span class="value">{{ user.department || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="label">职位</span>
            <span class="value">{{ user.position || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="label">角色</span>
            <span class="value">{{ user.roleName || '普通用户' }}</span>
          </div>
        </div>
        <div class="user-footer">
          <span class="user-email">{{ user.email || '暂无邮箱' }}</span>
          <div class="user-actions">
            <el-button type="primary" size="small" link @click="handleEdit(user)">编辑</el-button>
            <el-button type="primary" size="small" link @click="handleRole(user)">角色</el-button>
            <el-button v-if="user.username !== 'admin'" type="danger" size="small" link @click="handleDelete(user)">删除</el-button>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="部门">
          <el-input v-model="form.department" placeholder="请输入部门" />
        </el-form-item>
        <el-form-item label="职位">
          <el-input v-model="form.position" placeholder="请输入职位" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleVisible" title="分配角色" width="400px">
      <el-form label-width="60px">
        <el-form-item label="角色">
          <el-select v-model="selectedRoleId" placeholder="请选择角色">
            <el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRoleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getUserList, createUser, updateUser, deleteUser, assignUserRole, getRoleList } from '@/api/system'

interface UserItem {
  id?: number
  username: string
  nickname?: string
  email?: string
  department?: string
  position?: string
  role?: string
  roleName?: string
  status?: number
  roleId?: number
}

interface Role {
  id: number
  roleName: string
  roleCode: string
}

const users = ref<UserItem[]>([])
const roles = ref<Role[]>([])

const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const roleVisible = ref(false)
const currentUserId = ref<number | null>(null)
const selectedRoleId = ref<number | null>(null)

const form = reactive({
  id: null as number | null,
  username: '',
  nickname: '',
  email: '',
  department: '',
  position: '',
  status: 1
})

const loadUsers = async () => {
  try {
    const res = await getUserList()
    users.value = res.data || res || []
  } catch {
    ElMessage.error('获取用户列表失败，请检查后端服务')
    users.value = []
  }
}

const loadRoles = async () => {
  try {
    const res = await getRoleList()
    roles.value = res.data || res || []
  } catch {
    ElMessage.error('获取角色列表失败，请检查后端服务')
    roles.value = []
  }
}

const getAvatarClass = (username: string) => {
  const colors = ['admin', 'manager', 'user', 'qc', 'engineer']
  const index = username?.charCodeAt(0) % 5 || 0
  return colors[index]
}

const handleCreate = () => {
  Object.assign(form, { id: null, username: '', nickname: '', email: '', department: '', position: '', status: 1 })
  dialogTitle.value = '新增用户'
  dialogVisible.value = true
}

const handleEdit = (user: UserItem) => {
  Object.assign(form, user)
  dialogTitle.value = '编辑用户'
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    if (form.id) {
      await updateUser(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await createUser(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadUsers()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '操作失败')
  }
}

const handleRole = (user: UserItem) => {
  currentUserId.value = user.id || null
  selectedRoleId.value = user.roleId || null
  roleVisible.value = true
}

const handleRoleSubmit = async () => {
  try {
    await assignUserRole(currentUserId.value!, { roleId: selectedRoleId.value })
    ElMessage.success('角色分配成功')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '角色分配失败')
  }
  roleVisible.value = false
  loadUsers()
}

const handleDelete = (user: UserItem) => {
  ElMessageBox.confirm(`确定删除用户 "${user.nickname || user.username}" 吗？`, '删除确认', { type: 'warning' })
    .then(async () => {
      try {
        await deleteUser(user.id!)
        ElMessage.success('删除成功')
        loadUsers()
      } catch (e: any) {
        ElMessage.error(e?.response?.data?.message || e?.message || '删除失败')
      }
    }).catch(() => {})
}

onMounted(() => {
  loadUsers()
  loadRoles()
})
</script>

<style scoped>
.page-wrapper {
  background: var(--bg-app);
  min-height: 100%;
}


.user-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.user-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 12px;
  padding: 20px;
  transition: all 0.2s ease;
}

.user-card:hover {
  border-color: var(--accent);
  box-shadow: var(--shadow-md);
}

.user-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.user-avatar {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  font-size: 20px;
  font-weight: 600;
}

.user-avatar.admin { background: var(--accent-light); color: var(--accent); }
.user-avatar.manager { background: var(--success-light); color: var(--success); }
.user-avatar.user { background: var(--info-light); color: var(--info); }
.user-avatar.qc { background: var(--warning-light); color: var(--warning); }
.user-avatar.engineer { background: var(--danger-light); color: var(--danger); }

.user-info { flex: 1; display: flex; flex-direction: column; }
.user-name { font-size: 16px; font-weight: 600; color: var(--text-primary); }
.user-username { font-size: 12px; color: var(--text-muted); }

.user-details {
  display: flex;
  gap: 16px;
  padding: 12px 0;
  border-top: 1px solid var(--border-light);
  border-bottom: 1px solid var(--border-light);
}

.detail-item { display: flex; flex-direction: column; gap: 2px; }
.detail-item .label { font-size: 11px; color: var(--text-muted); }
.detail-item .value { font-size: 13px; color: var(--text-secondary); }

.user-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.user-email { font-size: 12px; color: var(--text-muted); }
.user-actions { display: flex; gap: 8px; }

html.light .user-card { box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05); }
</style>