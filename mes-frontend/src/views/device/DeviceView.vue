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
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import PageHeader from '@/components/common/PageHeader.vue'

use([CanvasRenderer, PieChart, BarChart, GridComponent, TooltipComponent, LegendComponent])

const deviceList = ref([
  { id: 1, name: 'CNC-001', code: 'CNC001', status: 'running', utilization: '95%', runtime: '120h', temperature: 45, power: 15 },
  { id: 2, name: 'CNC-002', code: 'CNC002', status: 'idle', utilization: '0%', runtime: '0h', temperature: 30, power: 0 },
  { id: 3, name: 'CNC-003', code: 'CNC003', status: 'running', utilization: '88%', runtime: '85h', temperature: 48, power: 14 },
  { id: 4, name: 'CNC-004', code: 'CNC004', status: 'maintenance', utilization: '0%', runtime: '0h', temperature: 25, power: 0 },
  { id: 5, name: 'CNC-005', code: 'CNC005', status: 'running', utilization: '92%', runtime: '200h', temperature: 42, power: 16 },
  { id: 6, name: 'CNC-006', code: 'CNC006', status: 'fault', utilization: '0%', runtime: '0h', temperature: 35, power: 0 },
  { id: 7, name: '组装线-01', code: 'ASM001', status: 'running', utilization: '85%', runtime: '150h', temperature: 40, power: 20 },
  { id: 8, name: '组装线-02', code: 'ASM002', status: 'running', utilization: '78%', runtime: '90h', temperature: 38, power: 18 }
])

const alarmList = ref([
  { time: '2026-04-04 10:30:00', deviceName: 'CNC-004', level: 'warning', message: '设备维护到期' },
  { time: '2026-04-04 09:15:00', deviceName: 'CNC-006', level: 'critical', message: '设备故障停机' },
  { time: '2026-04-03 16:20:00', deviceName: 'CNC-003', level: 'info', message: '温度偏高提醒' }
])

const utilizationOption = ref({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: ['CNC-001', 'CNC-002', 'CNC-003', 'CNC-004', 'CNC-005', 'CNC-006'] },
  yAxis: { type: 'value', max: 100 },
  series: [{
    data: [95, 0, 88, 0, 92, 0],
    type: 'bar',
    itemStyle: { color: '#e94560' }
  }]
})

const statusOption = ref({
  tooltip: { trigger: 'item' },
  series: [{
    type: 'pie',
    radius: ['40%', '70%'],
    data: [
      { value: 5, name: '运行中' },
      { value: 1, name: '空闲' },
      { value: 1, name: '维护中' },
      { value: 1, name: '故障' }
    ],
    label: { color: '#ffffff' }
  }]
})

const getStatusType = (status: string) => {
  const map: Record<string, string> = { running: 'success', idle: 'info', maintenance: 'warning', fault: 'danger' }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = { running: '运行中', idle: '空闲', maintenance: '维护中', fault: '故障' }
  return map[status] || '未知'
}

const handleRefresh = () => {
  ElMessage.success('刷新成功')
}

const handleDetail = (device: any) => {
  ElMessage.info(`查看设备详情: ${device.name}`)
}

const handleMaintain = (device: any) => {
  ElMessage.warning(`设备 ${device.name} 开始维护`)
}

const handleAck = (alarm: any) => {
  ElMessage.success('报警已确认')
  alarmList.value = alarmList.value.filter(a => a !== alarm)
}
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