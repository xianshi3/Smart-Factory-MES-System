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
  return request({ url: '/process/template/parameter/check', method: 'post', data })
}

export function deleteTemplate(id: number) {
  return request({ url: `/process/template/${id}`, method: 'delete' })
}

export function getQualityPage(params: any) {
  return request({ url: '/quality/record/page', method: 'get', params })
}

export function createQualityRecord(data: any) {
  return request({ url: '/quality/record', method: 'post', data })
}

export function passQuality(id: number) {
  return request({ url: `/quality/record/${id}/pass`, method: 'post' })
}

export function deleteQualityRecord(id: string | number) {
  return request({ url: `/quality/record/${id}`, method: 'delete' })
}

export function forwardTrace(sn: string) {
  return request({ url: '/quality/trace/forward', method: 'get', params: { sn } })
}

const AI_BASE_URL = import.meta.env.VITE_AI_SERVICE_URL || '/api/ai'

export function predictDeviceFault(data: any) {
  return request({ url: `${AI_BASE_URL}/api/v1/predict/device/fault`, method: 'post', data })
}

export function predictCapacity(data: any) {
  return request({ url: `${AI_BASE_URL}/api/v1/analysis/capacity/predict`, method: 'post', data })
}

export function analyzeSPC(data: any) {
  return request({ url: `${AI_BASE_URL}/api/v1/analysis/spc/analyze`, method: 'post', data })
}

export function llmChat(data: any) {
  return request({ url: `${AI_BASE_URL}/api/v1/llm/chat`, method: 'post', data })
}

export function optimizeEnergy(data: any) {
  return request({ url: `${AI_BASE_URL}/api/v1/analysis/energy/optimize`, method: 'post', data })
}

export function getAlarmDevices() {
  return request({ url: '/api/dashboard/alarms', method: 'get' })
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

export function acknowledgeAlarm(alarmId: number, userId: string) {
  return request({ url: `/api/alarm/${alarmId}/ack`, method: 'post', params: { userId } })
}

export function resolveAlarm(alarmId: number, userId: string, remarks: string) {
  return request({ url: `/api/alarm/${alarmId}/resolve`, method: 'post', params: { userId, remarks } })
}

export function deleteAlarm(alarmId: number) {
  return request({ url: `/api/alarm/${alarmId}`, method: 'delete' })
}
