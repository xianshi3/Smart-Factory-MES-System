<template>
  <div class="page-wrapper">
    <div class="page-header">
      <div class="header-left">
        <div class="page-title">
          <el-icon><Setting /></el-icon>
          <h1>工艺管理</h1>
        </div>
        <p class="page-desc">工艺模板 · 版本管理 · 流程配置</p>
      </div>
      <el-button type="primary" class="create-btn" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        新建模板
      </el-button>
    </div>

    <div class="filter-bar">
      <el-input v-model="searchForm.keyword" placeholder="模板名称..." clearable class="search-input">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="searchForm.status" placeholder="状态" clearable class="status-select">
        <el-option label="草稿" value="DRAFT" />
        <el-option label="已发布" value="PUBLISHED" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div class="card-grid">
      <div 
        v-for="(row, index) in tableData" 
        :key="row.id" 
        class="template-card"
      >
        <div class="card-header">
          <span :class="['status-dot', row.status === 'PUBLISHED' ? 'status-dot--success' : 'status-dot--info']"></span>
          <span class="template-version">v{{ row.version }}</span>
        </div>
        
        <div class="card-body">
          <div class="template-name">{{ row.templateName }}</div>
          <div class="template-code">{{ row.templateCode }}</div>
          <div class="template-model">
            <el-icon><Box /></el-icon>
            <span>{{ row.productModel }}</span>
          </div>
        </div>
        
        <div class="card-footer">
          <span :class="['status-tag', row.status === 'PUBLISHED' ? 'status-tag--success' : 'status-tag--info']">
            {{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
          </span>
          <div class="card-actions" @click.stop>
            <el-button type="warning" size="small" link @click="handleEdit(row)" v-if="row.status === 'DRAFT'">编辑</el-button>
            <el-button type="success" size="small" link @click="handleParamCheck(row)">参数校验</el-button>
            <el-button type="success" size="small" link @click="handlePublish(row)" v-if="row.status === 'DRAFT'">发布</el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)" v-if="row.status === 'DRAFT'">删除</el-button>
          </div>
        </div>
      </div>
      
      <div v-if="tableData.length === 0 && !loading" class="empty-state">
        <el-icon size="48"><Setting /></el-icon>
        <p>暂无模板数据</p>
      </div>
    </div>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[12, 24, 48]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="form.templateName" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="模板编码" prop="templateCode">
          <el-input v-model="form.templateCode" placeholder="请输入模板编码" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="产品型号" prop="productModel">
          <el-input v-model="form.productModel" placeholder="请输入产品型号" />
        </el-form-item>
        <el-form-item label="版本" prop="version">
          <el-input-number v-model="form.version" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="模板详情" width="480px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="模板名称">{{ detailData.templateName }}</el-descriptions-item>
        <el-descriptions-item label="版本">v{{ detailData.version }}</el-descriptions-item>
        <el-descriptions-item label="模板编码">{{ detailData.templateCode }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <span :class="['status-tag', detailData.status === 'PUBLISHED' ? 'status-tag--success' : 'status-tag--info']">
            {{ detailData.status === 'PUBLISHED' ? '已发布' : '草稿' }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="产品型号" :span="2">{{ detailData.productModel || '-' }}</el-descriptions-item>
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
import { getTemplatePage, getTemplateDetail, createTemplate, updateTemplate, publishTemplate, deleteTemplate, checkParameters } from '@/api/services'
import { Setting, Plus, Search, Box } from '@element-plus/icons-vue'

const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新建模板')
const detailVisible = ref(false)
const detailData = ref<any>({})
const tableData = ref<any[]>([])

const searchForm = reactive({ keyword: '', status: '' })
const pagination = reactive({ page: 1, size: 12, total: 0 })
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
  productModel: [{ required: true, message: '请输入产品型号', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getTemplatePage({ current: pagination.page, size: pagination.size, status: searchForm.status, keyword: searchForm.keyword })
    console.log('[Process] loadData res:', res)
    tableData.value = res?.data?.records || []
    pagination.total = res?.data?.total || 0
  } catch (error) { console.error('Failed to load:', error, res) }
  finally { loading.value = false }
}

const handleReset = () => { searchForm.keyword = ''; searchForm.status = ''; pagination.page = 1; loadData() }

const handleCreate = () => {
  form.id = null
  Object.assign(form, { templateName: '', templateCode: '', productModel: '', version: 1, description: '' })
  dialogTitle.value = '新建模板'
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  form.id = row.id
  Object.assign(form, row)
  dialogTitle.value = '编辑模板'
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    if (form.id) { await updateTemplate(form.id, form); ElMessage.success('更新成功') }
    else { await createTemplate(form); ElMessage.success('创建成功') }
    dialogVisible.value = false
    loadData()
  } catch (error: any) { ElMessage.error(error?.message || '操作失败') }
}

const handleDetail = async (row: any) => {
  try {
    const res = await getTemplateDetail(row.id)
    detailData.value = res.data || {}
    detailVisible.value = true
  } catch { ElMessage.error('获取详情失败') }
}

const handlePublish = async (row: any) => {
  try { await publishTemplate(row.id); ElMessage.success('发布成功'); loadData() }
  catch (e: any) { ElMessage.error(e?.message || '发布失败') }
}

const handleParamCheck = async (row: any) => {
  try {
    const res = await checkParameters({ templateId: row.id, parameters: row })
    const result = res?.data || res
    if (result?.passed) {
      ElMessage.success('参数校验通过')
    } else {
      ElMessage.warning(result?.message || '参数存在异常，请检查')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '参数校验请求失败')
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除模板 "${row.templateName}" 吗？`, '删除确认', { type: 'warning' })
    .then(async () => {
      try { await deleteTemplate(row.id); ElMessage.success('删除成功'); loadData() }
      catch (e: any) { ElMessage.error(e?.message || '删除失败') }
    }).catch(() => {})
}

onMounted(() => { loadData() })
</script>

<style scoped>
.page-wrapper { background: var(--bg-app); min-height: 100% }

.create-btn { height: 36px; padding: 0 16px; border-radius: var(--radius-md); }

.search-input { width: 200px; }
.status-select { width: 120px; }

.template-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 18px;
  cursor: pointer;
  transition: all var(--transition-normal);
  animation: fadeInUp 0.4s ease forwards;
  opacity: 0;
}

.template-card:hover {
  border-color: var(--accent);
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.status-dot { width: 8px; height: 8px; border-radius: 50%; }
.status-dot--success { background: var(--success); }
.status-dot--info { background: var(--info); }

.template-version {
  font-size: 11px;
  color: var(--accent);
  font-weight: 600;
  padding: 2px 8px;
  background: var(--accent-light);
  border-radius: 10px;
}

.card-body { margin-bottom: 14px; }

.template-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.template-code {
  font-size: 12px;
  font-family: monospace;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.template-model {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-muted);
}

.template-model .el-icon { font-size: 14px; }

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
}

.status-dot { width: 8px; height: 8px; border-radius: 50%; }
.status-dot--success { background: var(--success); }
.status-dot--info { background: var(--info); }
</style>