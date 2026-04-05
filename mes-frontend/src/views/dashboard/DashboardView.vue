<!-- Dashboard View - Production overview and statistics -->
<template>
  <div class="dashboard-container">
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #e94560;">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.todayWorkOrders }}</div>
            <div class="stat-label">今日工单</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #52c41a;">
            <el-icon><SuccessFilled /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.completedToday }}</div>
            <div class="stat-label">已完成</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #faad14;">
            <el-icon><Loading /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.inProgress }}</div>
            <div class="stat-label">进行中</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #1890ff;">
            <el-icon><TrendCharts /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.oee }}%</div>
            <div class="stat-label">OEE</div>
          </div>
        </div>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <div class="chart-card">
          <div class="chart-title">生产趋势</div>
          <v-chart :option="productionTrendOption" autoresize style="height: 300px;" />
        </div>
      </el-col>
      <el-col :span="12">
        <div class="chart-card">
          <div class="chart-title">工单状态分布</div>
          <v-chart :option="workOrderStatusOption" autoresize style="height: 300px;" />
        </div>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
        <div class="chart-card">
          <div class="chart-title">设备状态监控</div>
          <el-table :data="deviceList" style="width: 100%;">
            <el-table-column prop="name" label="设备名称" />
            <el-table-column prop="status" label="状态">
              <template #default="{ row }">
                <el-tag :type="getDeviceStatusType(row.status)">
                  {{ getDeviceStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="utilization" label="利用率" />
            <el-table-column prop="temperature" label="温度" />
            <el-table-column prop="power" label="功率" />
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, LineChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { getDeviceStatus, getAlarmDevices } from '@/api/services'

use([CanvasRenderer, PieChart, LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent])

const stats = ref({
  todayWorkOrders: 0,
  completedToday: 0,
  inProgress: 0,
  oee: 0
})

const deviceList = ref<any[]>([])

const productionTrendOption = ref({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: [] as string[] },
  yAxis: { type: 'value' },
  series: [{
    data: [] as number[],
    type: 'line',
    smooth: true,
    areaStyle: { opacity: 0.3 },
    itemStyle: { color: '#e94560' }
  }]
})

const workOrderStatusOption = ref({
  tooltip: { trigger: 'item' },
  series: [{
    type: 'pie',
    radius: ['40%', '70%'],
    data: [] as any[],
    label: { color: '#ffffff' }
  }]
})

const mapDbStatus = (status: string) => {
  const map: Record<string, string> = { ONLINE: 'running', OFFLINE: 'idle', MAINTENANCE: 'maintenance', ALARM: 'fault' }
  return map[status] || 'idle'
}

const getDeviceStatusType = (status: string) => {
  const map: Record<string, string> = { running: 'success', idle: 'info', maintenance: 'warning', fault: 'danger' }
  return map[status] || 'info'
}

const getDeviceStatusText = (status: string) => {
  const map: Record<string, string> = { running: '运行中', idle: '空闲', maintenance: '维护中', fault: '故障', alarm: '告警' }
  return map[status] || '未知'
}

const fetchDashboardData = async () => {
  try {
    const [devices, alarms] = await Promise.all([getDeviceStatus(), getAlarmDevices()])
    const devData = devices?.data || devices || []
    const alarmData = alarms?.data || alarms || []
    
    deviceList.value = devData.map((item: any) => ({
      name: item.deviceName || item.device_code,
      status: mapDbStatus(item.status),
      utilization: item.speed && item.speed > 0 ? Math.round(item.speed / 15) + '%' : '0%',
      temperature: (item.temperature || 0) + '°C',
      power: item.speed && item.speed > 0 ? Math.round(item.speed * 0.02 + 5) + 'kW' : '0kW'
    }))
    
    const onlineCount = devData.filter((d: any) => d.status === 'ONLINE').length
    const alarmCount = alarmData.length || devData.filter((d: any) => d.status === 'ALARM').length
    stats.value = {
      todayWorkOrders: devData.length,
      completedToday: onlineCount,
      inProgress: devData.filter((d: any) => d.status === 'ONLINE' && d.speed > 0).length,
      oee: onlineCount > 0 ? Math.round((onlineCount / devData.length) * 100) : 0
    }
    
    const deviceNames = deviceList.value.map((d: any) => d.name)
    const utilizations = deviceList.value.map((d: any) => parseInt(d.utilization) || 0)
    productionTrendOption.value = {
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: deviceNames },
      yAxis: { type: 'value' },
      series: [{
        data: utilizations,
        type: 'line',
        smooth: true,
        areaStyle: { opacity: 0.3 },
        itemStyle: { color: '#e94560' }
      }]
    }
    
    const statusCounts: Record<string, number> = {}
    deviceList.value.forEach((d: any) => {
      const text = getDeviceStatusText(d.status)
      statusCounts[text] = (statusCounts[text] || 0) + 1
    })
    workOrderStatusOption.value = {
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        data: Object.entries(statusCounts).map(([name, value]) => ({ value, name })),
        label: { color: '#ffffff' }
      }]
    }
  } catch (error) {
    console.error('Failed to fetch dashboard data:', error)
  }
}

onMounted(() => {
  fetchDashboardData()
})
</script>

<style scoped lang="scss">
.dashboard-container {
  .stats-row {
    .stat-card {
      background: rgba(255, 255, 255, 0.05);
      border-radius: 8px;
      padding: 20px;
      display: flex;
      align-items: center;
      gap: 16px;
      
      .stat-icon {
        width: 60px;
        height: 60px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        
        .el-icon {
          font-size: 28px;
          color: #ffffff;
        }
      }
      
      .stat-content {
        .stat-value {
          font-size: 28px;
          font-weight: bold;
          color: #ffffff;
        }
        
        .stat-label {
          font-size: 14px;
          color: rgba(255, 255, 255, 0.6);
          margin-top: 4px;
        }
      }
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