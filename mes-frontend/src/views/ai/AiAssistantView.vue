<template>
  <div class="ai-page">
    <AiAssistant :floating="false" />
  </div>
</template>

<script setup lang="ts">
import AiAssistant from '@/components/ai/AiAssistant.vue'
</script>

<style scoped>
.ai-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}
</style>
