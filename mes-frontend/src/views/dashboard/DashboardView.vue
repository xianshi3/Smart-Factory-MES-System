<template>
  <div class="db-page">
    <!-- Header -->
    <div class="db-header">
      <div class="db-greeting">
        <h1>欢迎回来，<span>{{ userStore.userInfo?.username || '管理员' }}</span></h1>
        <p>{{ currentDate }} {{ currentTime }}</p>
      </div>
      <div class="db-header-right">
        <span class="db-uptime">系统运行中</span>
        <span class="db-dot on"></span>
        <el-button circle size="small" @click="refresh"><el-icon><Refresh /></el-icon></el-button>
      </div>
    </div>

    <!-- Stats -->
    <div class="db-stats">
      <div v-for="s in stats" :key="s.label" class="db-stat" :class="'db-stat-'+s.theme">
        <span class="db-stat-icon"><el-icon :size="20"><component :is="s.icon" /></el-icon></span>
        <div class="db-stat-body">
          <span class="db-stat-val">{{ s.value }}</span>
          <span class="db-stat-lbl">{{ s.label }}</span>
        </div>
      </div>
    </div>

    <!-- Charts -->
    <div class="db-charts">
      <div class="db-chart-card">
        <div class="db-chart-head"><el-icon><TrendCharts /></el-icon> 生产趋势</div>
        <v-chart :option="productionChart" autoresize style="height:260px" />
      </div>
      <div class="db-chart-card">
        <div class="db-chart-head"><el-icon><PieChart /></el-icon> 设备状态分布</div>
        <v-chart :option="statusChart" autoresize style="height:260px" />
      </div>
    </div>

    <!-- Devices -->
    <div class="db-devices">
      <div class="db-devices-head">
        <span><el-icon><Monitor /></el-icon> 设备状态监控</span>
        <div class="db-devices-summary">
          <span class="db-ds-item run"><i></i>{{ onlineCount }} 运行</span>
          <span class="db-ds-item idle"><i></i>{{ idleCount }} 空闲</span>
          <span class="db-ds-item alarm"><i></i>{{ alarmCount }} 告警</span>
        </div>
      </div>
      <div class="db-devices-grid">
        <div v-for="(d, i) in devices.slice(0, 8)" :key="d.id || i" class="db-dc" :class="'db-dc-'+(d.status||'OFFLINE').toLowerCase()">
          <div class="db-dc-top">
            <span class="db-dc-name">{{ d.deviceName || d.device_code || '设备'+(i+1) }}</span>
            <span class="db-dc-tag">{{ statusText(d.status) }}</span>
          </div>
          <div class="db-dc-mid">
            <div><em>{{ d.temperature ?? '--' }}</em><label>°C</label></div>
            <div><em>{{ d.speed ?? '--' }}</em><label>rpm</label></div>
            <div><em>{{ d.speed ? Math.round(d.speed*0.02+5) : '--' }}</em><label>kW</label></div>
          </div>
          <div class="db-dc-bar">
            <div :style="{ width: (Math.min((d.speed || 0) / 15, 100)) + '%' }"></div>
          </div>
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
import { Refresh, Monitor, TrendCharts, PieChart, CircleCheck, VideoPause, Warning } from '@element-plus/icons-vue'

const userStore = useUserStore()
const themeStore = useThemeStore()
const chartTheme = useChartTheme()

const currentTime = ref('')
const currentDate = ref('')
const devices = ref<any[]>([])
let timeInterval: number
let wsUnsubscribe: (() => void) | null = null

const stats = ref([
  { label: '设备总数', value: 0, icon: 'Monitor', theme: 'primary' },
  { label: '运行中', value: 0, icon: 'CircleCheck', theme: 'success' },
  { label: '空闲', value: 0, icon: 'VideoPause', theme: 'info' },
  { label: '告警', value: 0, icon: 'Warning', theme: 'warning' },
])

const productionChart = ref({})
const statusChart = ref({})

const onlineCount = computed(() => devices.value.filter(d => d.status === 'ONLINE').length)
const idleCount = computed(() => devices.value.filter(d => d.status === 'OFFLINE').length)
const alarmCount = computed(() => devices.value.filter(d => d.status === 'ALARM').length)

const statusText = (s: string) => ({ ONLINE: '运行中', OFFLINE: '空闲', ALARM: '告警', MAINTENANCE: '维护中' } as any)[s] || '未知'

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  currentDate.value = now.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
}

const refresh = async () => {
  try {
    const res = await getDeviceStatus()
    const raw = res?.data || res
    devices.value = Array.isArray(raw) ? raw : (raw?.value || raw?.records || [])
    updateStats()
    updateCharts()
  } catch { /* ignore */ }
}

const updateStats = () => {
  stats.value = [
    { label: '设备总数', value: devices.value.length, icon: 'Monitor', theme: 'primary' },
    { label: '运行中', value: onlineCount.value, icon: 'CircleCheck', theme: 'success' },
    { label: '空闲', value: idleCount.value, icon: 'VideoPause', theme: 'info' },
    { label: '告警', value: alarmCount.value, icon: 'Warning', theme: 'warning' },
  ]
}

const updateCharts = () => {
  const t = chartTheme.value
  const tc = t.isDark ? '#aaa' : '#666'
  const lc = t.lineColor || '#333'
  const slc = t.splitLineColor || '#111'

  const names = devices.value.map(d => d.device_code || d.deviceName || '设备').slice(0, 12)
  const vals = devices.value.map(d => d.speed || 0).slice(0, 12)

  productionChart.value = {
    tooltip: { trigger: 'axis' },
    grid: { left: 10, right: 10, top: 15, bottom: 10, containLabel: true },
    xAxis: { type: 'category', data: names, axisLabel: { color: tc, fontSize: 10 } },
    yAxis: { type: 'value', axisLabel: { color: tc, fontSize: 10 }, splitLine: { lineStyle: { color: slc } } },
    series: [{
      data: vals, type: 'line', smooth: true, symbol: 'circle', symbolSize: 6,
      lineStyle: { color: '#6366f1', width: 2 },
      itemStyle: { color: '#6366f1' },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(99,102,241,.3)' }, { offset: 1, color: 'rgba(99,102,241,0)' }] } }
    }]
  }

  const sc: any = { '运行中': 0, '空闲': 0, '告警': 0, '维护中': 0 }
  devices.value.forEach(d => { const m: any = { ONLINE: '运行中', OFFLINE: '空闲', ALARM: '告警', MAINTENANCE: '维护中' }; sc[m[d.status] || '空闲']++ })
  statusChart.value = {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: ['50%', '75%'], center: ['50%', '50%'],
      data: Object.entries(sc).filter(([, v]) => v > 0).map(([k, v]) => ({
        name: k, value: v,
        itemStyle: { color: { '运行中': '#34c759', '空闲': '#8e8e93', '告警': '#ff3b30', '维护中': '#ff9500' }[k] }
      })),
      label: { color: tc, fontSize: 11 }
    }]
  }
}

onMounted(() => {
  updateTime()
  timeInterval = setInterval(updateTime, 1000)
  wsService.connect()
  wsUnsubscribe = wsService.subscribe((data: any) => {
    if (data.devices) {
      devices.value = data.devices
      updateStats()
      updateCharts()
    }
  })
  refresh()
})

onUnmounted(() => {
  clearInterval(timeInterval)
  wsUnsubscribe?.()
  wsService.disconnect()
})

watch(() => themeStore.isDark, () => { if (devices.value.length) updateCharts() })
</script>

<style scoped>
.db-page { color: var(--text-primary); font-size: 13px; }

.db-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; }
.db-greeting h1 { font-size: 22px; font-weight: 700; margin: 0 0 4px; color: var(--text-primary); }
.db-greeting h1 span { color: var(--accent); }
.db-greeting p { margin: 0; font-size: 13px; color: var(--text-muted); }
.db-header-right { display: flex; align-items: center; gap: 8px; }
.db-uptime { font-size: 11px; color: var(--text-muted); }
.db-dot { width: 6px; height: 6px; border-radius: 50%; background: var(--success); }
.db-dot.on { box-shadow: 0 0 6px var(--success); }

.db-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 20px; }
.db-stat { display: flex; align-items: center; gap: 12px; padding: 16px 20px; background: var(--bg-card); border: 1px solid var(--border-light); border-radius: 10px; }
.db-stat-icon { width: 42px; height: 42px; display: flex; align-items: center; justify-content: center; border-radius: 8px; flex-shrink: 0; }
.db-stat-primary .db-stat-icon { background: var(--accent-light); color: var(--accent); }
.db-stat-success .db-stat-icon { background: var(--success-light); color: var(--success); }
.db-stat-info .db-stat-icon { background: var(--info-light); color: var(--info); }
.db-stat-warning .db-stat-icon { background: var(--warning-light); color: var(--warning); }
.db-stat-val { display: block; font-size: 22px; font-weight: 800; color: var(--text-primary); line-height: 1.1; }
.db-stat-lbl { font-size: 12px; color: var(--text-muted); }

.db-charts { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-bottom: 20px; }
.db-chart-card { background: var(--bg-card); border: 1px solid var(--border-light); border-radius: 10px; padding: 14px 16px 8px; }
.db-chart-head { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 600; color: var(--text-primary); margin-bottom: 6px; }

.db-devices { }
.db-devices-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; font-size: 14px; font-weight: 600; color: var(--text-primary); }
.db-devices-head span { display: flex; align-items: center; gap: 6px; }
.db-devices-summary { display: flex; gap: 16px; }
.db-ds-item { display: flex; align-items: center; gap: 5px; font-size: 12px; font-weight: 500; color: var(--text-muted); }
.db-ds-item i { width: 7px; height: 7px; border-radius: 50%; display: inline-block; }
.db-ds-item.run i { background: var(--success); }
.db-ds-item.idle i { background: var(--info); }
.db-ds-item.alarm i { background: var(--danger); }

.db-devices-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.db-dc { background: var(--bg-card); border: 1px solid var(--border-light); border-left: 3px solid var(--border-color); border-radius: 8px; padding: 12px 14px; }
.db-dc-online { border-left-color: var(--success); }
.db-dc-offline { border-left-color: var(--info); }
.db-dc-alarm { border-left-color: var(--danger); }
.db-dc-maintenance { border-left-color: var(--warning); }
.db-dc-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.db-dc-name { font-size: 13px; font-weight: 600; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.db-dc-tag { font-size: 10px; padding: 1px 7px; border-radius: 6px; font-weight: 600; }
.db-dc-online .db-dc-tag { background: var(--success-light); color: var(--success); }
.db-dc-offline .db-dc-tag { background: var(--info-light); color: var(--info); }
.db-dc-alarm .db-dc-tag { background: var(--danger-light); color: var(--danger); }
.db-dc-maintenance .db-dc-tag { background: var(--warning-light); color: var(--warning); }
.db-dc-mid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 4px; margin-bottom: 8px; text-align: center; }
.db-dc-mid div { }
.db-dc-mid em { display: block; font-size: 16px; font-weight: 700; color: var(--text-primary); font-style: normal; }
.db-dc-mid label { font-size: 9px; color: var(--text-muted); }
.db-dc-bar { height: 3px; background: var(--border-color); border-radius: 2px; overflow: hidden; }
.db-dc-bar div { height: 100%; background: var(--accent); border-radius: 2px; min-width: 2px; }

@media (max-width: 1200px) { .db-stats, .db-devices-grid { grid-template-columns: repeat(2, 1fr); } .db-charts { grid-template-columns: 1fr; } }
@media (max-width: 600px) { .db-stats, .db-devices-grid { grid-template-columns: 1fr; } }
</style>
