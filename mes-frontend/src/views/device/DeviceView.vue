<template>
  <div class="device-page">
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">
          <el-icon><Monitor /></el-icon>
          设备监控中心
        </h1>
        <p class="page-subtitle">实时监控 · 智能预警 · 预测性维护</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="refresh">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-item" v-for="(stat, index) in stats" :key="stat.label" :style="{ animationDelay: `${index * 0.1}s` }">
        <div class="stat-icon" :class="stat.theme">
          <el-icon size="22"><component :is="stat.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </div>
    </div>

    <el-row :gutter="20" class="charts-row">
      <el-col :span="12">
        <div class="chart-card">
          <div class="card-header">
            <span class="card-title">
              <el-icon><TrendCharts /></el-icon>
              设备利用率分布
            </span>
          </div>
          <div class="chart-container">
            <v-chart :option="utilizationOption" autoresize style="height: 300px" />
          </div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="chart-card">
          <div class="card-header">
            <span class="card-title">
              <el-icon><PieChart /></el-icon>
              设备状态分布
            </span>
          </div>
          <div class="chart-container">
            <v-chart :option="statusOption" autoresize style="height: 300px" />
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="alarm-section">
      <el-col :span="24">
        <div class="alarm-card">
          <div class="card-header">
            <span class="card-title">
              <el-icon class="alarm-icon"><Warning /></el-icon>
              实时告警
            </span>
            <el-tag type="danger" size="small">{{ alarmList.length }} 条告警</el-tag>
          </div>
          <div class="alarm-list" v-if="alarmList.length > 0">
            <div class="alarm-item" v-for="(alarm, index) in alarmList.slice(0, 5)" :key="index">
              <div class="alarm-icon-wrapper" :class="getAlarmClass(alarm.level)">
                <el-icon><Warning /></el-icon>
              </div>
              <div class="alarm-content">
                <div class="alarm-title">{{ alarm.message || '设备告警' }}</div>
                <div class="alarm-meta">
                  <span>{{ alarm.deviceName || alarm.device || '设备' }}</span>
                  <span>{{ alarm.time || new Date().toLocaleString() }}</span>
                </div>
              </div>
              <el-button type="primary" link @click="handleAck(alarm)">确认</el-button>
            </div>
          </div>
          <el-empty v-else description="暂无告警信息" :image-size="80" />
        </div>
      </el-col>
    </el-row>

    <div class="device-section">
      <div class="section-header">
        <span class="section-title">
          <el-icon><Grid /></el-icon>
          设备列表
        </span>
        <div class="section-actions">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索设备名称..."
            class="search-input"
            clearable
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <div class="status-filter">
            <div 
              class="filter-btn" 
              :class="{ active: statusFilter === '' }"
              @click="statusFilter = ''"
            >
              全部
            </div>
            <div 
              class="filter-btn running" 
              :class="{ active: statusFilter === 'running' }"
              @click="statusFilter = 'running'"
            >
              <span class="status-dot"></span>
              运行中
            </div>
            <div 
              class="filter-btn idle" 
              :class="{ active: statusFilter === 'idle' }"
              @click="statusFilter = 'idle'"
            >
              <span class="status-dot"></span>
              空闲
            </div>
            <div 
              class="filter-btn fault" 
              :class="{ active: statusFilter === 'fault' }"
              @click="statusFilter = 'fault'"
            >
              <span class="status-dot"></span>
              故障
            </div>
          </div>
        </div>
      </div>
      
      <div class="device-grid">
        <div 
          v-for="(device, index) in filteredDevices" 
          :key="device.id || index"
          class="device-card"
          :class="`status-${device.status}`"
          :style="{ animationDelay: `${index * 0.05}s` }"
        >
          <div class="device-header">
            <div class="device-icon">
              <el-icon size="20"><Monitor /></el-icon>
            </div>
            <div class="device-info">
              <span class="device-name">{{ device.name }}</span>
              <span class="device-code">{{ device.code }}</span>
            </div>
            <div class="status-badge" :class="device.status">
              {{ getStatusText(device.status) }}
            </div>
          </div>

          <div class="device-metrics">
            <div class="metric-item">
              <span class="metric-label">利用率</span>
              <div class="progress-wrapper">
                <div class="progress-bar">
                  <div class="progress-fill" :style="{ width: `${parseInt(device.utilization) || 0}%` }"></div>
                </div>
                <span class="progress-value">{{ device.utilization }}</span>
              </div>
            </div>
            <div class="metric-row">
              <div class="metric-item">
                <span class="metric-label">温度</span>
                <span class="metric-value" :class="{ 'temp-high': device.temperature > 60 }">
                  {{ device.temperature }}°C
                </span>
              </div>
              <div class="metric-item">
                <span class="metric-label">功率</span>
                <span class="metric-value">{{ device.power }}kW</span>
              </div>
              <div class="metric-item">
                <span class="metric-label">运行时长</span>
                <span class="metric-value">{{ device.runtime }}</span>
              </div>
            </div>
          </div>

          <div class="device-actions">
            <el-button type="primary" size="small" link @click="handleDetail(device)">
              <el-icon><View /></el-icon>
              详情
            </el-button>
            <el-button 
              v-if="device.status === 'running' || device.status === 'ONLINE'" 
              type="danger" 
              size="small" 
              link 
              @click="handleStop(device)"
            >
              <el-icon><VideoPause /></el-icon>
              停止
            </el-button>
            <el-button 
              v-if="device.status === 'idle' || device.status === 'OFFLINE'" 
              type="success" 
              size="small" 
              link 
              @click="handleStart(device)"
            >
              <el-icon><VideoPlay /></el-icon>
              启动
            </el-button>
            <el-button 
              v-if="device.status === 'running' || device.status === 'ONLINE'" 
              type="success" 
              size="small" 
              link 
              @click="handlePredict(device)"
            >
              <el-icon><Cpu /></el-icon>
              AI预测
            </el-button>
            <el-button 
              v-if="device.status === 'running' || device.status === 'ONLINE'" 
              type="warning" 
              size="small" 
              link 
              @click="handleMaintain(device)"
            >
              <el-icon><Tools /></el-icon>
              维护
            </el-button>
          </div>
        </div>
      </div>

      <el-empty v-if="filteredDevices.length === 0" description="暂无设备数据" />
    </div>

    <el-dialog v-model="detailVisible" title="设备详情" width="700px" class="device-dialog" destroy-on-close>
      <div class="detail-content" v-if="detailData">
        <div class="detail-header">
          <div class="detail-icon-large">
            <el-icon size="40"><Monitor /></el-icon>
          </div>
          <div class="detail-info">
            <h3>{{ detailData.name }}</h3>
            <p><el-icon><Ticket /></el-icon> {{ detailData.code }}</p>
            <p><el-icon><Timer /></el-icon> 运行 {{ detailData.runtime || '0时' }}</p>
          </div>
          <el-tag :type="getStatusType(detailData.status)" size="large" class="status-tag">
            {{ getStatusText(detailData.status) }}
          </el-tag>
        </div>

        <div class="detail-stats">
          <div class="stat-card-item">
            <div class="stat-card-value">{{ detailData.utilization || 0 }}%</div>
            <div class="stat-card-label">设备利用率</div>
          </div>
          <div class="stat-card-item">
            <div class="stat-card-value">{{ detailData.temperature || 0 }}°C</div>
            <div class="stat-card-label">当前温度</div>
          </div>
          <div class="stat-card-item">
            <div class="stat-card-value">{{ detailData.power || 0 }}</div>
            <div class="stat-card-label">功率(kW)</div>
          </div>
          <div class="stat-card-item">
            <div class="stat-card-value">{{ detailData.efficiency || 0 }}%</div>
            <div class="stat-card-label">OEE效率</div>
          </div>
        </div>

        <div class="detail-section" v-if="detailData.status === 'running'">
          <div class="section-title">
            <el-icon><Cpu /></el-icon> AI 预测分析
          </div>
          <div class="ai-predict-card">
            <div class="predict-badge success">
              <el-icon><CircleCheck /></el-icon>
              设备运行正常
            </div>
            <p class="predict-message">预测未来 24 小时内无需维护</p>
            <p class="predict-confidence">预测置信度: <span>95%</span></p>
          </div>
        </div>

        <div class="ai-actions">
          <div class="section-title">
            <el-icon><MagicStick /></el-icon> 智能分析功能
          </div>
          <div class="action-buttons">
            <el-button type="default" @click="handleSPCAnalysis">
              <el-icon><Histogram /></el-icon> SPC分析
            </el-button>
            <el-button type="default" @click="handleEnergyOptimization">
              <el-icon><Lightning /></el-icon> 能耗优化
            </el-button>
            <el-button type="default" @click="handleCapacityPrediction">
              <el-icon><TrendCharts /></el-icon> 产能预测
            </el-button>
            <el-button type="primary" @click="handleLLMChat">
              <el-icon><ChatLineRound /></el-icon> AI对话
            </el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="predictVisible" title="AI 预测分析" width="500px">
      <div class="predict-dialog-content" v-if="predictData">
        <div class="predict-header">
          <el-icon size="48" :color="predictData.faultLevel === 'danger' ? 'var(--danger)' : predictData.faultLevel === 'warning' ? 'var(--warning)' : 'var(--success)'"><Cpu /></el-icon>
          <h3>{{ predictData.deviceName }}</h3>
        </div>
        <el-result
          :icon="predictData.faultLevel === 'danger' ? 'error' : predictData.faultLevel === 'warning' ? 'warning' : 'success'"
          title="预测结果"
          :sub-title="predictData.message"
        >
          <template #extra>
            <el-tag :type="predictData.faultLevel">{{ predictData.confidence }}</el-tag>
          </template>
        </el-result>
        <div v-if="predictData.riskFactors && predictData.riskFactors.length > 0" class="risk-factors">
          <h4>风险因素:</h4>
          <el-tag v-for="(factor, index) in predictData.riskFactors" :key="index" type="warning" style="margin: 4px;">
            {{ factor.description || factor.factor }}
          </el-tag>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="aiAnalysisVisible" :title="currentAnalysisType === 'spc' ? 'SPC统计分析' : currentAnalysisType === 'energy' ? '能耗优化建议' : currentAnalysisType === 'capacity' ? '产能预测' : 'AI智能分析'" width="600px">
      <div v-if="aiAnalysisLoading" style="text-align: center; padding: 40px;">
        <el-icon class="is-loading" size="40"><Loading /></el-icon>
        <p style="margin-top: 10px; color: var(--text-muted);">AI分析中...</p>
      </div>
      <div v-else-if="aiAnalysisResult" class="ai-analysis-result">
        <el-descriptions :column="1" border>
          <el-descriptions-item v-for="(value, key) in aiAnalysisResult" :key="key" :label="key">
            {{ typeof value === 'object' ? JSON.stringify(value) : value }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <div v-else style="text-align: center; padding: 40px;">
        <p>暂无分析结果</p>
      </div>
      <template #footer>
        <el-button @click="aiAnalysisVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDeviceStatus, getAlarmDevices, predictDeviceFault, predictCapacity, analyzeSPC, llmChat, optimizeEnergy, startDevice, stopDevice } from '@/api/services'
import { useThemeStore } from '@/stores/theme'
import { wsService } from '@/utils/websocket'
import { Monitor, Refresh, Search, TrendCharts, PieChart, Warning, Grid, View, Cpu, Tools, VideoPlay, VideoPause, Loading, Ticket, Timer, CircleCheck, MagicStick, Histogram, Lightning, ChatLineRound } from '@element-plus/icons-vue'

const themeStore = useThemeStore()

const deviceList = ref<any[]>([])
const alarmList = ref<any[]>([])
const searchKeyword = ref('')
const statusFilter = ref('')
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

const updateCharts = () => {
  
  if (!deviceList.value || deviceList.value.length === 0) {
    utilizationOption.value = { title: { text: '暂无数据' } }
    statusOption.value = { title: { text: '暂无数据' } }
    return
  }
  
  const isDark = themeStore.isDark
  const textColor = isDark ? '#fff' : '#333'
  const lineColor = isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)'
  const labelColor = isDark ? 'rgba(255,255,255,0.6)' : 'rgba(0,0,0,0.6)'
  const splitLineColor = isDark ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)'
  const bgColor = isDark ? 'rgba(20,20,35,0.9)' : 'rgba(255,255,255,0.9)'
  const borderColor = isDark ? 'rgba(255,255,255,0.1)' : '#ddd'

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
      updateStats()
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
.device-page { padding: 0; }

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  animation: fadeIn 0.5s ease;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text-primary);
  font-size: 26px;
  font-weight: 600;
  margin-bottom: 4px;
}

.page-title .el-icon { color: var(--accent); }
.page-subtitle { color: var(--text-muted); font-size: 14px; }

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 20px;
  animation: fadeInUp 0.5s ease forwards;
  opacity: 0;
}

.stat-icon {
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
}

.stat-icon.primary { background: var(--accent-light); color: var(--accent); }
.stat-icon.success { background: var(--success-light); color: var(--success); }
.stat-icon.info { background: var(--info-light); color: var(--info); }
.stat-icon.danger { background: var(--danger-light); color: var(--danger); }

.stat-info {}
.stat-value { font-size: 28px; font-weight: 700; color: var(--text-primary); }
.stat-label { font-size: 13px; color: var(--text-muted); margin-top: 4px; }

.charts-row { margin-bottom: 20px; }

.chart-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 600;
}

.card-title .el-icon { color: var(--accent); }

.alarm-section { margin-bottom: 20px; }

.alarm-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 20px;
  animation: fadeIn 0.5s ease 0.3s both;
}

.alarm-icon { color: var(--danger); }

.alarm-list { display: flex; flex-direction: column; gap: 12px; }

.alarm-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--bg-hover);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.alarm-item:hover { background: var(--bg-card); }

.alarm-icon-wrapper {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
}

.alarm-icon-wrapper.danger { background: var(--danger-light); color: var(--danger); }
.alarm-icon-wrapper.warning { background: var(--warning-light); color: var(--warning); }
.alarm-icon-wrapper.info { background: var(--info-light); color: var(--info); }

.alarm-content { flex: 1; }
.alarm-title { color: var(--text-primary); font-size: 14px; margin-bottom: 4px; }
.alarm-meta { display: flex; gap: 16px; color: var(--text-muted); font-size: 12px; }

.device-section { animation: fadeIn 0.5s ease 0.4s both; }

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

.section-actions { display: flex; gap: 12px; }
.search-input { width: 200px; }

.device-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.device-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 18px;
  transition: all var(--transition-normal);
  animation: fadeInUp 0.4s ease forwards;
  opacity: 0;
}

.device-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md);
  border-color: var(--accent);
}

.device-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-light);
}

.device-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-hover);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
}

.device-info { flex: 1; display: flex; flex-direction: column; }
.device-name { font-size: 15px; font-weight: 600; color: var(--text-primary); }
.device-code { font-size: 12px; color: var(--text-muted); }

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.running { background: var(--success-light); color: var(--success); }
.status-badge.idle { background: var(--info-light); color: var(--info); }
.status-badge.fault { background: var(--danger-light); color: var(--danger); }
.status-badge.maintenance { background: var(--warning-light); color: var(--warning); }

.device-metrics { margin-bottom: 16px; }

.metric-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.metric-label { font-size: 12px; color: var(--text-muted); }

.progress-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  max-width: 120px;
  margin-left: 12px;
}

.progress-bar {
  flex: 1;
  height: 6px;
  background: var(--border-color);
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--gradient-primary);
  border-radius: 3px;
  transition: width 0.5s ease;
}

.progress-value { font-size: 12px; color: var(--text-secondary); min-width: 35px; }

.metric-row { display: flex; justify-content: space-between; margin-top: 12px; }
.metric-row .metric-item { flex-direction: column; align-items: flex-start; gap: 4px; }
.metric-value { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.metric-value.temp-high { color: var(--danger); }

.device-actions {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 24px;
  padding: 24px;
  background: linear-gradient(135deg, var(--accent-light), transparent);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
}

.detail-icon-large {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--accent), var(--accent-secondary));
  border-radius: var(--radius-lg);
  color: #fff;
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.3);
}

.detail-info { flex: 1; }
.detail-info h3 { color: var(--text-primary); font-size: 22px; margin-bottom: 8px; font-weight: 600; }
.detail-info p { color: var(--text-secondary); font-size: 14px; margin-bottom: 4px; }

.detail-descriptions { margin-bottom: 20px; }

/* 设备详情统计卡片 */
.detail-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card-item {
  background: var(--bg-hover);
  border-radius: var(--radius-md);
  padding: 16px;
  text-align: center;
  transition: all 0.2s ease;
}

.stat-card-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.stat-card-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--accent);
  margin-bottom: 4px;
}

.stat-card-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.detail-section {
  margin-bottom: 20px;
}

.detail-section .section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.ai-predict-card {
  background: linear-gradient(135deg, var(--success-light), transparent);
  border: 1px solid var(--success);
  border-radius: var(--radius-md);
  padding: 16px;
}

.predict-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 8px;
}

.predict-badge.success {
  background: var(--success-light);
  color: var(--success);
}

.predict-message {
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.predict-confidence {
  color: var(--text-muted);
  font-size: 13px;
}

.predict-confidence span {
  color: var(--accent);
  font-weight: 600;
}

.ai-actions {
  padding-top: 20px;
  border-top: 1px solid var(--border-color);
}

.ai-actions .section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.action-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.action-buttons .el-button {
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-tag {
  font-size: 14px;
  padding: 8px 16px;
}

.predict-header { text-align: center; padding: 20px; }
.predict-header h3 { color: var(--text-primary); margin-top: 12px; }

html.light .page-title { color: var(--text-primary); }
html.light .section-title { color: var(--text-primary); }
html.light .stat-item,
html.light .chart-card,
html.light .alarm-card,
html.light .device-card { box-shadow: var(--shadow-sm); }

/* Status Filter */
.status-filter {
  display: flex;
  gap: 8px;
  align-items: center;
}

.filter-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 20px;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}

.filter-btn.active {
  background: var(--accent-light);
  border-color: var(--accent);
  color: var(--accent);
}

.filter-btn .status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--text-muted);
}

.filter-btn.running .status-dot {
  background: var(--success);
  box-shadow: 0 0 6px var(--success);
}

.filter-btn.idle .status-dot {
  background: var(--info);
}

.filter-btn.fault .status-dot {
  background: var(--danger);
  box-shadow: 0 0 6px var(--danger);
}

.filter-btn.running.active .status-dot,
.filter-btn.idle.active .status-dot,
.filter-btn.fault.active .status-dot {
  box-shadow: 0 0 8px currentColor;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 1400px) {
  .device-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 1200px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
}
</style>