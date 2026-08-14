<template>
  <div class="dashboard">
    <div class="dashboard-header">
      <div class="welcome-section">
        <h1 class="welcome-title">
          欢迎回来, <span class="username">{{ userStore.userInfo?.username || '管理员' }}</span>
        </h1>
        <p class="welcome-subtitle">{{ currentDate }} · {{ currentTime }}</p>
      </div>
      <div class="header-actions">
        <el-button circle class="refresh-btn" :loading="loading" @click="refresh">
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>
    </div>

    <el-alert v-if="error" :title="error" type="error" show-icon closable class="mb-4" @close="error = ''" />

    <div class="stats-grid">
      <template v-if="loading && !devices.length">
        <div v-for="i in 4" :key="i" class="stat-skeleton">
          <el-skeleton animated>
            <template #template>
              <el-skeleton-item variant="rect" style="height: 96px; border-radius: var(--radius-lg)" />
            </template>
          </el-skeleton>
        </div>
      </template>
      <template v-else>
        <StatCard
          v-for="(stat, index) in stats"
          :key="stat.label"
          :icon="stat.icon"
          :label="stat.label"
          :value="stat.value"
          :theme="stat.theme"
          :delay="index * 0.1"
        />
      </template>
    </div>

    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24" :md="12">
        <el-skeleton v-if="!devices.length" animated>
          <template #template>
            <el-skeleton-item variant="rect" style="height: 280px; border-radius: var(--radius-lg)" />
          </template>
        </el-skeleton>
        <ChartCard v-else title="生产趋势" icon="TrendCharts" :option="productionChart" :height="'280px'" />
      </el-col>
      <el-col :xs="24" :md="12">
        <el-skeleton v-if="!devices.length" animated>
          <template #template>
            <el-skeleton-item variant="rect" style="height: 280px; border-radius: var(--radius-lg)" />
          </template>
        </el-skeleton>
        <ChartCard v-else title="设备状态分布" icon="PieChart" :option="statusChart" :height="'280px'" />
      </el-col>
    </el-row>

    <div class="device-section">
      <div class="section-header">
        <span class="section-title">
          <el-icon><Monitor /></el-icon>
          设备状态监控
        </span>
        <div class="section-actions">
          <div class="device-summary">
            <span class="summary-item online">
              <span class="summary-dot"></span>
              {{ onlineCount }} 运行中
            </span>
            <span class="summary-item idle">
              <span class="summary-dot"></span>
              {{ idleCount }} 空闲
            </span>
            <span class="summary-item alarm">
              <span class="summary-dot"></span>
              {{ alarmCount }} 告警
            </span>
          </div>
          <router-link class="more-link" to="/device">
            查看全部 <el-icon :size="12"><ArrowRight /></el-icon>
          </router-link>
        </div>
      </div>
      <div class="device-grid">
        <template v-if="loading && !devices.length">
          <div v-for="i in 8" :key="i" class="device-skeleton">
            <el-skeleton animated>
              <template #template>
                <el-skeleton-item variant="rect" style="height: 152px; border-radius: var(--radius-lg)" />
              </template>
            </el-skeleton>
          </div>
        </template>
        <template v-else-if="devices.length">
          <DeviceCard
            v-for="(device, index) in devices.slice(0, 8)"
            :key="device.id || index"
            :name="device.deviceName || device.deviceCode || `设备${index + 1}`"
            :status="device.status"
            :utilization="Math.round((device.speed || 0) / 15)"
            :temperature="device.temperature ?? 0"
            :power="Math.round((device.speed || 0) * 0.02 + 5)"
            :delay="index * 0.05"
          />
        </template>
        <div v-else class="empty-state">
          <el-icon :size="40"><Monitor /></el-icon>
          <span>暂无设备数据</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { getDeviceStatus } from '@/api/dashboard'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { useChartTheme } from '@/composables/useChartTheme'
import { wsService } from '@/utils/websocket'
import StatCard from '@/components/common/StatCard.vue'
import DeviceCard from '@/components/common/DeviceCard.vue'
import ChartCard from '@/components/common/ChartCard.vue'
import { Refresh, Monitor, ArrowRight } from '@element-plus/icons-vue'

const themeStore = useThemeStore()
const chartTheme = useChartTheme()
const userStore = useUserStore()

const loading = ref(false)
const error = ref('')
const currentTime = ref('')
const currentDate = ref('')
const devices = ref<any[]>([])

interface Stat {
  label: string
  value: number
  icon: string
  theme: 'primary' | 'success' | 'warning' | 'info'
}

const stats = computed<Stat[]>(() => [
  { label: '设备总数', value: devices.value.length, icon: 'Monitor', theme: 'primary' },
  { label: '运行中', value: onlineCount.value, icon: 'CircleCheck', theme: 'success' },
  { label: '空闲', value: idleCount.value, icon: 'VideoPause', theme: 'info' },
  { label: '告警', value: alarmCount.value, icon: 'Warning', theme: 'warning' }
])

const productionChart = ref({})
const statusChart = ref({})

const onlineCount = computed(() => devices.value.filter(d => d.status === 'ONLINE').length)
const idleCount = computed(() => devices.value.filter(d => d.status === 'OFFLINE').length)
const alarmCount = computed(() => devices.value.filter(d => d.status === 'ALARM').length)

const statusMap: Record<string, string> = {
  ONLINE: '运行中', OFFLINE: '空闲', ALARM: '告警', MAINTENANCE: '维护中'
}
const chartColors: Record<string, string> = {
  '运行中': '#10b981', '空闲': '#06b6d4', '告警': '#ef4444', '维护中': '#f59e0b'
}

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  currentDate.value = now.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
}

const getChartTheme = () => {
  const t = chartTheme.value
  const bgColor = t.isDark ? 'rgba(20,20,35,0.9)' : 'rgba(255,255,255,0.9)'
  return { ...t, bgColor, borderColor: t.lineColor }
}

const updateCharts = () => {
  const { bgColor, borderColor, textColor, lineColor, labelColor, splitLineColor } = getChartTheme()
  const names = devices.value.map(d => d.deviceCode || d.deviceName || '设备')
  const speeds = devices.value.map(d => d.speed || 0)

  productionChart.value = {
    tooltip: { trigger: 'axis', backgroundColor: bgColor, borderColor, textStyle: { color: textColor } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: names, axisLine: { lineStyle: { color: lineColor } }, axisLabel: { color: labelColor } },
    yAxis: { type: 'value', axisLine: { lineStyle: { color: lineColor } }, axisLabel: { color: labelColor }, splitLine: { lineStyle: { color: splitLineColor } } },
    series: [{
      data: speeds, type: 'line', smooth: true, symbol: 'circle', symbolSize: 8,
      lineStyle: { color: '#6366f1', width: 3 },
      itemStyle: { color: '#6366f1' },
      areaStyle: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(99,102,241,0.4)' }, { offset: 1, color: 'rgba(99,102,241,0)' }] }
    }]
  }

  const counts: Record<string, number> = {}
  devices.value.forEach(d => {
    const name = statusMap[d.status] || '空闲'
    counts[name] = (counts[name] || 0) + 1
  })

  statusChart.value = {
    tooltip: { trigger: 'item', backgroundColor: bgColor, borderColor, textStyle: { color: textColor } },
    series: [{
      type: 'pie', radius: ['45%', '70%'], center: ['50%', '50%'],
      data: Object.entries(counts).filter(([, v]) => v > 0).map(([k, v]) => ({ name: k, value: v, itemStyle: { color: chartColors[k] } })),
      label: { show: true, color: textColor, fontSize: 13 },
      emphasis: { itemStyle: { shadowBlur: 20, shadowColor: 'rgba(0,0,0,0.5)' } }
    }]
  }
}

const refresh = async () => {
  loading.value = true
  error.value = ''
  try {
    const res = await getDeviceStatus()
    const raw = res?.data || res
    devices.value = Array.isArray(raw) ? raw : []
    updateCharts()
  } catch (e: any) {
    error.value = e?.message || '无法连接到后端服务'
    console.error(e)
  } finally {
    loading.value = false
  }
}

let timeInterval: number
let wsUnsubscribe: (() => void) | null = null

onMounted(() => {
  updateTime()
  timeInterval = setInterval(updateTime, 1000)

  wsService.connect()
  wsUnsubscribe = wsService.subscribe((data) => {
    if (data.devices) {
      devices.value = data.devices
      updateCharts()
    }
  })

  refresh()
})

onUnmounted(() => {
  clearInterval(timeInterval)
  wsUnsubscribe?.()
})

watch(() => themeStore.isDark, () => {
  if (devices.value.length) updateCharts()
})
</script>

<style scoped>
.dashboard { padding: 0; }

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.welcome-section { animation: fadeIn 0.5s ease; }
.welcome-title {
  color: var(--text-primary);
  font-size: 26px;
  font-weight: 600;
  margin-bottom: 4px;
}
.username {
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.welcome-subtitle {
  color: var(--text-muted);
  font-size: 14px;
}

.refresh-btn {
  width: 42px;
  height: 42px;
  background: var(--bg-card) !important;
  border: 1px solid var(--border-light) !important;
  color: var(--text-secondary) !important;
  transition: all var(--transition-normal);
  animation: fadeIn 0.5s ease;
}
.refresh-btn:hover { 
  transform: rotate(180deg);
  border-color: var(--accent) !important;
  color: var(--accent) !important;
}

:deep(.mb-4) { margin-bottom: 16px; }

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.charts-row { margin-bottom: 24px; }

.device-section {
  margin-top: 8px;
  animation: fadeIn 0.6s ease 0.2s both;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 600;
}

.device-summary {
  display: flex;
  gap: 20px;
}

.section-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.more-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-muted);
  text-decoration: none;
  transition: color var(--transition-normal);
}

.more-link:hover { color: var(--accent); }

.stat-skeleton,
.device-skeleton { width: 100%; }

.summary-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}

.summary-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.summary-item.online .summary-dot { background: var(--success); }
.summary-item.idle .summary-dot { background: var(--info); }
.summary-item.alarm .summary-dot { background: var(--danger); }

.device-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

@media (max-width: 1200px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .device-grid { grid-template-columns: repeat(2, 1fr); }
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
