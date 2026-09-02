import Collaboration from '@tiptap/extension-collaboration'
import CollaborationCaret from '@tiptap/extension-collaboration-caret'
import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'
import Placeholder from '@tiptap/extension-placeholder'
import TaskItem from '@tiptap/extension-task-item'
import TaskList from '@tiptap/extension-task-list'
import { Editor, Extension, Mark, Node, generateJSON, getSchema, mergeAttributes } from '@tiptap/core'
import StarterKit from '@tiptap/starter-kit'
import * as decoding from 'lib0/decoding'
import * as encoding from 'lib0/encoding'
import { prosemirrorJSONToYXmlFragment } from 'y-prosemirror'
import { Awareness } from 'y-protocols/awareness'
import * as awarenessProtocol from 'y-protocols/awareness'
import * as syncProtocol from 'y-protocols/sync'
import * as Y from 'yjs'

type NativeBridge = {
  onAwarenessChanged?: (payload: string) => void
  onCollabStatus?: (status: string, message: string) => void
  onContentChanged?: (text: string, html: string) => void
  onOpenExternalFile?: (name: string, url: string, mimeType: string, managedFileId: string) => void
  onPlayAudio?: (name: string, url: string, managedFileId: string) => void
  onReady?: () => void
  requestTicket?: (requestId: string) => void
}

type AndroidCollabConfig = {
  baseUrl?: string
  canEdit?: boolean
  collabWsUrl?: string
  initialHtml?: string
  noteId?: number
  placeholder?: string
  spaceId?: number
  user?: {
    avatarUrl?: string
    color?: string
    colorLight?: string
    name?: string
    userId?: number
  }
}

type TicketRequest = {
  reject: (error: Error) => void
  resolve: (ticket: string) => void
  timer: number
}

const MESSAGE_SYNC = 0
const MESSAGE_AWARENESS = 1
const FIRST_LINE_INDENT_VALUE = '2em'
const TICKET_TIMEOUT_MS = 12000
const RECONNECT_DELAY_MS = 1800
const BOOTSTRAP_FALLBACK_DELAY_MS = 2500
const DOCUMENT_STATE_FLUSH_DELAY_MS = 80
const DEFAULT_COLLAB_WS_PORT = '8081'
const DEFAULT_COLLAB_WS_PATH = '/ws'
const LOOPBACK_HOSTS = new Set(['localhost', '127.0.0.1', '::1', '[::1]'])

const nativeBridge = () => window.NativeCollab as NativeBridge | undefined
const pendingTickets = new Map<string, TicketRequest>()
let ticketSequence = 0
let editor: Editor | null = null
let provider: AndroidNoteCollabProvider | null = null
let ydoc: Y.Doc | null = null
let bootstrapped = false
let bootstrapFallbackTimer: number | null = null
let currentConfig: AndroidCollabConfig | null = null
let globalDiagnosticsInstalled = false

declare global {
  interface Window {
    NativeCollab?: NativeBridge
    NotaskEditor?: Record<string, unknown>
  }
}

function installGlobalDiagnostics() {
  if (globalDiagnosticsInstalled) {
    return
  }
  globalDiagnosticsInstalled = true
  window.addEventListener('error', (event) => {
    const stack = event.error instanceof Error ? event.error.stack : ''
    console.error(
      `[NotaskAndroidCollab] global error message=${event.message} source=${event.filename}:${event.lineno}:${event.colno} stack=${stack}`,
    )
  })
  window.addEventListener('unhandledrejection', (event) => {
    const reason = event.reason
    console.error(
      `[NotaskAndroidCollab] unhandled rejection message=${reason instanceof Error ? reason.message : String(reason)} stack=${
        reason instanceof Error ? reason.stack : ''
      }`,
    )
  })
}

const managedDataAttributes = () => ({
  managedFileId: {
    default: null,
    parseHTML: (element: HTMLElement) => element.getAttribute('data-managed-file-id'),
    renderHTML: (attributes: Record<string, string | null>) =>
      attributes.managedFileId ? { 'data-managed-file-id': attributes.managedFileId } : {},
  },
  attachmentId: {
    default: null,
    parseHTML: (element: HTMLElement) => element.getAttribute('data-attachment-id'),
    renderHTML: (attributes: Record<string, string | null>) =>
      attributes.attachmentId ? { 'data-attachment-id': attributes.attachmentId } : {},
  },
})

const TextStyleMark = Mark.create({
  name: 'textStyle',
  priority: 101,
  addAttributes() {
    return {
      color: {
        default: null,
        parseHTML: (element: HTMLElement) => element.style.color || null,
        renderHTML: (attributes: Record<string, string | null>) =>
          attributes.color ? { style: `color: ${attributes.color};` } : {},
      },
      fontFamily: {
        default: null,
        parseHTML: (element: HTMLElement) => element.style.fontFamily || null,
        renderHTML: (attributes: Record<string, string | null>) =>
          attributes.fontFamily ? { style: `font-family: ${attributes.fontFamily};` } : {},
      },
      fontSize: {
        default: null,
        parseHTML: (element: HTMLElement) => element.style.fontSize || null,
        renderHTML: (attributes: Record<string, string | null>) =>
          attributes.fontSize ? { style: `font-size: ${attributes.fontSize};` } : {},
      },
    }
  },
  parseHTML() {
    return [
      {
        tag: 'span',
        getAttrs: (element) => {
          if (!(element instanceof HTMLElement)) {
            return false
          }
          return element.style.color || element.style.fontFamily || element.style.fontSize ? null : false
        },
      },
    ]
  },
  renderHTML({ HTMLAttributes }) {
    return ['span', mergeAttributes(HTMLAttributes), 0]
  },
})

const TextBlockAlign = Extension.create({
  name: 'textBlockAlign',
  addGlobalAttributes() {
    return [
      {
        types: ['paragraph', 'heading'],
        attributes: {
          textAlign: {
            default: null,
            parseHTML: (element: HTMLElement) =>
              element.style.textAlign?.trim() || element.getAttribute('data-text-align') || null,
            renderHTML: (attributes: Record<string, string | null>) =>
              attributes.textAlign && attributes.textAlign !== 'left'
                ? {
                    'data-text-align': attributes.textAlign,
                    style: `text-align: ${attributes.textAlign};`,
                  }
                : {},
          },
        },
      },
    ]
  },
})

const FirstLineIndent = Extension.create({
  name: 'firstLineIndent',
  addGlobalAttributes() {
    return [
      {
        types: ['paragraph'],
        attributes: {
          firstLineIndent: {
            default: null,
            parseHTML: (element: HTMLElement) => {
              const inlineIndent = element.style.textIndent?.trim()
              if (inlineIndent) {
                return inlineIndent
              }
              return element.getAttribute('data-first-line-indent') === 'true' ? FIRST_LINE_INDENT_VALUE : null
            },
            renderHTML: (attributes: Record<string, string | null>) =>
              attributes.firstLineIndent
                ? {
                    'data-first-line-indent': 'true',
                    style: `text-indent: ${attributes.firstLineIndent};`,
                  }
                : {},
          },
        },
      },
    ]
  },
})

const TabIndent = Extension.create({
  name: 'tabIndent',
})

const ManagedLink = Link.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      ...managedDataAttributes(),
      mimeType: {
        default: null,
        parseHTML: (element: HTMLElement) => element.getAttribute('data-mime'),
        renderHTML: (attributes: Record<string, string | null>) =>
          attributes.mimeType ? { 'data-mime': attributes.mimeType } : {},
      },
      name: {
        default: null,
        parseHTML: (element: HTMLElement) => element.getAttribute('data-name'),
        renderHTML: (attributes: Record<string, string | null>) =>
          attributes.name ? { 'data-name': attributes.name } : {},
      },
      type: {
        default: null,
        parseHTML: (element: HTMLElement) => element.getAttribute('data-type'),
        renderHTML: (attributes: Record<string, string | null>) =>
          attributes.type ? { 'data-type': attributes.type } : {},
      },
    }
  },
})

const ManagedImage = Image.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      ...managedDataAttributes(),
    }
  },
})

const audioExtensions = new Set(['aac', 'flac', 'm4a', 'mp3', 'oga', 'ogg', 'opus', 'wav', 'weba', 'webm'])

const extensionFromValue = (value: string) => {
  const cleanValue = value.split('?')[0]?.split('#')[0] || ''
  const chunks = cleanValue.toLowerCase().split('.')
  return chunks.length > 1 ? chunks.pop() || '' : ''
}

const elementFileName = (element: HTMLElement) =>
  element.getAttribute('data-name') ||
  element.querySelector('.notask-audio-name, .file-main strong')?.textContent?.trim() ||
  element.textContent?.trim() ||
  '附件'

const isAudioElement = (element: HTMLElement) => {
  const dataType = element.getAttribute('data-type')?.toLowerCase()
  const mimeType = element.getAttribute('data-mime')?.toLowerCase() || ''
  const name = elementFileName(element)
  const href =
    element.getAttribute('href') ||
    element.getAttribute('data-url') ||
    element.getAttribute('data-preview-href') ||
    element.querySelector('a')?.getAttribute('href') ||
    ''
  return dataType === 'audio' || dataType === 'audio-file-card' || mimeType.startsWith('audio/') || audioExtensions.has(extensionFromValue(name)) || audioExtensions.has(extensionFromValue(href))
}

const AudioFileCard = Node.create({
  name: 'audioFileCard',
  priority: 1000,
  group: 'block',
  atom: true,
  selectable: true,
  addAttributes() {
    return {
      ...managedDataAttributes(),
      mimeType: {
        default: null,
        parseHTML: (element: HTMLElement) => element.getAttribute('data-mime'),
        renderHTML: (attributes: Record<string, string | null>) =>
          attributes.mimeType ? { 'data-mime': attributes.mimeType } : {},
      },
      name: {
        default: '音频文件',
        parseHTML: (element: HTMLElement) => elementFileName(element) || '音频文件',
        renderHTML: (attributes: Record<string, string | null>) =>
          attributes.name ? { 'data-name': attributes.name } : {},
      },
      previewHref: {
        default: null,
        parseHTML: (element: HTMLElement) =>
          element.getAttribute('data-preview-href') || element.getAttribute('href') || null,
        renderHTML: (attributes: Record<string, string | null>) =>
          attributes.previewHref ? { 'data-preview-href': attributes.previewHref } : {},
      },
      source: {
        default: null,
        parseHTML: (element: HTMLElement) =>
          element.getAttribute('data-url') || element.querySelector('audio, source')?.getAttribute('src') || null,
        renderHTML: (attributes: Record<string, string | null>) =>
          attributes.source ? { 'data-url': attributes.source } : {},
      },
    }
  },
  parseHTML() {
    return [
      { tag: 'div[data-type="audio-file-card"]' },
      { tag: 'div.notask-audio-card[data-managed-file-id]' },
      {
        tag: 'section.file-card[data-managed-file-id]',
        getAttrs: (element) => {
          if (!(element instanceof HTMLElement) || !isAudioElement(element)) {
            return false
          }
          return audioAttributesFromElement(element)
        },
      },
      {
        tag: 'a[data-managed-file-id]',
        getAttrs: (element) => {
          if (!(element instanceof HTMLElement) || !isAudioElement(element)) {
            return false
          }
          return audioAttributesFromElement(element)
        },
      },
    ]
  },
  renderHTML({ node, HTMLAttributes }) {
    const name = String(node.attrs.name || '音频文件')
    const mimeType = String(node.attrs.mimeType || '')
    const typeLabel = mimeType.split('/')[1]?.split(';')[0]?.toUpperCase() || '音频'
    return [
      'div',
      mergeAttributes(HTMLAttributes, {
        class: 'notask-audio-card file-card audio-card',
        contenteditable: 'false',
        'data-type': 'audio-file-card',
      }),
      [
        'button',
        {
          'aria-label': '播放音频',
          class: 'notask-audio-play-button media-play',
          contenteditable: 'false',
          'data-audio-action': 'toggle',
          type: 'button',
        },
        ['span', { class: 'play-shape', 'aria-hidden': 'true' }],
      ],
      [
        'span',
        { class: 'notask-audio-main file-main' },
        ['strong', { class: 'notask-audio-name' }, name],
        ['span', { class: 'notask-audio-type' }, `${typeLabel} 音频`],
      ],
      ['span', { class: 'notask-audio-wave wave', 'aria-hidden': 'true' }, ['i'], ['i'], ['i'], ['i'], ['i']],
    ]
  },
})

const FileCard = Node.create({
  name: 'fileCard',
  priority: 900,
  group: 'block',
  atom: true,
  selectable: true,
  addAttributes() {
    return {
      ...managedDataAttributes(),
      fileSize: {
        default: null,
        parseHTML: (element: HTMLElement) => element.getAttribute('data-size'),
        renderHTML: (attributes: Record<string, string | null>) =>
          attributes.fileSize ? { 'data-size': attributes.fileSize } : {},
      },
      mimeType: {
        default: null,
        parseHTML: (element: HTMLElement) => element.getAttribute('data-mime'),
        renderHTML: (attributes: Record<string, string | null>) =>
          attributes.mimeType ? { 'data-mime': attributes.mimeType } : {},
      },
      name: {
        default: '附件',
        parseHTML: (element: HTMLElement) => elementFileName(element) || '附件',
        renderHTML: (attributes: Record<string, string | null>) =>
          attributes.name ? { 'data-name': attributes.name } : {},
      },
      source: {
        default: null,
        parseHTML: (element: HTMLElement) => element.getAttribute('data-url') || element.getAttribute('href') || null,
        renderHTML: (attributes: Record<string, string | null>) =>
          attributes.source ? { 'data-url': attributes.source } : {},
      },
    }
  },
  parseHTML() {
    return [
      {
        tag: 'section.file-card[data-managed-file-id]',
        getAttrs: (element) => {
          if (!(element instanceof HTMLElement) || isAudioElement(element)) {
            return false
          }
          return fileAttributesFromElement(element)
        },
      },
      {
        tag: 'a[data-managed-file-id]',
        getAttrs: (element) => {
          if (!(element instanceof HTMLElement) || isAudioElement(element)) {
            return false
          }
          return fileAttributesFromElement(element)
        },
      },
    ]
  },
  renderHTML({ node, HTMLAttributes }) {
    const name = String(node.attrs.name || '附件')
    const mimeType = String(node.attrs.mimeType || '')
    return [
      'section',
      mergeAttributes(HTMLAttributes, {
        class: 'file-card attachment-card',
        contenteditable: 'false',
        'data-type': 'file',
      }),
      ['span', { class: 'file-icon' }, fileIconText(mimeType, name)],
      ['span', { class: 'file-main' }, ['strong', name], ['span', fileKindLabel(mimeType, name)]],
      ['button', { class: 'file-open', contenteditable: 'false', type: 'button' }, '打开'],
    ]
  },
})

const TableCard = Node.create({
  name: 'tableCard',
  group: 'block',
  atom: true,
  selectable: true,
  addAttributes() {
    return {
      html: {
        default: '',
        parseHTML: (element: HTMLElement) => element.innerHTML,
        renderHTML: () => ({}),
      },
    }
  },
  parseHTML() {
    return [{ tag: 'section.table-card' }]
  },
  renderHTML({ node }) {
    return ['section', { class: 'table-card' }, ['div', { class: 'table-scroll' }, ['div', { 'data-table-html': String(node.attrs.html || '') }]]]
  },
})

const renderCollaborationCaret = (user: Record<string, unknown>) => {
  const cursor = document.createElement('span')
  const color = String(user.color || '#3C79D0')
  const name = String(user.name || '协作者')
  cursor.classList.add('collaboration-carets__caret')
  cursor.setAttribute('style', `border-color: ${color}`)

  const label = document.createElement('span')
  label.classList.add('collaboration-carets__label')
  label.setAttribute('style', `background-color: ${color}`)
  label.textContent = name
  cursor.appendChild(label)
  return cursor
}

const renderCollaborationSelection = (user: Record<string, unknown>) => ({
  class: 'collaboration-carets__selection',
  nodeName: 'span',
  style: `background-color: ${String(user.colorLight || 'rgba(60, 121, 208, 0.18)')}`,
})

const readBlobAsArrayBuffer = (blob: Blob) =>
  new Promise<ArrayBuffer>((resolve, reject) => {
    if (typeof blob.arrayBuffer === 'function') {
      blob.arrayBuffer().then(resolve).catch(reject)
      return
    }
    const reader = new FileReader()
    reader.onload = () => {
      if (reader.result instanceof ArrayBuffer) {
        resolve(reader.result)
      } else {
        reject(new Error('Blob reader result is not ArrayBuffer'))
      }
    }
    reader.onerror = () => reject(reader.error || new Error('Blob reader failed'))
    reader.readAsArrayBuffer(blob)
  })

const toArrayBuffer = (payload: Uint8Array) =>
  payload.buffer.slice(payload.byteOffset, payload.byteOffset + payload.byteLength)

const toBase64 = (payload: Uint8Array) => {
  const chunkSize = 0x8000
  let binary = ''
  for (let index = 0; index < payload.length; index += chunkSize) {
    binary += String.fromCharCode(...payload.subarray(index, index + chunkSize))
  }
  return btoa(binary)
}

const createEditorExtensions = (placeholder: string, fragment?: Y.XmlFragment, awarenessProvider?: { awareness: Awareness }, user?: Record<string, unknown>) => {
  const extensions = [
    StarterKit.configure({
      heading: { levels: [1, 2, 3] },
      undoRedo: Boolean(fragment) ? false : undefined,
    }),
    FirstLineIndent,
    TextBlockAlign,
    TextStyleMark,
    AudioFileCard,
    ManagedLink.configure({
      openOnClick: false,
      HTMLAttributes: {
        rel: 'noopener noreferrer',
        target: '_blank',
      },
    }),
    ManagedImage.configure({ allowBase64: false }),
    Placeholder.configure({ placeholder }),
    TaskList,
    TaskItem.configure({ nested: true }),
    TabIndent,
  ]
  if (!fragment || !awarenessProvider) {
    return extensions
  }
  return [
    ...extensions,
    Collaboration.configure({ fragment }),
    CollaborationCaret.configure({
      provider: awarenessProvider,
      user: user || {},
      render: renderCollaborationCaret,
      selectionRender: renderCollaborationSelection,
    }),
  ]
}

const audioAttributesFromElement = (element: HTMLElement) => {
  const href = element.getAttribute('href') || element.querySelector('a')?.getAttribute('href') || ''
  return {
    attachmentId: element.getAttribute('data-attachment-id'),
    managedFileId: element.getAttribute('data-managed-file-id'),
    mimeType: element.getAttribute('data-mime'),
    name: elementFileName(element),
    previewHref: element.getAttribute('data-preview-href') || href,
    source: element.getAttribute('data-url') || element.querySelector('audio, source')?.getAttribute('src') || href,
  }
}

const fileAttributesFromElement = (element: HTMLElement) => ({
  attachmentId: element.getAttribute('data-attachment-id'),
  fileSize: element.getAttribute('data-size'),
  managedFileId: element.getAttribute('data-managed-file-id'),
  mimeType: element.getAttribute('data-mime'),
  name: elementFileName(element),
  source: element.getAttribute('data-url') || element.getAttribute('href'),
})

class AndroidNoteCollabProvider {
  public readonly awareness: Awareness

  private readonly doc: Y.Doc

  private readonly getTicket: () => Promise<string>

  private readonly presence: NonNullable<AndroidCollabConfig['user']>

  private readonly wsUrl: string

  private isReady = false

  private socket: WebSocket | null = null

  private reconnectTimer: number | null = null

  private documentStateFlushTimer: number | null = null

  private statusListeners = new Set<(status: string, message: string) => void>()

  private bootstrapListeners = new Set<(content: string) => void>()

  public constructor(options: {
    doc: Y.Doc
    getTicket: () => Promise<string>
    presence: NonNullable<AndroidCollabConfig['user']>
    wsUrl: string
  }) {
    this.doc = options.doc
    this.getTicket = options.getTicket
    this.presence = options.presence
    this.wsUrl = options.wsUrl
    this.awareness = new Awareness(this.doc)
    this.doc.on('update', this.handleDocumentUpdate)
    this.awareness.on('update', this.handleAwarenessUpdate)
  }

  public connect() {
    if (this.socket) {
      return
    }
    this.emitStatus('connecting', '正在连接协作服务')
    const socket = new WebSocket(this.wsUrl)
    socket.binaryType = 'arraybuffer'
    this.socket = socket
    socket.addEventListener('open', this.handleOpen)
    socket.addEventListener('message', this.handleMessage)
    socket.addEventListener('close', this.handleClose)
    socket.addEventListener('error', this.handleError)
  }

  public destroy() {
    if (this.reconnectTimer) {
      window.clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    if (this.documentStateFlushTimer) {
      window.clearTimeout(this.documentStateFlushTimer)
      this.documentStateFlushTimer = null
    }
    this.awareness.setLocalState(null)
    this.doc.off('update', this.handleDocumentUpdate)
    this.awareness.off('update', this.handleAwarenessUpdate)
    this.socket?.close()
    this.socket = null
  }

  public onBootstrap(listener: (content: string) => void) {
    this.bootstrapListeners.add(listener)
  }

  public onStatus(listener: (status: string, message: string) => void) {
    this.statusListeners.add(listener)
  }

  private readonly handleOpen = async () => {
    try {
      this.emitStatus('ticket', '正在获取协作票据')
      const ticket = await this.getTicket()
      this.socket?.send(JSON.stringify({ type: 'auth', ticket }))
      this.emitStatus('connected', '协作票据已发送')
    } catch {
      this.emitStatus('error', '协作票据获取失败')
      this.socket?.close()
    }
  }

  private readonly handleMessage = (event: MessageEvent<ArrayBuffer | Blob | string>) => {
    if (typeof event.data === 'string') {
      this.handleJsonMessage(event.data)
      return
    }
    if (event.data instanceof ArrayBuffer) {
      this.handleBinaryMessageSafely(new Uint8Array(event.data))
      return
    }
    if (ArrayBuffer.isView(event.data)) {
      const binaryView = event.data
      this.handleBinaryMessageSafely(new Uint8Array(binaryView.buffer, binaryView.byteOffset, binaryView.byteLength))
      return
    }
    if (typeof Blob !== 'undefined' && event.data instanceof Blob) {
      readBlobAsArrayBuffer(event.data)
        .then((buffer) => this.handleBinaryMessageSafely(new Uint8Array(buffer)))
        .catch(() => this.emitStatus('error', '协作消息解析失败'))
      return
    }
    this.emitStatus('error', '协作消息格式不支持')
  }

  private readonly handleClose = (event: CloseEvent) => {
    this.socket = null
    this.isReady = false
    this.awareness.setLocalState(null)
    const message = event.code === 1008 ? '协作认证失败' : event.code === 1006 ? '协作服务连接失败' : '协作连接已断开，正在重连'
    this.emitStatus('reconnecting', event.reason || message)
    this.reconnectTimer = window.setTimeout(() => {
      this.reconnectTimer = null
      this.connect()
    }, RECONNECT_DELAY_MS)
  }

  private readonly handleError = () => {
    this.emitStatus('error', '协作服务连接异常')
  }

  private readonly handleDocumentUpdate = (update: Uint8Array, origin: unknown) => {
    if (origin === this || !this.isReady) {
      return
    }
    this.sendDocumentUpdate(update)
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
    encoding.writeVarUint8Array(encoder, awarenessProtocol.encodeAwarenessUpdate(this.awareness, changedClients))
    this.sendBinary(encoding.toUint8Array(encoder))
    emitAwareness(this.awareness)
  }

  private handleJsonMessage(rawMessage: string) {
    const payload = JSON.parse(rawMessage) as { bootstrapContent?: string; type?: string; userId?: number }
    if (payload.type !== 'ready') {
      return
    }
    this.isReady = true
    const localUser = {
      ...this.presence,
      userId: payload.userId || this.presence.userId || 0,
    }
    this.awareness.setLocalStateField('user', localUser)
    this.bootstrapListeners.forEach((listener) => listener(payload.bootstrapContent || ''))
    this.sendSyncStepOne()
    this.sendLocalAwareness()
    this.emitStatus('ready', '协作服务已就绪')
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
        this.emitStatus('synced', '协作内容已同步')
        break
      }
      case MESSAGE_AWARENESS: {
        const update = decoding.readVarUint8Array(decoder)
        awarenessProtocol.applyAwarenessUpdate(this.awareness, update, this)
        emitAwareness(this.awareness)
        break
      }
      default:
        break
    }
  }

  private handleBinaryMessageSafely(message: Uint8Array) {
    try {
      this.handleBinaryMessage(message)
    } catch {
      this.emitStatus('error', '协作消息解析失败')
    }
  }

  private sendBinary(payload: Uint8Array) {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN || !this.isReady) {
      return
    }
    const textPayload = JSON.stringify({ payload: toBase64(payload), type: 'yjs-binary' })
    try {
      this.socket.send(textPayload)
      return
    } catch (error) {
      console.warn('[NotaskAndroidCollab] yjs text frame failed, fallback to binary', error)
    }

    try {
      this.socket.send(toArrayBuffer(payload))
    } catch (error) {
      console.error('[NotaskAndroidCollab] yjs frame send failed', error)
      this.emitStatus('error', '协作内容发送失败')
    }
  }

  private sendDocumentUpdate(update: Uint8Array) {
    const encoder = encoding.createEncoder()
    encoding.writeVarUint(encoder, MESSAGE_SYNC)
    syncProtocol.writeUpdate(encoder, update)
    this.sendBinary(encoding.toUint8Array(encoder))
  }

  public scheduleDocumentStateFlush() {
    if (!this.isReady) {
      return
    }
    if (this.documentStateFlushTimer) {
      window.clearTimeout(this.documentStateFlushTimer)
    }
    this.documentStateFlushTimer = window.setTimeout(() => {
      this.documentStateFlushTimer = null
      if (!this.isReady) {
        return
      }
      this.sendDocumentUpdate(Y.encodeStateAsUpdate(this.doc))
    }, DOCUMENT_STATE_FLUSH_DELAY_MS)
  }

  private sendSyncStepOne() {
    const encoder = encoding.createEncoder()
    encoding.writeVarUint(encoder, MESSAGE_SYNC)
    syncProtocol.writeSyncStep1(encoder, this.doc)
    this.sendBinary(encoding.toUint8Array(encoder))
    this.emitStatus('syncing', '正在同步协作文档')
  }

  private sendLocalAwareness() {
    const localState = this.awareness.getLocalState()
    if (!localState) {
      return
    }
    const encoder = encoding.createEncoder()
    encoding.writeVarUint(encoder, MESSAGE_AWARENESS)
    encoding.writeVarUint8Array(encoder, awarenessProtocol.encodeAwarenessUpdate(this.awareness, [this.doc.clientID]))
    this.sendBinary(encoding.toUint8Array(encoder))
    emitAwareness(this.awareness)
    this.emitStatus('awareness', '协作在线状态已同步')
  }

  private emitStatus(status: string, message: string) {
    this.statusListeners.forEach((listener) => listener(status, message))
    nativeBridge()?.onCollabStatus?.(status, message)
  }
}

function configure(config: AndroidCollabConfig) {
  installGlobalDiagnostics()
  currentConfig = config
  destroyCurrentEditor()

  const editorElement = document.querySelector<HTMLElement>('#editor')
  if (!editorElement) {
    return
  }

  ydoc = new Y.Doc()
  const fragment = ydoc.getXmlFragment('content')
  const presence = resolvePresence(config.user)
  provider = new AndroidNoteCollabProvider({
    doc: ydoc,
    getTicket: requestTicket,
    presence,
    wsUrl: resolveWsUrl(config),
  })
  provider.onBootstrap((html) => {
    if (html.trim()) {
      bootstrapCollaborationContent(html)
    }
  })

  editor = new Editor({
    element: editorElement,
    editable: config.canEdit !== false,
    extensions: createEditorExtensions(config.placeholder || '开始协作编辑', fragment, provider, presence),
    editorProps: {
      attributes: {
        class: 'notask-collab-editor-content',
      },
      handleClick: (_view, _pos, event) => handleEditorClick(event),
    },
    onUpdate: ({ editor: currentEditor }) => {
      nativeBridge()?.onContentChanged?.(currentEditor.getText({ blockSeparator: '\n' }), currentEditor.getHTML())
      provider?.scheduleDocumentStateFlush()
    },
  })

  provider.connect()
  scheduleBootstrapFallback(config.initialHtml || '')
  nativeBridge()?.onReady?.()
}

function bootstrapCollaborationContent(html: string) {
  if (!editor || !ydoc || bootstrapped || !html.trim()) {
    return
  }
  const fragment = ydoc.getXmlFragment('content')
  if (fragment.length > 0) {
    bootstrapped = true
    return
  }
  try {
    const extensions = createEditorExtensions(currentConfig?.placeholder || '开始协作编辑')
    const schema = getSchema(extensions)
    const json = generateJSON(html, extensions)
    prosemirrorJSONToYXmlFragment(schema, json, fragment)
    bootstrapped = true
    nativeBridge()?.onCollabStatus?.('bootstrap', '已载入协作文档正文')
  } catch {
    const fallback = textFallbackHtml(html)
    if (!fallback) {
      return
    }
    const extensions = createEditorExtensions(currentConfig?.placeholder || '开始协作编辑')
    const schema = getSchema(extensions)
    const json = generateJSON(fallback, extensions)
    prosemirrorJSONToYXmlFragment(schema, json, fragment)
    bootstrapped = true
    nativeBridge()?.onCollabStatus?.('bootstrap', '已载入协作文档正文')
  }
}

function scheduleBootstrapFallback(html: string) {
  if (bootstrapFallbackTimer) {
    window.clearTimeout(bootstrapFallbackTimer)
    bootstrapFallbackTimer = null
  }
  if (!html.trim()) {
    return
  }
  bootstrapFallbackTimer = window.setTimeout(() => {
    bootstrapFallbackTimer = null
    if (!editor || !ydoc || bootstrapped) {
      return
    }
    const fragment = ydoc.getXmlFragment('content')
    if (fragment.length > 0 || !editor.isEmpty) {
      bootstrapped = true
      return
    }
    nativeBridge()?.onCollabStatus?.('bootstrap', '协作快照为空，使用本地正文初始化')
    bootstrapCollaborationContent(html)
  }, BOOTSTRAP_FALLBACK_DELAY_MS)
}

function destroyCurrentEditor() {
  if (bootstrapFallbackTimer) {
    window.clearTimeout(bootstrapFallbackTimer)
    bootstrapFallbackTimer = null
  }
  provider?.destroy()
  provider = null
  editor?.destroy()
  editor = null
  ydoc?.destroy()
  ydoc = null
  bootstrapped = false
}

function requestTicket() {
  const requestId = `${Date.now()}-${ticketSequence++}`
  return new Promise<string>((resolve, reject) => {
    const timer = window.setTimeout(() => {
      pendingTickets.delete(requestId)
      reject(new Error('Ticket request timeout'))
    }, TICKET_TIMEOUT_MS)
    pendingTickets.set(requestId, { reject, resolve, timer })
    nativeBridge()?.requestTicket?.(requestId)
  })
}

function receiveTicket(requestId: string, ticket: string, errorMessage?: string) {
  const pending = pendingTickets.get(requestId)
  if (!pending) {
    return
  }
  window.clearTimeout(pending.timer)
  pendingTickets.delete(requestId)
  if (ticket) {
    pending.resolve(ticket)
  } else {
    pending.reject(new Error(errorMessage || 'Ticket request failed'))
  }
}

function isLoopbackHost(hostname: string) {
  return LOOPBACK_HOSTS.has(hostname.toLowerCase())
}

function resolveBaseHost(baseUrl?: string) {
  const trimmedBaseUrl = baseUrl?.trim()
  if (!trimmedBaseUrl) {
    return ''
  }
  try {
    return new URL(trimmedBaseUrl).hostname
  } catch {
    return ''
  }
}

function normalizeExplicitWsUrl(wsUrl: string, baseUrl?: string) {
  try {
    const parsedWsUrl = new URL(wsUrl)
    const baseHost = resolveBaseHost(baseUrl)
    if (isLoopbackHost(parsedWsUrl.hostname) && baseHost && !isLoopbackHost(baseHost)) {
      parsedWsUrl.hostname = baseHost
    }
    return parsedWsUrl.toString()
  } catch {
    return wsUrl
  }
}

function resolveWsUrl(config: AndroidCollabConfig) {
  const explicitUrl = config.collabWsUrl?.trim()
  if (explicitUrl) {
    return normalizeExplicitWsUrl(explicitUrl, config.baseUrl)
  }
  const baseUrl = config.baseUrl?.trim() || 'http://localhost:8080/'
  const parsedUrl = new URL(baseUrl)
  const protocol = parsedUrl.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${parsedUrl.hostname}:${DEFAULT_COLLAB_WS_PORT}${DEFAULT_COLLAB_WS_PATH}`
}

function resolvePresence(user?: AndroidCollabConfig['user']) {
  return {
    avatarUrl: user?.avatarUrl,
    color: user?.color || '#3C79D0',
    colorLight: user?.colorLight || '#D4E4FB',
    name: user?.name || 'Android',
    userId: user?.userId || 0,
  }
}

function emitAwareness(awareness: Awareness) {
  const currentClientId = ydoc?.clientID
  const users = Array.from(awareness.getStates().entries())
    .filter(([clientId]) => clientId !== currentClientId)
    .map(([clientId, state]) => ({
      ...(state.user || {}),
      clientId,
    }))
    .filter((user) => user.userId || user.name)
  nativeBridge()?.onAwarenessChanged?.(JSON.stringify(users))
}

function handleEditorClick(event: MouseEvent) {
  const target = event.target
  if (!(target instanceof HTMLElement)) {
    return false
  }

  const audioButton = target.closest('.notask-audio-play-button, .media-play')
  if (audioButton instanceof HTMLElement) {
    const card = audioButton.closest<HTMLElement>('.notask-audio-card, .audio-card')
    if (!card) {
      return false
    }
    event.preventDefault()
    nativeBridge()?.onPlayAudio?.(
      card.getAttribute('data-name') || card.querySelector('.notask-audio-name, .file-main strong')?.textContent?.trim() || '音频文件',
      card.getAttribute('data-url') || card.getAttribute('data-preview-href') || '',
      card.getAttribute('data-managed-file-id') || '',
    )
    return true
  }

  const openButton = target.closest('.file-open')
  if (openButton instanceof HTMLElement) {
    const card = openButton.closest<HTMLElement>('.file-card')
    if (!card) {
      return false
    }
    event.preventDefault()
    nativeBridge()?.onOpenExternalFile?.(
      card.getAttribute('data-name') || card.querySelector('.file-main strong')?.textContent?.trim() || '附件',
      card.getAttribute('data-url') || card.getAttribute('data-preview-href') || '',
      card.getAttribute('data-mime') || '',
      card.getAttribute('data-managed-file-id') || '',
    )
    return true
  }

  return false
}

function runCommand(command: string, value?: string) {
  const chain = editor?.chain().focus()
  if (!chain || !editor) {
    return
  }
  switch (command) {
    case 'undo':
      chain.undo().run()
      break
    case 'redo':
      chain.redo().run()
      break
    case 'formatBlock':
      setBlock(value || 'p')
      break
    case 'bold':
      chain.toggleBold().run()
      break
    case 'italic':
      chain.toggleItalic().run()
      break
    case 'underline':
      chain.toggleMark('underline').run()
      break
    case 'insertUnorderedList':
      chain.toggleBulletList().run()
      break
    case 'insertOrderedList':
      chain.toggleOrderedList().run()
      break
    case 'justifyLeft':
      applyBlockAttributes({ textAlign: null })
      break
    case 'justifyCenter':
      applyBlockAttributes({ textAlign: 'center' })
      break
    case 'justifyRight':
      applyBlockAttributes({ textAlign: 'right' })
      break
    default:
      break
  }
}

function setBlock(value: string) {
  const chain = editor?.chain().focus()
  if (!chain) {
    return
  }
  if (value === 'h1') {
    chain.setHeading({ level: 1 }).run()
  } else if (value === 'h2') {
    chain.setHeading({ level: 2 }).run()
  } else if (value === 'h3') {
    chain.setHeading({ level: 3 }).run()
  } else if (value === 'h4') {
    chain.setHeading({ level: 4 }).run()
  } else {
    chain.setParagraph().run()
  }
}

function applyFontSize(size: number) {
  editor?.chain().focus().setMark('textStyle', { fontSize: `${Math.max(12, Math.min(28, Number(size) || 16))}px` }).run()
}

function applyFontColor(color: string) {
  editor?.chain().focus().setMark('textStyle', { color: color || '#1d1b20' }).run()
}

function applyFontFamily(fontFamily: string) {
  editor?.chain().focus().setMark('textStyle', { fontFamily: fontFamily || 'system-ui, sans-serif' }).run()
}

function applyLineHeight(lineHeight: number) {
  applyBlockAttributes({ lineHeight: String(Math.max(1, Math.min(2, Number(lineHeight) || 1.5))) })
}

function toggleFirstLineIndent() {
  const hasIndent = editor?.isActive('paragraph', { firstLineIndent: FIRST_LINE_INDENT_VALUE }) === true
  applyBlockAttributes({ firstLineIndent: hasIndent ? null : FIRST_LINE_INDENT_VALUE })
}

function toggleBlockquote() {
  editor?.chain().focus().toggleBlockquote().run()
}

function toggleTodo() {
  editor?.chain().focus().toggleTaskList().run()
}

function alphaList() {
  editor?.chain().focus().toggleOrderedList().updateAttributes('orderedList', { style: 'list-style-type: lower-alpha;' }).run()
}

function insertHtml(html: string) {
  editor?.chain().focus().insertContent(html).run()
}

function insertLink(text: string, url: string) {
  const safeUrl = normalizeHref(url)
  const selection = editor?.state.selection
  if (!editor || !selection) {
    return
  }
  if (!selection.empty) {
    editor.chain().focus().setLink({ href: safeUrl }).run()
  } else {
    editor.chain().focus().insertContent(`<a href="${escapeHtml(safeUrl)}">${escapeHtml(text || safeUrl)}</a>`).run()
  }
}

function quickLink() {
  insertLink('链接', 'https://')
}

function removeLink() {
  editor?.chain().focus().unsetLink().run()
}

function setContent(html: string) {
  editor?.commands.setContent(html || '', { emitUpdate: true })
}

function snapshotContent() {
  return JSON.stringify({
    html: editor?.getHTML() || '',
    text: editor?.getText({ blockSeparator: '\n' }) || '',
  })
}

function focusEditor() {
  editor?.commands.focus()
}

function blurEditor() {
  editor?.commands.blur()
}

function resetAudioPlayback() {
  document.querySelectorAll('.audio-card.playing, .notask-audio-card.is-playing').forEach((element) => {
    element.classList.remove('playing', 'is-playing')
  })
}

function applyBlockAttributes(attributes: Record<string, string | null>) {
  const currentEditor = editor
  if (!currentEditor) {
    return
  }
  const { from, to } = currentEditor.state.selection
  const transaction = currentEditor.state.tr
  currentEditor.state.doc.nodesBetween(from, to, (node, pos) => {
    if (node.type.name !== 'paragraph' && node.type.name !== 'heading') {
      return
    }
    transaction.setNodeMarkup(pos, undefined, {
      ...node.attrs,
      ...attributes,
    })
  })
  currentEditor.view.dispatch(transaction)
}

function textFallbackHtml(html: string) {
  const body = new DOMParser().parseFromString(html, 'text/html').body
  const text = body.textContent?.trim()
  return text ? `<p>${escapeHtml(text)}</p>` : ''
}

function normalizeHref(value: string) {
  const trimmed = String(value || '').trim()
  if (/^(https?:|mailto:|tel:|\/)/i.test(trimmed)) {
    return trimmed
  }
  return `https://${trimmed}`
}

function fileKindLabel(mimeType: string, fileName: string) {
  const extension = extensionFromValue(fileName)
  if (extension === 'doc' || extension === 'docx') {
    return 'Word 文档'
  }
  if (extension === 'xls' || extension === 'xlsx') {
    return 'Excel 表格'
  }
  if (extension === 'ppt' || extension === 'pptx') {
    return 'PowerPoint 演示文稿'
  }
  if (extension === 'pdf') {
    return 'PDF 文档'
  }
  if (/^image\//i.test(mimeType)) {
    return '图片文件'
  }
  if (/^audio\//i.test(mimeType)) {
    return '音频文件'
  }
  return '附件'
}

function fileIconText(mimeType: string, fileName: string) {
  const extension = extensionFromValue(fileName)
  if (extension === 'doc' || extension === 'docx') {
    return 'W'
  }
  if (extension === 'xls' || extension === 'xlsx') {
    return 'X'
  }
  if (extension === 'ppt' || extension === 'pptx') {
    return 'P'
  }
  if (extension === 'pdf') {
    return 'P'
  }
  if (/^image\//i.test(mimeType)) {
    return 'I'
  }
  if (/^audio\//i.test(mimeType)) {
    return 'A'
  }
  return 'F'
}

function escapeHtml(value: string) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function injectStyles() {
  const style = document.createElement('style')
  style.textContent = `
    html, body { height: 100%; margin: 0; background: transparent; color: #1d1b20; font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; }
    #editor { min-height: 100%; box-sizing: border-box; padding: 18px 0 80px; outline: none; font-size: 16px; line-height: 1.55; }
    .ProseMirror { min-height: 100%; outline: none; }
    .ProseMirror p.is-editor-empty:first-child::before { color: #9b928d; content: attr(data-placeholder); float: left; height: 0; pointer-events: none; }
    p, blockquote, ul, ol, pre, .file-card, .notask-audio-card { margin: 0 0 14px; }
    blockquote { padding: 12px 14px; border-left: 3px solid #6750a4; border-radius: 10px; background: rgba(103, 80, 164, 0.08); color: #4f465b; }
    img { max-width: 100%; height: auto; border-radius: 12px; display: block; }
    .file-card, .notask-audio-card { display: flex; align-items: center; gap: 10px; width: 100%; box-sizing: border-box; overflow: hidden; padding: 10px 12px; border-radius: 12px; background: rgba(121, 116, 126, 0.10); }
    .file-icon { width: 36px; height: 36px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-weight: 700; background: rgba(103, 80, 164, 0.14); color: #6750a4; }
    .file-main, .notask-audio-main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 4px; }
    .file-main strong, .notask-audio-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; line-height: 1.35; }
    .file-main span, .notask-audio-type { font-size: 12px; color: #79747e; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .file-open { border: 0; border-radius: 999px; padding: 7px 11px; background: rgba(103, 80, 164, 0.14); color: #6750a4; font-weight: 700; }
    .media-play, .notask-audio-play-button { width: 38px; height: 38px; border: 0; border-radius: 50%; background: linear-gradient(135deg, #6750a4, #8f7bc8); color: #fff; display: flex; align-items: center; justify-content: center; }
    .play-shape { width: 0; height: 0; margin-left: 3px; border-top: 8px solid transparent; border-bottom: 8px solid transparent; border-left: 12px solid #fff; }
    .wave, .notask-audio-wave { display: flex; align-items: center; gap: 3px; height: 18px; }
    .wave i, .notask-audio-wave i { width: 4px; border-radius: 4px; background: #8f7bc8; }
    .wave i:nth-child(1) { height: 8px; } .wave i:nth-child(2) { height: 14px; } .wave i:nth-child(3) { height: 18px; } .wave i:nth-child(4) { height: 12px; } .wave i:nth-child(5) { height: 16px; }
    ul[data-type="taskList"] { list-style: none; padding: 0; }
    li[data-type="taskItem"] { display: flex; gap: 8px; align-items: flex-start; }
    li[data-type="taskItem"] > label { flex: 0 0 auto; }
    li[data-type="taskItem"] > div { flex: 1; min-width: 0; }
    .collaboration-carets__caret { border-left: 2px solid #3c79d0; border-right: 2px solid #3c79d0; margin-left: -1px; margin-right: -1px; position: relative; word-break: normal; pointer-events: none; }
    .collaboration-carets__label { border-radius: 999px; color: white; font-size: 11px; font-weight: 700; left: -1px; line-height: 1; padding: 4px 7px; position: absolute; top: -1.4em; white-space: nowrap; }
  `
  document.head.appendChild(style)
}

injectStyles()

window.NotaskEditor = {
  alphaList,
  applyFontColor,
  applyFontFamily,
  applyFontSize,
  applyLineHeight,
  blurEditor,
  command: runCommand,
  configure,
  destroy: destroyCurrentEditor,
  focusEditor,
  insertHtml,
  insertLink,
  quickLink,
  receiveTicket,
  removeLink,
  resetAudioPlayback,
  setContent,
  snapshotContent,
  toggleBlockquote,
  toggleFirstLineIndent,
  toggleTodo,
}
