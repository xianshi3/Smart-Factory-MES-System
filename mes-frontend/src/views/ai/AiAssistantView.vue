<template>
  <div class="ai-page">
    <div class="ai-side" :class="{ collapsed }">
      <div class="side-brand">
        <div class="brand-glow"></div>
        <div class="brand-icon-box" title="折叠侧栏" @click="collapsed = !collapsed">
          <el-icon :size="18"><MagicStick /></el-icon>
        </div>
        <span class="brand-name">AI 助理</span>
        <span class="brand-dot online"></span>
        <button class="collapse-btn" :title="collapsed ? '展开' : '折叠'" @click="collapsed = !collapsed">
          <el-icon :size="12"><DArrowLeft v-if="!collapsed" /><DArrowRight v-else /></el-icon>
        </button>
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
          <div v-if="store.loadingList" class="conv-loading">
            <el-skeleton v-for="i in 3" :key="i" animated>
              <template #template>
                <el-skeleton-item variant="rect" style="height: 32px; border-radius: 6px" />
              </template>
            </el-skeleton>
          </div>
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
import { MagicStick, Monitor, Warning, Document, Notebook, TrendCharts, Setting, ArrowRight, DArrowLeft, DArrowRight } from '@element-plus/icons-vue'
import { useAiChatStore } from '@/stores/aiChat'

const assistantRef = ref<InstanceType<typeof AiAssistant> | null>(null)
const store = useAiChatStore()
const collapsed = ref(false)

interface Capability {
  icon: Component; label: string; tag: string; prompt: string; grad: string; _loading: boolean; _active: boolean
}

const capabilities = reactive<Capability[]>([
  { icon: Monitor, label: '孪生总览', tag: '全厂健康', prompt: '请使用 get_all_device_health 获取全工厂所有设备的数字孪生健康状态总览，标注异常设备和健康评分', grad: 'accent', _loading: false, _active: false },
  { icon: Warning, label: '异常诊断', tag: '根因分析', prompt: '请检查当前有哪些设备存在异常告警，使用 get_device_digital_twin 获取完整孪生数据，对异常设备逐一分析根因并给出处理建议', grad: 'danger', _loading: false, _active: false },
  { icon: Document, label: '智能工单', tag: '自动闭环', prompt: '请先检查所有设备数字孪生数据，如果发现温度过高或运行异常的设备，自动为该设备创建维修工单并提示查看3D模型', grad: 'success', _loading: false, _active: false },
  { icon: Notebook, label: '手册检索', tag: '知识库', prompt: '请搜索知识库中关于 CNC 主轴温度过高的处理流程、维护规范和预防措施，结合设备数字孪生数据给出针对性建议', grad: 'info', _loading: false, _active: false },
  { icon: TrendCharts, label: '趋势预测', tag: '预测维护', prompt: '请使用 get_device_trend 分析关键设备的运行趋势，结合数字孪生数据预测潜在故障时间窗口，给出预防性维护建议', grad: 'warning', _loading: false, _active: false },
  { icon: Setting, label: '自动规则', tag: '孪生监控', prompt: '请帮我创建数字孪生监控规则：当任意设备温度超过55°C超过5分钟时，自动分析孪生数据并创建紧急维修工单通知设备工程师', grad: 'accent', _loading: false, _active: false },
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

onMounted(() => { store.loadList() })
</script>

<style scoped>
.ai-page { display: flex; height: 100%; }

/* ===== Side ===== */
.ai-side {
  width: 220px; flex-shrink: 0; overflow: hidden;
  background: var(--bg-sidebar, #0d0d12);
  border-right: 1px solid var(--border-color, #252530);
  display: flex; flex-direction: column; overflow: hidden;
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
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

/* capabilities */
.side-capabilities { padding: 8px; border-bottom: 1px solid var(--border-color, #252530); flex-shrink: 0; }
.side-label {
  font-size: 10px; font-weight: 600; color: var(--text-muted, #505060);
  text-transform: uppercase; letter-spacing: 0.5px; padding: 4px 8px 6px;
}
.scap-item {
  display: flex; align-items: center; gap: 12px; padding: 8px 10px;
  border-radius: 7px; cursor: pointer; transition: all 0.15s ease; position: relative;
}
.scap-item:hover { background: var(--bg-hover, #1a1a28); }
.scap-item.active {
  background: var(--accent-light, rgba(99,102,241,0.08));
  border-left: 2px solid var(--accent, #6366f1);
  border-radius: 0 7px 7px 0;
  margin-left: -2px; padding-left: 12px;
}
.scap-item.active .scap-title { color: var(--accent, #6366f1); font-weight: 600; }
.scap-item.loading { pointer-events: none; opacity: 0.6; }
.scap-dot {
  width: 28px; height: 28px; border-radius: 7px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
  background: var(--accent-light, rgba(99,102,241,0.1));
  color: var(--accent, #6366f1);
}
.scap-dot.accent  { background: var(--accent-light, rgba(99,102,241,0.1)); color: var(--accent, #6366f1); }
.scap-dot.success { background: var(--success-light, rgba(16,185,129,0.1)); color: var(--success, #10b981); }
.scap-dot.warning { background: var(--warning-light, rgba(245,158,11,0.1)); color: var(--warning, #f59e0b); }
.scap-dot.danger  { background: var(--danger-light, rgba(239,68,68,0.1)); color: var(--danger, #ef4444); }
.scap-dot.info    { background: var(--info-light, rgba(6,182,212,0.1)); color: var(--accent-secondary, #22d3ee); }
.scap-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
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
.conv-loading { display: flex; flex-direction: column; gap: 8px; padding: 4px 8px; }
.scap-item, .conv-item { animation: convIn 0.3s ease both; }
@keyframes convIn { from { opacity: 0; transform: translateY(6px); } }
.conv-item {
  display: flex; align-items: center; padding: 7px 8px; border-radius: 6px;
  cursor: pointer; transition: all 0.12s ease; gap: 6px; position: relative;
}
.conv-item:hover { background: var(--bg-hover, #1a1a28); }
.conv-item.current { background: var(--accent-light, rgba(99,102,241,0.1)); }
.conv-item.current .conv-name { color: var(--accent, #6366f1); font-weight: 500; }
.conv-name { font-size: 12px; color: var(--text-primary, #f0f0f5); flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.conv-time { font-size: 10px; color: var(--text-muted, #505060); flex-shrink: 0; }
.conv-note { font-size: 11px; color: var(--text-muted, #505060); padding: 12px 8px; text-align: center; }

/* ===== Collapse ===== */
.collapse-btn {
  width: 22px; height: 22px; border-radius: 5px; border: none;
  display: flex; align-items: center; justify-content: center; cursor: pointer;
  background: transparent; color: var(--text-muted, #505060);
  transition: all 0.15s ease; flex-shrink: 0;
}
.collapse-btn:hover { background: var(--bg-hover, #1a1a28); color: var(--text-primary, #f0f0f5); }

.ai-side.collapsed { width: 56px; }
.ai-side.collapsed .brand-name,
.ai-side.collapsed .brand-dot,
.ai-side.collapsed .side-label,
.ai-side.collapsed .scap-info,
.ai-side.collapsed .scap-go,
.ai-side.collapsed .st-lbl,
.ai-side.collapsed .side-convs { display: none; }
.ai-side.collapsed .side-brand { justify-content: center; padding: 14px 8px; }
.ai-side.collapsed .scap-item { justify-content: center; padding: 8px; }
.ai-side.collapsed .side-capabilities { padding: 4px; }
.ai-side.collapsed .scap-item.active { border-left: none; border-radius: 7px; margin-left: 0; padding-left: 8px; }

/* ===== Main ===== */
.ai-main { flex: 1; min-width: 0; }
</style>
