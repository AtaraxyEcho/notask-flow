const LOCALHOST_NAMES = new Set(['localhost', '127.0.0.1', '::1', '[::1]'])
const DEFAULT_COLLAB_WS_PATH = '/ws'
const DEFAULT_COLLAB_WS_PORT = '8081'

const isLocalHost = (host: string) => LOCALHOST_NAMES.has(host.toLowerCase())

const wsProtocolForPage = () => (window.location.protocol === 'https:' ? 'wss:' : 'ws:')

const wsProtocolForUrl = (url: URL) => (url.protocol === 'https:' ? 'wss:' : 'ws:')

const resolveRelativeWsUrl = (path: string) => `${wsProtocolForPage()}//${window.location.host}${path}`

const normalizeAbsoluteWsUrl = (rawUrl: string) => {
  const parsedUrl = new URL(rawUrl)
  if (isLocalHost(parsedUrl.hostname) && !isLocalHost(window.location.hostname)) {
    parsedUrl.hostname = window.location.hostname
  }
  return parsedUrl.toString()
}

export const resolveCollabWsUrl = (configuredUrl?: string) => {
  const trimmedUrl = configuredUrl?.trim()
  if (trimmedUrl) {
    if (trimmedUrl.startsWith('/')) {
      return resolveRelativeWsUrl(trimmedUrl)
    }
    return normalizeAbsoluteWsUrl(trimmedUrl)
  }

  return `${wsProtocolForPage()}//${window.location.hostname}:${DEFAULT_COLLAB_WS_PORT}${DEFAULT_COLLAB_WS_PATH}`
}

export const resolveCollabWsUrlFromApiBase = (baseUrl?: string) => {
  const trimmedBaseUrl = baseUrl?.trim()
  if (!trimmedBaseUrl) {
    return resolveCollabWsUrl()
  }

  const parsedUrl = new URL(trimmedBaseUrl, window.location.origin)
  return `${wsProtocolForUrl(parsedUrl)}//${parsedUrl.hostname}:${DEFAULT_COLLAB_WS_PORT}${DEFAULT_COLLAB_WS_PATH}`
}
