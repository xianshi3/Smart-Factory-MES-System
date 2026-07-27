<template>
  <div class="page-view">
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

    <div class="search-section">
      <el-input v-model="searchForm.keyword" placeholder="搜索物料名称或编码" clearable prefix-icon="Search" @clear="loadData" />
      <el-select v-model="searchForm.materialType" placeholder="物料类型" clearable @change="loadData" style="width:140px">
        <el-option label="全部" value="" /><el-option label="原材料" value="RAW" /><el-option label="半成品" value="SEMI" /><el-option label="成品" value="FINISHED" /><el-option label="辅助材料" value="SUPPLY" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
    </div>

    <div class="table-wrapper">
      <el-table :data="tableData" v-loading="loading" border stripe style="width:100%">
        <el-table-column type="index" label="#" width="40" align="center" />
        <el-table-column prop="materialCode" label="编码" width="105"><template #default="{ row }"><span class="cell-code">{{ row.materialCode }}</span></template></el-table-column>
        <el-table-column prop="materialName" label="物料名称" min-width="160" />
        <el-table-column label="类型" width="120" align="center"><template #default="{ row }"><el-tag :type="typeMap[row.materialType] || 'info'" size="small" effect="plain">{{ typeLabel(row.materialType) }}</el-tag></template></el-table-column>
        <el-table-column prop="unit" label="单位" width="70" align="center"><template #default="{ row }">{{ unitLabel(row.unit) }}</template></el-table-column>
        <el-table-column prop="spec" label="规格" min-width="140"><template #default="{ row }">{{ row.spec || '-' }}</template></el-table-column>
        <el-table-column prop="defaultPrice" label="单价" width="110" align="right"><template #default="{ row }">{{ row.defaultPrice ? '¥' + Number(row.defaultPrice).toFixed(2) : '-' }}</template></el-table-column>
        <el-table-column label="库存" width="110" align="center"><template #default="{ row }"><span :class="{'text-danger': (row.inventory?.quantity || 0) <= (row.minStock || 0)}">{{ row.inventory?.quantity || 0 }}{{ unitLabel(row.unit) }}</span></template></el-table-column>
        <el-table-column prop="status" label="状态" width="120" align="center"><template #default="{ row }"><el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small" effect="plain">{{ row.status === 'ACTIVE' ? '启用' : '停用' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button text type="primary" size="small" @click="handleInventory(row)">库存</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-bar">
        <el-pagination small v-model:current-page="pagination.page" v-model:page-size="pagination.size" :total="pagination.total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @size-change="loadData" @current-change="loadData" background />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑物料' : '新建物料'" width="560px" top="8vh">
      <el-form :model="form" label-width="80px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="物料编码" required><el-input v-model="form.materialCode" placeholder="如 RAW-001" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="物料名称" required><el-input v-model="form.materialName" placeholder="请输入名称" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="类型" required><el-select v-model="form.materialType" style="width:100%"><el-option label="原材料" value="RAW" /><el-option label="半成品" value="SEMI" /><el-option label="成品" value="FINISHED" /><el-option label="辅助材料" value="SUPPLY" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="单位" required><el-select v-model="form.unit" style="width:100%" allow-create filterable><el-option label="个" value="pcs" /><el-option label="张" value="sheet" /><el-option label="kg" value="kg" /><el-option label="米" value="m" /><el-option label="件" value="件" /><el-option label="桶" value="pail" /></el-select></el-form-item></el-col>
        </el-row>
        <el-form-item label="规格型号"><el-input v-model="form.spec" placeholder="如 1200x800x2mm" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="单价"><el-input-number v-model="form.defaultPrice" :precision="2" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="状态"><el-select v-model="form.status" style="width:100%"><el-option label="启用" value="ACTIVE" /><el-option label="停用" value="INACTIVE" /></el-select></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="最低库存"><el-input-number v-model="form.minStock" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="最高库存"><el-input-number v-model="form.maxStock" :min="0" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="备注"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
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
function typeLabel(v: string) { const m: Record<string, string> = { RAW: '原材料', SEMI: '半成品', FINISHED: '成品', SUPPLY: '辅助材料' }; return m[v] || v }
function unitLabel(v: string) { const m: Record<string, string> = { pcs: '个', sheet: '张', kg: 'kg', m: 'm', pail: '桶' }; return m[v] || v || '' }

async function loadData() {
  loading.value = true
  try {
    const res = await request({ url: '/api/dashboard/material/list', method: 'get', params: { ...searchForm, page: pagination.page, size: pagination.size } })
    const d = res?.data || res || []
    tableData.value = d.records || d || []
    pagination.total = d.total || 0
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

function handleCreate() {
  Object.assign(form, { materialCode: '', materialName: '', materialType: 'RAW', unit: 'pcs', spec: '', defaultPrice: 0, minStock: 0, maxStock: 0, status: 'ACTIVE', description: '' })
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
  ElMessageBox.confirm('确认删除「' + row.materialName + '」？', '删除确认', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
    .then(async () => { try { await request({ url: `/api/dashboard/material/${row.id}`, method: 'delete' }); ElMessage.success('已删除'); loadData() } catch { ElMessage.error('删除失败') } }).catch(() => {})
}

function handleInventory() { ElMessage.info('库存管理功能开发中') }

onMounted(() => loadData())
</script>

<style scoped>
.page-view { display: flex; flex-direction: column; gap: 16px; }

.search-section { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.search-section .el-input { width: 240px; }

.table-wrapper { border: 1px solid var(--border-color); border-radius: var(--radius-lg); overflow: hidden; }
.table-wrapper :deep(.el-table th) { font-weight: 600; color: var(--text-secondary); font-size: 13px; }
.cell-code { font-family: SF Mono, Consolas, monospace; color: var(--accent); font-weight: 500; }
.text-danger { color: var(--danger); font-weight: 600; }

.pagination-bar { display: flex; justify-content: flex-end; padding: 12px 16px; border-top: 1px solid var(--border-light); }
</style>