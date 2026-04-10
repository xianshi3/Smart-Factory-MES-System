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

export function getDeviceStatus() {
  return request({ url: '/api/dashboard/devices', method: 'get' })
}

export function getAlarmDevices() {
  return request({ url: '/api/dashboard/alarms', method: 'get' })
}

export function getAiHealth() {
  return request({ url: 'http://localhost:8086/api/v1/health', method: 'get' })
}

export function getAiModelStatus() {
  return request({ url: 'http://localhost:8086/api/v1/model/status', method: 'get' })
}

export function predictQuality(data: any) {
  return request({ url: 'http://localhost:8086/api/v1/predict/quality', method: 'post', data })
}

export function predictProduction(data: any) {
  return request({ url: 'http://localhost:8086/api/v1/predict/production', method: 'post', data })
}
