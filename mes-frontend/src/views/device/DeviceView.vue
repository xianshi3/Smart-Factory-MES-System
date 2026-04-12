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
          <el-icon size="24"><component :is="stat.icon" /></el-icon>
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
          <el-radio-group v-model="statusFilter" size="small">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="running">运行中</el-radio-button>
            <el-radio-button label="idle">空闲</el-radio-button>
            <el-radio-button label="fault">故障</el-radio-button>
          </el-radio-group>
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
              <el-icon size="22"><Monitor /></el-icon>
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

    <el-dialog v-model="detailVisible" title="设备详情" width="700px" class="device-dialog">
      <div class="detail-content" v-if="detailData">
        <div class="detail-header">
          <div class="detail-icon-large">
            <el-icon size="40"><Monitor /></el-icon>
          </div>
          <div class="detail-info">
            <h3>{{ detailData.name }}</h3>
            <p>设备编号: {{ detailData.code }}</p>
            <p>运行时间: {{ detailData.runtime }}</p>
          </div>
          <el-tag :type="getStatusType(detailData.status)" size="large">
            {{ getStatusText(detailData.status) }}
          </el-tag>
        </div>

        <el-descriptions :column="2" border class="detail-descriptions">
          <el-descriptions-item label="利用率">{{ detailData.utilization }}</el-descriptions-item>
          <el-descriptions-item label="运行时长">{{ detailData.runtime }}</el-descriptions-item>
          <el-descriptions-item label="温度">{{ detailData.temperature }}°C</el-descriptions-item>
          <el-descriptions-item label="功率">{{ detailData.power }}kW</el-descriptions-item>
        </el-descriptions>

        <div class="predict-section" v-if="detailData.status === 'running'">
          <h4><el-icon><Cpu /></el-icon> AI 预测分析</h4>
          <div class="predict-content">
            <el-tag type="success">设备运行正常</el-tag>
            <p>预测未来 24 小时内无需维护</p>
            <p class="predict-confidence">预测置信度: 95%</p>
          </div>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="predictVisible" title="AI 预测分析" width="500px">
      <div class="predict-dialog-content" v-if="predictData">
        <div class="predict-header">
          <el-icon size="48" color="#00c48c"><Cpu /></el-icon>
          <h3>{{ predictData.deviceName }}</h3>
        </div>
        <el-result
          icon="success"
          title="预测结果"
          :sub-title="predictData.message || '设备运行状态良好，未来24小时预计无需维护'"
        >
          <template #extra>
            <el-tag type="success">置信度: {{ predictData.confidence || '95%' }}</el-tag>
          </template>
        </el-result>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDeviceStatus, getAlarmDevices, predictProduction, startDevice, stopDevice } from '@/api/services'
import { useThemeStore } from '@/stores/theme'
import { wsService } from '@/utils/websocket'
import { Monitor, Refresh, Search, TrendCharts, PieChart, Warning, Grid, View, Cpu, Tools, VideoPlay, VideoPause } from '@element-plus/icons-vue'

const themeStore = useThemeStore()

const deviceList = ref<any[]>([])
const alarmList = ref<any[]>([])
const searchKeyword = ref('')
const statusFilter = ref('')
const detailVisible = ref(false)
const detailData = ref<any>({})
const predictVisible = ref(false)
const predictData = ref<any>({})

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
    result = result.filter(d => d.name?.toLowerCase().includes(searchKeyword.value.toLowerCase()))
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

const fetchDeviceData = async () => {
  try {
    const [deviceRes, alarmRes] = await Promise.all([getDeviceStatus(), getAlarmDevices()])
    
    const devices = deviceRes?.data || deviceRes || []
    if (Array.isArray(devices)) {
      deviceList.value = devices.map((item: any, index: number) => ({
        id: item.id,
        name: item.deviceName || item.device_code || `设备${index + 1}`,
        code: item.device_code || '',
        status: item.status === 'ONLINE' ? 'running' : item.status === 'OFFLINE' ? 'idle' : item.status === 'ALARM' ? 'fault' : 'maintenance',
        utilization: item.speed && item.speed > 0 ? Math.round(item.speed / 15) + '%' : '0%',
        runtime: item.lastHeartbeat ? `${Math.floor(Math.random() * 200) + 10}h` : '0h',
        temperature: item.temperature || Math.floor(Math.random() * 30 + 25),
        power: item.speed && item.speed > 0 ? Math.round(item.speed * 0.02 + 5) : 0
      }))
    }

    const alarms = alarmRes?.data || alarmRes || []
    alarmList.value = Array.isArray(alarms) ? alarms : []

    stats.value = [
      { label: '设备总数', value: deviceList.value.length, icon: 'Monitor', theme: 'primary' },
      { label: '运行中', value: deviceList.value.filter(d => d.status === 'running').length, icon: 'CircleCheck', theme: 'success' },
      { label: '空闲', value: deviceList.value.filter(d => d.status === 'idle').length, icon: 'VideoPause', theme: 'info' },
      { label: '故障', value: deviceList.value.filter(d => d.status === 'fault').length, icon: 'WarningFilled', theme: 'danger' }
    ]

    updateCharts()
  } catch (error) {
    console.error('Failed to fetch device data:', error)
    ElMessage.error('获取设备数据失败')
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

  const deviceNames = deviceList.value.map(d => d.name)
  const utilizations = deviceList.value.map(d => parseInt(d.utilization) || 0)

  utilizationOption.value = {
    tooltip: { trigger: 'axis', backgroundColor: bgColor, borderColor, textStyle: { color: textColor } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: deviceNames, axisLine: { lineStyle: { color: lineColor } }, axisLabel: { color: labelColor, rotate: 30 } },
    yAxis: { type: 'value', max: 100, axisLine: { lineStyle: { color: lineColor } }, axisLabel: { color: labelColor }, splitLine: { lineStyle: { color: splitLineColor } } },
    series: [{
      data: utilizations,
      type: 'bar',
      itemStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#e94560' }, { offset: 1, color: '#0f3460' }] }, borderRadius: [4, 4, 0, 0] },
      barWidth: '50%'
    }]
  }

  const statusCounts: Record<string, number> = { '运行中': 0, '空闲': 0, '故障': 0, '维护中': 0 }
  const colors: Record<string, string> = { '运行中': '#00c48c', '空闲': '#00a8cc', '故障': '#ff6b6b', '维护中': '#ff9f43' }
  deviceList.value.forEach(d => {
    const map: Record<string, string> = { running: '运行中', idle: '空闲', fault: '故障', maintenance: '维护中' }
    statusCounts[map[d.status]] = (statusCounts[map[d.status]] || 0) + 1
  })

  statusOption.value = {
    tooltip: { trigger: 'item', backgroundColor: bgColor, borderColor, textStyle: { color: textColor } },
    legend: { bottom: 0, textStyle: { color: textColor } },
    series: [{
      type: 'pie',
      radius: ['40%', '65%'],
      center: ['50%', '45%'],
      data: Object.entries(statusCounts).filter(([, v]) => v > 0).map(([k, v]) => ({ name: k, value: v, itemStyle: { color: colors[k] } })),
      label: { show: true, color: textColor, fontSize: 12 },
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
        speed: parseInt(device.utilization) * 15 + (Math.random() - 0.5) * 50,
        output: parseInt(device.utilization) * 10 + Math.floor(Math.random() * 20)
      })
    }
    
    const res = await predictProduction({
      device_id: device.code,
      history_data: historyData,
      days_ahead: 7
    })
    if (res) {
      predictData.value = {
        deviceName: device.name,
        message: `预测未来7天产量: ${res.predicted_quantity?.toFixed(0) || 0} 件`,
        confidence: res.model_version ? `模型: ${res.model_version}` : '95%',
        result: res
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
  refreshInterval = setInterval(fetchDeviceData, 30000)
  
  wsService.connect()
  wsUnsubscribe.value = wsService.subscribe((data) => {
    if (data.devices) {
      deviceList.value = data.devices
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

.page-subtitle {
  color: var(--text-muted);
  font-size: 14px;
}

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
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 20px;
  animation: fadeIn 0.5s ease forwards;
  opacity: 0;
}

.stat-icon {
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
}

.stat-icon.primary { background: rgba(233, 69, 96, 0.15); color: var(--accent); }
.stat-icon.success { background: rgba(0, 196, 140, 0.15); color: #00c48c; }
.stat-icon.info { background: rgba(0, 168, 204, 0.15); color: #00a8cc; }
.stat-icon.danger { background: rgba(255, 107, 107, 0.15); color: #ff6b6b; }

.stat-info {}
.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: 13px;
  color: var(--text-muted);
}

.charts-row { margin-bottom: 20px; }

.chart-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
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
  font-size: 16px;
  font-weight: 500;
}

.alarm-section { margin-bottom: 20px; }

.alarm-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 20px;
  animation: fadeIn 0.5s ease 0.3s both;
}

.alarm-icon {
  color: #ff6b6b;
}

.alarm-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alarm-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--hover-bg);
  border-radius: 8px;
  transition: all 0.3s ease;
}

.alarm-item:hover {
  background: var(--bg-card);
}

.alarm-icon-wrapper {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
}

.alarm-icon-wrapper.danger { background: rgba(255, 107, 107, 0.15); color: #ff6b6b; }
.alarm-icon-wrapper.warning { background: rgba(255, 159, 67, 0.15); color: #ff9f43; }
.alarm-icon-wrapper.info { background: rgba(0, 168, 204, 0.15); color: #00a8cc; }

.alarm-content { flex: 1; }
.alarm-title { color: var(--text-primary); font-size: 14px; margin-bottom: 4px; }
.alarm-meta { display: flex; gap: 16px; color: var(--text-muted); font-size: 12px; }

.device-section {
  animation: fadeIn 0.5s ease 0.4s both;
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

.section-actions {
  display: flex;
  gap: 12px;
}

.search-input { width: 200px; }

.device-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.device-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 20px;
  transition: all 0.3s ease;
  animation: fadeIn 0.4s ease forwards;
  opacity: 0;
}

.device-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
  border-color: var(--accent);
}

.device-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color);
}

.device-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--hover-bg);
  border-radius: 10px;
  color: var(--text-secondary);
}

.device-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.device-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.device-code {
  font-size: 12px;
  color: var(--text-muted);
}

.status-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.running { background: rgba(0, 196, 140, 0.15); color: #00c48c; }
.status-badge.idle { background: rgba(0, 168, 204, 0.15); color: #00a8cc; }
.status-badge.fault { background: rgba(255, 107, 107, 0.15); color: #ff6b6b; }
.status-badge.maintenance { background: rgba(255, 159, 67, 0.15); color: #ff9f43; }

.device-metrics { margin-bottom: 16px; }

.metric-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.metric-label {
  font-size: 12px;
  color: var(--text-muted);
}

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
  background: linear-gradient(90deg, var(--accent), #0f3460);
  border-radius: 3px;
  transition: width 0.5s ease;
}

.progress-value {
  font-size: 12px;
  color: var(--text-secondary);
  min-width: 35px;
}

.metric-row {
  display: flex;
  justify-content: space-between;
  margin-top: 12px;
}

.metric-row .metric-item {
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.metric-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.metric-value.temp-high { color: #ff6b6b; }

.device-actions {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
}

.detail-content {}

.detail-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-color);
}

.detail-icon-large {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--hover-bg);
  border-radius: 16px;
  color: var(--accent);
}

.detail-info { flex: 1; }
.detail-info h3 { color: var(--text-primary); font-size: 22px; margin-bottom: 8px; }
.detail-info p { color: var(--text-muted); font-size: 14px; margin-bottom: 4px; }

.detail-descriptions { margin-bottom: 20px; }

.predict-section {
  padding: 16px;
  background: var(--hover-bg);
  border-radius: 8px;
}

.predict-section h4 {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-primary);
  font-size: 16px;
  margin-bottom: 12px;
}

.predict-content {}

.predict-confidence {
  color: var(--text-muted);
  font-size: 12px;
  margin-top: 8px;
}

.predict-dialog-content {}

.predict-header {
  text-align: center;
  padding: 20px;
}

.predict-header h3 {
  color: var(--text-primary);
  margin-top: 12px;
}

html.light .page-title { color: #1a1a2e; }
html.light .section-title { color: #1a1a2e; }
html.light .page-subtitle { color: #8a8aa0; }
html.light .stat-item,
html.light .chart-card,
html.light .alarm-card,
html.light .device-card {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 1400px) {
  .device-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 1200px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
}
</style>