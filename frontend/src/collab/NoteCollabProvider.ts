import * as decoding from 'lib0/decoding'
import * as encoding from 'lib0/encoding'
import { Awareness } from 'y-protocols/awareness'
import * as awarenessProtocol from 'y-protocols/awareness'
import * as syncProtocol from 'y-protocols/sync'
import * as Y from 'yjs'
import { translate } from '@/i18n'

export type CollabConnectionStatus =
  | 'idle'
  | 'connecting'
  | 'connected'
  | 'synced'
  | 'reconnecting'
  | 'disconnected'
  | 'error'

export interface CollabPresenceUser {
  userId: number
  name: string
  color: string
  colorLight: string
  avatarUrl?: string
}

interface CollabStatusListenerPayload {
  message: string
  status: CollabConnectionStatus
}

interface CollabProviderOptions {
  doc: Y.Doc
  getTicket: () => Promise<string>
  presence: CollabPresenceUser
  wsUrl: string
}

const MESSAGE_SYNC = 0
const MESSAGE_AWARENESS = 1
const INITIAL_RECONNECT_DELAY_MS = 1500
const MAX_RECONNECT_DELAY_MS = 10000

export class NoteCollabProvider {
  public readonly awareness: Awareness

  private readonly doc: Y.Doc

  private readonly getTicket: () => Promise<string>

  private readonly presence: CollabPresenceUser

  private readonly wsUrl: string

  private readonly statusListeners = new Set<(payload: CollabStatusListenerPayload) => void>()

  private readonly syncedListeners = new Set<() => void>()

  private readonly bootstrapListeners = new Set<(content: string) => void>()

  private reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS

  private reconnectTimer: number | null = null

  private destroyed = false

  private socket: WebSocket | null = null

  private hasSynced = false

  private isReady = false

  public constructor(options: CollabProviderOptions) {
    this.doc = options.doc
    this.getTicket = options.getTicket
    this.presence = options.presence
    this.wsUrl = options.wsUrl
    this.awareness = new Awareness(this.doc)

    this.doc.on('update', this.handleDocumentUpdate)
    this.awareness.on('update', this.handleAwarenessUpdate)
  }

  public connect() {
    if (this.destroyed || this.socket) {
      return
    }

    this.hasSynced = false
    this.isReady = false
    this.emitStatus('connecting', translate('collab.connecting'))
    console.info('[NotaskCollab] ws connecting', { wsUrl: this.wsUrl })
    let socket: WebSocket
    try {
      socket = new WebSocket(this.wsUrl)
    } catch {
      this.emitStatus('error', `协作 WebSocket 地址不可用：${this.wsUrl}`)
      this.scheduleReconnect()
      return
    }
    socket.binaryType = 'arraybuffer'
    this.socket = socket

    socket.addEventListener('open', this.handleOpen)
    socket.addEventListener('message', this.handleMessage)
    socket.addEventListener('close', this.handleClose)
    socket.addEventListener('error', this.handleError)
  }

  public destroy() {
    this.destroyed = true
    this.clearReconnectTimer()
    this.isReady = false
    this.awareness.setLocalState(null)
    this.doc.off('update', this.handleDocumentUpdate)
    this.awareness.off('update', this.handleAwarenessUpdate)

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

  public onStatusChange(listener: (payload: CollabStatusListenerPayload) => void) {
    this.statusListeners.add(listener)
    return () => {
      this.statusListeners.delete(listener)
    }
  }

  public onSynced(listener: () => void) {
    this.syncedListeners.add(listener)
    return () => {
      this.syncedListeners.delete(listener)
    }
  }

  public onBootstrap(listener: (content: string) => void) {
    this.bootstrapListeners.add(listener)
    return () => {
      this.bootstrapListeners.delete(listener)
    }
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

      this.socket.send(
        JSON.stringify({
          type: 'auth',
          ticket,
        }),
      )
      console.info('[NotaskCollab] ws auth sent')
      this.emitStatus('connected', translate('collab.channelConnected'))
    } catch (error) {
      console.error('[NotaskCollab] ticket request failed', error)
      this.emitStatus('reconnecting', '协作票据获取失败，正在重试')
      this.socket?.close()
    }
  }

  private readonly handleMessage = (event: MessageEvent<ArrayBuffer | string>) => {
    if (typeof event.data === 'string') {
      this.handleJsonMessage(event.data)
      return
    }

    try {
      this.handleBinaryMessage(new Uint8Array(event.data))
    } catch (error) {
      console.error('[NotaskCollab] ws binary message failed', error)
      return
    }
  }

  private readonly handleClose = (event: CloseEvent) => {
    this.socket = null
    this.hasSynced = false
    this.isReady = false
    this.awareness.setLocalState(null)

    if (this.destroyed) {
      this.emitStatus('disconnected', translate('collab.connectionClosed'))
      return
    }

    console.warn('[NotaskCollab] ws closed', { code: event.code, reason: event.reason, wsUrl: this.wsUrl })
    this.emitStatus('reconnecting', this.resolveCloseMessage(event))
    this.scheduleReconnect()
  }

  private readonly handleError = () => {
    console.error('[NotaskCollab] ws error', { wsUrl: this.wsUrl })
    this.emitStatus('reconnecting', `协作服务连接异常：${this.wsUrl}`)
  }

  private readonly handleDocumentUpdate = (update: Uint8Array, origin: unknown) => {
    if (origin === this || !this.isReady) {
      return
    }

    const encoder = encoding.createEncoder()
    encoding.writeVarUint(encoder, MESSAGE_SYNC)
    syncProtocol.writeUpdate(encoder, update)
    this.sendBinary(encoding.toUint8Array(encoder))
  }

  private readonly handleAwarenessUpdate = (
    payload: { added: number[]; updated: number[]; removed: number[] },
    origin: unknown,
  ) => {
    if (origin === this || !this.isReady) {
      return
    }

    const changedClients = [...payload.added, ...payload.updated, ...payload.removed]
    if (!changedClients.length) {
      return
    }

    const encoder = encoding.createEncoder()
    encoding.writeVarUint(encoder, MESSAGE_AWARENESS)
    encoding.writeVarUint8Array(
      encoder,
      awarenessProtocol.encodeAwarenessUpdate(this.awareness, changedClients),
    )
    this.sendBinary(encoding.toUint8Array(encoder))
  }

  private handleJsonMessage(rawMessage: string) {
    try {
      const payload = JSON.parse(rawMessage) as {
        bootstrapContent?: string
        noteId?: number
        roomKey?: string
        spaceId?: number
        type?: string
      }
      if (payload.type === 'ready') {
        this.isReady = true
        console.info('[NotaskCollab] ws ready', {
          hasBootstrap: Boolean(payload.bootstrapContent),
          noteId: payload.noteId,
          roomKey: payload.roomKey,
          spaceId: payload.spaceId,
        })
        this.awareness.setLocalStateField('user', this.presence)
        if (payload.bootstrapContent) {
          this.bootstrapListeners.forEach((listener) => listener(payload.bootstrapContent || ''))
        }
        this.sendSyncStepOne()
        this.sendLocalAwareness()
      }
    } catch {
      return
    }
  }

  private handleBinaryMessage(message: Uint8Array) {
    const decoder = decoding.createDecoder(message)
    const messageType = decoding.readVarUint(decoder)

    switch (messageType) {
      case MESSAGE_SYNC: {
        const encoder = encoding.createEncoder()
        encoding.writeVarUint(encoder, MESSAGE_SYNC)
        syncProtocol.readSyncMessage(decoder, encoder, this.doc, this)
        const reply = encoding.toUint8Array(encoder)
        if (reply.length > 1) {
          this.sendBinary(reply)
        }

        if (!this.hasSynced) {
          this.hasSynced = true
          this.reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS
          console.info('[NotaskCollab] ws synced')
          this.emitStatus('synced', translate('collab.synced'))
          this.syncedListeners.forEach((listener) => listener())
        }
        break
      }
      case MESSAGE_AWARENESS: {
        const update = decoding.readVarUint8Array(decoder)
        awarenessProtocol.applyAwarenessUpdate(this.awareness, update, this)
        break
      }
      default:
        break
    }
  }

  private sendBinary(payload: Uint8Array) {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN || !this.isReady) {
      return
    }

    this.socket.send(payload)
  }

  private sendSyncStepOne() {
    const encoder = encoding.createEncoder()
    encoding.writeVarUint(encoder, MESSAGE_SYNC)
    syncProtocol.writeSyncStep1(encoder, this.doc)
    this.sendBinary(encoding.toUint8Array(encoder))
    console.info('[NotaskCollab] ws sync step1 sent', { clientId: this.doc.clientID })
  }

  private sendLocalAwareness() {
    if (!this.awareness.getLocalState()) {
      return
    }
    const encoder = encoding.createEncoder()
    encoding.writeVarUint(encoder, MESSAGE_AWARENESS)
    encoding.writeVarUint8Array(
      encoder,
      awarenessProtocol.encodeAwarenessUpdate(this.awareness, [this.doc.clientID]),
    )
    this.sendBinary(encoding.toUint8Array(encoder))
    console.info('[NotaskCollab] ws awareness sent', { clientId: this.doc.clientID })
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

  private emitStatus(status: CollabConnectionStatus, message: string) {
    this.statusListeners.forEach((listener) => listener({ status, message }))
  }

  private resolveCloseMessage(event: CloseEvent) {
    if (event.code === 1008) {
      return event.reason || '协作认证失败，请重新打开文档'
    }
    if (event.code === 1011) {
      return event.reason || '协作服务处理失败，正在重试'
    }
    if (event.code === 1006) {
      return `无法连接协作服务：${this.wsUrl}`
    }
    if (event.reason) {
      return `${translate('collab.reconnecting')}：${event.reason}`
    }
    return translate('collab.reconnecting')
  }
}

export function buildPresenceColor(userId: number) {
  const palette = [
    ['#C96B4A', '#F9D5C9'],
    ['#2F8F83', '#CDEFE8'],
    ['#7A58C1', '#E0D7F7'],
    ['#B86B2B', '#F7DFC8'],
    ['#3C79D0', '#D4E4FB'],
    ['#BE4D72', '#F8D2DE'],
  ] as const

  const [color, colorLight] = palette[Math.abs(userId) % palette.length]
  return { color, colorLight }
}
