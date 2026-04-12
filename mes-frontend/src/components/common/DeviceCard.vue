<template>
  <div class="device-card" :class="[`status-${statusClass}`]" :style="{ animationDelay: `${delay}s` }">
    <div class="device-header">
      <div class="device-icon">
        <el-icon size="20"><Monitor /></el-icon>
      </div>
      <span class="device-name">{{ name }}</span>
    </div>
    <div class="device-status">
      <span class="status-dot"></span>
      <span class="status-text">{{ statusText }}</span>
    </div>
    <div class="device-metrics">
      <div class="metric">
        <span class="metric-label">利用率</span>
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: `${utilization}%` }"></div>
          <span class="progress-text">{{ utilization }}%</span>
        </div>
      </div>
      <div class="metric-row">
        <div class="metric-item">
          <span class="metric-label">温度</span>
          <span class="metric-value" :class="{ 'temp-high': temperature > 60 }">{{ temperature }}°C</span>
        </div>
        <div class="metric-item">
          <span class="metric-label">功率</span>
          <span class="metric-value">{{ power }}kW</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Monitor } from '@element-plus/icons-vue'

const props = defineProps<{
  name: string
  status: string
  utilization: number
  temperature: number
  power: number
  delay?: number
}>()

const statusMap: Record<string, string> = {
  ONLINE: '运行中',
  OFFLINE: '空闲',
  ALARM: '告警',
  MAINTENANCE: '维护中'
}

const statusText = computed(() => statusMap[props.status] || '未知')

const statusClass = computed(() => {
  const map: Record<string, string> = {
    ONLINE: 'online',
    OFFLINE: 'idle',
    ALARM: 'alarm',
    MAINTENANCE: 'maintenance'
  }
  return map[props.status] || 'idle'
})
</script>

<style scoped>
.device-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 16px;
  transition: all 0.3s ease;
  animation: fadeInUp 0.4s ease forwards;
  opacity: 0;
}
.device-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.device-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.device-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--hover-bg);
  border-radius: 10px;
  color: var(--text-secondary);
}
.device-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.device-status {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}
.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.status-text { font-size: 12px; }

.status-online .status-dot { background: #00c48c; box-shadow: 0 0 8px #00c48c; }
.status-online .status-text { color: #00c48c; }
.status-idle .status-dot { background: #00a8cc; }
.status-idle .status-text { color: #00a8cc; }
.status-alarm .status-dot { background: #ff6b6b; box-shadow: 0 0 8px #ff6b6b; }
.status-alarm .status-text { color: #ff6b6b; }
.status-maintenance .status-dot { background: #ff9f43; }
.status-maintenance .status-text { color: #ff9f43; }

.device-metrics { display: flex; flex-direction: column; gap: 10px; }
.metric { display: flex; align-items: center; justify-content: space-between; }
.metric-label { font-size: 12px; color: var(--text-muted); }

.progress-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  max-width: 100px;
}
.progress-fill {
  height: 6px;
  background: linear-gradient(90deg, var(--accent), #0f3460);
  border-radius: 3px;
  transition: width 0.5s ease;
}
.progress-text { font-size: 12px; color: var(--text-secondary); }

.metric-row { display: flex; gap: 16px; }
.metric-item { display: flex; flex-direction: column; gap: 2px; }
.metric-value { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.metric-value.temp-high { color: #ff6b6b; }

html.light .device-card { box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05); }
html.light .device-card:hover { box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1); }
html.light .device-icon { background: #f0f2f5; color: #5a5a7a; }

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(15px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>