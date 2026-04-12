<template>
  <div class="page-wrapper">
    <div class="page-header">
      <div class="header-left">
        <div class="page-title">
          <el-icon><CircleCheck /></el-icon>
          <h1>质量管理</h1>
        </div>
        <p class="page-desc">质量检验 · 不良品追踪 · 全程追溯</p>
      </div>
      <el-button type="primary" class="create-btn" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        新建质检
      </el-button>
    </div>

    <div class="filter-bar">
      <el-input v-model="searchForm.sn" placeholder="产品序列号..." clearable class="search-input">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="searchForm.result" placeholder="结果" clearable class="status-select">
        <el-option label="合格" value="PASSED" />
        <el-option label="不合格" value="FAILED" />
        <el-option label="返工" value="REWORK" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div class="card-grid">
      <div 
        v-for="(row, index) in tableData" 
        :key="row.id" 
        class="quality-card"
        :style="{ animationDelay: `${index * 0.05}s` }"
      >
        <div class="card-header">
          <span class="sn-text">{{ row.sn }}</span>
          <span :class="['status-dot', `status-dot--${resultConfig[row.checkResult]?.tag}`]"></span>
        </div>
        
        <div class="card-body">
          <div class="quality-info">
            <el-icon class="info-icon"><Document /></el-icon>
            <div class="info-content">
              <span class="info-label">工单</span>
              <span class="info-value">{{ row.workOrderNo }}</span>
            </div>
          </div>
          <div class="quality-info">
            <el-icon class="info-icon"><Grid /></el-icon>
            <div class="info-content">
              <span class="info-label">类型</span>
              <span class="info-value">{{ row.checkType }}</span>
            </div>
          </div>
          <div class="quality-info">
            <el-icon class="info-icon"><Clock /></el-icon>
            <div class="info-content">
              <span class="info-label">时间</span>
              <span class="info-value">{{ row.checkTime?.substring(0, 16) || '-' }}</span>
            </div>
          </div>
        </div>
        
        <div class="card-footer">
          <span :class="['status-tag', `status-tag--${resultConfig[row.checkResult]?.tag}`]">
            {{ resultConfig[row.checkResult]?.text || row.checkResult }}
          </span>
          <div class="card-actions" @click.stop>
            <el-button type="primary" size="small" link @click="handleDetail(row)">详情</el-button>
            <el-button type="success" size="small" link @click="handlePass(row)" v-if="row.checkResult === 'PENDING'">合格</el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)" v-if="canDelete(row)">删除</el-button>
          </div>
        </div>
      </div>
      
      <div v-if="tableData.length === 0 && !loading" class="empty-state">
        <el-icon size="48"><CircleCheck /></el-icon>
        <p>暂无质检数据</p>
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

    <el-dialog v-model="createVisible" title="新建质检" width="420px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="产品SN">
          <el-input v-model="createForm.sn" placeholder="请输入产品序列号" />
        </el-form-item>
        <el-form-item label="检验类型">
          <el-select v-model="createForm.checkType" placeholder="请选择" style="width: 100%">
            <el-option label="IPQC" value="IPQC" />
            <el-option label="FQC" value="FQC" />
            <el-option label="OQC" value="OQC" />
          </el-select>
        </el-form-item>
        <el-form-item label="检验结果">
          <el-select v-model="createForm.checkResult" placeholder="请选择" style="width: 100%">
            <el-option label="合格" value="PASSED" />
            <el-option label="不合格" value="FAILED" />
            <el-option label="返工" value="REWORK" />
          </el-select>
        </el-form-item>
        <el-form-item label="缺陷描述">
          <el-input v-model="createForm.defectDesc" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate" :loading="createLoading">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="traceVisible" title="质量追溯" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="产品SN">{{ traceData.sn }}</el-descriptions-item>
        <el-descriptions-item label="工单编号">{{ traceData.workOrderCode }}</el-descriptions-item>
        <el-descriptions-item label="质检结果">
          <span :class="['status-tag', `status-tag--${resultConfig[traceData.qualityResultCode]?.tag}`]">
            {{ traceData.qualityResult }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="质检时间">{{ traceData.qualityTime }}</el-descriptions-item>
      </el-descriptions>
      <el-divider>生产工序</el-divider>
      <div class="timeline-wrap">
        <el-timeline v-if="traceData.steps?.length">
          <el-timeline-item v-for="(step, i) in traceData.steps" :key="i" :timestamp="step.time" placement="top">
            <div class="step-name">{{ step.name }}</div>
            <div class="step-operator">操作员：{{ step.operator || '未知' }}</div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无工序信息" :image-size="60" />
      </div>
      <template #footer>
        <el-button @click="traceVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getQualityPage, passQuality, deleteQualityRecord, createQualityRecord, forwardTrace } from '@/api/services'
import { CircleCheck, Plus, Search, Document, Grid, Clock } from '@element-plus/icons-vue'

const loading = ref(false)
const createLoading = ref(false)
const createVisible = ref(false)
const traceVisible = ref(false)
const tableData = ref<any[]>([])

const searchForm = reactive({ sn: '', result: '' })
const pagination = reactive({ page: 1, size: 12, total: 0 })

const createForm = reactive({ sn: '', checkType: '', checkResult: '', defectDesc: '' })

const traceData = ref<any>({ sn: '', workOrderCode: '', qualityResult: '', qualityResultCode: '', qualityTime: '', steps: [] })

const resultConfig: Record<string, { tag: string; text: string }> = {
  PASSED: { tag: 'success', text: '合格' },
  FAILED: { tag: 'danger', text: '不合格' },
  REWORK: { tag: 'warning', text: '返工' },
  PENDING: { tag: 'info', text: '待检' }
}

const canDelete = (row: any) => row.checkResult === 'PASSED' || row.checkResult === 'FAILED'

const loadData = async () => {
  loading.value = true
  try {
    const res = await getQualityPage({ current: pagination.page, size: pagination.size, keyword: searchForm.sn, result: searchForm.result })
    tableData.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error) { console.error('Failed to load:', error) }
  finally { loading.value = false }
}

const handleReset = () => { searchForm.sn = ''; searchForm.result = ''; pagination.page = 1; loadData() }

const handleCreate = () => {
  Object.assign(createForm, { sn: '', checkType: '', checkResult: '', defectDesc: '' })
  createVisible.value = true
}

const submitCreate = async () => {
  if (!createForm.sn) { ElMessage.warning('请输入产品序列号'); return }
  createLoading.value = true
  try {
    await createQualityRecord(createForm)
    ElMessage.success('创建成功')
    createVisible.value = false
    loadData()
  } catch (e: any) { ElMessage.error(e?.message || '创建失败') }
  finally { createLoading.value = false }
}

const handleDetail = async (row: any) => {
  try {
    const res = await forwardTrace(row.sn)
    const d = res.data || {}
    traceData.value = {
      sn: d.sn || row.sn,
      workOrderCode: d.workOrderCode || row.workOrderNo || '-',
      qualityResult: resultConfig[d.qualityResult]?.text || resultConfig[row.checkResult]?.text || '-',
      qualityResultCode: d.qualityResult || row.checkResult,
      qualityTime: d.qualityTime || row.checkTime || '-',
      steps: d.steps || []
    }
    traceVisible.value = true
  } catch { ElMessage.error('加载追溯信息失败') }
}

const handlePass = async (row: any) => {
  try { await passQuality(row.id); ElMessage.success('已标记为合格'); loadData() }
  catch { ElMessage.error('操作失败') }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除质检记录 "${row.sn}" 吗？`, '删除确认', { type: 'warning' })
    .then(async () => {
      try { await deleteQualityRecord(String(row.id)); ElMessage.success('删除成功'); loadData() }
      catch (e: any) { if (e !== 'cancel') ElMessage.error(e?.message || '删除失败') }
    }).catch(() => {})
}

onMounted(() => { loadData() })
</script>

<style scoped>
.page-wrapper { padding: 24px; background: var(--bg-app); min-height: 100% }

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-left { display: flex; align-items: center; gap: 16px; }
.page-title { display: flex; align-items: center; gap: 10px; }
.page-title h1 { font-size: 22px; font-weight: 600; color: var(--text-primary); }
.page-title .el-icon { font-size: 24px; color: var(--accent); }
.page-desc { font-size: 13px; color: var(--text-muted); }

.create-btn { height: 36px; padding: 0 16px; border-radius: var(--radius-md); }

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  padding: 14px 18px;
  background: var(--bg-card);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
}

.search-input { width: 200px; }
.status-select { width: 120px; }

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.quality-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 18px;
  cursor: pointer;
  transition: all var(--transition-normal);
  animation: fadeInUp 0.4s ease forwards;
  opacity: 0;
}

.quality-card:hover {
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

.sn-text {
  font-size: 14px;
  font-weight: 600;
  font-family: monospace;
  color: var(--accent);
}

.status-dot { width: 8px; height: 8px; border-radius: 50%; }
.status-dot--success { background: var(--success); box-shadow: 0 0 6px var(--success); }
.status-dot--danger { background: var(--danger); box-shadow: 0 0 6px var(--danger); }
.status-dot--warning { background: var(--warning); }
.status-dot--info { background: var(--info); }

.card-body { margin-bottom: 14px; }

.quality-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.info-icon {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-hover);
  border-radius: var(--radius-sm);
  color: var(--text-muted);
  font-size: 14px;
}

.info-content { display: flex; flex-direction: column; gap: 2px; }
.info-label { font-size: 11px; color: var(--text-muted); }
.info-value { font-size: 13px; color: var(--text-primary); font-weight: 500; }

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
}

.status-tag {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}
.status-tag--success { background: var(--success-light); color: var(--success); }
.status-tag--danger { background: var(--danger-light); color: var(--danger); }
.status-tag--warning { background: var(--warning-light); color: var(--warning); }
.status-tag--info { background: var(--info-light); color: var(--info); }

.card-actions { display: flex; gap: 4px; }

.empty-state {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: var(--text-muted);
}

.empty-state .el-icon { margin-bottom: 12px; opacity: 0.5; }

.pagination-wrapper { margin-top: 20px; display: flex; justify-content: center; }

.timeline-wrap { max-height: 250px; overflow-y: auto }
.step-name { font-weight: 500; color: var(--text-primary); }
.step-operator { font-size: 12px; color: var(--text-muted); }

html.light .page-title h1 { color: var(--text-primary); }
html.light .filter-bar { background: var(--bg-card); box-shadow: var(--shadow-sm); }
html.light .quality-card:hover { box-shadow: var(--shadow-md); }
html.light .sn-text { color: var(--accent); }

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(15px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>