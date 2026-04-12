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
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 20px;
  transition: all 0.3s ease;
}
.chart-card:hover {
  border-color: var(--accent);
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

.chart-container {
  width: 100%;
}

html.light .chart-card { box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05); }
html.light .card-title { color: #1a1a2e; }
</style>