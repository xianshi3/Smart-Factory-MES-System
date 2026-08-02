<template>
  <div class="settings-container">
    <div class="page-header">
      <div class="header-left">
        <div class="page-title">
          <el-icon><Setting /></el-icon>
          <h1>系统设置</h1>
        </div>
        <p class="page-desc">管理系统外观、生产、设备、质量与通知等偏好</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" class="save-btn" :loading="saving" @click="handleSave">
          <el-icon><Check /></el-icon>
          保存设置
        </el-button>
      </div>
    </div>

    <div class="settings-content" v-loading="loading">
      <!-- 外观设置 -->
      <div class="settings-section">
        <div class="section-title">
          <el-icon><Monitor /></el-icon>
          外观
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">主题模式</span>
            <span class="setting-desc">切换系统明暗主题</span>
          </div>
          <div class="setting-control">
            <el-switch
              v-model="isDark"
              :active-action-icon="Moon"
              :inactive-action-icon="Sunny"
              @change="handleThemeChange"
            />
          </div>
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">紧凑模式</span>
            <span class="setting-desc">减少界面间距，优化大屏显示</span>
          </div>
          <div class="setting-control">
            <el-switch v-model="settings.compactMode" />
          </div>
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">动画效果</span>
            <span class="setting-desc">启用页面过渡动画</span>
          </div>
          <div class="setting-control">
            <el-switch v-model="settings.animation" />
          </div>
        </div>
      </div>

      <!-- 生产管理 -->
      <div class="settings-section">
        <div class="section-title">
          <el-icon><Document /></el-icon>
          生产管理
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">工单自动编号</span>
            <span class="setting-desc">创建工单时自动生成编号</span>
          </div>
          <div class="setting-control">
            <el-switch v-model="settings.autoOrderNo" />
          </div>
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">默认工艺模板</span>
            <span class="setting-desc">新工单默认使用的工艺模板</span>
          </div>
          <div class="setting-control">
            <el-select v-model="settings.defaultTemplate" style="width: 160px">
              <el-option label="CNC加工工艺" :value="1" />
              <el-option label="组装工艺" :value="2" />
              <el-option label="测试工艺" :value="3" />
            </el-select>
          </div>
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">报工审核</span>
            <span class="setting-desc">报工记录需要主管审核</span>
          </div>
          <div class="setting-control">
            <el-switch v-model="settings.reportApproval" />
          </div>
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">报工自动结单</span>
            <span class="setting-desc">完成报工后自动完成工单</span>
          </div>
          <div class="setting-control">
            <el-switch v-model="settings.autoCompleteOrder" />
          </div>
        </div>
      </div>

      <!-- 设备管理 -->
      <div class="settings-section">
        <div class="section-title">
          <el-icon><Cpu /></el-icon>
          设备管理
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">设备心跳超时</span>
            <span class="setting-desc">设备离线判定时间（秒）</span>
          </div>
          <div class="setting-control">
            <el-input-number v-model="settings.heartbeatTimeout" :min="10" :max="300" style="width: 120px" />
          </div>
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">温度告警阈值</span>
            <span class="setting-desc">超过此温度触发告警（℃）</span>
          </div>
          <div class="setting-control">
            <el-input-number v-model="settings.tempThreshold" :min="40" :max="100" style="width: 120px" />
          </div>
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">WebSocket推送</span>
            <span class="setting-desc">实时推送设备状态变化</span>
          </div>
          <div class="setting-control">
            <el-switch v-model="settings.wsPush" />
          </div>
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">设备控制权限</span>
            <span class="setting-desc">允许远程启动/停止设备</span>
          </div>
          <div class="setting-control">
            <el-switch v-model="settings.deviceControl" />
          </div>
        </div>
      </div>

      <!-- 质量管理 -->
      <div class="settings-section">
        <div class="section-title">
          <el-icon><CircleCheck /></el-icon>
          质量管理
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">默认质检类型</span>
            <span class="setting-desc">新建质检记录时的默认类型</span>
          </div>
          <div class="setting-control">
            <el-select v-model="settings.defaultQcType" style="width: 140px">
              <el-option label="IPQC (制程检验)" value="IPQC" />
              <el-option label="FQC (最终检验)" value="FQC" />
              <el-option label="OQC (出货检验)" value="OQC" />
            </el-select>
          </div>
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">不良品自动标记</span>
            <span class="setting-desc">报工不良数大于0时自动标记</span>
          </div>
          <div class="setting-control">
            <el-switch v-model="settings.autoMarkDefect" />
          </div>
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">质量追溯</span>
            <span class="setting-desc">启用产品全生命周期追溯</span>
          </div>
          <div class="setting-control">
            <el-switch v-model="settings.traceability" />
          </div>
        </div>
      </div>

      <!-- 通知设置 -->
      <div class="settings-section">
        <div class="section-title">
          <el-icon><Bell /></el-icon>
          通知设置
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">设备告警通知</span>
            <span class="setting-desc">设备异常时推送通知</span>
          </div>
          <div class="setting-control">
            <el-switch v-model="settings.deviceAlarmNotify" />
          </div>
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">工单进度通知</span>
            <span class="setting-desc">工单状态变化时通知</span>
          </div>
          <div class="setting-control">
            <el-switch v-model="settings.orderNotify" />
          </div>
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">质量异常通知</span>
            <span class="setting-desc">质检不合格时通知相关人员</span>
          </div>
          <div class="setting-control">
            <el-switch v-model="settings.qualityNotify" />
          </div>
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">系统公告</span>
            <span class="setting-desc">推送系统公告通知</span>
          </div>
          <div class="setting-control">
            <el-switch v-model="settings.systemNotify" />
          </div>
        </div>
      </div>

      <!-- 数据设置 -->
      <div class="settings-section">
        <div class="section-title">
          <el-icon><DataAnalysis /></el-icon>
          数据设置
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">数据自动刷新</span>
            <span class="setting-desc">页面数据自动刷新间隔</span>
          </div>
          <div class="setting-control">
            <el-select v-model="settings.autoRefresh" style="width: 120px">
              <el-option label="5秒" :value="5" />
              <el-option label="10秒" :value="10" />
              <el-option label="30秒" :value="30" />
              <el-option label="1分钟" :value="60" />
              <el-option label="关闭" :value="0" />
            </el-select>
          </div>
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">历史数据保留</span>
            <span class="setting-desc">生产数据保留天数</span>
          </div>
          <div class="setting-control">
            <el-select v-model="settings.dataRetention" style="width: 120px">
              <el-option label="30天" :value="30" />
              <el-option label="90天" :value="90" />
              <el-option label="180天" :value="180" />
              <el-option label="365天" :value="365" />
              <el-option label="2年" :value="730" />
            </el-select>
          </div>
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">日志级别</span>
            <span class="setting-desc">系统日志记录详细程度</span>
          </div>
          <div class="setting-control">
            <el-select v-model="settings.logLevel" style="width: 120px">
              <el-option label="DEBUG" value="DEBUG" />
              <el-option label="INFO" value="INFO" />
              <el-option label="WARN" value="WARN" />
              <el-option label="ERROR" value="ERROR" />
            </el-select>
          </div>
        </div>
      </div>

      <!-- 关于 -->
      <div class="settings-section">
        <div class="section-title">
          <el-icon><InfoFilled /></el-icon>
          关于
        </div>
        <div class="about-grid">
          <div class="about-item">
            <span class="about-label">系统名称</span>
            <span class="about-value">Smart Factory MES</span>
          </div>
          <div class="about-item">
            <span class="about-label">系统版本</span>
            <span class="about-value">v1.0.0</span>
          </div>
          <div class="about-item">
            <span class="about-label">构建日期</span>
            <span class="about-value">2026-04-12</span>
          </div>
          <div class="about-item">
            <span class="about-label">前端技术</span>
            <span class="about-value">Vue 3 + TypeScript + Element Plus + ECharts</span>
          </div>
          <div class="about-item">
            <span class="about-label">后端技术</span>
            <span class="about-value">Spring Cloud + MyBatis-Plus + MySQL</span>
          </div>
          <div class="about-item">
            <span class="about-label">AI 服务</span>
            <span class="about-value">Python + FastAPI + LightGBM + XGBoost</span>
          </div>
        </div>
        <div class="about-actions">
          <el-button type="primary" plain>检查更新</el-button>
          <el-button plain>系统文档</el-button>
          <el-button plain>技术支持</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useThemeStore } from '@/stores/theme'
import { Setting, Monitor, Document, Cpu, CircleCheck, Bell, DataAnalysis, InfoFilled, Moon, Sunny, Check } from '@element-plus/icons-vue'
import request from '@/api'

const themeStore = useThemeStore()

const saving = ref(false)

const handleSave = async () => {
  saving.value = true
  try {
    await request({ url: '/auth/settings', method: 'put', data: settings })
    ElMessage.success('设置已保存')
  } catch {
    ElMessage.error('保存设置失败')
  } finally {
    saving.value = false
  }
}

const isDark = computed({
  get: () => themeStore.isDark,
  set: (val) => {
    if (val !== themeStore.isDark) {
      themeStore.toggleTheme()
    }
  }
})

const handleThemeChange = () => {
  saveSettings()
}

const saveSettings = async () => {
  try {
    await request({ url: '/auth/settings', method: 'put', data: settings })
  } catch {
    // ignore, theme still applied locally
  }
}

const loading = ref(false)

const settings = reactive({
  compactMode: false,
  animation: true,
  autoOrderNo: true,
  defaultTemplate: 1,
  reportApproval: false,
  autoCompleteOrder: false,
  heartbeatTimeout: 60,
  tempThreshold: 70,
  wsPush: true,
  deviceControl: true,
  defaultQcType: 'IPQC',
  autoMarkDefect: true,
  traceability: true,
  deviceAlarmNotify: true,
  orderNotify: true,
  qualityNotify: true,
  systemNotify: true,
  autoRefresh: 30,
  dataRetention: 90,
  logLevel: 'INFO'
})

const loadSettings = async () => {
  loading.value = true
  try {
    const res = await request({ url: '/auth/settings', method: 'get' })
    Object.assign(settings, res?.data || {})
  } catch {
    // ignore, use defaults
  } finally {
    loading.value = false
  }
}

const saveSettings = async () => {
  try {
    await request({ url: '/auth/settings', method: 'put', data: settings })
    ElMessage.success('设置已保存')
  } catch {
    ElMessage.error('保存设置失败')
  }
}

onMounted(() => {
  loadSettings()
})
</script>

<style scoped>
.settings-container {
  background: var(--bg-app);
  min-height: 100%;
}

.page-header {
  margin-bottom: 24px;
}

.save-btn { height: 36px; padding: 0 16px; border-radius: var(--radius-md); }

.settings-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 900px;
}

.settings-section {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 12px;
  padding: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-light);
}

.section-title .el-icon {
  color: var(--accent);
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid var(--border-light);
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.setting-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.setting-desc {
  font-size: 12px;
  color: var(--text-muted);
}

.about-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.about-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.about-label {
  font-size: 12px;
  color: var(--text-muted);
}

.about-value {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.about-actions {
  display: flex;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid var(--border-light);
}

html.light .settings-section {
  box-shadow: var(--shadow-sm);
}

@media (max-width: 768px) {
  .about-grid {
    grid-template-columns: 1fr;
  }
}
</style>