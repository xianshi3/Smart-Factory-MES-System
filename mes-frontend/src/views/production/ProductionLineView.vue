<template>
  <div class="page-wrapper">
    <div class="page-header">
      <div class="header-left">
        <div class="page-title">
          <el-icon><Connection /></el-icon>
          <h1>生产线管理</h1>
        </div>
        <p class="page-desc">生产线配置与管理</p>
      </div>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        新增生产线
      </el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="lineCode" label="生产线编码" width="150" />
      <el-table-column prop="lineName" label="生产线名称" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'NORMAL' ? 'success' : 'warning'" size="small">
            {{ row.status === 'NORMAL' ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="450px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="生产线编码">
          <el-input v-model="form.lineCode" placeholder="如: LINE-001" />
        </el-form-item>
        <el-form-item label="生产线名称">
          <el-input v-model="form.lineName" placeholder="如: 总装生产线" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option label="正常" value="NORMAL" />
            <el-option label="停用" value="STOPPED" />
          </el-select>
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
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Connection } from '@element-plus/icons-vue'
import { getProductionLineList, createProductionLine, updateProductionLine, deleteProductionLine } from '@/api/dashboard'

interface ProductionLine {
  id: number
  lineCode: string
  lineName: string
  status: string
  createTime: string
}

const list = ref<ProductionLine[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增生产线')
const form = ref<Partial<ProductionLine>>({ id: 0, lineCode: '', lineName: '', status: 'NORMAL' })

const loadData = async () => {
  loading.value = true
  try {
    const res = await getProductionLineList()
    list.value = res.data?.data || res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  form.value = { status: 'NORMAL' }
  dialogTitle.value = '新增生产线'
  dialogVisible.value = true
}

const handleEdit = (row: ProductionLine) => {
  form.value = { ...row }
  dialogTitle.value = '编辑生产线'
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    if (form.value.id) {
      await updateProductionLine(form.value)
      ElMessage.success('更新成功')
    } else {
      await createProductionLine(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

const handleDelete = async (row: ProductionLine) => {
  try {
    await ElMessageBox.confirm(`确定删除生产线 "${row.lineName}" 吗?`, '提示', { type: 'warning' })
    await deleteProductionLine(row.id)
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
</style>