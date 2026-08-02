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
      <div class="header-actions">
        <el-button type="primary" class="create-btn" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          新增工位
        </el-button>
      </div>
    </div>

    <div class="stats-grid">
      <div class="stat-card" :class="{ active: statusFilter === '' }" @click="setFilter('')">
        <div class="stat-icon-wrap info">
          <el-icon size="22"><Location /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ list.length }}</div>
          <div class="stat-label">工位总数</div>
        </div>
      </div>
      <div class="stat-card" :class="{ active: statusFilter === 'IDLE' }" @click="setFilter('IDLE')">
        <div class="stat-icon-wrap success">
          <el-icon size="22"><VideoPause /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ idleCount }}</div>
          <div class="stat-label">空闲</div>
        </div>
      </div>
      <div class="stat-card" :class="{ active: statusFilter === 'RUNNING' }" @click="setFilter('RUNNING')">
        <div class="stat-icon-wrap primary">
          <el-icon size="22"><Odometer /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ runningCount }}</div>
          <div class="stat-label">运行中</div>
        </div>
      </div>
      <div class="stat-card" :class="{ active: statusFilter === 'STOPPED' }" @click="setFilter('STOPPED')">
        <div class="stat-icon-wrap warning">
          <el-icon size="22"><CircleClose /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stoppedCount }}</div>
          <div class="stat-label">停用</div>
        </div>
      </div>
    </div>

    <div class="filter-bar">
      <el-input v-model="keyword" placeholder="搜索工位编码或名称..." clearable class="search-input">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="lineFilter" placeholder="所属生产线" clearable class="line-select">
        <el-option v-for="line in productionLines" :key="line.id" :label="line.lineName" :value="line.id" />
      </el-select>
      <el-select v-model="statusFilter" placeholder="状态" clearable class="status-select">
        <el-option label="空闲" value="IDLE" />
        <el-option label="运行中" value="RUNNING" />
        <el-option label="停用" value="STOPPED" />
      </el-select>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div class="table-panel">
      <div v-if="loading && !list.length" class="skeleton-wrap">
        <div v-for="i in 5" :key="i" class="skeleton-row">
          <el-skeleton animated>
            <template #template>
              <el-skeleton-item variant="rect" style="height: 44px; border-radius: 6px" />
            </template>
          </el-skeleton>
        </div>
      </div>
      <el-table v-else v-loading="loading" :data="filtered" style="width: 100%" empty-text="暂无工位数据">
        <el-table-column prop="workstationCode" label="工位编码" width="150">
          <template #default="{ row }">
            <span class="cell-code">{{ row.workstationCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="workstationName" label="工位名称" min-width="160">
          <template #default="{ row }">
            <div class="ws-name">
              <span class="name-icon"><el-icon :size="14"><Location /></el-icon></span>
              {{ row.workstationName }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="productionLineName" label="所属生产线" width="160">
          <template #default="{ row }">
            <span class="line-cell">{{ row.productionLineName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <span :class="['status-tag', `status-tag--${(row.status || '').toLowerCase()}`]">
              {{ statusMap[row.status] || row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="190">
          <template #default="{ row }">
            <span class="time-cell">{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" link size="small" @click="handleEdit(row)">
                <el-icon><Edit /></el-icon>编辑
              </el-button>
              <el-button type="danger" link size="small" @click="handleDelete(row)">
                <el-icon><Delete /></el-icon>删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="filtered.length" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          small
          background
          layout="total, prev, pager, next"
          :total="filtered.length"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="450px" class="theme-dialog">
      <el-form :model="form" label-width="100px">
        <el-form-item label="工位编码">
          <el-input v-model="form.workstationCode" placeholder="如: WS-001" />
        </el-form-item>
        <el-form-item label="工位名称">
          <el-input v-model="form.workstationName" placeholder="如: 加工工位1" />
        </el-form-item>
        <el-form-item label="所属生产线">
          <el-select v-model="form.productionLineId" placeholder="请选择生产线" style="width: 100%">
            <el-option v-for="line in productionLines" :key="line.id" :label="line.lineName" :value="line.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
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
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Location, VideoPause, Odometer, CircleClose, Search, Edit, Delete } from '@element-plus/icons-vue'
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
const keyword = ref('')
const statusFilter = ref('')
const lineFilter = ref<number | ''>('')
const currentPage = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('新增工位')
const form = ref<Partial<Workstation>>({ id: 0, workstationCode: '', workstationName: '', productionLineId: 0, status: 'IDLE' })

const statusMap: Record<string, string> = {
  IDLE: '空闲',
  RUNNING: '运行中',
  STOPPED: '停用'
}

const idleCount = computed(() => list.value.filter(r => r.status === 'IDLE').length)
const runningCount = computed(() => list.value.filter(r => r.status === 'RUNNING').length)
const stoppedCount = computed(() => list.value.filter(r => r.status === 'STOPPED').length)

const filtered = computed(() => {
  let arr = list.value
  if (statusFilter.value) {
    arr = arr.filter(r => r.status === statusFilter.value)
  }
  if (lineFilter.value !== '') {
    arr = arr.filter(r => r.productionLineId === lineFilter.value)
  }
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    arr = arr.filter(r => `${r.workstationCode} ${r.workstationName}`.toLowerCase().includes(kw))
  }
  return arr
})

const setFilter = (status: string) => { statusFilter.value = status; currentPage.value = 1 }

const handleReset = () => { keyword.value = ''; statusFilter.value = ''; lineFilter.value = ''; currentPage.value = 1 }

const formatTime = (time: string) => {
  if (!time) return '-'
  return String(time).replace('T', ' ').substring(0, 19)
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
  if (!form.value.workstationCode || !form.value.workstationName || !form.value.productionLineId) {
    ElMessage.warning('请填写编码、名称并选择生产线')
    return
  }
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
.page-wrapper { background: var(--bg-app); min-height: 100%; }

.create-btn { height: 36px; padding: 0 16px; border-radius: var(--radius-md); }

.stat-card {
  position: relative;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  overflow: hidden;
  transition: all var(--transition-normal);
  animation: fadeInUp 0.4s ease both;
}

.stat-card:hover {
  transform: translateY(-3px);
  border-color: var(--accent);
  box-shadow: var(--shadow-md);
}

.stat-card.active {
  border-color: var(--accent);
  background: var(--accent-light);
}

.stat-icon-wrap {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  flex-shrink: 0;
}

.stat-icon-wrap.info { background: var(--info-light); color: var(--info); }
.stat-icon-wrap.success { background: var(--success-light); color: var(--success); }
.stat-icon-wrap.primary { background: var(--accent-light); color: var(--accent); }
.stat-icon-wrap.warning { background: var(--warning-light); color: var(--warning); }

.stat-content { flex: 1; }
.stat-value { font-size: 26px; font-weight: 700; color: var(--text-primary); line-height: 1.2; }
.stat-label { font-size: 13px; color: var(--text-muted); margin-top: 3px; }

.search-input { width: 220px; }
.line-select { width: 170px; }
.status-select { width: 130px; }

.table-panel {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.table-panel :deep(.el-table th) { font-weight: 600; color: var(--text-secondary); font-size: 13px; }

.skeleton-wrap { display: flex; flex-direction: column; gap: 8px; padding: 16px; }

.cell-code { font-family: SF Mono, Consolas, monospace; color: var(--accent); font-weight: 500; }

.ws-name { display: flex; align-items: center; gap: 8px; color: var(--text-primary); }
.name-icon {
  width: 26px; height: 26px; display: flex; align-items: center; justify-content: center;
  background: var(--bg-hover); border-radius: var(--radius-sm); color: var(--accent); flex-shrink: 0;
}

.line-cell { color: var(--text-secondary); }

.status-tag--idle { background: var(--success-light); color: var(--success); }
.status-tag--running { background: var(--accent-light); color: var(--accent); }
.status-tag--stopped { background: var(--warning-light); color: var(--warning); }

.time-cell { color: var(--text-muted); font-size: 13px; }
.action-buttons { display: flex; gap: 4px; }

html.light .stat-card, html.light .table-panel { box-shadow: var(--shadow-sm); }
html.light .stat-card:hover { box-shadow: var(--shadow-md); }

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(15px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
