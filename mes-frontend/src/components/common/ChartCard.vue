<template>
  <div class="chart-card">
    <div class="card-header">
      <span class="card-title">
        <el-icon v-if="icon"><component :is="icon" /></el-icon>
        {{ title }}
      </span>
      <slot name="actions" />
    </div>
    <div class="chart-container">
      <v-chart :option="option" autoresize :style="{ height: height }" />
    </div>
  </div>
</template>

<script setup lang="ts">
import VChart from 'vue-echarts'

defineProps<{
  title: string
  icon?: string
  option: any
  height?: string
}>()
</script>

<style scoped>
.chart-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 20px;
  transition: all var(--transition-normal);
}

.chart-card:hover {
  border-color: var(--accent);
  box-shadow: var(--shadow-sm);
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

.card-title .el-icon {
  color: var(--accent);
}

.chart-container {
  width: 100%;
}

html.light .chart-card { box-shadow: var(--shadow-sm); }
html.light .card-title { color: var(--text-primary); }
</style>