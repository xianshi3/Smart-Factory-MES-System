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

    <div v-if="loading" class="card-grid">
      <el-skeleton v-for="i in 6" :key="i" animated>
        <template #template>
          <el-skeleton-item variant="rect" style="height: 200px; border-radius: var(--radius-lg)" />
        </template>
      </el-skeleton>
    </div>

    <div v-else class="card-grid">
      <div 
        v-for="row in tableData" 
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
          <div class="template-time">
            <el-icon><Clock /></el-icon>
            <span>更新: {{ row.updateTime ? row.updateTime.substring(0, 16) : '-' }}</span>
          </div>
        </div>
        
        <div class="card-footer">
          <span :class="['status-tag', row.status === 'PUBLISHED' ? 'status-tag--success' : 'status-tag--info']">
            {{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
          </span>
          <div class="card-actions" @click.stop>
            <el-button size="small" link @click="handleDetail(row)">详情</el-button>
            <el-dropdown trigger="click" @command="(cmd: string) => handleCommand(cmd, row)">
              <el-button size="small" link>
                <el-icon><MoreFilled /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="copy">复制为草稿</el-dropdown-item>
                  <el-dropdown-item command="check">参数校验</el-dropdown-item>
                  <el-dropdown-item v-if="row.status === 'DRAFT'" command="edit">编辑</el-dropdown-item>
                  <el-dropdown-item v-if="row.status === 'DRAFT'" command="publish">发布</el-dropdown-item>
                  <el-dropdown-item v-if="row.status === 'DRAFT'" command="delete" divided :class="'danger-item'">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
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

    <el-dialog v-model="detailVisible" title="模板详情" width="760px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="模板名称">{{ detail.template?.templateName }}</el-descriptions-item>
        <el-descriptions-item label="版本">v{{ detail.template?.version }}</el-descriptions-item>
        <el-descriptions-item label="模板编码">{{ detail.template?.templateCode }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <span :class="['status-tag', detail.template?.status === 'PUBLISHED' ? 'status-tag--success' : 'status-tag--info']">
            {{ detail.template?.status === 'PUBLISHED' ? '已发布' : '草稿' }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="产品型号" :span="2">{{ detail.template?.productModel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ detail.template?.description || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="detail-section">
        <div class="section-title">
          <span>工艺参数（{{ detail.parameters?.length || 0 }}）</span>
          <el-button v-if="detailEditable" size="small" type="primary" link @click="openParamDialog(null)">+ 新增参数</el-button>
        </div>
        <el-table :data="detail.parameters || []" size="small" border>
          <el-table-column prop="paramCode" label="参数编码" width="120" />
          <el-table-column prop="paramName" label="参数名称" width="120" />
          <el-table-column prop="paramValue" label="默认值" width="90" />
          <el-table-column label="范围" min-width="120">
            <template #default="{ row }">{{ row.minValue ?? '-' }} ~ {{ row.maxValue ?? '-' }} {{ row.unit || '' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="110" v-if="detailEditable">
            <template #default="{ row }">
              <el-button size="small" link type="primary" @click="openParamDialog(row)">编辑</el-button>
              <el-button size="small" link type="danger" @click="handleDeleteParam(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="!detail.parameters?.length" class="section-empty">暂无参数配置</div>
      </div>

      <div class="detail-section">
        <div class="section-title">
          <span>工序步骤（{{ detail.steps?.length || 0 }}）</span>
          <el-button v-if="detailEditable" size="small" type="primary" link @click="openStepDialog(null)">+ 新增工序</el-button>
        </div>
        <el-table :data="detail.steps || []" size="small" border>
          <el-table-column prop="sequence" label="顺序" width="60" />
          <el-table-column prop="stepNo" label="序号" width="60" />
          <el-table-column prop="stepName" label="工序名称" width="140" />
          <el-table-column prop="stepDesc" label="描述" min-width="160" />
          <el-table-column prop="durationMin" label="工时(分)" width="80" />
          <el-table-column label="操作" width="110" v-if="detailEditable">
            <template #default="{ row }">
              <el-button size="small" link type="primary" @click="openStepDialog(row)">编辑</el-button>
              <el-button size="small" link type="danger" @click="handleDeleteStep(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="!detail.steps?.length" class="section-empty">暂无工序步骤</div>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="paramDialogVisible" :title="paramForm.id ? '编辑参数' : '新增参数'" width="460px">
      <el-form :model="paramForm" label-width="80px">
        <el-form-item label="参数名称"><el-input v-model="paramForm.paramName" placeholder="如: 主轴转速" /></el-form-item>
        <el-form-item label="参数编码"><el-input v-model="paramForm.paramCode" placeholder="如: SPINDLE_SPEED" /></el-form-item>
        <el-form-item label="默认值"><el-input v-model="paramForm.paramValue" placeholder="可选" /></el-form-item>
        <el-form-item label="下限"><el-input-number v-model="paramForm.minValue" style="width: 100%" /></el-form-item>
        <el-form-item label="上限"><el-input-number v-model="paramForm.maxValue" style="width: 100%" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="paramForm.unit" placeholder="如: rpm / ℃ / mm" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="paramForm.sortOrder" :min="1" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="paramDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitParam">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="stepDialogVisible" :title="stepForm.id ? '编辑工序' : '新增工序'" width="460px">
      <el-form :model="stepForm" label-width="80px">
        <el-form-item label="工序名称"><el-input v-model="stepForm.stepName" placeholder="如: 粗加工" /></el-form-item>
        <el-form-item label="工序序号"><el-input-number v-model="stepForm.stepNo" :min="1" style="width: 100%" /></el-form-item>
        <el-form-item label="执行顺序"><el-input-number v-model="stepForm.sequence" :min="1" style="width: 100%" /></el-form-item>
        <el-form-item label="工时(分)"><el-input-number v-model="stepForm.durationMin" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="stepForm.stepDesc" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stepDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitStep">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="checkVisible" title="参数校验" width="560px">
      <el-form :model="checkForm" label-width="140px" v-if="checkParams.length">
        <el-form-item v-for="p in checkParams" :key="p.paramCode" :label="`${p.paramName} (${p.unit || '无量纲'})`">
          <el-input-number v-model="checkForm[p.paramCode]" :min="0" style="width: 100%" />
          <div class="form-hint" v-if="p.minValue != null || p.maxValue != null">范围: {{ p.minValue ?? '-' }} ~ {{ p.maxValue ?? '-' }}</div>
        </el-form-item>
      </el-form>
      <div v-if="checkResult" class="check-result">
        <div class="check-result-title">
          <el-icon :class="checkResult.passed ? 'check-ok' : 'check-fail'">
            <CircleCheck v-if="checkResult.passed" /><WarningFilled v-else />
          </el-icon>
          <span>{{ checkResult.passed ? '校验通过' : '校验不通过' }}</span>
        </div>
        <ul v-if="checkResult.errors?.length">
          <li v-for="(err, i) in checkResult.errors" :key="i" class="check-err">{{ err }}</li>
        </ul>
        <ul v-else>
          <li v-for="(d, i) in checkResult.details" :key="i" class="check-detail">{{ d }}</li>
        </ul>
      </div>
      <template #footer>
        <el-button @click="checkVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleDoCheck">开始校验</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getTemplatePage, createTemplate, updateTemplate, publishTemplate, deleteTemplate,
  getTemplateDetailInfo, copyTemplate,
  addTemplateParameter, updateTemplateParameter, deleteTemplateParameter,
  addTemplateStep, updateTemplateStep, deleteTemplateStep,
  checkParameters,
} from '@/api/services'
import { Setting, Plus, Search, Box, Clock, CircleCheck, WarningFilled, MoreFilled } from '@element-plus/icons-vue'

const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新建模板')
const detailVisible = ref(false)
const detail = ref<any>({})
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

// ===== 参数管理 =====
const paramDialogVisible = ref(false)
const paramForm = reactive({
  id: null as number | null,
  paramName: '', paramCode: '', paramValue: '',
  minValue: null as number | null, maxValue: null as number | null,
  unit: '', sortOrder: 1,
})

// ===== 工序步骤管理 =====
const stepDialogVisible = ref(false)
const stepForm = reactive({
  id: null as number | null,
  stepName: '', stepNo: 1, sequence: 1, durationMin: null as number | null, stepDesc: ''
})

// ===== 参数校验 =====
const checkVisible = ref(false)
const checkParams = ref<any[]>([])
const checkForm = reactive<any>({})
const checkResult = ref<any>(null)

const detailEditable = computed(() => detail.value?.template?.status === 'DRAFT')

const loadData = async () => {
  loading.value = true
  try {
    const res = await getTemplatePage({ current: pagination.page, size: pagination.size, status: searchForm.status, keyword: searchForm.keyword })
    tableData.value = res?.data?.records || []
    pagination.total = res?.data?.total || 0
  } catch (error) { console.error('Failed to load:', error) }
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
    const res = await getTemplateDetailInfo(row.id)
    detail.value = res?.data || {}
  } catch {
    detail.value = { template: row, parameters: [], steps: [] }
  }
  detailVisible.value = true
}

const handleCopy = async (row: any) => {
  try {
    await ElMessageBox.confirm(`复制模板 "${row.templateName}" 为新草稿？`, '复制模板', { type: 'info' })
    await copyTemplate(row.id)
    ElMessage.success('复制成功，已生成草稿副本')
    loadData()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e?.message || '复制失败') }
}

const handlePublish = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定发布模板 "${row.templateName}" 吗？发布后不可修改`, '发布确认', { type: 'warning' })
    await publishTemplate(row.id); ElMessage.success('发布成功'); loadData()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e?.message || '发布失败') }
}

const handleParamCheck = async (row: any) => {
  checkParams.value = []
  Object.keys(checkForm).forEach(k => delete checkForm[k])
  checkResult.value = null
  try {
    const params = await getTemplateParameters(row.id)
    const list = params?.data || []
    if (!list.length) {
      ElMessage.warning('该模板未配置工艺参数，请先添加参数')
      return
    }
    checkParams.value = list
    checkVisible.value = true
  } catch (e: any) {
    ElMessage.error(e?.message || '加载参数失败')
  }
}

const handleDoCheck = async () => {
  try {
    const res = await checkParameters({ templateId: checkParams.value[0]?.templateId, paramValues: checkForm })
    const data = res?.data
    if (res?.code === 200 || typeof data === 'string') {
      checkResult.value = { passed: true, errors: [], details: checkParams.value.map(p => `${p.paramName}: ${checkForm[p.paramCode] ?? p.paramValue ?? '-'} ${p.unit || ''}（范围 ${p.minValue ?? '-'}~${p.maxValue ?? '-'}）`) }
    }
  } catch (e: any) {
    checkResult.value = { passed: false, errors: [(e?.message || '校验不通过').replace(/^[^:]+:\s*/, '')], details: [] }
  }
}

const openParamDialog = (row: any) => {
  if (row) {
    Object.assign(paramForm, { id: row.id, paramName: row.paramName, paramCode: row.paramCode, paramValue: row.paramValue ?? '', minValue: row.minValue, maxValue: row.maxValue, unit: row.unit ?? '', sortOrder: row.sortOrder ?? 1 })
  } else {
    Object.assign(paramForm, { id: null, paramName: '', paramCode: '', paramValue: '', minValue: null, maxValue: null, unit: '', sortOrder: (detail.value?.parameters?.length || 0) + 1 })
  }
  paramDialogVisible.value = true
}

const handleSubmitParam = async () => {
  const tplId = detail.value.template?.id
  try {
    if (paramForm.id) { await updateTemplateParameter(paramForm.id, paramForm) }
    else { await addTemplateParameter(tplId, paramForm) }
    ElMessage.success(paramForm.id ? '参数已更新' : '参数已添加')
    paramDialogVisible.value = false
    const res = await getTemplateDetailInfo(tplId)
    detail.value = res?.data || {}
  } catch (e: any) { ElMessage.error(e?.message || '保存参数失败') }
}

const handleDeleteParam = async (row: any) => {
  try {
    await ElMessageBox.confirm(`删除参数 "${row.paramName}"？`, '删除确认', { type: 'warning' })
    await deleteTemplateParameter(row.id)
    ElMessage.success('参数已删除')
    const res = await getTemplateDetailInfo(detail.value.template?.id)
    detail.value = res?.data || {}
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e?.message || '删除失败') }
}

const openStepDialog = (row: any) => {
  if (row) {
    Object.assign(stepForm, { id: row.id, stepName: row.stepName, stepNo: row.stepNo, sequence: row.sequence, durationMin: row.durationMin, stepDesc: row.stepDesc ?? '' })
  } else {
    Object.assign(stepForm, { id: null, stepName: '', stepNo: (detail.value?.steps?.length || 0) + 1, sequence: (detail.value?.steps?.length || 0) + 1, durationMin: null, stepDesc: '' })
  }
  stepDialogVisible.value = true
}

const handleSubmitStep = async () => {
  const tplId = detail.value.template?.id
  try {
    if (stepForm.id) { await updateTemplateStep(stepForm.id, stepForm) }
    else { await addTemplateStep(tplId, stepForm) }
    ElMessage.success(stepForm.id ? '工序已更新' : '工序已添加')
    stepDialogVisible.value = false
    const res = await getTemplateDetailInfo(tplId)
    detail.value = res?.data || {}
  } catch (e: any) { ElMessage.error(e?.message || '保存工序失败') }
}

const handleDeleteStep = async (row: any) => {
  try {
    await ElMessageBox.confirm(`删除工序 "${row.stepName}"？`, '删除确认', { type: 'warning' })
    await deleteTemplateStep(row.id)
    ElMessage.success('工序已删除')
    const res = await getTemplateDetailInfo(detail.value.template?.id)
    detail.value = res?.data || {}
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e?.message || '删除失败') }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除模板 "${row.templateName}" 吗？`, '删除确认', { type: 'warning' })
    .then(async () => {
      try { await deleteTemplate(row.id); ElMessage.success('删除成功'); loadData() }
      catch (e: any) { ElMessage.error(e?.message || '删除失败') }
    }).catch(() => {})
}

const handleCommand = (cmd: string, row: any) => {
  if (cmd === 'edit') handleEdit(row)
  else if (cmd === 'copy') handleCopy(row)
  else if (cmd === 'check') handleParamCheck(row)
  else if (cmd === 'publish') handlePublish(row)
  else if (cmd === 'delete') handleDelete(row)
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
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-light);
}

.status-dot { width: 8px; height: 8px; border-radius: 50%; }
.status-dot--success { background: var(--success); box-shadow: 0 0 6px var(--success); }
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

.template-time {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 8px;
}
.template-time .el-icon { font-size: 13px; }

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
}

.card-actions { display: flex; align-items: center; gap: 2px; }
.card-actions :deep(.danger-item) { color: var(--danger); }
.detail-section { margin-top: 20px; }
.section-title { display: flex; justify-content: space-between; align-items: center; font-weight: 600; margin-bottom: 10px; }
.section-empty { text-align: center; color: var(--text-muted); padding: 16px 0; border: 1px dashed var(--border-light); border-radius: 6px; font-size: 13px; }
.form-hint { font-size: 12px; color: var(--text-muted); line-height: 1.6; }
.check-result { margin-top: 12px; padding: 12px 16px; border: 1px solid var(--border-light); border-radius: 6px; background: var(--bg-card); }
.check-result-title { display: flex; align-items: center; gap: 8px; font-weight: 600; margin-bottom: 8px; }
.check-ok { color: var(--success); font-size: 18px; }
.check-fail { color: var(--danger); font-size: 18px; }
.check-err { color: var(--danger); font-size: 13px; line-height: 1.8; }
.check-detail { color: var(--text-secondary); font-size: 13px; line-height: 1.8; }

</style>