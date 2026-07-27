import request from './index'

export function getOverview() {
  return request({ url: '/dashboard/overview', method: 'get' })
}

export function getDeviceStatus() {
  return request({ url: '/dashboard/devices', method: 'get' })
}

export function createDevice(data: any) {
  return request({ url: '/dashboard/device', method: 'post', data })
}

export function deleteDevice(deviceCode: string) {
  return request({ url: `/dashboard/device/${deviceCode}`, method: 'delete' })
}

export function deleteAllDevices() {
  return request({ url: '/dashboard/devices/all', method: 'delete' })
}

export function updateDevice(data: any) {
  return request({ url: '/dashboard/device', method: 'put', data })
}

export function getTodayStats() {
  return request({ url: '/dashboard/production/today', method: 'get' })
}

export function getOeeCalculate(params: any) {
  return request({ url: '/dashboard/oee/calculate', method: 'get', params })
}

export function getTrendData(days: number) {
  return request({ url: '/dashboard/trend', method: 'get', params: { days } })
}

export function getProductionLineList() {
  return request({ url: '/api/dashboard/production-line/list', method: 'get' })
}

export function createProductionLine(data: any) {
  return request({ url: '/api/dashboard/production-line', method: 'post', data })
}

export function updateProductionLine(data: any) {
  return request({ url: '/api/dashboard/production-line', method: 'put', data })
}

export function deleteProductionLine(id: number) {
  return request({ url: `/api/dashboard/production-line/${id}`, method: 'delete' })
}

export function getWorkstationList() {
  return request({ url: '/api/dashboard/workstation/list', method: 'get' })
}

export function createWorkstation(data: any) {
  return request({ url: '/api/dashboard/workstation', method: 'post', data })
}

export function updateWorkstation(data: any) {
  return request({ url: '/api/dashboard/workstation', method: 'put', data })
}

export function deleteWorkstation(id: number) {
  return request({ url: `/api/dashboard/workstation/${id}`, method: 'delete' })
}