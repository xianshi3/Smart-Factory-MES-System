<template>
  <div class="alarm-page">
    <!-- ===== 头部 ===== -->
    <div class="page-header">
      <div class="header-left">
        <div class="page-title">
          <div class="header-badge" :class="{ pulse: activeCount > 0 }">
            <el-icon><WarningFilled /></el-icon>
            {{ activeCount > 0 ? '紧急' : '正常' }}
          </div>
          <h1>报警管理中心</h1>
        </div>
      </div>
      <div class="header-actions">
        <el-button class="ai-btn" @click="aiVisible = true">
          <el-icon><MagicStick /></el-icon>
          AI 助手
        </el-button>
        <el-button :disabled="!alarms.length" @click="exportCsv">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
        <el-button circle title="刷新" @click="loadAlarms">
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- ===== 统计 + 图表 ===== -->
    <div class="alarm-top-grid">
      <div class="stat-cards">
        <div
          v-for="(s, i) in statCards"
          :key="s.label"
          class="stat-card"
          :class="[`stat-${s.theme}`, { active: statusFilter === s.filter }]"
          @click="filterStatus(s.filter)"
        >
          <div class="stat-glow"></div>
          <div class="stat-icon"><el-icon :size="22"><component :is="s.icon" /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">{{ displayCount(i) }}</div>
            <div class="stat-label">{{ s.label }}</div>
          </div>
        </div>
      </div>
      <div class="chart-card mini">
        <div class="chart-title">
          <el-icon><PieChart /></el-icon>
          级别分布
        </div>
        <ChartMini :option="levelChart" :height="'110px'" />
      </div>
      <div class="chart-card mini">
        <div class="chart-title">
          <el-icon><Histogram /></el-icon>
          今日告警时段
        </div>
        <ChartMini :option="hourChart" :height="'110px'" />
      </div>
    </div>

    <!-- ===== 筛选栏 ===== -->
    <div class="filter-bar">
      <div class="filters-left">
        <span class="fchip" :class="{ on: statusFilter === '' }" @click="filterStatus('')">全部</span>
        <span class="fchip" :class="{ on: statusFilter === 'ACTIVE' }" @click="filterStatus('ACTIVE')">活跃</span>
        <span class="fchip" :class="{ on: statusFilter === 'ACKNOWLEDGED' }" @click="filterStatus('ACKNOWLEDGED')">已确认</span>
        <span class="fchip" :class="{ on: statusFilter === 'RESOLVED' }" @click="filterStatus('RESOLVED')">已解决</span>
        <span class="fsep"></span>
        <span class="fchip lvl" :class="{ on: levelFilter === '' }" @click="levelFilter = ''">全部级别</span>
        <span class="fchip lvl critical" :class="{ on: levelFilter === 'CRITICAL' }" @click="levelFilter = 'CRITICAL'">严重</span>
        <span class="fchip lvl major" :class="{ on: levelFilter === 'MAJOR' }" @click="levelFilter = 'MAJOR'">警告</span>
        <span class="fchip lvl minor" :class="{ on: levelFilter === 'MINOR' }" @click="levelFilter = 'MINOR'">提示</span>
      </div>
      <div class="filters-right">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          size="default"
          value-format="YYYY-MM-DD"
        />
        <el-input
          v-model="searchKeyword"
          placeholder="搜索告警..."
          style="width: 190px"
          clearable
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <div v-if="selectedRows.length" class="batch-actions">
          <el-button size="small" type="primary" plain @click="batchAck">
            <el-icon><Check /></el-icon>
            批量确认 ({{ selectedRows.length }})
          </el-button>
          <el-button size="small" type="success" plain @click="batchResolve">
            <el-icon><CircleCheck /></el-icon>
            批量解决
          </el-button>
        </div>
      </div>
    </div>

    <!-- ===== 表格 ===== -->
    <div class="alarm-table-panel">
      <el-alert
        v-if="loadError"
        title="告警数据加载失败（后端服务未启动或不可达）"
        type="error"
        show-icon
        class="load-error-bar"
      >
        <template #default>
          <el-button size="small" @click="loadAlarms">重试</el-button>
        </template>
      </el-alert>
      <el-table
        v-loading="loading"
        :data="pagedAlarms"
        :row-class-name="rowClass"
        style="width: 100%"
        empty-text="暂无告警记录"
        @row-click="openDetail"
        @selection-change="(rows: any[]) => selectedRows = rows"
      >
        <el-table-column type="selection" width="42" />
        <el-table-column prop="alarmCode" label="告警编码" width="150" sortable>
          <template #default="{ row }">
            <div class="alarm-code">{{ row.alarmCode }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="告警信息" min-width="240">
          <template #default="{ row }">
            <div class="alarm-message" :title="row.message">{{ row.message }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="级别" width="100" sortable>
          <template #default="{ row }">
            <div class="level-tag" :class="(row.level || 'minor').toLowerCase()">
              <i class="level-dot"></i>{{ getLevelText(row.level) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="deviceName" label="设备" width="140" sortable>
          <template #default="{ row }">
            <div class="device-name">
              <el-icon><Monitor /></el-icon>
              {{ row.deviceName || row.deviceCode || '-' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110" sortable>
          <template #default="{ row }">
            <div class="status-tag" :class="(row.status || '').toLowerCase()">
              {{ getStatusText(row.status) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="occurrenceTime" label="发生时间" width="175" sortable>
          <template #default="{ row }">
            <div class="time-cell">
              <span class="rel-time" :class="{ hot: isRecent(row.occurrenceTime) }">{{ relTime(row.occurrenceTime) }}</span>
              <span class="abs-time">{{ formatTime(row.occurrenceTime) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons" @click.stop>
              <el-button v-if="row.status === 'ACTIVE'" type="primary" link size="small" @click="handleAck(row)">
                <el-icon><Check /></el-icon>
                确认
              </el-button>
              <el-button v-if="row.status === 'ACKNOWLEDGED'" type="success" link size="small" @click="handleResolve(row)">
                <el-icon><CircleCheck /></el-icon>
                解决
              </el-button>
              <el-button link size="small" @click="openDetail(row)">
                <el-icon><View /></el-icon>
                详情
              </el-button>
              <el-button v-if="row.status !== 'RESOLVED'" type="danger" link size="small" @click="handleDelete(row)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="pagedAlarms.length" class="pagination-wrap">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="filteredAlarms.length"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
        />
      </div>
    </div>

    <!-- ===== 解决弹窗 ===== -->
    <el-dialog v-model="resolveDialogVisible" title="解决告警" width="440px">
      <el-form label-width="80px">
        <el-form-item label="告警">{{ currentAlarm.alarmCode }} - {{ currentAlarm.message }}</el-form-item>
        <el-form-item label="解决备注" required>
          <el-input v-model="resolveRemarks" type="textarea" rows="4" placeholder="请描述解决方案和处理结果..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resolveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitResolve">确认解决</el-button>
      </template>
    </el-dialog>

    <!-- ===== 详情抽屉 ===== -->
    <el-drawer v-model="detailVisible" :title="detailAlarm?.alarmCode || '告警详情'" size="420px">
      <div v-if="detailAlarm" class="alarm-detail">
        <div class="detail-hero" :class="`lvl-${(detailAlarm.level || 'minor').toLowerCase()}`">
          <div class="detail-level">
            <el-icon size="26"><WarningFilled /></el-icon>
            {{ getLevelText(detailAlarm.level) }}
          </div>
          <div class="detail-msg">{{ detailAlarm.message }}</div>
        </div>

        <div class="detail-grid">
          <div class="dg-item">
            <span class="dg-label">告警编码</span>
            <span class="dg-value">{{ detailAlarm.alarmCode || '-' }}</span>
          </div>
          <div class="dg-item">
            <span class="dg-label">设备</span>
            <span class="dg-value">{{ detailAlarm.deviceName || detailAlarm.deviceCode || '-' }}</span>
          </div>
          <div class="dg-item">
            <span class="dg-label">状态</span>
            <span class="dg-value">{{ getStatusText(detailAlarm.status) }}</span>
          </div>
          <div class="dg-item">
            <span class="dg-label">发生时间</span>
            <span class="dg-value">{{ formatTime(detailAlarm.occurrenceTime) }}</span>
          </div>
          <div class="dg-item">
            <span class="dg-label">确认人</span>
            <span class="dg-value">{{ detailAlarm.acknowledgedBy || '-' }}</span>
          </div>
          <div class="dg-item">
            <span class="dg-label">确认时间</span>
            <span class="dg-value">{{ detailAlarm.acknowledgedTime ? formatTime(detailAlarm.acknowledgedTime) : '-' }}</span>
          </div>
          <div class="dg-item">
            <span class="dg-label">解决人</span>
            <span class="dg-value">{{ detailAlarm.resolvedBy || '-' }}</span>
          </div>
          <div class="dg-item">
            <span class="dg-label">解决备注</span>
            <span class="dg-value">{{ detailAlarm.resolveRemarks || '-' }}</span>
          </div>
        </div>

        <div class="detail-ai">
          <div class="dai-head">
            <el-icon><MagicStick /></el-icon>
            AI 告警分析
            <el-button size="small" type="primary" plain :loading="aiAnalyzing" @click="analyzeAlarm">
              {{ aiAnalyzing ? '分析中...' : aiAdvice ? '重新分析' : '开始分析' }}
            </el-button>
          </div>
          <div v-if="aiAnalyzing" class="dai-thinking">
            <div class="ai-thinking-dots"><i></i><i></i><i></i></div>
            <span>AI 正在分析告警根因…</span>
          </div>
          <div v-else-if="aiAdvice" class="dai-result" v-html="aiAdviceHtml"></div>
          <div v-else class="dai-empty">基于设备孪生数据与知识库，生成根因分析与处置建议</div>
        </div>
      </div>
    </el-drawer>

    <AiAssistant
      v-if="aiVisible"
      :visible="true"
      floating
      auto-new
      :context="aiContext"
      :scenarios="aiScenarios"
      @close="aiVisible = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAllAlarms, acknowledgeAlarm, resolveAlarm, deleteAlarm, llmChat } from '@/api/services'
import {
  WarningFilled, Warning, CircleCheck, Refresh, List, Monitor, Check, Delete, Search,
  MagicStick, DataAnalysis, Download, View, PieChart, Histogram,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import AiAssistant from '@/components/ai/AiAssistant.vue'
import ChartMini from '@/components/common/ChartMini.vue'
import { useChartTheme } from '@/composables/useChartTheme'
import { mdToHtml } from '@/utils/markdown'

const userStore = useUserStore()
const chartTheme = useChartTheme()

/* ===== 页面级 AI 助手 ===== */
const aiVisible = ref(false)
const aiScenarios = [
  { icon: DataAnalysis, text: '分析当前告警分布与根因' },
  { icon: Warning, text: '定位最高频告警设备与类型' },
  { icon: MagicStick, text: '给出告警处置优先级建议' },
  { icon: Search, text: '总结告警趋势与改进方向' },
]
const aiContext = computed(() => ({
  page: '报警中心',
  summary: {
    counts: { active: activeCount.value, acknowledged: ackCount.value, resolved: resolvedCount.value, total: totalCount.value },
    alarms: filteredAlarms.value.slice(0, 50).map((a: any) => ({
      deviceCode: a.deviceCode, deviceName: a.deviceName,
      alarmType: a.alarmType, level: a.level, status: a.status,
      message: a.message, time: a.alarmTime || a.createTime,
    })),
  },
}))

const alarms = ref<any[]>([])
const loading = ref(false)
const loadError = ref(false)
const statusFilter = ref('')
const levelFilter = ref('')
const searchKeyword = ref('')
const dateRange = ref<[Date, Date] | null>(null)
const currentPage = ref(1)
const pageSize = ref(10)
const resolveDialogVisible = ref(false)
const resolveRemarks = ref('')
const currentAlarm = ref<any>({})

/* ===== 详情抽屉 + AI 分析 ===== */
const detailVisible = ref(false)
const detailAlarm = ref<any>(null)
const aiAnalyzing = ref(false)
const aiAdvice = ref('')
const aiAdviceHtml = computed(() => mdToHtml(aiAdvice.value))

const openDetail = (row: any) => {
  detailAlarm.value = row
  detailVisible.value = true
}

const analyzeAlarm = async () => {
  if (!detailAlarm.value || aiAnalyzing.value) return
  aiAnalyzing.value = true
  aiAdvice.value = ''
  try {
    const a = detailAlarm.value
    const res = await llmChat({
      message: '请对这条工业设备告警进行专业分析，输出：1) 可能根因（2-3条） 2) 处置建议（按优先级） 3) 预防措施。简明扼要。',
      context: {
        alarm: {
          code: a.alarmCode, message: a.message, level: a.level,
          device: a.deviceName || a.deviceCode, status: a.status,
          time: a.occurrenceTime,
        },
      },
      history: [],
    })
    if (res && res.success && res.content) {
      aiAdvice.value = res.content
    } else {
      aiAdvice.value = '**AI 服务暂不可用**，请确认 mes-ai-service 已启动并配置智谱 API Key。'
    }
  } catch (e: any) {
    aiAdvice.value = '**AI 分析失败**：' + (e?.message || '网络错误')
  } finally {
    aiAnalyzing.value = false
  }
}

/* ===== 统计 ===== */
const activeCount = computed(() => alarms.value.filter(a => a.status === 'ACTIVE').length)
const ackCount = computed(() => alarms.value.filter(a => a.status === 'ACKNOWLEDGED').length)
const resolvedCount = computed(() => alarms.value.filter(a => a.status === 'RESOLVED').length)
const totalCount = computed(() => alarms.value.length)

const statCards = computed(() => [
  { label: '活跃告警', value: activeCount.value, icon: WarningFilled, theme: 'danger', filter: 'ACTIVE' },
  { label: '已确认', value: ackCount.value, icon: Warning, theme: 'warning', filter: 'ACKNOWLEDGED' },
  { label: '已解决', value: resolvedCount.value, icon: CircleCheck, theme: 'success', filter: 'RESOLVED' },
  { label: '告警总数', value: totalCount.value, icon: List, theme: 'primary', filter: '' },
])

/* 数字滚动 */
const animatedCounts = ref<number[]>([0, 0, 0, 0])
let countTimer: number | null = null
const displayCount = (i: number) => Math.round(animatedCounts.value[i])
const runCountUp = () => {
  if (countTimer) clearInterval(countTimer)
  const start = [...animatedCounts.value]
  const end = statCards.value.map(s => s.value)
  const t0 = performance.now()
  const dur = 450
  countTimer = window.setInterval(() => {
    const p = Math.min(1, (performance.now() - t0) / dur)
    const ease = 1 - Math.pow(1 - p, 3)
    animatedCounts.value = start.map((s, i) => s + (end[i] - s) * ease)
    if (p >= 1) {
      animatedCounts.value = [...end]
      if (countTimer) clearInterval(countTimer)
      countTimer = null
    }
  }, 16)
}

/* ===== 筛选 ===== */
const filteredAlarms = computed(() => {
  let list = alarms.value
  if (statusFilter.value) {
    list = list.filter(a => a.status === statusFilter.value)
  }
  if (levelFilter.value) {
    list = list.filter(a => (a.level || '').toUpperCase() === levelFilter.value)
  }
  if (dateRange.value) {
    const [start, end] = dateRange.value
    const startT = new Date(start).setHours(0, 0, 0, 0)
    const endT = new Date(end).setHours(23, 59, 59, 999)
    list = list.filter(a => {
      const t = new Date(a.occurrenceTime).getTime()
      return !Number.isNaN(t) && t >= startT && t <= endT
    })
  }
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(a =>
      (a.message || '').toLowerCase().includes(kw) ||
      (a.deviceName || '').toLowerCase().includes(kw) ||
      (a.alarmCode || '').toLowerCase().includes(kw)
    )
  }
  return list
})

watch([statusFilter, levelFilter, searchKeyword, dateRange], () => { currentPage.value = 1 })

const pagedAlarms = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredAlarms.value.slice(start, start + pageSize.value)
})

/* ===== 图表 ===== */
const levelChart = computed(() => {
  const t = chartTheme.value
  const levelMap: Record<string, string> = {
    CRITICAL: '严重', MAJOR: '警告', MINOR: '提示', WARNING: '警告', INFO: '提示', HIGH: '严重',
  }
  const colors: Record<string, string> = { 严重: '#ef4444', 警告: '#f59e0b', 提示: '#3b82f6' }
  const counts: Record<string, number> = {}
  alarms.value.forEach(a => {
    const k = levelMap[(a.level || '').toUpperCase()] || '提示'
    counts[k] = (counts[k] || 0) + 1
  })
  return {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['55%', '78%'],
      center: ['50%', '50%'],
      data: Object.entries(counts).map(([k, v]) => ({ name: k, value: v, itemStyle: { color: colors[k] } })),
      label: { show: true, color: t.textColor, fontSize: 10, formatter: '{b} {c}' },
      labelLine: { length: 6, length2: 4 },
    }],
  }
})

const hourChart = computed(() => {
  const t = chartTheme.value
  const hours = Array.from({ length: 24 }, (_, i) => i)
  const counts = new Array(24).fill(0)
  const today = new Date().toDateString()
  alarms.value.forEach(a => {
    const d = new Date(a.occurrenceTime)
    if (!Number.isNaN(d.getTime()) && d.toDateString() === today) {
      counts[d.getHours()]++
    }
  })
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 26, right: 8, top: 8, bottom: 18 },
    xAxis: {
      type: 'category',
      data: hours.map(h => `${h}h`),
      axisLabel: { color: t.labelColor, fontSize: 9, interval: 3 },
      axisLine: { lineStyle: { color: t.lineColor } },
    },
    yAxis: {
      type: 'value', minInterval: 1,
      axisLabel: { color: t.labelColor, fontSize: 9 },
      splitLine: { lineStyle: { color: t.splitLineColor } },
    },
    series: [{
      type: 'bar',
      data: counts,
      itemStyle: {
        borderRadius: [3, 3, 0, 0],
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [{ offset: 0, color: '#f87171' }, { offset: 1, color: 'rgba(239,68,68,0.25)' }],
        },
      },
      barWidth: '55%',
    }],
  }
})

/* ===== 行样式 ===== */
const rowClass = ({ row }: { row: any }) => {
  const lvl = (row.level || 'minor').toLowerCase()
  return row.status === 'ACTIVE' ? `row-active row-${lvl}` : ''
}

const getLevelText = (level: string) => {
  const map: Record<string, string> = {
    CRITICAL: '严重', MAJOR: '警告', MINOR: '提示',
    WARNING: '警告', INFO: '提示', HIGH: '严重', LOW: '提示',
  }
  return map[(level || '').toUpperCase()] || '提示'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = { ACTIVE: '活跃', ACKNOWLEDGED: '已确认', RESOLVED: '已解决' }
  return map[status] || status || '-'
}

const formatTime = (time: string) => {
  if (!time) return '-'
  const d = new Date(time)
  if (Number.isNaN(d.getTime())) return time
  return d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const relTime = (time: string) => {
  if (!time) return ''
  const d = new Date(time).getTime()
  if (Number.isNaN(d)) return ''
  const diff = Date.now() - d
  if (diff < 60_000) return '刚刚'
  if (diff < 3600_000) return Math.floor(diff / 60_000) + ' 分钟前'
  if (diff < 86400_000) return Math.floor(diff / 3600_000) + ' 小时前'
  return ''
}

const isRecent = (time: string) => {
  const d = new Date(time).getTime()
  return !Number.isNaN(d) && Date.now() - d < 3600_000
}

/* ===== 数据加载 ===== */
const loadAlarms = async () => {
  loading.value = true
  try {
    const res = await getAllAlarms()
    const raw = res?.data ?? res
    alarms.value = Array.isArray(raw) ? raw : Array.isArray(raw?.records) ? raw.records : []
    loadError.value = false
    runCountUp()
  } catch (e: any) {
    console.error('[Alarm] Load error:', e)
    loadError.value = true
  } finally {
    loading.value = false
  }
}

const filterStatus = (status: string) => {
  statusFilter.value = status
}

/* ===== 操作 ===== */
const selectedRows = ref<any[]>([])

const handleAck = async (row: any) => {
  try {
    await ElMessageBox.confirm('确认此告警?', '提示', { type: 'info' })
    await acknowledgeAlarm(row.id, userStore.userInfo?.username || 'unknown')
    ElMessage.success('告警已确认')
    loadAlarms()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const batchAck = async () => {
  try {
    await ElMessageBox.confirm(`批量确认 ${selectedRows.value.length} 条告警?`, '提示', { type: 'info' })
    for (const row of selectedRows.value) {
      if (row.status === 'ACTIVE') {
        await acknowledgeAlarm(row.id, userStore.userInfo?.username || 'unknown')
      }
    }
    ElMessage.success('批量确认完成')
    selectedRows.value = []
    loadAlarms()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

const handleResolve = async (row: any) => {
  currentAlarm.value = row
  resolveRemarks.value = ''
  resolveDialogVisible.value = true
}

const batchResolve = async () => {
  try {
    const { value } = await ElMessageBox.prompt('批量解决备注（可留空）', '批量解决', {
      inputValue: '批量处理',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    for (const row of selectedRows.value) {
      if (row.status === 'ACKNOWLEDGED') {
        await resolveAlarm(row.id, userStore.userInfo?.username || 'unknown', value || '批量处理')
      }
    }
    ElMessage.success('批量解决完成')
    selectedRows.value = []
    loadAlarms()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

const submitResolve = async () => {
  try {
    await resolveAlarm(currentAlarm.value.id, userStore.userInfo?.username || 'unknown', resolveRemarks.value)
    ElMessage.success('告警已解决')
    resolveDialogVisible.value = false
    loadAlarms()
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('删除后无法恢复,确定删除?', '警告', { type: 'warning' })
    await deleteAlarm(row.id)
    ElMessage.success('已删除')
    loadAlarms()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

/* ===== 导出 CSV ===== */
const exportCsv = () => {
  const header = ['告警编码', '告警信息', '级别', '设备', '状态', '发生时间']
  const rows = filteredAlarms.value.map(a => [
    a.alarmCode || '', (a.message || '').replace(/,/g, '，'),
    getLevelText(a.level), a.deviceName || a.deviceCode || '-',
    getStatusText(a.status), a.occurrenceTime || '',
  ])
  const csv = '\uFEFF' + [header, ...rows].map(r => r.map(c => `"${c}"`).join(',')).join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `告警记录_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('已导出 CSV')
}

/* ===== 生命周期 ===== */
let autoRefreshTimer: number | null = null

onMounted(() => {
  loadAlarms()
  autoRefreshTimer = window.setInterval(loadAlarms, 10000)
})

onUnmounted(() => {
  if (autoRefreshTimer) clearInterval(autoRefreshTimer)
  if (countTimer) clearInterval(countTimer)
})
</script>

<style scoped>
.alarm-page { padding: 0; }
.load-error-bar { margin-bottom: 12px; }

/* ===== 头部 ===== */
.header-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 700;
  color: var(--success);
  background: var(--success-light);
  border: 1px solid var(--success);
  transition: all var(--transition-normal);
}
.header-badge.pulse {
  color: var(--danger);
  background: var(--danger-light);
  border-color: var(--danger);
  animation: badge-pulse 1.5s ease-in-out infinite;
}
@keyframes badge-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.45); }
  50% { box-shadow: 0 0 0 8px rgba(239, 68, 68, 0); }
}

/* ===== 顶部统计+图表 ===== */
.alarm-top-grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}
.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.stat-card {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  padding: 14px 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--transition-normal);
  animation: fadeInUp 0.5s ease both;
}
.stat-card:hover, .stat-card.active {
  transform: translateY(-2px);
  border-color: var(--accent);
  box-shadow: var(--shadow-md);
}
.stat-glow {
  position: absolute;
  top: -40%; right: -30%;
  width: 100px; height: 100px;
  border-radius: 50%;
  filter: blur(30px);
  opacity: 0.2;
  pointer-events: none;
}
.stat-danger .stat-glow { background: #ef4444; }
.stat-warning .stat-glow { background: #f59e0b; }
.stat-success .stat-glow { background: #10b981; }
.stat-primary .stat-glow { background: #6366f1; }
.stat-icon {
  width: 38px; height: 38px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 10px;
}
.stat-danger .stat-icon { background: var(--danger-light); color: var(--danger); }
.stat-warning .stat-icon { background: var(--warning-light); color: var(--warning); }
.stat-success .stat-icon { background: var(--success-light); color: var(--success); }
.stat-primary .stat-icon { background: var(--accent-light); color: var(--accent); }
.stat-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.stat-label { font-size: 12px; color: var(--text-muted); }

.chart-card.mini {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  animation: fadeInUp 0.5s ease 0.1s both;
}
.chart-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}
.chart-title .el-icon { color: var(--accent); }

/* ===== 筛选栏 ===== */
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: var(--bg-card);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  flex-wrap: wrap;
}
.filters-left, .filters-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.fchip {
  padding: 5px 12px;
  border-radius: 16px;
  font-size: 12px;
  cursor: pointer;
  color: var(--text-secondary);
  background: var(--bg-hover);
  border: 1px solid transparent;
  transition: all var(--transition-fast);
  user-select: none;
}
.fchip:hover { color: var(--text-primary); border-color: var(--border-color); }
.fchip.on {
  color: var(--accent);
  background: var(--accent-light);
  border-color: var(--accent);
  font-weight: 600;
}
.fchip.lvl.critical.on { color: #ef4444; background: rgba(239,68,68,0.12); border-color: #ef4444; }
.fchip.lvl.major.on { color: #f59e0b; background: rgba(245,158,11,0.12); border-color: #f59e0b; }
.fchip.lvl.minor.on { color: #3b82f6; background: rgba(59,130,246,0.12); border-color: #3b82f6; }
.fsep {
  width: 1px; height: 16px;
  background: var(--border-color);
  margin: 0 4px;
}
.batch-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  padding-left: 8px;
  border-left: 1px solid var(--border-light);
}

/* ===== 表格 ===== */
.alarm-table-panel {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 8px;
}
.alarm-code { font-family: Consolas, monospace; font-size: 12px; color: var(--text-secondary); }
.alarm-message {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 420px;
  color: var(--text-primary);
}
.level-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
}
.level-dot { width: 6px; height: 6px; border-radius: 50%; }
.level-tag.critical { color: #ef4444; background: rgba(239,68,68,0.1); }
.level-tag.critical .level-dot { background: #ef4444; box-shadow: 0 0 6px #ef4444; }
.level-tag.major, .level-tag.warning { color: #f59e0b; background: rgba(245,158,11,0.1); }
.level-tag.major .level-dot, .level-tag.warning .level-dot { background: #f59e0b; }
.level-tag.minor, .level-tag.info, .level-tag.low { color: #3b82f6; background: rgba(59,130,246,0.1); }
.level-tag.minor .level-dot, .level-tag.info .level-dot { background: #3b82f6; }
.device-name { display: flex; align-items: center; gap: 6px; color: var(--text-primary); }
.status-tag {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
}
.status-tag.active { color: var(--danger); background: var(--danger-light); }
.status-tag.acknowledged { color: var(--warning); background: var(--warning-light); }
.status-tag.resolved { color: var(--success); background: var(--success-light); }
.time-cell { display: flex; flex-direction: column; }
.rel-time { font-size: 12px; color: var(--text-muted); }
.rel-time.hot { color: var(--danger); font-weight: 600; }
.abs-time { font-size: 11px; color: var(--text-muted); }
.action-buttons { display: flex; align-items: center; }

:deep(.row-active td) { background: rgba(239, 68, 68, 0.03); }
:deep(.row-active:hover td) { background: rgba(239, 68, 68, 0.07) !important; }
:deep(.row-active td:first-child) {
  box-shadow: inset 3px 0 0 #ef4444;
}
:deep(.row-active.row-major td:first-child) { box-shadow: inset 3px 0 0 #f59e0b; }
:deep(.row-active.row-minor td:first-child) { box-shadow: inset 3px 0 0 #3b82f6; }

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding: 12px 8px 4px;
}

/* ===== 详情抽屉 ===== */
.alarm-detail { display: flex; flex-direction: column; gap: 16px; }
.detail-hero {
  padding: 18px;
  border-radius: var(--radius-lg);
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.detail-hero.lvl-critical { background: rgba(239,68,68,0.08); border: 1px solid rgba(239,68,68,0.35); }
.detail-hero.lvl-major, .detail-hero.lvl-warning { background: rgba(245,158,11,0.08); border: 1px solid rgba(245,158,11,0.35); }
.detail-hero.lvl-minor, .detail-hero.lvl-info { background: rgba(59,130,246,0.08); border: 1px solid rgba(59,130,246,0.35); }
.detail-level {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
}
.detail-hero.lvl-critical .detail-level { color: #ef4444; }
.detail-hero.lvl-major .detail-level { color: #f59e0b; }
.detail-hero.lvl-minor .detail-level { color: #3b82f6; }
.detail-msg { font-size: 14px; color: var(--text-primary); line-height: 1.6; }

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.dg-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 10px 12px;
  background: var(--bg-hover);
  border-radius: 10px;
}
.dg-label { font-size: 11px; color: var(--text-muted); }
.dg-value { font-size: 13px; color: var(--text-primary); word-break: break-all; }

.detail-ai {
  padding: 14px;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(99, 102, 241, 0.35);
  background: rgba(99, 102, 241, 0.05);
}
.dai-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 10px;
}
.dai-head .el-icon { color: var(--accent); }
.dai-head .el-button { margin-left: auto; }
.dai-thinking {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text-secondary);
  font-size: 12px;
}
.ai-thinking-dots { display: flex; gap: 4px; }
.ai-thinking-dots i {
  width: 6px; height: 6px;
  border-radius: 50%;
  background: var(--accent);
  animation: bounce-dot 1.2s ease-in-out infinite;
}
.ai-thinking-dots i:nth-child(2) { animation-delay: 0.15s; }
.ai-thinking-dots i:nth-child(3) { animation-delay: 0.3s; }
.dai-result {
  font-size: 13px;
  line-height: 1.8;
  color: var(--text-secondary);
}
.dai-result :deep(strong) { color: var(--text-primary); }
.dai-result :deep(ol), .dai-result :deep(ul) { padding-left: 20px; }
.dai-empty { font-size: 12px; color: var(--text-muted); }

@keyframes bounce-dot {
  0%, 100% { transform: translateY(0); opacity: 0.5; }
  50% { transform: translateY(-4px); opacity: 1; }
}
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 1400px) {
  .alarm-top-grid { grid-template-columns: 1fr; }
}
</style>
