<template>
  <div class="page-view">
    <div class="page-header">
      <div class="header-left">
        <div class="page-title">
          <el-icon size="24"><List /></el-icon>
          <h1>BOM管理</h1>
        </div>
        <p class="page-desc">物料清单 · 产品结构 · BOM维护</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="handleCreate"><el-icon><Plus /></el-icon>新建BOM</el-button>
      </div>
    </div>

    <div class="search-section">
      <el-input v-model="searchForm.keyword" placeholder="搜索BOM编号或名称" clearable prefix-icon="Search" @clear="loadData" />
      <el-select v-model="searchForm.status" placeholder="状态" clearable style="width:120px" @change="loadData">
        <el-option label="全部" value="" />
        <el-option label="草稿" value="DRAFT" />
        <el-option label="已验证" value="VALIDATED" />
        <el-option label="已发布" value="RELEASED" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
    </div>

    <div class="table-wrapper">
      <el-table v-loading="loading" :data="tableData" border stripe style="width:100%">
        <el-table-column type="index" label="#" width="40" align="center" />
        <el-table-column prop="bomCode" label="BOM编号" width="130">
          <template #default="{ row }"><span class="cell-code">{{ row.bomCode }}</span></template>
        </el-table-column>
        <el-table-column prop="bomName" label="BOM名称" min-width="160" />
        <el-table-column prop="productId" label="产品" width="120" align="center">
          <template #default="{ row }">{{ row.productId ? `产品#${row.productId}` : '-' }}</template>
        </el-table-column>
        <el-table-column prop="productQuantity" label="产出数量" width="100" align="right">
          <template #default="{ row }">{{ row.productQuantity ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="70" align="center">
          <template #default="{ row }">{{ row.version || 'V1.0' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small" effect="plain">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button text type="primary" size="small" @click="handleItems(row)">物料</el-button>
            <el-button v-if="row.status === 'DRAFT'" text type="success" size="small" @click="handleValidate(row)">验证</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-bar">
        <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size" small :total="pagination.total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" background @size-change="loadData" @current-change="loadData" />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑BOM' : '新建BOM'" width="560px" top="10vh">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="BOM编号" required>
              <el-input v-model="form.bomCode" placeholder="如 BOM-001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="BOM名称" required>
              <el-input v-model="form.bomName" placeholder="请输入名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="产品ID" required>
              <el-input-number v-model="form.productId" :min="1" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="产出数量">
              <el-input-number v-model="form.productQuantity" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="RELEASED" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="itemVisible" :title="`BOM物料清单 - ${currentBom?.bomCode || ''}`" width="700px" top="8vh">
      <div class="item-toolbar">
        <el-button type="primary" size="small" @click="handleAddItem"><el-icon><Plus /></el-icon>添加物料</el-button>
      </div>
      <el-table :data="items" border stripe size="small" style="width:100%">
        <el-table-column type="index" label="#" width="40" align="center" />
        <el-table-column prop="materialId" label="物料ID" width="80" align="center" />
        <el-table-column label="数量" width="100" align="right">
          <template #default="{ row }">{{ row.quantity ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="单位" width="70" align="center">
          <template #default="{ row }">{{ row.unit || '-' }}</template>
        </el-table-column>
        <el-table-column label="损耗率" width="90" align="right">
          <template #default="{ row }">{{ row.scrapRate != null ? (row.scrapRate * 100).toFixed(1) + '%' : '-' }}</template>
        </el-table-column>
        <el-table-column prop="sequence" label="工序序号" width="80" align="center" />
        <el-table-column prop="remark" label="备注" min-width="120" />
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row, $index }">
            <el-button text type="primary" size="small" @click="handleEditItem(row, $index)">编辑</el-button>
            <el-button text type="danger" size="small" @click="handleDeleteItem(row, $index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="itemVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="itemFormVisible" :title="editingItemIndex >= 0 ? '编辑物料行项' : '添加物料行项'" width="480px" top="12vh">
      <el-form :model="itemForm" label-width="90px">
        <el-form-item label="物料ID" required>
          <el-input-number v-model="itemForm.materialId" :min="1" style="width:100%" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="数量" required>
              <el-input-number v-model="itemForm.quantity" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位">
              <el-input v-model="itemForm.unit" placeholder="个/kg/m" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="损耗率">
              <el-input-number v-model="itemForm.scrapRate" :min="0" :max="1" :step="0.01" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工序序号">
              <el-input-number v-model="itemForm.sequence" :min="0" :step="1" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="itemForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="itemSaving" @click="handleItemSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, List } from '@element-plus/icons-vue'
import { getBomList, createBom, updateBom, deleteBom, validateBom, getBomItems, createBomItem, updateBomItem, deleteBomItem } from '@/api/services'

const loading = ref(false)
const saving = ref(false)
const tableData = ref<any[]>([])
const dialogVisible = ref(false)
const searchForm = reactive({ keyword: '', status: '' })
const pagination = reactive({ page: 1, size: 20, total: 0 })
const form = reactive<any>({})

const itemVisible = ref(false)
const itemFormVisible = ref(false)
const itemSaving = ref(false)
const items = ref<any[]>([])
const currentBom = ref<any>(null)
const itemForm = reactive<any>({})
const editingItemIndex = ref(-1)

function statusTag(v: string) {
  const m: Record<string, string> = { DRAFT: 'info', VALIDATED: 'success', RELEASED: 'primary' }
  return m[v] || 'info'
}
function statusLabel(v: string) {
  const m: Record<string, string> = { DRAFT: '草稿', VALIDATED: '已验证', RELEASED: '已发布' }
  return m[v] || v || '未知'
}

async function loadData() {
  loading.value = true
  try {
    const res = await getBomList({ ...searchForm, page: pagination.page, size: pagination.size })
    const d = res?.data || res || []
    tableData.value = Array.isArray(d) ? d : d.records || []
    pagination.total = d.total || (Array.isArray(d) ? d.length : 0)
  } catch {
    ElMessage.error('加载BOM列表失败')
  } finally {
    loading.value = false
  }
}

function handleCreate() {
  Object.assign(form, { bomCode: '', bomName: '', productId: null, productQuantity: 1, status: 'DRAFT', description: '', version: 'V1.0' })
  dialogVisible.value = true
}

function handleEdit(row: any) {
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.bomCode || !form.bomName) {
    ElMessage.warning('请填写BOM编号和名称')
    return
  }
  saving.value = true
  try {
    if (form.id) {
      await updateBom(form)
      ElMessage.success('更新成功')
    } else {
      await createBom(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleValidate(row: any) {
  try {
    await validateBom(row.id)
    ElMessage.success('BOM验证成功')
    loadData()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '验证失败')
  }
}

function handleDelete(row: any) {
  ElMessageBox.confirm(`确认删除BOM「${row.bomName}」？`, '删除确认', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
    .then(async () => {
      try {
        await deleteBom(row.id)
        ElMessage.success('已删除')
        loadData()
      } catch {
        ElMessage.error('删除失败')
      }
    }).catch(() => {})
}

async function handleItems(row: any) {
  currentBom.value = row
  items.value = []
  itemVisible.value = true
  try {
    const res = await getBomItems(row.id)
    items.value = res?.data || []
  } catch {
    ElMessage.error('获取物料清单失败')
  }
}

function handleAddItem() {
  Object.assign(itemForm, { materialId: null, quantity: 1, unit: '', scrapRate: 0, sequence: 0, remark: '' })
  editingItemIndex.value = -1
  itemFormVisible.value = true
}

function handleEditItem(row: any, index: number) {
  Object.assign(itemForm, { ...row })
  editingItemIndex.value = index
  itemFormVisible.value = true
}

async function handleItemSubmit() {
  if (!currentBom.value?.id) return
  if (!itemForm.materialId || !itemForm.quantity) {
    ElMessage.warning('请填写物料ID和数量')
    return
  }
  itemSaving.value = true
  try {
    if (editingItemIndex.value >= 0) {
      await updateBomItem(currentBom.value.id, itemForm)
      ElMessage.success('更新成功')
    } else {
      await createBomItem(currentBom.value.id, itemForm)
      ElMessage.success('添加成功')
    }
    itemFormVisible.value = false
    const res = await getBomItems(currentBom.value.id)
    items.value = res?.data || []
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '操作失败')
  } finally {
    itemSaving.value = false
  }
}

async function handleDeleteItem(row: any, index: number) {
  if (!currentBom.value?.id || !row.id) return
  try {
    await deleteBomItem(currentBom.value.id, row.id)
    ElMessage.success('已删除')
    items.value.splice(index, 1)
  } catch {
    ElMessage.error('删除失败')
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.page-view { display: flex; flex-direction: column; gap: 16px; }

.search-section { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.search-section .el-input { width: 240px; }

.table-wrapper { border: 1px solid var(--border-color); border-radius: var(--radius-lg); overflow: hidden; }
.table-wrapper :deep(.el-table th) { font-weight: 600; color: var(--text-secondary); font-size: 13px; }
.cell-code { font-family: SF Mono, Consolas, monospace; color: var(--accent); font-weight: 500; }

.pagination-bar { display: flex; justify-content: flex-end; padding: 12px 16px; border-top: 1px solid var(--border-light); }

.item-toolbar { margin-bottom: 12px; }
</style>
