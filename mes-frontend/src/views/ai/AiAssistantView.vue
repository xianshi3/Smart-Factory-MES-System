<template>
  <div class="ai-page">
    <div class="ai-side">
      <div class="side-brand">
        <div class="brand-glow"></div>
        <div class="brand-icon-box">
          <el-icon :size="18"><MagicStick /></el-icon>
        </div>
        <span class="brand-name">AI 助理</span>
        <span class="brand-dot online"></span>
      </div>

      <div class="side-stats">
        <div class="st-item">
          <span class="st-icon accent"><el-icon :size="12"><Monitor /></el-icon></span>
          <span class="st-val">{{ stats[0].value }}</span>
          <span class="st-lbl">在线设备</span>
        </div>
        <div class="st-item">
          <span class="st-icon warning"><el-icon :size="12"><Warning /></el-icon></span>
          <span class="st-val">{{ stats[1].value }}</span>
          <span class="st-lbl">活跃告警</span>
        </div>
      </div>

      <div class="side-capabilities">
        <div class="side-label">AI 能力</div>
        <div
          v-for="cap in capabilities"
          :key="cap.label"
          class="scap-item"
          :class="{ loading: cap._loading, active: cap._active }"
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
          <el-icon v-else class="scap-go" :size="12"><ArrowRight /></el-icon>
        </div>
      </div>

      <div class="side-convs">
        <div class="side-label">聊天记录</div>
        <div class="conv-list">
          <div
            v-for="conv in store.conversations"
            :key="conv.id"
            class="conv-item"
            :class="{ current: conv.id === store.currentId }"
            @click="store.selectConversation(conv.id)"
          >
            <span class="conv-name">{{ conv.title }}</span>
            <span class="conv-time">{{ fmt(conv.updated_at) }}</span>
          </div>
          <div v-if="store.loadingList" class="conv-note">加载中...</div>
          <div v-else-if="!store.conversations.length" class="conv-note">暂无记录</div>
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
import { MagicStick, Monitor, Warning, Document, Notebook, TrendCharts, Setting, ArrowRight } from '@element-plus/icons-vue'
import { useAiChatStore } from '@/stores/aiChat'
import axios from 'axios'

const assistantRef = ref<InstanceType<typeof AiAssistant> | null>(null)
const store = useAiChatStore()

interface Capability {
  icon: Component; label: string; tag: string; prompt: string; grad: string; _loading: boolean; _active: boolean
}

const stats = reactive([
  { value: '--', label: '在线设备', cls: 'accent' },
  { value: '--', label: '活跃告警', cls: 'warning' },
])

const capabilities = reactive<Capability[]>([
  { icon: Monitor, label: '设备监控', tag: '实时状态', prompt: '请列出所有设备，显示设备编号、名称、运行状态、当前温度和主轴转速', grad: 'accent', _loading: false, _active: false },
  { icon: Warning, label: '异常诊断', tag: '智能分析', prompt: '请检查当前有哪些设备存在异常告警，对异常设备逐一分析可能的根因并给出处理建议', grad: 'danger', _loading: false, _active: false },
  { icon: Document, label: '工单创建', tag: '一键开工', prompt: '请先检查当前设备状态，如果发现温度过高或运行异常的设备，自动为该设备创建一条 HIGH 优先级的维修工单', grad: 'success', _loading: false, _active: false },
  { icon: Notebook, label: '手册检索', tag: '知识库', prompt: '请搜索知识库中关于 CNC 主轴温度过高的处理流程、维护规范和预防措施', grad: 'info', _loading: false, _active: false },
  { icon: TrendCharts, label: '数据分析', tag: '趋势洞察', prompt: '请分析最近的生产数据趋势，包括产能利用率、质检合格率和设备OEE，识别潜在瓶颈并给出优化建议', grad: 'warning', _loading: false, _active: false },
  { icon: Setting, label: '自动规则', tag: '智能监控', prompt: '请帮我创建一条监控规则：当任意设备温度超过55°C超过5分钟时，自动创建一条紧急维修工单并通知设备工程师', grad: 'accent', _loading: false, _active: false },
])

function triggerCapability(cap: Capability) {
  if (cap._loading || store.loading) return
  for (const c of capabilities) c._active = false
  cap._active = true
  cap._loading = true
  store.sendMessage(cap.prompt).finally(() => { cap._loading = false })
}

function fmt(iso: string): string {
  const d = new Date(iso), n = new Date()
  const diff = n.getTime() - d.getTime()
  if (diff < 60_000) return '刚刚'
  if (diff < 3600_000) return Math.floor(diff / 60_000) + 'm'
  if (diff < 86400_000) return Math.floor(diff / 3600_000) + 'h'
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
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

onMounted(() => { loadStats(); store.loadList() })
</script>

<style scoped>
.ai-page { display: flex; height: 100%; }

/* ===== Side ===== */
.ai-side {
  width: 220px; flex-shrink: 0;
  background: var(--bg-sidebar, #0d0d12);
  border-right: 1px solid var(--border-color, #252530);
  display: flex; flex-direction: column; overflow: hidden;
}

.side-brand {
  display: flex; align-items: center; gap: 8px;
  padding: 16px 14px 13px; border-bottom: 1px solid var(--border-color, #252530);
  position: relative;
}
.brand-glow {
  position: absolute; top: -10px; left: 20px;
  width: 40px; height: 40px; border-radius: 50%;
  background: radial-gradient(circle, rgba(99,102,241,0.12) 0%, transparent 70%);
}
.brand-icon-box {
  width: 30px; height: 30px; border-radius: 8px;
  background: var(--gradient-primary, linear-gradient(135deg, #6366f1, #8b5cf6));
  color: #fff; display: flex; align-items: center; justify-content: center; flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(99,102,241,0.3);
}
.brand-name { font-size: 13px; font-weight: 600; color: var(--text-primary, #f0f0f5); }
.brand-dot {
  width: 7px; height: 7px; border-radius: 50%; margin-left: auto;
  box-shadow: 0 0 6px rgba(16,185,129,0.5);
}
.brand-dot.online { background: var(--success, #10b981); }

/* stats */
.side-stats { display: flex; padding: 10px 8px; gap: 2px; border-bottom: 1px solid var(--border-color, #252530); }
.st-item {
  flex: 1; display: flex; flex-direction: column; align-items: center; gap: 3px;
  padding: 6px 2px; border-radius: 6px; transition: background 0.15s;
}
.st-item:hover { background: var(--bg-hover, #1a1a28); }
.st-icon { width: 24px; height: 24px; border-radius: 6px; display: flex; align-items: center; justify-content: center; }
.st-icon.accent  { background: rgba(99,102,241,0.12); color: var(--accent, #6366f1); }
.st-icon.warning { background: rgba(245,158,11,0.12); color: var(--warning, #f59e0b); }
.st-val { font-size: 15px; font-weight: 700; color: var(--text-primary, #f0f0f5); }
.st-lbl { font-size: 10px; color: var(--text-muted, #505060); }

/* capabilities */
.side-capabilities { padding: 8px; border-bottom: 1px solid var(--border-color, #252530); flex-shrink: 0; }
.side-label {
  font-size: 10px; font-weight: 600; color: var(--text-muted, #505060);
  text-transform: uppercase; letter-spacing: 0.5px; padding: 4px 8px 6px;
}
.scap-item {
  display: flex; align-items: center; gap: 10px; padding: 7px 8px;
  border-radius: 7px; cursor: pointer; transition: all 0.15s ease; position: relative;
}
.scap-item:hover { background: var(--bg-hover, #1a1a28); }
.scap-item.active { background: var(--accent-light, rgba(99,102,241,0.12)); }
.scap-item.active .scap-title { color: var(--accent, #6366f1); font-weight: 600; }
.scap-item.loading { pointer-events: none; }
.scap-dot {
  width: 28px; height: 28px; border-radius: 7px;
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
.scap-go { color: var(--text-muted, #505060); opacity: 0; transform: translateX(-4px); transition: all 0.2s; }
.scap-item:hover .scap-go { opacity: 1; transform: translateX(0); }
.scap-spin {
  width: 14px; height: 14px; border: 2px solid var(--border-color, #252530);
  border-top-color: var(--accent, #6366f1); border-radius: 50%; animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* conversations */
.side-convs { flex: 1; overflow-y: auto; padding: 8px; }
.conv-list { display: flex; flex-direction: column; gap: 1px; }
.conv-item {
  display: flex; align-items: center; padding: 7px 8px; border-radius: 6px;
  cursor: pointer; transition: all 0.12s ease; gap: 6px;
}
.conv-item:hover { background: var(--bg-hover, #1a1a28); }
.conv-item.current { background: var(--accent-light, rgba(99,102,241,0.1)); }
.conv-item.current .conv-name { color: var(--accent, #6366f1); font-weight: 500; }
.conv-name { font-size: 12px; color: var(--text-primary, #f0f0f5); flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.conv-time { font-size: 10px; color: var(--text-muted, #505060); flex-shrink: 0; }
.conv-note { font-size: 11px; color: var(--text-muted, #505060); padding: 12px 8px; text-align: center; }

/* ===== Main ===== */
.ai-main { flex: 1; min-width: 0; }
</style>
