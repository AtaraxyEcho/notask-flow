<template>
  <div class="min-h-[calc(100vh-4rem)]">
    <div v-if="!noteStore.notebooks.length" class="flex min-h-[calc(100vh-8rem)] items-center justify-center">
      <EmptyState
        :title="t('note.noNotebookTitle')"
        :description="t('note.noNotebookDescription')"
        icon="create_new_folder"
      >
        <button class="app-primary-button" type="button" @click="createDefaultNotebook">{{ t('note.createDefaultNotebook') }}</button>
      </EmptyState>
    </div>

    <div
      v-else
      class="flex h-[calc(100vh-4rem)] overflow-hidden rounded-[1.5rem] border border-[#E0E0E0] bg-surface"
      :class="isDragging ? 'select-none' : ''"
    >
      <section class="flex w-[340px] shrink-0 flex-col border-r border-[#E0E0E0] bg-surface">
        <div class="space-y-4 p-4">
          <button
            class="flex w-full items-center justify-center gap-2 rounded-2xl bg-primary-container py-3 font-bold text-on-primary-container shadow-sm transition-all hover:brightness-95"
            type="button"
            @click="createQuickNote"
          >
            <span class="material-symbols-outlined">add</span>
            {{ t('note.newNote') }}
          </button>

          <div
            v-if="creationHint"
            class="rounded-xl border border-primary-container/20 bg-primary-fixed/30 px-4 py-3 text-sm text-primary"
          >
            {{ creationHint }}
          </div>

          <label class="relative block">
            <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-sm text-stone-400">
              filter_list
            </span>
            <input
              v-model="searchKeyword"
              class="w-full rounded-xl border border-outline-variant/30 bg-surface-container-low py-2 pl-10 pr-4 text-body-secondary focus:outline-none focus:ring-1 focus:ring-primary-container"
              :placeholder="t('note.filterTags')"
              type="text"
              @keydown.enter="loadNotes"
            />
          </label>

          <el-select
            v-if="isTeamSpace && !isCollabMode"
            v-model="noteStore.query.projectId"
            clearable
            class="w-full"
            :placeholder="t('note.filterProject')"
            @change="loadNotes"
          >
            <el-option v-for="project in availableProjects" :key="project.id" :label="project.name" :value="project.id" />
          </el-select>
        </div>

        <div class="hide-scrollbar flex-1 space-y-3 overflow-y-auto px-4 pb-4">
          <button
            v-if="showParentNotebookEntry"
            class="group w-full cursor-pointer rounded-2xl border border-dashed border-outline-variant/40 bg-surface-container-low p-4 text-left transition-all hover:border-primary/40 hover:bg-white hover:shadow-sm"
            type="button"
            @click="goParentNotebook"
          >
            <div class="flex items-center gap-3">
              <span class="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-primary-fixed/40 text-primary">
                <span class="material-symbols-outlined text-[22px]">drive_folder_upload</span>
              </span>
              <div class="min-w-0 flex-1">
                <h3 class="truncate font-title-serif text-base text-on-surface transition-colors group-hover:text-primary">
                  {{ t('files.parentFolder') }}
                </h3>
                <p class="mt-1 text-[12px] text-stone-500">{{ t('note.notebookFallback') }}</p>
              </div>
            </div>
          </button>

          <button
            v-for="notebook in visibleChildNotebooks"
            :key="`notebook-${notebook.id}`"
            class="group w-full cursor-pointer rounded-2xl border border-outline-variant/20 bg-white/55 p-4 text-left transition-all hover:border-primary/40 hover:bg-white hover:shadow-sm"
            type="button"
            @click="openNotebookFolder(notebook.id)"
          >
            <div class="flex items-center gap-3">
              <span class="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-primary-fixed/40 text-primary">
                <span class="material-symbols-outlined text-[22px]">folder_open</span>
              </span>
              <div class="min-w-0 flex-1">
                <h3 class="truncate font-title-serif text-base text-on-surface transition-colors group-hover:text-primary">
                  {{ notebook.name }}
                </h3>
                <p class="mt-1 text-[12px] text-stone-500">
                  {{ t('note.folderDescription', { count: countNotesInNotebook(notebook) }) }}
                </p>
              </div>
              <span class="material-symbols-outlined text-[18px] text-stone-400 transition-transform group-hover:translate-x-0.5 group-hover:text-primary">
                chevron_right
              </span>
            </div>
          </button>

          <button
            v-for="note in visibleNotes"
            :key="note.id"
            class="group w-full cursor-pointer rounded-2xl border-l-4 p-4 text-left transition-all"
            :class="
              note.id === activeNoteId
                ? 'border-primary bg-white shadow-sm'
                : 'border-transparent hover:bg-white/50'
            "
            :draggable="true"
            type="button"
            @click="openNote(note.id)"
            @dragstart="startNoteDrag($event, note.id)"
          >
            <div class="mb-1 flex items-start justify-between gap-3">
              <h3 class="font-title-serif text-base text-on-surface transition-colors group-hover:text-primary">
                {{ note.title }}
              </h3>
              <span class="shrink-0 text-caption text-stone-400">{{ formatShortDate(note.gmtModified) }}</span>
            </div>
            <p class="mb-3 line-clamp-2 text-body-secondary text-stone-500">
              {{ note.content || t('note.fallbackExcerpt') }}
            </p>
            <div class="flex flex-wrap gap-1">
              <span
                v-for="tag in note.tags || []"
                :key="tag.id"
                class="rounded-full bg-primary-fixed/30 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider text-primary"
              >
                #{{ tag.name }}
              </span>
            </div>
          </button>

          <EmptyState
            v-if="isNoteDirectoryEmpty"
            :title="t('note.noNotesTitle')"
            :description="t('note.noNotesDescription')"
            icon="note_add"
          />
        </div>
      </section>

      <section class="flex min-w-0 flex-1 flex-col overflow-hidden bg-surface-container-lowest">
        <div class="flex h-14 shrink-0 items-center justify-end border-b border-outline-variant/20 bg-surface px-6">
          <div class="flex items-center gap-4">
            <div class="flex rounded-xl border border-outline-variant/20 bg-surface-container-low p-1">
              <button
                class="px-4 py-1.5 text-[12px] font-bold tracking-wide transition-all"
                :class="viewMode === 'edit' ? 'rounded-lg bg-white text-primary shadow-sm' : 'text-stone-500 hover:text-primary'"
                type="button"
                @click="viewMode = 'edit'"
              >
                {{ t('note.edit') }}
              </button>
              <button
                class="px-4 py-1.5 text-[12px] font-bold tracking-wide transition-all"
                :class="viewMode === 'preview' ? 'rounded-lg bg-white text-primary shadow-sm' : 'text-stone-500 hover:text-primary'"
                type="button"
                @click="viewMode = 'preview'"
              >
                {{ t('note.preview') }}
              </button>
            </div>

            <div class="flex items-center gap-2">
              <button
                class="flex h-9 w-9 items-center justify-center rounded-full border border-outline-variant/30 text-stone-500 transition-all hover:border-primary hover:text-primary"
                type="button"
                @click="shareDialogOpen = true"
              >
                <span class="material-symbols-outlined !text-[20px]">share</span>
              </button>
              <button
                class="flex h-9 w-9 items-center justify-center rounded-full border border-outline-variant/30 text-stone-500 transition-all hover:border-primary hover:text-primary"
                type="button"
                @click="historyDrawerOpen = true"
              >
                <span class="material-symbols-outlined !text-[20px]">history</span>
              </button>
              <el-dropdown trigger="click" @command="exportCurrentNote">
                <button
                  class="flex h-9 w-9 items-center justify-center rounded-full border border-outline-variant/30 text-stone-500 transition-all hover:border-primary hover:text-primary"
                  type="button"
                >
                  <span class="material-symbols-outlined !text-[20px]">download</span>
                </button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="pdf">导出 PDF</el-dropdown-item>
                    <el-dropdown-item command="word">导出 Word</el-dropdown-item>
                    <el-dropdown-item command="image">导出图片</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </div>

        <div
          v-if="noteStore.currentNote && !noteStore.currentNoteLoading"
          class="flex flex-wrap items-center gap-3 border-b border-outline-variant/10 bg-[#FCF7F4] px-6 py-3"
        >
          <div
            v-if="isTeamSpace && !isCollabMode"
            class="flex min-w-[240px] items-center gap-3 rounded-full border border-outline-variant/30 bg-white px-4 py-2"
          >
            <span class="text-[11px] font-bold uppercase tracking-[0.2em] text-stone-400">{{ t('note.project') }}</span>
            <el-select
              v-model="draft.projectId"
              :disabled="!canEditCurrentNote"
              clearable
              class="flex-1"
              :placeholder="t('note.selectProject')"
            >
              <el-option v-for="project in availableProjects" :key="project.id" :label="project.name" :value="project.id" />
            </el-select>
          </div>

          <span class="text-[11px] font-bold uppercase tracking-[0.2em] text-stone-400">{{ t('note.tags') }}</span>
          <div class="flex flex-1 flex-wrap gap-2">
            <button
              v-for="tag in noteStore.tags"
              :key="tag.id"
              class="rounded-full border px-3 py-1 text-[11px] font-bold uppercase tracking-[0.18em] transition-all"
              :class="
                draft.tagIds.includes(tag.id)
                  ? 'border-primary bg-primary-fixed/40 text-primary'
                  : 'border-outline-variant/30 bg-white/70 text-stone-500 hover:border-primary hover:text-primary'
              "
              type="button"
              :disabled="!canEditCurrentNote"
              @click="toggleTag(tag.id)"
            >
              #{{ tag.name }}
            </button>
          </div>

          <div class="flex min-w-[220px] items-center gap-2 rounded-full border border-outline-variant/30 bg-white px-3 py-2">
            <span class="material-symbols-outlined text-[16px] text-primary">sell</span>
            <input
              v-model="newTagName"
              :disabled="!canEditCurrentNote"
              class="w-full border-none bg-transparent p-0 text-sm text-stone-600 outline-none focus:ring-0"
              :placeholder="t('note.tagInputPlaceholder')"
              type="text"
              @keydown.enter.prevent="submitEditorTag"
            />
          </div>
        </div>

        <div
          v-if="noteStore.currentNote && !noteStore.currentNoteLoading"
          ref="editorToolbarHostRef"
          class="note-toolbar-host"
          :class="{ 'note-toolbar-host-hidden': viewMode !== 'edit' }"
        ></div>

        <div
          v-if="noteStore.currentNote && !noteStore.currentNoteLoading"
          ref="resizerContainerRef"
          class="resizer-container min-h-0 flex-1 overflow-hidden"
        >
          <div
            v-show="viewMode === 'edit'"
            class="editor-pane flex h-full overflow-hidden bg-[#F9F7F5]"
            :style="{ width: `${editorWidth}%` }"
          >
            <div
              v-if="false"
              class="flex w-12 shrink-0 flex-col items-center border-r border-outline-variant/10 bg-[#F1EEEC] py-8 text-[11px] font-mono leading-[1.8] text-stone-400 select-none"
            >
              <span
                v-for="line in editorLineNumbers"
                :key="line"
                class="w-full text-center"
                :class="line === activeLine ? 'bg-primary-fixed/30 font-bold text-primary' : ''"
              >
                {{ line }}
              </span>
            </div>

            <div
              class="flex flex-1 flex-col"
              :class="isCollabMode ? 'min-h-0 overflow-hidden' : 'min-h-0 overflow-hidden'"
            >
              <CollaborativeTipTapEditor
                v-if="isCollabMode && noteStore.currentNote"
                ref="collabEditorRef"
                :key="noteStore.currentNote.id"
                v-model="draft.content"
                v-model:html-value="draft.contentHtml"
                class="min-h-0 flex-1"
                :can-edit="Boolean(noteStore.currentNote.canEdit)"
                :file-button-label="t('files.references')"
                :note-id="noteStore.currentNote.id"
                :space-id="noteStore.currentNote.spaceId"
                :placeholder="t('note.editorPlaceholder')"
                :toolbar-target="editorToolbarHostRef"
                :active-style-target="activeStyleTarget"
                :title-style="titleToolbarStyle"
                @editor-focus="activeStyleTarget = 'editor'"
                @file-inserted="bindManagedFileReference"
                @persisted="handleCollabPersisted"
                @references-change="syncEditorFileReferences"
                @request-file="openFileReferenceDialog"
                @save-request="saveCurrentDocument"
                @style-change="handleEditorStyleChange"
                @title-style-change="handleTitleStyleChange"
                @status-change="handleCollabStatusChange"
              >
                <template #before-content>
                  <div class="note-title-editor">
                    <input
                      ref="titleInputRef"
                      v-model="draft.title"
                      :disabled="!canEditCurrentNote"
                      class="note-title-input"
                      :style="titleInputStyle"
                      :placeholder="t('note.titlePlaceholder')"
                      @focus="activeStyleTarget = 'title'"
                    />
                  </div>
                </template>
              </CollaborativeTipTapEditor>

              <TipTapRichEditor
                v-else
                ref="richEditorRef"
                v-model="draft.content"
                v-model:html-value="draft.contentHtml"
                class="min-h-0 flex-1"
                :editable="canEditCurrentNote"
                :placeholder="t('note.editorPlaceholder')"
                :file-button-label="t('files.references')"
                :space-id="spaceStore.currentSpaceId"
                :toolbar-target="editorToolbarHostRef"
                :active-style-target="activeStyleTarget"
                :title-style="titleToolbarStyle"
                @editor-focus="activeStyleTarget = 'editor'"
                @request-file="openFileReferenceDialog"
                @file-inserted="bindManagedFileReference"
                @references-change="syncEditorFileReferences"
                @style-change="handleEditorStyleChange"
                @title-style-change="handleTitleStyleChange"
              >
                <template #before-content>
                  <div class="note-title-editor">
                    <input
                      ref="titleInputRef"
                      v-model="draft.title"
                      :disabled="!canEditCurrentNote"
                      class="note-title-input"
                      :style="titleInputStyle"
                      :placeholder="t('note.titlePlaceholder')"
                      @focus="activeStyleTarget = 'title'"
                    />
                  </div>
                </template>
              </TipTapRichEditor>
            </div>
          </div>

          <div v-show="viewMode === 'edit'" class="resizer-handle" @pointerdown.prevent="startResize"></div>

          <div
            class="preview-pane hide-scrollbar h-full overflow-y-auto bg-white"
            :style="viewMode === 'edit' ? { width: `${100 - editorWidth}%` } : { width: '100%' }"
          >
            <div class="mx-auto max-w-3xl p-12">
              <nav class="mb-10 flex items-center gap-2 text-[11px] uppercase tracking-widest text-stone-400">
                <span>{{ currentNotebookName }}</span>
                <span class="material-symbols-outlined text-[10px]">chevron_right</span>
                <span v-if="isTeamSpace && draft.projectId">{{ currentProjectName }}</span>
                <span v-if="isTeamSpace && draft.projectId" class="material-symbols-outlined text-[10px]">chevron_right</span>
                <span class="text-primary font-bold">{{ draft.title || t('note.untitled') }}</span>
              </nav>

              <h1 class="mb-6 font-display-serif text-5xl leading-tight tracking-tight text-on-surface" :style="titleInputStyle">
                {{ draft.title || t('note.untitled') }}
              </h1>

              <div v-if="selectedTagNames.length" class="mb-8 flex flex-wrap gap-2">
                <span
                  v-for="tagName in selectedTagNames"
                  :key="tagName"
                  class="rounded-full bg-primary-fixed/30 px-3 py-1 text-[11px] font-bold uppercase tracking-[0.18em] text-primary"
                >
                  #{{ tagName }}
                </span>
              </div>

              <div class="notes-preview-body" :style="previewContentStyle" v-html="previewHtml"></div>
            </div>
          </div>
        </div>

        <div v-else-if="noteStore.currentNoteLoading" class="flex flex-1 items-center justify-center bg-[#FCF7F4]">
          <div class="w-full max-w-3xl space-y-6 px-10">
            <div class="h-10 w-1/3 animate-pulse rounded-2xl bg-primary-fixed/30"></div>
            <div class="h-4 w-1/4 animate-pulse rounded-full bg-primary-fixed/20"></div>
            <div class="space-y-3">
              <div class="h-4 w-full animate-pulse rounded-full bg-surface-container-high"></div>
              <div class="h-4 w-11/12 animate-pulse rounded-full bg-surface-container-high"></div>
              <div class="h-4 w-10/12 animate-pulse rounded-full bg-surface-container-high"></div>
              <div class="h-4 w-9/12 animate-pulse rounded-full bg-surface-container-high"></div>
            </div>
          </div>
        </div>

        <div v-else class="flex flex-1 items-center justify-center">
          <EmptyState
            :title="t('note.chooseNoteTitle')"
            :description="t('note.chooseNoteDescription')"
            icon="docs"
          />
        </div>

        <div class="flex h-10 shrink-0 items-center justify-between border-t border-outline-variant/20 bg-surface px-6">
          <div class="flex gap-4 text-[11px] uppercase tracking-widest text-stone-400">
            <span class="flex items-center gap-1.5">
              <span class="material-symbols-outlined !text-[14px]">article</span>
              {{ t('note.characters', { count: characterCount }) }}
            </span>
            <span class="h-3 w-px self-center bg-outline-variant/30"></span>
            <span class="flex items-center gap-1.5">
              <span class="material-symbols-outlined !text-[14px]">segment</span>
              {{ t('note.lines', { count: lineCount }) }}
            </span>
          </div>
          <div class="flex items-center gap-3 text-[11px] text-stone-400">
            <span class="flex items-center gap-1.5">
              <span class="material-symbols-outlined text-[14px] text-green-500" style="font-variation-settings: 'FILL' 1">
                cloud_done
              </span>
              <span class="tracking-wider">{{ saveStatus }}</span>
            </span>
          </div>
        </div>
      </section>
    </div>

    <NoteShareDialog
      v-model="shareDialogOpen"
      v-model:expire="shareExpire"
      :share-code="noteStore.currentNote?.shareCode"
      :share-link="shareLink"
      @share="shareCurrentNote"
    />

    <NoteFileReferenceDialog
      v-model="fileReferenceDialogOpen"
      v-model:keyword="fileReferenceKeyword"
      :files="fileReferenceFiles"
      :loading="fileReferenceLoading"
      @search="loadFileReferenceOptions"
      @select="insertFileReference"
    />

    <NoteHistoryDrawer
      v-model="historyDrawerOpen"
      :histories="noteStore.histories"
      @restore="restoreHistory"
    />
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import EmptyState from '@/components/common/EmptyState.vue'
import NoteFileReferenceDialog from '@/components/notes/NoteFileReferenceDialog.vue'
import NoteHistoryDrawer from '@/components/notes/NoteHistoryDrawer.vue'
import NoteShareDialog from '@/components/notes/NoteShareDialog.vue'
import TipTapRichEditor from '@/components/notes/TipTapRichEditor.vue'
import CollaborativeTipTapEditor from '@/components/team/CollaborativeTipTapEditor.vue'
import { attachmentService } from '@/api/modules/attachment'
import { fileService } from '@/api/modules/file'
import { noteService } from '@/api/modules/note'
import { useI18n } from '@/i18n'
import { useNoteStore } from '@/stores/note'
import { useProjectStore } from '@/stores/project'
import { useSpaceStore } from '@/stores/space'
import type { ManagedFile, Note, NoteHistorySaveType, Notebook } from '@/types/app'
import { formatDateTime, formatShortDate, fromLocalInputDateTime, toLocalInputDateTime } from '@/utils/date'
import { renderMarkdownLite } from '@/utils/markdown'
import { sanitizeEditorHtml, sanitizeHtml } from '@/utils/sanitize'

type InlineActionType = 'bold' | 'italic' | 'strike' | 'code'
type ListActionType = 'list' | 'ordered' | 'task' | 'quote'
type InsertActionType = 'codeblock' | 'divider' | 'image' | 'link'
type NoteExportFormat = 'pdf' | 'word' | 'image'
type EditorSelectionTransform = (selected: string) => { value: string; selectFrom?: number; selectTo?: number }
type CollabEditorHandle = {
  focus: () => void
  insertManagedFile: (file: ManagedFile) => void
  replaceSelection: (transform: EditorSelectionTransform) => void
  saveNow: (forceCheckpoint?: boolean) => Promise<void>
}

type TipTapEditorHandle = {
  focus: () => void
  insertManagedFile: (file: ManagedFile) => void
}

type EditorFileReference = {
  fileId: number
  attachmentId: number
  kind: 'file' | 'image'
}

const AUTO_SAVE_DELAY_MS = 30 * 1000

const route = useRoute()
const router = useRouter()
const noteStore = useNoteStore()
const projectStore = useProjectStore()
const spaceStore = useSpaceStore()
const { t } = useI18n()

const editorRef = ref<HTMLTextAreaElement | null>(null)
const collabEditorRef = ref<CollabEditorHandle | null>(null)
const richEditorRef = ref<TipTapEditorHandle | null>(null)
const resizerContainerRef = ref<HTMLElement | null>(null)
const titleInputRef = ref<HTMLInputElement | null>(null)
const editorToolbarHostRef = ref<HTMLElement | null>(null)

const historyDrawerOpen = ref(false)
const shareDialogOpen = ref(false)
const shareExpire = ref('')
const searchKeyword = ref('')
const newTagName = ref('')
const creationHint = ref('')
const fileReferenceDialogOpen = ref(false)
const fileReferenceLoading = ref(false)
const fileReferenceKeyword = ref('')
const fileReferenceFiles = ref<ManagedFile[]>([])
const boundEditorReferenceKeys = ref<Record<string, number>>({})
const viewMode = ref<'edit' | 'preview'>('edit')
const editorWidth = ref(50)
const isDragging = ref(false)
const editorDisplayStyle = reactive({
  fontSize: '16px',
  lineHeight: '1.5',
})
const activeStyleTarget = ref<'editor' | 'title'>('editor')
const titleAlignment = ref<'left' | 'center' | 'right'>('left')
const titleDisplayStyle = reactive({
  color: '',
  fontFamily: '',
  fontSize: '',
  lineHeight: '',
})

const draft = reactive({
  title: '',
  content: '',
  contentHtml: '',
  notebookId: 0,
  projectId: 0,
  tagIds: [] as number[],
})

const buildDraftStateKey = (state: {
  spaceId?: number
  noteId: number
  title: string
  content: string
  contentHtml?: string
  notebookId: number
  projectId: number
  tagIds: number[]
}) =>
  JSON.stringify({
    content: state.content,
    contentHtml: state.contentHtml || '',
    notebookId: state.notebookId,
    noteId: state.noteId,
    projectId: state.projectId,
    spaceId: state.spaceId || 0,
    tagIds: [...state.tagIds],
    title: state.title.trim(),
  })

type NotebookTreeItem = Notebook & { depth: number }

const headingActions = computed(() => [
  { id: 'h1', icon: 'format_h1', label: t('note.toolbar.heading1'), level: 1 },
  { id: 'h2', icon: 'format_h2', label: t('note.toolbar.heading2'), level: 2 },
  { id: 'h3', icon: 'format_h3', label: t('note.toolbar.heading3'), level: 3 },
])

const inlineActions = computed(() => [
  { id: 'bold', icon: 'format_bold', label: t('note.toolbar.bold'), type: 'bold' as InlineActionType },
  { id: 'italic', icon: 'format_italic', label: t('note.toolbar.italic'), type: 'italic' as InlineActionType },
  { id: 'strike', icon: 'format_strikethrough', label: t('note.toolbar.strike'), type: 'strike' as InlineActionType },
  { id: 'code', icon: 'code', label: t('note.toolbar.code'), type: 'code' as InlineActionType },
])

const listActions = computed(() => [
  { id: 'list', icon: 'format_list_bulleted', label: t('note.toolbar.bulletList'), type: 'list' as ListActionType },
  { id: 'ordered', icon: 'format_list_numbered', label: t('note.toolbar.numberedList'), type: 'ordered' as ListActionType },
  { id: 'task', icon: 'check_box', label: t('note.toolbar.taskList'), type: 'task' as ListActionType },
  { id: 'quote', icon: 'format_quote', label: t('note.toolbar.quote'), type: 'quote' as ListActionType },
])

const insertActions = computed(() => [
  { id: 'codeblock', icon: 'data_object', label: t('note.toolbar.codeBlock'), type: 'codeblock' as InsertActionType },
  { id: 'divider', icon: 'horizontal_rule', label: t('note.toolbar.divider'), type: 'divider' as InsertActionType },
  { id: 'image', icon: 'image', label: t('note.toolbar.image'), type: 'image' as InsertActionType },
  { id: 'link', icon: 'link', label: t('note.toolbar.link'), type: 'link' as InsertActionType },
])

const flatTree = (nodes: Notebook[], depth = 0): NotebookTreeItem[] =>
  nodes.flatMap((node) => [{ ...node, depth }, ...(node.children ? flatTree(node.children, depth + 1) : [])])

const findNotebookById = (nodes: Notebook[], notebookId: number): Notebook | null => {
  for (const node of nodes) {
    if (node.id === notebookId) {
      return node
    }

    const child = node.children ? findNotebookById(node.children, notebookId) : null
    if (child) {
      return child
    }
  }

  return null
}
const collectNotebookIds = (notebook: Notebook): number[] => [
  notebook.id,
  ...(notebook.children || []).flatMap((child) => collectNotebookIds(child)),
]
const countNotesInNotebook = (notebook: Notebook) => {
  const notebookIds = new Set(collectNotebookIds(notebook))
  return noteStore.sidebarNotes.filter((note) => notebookIds.has(note.notebookId)).length
}

const flatNotebooks = computed(() => flatTree(noteStore.notebooks))
const isTeamSpace = computed(() => spaceStore.currentSpace?.type === 'TEAM')
const isCollabMode = computed(() => isTeamSpace.value && Boolean(noteStore.currentNote?.collabEnabled))
const isCollabEditable = computed(() => isCollabMode.value && Boolean(noteStore.currentNote?.canEdit))
const canEditCurrentNote = computed(() => noteStore.currentNote?.canEdit ?? true)
const activeNoteId = computed(() => {
  const routeNoteId = Number(route.params.noteId)
  return Number.isNaN(routeNoteId) ? noteStore.activeNoteId : routeNoteId
})
const availableProjects = computed(() => projectStore.availableProjects)
const directoryFilterActive = computed(() =>
  Boolean((noteStore.query.keyword || '').trim() || noteStore.query.tagId || noteStore.query.projectId),
)
const currentNotebook = computed(() =>
  noteStore.query.notebookId ? findNotebookById(noteStore.notebooks, noteStore.query.notebookId) : null,
)
const showParentNotebookEntry = computed(() =>
  Boolean(noteStore.query.notebookId && !directoryFilterActive.value),
)
const visibleChildNotebooks = computed(() => {
  if (directoryFilterActive.value) {
    return []
  }

  return noteStore.query.notebookId ? currentNotebook.value?.children || [] : noteStore.notebooks
})
const visibleNotes = computed(() => {
  if (directoryFilterActive.value) {
    return noteStore.notes
  }

  if (!noteStore.query.notebookId) {
    return []
  }

  return noteStore.notes.filter((note) => note.notebookId === noteStore.query.notebookId)
})
const isNoteDirectoryEmpty = computed(() => !visibleChildNotebooks.value.length && !visibleNotes.value.length)
const currentNotebookName = computed(
  () => flatNotebooks.value.find((item: NotebookTreeItem) => item.id === draft.notebookId)?.name || t('note.notebookFallback'),
)
const currentProjectName = computed(
  () => availableProjects.value.find((project) => project.id === draft.projectId)?.name || t('note.unassignedProject'),
)
const selectedTagNames = computed(() =>
  noteStore.tags.filter((tag) => draft.tagIds.includes(tag.id)).map((tag) => tag.name),
)
const previewHtml = computed(() => sanitizeHtml(draft.contentHtml || renderMarkdownLite(draft.content)))
const previewContentStyle = computed(() => ({
  '--note-preview-block-gap': `${Math.min(1.2, Math.max(0.35, Number(editorDisplayStyle.lineHeight) * 0.42)).toFixed(2)}rem`,
  '--note-preview-list-gap': `${Math.min(0.9, Math.max(0.25, Number(editorDisplayStyle.lineHeight) * 0.28)).toFixed(2)}rem`,
  '--note-preview-font-size': editorDisplayStyle.fontSize,
  '--note-preview-line-height': editorDisplayStyle.lineHeight,
}))
const titleToolbarStyle = computed(() => ({
  color: titleDisplayStyle.color || '#1D1B20',
  fontFamily: titleDisplayStyle.fontFamily,
  fontSize: titleDisplayStyle.fontSize || '32px',
  lineHeight: titleDisplayStyle.lineHeight || '1.5',
  textAlign: titleAlignment.value,
}))
const titleInputStyle = computed(() => ({
  color: titleDisplayStyle.color || undefined,
  fontFamily: titleDisplayStyle.fontFamily || undefined,
  fontSize: titleDisplayStyle.fontSize || undefined,
  lineHeight: titleDisplayStyle.lineHeight || undefined,
  textAlign: titleAlignment.value,
}))

const handleEditorStyleChange = (style: { fontSize: string; lineHeight: string }) => {
  editorDisplayStyle.fontSize = style.fontSize
  editorDisplayStyle.lineHeight = style.lineHeight
}

const titleStyleStorageKey = computed(() => {
  const noteId = activeNoteId.value || noteStore.currentNote?.id || 0
  const spaceId = noteStore.currentNote?.spaceId || spaceStore.currentSpaceId || 0
  return `notask-note-title-style:${spaceId}:${noteId}`
})

const titleAlignmentStorageKey = computed(() => {
  const noteId = activeNoteId.value || noteStore.currentNote?.id || 0
  const spaceId = noteStore.currentNote?.spaceId || spaceStore.currentSpaceId || 0
  return `notask-note-title-align:${spaceId}:${noteId}`
})

const restoreTitleAlignment = () => {
  if (typeof localStorage === 'undefined') {
    titleAlignment.value = 'left'
    titleDisplayStyle.color = ''
    titleDisplayStyle.fontFamily = ''
    titleDisplayStyle.fontSize = ''
    titleDisplayStyle.lineHeight = ''
    return
  }

  const storedAlignment = localStorage.getItem(titleAlignmentStorageKey.value)
  titleAlignment.value = storedAlignment === 'center' || storedAlignment === 'right' ? storedAlignment : 'left'
  const storedStyle = localStorage.getItem(titleStyleStorageKey.value)
  if (!storedStyle) {
    titleDisplayStyle.color = ''
    titleDisplayStyle.fontFamily = ''
    titleDisplayStyle.fontSize = ''
    titleDisplayStyle.lineHeight = ''
    return
  }

  try {
    const parsedStyle = JSON.parse(storedStyle) as Partial<typeof titleDisplayStyle>
    titleDisplayStyle.color = typeof parsedStyle.color === 'string' ? parsedStyle.color : ''
    titleDisplayStyle.fontFamily = typeof parsedStyle.fontFamily === 'string' ? parsedStyle.fontFamily : ''
    titleDisplayStyle.fontSize = typeof parsedStyle.fontSize === 'string' ? parsedStyle.fontSize : ''
    titleDisplayStyle.lineHeight = typeof parsedStyle.lineHeight === 'string' ? parsedStyle.lineHeight : ''
  } catch {
    titleDisplayStyle.color = ''
    titleDisplayStyle.fontFamily = ''
    titleDisplayStyle.fontSize = ''
    titleDisplayStyle.lineHeight = ''
  }
}

const persistTitleStyle = () => {
  if (typeof localStorage === 'undefined') {
    return
  }

  localStorage.setItem(titleAlignmentStorageKey.value, titleAlignment.value)
  localStorage.setItem(
    titleStyleStorageKey.value,
    JSON.stringify({
      color: titleDisplayStyle.color,
      fontFamily: titleDisplayStyle.fontFamily,
      fontSize: titleDisplayStyle.fontSize,
      lineHeight: titleDisplayStyle.lineHeight,
    }),
  )
}

const handleTitleStyleChange = (
  style: Partial<{ color: string; fontFamily: string; fontSize: string; lineHeight: string; textAlign: 'left' | 'center' | 'right' }>,
) => {
  if (style.textAlign) {
    titleAlignment.value = style.textAlign
  }
  if (typeof style.color === 'string') {
    titleDisplayStyle.color = style.color
  }
  if (typeof style.fontFamily === 'string') {
    titleDisplayStyle.fontFamily = style.fontFamily
  }
  if (typeof style.fontSize === 'string') {
    titleDisplayStyle.fontSize = style.fontSize
  }
  if (typeof style.lineHeight === 'string') {
    titleDisplayStyle.lineHeight = style.lineHeight
  }
  persistTitleStyle()
}

const extractPlainText = (content: string) => {
  if (!content.trim()) {
    return ''
  }

  if (typeof DOMParser !== 'undefined' && /<[^>]+>/.test(content)) {
    const document = new DOMParser().parseFromString(content, 'text/html')
    return document.body.innerText || ''
  }

  return content
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<\/(p|div|li|h[1-6]|blockquote)>/gi, '\n')
    .replace(/<[^>]*>/g, ' ')
    .replace(/&nbsp;/gi, ' ')
}
const countContentCharacters = (content: string) => Array.from(content.replace(/\s/g, '')).length
const plainEditorText = computed(() => extractPlainText(draft.contentHtml || draft.content))
const characterCount = computed(() => countContentCharacters(plainEditorText.value))
const lineCount = computed(() => {
  const content = plainEditorText.value.replace(/\r\n/g, '\n')
  if (!content) {
    return 0
  }

  return content.split('\n').length
})
const editorLineNumbers = computed(() => Array.from({ length: Math.max(lineCount.value, 25) }, (_, index) => index + 1))
const activeLine = computed(() => Math.max(1, lineCount.value))
const currentNoteHydrationKey = computed(() => {
  if (!noteStore.currentNote) {
    return ''
  }

  return buildDraftStateKey({
    spaceId: noteStore.currentNote.spaceId,
    noteId: noteStore.currentNote.id,
    title: noteStore.currentNote.title,
    content: noteStore.currentNote.content || '',
    contentHtml: noteStore.currentNote.contentHtml || '',
    notebookId: noteStore.currentNote.notebookId,
    projectId: noteStore.currentNote.projectId || 0,
    tagIds: (noteStore.currentNote.tags || []).map((tag) => tag.id),
  })
})
const collabStatusMessage = ref(t('collab.connecting'))
const collabSaveMessage = ref('')
const saveStatus = computed(() => {
  if (isCollabMode.value) {
    return collabSaveMessage.value || collabStatusMessage.value
  }

  if (!noteStore.currentNote?.gmtModified) {
    return t('note.notSavedYet')
  }

  return t('note.savedAt', { time: formatDateTime(noteStore.currentNote.gmtModified) })
})
const shareLink = computed(() =>
  noteStore.currentNote?.shareCode ? `${window.location.origin}/public/notes/${noteStore.currentNote.shareCode}` : '',
)

let hydrating = false
let saveTimer: number | undefined
let searchTimer: number | undefined
let hintTimer: number | undefined
let pendingFocusNewNoteId: number | null = null
let lastSyncedDraftKey = ''
let shouldPrimeEditorReferences = false

const buildTrackedDraftKey = (state: {
  spaceId?: number
  noteId: number
  title: string
  content: string
  contentHtml?: string
  notebookId: number
  projectId: number
  tagIds: number[]
}) =>
  buildDraftStateKey({
    ...state,
    content: isCollabMode.value ? '' : state.content,
    contentHtml: isCollabMode.value ? '' : state.contentHtml,
  })

const clearHintLater = () => {
  if (hintTimer) {
    window.clearTimeout(hintTimer)
  }

  hintTimer = window.setTimeout(() => {
    creationHint.value = ''
  }, 3000)
}

const buildSavePayload = (saveType: NoteHistorySaveType = 'AUTO') => {
  const currentNote = noteStore.currentNote
  const noteId = currentNote?.id
  if (!currentNote || !noteId || currentNote.spaceId !== spaceStore.currentSpaceId || !draft.notebookId || !draft.title.trim()) {
    return null
  }

  return {
    noteId,
    spaceId: currentNote.spaceId,
    payload: {
      title: draft.title.trim(),
      content: draft.content,
      contentHtml: sanitizeEditorHtml(draft.contentHtml || renderMarkdownLite(draft.content)),
      notebookId: draft.notebookId,
      projectId: draft.projectId || undefined,
      tagIds: [...draft.tagIds],
      saveType,
    },
  }
}

const buildSnapshotKey = (snapshot: NonNullable<ReturnType<typeof buildSavePayload>>) =>
  buildTrackedDraftKey({
    spaceId: snapshot.spaceId,
    noteId: snapshot.noteId,
    title: snapshot.payload.title,
    content: snapshot.payload.content || '',
    contentHtml: snapshot.payload.contentHtml || '',
    notebookId: snapshot.payload.notebookId,
    projectId: snapshot.payload.projectId || 0,
    tagIds: snapshot.payload.tagIds || [],
  })

const persistPayload = async (snapshot: ReturnType<typeof buildSavePayload>) => {
  if (!snapshot) {
    return
  }

  if (snapshot.spaceId !== spaceStore.currentSpaceId) {
    return
  }

  const snapshotKey = buildSnapshotKey(snapshot)
  if (snapshotKey === lastSyncedDraftKey) {
    return
  }

  await noteStore.saveNote(snapshot.noteId, snapshot.payload, {
    updateCurrent: noteStore.currentNote?.id === snapshot.noteId,
  })
  if (snapshot.spaceId !== spaceStore.currentSpaceId) {
    return
  }

  lastSyncedDraftKey = snapshotKey
}

const flushPendingSave = async (saveType: NoteHistorySaveType = 'MANUAL') => {
  if (saveTimer) {
    window.clearTimeout(saveTimer)
    saveTimer = undefined
  }

  if (hydrating) {
    return
  }

  const snapshot = buildSavePayload(saveType)
  if (!snapshot) {
    return
  }

  if (buildSnapshotKey(snapshot) === lastSyncedDraftKey) {
    return
  }

  await persistPayload(snapshot)
}

const saveCurrentDocument = async () => {
  if (!noteStore.currentNote || noteStore.currentNote.spaceId !== spaceStore.currentSpaceId || !canEditCurrentNote.value) {
    return
  }

  await flushPendingSave('MANUAL')

  if (isCollabMode.value) {
    await collabEditorRef.value?.saveNow(true)
    collabSaveMessage.value = t('note.collabSaved')
    if (noteStore.currentNote) {
      await noteStore.loadHistory(noteStore.currentNote.id)
    }
    ElMessage.success(t('collab.checkpointSaved'))
    return
  }

  const snapshot = buildSavePayload()
  if (!snapshot) {
    return
  }

  await persistPayload(snapshot)
  await noteStore.loadHistory(snapshot.noteId)
  ElMessage.success(t('note.saved'))
}

const handleSaveShortcut = (event: KeyboardEvent) => {
  if (!(event.ctrlKey || event.metaKey) || event.key.toLowerCase() !== 's') {
    return
  }

  event.preventDefault()
  void saveCurrentDocument().catch(() => undefined)
}

const syncDraft = async (noteId?: number) => {
  if (noteId && noteStore.currentNote?.id && (noteStore.currentNote.id !== noteId || noteStore.currentNote.spaceId !== spaceStore.currentSpaceId)) {
    await flushPendingSave('MANUAL')
  }

  if (noteId) {
    await noteStore.selectNote(noteId)
    if (noteStore.currentNote) {
      await alignDirectoryToNote(noteStore.currentNote)
    }
  } else if (!noteStore.currentNote && visibleNotes.value.length) {
    await noteStore.selectNote(visibleNotes.value[0].id)
    if (noteStore.currentNote) {
      await alignDirectoryToNote(noteStore.currentNote)
    }
  }
}

const hydrateFromCurrent = async () => {
  if (!noteStore.currentNote || noteStore.currentNote.spaceId !== spaceStore.currentSpaceId) {
    clearLocalDraftState()
    return
  }

  hydrating = true
  boundEditorReferenceKeys.value = {}
  shouldPrimeEditorReferences = true
  draft.title = noteStore.currentNote.title
  draft.content = noteStore.currentNote.content || ''
  draft.contentHtml = sanitizeEditorHtml(noteStore.currentNote.contentHtml || renderMarkdownLite(draft.content))
  draft.notebookId = noteStore.currentNote.notebookId
  draft.projectId = noteStore.currentNote.projectId || 0
  draft.tagIds = (noteStore.currentNote.tags || []).map((tag) => tag.id)
  restoreTitleAlignment()
  shareExpire.value = toLocalInputDateTime(noteStore.currentNote.shareExpire)
  collabSaveMessage.value = noteStore.currentNote.gmtModified
    ? t('note.collabSavedAt', { time: formatDateTime(noteStore.currentNote.gmtModified) })
    : ''
  lastSyncedDraftKey = buildTrackedDraftKey({
    spaceId: noteStore.currentNote.spaceId,
    noteId: noteStore.currentNote.id,
    title: noteStore.currentNote.title,
    content: noteStore.currentNote.content || '',
    contentHtml: sanitizeEditorHtml(noteStore.currentNote.contentHtml || renderMarkdownLite(noteStore.currentNote.content || '')),
    notebookId: noteStore.currentNote.notebookId,
    projectId: noteStore.currentNote.projectId || 0,
    tagIds: (noteStore.currentNote.tags || []).map((tag) => tag.id),
  })
  hydrating = false

  if (pendingFocusNewNoteId === noteStore.currentNote.id) {
    await nextTick()
    titleInputRef.value?.focus()
    titleInputRef.value?.select()
    pendingFocusNewNoteId = null
  } else if (isCollabEditable.value) {
    await nextTick()
    collabEditorRef.value?.focus()
  } else {
    await nextTick()
    richEditorRef.value?.focus()
  }
}

const clearLocalDraftState = () => {
  if (saveTimer) {
    window.clearTimeout(saveTimer)
    saveTimer = undefined
  }

  hydrating = true
  draft.title = ''
  draft.content = ''
  draft.contentHtml = ''
  draft.notebookId = 0
  draft.projectId = 0
  draft.tagIds = []
  boundEditorReferenceKeys.value = {}
  shouldPrimeEditorReferences = false
  shareExpire.value = ''
  newTagName.value = ''
  activeStyleTarget.value = 'editor'
  titleAlignment.value = 'left'
  titleDisplayStyle.color = ''
  titleDisplayStyle.fontFamily = ''
  titleDisplayStyle.fontSize = ''
  titleDisplayStyle.lineHeight = ''
  collabSaveMessage.value = ''
  collabStatusMessage.value = t('collab.connecting')
  lastSyncedDraftKey = ''
  hydrating = false
}

const handleCollabStatusChange = (payload: { message: string; status: string }) => {
  collabStatusMessage.value = payload.message
  if (payload.status === 'error' || payload.status === 'synced') {
    collabSaveMessage.value = payload.message
  }
}

const handleCollabPersisted = (note: Note) => {
  if (note.spaceId !== spaceStore.currentSpaceId) {
    return
  }

  const merged = noteStore.patchLocalNote(note, 'preserve')
  if (noteStore.currentNote?.id === merged.id && noteStore.currentNote.spaceId === merged.spaceId) {
    noteStore.currentNote = merged
  }

  collabSaveMessage.value = merged.gmtModified
    ? t('note.collabSavedAt', { time: formatDateTime(merged.gmtModified) })
    : t('note.collabContentSaved')
  if (historyDrawerOpen.value) {
    noteStore.loadHistory(merged.id).catch(() => undefined)
  }
  lastSyncedDraftKey = buildTrackedDraftKey({
    spaceId: merged.spaceId,
    noteId: merged.id,
    title: draft.title,
    content: draft.content,
    contentHtml: draft.contentHtml,
    notebookId: draft.notebookId,
    projectId: draft.projectId || 0,
    tagIds: [...draft.tagIds],
  })
}

const ensureWorkspaceReady = async (forceWorkspaceReload = false) => {
  const spaceId = spaceStore.currentSpaceId
  if (!spaceId) {
    return
  }

  const workspaceChanged = noteStore.workspaceSpaceId !== null && noteStore.workspaceSpaceId !== spaceId
  if (workspaceChanged) {
    clearLocalDraftState()
    noteStore.resetWorkspaceState()
  }

  searchKeyword.value = noteStore.query.keyword || ''
  if (!isTeamSpace.value) {
    noteStore.query.projectId = undefined
  }

  const asyncTasks: Array<Promise<unknown>> = []

  if (forceWorkspaceReload || workspaceChanged || noteStore.workspaceSpaceId !== spaceId || !noteStore.notebooks.length) {
    asyncTasks.push(noteStore.loadWorkspace())
  }

  if (isTeamSpace.value && (forceWorkspaceReload || !availableProjects.value.length)) {
    asyncTasks.push(projectStore.loadProjectOptions())
  }

  if (asyncTasks.length) {
    await Promise.all(asyncTasks)
  }

  const routeNoteId = Number(route.params.noteId)
  await syncDraft(Number.isNaN(routeNoteId) ? undefined : routeNoteId)
}

const createDefaultNotebook = async () => {
  await noteStore.createNotebook(t('note.defaultNotebookName'))
}

const createQuickNote = async () => {
  let notebookId = noteStore.query.notebookId || noteStore.currentNote?.notebookId || noteStore.firstNotebookId || 0
  if (!notebookId) {
    const notebook = await noteStore.createNotebook(t('note.defaultNotebookName'))
    notebookId = notebook?.id || 0
  }

  if (!notebookId) {
    return
  }

  noteStore.query.notebookId = notebookId
  noteStore.query.tagId = undefined

  const note = await noteStore.createNote({
    title: t('note.untitled'),
    notebookId,
    projectId: noteStore.query.projectId || draft.projectId || noteStore.currentNote?.projectId || undefined,
    content: '',
    contentHtml: sanitizeEditorHtml(renderMarkdownLite('')),
  })

  if (note) {
    pendingFocusNewNoteId = note.id
    creationHint.value = t('note.creationHint')
    clearHintLater()
    await router.push(`/app/notes/${note.id}`)
  }
}

const openNote = async (noteId: number) => {
  const targetNote = noteStore.sidebarNotes.find((note) => note.id === noteId) || noteStore.notes.find((note) => note.id === noteId)
  if (activeNoteId.value === noteId) {
    if (targetNote) {
      await alignDirectoryToNote(targetNote)
    }
    return
  }

  await flushPendingSave('MANUAL')
  if (targetNote) {
    await alignDirectoryToNote(targetNote)
  }
  await router.push(`/app/notes/${noteId}`)
}

const alignDirectoryToNote = async (note: Note) => {
  const shouldReload =
    noteStore.query.notebookId !== note.notebookId ||
    Boolean(noteStore.query.tagId) ||
    Boolean(noteStore.query.projectId) ||
    Boolean((noteStore.query.keyword || '').trim())

  noteStore.query.notebookId = note.notebookId
  noteStore.query.tagId = undefined
  noteStore.query.projectId = undefined
  noteStore.query.keyword = ''
  searchKeyword.value = ''
  noteStore.query.pageNum = 1

  if (shouldReload) {
    await noteStore.loadNotes()
  }
}

const openNotebookFolder = async (notebookId: number) => {
  await flushPendingSave('MANUAL')
  noteStore.query.notebookId = notebookId
  noteStore.query.tagId = undefined
  noteStore.query.keyword = ''
  searchKeyword.value = ''
  noteStore.query.pageNum = 1
  await noteStore.loadNotes()
}

const goParentNotebook = async () => {
  await flushPendingSave('MANUAL')
  noteStore.query.notebookId = currentNotebook.value?.parentId || undefined
  noteStore.query.tagId = undefined
  noteStore.query.keyword = ''
  searchKeyword.value = ''
  noteStore.query.pageNum = 1
  await noteStore.loadNotes()
}

const startNoteDrag = (event: DragEvent, noteId: number) => {
  if (!event.dataTransfer) {
    return
  }

  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', `note:${noteId}`)
}

const loadNotes = async () => {
  const keyword = searchKeyword.value.trim()
  noteStore.query.keyword = keyword
  if (keyword || noteStore.query.projectId) {
    noteStore.query.notebookId = undefined
  }
  noteStore.query.pageNum = 1
  await noteStore.loadNotes()
}

const loadFileReferenceOptions = async () => {
  const spaceId = spaceStore.currentSpaceId
  if (!spaceId) {
    fileReferenceFiles.value = []
    return
  }

  fileReferenceLoading.value = true
  try {
    const page = await fileService.page(spaceId, {
      pageNum: 1,
      pageSize: 50,
      keyword: fileReferenceKeyword.value.trim(),
      trashed: false,
    })
    fileReferenceFiles.value = page.list
  } finally {
    fileReferenceLoading.value = false
  }
}

const openFileReferenceDialog = async () => {
  if (!canEditCurrentNote.value) {
    return
  }

  fileReferenceDialogOpen.value = true
  fileReferenceKeyword.value = ''
  await loadFileReferenceOptions()
}

const managedImageExtensions = new Set(['apng', 'avif', 'gif', 'jpeg', 'jpg', 'png', 'svg', 'webp'])
const managedAudioExtensions = new Set(['aac', 'flac', 'm4a', 'mp3', 'oga', 'ogg', 'opus', 'wav', 'weba', 'webm'])

const extensionFromManagedFileValue = (value: string) => {
  const cleanValue = (value.split('?')[0] || '').split('#')[0] || ''
  const chunks = cleanValue.toLowerCase().split('.')
  return chunks.length > 1 ? chunks.pop() || '' : ''
}

const managedFileCandidateValues = (file: ManagedFile) => [
  file.mimeType || '',
  file.fileName || '',
  file.displayName || '',
  file.downloadUrl || '',
  file.previewUrl || '',
]

const managedFileExtension = (file: ManagedFile) => {
  const candidate = managedFileCandidateValues(file).find((value) => extensionFromManagedFileValue(value))
  return candidate ? extensionFromManagedFileValue(candidate) : ''
}

const isManagedImageFile = (file: ManagedFile) => Boolean(file.mimeType?.startsWith('image/') || managedImageExtensions.has(managedFileExtension(file)))

const isManagedAudioFile = (file: ManagedFile) =>
  Boolean(file.mimeType?.startsWith('audio/') || managedFileCandidateValues(file).some((value) => managedAudioExtensions.has(extensionFromManagedFileValue(value))))

const editorReferenceKey = (fileId: number) => `rich-file:${fileId}`

const bindManagedFileReference = (file: ManagedFile) => {
  const spaceId = spaceStore.currentSpaceId
  const noteId = noteStore.currentNote?.id
  if (!spaceId || !noteId || !file.attachmentId) {
    return
  }

  const referenceKey = editorReferenceKey(file.id)
  if (boundEditorReferenceKeys.value[referenceKey] === file.attachmentId) {
    return
  }
  boundEditorReferenceKeys.value = {
    ...boundEditorReferenceKeys.value,
    [referenceKey]: file.attachmentId,
  }

  const referencePayload = {
    businessType: 'NOTE' as const,
    businessId: noteId,
    referenceKey,
  }

  void attachmentService
    .unbind(spaceId, file.attachmentId, referencePayload)
    .catch(() => undefined)
    .then(() =>
      attachmentService.bind(spaceId, {
        attachmentId: file.attachmentId,
        ...referencePayload,
      }),
    )
    .catch(() => undefined)
}

const syncEditorFileReferences = (references: EditorFileReference[]) => {
  const spaceId = spaceStore.currentSpaceId
  const noteId = noteStore.currentNote?.id
  if (!spaceId || !noteId) {
    boundEditorReferenceKeys.value = {}
    return
  }

  const nextReferences = references.reduce<Record<string, number>>((nextMap, reference) => {
    nextMap[editorReferenceKey(reference.fileId)] = reference.attachmentId
    return nextMap
  }, {})

  if (shouldPrimeEditorReferences) {
    boundEditorReferenceKeys.value = nextReferences
    shouldPrimeEditorReferences = false
    return
  }

  const removedReferences = Object.entries(boundEditorReferenceKeys.value).filter(([referenceKey]) => !(referenceKey in nextReferences))
  boundEditorReferenceKeys.value = Object.fromEntries(
    Object.entries(boundEditorReferenceKeys.value).filter(([referenceKey]) => referenceKey in nextReferences),
  )

  for (const [referenceKey, attachmentId] of removedReferences) {
    void attachmentService
      .unbind(spaceId, attachmentId, {
        businessType: 'NOTE',
        businessId: noteId,
        referenceKey,
      })
      .catch(() => undefined)
  }
}

const resolveEditorFile = async (file: ManagedFile) => {
  const spaceId = spaceStore.currentSpaceId
  if (!spaceId) {
    return file
  }

  let resolvedFile = file

  if (!resolvedFile.mimeType || !resolvedFile.fileName) {
    try {
      const detailFile = await fileService.detail(spaceId, file.id)
      resolvedFile = {
        ...resolvedFile,
        ...detailFile,
        attachmentId: file.attachmentId || detailFile.attachmentId,
      }
    } catch {
      resolvedFile = file
    }
  }

  if (resolvedFile.downloadUrl || (!isManagedImageFile(resolvedFile) && !isManagedAudioFile(resolvedFile))) {
    return resolvedFile
  }

  try {
    const previewFile = await fileService.previewUrl(spaceId, file.id)
    return {
      ...resolvedFile,
      ...previewFile,
      attachmentId: resolvedFile.attachmentId || previewFile.attachmentId,
      downloadUrl: previewFile.downloadUrl || previewFile.previewUrl || resolvedFile.downloadUrl,
      previewUrl: previewFile.previewUrl || resolvedFile.previewUrl,
    }
  } catch {
    return resolvedFile
  }
}

const insertFileReference = async (file: ManagedFile) => {
  const editorFile = await resolveEditorFile(file)
  if (isCollabMode.value) {
    collabEditorRef.value?.insertManagedFile(editorFile)
  } else {
    richEditorRef.value?.insertManagedFile(editorFile)
  }
  fileReferenceDialogOpen.value = false
  bindManagedFileReference(editorFile)
}

const shareCurrentNote = async () => {
  const sharedNote = await noteStore.shareCurrentNote(fromLocalInputDateTime(shareExpire.value))
  if (sharedNote?.shareCode) {
    await navigator.clipboard.writeText(`${window.location.origin}/public/notes/${sharedNote.shareCode}`)
    ElMessage.success(t('note.shareCopied'))
  }
  shareDialogOpen.value = false
}

const exportCurrentNote = async (command: string | number | object) => {
  const format = typeof command === 'string' ? command : ''
  if (!isNoteExportFormat(format) || !noteStore.currentNote || !spaceStore.currentSpaceId) {
    return
  }

  await ensureLatestContentBeforeExport()
  const spaceId = spaceStore.currentSpaceId
  const note = noteStore.currentNote
  if (!note || !spaceId) {
    return
  }
  const blob = await noteService.exportNote(spaceId, note.id, format)
  downloadBlob(blob, `${sanitizeFileName(note.title || 'Notask Flow 笔记')}.${exportExtension(format)}`)
  ElMessage.success('导出完成')
}

const ensureLatestContentBeforeExport = async () => {
  if (!canEditCurrentNote.value) {
    return
  }
  await flushPendingSave('MANUAL')
  if (isCollabMode.value) {
    await collabEditorRef.value?.saveNow(false)
  }
}

const isNoteExportFormat = (value: string): value is NoteExportFormat =>
  value === 'pdf' || value === 'word' || value === 'image'

const exportExtension = (format: NoteExportFormat) => {
  if (format === 'word') {
    return 'docx'
  }
  if (format === 'image') {
    return 'png'
  }
  return 'pdf'
}

const sanitizeFileName = (value: string) => value.replace(/[\\/:*?"<>|\r\n]+/g, '_').trim() || 'Notask Flow 笔记'

const downloadBlob = (blob: Blob, fileName: string) => {
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = fileName
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  URL.revokeObjectURL(url)
}

const restoreHistory = async (version: number) => {
  if (!noteStore.currentNote) {
    return
  }

  await noteStore.restoreHistory(noteStore.currentNote.id, version)
  historyDrawerOpen.value = false
  await hydrateFromCurrent()
}

const toggleTag = (tagId: number) => {
  if (!canEditCurrentNote.value) {
    return
  }

  if (draft.tagIds.includes(tagId)) {
    draft.tagIds = draft.tagIds.filter((item) => item !== tagId)
    return
  }

  draft.tagIds = [...draft.tagIds, tagId]
}

const submitEditorTag = async () => {
  if (!canEditCurrentNote.value) {
    return
  }

  const name = newTagName.value.trim().replace(/^#/, '')
  if (!name) {
    return
  }

  const existingTag = noteStore.tags.find((tag) => tag.name.toLowerCase() === name.toLowerCase())
  const targetTag = existingTag || (await noteStore.createTag(name))
  if (targetTag && !draft.tagIds.includes(targetTag.id)) {
    draft.tagIds = [...draft.tagIds, targetTag.id]
  }

  newTagName.value = ''
}

const updateSelection = (transform: (selected: string) => { value: string; selectFrom?: number; selectTo?: number }) => {
  if (!canEditCurrentNote.value) {
    return
  }

  if (viewMode.value !== 'edit') {
    viewMode.value = 'edit'
  }

  if (isCollabMode.value) {
    collabEditorRef.value?.replaceSelection(transform)
    return
  }

  const editor = editorRef.value
  if (!editor) {
    return
  }

  const start = editor.selectionStart ?? draft.content.length
  const end = editor.selectionEnd ?? draft.content.length
  const selected = draft.content.slice(start, end)
  const prefix = draft.content.slice(0, start)
  const suffix = draft.content.slice(end)
  const next = transform(selected)

  draft.content = `${prefix}${next.value}${suffix}`

  nextTick(() => {
    editor.focus()
    const selectionStart = typeof next.selectFrom === 'number' ? start + next.selectFrom : start
    const selectionEnd = typeof next.selectTo === 'number' ? start + next.selectTo : start + next.value.length
    editor.setSelectionRange(selectionStart, selectionEnd)
  }).catch(() => undefined)
}

const applyHeading = (level: number) => {
  updateSelection((selected) => {
    const content = selected || 'Heading'
    return {
      value: `${'#'.repeat(level)} ${content}`,
      selectFrom: level + 1,
      selectTo: level + 1 + content.length,
    }
  })
}

const applyInline = (type: InlineActionType) => {
  const wrappers = {
    bold: '**',
    italic: '*',
    strike: '~~',
    code: '`',
  } as const

  updateSelection((selected) => {
    const marker = wrappers[type]
    const content = selected || (type === 'code' ? 'code' : 'text')
    return {
      value: `${marker}${content}${marker}`,
      selectFrom: marker.length,
      selectTo: marker.length + content.length,
    }
  })
}

const applyList = (type: ListActionType) => {
  updateSelection((selected) => {
    const lines = (selected || (type === 'quote' ? 'Quoted text' : 'List item')).split(/\r?\n/)

    const value = lines
      .map((line, index) => {
        switch (type) {
          case 'list':
            return `- ${line}`
          case 'ordered':
            return `${index + 1}. ${line}`
          case 'task':
            return `- [ ] ${line}`
          case 'quote':
            return `> ${line}`
        }
      })
      .join('\n')

    const prefixLength =
      type === 'ordered'
        ? 3
        : type === 'task'
          ? 6
          : 2

    return {
      value,
      selectFrom: prefixLength,
      selectTo: value.length,
    }
  })
}

const applyInsert = (type: InsertActionType) => {
  updateSelection((selected) => {
    if (type === 'codeblock') {
      const content = selected || 'code'
      const value = `\`\`\`\n${content}\n\`\`\``
      return {
        value,
        selectFrom: 4,
        selectTo: 4 + content.length,
      }
    }

    if (type === 'divider') {
      return {
        value: '\n---\n',
        selectFrom: 5,
        selectTo: 5,
      }
    }

    const value = type === 'image' ? '![alt](https://)' : '[text](https://)'
    return {
      value,
      selectFrom: type === 'image' ? 2 : 1,
      selectTo: type === 'image' ? 5 : 5,
    }
  })
}

const handleResize = (event: PointerEvent) => {
  const container = resizerContainerRef.value
  if (!container || !isDragging.value) {
    return
  }

  const rect = container.getBoundingClientRect()
  const nextWidth = ((event.clientX - rect.left) / rect.width) * 100
  editorWidth.value = Math.min(70, Math.max(30, nextWidth))
}

const stopResize = () => {
  isDragging.value = false
  window.removeEventListener('pointermove', handleResize)
  window.removeEventListener('pointerup', stopResize)
}

const startResize = (event: PointerEvent) => {
  if (viewMode.value !== 'edit') {
    return
  }

  isDragging.value = true
  handleResize(event)
  window.addEventListener('pointermove', handleResize)
  window.addEventListener('pointerup', stopResize)
}

onMounted(() => {
  ensureWorkspaceReady(noteStore.workspaceSpaceId !== spaceStore.currentSpaceId)
    .then(() => hydrateFromCurrent())
    .catch(() => undefined)
  window.addEventListener('keydown', handleSaveShortcut)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleSaveShortcut)
  stopResize()
  void flushPendingSave('MANUAL').catch(() => undefined)
  if (searchTimer) {
    window.clearTimeout(searchTimer)
  }
  if (hintTimer) {
    window.clearTimeout(hintTimer)
  }
})

watch(
  () => historyDrawerOpen.value,
  (open) => {
    if (open && noteStore.currentNote) {
      noteStore.loadHistory(noteStore.currentNote.id).catch(() => undefined)
    }
  },
)

watch(
  () => spaceStore.currentSpaceId,
  (spaceId, previousSpaceId) => {
    if (!spaceId || spaceId === previousSpaceId) {
      return
    }

    clearLocalDraftState()
    noteStore.resetWorkspaceState()
    ensureWorkspaceReady(true).catch(() => undefined)
  },
)

watch(
  () => route.params.noteId,
  async (noteId) => {
    const resolvedId = Number(noteId)
    if (!Number.isNaN(resolvedId)) {
      await syncDraft(resolvedId)
    }
  },
)

watch(currentNoteHydrationKey, (key) => {
  if (!key) {
    return
  }

  hydrateFromCurrent().catch(() => undefined)
})

watch(
  () => noteStore.currentNote?.id,
  (noteId) => {
    if (noteId && historyDrawerOpen.value) {
      noteStore.loadHistory(noteId).catch(() => undefined)
    }
  },
)

watch(
  () => ({
    collabMode: isCollabMode.value,
    content: isCollabMode.value ? '' : draft.content,
    contentHtml: isCollabMode.value ? '' : draft.contentHtml,
    notebookId: draft.notebookId,
    projectId: draft.projectId,
    tagIds: JSON.stringify(draft.tagIds),
    title: draft.title,
  }),
  () => {
    if (hydrating || !noteStore.currentNote) {
      return
    }

    const snapshot = buildSavePayload()
    if (!snapshot) {
      return
    }

    const snapshotKey = buildTrackedDraftKey({
      spaceId: snapshot.spaceId,
      noteId: snapshot.noteId,
      title: snapshot.payload.title,
      content: snapshot.payload.content || '',
      contentHtml: snapshot.payload.contentHtml || '',
      notebookId: snapshot.payload.notebookId,
      projectId: snapshot.payload.projectId || 0,
      tagIds: snapshot.payload.tagIds || [],
    })

    if (snapshotKey === lastSyncedDraftKey) {
      return
    }

    if (saveTimer) {
      window.clearTimeout(saveTimer)
    }

    saveTimer = window.setTimeout(() => {
      persistPayload(snapshot).catch(() => undefined)
    }, AUTO_SAVE_DELAY_MS)
  },
)

watch(searchKeyword, () => {
  if (searchTimer) {
    window.clearTimeout(searchTimer)
  }

  searchTimer = window.setTimeout(() => {
    loadNotes().catch(() => undefined)
  }, 250)
})
</script>

<style scoped>
.resizer-container {
  display: flex;
  width: 100%;
  height: 100%;
}

.resizer-handle {
  width: 4px;
  cursor: col-resize;
  background-color: transparent;
  transition: background-color 0.2s, width 0.2s;
  position: relative;
  z-index: 10;
  flex-shrink: 0;
}

.resizer-handle:hover,
.resizer-handle:active {
  background-color: #ff8a65;
  width: 4px;
}

.resizer-handle::after {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 1px;
  height: 40px;
  background-color: #ddc0b8;
  border-radius: 99px;
  transition: height 0.2s;
}

.resizer-handle:hover::after {
  height: 100%;
  width: 2px;
  background-color: #ff8a65;
}

.note-toolbar-host {
  border-bottom: 1px solid rgba(221, 192, 184, 0.28);
  flex-shrink: 0;
  min-height: 50px;
  width: 100%;
}

.note-toolbar-host-hidden {
  display: none;
}

.note-title-editor {
  padding: 2rem 0 1rem;
}

.note-title-input {
  background: transparent;
  border: 0;
  color: var(--on-surface);
  font-family: Newsreader, serif;
  font-size: clamp(2rem, 4vw, 3.2rem);
  font-weight: 680;
  line-height: 1.12;
  outline: none;
  width: 100%;
}

.note-title-input::placeholder {
  color: color-mix(in srgb, var(--on-surface-variant) 48%, transparent);
}

.note-title-align-toolbar {
  align-items: center;
  background: var(--surface-container-lowest);
  border: 1px solid rgba(221, 192, 184, 0.68);
  border-radius: 9999px;
  display: inline-flex;
  gap: 0.2rem;
  height: 32px;
  padding: 0 0.25rem;
}

.note-title-align-button {
  align-items: center;
  border-radius: 9999px;
  color: var(--on-surface-variant);
  display: inline-flex;
  height: 26px;
  justify-content: center;
  transition: background-color 0.18s ease, color 0.18s ease, transform 0.18s ease;
  width: 28px;
}

.note-title-align-button:hover:not(:disabled),
.note-title-align-button-active {
  background: var(--primary-fixed);
  color: var(--primary);
}

.note-title-align-button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.note-title-align-button .material-symbols-outlined {
  font-size: 18px;
}

.notes-preview-body :deep(.markdown-empty) {
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-size: var(--note-preview-font-size, 16px);
  line-height: var(--note-preview-line-height, 1.5);
  color: var(--on-surface-variant);
  opacity: 0.7;
}

.notes-preview-body :deep(p) {
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-size: var(--note-preview-font-size, 16px);
  line-height: var(--note-preview-line-height, 1.5);
  color: var(--on-surface-variant);
  opacity: 0.9;
  margin-bottom: var(--note-preview-block-gap, 0.63rem);
  min-height: 1.5em;
  white-space: pre-wrap;
}

.notes-preview-body :deep(p:last-child) {
  margin-bottom: 0;
}

.notes-preview-body :deep(p[data-first-line-indent='true']) {
  text-indent: 2em;
}

.notes-preview-body :deep([data-text-align='center']) {
  text-align: center;
}

.notes-preview-body :deep([data-text-align='right']) {
  text-align: right;
}

.notes-preview-body :deep(p:empty::before) {
  content: '\00a0';
}

.notes-preview-body :deep(h1) {
  font-family: 'Newsreader', serif;
  font-size: 2.5rem;
  line-height: 1.1;
  color: var(--on-surface);
  margin: 0 0 2rem;
}

.notes-preview-body :deep(h2) {
  font-family: 'Newsreader', serif;
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--on-surface);
  margin: 3rem 0 2rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid rgba(221, 192, 184, 0.3);
}

.notes-preview-body :deep(h3) {
  font-family: 'Newsreader', serif;
  font-size: 1.25rem;
  color: var(--on-surface);
  margin: 2rem 0 1rem;
}

.notes-preview-body :deep(ul),
.notes-preview-body :deep(ol) {
  margin: var(--note-preview-list-gap, 0.42rem) 0 var(--note-preview-block-gap, 0.63rem);
  padding: 0;
  display: grid;
  gap: var(--note-preview-list-gap, 0.42rem);
}

.notes-preview-body :deep(ul) {
  list-style: none;
}

.notes-preview-body :deep(ol) {
  list-style: decimal;
  padding-left: 1.5rem;
}

.notes-preview-body :deep(li) {
  position: relative;
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-size: var(--note-preview-font-size, 16px);
  line-height: var(--note-preview-line-height, 1.5);
  color: var(--on-surface-variant);
}

.notes-preview-body :deep(ul li) {
  padding-left: 3rem;
}

.notes-preview-body :deep(ul li)::before {
  content: 'check';
  font-family: 'Material Symbols Outlined';
  font-size: 14px;
  position: absolute;
  left: 0;
  top: 0.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.5rem;
  height: 1.5rem;
  border-radius: 9999px;
  background: rgba(255, 219, 208, 0.4);
  color: var(--primary);
}

.notes-preview-body :deep(ul.task-list li::before) {
  content: 'check_box_outline_blank';
}

.notes-preview-body :deep(ul.task-list li[data-task-state='checked']::before) {
  content: 'check_box';
}

.notes-preview-body :deep(strong) {
  color: inherit;
  font-weight: 600;
}

.notes-preview-body :deep(em) {
  font-style: italic;
}

.notes-preview-body :deep(s) {
  text-decoration: line-through;
}

.notes-preview-body :deep(blockquote) {
  margin-top: 4rem;
  border-left: 4px solid var(--primary);
  border-radius: 0 1.5rem 1.5rem 0;
  background: #faf9f8;
  padding: 2.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.notes-preview-body :deep(blockquote p) {
  margin: 0 0 calc(var(--note-preview-block-gap, 0.63rem) * 0.8);
  font-family: 'Newsreader', serif;
  font-size: 1.25rem;
  line-height: var(--note-preview-line-height, 1.5);
  color: inherit;
  font-style: italic;
  opacity: 1;
}

.notes-preview-body :deep(blockquote p:last-child) {
  margin-bottom: 0;
}

.notes-preview-body :deep(code) {
  border-radius: 0.5rem;
  background: rgba(233, 225, 222, 0.5);
  padding: 0.15rem 0.45rem;
  color: var(--primary);
  font-size: 0.9rem;
}

.notes-preview-body :deep(pre) {
  margin: 2rem 0;
  overflow-x: auto;
  border-radius: 1.25rem;
  background: #241f1d;
  padding: 1rem;
  color: white;
}

.notes-preview-body :deep(hr) {
  margin: 3rem 0;
  border: 0;
  border-top: 1px solid rgba(221, 192, 184, 0.7);
}
</style>
