<template>
  <div class="search-bar">
    <el-input
      v-model="keyword"
      :placeholder="placeholder"
      clearable
      class="search-input"
      @keyup.enter="handleSearch"
    >
      <template #prefix>
        <el-icon><Search /></el-icon>
      </template>
    </el-input>
    <el-select v-model="status" placeholder="状态" clearable class="status-select" v-if="showStatus">
      <el-option label="全部" value="" />
      <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
    </el-select>
    <el-button type="primary" @click="handleSearch">
      <el-icon><Search /></el-icon>
      搜索
    </el-button>
    <el-button @click="handleReset">重置</el-button>
    <slot name="extra" />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  placeholder?: string
  showStatus?: boolean
  statusOptions?: { label: string; value: string }[]
}>(), {
  placeholder: '请输入关键词',
  showStatus: false,
  statusOptions: () => []
})

const emit = defineEmits<{
  search: [keyword: string, status: string]
  reset: []
}>()

const keyword = ref('')
const status = ref('')

watch([keyword, status], () => {
  emit('search', keyword.value, status.value)
})

const handleSearch = () => {
  emit('search', keyword.value, status.value)
}

const handleReset = () => {
  keyword.value = ''
  status.value = ''
  emit('reset')
}
</script>

<style scoped>
.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.search-input {
  width: 220px;
}

.status-select {
  width: 140px;
}

html.light .search-input :deep(.el-input__wrapper) {
  background: #fff;
  box-shadow: inset 0 0 0 1px #e0e2e8;
}
</style>