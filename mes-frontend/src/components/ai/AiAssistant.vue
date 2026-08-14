<template>
  <div v-if="visible !== false" class="ai-panel" :class="{ floating }">
    <div v-if="floating" class="panel-header">
      <div class="header-brand">
        <div class="brand-icon-box">
          <el-icon :size="20"><MagicStick /></el-icon>
        </div>
        <div class="brand-meta">
          <span class="brand-text">AI 生产助理</span>
          <span class="brand-tag">Agent</span>
        </div>
        <span v-if="store.currentTitle" class="brand-title-badge">{{ store.currentTitle }}</span>
      </div>
      <div class="header-actions">
        <button class="header-btn" title="新建对话" @click="handleNewChat">
          <el-icon :size="14"><Plus /></el-icon>
        </button>
        <button class="header-btn" title="清空" @click="handleClear">
          <el-icon :size="15"><Delete /></el-icon>
        </button>
        <button class="header-btn close-btn" title="关闭" @click="emit('close')">
          <el-icon :size="15"><Close /></el-icon>
        </button>
      </div>
    </div>
    <div v-else class="panel-topbar">
      <span class="topbar-title">{{ store.currentTitle || '新对话' }}</span>
      <span v-if="store.messageCount" class="topbar-count">{{ store.messageCount }} 条消息</span>
      <div class="header-actions">
        <button class="header-btn" title="清空" @click="handleClear"><el-icon :size="14"><Delete /></el-icon></button>
        <button class="header-btn new-chat-btn" title="新建对话" @click="handleNewChat">
          <el-icon :size="14"><Plus /></el-icon>
          <span>新对话</span>
        </button>
      </div>
    </div>

    <Transition name="banner">
      <div v-if="!store.aiOnline" class="offline-banner">
        <el-icon :size="16"><WarningFilled /></el-icon>
        <span>AI 服务未连接 — 请在终端启动 mes-ai-service</span>
        <button class="banner-retry" @click="store.checkHealth()">
          <el-icon :size="14"><Refresh /></el-icon> 重试
        </button>
      </div>
    </Transition>

    <div ref="bodyRef" class="panel-body">
      <template v-for="(msg, i) in store.messages" :key="i">
        <div v-if="i === 0 && msg.role === 'assistant' && !store.currentId" class="msg assistant welcome-msg">
          <div class="msg-avatar">
            <el-icon :size="16"><MagicStick /></el-icon>
          </div>
          <div class="welcome-card">
            <p class="welcome-greet" v-html="renderMarkdown('你好！我是 **AI 生产助理**，可以帮你：')" />
            <div class="welcome-features">
              <div class="wf-item">
                <span class="wf-dot accent"><el-icon :size="12"><Monitor /></el-icon></span>
                <span>查询设备实时状态</span>
              </div>
              <div class="wf-item">
                <span class="wf-dot cyan"><el-icon :size="12"><Warning /></el-icon></span>
                <span>诊断设备异常</span>
              </div>
              <div class="wf-item">
                <span class="wf-dot green"><el-icon :size="12"><Document /></el-icon></span>
                <span>创建维修工单</span>
              </div>
              <div class="wf-item">
                <span class="wf-dot amber"><el-icon :size="12"><Notebook /></el-icon></span>
                <span>搜索设备手册</span>
              </div>
            </div>
            <p class="welcome-hint">试试下方快捷指令</p>
          </div>
        </div>
        <div v-else class="msg" :class="msg.role">
          <div class="msg-avatar" :class="{ compact: i > 0 && store.messages[i - 1]?.role === msg.role }">
            <el-icon v-if="msg.role === 'assistant'" :size="16"><MagicStick /></el-icon>
            <el-icon v-else :size="16"><User /></el-icon>
          </div>
          <div class="msg-bubble">
            <div v-if="msg.plan && msg.plan.length" class="msg-plan">
              <div class="plan-label">
                <el-icon :size="12"><List /></el-icon>
                <span>执行计划 <em v-if="msg.intentLabel">· {{ msg.intentLabel }}</em></span>
              </div>
              <div v-for="p in msg.plan" :key="p.step" class="plan-item">
                <span class="plan-num">{{ p.step }}</span>
                <span class="plan-tool">{{ p.tool }}</span>
                <span class="plan-purpose">{{ p.purpose }}</span>
              </div>
            </div>
            <div class="msg-text" v-html="renderMarkdown(msg.content)" />
            <div v-if="msg.report" class="msg-report">
              <div v-if="msg.report.summary" class="report-summary">{{ msg.report.summary }}</div>
              <div v-if="msg.report.key_points?.length" class="report-section">
                <div class="report-label">关键结论</div>
                <ul class="report-points">
                  <li v-for="(kp, ki) in msg.report.key_points" :key="ki">{{ kp }}</li>
                </ul>
              </div>
              <div v-for="(tbl, ti) in (msg.report.tables || [])" :key="ti" class="report-section">
                <div class="report-label">{{ tbl.title }}</div>
                <table class="report-table">
                  <thead>
                    <tr><th v-for="(c, ci) in tbl.columns" :key="ci">{{ c }}</th></tr>
                  </thead>
                  <tbody>
                    <tr v-for="(row, ri) in tbl.rows" :key="ri">
                      <td v-for="(cell, ci) in row" :key="ci">{{ cell }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <div v-if="msg.report.recommendations?.length" class="report-section">
                <div class="report-label">处置建议</div>
                <ul class="report-points rec">
                  <li v-for="(r, ri) in msg.report.recommendations" :key="ri">
                    <el-icon :size="12"><CircleCheckFilled /></el-icon>{{ r }}
                  </li>
                </ul>
              </div>
            </div>
            <div v-if="msg.steps && msg.steps.length" class="msg-steps">
              <div class="steps-label">Agent 执行步骤</div>
              <div
                v-for="(step, j) in msg.steps"
                :key="j"
                class="step-item"
                @click="step.expanded = !step.expanded"
              >
                <div class="step-header">
                  <el-icon :size="12" class="step-chevron" :class="{ rotated: step.expanded }">
                    <ArrowRight />
                  </el-icon>
                  <span class="step-num">{{ j + 1 }}</span>
                  <span class="step-tool">{{ step.tool }}</span>
                  <span :class="['step-status', step.result?.success ? 'ok' : 'fail']">
                    <el-icon v-if="step.result?.success" :size="14"><CircleCheck /></el-icon>
                    <el-icon v-else :size="14"><CircleClose /></el-icon>
                  </span>
                </div>
                <Transition name="step-expand">
                  <div v-if="step.expanded" class="step-detail">
                    <div class="detail-row">
                      <span class="detail-label">参数</span>
                      <pre>{{ formatJson(step.args) }}</pre>
                    </div>
                    <div class="detail-row">
                      <span class="detail-label">结果</span>
                      <pre>{{ formatJson(step.result, 800) }}</pre>
                    </div>
                  </div>
                </Transition>
              </div>
            </div>
            <div class="msg-time">{{ formatTime(msg.timestamp) }}</div>
          </div>
        </div>
      </template>

      <div v-if="store.loading" class="msg assistant">
        <div class="msg-avatar pulsing">
          <el-icon :size="16"><MagicStick /></el-icon>
        </div>
        <div class="msg-bubble thinking">
          <div class="thinking-bar">
            <span class="thinking-bar-inner"></span>
          </div>
          <span class="thinking-text">正在分析...</span>
        </div>
      </div>
    </div>

    <div class="panel-footer">
      <div class="input-row">
        <input
          ref="inputRef"
          v-model="input"
          class="msg-input"
          :disabled="store.loading"
          placeholder="输入生产指令，例如：查看 DEV-001 温度状态"
          @keydown.enter.exact.prevent="handleSend"
        />
        <button
          class="send-btn"
          :disabled="!input.trim() || store.loading"
          @click="handleSend"
        >
          <svg v-if="!store.loading" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
            <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" />
          </svg>
          <div v-else class="send-spinner">
            <span class="spinner-ring"></span>
          </div>
        </button>
      </div>
      <div class="quick-bar">
        <button
          v-for="tip in suggestions"
          :key="tip.text"
          class="quick-btn"
          @click="pickSuggestion(tip)"
        >
          <el-icon :size="12"><component :is="tip.icon" /></el-icon>
          <span>{{ tip.text }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, watch, onMounted, onUnmounted, computed } from 'vue'
import { marked } from 'marked'
import type { Component } from 'vue'
import {
  MagicStick, Delete, Close, ArrowRight, CircleCheck, CircleCheckFilled, CircleClose, User,
  DataAnalysis, Search, Notebook, Document as DocIcon, Monitor, Warning, Plus, WarningFilled, Refresh, List,
} from '@element-plus/icons-vue'
import { useAiChatStore } from '@/stores/aiChat'

marked.setOptions({ breaks: true, gfm: true })

const props = withDefaults(defineProps<{
  visible?: boolean
  floating?: boolean
  /** 页面上下文：发送消息时注入给 AI（当前页面/筛选/数据摘要） */
  context?: any
  /** 自定义场景按钮（替换默认快捷指令） */
  scenarios?: { text: string; icon?: Component }[]
  /** 打开时自动开启新对话（页面级助手建议开启，避免跨页面串会话） */
  autoNew?: boolean
}>(), {
  visible: true,
  floating: true,
  context: null,
  scenarios: () => [],
  autoNew: false,
})
const emit = defineEmits<{ close: [] }>()

const store = useAiChatStore()

const inputRef = ref<HTMLInputElement | null>(null)
const bodyRef = ref<HTMLElement | null>(null)
const input = ref('')

interface Suggestion { icon: Component; text: string }

const defaultSuggestions: Suggestion[] = [
  { icon: DataAnalysis, text: '查看所有设备状态' },
  { icon: Search, text: '查看 DEV-001 温度' },
  { icon: Notebook, text: '主轴温度过高怎么处理' },
  { icon: DocIcon, text: '温度超过55°C就创建工单' },
]

/** 页面自定义场景优先，否则用默认快捷指令 */
const suggestions = computed<Suggestion[]>(() =>
  (props.scenarios && props.scenarios.length ? props.scenarios : defaultSuggestions).map(s => ({
    icon: s.icon || MagicStick,
    text: s.text,
  })),
)

const mdCache = new Map<string, string>()

function scrollToBottom(smooth = true) {
  nextTick(() => {
    const el = bodyRef.value
    if (!el) return
    el.scrollTo({ top: el.scrollHeight, behavior: smooth ? 'smooth' : 'instant' })
  })
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && props.visible && props.floating) emit('close')
}

function focusInput(text?: string) {
  if (text !== undefined) input.value = text
  nextTick(() => inputRef.value?.focus())
}

watch(() => props.visible, async (v) => {
  if (v) {
    // 页面级助手：每次打开开启新会话，避免与上一页/上一会话串上下文
    if (props.autoNew && store.currentId) {
      await store.newChat()
    }
    scrollToBottom(false)
    nextTick(() => inputRef.value?.focus())
  }
})

watch(() => store.messages.length, () => scrollToBottom())

onMounted(() => {
  store.loadList()
  store.checkHealth()
  document.addEventListener('keydown', onKeydown)
})
onUnmounted(() => document.removeEventListener('keydown', onKeydown))

async function handleSend() {
  const text = input.value.trim()
  if (!text) return
  input.value = ''
  await store.sendMessage(text, props.context)
}

async function handleNewChat() {
  await store.newChat()
  scrollToBottom(false)
  nextTick(() => inputRef.value?.focus())
}

async function handleClear() {
  await store.clearMessages()
  scrollToBottom(false)
}

function renderMarkdown(text: string): string {
  if (!text) return ''
  const cached = mdCache.get(text)
  if (cached) return cached
  let html = marked.parse(text) as string
  // 将 DEV-XXX 转为可点击的设备卡片，跳转3D数字孪生页面
  html = html.replace(/DEV-[\w-]+/g, (m) =>
    `<a class="device-link" href="/device?device=${m}" title="查看 ${m} 数字孪生模型">🔗 ${m}</a>`
  )
  mdCache.set(text, html)
  return html
}

function formatJson(obj: any, maxLen?: number): string {
  const s = JSON.stringify(obj, null, 2)
  return maxLen ? s.slice(0, maxLen) : s
}

function formatTime(d: Date): string {
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function pickSuggestion(tip: Suggestion) {
  input.value = tip.text
  handleSend()
}

defineExpose({ focusInput })
</script>

<style scoped>
/* ===== Panel ===== */
.ai-panel {
  width: 420px;
  height: 620px;
  background: var(--bg-card, #12121a);
  border: 1px solid var(--border-color, #252530);
  border-radius: var(--radius-lg, 14px);
  box-shadow: var(--shadow-lg, 0 8px 24px rgba(0, 0, 0, 0.5));
  display: flex;
  flex-direction: column;
  overflow: hidden;
  z-index: 9999;
}
.ai-panel.floating {
  position: fixed;
  bottom: 24px;
  right: 24px;
}
.ai-panel:not(.floating) {
  width: 100%;
  height: 100%;
  border-radius: 0;
  border: none;
  box-shadow: none;
}

/* ===== Header ===== */
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid var(--border-color, #252530);
  flex-shrink: 0;
  background: var(--bg-card, #12121a);
}
.ai-panel:not(.floating) .panel-header { padding: 18px 24px; }
.header-brand { display: flex; align-items: center; gap: 10px; }
.brand-icon-box {
  width: 36px; height: 36px;
  border-radius: 10px;
  background: var(--gradient-primary, linear-gradient(135deg, #6366f1, #8b5cf6));
  color: #fff; display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.brand-meta { display: flex; align-items: center; gap: 6px; }
.brand-text { font-size: 15px; font-weight: 600; color: var(--text-primary, #f0f0f5); }
.brand-tag {
  font-size: 10px; padding: 1px 6px; border-radius: 4px;
  background: var(--accent-light, rgba(99,102,241,0.15)); color: var(--accent, #6366f1); font-weight: 600;
}
.brand-model { font-size: 11px; color: var(--text-muted, #505060); margin-left: 4px; }
.brand-title-badge {
  font-size: 11px; color: var(--text-muted, #505060);
  max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.header-actions { display: flex; align-items: center; gap: 4px; }
.header-btn {
  width: 32px; height: 32px; border-radius: var(--radius-sm, 6px); border: none;
  display: flex; align-items: center; justify-content: center; cursor: pointer;
  background: transparent; color: var(--text-muted, #505060); transition: all 0.15s ease;
}
.header-btn:hover { background: var(--bg-hover, #1a1a28); color: var(--text-primary, #f0f0f5); }
.close-btn:hover { background: rgba(239,68,68,0.12); color: var(--danger, #ef4444); }

/* ===== Topbar (page mode) ===== */
.panel-topbar {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 24px; border-bottom: 1px solid var(--border-color, #252530);
  flex-shrink: 0; background: var(--bg-card, #12121a);
}
.topbar-title {
  font-size: 13px; font-weight: 600; color: var(--text-primary, #f0f0f5);
  max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.topbar-count {
  font-size: 11px; color: var(--text-muted, #505060);
}
.new-chat-btn {
  display: inline-flex; align-items: center; gap: 4px;
  width: auto; padding: 0 12px; height: 28px; border-radius: 6px;
  border: 1px solid var(--border-color, #252530); font-size: 11px;
  color: var(--text-secondary, #a0a0b0); font-family: inherit;
}
.new-chat-btn:hover { border-color: var(--accent, #6366f1); color: var(--accent, #6366f1); }

/* ===== Offline Banner ===== */
.offline-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  margin: 0;
  background: linear-gradient(90deg, rgba(239,68,68,0.12), rgba(245,158,11,0.08));
  border-bottom: 1px solid rgba(239,68,68,0.2);
  color: var(--danger, #ef4444);
  font-size: 12px;
  flex-shrink: 0;
  animation: bannerIn 0.3s ease;
}
@keyframes bannerIn { from { opacity: 0; transform: translateY(-4px); } to { opacity: 1; transform: translateY(0); } }
.banner-retry {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border-radius: 4px;
  border: 1px solid rgba(239,68,68,0.3);
  background: transparent;
  color: var(--danger, #ef4444);
  font-size: 11px;
  cursor: pointer;
  transition: all 0.15s ease;
  flex-shrink: 0;
  font-family: inherit;
}
.banner-retry:hover {
  background: rgba(239,68,68,0.15);
  border-color: var(--danger, #ef4444);
}
.banner-enter-active, .banner-leave-active { transition: all 0.25s ease; }
.banner-enter-from, .banner-leave-to { opacity: 0; max-height: 0; padding-top: 0; padding-bottom: 0; }
.banner-enter-to, .banner-leave-from { opacity: 1; max-height: 40px; }

/* ===== Body ===== */
.panel-body {
  flex: 1; overflow-y: auto; padding: 16px;
  display: flex; flex-direction: column; gap: 18px; background: var(--bg-app, #0a0a0f);
}
.ai-panel:not(.floating) .panel-body { padding: 24px 32px; gap: 20px; }
.ai-panel:not(.floating) .msg.compact-avatar { gap: 0; }

/* ===== Messages ===== */
.msg { display: flex; gap: 12px; max-width: 88%; animation: msgIn 0.35s ease; }
.ai-panel:not(.floating) .msg { max-width: 75%; }
.msg.user { align-self: flex-end; flex-direction: row-reverse; }
@keyframes msgIn { from { opacity: 0; transform: translateY(12px); } }
.msg-avatar {
  width: 32px; height: 32px; border-radius: 50%; display: flex;
  align-items: center; justify-content: center; flex-shrink: 0; margin-top: 2px;
}
.assistant .msg-avatar { background: var(--accent-light, rgba(99,102,241,0.15)); color: var(--accent, #6366f1); }
.user .msg-avatar { background: var(--success-light, rgba(16,185,129,0.15)); color: var(--success, #10b981); }
.msg-avatar.pulsing { animation: avatarPulse 2s ease-in-out infinite; }
@keyframes avatarPulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(99,102,241,0.3); }
  50% { box-shadow: 0 0 0 6px rgba(99,102,241,0); }
}
.msg-avatar.compact { visibility: hidden; width: 0; height: 0; margin: 0; }

.msg-bubble {
  padding: 12px 16px; border-radius: var(--radius-md, 10px);
  font-size: 13px; line-height: 1.7; min-width: 0; word-break: break-word;
}
.ai-panel:not(.floating) .msg-bubble { font-size: 14px; padding: 14px 20px; }
.assistant .msg-bubble {
  background: var(--bg-hover, #1a1a28); border: 1px solid var(--border-light, #1f1f28);
  color: var(--text-primary, #f0f0f5);
}
.user .msg-bubble {
  background: linear-gradient(135deg, rgba(99,102,241,0.25), rgba(139,92,246,0.18));
  border: 1px solid rgba(99,102,241,0.12); color: var(--text-primary, #f0f0f5);
}

/* Welcome Card */
.welcome-msg { max-width: 92%; }
.welcome-card {
  background: var(--bg-hover, #1a1a28); border: 1px solid var(--border-light, #1f1f28);
  border-radius: var(--radius-md, 10px); padding: 20px 22px;
}
.welcome-greet { font-size: 14px; color: var(--text-primary, #f0f0f5); margin: 0 0 16px; line-height: 1.5; }
.welcome-greet :deep(strong) { color: var(--accent, #6366f1); }
.welcome-features { display: flex; flex-direction: column; gap: 10px; }
.wf-item { display: flex; align-items: center; gap: 10px; font-size: 13px; color: var(--text-secondary, #a0a0b0); }
.wf-dot {
  width: 28px; height: 28px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.wf-dot.accent { background: var(--accent-light, rgba(99,102,241,0.1)); color: var(--accent, #6366f1); }
.wf-dot.cyan   { background: var(--info-light, rgba(6,182,212,0.1)); color: var(--accent-secondary, #22d3ee); }
.wf-dot.green  { background: var(--success-light, rgba(16,185,129,0.1)); color: var(--success, #10b981); }
.wf-dot.amber  { background: var(--warning-light, rgba(245,158,11,0.1)); color: var(--warning, #f59e0b); }
.welcome-hint { font-size: 12px; color: var(--text-muted, #505060); margin: 16px 0 0; }

/* Markdown */
.msg-text :deep(p) { margin: 4px 0; }
.msg-text :deep(p:first-child) { margin-top: 0; }
.msg-text :deep(p:last-child) { margin-bottom: 0; }
.msg-text :deep(table) { width: 100%; border-collapse: collapse; margin: 8px 0; font-size: 12px; }
.msg-text :deep(th) { background: var(--bg-app, #0a0a0f); color: var(--text-secondary, #a0a0b0); padding: 6px 8px; text-align: left; font-weight: 600; border-bottom: 1px solid var(--border-color, #252530); }
.msg-text :deep(td) { padding: 6px 8px; border-bottom: 1px solid var(--border-light, #1f1f28); color: var(--text-primary, #f0f0f5); }
.msg-text :deep(tr:last-child td) { border-bottom: none; }
.msg-text :deep(strong) { color: var(--accent, #6366f1); font-weight: 600; }
.msg-text :deep(code) { background: var(--bg-app, #0a0a0f); padding: 2px 6px; border-radius: 4px; font-size: 12px; color: var(--accent-secondary, #22d3ee); }
.msg-text :deep(ul), .msg-text :deep(ol) { padding-left: 18px; margin: 6px 0; }
.msg-text :deep(li) { margin: 3px 0; }
.msg-text :deep(li::marker) { color: var(--accent, #6366f1); }
.msg-text :deep(blockquote) {
  border-left: 3px solid var(--accent, #6366f1); margin: 8px 0; padding: 4px 12px;
  color: var(--text-secondary, #a0a0b0); font-style: italic;
}
.msg-text :deep(pre) {
  background: var(--bg-app, #0a0a0f); padding: 10px 14px; border-radius: 6px;
  overflow-x: auto; margin: 8px 0; font-size: 12px; line-height: 1.5;
  border: 1px solid var(--border-light, #1f1f28);
}
.msg-text :deep(pre code) {
  background: transparent; padding: 0; color: var(--text-secondary, #a0a0b0);
}
.msg-text :deep(hr) { border: none; border-top: 1px solid var(--border-color, #252530); margin: 12px 0; }
.msg-text :deep(h1), .msg-text :deep(h2), .msg-text :deep(h3) { font-size: 15px; color: var(--text-primary, #f0f0f5); margin: 12px 0 6px; font-weight: 600; }
.msg-text :deep(a.device-link) {
  display: inline-flex; align-items: center; gap: 2px;
  padding: 2px 8px; border-radius: 5px; font-size: 12px; font-weight: 500;
  background: var(--accent-light, rgba(99,102,241,0.1)); color: var(--accent, #6366f1);
  text-decoration: none; transition: all 0.15s; margin: 0 2px;
}
.msg-text :deep(a.device-link:hover) {
  background: var(--accent, #6366f1); color: #fff;
}

/* Steps */
.msg-plan {
  margin-bottom: 10px; padding: 8px 10px;
  background: var(--bg-app, #0a0a0f); border: 1px dashed var(--border-color, #252530);
  border-radius: var(--radius-sm, 6px);
}
.plan-label {
  display: flex; align-items: center; gap: 5px;
  font-size: 11px; color: var(--text-muted, #505060); font-weight: 600; margin-bottom: 6px;
}
.plan-label em { font-style: normal; color: var(--accent, #6366f1); font-weight: 500; }
.plan-item {
  display: flex; align-items: center; gap: 6px;
  font-size: 11px; color: var(--text-secondary, #a0a0b0); padding: 2px 0;
}
.plan-num {
  width: 15px; height: 15px; border-radius: 50%; flex-shrink: 0;
  background: var(--accent-light, rgba(99,102,241,0.12)); color: var(--accent, #6366f1);
  display: flex; align-items: center; justify-content: center; font-size: 9px; font-weight: 700;
}
.plan-tool { color: var(--accent-secondary, #22d3ee); font-family: monospace; font-size: 10px; flex-shrink: 0; }
.plan-purpose { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.msg-report {
  margin-top: 10px; padding: 10px 12px;
  background: var(--bg-app, #0a0a0f); border: 1px solid var(--border-color, #252530);
  border-radius: var(--radius-sm, 6px);
}
.report-summary {
  font-size: 12px; font-weight: 600; color: var(--text-primary, #f0f0f5);
  margin-bottom: 8px; line-height: 1.6;
}
.report-section { margin-top: 6px; }
.report-label { font-size: 11px; color: var(--text-muted, #505060); font-weight: 600; margin-bottom: 4px; }
.report-points { margin: 0; padding-left: 0; list-style: none; }
.report-points li {
  font-size: 11px; color: var(--text-secondary, #a0a0b0);
  padding: 3px 0 3px 12px; position: relative; line-height: 1.6;
}
.report-points li::before {
  content: ''; position: absolute; left: 0; top: 10px;
  width: 5px; height: 5px; border-radius: 50%;
  background: var(--accent, #6366f1);
}
.report-points.rec li { padding-left: 16px; display: flex; gap: 5px; align-items: flex-start; }
.report-points.rec li::before { display: none; }
.report-points.rec li .el-icon { color: var(--success, #10b981); margin-top: 3px; flex-shrink: 0; }
.report-table { width: 100%; border-collapse: collapse; font-size: 11px; }
.report-table th {
  background: var(--bg-card, #12121a); color: var(--text-secondary, #a0a0b0);
  padding: 4px 6px; text-align: left; font-weight: 600;
  border-bottom: 1px solid var(--border-color, #252530);
}
.report-table td { padding: 4px 6px; border-bottom: 1px solid var(--border-light, #1f1f28); color: var(--text-primary, #f0f0f5); }
.report-table tr:last-child td { border-bottom: none; }

.msg-steps { margin-top: 10px; }
.steps-label { font-size: 11px; color: var(--text-muted, #505060); margin-bottom: 6px; font-weight: 500; }
.step-item { border-radius: var(--radius-sm, 6px); background: var(--bg-app, #0a0a0f); margin-bottom: 3px; cursor: pointer; transition: all 0.15s ease; border: 1px solid transparent; }
.step-item:hover { background: var(--bg-hover, #1a1a28); border-color: var(--border-light, #1f1f28); }
.step-header { display: flex; align-items: center; gap: 8px; padding: 7px 10px; font-size: 12px; user-select: none; }
.step-chevron { transition: transform 0.2s ease; color: var(--text-muted, #505060); flex-shrink: 0; }
.step-chevron.rotated { transform: rotate(90deg); }
.step-num { width: 18px; height: 18px; border-radius: 50%; background: var(--border-color, #252530); color: var(--text-secondary, #a0a0b0); display: flex; align-items: center; justify-content: center; font-size: 10px; font-weight: 700; flex-shrink: 0; }
.step-tool { color: var(--accent-secondary, #22d3ee); font-family: monospace; font-size: 11px; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.step-status { display: flex; align-items: center; flex-shrink: 0; }
.step-status.ok { color: var(--success, #10b981); }
.step-status.fail { color: var(--danger, #ef4444); }
.step-detail { padding: 0 10px 10px; }
.detail-row { margin-top: 6px; }
.detail-label { font-size: 11px; color: var(--text-muted, #505060); margin-bottom: 3px; font-weight: 500; }
.detail-row pre { background: var(--bg-card, #12121a); padding: 8px 10px; border-radius: 4px; font-size: 11px; overflow-x: auto; max-height: 140px; color: var(--text-secondary, #a0a0b0); margin: 0; line-height: 1.5; }
.msg-time { font-size: 11px; color: var(--text-muted, #505060); margin-top: 8px; }

/* ===== Footer ===== */
.panel-footer { padding: 12px 16px 14px; border-top: 1px solid var(--border-color, #252530); flex-shrink: 0; background: var(--bg-card, #12121a); }
.ai-panel:not(.floating) .panel-footer { padding: 16px 24px 20px; }
.quick-bar { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 10px; }
.quick-btn {
  font-size: 11px; padding: 4px 12px; border-radius: 14px;
  border: 1px solid var(--border-color, #252530); background: transparent;
  color: var(--text-secondary, #a0a0b0); cursor: pointer; transition: all 0.15s ease;
  white-space: nowrap; font-family: inherit; display: inline-flex; align-items: center; gap: 5px;
}
.quick-btn:hover { border-color: var(--accent, #6366f1); color: var(--accent, #6366f1); background: rgba(99,102,241,0.08); }
.input-row { display: flex; gap: 10px; align-items: flex-end; }
.msg-input {
  flex: 1; padding: 10px 16px; border-radius: var(--radius-md, 10px);
  border: 1px solid var(--border-color, #252530); background: var(--bg-input, #1a1a24);
  color: var(--text-primary, #f0f0f5); font-size: 13px; font-family: inherit; outline: none; transition: all 0.2s ease;
}
.ai-panel:not(.floating) .msg-input { padding: 12px 18px; font-size: 14px; }
.msg-input:focus { border-color: var(--accent, #6366f1); box-shadow: 0 0 0 3px rgba(99,102,241,0.12); }
.msg-input::placeholder { color: var(--text-muted, #505060); }
.msg-input:disabled { opacity: 0.5; cursor: not-allowed; }
.send-btn {
  width: 42px; height: 42px; border-radius: var(--radius-md, 10px); border: none;
  display: flex; align-items: center; justify-content: center; cursor: pointer;
  background: var(--gradient-primary, linear-gradient(135deg, #6366f1, #8b5cf6)); color: #fff;
  transition: all 0.2s ease; flex-shrink: 0;
}
.ai-panel:not(.floating) .send-btn { width: 48px; height: 48px; border-radius: 12px; }
.send-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 16px rgba(99,102,241,0.45); }
.send-btn:disabled { opacity: 0.35; cursor: not-allowed; }

/* Thinking */
.thinking {
  display: flex; flex-direction: column; gap: 12px;
  padding: 14px 18px; background: var(--bg-hover, #1a1a28);
  border: 1px solid var(--border-light, #1f1f28);
  border-radius: var(--radius-md, 10px); min-width: 160px;
  animation: msgIn 0.3s ease;
}
.thinking-bar { width: 100%; height: 3px; background: var(--border-color, #252530); border-radius: 2px; overflow: hidden; }
.thinking-bar-inner {
  display: block; width: 30%; height: 100%;
  background: var(--gradient-primary, linear-gradient(90deg, #6366f1, #8b5cf6));
  border-radius: 2px; animation: progressBar 1.8s ease-in-out infinite;
}
@keyframes progressBar {
  0% { transform: translateX(-100%); width: 30%; }
  50% { width: 60%; }
  100% { transform: translateX(400%); width: 30%; }
}
.thinking-text { font-size: 12px; color: var(--text-muted, #505060); }
.send-spinner { width: 20px; height: 20px; display: flex; align-items: center; justify-content: center; }
.spinner-ring { width: 16px; height: 16px; border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff; border-radius: 50%; animation: spin 0.7s linear infinite; display: block; }
@keyframes spin { to { transform: rotate(360deg); } }
.step-expand-enter-active, .step-expand-leave-active { transition: all 0.2s ease; overflow: hidden; }
.step-expand-enter-from, .step-expand-leave-to { opacity: 0; max-height: 0; }
.step-expand-enter-to, .step-expand-leave-from { opacity: 1; max-height: 400px; }
</style>
