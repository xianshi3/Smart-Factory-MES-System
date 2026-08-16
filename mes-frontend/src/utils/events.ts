/**
 * 全局数据变更事件中心
 * AI 助手/跨组件操作写入数据后广播事件，相关页面自动刷新列表
 */

export const DATA_CHANGED_EVENT = 'mes:data-changed'

export type DataDomain =
  | 'workorder'      // 工单
  | 'planning'       // 排产
  | 'process'        // 工艺
  | 'quality'        // 质检
  | 'device'         // 设备
  | 'alarm'          // 告警
  | 'bom'            // BOM/物料/库存
  | 'all'

/** AI 工具名 → 数据域映射 */
export const TOOL_DATA_DOMAIN: Record<string, DataDomain> = {
  create_work_order: 'workorder',
  // 预留: 未来新增写入型工具在此登记
}

/** 广播数据变更（type 为数据域，payload 附带详情） */
export function notifyDataChanged(type: DataDomain, payload?: any) {
  window.dispatchEvent(new CustomEvent(DATA_CHANGED_EVENT, { detail: { type, payload, at: Date.now() } }))
}

/** 订阅数据变更，返回取消函数 */
export function onDataChanged(type: DataDomain | DataDomain[], cb: (payload?: any) => void): () => void {
  const types = Array.isArray(type) ? type : [type]
  const handler = (e: Event) => {
    const detail = (e as CustomEvent).detail
    if (!detail || !detail.type) return
    if (types.includes('all') || types.includes(detail.type)) {
      cb(detail.payload)
    }
  }
  window.addEventListener(DATA_CHANGED_EVENT, handler)
  return () => window.removeEventListener(DATA_CHANGED_EVENT, handler)
}
