<template>
  <Transition name="panel">
    <div v-if="visible !== false" class="ai-panel" :class="{ floating }">
      <div class="panel-header">
        <div class="header-brand">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18" class="brand-icon">
            <path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z" />
          </svg>
          <span class="brand-text">AI 生产助理</span>
          <span class="brand-badge">Agent</span>
        </div>
        <div class="header-actions">
          <button class="header-btn" @click="clearChat" title="清空对话">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="15" height="15">
              <path d="M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2" />
            </svg>
          </button>
          <button class="header-btn close-btn" @click="$emit('close')" title="关闭">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="15" height="15">
              <path d="M18 6L6 18M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>

      <div class="panel-body" ref="bodyRef">
        <div v-for="(msg, i) in messages" :key="i" class="msg" :class="msg.role">
          <div class="msg-avatar">
            <svg v-if="msg.role === 'assistant'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z" />
            </svg>
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" />
              <circle cx="12" cy="7" r="4" />
            </svg>
          </div>
          <div class="msg-bubble">
            <div class="msg-text" v-html="renderMarkdown(msg.content)" />
            <div v-if="msg.steps && msg.steps.length" class="msg-steps">
              <div class="steps-divider"></div>
              <div
                v-for="(step, j) in msg.steps"
                :key="j"
                class="step-item"
                @click="step.expanded = !step.expanded"
              >
                <div class="step-header">
                  <svg :class="step.expanded ? 'rotated' : ''" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="12" height="12">
                    <path d="M9 18l6-6-6-6" />
                  </svg>
                  <span class="step-num">{{ j + 1 }}</span>
                  <code class="step-tool">{{ step.tool }}</code>
                  <span :class="['step-status', step.result?.success ? 'ok' : 'fail']">
                    {{ step.result?.success ? '✓' : '✗' }}
                  </span>
                </div>
                <div v-if="step.expanded" class="step-detail">
                  <div class="detail-label">参数</div>
                  <pre>{{ JSON.stringify(step.args, null, 2) }}</pre>
                  <div class="detail-label">结果</div>
                  <pre class="result-truncate">{{ JSON.stringify(step.result, null, 2).slice(0, 800) }}</pre>
                </div>
              </div>
            </div>
            <div class="msg-time">{{ formatTime(msg.timestamp) }}</div>
          </div>
        </div>

        <div v-if="loading" class="msg assistant">
          <div class="msg-avatar">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z" />
            </svg>
          </div>
          <div class="msg-bubble thinking">
            <span class="dot">.</span><span class="dot">.</span><span class="dot">.</span>
          </div>
        </div>
      </div>

      <div class="panel-footer">
        <div class="suggestions">
          <button
            v-for="tip in suggestions"
            :key="tip.text"
            class="suggestion-btn"
            @click="pickSuggestion(tip)"
          >
            {{ tip.icon }} {{ tip.text }}
          </button>
        </div>
        <div class="input-row">
          <input
            ref="inputRef"
            v-model="input"
            class="msg-input"
            :disabled="loading"
            placeholder="输入生产指令..."
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
            <span v-else class="spinner"></span>
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref, nextTick, watch, onMounted, onUnmounted } from 'vue'
import { runAgent, type AgentStep } from '@/api/agent'
import { marked } from 'marked'

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
  if (e.key === 'Escape' && props.visible) {
    emit('close')
  }
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

function formatTime(d: Date): string {
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function pickSuggestion(tip: Suggestion) {
  input.value = tip.text
  sendMessage()
}
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
  padding: 14px 18px;
  border-bottom: 1px solid var(--border-color, #252530);
  flex-shrink: 0;
}
.header-brand {
  display: flex;
  align-items: center;
  gap: 8px;
}
.brand-icon { color: var(--accent, #6366f1); }
.brand-text {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary, #f0f0f5);
}
.brand-badge {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  background: var(--accent-light, rgba(99,102,241,0.15));
  color: var(--accent, #6366f1);
  font-weight: 600;
}
.header-btn {
  width: 30px;
  height: 30px;
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
.header-actions { display: flex; align-items: center; gap: 4px; }
.header-btn:hover {
  background: var(--bg-hover, #1a1a28);
  color: var(--danger, #ef4444);
}
.close-btn:hover { color: var(--danger, #ef4444); }

/* ===== Body ===== */
.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  background: var(--bg-app, #0a0a0f);
}

/* ===== Messages ===== */
.msg {
  display: flex;
  gap: 10px;
  max-width: 92%;
  animation: fadeSlideUp 0.25s ease;
}
.msg.user { align-self: flex-end; flex-direction: row-reverse; }

.msg-avatar {
  width: 30px;
  height: 30px;
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

.msg-bubble {
  padding: 10px 14px;
  border-radius: var(--radius-md, 10px);
  font-size: 13px;
  line-height: 1.7;
  min-width: 0;
  word-break: break-word;
}
.assistant .msg-bubble {
  background: var(--bg-hover, #1a1a28);
  border: 1px solid var(--border-light, #1f1f28);
  color: var(--text-primary, #f0f0f5);
}
.user .msg-bubble {
  background: linear-gradient(135deg, rgba(99,102,241,0.2), rgba(139,92,246,0.15));
  border: 1px solid var(--accent-light, rgba(99,102,241,0.15));
  color: var(--text-primary, #f0f0f5);
}

/* Markdown content */
.msg-text :deep(p) { margin: 4px 0; }
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
.msg-text :deep(strong) { color: var(--accent, #6366f1); }
.msg-text :deep(code) {
  background: var(--bg-app, #0a0a0f);
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 12px;
  color: var(--accent-secondary, #22d3ee);
}
.msg-text :deep(ul), .msg-text :deep(ol) { padding-left: 18px; margin: 4px 0; }
.msg-text :deep(li) { margin: 2px 0; }
.msg-text :deep(hr) {
  border: none;
  border-top: 1px solid var(--border-color, #252530);
  margin: 10px 0;
}
.msg-text :deep(h1), .msg-text :deep(h2), .msg-text :deep(h3) {
  font-size: 14px;
  color: var(--text-primary, #f0f0f5);
  margin: 10px 0 4px;
}

/* Steps */
.msg-steps { margin-top: 8px; }
.steps-divider {
  height: 1px;
  background: var(--border-color, #252530);
  margin-bottom: 8px;
}
.step-item {
  border-radius: var(--radius-sm, 6px);
  background: var(--bg-app, #0a0a0f);
  margin-bottom: 4px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.step-item:hover { background: var(--bg-hover, #1a1a28); }
.step-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  font-size: 12px;
}
.step-header svg {
  transition: transform 0.2s ease;
  color: var(--text-muted, #505060);
  flex-shrink: 0;
}
.step-header svg.rotated { transform: rotate(90deg); }
.step-num {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--border-color, #252530);
  color: var(--text-secondary, #a0a0b0);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 600;
  flex-shrink: 0;
}
.step-tool {
  color: var(--accent-secondary, #22d3ee);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.step-status { font-weight: 600; font-size: 11px; }
.step-status.ok { color: var(--success, #10b981); }
.step-status.fail { color: var(--danger, #ef4444); }
.step-detail { padding: 0 8px 8px; }
.detail-label {
  font-size: 11px;
  color: var(--text-muted, #505060);
  margin: 4px 0 2px;
}
.step-detail pre {
  background: var(--bg-app, #0a0a0f);
  padding: 6px 8px;
  border-radius: 4px;
  font-size: 11px;
  overflow-x: auto;
  max-height: 120px;
  color: var(--text-secondary, #a0a0b0);
  margin: 0;
  line-height: 1.5;
}

.msg-time {
  font-size: 11px;
  color: var(--text-muted, #505060);
  margin-top: 6px;
}

/* ===== Footer ===== */
.panel-footer {
  padding: 12px 16px 14px;
  border-top: 1px solid var(--border-color, #252530);
  flex-shrink: 0;
  background: var(--bg-card, #12121a);
}

.suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}
.suggestion-btn {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 12px;
  border: 1px solid var(--border-color, #252530);
  background: transparent;
  color: var(--text-secondary, #a0a0b0);
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
}
.suggestion-btn:hover {
  border-color: var(--accent, #6366f1);
  color: var(--accent, #6366f1);
  background: var(--accent-light, rgba(99,102,241,0.1));
}

.input-row {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}
.msg-input {
  flex: 1;
  padding: 10px 14px;
  border-radius: var(--radius-md, 10px);
  border: 1px solid var(--border-color, #252530);
  background: var(--bg-input, #1a1a24);
  color: var(--text-primary, #f0f0f5);
  font-size: 13px;
  font-family: inherit;
  outline: none;
  transition: all 0.15s ease;
  resize: none;
}
.msg-input:focus {
  border-color: var(--accent, #6366f1);
  box-shadow: 0 0 0 3px var(--accent-light, rgba(99,102,241,0.15));
}
.msg-input::placeholder { color: var(--text-muted, #505060); }
.msg-input:disabled { opacity: 0.5; }

.send-btn {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md, 10px);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: var(--gradient-primary, linear-gradient(135deg, #6366f1, #8b5cf6));
  color: #fff;
  transition: all 0.15s ease;
  flex-shrink: 0;
}
.send-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
}
.send-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* ===== Thinking Animation ===== */
.thinking { min-height: 32px; display: flex; align-items: center; }
.dot {
  font-size: 24px;
  line-height: 1;
  color: var(--accent, #6366f1);
  animation: blink 1.4s infinite both;
}
.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink {
  0%, 80%, 100% { opacity: 0; }
  40% { opacity: 1; }
}

/* ===== Transitions ===== */
.panel-enter-active { transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
.panel-leave-active { transition: all 0.2s ease; }
.panel-enter-from { opacity: 0; transform: translateY(20px) scale(0.96); }
.panel-leave-to { opacity: 0; transform: translateY(10px) scale(0.96); }

@keyframes fadeSlideUp {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ===== Spinner ===== */
.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
  display: block;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ===== Responsive ===== */
@media (max-width: 640px) {
  .ai-panel {
    width: calc(100vw - 32px);
    height: 70vh;
    right: -8px;
  }
}
</style>
