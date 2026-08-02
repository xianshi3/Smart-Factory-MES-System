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
      <div class="header-actions">
        <el-button type="primary" class="create-btn" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          新增角色
        </el-button>
      </div>
    </div>

    <div class="stats-grid">
      <div class="stat-card" :class="{ active: statusFilter === '' }" @click="setFilter('')">
        <div class="stat-icon-wrap info"><el-icon size="22"><Key /></el-icon></div>
        <div class="stat-content">
          <div class="stat-value">{{ roles.length }}</div>
          <div class="stat-label">角色总数</div>
        </div>
      </div>
      <div class="stat-card" :class="{ active: statusFilter === 1 }" @click="setFilter(1)">
        <div class="stat-icon-wrap success"><el-icon size="22"><CircleCheck /></el-icon></div>
        <div class="stat-content">
          <div class="stat-value">{{ enabledCount }}</div>
          <div class="stat-label">已启用</div>
        </div>
      </div>
      <div class="stat-card" :class="{ active: statusFilter === 0 }" @click="setFilter(0)">
        <div class="stat-icon-wrap warning"><el-icon size="22"><CircleClose /></el-icon></div>
        <div class="stat-content">
          <div class="stat-value">{{ disabledCount }}</div>
          <div class="stat-label">已禁用</div>
        </div>
      </div>
    </div>

    <div v-if="loading && !roles.length" class="role-list">
      <div v-for="i in 6" :key="i" class="skeleton-card">
        <el-skeleton animated>
          <template #template>
            <el-skeleton-item variant="rect" style="height: 168px; border-radius: var(--radius-lg)" />
          </template>
        </el-skeleton>
      </div>
    </div>

    <div v-else class="role-list">
      <div
        v-for="role in filteredRoles"
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
          <span :class="['status-tag', role.status === 1 ? 'status-tag--enabled' : 'status-tag--disabled']">
            {{ role.status === 1 ? '启用' : '禁用' }}
          </span>
        </div>
        <div class="role-desc">{{ role.description || '暂无描述' }}</div>
        <div class="role-footer">
          <span class="role-stats">
            <el-icon :size="12"><Lock /></el-icon>
            权限数量: {{ role.permissionCount || 0 }}
          </span>
          <div class="role-actions">
            <el-button type="primary" size="small" link @click="handleEdit(role)">
              <el-icon><Edit /></el-icon>编辑
            </el-button>
            <el-button type="success" size="small" link @click="handlePermission(role)">
              <el-icon><Key /></el-icon>权限
            </el-button>
            <el-button v-if="role.roleCode !== 'ADMIN'" type="danger" size="small" link @click="handleDelete(role)">
              <el-icon><Delete /></el-icon>删除
            </el-button>
          </div>
        </div>
      </div>
      <div v-if="!filteredRoles.length" class="empty-state">
        <el-icon :size="40"><Key /></el-icon>
        <span>暂无角色数据</span>
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
    <el-dialog v-model="permissionVisible" title="分配权限" width="600px" top="8vh">
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
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Key, Plus, User, Lock, Edit, Delete, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { getRoleList, getRolePermissionsTree, getRolePermissions, assignRolePermissions, createRole, updateRole, deleteRole } from '@/api/system'

const roles = ref<any[]>([])
const permissions = ref<any[]>([])
const checkedPermissions = ref<number[]>([])
const loading = ref(false)
const statusFilter = ref<number | ''>('')

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

const enabledCount = computed(() => roles.value.filter(r => r.status === 1).length)
const disabledCount = computed(() => roles.value.filter(r => r.status === 0).length)

const filteredRoles = computed(() => {
  if (statusFilter.value === '') return roles.value
  return roles.value.filter(r => r.status === statusFilter.value)
})

const setFilter = (status: number | '') => { statusFilter.value = status }

const loadRoles = async () => {
  loading.value = true
  try {
    const res = await getRoleList()
    roles.value = res.data || res || []
  } catch (e) {
    console.error('Load roles error:', e)
    ElMessage.error('加载角色失败')
    roles.value = []
  } finally {
    loading.value = false
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
  if (!form.roleName || !form.roleCode) {
    ElMessage.warning('请填写角色名称和编码')
    return
  }
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
  const tree = treeRef.value
  if (!tree) return
  const checkedKeys = [...(tree.getCheckedKeys() || []), ...(tree.getHalfCheckedKeys() || [])]
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

.create-btn { height: 36px; padding: 0 16px; border-radius: var(--radius-md); }

.stat-card {
  position: relative;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  overflow: hidden;
  transition: all var(--transition-normal);
  animation: fadeInUp 0.4s ease both;
}

.stat-card:hover {
  transform: translateY(-3px);
  border-color: var(--accent);
  box-shadow: var(--shadow-md);
}

.stat-card.active {
  border-color: var(--accent);
  background: var(--accent-light);
}

.stat-icon-wrap {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  flex-shrink: 0;
}

.stat-icon-wrap.info { background: var(--info-light); color: var(--info); }
.stat-icon-wrap.success { background: var(--success-light); color: var(--success); }
.stat-icon-wrap.warning { background: var(--warning-light); color: var(--warning); }

.stat-content { flex: 1; }
.stat-value { font-size: 26px; font-weight: 700; color: var(--text-primary); line-height: 1.2; }
.stat-label { font-size: 13px; color: var(--text-muted); margin-top: 3px; }

.role-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.role-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 20px;
  transition: all var(--transition-normal);
  animation: fadeInUp 0.4s ease both;
}

.role-card:hover {
  border-color: var(--accent);
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
}

.skeleton-card { width: 100%; }

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
  flex-shrink: 0;
}

.role-icon.admin { background: var(--accent-light); color: var(--accent); }
.role-icon.manager { background: var(--success-light); color: var(--success); }
.role-icon.user { background: var(--info-light); color: var(--info); }
.role-icon.qc { background: var(--warning-light); color: var(--warning); }
.role-icon.engineer { background: var(--danger-light); color: var(--danger); }
.role-icon.default { background: var(--bg-hover); color: var(--text-secondary); }

.role-info { flex: 1; display: flex; flex-direction: column; }
.role-name { font-size: 16px; font-weight: 600; color: var(--text-primary); }
.role-code { font-size: 12px; color: var(--text-muted); font-family: monospace; }

.status-tag--enabled { background: var(--success-light); color: var(--success); }
.status-tag--disabled { background: var(--danger-light); color: var(--danger); }

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

.role-stats {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-muted);
}

.role-actions { display: flex; gap: 4px; }

.permission-tree {
  max-height: 400px;
  overflow-y: auto;
  padding: 8px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: var(--bg-hover);
}

html.light .role-card, html.light .stat-card { box-shadow: var(--shadow-sm); }
html.light .role-card:hover { box-shadow: var(--shadow-md); }

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(15px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
