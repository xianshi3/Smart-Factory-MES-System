import request from './index'

/**
 * Get dashboard overview data
 */
export function getOverview() {
  return request({ url: '/dashboard/overview', method: 'get' })
}

/**
 * Get device status list
 */
export function getDeviceStatus() {
  return request({ url: '/dashboard/devices', method: 'get' })
}

/**
 * Create a new device
 */
export function createDevice(data: any) {
  return request({ url: '/dashboard/device', method: 'post', data })
}

/**
 * Delete a device
 */
export function deleteDevice(deviceId: number) {
  return request({ url: `/dashboard/device/${deviceId}`, method: 'delete' })
}

/**
 * Delete all devices
 */
export function deleteAllDevices() {
  return request({ url: '/dashboard/devices/all', method: 'delete' })
}

/**
 * Update a device
 */
export function updateDevice(data: any) {
  return request({ url: '/dashboard/device', method: 'put', data })
}

/**
 * Get today's production statistics
 */
export function getTodayStats() {
  return request({ url: '/dashboard/production/today', method: 'get' })
}

/**
 * Calculate OEE (Overall Equipment Effectiveness)
 */
export function getOeeCalculate(params: any) {
  return request({ url: '/dashboard/oee/calculate', method: 'get', params })
}

/**
 * Get production trend data
 */
export function getTrendData(days: number) {
  return request({ url: '/dashboard/trend', method: 'get', params: { days } })
}