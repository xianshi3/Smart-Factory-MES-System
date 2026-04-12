<!-- Quality Management View -->
<template>
  <div class="quality-container">
    <div class="page-header">
      <div class="header-title">
        <el-icon size="24"><CircleCheck /></el-icon>
        <h1>质量管理</h1>
      </div>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        <span>新建质检</span>
      </el-button>
    </div>
    
    <div class="search-bar">
      <el-input v-model="searchForm.sn" placeholder="产品序列号" clearable>
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="searchForm.result" placeholder="质检结果" clearable>
        <el-option label="合格" value="PASSED" />
        <el-option label="不合格" value="FAILED" />
        <el-option label="返工" value="REWORK" />
      </el-select>
      <el-button type="primary" @click="loadData">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>
    
    <div class="table-card">
      <el-table :data="tableData" v-loading="loading">
        <el-table-column prop="sn" label="产品序列号" min-width="150" />
        <el-table-column prop="workOrderNo" label="工单号" min-width="130" />
        <el-table-column prop="checkType" label="检验类型" width="100" />
        <el-table-column prop="checkResult" label="质检结果" width="100">
          <template #default="{ row }">
            <el-tag :type="getResultType(row.checkResult)">{{ getResultText(row.checkResult) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="checkTime" label="检验时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleDetail(row)">详情</el-button>
            <el-button type="success" link @click="handlePass(row)" v-if="row.checkResult === 'PENDING'">合格</el-button>
            <el-button type="danger" link @click="handleDelete(row)" v-if="row.checkResult === 'PASSED' || row.checkResult === 'FAILED'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>
    
    <el-dialog v-model="createDialogVisible" title="新建质检记录" width="500px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="工单ID">
          <el-input v-model="createForm.workOrderId" placeholder="请输入工单ID" />
        </el-form-item>
        <el-form-item label="产品序列号">
          <el-input v-model="createForm.sn" placeholder="请输入产品序列号SN" />
        </el-form-item>
        <el-form-item label="设备ID">
          <el-input v-model="createForm.deviceId" placeholder="请输入设备ID" />
        </el-form-item>
        <el-form-item label="检验类型">
          <el-select v-model="createForm.checkType" placeholder="请选择检验类型">
            <el-option label="IPQC" value="IPQC" />
            <el-option label="FQC" value="FQC" />
            <el-option label="OQC" value="OQC" />
          </el-select>
        </el-form-item>
        <el-form-item label="检验结果">
          <el-select v-model="createForm.checkResult" placeholder="请选择检验结果">
            <el-option label="合格" value="PASSED" />
            <el-option label="不合格" value="FAILED" />
            <el-option label="返工" value="REWORK" />
          </el-select>
        </el-form-item>
        <el-form-item label="缺陷类型">
          <el-input v-model="createForm.defectType" placeholder="请输入缺陷类型" />
        </el-form-item>
        <el-form-item label="缺陷描述">
          <el-input v-model="createForm.defectDesc" type="textarea" :rows="2" placeholder="请输入缺陷描述" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate" :loading="createLoading">创建</el-button>
      </template>
    </el-dialog>
    
    <el-dialog v-model="traceDialogVisible" title="质量追溯" width="800px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="产品序列号">{{ traceData.sn }}</el-descriptions-item>
        <el-descriptions-item label="产品名称">{{ traceData.productName }}</el-descriptions-item>
        <el-descriptions-item label="工单编号">{{ traceData.workOrderCode }}</el-descriptions-item>
        <el-descriptions-item label="生产时间">{{ traceData.produceTime }}</el-descriptions-item>
        <el-descriptions-item label="质检结果">
          <el-tag :type="getResultType(traceData.qualityResultCode)" size="small">{{ traceData.qualityResult }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="质检时间">{{ traceData.qualityTime }}</el-descriptions-item>
      </el-descriptions>
      
      <el-divider>生产工序</el-divider>
      
      <div class="timeline-container">
        <el-timeline v-if="traceData.steps && traceData.steps.length > 0">
          <el-timeline-item v-for="(step, index) in traceData.steps" :key="index" :timestamp="step.time" placement="top">
            <div class="timeline-content">
              <span>{{ step.name }}</span>
              <span class="operator">操作员：{{ step.operator || '未知' }}</span>
            </div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无生产工序信息" :image-size="80" />
      </div>
      
      <template #footer>
        <el-button @click="traceDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getQualityPage, passQuality, forwardTrace, deleteQualityRecord, createQualityRecord } from '@/api/services'

const loading = ref(false)
const createLoading = ref(false)
const createDialogVisible = ref(false)
const createForm = reactive({
  workOrderId: null as number | null,
  sn: '',
  deviceId: null as number | null,
  checkType: '',
  checkResult: '',
  defectType: '',
  defectDesc: '',
  remark: ''
})
const traceDialogVisible = ref(false)
const tableData = ref<any[]>([])
const searchForm = reactive({ sn: '', result: '' })
const pagination = reactive({ page: 1, size: 10, total: 0 })
const traceData = ref<any>({ sn: '', productName: '', workOrderCode: '', produceTime: '', qualityResult: '', qualityResultCode: '', qualityTime: '', steps: [] })

const getResultType = (result: string) => {
  const map: Record<string, string> = { PASSED: 'success', FAILED: 'danger', REWORK: 'warning', pending: 'info' }
  return map[result] || 'info'
}

const getResultText = (result: string) => {
  const map: Record<string, string> = { PASSED: '合格', FAILED: '不合格', REWORK: '返工', pending: '待检' }
  return map[result] || '未知'
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getQualityPage({ current: pagination.page, size: pagination.size, keyword: searchForm.sn, result: searchForm.result })
    tableData.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error) {
    console.error('Failed to load quality records:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  searchForm.sn = ''
  searchForm.result = ''
  pagination.page = 1
  loadData()
}

const handleCreate = () => {
  Object.assign(createForm, { workOrderId: null, sn: '', deviceId: null, checkType: '', checkResult: '', defectType: '', defectDesc: '', remark: '' })
  createDialogVisible.value = true
}

const submitCreate = async () => {
  if (!createForm.sn) { ElMessage.warning('请输入产品序列号'); return }
  if (!createForm.checkType) { ElMessage.warning('请选择检验类型'); return }
  if (!createForm.checkResult) { ElMessage.warning('请选择检验结果'); return }
  createLoading.value = true
  try {
    await createQualityRecord({
      workOrderId: createForm.workOrderId, sn: createForm.sn, deviceId: createForm.deviceId,
      checkType: createForm.checkType, checkResult: createForm.checkResult,
      defectType: createForm.defectType, defectDesc: createForm.defectDesc, remark: createForm.remark
    })
    ElMessage.success('创建成功')
    createDialogVisible.value = false
    loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '创建失败')
  } finally {
    createLoading.value = false
  }
}

const handleDetail = async (row: any) => {
  try {
    const res = await forwardTrace(row.sn)
    const data = res.data
    traceData.value = {
      sn: data.sn || row.sn, productName: data.productName || row.productName,
      workOrderCode: data.workOrderCode || row.workOrderCode, produceTime: data.produceTime || '-',
      qualityResult: getResultText(data.qualityResult || row.result), qualityResultCode: data.qualityResult || row.result,
      qualityTime: data.qualityTime || row.inspectTime, steps: data.steps || []
    }
    traceDialogVisible.value = true
  } catch (error) {
    console.error('Failed to load trace:', error)
    ElMessage.error('加载追溯信息失败')
  }
}

const handlePass = async (row: any) => {
  try {
    await passQuality(row.id)
    ElMessage.success(`产品 ${row.sn} 标记为合格`)
    loadData()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定要删除质检记录 "${row.sn}" 吗？`, '删除确认', { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' })
    await deleteQualityRecord(String(row.id))
    ElMessage.success('删除成功')
    loadData()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error?.message || '删除失败')
  }
}

onMounted(() => { loadData() })
</script>

<style scoped>
.quality-container {
  padding: 24px;
  background: var(--bg-app);
  min-height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  animation: fadeIn 0.5s ease;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--text-primary);
}

.header-title h1 {
  font-size: 24px;
  font-weight: 600;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  animation: fadeIn 0.5s ease 0.1s both;
}

.search-bar .el-input {
  width: 220px;
}

.search-bar .el-select {
  width: 150px;
}

.table-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 20px;
  animation: fadeIn 0.5s ease 0.2s both;
}

.table-card .el-table {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: var(--hover-bg);
  --el-table-border-color: var(--border-color);
  --el-table-text-color: var(--text-primary);
  --el-table-header-text-color: var(--text-secondary);
}

.table-card .el-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.timeline-container {
  max-height: 300px;
  overflow-y: auto;
}

.timeline-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.timeline-content .operator {
  font-size: 12px;
  color: var(--text-muted);
}

html.light .header-title h1 { color: #1a1a2e; }
html.light .table-card { box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05); }

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}</style>