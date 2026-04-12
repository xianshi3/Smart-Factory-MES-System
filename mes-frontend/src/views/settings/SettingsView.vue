<template>
  <div class="settings-container">
    <div class="page-header">
      <div class="header-title">
        <el-icon size="24"><Setting /></el-icon>
        <h1>系统设置</h1>
      </div>
    </div>

    <div class="settings-content">
      <div class="settings-section">
        <div class="section-title">外观</div>
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">主题模式</span>
            <span class="setting-desc">选择系统外观主题</span>
          </div>
          <div class="setting-control">
            <el-switch
              v-model="isDark"
              :active-action-icon="Moon"
              :inactive-action-icon="Sunny"
              @change="themeStore.toggleTheme()"
            />
          </div>
        </div>
      </div>

      <div class="settings-section">
        <div class="section-title">通知</div>
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">设备告警通知</span>
            <span class="setting-desc">设备状态变化时推送通知</span>
          </div>
          <div class="setting-control">
            <el-switch v-model="settings.deviceAlarm" />
          </div>
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">工单进度通知</span>
            <span class="setting-desc">工单状态变化时推送通知</span>
          </div>
          <div class="setting-control">
            <el-switch v-model="settings.workOrderNotify" />
          </div>
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">质量异常通知</span>
            <span class="setting-desc">质量检验不合格时推送通知</span>
          </div>
          <div class="setting-control">
            <el-switch v-model="settings.qualityNotify" />
          </div>
        </div>
      </div>

      <div class="settings-section">
        <div class="section-title">数据</div>
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">自动刷新</span>
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
            <span class="setting-label">数据保留天数</span>
            <span class="setting-desc">历史数据保留时间</span>
          </div>
          <div class="setting-control">
            <el-select v-model="settings.dataRetention" style="width: 120px">
              <el-option label="7天" :value="7" />
              <el-option label="30天" :value="30" />
              <el-option label="90天" :value="90" />
              <el-option label="180天" :value="180" />
              <el-option label="365天" :value="365" />
            </el-select>
          </div>
        </div>
      </div>

      <div class="settings-section">
        <div class="section-title">关于</div>
        <div class="about-info">
          <div class="about-item">
            <span class="about-label">系统版本</span>
            <span class="about-value">v1.0.0</span>
          </div>
          <div class="about-item">
            <span class="about-label">构建时间</span>
            <span class="about-value">2026-04-12</span>
          </div>
          <div class="about-item">
            <span class="about-label">前端框架</span>
            <span class="about-value">Vue 3 + Element Plus</span>
          </div>
          <div class="about-item">
            <span class="about-label">后端框架</span>
            <span class="about-value">Spring Cloud + MyBatis-Plus</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useThemeStore } from '@/stores/theme'
import { Setting, User, Moon, Sunny } from '@element-plus/icons-vue'

const themeStore = useThemeStore()

const isDark = computed({
  get: () => themeStore.isDark,
  set: () => themeStore.toggleTheme()
})

const settings = reactive({
  deviceAlarm: true,
  workOrderNotify: true,
  qualityNotify: true,
  autoRefresh: 30,
  dataRetention: 30
})
</script>

<style scoped>
.settings-container {
  padding: 24px;
  background: var(--bg-app);
  min-height: 100%;
}

.page-header {
  margin-bottom: 24px;
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

.header-title .el-icon {
  color: var(--accent);
}

.settings-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 800px;
}

.settings-section {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 12px;
  padding: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-light);
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
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

.about-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.about-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.about-label {
  font-size: 14px;
  color: var(--text-secondary);
}

.about-value {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

html.light .settings-section {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}
</style>