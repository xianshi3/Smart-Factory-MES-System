<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <div class="page-title">
          <el-icon size="24"><Box /></el-icon>
          <h1>物料管理</h1>
        </div>
        <p class="page-desc">物料主数据 · 库存查询 · BOM关联</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="handleCreate"><el-icon><Plus /></el-icon>新建物料</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-input v-model="searchForm.keyword" placeholder="搜索物料名称或编码" clearable prefix-icon="Search" @clear="loadData" />
      <el-select v-model="searchForm.materialType" placeholder="物料类型" clearable @change="loadData" style="width:140px">
        <el-option label="所有类型" value="" />
        <el-option label="原材料" value="RAW" />
        <el-option label="半成品" value="SEMI" />
        <el-option label="成品" value="FINISHED" />
        <el-option label="辅助材料" value="SUPPLY" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
    </div>

    <div class="table-card">
      <el-table :data="tableData" v-loading="loading" stripe style="width:100%">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="materialCode" label="物料编码" width="130">
          <template #default="{ row }">
            <span class="code-text">{{ row.materialCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="materialName" label="物料名称" min-width="160">
          <template #default="{ row }">
            <span class="name-text">{{ row.materialName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="typeMap[row.materialType] || 'info'" size="small" effect="plain">
              {{ typeLabel(row.materialType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="70" align="center" />
        <el-table-column prop="spec" label="规格型号" min-width="150">
          <template #default="{ row }">
            <span class="spec-text">{{ row.spec || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="defaultPrice" label="单价" width="100" align="right">
          <template #default="{ row }">
            <span class="price-text">{{ row.defaultPrice ? '¥' + row.defaultPrice.toFixed(2) : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="库存" width="130" align="center">
          <template #default="{ row }">
            <span :class="['stock-text', { 'stock-low': (row.currentStock || 0) <= (row.minStock || 0) }]">
              {{ row.currentStock || 0 }} {{ row.unit || '' }}
            </span>
            <el-tag v-if="(row.currentStock || 0) <= (row.minStock || 0)" type="danger" size="small" effect="dark" class="low-tag">偏低</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small" effect="plain">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" link @click="handleInventory(row)">库存</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="pagination-wrapper">
      <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size" :total="pagination.total" :page-sizes="[10,20,50,100]" layout="total, sizes, prev, pager, next" @size-change="loadData" @current-change="loadData" background />
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑物料' : '新建物料'" width="600px" class="form-dialog">
      <el-form :model="form" label-width="90px" size="default">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="物料编码" required>
              <el-input v-model="form.materialCode" placeholder="如 RAW-001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="物料名称" required>
              <el-input v-model="form.materialName" placeholder="请输入物料名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="物料类型" required>
              <el-select v-model="form.materialType" style="width:100%">
                <el-option label="原材料" value="RAW" />
                <el-option label="半成品" value="SEMI" />
                <el-option label="成品" value="FINISHED" />
                <el-option label="辅助材料" value="SUPPLY" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位" required>
              <el-select v-model="form.unit" style="width:100%" allow-create filterable>
                <el-option label="个" value="个" />
                <el-option label="张" value="张" />
                <el-option label="kg" value="kg" />
                <el-option label="m" value="m" />
                <el-option label="件" value="件" />
                <el-option label="桶" value="桶" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="规格型号">
          <el-input v-model="form.spec" placeholder="如 1200x800x2mm" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="默认单价">
              <el-input-number v-model="form.defaultPrice" :precision="2" :min="0" style="width:100%" placeholder="0.00" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width:100%">
                <el-option label="启用" value="ACTIVE" />
                <el-option label="停用" value="INACTIVE" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="最低库存">
              <el-input-number v-model="form.minStock" :min="0" style="width:100%" placeholder="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最高库存">
              <el-input-number v-model="form.maxStock" :min="0" style="width:100%" placeholder="0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Box, Plus } from '@element-plus/icons-vue'
import request from '@/api'

const loading = ref(false)
const saving = ref(false)
const tableData = ref<any[]>([])
const dialogVisible = ref(false)
const searchForm = reactive({ keyword: '', materialType: '' })
const pagination = reactive({ page: 1, size: 20, total: 0 })
const form = reactive<any>({})

const typeMap: Record<string, string> = { RAW: 'warning', SEMI: 'primary', FINISHED: 'success', SUPPLY: 'info' }

function typeLabel(v: string) {
  const m: Record<string, string> = { RAW: '原材料', SEMI: '半成品', FINISHED: '成品', SUPPLY: '辅助材料' }
  return m[v] || v
}

async function loadData() {
  loading.value = true
  try {
    const res = await request({ url: '/api/dashboard/material/list', method: 'get', params: { ...searchForm, page: pagination.page, size: pagination.size } })
    const body = res?.data || res || []
    tableData.value = body.records || body || []
    pagination.total = body.total || 0
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

function handleCreate() {
  Object.assign(form, { materialCode: '', materialName: '', materialType: 'RAW', unit: '个', spec: '', defaultPrice: 0, minStock: 0, maxStock: 0, status: 'ACTIVE', description: '' })
  dialogVisible.value = true
}

function handleEdit(row: any) { Object.assign(form, { ...row }); dialogVisible.value = true }

async function handleSubmit() {
  saving.value = true
  try {
    if (form.id) { await request({ url: `/api/dashboard/material/${form.id}`, method: 'put', data: form }); ElMessage.success('更新成功') }
    else { await request({ url: '/api/dashboard/material', method: 'post', data: form }); ElMessage.success('创建成功') }
    dialogVisible.value = false; loadData()
  } catch (e: any) { ElMessage.error(e?.message || '保存失败') }
  finally { saving.value = false }
}

function handleDelete(row: any) {
  ElMessageBox.confirm(`确认删除物料「${row.materialName}」？`, '删除确认', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
    .then(async () => { try { await request({ url: `/api/dashboard/material/${row.id}`, method: 'delete' }); ElMessage.success('已删除'); loadData() } catch { ElMessage.error('删除失败') } })
    .catch(() => {})
}

function handleInventory(row: any) { ElMessage.info('库存管理功能开发中') }

onMounted(() => loadData())
</script>

<style scoped>
.page-container { height: 100%; display: flex; flex-direction: column; }

.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; padding: 14px 18px; background: var(--bg-card); border-radius: var(--radius-md); border: 1px solid var(--border-light); align-items: center; flex-wrap: wrap; }
.filter-bar .el-input { width: 220px; }

.table-card { flex: 1; background: var(--bg-card); border-radius: var(--radius-lg); border: 1px solid var(--border-color); overflow: hidden; }
.table-card :deep(.el-table) { --el-table-bg-color: transparent; --el-table-tr-bg-color: transparent; --el-table-header-bg-color: rgba(0,0,0,0.02); --el-table-row-hover-bg-color: var(--bg-hover); }
.table-card :deep(.el-table th) { font-weight: 600; color: var(--text-secondary); font-size: 13px; }
.table-card :deep(.el-table td) { color: var(--text-primary); font-size: 13px; }
.table-card :deep(.el-table__body tr.el-table__row) { transition: background 0.2s; }
.table-card :deep(.el-table--striped .el-table__body tr.el-table__row--striped td) { background: rgba(0,0,0,0.02); }

.code-text { font-family: 'SF Mono', 'Consolas', monospace; font-weight: 500; color: var(--accent); }
.name-text { font-weight: 500; }
.spec-text { color: var(--text-secondary); }
.price-text { font-weight: 600; font-family: 'SF Mono', 'Consolas', monospace; }

.stock-text { font-weight: 500; }
.stock-text.stock-low { color: var(--danger); }
.low-tag { margin-left: 4px; }

.pagination-wrapper { margin-top: 16px; display: flex; justify-content: flex-end; }

.form-dialog :deep(.el-dialog__body) { padding: 20px 24px; }
</style>
