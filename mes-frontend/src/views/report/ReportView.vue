<!-- Production Report View -->
<template>
  <div class="report-container">
    <div class="page-header">
      <div class="header-title">
        <el-icon size="24"><DataAnalysis /></el-icon>
        <h1>生产报表</h1>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="handleExport">
          <el-icon><Download /></el-icon>
          导出报表
        </el-button>
        <el-button @click="loadData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <div class="search-bar">
      <el-date-picker
        v-model="searchForm.dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
      />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div class="stats-grid">
      <div class="stat-card" v-for="(stat, index) in stats" :key="stat.label" :style="{ animationDelay: `${index * 0.1}s` }">
        <div class="stat-icon" :class="stat.theme">
          <el-icon size="28"><component :is="stat.icon" /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </div>
    </div>

    <div class="chart-section">
      <div class="chart-card">
        <div class="card-header">
          <span class="card-title">
            <el-icon><TrendCharts /></el-icon>
            日产量趋势
          </span>
        </div>
        <div class="chart-container">
          <v-chart :option="outputChart" autoresize style="height: 300px" />
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="card-header">
        <span class="card-title">
          <el-icon><List /></el-icon>
          每日生产明细
        </span>
      </div>
      <el-table :data="tableData" v-loading="loading">
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column prop="output" label="产量" width="100" />
        <el-table-column prop="qualified" label="良品数" width="100" />
        <el-table-column prop="defective" label="不良数" width="100">
          <template #default="{ row }">
            {{ (row.output || 0) - (row.qualified || 0) }}
          </template>
        </el-table-column>
        <el-table-column prop="oee" label="OEE(%)" width="100">
          <template #default="{ row }">
            {{ row.oee ? row.oee.toFixed(1) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="良品率" width="100">
          <template #default="{ row }">
            <el-tag :type="getRateType(row)" size="small">
              {{ getRate(row) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getProductionReport } from '@/api/services'
import { useThemeStore } from '@/stores/theme'
import { DataAnalysis, Download, Refresh, TrendCharts, List, Coin, CircleCheck, Warning, Odometer } from '@element-plus/icons-vue'

const themeStore = useThemeStore()
const loading = ref(false)
const tableData = ref<any[]>([])
const searchForm = reactive({
  dateRange: [] as string[]
})

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

const getRateType = (row: any) => {
  if (!row.output) return 'info'
  const rate = (row.qualified / row.output) * 100
  if (rate >= 95) return 'success'
  if (rate >= 85) return 'warning'
  return 'danger'
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
  const isDark = themeStore.isDark
  const textColor = isDark ? '#fff' : '#333'
  const lineColor = isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)'
  const splitLineColor = isDark ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)'
  const bgColor = isDark ? 'rgba(20,20,35,0.9)' : 'rgba(255,255,255,0.9)'
  const borderColor = isDark ? 'rgba(255,255,255,0.1)' : '#ddd'

  outputChart.value = {
    tooltip: { trigger: 'axis', backgroundColor: bgColor, borderColor, textStyle: { color: textColor } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: { 
      type: 'category', 
      data: tableData.value.map(d => d.date),
      axisLine: { lineStyle: { color: lineColor } },
      axisLabel: { color: textColor }
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
        data: tableData.value.map(d => d.output),
        itemStyle: { color: '#5470c6' }
      },
      {
        name: '良品',
        type: 'bar',
        data: tableData.value.map(d => d.qualified),
        itemStyle: { color: '#91cc75' }
      }
    ]
  }
}

const handleReset = () => {
  searchForm.dateRange = []
  loadData()
}

const handleExport = () => {
  ElMessage.success('报表导出功能开发中')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.report-container {
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

.header-actions {
  display: flex;
  gap: 12px;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  animation: fadeIn 0.5s ease 0.1s both;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 20px;
  animation: fadeIn 0.5s ease both;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon.primary { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; }
.stat-icon.success { background: linear-gradient(135deg, #11998e, #38ef7d); color: #fff; }
.stat-icon.danger { background: linear-gradient(135deg, #eb3349, #f45c43); color: #fff; }
.stat-icon.info { background: linear-gradient(135deg, #4facfe, #00f2fe); color: #fff; }

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: var(--text-primary);
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.chart-section {
  margin-bottom: 24px;
}

.chart-card, .table-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 20px;
  animation: fadeIn 0.5s ease 0.3s both;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 600;
}

html.light .card-title { color: #1a1a2e; }
html.light .stat-card { box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05); }
html.light .chart-card, html.light .table-card { box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05); }

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 1200px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>