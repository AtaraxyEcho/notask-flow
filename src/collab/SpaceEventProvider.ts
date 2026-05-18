import type { SpaceRealtimeEvent } from '@/types/app'

interface SpaceEventProviderOptions {
  getTicket: () => Promise<string>
  onEvent: (event: SpaceRealtimeEvent) => void
  wsUrl: string
}

const INITIAL_RECONNECT_DELAY_MS = 1500
const MAX_RECONNECT_DELAY_MS = 10000

export class SpaceEventProvider {
  private readonly getTicket: () => Promise<string>

  private readonly onEvent: (event: SpaceRealtimeEvent) => void

  private readonly wsUrl: string

  private destroyed = false

  private reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS

  private reconnectTimer: number | null = null

  private socket: WebSocket | null = null

  public constructor(options: SpaceEventProviderOptions) {
    this.getTicket = options.getTicket
    this.onEvent = options.onEvent
    this.wsUrl = options.wsUrl
  }

  public connect() {
    if (this.destroyed || this.socket) {
      return
    }

    const socket = new WebSocket(this.wsUrl)
    this.socket = socket
    socket.addEventListener('open', this.handleOpen)
    socket.addEventListener('message', this.handleMessage)
    socket.addEventListener('close', this.handleClose)
    socket.addEventListener('error', this.handleError)
  }

  public destroy() {
    this.destroyed = true
    this.clearReconnectTimer()
    if (!this.socket) {
      return
    }

    this.socket.removeEventListener('open', this.handleOpen)
    this.socket.removeEventListener('message', this.handleMessage)
    this.socket.removeEventListener('close', this.handleClose)
    this.socket.removeEventListener('error', this.handleError)
    this.socket.close()
    this.socket = null
  }

  private readonly handleOpen = async () => {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
      return
    }

    try {
      const ticket = await this.getTicket()
      if (!ticket || !this.socket || this.socket.readyState !== WebSocket.OPEN) {
        return
      }

      this.socket.send(JSON.stringify({ type: 'space_auth', ticket }))
      this.reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS
    } catch {
      this.socket?.close()
    }
  }

  private readonly handleMessage = (event: MessageEvent<string>) => {
    if (typeof event.data !== 'string') {
      return
    }

    try {
      const payload = JSON.parse(event.data) as { event?: SpaceRealtimeEvent; type?: string }
      if (payload.type === 'space_event' && payload.event) {
        this.onEvent(payload.event)
      }
    } catch {
      return
    }
  }

  private readonly handleClose = () => {
    this.socket = null
    if (this.destroyed) {
      return
    }
    this.scheduleReconnect()
  }

  private readonly handleError = () => {
    this.socket?.close()
  }

  private scheduleReconnect() {
    if (this.destroyed || this.reconnectTimer) {
      return
    }

    this.reconnectTimer = window.setTimeout(() => {
      this.reconnectTimer = null
      this.connect()
    }, this.reconnectDelayMs)
    this.reconnectDelayMs = Math.min(this.reconnectDelayMs * 2, MAX_RECONNECT_DELAY_MS)
  }

  private clearReconnectTimer() {
    if (!this.reconnectTimer) {
      return
    }
    window.clearTimeout(this.reconnectTimer)
    this.reconnectTimer = null
  }
}
