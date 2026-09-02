import { translate } from '@/i18n'

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function renderInline(value: string) {
  return value
    .replace(/~~(.+?)~~/g, '<s>$1</s>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/`(.+?)`/g, '<code>$1</code>')
}

export function renderMarkdownLite(source?: string | null) {
  if (!source) {
    return `<p class="markdown-empty">${translate('markdown.empty')}</p>`
  }

  const escaped = escapeHtml(source)
  const lines = escaped.split(/\r?\n/)
  const blocks: string[] = []
  let listBuffer: string[] = []
  let orderedBuffer: string[] = []
  let taskBuffer: string[] = []
  let codeBuffer: string[] = []
  let inCode = false

  const flushList = () => {
    if (listBuffer.length) {
      blocks.push(`<ul>${listBuffer.join('')}</ul>`)
      listBuffer = []
    }
  }

  const flushOrdered = () => {
    if (orderedBuffer.length) {
      blocks.push(`<ol>${orderedBuffer.join('')}</ol>`)
      orderedBuffer = []
    }
  }

  const flushTask = () => {
    if (taskBuffer.length) {
      blocks.push(`<ul class="task-list">${taskBuffer.join('')}</ul>`)
      taskBuffer = []
    }
  }

  const flushCode = () => {
    if (codeBuffer.length) {
      blocks.push(`<pre><code>${codeBuffer.join('\n')}</code></pre>`)
      codeBuffer = []
    }
  }

  const flushAllLists = () => {
    flushList()
    flushOrdered()
    flushTask()
  }

  lines.forEach((line: string) => {
    if (line.startsWith('```')) {
      if (inCode) {
        flushCode()
      } else {
        flushAllLists()
      }
      inCode = !inCode
      return
    }

    if (inCode) {
      codeBuffer.push(line)
      return
    }

    if (!line.trim()) {
      flushAllLists()
      blocks.push('<div class="markdown-gap"></div>')
      return
    }

    const taskUnchecked = line.match(/^- \[ \] (.+)$/)
    if (taskUnchecked) {
      flushList()
      flushOrdered()
      taskBuffer.push(`<li data-task-state="unchecked">${renderInline(taskUnchecked[1])}</li>`)
      return
    }

    const taskChecked = line.match(/^- \[[xX]\] (.+)$/)
    if (taskChecked) {
      flushList()
      flushOrdered()
      taskBuffer.push(`<li data-task-state="checked">${renderInline(taskChecked[1])}</li>`)
      return
    }

    if (line.startsWith('- ')) {
      flushOrdered()
      flushTask()
      listBuffer.push(`<li>${renderInline(line.slice(2))}</li>`)
      return
    }

    const orderedMatch = line.match(/^\d+\. (.+)$/)
    if (orderedMatch) {
      flushList()
      flushTask()
      orderedBuffer.push(`<li>${renderInline(orderedMatch[1])}</li>`)
      return
    }

    flushAllLists()

    if (line === '---') {
      blocks.push('<hr />')
      return
    }

    if (line.startsWith('### ')) {
      blocks.push(`<h3>${renderInline(line.slice(4))}</h3>`)
      return
    }

    if (line.startsWith('## ')) {
      blocks.push(`<h2>${renderInline(line.slice(3))}</h2>`)
      return
    }

    if (line.startsWith('# ')) {
      blocks.push(`<h1>${renderInline(line.slice(2))}</h1>`)
      return
    }

    if (line.startsWith('> ')) {
      blocks.push(`<blockquote><p>${renderInline(line.slice(2))}</p></blockquote>`)
      return
    }

    blocks.push(`<p>${renderInline(line)}</p>`)
  })

  flushAllLists()
  flushCode()

  return blocks.join('')
}
