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