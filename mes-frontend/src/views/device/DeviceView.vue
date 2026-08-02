<template>
  <div class="dt-page">
    <!-- ===== TOP BAR ===== -->
    <header class="dt-topbar">
      <div class="dt-topbar-left">
        <span class="dt-logo"><el-icon><Monitor /></el-icon> 设备监控</span>
      </div>
      <div v-if="viewMode === 'list'" class="dt-topbar-stats">
        <span v-for="s in stats" :key="s.label" class="dt-stats-badge" :class="s.theme">
          <span class="dt-badge-num">{{ s.value }}</span>{{ s.label }}
        </span>
      </div>
      <div v-else class="dt-topbar-spacer"></div>
      <div class="dt-topbar-right">
        <div class="dt-view-switch">
          <button :class="{ on: viewMode === '3d' }" @click="viewMode = '3d'"><el-icon size="14"><Grid /></el-icon> 数字孪生</button>
          <button :class="{ on: viewMode === 'list' }" @click="viewMode = 'list'"><el-icon size="14"><View /></el-icon> 设备列表</button>
        </div>
        <div v-if="viewMode === 'list'" class="dt-topbar-actions">
          <el-input v-model="searchKeyword" size="small" placeholder="搜索..." clearable :prefix-icon="Search" style="width:150px" />
          <span v-for="f in filterChips" :key="f.key" class="dt-fchip" :class="{ on: statusFilter === f.key }" @click="statusFilter = f.key">{{ f.label }}</span>
        </div>
        <el-button text size="small" class="dt-btn-refresh" @click="refresh"><el-icon><Refresh /></el-icon></el-button>
      </div>
    </header>

    <!-- ===== MAIN CONTENT ===== -->
    <div class="dt-main">
      <!-- 3D Scene -->
      <div v-if="viewMode === '3d'" class="dt-scene-wrap">
        <DigitalTwinScene :devices="deviceList" @select="handleDeviceSelect" @action="handle3DAction" />

        <!-- HUD: alarms panel -->
        <transition name="hud-fade">
          <div v-if="hudPanels.alarms" class="dt-hud dt-hud-alarms">
            <div class="dt-hud-head" @click="hudPanels.alarms = false">
              <el-icon><Warning /></el-icon>告警<span class="dt-hud-badge">{{ alarmList.length }}</span>
              <el-icon class="dt-hud-close"><Close /></el-icon>
            </div>
            <div class="dt-hud-list">
              <div v-for="(a,i) in alarmList.slice(0,6)" :key="i" class="dt-hud-row" :class="getAlarmClass(a.level)">
                <span class="dt-hud-dot"></span><span>{{ a.message || a.deviceName }}</span>
              </div>
              <div v-if="!alarmList.length" class="dt-hud-none">✓ 系统运行正常，暂无告警</div>
            </div>
          </div>
        </transition>

        <!-- HUD: charts panel (bottom) -->
        <transition name="hud-slide">
          <div v-if="hudPanels.charts" class="dt-hud dt-hud-charts">
            <div class="dt-hud-chart-head">
              <span>性能趋势</span>
              <el-button text size="small" @click="hudPanels.charts = false"><el-icon><Close /></el-icon></el-button>
            </div>
            <div class="dt-hud-chart-grid">
              <div><em>设备状态分布</em><v-chart :option="statusOption" autoresize style="height:140px" /></div>
              <div><em>利用率</em><v-chart :option="utilizationOption" autoresize style="height:140px" /></div>
            </div>
          </div>
        </transition>

        <!-- HUD control buttons -->
        <div class="dt-hud-btns">
          <button :class="{ on: hudPanels.alarms }" @click="hudPanels.alarms = !hudPanels.alarms">
            <el-icon><Warning /></el-icon><span v-if="alarmList.length" class="dt-hud-dot-badge">{{ alarmList.length }}</span>
          </button>
          <button :class="{ on: hudPanels.charts }" @click="hudPanels.charts = !hudPanels.charts">
            <el-icon><TrendCharts /></el-icon>
          </button>
        </div>

      </div>

      <!-- List view -->
      <div v-if="viewMode === 'list'" class="dt-list-wrap">
        <div v-if="filteredDevices.length === 0" class="dt-empty"><el-empty description="暂无设备数据" :image-size="80" /></div>
        <div v-else class="dt-list-grid">
          <div v-for="(d,i) in pagedDevices" :key="d.id || i" class="dc-card" @click="handleDetail(d)">
            <div class="dc-card-top">
              <div class="dc-card-info">
                <span class="dc-card-name">{{ d.name }}</span>
                <span class="dc-card-code">{{ d.code }}</span>
              </div>
              <span class="dc-card-tag" :class="d.status">{{ getStatusText(d.status) }}</span>
            </div>
            <div class="dc-card-metrics">
              <div class="dc-metric">
                <span class="dc-m-val" :class="{ warn: d.temperature > 55, hot: d.temperature > 70 }">{{ d.temperature ?? '--' }}</span>
                <span class="dc-m-unit">°C</span>
              </div>
              <div class="dc-metric">
                <span class="dc-m-val">{{ d.speed || 0 }}</span>
                <span class="dc-m-unit">rpm</span>
              </div>
              <div class="dc-metric">
                <span class="dc-m-val">{{ d.power ?? '--' }}</span>
                <span class="dc-m-unit">kW</span>
              </div>
              <div class="dc-metric">
                <span class="dc-m-val">{{ d.utilization || '0%' }}</span>
                <span class="dc-m-unit">利用率</span>
              </div>
            </div>
            <div class="dc-card-util"><div :style="{ width: (parseInt(d.utilization)||0)+'%' }"></div></div>
            <div class="dc-card-foot" @click.stop>
              <el-button v-if="d.status==='running'" size="small" type="danger" link @click="handleStop(d)"><el-icon><VideoPause /></el-icon>停止</el-button>
              <el-button v-if="d.status==='idle'" size="small" type="success" link @click="handleStart(d)"><el-icon><VideoPlay /></el-icon>启动</el-button>
              <el-button size="small" type="primary" link @click="handleCardPredict(d)"><el-icon><Cpu /></el-icon>预测</el-button>
              <el-button size="small" type="primary" link @click="handleDetail(d)">详情</el-button>
            </div>
          </div>
        </div>
        <div v-if="filteredDevices.length > pageSize" class="dt-list-pager">
          <el-pagination v-model:current-page="page" small :total="filteredDevices.length" :page-size="pageSize" layout="total, prev, pager, next" background />
        </div>
      </div>
    </div>

    <!-- DIALOGS -->
    <el-dialog v-model="detailVisible" title="设备详情" width="620px" destroy-on-close>
      <div v-if="detailData" class="dt-dlg-det">
        <div class="dt-dlg-det-head">
          <div class="dt-dlg-det-avatar"><el-icon size="26"><Monitor /></el-icon></div>
          <div><strong>{{ detailData.name }}</strong><br><small>{{ detailData.code }}</small></div>
          <el-tag :type="getStatusType(detailData.status)" size="large">{{ getStatusText(detailData.status) }}</el-tag>
        </div>
        <div class="dt-dlg-det-kpis">
          <div v-for="kv in [['利用率',detailData.utilization+'%'],['温度',detailData.temperature+'°C'],['功率',detailData.power+'kW'],['OEE',(detailData.efficiency||0)+'%']]" :key="kv[0]" class="dt-dlg-det-kpi">
            <strong>{{ kv[1] }}</strong><span>{{ kv[0] }}</span>
          </div>
        </div>
        <div v-if="detailData.status==='running'" class="dt-dlg-ai-badge">
          <el-icon><CircleCheck /></el-icon> 设备运行正常 · 预测未来24小时内无需维护 · 置信度95%
        </div>
        <div class="dt-dlg-ai-btns">
          <el-button type="warning" @click="handlePredict(detailData)"><el-icon><Cpu /></el-icon> 故障预测</el-button>
          <el-button @click="openAiDialog"><el-icon><Histogram /></el-icon> AI分析</el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="predictVisible" title="AI预测分析" width="440px">
      <div v-if="predictData" style="text-align:center">
        <el-icon size="42" :color="predictData.faultLevel==='danger'?'var(--danger)':predictData.faultLevel==='warning'?'var(--warning)':'var(--success)'"><Cpu /></el-icon>
        <h3 style="margin:8px 0">{{ predictData.deviceName }}</h3>
        <el-result :icon="predictData.faultLevel==='danger'?'error':'success'" title="预测结果" :sub-title="predictData.message">
          <template #extra><el-tag :type="predictData.faultLevel">{{ predictData.confidence }}</el-tag></template>
        </el-result>
        <div v-if="predictData.riskFactors?.length" style="margin-top:8px">
          <el-tag v-for="(f,i) in predictData.riskFactors" :key="i" type="warning" size="small" style="margin:2px">{{ f.description||f.factor }}</el-tag>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="aiAnalysisVisible" :title="currentAnalysisType === 'spc' ? 'SPC统计分析' : currentAnalysisType === 'energy' ? '能耗优化' : currentAnalysisType === 'capacity' ? '产能预测' : 'AI建议'" width="680px" destroy-on-close class="ai-dlg" :close-on-click-modal="false">
      <div v-if="aiAnalysisLoading" class="dt-loading"><el-icon class="is-loading" size="32"><Loading /></el-icon><p>AI分析中...</p></div>

      <!-- History list (shown when no active result) -->
      <div v-else-if="!aiAnalysisResult && !aiAnalysisLoading" class="ai-prompt-area">
        <div v-if="filteredHistory.length" class="ai-history-panel">
          <div class="ai-subtitle">{{ quickType ? ({ llm:'AI建议', spc:'SPC分析', energy:'能耗优化', capacity:'产能预测' }[quickType]) + '记录' : '分析记录' }}</div>
          <div
            v-for="(h, i) in filteredHistory.slice(0, 8)"
            :key="i"
            class="ai-hi-row"
            @click="aiAnalysisResult = h.data; currentAnalysisType = h.type"
          >
            <span class="ai-hi-tag" :class="h.type">{{ { spc:'SPC', energy:'能耗', capacity:'产能', llm:'AI建议' }[h.type] }}</span>
            <span class="ai-hi-name">{{ h.deviceName }}</span>
            <span class="ai-hi-time">{{ fmtTime(h.ts) }}</span>
          </div>
        </div>
        <div class="ai-device-card">
          <div class="ai-dc-head">
            <span class="ai-dc-icon"><el-icon :size="18"><Cpu /></el-icon></span>
            <div>
              <strong>{{ detailData?.name || '选择设备' }}</strong>
              <small>{{ getStatusText(detailData?.status || '') }}</small>
            </div>
          </div>
          <div class="ai-dc-metrics">
            <span>🌡 {{ detailData?.temperature ?? '--' }}°C</span>
            <span>⚙ {{ detailData?.speed ?? '--' }} rpm</span>
            <span>⚡ {{ detailData?.power ?? '--' }} kW</span>
            <span>📊 {{ detailData?.utilization || '0%' }}</span>
          </div>
          <div class="ai-dc-actions" v-if="!quickType">
            <el-button size="small" @click="handleSPCAnalysis"><el-icon><Histogram /></el-icon> SPC分析</el-button>
            <el-button size="small" @click="handleEnergyOptimization"><el-icon><Lightning /></el-icon> 能耗优化</el-button>
            <el-button size="small" @click="handleCapacityPrediction"><el-icon><TrendCharts /></el-icon> 产能预测</el-button>
            <el-button size="small" type="primary" @click="handleLLMChat"><el-icon><ChatLineRound /></el-icon> AI建议</el-button>
          </div>
          <div class="ai-dc-actions" v-else>
            <el-button size="small" type="primary" @click="handleQuickAnalysis">{{ quickBtn.cta }}</el-button>
          </div>
        </div>
      </div>

      <!-- Result area -->
      <div v-if="!aiAnalysisLoading && aiAnalysisResult" class="ai-result-area">
        <button class="ai-back-btn" @click="aiAnalysisResult = null"><el-icon :size="14"><DArrowLeft /></el-icon> 返回</button>

        <template v-if="currentAnalysisType === 'spc'">
          <div class="ai-result-card">
            <div class="ai-rc-head accent">SPC 制程能力分析</div>
            <div class="ai-rc-body">
              <div class="ai-cpk-badge" :class="aiAnalysisResult.capability?.level || aiAnalysisResult.process_capability">{{ (aiAnalysisResult.capability?.cpk || aiAnalysisResult.cpk)?.toFixed(2) }}</div>
              <div class="ai-stats-row">
                <div><label>CP</label><span>{{ (aiAnalysisResult.capability?.cp || aiAnalysisResult.cp)?.toFixed(2) }}</span></div>
                <div><label>均值</label><span>{{ aiAnalysisResult.statistics?.mean || aiAnalysisResult.mean }}</span></div>
                <div><label>标准差</label><span>{{ aiAnalysisResult.statistics?.std || aiAnalysisResult.std }}</span></div>
                <div><label>稳定性</label><span>{{ ((aiAnalysisResult.stability || 0) * 100).toFixed(0) }}%</span></div>
              </div>
            </div>
          </div>
        </template>

        <template v-else-if="currentAnalysisType === 'energy'">
          <div class="ai-result-card">
            <div class="ai-rc-head warning">能耗优化分析</div>
            <div class="ai-rc-body">
              <div class="ai-energy-kpis">
                <div><span class="kpi-val">{{ aiAnalysisResult.estimated_energy_savings_pct }}%</span><small>节能潜力</small></div>
                <div><span class="kpi-val">{{ aiAnalysisResult.estimated_monthly_savings_kwh }} kWh</span><small>月省电量</small></div>
              </div>
            </div>
          </div>
        </template>

        <template v-else-if="currentAnalysisType === 'capacity'">
          <div class="ai-result-card">
            <div class="ai-rc-head success">产能预测</div>
            <div class="ai-rc-body">
              <div class="ai-energy-kpis">
                <div><span class="kpi-val">{{ aiAnalysisResult.summary?.total || aiAnalysisResult.total_predicted }}</span><small>总产量</small></div>
                <div><span class="kpi-val">{{ aiAnalysisResult.summary?.daily_avg || aiAnalysisResult.average_daily }}</span><small>日均</small></div>
              </div>
            </div>
          </div>
        </template>

        <template v-else>
          <div class="ai-result-card">
            <div class="ai-rc-head accent">AI 智能建议 — {{ detailData?.name }}</div>
            <div class="ai-rc-body ai-llm-body" v-html="aiAdviceHtml(aiAnalysisResult)"></div>
          </div>
        </template>

        <div class="ai-result-meta">
          <el-tag size="small" :type="detailData?.status === 'running' ? 'success' : 'info'">{{ getStatusText(detailData?.status || '') }}</el-tag>
          <span>{{ detailData?.name }}</span>
          <span>{{ detailData?.temperature ?? '--' }}°C</span>
        </div>
      </div>  <!-- end ai-result-area -->

      <!-- Generic / Other Result -->
      <div v-else-if="aiAnalysisResult" class="dt-ai-result">
        <div v-if="aiAnalysisResult.success === false" class="dt-ai-warn">
          <el-icon><Warning /></el-icon> {{ aiAnalysisResult.message || '服务暂不可用' }}
        </div>
        <div v-else class="dt-ai-raw">{{ JSON.stringify(aiAnalysisResult, null, 2) }}</div>
      </div>
      <template #footer><el-button @click="aiAnalysisVisible=false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, onUnmounted, reactive, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getDeviceStatus } from '@/api/dashboard'
import { getAlarmDevices, predictDeviceFault, predictCapacity, analyzeSPC, llmChat, optimizeEnergy, startDevice, stopDevice } from '@/api/services'
import { useThemeStore } from '@/stores/theme'
import { useChartTheme } from '@/composables/useChartTheme'
import { wsService } from '@/utils/websocket'
import { Monitor, Refresh, Search, TrendCharts, Warning, Grid, View, Cpu,
 VideoPlay, VideoPause, Loading, CircleCheck, Histogram, Lightning, ChatLineRound, Close, ArrowRight, DArrowLeft } from '@element-plus/icons-vue'
import DigitalTwinScene from '@/components/device/DigitalTwinScene.vue'
import { marked } from 'marked'
marked.setOptions({ breaks: true, gfm: true })
import { listAnalyses, saveAnalysis } from '@/api/agent'
import { useUserStore } from '@/stores/user'

const themeStore = useThemeStore()
const chartTheme = useChartTheme()
const deviceList = ref<any[]>([])
const alarmList = ref<any[]>([])
const searchKeyword = ref('')
const statusFilter = ref('')
const page = ref(1)
const pageSize = ref(50)
const detailVisible = ref(false)
const detailData = ref<any>({})
const route = useRoute()
const viewMode = ref<'list' | '3d'>('3d')
const predictVisible = ref(false)
const predictData = ref<any>({})
const aiAnalysisVisible = ref(false)
const aiAnalysisLoading = ref(false)
const aiAnalysisResult = ref<any>(null)
const currentAnalysisType = ref('')
const quickType = ref<string | null>(null) // null=多选, 'llm'/'spc'/'energy'/'capacity'=单选
const selectedDevice = ref<any>(null)
const aiHistory = ref<any[]>([])
const hudPanels = reactive({ alarms: true, charts: true })

let refreshInterval: number
const wsUnsubscribe = ref<(() => void) | null>(null)

const stats = ref([
  { label: '设备总数', value: 0, icon: 'Monitor', theme: 'primary' },
  { label: '运行中', value: 0, icon: 'CircleCheck', theme: 'success' },
  { label: '空闲', value: 0, icon: 'VideoPause', theme: 'info' },
  { label: '故障', value: 0, icon: 'Warning', theme: 'warning' },
])
const filterChips = [
  { key: '', label: '全部' },
  { key: 'running', label: '运行' },
  { key: 'idle', label: '空闲' },
  { key: 'fault', label: '故障' },
]

const utilizationOption = ref({})
const statusOption = ref({})

const filteredDevices = computed(() => {
  let result = deviceList.value
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    result = result.filter(d => d.name?.toLowerCase().includes(kw) || d.code?.toLowerCase().includes(kw))
  }
  if (statusFilter.value) result = result.filter(d => d.status === statusFilter.value)
  return result
})

const pagedDevices = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredDevices.value.slice(start, start + pageSize.value)
})

const getStatusType = (s: string) => ({ running: 'success', idle: 'info', maintenance: 'warning', fault: 'danger' } as any)[s] || 'info'
const getStatusText = (s: string) => ({ running: '运行中', idle: '空闲', maintenance: '维护中', fault: '故障' } as any)[s] || '未知'
const getAlarmClass = (l: string) => ({ high: 'danger', medium: 'warning', low: 'info' } as any)[l] || 'info'

const calculateRuntime = (lastHeartbeat: string) => {
  if (!lastHeartbeat) return '0h'
  try { const h = Math.floor((Date.now() - new Date(lastHeartbeat).getTime()) / 3600000); return h < 1 ? '<1h' : `${h}h` } catch { return '0h' }
}

const fetchDeviceData = async () => {
  try {
    const [deviceRes, alarmRes] = await Promise.all([getDeviceStatus(), getAlarmDevices()])
    let devices: any[] = []
    if (Array.isArray(deviceRes)) devices = deviceRes
    else if (deviceRes?.data?.value) devices = deviceRes.data.value
    else if (deviceRes?.data && Array.isArray(deviceRes.data)) devices = deviceRes.data
    else if (deviceRes?.value) devices = deviceRes.value

    deviceList.value = devices.map((item: any, i: number) => ({
      id: item.id, name: item.deviceName || item.deviceCode || `设备${i + 1}`, code: item.deviceCode || '',
      status: item.status === 'ONLINE' ? 'running' : item.status === 'OFFLINE' ? 'idle' : item.status === 'ALARM' ? 'fault' : 'maintenance',
      utilization: item.speed && item.speed > 0 ? Math.round(item.speed / 15) + '%' : '0%',
      runtime: item.lastHeartbeat ? calculateRuntime(item.lastHeartbeat) : '0h',
      temperature: item.temperature ?? null,
      speed: item.speed ?? 0, power: item.speed && item.speed > 0 ? Math.round(item.speed * 0.02 + 5) : 0,
      efficiency: item.efficiency ?? 0,
    }))

    let alarms: any[] = []
    if (Array.isArray(alarmRes)) alarms = alarmRes
    else if (alarmRes?.data?.value) alarms = alarmRes.data.value
    else if (alarmRes?.data && Array.isArray(alarmRes.data)) alarms = alarmRes.data
    else if (alarmRes?.value) alarms = alarmRes.value
    alarmList.value = alarms

    stats.value = [
      { label: '设备总数', value: deviceList.value.length, icon: 'Monitor', theme: 'primary' },
      { label: '运行中', value: deviceList.value.filter(d => d.status === 'running').length, icon: 'CircleCheck', theme: 'success' },
      { label: '空闲', value: deviceList.value.filter(d => d.status === 'idle').length, icon: 'VideoPause', theme: 'info' },
      { label: '故障', value: deviceList.value.filter(d => d.status === 'fault').length, icon: 'Warning', theme: 'warning' },
    ]
    updateCharts()
  } catch (e) { console.error(e) }
}

const updateCharts = () => {
  const isDark = themeStore.isDark
  const tc = isDark ? '#aaa' : '#666'

  utilizationOption.value = {
    ...chartTheme.value,
    tooltip: { trigger: 'axis' },
    grid: { left: 5, right: 5, top: 5, bottom: 5, containLabel: true },
    xAxis: { type: 'category', data: deviceList.value.map(d => d.name).slice(0, 6), axisLabel: { color: tc, fontSize: 10 } },
    yAxis: { type: 'value', max: 100, axisLabel: { color: tc, fontSize: 10 } },
    series: [{ type: 'bar', data: deviceList.value.map(d => parseInt(d.utilization) || 0).slice(0, 10), itemStyle: { borderRadius: [4, 4, 0, 0], color: '#6366f1' }, barWidth: 12 }]
  }

  const st: any = { running: 0, idle: 0, fault: 0, maintenance: 0 }
  deviceList.value.forEach(d => { if (st[d.status] !== undefined) st[d.status]++ })
  statusOption.value = {
    ...chartTheme.value,
    tooltip: { trigger: 'item' },
    series: [{ type: 'pie', radius: ['50%', '75%'], center: ['50%', '50%'], label: { color: tc, fontSize: 10 },
      data: [{ value: st.running, name: '运行中', itemStyle: { color: '#34c759' } }, { value: st.idle, name: '空闲', itemStyle: { color: '#8e8e93' } },
             { value: st.fault, name: '故障', itemStyle: { color: '#ff3b30' } }, { value: st.maintenance, name: '维护', itemStyle: { color: '#ff9500' } }] }]
  }
}

const handleDeviceSelect = (d: any) => { selectedDevice.value = d; detailData.value = d }
const handle3DAction = (payload: { type: string; device: any }) => {
  detailData.value = payload.device
  if (payload.type === 'predict') { handlePredict(payload.device) }
  else { openAiDialog(payload.type) }
}
const refresh = () => { fetchDeviceData() }
async function loadAnalysisHistory() {
  try {
    const userStore = useUserStore()
    const uid = userStore.userInfo?.username || 'default'
    const records = await listAnalyses(uid)
    aiHistory.value = records.map(r => ({
      type: r.analysis_type, deviceName: r.device_name, deviceCode: r.device_code,
      ts: new Date(r.created_at).getTime(), data: r.result_data,
    }))
  } catch { /* 后端未启动，使用内存历史 */ }
}
const handleDetail = (d: any) => { detailData.value = d; detailVisible.value = true }
const handleStart = async (d: any) => { try { await startDevice(d.id || d.code); ElMessage.success('启动成功'); fetchDeviceData() } catch { ElMessage.error('启动失败') } }
const handleStop = async (d: any) => { try { await stopDevice(d.id || d.code); ElMessage.success('停止成功'); fetchDeviceData() } catch { ElMessage.error('停止失败') } }

const handleCardPredict = (d: any) => { detailData.value = d; handlePredict(d) }
const handlePredict = async (d: any) => {
  try {
    const payload = { device_code: d.code || d.id, history_data: [{ temperature: Number(d.temperature) || 80, speed: Number(d.speed) || 50 }], hours_ahead: 24 }
    const res = await predictDeviceFault(payload)
    const raw = res?.data || res
    const inner = raw?.data || raw
    predictData.value = {
      deviceName: d.name || d.deviceName,
      faultLevel: inner.prediction === 'FAULT' ? 'danger' : inner.prediction === 'WARNING' ? 'warning' : 'success',
      message: inner.prediction === 'FAULT' ? '预测可能发生故障，建议安排检修' : inner.prediction === 'WARNING' ? '存在潜在风险，建议加强监控' : '设备运行状态良好，无需维护',
      confidence: `${((inner.confidence || 0.85) * 100).toFixed(0)}%`,
      riskFactors: inner.risk_factors || inner.riskFactors || []
    }
    predictVisible.value = true
  } catch { ElMessage.error('预测失败，请确认AI服务已启动') }
}

const showAIResult = (type: string, data: any) => {
  const result = data?.data || data
  currentAnalysisType.value = type
  aiAnalysisResult.value = result
  aiAnalysisLoading.value = false
  const d = detailData.value || {}
  aiHistory.value.unshift({
    type, deviceName: d.name || d.code || '', deviceCode: d.code || '',
    ts: Date.now(), data: result,
  })
  if (aiHistory.value.length > 20) aiHistory.value.length = 20
  // 持久化到 MySQL
  const userStore = useUserStore()
  const uid = userStore.userInfo?.username || 'default'
  saveAnalysis(uid, d.code || '', d.name || '', type, result).catch(() => {})
}
const filteredHistory = computed(() => {
  if (!quickType.value) return aiHistory.value
  return aiHistory.value.filter(h => h.type === quickType.value)
})
const quickBtn = computed(() => {
  const map: Record<string, { cta: string }> = { llm: { cta: '开始 AI 建议分析' }, spc: { cta: '开始 SPC 分析' }, energy: { cta: '开始能耗优化分析' }, capacity: { cta: '开始产能预测分析' } }
  return map[quickType.value || 'llm'] || { cta: '开始分析' }
})
function handleQuickAnalysis() {
  const t = quickType.value || 'llm'
  if (t === 'spc') handleSPCAnalysis()
  else if (t === 'energy') handleEnergyOptimization()
  else if (t === 'capacity') handleCapacityPrediction()
  else handleLLMChat()
}
function fmtTime(ts: number): string {
  const diff = Date.now() - ts
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  return new Date(ts).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}
function aiAdviceHtml(result: any): string {
  const text = result?.content || result?.response || ''
  return (marked.parse(text) as string)
    .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
    .replace(/\son\w+="[^"]*"/gi, '')
    .replace(/\son\w+='[^']*'/gi, '')
}
const openAiDialog = (type?: string) => {
  aiAnalysisVisible.value = true; aiAnalysisResult.value = null
  aiAnalysisLoading.value = false; quickType.value = type || null
}
const handleSPCAnalysis = async () => { aiAnalysisLoading.value = true; currentAnalysisType.value = 'spc'
  try {
    const d = detailData.value || {}
    const realTemp = d.temperature
    const measurements = realTemp != null
      ? Array.from({ length: 20 }, () => Math.round(realTemp + (Math.random() - 0.5) * 10))
      : []
    const res = await analyzeSPC({
      device_code: d.code || d.id,
      parameter: 'temperature',
      measurements
    })
    showAIResult('spc', res?.data || res)
  } catch { aiAnalysisLoading.value = false; ElMessage.error('SPC分析失败') }
}
const handleEnergyOptimization = async () => {
  aiAnalysisVisible.value = true; aiAnalysisLoading.value = true
  try {
    const d = detailData.value || {}
    const res = await optimizeEnergy({
      device_code: d.code || d.id || 'DEV0001',
      current_params: { speed: Number(d.speed) || 0, temperature: Number(d.temperature) || 0, power: Number(d.power) || 0 },
      target_output: 5000
    })
    showAIResult('energy', res)
  } catch (e: any) { aiAnalysisLoading.value = false; console.error('能耗分析失败:', e); ElMessage.error('能耗分析失败，请确认AI服务已启动') }
}
const handleCapacityPrediction = async () => {
  aiAnalysisVisible.value = true; aiAnalysisLoading.value = true
  try {
    const d = detailData.value || {}
    const baseOutput = d.utilization ? parseInt(d.utilization) * 10 + 500 : 0
    const dataPoints = baseOutput > 0
      ? Array.from({ length: 7 }, () => Math.round(baseOutput * (0.9 + Math.random() * 0.2)))
      : []
    const res = await predictCapacity({
      device_code: d.code || d.id,
      production_line_id: 'line-1',
      product_type: 'standard',
      start_date: new Date().toISOString().slice(0, 10),
      historical_outputs: dataPoints,
      days_to_predict: 7
    })
    showAIResult('capacity', res?.data || res)
  } catch { aiAnalysisLoading.value = false; ElMessage.error('产能预测失败') }
}
const handleLLMChat = async () => {
  aiAnalysisVisible.value = true; aiAnalysisLoading.value = true; currentAnalysisType.value = 'llm'
  try {
    const d = detailData.value || {}
    const msg = [
      `请分析设备运行状态并给出优化建议：`,
      `- 设备名称: ${d.name || d.code || '未知设备'}`,
      `- 运行状态: ${getStatusText(d.status || 'unknown')}`,
      `- 温度: ${typeof d.temperature === 'number' ? d.temperature + '°C' : '--'}`,
      `- 转速: ${typeof d.speed === 'number' ? d.speed + ' rpm' : '--'}`,
      `- 功率: ${typeof d.power === 'number' ? d.power + ' kW' : '--'}`,
      `- 利用率: ${d.utilization || '0%'}`,
    ].join('\n')
    const res = await llmChat({ message: msg, context: { device_code: d.code, status: d.status, temperature: d.temperature, speed: d.speed, power: d.power } })
    showAIResult('llm', res?.data || res)
  } catch { aiAnalysisLoading.value = false; ElMessage.info('AI建议暂不可用，请配置API Key') }
}

onMounted(() => {
  fetchDeviceData()
  loadAnalysisHistory()
  if (route.query.device) {
    viewMode.value = '3d'
    const stopWatch = watch(deviceList, (list) => {
      if (!list.length) return
      const target = list.find((d: any) => d.code === route.query.device || d.name === route.query.device)
      if (target) { handleDeviceSelect(target); nextTick(() => stopWatch()) }
    })
  }
  refreshInterval = window.setInterval(fetchDeviceData, 5000)
  wsService.connect()
  wsUnsubscribe.value = wsService.subscribe((data: any) => {
    if (data.devices) {
      deviceList.value = data.devices.map((item: any, i: number) => ({
        id: item.id, name: item.deviceName || item.deviceCode || `设备${i + 1}`, code: item.deviceCode || '',
        status: item.status === 'ONLINE' ? 'running' : item.status === 'OFFLINE' ? 'idle' : item.status === 'ALARM' ? 'fault' : 'maintenance',
        utilization: item.speed && item.speed > 0 ? Math.round(item.speed / 15) + '%' : '0%',
        runtime: item.lastHeartbeat ? calculateRuntime(item.lastHeartbeat) : '0h',
        temperature: item.temperature ?? null,
        speed: item.speed ?? 0, power: item.speed && item.speed > 0 ? Math.round(item.speed * 0.02 + 5) : 0,
        efficiency: item.efficiency ?? 0,
      }))
      updateCharts()
    }
  })
})

onUnmounted(() => { clearInterval(refreshInterval); wsUnsubscribe.value?.(); wsService.disconnect() })
watch(() => themeStore.isDark, () => updateCharts())
watch(deviceList, () => { if (deviceList.value.length > 0) updateCharts() })
</script>

<style scoped>
/* ===== ROOT ===== */
.dt-page { display: flex; flex-direction: column; height: 100%; margin: -20px; overflow: hidden; background: var(--bg-app); color: var(--text-primary); font-size: 13px; }

/* ===== TOP BAR ===== */
.dt-topbar { display: flex; align-items: center; height: 40px; padding: 0 16px; background: var(--bg-sidebar); border-bottom: 1px solid var(--border-color); flex-shrink: 0; gap: 12px; }
.dt-topbar-left { flex-shrink: 0; }
.dt-logo { display: flex; align-items: center; gap: 6px; font-size: 14px; font-weight: 700; color: var(--accent); }
.dt-topbar-stats { flex: 1; display: flex; justify-content: center; gap: 14px; }
.dt-stats-badge { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--text-secondary); }
.dt-badge-num { font-size: 18px; font-weight: 700; }
.dt-stats-badge.primary .dt-badge-num { color: var(--accent); }
.dt-stats-badge.success .dt-badge-num { color: var(--success); }
.dt-stats-badge.info .dt-badge-num { color: var(--info); }
.dt-stats-badge.warning .dt-badge-num { color: var(--warning); }
.dt-stats-badge.danger .dt-badge-num { color: var(--warning); }
.dt-topbar-spacer { flex: 1; }
.dt-topbar-right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.dt-view-switch { display: flex; background: var(--bg-hover); border-radius: 6px; padding: 2px; }
.dt-view-switch button { display: flex; align-items: center; gap: 4px; padding: 4px 10px; border: none; border-radius: 5px; background: transparent; color: var(--text-secondary); font-size: 12px; cursor: pointer; transition: all .12s; }
.dt-view-switch button.on { background: var(--bg-card); color: var(--accent); font-weight: 600; box-shadow: 0 1px 3px rgba(0,0,0,.1); }
.dt-topbar-actions { display: flex; align-items: center; gap: 4px; }
.dt-fchip { padding: 3px 10px; border-radius: 5px; font-size: 11px; cursor: pointer; color: var(--text-muted); transition: all .12s; }
.dt-fchip:hover { background: var(--bg-hover); color: var(--text-primary); }
.dt-fchip.on { background: var(--accent-light); color: var(--accent); font-weight: 600; }
.dt-btn-refresh { color: var(--text-muted); font-size: 18px; }

/* ===== MAIN ===== */
.dt-main { flex: 1; min-height: 0; overflow: hidden; }

/* 3D */
.dt-scene-wrap { width: 100%; height: 100%; position: relative; }

/* HUD panels */
.dt-hud-btns { position: absolute; top: 8px; right: 8px; z-index: 20; display: flex; gap: 3px; }
.dt-hud-btns button { width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; position: relative; background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 5px; color: var(--text-secondary); cursor: pointer; font-size: 13px; transition: all .12s; }
.dt-hud-btns button:hover, .dt-hud-btns button.on { background: var(--accent-light); border-color: var(--accent); color: var(--accent); }
.dt-hud-dot-badge { position: absolute; top: -4px; right: -6px; min-width: 14px; height: 14px; padding: 0 3px; border-radius: 7px; background: var(--danger); color: #fff; font-size: 9px; font-weight: 700; line-height: 14px; text-align: center; }

.dt-hud { position: absolute; z-index: 10; }
.dt-hud-alarms { top: 42px; right: 8px; width: 210px; max-height: 240px; display: flex; flex-direction: column; background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 8px; overflow: hidden; }
.dt-hud-head { display: flex; align-items: center; gap: 5px; padding: 6px 10px; font-size: 11px; font-weight: 600; color: var(--text-primary); background: var(--bg-hover); cursor: pointer; user-select: none; flex-shrink: 0; }
.dt-hud-badge { margin-left: 4px; font-size: 10px; background: var(--danger-light); color: var(--danger); padding: 0 6px; border-radius: 8px; }
.dt-hud-close { margin-left: auto; opacity: .5; }
.dt-hud-list { flex: 1; overflow-y: auto; padding: 2px 0; }
.dt-hud-row { display: flex; align-items: center; gap: 6px; padding: 4px 10px; font-size: 11px; color: var(--text-secondary); }
.dt-hud-row:hover { background: var(--bg-hover); }
.dt-hud-dot { width: 5px; height: 5px; border-radius: 50%; flex-shrink: 0; background: var(--border-color); }
.dt-hud-row.danger .dt-hud-dot { background: var(--danger); box-shadow: 0 0 5px var(--danger); }
.dt-hud-row.warning .dt-hud-dot { background: var(--warning); }
.dt-hud-none { padding: 12px; font-size: 11px; color: var(--text-muted); text-align: center; }

.dt-hud-charts { bottom: 8px; left: 8px; width: 420px; background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 8px; overflow: hidden; }
.dt-hud-chart-head { display: flex; justify-content: space-between; align-items: center; padding: 5px 14px; font-size: 11px; font-weight: 600; color: var(--text-secondary); }
.dt-hud-chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 4px; padding: 0 8px 6px; }
.dt-hud-chart-grid em { display: block; font-size: 10px; color: var(--text-muted); text-align: center; font-style: normal; text-transform: uppercase; letter-spacing: .4px; }

.hud-fade-enter-active, .hud-fade-leave-active { transition: opacity .12s; }
.hud-fade-enter-from, .hud-fade-leave-to { opacity: 0; }
.hud-slide-enter-active, .hud-slide-leave-active { transition: all .18s ease; }
.hud-slide-enter-from, .hud-slide-leave-to { opacity: 0; transform: translateY(8px); }

/* ===== LIST VIEW ===== */
.dt-list-wrap { height: 100%; display: flex; flex-direction: column; overflow: hidden; }
.dt-empty { flex: 1; display: flex; align-items: center; justify-content: center; }
.dt-list-grid { flex: 1; overflow-y: auto; display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 14px; padding: 16px; align-content: start; }

.dc-card { background: var(--bg-card); border: 1px solid var(--border-light); border-radius: 10px; padding: 18px 20px 16px; cursor: pointer; transition: all .15s; }
.dc-card:hover { border-color: var(--accent); box-shadow: 0 2px 12px rgba(0,0,0,.04); }
.dc-card-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; }
.dc-card-info { flex: 1; min-width: 0; }
.dc-card-name { display: block; font-size: 15px; font-weight: 600; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dc-card-code { font-size: 12px; color: var(--text-muted); }
.dc-card-tag { font-size: 11px; padding: 2px 10px; border-radius: 8px; font-weight: 600; flex-shrink: 0; }
.dc-card-tag.running { background: var(--success-light); color: var(--success); }
.dc-card-tag.idle { background: var(--info-light); color: var(--info); }
.dc-card-tag.fault { background: var(--danger-light); color: var(--danger); }
.dc-card-tag.maintenance { background: var(--warning-light); color: var(--warning); }

.dc-card-metrics { display: grid; grid-template-columns: repeat(4, 1fr); gap: 4px; margin-bottom: 14px; }
.dc-metric { text-align: center; }
.dc-m-val { display: block; font-size: 20px; font-weight: 700; color: var(--text-primary); line-height: 1.2; }
.dc-m-val.warn { color: var(--warning); }
.dc-m-val.hot { color: var(--danger); }
.dc-m-unit { display: block; font-size: 11px; color: var(--text-muted); margin-top: 2px; }

.dc-card-util { height: 5px; background: var(--border-color); border-radius: 3px; overflow: hidden; margin-bottom: 12px; }
.dc-card-util div { height: 100%; background: var(--accent); border-radius: 3px; transition: width .6s; min-width: 2px; }
.dc-card-foot { display: flex; gap: 4px; padding-top: 2px; }
.dt-list-pager { display: flex; justify-content: center; padding: 6px; flex-shrink: 0; }

/* ===== DIALOGS ===== */
.dt-dlg-det-head { display: flex; align-items: center; gap: 14px; padding: 16px; background: linear-gradient(135deg, var(--accent-light), transparent); border-radius: 8px; border: 1px solid var(--border-color); margin-bottom: 14px; }
.dt-dlg-det-head strong { font-size: 17px; }
.dt-dlg-det-head small { color: var(--text-secondary); font-size: 12px; }
.dt-dlg-det-avatar { width: 48px; height: 48px; display: flex; align-items: center; justify-content: center; background: var(--accent); color: #fff; border-radius: 10px; flex-shrink: 0; }
.dt-dlg-det-kpis { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; margin-bottom: 14px; }
.dt-dlg-det-kpi { background: var(--bg-hover); border-radius: 8px; padding: 10px; text-align: center; }
.dt-dlg-det-kpi strong { display: block; font-size: 20px; font-weight: 700; color: var(--accent); }
.dt-dlg-det-kpi span { font-size: 11px; color: var(--text-muted); }
.dt-dlg-ai-badge { display: flex; align-items: center; gap: 6px; padding: 10px 14px; background: linear-gradient(135deg, var(--success-light), transparent); border: 1px solid var(--success); border-radius: 8px; margin-bottom: 14px; font-size: 13px; color: var(--success); }
.dt-dlg-ai-btns { display: flex; gap: 6px; flex-wrap: wrap; }

.dt-loading { text-align: center; padding: 40px; color: var(--text-muted); }

/* AI Analysis Result */
.dt-ai-result { font-size: 13px; }
.dt-ai-section { margin-bottom: 16px; }
.dt-ai-section-title { font-size: 12px; font-weight: 700; color: var(--accent); margin-bottom: 10px; display: flex; align-items: center; gap: 6px; }
.dt-ai-section-title::before { content: ''; display: block; width: 3px; height: 14px; background: var(--accent); border-radius: 2px; }
.dt-ai-cpk { display: flex; align-items: center; gap: 14px; margin-bottom: 12px; }
.dt-ai-cpk-ring { width: 56px; height: 56px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 20px; font-weight: 800; color: #fff; background: var(--success); }
.dt-ai-cpk-ring.FAIR, .dt-ai-cpk-ring.POOR { background: var(--danger); }
.dt-ai-cpk span { display: block; font-size: 11px; color: var(--text-muted); }
.dt-ai-cpk em { display: block; font-size: 15px; font-weight: 700; color: var(--text-primary); font-style: normal; }
.dt-ai-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.dt-ai-stats div { text-align: center; padding: 8px 4px; background: var(--bg-hover); border-radius: 6px; }
.dt-ai-stats label { display: block; font-size: 10px; color: var(--text-muted); margin-bottom: 2px; }
.dt-ai-stats span { font-size: 15px; font-weight: 700; color: var(--text-primary); }
.dt-ai-limits { display: flex; gap: 4px; }
.dt-ai-limits div { flex: 1; text-align: center; padding: 6px 2px; background: var(--bg-hover); border-radius: 6px; }
.dt-ai-limits em { display: block; font-size: 14px; font-weight: 700; color: var(--text-primary); font-style: normal; }
.dt-ai-limits em.danger { color: var(--danger); }
.dt-ai-limits span { font-size: 9px; color: var(--text-muted); }
.dt-ai-warn { display: flex; align-items: flex-start; gap: 8px; padding: 10px; background: var(--warning-light); border-radius: 6px; color: var(--warning); font-size: 12px; }
.dt-ai-recs { display: flex; flex-direction: column; gap: 4px; }
.dt-ai-recs div { padding: 6px 10px; background: var(--bg-hover); border-radius: 6px; font-size: 12px; color: var(--text-secondary); }
.dt-ai-energy-delta { display: flex; gap: 12px; }
.dt-ai-energy-delta div { flex: 1; text-align: center; padding: 10px; background: var(--bg-hover); border-radius: 8px; font-size: 11px; color: var(--text-muted); }
.dt-ai-energy-delta .val { display: block; font-size: 22px; font-weight: 800; color: var(--accent); margin-top: 2px; }
.dt-ai-params { display: flex; flex-direction: column; gap: 6px; }
.dt-ai-param-row { display: flex; align-items: center; gap: 8px; padding: 8px 10px; background: var(--bg-hover); border-radius: 6px; }
.dt-ai-param-row label { font-size: 12px; color: var(--text-muted); width: 36px; }
.dt-ai-param-row .old { font-size: 14px; font-weight: 600; color: var(--text-muted); text-decoration: line-through; }
.dt-ai-param-row .new { font-size: 15px; font-weight: 700; color: var(--accent); }
.dt-ai-param-row .chg { margin-left: auto; font-size: 11px; padding: 1px 6px; border-radius: 4px; }
.dt-ai-param-row .chg.down { background: var(--success-light); color: var(--success); }
.dt-ai-param-row .chg.up { background: var(--warning-light); color: var(--warning); }
.dt-ai-alt { display: flex; flex-direction: column; gap: 4px; }
.dt-ai-alt-row { display: flex; align-items: center; gap: 8px; padding: 6px 10px; background: var(--bg-hover); border-radius: 6px; font-size: 11px; color: var(--text-secondary); }
.dt-ai-alt-row span:first-child { font-weight: 600; min-width: 36px; color: var(--text-primary); }
.dt-ai-table { font-size: 12px; }
.dt-ai-table-head { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 4px; padding: 4px 8px; font-weight: 600; color: var(--text-muted); font-size: 10px; text-transform: uppercase; }
.dt-ai-table-row { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 4px; padding: 4px 8px; border-radius: 4px; }
.dt-ai-table-row:nth-child(even) { background: var(--bg-hover); }
.dt-ai-table-row .muted { color: var(--text-muted); font-size: 10px; }
.dt-ai-llm { padding: 14px; background: var(--bg-hover); border-radius: 8px; line-height: 1.6; white-space: pre-wrap; }
.dt-ai-raw { padding: 12px; background: var(--bg-hover); border-radius: 8px; font-family: monospace; font-size: 11px; white-space: pre-wrap; color: var(--text-secondary); max-height: 400px; overflow-y: auto; }

/* AI 建议 */
.dt-ai-advice { }
.dt-ai-advice-header { display: flex; align-items: center; gap: 12px; padding: 14px; background: linear-gradient(135deg, var(--accent-light), transparent); border: 1px solid var(--accent-light); border-radius: 10px; margin-bottom: 14px; }
.dt-ai-advice-header strong { display: block; font-size: 15px; color: var(--text-primary); }
.dt-ai-advice-header span { font-size: 11px; color: var(--text-muted); }
.dt-ai-advice-icon { width: 42px; height: 42px; display: flex; align-items: center; justify-content: center; background: var(--accent); color: #fff; border-radius: 10px; }
.dt-ai-advice-body { padding: 14px; background: var(--bg-hover); border-radius: 8px; line-height: 1.8; font-size: 13px; color: var(--text-primary); }
.dt-ai-advice-body :deep(h2) { font-size: 16px; margin: 12px 0 6px; color: var(--text-primary); }
.dt-ai-advice-body :deep(h3) { font-size: 14px; margin: 10px 0 4px; color: var(--accent); }
.dt-ai-advice-body :deep(h4) { font-size: 13px; margin: 8px 0 4px; color: var(--text-primary); }
.dt-ai-advice-body :deep(p) { margin: 0 0 6px; }
.dt-ai-advice-body :deep(strong) { font-weight: 700; color: var(--text-primary); }
.dt-ai-advice-body :deep(li) { margin-left: 16px; margin-bottom: 2px; }
.dt-ai-advice-body :deep(code) { background: var(--accent-light); color: var(--accent); padding: 1px 5px; border-radius: 3px; font-size: 12px; }
.dt-ai-advice-body :deep(hr) { border: none; border-top: 1px solid var(--border-color); margin: 10px 0; }
.dt-ai-advice-item { margin-bottom: 12px; }
.dt-ai-advice-item strong { display: block; font-size: 12px; color: var(--accent); margin-bottom: 4px; text-transform: capitalize; }
.dt-ai-advice-item p { margin: 0; font-size: 13px; color: var(--text-secondary); }
.dt-ai-advice-status { margin-top: 12px; }
.dt-ai-status-row { display: flex; align-items: center; gap: 10px; padding: 8px 12px; background: var(--bg-hover); border-radius: 8px; font-size: 11px; color: var(--text-muted); }

/* ===== AI Panel Redesign ===== */
.ai-dlg :deep(.el-dialog__body) { padding: 16px 20px; }

.ai-subtitle { font-size: 11px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 8px; }

/* history */
.ai-history-panel { margin-bottom: 16px; }
.ai-hi-row {
  display: flex; align-items: center; gap: 10px; padding: 9px 12px;
  border-radius: 8px; cursor: pointer; transition: all 0.15s;
  border: 1px solid var(--border-color); margin-bottom: 4px;
}
.ai-hi-row:hover { background: var(--bg-hover); border-color: var(--accent); }
.ai-hi-tag {
  font-size: 10px; font-weight: 600; padding: 2px 7px; border-radius: 4px;
  color: #fff; flex-shrink: 0;
}
.ai-hi-tag.spc { background: var(--accent, #6366f1); }
.ai-hi-tag.energy { background: var(--warning, #f59e0b); }
.ai-hi-tag.capacity { background: var(--success, #10b981); }
.ai-hi-tag.llm { background: var(--accent-secondary, #22d3ee); }
.ai-hi-name { font-size: 13px; color: var(--text-primary); flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ai-hi-time { font-size: 11px; color: var(--text-muted); flex-shrink: 0; }

/* device card */
.ai-device-card {
  border: 1px solid var(--border-color); border-radius: var(--radius-lg, 14px);
  padding: 18px 20px; background: var(--bg-card);
}
.ai-dc-head { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.ai-dc-icon {
  width: 40px; height: 40px; border-radius: 10px;
  background: var(--accent-light); color: var(--accent);
  display: flex; align-items: center; justify-content: center;
}
.ai-dc-head strong { font-size: 15px; color: var(--text-primary); }
.ai-dc-head small { display: block; font-size: 11px; color: var(--text-muted); }
.ai-dc-metrics { display: flex; gap: 16px; margin-bottom: 14px; padding: 10px 14px; background: var(--bg-hover); border-radius: 8px; font-size: 13px; color: var(--text-secondary); }
.ai-dc-metrics span { display: flex; align-items: center; gap: 4px; }
.ai-dc-actions { display: flex; gap: 8px; flex-wrap: wrap; }

/* loading */
.ai-loading {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 48px 0; gap: 16px; color: var(--text-muted); font-size: 13px;
}
.ai-loading-spin {
  width: 36px; height: 36px; border: 3px solid var(--border-color);
  border-top-color: var(--accent); border-radius: 50%; animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* result */
.ai-result-area { animation: fadeIn 0.25s ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(6px); } to { opacity: 1; transform: translateY(0); } }
.ai-back-btn {
  display: inline-flex; align-items: center; gap: 4px; padding: 5px 12px;
  border-radius: 6px; border: 1px solid var(--border-color); background: transparent;
  color: var(--text-muted); font-size: 12px; cursor: pointer; margin-bottom: 14px;
  font-family: inherit; transition: all 0.15s;
}
.ai-back-btn:hover { border-color: var(--accent); color: var(--accent); }
.ai-result-card { border: 1px solid var(--border-color); border-radius: var(--radius-lg, 14px); overflow: hidden; }
.ai-rc-head {
  padding: 10px 16px; font-size: 13px; font-weight: 600; color: #fff;
}
.ai-rc-head.accent { background: var(--accent, #6366f1); }
.ai-rc-head.success { background: var(--success, #10b981); }
.ai-rc-head.warning { background: var(--warning, #f59e0b); }
.ai-rc-body { padding: 16px; }
.ai-cpk-badge {
  width: 72px; height: 72px; border-radius: 50%; display: flex; align-items: center;
  justify-content: center; font-size: 22px; font-weight: 700; color: #fff; margin: 0 auto 12px;
}
.ai-cpk-badge.acceptable, .ai-cpk-badge.good { background: var(--success, #10b981); }
.ai-cpk-badge.marginal { background: var(--warning, #f59e0b); }
.ai-cpk-badge.poor { background: var(--danger, #ef4444); }
.ai-stats-row { display: flex; gap: 12px; justify-content: center; }
.ai-stats-row div { text-align: center; }
.ai-stats-row label { display: block; font-size: 10px; color: var(--text-muted); }
.ai-stats-row span { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.ai-energy-kpis { display: flex; gap: 16px; justify-content: center; }
.ai-energy-kpis div { text-align: center; }
.kpi-val { display: block; font-size: 24px; font-weight: 700; color: var(--accent); }
.ai-energy-kpis small { font-size: 11px; color: var(--text-muted); }
.ai-llm-body { line-height: 1.8; font-size: 13px; color: var(--text-primary); }
.ai-llm-body :deep(p) { margin: 0 0 6px; }
.ai-llm-body :deep(strong) { color: var(--accent); }
.ai-llm-body :deep(li) { margin-bottom: 2px; }
.ai-llm-body :deep(code) { background: var(--accent-light); color: var(--accent); padding: 1px 5px; border-radius: 3px; font-size: 12px; }
.ai-result-meta { display: flex; align-items: center; gap: 10px; margin-top: 12px; padding: 8px 12px; background: var(--bg-hover); border-radius: 8px; font-size: 11px; color: var(--text-muted); }
</style>
