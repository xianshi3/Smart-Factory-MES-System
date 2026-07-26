<template>
  <div class="page-wrapper">
    <div class="page-header">
      <div class="header-left">
        <div class="page-title">
          <el-icon><Document /></el-icon>
          <h1>工单管理</h1>
        </div>
        <p class="page-desc">生产工单 · 进度跟踪 · 报工管理</p>
      </div>
      <el-button type="primary" class="create-btn" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        新建工单
      </el-button>
    </div>

    <div class="filter-bar">
      <el-input v-model="searchForm.keyword" placeholder="工单编号..." clearable class="search-input">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="searchForm.status" placeholder="状态" clearable class="status-select">
        <el-option label="已创建" value="CREATED" />
        <el-option label="已下发" value="ISSUED" />
        <el-option label="生产中" value="IN_PRODUCTION" />
        <el-option label="已完成" value="COMPLETED" />
        <el-option label="已关闭" value="CLOSED" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div class="card-grid">
      <div 
        v-for="(row, index) in tableData" 
        :key="row.id" 
        class="order-card"
        :style="{ animationDelay: `${index * 0.05}s` }"
      >
        <div class="card-header">
          <span class="order-no">{{ row.orderNo }}</span>
          <span :class="['status-dot', `status-dot--${statusConfig[row.status]?.tag}`]"></span>
        </div>
        
        <div class="card-body">
          <div class="card-product">{{ row.productName }}</div>
          <div class="card-meta">
            <span class="meta-item">
              <el-icon><Coin /></el-icon>
              <span>计划: {{ row.planQuantity }}</span>
            </span>
            <span class="meta-item">
              <el-icon><CircleCheck /></el-icon>
              <span>完成: {{ row.completedQuantity || 0 }}</span>
            </span>
          </div>
          
          <div class="card-progress">
            <div class="progress-info">
              <span class="progress-label">进度</span>
              <span class="progress-value">{{ getProgress(row) }}%</span>
            </div>
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: getProgress(row) + '%' }"></div>
            </div>
          </div>
        </div>
        
        <div class="card-footer">
          <span :class="['status-tag', `status-tag--${statusConfig[row.status]?.tag}`]">
            {{ statusConfig[row.status]?.text }}
          </span>
          <div class="card-actions" @click.stop>
            <el-button type="danger" size="small" link @click="handleDelete(row)" v-if="canDelete(row)">删除</el-button>
            <el-button type="primary" size="small" link @click="handleStart(row)" v-if="canStart(row)">开始</el-button>
            <el-button type="warning" size="small" link @click="handleIssue(row)" v-if="row.status === 'CREATED'">下发</el-button>
            <el-button type="info" size="small" link @click="handleReport(row)" v-if="canReport(row)">报工</el-button>
            <el-button type="success" size="small" link @click="handleComplete(row)" v-if="canComplete(row)">完成</el-button>
          </div>
        </div>
      </div>
      
      <div v-if="tableData.length === 0 && !loading" class="empty-state">
        <el-icon size="48"><Document /></el-icon>
        <p>暂无工单数据</p>
      </div>
    </div>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[12, 24, 48]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="产品名称" prop="productName">
          <el-input v-model="form.productName" placeholder="请输入产品名称" />
        </el-form-item>
        <el-form-item label="产品型号" prop="productModel">
          <el-input v-model="form.productModel" placeholder="请输入产品型号" />
        </el-form-item>
        <el-form-item label="计划数量" prop="planQuantity">
          <el-input-number v-model="form.planQuantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="工位" prop="workstationId">
          <el-select v-model="form.workstationId" placeholder="请选择" style="width: 100%">
            <el-option label="工位1" :value="1" />
            <el-option label="工位2" :value="2" />
            <el-option label="工位3" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="工艺模板" prop="processTemplateId">
          <el-select v-model="form.processTemplateId" placeholder="请选择" style="width: 100%">
            <el-option label="CNC加工工艺" :value="1" />
            <el-option label="组装工艺" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="form.priority" style="width: 100%">
            <el-option label="低" value="LOW" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="高" value="HIGH" />
          </el-select>
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

    <el-dialog v-model="detailVisible" title="工单详情" width="500px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="工单编号">{{ detailData.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <span :class="['status-tag', `status-tag--${statusConfig[detailData.status]?.tag}`]">
            {{ statusConfig[detailData.status]?.text }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="产品名称">{{ detailData.productName }}</el-descriptions-item>
        <el-descriptions-item label="产品型号">{{ detailData.productModel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="计划数量">{{ detailData.planQuantity }}</el-descriptions-item>
        <el-descriptions-item label="已完成">{{ detailData.completedQuantity || 0 }}</el-descriptions-item>
        <el-descriptions-item label="计划开始">{{ detailData.plannedStartTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="计划结束">{{ detailData.plannedEndTime || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reportVisible" title="提交报工" width="400px">
      <el-form :model="reportForm" label-width="80px">
        <el-form-item label="报工数量">
          <el-input-number v-model="reportForm.reportQuantity" :min="1" :max="reportForm.remaining" style="width: 100%" />
        </el-form-item>
        <el-form-item label="良品数量">
          <el-input-number v-model="reportForm.qualifiedQuantity" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="不良数量">
          <el-input-number v-model="reportForm.defectiveQuantity" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="reportForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitReport">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getWorkOrderPage, getWorkOrderDetail, createWorkOrder, issueWorkOrder, startWorkOrder, completeWorkOrder, deleteWorkOrder, submitReport } from '@/api/services'
import { Document, Plus, Search, Coin, CircleCheck } from '@element-plus/icons-vue'

const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新建工单')
const detailVisible = ref(false)
const reportVisible = ref(false)
const detailData = ref<any>({})
const tableData = ref<any[]>([])

const searchForm = reactive({ keyword: '', status: '' })
const pagination = reactive({ page: 1, size: 12, total: 0 })
const formRef = ref()

const form = reactive({
  id: null as number | null,
  productName: '',
  productModel: '',
  planQuantity: 100,
  workstationId: null as number | null,
  processTemplateId: null as number | null,
  priority: 'MEDIUM',
  remark: ''
})

const reportForm = reactive({
  workOrderId: null as number | null,
  orderNo: '',
  reportQuantity: 1,
  qualifiedQuantity: 1,
  defectiveQuantity: 0,
  remark: '',
  remaining: 0
})

const statusConfig: Record<string, { tag: string; text: string }> = {
  CREATED: { tag: 'info', text: '已创建' },
  ISSUED: { tag: 'warning', text: '已下发' },
  IN_PRODUCTION: { tag: 'primary', text: '生产中' },
  PENDING_QC: { tag: 'warning', text: '待质检' },
  COMPLETED: { tag: 'success', text: '已完成' },
  CLOSED: { tag: 'info', text: '已关闭' }
}

const rules = {
  productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  productModel: [{ required: true, message: '请输入产品型号', trigger: 'blur' }],
  workstationId: [{ required: true, message: '请选择工位', trigger: 'change' }],
  processTemplateId: [{ required: true, message: '请选择工艺模板', trigger: 'change' }]
}

const getProgress = (row: any) => {
  if (!row.planQuantity) return 0
  return Math.round(((row.completedQuantity || 0) / row.planQuantity) * 100)
}
const canStart = (row: any) => row.status === 'ISSUED'
const canDelete = (row: any) => row.status === 'CREATED' || row.status === 'CLOSED'
const canReport = (row: any) => row.status === 'IN_PRODUCTION' || row.status === 'PENDING_QC'
const canComplete = (row: any) => row.status === 'IN_PRODUCTION' || row.status === 'PENDING_QC'

const loadData = async () => {
  loading.value = true
  try {
    const res = await getWorkOrderPage({ current: pagination.page, size: pagination.size, status: searchForm.status, keyword: searchForm.keyword })
    tableData.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error) { console.error('Failed to load:', error) }
  finally { loading.value = false }
}

const handleReset = () => { searchForm.keyword = ''; searchForm.status = ''; pagination.page = 1; loadData() }

const handleCreate = () => {
  form.id = null
  Object.assign(form, { productName: '', productModel: '', planQuantity: 100, workstationId: null, processTemplateId: null, priority: 'MEDIUM', remark: '' })
  dialogTitle.value = '新建工单'
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    await createWorkOrder(form)
    ElMessage.success('创建成功')
    dialogVisible.value = false
    loadData()
  } catch (error: any) { ElMessage.error(error?.message || '创建失败') }
}

const handleDetail = async (row: any) => {
  try {
    const res = await getWorkOrderDetail(row.id)
    detailData.value = res.data || {}
    detailVisible.value = true
  } catch { ElMessage.error('获取详情失败') }
}

const handleStart = async (row: any) => {
  try { 
    await startWorkOrder(row.id); 
    ElMessage.success('工单已开始'); 
    loadData() 
  } catch (e: any) { 
    ElMessage.error(e?.message || '操作失败，请先下发工单') 
  }
}

const handleIssue = async (row: any) => {
  try { 
    await issueWorkOrder(row.id); 
    ElMessage.success('工单已下发'); 
    loadData() 
  } catch (e: any) { 
    ElMessage.error(e?.message || '下发失败') 
  }
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定删除工单 "${row.orderNo}" 吗？`, '确认删除', { type: 'warning' })
    await deleteWorkOrder(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e?.message || '删除失败') }
}

const handleComplete = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定完成工单 "${row.orderNo}" 吗？`, '确认', { type: 'warning' })
    await completeWorkOrder(row.id)
    ElMessage.success('工单已完成')
    loadData()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e?.message || '操作失败') }
}

const handleReport = (row: any) => {
  Object.assign(reportForm, { workOrderId: row.id, orderNo: row.orderNo, reportQuantity: 1, qualifiedQuantity: 1, defectiveQuantity: 0, remark: '', remaining: (row.planQuantity || 0) - (row.completedQuantity || 0) })
  reportVisible.value = true
}

const handleSubmitReport = async () => {
  try {
    await submitReport({ workOrderId: reportForm.workOrderId, reportQuantity: reportForm.reportQuantity, qualifiedQuantity: reportForm.qualifiedQuantity, defectiveQuantity: reportForm.defectiveQuantity, remark: reportForm.remark })
    ElMessage.success('报工成功')
    reportVisible.value = false
    loadData()
  } catch (e: any) { ElMessage.error(e?.message || '报工失败') }
}

onMounted(() => { loadData() })
</script>

<style scoped>
.page-wrapper {
  background: var(--bg-app);
  min-height: 100%;
}

.create-btn { height: 36px; padding: 0 16px; border-radius: var(--radius-md); }

.search-input { width: 200px; }
.status-select { width: 120px; }

.order-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 18px;
  cursor: pointer;
  transition: all var(--transition-normal);
  animation: fadeInUp 0.4s ease forwards;
  opacity: 0;
}

.order-card:hover {
  border-color: var(--accent);
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-light);
}

.order-no {
  font-size: 14px;
  font-weight: 600;
  color: var(--accent);
  font-family: monospace;
}

.status-dot { width: 8px; height: 8px; border-radius: 50%; }
.status-dot--info { background: var(--info); }
.status-dot--warning { background: var(--warning); }
.status-dot--primary { background: var(--accent); }
.status-dot--success { background: var(--success); }

.card-body { margin-bottom: 14px; }

.card-product {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 10px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-meta {
  display: flex;
  gap: 16px;
  margin-bottom: 14px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-muted);
}

.meta-item .el-icon { font-size: 14px; }

.card-progress {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-label { font-size: 12px; color: var(--text-muted); }
.progress-value { font-size: 13px; font-weight: 600; color: var(--text-secondary); }

.progress-bar {
  height: 6px;
  background: var(--border-color);
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--gradient-primary);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
}
</style>