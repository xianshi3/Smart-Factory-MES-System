import { ref, onUnmounted } from 'vue'
import { getToken } from './auth'

/**
 * WebSocket 地址推导：
 * 1. 优先使用 VITE_WS_URL（生产环境显式配置）；
 * 2. 否则走网关同源 WebSocket 端点（与 HTTP 同源，自动带 token）。
 */
function resolveWsUrl(): string {
  const configured = import.meta.env.VITE_WS_URL as string | undefined
  if (configured) return configured
  const proto = window.location.protocol === 'https:' ? 'wss' : 'ws'
  const host = window.location.host || 'localhost:9090'
  const base = `${proto}://${host}/api/ws/dashboard`
  const token = getToken()
  return token ? `${base}?token=${encodeURIComponent(token)}` : base
}

class WebSocketService {
  private ws: WebSocket | null = null
  private reconnectTimer: number | null = null
  private messageHandlers: ((data: any) => void)[] = []
  private intentionalDisconnect = false
  /** 订阅者引用计数：所有订阅者退出后才真正断开 */
  private refCount = 0
  private connected = ref(false)

  connect() {
    if (this.ws?.readyState === WebSocket.OPEN || this.ws?.readyState === WebSocket.CONNECTING) return

    this.refCount = Math.max(this.refCount, 1)
    if (this.ws) return

    try {
      this.intentionalDisconnect = false
      this.ws = new WebSocket(resolveWsUrl())

      this.ws.onopen = () => {
        this.connected.value = true
        wsConnected.value = true
        this.scheduleReconnect(null)
      }

      this.ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          this.messageHandlers.forEach(handler => handler(data))
        } catch (e) {
          console.error('[WebSocket] Parse error:', e)
        }
      }

      this.ws.onclose = () => {
        this.connected.value = false
        wsConnected.value = false
        this.ws = null
        if (!this.intentionalDisconnect && this.refCount > 0) {
          this.scheduleReconnect(5000)
        }
      }

      this.ws.onerror = () => {
        if (this.refCount > 0) {
          this.scheduleReconnect(5000)
        }
      }
    } catch {
      this.scheduleReconnect(5000)
    }
  }

  private scheduleReconnect(delay: number | null) {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
    }
    if (delay !== null) {
      this.reconnectTimer = window.setTimeout(() => this.connect(), delay)
    }
  }

  subscribe(handler: (data: any) => void) {
    this.messageHandlers.push(handler)
    this.refCount++
    return () => {
      const index = this.messageHandlers.indexOf(handler)
      if (index > -1) this.messageHandlers.splice(index, 1)
      this.refCount = Math.max(0, this.refCount - 1)
      if (this.refCount === 0) {
        this.disconnect()
      }
    }
  }

  send(message: string) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(message)
    }
  }

  disconnect() {
    this.refCount = 0
    this.intentionalDisconnect = true
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    this.ws?.close()
    this.ws = null
  }
}

export const wsService = new WebSocketService()
export const wsConnected = ref(false)

export function useWebSocket(onMessage: (data: any) => void) {
  const handler = (data: any) => {
    onMessage(data)
    wsConnected.value = true
  }

  const unsubscribe = wsService.subscribe(handler)

  onUnmounted(() => {
    unsubscribe()
  })

  return {
    connect: () => wsService.connect(),
    disconnect: () => {
      unsubscribe()
    }
  }
}
