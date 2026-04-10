import request from './index'

/**
 * Get quality inspection record list with pagination
 */
export function getQualityPage(params: any) {
  return request({ url: '/quality/record/page', method: 'get', params })
}

/**
 * Get quality inspection record detail by ID
 */
export function getQualityDetail(id: number) {
  return request({ url: `/quality/record/${id}`, method: 'get' })
}

/**
 * Create new quality inspection record
 */
export function createQualityRecord(data: any) {
  return request({ url: '/quality/record', method: 'post', data })
}

/**
 * Delete quality inspection record
 */
export function deleteQualityRecord(id: string | number) {
  return request({ url: `/quality/record/${id}`, method: 'delete' })
}

/**
 * Mark quality inspection as passed
 */
export function passQuality(id: number) {
  return request({ url: `/quality/record/${id}/pass`, method: 'post' })
}

/**
 * Mark quality inspection as failed
 */
export function failQuality(id: number, reason: string) {
  return request({ url: `/quality/record/${id}/fail`, method: 'post', data: { reason } })
}

/**
 * Forward trace by product serial number
 */
export function forwardTrace(sn: string) {
  return request({ url: '/quality/trace/forward', method: 'get', params: { sn } })
}

/**
 * Reverse trace by work order ID
 */
export function reverseTrace(workOrderId: number) {
  return request({ url: '/quality/trace/reverse', method: 'get', params: { workOrderId } })
}