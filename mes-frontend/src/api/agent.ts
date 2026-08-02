import axios from 'axios'

const aiRequest = axios.create({
  baseURL: '/ai',
  timeout: 60000,
})

export interface AgentStep {
  tool: string
  args: Record<string, any>
  result: Record<string, any>
}

export interface AgentResponse {
  success: boolean
  content: string | null
  steps: AgentStep[]
  session_id: string | null
  timestamp: string
}

export async function runAgent(
  message: string,
  history?: { role: string; content: string }[]
): Promise<AgentResponse> {
  const res = await aiRequest.post('/api/v1/agent/run', {
    message,
    history: history || [],
  })
  return res.data
}

export async function getAgentTools() {
  const res = await aiRequest.get('/api/v1/agent/tools')
  return res.data
}

export async function searchKnowledgeBase(query: string, topK = 3) {
  const res = await aiRequest.post('/api/v1/agent/kb/search', null, {
    params: { query, top_k: topK },
  })
  return res.data
}

// ========== 对话历史 ==========

export interface ConversationListItem {
  id: string
  title: string
  created_at: string
  updated_at: string
}

export interface ConversationMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
  steps: AgentStep[]
  created_at: string
}

export interface ConversationDetail {
  id: string
  user_id: string
  title: string
  created_at: string
  updated_at: string
  messages: ConversationMessage[]
}

export async function createConversation(userId: string, title = '新对话'): Promise<ConversationListItem> {
  const res = await aiRequest.post('/api/v1/agent/conversations', { title }, {
    params: { user_id: userId },
  })
  return res.data.conversation
}

export async function listConversations(userId: string): Promise<ConversationListItem[]> {
  const res = await aiRequest.get('/api/v1/agent/conversations', {
    params: { user_id: userId },
  })
  return res.data.conversations
}

export async function getConversation(id: string): Promise<ConversationDetail> {
  const res = await aiRequest.get(`/api/v1/agent/conversations/${id}`)
  return res.data.conversation
}

export async function addConversationMessage(
  id: string,
  role: 'user' | 'assistant',
  content: string,
  steps?: AgentStep[],
  autoTitle = false,
) {
  await aiRequest.post(`/api/v1/agent/conversations/${id}/messages`, {
    role,
    content,
    steps: steps || null,
    auto_title: autoTitle,
  })
}

export async function deleteConversation(id: string) {
  await aiRequest.delete(`/api/v1/agent/conversations/${id}`)
}

// ========== 分析历史 ==========

export interface AnalysisRecord {
  id: number
  device_code: string
  device_name: string
  analysis_type: string
  result_data: any
  created_at: string
}

export async function saveAnalysis(userId: string, deviceCode: string, deviceName: string, analysisType: string, resultData: any): Promise<number> {
  const res = await aiRequest.post('/api/v1/agent/analysis', {
    user_id: userId,
    device_code: deviceCode,
    device_name: deviceName,
    analysis_type: analysisType,
    result_data: resultData,
  })
  return res.data?.id || 0
}

export async function listAnalyses(userId: string, type?: string, deviceCode?: string): Promise<AnalysisRecord[]> {
  const res = await aiRequest.get('/api/v1/agent/analysis', { params: { user_id: userId, type, device_code: deviceCode } })
  return res.data.analyses || []
}

export async function deleteAnalysis(id: number, userId: string) {
  await aiRequest.delete(`/api/v1/agent/analysis/${id}`, { params: { user_id: userId } })
}
