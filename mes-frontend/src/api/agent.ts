import request from './index'

const AI_BASE_URL = import.meta.env.VITE_AI_SERVICE_URL || '/ai'

export interface AgentStep {
  tool: string
  args: Record<string, any>
  result: Record<string, any>
}

export interface AgentPlanStep {
  step: number
  tool: string
  args: Record<string, any>
  purpose: string
}

export interface AgentReport {
  summary: string
  key_points: string[]
  tables: { title: string; columns: string[]; rows: any[][] }[]
  recommendations: string[]
  follow_ups: string[]
}

export interface AgentResponse {
  success: boolean
  content: string | null
  steps: AgentStep[]
  plan: AgentPlanStep[]
  report: AgentReport | null
  intent: string | null
  intent_label: string | null
  session_id: string | null
  timestamp: string
}

export async function runAgent(
  message: string,
  history?: { role: string; content: string }[],
  context?: any,
): Promise<AgentResponse> {
  const res = await request.post(`${AI_BASE_URL}/api/v1/agent/run`, {
    message,
    history: history || [],
    context: context || null,
  })
  return res
}

export async function getAgentTools() {
  const res = await request.get(`${AI_BASE_URL}/api/v1/agent/tools`)
  return res
}

export async function searchKnowledgeBase(query: string, topK = 3) {
  const res = await request.post(`${AI_BASE_URL}/api/v1/agent/kb/search`, null, {
    params: { query, top_k: topK },
  })
  return res
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
  const res = await request.post(`${AI_BASE_URL}/api/v1/agent/conversations`, { title }, {
    params: { user_id: userId },
  })
  return res.conversation
}

export async function listConversations(userId: string): Promise<ConversationListItem[]> {
  const res = await request.get(`${AI_BASE_URL}/api/v1/agent/conversations`, {
    params: { user_id: userId },
  })
  return res.conversations || []
}

export async function getConversation(id: string): Promise<ConversationDetail> {
  const res = await request.get(`${AI_BASE_URL}/api/v1/agent/conversations/${id}`)
  return res.conversation
}

export async function addConversationMessage(
  id: string,
  role: 'user' | 'assistant',
  content: string,
  steps?: AgentStep[],
  autoTitle = false,
) {
  await request.post(`${AI_BASE_URL}/api/v1/agent/conversations/${id}/messages`, {
    role,
    content,
    steps: steps || null,
    auto_title: autoTitle,
  })
}

export async function deleteConversation(id: string) {
  await request.delete(`${AI_BASE_URL}/api/v1/agent/conversations/${id}`)
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
  const res = await request.post(`${AI_BASE_URL}/api/v1/agent/analysis`, {
    user_id: userId,
    device_code: deviceCode,
    device_name: deviceName,
    analysis_type: analysisType,
    result_data: resultData,
  })
  return res?.id || 0
}

export async function listAnalyses(userId: string, type?: string, deviceCode?: string): Promise<AnalysisRecord[]> {
  const res = await request.get(`${AI_BASE_URL}/api/v1/agent/analysis`, { params: { user_id: userId, type, device_code: deviceCode } })
  return res.analyses || []
}

export async function deleteAnalysis(id: number, userId: string) {
  await request.delete(`${AI_BASE_URL}/api/v1/agent/analysis/${id}`, { params: { user_id: userId } })
}
