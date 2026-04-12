<template>
  <div class="data-table">
    <el-table
      :data="data"
      :border="border"
      :stripe="stripe"
      :highlight-current-row="highlight"
      :row-class-name="rowClassName"
      @row-click="handleRowClick"
      @selection-change="handleSelectionChange"
    >
      <el-table-column v-if="showSelection" type="selection" width="50" align="center" />
      
      <el-table-column
        v-for="col in columns"
        :key="col.prop"
        :prop="col.prop"
        :label="col.label"
        :width="col.width"
        :min-width="col.minWidth"
        :align="col.align || 'left'"
        :fixed="col.fixed"
        :sortable="col.sortable"
        :show-overflow-tooltip="col.tooltip !== false"
      >
        <template #default="{ row }">
          <!-- Tag rendering for status columns -->
          <el-tag
            v-if="col.type === 'tag'"
            :type="col.getTagType ? col.getTagType(row[col.prop]) : 'info'"
            size="small"
          >
            {{ col.formatter ? col.formatter(row[col.prop], row) : row[col.prop] }}
          </el-tag>
          
          <!-- Button actions -->
          <div v-else-if="col.type === 'actions'" class="action-buttons">
            <el-button
              v-for="(btn, idx) in col.buttons"
              :key="idx"
              :type="btn.type || 'primary'"
              :link="btn.link !== false"
              :size="btn.size || 'small'"
              :disabled="btn.disabled ? btn.disabled(row) : false"
              @click.stop="btn.click(row)"
            >
              {{ btn.text }}
            </el-button>
          </div>
          
          <!-- Custom slot -->
          <slot v-else-if="col.slot" :name="col.slot" :row="row" />
          
          <!-- Default text with optional formatter -->
          <span v-else>
            {{ col.formatter ? col.formatter(row[col.prop], row) : row[col.prop] }}
          </span>
        </template>
      </el-table-column>
    </el-table>
    
    <!-- Pagination -->
    <div v-if="showPagination" class="table-pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="pageSizes"
        :layout="paginationLayout"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

interface Column {
  prop: string
  label: string
  width?: string | number
  minWidth?: string | number
  align?: 'left' | 'center' | 'right'
  fixed?: 'left' | 'right'
  sortable?: boolean
  tooltip?: boolean
  type?: 'tag' | 'actions' | 'slot'
  getTagType?: (val: any) => string
  formatter?: (val: any, row: any) => string
  slot?: string
  buttons?: Array<{
    text: string
    type?: string
    link?: boolean
    size?: string
    disabled?: (row: any) => boolean
    click: (row: any) => void
  }>
}

interface Props {
  data?: any[]
  columns?: Column[]
  border?: boolean
  stripe?: boolean
  highlight?: boolean
  showSelection?: boolean
  showPagination?: boolean
  total?: number
  current?: number
  size?: number
  pageSizes?: number[]
  rowClassName?: (row: any) => string
}

const props = withDefaults(defineProps<Props>(), {
  data: () => [],
  columns: () => [],
  border: false,
  stripe: true,
  highlight: false,
  showSelection: false,
  showPagination: true,
  total: 0,
  current: 1,
  size: 10,
  pageSizes: () => [10, 20, 50],
  rowClassName: () => ''
})

const emit = defineEmits(['row-click', 'selection-change', 'page-change', 'size-change'])

const currentPage = ref(props.current)
const currentSize = ref(props.size)

watch(() => props.current, (val) => { currentPage.value = val })
watch(() => props.size, (val) => { currentSize.value = val })

const handleRowClick = (row: any) => {
  emit('row-click', row)
}

const handleSelectionChange = (selection: any[]) => {
  emit('selection-change', selection)
}

const handleSizeChange = (val: number) => {
  emit('size-change', val)
  emit('page-change', { page: currentPage.value, size: val })
}

const handleCurrentChange = (val: number) => {
  emit('page-change', { page: val, size: currentSize.value })
}
</script>

<style scoped>
.data-table {
  width: 100%;
}

.data-table :deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-border-color: var(--border-color);
  --el-table-text-color: var(--text-primary);
  width: 100%;
}

.data-table :deep(.el-table::before),
.data-table :deep(.el-table::after) {
  display: none;
}

.data-table :deep(.el-table th.el-table__cell) {
  background: var(--table-header-bg);
  color: var(--text-secondary);
  font-weight: 600;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 12px;
  border-bottom: 1px solid var(--border-color);
}

.data-table :deep(.el-table td.el-table__cell) {
  padding: 12px;
  border-bottom: 1px solid var(--border-color);
}

.data-table :deep(.el-table__body tr:hover > td.el-table__cell) {
  background: var(--table-row-hover);
}

.data-table :deep(.el-table__body tr.el-table__row--striped > td.el-table__cell) {
  background: var(--table-stripe);
}

.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.table-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

/* Light theme */
html.light .data-table :deep(.el-table th.el-table__cell) {
  background: #f8f9fb;
}

html.light .data-table :deep(.el-table__body tr:hover > td.el-table__cell) {
  background: #f0f2f5;
}

html.light .data-table :deep(.el-table__body tr.el-table__row--striped > td) {
  background: #fafafa;
}
</style>