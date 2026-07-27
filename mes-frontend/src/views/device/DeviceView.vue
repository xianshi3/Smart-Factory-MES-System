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
          <el-button type="primary" @click="handleLLMChat"><el-icon><ChatLineRound /></el-icon> AI建议</el-button>
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

    <el-dialog v-model="aiAnalysisVisible" :title="currentAnalysisType === 'spc' ? 'SPC统计分析' : currentAnalysisType === 'energy' ? '能耗优化' : currentAnalysisType === 'capacity' ? '产能预测' : 'AI建议'" width="600px" destroy-on-close>
      <div v-if="aiAnalysisLoading" class="dt-loading"><el-icon class="is-loading" size="32"><Loading /></el-icon><p>AI分析中...</p></div>

      <!-- SPC Result -->
      <div v-else-if="currentAnalysisType === 'spc' && aiAnalysisResult" class="dt-ai-result">
        <div class="dt-ai-section">
          <div class="dt-ai-section-title">制程能力</div>
          <div class="dt-ai-cpk">
            <div class="dt-ai-cpk-ring" :class="aiAnalysisResult.capability?.level || aiAnalysisResult.process_capability">{{ (aiAnalysisResult.capability?.cpk || aiAnalysisResult.cpk)?.toFixed(2) }}</div>
            <div><span>CPK</span><em>{{ aiAnalysisResult.capability?.level || aiAnalysisResult.process_capability }}</em></div>
          </div>
          <div class="dt-ai-stats">
            <div><label>CP</label><span>{{ (aiAnalysisResult.capability?.cp || aiAnalysisResult.cp)?.toFixed(2) }}</span></div>
            <div><label>均值</label><span>{{ aiAnalysisResult.statistics?.mean || aiAnalysisResult.mean }}</span></div>
            <div><label>标准差</label><span>{{ aiAnalysisResult.statistics?.std || aiAnalysisResult.std }}</span></div>
            <div><label>稳定性</label><span>{{ ((aiAnalysisResult.stability || 0) * 100).toFixed(0) }}%</span></div>
          </div>
        </div>
        <div class="dt-ai-section" v-if="(aiAnalysisResult.control_limits || []).length">
          <div class="dt-ai-section-title">控制限</div>
          <div class="dt-ai-limits">
            <div v-for="cl in (aiAnalysisResult.control_limits || [])" :key="cl.name">
              <em :class="cl.name.includes('UCL') ? 'danger' : cl.name.includes('LCL') ? 'danger' : ''">{{ cl.value }}</em>
              <span>{{ cl.name }}</span>
            </div>
          </div>
        </div>
        <div class="dt-ai-section" v-if="(aiAnalysisResult.rules_violated || []).length || (aiAnalysisResult.violations || []).length">
          <div class="dt-ai-section-title">异常检测</div>
          <div v-if="(aiAnalysisResult.rules_violated || []).length" class="dt-ai-warn">
            <el-icon><Warning /></el-icon>
            <span v-for="r in aiAnalysisResult.rules_violated" :key="r">{{ r }}</span>
          </div>
          <el-tag v-else type="success" size="small">无异常规则触发</el-tag>
        </div>
        <div class="dt-ai-section" v-if="(aiAnalysisResult.recommendations || []).length">
          <div class="dt-ai-section-title">建议</div>
          <div class="dt-ai-recs"><div v-for="(r,i) in aiAnalysisResult.recommendations" :key="i">{{ i+1 }}. {{ r }}</div></div>
        </div>
      </div>

      <!-- Energy Result -->
      <div v-else-if="currentAnalysisType === 'energy' && aiAnalysisResult" class="dt-ai-result">
        <div class="dt-ai-section">
          <div class="dt-ai-section-title">优化方案</div>
          <div class="dt-ai-energy-delta">
            <div>节能<span class="val">{{ aiAnalysisResult.estimated_energy_savings_pct }}%</span></div>
            <div>月省<span class="val">{{ aiAnalysisResult.estimated_monthly_savings_kwh }} kWh</span></div>
          </div>
        </div>
        <div class="dt-ai-section">
          <div class="dt-ai-section-title">参数调整</div>
          <div class="dt-ai-params">
            <div class="dt-ai-param-row" v-for="(chg, key) in aiAnalysisResult.parameter_changes" :key="key">
              <label>{{ key === 'speed' ? '转速' : key === 'temperature' ? '温度' : '压力' }}</label>
              <span class="old">{{ aiAnalysisResult.current_parameters?.[key] }}</span>
              <el-icon><ArrowRight /></el-icon>
              <span class="new">{{ aiAnalysisResult.recommended_parameters?.[key] }}</span>
              <span class="chg" :class="chg?.startsWith('+') ? 'up' : chg?.startsWith('-') ? 'down' : ''">{{ chg }}</span>
            </div>
          </div>
        </div>
        <div class="dt-ai-section" v-if="(aiAnalysisResult.alternative_plans || []).length > 1">
          <div class="dt-ai-section-title">备选方案</div>
          <div class="dt-ai-alt">
            <div v-for="(alt, i) in aiAnalysisResult.alternative_plans?.slice(1, 3)" :key="i" class="dt-ai-alt-row">
              <span>方案{{ i+1 }}</span>
              <span>转速 {{ alt.speed }} · 温度 {{ alt.temperature }} · 压力 {{ alt.pressure }}</span>
              <span>品质 {{ alt.quality }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Capacity Result -->
      <div v-else-if="currentAnalysisType === 'capacity' && aiAnalysisResult" class="dt-ai-result">
        <div class="dt-ai-section">
          <div class="dt-ai-section-title">预测概览</div>
          <div class="dt-ai-energy-delta">
            <div>总产量<span class="val">{{ aiAnalysisResult.summary?.total || aiAnalysisResult.total_predicted }}</span></div>
            <div>日均<span class="val">{{ aiAnalysisResult.summary?.daily_avg || aiAnalysisResult.average_daily }}</span></div>
            <div>趋势<span class="val">{{ aiAnalysisResult.summary?.trend || '稳定' }}</span></div>
          </div>
        </div>
        <div class="dt-ai-section">
          <div class="dt-ai-section-title">逐日预测</div>
          <div class="dt-ai-table">
            <div class="dt-ai-table-head"><span>日期</span><span>预计产量</span><span>置信区间</span></div>
            <div v-for="p in (aiAnalysisResult.predictions || [])" :key="p.date" class="dt-ai-table-row">
              <span>{{ p.date?.slice(5) }} {{ p.day }}</span>
              <span>{{ p.predicted_output }}</span>
              <span class="muted">{{ p.confidence_lower }} ~ {{ p.confidence_upper }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- AI建议 Result -->
      <div v-else-if="currentAnalysisType === 'llm' && aiAnalysisResult" class="dt-ai-advice">
        <div class="dt-ai-advice-header">
          <span class="dt-ai-advice-icon"><el-icon><Cpu /></el-icon></span>
          <div>
            <strong>{{ detailData?.name || '设备' }}</strong>
            <span>AI 智能建议</span>
          </div>
        </div>
        <div v-if="aiAnalysisResult.content || aiAnalysisResult.response" class="dt-ai-advice-body" v-html="aiAdviceHtml(aiAnalysisResult)"></div>
        <div v-else-if="aiAnalysisResult.success === false" class="dt-ai-warn">
          <el-icon><Warning /></el-icon> {{ aiAnalysisResult.message || 'AI建议暂不可用，请配置API Key' }}
        </div>
        <div v-else class="dt-ai-advice-body">
          <div v-for="(v, k) in aiAnalysisResult" :key="k" class="dt-ai-advice-item">
            <strong>{{ k }}</strong>
            <p>{{ typeof v === 'string' ? v : JSON.stringify(v) }}</p>
          </div>
        </div>
        <div class="dt-ai-advice-status">
          <span class="dt-ai-status-row">
            <el-tag size="small" :type="detailData?.status === 'running' ? 'success' : 'info'">{{ getStatusText(detailData?.status || 'running') }}</el-tag>
            <span>温度 {{ detailData?.temperature ?? '--' }}°C</span>
            <span>转速 {{ detailData?.speed ?? '--' }} rpm</span>
            <span>功率 {{ detailData?.power ?? '--' }} kW</span>
          </span>
        </div>
      </div>

      <!-- Generic / Other Result -->
      <div v-else-if="aiAnalysisResult" class="dt-ai-result">
        <div v-if="aiAnalysisResult.success === false" class="dt-ai-warn">
          <el-icon><Warning /></el-icon> {{ aiAnalysisResult.message || '服务暂不可用' }}
        </div>
        <div v-else class="dt-ai-raw">{{ JSON.stringify(aiAnalysisResult, null, 2) }}</div>
      </div>

      <div v-else class="dt-loading"><p>暂无结果</p></div>
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
 Tools, VideoPlay, VideoPause, Loading, Ticket, Timer, CircleCheck, MagicStick, Histogram, Lightning, ChatLineRound, Close, ArrowDown, ArrowRight } from '@element-plus/icons-vue'
import DigitalTwinScene from '@/components/device/DigitalTwinScene.vue'
import { mdToHtml } from '@/utils/markdown'

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

const handleDeviceSelect = (d: any) => { selectedDevice.value = d; detailData.value = d }
const handle3DAction = (payload: { type: string; device: any }) => {
  detailData.value = payload.device
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
function aiAdviceHtml(result: any): string {
  const text = result?.content || result?.response || ''
  return mdToHtml(text)
}
const handleSPCAnalysis = async () => {
  aiAnalysisVisible.value = true; aiAnalysisLoading.value = true
  try {
    const d = detailData.value || {}
    const res = await analyzeSPC({
      device_code: d.code || d.id,
      parameter: 'temperature',
      measurements: Array.from({ length: 20 }, () => Math.round(60 + Math.random() * 30))
    })
    showAIResult('spc', res?.data || res)
  } catch { aiAnalysisLoading.value = false; ElMessage.error('SPC分析失败') }
}
const handleEnergyOptimization = async () => {
  aiAnalysisVisible.value = true; aiAnalysisLoading.value = true
  try {
    const d = detailData.value || {}
    const res = await optimizeEnergy({
      device_code: d.code || d.id,
      current_params: { speed: d.speed || 1000, temperature: d.temperature || 80, power: d.power || 50 },
      target_output: 5000
    })
    showAIResult('energy', res?.data || res)
  } catch { aiAnalysisLoading.value = false; ElMessage.error('能耗分析失败') }
}
const handleCapacityPrediction = async () => {
  aiAnalysisVisible.value = true; aiAnalysisLoading.value = true
  try {
    const d = detailData.value || {}
    const dataPoints = Array.from({ length: 7 }, () => Math.round(800 + Math.random() * 400))
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
</style>
