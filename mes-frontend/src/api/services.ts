import request from './index'

const WORKORDER_URL = 'http://localhost:8082'
const PROCESS_URL = 'http://localhost:8083'
const QUALITY_URL = 'http://localhost:8084/quality'
const DASHBOARD_URL = 'http://localhost:8085/api/dashboard'

export function getWorkOrderPage(params: any) {
  return request({ url: `${WORKORDER_URL}/workorder/page`, method: 'get', params })
}

export function getWorkOrderDetail(id: number) {
  return request({ url: `${WORKORDER_URL}/workorder/${id}`, method: 'get' })
}

export function createWorkOrder(data: any) {
  return request({ url: `${WORKORDER_URL}/workorder`, method: 'post', data })
}

export function updateWorkOrder(id: number, data: any) {
  return request({ url: `${WORKORDER_URL}/workorder/${id}`, method: 'put', data })
}

export function issueWorkOrder(id: number) {
  return request({ url: `${WORKORDER_URL}/workorder/${id}/issue`, method: 'post' })
}

export function startWorkOrder(id: number) {
  return request({ url: `${WORKORDER_URL}/workorder/${id}/start`, method: 'post' })
}

export function submitReport(data: any) {
  return request({ url: `${WORKORDER_URL}/workorder/report`, method: 'post', data })
}

export function getTemplatePage(params: any) {
  return request({ url: `${PROCESS_URL}/template/page`, method: 'get', params })
}

export function getTemplateDetail(id: number) {
  return request({ url: `${PROCESS_URL}/template/${id}`, method: 'get' })
}

export function createTemplate(data: any) {
  return request({ url: `${PROCESS_URL}/template`, method: 'post', data })
}

export function updateTemplate(id: number, data: any) {
  return request({ url: `${PROCESS_URL}/template/${id}`, method: 'put', data })
}

export function publishTemplate(id: number) {
  return request({ url: `${PROCESS_URL}/template/${id}/publish`, method: 'post' })
}

export function checkParameters(data: any) {
  return request({ url: `${PROCESS_URL}/parameter/check`, method: 'post', data })
}

export function getQualityPage(params: any) {
  return request({ url: `${QUALITY_URL}/record/page`, method: 'get', params })
}

export function getQualityDetail(id: number) {
  return request({ url: `${QUALITY_URL}/record/${id}`, method: 'get' })
}

export function createQualityRecord(data: any) {
  return request({ url: `${QUALITY_URL}/record`, method: 'post', data })
}

export function passQuality(id: number) {
  return request({ url: `${QUALITY_URL}/record/${id}/pass`, method: 'post' })
}

export function failQuality(id: number, reason: string) {
  return request({ url: `${QUALITY_URL}/record/${id}/fail`, method: 'post', data: { reason } })
}

export function forwardTrace(sn: string) {
  return request({ url: `${QUALITY_URL}/trace/forward`, method: 'get', params: { sn } })
}

export function reverseTrace(workOrderId: number) {
  return request({ url: `${QUALITY_URL}/trace/reverse`, method: 'get', params: { workOrderId } })
}

export function getDeviceStatus() {
  return request({ url: `${DASHBOARD_URL}/devices`, method: 'get' })
}
