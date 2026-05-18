const NOTE_DETAIL_PATH_PATTERN = /^\/app\/notes\/\d+(?:[?#].*)?$/

export function normalizeAuthRedirect(redirect: unknown, fallback = '/app/notes') {
  if (typeof redirect !== 'string') {
    return fallback
  }

  const normalizedRedirect = redirect.trim()
  if (!normalizedRedirect || !normalizedRedirect.startsWith('/') || normalizedRedirect.startsWith('//')) {
    return fallback
  }

  if (NOTE_DETAIL_PATH_PATTERN.test(normalizedRedirect)) {
    return fallback
  }

  return normalizedRedirect
}
