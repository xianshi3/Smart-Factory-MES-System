<!-- Page Header Component - Common page title and actions -->
<template>
  <div class="page-header">
    <div class="header-title">
      <h2>{{ title }}</h2>
    </div>
    <div class="header-actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
}>()
</script>

<style scoped lang="scss">
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  
  .header-title {
    h2 {
      font-size: 20px;
      font-weight: bold;
      color: #ffffff;
      margin: 0;
    }
  }
  
  .header-actions {
    display: flex;
    gap: 12px;
  }
}
</style>