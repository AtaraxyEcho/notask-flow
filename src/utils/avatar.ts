const AVATAR_PATH_PREFIX = '/api/v1/public/users/'
const API_PREFIX = '/api/v1'

export function resolveAvatarUrl(avatarUrl?: string | null): string | undefined {
  const value = avatarUrl?.trim()
  if (!value) {
    return undefined
  }
  const storageUserId = resolveAvatarStorageUserId(value)
  if (storageUserId) {
    return joinApiAssetUrl(resolveApiBaseUrl(), `${AVATAR_PATH_PREFIX}${storageUserId}/avatar`)
  }
  if (value.startsWith('http://') || value.startsWith('https://')) {
    return value
  }
  if (value.startsWith('/api/v1/public/users/') || value.startsWith('/api/v1/user/')) {
    const normalized = value.replace('/api/v1/user/', AVATAR_PATH_PREFIX)
    return joinApiAssetUrl(resolveApiBaseUrl(), normalized)
  }
  return value.startsWith('/') ? value : `${AVATAR_PATH_PREFIX}${value}`
}

function resolveAvatarStorageUserId(value: string) {
  const match = value.match(/(?:^|\/)avatars\/(\d+)\//)
  return match?.[1]
}

function resolveApiBaseUrl() {
  return (import.meta.env.VITE_API_BASE_URL as string | undefined) || ''
}

function joinApiAssetUrl(baseUrl: string, path: string): string {
  const normalizedBaseUrl = baseUrl.replace(/\/$/, '')
  if (!normalizedBaseUrl) {
    return path
  }
  if (normalizedBaseUrl.endsWith(API_PREFIX) && path.startsWith(`${API_PREFIX}/`)) {
    return `${normalizedBaseUrl}${path.slice(API_PREFIX.length)}`
  }
  return `${normalizedBaseUrl}${path}`
}
