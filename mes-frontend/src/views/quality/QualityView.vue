<!-- Quality Management View - Quality inspection and traceability -->
<template>
  <div class="quality-container">
    <page-header title="质量管理">
      <template #actions>
        <el-button type="primary" @click="handleCreate">新建质检</el-button>
      </template>
    </page-header>
    
    <div class="search-bar">
      <el-input v-model="searchForm.sn" placeholder="产品序列号" clearable style="width: 200px;" />
      <el-select v-model="searchForm.result" placeholder="质检结果" clearable style="width: 150px;">
        <el-option label="合格" value="PASSED" />
        <el-option label="不合格" value="FAILED" />
        <el-option label="返工" value="REWORK" />
      </el-select>
      <el-button type="primary" @click="loadData">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>
    
    <el-table :data="tableData" style="width: 100%; margin-top: 20px;" v-loading="loading">
      <el-table-column prop="sn" label="产品序列号" />
      <el-table-column prop="workOrderNo" label="工单号" />
      <el-table-column prop="checkType" label="检验类型" />
      <el-table-column prop="checkResult" label="质检结果">
        <template #default="{ row }">
          <el-tag :type="getResultType(row.checkResult)">{{ getResultText(row.checkResult) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="checkTime" label="检验时间" />
      <el-table-column label="操作" width="250">
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
      style="margin-top: 20px; justify-content: flex-end;"
      @size-change="loadData"
      @current-change="loadData"
    />
    
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
    
    <el-dialog 
      v-model="traceDialogVisible" 
      title="质量追溯" 
      width="800px"
      class="quality-trace-dialog"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="产品序列号">
          <span class="trace-text">{{ traceData.sn }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="产品名称">
          <span class="trace-text">{{ traceData.productName }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="工单编号">
          <span class="trace-text">{{ traceData.workOrderCode }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="生产时间">
          <span class="trace-text">{{ traceData.produceTime }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="质检结果">
          <el-tag :type="getResultType(traceData.qualityResultCode)" size="small">
            {{ traceData.qualityResult }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="质检时间">
          <span class="trace-text">{{ traceData.qualityTime }}</span>
        </el-descriptions-item>
      </el-descriptions>
      
      <el-divider content-position="left">
        <span class="divider-title">生产工序</span>
      </el-divider>
      
      <div class="timeline-container">
        <el-timeline v-if="traceData.steps && traceData.steps.length > 0">
          <el-timeline-item 
            v-for="(step, index) in traceData.steps" 
            :key="index" 
            :timestamp="step.time" 
            placement="top"
            :type="step.type || 'primary'"
            :hollow="true"
          >
            <div class="timeline-content">
              <span class="step-name">{{ step.name }}</span>
              <span class="step-operator">操作员：{{ step.operator || '未知' }}</span>
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
import PageHeader from '@/components/common/PageHeader.vue'
import { getQualityPage, passQuality, failQuality, forwardTrace, deleteQualityRecord, createQualityRecord } from '@/api/services'

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
const traceData = ref<any>({
  sn: '',
  productName: '',
  workOrderCode: '',
  produceTime: '',
  qualityResult: '',
  qualityResultCode: '',
  qualityTime: '',
  steps: []
})

const getResultType = (result: string) => {
  const map: Record<string, string> = { 
    PASSED: 'success', 
    FAILED: 'danger', 
    REWORK: 'warning',
    pending: 'info'
  }
  return map[result] || 'info'
}

const getResultText = (result: string) => {
  const map: Record<string, string> = { 
    PASSED: '合格', 
    FAILED: '不合格', 
    REWORK: '返工',
    pending: '待检'
  }
  return map[result] || '未知'
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getQualityPage({
      current: pagination.page,
      size: pagination.size,
      keyword: searchForm.sn,
      result: searchForm.result
    })
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
  Object.assign(createForm, {
    workOrderId: null,
    sn: '',
    deviceId: null,
    checkType: '',
    checkResult: '',
    defectType: '',
    defectDesc: '',
    remark: ''
  })
  createDialogVisible.value = true
}

const submitCreate = async () => {
  if (!createForm.sn) {
    ElMessage.warning('请输入产品序列号')
    return
  }
  if (!createForm.checkType) {
    ElMessage.warning('请选择检验类型')
    return
  }
  if (!createForm.checkResult) {
    ElMessage.warning('请选择检验结果')
    return
  }
  createLoading.value = true
  try {
    await createQualityRecord({
      workOrderId: createForm.workOrderId,
      sn: createForm.sn,
      deviceId: createForm.deviceId,
      checkType: createForm.checkType,
      checkResult: createForm.checkResult,
      defectType: createForm.defectType,
      defectDesc: createForm.defectDesc,
      remark: createForm.remark
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
      sn: data.sn || row.sn,
      productName: data.productName || row.productName,
      workOrderCode: data.workOrderCode || row.workOrderCode,
      produceTime: data.produceTime || '-',
      qualityResult: getResultText(data.qualityResult || row.result),
      qualityResultCode: data.qualityResult || row.result,
      qualityTime: data.qualityTime || row.inspectTime,
      steps: data.steps || []
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

const handleFail = async (row: any) => {
  try {
    await failQuality(row.id, '质量问题')
    ElMessage.warning(`产品 ${row.sn} 标记为不合格`)
    loadData()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleDelete = async (row: any) => {
  const deleteId = String(row.id)
  try {
    await ElMessageBox.confirm(
      `确定要删除质检记录 "${row.sn}" 吗？`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await deleteQualityRecord(deleteId)
    ElMessage.success('删除成功')
    loadData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '删除失败')
    }
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.quality-container {
  .search-bar {
    display: flex;
    gap: 12px;
    margin-top: 20px;
    
  }
  
  :deep(.el-table) {
    background: transparent;
    
    th, td {
      background: transparent;
      color: #ffffff;
      border-color: rgba(255, 255, 255, 0.1);
    }
    
    th {
      background: rgba(255, 255, 255, 0.05);
    }
    
    .el-table__row {
      &:hover {
        background: rgba(255, 255, 255, 0.05);
      }
    }
  }
  
  :deep(.el-pagination) {
    .el-pagination__total, 
    .el-pagination__jump,
    .el-pagination__editor {
      color: #ffffff;
      background-color: transparent;
    }
    
    button:not(:disabled) {
      color: #ffffff;
      
      &:hover {
        color: #e94560;
      }
    }
    
    .el-pager li {
      background: transparent;
      color: #ffffff;
      
      &:hover {
        color: #e94560;
      }
      
      &.is-active {
        color: #e94560;
        background: rgba(233, 69, 96, 0.1);
      }
    }
  }
}

// 修复质量追溯对话框的样式
:deep(.quality-trace-dialog) {
  .el-dialog {
    background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
    border-radius: 12px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
    
    .el-dialog__header {
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);
      
      .el-dialog__title {
        color: #ffffff;
        font-weight: 600;
        font-size: 18px;
      }
    }
    
    .el-dialog__body {
      color: #ffffff;
    }
    
    .el-descriptions {
      .el-descriptions__label {
        background: rgba(255, 255, 255, 0.08);
        color: #a0a0a0;
        font-weight: 500;
        border-color: rgba(255, 255, 255, 0.1);
      }
      
      .el-descriptions__content {
        background: rgba(255, 255, 255, 0.05);
        color: #ffffff;
        border-color: rgba(255, 255, 255, 0.1);
      }
    }
    
    .trace-text {
      color: #ffffff;
    }
    
    .el-divider {
      .el-divider__text {
        background: transparent;
        color: #e94560;
        font-weight: 500;
      }
    }
    
    .divider-title {
      color: #e94560;
    }
    
    .timeline-container {
      max-height: 400px;
      overflow-y: auto;
      padding: 10px;
      
      &::-webkit-scrollbar {
        width: 6px;
      }
      
      &::-webkit-scrollbar-track {
        background: rgba(255, 255, 255, 0.1);
        border-radius: 3px;
      }
      
      &::-webkit-scrollbar-thumb {
        background: rgba(233, 69, 96, 0.5);
        border-radius: 3px;
        
        &:hover {
          background: rgba(233, 69, 96, 0.8);
        }
      }
    }
    
    .el-timeline {
      .el-timeline-item__timestamp {
        color: #a0a0a0;
      }
      
      .el-timeline-item__content {
        color: #ffffff;
      }
      
      .timeline-content {
        display: flex;
        flex-direction: column;
        gap: 4px;
        
        .step-name {
          font-weight: 500;
          color: #e94560;
        }
        
        .step-operator {
          font-size: 12px;
          color: #a0a0a0;
        }
      }
    }
    
    .el-empty {
      .el-empty__description {
        p {
          color: #a0a0a0;
        }
      }
    }
    
    .el-dialog__footer {
      border-top: 1px solid rgba(255, 255, 255, 0.1);
      
      .el-button {
        background: rgba(255, 255, 255, 0.1);
        border-color: rgba(255, 255, 255, 0.2);
        color: #ffffff;
        
        &:hover {
          background: rgba(255, 255, 255, 0.2);
          border-color: rgba(255, 255, 255, 0.3);
        }
      }
    }
  }
}
</style>