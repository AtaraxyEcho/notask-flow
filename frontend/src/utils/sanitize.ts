import DOMPurify, { type Config } from 'dompurify'

const SAFE_URI_PATTERN =
  /^(?:(?:https?|mailto|tel|blob):|[^a-z]|[a-z+.\-]+(?:[^a-z+.\-:]|$))/i

const HTML_SANITIZE_CONFIG: Config = {
  ALLOWED_TAGS: [
    'a',
    'article',
    'audio',
    'b',
    'blockquote',
    'br',
    'button',
    'caption',
    'code',
    'div',
    'em',
    'figcaption',
    'figure',
    'h1',
    'h2',
    'h3',
    'h4',
    'h5',
    'h6',
    'hr',
    'i',
    'img',
    'li',
    'mark',
    'ol',
    'p',
    'pre',
    's',
    'section',
    'source',
    'span',
    'strong',
    'sub',
    'sup',
    'table',
    'tbody',
    'td',
    'th',
    'thead',
    'tr',
    'u',
    'ul',
  ],
  ALLOWED_ATTR: [
    'alt',
    'aria-label',
    'class',
    'colspan',
    'controls',
    'data-attachment-id',
    'data-checked',
    'data-mime',
    'data-name',
    'data-managed-file-id',
    'data-preview-href',
    'data-size',
    'data-task-state',
    'data-text-align',
    'data-type',
    'data-url',
    'data-audio-action',
    'data-first-line-indent',
    'href',
    'rel',
    'rowspan',
    'src',
    'style',
    'target',
    'title',
    'type',
    'preload',
  ],
  ALLOW_DATA_ATTR: true,
  ALLOWED_URI_REGEXP: SAFE_URI_PATTERN,
  FORBID_TAGS: ['form', 'iframe', 'input', 'math', 'meta', 'object', 'script', 'svg', 'template'],
  FORBID_ATTR: ['onerror', 'onload', 'onclick', 'onmouseover', 'srcdoc'],
}

/**
 * 净化即将通过 v-html 或 srcdoc 渲染的 HTML 内容。
 */
export function sanitizeHtml(value?: string | null) {
  if (!value) {
    return ''
  }

  return normalizeManagedFileCards(DOMPurify.sanitize(value, HTML_SANITIZE_CONFIG))
}

/**
 * 净化富文本编辑器产生的 HTML 内容。
 */
export function sanitizeEditorHtml(value?: string | null) {
  return sanitizeHtml(value)
}

const AUDIO_EXTENSIONS = new Set(['aac', 'flac', 'm4a', 'mp3', 'oga', 'ogg', 'opus', 'wav', 'weba', 'webm'])

function extensionFromValue(value: string) {
  const cleanValue = (value.split('?')[0] || '').split('#')[0] || ''
  const chunks = cleanValue.toLowerCase().split('.')
  return chunks.length > 1 ? chunks.pop() || '' : ''
}

function isAudioReference(card: Element, type: string, mimeType: string, name: string, href: string, source: string) {
  return (
    type === 'audio' ||
    type === 'audio-file-card' ||
    card.classList.contains('audio-card') ||
    card.classList.contains('notask-audio-card') ||
    card.classList.contains('notask-media-card-audio') ||
    mimeType.startsWith('audio/') ||
    [name, href, source].some((value) => AUDIO_EXTENSIONS.has(extensionFromValue(value)))
  )
}

function normalizeManagedFileCards(value: string) {
  if (!value || typeof DOMParser === 'undefined') {
    return value
  }

  const document = new DOMParser().parseFromString(`<div>${value}</div>`, 'text/html')
  const root = document.body.firstElementChild
  if (!root) {
    return value
  }

  root.querySelectorAll(
    '.file-card[data-managed-file-id], .notask-audio-card[data-managed-file-id], .notask-media-card[data-managed-file-id], a[data-managed-file-id]',
  ).forEach((card) => {
    const fileId = card.getAttribute('data-managed-file-id') || ''
    if (!fileId) {
      return
    }

    const attachmentId = card.getAttribute('data-attachment-id') || ''
    const name =
      card.getAttribute('data-name') ||
      card.querySelector('.notask-media-card-name')?.textContent?.trim() ||
      card.querySelector('.notask-audio-name')?.textContent?.trim() ||
      card.querySelector('.file-main strong')?.textContent?.trim() ||
      card.textContent?.trim() ||
      '附件'
    const mimeType = card.getAttribute('data-mime') || ''
    const size = card.getAttribute('data-size') || ''
    const href = card.getAttribute('href') || card.getAttribute('data-preview-href') || card.getAttribute('data-url') || `/app/files/preview/${fileId}`
    const rawSource = card.getAttribute('data-url') || card.querySelector('audio, source')?.getAttribute('src') || ''
    const fallbackSource = href.includes('/app/files/preview/') ? '' : href
    const source = rawSource || fallbackSource
    const rawType = card.getAttribute('data-type') || ''
    const type = isAudioReference(card, rawType, mimeType, name, href, source) ? 'audio' : 'file'
    const nextLink = document.createElement('a')
    nextLink.className = type === 'audio' ? 'notask-media-link notask-media-link-audio' : 'notask-media-link notask-media-link-file'
    nextLink.setAttribute('data-managed-file-id', fileId)
    nextLink.setAttribute('data-type', type)
    nextLink.setAttribute('data-name', name)
    nextLink.setAttribute('href', href)
    nextLink.setAttribute('target', '_blank')
    nextLink.setAttribute('rel', 'noopener noreferrer')
    if (attachmentId) {
      nextLink.setAttribute('data-attachment-id', attachmentId)
    }
    if (mimeType) {
      nextLink.setAttribute('data-mime', mimeType)
    }
    if (source) {
      nextLink.setAttribute('data-url', source)
    }
    if (size) {
      nextLink.setAttribute('data-size', size)
    }
    nextLink.textContent = normalizeManagedFileName(name, type, mimeType)

    card.replaceWith(nextLink)
  })

  return root.innerHTML
}

function normalizeManagedFileName(name: string, type: string, mimeType: string) {
  const normalizedName = name.trim() || (type === 'audio' ? '音频文件' : '附件')
  if (extensionFromValue(normalizedName)) {
    return normalizedName
  }
  const subtype = mimeType.split('/')[1]?.split(';')[0]?.trim()
  return subtype ? `${normalizedName}.${subtype}` : normalizedName
}
