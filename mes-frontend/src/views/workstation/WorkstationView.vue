<template>
  <div class="page-wrapper">
    <div class="page-header">
      <div class="header-left">
        <div class="page-title">
          <el-icon><Location /></el-icon>
          <h1>工位管理</h1>
        </div>
        <p class="page-desc">工位配置与管理</p>
      </div>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        新增工位
      </el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="workstationCode" label="工位编码" width="150" />
      <el-table-column prop="workstationName" label="工位名称" />
      <el-table-column prop="productionLineName" label="所属生产线" width="150" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'IDLE' ? 'success' : row.status === 'RUNNING' ? 'primary' : 'info'" size="small">
            {{ statusMap[row.status] || row.status }}
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
        <el-form-item label="工位编码">
          <el-input v-model="form.workstationCode" placeholder="如: WS-001" />
        </el-form-item>
        <el-form-item label="工位名称">
          <el-input v-model="form.workstationName" placeholder="如: 加工工位1" />
        </el-form-item>
        <el-form-item label="所属生产线">
          <el-select v-model="form.productionLineId" placeholder="请选择生产线">
            <el-option v-for="line in productionLines" :key="line.id" :label="line.lineName" :value="line.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option label="空闲" value="IDLE" />
            <el-option label="运行中" value="RUNNING" />
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
import { Plus, Location } from '@element-plus/icons-vue'
import { getProductionLineList, getWorkstationList, createWorkstation, updateWorkstation, deleteWorkstation } from '@/api/dashboard'

interface Workstation {
  id: number
  workstationCode: string
  workstationName: string
  productionLineId: number
  productionLineName?: string
  status: string
  createTime: string
}

interface ProductionLine {
  id: number
  lineName: string
}

const list = ref<Workstation[]>([])
const productionLines = ref<ProductionLine[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增工位')
const form = ref<Partial<Workstation>>({ id: 0, workstationCode: '', workstationName: '', productionLineId: 0, status: 'IDLE' })

const statusMap: Record<string, string> = {
  IDLE: '空闲',
  RUNNING: '运行中',
  STOPPED: '停用'
}

const loadProductionLines = async () => {
  try {
    const res = await getProductionLineList()
    productionLines.value = res.data?.data || res.data || []
  } catch (e) {
    console.error(e)
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getWorkstationList()
    const stations = res.data?.data || res.data || []
    list.value = stations.map((s: Workstation) => {
      const line = productionLines.value.find(p => p.id === s.productionLineId)
      return { ...s, productionLineName: line?.lineName || '-' }
    })
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  form.value = { status: 'IDLE' }
  dialogTitle.value = '新增工位'
  dialogVisible.value = true
}

const handleEdit = (row: Workstation) => {
  form.value = { ...row }
  dialogTitle.value = '编辑工位'
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    if (form.value.id) {
      await updateWorkstation(form.value)
      ElMessage.success('更新成功')
    } else {
      await createWorkstation(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

const handleDelete = async (row: Workstation) => {
  try {
    await ElMessageBox.confirm(`确定删除工位 "${row.workstationName}" 吗?`, '提示', { type: 'warning' })
    await deleteWorkstation(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '删除失败')
    }
  }
}

onMounted(async () => {
  await loadProductionLines()
  loadData()
})
</script>

<style scoped>
.page-wrapper { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.header-left { display: flex; flex-direction: column; }
.page-title { display: flex; align-items: center; gap: 8px; font-size: 20px; font-weight: 600; }
.page-desc { color: #999; font-size: 14px; margin-top: 4px; }
</style>