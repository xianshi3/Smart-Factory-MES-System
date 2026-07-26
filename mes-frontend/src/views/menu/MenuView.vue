<template>
  <div class="page-wrapper">
    <div class="page-header">
      <div class="header-left">
        <div class="page-title">
          <el-icon><Menu /></el-icon>
          <h1>菜单管理</h1>
        </div>
        <p class="page-desc">系统菜单与权限配置</p>
      </div>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        新增菜单
      </el-button>
    </div>

    <el-table :data="menus" row-key="id" :tree-props="{ children: 'children' }" default-expand-all>
      <el-table-column prop="menuName" label="菜单名称">
        <template #default="{ row }">{{ row.menuName }}</template>
      </el-table-column>
      <el-table-column prop="menuCode" label="菜单编码" />
      <el-table-column prop="path" label="路由路径" />
      <el-table-column prop="component" label="组件" />
      <el-table-column prop="icon" label="图标" width="80">
        <template #default="{ row }">
          <el-icon v-if="row.icon"><component :is="row.icon" /></el-icon>
        </template>
      </el-table-column>
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

    <!-- 菜单编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="菜单名称">
          <el-input v-model="form.menuName" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item label="菜单编码">
          <el-input v-model="form.menuCode" placeholder="请输入菜单编码" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="路由路径">
          <el-input v-model="form.path" placeholder="请输入路由路径" />
        </el-form-item>
        <el-form-item label="组件">
          <el-input v-model="form.component" placeholder="请输入组件路径" />
        </el-form-item>
        <el-form-item label="父级菜单">
          <el-select v-model="form.parentId" placeholder="请选择父级菜单" clearable>
            <el-option v-for="m in topMenus" :key="m.id" :label="m.menuName" :value="m.id" />
          </el-select>
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
import { Plus, Menu } from '@element-plus/icons-vue'
import request from '@/api'

interface Menu {
  id: number
  menuName: string
  menuCode: string
  path: string
  component: string
  parentId: number
  icon: string
  sort: number
  status: number
  children?: Menu[]
}

const menus = ref<Menu[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增菜单')
const form = ref<Partial<Menu>>({})

const topMenus = computed(() => menus.value.filter(m => !m.parentId || m.parentId === 0))

const loadData = async () => {
  try {
    const res = await request({ url: '/auth/menu/list', method: 'get' })
    const list = res.data?.data
    if (list) {
      menus.value = list
    } else {
      menus.value = res.data || res || []
    }
  } catch (e) {
    console.error('Load menu error:', e)
  }
}

const handleCreate = () => {
  form.value = { parentId: 0, sort: 0, status: 1 }
  dialogTitle.value = '新增菜单'
  dialogVisible.value = true
}

const handleEdit = (row: Menu) => {
  form.value = { ...row }
  dialogTitle.value = '编辑菜单'
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    if (form.value.id) {
      await request({ url: '/auth/menu', method: 'put', data: form.value })
      ElMessage.success('更新成功')
    } else {
      await request({ url: '/auth/menu', method: 'post', data: form.value })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

const handleDelete = async (row: Menu) => {
  try {
    await ElMessageBox.confirm(`确定删除菜单 "${row.menuName}" 吗?`, '提示', { type: 'warning' })
    await request({ url: `/auth/menu/${row.id}`, method: 'delete' })
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