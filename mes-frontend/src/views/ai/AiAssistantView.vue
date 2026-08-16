<template>
  <div class="ai-page">
    <!-- ===== 顶部 Hero 区 ===== -->
    <div class="ai-hero">
      <div class="hero-glow"></div>
      <div class="hero-left">
        <div class="hero-icon">
          <el-icon :size="22"><MagicStick /></el-icon>
        </div>
        <div class="hero-info">
          <div class="hero-title-row">
            <span class="hero-title">AI 生产助理</span>
            <span class="hero-badge">智能 Agent</span>
          </div>
          <span class="hero-sub">基于数字孪生 · 全厂智能诊断 · 自动工单闭环</span>
        </div>
      </div>
      <div class="hero-right">
        <div class="hero-stat" :class="{ offline: !store.aiOnline }">
          <span class="hero-stat-dot" :class="{ online: store.aiOnline }"></span>
          <div class="hero-stat-info">
            <span class="hero-stat-label">{{ store.aiOnline ? '服务在线' : '服务离线' }}</span>
            <span class="hero-stat-value">{{ store.messageCount }} 条消息</span>
          </div>
        </div>
        <button class="hero-new" @click="handleNewChat">
          <el-icon :size="14"><Plus /></el-icon>
          <span>新对话</span>
        </button>
      </div>
    </div>

    <div class="ai-body">
      <!-- ===== 侧栏 ===== -->
      <div class="ai-side" :class="{ collapsed }">
        <div class="side-capabilities">
          <div class="side-label-row">
            <span class="side-label">AI 能力</span>
            <button class="side-collapse" :title="collapsed ? '展开' : '折叠'" @click="collapsed = !collapsed">
              <el-icon :size="12"><DArrowLeft v-if="!collapsed" /><DArrowRight v-else /></el-icon>
            </button>
          </div>
          <div class="scap-grid">
            <div
              v-for="cap in capabilities"
              :key="cap.label"
              class="scap-item"
              :class="{ loading: cap._loading, active: cap._active, collapsed }"
              @click="triggerCapability(cap)"
            >
              <div class="scap-dot" :class="cap.grad">
                <el-icon :size="14"><component :is="cap.icon" /></el-icon>
              </div>
              <div class="scap-info">
                <span class="scap-title">{{ cap.label }}</span>
                <span class="scap-hint">{{ cap.tag }}</span>
              </div>
              <div v-if="cap._loading" class="scap-spin"></div>
              <el-icon v-else class="scap-go" :size="12"><ArrowRight /></el-icon>
            </div>
          </div>
        </div>

        <div class="side-convs">
          <div class="side-label-row">
            <span class="side-label">聊天记录</span>
            <span class="conv-total">{{ filteredConversations.length }}</span>
          </div>
          <div class="conv-search">
            <el-icon :size="12"><Search /></el-icon>
            <input v-model="convSearch" class="conv-search-input" placeholder="搜索对话" />
          </div>
          <div class="conv-list">
            <div
              v-for="conv in filteredConversations"
              :key="conv.id"
              class="conv-item"
              :class="{ current: conv.id === store.currentId }"
              @click="store.selectConversation(conv.id)"
            >
              <span class="conv-msg-icon"><el-icon :size="10"><ChatDotRound /></el-icon></span>
              <span class="conv-name">{{ conv.title || '新对话' }}</span>
              <span class="conv-time">{{ fmt(conv.updated_at) }}</span>
              <button class="conv-del" title="删除对话" @click.stop="handleRemoveConversation(conv.id)">
                <el-icon :size="11"><Close /></el-icon>
              </button>
            </div>
            <div v-if="store.loadingList" class="conv-loading">
              <el-skeleton v-for="i in 3" :key="i" animated>
                <template #template>
                  <el-skeleton-item variant="rect" style="height: 32px; border-radius: 6px" />
                </template>
              </el-skeleton>
            </div>
            <div v-else-if="!filteredConversations.length" class="conv-note">
              {{ convSearch ? '无匹配对话' : '暂无记录，开始你的第一段对话吧' }}
            </div>
          </div>
        </div>
      </div>

      <!-- ===== 主聊天区 ===== -->
      <div class="ai-main">
        <AiAssistant ref="assistantRef" :floating="false" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, markRaw } from 'vue'
import type { Component } from 'vue'
import AiAssistant from '@/components/ai/AiAssistant.vue'
import { MagicStick, Monitor, Warning, Document, Notebook, TrendCharts, Setting, ArrowRight, DArrowLeft, DArrowRight, Close, Search, Plus, ChatDotRound } from '@element-plus/icons-vue'
import { useAiChatStore } from '@/stores/aiChat'

const assistantRef = ref<InstanceType<typeof AiAssistant> | null>(null)
const store = useAiChatStore()
const collapsed = ref(false)
const convSearch = ref('')

const filteredConversations = computed(() => {
  const kw = convSearch.value.trim().toLowerCase()
  if (!kw) return store.conversations
  return store.conversations.filter(c => (c.title || '').toLowerCase().includes(kw))
})

interface Capability {
  icon: Component; label: string; tag: string; prompt: string; grad: string; _loading: boolean; _active: boolean
}

const capabilities = reactive<Capability[]>([
  { icon: markRaw(Monitor), label: '孪生总览', tag: '全厂健康', prompt: '请使用 get_all_device_health 获取全工厂所有设备的数字孪生健康状态总览，标注异常设备和健康评分', grad: 'accent', _loading: false, _active: false },
  { icon: markRaw(Warning), label: '异常诊断', tag: '根因分析', prompt: '请检查当前有哪些设备存在异常告警，使用 get_device_digital_twin 获取完整孪生数据，对异常设备逐一分析根因并给出处理建议', grad: 'danger', _loading: false, _active: false },
  { icon: markRaw(Document), label: '智能工单', tag: '自动闭环', prompt: '请先检查所有设备数字孪生数据，如果发现温度过高或运行异常的设备，自动为该设备创建维修工单并提示查看3D模型', grad: 'success', _loading: false, _active: false },
  { icon: markRaw(Notebook), label: '手册检索', tag: '知识库', prompt: '请搜索知识库中关于 CNC 主轴温度过高的处理流程、维护规范和预防措施，结合设备数字孪生数据给出针对性建议', grad: 'info', _loading: false, _active: false },
  { icon: markRaw(TrendCharts), label: '趋势预测', tag: '预测维护', prompt: '请使用 get_device_trend 分析关键设备的运行趋势，结合数字孪生数据预测潜在故障时间窗口，给出预防性维护建议', grad: 'warning', _loading: false, _active: false },
  { icon: markRaw(Setting), label: '自动规则', tag: '孪生监控', prompt: '请帮我创建数字孪生监控规则：当任意设备温度超过55°C超过5分钟时，自动分析孪生数据并创建紧急维修工单通知设备工程师', grad: 'accent', _loading: false, _active: false },
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

async function handleRemoveConversation(id: string) {
  await store.removeConversation(id)
}

async function handleNewChat() {
  await store.newChat()
}

onMounted(() => { store.loadList() })
</script>

<style scoped>
.ai-page { display: flex; flex-direction: column; height: 100%; }

/* ===== Hero ===== */
.ai-hero {
  position: relative;
  display: flex; align-items: center; justify-content: space-between; gap: 16px;
  padding: 16px 24px;
  background:
    radial-gradient(600px 100px at 15% 0%, rgba(99,102,241,0.16), transparent 70%),
    radial-gradient(500px 90px at 85% 0%, rgba(139,92,246,0.12), transparent 70%),
    var(--bg-card, #12121a);
  border-bottom: 1px solid var(--border-color, #252530);
  flex-shrink: 0;
  overflow: hidden;
}
.hero-glow {
  position: absolute; top: -40px; right: 20%;
  width: 180px; height: 180px; border-radius: 50%;
  background: radial-gradient(circle, rgba(99,102,241,0.12) 0%, transparent 65%);
  pointer-events: none;
}
.hero-left { display: flex; align-items: center; gap: 14px; position: relative; z-index: 1; }
.hero-icon {
  width: 46px; height: 46px; border-radius: 13px;
  background: var(--gradient-primary, linear-gradient(135deg, #6366f1, #8b5cf6));
  color: #fff; display: flex; align-items: center; justify-content: center; flex-shrink: 0;
  box-shadow: 0 6px 18px rgba(99,102,241,0.4);
}
.hero-info { display: flex; flex-direction: column; gap: 4px; }
.hero-title-row { display: flex; align-items: center; gap: 8px; }
.hero-title { font-size: 17px; font-weight: 700; color: var(--text-primary, #f0f0f5); letter-spacing: 0.3px; }
.hero-badge {
  font-size: 10px; padding: 2px 8px; border-radius: 10px; font-weight: 600;
  background: var(--accent-light, rgba(99,102,241,0.18)); color: var(--accent, #6366f1);
  border: 1px solid rgba(99,102,241,0.3);
}
.hero-sub { font-size: 12px; color: var(--text-muted, #505060); }
.hero-right { display: flex; align-items: center; gap: 12px; position: relative; z-index: 1; }
.hero-stat {
  display: flex; align-items: center; gap: 9px;
  padding: 8px 14px; border-radius: 10px;
  background: var(--bg-hover, #1a1a28); border: 1px solid var(--border-color, #252530);
}
.hero-stat-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.hero-stat-dot.online {
  background: var(--success, #10b981);
  box-shadow: 0 0 8px rgba(16,185,129,0.6);
  animation: dotPulse 2s ease-in-out infinite;
}
.hero-stat.offline .hero-stat-dot { background: var(--danger, #ef4444); }
@keyframes dotPulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.35; } }
.hero-stat-info { display: flex; flex-direction: column; gap: 1px; }
.hero-stat-label { font-size: 10px; color: var(--text-muted, #505060); }
.hero-stat-value { font-size: 11px; font-weight: 600; color: var(--text-primary, #f0f0f5); }
.hero-new {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 9px 18px; border: none; border-radius: 10px; cursor: pointer;
  background: var(--gradient-primary, linear-gradient(135deg, #6366f1, #8b5cf6));
  color: #fff; font-size: 12px; font-weight: 600; font-family: inherit;
  transition: all 0.2s ease; box-shadow: 0 4px 14px rgba(99,102,241,0.35);
}
.hero-new:hover { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(99,102,241,0.5); }

/* ===== Body ===== */
.ai-body { display: flex; flex: 1; min-height: 0; }

/* ===== Side ===== */
.ai-side {
  width: 236px; flex-shrink: 0;
  background: var(--bg-sidebar, #0d0d12);
  border-right: 1px solid var(--border-color, #252530);
  display: flex; flex-direction: column; overflow: hidden;
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

/* capabilities */
.side-capabilities { padding: 14px 12px 10px; border-bottom: 1px solid var(--border-color, #252530); flex-shrink: 0; }
.side-label-row { display: flex; align-items: center; justify-content: space-between; padding: 0 4px 8px; }
.side-label {
  font-size: 10px; font-weight: 600; color: var(--text-muted, #505060);
  text-transform: uppercase; letter-spacing: 0.6px;
}
.side-collapse {
  width: 22px; height: 22px; border-radius: 5px; border: none;
  display: flex; align-items: center; justify-content: center; cursor: pointer;
  background: transparent; color: var(--text-muted, #505060);
  transition: all 0.15s ease;
}
.side-collapse:hover { background: var(--bg-hover, #1a1a28); color: var(--text-primary, #f0f0f5); }
.scap-grid { display: flex; flex-direction: column; gap: 3px; }
.scap-item {
  display: flex; align-items: center; gap: 11px; padding: 8px 10px;
  border-radius: 8px; cursor: pointer; transition: all 0.15s ease; position: relative;
}
.scap-item:hover { background: var(--bg-hover, #1a1a28); transform: translateX(2px); }
.scap-item.active {
  background: var(--accent-light, rgba(99,102,241,0.08));
  border: 1px solid rgba(99,102,241,0.25);
  box-shadow: 0 2px 8px rgba(99,102,241,0.08);
}
.scap-item.active .scap-title { color: var(--accent, #6366f1); font-weight: 600; }
.scap-item.loading { pointer-events: none; opacity: 0.6; }
.scap-dot {
  width: 30px; height: 30px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.scap-dot.accent  { background: var(--accent-light, rgba(99,102,241,0.12)); color: var(--accent, #6366f1); }
.scap-dot.success { background: var(--success-light, rgba(16,185,129,0.12)); color: var(--success, #10b981); }
.scap-dot.warning { background: var(--warning-light, rgba(245,158,11,0.12)); color: var(--warning, #f59e0b); }
.scap-dot.danger  { background: var(--danger-light, rgba(239,68,68,0.12)); color: var(--danger, #ef4444); }
.scap-dot.info    { background: var(--info-light, rgba(6,182,212,0.12)); color: var(--accent-secondary, #22d3ee); }
.scap-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.scap-title { font-size: 12px; font-weight: 500; color: var(--text-primary, #f0f0f5); }
.scap-hint { font-size: 10px; color: var(--text-muted, #505060); }
.scap-go { color: var(--text-muted, #505060); opacity: 0; transform: translateX(-4px); transition: all 0.2s; flex-shrink: 0; }
.scap-item:hover .scap-go { opacity: 1; transform: translateX(0); }
.scap-spin {
  width: 14px; height: 14px; border: 2px solid var(--border-color, #252530);
  border-top-color: var(--accent, #6366f1); border-radius: 50%; animation: spin 0.6s linear infinite; flex-shrink: 0;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* conversations */
.side-convs { flex: 1; overflow-y: auto; padding: 12px; min-height: 0; }
.conv-total {
  font-size: 10px; padding: 1px 7px; border-radius: 9px;
  background: var(--accent-light, rgba(99,102,241,0.15)); color: var(--accent, #6366f1); font-weight: 600;
}
.conv-search {
  display: flex; align-items: center; gap: 6px;
  margin: 0 0 8px; padding: 6px 9px; border-radius: 7px;
  background: var(--bg-input, #1a1a24); border: 1px solid var(--border-color, #252530);
  color: var(--text-muted, #505060);
}
.conv-search:focus-within { border-color: var(--accent, #6366f1); }
.conv-search-input {
  flex: 1; background: transparent; border: none; outline: none;
  color: var(--text-primary, #f0f0f5); font-size: 11px; font-family: inherit;
}
.conv-search-input::placeholder { color: var(--text-muted, #505060); }
.conv-list { display: flex; flex-direction: column; gap: 2px; }
.conv-loading { display: flex; flex-direction: column; gap: 8px; padding: 4px 8px; }
.conv-item {
  display: flex; align-items: center; padding: 8px 9px; border-radius: 7px;
  cursor: pointer; transition: all 0.12s ease; gap: 7px; position: relative;
  animation: convIn 0.3s ease both;
}
@keyframes convIn { from { opacity: 0; transform: translateY(6px); } }
.conv-item:hover { background: var(--bg-hover, #1a1a28); }
.conv-item.current {
  background: var(--accent-light, rgba(99,102,241,0.1));
  border-left: 2px solid var(--accent, #6366f1);
  border-radius: 4px 7px 7px 4px;
  padding-left: 8px;
}
.conv-item.current .conv-name { color: var(--accent, #6366f1); font-weight: 500; }
.conv-msg-icon {
  width: 18px; height: 18px; border-radius: 5px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  background: var(--bg-hover, #1a1a28); color: var(--text-muted, #505060);
}
.conv-item.current .conv-msg-icon { background: rgba(99,102,241,0.15); color: var(--accent, #6366f1); }
.conv-name { font-size: 12px; color: var(--text-primary, #f0f0f5); flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.conv-time { font-size: 10px; color: var(--text-muted, #505060); flex-shrink: 0; }
.conv-del {
  width: 18px; height: 18px; border-radius: 4px; border: none; cursor: pointer;
  display: none; align-items: center; justify-content: center;
  background: transparent; color: var(--text-muted, #505060); flex-shrink: 0; padding: 0;
}
.conv-del:hover { background: rgba(239, 68, 68, 0.15); color: var(--danger, #ef4444); }
.conv-item:hover .conv-del { display: inline-flex; }
.conv-item.current .conv-del { display: inline-flex; }
.conv-note { font-size: 11px; color: var(--text-muted, #505060); padding: 18px 8px; text-align: center; line-height: 1.7; }

/* ===== Collapse ===== */
.ai-side.collapsed { width: 56px; }
.ai-side.collapsed .side-label-row .side-label,
.ai-side.collapsed .scap-info,
.ai-side.collapsed .scap-go,
.ai-side.collapsed .conv-total,
.ai-side.collapsed .conv-search,
.ai-side.collapsed .side-convs { display: none; }
.ai-side.collapsed .side-label-row { justify-content: center; padding: 0 0 8px; }
.ai-side.collapsed .scap-item { justify-content: center; padding: 8px; }
.ai-side.collapsed .scap-item:hover { transform: none; }
.ai-side.collapsed .scap-item.active { border-radius: 8px; }
.ai-side.collapsed .side-capabilities { padding: 14px 8px 10px; }

/* ===== Main ===== */
.ai-main { flex: 1; min-width: 0; background: var(--bg-app, #0a0a0f); }
</style>