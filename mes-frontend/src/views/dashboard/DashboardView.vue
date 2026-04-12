<template>
  <div class="dashboard">
    <div class="dashboard-header">
      <div class="welcome-section">
        <h1 class="welcome-title">
          欢迎回来, <span class="username">{{ userStore.user?.username || '管理员' }}</span>
        </h1>
        <p class="welcome-subtitle">{{ currentDate }} · {{ currentTime }}</p>
      </div>
      <div class="header-actions">
        <el-button circle @click="refresh" class="refresh-btn">
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>
    </div>

    <div class="stats-grid">
      <StatCard
        v-for="(stat, index) in stats"
        :key="stat.label"
        :icon="stat.icon"
        :label="stat.label"
        :value="stat.value"
        :theme="stat.theme"
        :trend="stat.trend"
        :delay="index * 0.1"
      />
    </div>

    <el-row :gutter="20" class="charts-row">
      <el-col :span="12">
        <ChartCard title="生产趋势" icon="TrendCharts" :option="productionChart" :height="'280px'" />
      </el-col>
      <el-col :span="12">
        <ChartCard title="设备状态分布" icon="PieChart" :option="statusChart" :height="'280px'" />
      </el-col>
    </el-row>

    <div class="device-section">
      <div class="section-header">
        <span class="section-title">
          <el-icon><Monitor /></el-icon>
          设备状态监控
        </span>
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
      </div>
      <div class="device-grid">
        <DeviceCard
          v-for="(device, index) in devices.slice(0, 8)"
          :key="device.id || index"
          :name="device.deviceName || device.device_code || `设备${index + 1}`"
          :status="device.status"
          :utilization="Math.round((device.speed || 0) / 15)"
          :temperature="device.temperature || 0"
          :power="Math.round((device.speed || 0) * 0.02 + 5)"
          :delay="index * 0.05"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import VChart from 'vue-echarts'
import { getDeviceStatus } from '@/api/services'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { wsService } from '@/utils/websocket'
import StatCard from '@/components/common/StatCard.vue'
import DeviceCard from '@/components/common/DeviceCard.vue'
import ChartCard from '@/components/common/ChartCard.vue'
import { Refresh, Monitor, TrendCharts, PieChart } from '@element-plus/icons-vue'

const userStore = useUserStore()
const themeStore = useThemeStore()

const currentTime = ref('')
const currentDate = ref('')
const devices = ref<any[]>([])
let timeInterval: number
let wsUnsubscribe: (() => void) | null = null

const stats = ref([
  { label: '设备总数', value: 0, icon: 'Monitor', theme: 'primary' as const, trend: 0 },
  { label: '运行中', value: 0, icon: 'CircleCheck', theme: 'success' as const, trend: 0 },
  { label: '空闲', value: 0, icon: 'VideoPause', theme: 'info' as const, trend: 0 },
  { label: '告警', value: 0, icon: 'Warning', theme: 'warning' as const, trend: 0 }
])

const productionChart = ref({})
const statusChart = ref({})

const onlineCount = computed(() => devices.value.filter(d => d.status === 'ONLINE').length)
const idleCount = computed(() => devices.value.filter(d => d.status === 'OFFLINE').length)
const alarmCount = computed(() => devices.value.filter(d => d.status === 'ALARM').length)

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  currentDate.value = now.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
}

const refresh = async () => {
  try {
    const res = await getDeviceStatus()
    devices.value = res?.data || res || []
    
    stats.value = [
      { label: '设备总数', value: devices.value.length, icon: 'Monitor', theme: 'primary' as const, trend: 0 },
      { label: '运行中', value: onlineCount.value, icon: 'CircleCheck', theme: 'success' as const, trend: 0 },
      { label: '空闲', value: idleCount.value, icon: 'VideoPause', theme: 'info' as const, trend: 0 },
      { label: '告警', value: alarmCount.value, icon: 'Warning', theme: 'warning' as const, trend: 0 }
    ]
    
    updateCharts()
  } catch (e) {
    console.error(e)
  }
}

const updateCharts = () => {
  const isDark = themeStore.isDark
  const textColor = isDark ? '#fff' : '#333'
  const lineColor = isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)'
  const labelColor = isDark ? 'rgba(255,255,255,0.6)' : 'rgba(0,0,0,0.6)'
  const splitLineColor = isDark ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)'
  const bgColor = isDark ? 'rgba(20,20,35,0.9)' : 'rgba(255,255,255,0.9)'
  const borderColor = isDark ? 'rgba(255,255,255,0.1)' : '#ddd'

  const deviceNames = devices.value.map(d => d.device_code || d.deviceName || '设备')
  const speeds = devices.value.map(d => d.speed || 0)

  productionChart.value = {
    tooltip: { trigger: 'axis', backgroundColor: bgColor, borderColor, textStyle: { color: textColor } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: deviceNames, axisLine: { lineStyle: { color: lineColor } }, axisLabel: { color: labelColor } },
    yAxis: { type: 'value', axisLine: { lineStyle: { color: lineColor } }, axisLabel: { color: labelColor }, splitLine: { lineStyle: { color: splitLineColor } } },
    series: [{
      data: speeds,
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: { color: '#6366f1', width: 3 },
      itemStyle: { color: '#6366f1' },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(99,102,241,0.4)' }, { offset: 1, color: 'rgba(99,102,241,0)' }] } }
    }]
  }

  const statusCounts: Record<string, number> = { '运行中': 0, '空闲': 0, '告警': 0, '维护中': 0 }
  const colors: Record<string, string> = { '运行中': '#10b981', '空闲': '#06b6d4', '告警': '#ef4444', '维护中': '#f59e0b' }
  devices.value.forEach(d => {
    const statusMap: Record<string, string> = { ONLINE: '运行中', OFFLINE: '空闲', ALARM: '告警', MAINTENANCE: '维护中' }
    const statusText = statusMap[d.status] || '空闲'
    statusCounts[statusText] = (statusCounts[statusText] || 0) + 1
  })

  statusChart.value = {
    tooltip: { trigger: 'item', backgroundColor: bgColor, borderColor, textStyle: { color: textColor } },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['50%', '50%'],
      data: Object.entries(statusCounts).filter(([, v]) => v > 0).map(([k, v]) => ({ name: k, value: v, itemStyle: { color: colors[k] } })),
      label: { show: true, color: textColor, fontSize: 13 },
      emphasis: { itemStyle: { shadowBlur: 20, shadowColor: 'rgba(0,0,0,0.5)' } }
    }]
  }
}

onMounted(() => {
  updateTime()
  timeInterval = setInterval(updateTime, 1000)
  
  wsService.connect()
  wsUnsubscribe = wsService.subscribe((data) => {
    if (data.devices) {
      devices.value = data.devices
      stats.value = [
        { label: '设备总数', value: devices.value.length, icon: 'Monitor', theme: 'primary' as const, trend: 0 },
        { label: '运行中', value: onlineCount.value, icon: 'CircleCheck', theme: 'success' as const, trend: 0 },
        { label: '空闲', value: idleCount.value, icon: 'VideoPause', theme: 'info' as const, trend: 0 },
        { label: '告警', value: alarmCount.value, icon: 'Warning', theme: 'warning' as const, trend: 0 }
      ]
      updateCharts()
    }
  })
  
  refresh()
})

onUnmounted(() => {
  clearInterval(timeInterval)
  if (wsUnsubscribe) {
    wsUnsubscribe()
  }
  wsService.disconnect()
})

watch(() => themeStore.isDark, () => {
  if (devices.value.length > 0) {
    updateCharts()
  }
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
  transition: all var(--transition-normal);
  animation: fadeIn 0.5s ease;
}
.refresh-btn:hover { transform: rotate(180deg); }

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

html.light .section-title { color: var(--text-primary); }
html.light .welcome-title { color: var(--text-primary); }
html.light .welcome-subtitle { color: var(--text-muted); }

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 1200px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .device-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>