<template>
  <div class="alarm-page">
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">
          <el-icon><WarningFilled /></el-icon>
          报警管理
        </h1>
        <p class="page-subtitle">告警监控 · 确认处理 · 历史记录</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="loadAlarms">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-item" @click="filterStatus('ACTIVE')">
        <div class="stat-icon danger">
          <el-icon size="22"><WarningFilled /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ activeCount }}</div>
          <div class="stat-label">活跃告警</div>
        </div>
      </div>
      <div class="stat-item" @click="filterStatus('ACKNOWLEDGED')">
        <div class="stat-icon warning">
          <el-icon size="22"><Warning /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ ackCount }}</div>
          <div class="stat-label">已确认</div>
        </div>
      </div>
      <div class="stat-item" @click="filterStatus('RESOLVED')">
        <div class="stat-icon success">
          <el-icon size="22"><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ resolvedCount }}</div>
          <div class="stat-label">已解决</div>
        </div>
      </div>
    </div>

    <div class="filter-row">
      <el-radio-group v-model="statusFilter" @change="loadAlarms">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="ACTIVE">活跃</el-radio-button>
        <el-radio-button value="ACKNOWLEDGED">已确认</el-radio-button>
        <el-radio-button value="RESOLVED">已解决</el-radio-button>
      </el-radio-group>
    </div>

    <div class="alarm-table-card">
      <el-table :data="alarms" style="width: 100%" stripe>
        <el-table-column prop="alarmCode" label="告警编码" width="150" />
        <el-table-column prop="message" label="告警信息" min-width="200" />
        <el-table-column prop="level" label="级别" width="100">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.level)">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deviceName" label="设备" width="120" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="occurrenceTime" label="发生时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.occurrenceTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'ACTIVE'" type="primary" link @click="handleAck(row)">确认</el-button>
            <el-button v-if="row.status === 'ACKNOWLEDGED'" type="success" link @click="handleResolve(row)">解决</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="resolveDialogVisible" title="解决告警" width="400px">
      <el-form>
        <el-form-item label="备注">
          <el-input v-model="resolveRemarks" type="textarea" rows="3" placeholder="请输入解决备注..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resolveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitResolve">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getAllAlarms, acknowledgeAlarm, resolveAlarm, deleteAlarm } from '@/api/services'

const alarms = ref<any[]>([])
const statusFilter = ref('')
const activeCount = ref(0)
const ackCount = ref(0)
const resolvedCount = ref(0)
const resolveDialogVisible = ref(false)
const currentAlarm = ref<any>(null)
const resolveRemarks = ref('')

const userStore = JSON.parse(localStorage.getItem('user') || '{}')
const currentUser = userStore.username || 'admin'

const loadAlarms = async () => {
  try {
    const res: any = await getAllAlarms()
    console.log('Alarm API response:', res)
    let allAlarms = []
    if (Array.isArray(res)) {
      allAlarms = res
    } else if (Array.isArray(res?.data)) {
      allAlarms = res.data
    } else if (res?.data && typeof res.data === 'object') {
      allAlarms = Object.values(res.data).filter(v => typeof v === 'object')
    }
    if (statusFilter.value) {
      allAlarms = allAlarms.filter((a: any) => a.status === statusFilter.value)
    }
    alarms.value = allAlarms
    activeCount.value = allAlarms.filter((a: any) => a.status === 'ACTIVE').length
    ackCount.value = allAlarms.filter((a: any) => a.status === 'ACKNOWLEDGED').length
    resolvedCount.value = allAlarms.filter((a: any) => a.status === 'RESOLVED').length
  } catch (e: any) {
    console.error('Load alarms error:', e)
    ElMessage.error('加载告警失败: ' + e.message)
  }
}

const filterStatus = (status: string) => {
  statusFilter.value = status
  loadAlarms()
}

const getLevelType = (level: string) => {
  const map: Record<string, any> = { CRITICAL: 'danger', WARNING: 'warning', INFO: 'info' }
  return map[level] || 'info'
}

const getStatusType = (status: string) => {
  const map: Record<string, any> = { ACTIVE: 'danger', ACKNOWLEDGED: 'warning', RESOLVED: 'success' }
  return map[status] || 'info'
}

const formatTime = (time: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString()
}

const handleAck = async (alarm: any) => {
  try {
    await acknowledgeAlarm(alarm.id, currentUser)
    ElMessage.success('确认成功')
    loadAlarms()
  } catch (e: any) {
    ElMessage.error('确认失败: ' + e.message)
  }
}

const handleResolve = (alarm: any) => {
  currentAlarm.value = alarm
  resolveRemarks.value = ''
  resolveDialogVisible.value = true
}

const submitResolve = async () => {
  try {
    await resolveAlarm(currentAlarm.value.id, currentUser, resolveRemarks.value)
    ElMessage.success('解决成功')
    resolveDialogVisible.value = false
    loadAlarms()
  } catch (e: any) {
    ElMessage.error('解决失败: ' + e.message)
  }
}

const handleDelete = async (alarm: any) => {
  try {
    await deleteAlarm(alarm.id)
    ElMessage.success('删除成功')
    loadAlarms()
  } catch (e: any) {
    ElMessage.error('删除失败: ' + e.message)
  }
}

onMounted(() => {
  loadAlarms()
})
</script>

<style scoped>
.alarm-page {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.page-subtitle {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: var(--bg-card);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.stat-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 12px;
}

.stat-icon.danger {
  background: var(--danger-light);
  color: var(--danger);
}

.stat-icon.warning {
  background: var(--warning-light);
  color: var(--warning);
}

.stat-icon.success {
  background: var(--success-light);
  color: var(--success);
}

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: 14px;
  color: var(--text-muted);
}

.filter-row {
  margin-bottom: 16px;
}

.alarm-table-card {
  background: var(--bg-card);
  border-radius: 12px;
  padding: 20px;
}
</style>