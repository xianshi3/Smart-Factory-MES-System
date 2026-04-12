<!-- Process Template Management View -->
<template>
  <div class="process-container">
    <div class="page-header">
      <div class="header-title">
        <el-icon size="24"><Setting /></el-icon>
        <h1>工艺管理</h1>
      </div>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        <span>新建模板</span>
      </el-button>
    </div>
    
    <div class="search-bar">
      <el-input v-model="searchForm.name" placeholder="模板名称" clearable>
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="searchForm.status" placeholder="状态" clearable>
        <el-option label="草稿" value="DRAFT" />
        <el-option label="已发布" value="PUBLISHED" />
      </el-select>
      <el-button type="primary" @click="loadData">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>
    
    <div class="table-card">
      <el-table :data="tableData" v-loading="loading">
        <el-table-column prop="templateName" label="模板名称" min-width="150" />
        <el-table-column prop="version" label="版本" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'">
              {{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="productModel" label="适用型号" min-width="150" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleDetail(row)">详情</el-button>
            <el-button type="warning" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="success" link @click="handlePublish(row)" v-if="row.status === 'DRAFT'">发布</el-button>
            <el-button type="danger" link @click="handleDelete(row)" v-if="row.status === 'DRAFT'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>
    
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="模板名称" prop="templateName">
              <el-input v-model="form.templateName" placeholder="请输入模板名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板编码" prop="templateCode">
              <el-input v-model="form.templateCode" placeholder="如CNC-001" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="适用型号" prop="productModel">
              <el-input v-model="form.productModel" placeholder="如iPhone 16 Pro" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="版本" prop="version">
              <el-input-number v-model="form.version" :min="1" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
    
    <el-dialog v-model="detailVisible" title="模板详情" width="700px">
      <el-descriptions :column="2" border v-if="detailData.id">
        <el-descriptions-item label="模板ID">{{ detailData.id }}</el-descriptions-item>
        <el-descriptions-item label="模板名称">{{ detailData.templateName }}</el-descriptions-item>
        <el-descriptions-item label="模板编码">{{ detailData.templateCode }}</el-descriptions-item>
        <el-descriptions-item label="适用产品型号">{{ detailData.productModel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(detailData.status)">{{ getStatusText(detailData.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="版本号">{{ detailData.version }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailData.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detailData.updateTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ detailData.description || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTemplatePage, getTemplateDetail, createTemplate, updateTemplate, publishTemplate, deleteTemplate } from '@/api/services'

const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新建模板')
const detailVisible = ref(false)
const detailData = ref<any>({})
const tableData = ref<any[]>([])
const searchForm = reactive({ name: '', status: '' })
const pagination = reactive({ page: 1, size: 10, total: 0 })
const formRef = ref()
const form = reactive({
  id: null as number | null,
  templateName: '',
  templateCode: '',
  productModel: '',
  version: 1,
  description: ''
})

const rules = {
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  templateCode: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
  productModel: [{ required: true, message: '请输入适用产品型号', trigger: 'blur' }]
}

const getStatusType = (status: string) => status === 'PUBLISHED' ? 'success' : 'info'
const getStatusText = (status: string) => status === 'PUBLISHED' ? '已发布' : '草稿'

const loadData = async () => {
  loading.value = true
  try {
    const params: any = { current: pagination.page, size: pagination.size }
    if (searchForm.name) params.keyword = searchForm.name
    const res = await getTemplatePage(params)
    tableData.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error) {
    console.error('Failed to load templates:', error)
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  searchForm.name = ''
  searchForm.status = ''
  pagination.page = 1
  loadData()
}

const handleCreate = () => {
  form.id = null
  form.templateName = ''
  form.templateCode = ''
  form.productModel = ''
  form.version = 1
  form.description = ''
  dialogTitle.value = '新建模板'
  dialogVisible.value = true
}

const handleDetail = async (row: any) => {
  try {
    const res = await getTemplateDetail(row.id)
    detailData.value = res.data || {}
    detailVisible.value = true
  } catch (error) {
    ElMessage.error('获取详情失败')
  }
}

const handleEdit = async (row: any) => {
  if (row.status === 'PUBLISHED') {
    ElMessage.warning('已发布的模板不能直接修改，请先创建新版本')
    return
  }
  try {
    const res = await getTemplateDetail(row.id)
    const data = res.data
    form.id = data.id
    form.templateName = data.templateName
    form.templateCode = data.templateCode
    form.productModel = data.productModel
    form.version = data.version
    form.description = data.description || ''
    dialogTitle.value = '编辑模板'
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error('加载模板详情失败')
  }
}

const handlePublish = async (row: any) => {
  try {
    await publishTemplate(row.id)
    ElMessage.success('发布成功')
    loadData()
  } catch (error) {
    ElMessage.error('发布失败')
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(
    `确定要删除模板 "${row.templateName}" 吗？`,
    '删除确认',
    { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      await deleteTemplate(row.id)
      ElMessage.success('删除成功')
      loadData()
    } catch (error: any) {
      ElMessage.error(error?.response?.data?.message || '删除失败')
    }
  }).catch(() => {})
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (form.id) {
          await updateTemplate(form.id, form)
          ElMessage.success('更新成功')
        } else {
          await createTemplate(form)
          ElMessage.success('创建成功')
        }
        dialogVisible.value = false
        loadData()
      } catch (error: any) {
        const msg = error?.response?.data?.message || error?.message || '保存失败'
        if (msg.includes('已发布') || msg.includes('版本')) {
          ElMessage.warning('已发布的模板不能直接修改，请先创建新版本')
        } else {
          ElMessage.error(msg)
        }
      }
    }
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.process-container {
  padding: 24px;
  background: var(--bg-app);
  min-height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  animation: fadeIn 0.5s ease;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--text-primary);
}

.header-title h1 {
  font-size: 24px;
  font-weight: 600;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  animation: fadeIn 0.5s ease 0.1s both;
}

.search-bar .el-input {
  width: 220px;
}

.search-bar .el-select {
  width: 150px;
}

.table-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 20px;
  animation: fadeIn 0.5s ease 0.2s both;
}

.table-card .el-table {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: var(--hover-bg);
  --el-table-border-color: var(--border-color);
  --el-table-text-color: var(--text-primary);
  --el-table-header-text-color: var(--text-secondary);
}

.table-card .el-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

html.light .header-title h1 { color: #1a1a2e; }
html.light .table-card { box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05); }

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}</style>