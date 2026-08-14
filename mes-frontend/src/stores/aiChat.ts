import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  type ConversationListItem,
  type ConversationDetail,
  type ConversationMessage,
  type AgentStep,
  type AgentPlanStep,
  type AgentReport,
  listConversations,
  getConversation,
  createConversation,
  deleteConversation,
  addConversationMessage,
  runAgent,
} from '@/api/agent'
import { useUserStore } from '@/stores/user'

export const useAiChatStore = defineStore('aiChat', () => {
  const userStore = useUserStore()

  const userId = computed(() => userStore.userInfo?.username || 'default')
  const aiOnline = ref(true)
  let _pollTimer: ReturnType<typeof setInterval> | null = null
  const conversations = ref<ConversationListItem[]>([])
  const currentId = ref<string | null>(null)
  const messages = ref<MessageItem[]>([])
  const loading = ref(false)
  const loadingList = ref(false)

  /** 确保当前用户信息已加载（user_id 依赖 username，加载失败时回退 'default'） */
  async function ensureUser() {
    if (!userStore.userInfo && userStore.token) {
      try {
        await userStore.getUserInfo()
      } catch { /* 静默，回退 'default' */ }
    }
  }

  interface MessageItem {
    id?: number
    role: 'user' | 'assistant'
    content: string
    steps?: (AgentStep & { expanded?: boolean })[]
    plan?: AgentPlanStep[]
    report?: AgentReport | null
    intentLabel?: string
    timestamp: Date
    saved: boolean
  }

  const currentTitle = computed(() => {
    const c = conversations.value.find(c => c.id === currentId.value)
    return c?.title || ''
  })

  const isEmpty = computed(() => messages.value.length === 0)
  const messageCount = computed(() => messages.value.length)

  async function checkHealth(): Promise<boolean> {
    try {
      const { default: request } = await import('@/api')
      await request.get('/ai/api/v1/agent/tools', { timeout: 3000 })
      aiOnline.value = true
      stopPolling()
      return true
    } catch {
      if (aiOnline.value) {
        aiOnline.value = false
        startPolling()
      }
      return false
    }
  }

  function startPolling() {
    stopPolling()
    _pollTimer = setInterval(checkHealth, 5000)
  }

  function stopPolling() {
    if (_pollTimer) { clearInterval(_pollTimer); _pollTimer = null }
  }

  async function loadList() {
    loadingList.value = true
    try {
      await ensureUser()
      conversations.value = await listConversations(userId.value)
    } catch { /* 静默 */ } finally {
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
      await ensureUser()
      const conv = await createConversation(userId.value, '新对话')
      conversations.value.unshift(conv)
      await selectConversation(conv.id)
      return conv.id
    } catch {
      return null
    }
  }

  async function sendMessage(text: string, context?: any): Promise<boolean> {
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
      // 仅携带已持久化的历史消息（slice 已排除刚推送的当前用户消息）
      const history = messages.value
        .slice(0, -1)
        .filter(m => m.saved)
        .map(m => ({ role: m.role, content: m.content }))

      const res = await runAgent(text, history.slice(-10), context)

      aiOnline.value = true; stopPolling()

      const assistantMsg: MessageItem = {
        role: 'assistant',
        content: res.success ? (res.content || '已完成') : ('执行失败：' + (res.content || '未知错误')),
        steps: (res.steps || []).map(s => ({ ...s, expanded: false })),
        plan: res.plan || [],
        report: res.report || null,
        intentLabel: res.intent_label || undefined,
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
      aiOnline.value = false
      startPolling()
      const errorMsg: MessageItem = {
        role: 'assistant',
        content: '**AI 服务未连接**\n\n请在终端执行以下命令启动：\n\n```bash\ncd mes-ai-service && python -m src.main\n```',
        timestamp: new Date(),
        saved: false,
      }
      messages.value.push(errorMsg)
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
    // 清空消息但保留当前对话（继续发送会回到同一对话），避免每次清空都新建一条记录
    messages.value = []
    if (!currentId.value) {
      const ok = await newChat()
      if (!ok) {
        currentId.value = null
      }
    }
  }

  return {
    conversations,
    currentId,
    messages,
    loading,
    loadingList,
    aiOnline,
    currentTitle,
    isEmpty,
    messageCount,
    checkHealth,
    loadList,
    selectConversation,
    newChat,
    sendMessage,
    removeConversation,
    clearMessages,
  }
})
