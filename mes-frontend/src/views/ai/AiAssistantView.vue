<template>
  <div class="ai-page">
    <div class="ai-side">
      <div class="side-brand">
        <div class="brand-icon-wrap">
          <el-icon :size="18"><MagicStick /></el-icon>
        </div>
        <span class="brand-name">AI 助理</span>
      </div>

      <div class="side-stats">
        <div class="st-item">
          <span class="st-val accent">{{ stats[0].value }}</span>
          <span class="st-lbl">在线设备</span>
        </div>
        <div class="st-item">
          <span class="st-val warning">{{ stats[1].value }}</span>
          <span class="st-lbl">活跃告警</span>
        </div>
        <div class="st-item">
          <span class="st-val success">{{ stats[2].value }}</span>
          <span class="st-lbl">进行中工单</span>
        </div>
      </div>

      <div class="side-list">
        <div
          v-for="cap in capabilities"
          :key="cap.label"
          class="scap-item"
          :class="{ loading: cap._loading }"
          @click="triggerCapability(cap)"
        >
          <div class="scap-dot" :class="cap.grad">
            <el-icon :size="13"><component :is="cap.icon" /></el-icon>
          </div>
          <div class="scap-info">
            <span class="scap-title">{{ cap.label }}</span>
            <span class="scap-hint">{{ cap.tag }}</span>
          </div>
          <div v-if="cap._loading" class="scap-spin"></div>
        </div>
      </div>
    </div>

    <div class="ai-main">
      <AiAssistant ref="assistantRef" :floating="false" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { Component } from 'vue'
import AiAssistant from '@/components/ai/AiAssistant.vue'
import {
  MagicStick, Monitor, Warning, Document, Notebook, TrendCharts, Setting,
} from '@element-plus/icons-vue'
import { useAiChatStore } from '@/stores/aiChat'
import axios from 'axios'

const assistantRef = ref<InstanceType<typeof AiAssistant> | null>(null)
const store = useAiChatStore()

interface Capability {
  icon: Component; label: string; tag: string; prompt: string; grad: string; _loading: boolean
}

const stats = reactive([
  { label: '在线设备', value: '--', cls: 'accent' },
  { label: '活跃告警', value: '--', cls: 'warning' },
  { label: '进行中工单', value: '--', cls: 'success' },
])

const capabilities = reactive<Capability[]>([
  { icon: Monitor, label: '设备监控', tag: '实时', prompt: '请列出所有设备，显示设备编号、名称、运行状态、当前温度和主轴转速', grad: 'accent', _loading: false },
  { icon: Warning, label: '异常诊断', tag: '诊断', prompt: '请检查当前有哪些设备存在异常告警，对异常设备逐一分析可能的根因并给出处理建议', grad: 'danger', _loading: false },
  { icon: Document, label: '工单创建', tag: '开工', prompt: '请先检查当前设备状态，如果发现温度过高或运行异常的设备，自动为该设备创建一条 HIGH 优先级的维修工单', grad: 'success', _loading: false },
  { icon: Notebook, label: '手册检索', tag: '知识', prompt: '请搜索知识库中关于 CNC 主轴温度过高的处理流程、维护规范和预防措施', grad: 'info', _loading: false },
  { icon: TrendCharts, label: '数据分析', tag: '洞察', prompt: '请分析最近的生产数据趋势，包括产能利用率、质检合格率和设备OEE，识别潜在瓶颈并给出优化建议', grad: 'warning', _loading: false },
  { icon: Setting, label: '自动规则', tag: '监控', prompt: '请帮我创建一条监控规则：当任意设备温度超过55°C超过5分钟时，自动创建一条紧急维修工单并通知设备工程师', grad: 'accent', _loading: false },
])

function triggerCapability(cap: Capability) {
  if (cap._loading || store.loading) return
  cap._loading = true
  store.sendMessage(cap.prompt).finally(() => { cap._loading = false })
}

async function loadStats() {
  try {
    const [devRes, alarmRes] = await Promise.all([
      axios.get('/api/dashboard/device/status', { timeout: 4000 }),
      axios.get('/api/dashboard/alarm/active-count', { timeout: 4000 }).catch(() => ({ data: { count: 0 } })),
    ])
    const devices = devRes.data?.data ?? devRes.data ?? []
    stats[0].value = String(Array.isArray(devices) ? devices.length : '--')
    stats[1].value = String(alarmRes.data?.count ?? alarmRes.data?.data?.count ?? '--')
  } catch { stats[0].value = '离线'; stats[1].value = '--' }
}

onMounted(() => { loadStats() })
</script>

<style scoped>
.ai-page { display: flex; height: 100%; }

/* ===== Left Side ===== */
.ai-side {
  width: 220px; flex-shrink: 0;
  background: var(--bg-sidebar, #0d0d12);
  border-right: 1px solid var(--border-color, #252530);
  display: flex; flex-direction: column; gap: 0;
}
.side-brand {
  display: flex; align-items: center; gap: 8px;
  padding: 16px 16px 12px; border-bottom: 1px solid var(--border-color, #252530);
}
.brand-icon-wrap {
  width: 30px; height: 30px; border-radius: 8px;
  background: var(--gradient-primary, linear-gradient(135deg, #6366f1, #8b5cf6));
  color: #fff; display: flex; align-items: center; justify-content: center;
}
.brand-name { font-size: 13px; font-weight: 600; color: var(--text-primary, #f0f0f5); }

.side-stats {
  display: flex; gap: 0; padding: 12px 12px;
  border-bottom: 1px solid var(--border-color, #252530);
}
.st-item { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 1px; }
.st-val { font-size: 16px; font-weight: 700; }
.st-val.accent { color: var(--accent, #6366f1); }
.st-val.warning { color: var(--warning, #f59e0b); }
.st-val.success { color: var(--success, #10b981); }
.st-lbl { font-size: 10px; color: var(--text-muted, #505060); }

.side-list { flex: 1; overflow-y: auto; padding: 8px; }
.scap-item {
  display: flex; align-items: center; gap: 10px;
  padding: 9px 10px; border-radius: 8px;
  cursor: pointer; transition: all 0.15s ease; position: relative;
}
.scap-item:hover { background: var(--bg-hover, #1a1a28); }
.scap-item.loading { pointer-events: none; opacity: 0.6; }
.scap-dot {
  width: 30px; height: 30px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0; color: #fff;
}
.scap-dot.accent  { background: var(--accent, #6366f1); }
.scap-dot.success { background: var(--success, #10b981); }
.scap-dot.warning { background: var(--warning, #f59e0b); }
.scap-dot.danger  { background: var(--danger, #ef4444); }
.scap-dot.info    { background: var(--accent-secondary, #22d3ee); }
.scap-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 1px; }
.scap-title { font-size: 12px; font-weight: 500; color: var(--text-primary, #f0f0f5); }
.scap-hint { font-size: 10px; color: var(--text-muted, #505060); }
.scap-spin {
  width: 14px; height: 14px; border: 2px solid var(--border-color, #252530);
  border-top-color: var(--accent, #6366f1); border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ===== Main Chat ===== */
.ai-main {
  flex: 1; min-width: 0;
  background: var(--bg-card, #12121a);
}
</style>
