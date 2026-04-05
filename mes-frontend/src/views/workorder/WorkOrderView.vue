<!-- Work Order Management View -->
<template>
  <div class="workorder-container">
    <page-header title="工单管理">
      <template #actions>
        <el-button type="primary" @click="handleCreate">新建工单</el-button>
      </template>
    </page-header>
    
    <div class="search-bar">
      <el-input v-model="searchForm.code" placeholder="工单编号" clearable style="width: 200px;" />
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
      <el-table-column prop="code" label="工单编号" />
      <el-table-column prop="productName" label="产品名称" />
      <el-table-column prop="quantity" label="数量" />
      <el-table-column prop="status" label="状态">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="progress" label="进度" />
      <el-table-column prop="planStartDate" label="计划开始" />
      <el-table-column prop="planEndDate" label="计划结束" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleDetail(row)">详情</el-button>
          <el-button type="success" link @click="handleStart(row)" v-if="row.status === 'CREATED' || row.status === 'ISSUED'">开始</el-button>
          <el-button type="warning" link @click="handleIssue(row)" v-if="row.status === 'CREATED'">下发</el-button>
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
        <el-form-item label="工单编号" prop="code">
          <el-input v-model="form.code" />
        </el-form-item>
        <el-form-item label="产品名称" prop="productName">
          <el-input v-model="form.productName" />
        </el-form-item>
        <el-form-item label="数量" prop="quantity">
          <el-input-number v-model="form.quantity" :min="1" />
        </el-form-item>
        <el-form-item label="工艺模板" prop="templateId">
          <el-select v-model="form.templateId" placeholder="请选择">
            <el-option label="CNC加工工艺" :value="1" />
            <el-option label="组装工艺" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划开始" prop="planStartDate">
          <el-date-picker v-model="form.planStartDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="计划结束" prop="planEndDate">
          <el-date-picker v-model="form.planEndDate" type="date" value-format="YYYY-MM-DD" />
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
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/common/PageHeader.vue'
import { getWorkOrderPage, createWorkOrder, issueWorkOrder, startWorkOrder } from '@/api/services'

const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新建工单')
const tableData = ref<any[]>([])
const searchForm = reactive({ code: '', status: '' })
const pagination = reactive({ page: 1, size: 10, total: 0 })
const formRef = ref()
const form = reactive({
  id: null as number | null,
  code: '',
  productName: '',
  quantity: 100,
  templateId: null as number | null,
  planStartDate: '',
  planEndDate: ''
})

const rules = {
  code: [{ required: true, message: '请输入工单编号', trigger: 'blur' }],
  productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }]
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
      keyword: searchForm.code
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
  searchForm.code = ''
  searchForm.status = ''
  pagination.page = 1
  loadData()
}

const handleCreate = () => {
  form.id = null
  form.code = ''
  form.productName = ''
  form.quantity = 100
  form.templateId = null
  form.planStartDate = ''
  form.planEndDate = ''
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

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await createWorkOrder(form)
        ElMessage.success('保存成功')
        dialogVisible.value = false
        loadData()
      } catch (error) {
        ElMessage.error('保存失败')
      }
    }
  })
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
