import { ref, onUnmounted } from 'vue'

const WS_URL = import.meta.env.VITE_WS_URL || 'ws://localhost:8085/ws/dashboard'

class WebSocketService {
  private ws: WebSocket | null = null
  private reconnectTimer: number | null = null
  private messageHandlers: ((data: any) => void)[] = []
  private intentionalDisconnect = false
  private connected = ref(false)

  connect() {
    if (this.ws?.readyState === WebSocket.OPEN) return

    try {
      this.intentionalDisconnect = false
      this.ws = new WebSocket(WS_URL)

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
        if (!this.intentionalDisconnect) {
          this.scheduleReconnect(5000)
        }
      }

      this.ws.onerror = () => {
        this.scheduleReconnect(5000)
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
      wsConnected.value = false
      wsService.disconnect()
    }
  }
}