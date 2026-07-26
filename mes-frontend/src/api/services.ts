import request from './index'

export function getWorkOrderPage(params: any) {
  return request({ url: '/workorder/page', method: 'get', params })
}

export function getWorkOrderDetail(id: string | number) {
  return request({ url: `/workorder/${id}`, method: 'get' })
}

export function createWorkOrder(data: any) {
  return request({ url: '/workorder', method: 'post', data })
}

export function updateWorkOrder(id: number, data: any) {
  return request({ url: `/workorder/${id}`, method: 'put', data })
}

export function issueWorkOrder(id: number) {
  return request({ url: `/workorder/${id}/issue`, method: 'post' })
}

export function startWorkOrder(id: number) {
  return request({ url: `/workorder/${id}/start`, method: 'post' })
}

export function completeWorkOrder(id: number) {
  return request({ url: `/workorder/${id}/complete`, method: 'post' })
}

export function submitReport(data: any) {
  return request({ url: '/workorder/report', method: 'post', data })
}

export function deleteWorkOrder(id: string | number) {
  return request({ url: `/workorder/${id}`, method: 'delete' })
}

export function getTemplatePage(params: any) {
  return request({ url: '/process/template/page', method: 'get', params })
}

export function getTemplateDetail(id: number) {
  return request({ url: `/process/template/${id}`, method: 'get' })
}

export function createTemplate(data: any) {
  return request({ url: '/process/template', method: 'post', data })
}

export function updateTemplate(id: number, data: any) {
  return request({ url: `/process/template/${id}`, method: 'put', data })
}

export function publishTemplate(id: number) {
  return request({ url: `/process/template/${id}/publish`, method: 'post' })
}

export function checkParameters(data: any) {
  return request({ url: '/process/parameter/check', method: 'post', data })
}

export function deleteTemplate(id: number) {
  return request({ url: `/process/template/${id}`, method: 'delete' })
}

export function getQualityPage(params: any) {
  return request({ url: '/quality/record/page', method: 'get', params })
}

export function getQualityDetail(id: number) {
  return request({ url: `/quality/record/${id}`, method: 'get' })
}

export function createQualityRecord(data: any) {
  return request({ url: '/quality/record', method: 'post', data })
}

export function passQuality(id: number) {
  return request({ url: `/quality/record/${id}/pass`, method: 'post' })
}

export function failQuality(id: number, reason: string) {
  return request({ url: `/quality/record/${id}/fail`, method: 'post', data: { reason } })
}

export function deleteQualityRecord(id: string | number) {
  return request({ url: `/quality/record/${id}`, method: 'delete' })
}

export function forwardTrace(sn: string) {
  return request({ url: '/quality/trace/forward', method: 'get', params: { sn } })
}

export function reverseTrace(workOrderId: number) {
  return request({ url: '/quality/trace/reverse', method: 'get', params: { workOrderId } })
}

export function getAlarmDevices() {
  return request({ url: '/api/dashboard/alarms', method: 'get' })
}

const AI_BASE_URL = import.meta.env.VITE_AI_SERVICE_URL || '/api/ai'

export function getAiHealth() {
  return request({ url: `${AI_BASE_URL}/api/v1/health`, method: 'get' })
}

export function getAiModelStatus() {
  return request({ url: `${AI_BASE_URL}/api/v1/model/status`, method: 'get' })
}

export function predictQuality(data: any) {
  return request({ url: `${AI_BASE_URL}/api/v1/predict/quality`, method: 'post', data })
}

export function predictProduction(data: any) {
  return request({ url: `${AI_BASE_URL}/api/v1/predict/production`, method: 'post', data })
}

export function predictDeviceFault(data: any) {
  return request({ url: `${AI_BASE_URL}/api/v1/predict/device/fault`, method: 'post', data })
}

export function recommendProcessParams(data: any) {
  return request({ url: `${AI_BASE_URL}/api/v1/predict/process/recommend`, method: 'post', data })
}

export function detectAnomaly(data: any) {
  return request({ url: `${AI_BASE_URL}/api/v1/predict/anomaly`, method: 'post', data })
}

export function getAiModelInfo() {
  return request({ url: `${AI_BASE_URL}/api/v1/predict/model/info`, method: 'get' })
}

export function llmChat(data: any) {
  return request({ url: `${AI_BASE_URL}/api/v1/llm/chat`, method: 'post', data })
}

export function llmAnalyze(data: any) {
  return request({ url: `${AI_BASE_URL}/api/v1/llm/analyze`, method: 'post', data })
}

export function llmRecommend(data: any) {
  return request({ url: `${AI_BASE_URL}/api/v1/llm/recommend`, method: 'post', data })
}

export function getLlmInfo() {
  return request({ url: `${AI_BASE_URL}/api/v1/llm/info`, method: 'get' })
}

export function analyzeSPC(data: any) {
  return request({ url: `${AI_BASE_URL}/api/v1/analysis/spc/analyze`, method: 'post', data })
}

export function predictCapacity(data: any) {
  return request({ url: `${AI_BASE_URL}/api/v1/analysis/capacity/predict`, method: 'post', data })
}

export function analyzeRootCause(data: any) {
  return request({ url: `${AI_BASE_URL}/api/v1/analysis/root-cause/analyze`, method: 'post', data })
}

export function predictDelivery(data: any) {
  return request({ url: `${AI_BASE_URL}/api/v1/analysis/delivery/predict`, method: 'post', data })
}

export function optimizeEnergy(data: any) {
  return request({ url: `${AI_BASE_URL}/api/v1/analysis/energy/optimize`, method: 'post', data })
}

export function startDevice(deviceId: number) {
  return request({ url: `/api/dashboard/device/${deviceId}/start`, method: 'post' })
}

export function stopDevice(deviceId: number) {
  return request({ url: `/api/dashboard/device/${deviceId}/stop`, method: 'post' })
}

export function getProductionReport(params: any) {
  return request({ url: `/api/dashboard/report/production`, method: 'get', params })
}

export function getAllAlarms() {
  return request({ url: '/api/alarm', method: 'get' })
}

export function getAlarmsByStatus(status: string) {
  return request({ url: `/api/alarm/status/${status}`, method: 'get' })
}

export function createAlarm(data: any) {
  return request({ url: '/api/alarm', method: 'post', data })
}

export function acknowledgeAlarm(alarmId: number, userId: string) {
  return request({ url: `/api/alarm/${alarmId}/ack`, method: 'post', params: { userId } })
}

export function resolveAlarm(alarmId: number, userId: string, remarks: string) {
  return request({ url: `/api/alarm/${alarmId}/resolve`, method: 'post', params: { userId, remarks } })
}

export function deleteAlarm(alarmId: number) {
  return request({ url: `/api/alarm/${alarmId}`, method: 'delete' })
}

export function getActiveAlarmCount() {
  return request({ url: '/api/alarm/count/active', method: 'get' })
}
