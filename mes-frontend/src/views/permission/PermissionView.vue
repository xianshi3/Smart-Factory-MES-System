<template>
  <div class="page-wrapper">
    <div class="page-header">
      <div class="header-left">
        <div class="page-title">
          <el-icon><Lock /></el-icon>
          <h1>权限管理</h1>
        </div>
        <p class="page-desc">系统权限与资源管理</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" class="create-btn" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          新增权限
        </el-button>
      </div>
    </div>

    <div class="table-panel">
      <div v-if="loading && !permissions.length" class="skeleton-wrap">
        <div v-for="i in 6" :key="i" class="skeleton-row">
          <el-skeleton animated>
            <template #template>
              <el-skeleton-item variant="rect" style="height: 44px; border-radius: 6px" />
            </template>
          </el-skeleton>
        </div>
      </div>
      <el-table v-else v-loading="loading" :data="permissions" row-key="id" :tree-props="{ children: 'children' }" default-expand-all style="width: 100%" empty-text="暂无权限数据">
        <el-table-column prop="permissionName" label="权限名称" min-width="180">
          <template #default="{ row }">
            <div class="perm-name">
              <el-icon class="perm-icon"><component :is="permIcon(row.permissionType)" /></el-icon>
              <span>{{ row.permissionName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="permissionCode" label="权限编码" min-width="160">
          <template #default="{ row }">
            <span class="cell-code">{{ row.permissionCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="permissionType" label="类型" width="100" align="center">
          <template #default="{ row }">
            <span :class="['status-tag', `status-tag--${(row.permissionType || 'API').toLowerCase()}`]">
              {{ typeLabel(row.permissionType) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路径" min-width="160">
          <template #default="{ row }">
            <span class="path-cell">{{ row.path || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="70" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <span :class="['status-tag', row.status === 1 ? 'status-tag--enabled' : 'status-tag--disabled']">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" link size="small" @click="handleEdit(row)">
                <el-icon><Edit /></el-icon>编辑
              </el-button>
              <el-button type="danger" link size="small" @click="handleDelete(row)">
                <el-icon><Delete /></el-icon>删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 权限编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="权限名称">
          <el-input v-model="form.permissionName" placeholder="请输入权限名称" />
        </el-form-item>
        <el-form-item label="权限编码">
          <el-input v-model="form.permissionCode" placeholder="请输入权限编码" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="权限类型">
          <el-select v-model="form.permissionType" style="width: 100%">
            <el-option label="菜单" value="MENU" />
            <el-option label="按钮" value="BUTTON" />
            <el-option label="API" value="API" />
          </el-select>
        </el-form-item>
        <el-form-item label="父级权限">
          <el-select v-model="form.parentId" placeholder="请选择父级权限" clearable style="width: 100%">
            <el-option v-for="p in topPermissions" :key="p.id" :label="p.permissionName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="路径">
          <el-input v-model="form.path" placeholder="请输入路径" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Lock, Menu, Grid, Connection, Edit, Delete } from '@element-plus/icons-vue'
import { getPermissionList, createPermission, updatePermission, deletePermission } from '@/api/system'

interface Permission {
  id: number
  permissionName: string
  permissionCode: string
  permissionType: string
  parentId: number
  path: string
  sort: number
  status: number
  children?: Permission[]
}

const permissions = ref<Permission[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增权限')
const form = ref<Partial<Permission>>({})

const topPermissions = computed(() => permissions.value.filter(p => !p.parentId || p.parentId === 0))

const typeLabel = (t: string) => {
  const map: Record<string, string> = { MENU: '菜单', BUTTON: '按钮', API: 'API' }
  return map[t] || t
}

const permIcon = (t: string) => {
  if (t === 'MENU') return Menu
  if (t === 'BUTTON') return Grid
  return Connection
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getPermissionList()
    permissions.value = res.data?.data || res?.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  form.value = { parentId: 0, sort: 0, status: 1, permissionType: 'MENU' }
  dialogTitle.value = '新增权限'
  dialogVisible.value = true
}

const handleEdit = (row: Permission) => {
  form.value = { ...row }
  dialogTitle.value = '编辑权限'
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!form.value.permissionName || !form.value.permissionCode) {
    ElMessage.warning('请填写权限名称和编码')
    return
  }
  try {
    if (form.value.id) {
      await updatePermission(form.value)
      ElMessage.success('更新成功')
    } else {
      await createPermission(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

const handleDelete = async (row: Permission) => {
  try {
    await ElMessageBox.confirm(`确定删除权限 "${row.permissionName}" 吗?`, '提示', { type: 'warning' })
    await deletePermission(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '删除失败')
    }
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.page-wrapper {
  background: var(--bg-app);
  min-height: 100%;
}

.create-btn { height: 36px; padding: 0 16px; border-radius: var(--radius-md); }

.table-panel {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.table-panel :deep(.el-table th) { font-weight: 600; color: var(--text-secondary); font-size: 13px; }

.skeleton-wrap { display: flex; flex-direction: column; gap: 8px; padding: 16px; }

.perm-name { display: flex; align-items: center; gap: 8px; color: var(--text-primary); }
.perm-icon { color: var(--accent); flex-shrink: 0; }

.cell-code { font-family: SF Mono, Consolas, monospace; color: var(--accent); font-weight: 500; }
.path-cell { font-family: SF Mono, Consolas, monospace; font-size: 12px; color: var(--text-secondary); }

.status-tag--menu { background: var(--accent-light); color: var(--accent); }
.status-tag--button { background: var(--success-light); color: var(--success); }
.status-tag--api { background: var(--warning-light); color: var(--warning); }
.status-tag--enabled { background: var(--success-light); color: var(--success); }
.status-tag--disabled { background: var(--danger-light); color: var(--danger); }

.action-buttons { display: flex; gap: 4px; }

html.light .table-panel { box-shadow: var(--shadow-sm); }
</style>
