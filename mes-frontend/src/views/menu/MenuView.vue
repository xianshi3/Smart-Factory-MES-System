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
      <div class="header-actions">
        <el-button type="primary" class="create-btn" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          新增菜单
        </el-button>
      </div>
    </div>

    <div class="table-panel">
      <div v-if="loading && !menus.length" class="skeleton-wrap">
        <div v-for="i in 6" :key="i" class="skeleton-row">
          <el-skeleton animated>
            <template #template>
              <el-skeleton-item variant="rect" style="height: 44px; border-radius: 6px" />
            </template>
          </el-skeleton>
        </div>
      </div>
      <el-table v-else v-loading="loading" :data="menus" row-key="id" :tree-props="{ children: 'children' }" default-expand-all style="width: 100%" empty-text="暂无菜单数据">
        <el-table-column prop="menuName" label="菜单名称" min-width="180">
          <template #default="{ row }">
            <div class="menu-name">
              <el-icon v-if="row.icon" class="menu-icon"><component :is="row.icon" /></el-icon>
              <span>{{ row.menuName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="menuCode" label="菜单编码" min-width="140">
          <template #default="{ row }">
            <span class="cell-code">{{ row.menuCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由路径" min-width="140">
          <template #default="{ row }">
            <span class="path-cell">{{ row.path || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="component" label="组件" min-width="160">
          <template #default="{ row }">
            <span class="path-cell">{{ row.component || '-' }}</span>
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
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="如: Monitor / Connection" />
        </el-form-item>
        <el-form-item label="父级菜单">
          <el-select v-model="form.parentId" placeholder="请选择父级菜单" clearable style="width: 100%">
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
import { Plus, Menu, Edit, Delete } from '@element-plus/icons-vue'
import { getMenuList, createMenu, updateMenu, deleteMenu } from '@/api/system'

interface MenuItem {
  id: number
  menuName: string
  menuCode: string
  path: string
  component: string
  parentId: number
  icon: string
  sort: number
  status: number
  children?: MenuItem[]
}

const menus = ref<MenuItem[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增菜单')
const form = ref<Partial<MenuItem>>({})

const topMenus = computed(() => menus.value.filter(m => !m.parentId || m.parentId === 0))

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMenuList()
    const list = res.data?.data
    if (list) {
      menus.value = list
    } else {
      menus.value = res.data || res || []
    }
  } catch (e) {
    console.error('Load menu error:', e)
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  form.value = { parentId: 0, sort: 0, status: 1 }
  dialogTitle.value = '新增菜单'
  dialogVisible.value = true
}

const handleEdit = (row: MenuItem) => {
  form.value = { ...row }
  dialogTitle.value = '编辑菜单'
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!form.value.menuName || !form.value.menuCode) {
    ElMessage.warning('请填写菜单名称和编码')
    return
  }
  try {
    if (form.value.id) {
      await updateMenu(form.value)
      ElMessage.success('更新成功')
    } else {
      await createMenu(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

const handleDelete = async (row: MenuItem) => {
  try {
    await ElMessageBox.confirm(`确定删除菜单 "${row.menuName}" 吗?`, '提示', { type: 'warning' })
    await deleteMenu(row.id)
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

.menu-name { display: flex; align-items: center; gap: 8px; color: var(--text-primary); }
.menu-icon { color: var(--accent); flex-shrink: 0; }

.cell-code { font-family: SF Mono, Consolas, monospace; color: var(--accent); font-weight: 500; }
.path-cell { font-family: SF Mono, Consolas, monospace; font-size: 12px; color: var(--text-secondary); }

.status-tag--enabled { background: var(--success-light); color: var(--success); }
.status-tag--disabled { background: var(--danger-light); color: var(--danger); }

.action-buttons { display: flex; gap: 4px; }

html.light .table-panel { box-shadow: var(--shadow-sm); }
</style>
