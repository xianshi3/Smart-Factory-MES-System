<template>
  <div class="dashboard">
    <!-- ===== 欢迎栏 ===== -->
    <div class="dashboard-header">
      <div class="welcome-section">
        <h1 class="welcome-title">
          欢迎回来, <span class="username">{{ userStore.userInfo?.username || '管理员' }}</span>
        </h1>
        <p class="welcome-subtitle">{{ currentDate }} · {{ currentTime }}</p>
      </div>
      <div class="header-actions">
        <el-button class="ai-glow-btn" :loading="aiLoading" @click="generateInsight(true)">
          <el-icon><MagicStick /></el-icon>
          {{ aiLoading ? 'AI 分析中' : 'AI 洞察' }}
        </el-button>
        <el-button circle class="refresh-btn" :loading="loading" @click="refresh">
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>
    </div>

    <el-alert v-if="error" :title="error" type="error" show-icon closable class="mb-4" @close="error = ''" />

    <!-- ===== 系统健康监控条 ===== -->
    <div class="sys-health-panel">
      <div class="sys-health-head">
        <span class="sys-health-title">
          <el-icon><Cpu /></el-icon>
          系统运行状态
        </span>
        <span class="sys-health-meta">
          <span class="health-summary">
            <i class="hd-dot ok"></i>{{ onlineServices }} / {{ services.length }} 在线
          </span>
          <span class="health-summary avg-latency">
            <el-icon><Timer /></el-icon>{{ avgLatency }}ms
          </span>
          <span class="health-auto">自动刷新 15s</span>
        </span>
      </div>
      <div class="sys-health-grid">
        <div
          v-for="svc in services"
          :key="svc.key"
          class="svc-chip"
          :class="`svc-${svc.status}`"
          :title="`${svc.name} · ${svc.latency >= 0 ? svc.latency + 'ms' : svc.note}`"
        >
          <span class="svc-dot"></span>
          <span class="svc-name">{{ svc.name }}</span>
          <span class="svc-port">{{ svc.port }}</span>
          <span class="svc-latency">{{ svc.latency >= 0 ? svc.latency + 'ms' : '--' }}</span>
        </div>
      </div>
    </div>

    <!-- ===== 统计卡片（数字滚动动画） ===== -->
    <div class="stats-grid">
      <template v-if="loading && !devices.length">
        <div v-for="i in 4" :key="i" class="stat-skeleton">
          <el-skeleton animated>
            <template #template>
              <el-skeleton-item variant="rect" style="height: 110px; border-radius: var(--radius-lg)" />
            </template>
          </el-skeleton>
        </div>
      </template>
      <template v-else>
        <div
          v-for="(stat, index) in stats"
          :key="stat.label"
          class="stat-card"
          :class="`stat-theme-${stat.theme}`"
          :style="{ animationDelay: `${index * 0.08}s` }"
        >
          <div class="stat-glow"></div>
          <div class="stat-icon">
            <el-icon :size="22"><component :is="stat.iconComp" /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ displayValue(index) }}</div>
            <div class="stat-label">{{ stat.label }}</div>
          </div>
          <div class="stat-ring">
            <svg viewBox="0 0 40 40">
              <circle class="ring-bg" cx="20" cy="20" r="16" />
              <circle
                class="ring-fg"
                cx="20" cy="20" r="16"
                :stroke-dasharray="`${ringPct(index) * 100.53} 100.53`"
                :class="`ring-${stat.theme}`"
              />
            </svg>
          </div>
        </div>
      </template>
    </div>

    <!-- ===== AI 智能洞察面板 ===== -->
    <div class="ai-insight-panel" :class="{ 'is-loading': aiLoading, 'has-content': !!aiContent }">
      <div class="ai-insight-glow"></div>
      <div class="ai-insight-head">
        <div class="ai-insight-brand">
          <span class="ai-orbit"><el-icon :size="16"><MagicStick /></el-icon></span>
          <span class="ai-insight-title">AI 生产智能洞察</span>
          <span v-if="aiOnline" class="ai-live-badge"><i></i> LIVE</span>
          <span v-else class="ai-off-badge">AI 服务未连接</span>
        </div>
        <div class="ai-insight-actions">
          <el-switch v-model="autoInsight" size="small" inline-prompt active-text="自动" />
          <el-button text size="small" class="ai-regen" :disabled="aiLoading" @click="generateInsight(true)">
            <el-icon><Refresh /></el-icon>
            重新生成
          </el-button>
        </div>
      </div>
      <div class="ai-insight-body">
        <div v-if="aiLoading" class="ai-thinking">
          <div class="ai-thinking-dots"><i></i><i></i><i></i></div>
          <span>AI 正在分析 {{ services.filter(s => s.status === 'up').length }} 个在线服务的实时数据…</span>
        </div>
        <div v-else-if="aiContent" class="ai-insight-text">
          <span v-html="renderInsight"></span><span v-if="typing" class="type-cursor">▍</span>
        </div>
        <div v-else class="ai-insight-empty">
          <el-icon :size="18"><MagicStick /></el-icon>
          点击「AI 洞察」让 AI 分析当前生产与系统状态
        </div>
      </div>
    </div>

    <!-- ===== 3D 工厂总览（孪生 + 趋势 + 状态分布三合一） ===== -->
    <FactoryTwin :devices="devices" :trend-option="productionChart" :status-option="statusChart" />

    <!-- ===== 设备列表 ===== -->
    <div class="device-section">
      <div class="section-header">
        <span class="section-title">
          <el-icon><Monitor /></el-icon>
          设备状态监控
        </span>
        <div class="section-actions">
          <div class="device-summary">
            <span class="summary-item online"><span class="summary-dot"></span>{{ onlineCount }} 运行中</span>
            <span class="summary-item idle"><span class="summary-dot"></span>{{ idleCount }} 空闲</span>
            <span class="summary-item alarm"><span class="summary-dot"></span>{{ alarmCount }} 告警</span>
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
import { llmChat } from '@/api/services'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { useChartTheme } from '@/composables/useChartTheme'
import { wsService } from '@/utils/websocket'
import DeviceCard from '@/components/common/DeviceCard.vue'
import FactoryTwin from '@/components/dashboard/FactoryTwin.vue'
import { Refresh, Monitor, ArrowRight, MagicStick, Cpu, Timer, CircleCheck, VideoPause, Warning } from '@element-plus/icons-vue'
import { mdToHtml } from '@/utils/markdown'

const themeStore = useThemeStore()
const chartTheme = useChartTheme()
const userStore = useUserStore()

const loading = ref(false)
const error = ref('')
const currentTime = ref('')
const currentDate = ref('')
const devices = ref<any[]>([])

/* ===== 统计与图表 ===== */
const onlineCount = computed(() => devices.value.filter(d => d.status === 'ONLINE').length)
const idleCount = computed(() => devices.value.filter(d => d.status === 'OFFLINE').length)
const alarmCount = computed(() => devices.value.filter(d => d.status === 'ALARM').length)

/* ===== 数字滚动动画 ===== */
const animatedValues = ref<number[]>([0, 0, 0, 0])
const targetValues = computed<number[]>(() => [
  devices.value.length, onlineCount.value, idleCount.value, alarmCount.value,
])
let countTimer: number | null = null

const displayValue = (i: number) => Math.round(animatedValues.value[i])

/** 数字滚动动画：从当前显示值平滑过渡到目标值（setInterval 驱动，后台节流也不卡死） */
const runCountUp = () => {
  if (countTimer) clearInterval(countTimer)
  const start = [...animatedValues.value]
  const end = targetValues.value
  const t0 = performance.now()
  const dur = 500
  countTimer = window.setInterval(() => {
    const p = Math.min(1, (performance.now() - t0) / dur)
    const ease = 1 - Math.pow(1 - p, 3)
    animatedValues.value = start.map((s, i) => s + (end[i] - s) * ease)
    if (p >= 1) {
      animatedValues.value = [...end]
      if (countTimer) clearInterval(countTimer)
      countTimer = null
    }
  }, 16)
}

const ringPct = (i: number) => {
  const total = Math.max(1, devices.value.length)
  return Math.min(1, targetValues.value[i] / total)
}

/* ===== 系统健康检查 ===== */
interface SvcStatus {
  key: string
  name: string
  port: string
  url: string
  status: 'up' | 'down' | 'unknown'
  latency: number
  note: string
}

const services = ref<SvcStatus[]>([
  { key: 'gateway', name: 'API 网关', port: '9090', url: '/api/actuator/health', status: 'unknown', latency: -1, note: '检测中' },
  { key: 'auth', name: '认证服务', port: '8081', url: '/auth/actuator/health', status: 'unknown', latency: -1, note: '检测中' },
  { key: 'workorder', name: '工单服务', port: '8082', url: '/workorder/actuator/health', status: 'unknown', latency: -1, note: '检测中' },
  { key: 'process', name: '工艺服务', port: '8083', url: '/process/actuator/health', status: 'unknown', latency: -1, note: '检测中' },
  { key: 'quality', name: '质量服务', port: '8084', url: '/quality/actuator/health', status: 'unknown', latency: -1, note: '检测中' },
  { key: 'dashboard', name: '看板服务', port: '8085', url: '/dashboard/actuator/health', status: 'unknown', latency: -1, note: '检测中' },
  { key: 'ai', name: 'AI 服务', port: '8087', url: '/ai/api/v1/health', status: 'unknown', latency: -1, note: '检测中' },
])

const onlineServices = computed(() => services.value.filter(s => s.status === 'up').length)
const avgLatency = computed(() => {
  const up = services.value.filter(s => s.latency >= 0)
  if (!up.length) return '--'
  return Math.round(up.reduce((a, b) => a + b.latency, 0) / up.length)
})

const probeService = async (svc: SvcStatus) => {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), 4000)
  const t0 = performance.now()
  try {
    const resp = await fetch(svc.url, { signal: controller.signal, cache: 'no-store' })
    const ms = Math.round(performance.now() - t0)
    let body: any = null
    try { body = await resp.json() } catch { /* 非 JSON */ }
    // 能连通即视为在线（健康端点 404 仅代表 actuator 缺失，不代表服务宕机）
    svc.status = 'up'
    svc.latency = ms
    svc.note = resp.ok ? `${ms}ms` : `HTTP ${resp.status}`
  } catch {
    svc.status = 'down'
    svc.latency = -1
    svc.note = '不可达'
  } finally {
    clearTimeout(timer)
  }
}

const probeAllServices = () => {
  services.value.forEach(probeService)
}

/* ===== AI 智能洞察 ===== */
const aiContent = ref('')
const aiLoading = ref(false)
const aiOnline = ref(false)
const autoInsight = ref(true)
const typing = ref(false)
const typedLen = ref(0)
let typeTimer: number | null = null
let insightTimer: number | null = null

const renderedContent = computed(() => mdToHtml(aiContent.value || ''))
const renderInsight = computed(() => {
  // 打字机：按字符截断渲染（不破坏已渲染 markdown 的稳定性，超过已打字数后整体渲染）
  if (typedLen.value < aiContent.value.length) {
    const slice = aiContent.value.slice(0, typedLen.value)
    return mdToHtml(slice)
  }
  return renderedContent.value
})

const startTyping = (text: string) => {
  typedLen.value = 0
  typing.value = true
  if (typeTimer) clearInterval(typeTimer)
  typeTimer = window.setInterval(() => {
    typedLen.value += 3
    if (typedLen.value >= text.length) {
      typedLen.value = text.length
      typing.value = false
      if (typeTimer) clearInterval(typeTimer)
      typeTimer = null
    }
  }, 24)
}

const generateInsight = async (manual = false) => {
  if (aiLoading.value) return
  aiLoading.value = true
  try {
    const svcSummary = services.value.map(s => `${s.name}(${s.status === 'up' ? '在线' : s.status === 'down' ? '离线' : '未知'}${s.latency >= 0 ? '/' + s.latency + 'ms' : ''})`).join('、')
    const ctx = {
      page: '工作台',
      services: svcSummary,
      counts: {
        total: devices.value.length,
        online: onlineCount.value,
        idle: idleCount.value,
        alarm: alarmCount.value,
      },
      devices: devices.value.slice(0, 8).map(d => ({
        code: d.deviceCode,
        status: d.status,
        temperature: d.temperature,
        speed: d.speed,
      })),
    }
    const res = await llmChat({
      message: '你是智能工厂的生产总监 AI。请基于上方数据生成 3 条简短的生产洞察（每条不超过 45 字），用「洞察一 / 洞察二 / 洞察三」开头，关注：设备健康风险、产能利用瓶颈、告警处置优先级。直接输出结论，不要客套。',
      context: ctx,
      history: [],
    })
    if (res && res.success && res.content) {
      aiContent.value = res.content
      aiOnline.value = true
      startTyping(res.content)
    } else {
      aiOnline.value = false
      aiContent.value = ''
    }
  } catch (e: any) {
    aiOnline.value = false
    aiContent.value = ''
    if (manual) {
      error.value = 'AI 洞察生成失败：' + (e?.message || 'AI 服务未连接')
    }
  } finally {
    aiLoading.value = false
  }
}

/* ===== 统计与图表 ===== */
interface Stat {
  label: string
  value: number
  iconComp: any
  theme: 'primary' | 'success' | 'warning' | 'info'
}

const stats = computed<Stat[]>(() => [
  { label: '设备总数', value: devices.value.length, iconComp: Monitor, theme: 'primary' },
  { label: '运行中', value: onlineCount.value, iconComp: CircleCheck, theme: 'success' },
  { label: '空闲', value: idleCount.value, iconComp: VideoPause, theme: 'info' },
  { label: '告警', value: alarmCount.value, iconComp: Warning, theme: 'warning' },
])

const productionChart = ref({})
const statusChart = ref({})

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
    runCountUp()
  } catch (e: any) {
    error.value = e?.message || '无法连接到后端服务'
    console.error(e)
  } finally {
    loading.value = false
  }
}

/* ===== 生命周期 ===== */
let timeInterval: number
let healthInterval: number
let wsUnsubscribe: (() => void) | null = null

onMounted(() => {
  updateTime()
  timeInterval = setInterval(updateTime, 1000)
  healthInterval = setInterval(probeAllServices, 15000)

  wsService.connect()
  wsUnsubscribe = wsService.subscribe((data) => {
    if (data.devices) {
      devices.value = data.devices
      updateCharts()
      runCountUp()
    }
  })

  refresh()
  probeAllServices()

  // 首次加载后自动生成一次 AI 洞察
  insightTimer = window.setTimeout(() => {
    if (autoInsight.value) generateInsight()
  }, 6000)
})

onUnmounted(() => {
  clearInterval(timeInterval)
  clearInterval(healthInterval)
  if (insightTimer) clearTimeout(insightTimer)
  if (typeTimer) clearInterval(typeTimer)
  if (countTimer) clearInterval(countTimer)
  wsUnsubscribe?.()
})

watch(() => themeStore.isDark, () => {
  if (devices.value.length) updateCharts()
})
</script>

<style scoped>
.dashboard { padding: 0; }

/* ===== 欢迎栏 ===== */
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  position: relative;
}
.welcome-section { animation: fadeIn 0.5s ease; position: relative; z-index: 1; }
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
.welcome-subtitle { color: var(--text-muted); font-size: 14px; }
.header-actions { display: flex; gap: 10px; align-items: center; }

.ai-glow-btn {
  background: var(--gradient-primary) !important;
  border: none !important;
  color: #fff !important;
  font-weight: 600;
  box-shadow: 0 0 18px rgba(99, 102, 241, 0.35);
  transition: box-shadow var(--transition-normal), transform var(--transition-normal);
}
.ai-glow-btn:hover {
  box-shadow: 0 0 28px rgba(139, 92, 246, 0.55);
  transform: translateY(-1px);
}

.refresh-btn {
  width: 42px; height: 42px;
  background: var(--bg-card) !important;
  border: 1px solid var(--border-light) !important;
  color: var(--text-secondary) !important;
  transition: all var(--transition-normal);
}
.refresh-btn:hover {
  transform: rotate(180deg);
  border-color: var(--accent) !important;
  color: var(--accent) !important;
}

/* ===== 系统健康条 ===== */
.sys-health-panel {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 14px 18px;
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;
  animation: fadeIn 0.5s ease 0.05s both;
}
.sys-health-panel::before {
  content: '';
  position: absolute;
  inset: 0 0 auto 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(99, 102, 241, 0.6), rgba(34, 211, 238, 0.6), transparent);
}
.sys-health-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.sys-health-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}
.sys-health-title .el-icon { color: var(--accent); }
.sys-health-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 12px;
  color: var(--text-muted);
}
.health-summary { display: inline-flex; align-items: center; gap: 6px; }
.hd-dot { width: 7px; height: 7px; border-radius: 50%; background: var(--success); box-shadow: 0 0 8px var(--success); }
.avg-latency { display: inline-flex; align-items: center; gap: 4px; }
.health-auto { opacity: 0.7; }

.sys-health-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 10px;
}
.svc-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 12px;
  border-radius: 10px;
  border: 1px solid var(--border-light);
  background: var(--bg-hover);
  transition: all var(--transition-fast);
  position: relative;
}
.svc-chip:hover { transform: translateY(-1px); border-color: var(--accent); }
.svc-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.svc-up .svc-dot {
  background: var(--success);
  box-shadow: 0 0 8px var(--success);
  animation: pulse-green 2s ease-in-out infinite;
}
.svc-down .svc-dot {
  background: var(--danger);
  box-shadow: 0 0 8px var(--danger);
  animation: pulse-red 1.2s ease-in-out infinite;
}
.svc-unknown .svc-dot { background: var(--warning); }
.svc-name { font-size: 13px; font-weight: 500; color: var(--text-primary); flex: 1; }
.svc-port { font-size: 11px; color: var(--text-muted); }
.svc-latency { font-size: 11px; font-family: Consolas, monospace; }
.svc-up .svc-latency { color: var(--success); }
.svc-down .svc-latency { color: var(--danger); }
.svc-unknown .svc-latency { color: var(--text-muted); }

@keyframes pulse-green {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
@keyframes pulse-red {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

/* ===== 统计卡片 ===== */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.stat-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  overflow: hidden;
  animation: fadeInUp 0.5s ease both;
  transition: transform var(--transition-normal), border-color var(--transition-normal), box-shadow var(--transition-normal);
}
.stat-card:hover {
  transform: translateY(-3px);
  border-color: var(--accent);
  box-shadow: var(--shadow-lg);
}
.stat-glow {
  position: absolute;
  top: -40%;
  right: -20%;
  width: 140px; height: 140px;
  border-radius: 50%;
  filter: blur(40px);
  opacity: 0.22;
  pointer-events: none;
  transition: opacity var(--transition-normal);
}
.stat-card:hover .stat-glow { opacity: 0.45; }
.stat-theme-primary .stat-glow { background: #6366f1; }
.stat-theme-success .stat-glow { background: #10b981; }
.stat-theme-warning .stat-glow { background: #f59e0b; }
.stat-theme-info .stat-glow { background: #06b6d4; }

.stat-icon {
  width: 46px; height: 46px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 12px;
  flex-shrink: 0;
}
.stat-theme-primary .stat-icon { background: var(--accent-light); color: var(--accent); }
.stat-theme-success .stat-icon { background: var(--success-light); color: var(--success); }
.stat-theme-warning .stat-icon { background: var(--warning-light); color: var(--warning); }
.stat-theme-info .stat-icon { background: var(--info-light); color: var(--info); }

.stat-content { flex: 1; min-width: 0; }
.stat-value {
  font-size: 30px;
  font-weight: 700;
  line-height: 1.1;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.stat-label { font-size: 13px; color: var(--text-muted); margin-top: 2px; }

.stat-ring { width: 44px; height: 44px; flex-shrink: 0; }
.stat-ring svg { width: 100%; height: 100%; transform: rotate(-90deg); }
.ring-bg { fill: none; stroke: var(--border-light); stroke-width: 3; }
.ring-fg { fill: none; stroke-width: 3; stroke-linecap: round; transition: stroke-dasharray 0.6s ease; }
.ring-primary { stroke: #6366f1; }
.ring-success { stroke: #10b981; }
.ring-warning { stroke: #f59e0b; }
.ring-info { stroke: #06b6d4; }

.stat-skeleton { width: 100%; }

/* ===== AI 洞察面板 ===== */
.ai-insight-panel {
  position: relative;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 18px 22px;
  margin-bottom: 20px;
  overflow: hidden;
  animation: fadeIn 0.6s ease 0.1s both;
}
.ai-insight-panel.is-loading { border-color: rgba(99, 102, 241, 0.4); }
.ai-insight-panel.has-content { border-color: rgba(139, 92, 246, 0.35); }
.ai-insight-glow {
  position: absolute;
  top: -60px; left: -60px;
  width: 220px; height: 220px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.18), transparent 70%);
  filter: blur(30px);
  pointer-events: none;
}
.ai-insight-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  position: relative;
}
.ai-insight-brand { display: flex; align-items: center; gap: 10px; }
.ai-orbit {
  width: 30px; height: 30px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 50%;
  background: var(--gradient-primary);
  color: #fff;
  box-shadow: 0 0 14px rgba(99, 102, 241, 0.5);
  animation: orbit-glow 2.4s ease-in-out infinite;
}
.ai-insight-title { font-size: 15px; font-weight: 600; color: var(--text-primary); }
.ai-live-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 1px;
  color: var(--success);
  padding: 2px 8px;
  border: 1px solid var(--success);
  border-radius: 20px;
}
.ai-live-badge i {
  width: 6px; height: 6px; border-radius: 50%;
  background: var(--success);
  animation: pulse-green 1.6s ease-in-out infinite;
}
.ai-off-badge {
  font-size: 11px;
  color: var(--danger);
  padding: 2px 8px;
  border: 1px solid var(--danger);
  border-radius: 20px;
}
.ai-insight-actions { display: flex; align-items: center; gap: 12px; }
.ai-regen { color: var(--accent); font-size: 12px; }

.ai-insight-body { position: relative; min-height: 52px; }
.ai-thinking {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--text-secondary);
  font-size: 13px;
}
.ai-thinking-dots { display: flex; gap: 5px; }
.ai-thinking-dots i {
  width: 7px; height: 7px;
  border-radius: 50%;
  background: var(--accent);
  animation: bounce-dot 1.2s ease-in-out infinite;
}
.ai-thinking-dots i:nth-child(2) { animation-delay: 0.15s; }
.ai-thinking-dots i:nth-child(3) { animation-delay: 0.3s; }

.ai-insight-text {
  font-size: 14px;
  line-height: 1.9;
  color: var(--text-secondary);
}
.ai-insight-text :deep(strong) { color: var(--text-primary); }
.ai-insight-text :deep(ol), .ai-insight-text :deep(ul) { padding-left: 22px; margin: 4px 0; }
.type-cursor {
  color: var(--accent);
  animation: blink 0.9s step-end infinite;
}
.ai-insight-empty {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-muted);
  font-size: 13px;
}
.ai-insight-empty .el-icon { color: var(--accent); }

@keyframes orbit-glow {
  0%, 100% { box-shadow: 0 0 14px rgba(99, 102, 241, 0.5); }
  50% { box-shadow: 0 0 26px rgba(139, 92, 246, 0.75); }
}
@keyframes bounce-dot {
  0%, 100% { transform: translateY(0); opacity: 0.5; }
  50% { transform: translateY(-5px); opacity: 1; }
}
@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* ===== 图表与设备 ===== */
.device-section { margin-top: 8px; animation: fadeIn 0.6s ease 0.2s both; }
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.section-title {
  display: flex; align-items: center; gap: 8px;
  color: var(--text-primary);
  font-size: 18px; font-weight: 600;
}
.device-summary { display: flex; gap: 20px; }
.section-actions { display: flex; align-items: center; gap: 16px; }
.more-link {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 13px; color: var(--text-muted);
  text-decoration: none;
  transition: color var(--transition-normal);
}
.more-link:hover { color: var(--accent); }

.stat-skeleton, .device-skeleton { width: 100%; }

.summary-item {
  display: flex; align-items: center; gap: 6px;
  font-size: 13px; color: var(--text-secondary);
}
.summary-dot { width: 8px; height: 8px; border-radius: 50%; }
.summary-item.online .summary-dot { background: var(--success); }
.summary-item.idle .summary-dot { background: var(--info); }
.summary-item.alarm .summary-dot { background: var(--danger); }

.device-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.empty-state {
  grid-column: 1 / -1;
  display: flex; flex-direction: column; align-items: center; gap: 10px;
  padding: 40px 0;
  color: var(--text-muted);
}

@media (max-width: 1200px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .device-grid { grid-template-columns: repeat(2, 1fr); }
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(14px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
