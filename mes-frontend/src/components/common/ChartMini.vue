<template>
  <div class="chart-mini" :style="{ height }">
    <v-chart :option="option" autoresize />
  </div>
</template>

<script setup lang="ts">
import VChart from 'vue-echarts'

defineProps<{ option: any; height?: string }>()
</script>

<style scoped>
.chart-mini { width: 100%; }
</style>
