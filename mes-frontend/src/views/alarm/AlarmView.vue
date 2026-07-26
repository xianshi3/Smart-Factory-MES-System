<template>
  <div class="alarm-page">
    <div class="page-header">
      <div class="header-left">
        <div class="page-title">
          <div class="header-badge" :class="{ pulse: activeCount > 0 }">
            <el-icon><WarningFilled /></el-icon>
            {{ activeCount > 0 ? '紧急' : '正常' }}
          </div>
          <h1>报警管理中心</h1>
        </div>
      </div>
      <div class="header-actions">
        <el-button circle @click="loadAlarms" title="刷新">
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>
    </div>

    <div class="stats-grid">
      <div class="stat-card" :class="{ active: statusFilter === 'ACTIVE' }" @click="filterStatus('ACTIVE')">
        <div class="stat-card-inner">
          <div class="stat-icon-wrap danger">
            <el-icon size="24"><WarningFilled /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ activeCount }}</div>
            <div class="stat-label">活跃告警</div>
          </div>
        </div>
        <div class="stat-glow danger"></div>
      </div>

      <div class="stat-card" :class="{ active: statusFilter === 'ACKNOWLEDGED' }" @click="filterStatus('ACKNOWLEDGED')">
        <div class="stat-card-inner">
          <div class="stat-icon-wrap warning">
            <el-icon size="24"><Warning /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ ackCount }}</div>
            <div class="stat-label">已确认</div>
          </div>
        </div>
        <div class="stat-glow warning"></div>
      </div>

      <div class="stat-card" :class="{ active: statusFilter === 'RESOLVED' }" @click="filterStatus('RESOLVED')">
        <div class="stat-card-inner">
          <div class="stat-icon-wrap success">
            <el-icon size="24"><CircleCheck /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ resolvedCount }}</div>
            <div class="stat-label">已解决</div>
          </div>
        </div>
        <div class="stat-glow success"></div>
      </div>

      <div class="stat-card" @click="filterStatus('')">
        <div class="stat-card-inner">
          <div class="stat-icon-wrap info">
            <el-icon size="24"><List /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ totalCount }}</div>
            <div class="stat-label">告警总数</div>
          </div>
        </div>
        <div class="stat-glow info"></div>
      </div>
    </div>

    <div class="filter-panel">
      <div class="panel-tabs">
        <div 
          class="tab-item" 
          :class="{ active: statusFilter === '' }"
          @click="filterStatus('')"
        >
          全部
          <span class="tab-count">{{ totalCount }}</span>
        </div>
        <div 
          class="tab-item" 
          :class="{ active: statusFilter === 'ACTIVE' }"
          @click="filterStatus('ACTIVE')"
        >
          活跃
          <span class="tab-count danger">{{ activeCount }}</span>
        </div>
        <div 
          class="tab-item" 
          :class="{ active: statusFilter === 'ACKNOWLEDGED' }"
          @click="filterStatus('ACKNOWLEDGED')"
        >
          已确认
          <span class="tab-count warning">{{ ackCount }}</span>
        </div>
        <div 
          class="tab-item" 
          :class="{ active: statusFilter === 'RESOLVED' }"
          @click="filterStatus('RESOLVED')"
        >
          已解决
          <span class="tab-count success">{{ resolvedCount }}</span>
        </div>
      </div>
      <div class="panel-tools">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          size="default"
        />
        <el-input
          v-model="searchKeyword"
          placeholder="搜索告警..."
          style="width: 200px"
          clearable
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
    </div>

    <div class="alarm-table-panel">
      <el-table :data="filteredAlarms" style="width: 100%" v-loading="loading">
        <el-table-column prop="alarmCode" label="告警编码" width="150">
          <template #default="{ row }">
            <div class="alarm-code">{{ row.alarmCode }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="告警信息" min-width="200">
          <template #default="{ row }">
            <div class="alarm-message">{{ row.message }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="级别" width="100">
          <template #default="{ row }">
            <div class="level-tag" :class="(row.level || '').toLowerCase()">
              {{ getLevelText(row.level) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="deviceName" label="设备" width="130">
          <template #default="{ row }">
            <div class="device-name">
              <el-icon><Monitor /></el-icon>
              {{ row.deviceName || '-' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <div class="status-tag" :class="(row.status || '').toLowerCase()">
              {{ getStatusText(row.status) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="occurrenceTime" label="发生时间" width="170">
          <template #default="{ row }">
            <div class="time-cell">{{ formatTime(row.occurrenceTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button 
                v-if="row.status === 'ACTIVE'" 
                type="primary" 
                link 
                size="small"
                @click="handleAck(row)"
              >
                <el-icon><Check /></el-icon>
                确认
              </el-button>
              <el-button 
                v-if="row.status === 'ACKNOWLEDGED'" 
                type="success" 
                link 
                size="small"
                @click="handleResolve(row)"
              >
                <el-icon><CircleCheck /></el-icon>
                解决
              </el-button>
              <el-button 
                type="danger" 
                link 
                size="small"
                @click="handleDelete(row)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="resolveDialogVisible" title="解决告警" width="450px" class="resolve-dialog">
      <div class="resolve-header">
        <el-icon size="40" color="var(--success)"><CircleCheck /></el-icon>
        <div class="resolve-info">
          <h3>确认解决方案</h3>
          <p>请输入解决备注以便后续追溯</p>
        </div>
      </div>
      <el-form>
        <el-form-item label="解决备注">
          <el-input 
            v-model="resolveRemarks" 
            type="textarea" 
            rows="4" 
            placeholder="请描述解决方案和处理结果..." 
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resolveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitResolve">确认解决</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAllAlarms, acknowledgeAlarm, resolveAlarm, deleteAlarm } from '@/api/services'
import { WarningFilled, Warning, CircleCheck, Refresh, List, Monitor, Check, Delete, Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const alarms = ref<any[]>([])
const loading = ref(false)
const statusFilter = ref('')
const searchKeyword = ref('')
const dateRange = ref<[Date, Date] | null>(null)
const resolveDialogVisible = ref(false)
const resolveRemarks = ref('')
const currentAlarm = ref<any>({})

const activeCount = computed(() => alarms.value.filter(a => a.status === 'ACTIVE').length)
const ackCount = computed(() => alarms.value.filter(a => a.status === 'ACKNOWLEDGED').length)
const resolvedCount = computed(() => alarms.value.filter(a => a.status === 'RESOLVED').length)
const totalCount = computed(() => alarms.value.length)

const filteredAlarms = computed(() => {
  let list = alarms.value
  if (statusFilter.value) {
    list = list.filter(a => a.status === statusFilter.value)
  }
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(a => 
      a.message?.toLowerCase().includes(kw) || 
      a.deviceName?.toLowerCase().includes(kw) ||
      a.alarmCode?.toLowerCase().includes(kw)
    )
  }
  return list
})

const loadAlarms = async () => {
  loading.value = true
  try {
    const res = await getAllAlarms()
    alarms.value = res.data || res || []
  } catch (e: any) {
    console.error('[Alarm] Load error:', e)
    ElMessage.error('获取告警列表失败')
  } finally {
    loading.value = false
  }
}

const filterStatus = (status: string) => {
  statusFilter.value = status
  loadAlarms()
}

const handleAck = async (row: any) => {
  try {
    await ElMessageBox.confirm('确认此告警?', '提示', { type: 'info' })
    await acknowledgeAlarm(row.id, userStore.userInfo?.username || 'unknown')
    ElMessage.success('告警已确认')
    loadAlarms()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const handleResolve = async (row: any) => {
  currentAlarm.value = row
  resolveRemarks.value = ''
  resolveDialogVisible.value = true
}

const submitResolve = async () => {
  try {
    await resolveAlarm(currentAlarm.value.id, userStore.userInfo?.username || 'unknown', resolveRemarks.value)
    ElMessage.success('告警已解决')
    resolveDialogVisible.value = false
    loadAlarms()
  } catch (e: any) {
    ElMessage.error('操作失败')
  }
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('删除后无法恢复,确定删除?', '警告', { type: 'warning' })
    await deleteAlarm(row.id)
    ElMessage.success('删除成功')
    loadAlarms()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const getLevelType = (level: string) => {
  const map: Record<string, string> = {
    CRITICAL: 'danger',
    WARNING: 'warning',
    INFO: 'info'
  }
  return map[level] || 'info'
}

const getLevelText = (level: string) => {
  const map: Record<string, string> = {
    CRITICAL: '严重',
    WARNING: '警告',
    INFO: '信息'
  }
  return map[level] || level
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    ACTIVE: '活跃',
    ACKNOWLEDGED: '已确认',
    RESOLVED: '已解决'
  }
  return map[status] || status
}

const formatTime = (time: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
  loadAlarms()
})
</script>

<style scoped>
.alarm-page { padding: 0; }

/* 头部徽章 */
.header-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  background: var(--success-light);
  color: var(--success);
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  width: fit-content;
}

.header-badge.pulse {
  background: var(--danger-light);
  color: var(--danger);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

/* 统计卡片 */
.stat-card {
  position: relative;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: all 0.3s ease;
  cursor: pointer;
}

.stat-card:hover {
  transform: translateY(-4px);
  border-color: var(--accent);
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.15);
}

.stat-card.active {
  border-color: var(--accent);
  background: var(--accent-light);
}

.stat-card-inner {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  position: relative;
  z-index: 1;
}

.stat-icon-wrap {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  flex-shrink: 0;
}

.stat-icon-wrap.danger { background: var(--danger-light); color: var(--danger); }
.stat-icon-wrap.warning { background: var(--warning-light); color: var(--warning); }
.stat-icon-wrap.success { background: var(--success-light); color: var(--success); }
.stat-icon-wrap.info { background: var(--info-light); color: var(--info); }

.stat-content { flex: 1; }

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 4px;
}

.stat-glow {
  position: absolute;
  bottom: -20px;
  left: 50%;
  transform: translateX(-50%);
  width: 100px;
  height: 100px;
  border-radius: 50%;
  filter: blur(40px);
  opacity: 0.3;
}

.stat-glow.danger { background: var(--danger); }
.stat-glow.warning { background: var(--warning); }
.stat-glow.success { background: var(--success); }
.stat-glow.info { background: var(--info); }

/* 筛选面板 */
.filter-panel {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 12px 20px;
  margin-bottom: 20px;
}

.panel-tabs {
  display: flex;
  gap: 8px;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
}

.tab-item:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.tab-item.active {
  background: var(--accent-light);
  color: var(--accent);
  font-weight: 500;
}

.tab-count {
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
  background: var(--bg-hover);
}

.tab-count.danger { background: var(--danger-light); color: var(--danger); }
.tab-count.warning { background: var(--warning-light); color: var(--warning); }
.tab-count.success { background: var(--success-light); color: var(--success); }

.panel-tools {
  display: flex;
  gap: 12px;
  align-items: center;
}

/* 表格面板 */
.alarm-table-panel {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.alarm-code {
  font-family: monospace;
  color: var(--accent);
  font-weight: 500;
}

.alarm-message {
  color: var(--text-primary);
}

.level-tag {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.level-tag.critical {
  background: var(--danger-light);
  color: var(--danger);
}

.level-tag.warning {
  background: var(--warning-light);
  color: var(--warning);
}

.level-tag.info {
  background: var(--info-light);
  color: var(--info);
}

.device-name {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);
}

.status-tag {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-tag.active {
  background: var(--danger-light);
  color: var(--danger);
}

.status-tag.acknowledged {
  background: var(--warning-light);
  color: var(--warning);
}

.status-tag.resolved {
  background: var(--success-light);
  color: var(--success);
}

.time-cell {
  color: var(--text-muted);
  font-size: 13px;
}

.action-buttons {
  display: flex;
  gap: 4px;
}

/* 解决对话框 */
.resolve-dialog .resolve-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: var(--success-light);
  border-radius: var(--radius-lg);
  margin-bottom: 20px;
}

.resolve-info h3 {
  margin: 0 0 4px;
  color: var(--text-primary);
}

.resolve-info p {
  margin: 0;
  color: var(--text-muted);
  font-size: 14px;
}

/* 响应式 */
@media (max-width: 1400px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 1200px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .filter-panel { flex-direction: column; gap: 12px; }
  .panel-tabs { flex-wrap: wrap; }
}
</style>