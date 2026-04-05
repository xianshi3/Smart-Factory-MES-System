import request from './index'

const BASE_URL = '/workorder'

export function getWorkOrderPage(params: any) {
  return request({ url: `${BASE_URL}/page`, method: 'get', params })
}

export function getWorkOrderDetail(id: number) {
  return request({ url: `${BASE_URL}/${id}`, method: 'get' })
}

export function createWorkOrder(data: any) {
  return request({ url: BASE_URL, method: 'post', data })
}

export function updateWorkOrder(id: number, data: any) {
  return request({ url: `${BASE_URL}/${id}`, method: 'put', data })
}

export function issueWorkOrder(id: number) {
  return request({ url: `${BASE_URL}/${id}/issue`, method: 'post' })
}

export function startWorkOrder(id: number) {
  return request({ url: `${BASE_URL}/${id}/start`, method: 'post' })
}

export function submitReport(data: any) {
  return request({ url: `${BASE_URL}/report`, method: 'post', data })
}