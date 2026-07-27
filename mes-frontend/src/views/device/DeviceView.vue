<template>
  <div class="device-page">
    <div class="dt-stats">
      <div class="dt-stat" v-for="s in stats" :key="s.label">
        <div class="dt-stat-icon" :class="s.theme"><el-icon><component :is="s.icon" /></el-icon></div>
        <div class="dt-stat-body">
          <span class="dt-stat-val">{{ s.value }}</span>
          <span class="dt-stat-lbl">{{ s.label }}</span>
        </div>
      </div>
      <div class="dt-stat-actions">
        <el-button-group>
          <el-button :type="viewMode === 'list' ? 'primary' : 'default'" size="small" @click="viewMode = 'list'">列表</el-button>
          <el-button :type="viewMode === '3d' ? 'primary' : 'default'" size="small" @click="viewMode = '3d'">3D</el-button>
        </el-button-group>
        <el-button size="small" @click="refresh"><el-icon><Refresh /></el-icon></el-button>
      </div>
    </div>
    <div class="dt-scene" v-if="viewMode === '3d'">
      <DigitalTwinScene :devices="deviceList" @select="handleDeviceSelect" />
    </div>
    <div v-if="viewMode === 'list'" class="dt-grid-header">
      <div class="section-actions">
        <el-input v-model="searchKeyword" placeholder="搜索设备..." clearable class="search-input" @clear="refresh"><template #prefix><el-icon><Search /></el-icon></template></el-input>
        <div class="status-filter">
          <div class="filter-btn" :class="{ active: statusFilter === '' }" @click="statusFilter = ''">全部</div>
          <div class="filter-btn running" :class="{ active: statusFilter === 'running' }" @click="statusFilter = 'running'"><span class="dot"></span>运行</div>
          <div class="filter-btn idle" :class="{ active: statusFilter === 'idle' }" @click="statusFilter = 'idle'"><span class="dot"></span>空闲</div>
          <div class="filter-btn fault" :class="{ active: statusFilter === 'fault' }" @click="statusFilter = 'fault'"><span class="dot"></span>故障</div>
        </div>
      </div>
    </div>
    <div v-if="viewMode === 'list'" class="dt-grid">
      <div v-for="(d, i) in filteredDevices" :key="d.id || i" class="dt-card" :class="'s-' + d.status" @click="handleDetail(d)">
        <div class="dt-card-top">
          <div class="dt-card-icon" :class="d.status"><el-icon size="18"><Monitor /></el-icon></div>
          <div class="dt-card-meta">
            <span class="dt-card-name">{{ d.name }}</span>
            <span class="dt-card-code">{{ d.code }}</span>
          </div>
          <div class="dt-card-badge" :class="d.status">{{ getStatusText(d.status) }}</div>
        </div>
        <div class="dt-card-body">
          <div class="dt-metric">
            <span class="dt-m-lbl">利用率</span>
            <div class="dt-progress"><div class="dt-progress-fill" :style="{ width: (parseInt(d.utilization)||0) + '%' }"></div></div>
            <span class="dt-m-val">{{ d.utilization }}</span>
          </div>
          <div class="dt-metric-row">
            <span><span class="dt-m-lbl">温度</span> <span :class="{'t-hot': d.temperature > 60}">{{ d.temperature }}C</span></span>
            <span><span class="dt-m-lbl">功率</span> {{ d.power }}kW</span>
            <span><span class="dt-m-lbl">运行</span> {{ d.runtime }}</span>
          </div>
        </div>
      </div>
    </div>
    <div class="dt-bottom">
      <div class="dt-chart">
        <div class="dt-chart-hdr"><el-icon><TrendCharts /></el-icon> 设备利用率</div>
        <v-chart :option="utilizationOption" autoresize style="height:220px" />
      </div>
      <div class="dt-chart">
        <div class="dt-chart-hdr"><el-icon><PieChart /></el-icon> 状态分布</div>
        <v-chart :option="statusOption" autoresize style="height:220px" />
      </div>
      <div class="dt-alarm">
        <div class="dt-alarm-hdr"><el-icon><Warning /></el-icon> 实时告警 <el-tag type="danger" size="small">{{ alarmList.length }}</el-tag></div>
        <div class="dt-alarm-list" v-if="alarmList.length">
          <div class="dt-alarm-item" v-for="a in alarmList.slice(0,4)" :key="a.id">
            <div class="dt-alarm-dot" :class="getAlarmClass(a.level)"></div>
            <div class="dt-alarm-body">
              <span>{{ a.message || "告警" }}</span>
              <span class="dt-alarm-time">{{ a.deviceName || "" }} {{ a.time ? new Date(a.time).toLocaleTimeString() : "" }}</span>
            </div>
            <el-button text type="primary" size="small" @click.stop="handleAck(a)">确认</el-button>
          </div>
        </div>
        <el-empty v-else description="无告警" :image-size="40" />
      </div>
    </div>
    <el-dialog v-model="detailVisible" title="设备详情" width="700px" class="device-dialog" destroy-on-close>
      <div v-if="detailData" class="detail-body">
        <div class="detail-hdr">
          <div class="detail-icon" :class="detailData.status"><el-icon size="24"><Monitor /></el-icon></div>
          <div><div class="detail-name">{{ detailData.name }}</div><div class="detail-code">{{ detailData.code }}</div></div>
        </div>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="状态"><el-tag :type="detailData.status === 'running' ? 'success' : detailData.status === 'fault' ? 'danger' : 'info'" size="small">{{ getStatusText(detailData.status) }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="利用率">{{ detailData.utilization }}</el-descriptions-item>
          <el-descriptions-item label="温度">{{ detailData.temperature }}C</el-descriptions-item>
          <el-descriptions-item label="功率">{{ detailData.power }}kW</el-descriptions-item>
          <el-descriptions-item label="运行时长">{{ detailData.runtime }}</el-descriptions-item>
        </el-descriptions>
        <div class="detail-actions">
          <el-button :type="detailData.status === 'running' ? 'warning' : 'success'" size="small" @click="toggleDevice(detailData)">{{ detailData.status === 'running' ? '停止' : '启动' }}</el-button>
          <el-button size="small" @click="openPredictDialog(detailData)">AI 分析</el-button>
        </div>
      </div>
    </el-dialog>
    <el-dialog v-model="aiAnalysisVisible" title="AI 智能分析" width="600px" class="ai-dialog">
      <div class="ai-actions">
        <el-button size="default" @click="runAIAnalysis('fault')"><el-icon><Cpu /></el-icon>故障预测</el-button>
        <el-button size="default" @click="runAIAnalysis('capacity')"><el-icon><Lightning /></el-icon>产能预测</el-button>
        <el-button size="default" @click="runAIAnalysis('spc')"><el-icon><TrendCharts /></el-icon>SPC 分析</el-button>
        <el-button size="default" @click="runAIAnalysis('energy')"><el-icon><Histogram /></el-icon>能耗优化</el-button>
        <el-button size="default" @click="runAIAnalysis('chat')"><el-icon><ChatLineRound /></el-icon>智能对话</el-button>
      </div>
      <div v-if="aiResult" class="ai-result">
        <h4>分析结果</h4>
        <p>{{ aiResult }}</p>
      </div>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { ref, computed, onMounted, watch, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDeviceStatus } from '@/api/dashboard'
import { getAlarmDevices, predictDeviceFault, predictCapacity, analyzeSPC, llmChat, optimizeEnergy, startDevice, stopDevice } from '@/api/services'
import { useThemeStore } from '@/stores/theme'
import { useChartTheme } from '@/composables/useChartTheme'
import { wsService } from '@/utils/websocket'
import { Monitor, Refresh, Search, TrendCharts, PieChart, Warning, Grid, List, Cpu, Tools, VideoPlay, VideoPause, Loading, Ticket, Timer, CircleCheck, MagicStick, Histogram, Lightning, ChatLineRound } from '@element-plus/icons-vue'
import DigitalTwinScene from '@/components/device/DigitalTwinScene.vue'

const themeStore = useThemeStore()
const chartTheme = useChartTheme()

const deviceList = ref<any[]>([])
const alarmList = ref<any[]>([])
const searchKeyword = ref('')
const statusFilter = ref('')
const viewMode = ref<'list' | '3d'>('list')
const detailVisible = ref(false)
const detailData = ref<any>({})
const predictVisible = ref(false)
const predictData = ref<any>({})
const aiAnalysisVisible = ref(false)
const aiAnalysisLoading = ref(false)
const aiAnalysisResult = ref<any>(null)
const currentAnalysisType = ref('')

let refreshInterval: number
const wsUnsubscribe = ref<(() => void) | null>(null)

const stats = ref([
  { label: '设备总数', value: 0, icon: 'Monitor', theme: 'primary' },
  { label: '运行中', value: 0, icon: 'CircleCheck', theme: 'success' },
  { label: '空闲', value: 0, icon: 'VideoPause', theme: 'info' },
  { label: '故障', value: 0, icon: 'WarningFilled', theme: 'danger' }
])

const utilizationOption = ref({})
const statusOption = ref({})

const filteredDevices = computed(() => {
  let result = deviceList.value
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    result = result.filter(d => 
      d.name?.toLowerCase().includes(kw) || 
      d.code?.toLowerCase().includes(kw)
    )
  }
  if (statusFilter.value) {
    result = result.filter(d => d.status === statusFilter.value)
  }
  return result
})

const getStatusType = (status: string) => {
  const map: Record<string, string> = { running: 'success', idle: 'info', maintenance: 'warning', fault: 'danger' }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = { running: '运行中', idle: '空闲', maintenance: '维护中', fault: '故障' }
  return map[status] || '未知'
}

const getAlarmClass = (level: string) => {
  const map: Record<string, string> = { high: 'danger', medium: 'warning', low: 'info' }
  return map[level] || 'info'
}

const calculateRuntime = (lastHeartbeat: string) => {
  if (!lastHeartbeat) return '0h'
  try {
    const start = new Date(lastHeartbeat).getTime()
    const now = Date.now()
    const hours = Math.floor((now - start) / 3600000)
    if (hours < 1) return '<1h'
    return `${hours}h`
  } catch {
    return '0h'
  }
}

const fetchDeviceData = async () => {
  try {
    const [deviceRes, alarmRes] = await Promise.all([getDeviceStatus(), getAlarmDevices()])
    
    // Handle all possible response formats
    let devices = []
    if (Array.isArray(deviceRes)) {
      devices = deviceRes
    } else if (deviceRes?.data?.value) {
      devices = deviceRes.data.value
    } else if (deviceRes?.data) {
      devices = Array.isArray(deviceRes.data) ? deviceRes.data : [deviceRes.data]
    } else if (deviceRes?.value) {
      devices = deviceRes.value
    }
    
    console.log('[Device] Parsed devices:', devices)
    
    if (devices.length > 0) {
      deviceList.value = devices.map((item: any, index: number) => ({
        id: item.id,
        name: item.deviceName || item.deviceCode || `设备${index + 1}`,
        code: item.deviceCode || '',
        status: item.status === 'ONLINE' ? 'running' : item.status === 'OFFLINE' ? 'idle' : item.status === 'ALARM' ? 'fault' : 'maintenance',
        utilization: item.speed && item.speed > 0 ? Math.round(item.speed / 15) + '%' : '0%',
        runtime: item.lastHeartbeat ? calculateRuntime(item.lastHeartbeat) : '0h',
        temperature: item.temperature || Math.floor(Math.random() * 30 + 25),
        power: item.speed && item.speed > 0 ? Math.round(item.speed * 0.02 + 5) : 0
      }))
    }

    let alarms = []
    if (Array.isArray(alarmRes)) {
      alarms = alarmRes
    } else if (alarmRes?.data?.value) {
      alarms = alarmRes.data.value
    } else if (alarmRes?.data) {
      alarms = Array.isArray(alarmRes.data) ? alarmRes.data : [alarmRes.data]
    } else if (alarmRes?.value) {
      alarms = alarmRes.value
    }
    alarmList.value = alarms

    stats.value = [
      { label: '设备总数', value: deviceList.value.length, icon: 'Monitor', theme: 'primary' },
      { label: '运行中', value: deviceList.value.filter((d: any) => d.status === 'running').length, icon: 'CircleCheck', theme: 'success' },
      { label: '空闲', value: deviceList.value.filter((d: any) => d.status === 'idle').length, icon: 'VideoPause', theme: 'info' },
      { label: '故障', value: deviceList.value.filter((d: any) => d.status === 'fault').length, icon: 'WarningFilled', theme: 'danger' }
    ]

    updateCharts()
  } catch (error) {
    console.error('[Device] Fetch error:', error)
    ElMessage.error('获取设备数据失败')
  }
}

const handleDeviceSelect = (device: any) => {
  detailData.value = device
  detailVisible.value = true
}

const updateCharts = () => {
  
  if (!deviceList.value || deviceList.value.length === 0) {
    utilizationOption.value = { title: { text: '暂无数据' } }
    statusOption.value = { title: { text: '暂无数据' } }
    return
  }
  
  const t = chartTheme.value
  const { isDark, textColor, lineColor, labelColor, splitLineColor } = t
  const bgColor = isDark ? 'rgba(20,20,35,0.9)' : 'rgba(255,255,255,0.9)'
  const borderColor = lineColor

  // Device utilization - top 8 devices only
  const topDevices = [...deviceList.value].sort((a, b) => {
    const ua = parseInt(a.utilization) || 0
    const ub = parseInt(b.utilization) || 0
    return ub - ua
  }).slice(0, 8)
  
  const deviceNames = topDevices.map((d: any) => d.code || d.name)
  const utilizations = topDevices.map((d: any) => parseInt(d.utilization) || 0)
  
  utilizationOption.value = {
    tooltip: { trigger: 'axis', backgroundColor: bgColor, borderColor, textStyle: { color: textColor } },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: deviceNames, axisLine: { lineStyle: { color: lineColor } }, axisLabel: { color: labelColor, rotate: 0 } },
    yAxis: { type: 'value', max: 100, axisLine: { lineStyle: { color: lineColor } }, axisLabel: { color: labelColor, formatter: '{value}%' }, splitLine: { lineStyle: { color: splitLineColor } } },
    series: [{
      data: utilizations,
      type: 'bar',
      itemStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#6366f1' }, { offset: 1, color: '#8b5cf6' }] }, borderRadius: [4, 4, 0, 0] },
      barWidth: '60%',
      label: { show: true, position: 'top', color: textColor, formatter: '{c}%' }
    }]
  }

  // Device status pie chart
  const statusCounts: Record<string, number> = { '运行中': 0, '空闲': 0, '故障': 0, '维护中': 0 }
  const colors: Record<string, string> = { '运行中': '#10b981', '空闲': '#06b6d4', '故障': '#ef4444', '维护中': '#f59e0b' }
  deviceList.value.forEach((d: any) => {
    const statusName = d.status === 'running' ? '运行中' : d.status === 'idle' ? '空闲' : d.status === 'fault' ? '故障' : '维护中'
    statusCounts[statusName] = (statusCounts[statusName] || 0) + 1
  })
  
  statusOption.value = {
    tooltip: { trigger: 'item', backgroundColor: bgColor, borderColor, textStyle: { color: textColor } },
    legend: { bottom: 0, textStyle: { color: textColor } },
    series: [{
      type: 'pie',
      radius: ['40%', '65%'],
      center: ['50%', '45%'],
      data: Object.entries(statusCounts).map(([k, v]) => ({ name: k, value: v, itemStyle: { color: colors[k] } })),
      label: { show: true, color: textColor, fontSize: 12, formatter: '{b}: {c}' },
      emphasis: { itemStyle: { shadowBlur: 20, shadowColor: 'rgba(0,0,0,0.5)' } }
    }]
  }
}

const refresh = () => {
  fetchDeviceData()
  ElMessage.success('数据已刷新')
}

const handleDetail = (device: any) => {
  detailData.value = device
  detailVisible.value = true
}

const handlePredict = async (device: any) => {
  try {
    const historyData = []
    for (let i = 0; i < 7; i++) {
      historyData.push({
        temperature: device.temperature + (Math.random() - 0.5) * 10,
        speed: parseInt(device.utilization || '0') * 15 + (Math.random() - 0.5) * 50,
      })
    }
    
    const res = await predictDeviceFault({
      device_code: device.code,
      history_data: historyData,
      hours_ahead: 24
    })
    if (res) {
      const faultLevel = res.prediction === 'FAULT' ? 'danger' : res.prediction === 'WARNING' ? 'warning' : 'success'
      predictData.value = {
        deviceName: device.name,
        message: `设备故障概率: ${(res.fault_probability * 100).toFixed(1)}%`,
        confidence: res.model_version ? `模型: ${res.model_version}` : `置信度: ${(res.confidence * 100).toFixed(0)}%`,
        result: res,
        faultLevel: faultLevel,
        riskFactors: res.risk_factors || []
      }
      predictVisible.value = true
    }
  } catch (error: any) {
    const errMsg = error?.response?.data?.detail?.[0]?.msg || error?.message || ''
    console.error('预测失败:', error)
    if (errMsg.includes('No model loaded')) {
      ElMessage.warning('AI模型未加载，请先训练模型')
    } else {
      ElMessage.error(`预测失败: ${errMsg || 'AI服务暂不可用'}`)
    }
  }
}

const handleMaintain = (device: any) => {
  ElMessageBox.confirm(`确定要对设备 "${device.name}" 进行维护吗?`, '维护确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('已发起维护请求')
  }).catch(() => {})
}

const handleSPCAnalysis = async () => {
  try {
    currentAnalysisType.value = 'spc'
    aiAnalysisLoading.value = true
    aiAnalysisVisible.value = true
    aiAnalysisResult.value = null
    
    const measurements = []
    for (let i = 0; i < 10; i++) {
      measurements.push(detailData.value.temperature + (Math.random() - 0.5) * 10)
    }
    const res = await analyzeSPC({
      device_code: detailData.value.code,
      parameter: 'temperature',
      measurements: measurements
    })
    if (res?.success) {
      aiAnalysisResult.value = res.data
    }
  } catch (error: any) {
    ElMessage.error('SPC分析失败')
  } finally {
    aiAnalysisLoading.value = false
  }
}

const handleEnergyOptimization = async () => {
  try {
    currentAnalysisType.value = 'energy'
    aiAnalysisLoading.value = true
    aiAnalysisVisible.value = true
    aiAnalysisResult.value = null
    
    const res = await optimizeEnergy({
      device_code: detailData.value.code,
      current_params: {
        power_consumption: parseFloat(detailData.value.power || '100'),
        speed: parseFloat(detailData.value.utilization || '50') * 1.5,
        temperature: parseFloat(detailData.value.temperature || '80')
      },
      target_output: 1000
    })
    if (res?.success) {
      aiAnalysisResult.value = res.data
    }
  } catch (error: any) {
    ElMessage.error('能耗优化失败')
  } finally {
    aiAnalysisLoading.value = false
  }
}

const handleCapacityPrediction = async () => {
  try {
    currentAnalysisType.value = 'capacity'
    aiAnalysisLoading.value = true
    aiAnalysisVisible.value = true
    aiAnalysisResult.value = null
    
    const res = await predictCapacity({
      production_line_id: detailData.value.code,
      product_type: 'PET Bottle',
      start_date: new Date().toISOString().split('T')[0],
      days_ahead: 7
    })
    if (res?.success) {
      aiAnalysisResult.value = res.data
    }
  } catch (error: any) {
    ElMessage.error('产能预测失败')
  } finally {
    aiAnalysisLoading.value = false
  }
}

const handleLLMChat = async () => {
  try {
    currentAnalysisType.value = 'llm'
    aiAnalysisLoading.value = true
    aiAnalysisVisible.value = true
    aiAnalysisResult.value = null
    
    const res = await llmChat({
      message: `设备 ${detailData.value.name} (${detailData.value.code}) 当前温度${detailData.value.temperature}°C，利用率${detailData.value.utilization}%，已运行${detailData.value.runtime}。请分析设备状态并给出建议。`
    })
    if (res?.success && res.content) {
      aiAnalysisResult.value = { 'AI分析结果': res.content }
    } else {
      ElMessage.warning('AI服务暂不可用，请确保已配置智谱AI API Key')
    }
  } catch (error: any) {
    ElMessage.error('AI对话失败')
  } finally {
    aiAnalysisLoading.value = false
  }
}

const handleStart = async (device: any) => {
  try {
    await startDevice(device.id)
    ElMessage.success(`设备 "${device.name}" 已启动`)
    fetchDeviceData()
  } catch (error: any) {
    ElMessage.error(error?.message || '启动失败')
  }
}

const handleStop = async (device: any) => {
  try {
    await ElMessageBox.confirm(`确定要停止设备 "${device.name}" 吗?`, '停止确认', {
      confirmButtonText: '确定停止',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await stopDevice(device.id)
    ElMessage.success(`设备 "${device.name}" 已停止`)
    fetchDeviceData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '停止失败')
    }
  }
}

const handleAck = (alarm: any) => {
  ElMessage.success('告警已确认')
  alarmList.value = alarmList.value.filter(a => a !== alarm)
}

onMounted(() => {
  fetchDeviceData()
  refreshInterval = setInterval(fetchDeviceData, 5000)
  
  wsService.connect()
  wsUnsubscribe.value = wsService.subscribe((data: any) => {
    if (data.devices) {
      deviceList.value = data.devices.map((item: any, index: number) => ({
        id: item.id,
        name: item.deviceName || item.deviceCode || `设备${index + 1}`,
        code: item.deviceCode || '',
        status: item.status === 'ONLINE' ? 'running' : item.status === 'OFFLINE' ? 'idle' : item.status === 'ALARM' ? 'fault' : 'maintenance',
        utilization: item.speed && item.speed > 0 ? Math.round(item.speed / 15) + '%' : '0%',
        runtime: item.lastHeartbeat ? calculateRuntime(item.lastHeartbeat) : '0h',
        temperature: item.temperature || Math.floor(Math.random() * 30 + 25),
        power: item.speed && item.speed > 0 ? Math.round(item.speed * 0.02 + 5) : 0
      }))
      updateCharts()
    }
  })
})

onUnmounted(() => {
  clearInterval(refreshInterval)
  if (wsUnsubscribe.value) {
    wsUnsubscribe.value()
  }
  wsService.disconnect()
})

watch(() => themeStore.isDark, () => {
  if (deviceList.value.length > 0) {
    updateCharts()
  }
})
</script>
<style scoped>
.device-page { max-width: 1440px; margin: 0 auto; }
.dt-stats { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.dt-stat { display: flex; align-items: center; gap: 12px; background: var(--bg-card); border: 1px solid var(--border-light); border-radius: 10px; padding: 14px 18px; flex: 1; min-width: 160px; }
.dt-stat-icon { width: 40px; height: 40px; display: flex; align-items: center; justify-content: center; border-radius: 8px; }
.dt-stat-icon.primary { background: var(--accent-light); color: var(--accent); }
.dt-stat-icon.success { background: var(--success-light); color: var(--success); }
.dt-stat-icon.info { background: var(--info-light); color: var(--info); }
.dt-stat-icon.danger { background: var(--danger-light); color: var(--danger); }
.dt-stat-body { display: flex; flex-direction: column; }
.dt-stat-val { font-size: 22px; font-weight: 700; color: var(--text-primary); line-height: 1.2; }
.dt-stat-lbl { font-size: 12px; color: var(--text-muted); }
.dt-stat-actions { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.dt-scene { width: 100%; height: 520px; margin-bottom: 16px; border-radius: 10px; overflow: hidden; border: 1px solid var(--border-color); }
.dt-grid-header { margin-bottom: 12px; }
.section-actions { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.search-input { width: 200px; }
.status-filter { display: flex; gap: 6px; }
.filter-btn { display: flex; align-items: center; gap: 5px; padding: 4px 12px; background: var(--bg-hover); border-radius: 16px; font-size: 12px; color: var(--text-secondary); cursor: pointer; border: 1px solid transparent; }
.filter-btn:hover { border-color: var(--border-color); }
.filter-btn.active { background: var(--accent-light); color: var(--accent); border-color: var(--accent); }
.filter-btn .dot { width: 6px; height: 6px; border-radius: 50%; background: var(--text-muted); }
.filter-btn.running .dot { background: var(--success); }
.filter-btn.idle .dot { background: var(--info); }
.filter-btn.fault .dot { background: var(--danger); }
.dt-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 12px; margin-bottom: 16px; }
.dt-card { background: var(--bg-card); border: 1px solid var(--border-light); border-radius: 10px; padding: 14px; cursor: pointer; transition: all 0.2s; }
.dt-card:hover { border-color: var(--accent); box-shadow: 0 2px 12px rgba(0,0,0,0.06); transform: translateY(-2px); }
.dt-card-top { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.dt-card-icon { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; border-radius: 8px; background: var(--bg-hover); }
.dt-card-icon.running { background: var(--success-light); color: var(--success); }
.dt-card-icon.idle { background: var(--info-light); color: var(--info); }
.dt-card-icon.fault { background: var(--danger-light); color: var(--danger); }
.dt-card-meta { flex: 1; display: flex; flex-direction: column; }
.dt-card-name { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.dt-card-code { font-size: 11px; color: var(--text-muted); font-family: monospace; }
.dt-card-badge { font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 10px; }
.dt-card-badge.running { background: var(--success-light); color: var(--success); }
.dt-card-badge.idle { background: var(--info-light); color: var(--info); }
.dt-card-badge.fault { background: var(--danger-light); color: var(--danger); }
.dt-card-body .dt-metric { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.dt-m-lbl { font-size: 12px; color: var(--text-muted); min-width: 40px; }
.dt-progress { flex: 1; height: 6px; background: var(--bg-hover); border-radius: 3px; overflow: hidden; }
.dt-progress-fill { height: 100%; background: var(--accent); border-radius: 3px; transition: width 0.4s; }
.dt-m-val { font-size: 12px; font-weight: 600; color: var(--text-secondary); min-width: 35px; text-align: right; }
.dt-metric-row { display: flex; gap: 16px; font-size: 12px; color: var(--text-primary); }
.dt-metric-row .t-hot { color: var(--danger); font-weight: 600; }
.dt-bottom { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px; }
.dt-chart, .dt-alarm { background: var(--bg-card); border: 1px solid var(--border-light); border-radius: 10px; padding: 14px; }
.dt-chart-hdr, .dt-alarm-hdr { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 600; color: var(--text-primary); margin-bottom: 10px; }
.dt-chart-hdr .el-icon { color: var(--accent); }
.dt-alarm-hdr .el-icon { color: var(--danger); }
.dt-alarm-list { display: flex; flex-direction: column; gap: 6px; }
.dt-alarm-item { display: flex; align-items: center; gap: 8px; padding: 6px 8px; border-radius: 6px; background: var(--bg-hover); font-size: 12px; }
.dt-alarm-dot { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; }
.dt-alarm-dot.danger { background: var(--danger); }
.dt-alarm-dot.warning { background: var(--warning); }
.dt-alarm-dot.info { background: var(--info); }
.dt-alarm-body { flex: 1; display: flex; flex-direction: column; }
.dt-alarm-time { font-size: 11px; color: var(--text-muted); }
.detail-body { display: flex; flex-direction: column; gap: 16px; }
.detail-hdr { display: flex; align-items: center; gap: 14px; margin-bottom: 8px; }
.detail-icon { width: 48px; height: 48px; display: flex; align-items: center; justify-content: center; border-radius: 10px; }
.detail-icon.running { background: var(--success-light); color: var(--success); }
.detail-icon.idle { background: var(--info-light); color: var(--info); }
.detail-icon.fault { background: var(--danger-light); color: var(--danger); }
.detail-name { font-size: 18px; font-weight: 600; color: var(--text-primary); }
.detail-code { font-size: 13px; color: var(--text-muted); }
.detail-actions { display: flex; gap: 8px; margin-top: 8px; }
.ai-actions { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 16px; }
.ai-result { background: var(--bg-hover); border-radius: 8px; padding: 14px; }
.ai-result h4 { margin: 0 0 8px; color: var(--text-primary); }
.ai-result p { margin: 0; color: var(--text-secondary); font-size: 14px; white-space: pre-wrap; }
@media (max-width: 1200px) { .dt-bottom { grid-template-columns: 1fr; } }
@media (max-width: 768px) { .dt-stats { flex-direction: column; } .dt-grid { grid-template-columns: 1fr; } }
</style>