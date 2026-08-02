<template>
  <div class="ai-page">
    <div class="ai-hero">
      <div class="hero-content">
        <div class="hero-icon-wrap">
          <el-icon :size="22"><MagicStick /></el-icon>
        </div>
        <div>
          <h1 class="hero-title">AI 生产助理</h1>
          <p class="hero-sub">智能工厂中枢 — 连接设备、数据与决策，让 AI 管理生产</p>
        </div>
      </div>
      <div class="hero-stats">
        <div class="stat-item" v-for="s in stats" :key="s.label">
          <span class="stat-val" :class="s.cls">{{ s.value }}</span>
          <span class="stat-lbl">{{ s.label }}</span>
        </div>
      </div>
    </div>

    <div class="ai-dashboard">
      <div
        v-for="cap in capabilities"
        :key="cap.label"
        class="cap-card"
        :class="{ loading: cap._loading }"
        @click="triggerCapability(cap)"
      >
        <div class="cap-grad" :class="cap.grad"></div>
        <div class="cap-icon">
          <el-icon :size="22"><component :is="cap.icon" /></el-icon>
        </div>
        <div class="cap-body">
          <h3 class="cap-title">{{ cap.label }}</h3>
          <p class="cap-desc">{{ cap.desc }}</p>
          <span class="cap-tag">{{ cap.tag }}</span>
        </div>
        <div v-if="cap._loading" class="cap-loading-overlay">
          <span class="cap-spin"></span>
        </div>
        <div v-else class="cap-arrow">
          <el-icon :size="14"><ArrowRight /></el-icon>
        </div>
      </div>
    </div>

    <div class="ai-chat-area">
      <AiAssistant ref="assistantRef" :floating="false" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { Component } from 'vue'
import AiAssistant from '@/components/ai/AiAssistant.vue'
import {
  MagicStick, ArrowRight,
  Monitor, Warning, Document, Notebook, TrendCharts, Setting,
} from '@element-plus/icons-vue'
import { useAiChatStore } from '@/stores/aiChat'
import axios from 'axios'

const assistantRef = ref<InstanceType<typeof AiAssistant> | null>(null)
const store = useAiChatStore()

interface Capability {
  icon: Component
  label: string
  desc: string
  tag: string
  prompt: string
  grad: string
  _loading: boolean
}

const stats = reactive([
  { label: '在线设备', value: '--', cls: 'accent' },
  { label: '活跃告警', value: '--', cls: 'warning' },
  { label: '进行中工单', value: '--', cls: 'success' },
])

const capabilities = reactive<Capability[]>([
  {
    icon: Monitor, label: '设备监控', desc: '查看所有设备实时运行状态与温度数据', tag: '实时状态',
    prompt: '请列出所有设备，显示设备编号、名称、运行状态、当前温度和主轴转速',
    grad: 'accent', _loading: false,
  },
  {
    icon: Warning, label: '异常诊断', desc: '自动检测设备异常并给出根因分析和处理建议', tag: '智能诊断',
    prompt: '请检查当前有哪些设备存在异常告警，对异常设备逐一分析可能的根因并给出处理建议',
    grad: 'danger', _loading: false,
  },
  {
    icon: Document, label: '工单创建', desc: '根据异常情况智能生成维修工单，自动设置优先级', tag: '一键开工',
    prompt: '请先检查当前设备状态，如果发现温度过高或运行异常的设备，自动为该设备创建一条 HIGH 优先级的维修工单',
    grad: 'success', _loading: false,
  },
  {
    icon: Notebook, label: '手册检索', desc: '搜索设备维护手册，快速获取操作规程和维修指南', tag: '知识库',
    prompt: '请搜索知识库中关于 CNC 主轴温度过高的处理流程、维护规范和预防措施',
    grad: 'info', _loading: false,
  },
  {
    icon: TrendCharts, label: '数据分析', desc: '分析近期生产趋势、产能利用率和质量变化', tag: '趋势洞察',
    prompt: '请分析最近的生产数据趋势，包括产能利用率、质检合格率和设备OEE，识别潜在瓶颈并给出优化建议',
    grad: 'warning', _loading: false,
  },
  {
    icon: Setting, label: '自动规则', desc: '配置智能监控规则，条件触发时 AI 自动执行操作', tag: '智能监控',
    prompt: '请帮我创建一条监控规则：当任意设备温度超过55°C超过5分钟时，自动创建一条紧急维修工单并通知设备工程师',
    grad: 'accent', _loading: false,
  },
])

function triggerCapability(cap: Capability) {
  if (cap._loading || store.loading) return
  cap._loading = true
  assistantRef.value?.focusInput(cap.prompt)
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
    stats[2].value = '--' // workorder count needs separate endpoint
  } catch {
    stats[0].value = '离线'
    stats[1].value = '--'
  }
}

onMounted(() => { loadStats() })
</script>

<style scoped>
.ai-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--bg-app, #0a0a0f);
}

/* ===== Hero ===== */
.ai-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 28px;
  border-bottom: 1px solid var(--border-color, #252530);
  background: var(--bg-card, #12121a);
  flex-shrink: 0;
  gap: 24px;
}
.hero-content { display: flex; align-items: center; gap: 14px; }
.hero-icon-wrap {
  width: 46px; height: 46px; border-radius: 12px;
  background: var(--gradient-primary, linear-gradient(135deg, #6366f1, #8b5cf6));
  color: #fff; display: flex; align-items: center; justify-content: center;
  box-shadow: 0 6px 20px rgba(99,102,241,0.3);
}
.hero-title { font-size: 20px; font-weight: 700; color: var(--text-primary, #f0f0f5); margin: 0; line-height: 1.2; }
.hero-sub { font-size: 12px; color: var(--text-muted, #505060); margin: 2px 0 0; }

.hero-stats { display: flex; gap: 20px; flex-shrink: 0; }
.stat-item { display: flex; flex-direction: column; align-items: center; gap: 2px; min-width: 64px; }
.stat-val { font-size: 20px; font-weight: 700; }
.stat-val.accent { color: var(--accent, #6366f1); }
.stat-val.warning { color: var(--warning, #f59e0b); }
.stat-val.success { color: var(--success, #10b981); }
.stat-lbl { font-size: 11px; color: var(--text-muted, #505060); white-space: nowrap; }

/* ===== Dashboard Cards ===== */
.ai-dashboard {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  padding: 18px 28px;
  flex-shrink: 0;
  background: var(--bg-app, #0a0a0f);
}

.cap-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 0;
  padding: 20px 18px 16px;
  border-radius: var(--radius-lg, 14px);
  border: 1px solid var(--border-color, #252530);
  background: var(--bg-card, #12121a);
  cursor: pointer;
  transition: all 0.2s ease;
  overflow: hidden;
  min-height: 140px;
}
.cap-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md, 0 4px 12px rgba(0,0,0,0.4));
  border-color: var(--accent, #6366f1);
}
.cap-card.loading { pointer-events: none; opacity: 0.7; }

.cap-grad {
  position: absolute; top: 0; left: 0; right: 0; height: 3px;
}
.cap-grad.accent { background: var(--gradient-primary, linear-gradient(90deg, #6366f1, #8b5cf6)); }
.cap-grad.success { background: var(--gradient-success, linear-gradient(90deg, #10b981, #34d399)); }
.cap-grad.warning { background: var(--gradient-warning, linear-gradient(90deg, #f59e0b, #fbbf24)); }
.cap-grad.danger { background: var(--gradient-danger, linear-gradient(90deg, #ef4444, #f87171)); }
.cap-grad.info { background: linear-gradient(90deg, #06b6d4, #22d3ee); }

.cap-icon {
  width: 42px; height: 42px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 12px; color: var(--accent, #6366f1);
  background: var(--accent-light, rgba(99,102,241,0.12));
}
.cap-body { flex: 1; }
.cap-title { font-size: 15px; font-weight: 600; color: var(--text-primary, #f0f0f5); margin: 0 0 4px; }
.cap-desc { font-size: 12px; color: var(--text-muted, #505060); line-height: 1.5; margin: 0 0 10px; }
.cap-tag {
  display: inline-block; font-size: 10px; padding: 2px 8px; border-radius: 4px;
  background: var(--bg-hover, #1a1a28); color: var(--text-tertiary, #606070);
}

.cap-arrow {
  position: absolute; top: 18px; right: 18px;
  width: 26px; height: 26px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: var(--text-muted, #505060); opacity: 0;
  transform: translateX(-4px); transition: all 0.2s ease;
}
.cap-card:hover .cap-arrow { opacity: 1; transform: translateX(0); }

.cap-loading-overlay {
  position: absolute; inset: 0;
  display: flex; align-items: center; justify-content: center;
  background: rgba(0,0,0,0.25); border-radius: var(--radius-lg, 14px);
}
.cap-spin {
  width: 22px; height: 22px; border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff; border-radius: 50%; animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ===== Chat Area ===== */
.ai-chat-area {
  flex: 1;
  min-height: 0;
  border-top: 1px solid var(--border-color, #252530);
  background: var(--bg-card, #12121a);
}
</style>
