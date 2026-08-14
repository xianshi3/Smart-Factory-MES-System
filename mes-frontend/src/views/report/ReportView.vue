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
        <el-button class="ai-btn" @click="aiVisible = true">
          <el-icon><MagicStick /></el-icon>
          AI 助手
        </el-button>
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
      <el-select v-model="searchForm.dimension" class="dimension-select">
        <el-option label="按日统计" value="day" />
        <el-option label="按工位" value="workstation" />
        <el-option label="按工单" value="workOrder" />
      </el-select>
      <el-button type="primary" @click="loadData">
        <el-icon><Search /></el-icon>
        查询
      </el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div class="stats-row">
      <div v-for="(stat, index) in stats" :key="stat.label" class="stat-card" :style="{ animationDelay: `${index * 0.1}s` }">
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
      <div class="chart-card chart-main">
        <div class="card-header">
          <span class="card-title">
            <el-icon><TrendCharts /></el-icon>
            产量与良品率趋势
          </span>
        </div>
        <div class="chart-container">
          <v-chart :option="outputChart" autoresize style="height: 300px" />
        </div>
      </div>
      <div class="chart-card chart-side">
        <div class="card-header">
          <span class="card-title">
            <el-icon><Odometer /></el-icon>
            质量概览
          </span>
        </div>
        <div class="chart-container">
          <v-chart :option="qualityGauge" autoresize style="height: 300px" />
        </div>
      </div>
    </div>

    <div class="oee-card">
      <div class="card-header">
        <span class="card-title">
          <el-icon><PieChart /></el-icon>
          OEE 分解（可用率 × 性能率 × 质量率）
        </span>
        <span class="oee-total">综合OEE：<b :class="getOeeClass(oeeSummary.avgOee)">{{ oeeSummary.avgOee.toFixed(1) }}%</b></span>
      </div>
      <div class="oee-bars">
        <div v-for="item in oeeItems" :key="item.label" class="oee-item">
          <div class="oee-item-header">
            <span class="oee-item-label">{{ item.label }}</span>
            <span class="oee-item-value" :class="getOeeClass(item.value)">{{ item.value.toFixed(1) }}%</span>
          </div>
          <div class="oee-track">
            <div class="oee-fill" :class="getOeeClass(item.value)" :style="{ width: Math.min(item.value, 100) + '%' }"></div>
          </div>
        </div>
      </div>
    </div>

    <div class="table-section">
      <div class="section-header">
        <span class="section-title">
          <el-icon><List /></el-icon>
          生产明细
        </span>
        <span class="data-count">共 {{ totalRows }} 条记录</span>
      </div>
      
      <div class="table-wrapper">
        <el-table 
          v-loading="loading" 
          :data="pagedData"
          stripe
          class="modern-table"
        >
          <el-table-column :prop="searchForm.dimension === 'day' ? 'date' : 'date'" label="统计维度" min-width="120" align="center">
            <template #default="{ row }">
              <span class="date-cell">{{ row.date }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="output" label="产量" min-width="90" align="center">
            <template #default="{ row }">
              <span class="number-cell primary">{{ row.output || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="qualified" label="良品数" min-width="90" align="center">
            <template #default="{ row }">
              <span class="number-cell success">{{ row.qualified || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="defective" label="不良数" min-width="90" align="center">
            <template #default="{ row }">
              <span class="number-cell danger">{{ row.defective || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="可用率A" min-width="90" align="center">
            <template #default="{ row }">
              <span class="number-cell" :class="getOeeClass(row.availability)">{{ row.availability ? row.availability.toFixed(1) + '%' : '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="性能率P" min-width="90" align="center">
            <template #default="{ row }">
              <span class="number-cell" :class="getOeeClass(row.performance)">{{ row.performance ? row.performance.toFixed(1) + '%' : '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="质量率Q" min-width="90" align="center">
            <template #default="{ row }">
              <span class="number-cell" :class="getOeeClass(row.quality)">{{ row.quality ? row.quality.toFixed(1) + '%' : '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="oee" label="OEE" min-width="90" align="center">
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
      
      <div v-if="tableData.length > 0" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="tableData.length"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </div>

    <AiAssistant
      v-if="aiVisible"
      :visible="true"
      floating
      :context="aiContext"
      :scenarios="aiScenarios"
      @close="aiVisible = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getProductionReport } from '@/api/services'
import { useChartTheme } from '@/composables/useChartTheme'
import { DataAnalysis, Download, Refresh, TrendCharts, List, Search, Odometer, PieChart, MagicStick, Warning } from '@element-plus/icons-vue'
import AiAssistant from '@/components/ai/AiAssistant.vue'

const chartTheme = useChartTheme()
const loading = ref(false)
const tableData = ref<any[]>([])
const searchForm = reactive({ dateRange: [] as string[], dimension: 'day' })
const pagination = reactive({ page: 1, size: 10 })

/* ===== 页面级 AI 助手（生产报表） ===== */
const aiVisible = ref(false)
const aiScenarios = [
  { icon: DataAnalysis, text: '解读当前生产报表数据' },
  { icon: Warning, text: '预警产量/OEE异常波动' },
  { icon: TrendCharts, text: '分析生产趋势与影响因素' },
  { icon: MagicStick, text: '给出产能提升建议' },
]
const aiContext = computed(() => ({
  page: '生产报表',
  filters: { dimension: searchForm.dimension, dateRange: searchForm.dateRange },
  summary: {
    stats: stats.value.map((s: any) => ({ label: s.label, value: s.value })),
    oee: { avgOee: oeeSummary.avgOee, availability: oeeSummary.avgAvailability, performance: oeeSummary.avgPerformance, quality: oeeSummary.avgQuality },
    rows: tableData.value.map((r: any) => ({
      date: r.date, workstation: r.workstationName || r.workStationName, workOrder: r.workOrderNo,
      output: r.output, qualified: r.qualified, defective: r.defective, oee: r.oee,
    })),
  },
}))

const pagedData = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  return tableData.value.slice(start, start + pagination.size)
})
const totalRows = computed(() => tableData.value.length)

const oeeSummary = reactive({ avgOee: 0, avgAvailability: 0, avgPerformance: 0, avgQuality: 0 })
const oeeItems = computed(() => [
  { label: '可用率 (Availability)', value: oeeSummary.avgAvailability },
  { label: '性能率 (Performance)', value: oeeSummary.avgPerformance },
  { label: '质量率 (Quality)', value: oeeSummary.avgQuality }
])

const stats = ref([
  { label: '总产量', value: 0, icon: 'Coin', theme: 'primary' },
  { label: '良品数', value: 0, icon: 'CircleCheck', theme: 'success' },
  { label: '不良数', value: 0, icon: 'Warning', theme: 'danger' },
  { label: '平均OEE', value: '0.0%', icon: 'Odometer', theme: 'info' }
])

const outputChart = ref({})
const qualityGauge = ref({})

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

const getOeeClass = (value: number) => {
  if (!value || value === 0) return ''
  if (value >= 85) return 'high'
  if (value >= 70) return 'medium'
  return 'low'
}

const loadData = async () => {
  loading.value = true
  try {
    const params: any = { dimension: searchForm.dimension }
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }
    const res = await getProductionReport(params)
    const data = res.data || res
    
    stats.value = [
      { label: '总产量', value: data.totalOutput || 0, icon: 'Coin', theme: 'primary' },
      { label: '良品数', value: data.totalQualified || 0, icon: 'CircleCheck', theme: 'success' },
      { label: '不良数', value: data.totalDefective ?? ((data.totalOutput || 0) - (data.totalQualified || 0)), icon: 'Warning', theme: 'danger' },
      { label: '平均OEE', value: (data.avgOee || 0).toFixed(1) + '%', icon: 'Odometer', theme: 'info' }
    ]
    
    oeeSummary.avgOee = data.avgOee || 0
    oeeSummary.avgAvailability = data.avgAvailability || 0
    oeeSummary.avgPerformance = data.avgPerformance || 0
    oeeSummary.avgQuality = data.avgQuality || 0
    
    tableData.value = data.dailyData || []
    pagination.page = 1
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
    legend: { data: ['产量', '良品', 'OEE', '良品率'], textStyle: { color: textColor, fontSize: 12 }, top: 0 },
    grid: { left: '3%', right: '5%', bottom: '3%', top: '14%', containLabel: true },
    xAxis: { 
      type: 'category', 
      data: tableData.value.map(d => d.date),
      axisLine: { lineStyle: { color: lineColor } },
      axisLabel: { color: textColor, fontSize: 12 }
    },
    yAxis: [
      { 
        type: 'value', name: '数量',
        axisLine: { lineStyle: { color: lineColor } },
        axisLabel: { color: textColor },
        splitLine: { lineStyle: { color: splitLineColor } }
      },
      {
        type: 'value', name: '比率(%)', min: 0, max: 100,
        axisLine: { show: false },
        axisLabel: { color: textColor, formatter: '{value}%' },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '产量',
        type: 'bar',
        barWidth: '20%',
        data: tableData.value.map(d => d.output),
        itemStyle: { 
          color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#6366f1' }, { offset: 1, color: '#8b5cf6' }] },
          borderRadius: [4, 4, 0, 0]
        }
      },
      {
        name: '良品',
        type: 'bar',
        barWidth: '20%',
        data: tableData.value.map(d => d.qualified),
        itemStyle: { 
          color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#10b981' }, { offset: 1, color: '#34d399' }] },
          borderRadius: [4, 4, 0, 0]
        }
      },
      {
        name: 'OEE',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        data: tableData.value.map(d => d.oee),
        itemStyle: { color: '#f59e0b' },
        lineStyle: { width: 2 },
        symbolSize: 5
      },
      {
        name: '良品率',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        data: tableData.value.map(d => d.output ? (d.qualified / d.output * 100) : null),
        itemStyle: { color: '#3b82f6' },
        lineStyle: { width: 2, type: 'dashed' },
        symbolSize: 5
      }
    ]
  }

  // 质量概览仪表盘（良品率 + 平均OEE）
  const total = tableData.value.reduce((s, d) => s + (d.output || 0), 0)
  const qualified = tableData.value.reduce((s, d) => s + (d.qualified || 0), 0)
  const rate = total ? Math.round((qualified / total) * 100) : 0
  const avgOee = Math.round(oeeSummary.avgOee || 0)
  const gaugeCommon = {
    pointer: { length: '55%', width: 4 },
    axisLine: { lineStyle: { width: 10 } },
    axisTick: { show: false },
    splitLine: { length: 8, lineStyle: { width: 1 } },
    axisLabel: { color: textColor, fontSize: 10, distance: 14 },
    detail: { fontSize: 20, fontWeight: 700, offsetCenter: [0, '62%'], color: textColor },
    title: { offsetCenter: [0, '88%'], fontSize: 12, color: textColor }
  }
  qualityGauge.value = {
    tooltip: { trigger: 'item', backgroundColor: bgColor, borderColor, textStyle: { color: textColor } },
    series: [
      {
        type: 'gauge', min: 0, max: 100, startAngle: 200, endAngle: -20,
        radius: '85%', center: ['30%', '52%'],
        ...gaugeCommon,
        progress: { show: true, width: 10, itemStyle: { color: '#10b981' } },
        data: [{ value: rate, name: '良品率' }]
      },
      {
        type: 'gauge', min: 0, max: 100, startAngle: 200, endAngle: -20,
        radius: '85%', center: ['78%', '52%'],
        ...gaugeCommon,
        progress: { show: true, width: 10, itemStyle: { color: '#6366f1' } },
        data: [{ value: avgOee, name: '平均OEE' }]
      }
    ]
  }
}

const handleReset = () => {
  searchForm.dateRange = []
  searchForm.dimension = 'day'
  loadData()
}

const handleExport = () => {
  if (!tableData.value || tableData.value.length === 0) {
    ElMessage.warning('无数据可导出')
    return
  }
  const headers = ['统计维度', '产量', '良品数', '不良数', '可用率A(%)', '性能率P(%)', '质量率Q(%)', 'OEE(%)', '良品率(%)']
  const rows = tableData.value.map(r => [
    r.date,
    r.output || 0,
    r.qualified || 0,
    r.defective || 0,
    r.availability?.toFixed(1) ?? '',
    r.performance?.toFixed(1) ?? '',
    r.quality?.toFixed(1) ?? '',
    r.oee?.toFixed(1) ?? '',
    r.output ? ((r.qualified / r.output) * 100).toFixed(1) : ''
  ])
  const csv = [headers.join(','), ...rows.map(r => r.map(v => `"${String(v)}"`).join(','))].join('\n')
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

.dimension-select { width: 140px; }

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
  transition: all var(--transition-normal);
}

.stat-card:hover {
  transform: translateY(-3px);
  border-color: var(--accent);
  box-shadow: var(--shadow-md);
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

.content-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
  margin-bottom: 20px;
}

.chart-card,
.oee-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 12px;
  padding: 20px;
  animation: fadeIn 0.5s ease 0.2s both;
}

.oee-card { margin-bottom: 20px; }

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

.oee-total { font-size: 13px; color: var(--text-muted); }
.oee-total b { font-size: 15px; margin-left: 4px; }

.oee-bars { display: flex; flex-direction: column; gap: 14px; }

.oee-item-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 13px;
}

.oee-item-label { color: var(--text-secondary); }
.oee-item-value { font-weight: 600; }

.oee-track {
  height: 10px;
  background: var(--border-color);
  border-radius: 5px;
  overflow: hidden;
}

.oee-fill {
  height: 100%;
  border-radius: 5px;
  transition: width 0.6s ease;
}

.oee-fill.high { background: linear-gradient(90deg, #10b981, #34d399); }
.oee-fill.medium { background: linear-gradient(90deg, #f59e0b, #fbbf24); }
.oee-fill.low { background: linear-gradient(90deg, #ef4444, #f87171); }

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

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

html.light .stat-card,
html.light .chart-card,
html.light .oee-card,
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

@media (max-width: 900px) {
  .content-row { grid-template-columns: 1fr; }
}

@media (max-width: 768px) {
  .stats-row { grid-template-columns: 1fr; }
  .page-header { flex-direction: column; gap: 16px; }
  .header-actions { width: 100%; }
  .filter-bar { flex-wrap: wrap; }
  .date-picker { width: 100%; }
}
</style>
