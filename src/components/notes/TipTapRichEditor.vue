<template>
  <div class="tiptap-editor flex h-full min-h-0 flex-col bg-[#F9F7F5]" :style="editorStyle">
    <Teleport :disabled="!toolbarTarget" :to="toolbarTarget || 'body'">
        <div class="tiptap-toolbar-shell" :class="{ 'tiptap-toolbar-shell-external': toolbarTarget }">
        <div class="tiptap-toolbar flex shrink-0 items-center gap-1 border-b border-outline-variant/20 bg-surface px-4 py-2">
          <button class="tiptap-tool" type="button" :class="toolClass('heading', { level: 1 })" :disabled="!editable" @click="setHeading(1)">
            <span class="material-symbols-outlined">format_h1</span>
          </button>
          <button class="tiptap-tool" type="button" :class="toolClass('heading', { level: 2 })" :disabled="!editable" @click="setHeading(2)">
            <span class="material-symbols-outlined">format_h2</span>
          </button>
          <button class="tiptap-tool" type="button" :class="toolClass('heading', { level: 3 })" :disabled="!editable" @click="setHeading(3)">
            <span class="material-symbols-outlined">format_h3</span>
          </button>

          <span class="tiptap-divider"></span>

          <button class="tiptap-tool" type="button" :class="toolClass('bold')" :disabled="!editable" @click="runCommand('toggleBold')">
            <span class="material-symbols-outlined">format_bold</span>
          </button>
          <button class="tiptap-tool" type="button" :class="toolClass('italic')" :disabled="!editable" @click="runCommand('toggleItalic')">
            <span class="material-symbols-outlined">format_italic</span>
          </button>
          <button class="tiptap-tool" type="button" :class="toolClass('strike')" :disabled="!editable" @click="runCommand('toggleStrike')">
            <span class="material-symbols-outlined">format_strikethrough</span>
          </button>
          <button class="tiptap-tool" type="button" :class="toolClass('code')" :disabled="!editable" @click="runCommand('toggleCode')">
            <span class="material-symbols-outlined">code</span>
          </button>

          <span class="tiptap-divider"></span>

          <select v-model="selectedFontFamily" class="tiptap-select tiptap-font-select" :disabled="!editable" title="字体" @change="applyFontFamily">
            <option v-for="option in fontFamilyOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
          <select v-model="selectedFontSize" class="tiptap-select tiptap-size-select" :disabled="!editable" title="字号" @change="applyFontSize">
            <option v-for="option in fontSizeOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
          <select v-model="selectedLineHeight" class="tiptap-select tiptap-line-select" :disabled="!editable" title="行距">
            <option v-for="option in lineHeightOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
          <button
            class="tiptap-tool"
            type="button"
            :class="firstLineIndentToolClass"
            :disabled="!editable"
            title="首行缩进"
            @click="toggleFirstLineIndent"
          >
            <span class="material-symbols-outlined">format_indent_increase</span>
          </button>
          <div class="tiptap-align-group" aria-label="段落对齐">
            <button
              v-for="option in textAlignOptions"
              :key="option.value"
              class="tiptap-tool tiptap-align-tool"
              type="button"
              :class="{ 'tiptap-tool-active': selectedTextAlign === option.value }"
              :disabled="!editable"
              :title="option.label"
              @click="applyTextAlign(option.value)"
            >
              <span class="material-symbols-outlined">{{ option.icon }}</span>
            </button>
          </div>
          <div class="tiptap-color-picker">
            <button
              ref="colorTriggerRef"
              class="tiptap-color-trigger"
              type="button"
              :disabled="!editable"
              title="字体颜色"
              @click="toggleColorPanel"
            >
              <span class="material-symbols-outlined">format_color_text</span>
              <span class="tiptap-color-current" :style="{ backgroundColor: selectedFontColor }"></span>
            </button>
          </div>

          <span class="tiptap-divider"></span>

          <button class="tiptap-tool" type="button" :class="toolClass('bulletList')" :disabled="!editable" @click="runCommand('toggleBulletList')">
            <span class="material-symbols-outlined">format_list_bulleted</span>
          </button>
          <button class="tiptap-tool" type="button" :class="toolClass('orderedList')" :disabled="!editable" @click="runCommand('toggleOrderedList')">
            <span class="material-symbols-outlined">format_list_numbered</span>
          </button>
          <button class="tiptap-tool" type="button" :class="toolClass('taskList')" :disabled="!editable" @click="runCommand('toggleTaskList')">
            <span class="material-symbols-outlined">check_box</span>
          </button>
          <button class="tiptap-tool" type="button" :class="toolClass('blockquote')" :disabled="!editable" @click="runCommand('toggleBlockquote')">
            <span class="material-symbols-outlined">format_quote</span>
          </button>

          <span class="tiptap-divider"></span>

          <button class="tiptap-tool" type="button" :disabled="!editable" @click="insertDivider">
            <span class="material-symbols-outlined">horizontal_rule</span>
          </button>
          <button class="tiptap-tool" type="button" :class="toolClass('link')" :disabled="!editable" @click="openLinkPanel">
            <span class="material-symbols-outlined">link</span>
          </button>
          <button class="tiptap-tool" type="button" :disabled="!editable || uploadingImage" @click="triggerImageUpload">
            <span class="material-symbols-outlined">{{ uploadingImage ? 'progress_activity' : 'image' }}</span>
          </button>
          <button class="tiptap-tool tiptap-file-tool" type="button" :disabled="!editable" @click="$emit('request-file')">
            <span class="material-symbols-outlined">attach_file</span>
            <span class="hidden text-[12px] font-semibold sm:inline">{{ fileButtonLabel }}</span>
          </button>
          <slot name="toolbar-extra" />
          <input ref="imageInputRef" class="hidden" type="file" accept="image/*" @change="handleImageSelected" />
        </div>

        <div v-if="colorPanelOpen" class="tiptap-color-panel" :style="colorPanelStyle">
          <div class="tiptap-color-panel-title">字体颜色</div>
          <div class="tiptap-color-grid">
            <button
              v-for="option in fontColorOptions"
              :key="option.value"
              class="tiptap-color-option"
              type="button"
              :class="{ 'tiptap-color-option-active': selectedFontColor === option.value }"
              :style="{ backgroundColor: option.value }"
              :title="option.label"
              @click="applyFontColor(option.value)"
            ></button>
          </div>
          <label class="tiptap-color-custom">
            <span>自定义</span>
            <input v-model="customFontColor" type="color" @input="applyFontColor(customFontColor)" />
          </label>
        </div>

        <div v-if="linkPanelOpen" class="tiptap-link-panel">
          <input
            v-model="linkText"
            class="tiptap-link-input"
            :placeholder="selectedText || 'Text'"
            type="text"
            @keydown.enter.prevent="applyLink"
          />
          <input
            v-model="linkHref"
            class="tiptap-link-input tiptap-link-url"
            placeholder="https://"
            type="url"
            @keydown.enter.prevent="applyLink"
          />
          <button class="tiptap-link-action primary" type="button" @click="applyLink">
            <span class="material-symbols-outlined">check</span>
          </button>
          <button class="tiptap-link-action" type="button" @click="clearLink">
            <span class="material-symbols-outlined">link_off</span>
          </button>
          <button class="tiptap-link-action" type="button" @click="linkPanelOpen = false">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
      </div>
    </Teleport>

    <div v-if="$slots['after-toolbar']" class="tiptap-after-toolbar">
      <slot name="after-toolbar" />
    </div>

    <div ref="contentScrollRef" class="tiptap-content custom-scrollbar relative min-h-0 flex-1 overflow-y-auto" @scroll="scheduleRemoteCursorRefresh">
      <div v-if="$slots['before-content']" class="tiptap-before-content">
        <slot name="before-content" />
      </div>
      <EditorContent class="tiptap-content-inner" :editor="editor" />
      <template v-if="!collaboration" v-for="cursor in remoteCursorOverlays" :key="cursor.id">
        <span
          v-for="(selectionStyle, index) in cursor.selectionStyles"
          :key="`${cursor.id}-selection-${index}`"
          class="tiptap-remote-selection"
          :style="selectionStyle"
        ></span>
        <span class="tiptap-remote-cursor" :style="cursor.cursorStyle">
          <span class="tiptap-remote-label" :style="cursor.labelStyle">{{ cursor.name }}</span>
        </span>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import Collaboration from '@tiptap/extension-collaboration'
import CollaborationCaret from '@tiptap/extension-collaboration-caret'
import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'
import Placeholder from '@tiptap/extension-placeholder'
import TaskItem from '@tiptap/extension-task-item'
import TaskList from '@tiptap/extension-task-list'
import { Extension, Mark, Node, generateJSON, getSchema, mergeAttributes } from '@tiptap/core'
import type { Editor as TipTapEditor } from '@tiptap/core'
import StarterKit from '@tiptap/starter-kit'
import { EditorContent, useEditor } from '@tiptap/vue-3'
import { ElMessage } from 'element-plus'
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { prosemirrorJSONToYXmlFragment } from 'y-prosemirror'
import type { XmlFragment } from 'yjs'
import { fileService } from '@/api/modules/file'
import type { ManagedFile } from '@/types/app'
import { renderMarkdownLite } from '@/utils/markdown'
import { sanitizeEditorHtml } from '@/utils/sanitize'

type ChainCommand =
  | 'toggleBold'
  | 'toggleItalic'
  | 'toggleStrike'
  | 'toggleCode'
  | 'toggleBulletList'
  | 'toggleOrderedList'
  | 'toggleTaskList'
  | 'toggleBlockquote'

type ManagedFileReference = {
  fileId: number
  attachmentId: number
  kind: 'file' | 'image'
}

type EditorSelectionPayload = {
  from: number
  to: number
  updatedAt: number
}

type RemoteCursorUser = {
  userId: number
  name: string
  color: string
  colorLight: string
}

type RemoteCursor = RemoteCursorUser & {
  clientId: number
  from: number
  to: number
}

type RemoteCursorOverlay = {
  id: string
  name: string
  cursorStyle: Record<string, string>
  labelStyle: Record<string, string>
  selectionStyles: Array<Record<string, string>>
}

type CollaborationConfig = {
  bootstrapHtml?: string
  fragment: XmlFragment
  provider: {
    awareness: unknown
  }
  user: Record<string, unknown>
}

const props = withDefaults(
  defineProps<{
    collaboration?: CollaborationConfig | null
    modelValue: string
    htmlValue?: string
    placeholder?: string
    editable?: boolean
    fileButtonLabel?: string
    remoteCursors?: RemoteCursor[]
    spaceId?: number | null
    toolbarTarget?: HTMLElement | null
    activeStyleTarget?: 'editor' | 'title'
    titleStyle?: {
      color?: string
      fontFamily?: string
      fontSize?: string
      lineHeight?: string
      textAlign?: TextAlignValue
    }
  }>(),
  {
    activeStyleTarget: 'editor',
    collaboration: null,
    htmlValue: '',
    placeholder: '',
    editable: true,
    fileButtonLabel: 'File',
    remoteCursors: () => [],
    spaceId: null,
    toolbarTarget: null,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'update:htmlValue': [value: string]
  'request-file': []
  'file-inserted': [file: ManagedFile]
  'references-change': [references: ManagedFileReference[]]
  'selection-change': [selection: EditorSelectionPayload]
  'editor-focus': []
  'style-change': [style: { fontSize: string; lineHeight: string }]
  'title-style-change': [
    style: Partial<{
      color: string
      fontFamily: string
      fontSize: string
      lineHeight: string
      textAlign: TextAlignValue
    }>,
  ]
}>()

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

const ManagedLink = Link.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      ...managedDataAttributes(),
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
const FIRST_LINE_INDENT_VALUE = '2em'
type TextAlignValue = 'left' | 'center' | 'right'

const extensionFromValue = (value: string) => {
  const cleanValue = value.split('?')[0]?.split('#')[0] || ''
  const chunks = cleanValue.toLowerCase().split('.')
  return chunks.length > 1 ? chunks.pop() || '' : ''
}

const managedFileNameFromElement = (element: HTMLElement) =>
  element.getAttribute('data-name') ||
  element.querySelector('.notask-media-card-name')?.textContent?.trim() ||
  element.querySelector('.notask-audio-name')?.textContent?.trim() ||
  element.getAttribute('title') ||
  element.textContent?.trim() ||
  '音频文件'

const isAudioLikeElement = (element: HTMLElement) => {
  const dataType = element.getAttribute('data-type')?.toLowerCase()
  const mimeType = element.getAttribute('data-mime')?.toLowerCase() || ''
  const name = managedFileNameFromElement(element)
  const href =
    element.getAttribute('href') ||
    element.getAttribute('data-url') ||
    element.getAttribute('data-preview-href') ||
    element.querySelector('a')?.getAttribute('href') ||
    element.querySelector('audio, source')?.getAttribute('src') ||
    ''
  return dataType === 'audio' || dataType === 'audio-file-card' || mimeType.startsWith('audio/') || audioExtensions.has(extensionFromValue(name)) || audioExtensions.has(extensionFromValue(href))
}

const audioCardAttributesFromElement = (element: HTMLElement) => {
  const href = element.getAttribute('href') || element.querySelector('a')?.getAttribute('href') || ''
  const source = element.getAttribute('data-url') || element.querySelector('audio, source')?.getAttribute('src') || (href.startsWith('/app/files/preview/') ? '' : href)
  return {
    attachmentId: element.getAttribute('data-attachment-id'),
    managedFileId: element.getAttribute('data-managed-file-id'),
    mimeType: element.getAttribute('data-mime'),
    name: managedFileNameFromElement(element),
    previewHref: element.getAttribute('data-preview-href') || (href.startsWith('/app/files/preview/') ? href : ''),
    source,
  }
}

const audioMimeLabel = (mimeType: string) => {
  if (!mimeType) {
    return '音频文件'
  }
  const subtype = mimeType.split('/')[1]?.split(';')[0]?.toUpperCase()
  return subtype ? `${subtype} 音频` : '音频文件'
}

const AudioFileCard = Node.create({
  name: 'audioFileCard',
  priority: 1000,
  group: 'block',
  atom: true,
  selectable: true,
  draggable: true,

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
        parseHTML: (element: HTMLElement) => element.getAttribute('data-name') || element.textContent?.trim() || '音频文件',
        renderHTML: (attributes: Record<string, string | null>) =>
          attributes.name ? { 'data-name': attributes.name } : {},
      },
      previewHref: {
        default: null,
        parseHTML: (element: HTMLElement) => element.getAttribute('data-preview-href') || element.getAttribute('href'),
        renderHTML: (attributes: Record<string, string | null>) =>
          attributes.previewHref ? { 'data-preview-href': attributes.previewHref } : {},
      },
      source: {
        default: null,
        parseHTML: (element: HTMLElement) => element.getAttribute('data-url') || element.getAttribute('src'),
        renderHTML: (attributes: Record<string, string | null>) =>
          attributes.source ? { 'data-url': attributes.source } : {},
      },
    }
  },

  parseHTML() {
    return [
      {
        tag: 'div[data-type="audio-file-card"]',
      },
      {
        tag: 'div.notask-audio-card[data-managed-file-id]',
      },
      {
        tag: 'article.notask-media-card[data-managed-file-id]',
        getAttrs: (element) => {
          if (!(element instanceof HTMLElement) || !isAudioLikeElement(element)) {
            return false
          }
          return audioCardAttributesFromElement(element)
        },
      },
      {
        tag: 'a[data-managed-file-id]',
        getAttrs: (element) => {
          if (!(element instanceof HTMLElement) || !isAudioLikeElement(element)) {
            return false
          }
          return audioCardAttributesFromElement(element)
        },
      },
    ]
  },

  renderHTML({ node, HTMLAttributes }) {
    const name = String(node.attrs.name || '音频文件')
    const mimeType = String(node.attrs.mimeType || '')
    const attrs = mergeAttributes(HTMLAttributes, {
      class: 'notask-audio-card',
      contenteditable: 'false',
      'data-type': 'audio-file-card',
    })

    return [
      'div',
      attrs,
      [
        'button',
        {
          'aria-label': '播放音频',
          class: 'notask-audio-play-button',
          contenteditable: 'false',
          'data-audio-action': 'toggle',
          type: 'button',
        },
        ['span', { class: 'notask-audio-play-icon', 'aria-hidden': 'true' }],
      ],
      [
        'span',
        { class: 'notask-audio-main' },
        ['strong', { class: 'notask-audio-name' }, name],
        ['span', { class: 'notask-audio-type' }, audioMimeLabel(mimeType)],
      ],
      [
        'span',
        { class: 'notask-audio-wave', 'aria-hidden': 'true' },
        ['span'],
        ['span'],
        ['span'],
        ['span'],
        ['span'],
      ],
    ]
  },
})

const initialContent = computed(() => sanitizeEditorHtml(props.htmlValue || renderMarkdownLite(props.modelValue || '')))
const imageInputRef = ref<HTMLInputElement | null>(null)
const contentScrollRef = ref<HTMLDivElement | null>(null)
const colorTriggerRef = ref<HTMLButtonElement | null>(null)
const uploadingImage = ref(false)
const linkPanelOpen = ref(false)
const linkHref = ref('')
const linkText = ref('')
const selectedText = ref('')
const remoteCursorOverlays = ref<RemoteCursorOverlay[]>([])
let remoteCursorFrame: number | null = null
let suppressSelectionEmit = false
let suppressStyleEmit = false

const fontSizeOptions = [
  { label: '14px', value: '14px' },
  { label: '16px', value: '16px' },
  { label: '18px', value: '18px' },
  { label: '20px', value: '20px' },
  { label: '24px', value: '24px' },
  { label: '28px', value: '28px' },
  { label: '32px', value: '32px' },
  { label: '36px', value: '36px' },
]
const lineHeightOptions = [
  { label: '1.0', value: '1' },
  { label: '1.5', value: '1.5' },
  { label: '2.0', value: '2' },
  { label: '2.5', value: '2.5' },
]
const selectedFontSize = ref('16px')
const selectedLineHeight = ref('1.5')
const selectedFontFamily = ref('system')
const selectedFontColor = ref('#1D1B20')
const selectedTextAlign = ref<TextAlignValue>('left')
const editorBaseFontSize = ref('16px')
const editorBaseLineHeight = ref('1.5')
const colorPanelOpen = ref(false)
const customFontColor = ref('#1D1B20')
const colorPanelStyle = ref<Record<string, string>>({})
const fontFamilyOptions = [
  {
    label: '系统默认',
    value: 'system',
    cssValue: 'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
  },
  {
    label: '思源黑体',
    value: 'noto-sans-sc',
    cssValue: '"Noto Sans SC", "Source Han Sans SC", "Microsoft YaHei", sans-serif',
  },
  {
    label: '思源宋体',
    value: 'noto-serif-sc',
    cssValue: '"Noto Serif SC", "Source Han Serif SC", "SimSun", serif',
  },
  {
    label: '霞鹜文楷',
    value: 'lxgw-wenkai',
    cssValue: '"LXGW WenKai", "KaiTi", serif',
  },
  {
    label: '阿里普惠体',
    value: 'alibaba-puhuiti',
    cssValue: '"Alibaba PuHuiTi", "MiSans", "Microsoft YaHei", sans-serif',
  },
  {
    label: '等宽字体',
    value: 'mono',
    cssValue: '"JetBrains Mono", "Cascadia Mono", Consolas, monospace',
  },
]
const fontColorOptions = [
  { label: '墨黑', value: '#1D1B20' },
  { label: '石灰', value: '#5F5A57' },
  { label: '朱红', value: '#C2410C' },
  { label: '琥珀', value: '#B45309' },
  { label: '松绿', value: '#047857' },
  { label: '湖蓝', value: '#0369A1' },
  { label: '靛紫', value: '#6D28D9' },
  { label: '玫红', value: '#BE185D' },
]
const textAlignOptions = [
  { label: '左对齐', value: 'left' as const, icon: 'format_align_left' },
  { label: '居中', value: 'center' as const, icon: 'format_align_center' },
  { label: '右对齐', value: 'right' as const, icon: 'format_align_right' },
]
const editorStyle = computed(() => ({
  '--tiptap-block-gap': `${Math.min(1.2, Math.max(0.35, Number(editorBaseLineHeight.value) * 0.42)).toFixed(2)}rem`,
  '--tiptap-list-gap': `${Math.min(0.9, Math.max(0.25, Number(editorBaseLineHeight.value) * 0.28)).toFixed(2)}rem`,
  '--tiptap-font-size': editorBaseFontSize.value,
  '--tiptap-line-height': editorBaseLineHeight.value,
}))

const selectedFontFamilyOption = computed(
  () => fontFamilyOptions.find((option) => option.value === selectedFontFamily.value) || fontFamilyOptions[0],
)

const fontFamilyValueFromCss = (fontFamily?: string) =>
  fontFamilyOptions.find((option) => option.cssValue === fontFamily)?.value || 'system'

const syncTitleStyleState = () => {
  suppressStyleEmit = true
  selectedFontSize.value = props.titleStyle?.fontSize || '32px'
  selectedLineHeight.value = props.titleStyle?.lineHeight || '1.5'
  selectedFontFamily.value = fontFamilyValueFromCss(props.titleStyle?.fontFamily)
  selectedFontColor.value = props.titleStyle?.color || '#1D1B20'
  customFontColor.value = selectedFontColor.value
  selectedTextAlign.value = props.titleStyle?.textAlign || 'left'
  nextTick(() => {
    suppressStyleEmit = false
  }).catch(() => {
    suppressStyleEmit = false
  })
}

const emitTitleStyleChange = (style: Partial<{ color: string; fontFamily: string; fontSize: string; lineHeight: string; textAlign: TextAlignValue }>) => {
  emit('title-style-change', style)
}

const selectedParagraphPositions = (currentEditor: TipTapEditor) => {
  const positions: Array<{ pos: number; attrs: Record<string, unknown> }> = []
  const { from, to } = currentEditor.state.selection
  currentEditor.state.doc.nodesBetween(from, to, (node, pos) => {
    if (node.type.name === 'paragraph') {
      positions.push({ pos, attrs: node.attrs })
    }
  })
  return positions
}

const selectedAlignableBlockPositions = (currentEditor: TipTapEditor) => {
  const positions: Array<{ pos: number; attrs: Record<string, unknown> }> = []
  const { from, to } = currentEditor.state.selection
  currentEditor.state.doc.nodesBetween(from, to, (node, pos) => {
    if (node.type.name === 'paragraph' || node.type.name === 'heading') {
      positions.push({ pos, attrs: node.attrs })
    }
  })
  return positions
}

const applyFirstLineIndentToSelection = (currentEditor: TipTapEditor, enabled: boolean) => {
  const positions = selectedParagraphPositions(currentEditor)
  if (!positions.length) {
    return currentEditor.commands.updateAttributes('paragraph', {
      firstLineIndent: enabled ? FIRST_LINE_INDENT_VALUE : null,
    })
  }

  const transaction = currentEditor.state.tr
  positions.forEach(({ pos, attrs }) => {
    transaction.setNodeMarkup(pos, undefined, {
      ...attrs,
      firstLineIndent: enabled ? FIRST_LINE_INDENT_VALUE : null,
    })
  })
  currentEditor.view.dispatch(transaction)
  return true
}

const selectionHasFirstLineIndent = (currentEditor: TipTapEditor) => {
  const positions = selectedParagraphPositions(currentEditor)
  if (!positions.length) {
    return currentEditor.isActive('paragraph', { firstLineIndent: FIRST_LINE_INDENT_VALUE })
  }
  return positions.every(({ attrs }) => attrs.firstLineIndent === FIRST_LINE_INDENT_VALUE)
}

const applyTextAlignToSelection = (currentEditor: TipTapEditor, align: TextAlignValue) => {
  const positions = selectedAlignableBlockPositions(currentEditor)
  if (!positions.length) {
    return currentEditor.commands.updateAttributes('paragraph', {
      textAlign: align === 'left' ? null : align,
    })
  }

  const transaction = currentEditor.state.tr
  positions.forEach(({ pos, attrs }) => {
    transaction.setNodeMarkup(pos, undefined, {
      ...attrs,
      textAlign: align === 'left' ? null : align,
    })
  })
  currentEditor.view.dispatch(transaction)
  return true
}

const selectionTextAlign = (currentEditor: TipTapEditor): TextAlignValue => {
  const positions = selectedAlignableBlockPositions(currentEditor)
  const align = positions[0]?.attrs.textAlign
  return align === 'center' || align === 'right' ? align : 'left'
}

const TabIndent = Extension.create({
  name: 'tabIndent',
  addKeyboardShortcuts() {
    return {
      Tab: () => applyFirstLineIndentToSelection(this.editor, true),
      'Shift-Tab': () => applyFirstLineIndentToSelection(this.editor, false),
    }
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
            parseHTML: (element: HTMLElement) => {
              const inlineAlign = element.style.textAlign?.trim()
              const dataAlign = element.getAttribute('data-text-align')?.trim()
              return inlineAlign || dataAlign || null
            },
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

const createEditorExtensions = (collaborative: boolean) => [
  StarterKit.configure({
    heading: {
      levels: [1, 2, 3],
    },
    ...(collaborative ? { undoRedo: false } : {}),
  }),
  FirstLineIndent,
  TextBlockAlign,
  TextStyleMark,
  AudioFileCard,
  ManagedLink.configure({
    openOnClick: true,
    HTMLAttributes: {
      rel: 'noopener noreferrer',
      target: '_blank',
    },
  }),
  ManagedImage.configure({
    allowBase64: false,
  }),
  Placeholder.configure({
    placeholder: props.placeholder,
  }),
  TaskList,
  TaskItem.configure({
    nested: true,
  }),
  TabIndent,
]

const renderCollaborationCaret = (user: Record<string, unknown>) => {
  const cursor = document.createElement('span')
  const color = String(user.color || '#9f4122')
  const name = String(user.name || 'Collaborator')
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
  style: `background-color: ${String(user.colorLight || 'rgba(159, 65, 34, 0.18)')}`,
})

const baseEditorExtensions = createEditorExtensions(Boolean(props.collaboration))
const editorExtensions = props.collaboration
  ? [
      ...baseEditorExtensions,
      Collaboration.configure({
        fragment: props.collaboration.fragment,
      }),
      CollaborationCaret.configure({
        provider: props.collaboration.provider,
        user: props.collaboration.user,
        render: renderCollaborationCaret,
        selectionRender: renderCollaborationSelection,
      }),
    ]
  : baseEditorExtensions
let collaborationBootstrapped = false

const parseReferenceId = (value: string | null) => {
  const parsedValue = Number(value)
  return Number.isFinite(parsedValue) && parsedValue > 0 ? parsedValue : null
}

const extractManagedFileReferences = (html: string): ManagedFileReference[] => {
  const dom = new DOMParser().parseFromString(`<div>${html}</div>`, 'text/html')
  const elements = Array.from(dom.querySelectorAll('[data-managed-file-id][data-attachment-id]'))
  const seen = new Set<string>()

  return elements.reduce<ManagedFileReference[]>((references, element) => {
    const fileId = parseReferenceId(element.getAttribute('data-managed-file-id'))
    const attachmentId = parseReferenceId(element.getAttribute('data-attachment-id'))
    if (!fileId || !attachmentId) {
      return references
    }

    const referenceKey = `${attachmentId}:${fileId}`
    if (seen.has(referenceKey)) {
      return references
    }
    seen.add(referenceKey)

    references.push({
      fileId,
      attachmentId,
      kind: element.tagName.toLowerCase() === 'img' ? 'image' : 'file',
    })
    return references
  }, [])
}

const emitManagedFileReferences = (html: string) => {
  emit('references-change', extractManagedFileReferences(html))
}

const writeHtmlToCollaborationFragment = (html: string) => {
  const fragment = props.collaboration?.fragment
  if (!fragment) {
    return
  }

  const schema = getSchema(baseEditorExtensions)
  const json = generateJSON(html, baseEditorExtensions)
  prosemirrorJSONToYXmlFragment(schema, json, fragment)
}

const collaborationFallbackHtml = (html: string) => {
  const body = new DOMParser().parseFromString(html, 'text/html').body
  const text = body.textContent?.trim()
  if (!text) {
    return ''
  }

  const fallbackDocument = document.implementation.createHTMLDocument('')
  const paragraph = fallbackDocument.createElement('p')
  paragraph.textContent = text
  return paragraph.outerHTML
}

const applyCollaborationBootstrap = (html?: string) => {
  if (!props.collaboration) {
    return
  }

  if (collaborationBootstrapped) {
    console.info('[NotaskCollab] editor bootstrap skipped: already applied')
    return
  }

  if (!html?.trim()) {
    console.info('[NotaskCollab] editor bootstrap skipped: empty content')
    return
  }

  if (props.collaboration.fragment.length > 0) {
    collaborationBootstrapped = true
    console.info('[NotaskCollab] editor bootstrap skipped: fragment already has content', {
      fragmentLength: props.collaboration.fragment.length,
    })
    return
  }

  try {
    const safeHtml = sanitizeEditorHtml(html)
    writeHtmlToCollaborationFragment(safeHtml)
    collaborationBootstrapped = true
    console.info('[NotaskCollab] editor bootstrap applied', {
      fragmentLength: props.collaboration.fragment.length,
      htmlLength: safeHtml.length,
    })
  } catch (error) {
    console.error('[NotaskCollab] editor bootstrap failed', error)
    collaborationBootstrapped = false
    const fallbackHtml = collaborationFallbackHtml(html)
    if (!fallbackHtml) {
      return
    }

    try {
      writeHtmlToCollaborationFragment(fallbackHtml)
      collaborationBootstrapped = true
      console.info('[NotaskCollab] editor bootstrap fallback applied', {
        fragmentLength: props.collaboration.fragment.length,
        htmlLength: fallbackHtml.length,
      })
    } catch (fallbackError) {
      console.error('[NotaskCollab] editor bootstrap fallback failed', fallbackError)
    }
  }
}

applyCollaborationBootstrap(props.collaboration?.bootstrapHtml)

const emitSelectionChange = () => {
  if (suppressSelectionEmit) {
    return
  }

  const currentEditor = editor.value
  if (!currentEditor) {
    return
  }

  const { from, to } = currentEditor.state.selection
  emit('selection-change', {
    from,
    to,
    updatedAt: Date.now(),
  })
}

let activeAudioElement: HTMLAudioElement | null = null
let activeAudioCard: HTMLElement | null = null
let editorDomElement: HTMLElement | null = null
let editorUnmounted = false

const mountedEditorDom = () => {
  const currentEditor = editor.value
  if (editorUnmounted || !currentEditor || currentEditor.isDestroyed) {
    return null
  }

  try {
    return currentEditor.view.dom
  } catch {
    return null
  }
}

const setAudioCardPlaying = (card: HTMLElement | null, playing: boolean) => {
  if (!card) {
    return
  }

  card.classList.toggle('is-playing', playing)
  const button = card.querySelector<HTMLButtonElement>('.notask-audio-play-button')
  button?.setAttribute('aria-label', playing ? '暂停音频' : '播放音频')
}

const stopActiveAudio = () => {
  if (activeAudioElement) {
    activeAudioElement.pause()
    activeAudioElement.currentTime = 0
  }
  setAudioCardPlaying(activeAudioCard, false)
  activeAudioElement = null
  activeAudioCard = null
}

const resolveAudioSource = async (card: HTMLElement) => {
  const directSource = card.dataset.url?.trim()
  if (directSource) {
    return directSource
  }

  const previewHref = card.dataset.previewHref?.trim()
  if (previewHref && !previewHref.startsWith('/app/files/preview/')) {
    card.dataset.url = previewHref
    return previewHref
  }

  const fileId = parseReferenceId(card.dataset.managedFileId || null)
  if (!fileId || !props.spaceId) {
    return ''
  }

  const previewFile = await fileService.previewUrl(props.spaceId, fileId)
  const previewSource = previewFile.downloadUrl || ''
  if (previewSource) {
    card.dataset.url = previewSource
  }
  return previewSource
}

const handleEditorContentClick = async (event: MouseEvent) => {
  if (editorUnmounted) {
    return
  }

  const target = event.target
  if (!(target instanceof HTMLElement)) {
    return
  }

  const button = target.closest('.notask-audio-play-button[data-audio-action="toggle"]')
  if (!(button instanceof HTMLButtonElement)) {
    return
  }

  event.preventDefault()
  event.stopPropagation()

  const card = button.closest('.notask-audio-card')
  if (!(card instanceof HTMLElement)) {
    return
  }

  if (activeAudioCard === card && activeAudioElement) {
    if (activeAudioElement.paused) {
      await activeAudioElement.play().catch(() => {
        ElMessage.warning('音频暂时无法播放')
      })
      setAudioCardPlaying(card, !activeAudioElement.paused)
    } else {
      activeAudioElement.pause()
      setAudioCardPlaying(card, false)
    }
    return
  }

  stopActiveAudio()

  const source = await resolveAudioSource(card)
  if (!source) {
    ElMessage.warning('音频地址暂不可用')
    return
  }

  const nextAudio = new Audio(source)
  nextAudio.preload = 'metadata'
  nextAudio.addEventListener('ended', () => {
    setAudioCardPlaying(card, false)
    if (activeAudioElement === nextAudio) {
      activeAudioElement = null
      activeAudioCard = null
    }
  })
  nextAudio.addEventListener('pause', () => {
    if (activeAudioElement === nextAudio && nextAudio.currentTime < nextAudio.duration) {
      setAudioCardPlaying(card, false)
    }
  })

  activeAudioElement = nextAudio
  activeAudioCard = card
  try {
    await nextAudio.play()
    setAudioCardPlaying(card, true)
  } catch {
    stopActiveAudio()
    ElMessage.warning('音频暂时无法播放')
  }
}

const editor = useEditor({
  editable: props.editable,
  ...(props.collaboration ? {} : { content: initialContent.value }),
  extensions: editorExtensions,
  editorProps: {
    handleDOMEvents: {
      keydown: (_view, event) => {
        if (event.key !== 'Tab' || !props.editable) {
          return false
        }

        const currentEditor = editor.value
        if (!currentEditor) {
          return false
        }

        event.preventDefault()
        event.stopPropagation()
        applyFirstLineIndentToSelection(currentEditor, !event.shiftKey)
        return true
      },
    },
  },
  onCreate({ editor: currentEditor }) {
    if (editorUnmounted) {
      return
    }
    editorDomElement = currentEditor.view.dom
    editorDomElement.addEventListener('click', handleEditorContentClick)
    emitManagedFileReferences(currentEditor.getHTML())
    nextTick(() => {
      if (!props.collaboration) {
        emitSelectionChange()
      }
      updateRemoteCursorOverlays()
    }).catch(() => undefined)
  },
  onUpdate({ editor: currentEditor }) {
    if (editorUnmounted || currentEditor.isDestroyed) {
      return
    }
    const html = sanitizeEditorHtml(currentEditor.getHTML())
    emit('update:modelValue', currentEditor.getText({ blockSeparator: '\n' }))
    emit('update:htmlValue', html)
    emitManagedFileReferences(html)
    scheduleRemoteCursorRefresh()
  },
  onSelectionUpdate() {
    if (editorUnmounted) {
      return
    }
    syncTextStyleState()
    if (!props.collaboration) {
      emitSelectionChange()
    }
    scheduleRemoteCursorRefresh()
  },
  onFocus() {
    emit('editor-focus')
    nextTick(() => {
      syncTextStyleState()
    }).catch(() => undefined)
  },
})

watch(
  () => props.editable,
  (editable) => {
    if (editorUnmounted) {
      return
    }
    editor.value?.setEditable(editable)
  },
)

watch(
  [selectedFontSize, selectedLineHeight],
  () => {
    if (editorUnmounted || suppressStyleEmit) {
      return
    }
    if (props.activeStyleTarget === 'title') {
      emitTitleStyleChange({
        fontSize: selectedFontSize.value,
        lineHeight: selectedLineHeight.value,
      })
      return
    }
    editorBaseFontSize.value = selectedFontSize.value
    editorBaseLineHeight.value = selectedLineHeight.value
    emit('style-change', {
      fontSize: selectedFontSize.value,
      lineHeight: selectedLineHeight.value,
    })
  },
  { immediate: true },
)

watch(
  () => [props.activeStyleTarget, props.titleStyle?.color, props.titleStyle?.fontFamily, props.titleStyle?.fontSize, props.titleStyle?.lineHeight, props.titleStyle?.textAlign],
  () => {
    if (editorUnmounted || props.activeStyleTarget !== 'title') {
      return
    }
    syncTitleStyleState()
  },
  { immediate: true },
)

watch(
  () => [props.htmlValue, props.modelValue],
  () => {
    if (editorUnmounted) {
      return
    }
    if (props.collaboration) {
      return
    }

    const currentEditor = editor.value
    if (!currentEditor) {
      return
    }
    const nextContent = initialContent.value
    if (currentEditor.getHTML() === nextContent) {
      emitManagedFileReferences(currentEditor.getHTML())
      return
    }
    const wasFocused = currentEditor.isFocused
    const previousSelection = currentEditor.state.selection
    suppressSelectionEmit = true
    currentEditor.commands.setContent(nextContent, { emitUpdate: false })
    if (wasFocused) {
      const maxPosition = currentEditor.state.doc.content.size
      currentEditor.commands.setTextSelection({
        from: Math.max(0, Math.min(previousSelection.from, maxPosition)),
        to: Math.max(0, Math.min(previousSelection.to, maxPosition)),
      })
    }
    emitManagedFileReferences(currentEditor.getHTML())
    nextTick(() => {
      suppressSelectionEmit = false
      if (wasFocused) {
        emitSelectionChange()
      }
      updateRemoteCursorOverlays()
    }).catch(() => {
      suppressSelectionEmit = false
    })
  },
)

watch(
  () => props.collaboration?.bootstrapHtml,
  (html) => {
    if (editorUnmounted) {
      return
    }
    applyCollaborationBootstrap(html)
  },
  { immediate: true },
)

watch(
  () => props.remoteCursors,
  () => {
    if (editorUnmounted) {
      return
    }
    scheduleRemoteCursorRefresh()
  },
  { deep: true },
)

const toolClass = (name: string, attributes?: Record<string, unknown>) =>
  editor.value?.isActive(name, attributes) ? 'tiptap-tool-active' : ''

const firstLineIndentToolClass = computed(() =>
  editor.value && selectionHasFirstLineIndent(editor.value) ? 'tiptap-tool-active' : '',
)

const syncTextStyleState = () => {
  const currentEditor = editor.value
  if (!currentEditor || props.activeStyleTarget === 'title') {
    return
  }

  selectedTextAlign.value = selectionTextAlign(currentEditor)
  const attrs = currentEditor.getAttributes('textStyle')
  const color = typeof attrs.color === 'string' ? attrs.color : ''
  const fontFamily = typeof attrs.fontFamily === 'string' ? attrs.fontFamily : ''
  const fontSize = typeof attrs.fontSize === 'string' ? attrs.fontSize : ''
  const colorOption = fontColorOptions.find((option) => option.value.toLowerCase() === color.toLowerCase())
  const fontOption = fontFamilyOptions.find((option) => option.cssValue === fontFamily)
  const fontSizeOption = fontSizeOptions.find((option) => option.value === fontSize)

  suppressStyleEmit = true
  if (colorOption) {
    selectedFontColor.value = colorOption.value
    customFontColor.value = colorOption.value
  }
  if (fontOption) {
    selectedFontFamily.value = fontOption.value
  } else {
    selectedFontFamily.value = 'system'
  }
  if (fontSizeOption) {
    selectedFontSize.value = fontSizeOption.value
  } else {
    selectedFontSize.value = editorBaseFontSize.value
  }
  nextTick(() => {
    suppressStyleEmit = false
  }).catch(() => {
    suppressStyleEmit = false
  })
}

const runCommand = (command: ChainCommand) => {
  const chain = editor.value?.chain().focus()
  if (!chain) {
    return
  }
  chain[command]().run()
}

const setHeading = (level: 1 | 2 | 3) => {
  editor.value?.chain().focus().toggleHeading({ level }).run()
}

const toggleFirstLineIndent = () => {
  const currentEditor = editor.value
  if (!currentEditor) {
    return
  }
  currentEditor.chain().focus().run()
  applyFirstLineIndentToSelection(currentEditor, !selectionHasFirstLineIndent(currentEditor))
}

const applyTextAlign = (align: TextAlignValue) => {
  if (props.activeStyleTarget === 'title') {
    selectedTextAlign.value = align
    emitTitleStyleChange({ textAlign: align })
    return
  }

  const currentEditor = editor.value
  if (!currentEditor) {
    return
  }

  currentEditor.chain().focus().run()
  applyTextAlignToSelection(currentEditor, align)
  selectedTextAlign.value = align
}

const applyFontFamily = () => {
  if (props.activeStyleTarget === 'title') {
    emitTitleStyleChange({ fontFamily: selectedFontFamilyOption.value.cssValue })
    return
  }

  const currentEditor = editor.value
  if (!currentEditor) {
    return
  }
  const attrs = currentEditor.getAttributes('textStyle')
  currentEditor
    .chain()
    .focus()
    .setMark('textStyle', {
      ...attrs,
      fontFamily: selectedFontFamilyOption.value.cssValue,
    })
    .run()
}

const applyFontSize = () => {
  if (props.activeStyleTarget === 'title') {
    emitTitleStyleChange({ fontSize: selectedFontSize.value })
    return
  }

  const currentEditor = editor.value
  if (!currentEditor) {
    return
  }
  const attrs = currentEditor.getAttributes('textStyle')
  currentEditor
    .chain()
    .focus()
    .setMark('textStyle', {
      ...attrs,
      fontSize: selectedFontSize.value,
    })
    .run()
}

const toggleColorPanel = () => {
  if (!props.editable) {
    return
  }
  customFontColor.value = selectedFontColor.value
  if (colorPanelOpen.value) {
    colorPanelOpen.value = false
    return
  }

  updateColorPanelPosition()
  colorPanelOpen.value = true
}

const updateColorPanelPosition = () => {
  const trigger = colorTriggerRef.value
  if (!trigger || typeof window === 'undefined') {
    colorPanelStyle.value = {}
    return
  }

  const rect = trigger.getBoundingClientRect()
  const panelWidth = 218
  const viewportPadding = 12
  const left = Math.min(
    window.innerWidth - panelWidth - viewportPadding,
    Math.max(viewportPadding, rect.left + rect.width / 2 - panelWidth / 2),
  )

  colorPanelStyle.value = {
    left: `${left}px`,
    top: `${rect.bottom + 8}px`,
  }
}

const applyFontColor = (color: string) => {
  selectedFontColor.value = color
  customFontColor.value = color
  if (props.activeStyleTarget === 'title') {
    emitTitleStyleChange({ color })
    return
  }

  const currentEditor = editor.value
  if (!currentEditor) {
    return
  }
  const attrs = currentEditor.getAttributes('textStyle')
  currentEditor
    .chain()
    .focus()
    .setMark('textStyle', {
      ...attrs,
      color,
    })
    .run()
}

const insertDivider = () => {
  editor.value?.chain().focus().setHorizontalRule().run()
}

const openLinkPanel = () => {
  const currentEditor = editor.value
  if (!currentEditor) {
    return
  }

  const { from, to } = currentEditor.state.selection
  selectedText.value = currentEditor.state.doc.textBetween(from, to, ' ')
  linkText.value = selectedText.value
  linkHref.value = currentEditor.getAttributes('link').href || ''
  linkPanelOpen.value = true
}

const normalizeHref = (href: string) => {
  const trimmedHref = href.trim()
  if (!trimmedHref) {
    return ''
  }
  if (/^(https?:|mailto:|tel:|\/)/i.test(trimmedHref)) {
    return trimmedHref
  }
  return `https://${trimmedHref}`
}

const applyLink = () => {
  const currentEditor = editor.value
  if (!currentEditor) {
    return
  }

  const href = normalizeHref(linkHref.value)
  if (!href) {
    clearLink()
    return
  }

  const { empty } = currentEditor.state.selection
  if (empty && linkText.value.trim()) {
    currentEditor
      .chain()
      .focus()
      .insertContent(`<a href="${href}">${escapeHtml(linkText.value.trim())}</a>`)
      .run()
  } else {
    currentEditor.chain().focus().extendMarkRange('link').setLink({ href }).run()
  }
  linkPanelOpen.value = false
}

const clearLink = () => {
  editor.value?.chain().focus().extendMarkRange('link').unsetLink().run()
  linkPanelOpen.value = false
}

const triggerImageUpload = () => {
  imageInputRef.value?.click()
}

const handleImageSelected = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !props.spaceId) {
    input.value = ''
    return
  }
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    input.value = ''
    return
  }

  uploadingImage.value = true
  try {
    const managedFile = await fileService.editorUpload(props.spaceId, file)
    const imageSource = managedFile.downloadUrl || ''
    if (imageSource) {
      const name = managedFile.displayName || managedFile.fileName || file.name
      editor.value
        ?.chain()
        .focus()
        .insertContent(
          `<img src="${escapeHtml(imageSource)}" alt="${escapeHtml(name)}" title="${escapeHtml(name)}" data-managed-file-id="${managedFile.id}" data-attachment-id="${managedFile.attachmentId}" />`,
        )
        .run()
    }
    emit('file-inserted', managedFile)
  } finally {
    uploadingImage.value = false
    input.value = ''
  }
}

const escapeHtml = (value: string) =>
  value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')

const imageExtensions = new Set(['apng', 'avif', 'gif', 'jpeg', 'jpg', 'png', 'svg', 'webp'])

const managedFileCandidateValues = (file: ManagedFile) => [
  file.mimeType || '',
  file.fileName || '',
  file.displayName || '',
  file.downloadUrl || '',
  file.previewUrl || '',
]

const fileExtension = (file: ManagedFile) => {
  const candidate = managedFileCandidateValues(file).find((value) => extensionFromValue(value))
  return candidate ? extensionFromValue(candidate) : ''
}

const isImageFile = (file: ManagedFile) => Boolean(file.mimeType?.startsWith('image/') || imageExtensions.has(fileExtension(file)))

const isAudioFile = (file: ManagedFile) =>
  Boolean(file.mimeType?.startsWith('audio/') || managedFileCandidateValues(file).some((value) => audioExtensions.has(extensionFromValue(value))))

const managedFileMimeType = (file: ManagedFile) => {
  if (file.mimeType) {
    return file.mimeType
  }
  return isAudioFile(file) ? 'audio/*' : ''
}

const clampEditorPosition = (position: number) => {
  const currentEditor = editor.value
  if (!currentEditor) {
    return 0
  }

  return Math.max(0, Math.min(position, currentEditor.state.doc.content.size))
}

const selectionStylesForRange = (from: number, to: number, colorLight: string) => {
  const currentEditor = editor.value
  const scrollElement = contentScrollRef.value
  if (!currentEditor || currentEditor.isDestroyed || !scrollElement || from === to) {
    return []
  }

  const selectionFrom = clampEditorPosition(Math.min(from, to))
  const selectionTo = clampEditorPosition(Math.max(from, to))
  if (selectionFrom === selectionTo) {
    return []
  }

  try {
    const range = document.createRange()
    const startPosition = currentEditor.view.domAtPos(selectionFrom)
    const endPosition = currentEditor.view.domAtPos(selectionTo)
    range.setStart(startPosition.node, startPosition.offset)
    range.setEnd(endPosition.node, endPosition.offset)

    const containerRect = scrollElement.getBoundingClientRect()
    const styles = Array.from(range.getClientRects()).map((rect) => ({
      backgroundColor: colorLight,
      height: `${Math.max(rect.height, 16)}px`,
      left: `${rect.left - containerRect.left + scrollElement.scrollLeft}px`,
      opacity: '0.42',
      top: `${rect.top - containerRect.top + scrollElement.scrollTop}px`,
      width: `${Math.max(rect.width, 2)}px`,
    }))
    range.detach()
    return styles
  } catch {
    return []
  }
}

const updateRemoteCursorOverlays = () => {
  const currentEditor = editor.value
  const scrollElement = contentScrollRef.value
  if (!currentEditor || currentEditor.isDestroyed || !scrollElement) {
    remoteCursorOverlays.value = []
    return
  }

  const containerRect = scrollElement.getBoundingClientRect()
  remoteCursorOverlays.value = props.remoteCursors.flatMap((cursor) => {
    const position = clampEditorPosition(cursor.to)
    try {
      const coords = currentEditor.view.coordsAtPos(position)
      const left = coords.left - containerRect.left + scrollElement.scrollLeft
      const top = coords.top - containerRect.top + scrollElement.scrollTop
      const height = Math.max(coords.bottom - coords.top, 18)

      return [
        {
          id: `${cursor.clientId}-${cursor.userId}`,
          name: cursor.name,
          cursorStyle: {
            backgroundColor: cursor.color,
            height: `${height}px`,
            left: `${left}px`,
            top: `${top}px`,
          },
          labelStyle: {
            backgroundColor: cursor.color,
            color: '#ffffff',
          },
          selectionStyles: selectionStylesForRange(cursor.from, cursor.to, cursor.colorLight),
        },
      ]
    } catch {
      return []
    }
  })
}

const scheduleRemoteCursorRefresh = () => {
  if (editorUnmounted) {
    return
  }

  if (remoteCursorFrame) {
    window.cancelAnimationFrame(remoteCursorFrame)
  }

  remoteCursorFrame = window.requestAnimationFrame(() => {
    remoteCursorFrame = null
    updateRemoteCursorOverlays()
  })
}

const insertManagedFile = (file: ManagedFile) => {
  const name = file.displayName || file.fileName || `File #${file.id}`
  const href = `/app/files/preview/${file.id}`
  const mimeType = managedFileMimeType(file)
  const currentEditor = editor.value
  if (!currentEditor) {
    return
  }

  if (isImageFile(file) && file.downloadUrl) {
    currentEditor
      .chain()
      .focus()
      .insertContent(
        `<img src="${escapeHtml(file.downloadUrl)}" alt="${escapeHtml(name)}" title="${escapeHtml(name)}" data-managed-file-id="${file.id}" data-attachment-id="${file.attachmentId}" />`,
      )
      .run()
    return
  }

  if (isAudioFile(file)) {
    const source = file.downloadUrl || file.previewUrl || ''
    currentEditor
      .chain()
      .focus()
      .insertContent([
        {
          type: 'audioFileCard',
          attrs: {
            attachmentId: String(file.attachmentId),
            managedFileId: String(file.id),
            mimeType,
            name,
            previewHref: href,
            source,
          },
        },
        {
          type: 'paragraph',
        },
      ])
      .run()
    return
  }

  currentEditor
    .chain()
    .focus()
    .insertContent(
      `<a href="${href}" data-managed-file-id="${file.id}" data-attachment-id="${file.attachmentId}" data-name="${escapeHtml(name)}" data-mime="${escapeHtml(mimeType)}" data-type="file">${escapeHtml(name)}</a>`,
    )
    .run()
}

const focus = () => {
  editor.value?.chain().focus().run()
}

defineExpose({
  focus,
  insertManagedFile,
})

onBeforeUnmount(() => {
  editorUnmounted = true
  if (remoteCursorFrame) {
    window.cancelAnimationFrame(remoteCursorFrame)
    remoteCursorFrame = null
  }
  const domElement = editorDomElement || mountedEditorDom()
  domElement?.removeEventListener('click', handleEditorContentClick)
  editorDomElement = null
  stopActiveAudio()
})
</script>

<style scoped>
.tiptap-toolbar-shell {
  flex-shrink: 0;
  position: relative;
}

.tiptap-toolbar-shell-external {
  width: 100%;
}

.tiptap-toolbar {
  flex-wrap: nowrap;
  overflow-x: auto;
  overflow-y: hidden;
  overscroll-behavior-inline: contain;
  scrollbar-width: none;
  scroll-snap-type: x proximity;
  white-space: nowrap;
}

.tiptap-toolbar::-webkit-scrollbar {
  display: none;
}

.tiptap-toolbar > * {
  flex: 0 0 auto;
  scroll-snap-align: start;
}

.tiptap-tool {
  align-items: center;
  border-radius: 0.75rem;
  color: var(--on-surface-variant);
  display: inline-flex;
  gap: 0.35rem;
  height: 34px;
  justify-content: center;
  min-width: 34px;
  padding: 0 0.45rem;
  transition: all 0.18s ease;
}

.tiptap-tool:hover:not(:disabled),
.tiptap-tool-active {
  background: var(--primary-fixed);
  color: var(--primary);
}

.tiptap-tool:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.tiptap-tool .material-symbols-outlined {
  font-size: 19px;
}

.tiptap-file-tool {
  border: 1px solid var(--outline-variant);
  padding-inline: 0.65rem;
}

.tiptap-select {
  appearance: none;
  background-color: var(--surface-container-lowest);
  background-image:
    linear-gradient(45deg, transparent 50%, var(--on-surface-variant) 50%),
    linear-gradient(135deg, var(--on-surface-variant) 50%, transparent 50%);
  background-position:
    calc(100% - 14px) 13px,
    calc(100% - 9px) 13px;
  background-repeat: no-repeat;
  background-size: 5px 5px, 5px 5px;
  border: 1px solid rgba(221, 192, 184, 0.68);
  border-radius: 9999px;
  box-shadow: 0 6px 16px rgba(74, 55, 47, 0.05);
  color: var(--on-surface-variant);
  cursor: pointer;
  font-size: 12px;
  font-weight: 700;
  height: 34px;
  outline: none;
  padding: 0 1.55rem 0 0.8rem;
  transition: all 0.18s ease;
}

.tiptap-select:hover:not(:disabled) {
  border-color: rgba(159, 65, 34, 0.42);
  color: var(--primary);
}

.tiptap-select:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(159, 65, 34, 0.12);
}

.tiptap-select:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.tiptap-font-select {
  min-width: 126px;
  max-width: 148px;
}

.tiptap-size-select {
  min-width: 76px;
}

.tiptap-line-select {
  min-width: 72px;
}

.tiptap-align-group {
  align-items: center;
  background: var(--surface-container-lowest);
  border: 1px solid rgba(221, 192, 184, 0.65);
  border-radius: 9999px;
  display: inline-flex;
  gap: 0.12rem;
  height: 32px;
  padding: 0 0.22rem;
}

.tiptap-align-tool {
  border-radius: 9999px;
  height: 26px;
  min-width: 28px;
  padding: 0;
}

.tiptap-align-tool .material-symbols-outlined {
  font-size: 18px;
}

.tiptap-color-picker {
  position: relative;
}

.tiptap-color-trigger {
  align-items: center;
  background: var(--surface-container-lowest);
  border: 1px solid rgba(221, 192, 184, 0.65);
  border-radius: 9999px;
  color: var(--on-surface-variant);
  display: inline-flex;
  gap: 0.35rem;
  height: 34px;
  justify-content: center;
  min-width: 48px;
  padding: 0 0.52rem;
  transition: all 0.18s ease;
}

.tiptap-color-trigger:hover:not(:disabled) {
  background: var(--primary-fixed);
  border-color: transparent;
  color: var(--primary);
}

.tiptap-color-trigger:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.tiptap-color-trigger .material-symbols-outlined {
  font-size: 18px;
}

.tiptap-color-current {
  border: 2px solid transparent;
  border-radius: 9999px;
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.12), 0 0 0 1px rgba(255, 255, 255, 0.78);
  height: 16px;
  width: 16px;
}

.tiptap-color-panel {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(221, 192, 184, 0.62);
  border-radius: 1rem;
  box-shadow: 0 18px 45px rgba(74, 55, 47, 0.16);
  display: grid;
  gap: 0.65rem;
  padding: 0.78rem;
  position: fixed;
  width: 218px;
  z-index: 80;
}

.tiptap-color-panel::before {
  background: rgba(255, 255, 255, 0.96);
  border-left: 1px solid rgba(221, 192, 184, 0.62);
  border-top: 1px solid rgba(221, 192, 184, 0.62);
  content: '';
  height: 10px;
  left: 50%;
  position: absolute;
  top: -6px;
  transform: translateX(-50%) rotate(45deg);
  width: 10px;
}

.tiptap-color-panel-title {
  color: var(--on-surface-variant);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.tiptap-color-grid {
  display: grid;
  gap: 0.5rem;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.tiptap-color-option {
  border: 2px solid transparent;
  border-radius: 9999px;
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.1);
  height: 28px;
  transition: transform 0.16s ease, box-shadow 0.16s ease;
  width: 28px;
}

.tiptap-color-option:hover,
.tiptap-color-option-active {
  border-color: #ffffff;
  box-shadow: 0 0 0 2px var(--primary), inset 0 0 0 1px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.tiptap-color-custom {
  align-items: center;
  background: var(--surface-container-lowest);
  border: 1px solid rgba(221, 192, 184, 0.55);
  border-radius: 0.8rem;
  color: var(--on-surface-variant);
  display: flex;
  font-size: 12px;
  font-weight: 700;
  justify-content: space-between;
  padding: 0.48rem 0.6rem;
}

.tiptap-color-custom input {
  background: transparent;
  border: 0;
  cursor: pointer;
  height: 28px;
  padding: 0;
  width: 42px;
}

.tiptap-divider {
  background: var(--outline-variant);
  height: 22px;
  margin: 0 0.25rem;
  opacity: 0.35;
  width: 1px;
}

.tiptap-before-content {
  padding: 0;
}

.tiptap-after-toolbar {
  flex-shrink: 0;
}

.tiptap-link-panel {
  align-items: center;
  background: linear-gradient(135deg, rgba(255, 248, 246, 0.96), rgba(245, 236, 233, 0.96));
  border-bottom: 1px solid rgba(221, 192, 184, 0.45);
  display: grid;
  gap: 0.65rem;
  grid-template-columns: minmax(120px, 0.8fr) minmax(180px, 1.2fr) auto auto auto;
  padding: 0.75rem 1rem;
}

.tiptap-link-input {
  background: var(--surface-container-lowest);
  border: 1px solid rgba(221, 192, 184, 0.75);
  border-radius: 9999px;
  color: var(--on-surface);
  font-size: 13px;
  height: 36px;
  outline: none;
  padding: 0 0.85rem;
  transition: all 0.18s ease;
}

.tiptap-link-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(159, 65, 34, 0.12);
}

.tiptap-link-action {
  align-items: center;
  background: var(--surface-container-lowest);
  border: 1px solid rgba(221, 192, 184, 0.75);
  border-radius: 9999px;
  color: var(--on-surface-variant);
  display: inline-flex;
  height: 36px;
  justify-content: center;
  width: 36px;
}

.tiptap-link-action.primary {
  background: var(--primary);
  border-color: var(--primary);
  color: var(--on-primary);
}

.tiptap-content {
  color: var(--on-surface);
  padding: 1rem 2.5rem 2rem;
}

.tiptap-content-inner {
  min-height: 100%;
}

.tiptap-content :deep(.ProseMirror) {
  min-height: 100%;
  outline: none;
  white-space: pre-wrap;
}

.tiptap-remote-cursor {
  border-radius: 9999px;
  pointer-events: none;
  position: absolute;
  transform: translateX(-50%);
  width: 2px;
  z-index: 8;
}

.tiptap-remote-label {
  border-radius: 9999px;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.12);
  font-size: 11px;
  font-weight: 700;
  left: 0;
  line-height: 1;
  max-width: 140px;
  overflow: hidden;
  padding: 0.32rem 0.55rem;
  position: absolute;
  text-overflow: ellipsis;
  top: -1.65rem;
  transform: translateX(-0.25rem);
  white-space: nowrap;
}

.tiptap-remote-selection {
  border-radius: 0.35rem;
  pointer-events: none;
  position: absolute;
  z-index: 3;
}

.tiptap-content :deep(.collaboration-carets__caret) {
  border-left: 2px solid;
  border-radius: 9999px;
  margin-left: -1px;
  margin-right: -1px;
  pointer-events: none;
  position: relative;
  word-break: normal;
}

.tiptap-content :deep(.collaboration-carets__label) {
  border-radius: 9999px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.14);
  color: #ffffff;
  font-size: 11px;
  font-weight: 750;
  left: -0.35rem;
  line-height: 1;
  max-width: 150px;
  overflow: hidden;
  padding: 0.34rem 0.58rem;
  position: absolute;
  text-overflow: ellipsis;
  top: -1.85rem;
  white-space: nowrap;
}

.tiptap-content :deep(.collaboration-carets__selection) {
  border-radius: 0.35rem;
  opacity: 0.42;
}

.tiptap-content :deep(.ProseMirror p.is-editor-empty:first-child::before) {
  color: #a69a95;
  content: attr(data-placeholder);
  float: left;
  height: 0;
  pointer-events: none;
}

.tiptap-content :deep(h1) {
  font-family: Newsreader, serif;
  font-size: 2rem;
  font-weight: 650;
  line-height: 1.25;
  margin: 1.4rem 0 0.75rem;
}

.tiptap-content :deep(h2) {
  font-family: Newsreader, serif;
  font-size: 1.55rem;
  font-weight: 620;
  margin: 1.25rem 0 0.65rem;
}

.tiptap-content :deep(h3) {
  font-size: 1.15rem;
  font-weight: 700;
  margin: 1rem 0 0.5rem;
}

.tiptap-content :deep(p),
.tiptap-content :deep(li) {
  font-size: var(--tiptap-font-size, 16px);
  line-height: var(--tiptap-line-height, 1.5);
}

.tiptap-content :deep(p) {
  margin: 0 0 var(--tiptap-block-gap, 0.63rem);
  min-height: 1.5em;
}

.tiptap-content :deep(p:last-child) {
  margin-bottom: 0;
}

.tiptap-content :deep(p[data-first-line-indent='true']) {
  text-indent: 2em;
}

.tiptap-content :deep(p[data-text-align='center']),
.tiptap-content :deep(h1[data-text-align='center']),
.tiptap-content :deep(h2[data-text-align='center']),
.tiptap-content :deep(h3[data-text-align='center']) {
  text-align: center;
}

.tiptap-content :deep(p[data-text-align='right']),
.tiptap-content :deep(h1[data-text-align='right']),
.tiptap-content :deep(h2[data-text-align='right']),
.tiptap-content :deep(h3[data-text-align='right']) {
  text-align: right;
}

.tiptap-content :deep(p:empty::before) {
  content: '\00a0';
}

.tiptap-content :deep(ul),
.tiptap-content :deep(ol) {
  margin: var(--tiptap-list-gap, 0.42rem) 0 var(--tiptap-block-gap, 0.63rem);
  padding-left: 1.45rem;
}

.tiptap-content :deep(li) {
  margin: calc(var(--tiptap-list-gap, 0.42rem) * 0.55) 0;
}

.tiptap-content :deep(blockquote) {
  border-left: 4px solid var(--primary);
  color: var(--on-surface-variant);
  margin: 1rem 0;
  padding-left: 1rem;
}

.tiptap-content :deep(blockquote p) {
  line-height: var(--tiptap-line-height, 1.5);
  margin-bottom: calc(var(--tiptap-block-gap, 0.63rem) * 0.8);
}

.tiptap-content :deep(blockquote p:last-child) {
  margin-bottom: 0;
}

.tiptap-content :deep(pre) {
  background: #241f1d;
  border-radius: 1rem;
  color: #fff;
  padding: 1rem;
}

.tiptap-content :deep(a) {
  color: var(--primary);
  font-weight: 650;
  text-decoration: underline;
  text-decoration-thickness: 1px;
  text-underline-offset: 3px;
}

.tiptap-content :deep(img) {
  border-radius: 1.25rem;
  box-shadow: 0 14px 36px rgba(0, 0, 0, 0.08);
  display: block;
  margin: 1.25rem auto;
  max-width: 100%;
}

.tiptap-content :deep(.notask-audio-card) {
  align-items: center;
  background: linear-gradient(135deg, var(--surface-container-lowest), var(--surface-container-low));
  border: 1px solid rgba(221, 192, 184, 0.52);
  border-radius: 1.15rem;
  box-shadow: 0 14px 34px rgba(74, 55, 47, 0.09);
  display: grid;
  gap: 0.8rem;
  grid-template-columns: auto minmax(0, 1fr) auto;
  margin: 1rem 0;
  max-width: 100%;
  padding: 0.78rem 0.9rem;
  width: min(100%, 560px);
}

.tiptap-content :deep(.notask-audio-card.ProseMirror-selectednode) {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(159, 65, 34, 0.12), 0 14px 34px rgba(74, 55, 47, 0.1);
}

.tiptap-content :deep(.notask-audio-play-button) {
  align-items: center;
  background: var(--primary);
  border: 0;
  border-radius: 9999px;
  box-shadow: 0 10px 22px rgba(159, 65, 34, 0.24);
  color: var(--on-primary);
  cursor: pointer;
  display: inline-flex;
  height: 40px;
  justify-content: center;
  transition: transform 0.16s ease, box-shadow 0.16s ease, background-color 0.16s ease;
  width: 40px;
}

.tiptap-content :deep(.notask-audio-play-button:hover) {
  box-shadow: 0 12px 26px rgba(159, 65, 34, 0.3);
  transform: translateY(-1px);
}

.tiptap-content :deep(.notask-audio-play-icon::before) {
  content: 'play_arrow';
  font-family: 'Material Symbols Outlined';
  font-size: 24px;
  font-variation-settings: 'FILL' 1, 'wght' 500, 'GRAD' 0, 'opsz' 24;
  line-height: 1;
}

.tiptap-content :deep(.notask-audio-card.is-playing .notask-audio-play-icon::before) {
  content: 'pause';
}

.tiptap-content :deep(.notask-audio-main) {
  display: flex;
  flex-direction: column;
  gap: 0.18rem;
  min-width: 0;
}

.tiptap-content :deep(.notask-audio-name) {
  color: var(--on-surface);
  display: block;
  font-size: 0.92rem;
  font-weight: 760;
  line-height: 1.25;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tiptap-content :deep(.notask-audio-type) {
  color: var(--on-surface-variant);
  font-size: 0.74rem;
  font-weight: 650;
  line-height: 1.2;
}

.tiptap-content :deep(.notask-audio-wave) {
  align-items: center;
  display: inline-flex;
  gap: 3px;
  height: 28px;
  justify-content: flex-end;
  width: 58px;
}

.tiptap-content :deep(.notask-audio-wave span) {
  animation: notask-audio-wave 0.9s ease-in-out infinite;
  animation-play-state: paused;
  background: color-mix(in srgb, var(--primary) 72%, var(--surface-container-lowest));
  border-radius: 9999px;
  display: block;
  height: 9px;
  opacity: 0.55;
  transform-origin: center;
  width: 4px;
}

.tiptap-content :deep(.notask-audio-wave span:nth-child(2)) {
  animation-delay: 0.08s;
  height: 15px;
}

.tiptap-content :deep(.notask-audio-wave span:nth-child(3)) {
  animation-delay: 0.16s;
  height: 22px;
}

.tiptap-content :deep(.notask-audio-wave span:nth-child(4)) {
  animation-delay: 0.24s;
  height: 14px;
}

.tiptap-content :deep(.notask-audio-wave span:nth-child(5)) {
  animation-delay: 0.32s;
  height: 18px;
}

.tiptap-content :deep(.notask-audio-card.is-playing .notask-audio-wave span) {
  animation-play-state: running;
  opacity: 0.92;
}

@keyframes notask-audio-wave {
  0%,
  100% {
    transform: scaleY(0.58);
  }

  50% {
    transform: scaleY(1.08);
  }
}

@media (max-width: 900px) {
  .tiptap-toolbar {
    gap: 0.35rem;
    padding: 0.6rem;
  }

  .tiptap-tool {
    height: 31px;
    min-width: 31px;
  }

  .tiptap-divider {
    display: none;
  }

  .tiptap-link-panel {
    grid-template-columns: 1fr;
  }

  .tiptap-link-action {
    width: 100%;
  }

  .tiptap-content :deep(.notask-audio-card) {
    grid-template-columns: auto minmax(0, 1fr);
    width: 100%;
  }

  .tiptap-content :deep(.notask-audio-wave) {
    display: none;
  }
}
</style>
