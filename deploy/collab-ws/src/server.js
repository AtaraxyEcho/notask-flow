const http = require('http')
const { WebSocketServer, WebSocket } = require('ws')
const Y = require('yjs')
const syncProtocol = require('y-protocols/sync')
const awarenessProtocol = require('y-protocols/awareness')
const encoding = require('lib0/encoding')
const decoding = require('lib0/decoding')

const MESSAGE_SYNC = 0
const MESSAGE_AWARENESS = 1
const ROOM_RETENTION_MS = 5 * 60 * 1000
const BOOTSTRAP_LOCK_TIMEOUT_MS = 5000

const port = Number(process.env.PORT || 8081)
const host = process.env.HOST || '0.0.0.0'
const wsPath = process.env.WS_PATH || '/ws'
const apiBaseUrl = (process.env.API_BASE_URL || 'http://app:8080').replace(/\/$/, '')
const ticketVerifyPath = process.env.TICKET_VERIFY_PATH || '/api/v1/internal/collab/tickets/consume'
const spaceEventTicketVerifyPath =
  process.env.SPACE_EVENT_TICKET_VERIFY_PATH || '/api/v1/internal/collab/space-events/tickets/consume'
const internalToken = process.env.INTERNAL_TOKEN || ''
const authTimeoutMs = 15000
const debugLogsEnabled = process.env.COLLAB_DEBUG_LOGS === 'true'

const rooms = new Map()
const spaceEventSubscribers = new Map()

const debugLog = (...args) => {
  if (!debugLogsEnabled) {
    return
  }

  console.info('[collab-ws]', ...args)
}

const buildRoomKey = (payload) => `space:${payload.spaceId}:note:${payload.noteId}`
const buildSpaceEventKey = (spaceId) => `space:${spaceId}`

const extractAwarenessClientIds = (update) => {
  const decoder = decoding.createDecoder(update)
  const size = decoding.readVarUint(decoder)
  const clientIds = []

  for (let index = 0; index < size; index += 1) {
    clientIds.push(decoding.readVarUint(decoder))
    decoding.readVarUint(decoder)
    decoding.readVarString(decoder)
  }

  return clientIds
}

const encodeSyncUpdate = (update) => {
  const encoder = encoding.createEncoder()
  encoding.writeVarUint(encoder, MESSAGE_SYNC)
  syncProtocol.writeUpdate(encoder, update)
  return encoding.toUint8Array(encoder)
}

const encodeDocumentSnapshot = (doc) => encodeSyncUpdate(Y.encodeStateAsUpdate(doc))

const encodeSyncStepOne = (doc) => {
  const encoder = encoding.createEncoder()
  encoding.writeVarUint(encoder, MESSAGE_SYNC)
  syncProtocol.writeSyncStep1(encoder, doc)
  return encoding.toUint8Array(encoder)
}

const encodeAwarenessUpdate = (awareness, clientIds) => {
  const encoder = encoding.createEncoder()
  encoding.writeVarUint(encoder, MESSAGE_AWARENESS)
  encoding.writeVarUint8Array(encoder, awarenessProtocol.encodeAwarenessUpdate(awareness, clientIds))
  return encoding.toUint8Array(encoder)
}

const sendBinary = (socket, payload) => {
  if (socket.readyState !== WebSocket.OPEN) {
    return
  }

  socket.send(payload, { binary: true })
}

const decodeTextYjsPayload = (payload) => {
  let message
  try {
    message = JSON.parse(payload.toString('utf8'))
  } catch {
    return null
  }

  if (!message || message.type !== 'yjs-binary' || typeof message.payload !== 'string') {
    return null
  }

  return Buffer.from(message.payload, 'base64')
}

const broadcastBinary = (room, payload, originSocket) => {
  for (const client of room.clients) {
    if (client === originSocket || client.readyState !== WebSocket.OPEN) {
      continue
    }

    client.send(payload, { binary: true })
  }
}

const destroyRoom = (roomKey) => {
  const room = rooms.get(roomKey)
  if (!room || room.clients.size) {
    return
  }

  room.awareness.destroy()
  room.doc.destroy()
  rooms.delete(roomKey)
}

const scheduleRoomDestroy = (roomKey) => {
  const room = rooms.get(roomKey)
  if (!room || room.destroyTimer) {
    return
  }

  room.destroyTimer = setTimeout(() => {
    const latestRoom = rooms.get(roomKey)
    if (!latestRoom || latestRoom.clients.size) {
      return
    }

    destroyRoom(roomKey)
  }, ROOM_RETENTION_MS)
}

const getOrCreateRoom = (roomKey, initialContent) => {
  const existingRoom = rooms.get(roomKey)
  if (existingRoom) {
    if (existingRoom.destroyTimer) {
      clearTimeout(existingRoom.destroyTimer)
      existingRoom.destroyTimer = null
    }
    return existingRoom
  }

  const doc = new Y.Doc()
  const yXmlFragment = doc.getXmlFragment('content')

  const awareness = new awarenessProtocol.Awareness(doc)
  const room = {
    key: roomKey,
    doc,
    yXmlFragment,
    awareness,
    clients: new Set(),
    bootstrapContent: typeof initialContent === 'string' ? initialContent : '',
    bootstrapLockedAt: 0,
    bootstrapSocket: null,
    bootstrapped: yXmlFragment.length > 0,
    destroyTimer: null,
  }

  doc.on('update', (update, origin) => {
    debugLog('broadcast update', {
      roomKey,
      bytes: update.length,
      clients: room.clients.size,
      fragmentLength: yXmlFragment.length,
    })
    broadcastBinary(room, encodeSyncUpdate(update), origin)
  })

  awareness.on('update', ({ added, updated, removed }, origin) => {
    const changedClients = [...added, ...updated, ...removed]
    if (!changedClients.length) {
      return
    }

    debugLog('broadcast awareness', {
      roomKey,
      changedClients,
      clients: room.clients.size,
    })
    broadcastBinary(room, encodeAwarenessUpdate(awareness, changedClients), origin)
  })

  rooms.set(roomKey, room)
  return room
}

const detachSocketFromRoom = (socket) => {
  const roomKey = socket.roomKey
  if (!roomKey) {
    return
  }

  const room = rooms.get(roomKey)
  if (!room) {
    socket.roomKey = ''
    socket.awarenessClientIds = new Set()
    return
  }

  room.clients.delete(socket)
  if (socket.awarenessClientIds && socket.awarenessClientIds.size) {
    awarenessProtocol.removeAwarenessStates(room.awareness, Array.from(socket.awarenessClientIds), socket)
  }

  if (room.bootstrapSocket === socket && !room.bootstrapped && room.yXmlFragment.length === 0) {
    room.bootstrapSocket = null
    room.bootstrapLockedAt = 0
  }

  socket.roomKey = ''
  socket.awarenessClientIds = new Set()

  if (!room.clients.size) {
    scheduleRoomDestroy(roomKey)
  }
}

const detachSocketFromSpaceEvents = (socket) => {
  const spaceKey = socket.spaceEventKey
  if (!spaceKey) {
    return
  }

  const subscribers = spaceEventSubscribers.get(spaceKey)
  if (subscribers) {
    subscribers.delete(socket)
    if (!subscribers.size) {
      spaceEventSubscribers.delete(spaceKey)
    }
  }

  socket.spaceEventKey = ''
}

const verifyTicket = async (ticket) => {
  const response = await fetch(`${apiBaseUrl}${ticketVerifyPath}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(internalToken ? { 'X-Internal-Token': internalToken } : {}),
    },
    body: JSON.stringify({ ticket }),
  })

  if (!response.ok) {
    throw new Error(`Ticket verification failed with status ${response.status}`)
  }

  const envelope = await response.json()
  if (!envelope || envelope.code !== 200 || !envelope.data) {
    throw new Error('Ticket verification response is invalid')
  }

  return envelope.data
}

const verifySpaceEventTicket = async (ticket) => {
  const response = await fetch(`${apiBaseUrl}${spaceEventTicketVerifyPath}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(internalToken ? { 'X-Internal-Token': internalToken } : {}),
    },
    body: JSON.stringify({ ticket }),
  })

  if (!response.ok) {
    throw new Error(`Space event ticket verification failed with status ${response.status}`)
  }

  const envelope = await response.json()
  if (!envelope || envelope.code !== 200 || !envelope.data) {
    throw new Error('Space event ticket verification response is invalid')
  }

  return envelope.data
}

const readJsonBody = (request) =>
  new Promise((resolve, reject) => {
    let rawBody = ''
    request.on('data', (chunk) => {
      rawBody += chunk.toString()
      if (rawBody.length > 256 * 1024) {
        reject(new Error('Request body too large'))
      }
    })
    request.on('end', () => {
      try {
        resolve(rawBody ? JSON.parse(rawBody) : {})
      } catch (error) {
        reject(error)
      }
    })
    request.on('error', reject)
  })

const broadcastSpaceEvent = (event) => {
  if (!event || !event.spaceId) {
    return 0
  }

  const subscribers = spaceEventSubscribers.get(buildSpaceEventKey(event.spaceId))
  if (!subscribers || !subscribers.size) {
    return 0
  }

  const payload = JSON.stringify({
    type: 'space_event',
    event,
  })
  let sentCount = 0
  for (const subscriber of subscribers) {
    if (subscriber.readyState !== WebSocket.OPEN) {
      continue
    }
    subscriber.send(payload)
    sentCount += 1
  }
  return sentCount
}

const server = http.createServer(async (request, response) => {
  if (request.url === '/health') {
    const connectionCount = Array.from(rooms.values()).reduce((count, room) => count + room.clients.size, 0)
    const eventConnectionCount = Array.from(spaceEventSubscribers.values()).reduce(
      (count, subscribers) => count + subscribers.size,
      0,
    )
    response.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' })
    response.end(
      JSON.stringify({
        status: 'ok',
        service: 'notask-flow-collab-ws',
        rooms: rooms.size,
        connections: connectionCount,
        eventConnections: eventConnectionCount,
      }),
    )
    return
  }

  if (request.method === 'POST' && request.url === '/internal/broadcast') {
    if (!internalToken || request.headers['x-internal-token'] !== internalToken) {
      response.writeHead(403, { 'Content-Type': 'application/json; charset=utf-8' })
      response.end(JSON.stringify({ message: 'Forbidden' }))
      return
    }

    try {
      const event = await readJsonBody(request)
      const sent = broadcastSpaceEvent(event)
      response.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' })
      response.end(JSON.stringify({ sent }))
    } catch (error) {
      response.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' })
      response.end(JSON.stringify({ message: 'Invalid event payload' }))
    }
    return
  }

  response.writeHead(404, { 'Content-Type': 'application/json; charset=utf-8' })
  response.end(JSON.stringify({ message: 'Not Found' }))
})

const wss = new WebSocketServer({ noServer: true })

wss.on('connection', (socket) => {
  let authenticated = false
  socket.connectionMode = ''
  socket.roomKey = ''
  socket.spaceEventKey = ''
  socket.awarenessClientIds = new Set()

  const authTimer = setTimeout(() => {
    if (!authenticated && socket.readyState === WebSocket.OPEN) {
      socket.close(1008, 'Authentication timeout')
    }
  }, authTimeoutMs)

  socket.on('message', async (payload, isBinary) => {
    if (!authenticated) {
      if (isBinary) {
        socket.close(1008, 'First message must be auth payload')
        return
      }

      let parsed
      try {
        parsed = JSON.parse(payload.toString())
      } catch (error) {
        socket.close(1008, 'Invalid auth payload')
        return
      }

      if (
        !['auth', 'space_auth'].includes(parsed.type) ||
        typeof parsed.ticket !== 'string' ||
        !parsed.ticket.trim()
      ) {
        socket.close(1008, 'Missing ticket')
        return
      }

      try {
        if (parsed.type === 'space_auth') {
          const verification = await verifySpaceEventTicket(parsed.ticket.trim())
          if (!verification || verification.valid === false) {
            socket.close(1008, 'Ticket rejected')
            return
          }

          const spaceKey = buildSpaceEventKey(verification.spaceId)
          const subscribers = spaceEventSubscribers.get(spaceKey) || new Set()
          subscribers.add(socket)
          spaceEventSubscribers.set(spaceKey, subscribers)
          socket.spaceEventKey = spaceKey
          socket.connectionMode = 'space_event'
          authenticated = true
          clearTimeout(authTimer)

          socket.send(
            JSON.stringify({
              type: 'space_ready',
              spaceId: verification.spaceId,
              userId: verification.userId,
            }),
          )
          return
        }

        const verification = await verifyTicket(parsed.ticket.trim())
        if (!verification || verification.valid === false || verification.canEdit === false) {
          socket.close(1008, 'Ticket rejected')
          return
        }

        const roomKey = buildRoomKey(verification)
        const room = getOrCreateRoom(roomKey, verification.content || '')
        const bootstrapLockStale =
          room.bootstrapSocket &&
          (room.bootstrapSocket.readyState !== WebSocket.OPEN ||
            Date.now() - room.bootstrapLockedAt > BOOTSTRAP_LOCK_TIMEOUT_MS)
        if (bootstrapLockStale) {
          room.bootstrapSocket = null
          room.bootstrapLockedAt = 0
        }
        const shouldBootstrap = !room.bootstrapped && !room.bootstrapSocket && room.yXmlFragment.length === 0
        if (shouldBootstrap) {
          room.bootstrapSocket = socket
          room.bootstrapLockedAt = Date.now()
        }
        room.clients.add(socket)
        socket.roomKey = roomKey
        socket.connectionMode = 'yjs_note'
        authenticated = true
        clearTimeout(authTimer)

        socket.send(
          JSON.stringify({
            type: 'ready',
            roomKey,
            userId: verification.userId,
            noteId: verification.noteId,
            spaceId: verification.spaceId,
            bootstrapContent: shouldBootstrap ? room.bootstrapContent : '',
          }),
        )
        debugLog('client ready', {
          roomKey,
          userId: verification.userId,
          noteId: verification.noteId,
          spaceId: verification.spaceId,
          shouldBootstrap,
          clients: room.clients.size,
          fragmentLength: room.yXmlFragment.length,
        })
        sendBinary(socket, encodeDocumentSnapshot(room.doc))
        sendBinary(socket, encodeSyncStepOne(room.doc))
        const awarenessClientIds = Array.from(room.awareness.getStates().keys())
        if (awarenessClientIds.length) {
          sendBinary(socket, encodeAwarenessUpdate(room.awareness, awarenessClientIds))
        }
        return
      } catch (error) {
        socket.close(1011, 'Ticket verification failed')
        return
      }
    }

    if (socket.connectionMode === 'space_event') {
      return
    }

    const room = rooms.get(socket.roomKey)
    if (!room) {
      socket.close(1011, 'Room not initialized')
      return
    }

    const binaryPayload = isBinary ? payload : decodeTextYjsPayload(payload)
    if (!binaryPayload) {
      return
    }

    try {
      const message = binaryPayload instanceof Uint8Array ? binaryPayload : new Uint8Array(binaryPayload)
      const decoder = decoding.createDecoder(message)
      const messageType = decoding.readVarUint(decoder)
      debugLog('message received', {
        roomKey: socket.roomKey,
        source: isBinary ? 'binary' : 'text',
        messageType,
        bytes: message.length,
      })

      switch (messageType) {
        case MESSAGE_SYNC: {
          const encoder = encoding.createEncoder()
          encoding.writeVarUint(encoder, MESSAGE_SYNC)
          syncProtocol.readSyncMessage(decoder, encoder, room.doc, socket)
          if (room.bootstrapSocket === socket && room.yXmlFragment.length > 0) {
            room.bootstrapped = true
            room.bootstrapSocket = null
            room.bootstrapLockedAt = 0
            room.bootstrapContent = ''
          }
          const reply = encoding.toUint8Array(encoder)
          if (reply.length > 1) {
            sendBinary(socket, reply)
          }
          debugLog('sync handled', {
            roomKey: socket.roomKey,
            source: isBinary ? 'binary' : 'text',
            replyBytes: reply.length,
            fragmentLength: room.yXmlFragment.length,
            clients: room.clients.size,
          })
          break
        }
        case MESSAGE_AWARENESS: {
          const update = decoding.readVarUint8Array(decoder)
          socket.awarenessClientIds = new Set(extractAwarenessClientIds(update))
          awarenessProtocol.applyAwarenessUpdate(room.awareness, update, socket)
          debugLog('awareness handled', {
            roomKey: socket.roomKey,
            source: isBinary ? 'binary' : 'text',
            clients: room.clients.size,
            awarenessClients: socket.awarenessClientIds.size,
          })
          break
        }
        default:
          return
      }
    } catch (error) {
      console.error('collab-ws message handling failed', error)
      socket.close(1011, 'Yjs message handling failed')
    }
  })

  socket.on('close', () => {
    clearTimeout(authTimer)
    detachSocketFromRoom(socket)
    detachSocketFromSpaceEvents(socket)
  })

  socket.on('error', () => {
    clearTimeout(authTimer)
    detachSocketFromRoom(socket)
    detachSocketFromSpaceEvents(socket)
  })
})

server.on('upgrade', (request, socket, head) => {
  const pathname = new URL(request.url, 'http://localhost').pathname
  if (pathname !== wsPath) {
    socket.destroy()
    return
  }

  wss.handleUpgrade(request, socket, head, (client) => {
    wss.emit('connection', client, request)
  })
})

server.listen(port, host, () => {
  console.log(`notask-flow-collab-ws listening on ${host}:${port}${wsPath}`)
})
