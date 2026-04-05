<!-- Process Template Management View -->
<template>
  <div class="process-container">
    <page-header title="工艺管理">
      <template #actions>
        <el-button type="primary" @click="handleCreate">新建模板</el-button>
      </template>
    </page-header>
    
    <div class="search-bar">
      <el-input v-model="searchForm.name" placeholder="模板名称" clearable style="width: 200px;" />
      <el-select v-model="searchForm.status" placeholder="状态" clearable style="width: 150px;">
        <el-option label="草稿" value="draft" />
        <el-option label="已发布" value="published" />
      </el-select>
      <el-button type="primary" @click="loadData">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>
    
    <el-table :data="tableData" style="width: 100%; margin-top: 20px;" v-loading="loading">
      <el-table-column prop="name" label="模板名称" />
      <el-table-column prop="version" label="版本" />
      <el-table-column prop="status" label="状态">
        <template #default="{ row }">
          <el-tag :type="row.status === 'published' ? 'success' : 'info'">
            {{ row.status === 'published' ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="category" label="类别" />
      <el-table-column prop="createTime" label="创建时间" />
      <el-table-column label="操作" width="250">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleDetail(row)">详情</el-button>
          <el-button type="warning" link @click="handleEdit(row)">编辑</el-button>
          <el-button type="success" link @click="handlePublish(row)" v-if="row.status === 'draft'">发布</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <el-pagination
      v-model:current-page="pagination.page"
      v-model:page-size="pagination.size"
      :total="pagination.total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      style="margin-top: 20px; justify-content: flex-end;"
      @size-change="loadData"
      @current-change="loadData"
    />
    
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="800px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="模板名称" prop="name">
              <el-input v-model="form.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="版本" prop="version">
              <el-input v-model="form.version" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="类别" prop="category">
              <el-select v-model="form.category" placeholder="请选择">
                <el-option label="CNC加工" value="CNC" />
                <el-option label="组装" value="Assembly" />
                <el-option label="检测" value="Inspection" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工序数量">
              <el-input-number v-model="form.stepCount" :min="1" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="工艺描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/common/PageHeader.vue'
import { getTemplatePage, getTemplateDetail, createTemplate, updateTemplate, publishTemplate } from '@/api/services'

const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新建模板')
const tableData = ref<any[]>([])
const searchForm = reactive({ name: '', status: '' })
const pagination = reactive({ page: 1, size: 10, total: 0 })
const formRef = ref()
const form = reactive({
  id: null as number | null,
  name: '',
  version: 'V1.0',
  category: '',
  stepCount: 1,
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  version: [{ required: true, message: '请输入版本', trigger: 'blur' }],
  category: [{ required: true, message: '请选择类别', trigger: 'change' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getTemplatePage({
      current: pagination.page,
      size: pagination.size,
      name: searchForm.name,
      status: searchForm.status
    })
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
  form.name = ''
  form.version = 'V1.0'
  form.category = ''
  form.stepCount = 1
  form.description = ''
  dialogTitle.value = '新建模板'
  dialogVisible.value = true
}

const handleDetail = async (row: any) => {
  try {
    const res = await getTemplateDetail(row.id)
    ElMessage.info(`查看模板详情: ${res.data.name}`)
  } catch (error) {
    console.error('Failed to load template detail:', error)
  }
}

const handleEdit = async (row: any) => {
  try {
    const res = await getTemplateDetail(row.id)
    const data = res.data
    form.id = data.id
    form.name = data.name
    form.version = data.version
    form.category = data.category
    form.stepCount = data.stepCount || 1
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
      } catch (error) {
        ElMessage.error('保存失败')
      }
    }
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.process-container {
  .search-bar {
    display: flex;
    gap: 12px;
    margin-top: 20px;
  }
  
  :deep(.el-table) {
    background: transparent;
    
    th, td {
      background: transparent;
      color: #ffffff;
      border-color: rgba(255, 255, 255, 0.1);
    }
    
    th {
      background: rgba(255, 255, 255, 0.05);
    }
  }
  
  :deep(.el-pagination) {
    .el-pagination__total, .el-pagination__jump {
      color: #ffffff;
    }
    
    .el-pager li {
      background: transparent;
      color: #ffffff;
      
      &.is-active {
        color: #e94560;
      }
    }
  }
}
</style>