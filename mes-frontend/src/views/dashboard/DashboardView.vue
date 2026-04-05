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

use([CanvasRenderer, PieChart, LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent])

const stats = ref({
  todayWorkOrders: 12,
  completedToday: 8,
  inProgress: 4,
  oee: 85
})

const deviceList = ref([
  { name: 'CNC-001', status: 'running', utilization: '95%', temperature: '45°C', power: '15kW' },
  { name: 'CNC-002', status: 'idle', utilization: '0%', temperature: '30°C', power: '0kW' },
  { name: 'CNC-003', status: 'running', utilization: '88%', temperature: '48°C', power: '14kW' },
  { name: 'CNC-004', status: 'maintenance', utilization: '0%', temperature: '25°C', power: '0kW' },
  { name: 'CNC-005', status: 'running', utilization: '92%', temperature: '42°C', power: '16kW' }
])

const productionTrendOption = ref({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'] },
  yAxis: { type: 'value' },
  series: [{
    data: [120, 150, 180, 140, 160, 190, 170],
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
    data: [
      { value: 8, name: '已完成' },
      { value: 4, name: '进行中' },
      { value: 2, name: '待开始' },
      { value: 1, name: '已取消' }
    ],
    label: { color: '#ffffff' }
  }]
})

const getDeviceStatusType = (status: string) => {
  const map: Record<string, string> = { running: 'success', idle: 'info', maintenance: 'warning', fault: 'danger' }
  return map[status] || 'info'
}

const getDeviceStatusText = (status: string) => {
  const map: Record<string, string> = { running: '运行中', idle: '空闲', maintenance: '维护中', fault: '故障' }
  return map[status] || '未知'
}

onMounted(() => {})
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