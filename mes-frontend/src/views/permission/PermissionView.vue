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
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        新增权限
      </el-button>
    </div>

    <el-table :data="permissions" row-key="id" :tree-props="{ children: 'children' }" default-expand-all>
      <el-table-column prop="permissionName" label="权限名称" />
      <el-table-column prop="permissionCode" label="权限编码" />
      <el-table-column prop="permissionType" label="类型" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.permissionType === 'MENU'" type="primary" size="small">菜单</el-tag>
          <el-tag v-else-if="row.permissionType === 'BUTTON'" type="success" size="small">按钮</el-tag>
          <el-tag v-else type="warning" size="small">API</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="path" label="路径" />
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

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
          <el-select v-model="form.permissionType">
            <el-option label="菜单" value="MENU" />
            <el-option label="按钮" value="BUTTON" />
            <el-option label="API" value="API" />
          </el-select>
        </el-form-item>
        <el-form-item label="父级权限">
          <el-select v-model="form.parentId" placeholder="请选择父级权限" clearable>
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
import { Plus, Lock } from '@element-plus/icons-vue'
import request from '@/api'

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
const dialogVisible = ref(false)
const dialogTitle = ref('新增权限')
const form = ref<Partial<Permission>>({})

const topPermissions = computed(() => permissions.value.filter(p => !p.parentId || p.parentId === 0))

const loadData = async () => {
  try {
    const res = await request({ url: '/auth/permission/list', method: 'get' })
    permissions.value = res.data?.data || res?.data || []
  } catch (e) {
    console.error(e)
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
  try {
    if (form.value.id) {
      await request({ url: '/auth/permission', method: 'put', data: form.value })
      ElMessage.success('更新成功')
    } else {
      await request({ url: '/auth/permission', method: 'post', data: form.value })
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
    await request({ url: `/auth/permission/${row.id}`, method: 'delete' })
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
  padding: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.header-left {
  display: flex;
  flex-direction: column;
}
.page-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 600;
}
.page-desc {
  color: #999;
  font-size: 14px;
  margin-top: 4px;
}
</style>