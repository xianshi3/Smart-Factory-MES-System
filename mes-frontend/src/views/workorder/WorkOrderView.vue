<!-- Work Order Management View -->
<template>
  <div class="workorder-container">
    <page-header title="工单管理">
      <template #actions>
        <el-button type="primary" @click="handleCreate">新建工单</el-button>
      </template>
    </page-header>
    
    <div class="search-bar">
      <el-input v-model="searchForm.keyword" placeholder="工单编号" clearable style="width: 200px;" />
      <el-select v-model="searchForm.status" placeholder="状态" clearable style="width: 150px;">
        <el-option label="已创建" value="CREATED" />
        <el-option label="已下发" value="ISSUED" />
        <el-option label="生产中" value="IN_PRODUCTION" />
        <el-option label="待质检" value="PENDING_QC" />
        <el-option label="已完成" value="COMPLETED" />
        <el-option label="已关闭" value="CLOSED" />
      </el-select>
      <el-button type="primary" @click="loadData">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>
    
    <el-table :data="tableData" style="width: 100%; margin-top: 20px;" v-loading="loading">
      <el-table-column prop="id" label="ID" width="180" />
      <el-table-column prop="orderNo" label="工单编号" />
      <el-table-column prop="productName" label="产品名称" />
      <el-table-column prop="planQuantity" label="计划数量" />
      <el-table-column prop="status" label="状态">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="进度">
        <template #default="{ row }">
          {{ row.completedQuantity || 0 }} / {{ row.planQuantity }}
        </template>
      </el-table-column>
      <el-table-column prop="plannedStartTime" label="计划开始" />
      <el-table-column prop="plannedEndTime" label="计划结束" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleDetail(row)">详情</el-button>
          <el-button type="success" link @click="handleStart(row)" v-if="row.status === 'CREATED' || row.status === 'ISSUED'">开始</el-button>
          <el-button type="warning" link @click="handleIssue(row)" v-if="row.status === 'CREATED'">下发</el-button>
          <el-button type="danger" link @click="handleDelete(row)" v-if="row.status === 'CREATED' || row.status === 'CLOSED'">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <el-pagination
      v-model:current-page="pagination.page"
      v-model:page-size="pagination.size"
      :total="pagination.total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      style="margin-top: 20px; justify-content: flex-end;"
      @size-change="loadData"
      @current-change="loadData"
    />
    
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="产品名称" prop="productName">
          <el-input v-model="form.productName" />
        </el-form-item>
        <el-form-item label="产品型号" prop="productModel">
          <el-input v-model="form.productModel" />
        </el-form-item>
        <el-form-item label="计划数量" prop="planQuantity">
          <el-input-number v-model="form.planQuantity" :min="1" />
        </el-form-item>
        <el-form-item label="工位" prop="workstationId">
          <el-select v-model="form.workstationId" placeholder="请选择工位">
            <el-option label="工位1" :value="1" />
            <el-option label="工位2" :value="2" />
            <el-option label="工位3" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="工艺模板" prop="processTemplateId">
          <el-select v-model="form.processTemplateId" placeholder="请选择">
            <el-option label="CNC加工工艺" :value="1" />
            <el-option label="组装工艺" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="form.priority">
            <el-option label="低" value="LOW" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="高" value="HIGH" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划开始" prop="plannedStartTime">
          <el-date-picker v-model="form.plannedStartTime" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="计划结束" prop="plannedEndTime">
          <el-date-picker v-model="form.plannedEndTime" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/common/PageHeader.vue'
import { getWorkOrderPage, createWorkOrder, issueWorkOrder, startWorkOrder, deleteWorkOrder } from '@/api/services'

const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新建工单')
const tableData = ref<any[]>([])
const searchForm = reactive({ keyword: '', status: '' })
const pagination = reactive({ page: 1, size: 10, total: 0 })
const formRef = ref()
const form = reactive({
  id: null as number | null,
  productName: '',
  productModel: '',
  planQuantity: 100,
  workstationId: null as number | null,
  processTemplateId: null as number | null,
  priority: 'MEDIUM',
  plannedStartTime: null as string | null,
  plannedEndTime: null as string | null,
  remark: ''
})

const rules = {
  productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  productModel: [{ required: true, message: '请输入产品型号', trigger: 'blur' }],
  planQuantity: [{ required: true, message: '请输入计划数量', trigger: 'blur' }],
  workstationId: [{ required: true, message: '请选择工位', trigger: 'change' }],
  processTemplateId: [{ required: true, message: '请选择工艺模板', trigger: 'change' }]
}

const getStatusType = (status: string) => {
  const map: Record<string, string> = { CREATED: 'info', ISSUED: 'warning', IN_PRODUCTION: 'primary', PENDING_QC: 'warning', COMPLETED: 'success', CLOSED: 'info' }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = { CREATED: '已创建', ISSUED: '已下发', IN_PRODUCTION: '生产中', PENDING_QC: '待质检', COMPLETED: '已完成', CLOSED: '已关闭' }
  return map[status] || status
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getWorkOrderPage({
      current: pagination.page,
      size: pagination.size,
      status: searchForm.status,
      keyword: searchForm.keyword
    })
    tableData.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error) {
    console.error('Failed to load work orders:', error)
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.status = ''
  pagination.page = 1
  loadData()
}

const handleCreate = () => {
  form.id = null
  form.productName = ''
  form.productModel = ''
  form.planQuantity = 100
  form.workstationId = null
  form.processTemplateId = null
  form.priority = 'MEDIUM'
  form.plannedStartTime = null
  form.plannedEndTime = null
  form.remark = ''
  dialogTitle.value = '新建工单'
  dialogVisible.value = true
}

const handleDetail = (row: any) => {
  ElMessage.info(`查看工单详情: ${row.code}`)
}

const handleStart = async (row: any) => {
  try {
    await startWorkOrder(row.id)
    ElMessage.success('工单已开始生产')
    loadData()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleIssue = async (row: any) => {
  try {
    await issueWorkOrder(row.id)
    ElMessage.success('工单已下发')
    loadData()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleDelete = (row: any) => {
  const deleteId = String(row.id)
  console.log('删除工单, id:', deleteId, 'status:', row.status, '原始ID:', row.id)
  ElMessageBox.confirm(
    `确定要删除工单 "${row.orderNo}" 吗？`,
    '删除确认',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      console.log('开始删除, id:', deleteId)
      await deleteWorkOrder(deleteId)
      ElMessage.success('删除成功')
      loadData()
    } catch (error: any) {
      console.error('删除失败:', error)
      ElMessage.error(error?.message || error?.response?.data?.message || '删除失败')
    }
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

const handleSubmit = async () => {
  if (!formRef.value) return
  // 临时跳过验证，直接提交
  try {
const data = {
          productName: form.productName,
          productModel: form.productModel,
          planQuantity: form.planQuantity,
          workstationId: form.workstationId,
          processTemplateId: form.processTemplateId,
          priority: form.priority,
          remark: form.remark
        }
    console.log('提交数据:', JSON.stringify(data))
    await createWorkOrder(data)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } catch (error: any) {
    console.error('保存失败:', error)
    ElMessage.error(error?.message || '保存失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.workorder-container {
  .search-bar {
    display: flex;
    gap: 12px;
    margin-top: 20px;
  }
}
</style>
