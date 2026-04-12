<template>
  <div class="stat-card" :class="[`stat-${theme}`, { 'has-trend': trend !== undefined }]" :style="{ animationDelay: `${delay}s` }">
    <div class="stat-glow"></div>
    <div class="stat-icon">
      <el-icon size="22"><component :is="icon" /></el-icon>
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
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 20px;
  position: relative;
  overflow: hidden;
  transition: all var(--transition-normal);
  animation: fadeInUp 0.5s ease forwards;
  opacity: 0;
}

.stat-card:hover {
  transform: translateY(-4px);
  border-color: var(--accent);
  box-shadow: var(--shadow-lg);
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

.stat-primary .stat-glow { background: radial-gradient(circle, var(--accent-light) 0%, transparent 70%); }
.stat-success .stat-glow { background: radial-gradient(circle, var(--success-light) 0%, transparent 70%); }
.stat-warning .stat-glow { background: radial-gradient(circle, var(--warning-light) 0%, transparent 70%); }
.stat-info .stat-glow { background: radial-gradient(circle, var(--info-light) 0%, transparent 70%); }

.stat-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  margin-bottom: 14px;
}

.stat-primary .stat-icon { background: var(--accent-light); color: var(--accent); }
.stat-success .stat-icon { background: var(--success-light); color: var(--success); }
.stat-warning .stat-icon { background: var(--warning-light); color: var(--warning); }
.stat-info .stat-icon { background: var(--info-light); color: var(--info); }

.stat-content { position: relative; z-index: 1; }
.stat-value {
  font-size: 26px;
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
.stat-trend.up { background: var(--success-light); color: var(--success); }
.stat-trend.down { background: var(--danger-light); color: var(--danger); }

html.light .stat-card { box-shadow: var(--shadow-sm); }
html.light .stat-card:hover { box-shadow: var(--shadow-md); }
html.light .stat-icon { background: var(--bg-hover) !important; }

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>