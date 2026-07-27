<template>
  <div>
    <div class="page-header">
      <div class="header-left">
        <div class="page-title">
          <el-icon><Box /></el-icon>
          <h1>物料管理</h1>
        </div>
        <p class="page-desc">物料主数据 · 库存查询 · 出入库管理</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>新建物料
        </el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-input v-model="searchForm.keyword" placeholder="物料名称/编码" clearable class="search-input" @clear="loadData" />
      <el-select v-model="searchForm.materialType" placeholder="物料类型" clearable @change="loadData">
        <el-option label="原材料" value="RAW" />
        <el-option label="半成品" value="半成品" />
        <el-option label="成品" value="成品" />
        <el-option label="辅助材料" value="辅助" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="materialCode" label="物料编码" width="140" />
      <el-table-column prop="materialName" label="物料名称" min-width="160" />
      <el-table-column prop="materialType" label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.materialType === 'RAW' ? 'warning' : row.materialType === '成品' ? 'success' : 'info'" size="small">{{ row.materialType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="unit" label="单位" width="70" />
      <el-table-column prop="spec" label="规格型号" width="150" show-overflow-tooltip />
      <el-table-column prop="defaultPrice" label="单价" width="100" align="right">
        <template #default="{ row }">¥{{ row.defaultPrice?.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="库存" width="160">
        <template #default="{ row }">
          <span>{{ row.inventory?.quantity || 0 }} {{ row.unit }}</span>
          <span v-if="row.inventory && row.inventory.quantity <= (row.minStock || 0)" class="stock-warning"> 偏低</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">{{ row.status === 'ACTIVE' ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button type="warning" link size="small" @click="handleInventory(row)">库存</el-button>
          <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size" :total="pagination.total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @size-change="loadData" @current-change="loadData" />
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑物料' : '新建物料'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="物料编码" required>
              <el-input v-model="form.materialCode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="物料名称" required>
              <el-input v-model="form.materialName" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="物料类型" required>
              <el-select v-model="form.materialType" style="width:100%">
                <el-option label="原材料" value="RAW" />
                <el-option label="半成品" value="半成品" />
                <el-option label="成品" value="成品" />
                <el-option label="辅助材料" value="辅助" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位" required>
              <el-input v-model="form.unit" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="规格型号">
          <el-input v-model="form.spec" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="默认单价">
              <el-input-number v-model="form.defaultPrice" :precision="2" :min="0" style="width:100%" />
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
              <el-input-number v-model="form.minStock" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最高库存">
              <el-input-number v-model="form.maxStock" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="2" />
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

const loadData = async () => {
  loading.value = true
  try {
    const res = await request({ url: '/api/dashboard/material/list', method: 'get', params: { ...searchForm, ...pagination } })
    tableData.value = res?.data?.records || res?.data || []
    pagination.total = res?.data?.total || 0
  } catch (e: any) { ElMessage.error(e?.message || '加载失败') }
  finally { loading.value = false }
}

const handleCreate = () => {
  Object.assign(form, { materialCode: '', materialName: '', materialType: 'RAW', unit: '个', spec: '', defaultPrice: 0, minStock: 0, maxStock: 0, status: 'ACTIVE', description: '' })
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  saving.value = true
  try {
    if (form.id) {
      await request({ url: `/api/dashboard/material/${form.id}`, method: 'put', data: form })
      ElMessage.success('更新成功')
    } else {
      await request({ url: '/api/dashboard/material', method: 'post', data: form })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e: any) { ElMessage.error(e?.message || '保存失败') }
  finally { saving.value = false }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除物料 "${row.materialName}"?`, '确认', { type: 'warning' }).then(async () => {
    try {
      await request({ url: `/api/dashboard/material/${row.id}`, method: 'delete' })
      ElMessage.success('删除成功')
      loadData()
    } catch (e: any) { ElMessage.error(e?.message || '删除失败') }
  }).catch(() => {})
}

const handleInventory = (row: any) => {
  ElMessage.info('库存管理功能开发中')
}

onMounted(() => { loadData() })
</script>

<style scoped>
.search-input { width: 200px; }
.stock-warning { color: var(--danger); font-weight: 600; }
</style>