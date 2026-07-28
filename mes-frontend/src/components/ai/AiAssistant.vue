<template>
  <div v-if="visible !== false" class="ai-panel" :class="{ floating }">
    <div class="panel-header">
      <div class="header-brand">
        <div class="brand-icon-box">
          <el-icon :size="20"><MagicStick /></el-icon>
        </div>
        <div class="brand-meta">
          <span class="brand-text">AI 生产助理</span>
          <span class="brand-tag">Agent</span>
        </div>
        <span v-if="!floating" class="brand-model">基于大语言模型</span>
      </div>
      <div class="header-actions">
        <button class="header-btn" @click="clearChat" title="清空对话">
          <el-icon :size="15"><Delete /></el-icon>
        </button>
        <button v-if="floating" class="header-btn close-btn" @click="emit('close')" title="关闭">
          <el-icon :size="15"><Close /></el-icon>
        </button>
      </div>
    </div>

    <div class="panel-body" ref="bodyRef">
      <template v-for="(msg, i) in messages" :key="i">
        <div class="msg" :class="msg.role">
          <div class="msg-avatar">
            <el-icon v-if="msg.role === 'assistant'" :size="16"><MagicStick /></el-icon>
            <el-icon v-else :size="16"><User /></el-icon>
          </div>
          <div class="msg-bubble">
            <div class="msg-text" v-html="renderMarkdown(msg.content)" />
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

      <div v-if="loading" class="msg assistant">
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
          :disabled="loading"
          placeholder="输入生产指令，例如：查看 DEV-001 温度状态"
          @keydown.enter.exact.prevent="sendMessage"
        />
        <button
          class="send-btn"
          :disabled="!input.trim() || loading"
          @click="sendMessage"
        >
          <svg v-if="!loading" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
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
          {{ tip.icon }} {{ tip.text }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, watch, onMounted, onUnmounted } from 'vue'
import { runAgent, type AgentStep } from '@/api/agent'
import { marked } from 'marked'
import { MagicStick, Delete, Close, ArrowRight, CircleCheck, CircleClose, User } from '@element-plus/icons-vue'

marked.setOptions({ breaks: true, gfm: true })

const props = withDefaults(defineProps<{ visible?: boolean; floating?: boolean }>(), {
  visible: true,
  floating: true,
})
const emit = defineEmits<{ close: [] }>()

interface StepEx extends AgentStep {
  expanded?: boolean
}

interface Message {
  role: 'user' | 'assistant'
  content: string
  steps?: StepEx[]
  timestamp: Date
}

interface Suggestion {
  icon: string
  text: string
}

const inputRef = ref<HTMLInputElement | null>(null)
const bodyRef = ref<HTMLElement | null>(null)
const input = ref('')
const loading = ref(false)

const messages = ref<Message[]>([
  {
    role: 'assistant',
    content: '你好！我是 **AI 生产助理**，可以帮你：\n\n- 📊 查询设备实时状态\n- 🔍 诊断设备异常\n- 📝 创建维修工单\n- 📖 搜索设备手册\n\n试试下方快捷指令 👇',
    timestamp: new Date(),
  },
])

const suggestions: Suggestion[] = [
  { icon: '📊', text: '查看所有设备状态' },
  { icon: '🔍', text: '查看 DEV-001 温度' },
  { icon: '📖', text: '主轴温度过高怎么处理' },
  { icon: '📝', text: '温度超过55°C就创建工单' },
]

const mdCache = new Map<string, string>()

function scrollToBottom(smooth = true) {
  nextTick(() => {
    const el = bodyRef.value
    if (!el) return
    el.scrollTo({ top: el.scrollHeight, behavior: smooth ? 'smooth' : 'instant' })
  })
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && props.visible && props.floating) {
    emit('close')
  }
}

function focusInput(text?: string) {
  if (text !== undefined) input.value = text
  nextTick(() => inputRef.value?.focus())
}

watch(() => props.visible, (v) => {
  if (v) {
    scrollToBottom(false)
    nextTick(() => inputRef.value?.focus())
  }
})

onMounted(() => document.addEventListener('keydown', onKeydown))
onUnmounted(() => document.removeEventListener('keydown', onKeydown))

async function sendMessage() {
  const text = input.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text, timestamp: new Date() })
  input.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const history = messages.value
      .filter(m => m !== messages.value[messages.value.length - 1])
      .map(m => ({ role: m.role, content: m.content }))

    const res = await runAgent(text, history.slice(-10))

    messages.value.push({
      role: 'assistant',
      content: res.success ? (res.content || '已完成') : ('执行失败：' + (res.content || '未知错误')),
      steps: (res.steps || []).map(s => ({ ...s, expanded: false })),
      timestamp: new Date(),
    })
  } catch (e: any) {
    messages.value.push({
      role: 'assistant',
      content: '连接 AI 服务失败：' + (e?.message || '网络异常') + '。请确认 AI 服务已启动。',
      timestamp: new Date(),
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function clearChat() {
  messages.value = [{
    role: 'assistant',
    content: '对话已清空，有什么可以帮你的？',
    timestamp: new Date(),
  }]
  mdCache.clear()
}

function renderMarkdown(text: string): string {
  if (!text) return ''
  const cached = mdCache.get(text)
  if (cached) return cached
  const html = marked.parse(text) as string
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
  sendMessage()
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
.ai-panel:not(.floating) .panel-header {
  padding: 18px 24px;
}
.header-brand { display: flex; align-items: center; gap: 10px; }
.brand-icon-box {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--gradient-primary, linear-gradient(135deg, #6366f1, #8b5cf6));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.brand-meta { display: flex; align-items: center; gap: 6px; }
.brand-text {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary, #f0f0f5);
}
.brand-tag {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  background: var(--accent-light, rgba(99,102,241,0.15));
  color: var(--accent, #6366f1);
  font-weight: 600;
}
.brand-model {
  font-size: 11px;
  color: var(--text-muted, #505060);
  margin-left: 4px;
}
.header-actions { display: flex; align-items: center; gap: 4px; }
.header-btn {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm, 6px);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: transparent;
  color: var(--text-muted, #505060);
  transition: all 0.15s ease;
}
.header-btn:hover {
  background: var(--bg-hover, #1a1a28);
  color: var(--text-primary, #f0f0f5);
}
.close-btn:hover { background: rgba(239,68,68,0.12); color: var(--danger, #ef4444); }

/* ===== Body ===== */
.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  background: var(--bg-app, #0a0a0f);
}
.ai-panel:not(.floating) .panel-body {
  padding: 24px 32px;
  gap: 24px;
}

/* ===== Messages ===== */
.msg {
  display: flex;
  gap: 12px;
  max-width: 88%;
  animation: fadeSlideUp 0.3s ease;
}
.ai-panel:not(.floating) .msg { max-width: 75%; }
.msg.user { align-self: flex-end; flex-direction: row-reverse; }

.msg-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
}
.assistant .msg-avatar {
  background: var(--accent-light, rgba(99,102,241,0.15));
  color: var(--accent, #6366f1);
}
.user .msg-avatar {
  background: var(--success-light, rgba(16,185,129,0.15));
  color: var(--success, #10b981);
}
.msg-avatar.pulsing {
  animation: avatarPulse 2s ease-in-out infinite;
}
@keyframes avatarPulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(99,102,241,0.3); }
  50% { box-shadow: 0 0 0 6px rgba(99,102,241,0); }
}

.msg-bubble {
  padding: 12px 16px;
  border-radius: var(--radius-md, 10px);
  font-size: 13px;
  line-height: 1.7;
  min-width: 0;
  word-break: break-word;
}
.ai-panel:not(.floating) .msg-bubble { font-size: 14px; padding: 14px 20px; }
.assistant .msg-bubble {
  background: var(--bg-hover, #1a1a28);
  border: 1px solid var(--border-light, #1f1f28);
  color: var(--text-primary, #f0f0f5);
}
.user .msg-bubble {
  background: linear-gradient(135deg, rgba(99,102,241,0.25), rgba(139,92,246,0.18));
  border: 1px solid rgba(99,102,241,0.12);
  color: var(--text-primary, #f0f0f5);
}

/* Markdown */
.msg-text :deep(p) { margin: 4px 0; }
.msg-text :deep(p:first-child) { margin-top: 0; }
.msg-text :deep(p:last-child) { margin-bottom: 0; }
.msg-text :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 8px 0;
  font-size: 12px;
}
.msg-text :deep(th) {
  background: var(--bg-app, #0a0a0f);
  color: var(--text-secondary, #a0a0b0);
  padding: 6px 8px;
  text-align: left;
  font-weight: 600;
  border-bottom: 1px solid var(--border-color, #252530);
}
.msg-text :deep(td) {
  padding: 6px 8px;
  border-bottom: 1px solid var(--border-light, #1f1f28);
  color: var(--text-primary, #f0f0f5);
}
.msg-text :deep(tr:last-child td) { border-bottom: none; }
.msg-text :deep(strong) { color: var(--accent, #6366f1); font-weight: 600; }
.msg-text :deep(code) {
  background: var(--bg-app, #0a0a0f);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  color: var(--accent-secondary, #22d3ee);
}
.msg-text :deep(ul), .msg-text :deep(ol) { padding-left: 20px; margin: 6px 0; }
.msg-text :deep(li) { margin: 3px 0; }
.msg-text :deep(hr) {
  border: none;
  border-top: 1px solid var(--border-color, #252530);
  margin: 12px 0;
}
.msg-text :deep(h1), .msg-text :deep(h2), .msg-text :deep(h3) {
  font-size: 15px;
  color: var(--text-primary, #f0f0f5);
  margin: 12px 0 6px;
  font-weight: 600;
}

/* Steps */
.msg-steps { margin-top: 10px; }
.steps-label {
  font-size: 11px;
  color: var(--text-muted, #505060);
  margin-bottom: 6px;
  font-weight: 500;
}
.step-item {
  border-radius: var(--radius-sm, 6px);
  background: var(--bg-app, #0a0a0f);
  margin-bottom: 3px;
  cursor: pointer;
  transition: all 0.15s ease;
  border: 1px solid transparent;
}
.step-item:hover { background: var(--bg-hover, #1a1a28); border-color: var(--border-light, #1f1f28); }
.step-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  font-size: 12px;
  user-select: none;
}
.step-chevron {
  transition: transform 0.2s ease;
  color: var(--text-muted, #505060);
  flex-shrink: 0;
}
.step-chevron.rotated { transform: rotate(90deg); }
.step-num {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--border-color, #252530);
  color: var(--text-secondary, #a0a0b0);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  flex-shrink: 0;
}
.step-tool {
  color: var(--accent-secondary, #22d3ee);
  font-family: monospace;
  font-size: 11px;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.step-status { display: flex; align-items: center; flex-shrink: 0; }
.step-status.ok { color: var(--success, #10b981); }
.step-status.fail { color: var(--danger, #ef4444); }

.step-detail { padding: 0 10px 10px; }
.detail-row { margin-top: 6px; }
.detail-label {
  font-size: 11px;
  color: var(--text-muted, #505060);
  margin-bottom: 3px;
  font-weight: 500;
}
.detail-row pre {
  background: var(--bg-card, #12121a);
  padding: 8px 10px;
  border-radius: 4px;
  font-size: 11px;
  overflow-x: auto;
  max-height: 140px;
  color: var(--text-secondary, #a0a0b0);
  margin: 0;
  line-height: 1.5;
}

.msg-time {
  font-size: 11px;
  color: var(--text-muted, #505060);
  margin-top: 8px;
}

/* ===== Footer ===== */
.panel-footer {
  padding: 12px 16px 14px;
  border-top: 1px solid var(--border-color, #252530);
  flex-shrink: 0;
  background: var(--bg-card, #12121a);
}
.ai-panel:not(.floating) .panel-footer {
  padding: 16px 24px 20px;
}

.quick-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}
.quick-btn {
  font-size: 11px;
  padding: 4px 12px;
  border-radius: 14px;
  border: 1px solid var(--border-color, #252530);
  background: transparent;
  color: var(--text-secondary, #a0a0b0);
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
  font-family: inherit;
}
.quick-btn:hover {
  border-color: var(--accent, #6366f1);
  color: var(--accent, #6366f1);
  background: rgba(99,102,241,0.08);
}

.input-row {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}
.msg-input {
  flex: 1;
  padding: 10px 16px;
  border-radius: var(--radius-md, 10px);
  border: 1px solid var(--border-color, #252530);
  background: var(--bg-input, #1a1a24);
  color: var(--text-primary, #f0f0f5);
  font-size: 13px;
  font-family: inherit;
  outline: none;
  transition: all 0.2s ease;
}
.ai-panel:not(.floating) .msg-input { padding: 12px 18px; font-size: 14px; }
.msg-input:focus {
  border-color: var(--accent, #6366f1);
  box-shadow: 0 0 0 3px rgba(99,102,241,0.12);
}
.msg-input::placeholder { color: var(--text-muted, #505060); }
.msg-input:disabled { opacity: 0.5; cursor: not-allowed; }

.send-btn {
  width: 42px;
  height: 42px;
  border-radius: var(--radius-md, 10px);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: var(--gradient-primary, linear-gradient(135deg, #6366f1, #8b5cf6));
  color: #fff;
  transition: all 0.2s ease;
  flex-shrink: 0;
}
.ai-panel:not(.floating) .send-btn { width: 48px; height: 48px; border-radius: 12px; }
.send-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.45);
}
.send-btn:disabled { opacity: 0.35; cursor: not-allowed; }

/* ===== Thinking ===== */
.thinking {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 18px;
  background: var(--bg-hover, #1a1a28);
  border: 1px solid var(--border-light, #1f1f28);
  border-radius: var(--radius-md, 10px);
  min-width: 180px;
}
.thinking-bar {
  width: 100%;
  height: 3px;
  background: var(--border-color, #252530);
  border-radius: 2px;
  overflow: hidden;
}
.thinking-bar-inner {
  display: block;
  width: 40%;
  height: 100%;
  background: var(--gradient-primary, linear-gradient(90deg, #6366f1, #8b5cf6));
  border-radius: 2px;
  animation: progressBar 1.6s ease-in-out infinite;
}
@keyframes progressBar {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(350%); }
}
.thinking-text {
  font-size: 12px;
  color: var(--text-muted, #505060);
}

/* ===== Send Spinner ===== */
.send-spinner {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.spinner-ring {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  display: block;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ===== Transitions ===== */
.step-expand-enter-active, .step-expand-leave-active {
  transition: all 0.2s ease;
  overflow: hidden;
}
.step-expand-enter-from, .step-expand-leave-to {
  opacity: 0;
  max-height: 0;
}
.step-expand-enter-to, .step-expand-leave-from {
  opacity: 1;
  max-height: 400px;
}

@keyframes fadeSlideUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
