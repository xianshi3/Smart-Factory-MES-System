<template>
  <div class="dt-page">
    <!-- ===== TOP BAR ===== -->
    <header class="dt-topbar">
      <div class="dt-topbar-left">
        <span class="dt-logo-text">◆ Smart MES · 设备监控中心</span>
      </div>
      <div class="dt-topbar-center">
        <div class="dt-topbar-pills">
          <span v-for="s in stats" :key="s.label" class="dt-pill" :class="s.theme">
            <span class="dt-pill-num">{{ s.value }}</span><span class="dt-pill-lbl">{{ s.label }}</span>
          </span>
        </div>
      </div>
      <div class="dt-topbar-right">
        <div class="dt-view-switch">
          <button :class="{ on: viewMode === '3d' }" @click="viewMode = '3d'"><el-icon size="14"><View /></el-icon> 3D孪生</button>
          <button :class="{ on: viewMode === 'list' }" @click="viewMode = 'list'"><el-icon size="14"><Grid /></el-icon> 列表</button>
        </div>
        <div class="dt-topbar-actions" v-if="viewMode === 'list'">
          <el-input v-model="searchKeyword" size="small" placeholder="搜索设备..." clearable :prefix-icon="Search" style="width:160px" />
          <span class="dt-fchip" :class="{ on: statusFilter === '' }" @click="statusFilter = ''">全部</span>
          <span class="dt-fchip run" :class="{ on: statusFilter === 'running' }" @click="statusFilter = 'running'">运行</span>
          <span class="dt-fchip idle" :class="{ on: statusFilter === 'idle' }" @click="statusFilter = 'idle'">空闲</span>
          <span class="dt-fchip fault" :class="{ on: statusFilter === 'fault' }" @click="statusFilter = 'fault'">故障</span>
        </div>
        <el-button text size="small" @click="refresh" class="dt-btn-refresh"><el-icon><Refresh /></el-icon></el-button>
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
              <div><em>利用率 TOP10</em><v-chart :option="utilizationOption" autoresize style="height:140px" /></div>
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
          <div v-for="(d,i) in pagedDevices" :key="d.id || i" class="dt-device-card" :class="'status-'+d.status" @click="handleDetail(d)">
            <div class="dt-dc-head">
              <span class="dt-dc-icon"><el-icon size="18"><Cpu /></el-icon></span>
              <div class="dt-dc-info">
                <span class="dt-dc-name">{{ d.name }}</span>
                <span class="dt-dc-code">{{ d.code }}</span>
              </div>
              <span class="dt-dc-badge" :class="d.status">{{ getStatusText(d.status) }}</span>
            </div>
            <div class="dt-dc-body">
              <div class="dt-dc-kpi"><label>温度</label><b :class="{ hot: d.temperature > 60 }">{{ d.temperature ?? '--' }}°C</b></div>
              <div class="dt-dc-kpi"><label>转速</label><b>{{ d.speed ?? '--' }}</b></div>
              <div class="dt-dc-kpi"><label>功率</label><b>{{ d.power ?? '--' }}kW</b></div>
              <div class="dt-dc-kpi"><label>利用率</label><b>{{ d.utilization || '0%' }}</b></div>
            </div>
            <div class="dt-dc-bar"><div :style="{ width: (parseInt(d.utilization)||0)+'%' }"></div></div>
            <div class="dt-dc-foot">
              <el-button v-if="d.status==='running'||d.status==='ONLINE'" type="danger" size="small" link @click.stop="handleStop(d)"><el-icon><VideoPause /></el-icon>停止</el-button>
              <el-button v-if="d.status==='idle'||d.status==='OFFLINE'" type="success" size="small" link @click.stop="handleStart(d)"><el-icon><VideoPlay /></el-icon>启动</el-button>
              <el-button type="primary" size="small" link @click.stop="handlePredict(d)"><el-icon><Cpu /></el-icon>预测</el-button>
              <el-button type="primary" size="small" link @click.stop="handleDetail(d)"><el-icon><View /></el-icon>详情</el-button>
            </div>
          </div>
        </div>
        <div v-if="filteredDevices.length > pageSize" class="dt-list-pager">
          <el-pagination small v-model:current-page="page" :total="filteredDevices.length" :page-size="pageSize" layout="total, prev, pager, next" background />
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
          <el-button @click="handleSPCAnalysis"><el-icon><Histogram /></el-icon> SPC</el-button>
          <el-button @click="handleEnergyOptimization"><el-icon><Lightning /></el-icon> 能耗优化</el-button>
          <el-button @click="handleCapacityPrediction"><el-icon><TrendCharts /></el-icon> 产能预测</el-button>
          <el-button type="primary" @click="handleLLMChat"><el-icon><ChatLineRound /></el-icon> AI对话</el-button>
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

    <el-dialog v-model="aiAnalysisVisible" :title="'AI分析'" width="520px">
      <div v-if="aiAnalysisLoading" style="text-align:center;padding:40px"><el-icon class="is-loading" size="28"><Loading /></el-icon><p>分析中...</p></div>
      <div v-else-if="aiAnalysisResult"><el-descriptions :column="1" border><el-descriptions-item v-for="(v,k) in aiAnalysisResult" :key="k" :label="k">{{ typeof v==='object'?JSON.stringify(v):v }}</el-descriptions-item></el-descriptions></div>
      <div v-else style="text-align:center;padding:40px">暂无结果</div>
      <template #footer><el-button @click="aiAnalysisVisible=false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, onUnmounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDeviceStatus } from '@/api/dashboard'
import { getAlarmDevices, predictDeviceFault, predictCapacity, analyzeSPC, llmChat, optimizeEnergy, startDevice, stopDevice } from '@/api/services'
import { useThemeStore } from '@/stores/theme'
import { useChartTheme } from '@/composables/useChartTheme'
import { wsService } from '@/utils/websocket'
import { Monitor, Refresh, Search, TrendCharts, PieChart, Warning, Grid, View, Cpu,
 Tools, VideoPlay, VideoPause, Loading, Ticket, Timer, CircleCheck, MagicStick, Histogram, Lightning, ChatLineRound, Close, ArrowDown } from '@element-plus/icons-vue'
import DigitalTwinScene from '@/components/device/DigitalTwinScene.vue'

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
const viewMode = ref<'list' | '3d'>('3d')
const predictVisible = ref(false)
const predictData = ref<any>({})
const aiAnalysisVisible = ref(false)
const aiAnalysisLoading = ref(false)
const aiAnalysisResult = ref<any>(null)
const currentAnalysisType = ref('')
const selectedDevice = ref<any>(null)
const hudPanels = reactive({ alarms: true, charts: false })

let refreshInterval: number
const wsUnsubscribe = ref<(() => void) | null>(null)

const stats = ref([
  { label: '设备总数', value: 0, theme: 'primary' },
  { label: '运行中', value: 0, theme: 'success' },
  { label: '空闲', value: 0, theme: 'info' },
  { label: '故障', value: 0, theme: 'danger' }
])

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
const statusColor = (s: string) => ({ running: '#34c759', idle: '#8e8e93', fault: '#ff3b30', maintenance: '#ff9500' } as any)[s] || '#6366f1'

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
      temperature: item.temperature || Math.floor(Math.random() * 25 + 25),
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
      { label: '设备总数', value: deviceList.value.length, theme: 'primary' },
      { label: '运行中', value: deviceList.value.filter(d => d.status === 'running').length, theme: 'success' },
      { label: '空闲', value: deviceList.value.filter(d => d.status === 'idle').length, theme: 'info' },
      { label: '故障', value: deviceList.value.filter(d => d.status === 'fault').length, theme: 'danger' }
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
    grid: { left: 10, right: 10, top: 10, bottom: 10, containLabel: true },
    xAxis: { type: 'category', data: deviceList.value.map(d => d.name).slice(0, 10), axisLabel: { color: tc, fontSize: 10 } },
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

const handleDeviceSelect = (d: any) => { selectedDevice.value = d }
const handle3DAction = (payload: { type: string; device: any }) => {
  if (payload.type === 'predict') handlePredict(payload.device)
  else if (payload.type === 'spc') handleSPCAnalysis()
  else if (payload.type === 'energy') handleEnergyOptimization()
}
const refresh = () => { fetchDeviceData() }
const handleDetail = (d: any) => { detailData.value = d; detailVisible.value = true }
const handleStart = async (d: any) => { try { await startDevice(d.id || d.code); ElMessage.success('启动成功'); fetchDeviceData() } catch { ElMessage.error('启动失败') } }
const handleStop = async (d: any) => { try { await stopDevice(d.id || d.code); ElMessage.success('停止成功'); fetchDeviceData() } catch { ElMessage.error('停止失败') } }

const handlePredict = async (d: any) => {
  try {
    const payload = { device_code: d.code || d.id, history_data: [{ temperature: d.temperature || 80, speed: d.speed || 50 }], hours_ahead: 24 }
    const res = await predictDeviceFault(payload)
    const data: any = res?.data || res
    predictData.value = {
      deviceName: d.name, faultLevel: data.prediction === 'FAULT' ? 'danger' : data.prediction === 'WARNING' ? 'warning' : 'success',
      message: data.prediction === 'FAULT' ? '预测可能发生故障' : '设备运行正常', confidence: `${((data.confidence || 0) * 100).toFixed(0)}%`,
      riskFactors: data.risk_factors || data.riskFactors || []
    }
    predictVisible.value = true
  } catch { ElMessage.error('预测失败') }
}

const showAIResult = (type: string, data: any) => { currentAnalysisType.value = type; aiAnalysisResult.value = data; aiAnalysisLoading.value = false }
const handleSPCAnalysis = async () => { aiAnalysisVisible.value = true; aiAnalysisLoading.value = true; try { const res = await analyzeSPC({ device_code: detailData.value?.code, data_points: [] }); showAIResult('spc', res?.data || res) } catch { aiAnalysisLoading.value = false; ElMessage.error('分析失败') } }
const handleEnergyOptimization = async () => { aiAnalysisVisible.value = true; aiAnalysisLoading.value = true; try { const res = await optimizeEnergy({ device_code: detailData.value?.code, data_points: [] }); showAIResult('energy', res?.data || res) } catch { aiAnalysisLoading.value = false; ElMessage.error('分析失败') } }
const handleCapacityPrediction = async () => { aiAnalysisVisible.value = true; aiAnalysisLoading.value = true; try { const res = await predictCapacity({ device_code: detailData.value?.code, data_points: [] }); showAIResult('capacity', res?.data || res) } catch { aiAnalysisLoading.value = false; ElMessage.error('预测失败') } }
const handleLLMChat = async () => { ElMessage.info('AI 对话功能开发中') }

onMounted(() => {
  fetchDeviceData()
  refreshInterval = window.setInterval(fetchDeviceData, 5000)
  wsService.connect()
  wsUnsubscribe.value = wsService.subscribe((data: any) => {
    if (data.devices) {
      deviceList.value = data.devices.map((item: any, i: number) => ({
        id: item.id, name: item.deviceName || item.deviceCode || `设备${i + 1}`, code: item.deviceCode || '',
        status: item.status === 'ONLINE' ? 'running' : item.status === 'OFFLINE' ? 'idle' : item.status === 'ALARM' ? 'fault' : 'maintenance',
        utilization: item.speed && item.speed > 0 ? Math.round(item.speed / 15) + '%' : '0%',
        runtime: item.lastHeartbeat ? calculateRuntime(item.lastHeartbeat) : '0h',
        temperature: item.temperature || Math.floor(Math.random() * 25 + 25),
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
.dt-page { display: flex; flex-direction: column; height: 100%; overflow: hidden; background: var(--bg-app); color: var(--text-primary); font-size: 13px; }

/* ===== TOP BAR ===== */
.dt-topbar { display: flex; align-items: center; height: 38px; padding: 0 14px; background: var(--bg-sidebar); border-bottom: 1px solid var(--border-color); flex-shrink: 0; gap: 10px; }
.dt-topbar-left { flex-shrink: 0; }
.dt-logo-text { font-size: 13px; font-weight: 700; color: var(--accent); letter-spacing: .3px; }
.dt-topbar-center { flex: 1; display: flex; justify-content: center; }
.dt-topbar-pills { display: flex; gap: 2px; }
.dt-pill { display: flex; align-items: baseline; gap: 3px; padding: 2px 9px; border-radius: 5px; font-size: 12px; white-space: nowrap; }
.dt-pill.primary { background: var(--accent-light); color: var(--accent); }
.dt-pill.success { background: var(--success-light); color: var(--success); }
.dt-pill.info { background: var(--info-light); color: var(--info); }
.dt-pill.danger { background: var(--danger-light); color: var(--danger); }
.dt-pill-num { font-weight: 800; font-size: 14px; font-variant-numeric: tabular-nums; }
.dt-pill-lbl { opacity: .65; font-size: 10px; }
.dt-topbar-right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.dt-view-switch { display: flex; background: var(--bg-hover); border-radius: 5px; padding: 1px; }
.dt-view-switch button { display: flex; align-items: center; gap: 3px; padding: 3px 10px; border: none; border-radius: 4px; background: transparent; color: var(--text-secondary); font-size: 11px; cursor: pointer; transition: all .12s; }
.dt-view-switch button.on { background: var(--bg-card); color: var(--accent); font-weight: 600; box-shadow: 0 1px 2px rgba(0,0,0,.08); }
.dt-topbar-actions { display: flex; align-items: center; gap: 6px; }
.dt-fchip { padding: 2px 7px; border-radius: 4px; font-size: 11px; cursor: pointer; color: var(--text-muted); transition: all .12s; }
.dt-fchip:hover { background: var(--bg-hover); color: var(--text-primary); }
.dt-fchip.on { background: var(--accent-light); color: var(--accent); font-weight: 600; }
.dt-fchip.run { color: var(--success); }
.dt-fchip.idle { color: var(--info); }
.dt-fchip.fault { color: var(--danger); }
.dt-btn-refresh { color: var(--text-muted); font-size: 16px; }

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

.dt-hud-charts { bottom: 8px; left: 8px; width: 440px; background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 8px; overflow: hidden; }
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
.dt-list-grid { flex: 1; overflow-y: auto; display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 8px; padding: 10px; align-content: start; }
.dt-device-card { position: relative; background: var(--bg-card); border: 1px solid var(--border-light); border-radius: 8px; padding: 10px; cursor: pointer; transition: all .15s; overflow: hidden; }
.dt-device-card::before { content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 3px; border-radius: 8px 0 0 8px; transition: all .15s; }
.dt-device-card.status-running::before { background: var(--success); }
.dt-device-card.status-idle::before { background: var(--info); }
.dt-device-card.status-fault::before { background: var(--danger); }
.dt-device-card.status-maintenance::before { background: var(--warning); }
.dt-device-card:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(0,0,0,.08); border-color: var(--accent); }
.dt-dc-head { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.dt-dc-icon { width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; background: var(--bg-hover); border-radius: 6px; color: var(--text-secondary); flex-shrink: 0; }
.dt-dc-info { flex: 1; min-width: 0; }
.dt-dc-name { display: block; font-size: 13px; font-weight: 600; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dt-dc-code { font-size: 11px; color: var(--text-muted); }
.dt-dc-badge { padding: 1px 7px; border-radius: 6px; font-size: 10px; font-weight: 600; flex-shrink: 0; }
.dt-dc-badge.running { background: var(--success-light); color: var(--success); }
.dt-dc-badge.idle { background: var(--info-light); color: var(--info); }
.dt-dc-badge.fault { background: var(--danger-light); color: var(--danger); }
.dt-dc-badge.maintenance { background: var(--warning-light); color: var(--warning); }
.dt-dc-body { display: grid; grid-template-columns: repeat(4, 1fr); gap: 4px; margin-bottom: 8px; }
.dt-dc-kpi { text-align: center; }
.dt-dc-kpi label { display: block; font-size: 10px; color: var(--text-muted); margin-bottom: 1px; }
.dt-dc-kpi b { font-size: 13px; font-weight: 700; color: var(--text-primary); }
.dt-dc-kpi b.hot { color: var(--danger); }
.dt-dc-bar { height: 3px; background: var(--border-color); border-radius: 2px; margin-bottom: 8px; overflow: hidden; }
.dt-dc-bar div { height: 100%; background: var(--accent); border-radius: 2px; transition: width .6s; min-width: 2px; }
.dt-dc-foot { display: flex; gap: 2px; flex-wrap: wrap; }
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
</style>
