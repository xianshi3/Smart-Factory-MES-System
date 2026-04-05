import request from './index'

/**
 * Get process template list with pagination
 */
export function getTemplatePage(params: any) {
  return request({ url: '/process/template/page', method: 'get', params })
}

/**
 * Get process template detail by ID
 */
export function getTemplateDetail(id: number) {
  return request({ url: `/process/template/${id}`, method: 'get' })
}

/**
 * Create new process template
 */
export function createTemplate(data: any) {
  return request({ url: '/process/template', method: 'post', data })
}

/**
 * Update existing process template
 */
export function updateTemplate(id: number, data: any) {
  return request({ url: `/process/template/${id}`, method: 'put', data })
}

/**
 * Publish process template
 */
export function publishTemplate(id: number) {
  return request({ url: `/process/template/${id}/publish`, method: 'post' })
}

/**
 * Check process parameters validity
 */
export function checkParameters(data: any) {
  return request({ url: '/process/parameter/check', method: 'post', data })
}