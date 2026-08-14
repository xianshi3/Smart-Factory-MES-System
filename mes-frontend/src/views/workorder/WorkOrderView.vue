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
      <div class="header-right">
        <el-button class="ai-btn" @click="aiVisible = true">
          <el-icon><MagicStick /></el-icon>
          AI 助手
        </el-button>
        <el-button type="primary" class="create-btn" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          新建工单
        </el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-input v-model="searchForm.keyword" placeholder="工单编号..." clearable class="search-input">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="searchForm.status" placeholder="状态" clearable class="status-select">
        <el-option label="已创建" value="CREATED" />
        <el-option label="已下发" value="ISSUED" />
        <el-option label="生产中" value="IN_PRODUCTION" />
        <el-option label="待质检" value="PENDING_QC" />
        <el-option label="已完成" value="COMPLETED" />
        <el-option label="已关闭" value="CLOSED" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div v-if="loading" class="card-grid">
      <el-skeleton v-for="i in 6" :key="i" animated>
        <template #template>
          <el-skeleton-item variant="rect" style="height: 190px; border-radius: var(--radius-lg)" />
        </template>
      </el-skeleton>
    </div>

    <div v-else class="card-grid">
      <div 
        v-for="row in tableData" 
        :key="row.id" 
        class="order-card"
        @click="handleDetail(row)"
      >
        <div class="card-header">
          <span class="order-no">{{ row.orderNo }}</span>
          <span :class="['status-dot', `status-dot--${statusConfig[row.status]?.tag}`]"></span>
        </div>
        
        <div class="card-body">
          <div class="card-product-row">
            <div class="card-product">{{ row.productName }}</div>
            <span :class="['priority-tag', `priority-tag--${(row.priority || 'MEDIUM').toLowerCase()}`]">
              {{ priorityText(row.priority) }}
            </span>
          </div>
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

          <div class="card-time">
            <el-icon><Calendar /></el-icon>
            <span>{{ row.plannedStartTime ? row.plannedStartTime.substring(5, 16) : '--' }} ~ {{ row.plannedEndTime ? row.plannedEndTime.substring(5, 16) : '--' }}</span>
          </div>
        </div>
        
        <div class="card-footer">
          <span :class="['status-tag', `status-tag--${statusConfig[row.status]?.tag}`]">
            {{ statusConfig[row.status]?.text }}
          </span>
          <div class="card-actions" @click.stop>
            <el-button size="small" link @click="handleDetail(row)">详情</el-button>
            <el-dropdown trigger="click" @command="(cmd: string) => handleCommand(cmd, row)">
              <el-button size="small" link>
                <el-icon><MoreFilled /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="row.status === 'CREATED'" command="issue">下发</el-dropdown-item>
                  <el-dropdown-item v-if="canStart(row)" command="start">开始生产</el-dropdown-item>
                  <el-dropdown-item v-if="canReport(row)" command="report">报工</el-dropdown-item>
                  <el-dropdown-item v-if="canComplete(row)" command="complete">完成</el-dropdown-item>
                  <el-dropdown-item v-if="canClose(row)" command="close">关闭</el-dropdown-item>
                  <el-dropdown-item v-if="canEdit(row)" command="edit" divided>编辑</el-dropdown-item>
                  <el-dropdown-item v-if="canDelete(row)" command="delete" :class="'danger-item'">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
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
            <el-option v-for="ws in workstationOptions" :key="ws.id" :label="ws.workstationName || ws.name" :value="ws.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="工艺模板" prop="processTemplateId">
          <el-select v-model="form.processTemplateId" placeholder="请选择" style="width: 100%">
            <el-option v-for="tpl in templateOptions" :key="tpl.id" :label="tpl.templateName" :value="tpl.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="form.priority" style="width: 100%">
            <el-option label="低" value="LOW" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="高" value="HIGH" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划开始" prop="plannedStartTime">
          <el-date-picker v-model="form.plannedStartTime" type="datetime" placeholder="选择开始时间" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="计划结束" prop="plannedEndTime">
          <el-date-picker v-model="form.plannedEndTime" type="datetime" placeholder="选择结束时间" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
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

    <el-dialog v-model="detailVisible" title="工单详情" width="560px">
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
        <el-descriptions-item label="优先级">{{ priorityText(detailData.priority) }}</el-descriptions-item>
        <el-descriptions-item label="工位">
          {{ workstationName(detailData.workstationId) }}
        </el-descriptions-item>
        <el-descriptions-item label="工艺模板">
          {{ templateName(detailData.processTemplateId) }}
        </el-descriptions-item>
        <el-descriptions-item label="计划开始">{{ detailData.plannedStartTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="计划结束">{{ detailData.plannedEndTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="实际开始">{{ detailData.actualStartTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="实际结束">{{ detailData.actualEndTime || '-' }}</el-descriptions-item>
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
          <div class="form-hint">剩余可报 {{ reportForm.remaining }}</div>
        </el-form-item>
        <el-form-item label="良品数量">
          <el-input-number v-model="reportForm.qualifiedQuantity" :min="0" :max="reportForm.reportQuantity" style="width: 100%" />
        </el-form-item>
        <el-form-item label="不良数量">
          <el-input-number v-model="reportForm.defectiveQuantity" :min="0" :max="reportForm.reportQuantity" style="width: 100%" />
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

    <AiAssistant
      v-if="aiVisible"
      :visible="true"
      floating
      :context="aiContext"
      :scenarios="aiScenarios"
      auto-new
      @close="aiVisible = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getWorkOrderPage, getWorkOrderDetail, createWorkOrder, updateWorkOrder, issueWorkOrder, startWorkOrder, completeWorkOrder, closeWorkOrder, deleteWorkOrder, submitReport, getTemplatePage } from '@/api/services'
import { getWorkstationList } from '@/api/dashboard'
import { Document, Plus, Search, Coin, CircleCheck, Calendar, MoreFilled, MagicStick, DataAnalysis, Warning, TrendCharts } from '@element-plus/icons-vue'
import AiAssistant from '@/components/ai/AiAssistant.vue'

const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新建工单')
const detailVisible = ref(false)
const reportVisible = ref(false)
const detailData = ref<any>({})
const tableData = ref<any[]>([])
const workstationOptions = ref<any[]>([])
const templateOptions = ref<any[]>([])

/* ===== 页面级 AI 助手（工单管理） ===== */
const aiVisible = ref(false)
const aiScenarios = [
  { icon: DataAnalysis, text: '分析当前工单的交期风险' },
  { icon: TrendCharts, text: '生产进度总览与瓶颈分析' },
  { icon: Warning, text: '排查异常或延期工单' },
  { icon: MagicStick, text: '评估产线产能负荷' },
]
const aiContext = computed(() => ({
  page: '工单管理',
  filters: { status: searchForm.status || null, keyword: searchForm.keyword || null },
  summary: `当前页面显示 ${tableData.value.length} 个工单（筛选：${searchForm.status || '全部状态'}${searchForm.keyword ? '，关键字 ' + searchForm.keyword : ''}）。工单列表：` +
    tableData.value.map((r: any) => ({
      orderNo: r.orderNo,
      product: r.productName,
      status: r.status,
      priority: r.priority,
      plan: r.planQuantity,
      completed: r.completedQuantity || 0,
      progress: getProgress(r) + '%',
      window: `${r.plannedStartTime || '--'} ~ ${r.plannedEndTime || '--'}`,
    })),
}))

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
  plannedStartTime: '',
  plannedEndTime: '',
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
  planQuantity: [{ required: true, message: '请输入计划数量', trigger: 'blur' }],
  workstationId: [{ required: true, message: '请选择工位', trigger: 'change' }],
  processTemplateId: [{ required: true, message: '请选择工艺模板', trigger: 'change' }]
}

const getProgress = (row: any) => {
  if (!row.planQuantity) return 0
  return Math.round(((row.completedQuantity || 0) / row.planQuantity) * 100)
}
const priorityText = (p: string) => ({ HIGH: '高优先级', MEDIUM: '中优先级', LOW: '低优先级' } as any)[p] || '中优先级'
const canStart = (row: any) => row.status === 'ISSUED'
const canDelete = (row: any) => row.status === 'CREATED' || row.status === 'CLOSED'
const canEdit = (row: any) => row.status === 'CREATED'
const canReport = (row: any) => row.status === 'IN_PRODUCTION' || row.status === 'PENDING_QC'
const canComplete = (row: any) => row.status === 'IN_PRODUCTION' || row.status === 'PENDING_QC'
const canClose = (row: any) => row.status !== 'CLOSED' && row.status !== 'PENDING_QC'

const workstationName = (id: any) => {
  if (!id) return '-'
  const ws = workstationOptions.value.find((w: any) => w.id === id)
  return ws ? (ws.workstationName || ws.name) : `工位#${id}`
}
const templateName = (id: any) => {
  if (!id) return '-'
  const tpl = templateOptions.value.find((t: any) => t.id === id)
  return tpl ? tpl.templateName : `模板#${id}`
}

const loadOptions = async () => {
  try {
    const ws = await getWorkstationList()
    workstationOptions.value = ws?.data || []
  } catch { /* 静默 */ }
  try {
    const tpl = await getTemplatePage({ current: 1, size: 200 })
    templateOptions.value = tpl?.data?.records || []
  } catch { /* 静默 */ }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getWorkOrderPage({ current: pagination.page, size: pagination.size, status: searchForm.status, keyword: searchForm.keyword })
    tableData.value = res?.data?.records || []
    pagination.total = res?.data?.total || 0
  } catch (error) { console.error('Failed to load:', error) }
  finally { loading.value = false }
}

const handleReset = () => { searchForm.keyword = ''; searchForm.status = ''; pagination.page = 1; loadData() }

const handleCreate = () => {
  form.id = null
  Object.assign(form, { productName: '', productModel: '', planQuantity: 100, workstationId: null, processTemplateId: null, priority: 'MEDIUM', plannedStartTime: '', plannedEndTime: '', remark: '' })
  dialogTitle.value = '新建工单'
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  form.id = row.id
  Object.assign(form, {
    productName: row.productName,
    productModel: row.productModel,
    planQuantity: row.planQuantity,
    workstationId: row.workstationId ?? null,
    processTemplateId: row.processTemplateId ?? null,
    priority: row.priority || 'MEDIUM',
    plannedStartTime: row.plannedStartTime || '',
    plannedEndTime: row.plannedEndTime || '',
    remark: row.remark || ''
  })
  dialogTitle.value = '编辑工单'
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    if (form.id) {
      await updateWorkOrder(form.id, {
        productName: form.productName,
        productModel: form.productModel,
        planQuantity: form.planQuantity,
        workstationId: form.workstationId,
        processTemplateId: form.processTemplateId,
        priority: form.priority,
        plannedStartTime: form.plannedStartTime,
        plannedEndTime: form.plannedEndTime,
        remark: form.remark
      })
      ElMessage.success('更新成功')
    } else {
      await createWorkOrder(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (error: any) { ElMessage.error(error?.message || '保存失败') }
}

const handleDetail = async (row: any) => {
  try {
    const res = await getWorkOrderDetail(row.id)
    detailData.value = res?.data || row
  } catch {
    detailData.value = row
  }
  detailVisible.value = true
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

const handleClose = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定关闭工单 "${row.orderNo}" 吗？关闭后不可再操作`, '确认关闭', { type: 'warning' })
    await closeWorkOrder(row.id)
    ElMessage.success('工单已关闭')
    loadData()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e?.message || '关闭失败') }
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

const handleCommand = (cmd: string, row: any) => {
  if (cmd === 'issue') handleIssue(row)
  else if (cmd === 'start') handleStart(row)
  else if (cmd === 'report') handleReport(row)
  else if (cmd === 'complete') handleComplete(row)
  else if (cmd === 'close') handleClose(row)
  else if (cmd === 'edit') handleEdit(row)
  else if (cmd === 'delete') handleDelete(row)
}

const handleSubmitReport = async () => {
  if (!reportForm.reportQuantity || reportForm.reportQuantity <= 0) {
    ElMessage.warning('请输入报工数量'); return
  }
  if (reportForm.reportQuantity > reportForm.remaining) {
    ElMessage.warning(`报工数量不能超过剩余量 ${reportForm.remaining}`); return
  }
  if (reportForm.qualifiedQuantity + reportForm.defectiveQuantity !== reportForm.reportQuantity) {
    ElMessage.warning('良品数量 + 不良数量 必须等于报工数量'); return
  }
  try {
    await submitReport({ workOrderId: reportForm.workOrderId, reportQuantity: reportForm.reportQuantity, qualifiedQuantity: reportForm.qualifiedQuantity, defectiveQuantity: reportForm.defectiveQuantity, remark: reportForm.remark })
    ElMessage.success('报工成功')
    reportVisible.value = false
    loadData()
  } catch (e: any) { ElMessage.error(e?.message || '报工失败') }
}

onMounted(() => { loadData(); loadOptions() })
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
.status-dot--warning { background: var(--warning); box-shadow: 0 0 6px var(--warning); }
.status-dot--primary { background: var(--accent); box-shadow: 0 0 6px var(--accent); }
.status-dot--success { background: var(--success); box-shadow: 0 0 6px var(--success); }

.card-body { margin-bottom: 14px; }

.card-product-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.card-product {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.priority-tag {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 10px;
}
.priority-tag--high { background: var(--danger-light); color: var(--danger); }
.priority-tag--medium { background: var(--warning-light); color: var(--warning); }
.priority-tag--low { background: var(--info-light); color: var(--info); }

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

.card-time {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 10px;
}
.card-time .el-icon { font-size: 13px; }

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
}

.card-actions { display: flex; align-items: center; gap: 2px; }
.card-actions :deep(.danger-item) { color: var(--danger); }

.form-hint {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.6;
  margin-top: 4px;
}
</style>