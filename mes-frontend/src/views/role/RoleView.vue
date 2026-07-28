<template>
  <div class="page-wrapper">
    <div class="page-header">
      <div class="header-left">
        <div class="page-title">
          <el-icon><Key /></el-icon>
          <h1>角色管理</h1>
        </div>
        <p class="page-desc">系统角色与权限配置</p>
      </div>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        新增角色
      </el-button>
    </div>

    <div class="role-list">
      <div 
        v-for="role in roles" 
        :key="role.id" 
        class="role-card"
      >
        <div class="role-header">
          <div class="role-icon" :class="getRoleClass(role.roleCode)">
            <el-icon><User /></el-icon>
          </div>
          <div class="role-info">
            <span class="role-name">{{ role.roleName }}</span>
            <span class="role-code">{{ role.roleCode }}</span>
          </div>
          <el-tag :type="role.status === 1 ? 'success' : 'danger'" size="small">
            {{ role.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </div>
        <div class="role-desc">{{ role.description || '暂无描述' }}</div>
        <div class="role-footer">
          <span class="role-stats">权限数量: {{ role.permissionCount || 0 }}</span>
          <div class="role-actions">
            <el-button type="primary" size="small" link @click="handleEdit(role)">编辑</el-button>
            <el-button type="primary" size="small" link @click="handlePermission(role)">权限</el-button>
            <el-button v-if="role.roleCode !== 'ADMIN'" type="danger" size="small" link @click="handleDelete(role)">删除</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 角色编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="角色名称">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码">
          <el-input v-model="form.roleCode" placeholder="请输入角色编码" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
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

    <!-- 权限分配弹窗 -->
    <el-dialog v-model="permissionVisible" title="分配权限" width="600px">
      <div class="permission-tree">
        <el-tree
          ref="treeRef"
          :data="permissions"
          :props="{ label: 'permissionName', children: 'children' }"
          show-checkbox
          node-key="id"
          :default-checked-keys="checkedPermissions"
        />
      </div>
      <template #footer>
        <el-button @click="permissionVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePermissionSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Key, Plus, User } from '@element-plus/icons-vue'
import { getRoleList, getRolePermissionsTree, getRolePermissions, assignRolePermissions, createRole, updateRole, deleteRole } from '@/api/system'

const roles = ref<any[]>([])
const permissions = ref<any[]>([])
const checkedPermissions = ref<number[]>([])

const dialogVisible = ref(false)
const dialogTitle = ref('新增角色')
const permissionVisible = ref(false)
const currentRoleId = ref<number | null>(null)
const treeRef = ref()

const form = reactive({
  id: null as number | null,
  roleName: '',
  roleCode: '',
  description: '',
  status: 1
})

const loadRoles = async () => {
  try {
    const res = await getRoleList()
    roles.value = res.data || res || []
  } catch (e) {
    console.error('Load roles error:', e)
    ElMessage.error('加载角色失败')
    roles.value = []
  }
}

const loadPermissions = async () => {
  try {
    const res = await getRolePermissionsTree()
    permissions.value = res.data || res || []
  } catch {
    ElMessage.error('加载权限失败')
    permissions.value = []
  }
}

const getRoleClass = (code: string) => {
  const map: Record<string, string> = {
    ADMIN: 'admin',
    MANAGER: 'manager',
    USER: 'user',
    QC: 'qc',
    ENGINEER: 'engineer'
  }
  return map[code] || 'default'
}

const handleCreate = () => {
  Object.assign(form, { id: null, roleName: '', roleCode: '', description: '', status: 1 })
  dialogTitle.value = '新增角色'
  dialogVisible.value = true
}

const handleEdit = (role: any) => {
  Object.assign(form, role)
  dialogTitle.value = '编辑角色'
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    if (form.id) {
      await updateRole(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await createRole(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadRoles()
  } catch {
    ElMessage.error('操作失败')
    dialogVisible.value = false
  }
}

const handlePermission = async (role: any) => {
  currentRoleId.value = role.id
  try {
    const res = await getRolePermissions(role.id)
    checkedPermissions.value = res.data || []
  } catch {
    checkedPermissions.value = [11, 21, 22, 31]
  }
  permissionVisible.value = true
}

const handlePermissionSubmit = async () => {
  const checkedKeys = treeRef.value?.getCheckedKeys() || []
  try {
    await assignRolePermissions(currentRoleId.value!, checkedKeys)
    ElMessage.success('权限分配成功')
  } catch {
    ElMessage.error('权限分配失败')
  }
  permissionVisible.value = false
}

const handleDelete = (role: any) => {
  ElMessageBox.confirm(`确定删除角色 "${role.roleName}" 吗？`, '删除确认', { type: 'warning' })
    .then(async () => {
      try {
        await deleteRole(role.id)
        ElMessage.success('删除成功')
        loadRoles()
      } catch {
        ElMessage.error('删除失败')
      }
    }).catch(() => {})
}

onMounted(() => {
  loadRoles()
  loadPermissions()
})
</script>

<style scoped>
.page-wrapper {
  background: var(--bg-app);
  min-height: 100%;
}


.role-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.role-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 12px;
  padding: 20px;
  transition: all 0.2s ease;
}

.role-card:hover {
  border-color: var(--accent);
  box-shadow: var(--shadow-md);
}

.role-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.role-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  font-size: 24px;
}

.role-icon.admin { background: var(--accent-light); color: var(--accent); }
.role-icon.manager { background: var(--success-light); color: var(--success); }
.role-icon.user { background: var(--info-light); color: var(--info); }
.role-icon.qc { background: var(--warning-light); color: var(--warning); }
.role-icon.engineer { background: var(--danger-light); color: var(--danger); }

.role-info { flex: 1; display: flex; flex-direction: column; }
.role-name { font-size: 16px; font-weight: 600; color: var(--text-primary); }
.role-code { font-size: 12px; color: var(--text-muted); }

.role-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-light);
}

.role-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.role-stats { font-size: 12px; color: var(--text-muted); }
.role-actions { display: flex; gap: 8px; }

.permission-tree {
  max-height: 400px;
  overflow-y: auto;
}

html.light .role-card { box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05); }
</style>