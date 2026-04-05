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
      <el-table-column prop="workOrderCode" label="工单号" />
      <el-table-column prop="productName" label="产品名称" />
      <el-table-column prop="result" label="质检结果">
        <template #default="{ row }">
          <el-tag :type="getResultType(row.result)">{{ getResultText(row.result) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="inspector" label="质检员" />
      <el-table-column prop="inspectTime" label="质检时间" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleDetail(row)">详情</el-button>
          <el-button type="success" link @click="handlePass(row)" v-if="row.result === 'pending'">合格</el-button>
          <el-button type="danger" link @click="handleFail(row)" v-if="row.result === 'pending'">不合格</el-button>
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
    
    <el-dialog v-model="traceDialogVisible" title="质量追溯" width="800px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="产品序列号">{{ traceData.sn }}</el-descriptions-item>
        <el-descriptions-item label="产品名称">{{ traceData.productName }}</el-descriptions-item>
        <el-descriptions-item label="工单编号">{{ traceData.workOrderCode }}</el-descriptions-item>
        <el-descriptions-item label="生产时间">{{ traceData.produceTime }}</el-descriptions-item>
        <el-descriptions-item label="质检结果">{{ traceData.qualityResult }}</el-descriptions-item>
        <el-descriptions-item label="质检时间">{{ traceData.qualityTime }}</el-descriptions-item>
      </el-descriptions>
      <el-divider>生产工序</el-divider>
      <el-timeline>
        <el-timeline-item v-for="(step, index) in traceData.steps" :key="index" :timestamp="step.time" placement="top">
          {{ step.name }} - {{ step.operator }}
        </el-timeline-item>
      </el-timeline>
      <template #footer>
        <el-button @click="traceDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/common/PageHeader.vue'
import { getQualityPage, passQuality, failQuality, forwardTrace } from '@/api/services'

const loading = ref(false)
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
  qualityTime: '',
  steps: []
})

const getResultType = (result: string) => {
  const map: Record<string, string> = { PASSED: 'success', FAILED: 'danger', REWORK: 'warning' }
  return map[result] || 'info'
}

const getResultText = (result: string) => {
  const map: Record<string, string> = { PASSED: '合格', FAILED: '不合格', REWORK: '返工' }
  return map[result] || '未知'
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getQualityPage({
      current: pagination.page,
      size: pagination.size,
      sn: searchForm.sn,
      result: searchForm.result
    })
    tableData.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error) {
    console.error('Failed to load quality records:', error)
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
  ElMessage.info('新建质检记录')
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
      qualityTime: data.qualityTime || row.inspectTime,
      steps: data.steps || []
    }
    traceDialogVisible.value = true
  } catch (error) {
    console.error('Failed to load trace:', error)
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
  }
  
  :deep(.el-pagination) {
    .el-pagination__total, .el-pagination__jump {
      color: #ffffff;
    }
    
    .el-pager li {
      background: transparent;
      color: #ffffff;
      
      &.is-active {
        color: #e94560;
      }
    }
  }
  
  :deep(.el-descriptions) {
    color: #ffffff;
  }
  
  :deep(.el-timeline) {
    color: #ffffff;
  }
}
</style>
