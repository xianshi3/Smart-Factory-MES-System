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
