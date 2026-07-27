<template>
  <div class="report-container">
    <div class="page-header">
      <div class="header-left">
        <div class="page-title">
          <el-icon size="24"><DataAnalysis /></el-icon>
          <h1>生产报表</h1>
        </div>
        <p class="page-desc">产量统计 · 良品率分析 · OEE监控</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
        <el-button @click="loadData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-date-picker
        v-model="searchForm.dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        class="date-picker"
      />
      <el-button type="primary" @click="loadData">
        <el-icon><Search /></el-icon>
        查询
      </el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div class="stats-row">
      <div class="stat-card" v-for="(stat, index) in stats" :key="stat.label" :style="{ animationDelay: `${index * 0.1}s` }">
        <div class="stat-icon" :class="stat.theme">
          <el-icon size="24"><component :is="stat.icon" /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </div>
    </div>

    <div class="content-row">
      <div class="chart-card">
        <div class="card-header">
          <span class="card-title">
            <el-icon><TrendCharts /></el-icon>
            日产量趋势
          </span>
        </div>
        <div class="chart-container">
          <v-chart :option="outputChart" autoresize style="height: 280px" />
        </div>
      </div>
    </div>

    <div class="table-section">
      <div class="section-header">
        <span class="section-title">
          <el-icon><List /></el-icon>
          生产明细
        </span>
        <span class="data-count">共 {{ tableData.length }} 条记录</span>
      </div>
      
      <div class="table-wrapper">
        <el-table 
          :data="tableData" 
          v-loading="loading"
          stripe
          class="modern-table"
        >
          <el-table-column prop="date" label="日期" min-width="120" align="center">
            <template #default="{ row }">
              <span class="date-cell">{{ row.date }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="output" label="产量" min-width="100" align="center">
            <template #default="{ row }">
              <span class="number-cell primary">{{ row.output || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="qualified" label="良品数" min-width="100" align="center">
            <template #default="{ row }">
              <span class="number-cell success">{{ row.qualified || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="不良数" min-width="100" align="center">
            <template #default="{ row }">
              <span class="number-cell danger">{{ (row.output || 0) - (row.qualified || 0) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="oee" label="OEE" min-width="100" align="center">
            <template #default="{ row }">
              <span class="number-cell" :class="getOeeClass(row.oee)">
                {{ row.oee ? row.oee.toFixed(1) + '%' : '-' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="良品率" min-width="120" align="center">
            <template #default="{ row }">
              <div class="rate-cell">
                <div class="rate-bar">
                  <div class="rate-fill" :style="{ width: getRate(row) }" :class="getRateClass(row)"></div>
                </div>
                <span class="rate-text" :class="getRateClass(row)">{{ getRate(row) }}</span>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
      
      <el-empty v-if="!loading && tableData.length === 0" description="暂无生产数据" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getProductionReport } from '@/api/services'
import { useThemeStore } from '@/stores/theme'
import { useChartTheme } from '@/composables/useChartTheme'
import { DataAnalysis, Download, Refresh, TrendCharts, List, Coin, CircleCheck, Warning, Odometer, Search } from '@element-plus/icons-vue'

const themeStore = useThemeStore()
const chartTheme = useChartTheme()
const loading = ref(false)
const tableData = ref<any[]>([])
const searchForm = reactive({ dateRange: [] as string[] })

const stats = ref([
  { label: '总产量', value: 0, icon: 'Coin', theme: 'primary' },
  { label: '良品数', value: 0, icon: 'CircleCheck', theme: 'success' },
  { label: '不良数', value: 0, icon: 'Warning', theme: 'danger' },
  { label: '平均OEE', value: '0%', icon: 'Odometer', theme: 'info' }
])

const outputChart = ref({})

const getRate = (row: any) => {
  if (!row.output || row.output === 0) return '-'
  return ((row.qualified / row.output) * 100).toFixed(1) + '%'
}

const getRateClass = (row: any) => {
  if (!row.output) return ''
  const rate = (row.qualified / row.output) * 100
  if (rate >= 95) return 'high'
  if (rate >= 85) return 'medium'
  return 'low'
}

const getOeeClass = (oee: number) => {
  if (!oee) return ''
  if (oee >= 85) return 'high'
  if (oee >= 70) return 'medium'
  return 'low'
}

const loadData = async () => {
  loading.value = true
  try {
    const params: any = {}
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }
    const res = await getProductionReport(params)
    const data = res.data || res
    
    stats.value = [
      { label: '总产量', value: data.totalOutput || 0, icon: 'Coin', theme: 'primary' },
      { label: '良品数', value: data.totalQualified || 0, icon: 'CircleCheck', theme: 'success' },
      { label: '不良数', value: (data.totalOutput || 0) - (data.totalQualified || 0), icon: 'Warning', theme: 'danger' },
      { label: '平均OEE', value: (data.avgOee || 0).toFixed(1) + '%', icon: 'Odometer', theme: 'info' }
    ]
    
    tableData.value = data.dailyData || []
    updateChart()
  } catch (error) {
    console.error('Failed to load report:', error)
    ElMessage.error('加载报表失败')
  } finally {
    loading.value = false
  }
}

const updateChart = () => {
  const t = chartTheme.value
  const { isDark, textColor, lineColor, splitLineColor } = t
  const bgColor = isDark ? 'rgba(20,20,35,0.9)' : 'rgba(255,255,255,0.9)'
  const borderColor = lineColor

  outputChart.value = {
    tooltip: { 
      trigger: 'axis', 
      backgroundColor: bgColor, 
      borderColor, 
      textStyle: { color: textColor },
      axisPointer: { type: 'shadow' }
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: { 
      type: 'category', 
      data: tableData.value.map(d => d.date),
      axisLine: { lineStyle: { color: lineColor } },
      axisLabel: { color: textColor, fontSize: 12 }
    },
    yAxis: { 
      type: 'value',
      axisLine: { lineStyle: { color: lineColor } },
      axisLabel: { color: textColor },
      splitLine: { lineStyle: { color: splitLineColor } }
    },
    series: [
      {
        name: '产量',
        type: 'bar',
        barWidth: '35%',
        data: tableData.value.map(d => d.output),
        itemStyle: { 
          color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#6366f1' }, { offset: 1, color: '#8b5cf6' }] },
          borderRadius: [4, 4, 0, 0]
        }
      },
      {
        name: '良品',
        type: 'bar',
        barWidth: '35%',
        data: tableData.value.map(d => d.qualified),
        itemStyle: { 
          color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#10b981' }, { offset: 1, color: '#34d399' }] },
          borderRadius: [4, 4, 0, 0]
        }
      }
    ]
  }
}

const handleReset = () => {
  searchForm.dateRange = []
  loadData()
}

const handleExport = () => {
  if (!tableData.value || tableData.value.length === 0) {
    ElMessage.warning('无数据可导出')
    return
  }
  const headers = Object.keys(tableData.value[0] || {})
  const csv = [headers.join(','), ...tableData.value.map(r => headers.map(h => `"${String(r[h] || '')}"`).join(','))].join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `生产报表_${new Date().toISOString().slice(0,10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('报表已导出为 CSV')
}

onMounted(() => { loadData() })
</script>

<style scoped>
.report-container {
  background: var(--bg-app);
  min-height: 100%;
}


.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  padding: 16px 20px;
  background: var(--bg-card);
  border-radius: 12px;
  border: 1px solid var(--border-light);
  align-items: center;
}

.date-picker {
  width: 280px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 12px;
  padding: 20px;
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon.primary { background: var(--accent-light); color: var(--accent); }
.stat-icon.success { background: var(--success-light); color: var(--success); }
.stat-icon.danger { background: var(--danger-light); color: var(--danger); }
.stat-icon.info { background: var(--info-light); color: var(--info); }

.stat-content { flex: 1; }
.stat-value { font-size: 26px; font-weight: 700; color: var(--text-primary); }
.stat-label { font-size: 13px; color: var(--text-muted); margin-top: 4px; }

.content-row { margin-bottom: 20px; }

.chart-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 12px;
  padding: 20px;
  animation: fadeIn 0.5s ease 0.2s both;
}

.card-header { margin-bottom: 16px; }

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 600;
}

.card-title .el-icon { color: var(--accent); }

.table-section {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 12px;
  padding: 20px;
  animation: fadeIn 0.5s ease 0.3s both;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 600;
}

.section-title .el-icon { color: var(--accent); }

.data-count {
  color: var(--text-muted);
  font-size: 13px;
}

.table-wrapper {
  overflow-x: auto;
}

.modern-table {
  width: 100%;
}

:deep(.modern-table .el-table__header th) {
  background: var(--bg-hover) !important;
  color: var(--text-secondary);
  font-weight: 600;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

:deep(.modern-table .el-table__body td) {
  padding: 14px 8px;
}

:deep(.modern-table .el-table__row--striped td) {
  background: var(--bg-hover) !important;
}

:deep(.modern-table .el-table__row:hover td) {
  background: var(--bg-hover) !important;
}

.date-cell {
  color: var(--text-secondary);
  font-size: 13px;
}

.number-cell {
  font-weight: 600;
  font-size: 14px;
}

.number-cell.primary { color: var(--accent); }
.number-cell.success { color: var(--success); }
.number-cell.danger { color: var(--danger); }
.number-cell.high { color: var(--success); }
.number-cell.medium { color: var(--warning); }
.number-cell.low { color: var(--danger); }

.rate-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rate-bar {
  flex: 1;
  height: 8px;
  background: var(--border-color);
  border-radius: 4px;
  overflow: hidden;
  max-width: 80px;
}

.rate-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.3s ease;
}

.rate-fill.high { background: linear-gradient(90deg, #10b981, #34d399); }
.rate-fill.medium { background: linear-gradient(90deg, #f59e0b, #fbbf24); }
.rate-fill.low { background: linear-gradient(90deg, #ef4444, #f87171); }

.rate-text {
  font-size: 13px;
  font-weight: 600;
  min-width: 45px;
}

.rate-text.high { color: var(--success); }
.rate-text.medium { color: var(--warning); }
.rate-text.low { color: var(--danger); }

html.light .stat-card,
html.light .chart-card,
html.light .table-section {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 1200px) {
  .stats-grid,
  .stats-row { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 768px) {
  .stats-row { grid-template-columns: 1fr; }
  .page-header { flex-direction: column; gap: 16px; }
  .header-actions { width: 100%; }
  .filter-bar { flex-wrap: wrap; }
  .date-picker { width: 100%; }
}
</style>