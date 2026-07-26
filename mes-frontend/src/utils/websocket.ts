import { ref, onUnmounted } from 'vue'

const WS_URL = import.meta.env.VITE_WS_URL || 'ws://localhost:8085/ws/dashboard'

class WebSocketService {
  private ws: WebSocket | null = null
  private reconnectTimer: number | null = null
  private messageHandlers: ((data: any) => void)[] = []
  private connected = ref(false)

  connect() {
    if (this.ws?.readyState === WebSocket.OPEN) return

    try {
      this.ws = new WebSocket(WS_URL)

      this.ws.onopen = () => {
        console.log('[WebSocket] Connected')
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
        console.log('[WebSocket] Disconnected')
        this.connected.value = false
        wsConnected.value = false
        this.scheduleReconnect(5000)
      }

      this.ws.onerror = (error) => {
        console.error('[WebSocket] Error:', error)
      }
    } catch (e) {
      console.error('[WebSocket] Connection failed:', e)
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
    return () => {
      const index = this.messageHandlers.indexOf(handler)
      if (index > -1) this.messageHandlers.splice(index, 1)
    }
  }

  send(message: string) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(message)
    }
  }

  disconnect() {
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
      wsConnected.value = false
      wsService.disconnect()
    }
  }
}