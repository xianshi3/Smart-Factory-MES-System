import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  type ConversationListItem,
  type ConversationDetail,
  type ConversationMessage,
  type AgentStep,
  listConversations,
  getConversation,
  createConversation,
  deleteConversation,
  addConversationMessage,
  runAgent,
} from '@/api/agent'

export const useAiChatStore = defineStore('aiChat', () => {
  const conversations = ref<ConversationListItem[]>([])
  const currentId = ref<string | null>(null)
  const messages = ref<MessageItem[]>([])
  const loading = ref(false)
  const loadingList = ref(false)

  interface MessageItem {
    id?: number
    role: 'user' | 'assistant'
    content: string
    steps?: (AgentStep & { expanded?: boolean })[]
    timestamp: Date
    saved: boolean
  }

  const currentTitle = computed(() => {
    const c = conversations.value.find(c => c.id === currentId.value)
    return c?.title || ''
  })

  const isEmpty = computed(() => messages.value.length === 0)
  const messageCount = computed(() => messages.value.length)

  async function loadList() {
    loadingList.value = true
    try {
      conversations.value = await listConversations()
    } catch { /* 静默失败 - 后端可能未启动 */ } finally {
      loadingList.value = false
    }
  }

  async function selectConversation(id: string) {
    if (id === currentId.value) return
    currentId.value = id
    messages.value = []
    try {
      const detail: ConversationDetail = await getConversation(id)
      messages.value = detail.messages.map(m => ({
        id: m.id,
        role: m.role,
        content: m.content,
        steps: (m.steps || []).map(s => ({ ...s, expanded: false })),
        timestamp: new Date(m.created_at),
        saved: true,
      }))
    } catch {
      // 加载失败 — 保持空列表
    }
  }

  async function newChat(): Promise<string | null> {
    try {
      const conv = await createConversation('新对话')
      conversations.value.unshift(conv)
      await selectConversation(conv.id)
      return conv.id
    } catch {
      return null
    }
  }

  async function sendMessage(text: string): Promise<boolean> {
    if (!text.trim() || loading.value) return false

    const now = new Date()

    // 确保有当前对话
    if (!currentId.value) {
      const ok = await newChat()
      if (!ok) return false
    }

    const convId = currentId.value!

    // 添加用户消息
    const userMsg: MessageItem = { role: 'user', content: text, timestamp: now, saved: false }
    messages.value.push(userMsg)

    // 保存用户消息到后端
    try {
      await addConversationMessage(convId, 'user', text, undefined, messages.value.length === 1)
      userMsg.saved = true
    } catch { /* 静默 */ }

    // 调用 Agent
    loading.value = true
    try {
      const history = messages.value.slice(0, -1)
        .filter(m => m.role !== m.role || m !== messages.value[messages.value.length - 1])
        .map(m => ({ role: m.role, content: m.content }))

      const res = await runAgent(text, history.slice(-10))

      const assistantMsg: MessageItem = {
        role: 'assistant',
        content: res.success ? (res.content || '已完成') : ('执行失败：' + (res.content || '未知错误')),
        steps: (res.steps || []).map(s => ({ ...s, expanded: false })),
        timestamp: new Date(res.timestamp),
        saved: false,
      }
      messages.value.push(assistantMsg)

      // 保存 AI 回复到后端
      try {
        await addConversationMessage(convId, 'assistant', assistantMsg.content, res.steps as AgentStep[])
        assistantMsg.saved = true
      } catch { /* 静默 */ }

      return true
    } catch (e: any) {
      const errorMsg: MessageItem = {
        role: 'assistant',
        content: '连接 AI 服务失败：' + (e?.message || '网络异常') + '。请确认 AI 服务已启动。',
        timestamp: new Date(),
        saved: false,
      }
      messages.value.push(errorMsg)
      try {
        await addConversationMessage(convId, 'assistant', errorMsg.content)
        errorMsg.saved = true
      } catch { /* 静默 */ }
      return false
    } finally {
      loading.value = false
      // 刷新列表以更新标题/时间
      loadList()
    }
  }

  async function removeConversation(id: string) {
    try {
      await deleteConversation(id)
      conversations.value = conversations.value.filter(c => c.id !== id)
      if (currentId.value === id) {
        currentId.value = null
        messages.value = []
      }
    } catch { /* 静默 */ }
  }

  async function clearMessages() {
    messages.value = []
    const ok = await newChat()
    if (!ok) {
      // 回退到本地空列表
      currentId.value = null
    }
  }

  return {
    conversations,
    currentId,
    messages,
    loading,
    loadingList,
    currentTitle,
    isEmpty,
    messageCount,
    loadList,
    selectConversation,
    newChat,
    sendMessage,
    removeConversation,
    clearMessages,
  }
})
