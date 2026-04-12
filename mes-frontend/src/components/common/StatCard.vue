<template>
  <div class="stat-card" :class="[`stat-${theme}`, { 'has-trend': trend !== undefined }]" :style="{ animationDelay: `${delay}s` }">
    <div class="stat-glow"></div>
    <div class="stat-icon">
      <el-icon size="24"><component :is="icon" /></el-icon>
    </div>
    <div class="stat-content">
      <div class="stat-value">{{ value }}</div>
      <div class="stat-label">{{ label }}</div>
    </div>
    <div v-if="trend !== undefined" class="stat-trend" :class="trend >= 0 ? 'up' : 'down'">
      <el-icon><CaretTop v-if="trend >= 0" /><CaretBottom v-else /></el-icon>
      <span>{{ Math.abs(trend) }}%</span>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  icon: string
  label: string
  value: string | number
  theme?: 'primary' | 'success' | 'warning' | 'info'
  trend?: number
  delay?: number
}>()
</script>

<style scoped>
.stat-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 20px;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
  animation: fadeInUp 0.5s ease forwards;
  opacity: 0;
}
.stat-card:hover {
  transform: translateY(-4px);
  border-color: var(--accent);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.2);
}
.stat-glow {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  border-radius: 50%;
  opacity: 0;
  transition: 0.5s ease;
  pointer-events: none;
}
.stat-card:hover .stat-glow { opacity: 1; }

.stat-primary .stat-glow { background: radial-gradient(circle, rgba(233, 69, 96, 0.2) 0%, transparent 70%); }
.stat-success .stat-glow { background: radial-gradient(circle, rgba(0, 196, 140, 0.2) 0%, transparent 70%); }
.stat-warning .stat-glow { background: radial-gradient(circle, rgba(255, 159, 67, 0.2) 0%, transparent 70%); }
.stat-info .stat-glow { background: radial-gradient(circle, rgba(0, 168, 204, 0.2) 0%, transparent 70%); }

.stat-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  margin-bottom: 14px;
}
.stat-primary .stat-icon { background: rgba(233, 69, 96, 0.15); color: var(--accent); }
.stat-success .stat-icon { background: rgba(0, 196, 140, 0.15); color: #00c48c; }
.stat-warning .stat-icon { background: rgba(255, 159, 67, 0.15); color: #ff9f43; }
.stat-info .stat-icon { background: rgba(0, 168, 204, 0.15); color: #00a8cc; }

.stat-content { position: relative; z-index: 1; }
.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}
.stat-label {
  color: var(--text-muted);
  font-size: 13px;
  margin-top: 4px;
}

.stat-trend {
  position: absolute;
  top: 16px;
  right: 16px;
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 20px;
}
.stat-trend.up { background: rgba(0, 196, 140, 0.15); color: #00c48c; }
.stat-trend.down { background: rgba(255, 107, 107, 0.15); color: #ff6b6b; }

html.light .stat-card { box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06); }
html.light .stat-card:hover { box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1); }
html.light .stat-icon { background: #f0f2f5 !important; }
html.light .stat-label { color: #8a8aa0; }

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>