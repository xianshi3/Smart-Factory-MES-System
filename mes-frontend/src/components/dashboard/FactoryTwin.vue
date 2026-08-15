<template>
  <div class="factory-twin">
    <div class="ft-head">
      <div class="ft-head-left">
        <span class="ft-title">
          <el-icon :size="16"><Cpu /></el-icon>
          3D 工厂总览
        </span>
        <span class="ft-live"><i></i> 实时孪生</span>
      </div>
      <div class="ft-head-stats">
        <span class="ft-stat"><em>{{ devices.length }}</em> 设备</span>
        <span class="ft-stat ok"><em>{{ online }}</em> 运行</span>
        <span class="ft-stat idle"><em>{{ idle }}</em> 空闲</span>
        <span class="ft-stat alarm"><em>{{ alarm }}</em> 告警</span>
      </div>
      <span class="ft-hint">拖拽旋转 · 滚轮缩放 · 点击设备查看详情</span>
    </div>

    <div class="ft-body">
      <Factory3D :devices="devices" class="ft-3d" />

      <!-- 左下：生产趋势浮层 -->
      <div class="ft-overlay ft-trend">
        <div class="ov-title">
          <el-icon :size="13"><TrendCharts /></el-icon>
          生产趋势
        </div>
        <ChartMini :option="trendOption" :height="'132px'" />
      </div>

      <!-- 右下：设备状态分布浮层 -->
      <div class="ft-overlay ft-status">
        <div class="ov-title">
          <el-icon :size="13"><PieChart /></el-icon>
          设备状态分布
        </div>
        <ChartMini :option="statusOption" :height="'132px'" />
      </div>
    </div>

    <div class="ft-legend">
      <span class="lg-item"><i class="lg-dot" style="background:#10b981"></i>运行中</span>
      <span class="lg-item"><i class="lg-dot" style="background:#06b6d4"></i>空闲</span>
      <span class="lg-item"><i class="lg-dot" style="background:#ef4444"></i>告警</span>
      <span class="lg-item"><i class="lg-dot" style="background:#f59e0b"></i>维护</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Cpu, TrendCharts, PieChart } from '@element-plus/icons-vue'
import Factory3D from './Factory3D.vue'
import ChartMini from '@/components/common/ChartMini.vue'

const props = defineProps<{
  devices: any[]
  trendOption: any
  statusOption: any
}>()

const online = computed(() => props.devices.filter(d => d.status === 'ONLINE').length)
const idle = computed(() => props.devices.filter(d => d.status === 'OFFLINE').length)
const alarm = computed(() => props.devices.filter(d => d.status === 'ALARM').length)
</script>

<style scoped>
.factory-twin {
  position: relative;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  overflow: hidden;
  margin-bottom: 20px;
  animation: fadeIn 0.6s ease 0.15s both;
}
.factory-twin::before {
  content: '';
  position: absolute;
  inset: 0 0 auto 0;
  height: 1px;
  z-index: 5;
  background: linear-gradient(90deg, transparent, rgba(99, 102, 241, 0.7), rgba(34, 211, 238, 0.7), transparent);
}

.ft-head {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 18px;
  border-bottom: 1px solid var(--border-light);
  position: relative;
  z-index: 6;
}
.ft-head-left { display: flex; align-items: center; gap: 10px; }
.ft-title {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}
.ft-title .el-icon { color: var(--accent); }
.ft-live {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 1px;
  color: var(--success);
  padding: 2px 8px;
  border: 1px solid var(--success);
  border-radius: 20px;
}
.ft-live i {
  width: 6px; height: 6px; border-radius: 50%;
  background: var(--success);
  animation: pulse-green 1.6s ease-in-out infinite;
}
.ft-head-stats {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-left: auto;
}
.ft-stat {
  display: inline-flex;
  align-items: baseline;
  gap: 4px;
  font-size: 12px;
  color: var(--text-muted);
}
.ft-stat em {
  font-style: normal;
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.ft-stat.ok em { color: var(--success); }
.ft-stat.idle em { color: var(--info); }
.ft-stat.alarm em { color: var(--danger); }
.ft-hint { font-size: 11px; color: var(--text-muted); opacity: 0.8; }

.ft-body {
  position: relative;
  height: 460px;
}
.ft-3d {
  position: absolute;
  inset: 0;
}

/* 数据浮层（毛玻璃） */
.ft-overlay {
  position: absolute;
  z-index: 4;
  width: 250px;
  padding: 10px 12px;
  border-radius: 14px;
  border: 1px solid rgba(99, 102, 241, 0.25);
  background: rgba(13, 13, 20, 0.68);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.35);
  transition: border-color var(--transition-normal);
}
.ft-overlay:hover { border-color: rgba(99, 102, 241, 0.55); }
:global(html.light) .ft-overlay {
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.1);
}
.ft-trend { left: 16px; bottom: 16px; }
.ft-status { right: 16px; bottom: 16px; }
.ov-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 2px;
}
.ov-title .el-icon { color: var(--accent); }

.ft-legend {
  position: absolute;
  z-index: 4;
  left: 18px;
  top: 56px;
  display: flex;
  gap: 14px;
  font-size: 11px;
  color: var(--text-secondary);
  pointer-events: none;
}
.lg-item { display: inline-flex; align-items: center; gap: 5px; }
.lg-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
}

@keyframes pulse-green {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 768px) {
  .ft-overlay { width: 190px; }
  .ft-hint { display: none; }
}
</style>
