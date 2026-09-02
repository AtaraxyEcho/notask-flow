<template>
  <div
    class="relative flex min-h-0 flex-1 flex-col overflow-hidden rounded-[1.25rem] border border-outline-variant/20 bg-[#F9F7F5]"
    @keydown.capture="handleSaveShortcut"
  >
    <TipTapRichEditor
      ref="richEditorRef"
      v-model="editorText"
      v-model:html-value="editorHtml"
      class="h-full min-h-0"
      :editable="canEdit"
      :file-button-label="fileButtonLabel"
      :collaboration="collaborationConfig"
      :placeholder="placeholder || t('collab.placeholder')"
      :remote-cursors="remoteCursors"
      :space-id="spaceId"
      :toolbar-target="toolbarTarget"
      :active-style-target="activeStyleTarget"
      :title-style="titleStyle"
      @file-inserted="$emit('file-inserted', $event)"
      @editor-focus="$emit('editor-focus')"
      @references-change="$emit('references-change', $event)"
      @request-file="$emit('request-file')"
      @selection-change="handleSelectionChange"
      @style-change="handleStyleChange"
      @title-style-change="$emit('title-style-change', $event)"
    >
      <template v-if="$slots['toolbar-extra']" #toolbar-extra>
        <slot name="toolbar-extra" />
      </template>

      <template #after-toolbar>
        <div class="flex flex-wrap items-center justify-between gap-3 border-b border-outline-variant/15 bg-white/70 px-4 py-3">
          <div class="flex items-center gap-2 text-sm text-stone-500">
            <span class="material-symbols-outlined text-[18px] text-primary">groups</span>
            <span>{{ statusMessage }}</span>
          </div>

          <div class="flex flex-wrap items-center justify-end gap-3">
            <div class="flex flex-wrap items-center justify-end gap-2">
              <span class="text-[11px] font-bold uppercase tracking-[0.18em] text-stone-400">{{ t('collab.editing') }}</span>
              <span
                v-for="user in editingUsers"
                :key="`${user.userId}-${user.name}`"
                class="inline-flex items-center gap-2 rounded-full border px-3 py-1 text-[11px] font-bold tracking-[0.12em]"
                :style="{
                  borderColor: user.color,
                  color: user.color,
                  backgroundColor: user.colorLight,
                }"
              >
                <span class="inline-flex h-2.5 w-2.5 rounded-full" :style="{ backgroundColor: user.color }"></span>
                {{ user.name }}
              </span>
            </div>
          </div>
        </div>
      </template>

      <template v-if="$slots['before-content']" #before-content>
        <slot name="before-content" />
      </template>
    </TipTapRichEditor>

      <div
        v-if="showConnectionNotice"
        class="pointer-events-none absolute right-4 top-4 z-10 max-w-[320px]"
      >
        <div class="rounded-2xl border border-outline-variant/20 bg-white/90 px-4 py-3 text-sm text-stone-600 shadow-sm backdrop-blur-sm">
          {{ statusMessage }}
        </div>
      </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import * as Y from 'yjs'
import { noteService } from '@/api/services'
import TipTapRichEditor from '@/components/notes/TipTapRichEditor.vue'
import {
  buildPresenceColor,
  NoteCollabProvider,
  type CollabConnectionStatus,
  type CollabPresenceUser,
} from '@/collab/NoteCollabProvider'
import { useI18n } from '@/i18n'
import { useUserStore } from '@/stores/user'
import type { ManagedFile, Note, UserProfile } from '@/types/app'
import { resolveCollabWsUrl } from '@/utils/collabWs'
import { renderMarkdownLite } from '@/utils/markdown'
import { sanitizeEditorHtml } from '@/utils/sanitize'

type EditorFileReference = {
  fileId: number
  attachmentId: number
  kind: 'file' | 'image'
}

type TipTapEditorHandle = {
  focus: () => void
  insertManagedFile: (file: ManagedFile) => void
}

type EditorSelectionPayload = {
  from: number
  to: number
  updatedAt: number
}

type RemoteCursor = CollabPresenceUser & {
  clientId: number
  from: number
  to: number
}

type CollabAwarenessState = {
  cursor?: EditorSelectionPayload
  user?: CollabPresenceUser
}

const AUTO_SAVE_DELAY_MS = 3000

const props = defineProps<{
  canEdit: boolean
  fileButtonLabel?: string
  htmlValue?: string
  modelValue: string
  noteId: number
  placeholder?: string
  spaceId: number
  toolbarTarget?: HTMLElement | null
  activeStyleTarget?: 'editor' | 'title'
  titleStyle?: {
    color?: string
    fontFamily?: string
    fontSize?: string
    lineHeight?: string
    textAlign?: 'left' | 'center' | 'right'
  }
}>()

const emit = defineEmits<{
  (event: 'file-inserted', file: ManagedFile): void
  (event: 'editor-focus'): void
  (event: 'persisted', note: Note): void
  (event: 'references-change', references: EditorFileReference[]): void
  (event: 'request-file'): void
  (event: 'save-request'): void
  (event: 'status-change', payload: { message: string; status: CollabConnectionStatus }): void
  (event: 'style-change', style: { fontSize: string; lineHeight: string }): void
  (event: 'title-style-change', style: Partial<{ color: string; fontFamily: string; fontSize: string; lineHeight: string; textAlign: 'left' | 'center' | 'right' }>): void
  (event: 'update:htmlValue', value: string): void
  (event: 'update:modelValue', value: string): void
}>()

const userStore = useUserStore()
const { t } = useI18n()
const richEditorRef = ref<TipTapEditorHandle | null>(null)
const statusMessage = ref(props.canEdit ? t('collab.connecting') : t('collab.readOnly'))
const connectionStatus = ref<CollabConnectionStatus>(props.canEdit ? 'connecting' : 'idle')
const remoteUsers = ref<CollabPresenceUser[]>([])
const remoteCursors = ref<RemoteCursor[]>([])
const editorHtml = ref(resolveInitialHtml(props.htmlValue, props.modelValue))
const editorText = ref(htmlToText(editorHtml.value))

const doc = new Y.Doc()
const yXmlFragment = doc.getXmlFragment('content')

const collabProvider = shallowRef<NoteCollabProvider | null>(null)
const collaborationBootstrapHtml = ref('')
let saveTimer: number | null = null
let removeStatusListener: (() => void) | null = null
let removeSyncedListener: (() => void) | null = null
let removeBootstrapListener: (() => void) | null = null
let persistPromise: Promise<void> | null = null
let isUnmounting = false

const localPresenceUser = computed(() => resolvePresenceUser(userStore.profile))
const editingUsers = computed(() => [localPresenceUser.value, ...remoteUsers.value])
const collaborationConfig = computed(() =>
  props.canEdit && collabProvider.value
    ? {
        bootstrapHtml: collaborationBootstrapHtml.value,
        fragment: yXmlFragment,
        provider: collabProvider.value,
        user: { ...localPresenceUser.value },
      }
    : null,
)
const showConnectionNotice = computed(
  () => props.canEdit && ['connecting', 'connected', 'reconnecting', 'error'].includes(connectionStatus.value),
)

function resolveInitialHtml(htmlValue?: string, modelValue?: string) {
  if (htmlValue?.trim()) {
    return sanitizeEditorHtml(htmlValue)
  }
  return sanitizeEditorHtml(renderMarkdownLite(modelValue || ''))
}

function htmlToText(html: string) {
  if (!html.trim()) {
    return ''
  }
  const documentBody = new DOMParser().parseFromString(html, 'text/html').body
  return documentBody.textContent || ''
}

const resolveWsUrl = () => {
  const androidOverrideUrl = window.sessionStorage.getItem('notask-flow-android-collab-ws-url')?.trim()
  if (androidOverrideUrl) {
    return resolveCollabWsUrl(androidOverrideUrl)
  }

  return resolveCollabWsUrl(import.meta.env.VITE_COLLAB_WS_URL)
}

const resolvePresenceUser = (profile: UserProfile | null): CollabPresenceUser => {
  const userId = profile?.id || 0
  const { color, colorLight } = buildPresenceColor(userId)
  return {
    userId,
    name: profile?.nickname || profile?.username || t('collab.me'),
    avatarUrl: profile?.avatarUrl,
    color,
    colorLight,
  }
}

const syncRemoteUsers = () => {
  if (isUnmounting) {
    return
  }

  if (!collabProvider.value) {
    remoteUsers.value = []
    remoteCursors.value = []
    return
  }

  const deduped = new Map<number, CollabPresenceUser>()
  const cursors: RemoteCursor[] = []

  collabProvider.value.awareness.getStates().forEach((state: CollabAwarenessState, clientId) => {
    const user = state.user
    if (!user || clientId === doc.clientID) {
      return
    }

    if (!deduped.has(user.userId)) {
      deduped.set(user.userId, user)
    }

    if (state.cursor) {
      cursors.push({
        ...user,
        clientId,
        from: state.cursor.from,
        to: state.cursor.to,
      })
    }
  })

  remoteUsers.value = Array.from(deduped.values())
  remoteCursors.value = cursors
}

const normalizeCollaborativeHtml = (value: string) => {
  if (!value.trim()) {
    return ''
  }
  const html = /<\/?[a-z][\s\S]*>/i.test(value) ? value : renderMarkdownLite(value)
  return sanitizeEditorHtml(html)
}

const applyCollaborativeHtml = (html: string) => {
  const normalizedHtml = normalizeCollaborativeHtml(html)
  if (normalizedHtml === editorHtml.value) {
    return
  }

  editorHtml.value = normalizedHtml
  editorText.value = htmlToText(normalizedHtml)
  emit('update:htmlValue', normalizedHtml)
  emit('update:modelValue', editorText.value)
}

const performPersistContent = async (forceCheckpoint = false) => {
  if (!props.canEdit) {
    return
  }

  const contentHtml = sanitizeEditorHtml(editorHtml.value)
  const content = htmlToText(contentHtml)
  const saveRequest = forceCheckpoint ? noteService.createCheckpoint : noteService.saveCollabContent
  const note = await saveRequest(props.spaceId, props.noteId, {
    content,
    contentHtml,
  })

  if (isUnmounting) {
    return
  }

  emit('persisted', note)
  emit('status-change', {
    status: 'synced',
    message: forceCheckpoint ? t('collab.checkpointSaved') : t('collab.contentSaved'),
  })
}

const persistContent = async (forceCheckpoint = false) => {
  if (persistPromise) {
    await persistPromise
    if (!forceCheckpoint) {
      return
    }
  }

  persistPromise = performPersistContent(forceCheckpoint).finally(() => {
    persistPromise = null
  })
  await persistPromise
}

const handlePersistFailure = () => {
  if (isUnmounting) {
    return
  }

  statusMessage.value = t('collab.saveFailed')
  emit('status-change', {
    status: 'error',
    message: t('collab.saveFailed'),
  })

  if (saveTimer) {
    return
  }

  saveTimer = window.setTimeout(() => {
    saveTimer = null
    persistContent().catch(handlePersistFailure)
  }, AUTO_SAVE_DELAY_MS * 2)
}

const scheduleSave = () => {
  if (isUnmounting || !props.canEdit) {
    return
  }

  if (saveTimer) {
    window.clearTimeout(saveTimer)
  }

  saveTimer = window.setTimeout(() => {
    saveTimer = null
    persistContent().catch(handlePersistFailure)
  }, AUTO_SAVE_DELAY_MS)
}

const createCollabProvider = () =>
  new NoteCollabProvider({
    doc,
    getTicket: async () => {
      const response = await noteService.createCollabTicket(props.spaceId, props.noteId)
      return response.ticket
    },
    presence: localPresenceUser.value,
    wsUrl: resolveWsUrl(),
  })

if (props.canEdit) {
  collabProvider.value = createCollabProvider()
}

const setupCollaboration = () => {
  if (!collabProvider.value) {
    collabProvider.value = createCollabProvider()
  }

  const provider = collabProvider.value

  removeStatusListener = provider.onStatusChange((payload) => {
    if (isUnmounting) {
      return
    }

    connectionStatus.value = payload.status
    statusMessage.value = payload.message
    emit('status-change', payload)
  })

  removeSyncedListener = provider.onSynced(() => undefined)
  removeBootstrapListener = provider.onBootstrap((content) => {
    if (isUnmounting) {
      return
    }

    collaborationBootstrapHtml.value = normalizeCollaborativeHtml(content)
  })

  provider.awareness.on('change', syncRemoteUsers)
  provider.connect()
}

const focus = () => {
  richEditorRef.value?.focus()
}

const saveNow = async (forceCheckpoint = false) => {
  if (saveTimer) {
    window.clearTimeout(saveTimer)
    saveTimer = null
  }
  await persistContent(forceCheckpoint)
}

const replaceSelection = () => {
  focus()
}

const insertManagedFile = (file: ManagedFile) => {
  richEditorRef.value?.insertManagedFile(file)
}

const handleSelectionChange = (selection: EditorSelectionPayload) => {
  if (isUnmounting || !props.canEdit || !collabProvider.value) {
    return
  }

  collabProvider.value.awareness.setLocalStateField('cursor', selection)
}

const handleStyleChange = (style: { fontSize: string; lineHeight: string }) => {
  emit('style-change', style)
}

const handleSaveShortcut = (event: KeyboardEvent) => {
  if (!(event.ctrlKey || event.metaKey) || event.key.toLowerCase() !== 's') {
    return
  }

  event.preventDefault()
  event.stopPropagation()
  emit('save-request')
}

watch(
  () => editorHtml.value,
  (html) => {
    if (isUnmounting) {
      return
    }

    const text = htmlToText(html)
    if (editorText.value !== text) {
      editorText.value = text
    }
    emit('update:htmlValue', html)
    emit('update:modelValue', text)

    if (!props.canEdit) {
      return
    }

    scheduleSave()
  },
)

watch(
  () => [props.htmlValue, props.modelValue],
  () => {
    if (isUnmounting) {
      return
    }

    if (props.canEdit) {
      return
    }

    applyCollaborativeHtml(resolveInitialHtml(props.htmlValue, props.modelValue))
  },
)

onMounted(() => {
  if (!props.canEdit) {
    return
  }

  setupCollaboration()
})

onBeforeUnmount(() => {
  isUnmounting = true
  if (saveTimer) {
    window.clearTimeout(saveTimer)
    saveTimer = null
  }

  if (props.canEdit) {
    persistContent().catch(() => undefined)
  }

  if (collabProvider.value) {
    collabProvider.value.awareness.off('change', syncRemoteUsers)
  }
  removeStatusListener?.()
  removeSyncedListener?.()
  removeBootstrapListener?.()
  collabProvider.value?.destroy()
  doc.destroy()
})

defineExpose({
  focus,
  insertManagedFile,
  replaceSelection,
  saveNow,
})
</script>
