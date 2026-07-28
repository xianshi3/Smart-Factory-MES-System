<template>
  <div class="ai-assistant">
    <el-button
      class="ai-toggle-btn"
      :type="visible ? 'primary' : 'warning'"
      :icon="visible ? Close : ChatDotSquare"
      circle
      :size="visible ? 'default' : 'large'"
      @click="visible = !visible"
    />

    <Transition name="slide-fade">
      <div v-if="visible" class="ai-panel">
        <div class="ai-header">
          <el-icon><MagicStick /></el-icon>
          <span>AI 生产助理</span>
          <span class="ai-badge">Agent</span>
          <el-button text @click="clearChat" size="small" style="margin-left: auto">
            清空
          </el-button>
        </div>

        <div class="ai-messages" ref="messagesRef">
          <div v-for="(msg, i) in messages" :key="i" class="ai-msg" :class="msg.role">
            <div class="msg-avatar">
              <el-icon v-if="msg.role === 'assistant'"><MagicStick /></el-icon>
              <el-icon v-else><User /></el-icon>
            </div>
            <div class="msg-bubble">
              <div class="msg-content" v-html="renderContent(msg.content)" />
              <div v-if="msg.steps && msg.steps.length" class="msg-steps">
                <el-collapse accordion>
                  <el-collapse-item
                    v-for="(step, j) in msg.steps"
                    :key="j"
                    :title="`${j + 1}. 调用 ${step.tool}`"
                    name="step"
                  >
                    <div class="step-detail">
                      <div class="step-label">参数:</div>
                      <pre>{{ JSON.stringify(step.args, null, 2) }}</pre>
                      <div class="step-label">结果:</div>
                      <pre>{{ JSON.stringify(step.result, null, 2).slice(0, 500) }}</pre>
                    </div>
                  </el-collapse-item>
                </el-collapse>
              </div>
              <div class="msg-time">{{ formatTime(msg.timestamp) }}</div>
            </div>
          </div>

          <div v-if="loading" class="ai-msg assistant">
            <div class="msg-avatar">
              <el-icon><MagicStick /></el-icon>
            </div>
            <div class="msg-bubble">
              <span class="typing-dots">
                <span>.</span><span>.</span><span>.</span>
              </span>
            </div>
          </div>
        </div>

        <div class="ai-input">
          <el-input
            v-model="input"
            type="textarea"
            :rows="2"
            :disabled="loading"
            placeholder="输入生产指令，例如：查询 DEV-001 设备状态"
            @keydown.enter.exact.prevent="sendMessage"
          />
          <el-button
            type="primary"
            :loading="loading"
            :disabled="!input.trim()"
            @click="sendMessage"
            style="margin-top: 8px; width: 100%"
          >
            {{ loading ? '思考中...' : '发送指令' }}
          </el-button>
          <div class="ai-tips">
            <span @click="quickTip('查看所有设备状态')">📊 设备状态</span>
            <span @click="quickTip('查询 DEV-001 详情，温度是否正常')">🔍 设备诊断</span>
            <span @click="quickTip('创建一条 HIGH 优先级的维修工单')">📝 创建工单</span>
            <span @click="quickTip('主轴温度过高怎么处理')">📖 查手册</span>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Close, ChatDotSquare, MagicStick, User } from '@element-plus/icons-vue'
import { runAgent, type AgentStep } from '@/api/agent'

const visible = ref(false)
const input = ref('')
const loading = ref(false)
const messagesRef = ref<HTMLElement | null>(null)

interface Message {
  role: 'user' | 'assistant'
  content: string
  steps?: AgentStep[]
  timestamp: Date
}

const messages = ref<Message[]>([
  {
    role: 'assistant',
    content: '你好！我是 <b>AI 生产助理</b>，可以帮你：<br>'
      + '• 查询设备实时状态<br>'
      + '• 诊断设备异常并创建维修工单<br>'
      + '• 查询生产线和工位信息<br>'
      + '• 搜索设备维护手册和质检标准<br><br>'
      + '试试点击下方快捷指令 👇',
    timestamp: new Date(),
  },
])

async function sendMessage() {
  const text = input.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text, timestamp: new Date() })
  input.value = ''
  loading.value = true

  try {
    const history = messages.value
      .filter(m => m.role !== 'assistant' || m !== messages.value[messages.value.length - 1])
      .map(m => ({ role: m.role, content: m.content }))

    const res = await runAgent(text, history.slice(-10))

    if (res.success) {
      messages.value.push({
        role: 'assistant',
        content: res.content || '已完成',
        steps: res.steps,
        timestamp: new Date(),
      })
    } else {
      messages.value.push({
        role: 'assistant',
        content: '抱歉，执行失败：' + (res.content || '未知错误'),
        timestamp: new Date(),
      })
    }
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

function quickTip(text: string) {
  input.value = text
}

function clearChat() {
  messages.value = [
    {
      role: 'assistant',
      content: '对话已清空，有什么可以帮你的？',
      timestamp: new Date(),
    },
  ]
}

function renderContent(text: string): string {
  return text
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
}

function formatTime(d: Date): string {
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

watch(visible, () => {
  if (visible.value) {
    nextTick(scrollToBottom)
  }
})
</script>

<style scoped>
.ai-assistant {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 9999;
}

.ai-toggle-btn {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.ai-panel {
  position: absolute;
  bottom: 60px;
  right: 0;
  width: 400px;
  height: 580px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.ai-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color-light);
  font-weight: 600;
  font-size: 14px;
}

.ai-badge {
  background: var(--el-color-warning);
  color: #fff;
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 500;
}

.ai-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ai-msg {
  display: flex;
  gap: 8px;
  max-width: 90%;
}

.ai-msg.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.msg-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  flex-shrink: 0;
}

.assistant .msg-avatar {
  background: var(--el-color-warning-light-8);
  color: var(--el-color-warning);
}

.user .msg-avatar {
  background: var(--el-color-primary-light-8);
  color: var(--el-color-primary);
}

.msg-bubble {
  background: var(--el-fill-color-light);
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 13px;
  line-height: 1.6;
  min-width: 0;
}

.user .msg-bubble {
  background: var(--el-color-primary-light-8);
}

.msg-content {
  word-break: break-word;
}

.msg-steps {
  margin-top: 8px;
}

.step-detail {
  font-size: 12px;
}

.step-label {
  font-weight: 600;
  color: var(--el-text-color-secondary);
  margin: 4px 0 2px;
}

.step-detail pre {
  background: var(--el-fill-color);
  padding: 6px;
  border-radius: 4px;
  font-size: 11px;
  overflow-x: auto;
  max-height: 120px;
}

.msg-time {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
  margin-top: 4px;
}

.ai-input {
  padding: 12px;
  border-top: 1px solid var(--el-border-color-light);
}

.ai-input :deep(.el-textarea__inner) {
  font-size: 13px;
}

.ai-tips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.ai-tips span {
  font-size: 11px;
  padding: 2px 8px;
  background: var(--el-fill-color);
  border-radius: 12px;
  cursor: pointer;
  color: var(--el-text-color-secondary);
  transition: all 0.2s;
}

.ai-tips span:hover {
  background: var(--el-color-warning-light-8);
  color: var(--el-color-warning);
}

.typing-dots span {
  animation: blink 1.4s infinite both;
  font-size: 20px;
  line-height: 1;
}
.typing-dots span:nth-child(2) { animation-delay: 0.2s; }
.typing-dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes blink {
  0%, 80%, 100% { opacity: 0; }
  40% { opacity: 1; }
}

.slide-fade-enter-active {
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}
.slide-fade-leave-active {
  transition: all 0.2s ease;
}
.slide-fade-enter-from {
  opacity: 0;
  transform: translateY(20px) scale(0.95);
}
.slide-fade-leave-to {
  opacity: 0;
  transform: translateY(10px) scale(0.95);
}

@media (max-width: 640px) {
  .ai-panel {
    width: calc(100vw - 32px);
    height: 70vh;
    right: -8px;
  }
}
</style>
