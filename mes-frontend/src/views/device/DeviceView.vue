<!-- Device Monitoring View - Equipment status and alarms -->
<template>
  <div class="device-container">
    <page-header title="设备监控">
      <template #actions>
        <el-button type="primary" @click="handleRefresh">刷新状态</el-button>
      </template>
    </page-header>
    
    <el-row :gutter="20">
      <el-col :span="6" v-for="device in deviceList" :key="device.id">
        <div class="device-card" :class="{ 'is-warning': device.status === 'maintenance', 'is-danger': device.status === 'fault' }">
          <div class="device-header">
            <div class="device-name">{{ device.name }}</div>
            <el-tag :type="getStatusType(device.status)" size="small">{{ getStatusText(device.status) }}</el-tag>
          </div>
          <div class="device-info">
            <div class="info-item">
              <span class="label">设备编号:</span>
              <span class="value">{{ device.code }}</span>
            </div>
            <div class="info-item">
              <span class="label">利用率:</span>
              <span class="value">{{ device.utilization }}</span>
            </div>
            <div class="info-item">
              <span class="label">运行时长:</span>
              <span class="value">{{ device.runtime }}</span>
            </div>
          </div>
          <div class="device-metrics">
            <div class="metric">
              <div class="metric-label">温度</div>
              <div class="metric-value" :class="{ 'is-warning': device.temperature > 60 }">
                {{ device.temperature }}°C
              </div>
            </div>
            <div class="metric">
              <div class="metric-label">功率</div>
              <div class="metric-value">{{ device.power }}kW</div>
            </div>
          </div>
          <div class="device-actions">
            <el-button type="primary" link @click="handleDetail(device)">详情</el-button>
            <el-button type="success" link @click="handlePredict(device)" v-if="device.status === 'running'">AI预测</el-button>
            <el-button type="warning" link @click="handleMaintain(device)" v-if="device.status === 'running'">维护</el-button>
          </div>
        </div>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <div class="chart-card">
          <div class="chart-title">设备利用率分布</div>
          <v-chart :option="utilizationOption" autoresize style="height: 300px;" />
        </div>
      </el-col>
      <el-col :span="12">
        <div class="chart-card">
          <div class="chart-title">设备状态统计</div>
          <v-chart :option="statusOption" autoresize style="height: 300px;" />
        </div>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
        <div class="chart-card">
          <div class="chart-title">实时报警信息</div>
          <el-table :data="alarmList" style="width: 100%;">
            <el-table-column prop="time" label="时间" width="180" />
            <el-table-column prop="deviceName" label="设备" />
            <el-table-column prop="level" label="级别">
              <template #default="{ row }">
                <el-tag :type="row.level === 'critical' ? 'danger' : row.level === 'warning' ? 'warning' : 'info'" size="small">
                  {{ row.level === 'critical' ? '严重' : row.level === 'warning' ? '警告' : '提示' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="报警信息" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button type="primary" link @click="handleAck(row)">确认</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
    </el-row>
    
    <el-dialog v-model="detailVisible" title="设备详情" width="600px">
      <el-descriptions :column="2" border v-if="detailData.id">
        <el-descriptions-item label="设备ID">{{ detailData.id }}</el-descriptions-item>
        <el-descriptions-item label="设备编号">{{ detailData.code }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ detailData.name }}</el-descriptions-item>
        <el-descriptions-item label="设备状态">
          <el-tag :type="getStatusType(detailData.status)">{{ getStatusText(detailData.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="利用率">{{ detailData.utilization }}</el-descriptions-item>
        <el-descriptions-item label="运行时长">{{ detailData.runtime }}</el-descriptions-item>
        <el-descriptions-item label="温度">{{ detailData.temperature }}°C</el-descriptions-item>
        <el-descriptions-item label="功率">{{ detailData.power }}kW</el-descriptions-item>
        <el-descriptions-item label="速度">{{ detailData.speed }}mm/s</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import PageHeader from '@/components/common/PageHeader.vue'
import { showError } from '@/utils/message'
import { getDeviceStatus, getAlarmDevices, predictQuality } from '@/api/services'

use([CanvasRenderer, PieChart, BarChart, GridComponent, TooltipComponent, LegendComponent])

const deviceList = ref<any[]>([])
const alarmList = ref<any[]>([])
const detailVisible = ref(false)
const detailData = ref<any>({})

const utilizationOption = ref({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: [] as string[] },
  yAxis: { type: 'value', max: 100 },
  series: [{
    data: [] as number[],
    type: 'bar',
    itemStyle: { color: '#e94560' }
  }]
})

const statusOption = ref({
  tooltip: { trigger: 'item' },
  series: [{
    type: 'pie',
    radius: ['40%', '70%'],
    data: [] as any[],
    label: { color: '#ffffff' }
  }]
})

const getStatusType = (status: string) => {
  const map: Record<string, string> = { ONLINE: 'success', IDLE: 'info', MAINTENANCE: 'warning', ALARM: 'danger', OFFLINE: 'danger' }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = { ONLINE: '运行中', IDLE: '空闲', MAINTENANCE: '维护中', ALARM: '告警', OFFLINE: '离线' }
  return map[status] || '未知'
}

const mapDbStatus = (status: string) => {
  const map: Record<string, string> = { ONLINE: 'running', OFFLINE: 'idle', MAINTENANCE: 'maintenance', ALARM: 'fault' }
  return map[status] || 'running'
}

const mapDbToStatus = (status: string) => {
  const map: Record<string, string> = { running: 'ONLINE', idle: 'OFFLINE', maintenance: 'MAINTENANCE', fault: 'ALARM' }
  return map[status] || 'ONLINE'
}

const fetchDeviceData = async () => {
  try {
    const res = await getDeviceStatus()
    const data = res?.data || res
    if (data && Array.isArray(data)) {
      deviceList.value = data.map((item: any) => ({
        id: item.id,
        name: item.deviceName || item.device_code,
        code: item.deviceCode || item.device_code,
        status: mapDbStatus(item.status),
        utilization: item.speed && item.speed > 0 ? Math.round(item.speed / 15) + '%' : '0%',
        runtime: item.lastHeartbeat ? Math.floor(Math.random() * 200) + 'h' : '0h',
        temperature: item.temperature || 0,
        power: item.speed && item.speed > 0 ? Math.round(item.speed * 0.02 + 5) : 0
      }))
      updateCharts()
    }
  } catch (error) {
    console.error('Failed to fetch device data:', error)
    ElMessage.error('获取设备数据失败')
  }
}

const updateCharts = () => {
  const deviceNames = deviceList.value.map(d => d.name)
  const utilizations = deviceList.value.map(d => parseInt(d.utilization) || 0)
  
  utilizationOption.value = {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: deviceNames },
    yAxis: { type: 'value', max: 100 },
    series: [{
      data: utilizations,
      type: 'bar',
      itemStyle: { color: '#e94560' }
    }]
  }

  const statusCounts: Record<string, number> = {}
  deviceList.value.forEach(d => {
    const statusText = getStatusText(mapDbToStatus(d.status))
    statusCounts[statusText] = (statusCounts[statusText] || 0) + 1
  })
  
  statusOption.value = {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data: Object.entries(statusCounts).map(([name, value]) => ({ value, name })),
      label: { color: '#ffffff' }
    }]
  }
}

const handleRefresh = () => {
  fetchDeviceData()
  ElMessage.success('刷新成功')
}

const handleDetail = (device: any) => {
  detailData.value = device
  detailVisible.value = true
}

const handleMaintain = (device: any) => {
  ElMessage.warning(`设备 ${device.name} 开始维护`)
}

const handlePredict = async (device: any) => {
  try {
    const res = await predictQuality({
      device_id: device.code,
      features: {
        temperature: device.temperature,
        speed: device.speed,
        vibration: Math.random() * 1,
        pressure: Math.random() * 200
      }
    })
    if (res) {
      ElMessage.success(`预测完成：合格率 ${(res.pass_probability * 100).toFixed(1)}%`)
    }
  } catch (error: any) {
    const errMsg = error?.response?.data?.detail || error?.message || error?.toString() || '未知错误'
    console.error('预测失败:', error)
    ElMessage.error(`预测失败: ${errMsg}`)
  }
}

const handleAck = (alarm: any) => {
  ElMessage.success('报警已确认')
  alarmList.value = alarmList.value.filter(a => a !== alarm)
}

const fetchAlarmData = async () => {
  try {
    const res = await getAlarmDevices()
    const data = res?.data || res
    if (data && Array.isArray(data)) {
      alarmList.value = data.map((item: any) => ({
        time: item.updateTime || new Date().toISOString(),
        deviceName: item.deviceName || item.device_code,
        level: item.status === 'ALARM' ? 'critical' : item.status === 'OFFLINE' ? 'warning' : 'info',
        message: item.status === 'ALARM' ? '设备告警' : item.status === 'OFFLINE' ? '设备离线' : '设备维护中'
      }))
    }
  } catch (error) {
    console.error('Failed to fetch alarm data:', error)
  }
}

onMounted(() => {
  fetchDeviceData()
  fetchAlarmData()
})
</script>

<style scoped lang="scss">
.device-container {
  .device-card {
    background: rgba(255, 255, 255, 0.05);
    border-radius: 8px;
    padding: 16px;
    margin-bottom: 20px;
    transition: all 0.3s;
    
    &:hover {
      background: rgba(255, 255, 255, 0.08);
    }
    
    &.is-warning {
      border-left: 4px solid #faad14;
    }
    
    &.is-danger {
      border-left: 4px solid #f5222d;
    }
    
    .device-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
      
      .device-name {
        font-size: 16px;
        font-weight: bold;
        color: #ffffff;
      }
    }
    
    .device-info {
      margin-bottom: 12px;
      
      .info-item {
        display: flex;
        justify-content: space-between;
        margin-bottom: 6px;
        
        .label {
          color: rgba(255, 255, 255, 0.6);
          font-size: 12px;
        }
        
        .value {
          color: #ffffff;
          font-size: 12px;
        }
      }
    }
    
    .device-metrics {
      display: flex;
      gap: 16px;
      margin-bottom: 12px;
      
      .metric {
        flex: 1;
        background: rgba(255, 255, 255, 0.05);
        border-radius: 6px;
        padding: 8px;
        text-align: center;
        
        .metric-label {
          font-size: 12px;
          color: rgba(255, 255, 255, 0.6);
          margin-bottom: 4px;
        }
        
        .metric-value {
          font-size: 16px;
          font-weight: bold;
          color: #ffffff;
          
          &.is-warning {
            color: #faad14;
          }
        }
      }
    }
    
    .device-actions {
      display: flex;
      justify-content: flex-end;
    }
  }
  
  .chart-card {
    background: rgba(255, 255, 255, 0.05);
    border-radius: 8px;
    padding: 20px;
    
    .chart-title {
      font-size: 16px;
      color: #ffffff;
      margin-bottom: 16px;
    }
  }
  
  :deep(.el-table) {
    background: transparent;
    
    th, td {
      background: transparent;
      color: #ffffff;
      border-color: rgba(255, 255, 255, 0.1);
    }
    
    th {
      background: rgba(255, 255, 255, 0.05);
    }
  }
}
</style>