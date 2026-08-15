<template>
  <div class="dashboard">
    <!-- ===== 欢迎栏（紧凑单行） ===== -->
    <div class="dashboard-header">
      <div class="welcome-section">
        <h1 class="welcome-title">
          欢迎回来, <span class="username">{{ userStore.userInfo?.username || '管理员' }}</span>
          <span class="welcome-time">{{ currentDate }} · {{ currentTime }}</span>
        </h1>
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

    <!-- ===== 系统健康监控条（单行紧凑） ===== -->
    <div class="sys-health-panel">
      <span class="sys-health-title">
        <el-icon><Cpu /></el-icon>
        系统
      </span>
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
          <span class="svc-latency">{{ svc.latency >= 0 ? svc.latency + 'ms' : '--' }}</span>
        </div>
      </div>
      <span class="sys-health-meta">
        <span class="health-summary"><i class="hd-dot ok"></i>{{ onlineServices }}/{{ services.length }}</span>
        <span class="health-summary avg-latency">
          <el-icon><Timer /></el-icon>{{ avgLatency }}ms
        </span>
      </span>
    </div>

    <!-- ===== 主体：左列(统计+AI洞察) / 右列 3D 孪生 ===== -->
    <div class="main-grid">
      <div class="left-col">
        <!-- 统计卡片 2x2 -->
        <div class="stats-grid">
          <template v-if="loading && !devices.length">
            <div v-for="i in 4" :key="i" class="stat-skeleton">
              <el-skeleton animated>
                <template #template>
                  <el-skeleton-item variant="rect" style="height: 86px; border-radius: var(--radius-lg)" />
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
              :style="{ animationDelay: `${index * 0.06}s` }"
            >
              <div class="stat-glow"></div>
              <div class="stat-icon">
                <el-icon :size="18"><component :is="stat.iconComp" /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-value">{{ displayValue(index) }}</div>
                <div class="stat-label">{{ stat.label }}</div>
              </div>
            </div>
          </template>
        </div>

        <!-- AI 智能洞察（紧凑） -->
        <div class="ai-insight-panel" :class="{ 'is-loading': aiLoading, 'has-content': !!aiContent }">
          <div class="ai-insight-glow"></div>
          <div class="ai-insight-head">
            <div class="ai-insight-brand">
              <span class="ai-orbit"><el-icon :size="14"><MagicStick /></el-icon></span>
              <span class="ai-insight-title">AI 生产智能洞察</span>
              <span v-if="aiOnline" class="ai-live-badge"><i></i> LIVE</span>
              <span v-else class="ai-off-badge">AI 服务未连接</span>
            </div>
            <div class="ai-insight-actions">
              <el-switch v-model="autoInsight" size="small" inline-prompt active-text="自动" />
              <el-button text size="small" class="ai-regen" :disabled="aiLoading" @click="generateInsight(true)">
                <el-icon><Refresh /></el-icon>
              </el-button>
            </div>
          </div>
          <div class="ai-insight-body">
            <div v-if="aiLoading" class="ai-thinking">
              <div class="ai-thinking-dots"><i></i><i></i><i></i></div>
              <span>AI 分析中…</span>
            </div>
            <template v-else-if="aiContent">
              <!-- 打字机阶段 -->
              <div v-if="typing" class="ai-insight-text">
                <span v-html="renderInsight"></span><span class="type-cursor">▍</span>
              </div>
              <!-- 完成阶段：条目卡片 -->
              <div v-else class="insight-cards">
                <div
                  v-for="(item, i) in insightItems"
                  :key="i"
                  class="insight-card"
                  :style="{ animationDelay: `${i * 0.12}s` }"
                >
                  <div class="ic-icon" :class="`ic-${item.theme}`">
                    <el-icon :size="14"><component :is="item.icon" /></el-icon>
                  </div>
                  <div class="ic-body">
                    <div class="ic-title">{{ item.title }}</div>
                    <div class="ic-text">{{ item.text }}</div>
                  </div>
                </div>
              </div>
            </template>
            <div v-else class="ai-insight-empty">
              <el-icon :size="16"><MagicStick /></el-icon>
              点击「AI 洞察」分析当前生产状态
            </div>
          </div>

          <!-- 底部：元信息 + 快捷追问 -->
          <div class="ai-insight-foot">
            <div class="aif-meta">
              <span class="aif-model"><i></i>{{ AI_MODEL }}</span>
              <template v-if="lastInsightAt">
                <span class="aif-sep">·</span>
                <span class="aif-time">{{ lastInsightAt }}</span>
                <span class="aif-sep">·</span>
                <span class="aif-ms">{{ insightMs }}ms</span>
              </template>
            </div>
            <div v-if="!aiLoading" class="aif-quick">
              <span
                v-for="q in followUps"
                :key="q.text"
                class="quick-chip"
                @click="quickAsk(q.text)"
              >
                <el-icon :size="10"><MagicStick /></el-icon>
                {{ q.text }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 3D 工厂总览（孪生 + 趋势 + 状态分布三合一） -->
      <div class="right-col">
        <FactoryTwin :devices="devices" :trend-option="productionChart" :status-option="statusChart" />
      </div>
    </div>

    <!-- ===== 设备状态监控（紧凑横排） ===== -->
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
      <div class="device-strip">
        <template v-if="loading && !devices.length">
          <div v-for="i in 8" :key="i" class="device-skeleton">
            <el-skeleton animated>
              <template #template>
                <el-skeleton-item variant="rect" style="height: 64px; border-radius: var(--radius-md)" />
              </template>
            </el-skeleton>
          </div>
        </template>
        <template v-else-if="devices.length">
          <div
            v-for="(device, index) in devices.slice(0, 10)"
            :key="device.id || index"
            class="mini-device"
            :class="`md-${(device.status || 'offline').toLowerCase()}`"
            :style="{ animationDelay: `${index * 0.04}s` }"
            @click="goDevice(device)"
          >
            <span class="md-dot"></span>
            <div class="md-body">
              <div class="md-name">{{ device.deviceName || device.deviceCode || `设备${index + 1}` }}</div>
              <div class="md-meta">
                <span class="md-temp">{{ Math.round(device.temperature ?? 0) }}°C</span>
                <span class="md-util">{{ Math.round(device.speed || 0) }}rpm</span>
              </div>
            </div>
            <span class="md-status">{{ statusText(device.status) }}</span>
          </div>
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
import { useRouter } from 'vue-router'
import { getDeviceStatus } from '@/api/dashboard'
import { llmChat } from '@/api/services'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { useChartTheme } from '@/composables/useChartTheme'
import { wsService } from '@/utils/websocket'
import FactoryTwin from '@/components/dashboard/FactoryTwin.vue'
import { Refresh, Monitor, ArrowRight, MagicStick, Cpu, Timer, CircleCheck, VideoPause, Warning, WarningFilled, Lightning, AlarmClock, Star } from '@element-plus/icons-vue'
import { mdToHtml } from '@/utils/markdown'

const router = useRouter()
const themeStore = useThemeStore()
const chartTheme = useChartTheme()
const userStore = useUserStore()

const statusText = (s: string) =>
  ({ ONLINE: '运行中', OFFLINE: '空闲', ALARM: '告警', MAINTENANCE: '维护' } as Record<string, string>)[s] || '空闲'

const goDevice = (d: any) => {
  router.push({ path: '/device', query: { code: d.deviceCode || d.deviceName } })
}

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

/* ===== 洞察条目解析（按主题配图标） ===== */
const INSIGHT_STYLE: { match: RegExp; icon: any; theme: string }[] = [
  { match: /健康|风险|温度|过热|故障/, icon: WarningFilled, theme: 'danger' },
  { match: /产能|瓶颈|效率|利用|产量/, icon: Lightning, theme: 'violet' },
  { match: /告警|处置|优先|异常/, icon: AlarmClock, theme: 'warning' },
  { match: /趋势|预测|维护|改进/, icon: Star, theme: 'cyan' },
]

const makeInsightItem = (title: string, text: string) => {
  const t = text.trim().replace(/^\*\*|\*\*$/g, '').replace(/^\s*[/:：]\s*/, '')
  const found = INSIGHT_STYLE.find(x => x.match.test(title + t))
  return {
    title: title.replace(/^\s*[:：]\s*/, ''),
    text: t,
    icon: found?.icon ?? Star,
    theme: found?.theme ?? 'primary',
  }
}

const insightItems = computed(() => {
  const text = aiContent.value || ''
  const items: { title: string; text: string; icon: any; theme: string }[] = []
  const re = /洞察[一二三四五六七八九十]+/g
  let lastIdx = 0
  let lastTitle = '智能洞察'
  let m: RegExpExecArray | null
  while ((m = re.exec(text)) !== null) {
    if (m.index > lastIdx) {
      const seg = text.slice(lastIdx, m.index)
      if (seg.trim()) items.push(makeInsightItem(lastTitle, seg))
    }
    lastTitle = m[0]
    lastIdx = re.lastIndex
  }
  if (lastIdx < text.length) {
    const seg = text.slice(lastIdx)
    if (seg.trim()) items.push(makeInsightItem(lastTitle, seg))
  }
  if (!items.length && text.trim()) {
    items.push(makeInsightItem('智能洞察', text))
  }
  return items
})

/* ===== AI 洞察请求（共享上下文构建 + 耗时记录） ===== */
const lastInsightAt = ref('')
const insightMs = ref(0)
const AI_MODEL = 'glm-4-flash'

const buildInsightContext = () => {
  const svcSummary = services.value.map(s => `${s.name}(${s.status === 'up' ? '在线' : s.status === 'down' ? '离线' : '未知'}${s.latency >= 0 ? '/' + s.latency + 'ms' : ''})`).join('、')
  return {
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
}

const askInsight = async (message: string, manual = false) => {
  if (aiLoading.value) return
  aiLoading.value = true
  const t0 = performance.now()
  try {
    const res = await llmChat({
      message,
      context: buildInsightContext(),
      history: [],
    })
    if (res && res.success && res.content) {
      aiContent.value = res.content
      aiOnline.value = true
      lastInsightAt.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
      insightMs.value = Math.round(performance.now() - t0)
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

const DEFAULT_INSIGHT_PROMPT = '你是智能工厂的生产总监 AI。请基于上方数据生成 3 条简短的生产洞察（每条不超过 45 字），用「洞察一 / 洞察二 / 洞察三」开头，关注：设备健康风险、产能利用瓶颈、告警处置优先级。直接输出结论，不要客套。'

const generateInsight = (manual = false) => askInsight(DEFAULT_INSIGHT_PROMPT, manual)

/** 快捷追问（基于当前实时数据动态生成） */
const followUps = computed(() => {
  const list: { text: string }[] = []
  const alarmDevs = devices.value.filter(d => d.status === 'ALARM').slice(0, 2)
  if (alarmDevs.length) {
    list.push({ text: `深入分析 ${alarmDevs[0].deviceCode}` })
  }
  const hot = devices.value.filter(d => (d.temperature ?? 0) > 80).slice(0, 2)
  if (hot.length) {
    list.push({ text: `${hot[0].deviceCode} 温度过高怎么办` })
  }
  if (!list.length) {
    list.push({ text: '生成本周生产总结' })
    list.push({ text: '分析产线产能瓶颈' })
  }
  return list.slice(0, 2)
})

const quickAsk = (text: string) => {
  askInsight(`请基于当前数据回答：${text}。回答简洁，不超过 80 字。`, true)
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
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
  min-height: 0;
}

/* ===== 欢迎栏（紧凑单行） ===== */
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}
.welcome-section { animation: fadeIn 0.4s ease; min-width: 0; }
.welcome-title {
  display: flex;
  align-items: baseline;
  gap: 12px;
  color: var(--text-primary);
  font-size: 22px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
}
.username {
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.welcome-time {
  font-size: 13px;
  font-weight: 400;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}
.header-actions { display: flex; gap: 10px; align-items: center; flex-shrink: 0; }

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
  width: 40px; height: 40px;
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

/* ===== 系统健康条（单行紧凑） ===== */
.sys-health-panel {
  display: flex;
  align-items: center;
  gap: 14px;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 8px 16px;
  flex-shrink: 0;
  position: relative;
  overflow: hidden;
  animation: fadeIn 0.4s ease 0.03s both;
}
.sys-health-panel::before {
  content: '';
  position: absolute;
  inset: 0 0 auto 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(99, 102, 241, 0.6), rgba(34, 211, 238, 0.6), transparent);
}
.sys-health-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  flex-shrink: 0;
}
.sys-health-title .el-icon { color: var(--accent); }
.sys-health-grid {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
  overflow-x: auto;
  scrollbar-width: none;
}
.sys-health-grid::-webkit-scrollbar { display: none; }
.svc-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  border-radius: 8px;
  border: 1px solid var(--border-light);
  background: var(--bg-hover);
  white-space: nowrap;
  flex-shrink: 0;
  transition: all var(--transition-fast);
}
.svc-chip:hover { border-color: var(--accent); }
.svc-dot { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }
.svc-up .svc-dot { background: var(--success); box-shadow: 0 0 8px var(--success); animation: pulse-green 2s ease-in-out infinite; }
.svc-down .svc-dot { background: var(--danger); box-shadow: 0 0 8px var(--danger); animation: pulse-red 1.2s ease-in-out infinite; }
.svc-unknown .svc-dot { background: var(--warning); }
.svc-name { font-size: 12px; font-weight: 500; color: var(--text-primary); }
.svc-latency { font-size: 10px; font-family: Consolas, monospace; }
.svc-up .svc-latency { color: var(--success); }
.svc-down .svc-latency { color: var(--danger); }
.svc-unknown .svc-latency { color: var(--text-muted); }
.sys-health-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--text-muted);
  flex-shrink: 0;
}
.health-summary { display: inline-flex; align-items: center; gap: 5px; }
.hd-dot { width: 7px; height: 7px; border-radius: 50%; background: var(--success); box-shadow: 0 0 8px var(--success); }
.avg-latency { display: inline-flex; align-items: center; gap: 4px; }

@keyframes pulse-green {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
@keyframes pulse-red {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

/* ===== 主体网格 ===== */
.main-grid {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 12px;
  flex: 1;
  min-height: 0;
}
.left-col {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  overflow-y: auto;
  scrollbar-width: thin;
}
.right-col {
  min-height: 0;
  min-width: 0;
}

/* ===== 统计卡片（2x2 紧凑） ===== */
.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  flex-shrink: 0;
}
.stat-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  overflow: hidden;
  animation: fadeInUp 0.4s ease both;
  transition: transform var(--transition-normal), border-color var(--transition-normal), box-shadow var(--transition-normal);
}
.stat-card:hover {
  transform: translateY(-2px);
  border-color: var(--accent);
  box-shadow: var(--shadow-md);
}
.stat-glow {
  position: absolute;
  top: -40%; right: -25%;
  width: 90px; height: 90px;
  border-radius: 50%;
  filter: blur(30px);
  opacity: 0.2;
  pointer-events: none;
}
.stat-card:hover .stat-glow { opacity: 0.4; }
.stat-theme-primary .stat-glow { background: #6366f1; }
.stat-theme-success .stat-glow { background: #10b981; }
.stat-theme-warning .stat-glow { background: #f59e0b; }
.stat-theme-info .stat-glow { background: #06b6d4; }
.stat-icon {
  width: 36px; height: 36px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 10px;
  flex-shrink: 0;
}
.stat-theme-primary .stat-icon { background: var(--accent-light); color: var(--accent); }
.stat-theme-success .stat-icon { background: var(--success-light); color: var(--success); }
.stat-theme-warning .stat-icon { background: var(--warning-light); color: var(--warning); }
.stat-theme-info .stat-icon { background: var(--info-light); color: var(--info); }
.stat-content { min-width: 0; }
.stat-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.1;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.stat-label { font-size: 11px; color: var(--text-muted); margin-top: 2px; }
.stat-skeleton { width: 100%; }

/* ===== AI 洞察面板（紧凑） ===== */
.ai-insight-panel {
  position: relative;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 12px 14px;
  overflow: hidden;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  animation: fadeIn 0.5s ease 0.08s both;
}
.ai-insight-panel::before {
  content: '';
  position: absolute;
  inset: 0 0 auto 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(139, 92, 246, 0.7), rgba(34, 211, 238, 0.7), transparent);
}
.ai-insight-panel.is-loading { border-color: rgba(99, 102, 241, 0.4); }
.ai-insight-panel.has-content { border-color: rgba(139, 92, 246, 0.35); }
.ai-insight-glow {
  position: absolute;
  top: -50px; left: -50px;
  width: 160px; height: 160px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.16), transparent 70%);
  filter: blur(26px);
  pointer-events: none;
}
.ai-insight-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
  position: relative;
  flex-shrink: 0;
}
.ai-insight-brand { display: flex; align-items: center; gap: 8px; min-width: 0; }
.ai-orbit {
  width: 26px; height: 26px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 50%;
  background: var(--gradient-primary);
  color: #fff;
  box-shadow: 0 0 12px rgba(99, 102, 241, 0.5);
  animation: orbit-glow 2.4s ease-in-out infinite;
  flex-shrink: 0;
}
.ai-insight-title { font-size: 13px; font-weight: 600; color: var(--text-primary); white-space: nowrap; }
.ai-live-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 1px;
  color: var(--success);
  padding: 1px 6px;
  border: 1px solid var(--success);
  border-radius: 20px;
  flex-shrink: 0;
}
.ai-live-badge i {
  width: 5px; height: 5px; border-radius: 50%;
  background: var(--success);
  animation: pulse-green 1.6s ease-in-out infinite;
}
.ai-off-badge {
  font-size: 10px;
  color: var(--danger);
  padding: 1px 6px;
  border: 1px solid var(--danger);
  border-radius: 20px;
  flex-shrink: 0;
}
.ai-insight-actions { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
.ai-regen { color: var(--accent); font-size: 12px; padding: 2px !important; }

.ai-insight-body { position: relative; overflow-y: auto; flex: 1; min-height: 0; }
.ai-thinking {
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
.ai-insight-text {
  font-size: 12.5px;
  line-height: 1.75;
  color: var(--text-secondary);
}
.ai-insight-text :deep(strong) { color: var(--text-primary); }
.ai-insight-text :deep(ol), .ai-insight-text :deep(ul) { padding-left: 18px; margin: 2px 0; }
.type-cursor { color: var(--accent); animation: blink 0.9s step-end infinite; }

/* ===== 洞察条目卡片 ===== */
.insight-cards {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.insight-card {
  display: flex;
  gap: 10px;
  padding: 9px 10px;
  border-radius: var(--radius-md);
  background: var(--bg-hover);
  border: 1px solid var(--border-light);
  animation: insightIn 0.45s ease both;
  transition: all var(--transition-fast);
  position: relative;
  overflow: hidden;
}
.insight-card::before {
  content: '';
  position: absolute;
  left: 0; top: 0; bottom: 0;
  width: 2px;
  opacity: 0.9;
}
.insight-card:hover {
  transform: translateX(3px);
  border-color: var(--accent);
  box-shadow: var(--shadow-sm);
}
.ic-icon {
  width: 30px; height: 30px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 9px;
  flex-shrink: 0;
}
.ic-icon :deep(.el-icon) { font-size: 14px; }
.ic-danger { background: var(--danger-light); color: var(--danger); }
.ic-warning { background: var(--warning-light); color: var(--warning); }
.ic-violet { background: rgba(139, 92, 246, 0.14); color: #8b5cf6; }
.ic-cyan { background: var(--info-light); color: var(--info); }
.ic-primary { background: var(--accent-light); color: var(--accent); }

.insight-card:has(.ic-danger)::before { background: var(--danger); }
.insight-card:has(.ic-warning)::before { background: var(--warning); }
.insight-card:has(.ic-violet)::before { background: #8b5cf6; }
.insight-card:has(.ic-cyan)::before { background: var(--info); }
.insight-card:has(.ic-primary)::before { background: var(--accent); }

.ic-body { flex: 1; min-width: 0; }
.ic-title {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 2px;
}
.ic-text {
  font-size: 11.5px;
  line-height: 1.6;
  color: var(--text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

@keyframes insightIn {
  from { opacity: 0; transform: translateX(-8px); }
  to { opacity: 1; transform: translateX(0); }
}

/* ===== 洞察底部信息栏 ===== */
.ai-insight-foot {
  margin-top: auto;
  padding-top: 8px;
  border-top: 1px dashed var(--border-light);
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex-shrink: 0;
}
.aif-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 10.5px;
  color: var(--text-muted);
}
.aif-model {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--accent);
  font-weight: 600;
}
.aif-model i {
  width: 5px; height: 5px;
  border-radius: 50%;
  background: var(--accent);
  box-shadow: 0 0 6px var(--accent);
}
.aif-sep { opacity: 0.5; }
.aif-ms { font-family: Consolas, monospace; }

.aif-quick {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.quick-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 9px;
  border-radius: 14px;
  font-size: 10.5px;
  color: var(--text-secondary);
  background: var(--bg-hover);
  border: 1px solid var(--border-light);
  cursor: pointer;
  transition: all var(--transition-fast);
  user-select: none;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 150px;
}
.quick-chip .el-icon { color: var(--accent); flex-shrink: 0; }
.quick-chip:hover {
  color: var(--accent);
  border-color: var(--accent);
  background: var(--accent-light);
  transform: translateY(-1px);
}
.ai-insight-empty {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-muted);
  font-size: 12px;
}
.ai-insight-empty .el-icon { color: var(--accent); }

@keyframes orbit-glow {
  0%, 100% { box-shadow: 0 0 12px rgba(99, 102, 241, 0.5); }
  50% { box-shadow: 0 0 22px rgba(139, 92, 246, 0.75); }
}
@keyframes bounce-dot {
  0%, 100% { transform: translateY(0); opacity: 0.5; }
  50% { transform: translateY(-4px); opacity: 1; }
}
@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* ===== 设备状态监控（紧凑横排） ===== */
.device-section {
  flex-shrink: 0;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 10px 16px 12px;
  animation: fadeIn 0.5s ease 0.12s both;
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.section-title {
  display: flex; align-items: center; gap: 7px;
  color: var(--text-primary);
  font-size: 14px; font-weight: 600;
}
.device-summary { display: flex; gap: 16px; }
.section-actions { display: flex; align-items: center; gap: 14px; }
.more-link {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 12px; color: var(--text-muted);
  text-decoration: none;
  transition: color var(--transition-normal);
}
.more-link:hover { color: var(--accent); }
.summary-item {
  display: flex; align-items: center; gap: 5px;
  font-size: 12px; color: var(--text-secondary);
}
.summary-dot { width: 7px; height: 7px; border-radius: 50%; }
.summary-item.online .summary-dot { background: var(--success); }
.summary-item.idle .summary-dot { background: var(--info); }
.summary-item.alarm .summary-dot { background: var(--danger); }

.device-strip {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 8px;
}
.mini-device {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: var(--bg-hover);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
  animation: fadeInUp 0.35s ease both;
  overflow: hidden;
}
.mini-device:hover {
  transform: translateY(-2px);
  border-color: var(--accent);
  box-shadow: var(--shadow-sm);
}
.md-dot {
  width: 9px; height: 9px;
  border-radius: 50%;
  flex-shrink: 0;
}
.md-online .md-dot { background: var(--success); box-shadow: 0 0 7px var(--success); }
.md-offline .md-dot { background: var(--info); }
.md-alarm .md-dot { background: var(--danger); box-shadow: 0 0 7px var(--danger); animation: pulse-red 1.2s ease-in-out infinite; }
.md-maintenance .md-dot { background: var(--warning); }
.md-body { flex: 1; min-width: 0; }
.md-name {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.md-meta {
  display: flex;
  gap: 8px;
  font-size: 10.5px;
  color: var(--text-muted);
  margin-top: 1px;
}
.md-status {
  font-size: 10px;
  color: var(--text-muted);
  flex-shrink: 0;
}
.md-online .md-status { color: var(--success); }
.md-offline .md-status { color: var(--info); }
.md-alarm .md-status { color: var(--danger); font-weight: 600; }
.md-maintenance .md-status { color: var(--warning); }

.device-skeleton { width: 100%; }
.empty-state {
  grid-column: 1 / -1;
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  padding: 20px 0;
  color: var(--text-muted);
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 小屏降级 */
@media (max-width: 1280px) {
  .main-grid { grid-template-columns: 280px 1fr; }
  .device-strip { grid-template-columns: repeat(auto-fill, minmax(130px, 1fr)); }
}
@media (max-width: 1024px) {
  .main-grid { grid-template-columns: 1fr; }
  .right-col { min-height: 420px; }
}
</style>
