<template>
  <aside
    class="fixed bottom-0 left-0 top-16 z-30 hidden w-64 flex-col border-r border-outline-variant/40 bg-surface-container/95 p-4 font-title-serif text-on-surface md:flex"
  >
    <div class="mb-8 px-2">
      <div class="mb-1 flex items-center gap-3">
        <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-white shadow-sm">
          <span class="material-symbols-outlined text-sm">auto_awesome</span>
        </div>
        <div>
          <h2 class="text-label-bold leading-tight">{{ t('sidebar.personalSpace') }}</h2>
          <p class="text-caption text-stone-500">{{ t('sidebar.personalSubtitle') }}</p>
        </div>
      </div>
    </div>

    <nav class="hide-scrollbar flex-1 space-y-1 overflow-y-auto pr-1">
      <RouterLink
        v-for="item in navigation"
        :key="item.to"
        :to="item.to"
        class="flex items-center px-3 py-2 transition-all"
        :class="
          $route.path.startsWith(item.active)
            ? 'rounded-lg bg-surface text-primary shadow-sm'
            : 'text-on-surface-variant hover:bg-surface/70 hover:text-primary'
        "
      >
        <span class="material-symbols-outlined mr-3">{{ item.icon }}</span>
        <span :class="$route.path.startsWith(item.active) ? 'font-semibold' : ''">{{ item.label }}</span>
      </RouterLink>

      <div class="pb-2 pt-6">
        <div class="mb-2 flex items-center justify-between px-3">
          <span class="text-caption font-bold uppercase tracking-widest text-stone-400">{{ t('sidebar.notebookTree') }}</span>
          <div class="flex items-center gap-2">
            <button
              v-if="selectedNotebook"
              class="material-symbols-outlined text-sm transition-colors hover:text-red-500"
              type="button"
              @click="confirmDeleteNotebook(selectedNotebook)"
            >
              delete
            </button>
            <button
              class="material-symbols-outlined text-sm transition-colors hover:text-primary"
              type="button"
              @click="startCreateNotebook"
            >
              add
            </button>
          </div>
        </div>

        <div class="space-y-1">
          <div
            v-for="row in visibleSidebarRows"
            :key="`${row.kind}-${row.id}`"
            class="group flex w-full items-center rounded-lg py-1.5 pr-2 text-left text-body-secondary transition-all"
            :class="rowClasses(row)"
            :draggable="true"
            :style="{ paddingLeft: `${12 + row.depth * 20}px` }"
            role="button"
            tabindex="0"
            @click="handleRowClick(row)"
            @dragend="handleDragEnd"
            @dragover="row.kind === 'notebook' ? handleNotebookDragOver($event, row.notebook.id) : undefined"
            @dragstart="startRowDrag($event, row)"
            @drop="row.kind === 'notebook' ? dropOnNotebook($event, row.notebook.id) : undefined"
            @keydown.enter.prevent="handleRowClick(row)"
          >
            <button
              v-if="row.kind === 'notebook' && row.expandable"
              class="mr-1 flex h-5 w-5 items-center justify-center rounded-full text-stone-400 transition hover:bg-white/60 hover:text-primary"
              type="button"
              @click.stop="toggleNotebookExpanded(row.notebook.id)"
            >
              <span class="material-symbols-outlined text-[16px]">
                {{ isNotebookCollapsed(row.notebook.id) ? 'chevron_right' : 'expand_more' }}
              </span>
            </button>
            <span v-else class="mr-1 inline-block h-5 w-5"></span>

            <span class="material-symbols-outlined mr-2 text-sm">
              {{
                row.kind === 'notebook'
                  ? isNotebookCollapsed(row.notebook.id)
                    ? 'folder'
                    : 'folder_open'
                  : 'description'
              }}
            </span>
            <span class="truncate">{{ row.label }}</span>
            <span
              class="material-symbols-outlined ml-auto text-[16px] text-stone-300 opacity-0 transition-opacity group-hover:opacity-100"
            >
              drag_indicator
            </span>
          </div>

          <div v-if="creatingNotebook" class="flex items-center px-3 py-1.5 text-body-secondary text-stone-600">
            <span class="mr-1 inline-block h-5 w-5"></span>
            <span class="material-symbols-outlined mr-2 text-sm text-primary">create_new_folder</span>
            <input
              ref="notebookInputRef"
              v-model="newNotebookName"
              class="w-full border-none bg-transparent p-0 text-body-secondary outline-none focus:ring-0"
              :placeholder="t('sidebar.notebookPlaceholder')"
              @blur="handleNotebookBlur"
              @keydown.enter.prevent="submitCreateNotebook"
              @keydown.esc.prevent="cancelCreateNotebook"
            />
          </div>

          <div
            v-if="draggingItem?.kind === 'notebook'"
            class="mt-2 rounded-xl border border-dashed px-3 py-3 text-center text-[11px] font-bold uppercase tracking-[0.18em] transition-all"
            :class="
              dragTargetMode === 'root'
                ? 'border-primary bg-primary-fixed/30 text-primary'
                : 'border-outline-variant/40 text-stone-400'
            "
            @dragover="handleRootDragOver"
            @drop="dropNotebookToRoot"
          >
            {{ t('sidebar.dropHereForTopLevel') }}
          </div>

          <button
            v-if="hasMoreNotebookRoots"
            class="w-full rounded-[1.1rem] border border-outline-variant/30 bg-white/50 px-3 py-2 text-left text-sm text-primary transition hover:bg-white/80"
            type="button"
            @click="notebookDialogOpen = true"
          >
            {{ t('sidebar.more') }}
          </button>
        </div>
      </div>

      <div class="pb-2 pt-4">
        <div class="mb-2 flex items-center justify-between px-3">
          <span class="text-caption font-bold uppercase tracking-widest text-stone-400">{{ t('sidebar.tagGroups') }}</span>
          <div class="flex items-center gap-2">
            <button
              v-if="selectedTag"
              class="material-symbols-outlined text-sm transition-colors hover:text-red-500"
              type="button"
              @click="confirmDeleteTag(selectedTag)"
            >
              delete
            </button>
            <button
              class="material-symbols-outlined text-sm transition-colors hover:text-primary"
              type="button"
              @click="startCreateTag"
            >
              add
            </button>
          </div>
        </div>

        <div class="space-y-1">
          <div
            v-for="tag in visibleTagGroups"
            :key="tag.id"
            class="group flex w-full items-center rounded-lg px-3 py-1.5 text-left text-body-secondary transition-all"
            :class="
              isNotesRoute && noteStore.query.tagId === tag.id
                ? 'bg-white text-primary shadow-sm'
                : 'text-stone-600 hover:bg-white/60 hover:text-primary'
            "
            role="button"
            tabindex="0"
            @click="filterByTag(tag.id)"
            :draggable="true"
            @dragend="handleDragEnd"
            @dragstart="startTagDrag($event, tag)"
            @keydown.enter.prevent="filterByTag(tag.id)"
          >
            <span class="material-symbols-outlined mr-2 text-sm">sell</span>
            <span class="truncate">#{{ tag.name }}</span>
            <span class="ml-auto rounded-full bg-white/70 px-2 py-0.5 text-[10px] font-bold text-stone-400">
              {{ tag.count }}
            </span>
            <button
              class="material-symbols-outlined ml-2 text-[16px] text-stone-300 opacity-0 transition-opacity group-hover:opacity-100 hover:text-red-500"
              type="button"
              @click.stop="confirmDeleteTag(tag)"
            >
              delete
            </button>
          </div>

          <div v-if="creatingTag" class="flex items-center px-3 py-1.5 text-body-secondary text-stone-600">
            <span class="material-symbols-outlined mr-2 text-sm text-primary">sell</span>
            <input
              ref="tagInputRef"
              v-model="newTagName"
              class="w-full border-none bg-transparent p-0 text-body-secondary outline-none focus:ring-0"
              :placeholder="t('sidebar.tagPlaceholder')"
              @blur="handleTagBlur"
              @keydown.enter.prevent="submitCreateTag"
              @keydown.esc.prevent="cancelCreateTag"
            />
          </div>

          <button
            v-if="hasMoreTagGroups"
            class="w-full rounded-[1.1rem] border border-outline-variant/30 bg-white/50 px-3 py-2 text-left text-sm text-primary transition hover:bg-white/80"
            type="button"
            @click="tagDialogOpen = true"
          >
            {{ t('sidebar.more') }}
          </button>
        </div>
      </div>

      <div class="pb-2 pt-4">
        <div class="mb-2 px-3">
          <span class="text-caption font-bold uppercase tracking-widest text-stone-400">{{ t('sidebar.recycleBin') }}</span>
        </div>
        <div
          class="mx-2 rounded-2xl border border-dashed px-4 py-4 transition-all"
          :class="
            trashActive
              ? 'border-red-400 bg-red-50 text-red-500 shadow-sm'
              : 'border-outline-variant/40 bg-white/40 text-stone-500'
          "
          @dragenter.prevent="handleTrashDragOver"
          @dragleave="handleTrashDragLeave"
          @dragover="handleTrashDragOver"
          @drop="dropToTrash"
        >
          <div class="flex items-center gap-3">
            <span class="material-symbols-outlined text-[18px]">delete</span>
            <div>
              <div class="text-sm font-semibold">{{ t('sidebar.dropHereToDelete') }}</div>
              <div class="text-[11px] tracking-[0.08em] opacity-80">{{ t('sidebar.dropNotesToDelete') }}</div>
            </div>
          </div>
        </div>
      </div>
    </nav>

    <div class="mt-auto">
      <div v-if="showRandomWalkCard" class="mb-4 rounded-xl border border-outline-variant/20 bg-white/60 p-4 shadow-sm">
        <div class="mb-2 flex items-center justify-between">
          <span class="text-label-bold text-primary">{{ t('sidebar.randomWalk') }}</span>
          <button
            class="material-symbols-outlined text-sm text-stone-400 transition hover:text-stone-600"
            type="button"
            @click.stop="showRandomWalkCard = false"
          >
            close
          </button>
        </div>
        <button class="block w-full text-left" type="button" @click="openRandomWalk">
          <div
            class="mb-2 h-24 rounded-lg bg-[radial-gradient(circle_at_top_left,_rgba(255,138,101,0.28),_transparent_42%),linear-gradient(135deg,_#f7ede8,_#efe6e3)]"
          ></div>
          <p class="text-sm font-medium text-on-surface">{{ randomWalkTitle }}</p>
          <p class="mt-2 text-caption italic text-stone-600">"{{ dailyQuote }}"</p>
        </button>
      </div>

      <button
        class="flex w-full items-center justify-center rounded-lg bg-primary py-2 font-bold text-white shadow-md transition-transform active:scale-95"
        type="button"
        @click="startCreateNotebook"
      >
        <span class="material-symbols-outlined mr-2">create_new_folder</span>
        {{ t('sidebar.newFolder') }}
      </button>
    </div>

    <el-dialog v-model="notebookDialogOpen" append-to-body :title="t('sidebar.fullNoteTree')" width="560px">
      <div class="hide-scrollbar max-h-[62vh] space-y-3 overflow-y-auto pr-1">
        <button
          v-for="row in allSidebarRows"
          :key="`dialog-${row.kind}-${row.id}`"
          class="flex w-full items-center rounded-[1.25rem] border border-outline-variant/20 bg-surface-container-lowest/80 px-4 py-3 text-left text-sm shadow-sm transition hover:-translate-y-0.5 hover:bg-white"
          :style="{ paddingLeft: `${16 + row.depth * 22}px` }"
          type="button"
          @click="handleDialogRowClick(row)"
        >
          <span class="material-symbols-outlined mr-3 text-[18px] text-primary/70">
            {{
              row.kind === 'notebook'
                ? isNotebookCollapsed(row.notebook.id)
                  ? 'folder'
                  : 'folder_open'
                : 'description'
            }}
          </span>
          <span class="truncate text-on-surface">{{ row.label }}</span>
          <span
            v-if="row.kind === 'notebook' && row.expandable"
            class="ml-auto rounded-full bg-surface-container px-2 py-1 text-[10px] font-bold uppercase tracking-[0.16em] text-on-surface-variant"
          >
            {{ isNotebookCollapsed(row.notebook.id) ? t('sidebar.closed') : t('sidebar.open') }}
          </span>
        </button>
      </div>
    </el-dialog>

    <el-dialog v-model="tagDialogOpen" append-to-body :title="t('sidebar.allTags')" width="460px">
      <div class="hide-scrollbar max-h-[62vh] space-y-3 overflow-y-auto pr-1">
        <div
          v-for="tag in tagGroups"
          :key="`dialog-tag-${tag.id}`"
          class="flex w-full items-center rounded-[1.25rem] border border-outline-variant/20 bg-surface-container-lowest/80 px-4 py-3 text-left text-sm shadow-sm transition hover:-translate-y-0.5 hover:bg-white"
          role="button"
          tabindex="0"
          @click="handleDialogTagClick(tag.id)"
          @keydown.enter.prevent="handleDialogTagClick(tag.id)"
        >
          <div class="min-w-0 flex-1">
            <div class="truncate font-medium text-on-surface">#{{ tag.name }}</div>
            <div class="mt-1 text-[11px] uppercase tracking-[0.16em] text-on-surface-variant">
              {{ t('sidebar.notesCount', { count: tag.count }) }}
            </div>
          </div>
          <button
            class="material-symbols-outlined mr-3 text-[18px] text-stone-400 transition hover:text-red-500"
            type="button"
            @click.stop="confirmDeleteTag(tag)"
          >
            delete
          </button>
          <span class="rounded-full bg-surface-container px-2 py-1 text-[10px] font-bold uppercase tracking-[0.16em] text-on-surface-variant">
            {{ t('sidebar.pick') }}
          </span>
        </div>
      </div>
    </el-dialog>
  </aside>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, nextTick, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from '@/i18n'
import { useNoteStore } from '@/stores/note'
import { useSpaceStore } from '@/stores/space'
import type { Note, Notebook, Tag } from '@/types/app'

type SidebarNotebookRow = {
  kind: 'notebook'
  id: number
  depth: number
  label: string
  notebook: Notebook
  expandable: boolean
}

type SidebarNoteRow = {
  kind: 'note'
  id: number
  depth: number
  label: string
  note: Note
}

type SidebarRow = SidebarNotebookRow | SidebarNoteRow
type DraggingItem = { kind: 'notebook' | 'note' | 'tag'; id: number } | null

interface TagGroupItem extends Tag {
  count: number
}

const MAX_VISIBLE_ROOT_NOTEBOOKS = 6
const MAX_VISIBLE_TAG_GROUPS = 6
const RANDOM_QUOTES = [
  'Slow progress is still progress worth protecting.',
  'A calm mind builds better work than a rushed one.',
  'What you repeat quietly becomes your strength.',
  'Small focused steps can carry very ambitious dreams.',
  'Rest is not retreat, it is part of the rhythm of good work.',
  'Clarity grows when you stay with the work a little longer.',
  'A steady pace often outlasts bursts of pressure.',
]

const noteStore = useNoteStore()
const router = useRouter()
const spaceStore = useSpaceStore()
const { t } = useI18n()
const tf = (key: string, fallback: string) => {
  const translated = t(key)
  return translated === key ? fallback : translated
}

const creatingNotebook = ref(false)
const creatingTag = ref(false)
const newNotebookName = ref('')
const newTagName = ref('')
const notebookInputRef = ref<HTMLInputElement | null>(null)
const tagInputRef = ref<HTMLInputElement | null>(null)
const collapsedNotebookIds = ref<number[]>([])
const draggingItem = ref<DraggingItem>(null)
const dragTargetNotebookId = ref<number | null>(null)
const dragTargetMode = ref<'child' | 'note' | 'root' | null>(null)
const movingItem = ref(false)
const trashActive = ref(false)
const notebookDialogOpen = ref(false)
const tagDialogOpen = ref(false)
const showRandomWalkCard = ref(true)

const navigation = computed(() => [
  { to: '/app/notes', active: '/app/notes', label: t('nav.notes'), icon: 'description' },
  { to: '/app/tasks', active: '/app/tasks', label: t('nav.tasks'), icon: 'check_circle' },
  { to: '/app/todos', active: '/app/todos', label: t('nav.todo'), icon: 'list_alt' },
  { to: '/app/files', active: '/app/files', label: tf('nav.files', '文件管理'), icon: 'folder_open' },
  { to: '/app/stats', active: '/app/stats', label: t('nav.stats'), icon: 'bar_chart' },
  { to: '/app/notifications', active: '/app/notifications', label: t('nav.notifications'), icon: 'notifications' },
])

const findNotebookById = (nodes: Notebook[], notebookId: number): Notebook | null => {
  for (const node of nodes) {
    if (node.id === notebookId) {
      return node
    }

    const matchedChild = node.children ? findNotebookById(node.children, notebookId) : null
    if (matchedChild) {
      return matchedChild
    }
  }

  return null
}

const containsDescendant = (node: Notebook | null, targetId: number): boolean => {
  if (!node?.children?.length) {
    return false
  }

  return node.children.some((child) => child.id === targetId || containsDescendant(child, targetId))
}

const isNotebookCollapsed = (notebookId: number) => collapsedNotebookIds.value.includes(notebookId)

const buildSidebarRows = (nodes: Notebook[], noteMap: Map<number, Note[]>, depth = 0): SidebarRow[] =>
  nodes.flatMap((node) => {
    const notes = noteMap.get(node.id) || []
    const expandable = Boolean(node.children?.length || notes.length)
    const notebookRow: SidebarNotebookRow = {
      kind: 'notebook',
      id: node.id,
      depth,
      label: node.name,
      notebook: node,
      expandable,
    }

    if (expandable && isNotebookCollapsed(node.id)) {
      return [notebookRow]
    }

    const childRows = node.children ? buildSidebarRows(node.children, noteMap, depth + 1) : []
    const noteRows = notes.map<SidebarNoteRow>((note) => ({
      kind: 'note',
      id: note.id,
      depth: depth + 1,
      label: note.title,
      note,
    }))

    return [notebookRow, ...childRows, ...noteRows]
  })

const noteMap = computed(() => {
  const nextMap = new Map<number, Note[]>()

  for (const note of noteStore.sidebarNotes) {
    const bucket = nextMap.get(note.notebookId) || []
    bucket.push(note)
    nextMap.set(note.notebookId, bucket)
  }

  return nextMap
})

const visibleRootNotebooks = computed(() => noteStore.notebooks.slice(0, MAX_VISIBLE_ROOT_NOTEBOOKS))
const hasMoreNotebookRoots = computed(() => noteStore.notebooks.length > MAX_VISIBLE_ROOT_NOTEBOOKS)
const visibleSidebarRows = computed(() => buildSidebarRows(visibleRootNotebooks.value, noteMap.value))
const allSidebarRows = computed(() => buildSidebarRows(noteStore.notebooks, noteMap.value))

const tagGroups = computed<TagGroupItem[]>(() => {
  const countMap = new Map<number, number>()

  for (const note of noteStore.sidebarNotes) {
    for (const tag of note.tags || []) {
      countMap.set(tag.id, (countMap.get(tag.id) || 0) + 1)
    }
  }

  return noteStore.tags.map((tag) => ({
    ...tag,
    count: countMap.get(tag.id) || 0,
  }))
})

const visibleTagGroups = computed(() => tagGroups.value.slice(0, MAX_VISIBLE_TAG_GROUPS))
const hasMoreTagGroups = computed(() => tagGroups.value.length > MAX_VISIBLE_TAG_GROUPS)
const isNotesRoute = computed(() => router.currentRoute.value.path.startsWith('/app/notes'))
const selectedNotebook = computed(() =>
  isNotesRoute.value && noteStore.query.notebookId ? findNotebookById(noteStore.notebooks, noteStore.query.notebookId) : null,
)
const selectedTag = computed(() =>
  isNotesRoute.value ? noteStore.tags.find((tag) => tag.id === noteStore.query.tagId) || null : null,
)
const activeNoteId = computed(() => {
  if (!isNotesRoute.value) {
    return null
  }

  const routeNoteId = Number(router.currentRoute.value.params.noteId)
  return Number.isNaN(routeNoteId) ? noteStore.activeNoteId : routeNoteId
})
const dailyQuote = computed(() => {
  const dateKey = new Date().toISOString().slice(0, 10)
  const hash = Array.from(dateKey).reduce((total, char) => total + char.charCodeAt(0), 0)
  return RANDOM_QUOTES[hash % RANDOM_QUOTES.length]
})
const randomWalkTitle = computed(() =>
  noteStore.sidebarNotes.length ? t('sidebar.randomWalkTitle') : t('sidebar.randomWalkEmpty'),
)

const rowClasses = (row: SidebarRow) => {
  if (row.kind === 'notebook') {
    return [
      isNotesRoute.value && noteStore.query.notebookId === row.notebook.id
        ? 'bg-white text-primary shadow-sm'
        : 'text-stone-600 hover:bg-white/60 hover:text-primary',
      draggingItem.value?.kind === 'notebook' && draggingItem.value.id === row.notebook.id ? 'opacity-45' : '',
      dragTargetNotebookId.value === row.notebook.id && dragTargetMode.value === 'child'
        ? 'bg-primary-fixed/40 text-primary shadow-sm ring-1 ring-primary/10'
        : '',
    ]
  }

  return [
    activeNoteId.value === row.note.id
      ? 'bg-white text-primary shadow-sm'
      : 'text-stone-500 hover:bg-white/50 hover:text-primary',
    draggingItem.value?.kind === 'note' && draggingItem.value.id === row.note.id ? 'opacity-45' : '',
  ]
}

const toggleNotebookExpanded = (notebookId: number) => {
  if (isNotebookCollapsed(notebookId)) {
    collapsedNotebookIds.value = collapsedNotebookIds.value.filter((id) => id !== notebookId)
    return
  }

  collapsedNotebookIds.value = [...collapsedNotebookIds.value, notebookId]
}

const openNotebook = async (notebookId: number) => {
  noteStore.query.notebookId = notebookId
  noteStore.query.tagId = undefined
  noteStore.query.projectId = undefined
  noteStore.query.pageNum = 1

  const loadPromise = noteStore.loadNotes()
  if (!router.currentRoute.value.path.startsWith('/app/notes')) {
    await router.push('/app/notes')
  }
  await loadPromise
}

const openNoteFromTree = async (note: Note) => {
  noteStore.query.notebookId = note.notebookId
  noteStore.query.tagId = undefined
  noteStore.query.projectId = undefined
  noteStore.query.keyword = ''
  noteStore.query.pageNum = 1
  await noteStore.loadNotes()
  await router.push(`/app/notes/${note.id}`)
}

const filterByTag = async (tagId: number) => {
  noteStore.query.tagId = tagId
  noteStore.query.notebookId = undefined
  noteStore.query.projectId = undefined
  noteStore.query.pageNum = 1

  const loadPromise = noteStore.loadNotes()
  if (!router.currentRoute.value.path.startsWith('/app/notes')) {
    await router.push('/app/notes')
  }
  await loadPromise
}

const handleRowClick = async (row: SidebarRow) => {
  if (row.kind === 'notebook') {
    await openNotebook(row.notebook.id)
    return
  }

  await openNoteFromTree(row.note)
}

const handleDialogRowClick = async (row: SidebarRow) => {
  notebookDialogOpen.value = false
  await handleRowClick(row)
}

const handleDialogTagClick = async (tagId: number) => {
  tagDialogOpen.value = false
  await filterByTag(tagId)
}

const startCreateNotebook = async () => {
  creatingNotebook.value = true
  newNotebookName.value = ''
  await nextTick()
  notebookInputRef.value?.focus()
}

const cancelCreateNotebook = () => {
  creatingNotebook.value = false
  newNotebookName.value = ''
}

const submitCreateNotebook = async () => {
  const notebookName = newNotebookName.value.trim()
  if (!notebookName) {
    cancelCreateNotebook()
    return
  }

  const notebook = await noteStore.createNotebook(notebookName)
  cancelCreateNotebook()
  if (notebook) {
    await openNotebook(notebook.id)
  }
}

const handleNotebookBlur = () => {
  submitCreateNotebook().catch(() => undefined)
}

const startCreateTag = async () => {
  creatingTag.value = true
  newTagName.value = ''
  await nextTick()
  tagInputRef.value?.focus()
}

const cancelCreateTag = () => {
  creatingTag.value = false
  newTagName.value = ''
}

const submitCreateTag = async () => {
  const tagName = newTagName.value.trim().replace(/^#/, '')
  if (!tagName) {
    cancelCreateTag()
    return
  }

  const tag = await noteStore.createTag(tagName)
  cancelCreateTag()
  if (tag) {
    await filterByTag(tag.id)
  }
}

const handleTagBlur = () => {
  submitCreateTag().catch(() => undefined)
}

const confirmDeleteNotebook = async (notebook: Notebook) => {
  const confirmed = await ElMessageBox.confirm(
    t('confirm.deleteNotebookMessage', { name: notebook.name }),
    t('confirm.deleteNotebookTitle'),
    {
      confirmButtonText: t('confirm.confirmDelete'),
      cancelButtonText: t('confirm.cancel'),
      type: 'warning',
    },
  ).catch(() => false)

  if (!confirmed) {
    return
  }

  const result = await noteStore.deleteNotebook(notebook.id)
  const currentRouteNoteId = Number(router.currentRoute.value.params.noteId)
  if (!Number.isNaN(currentRouteNoteId) && result.deletedNoteIds.includes(currentRouteNoteId)) {
    await router.push('/app/notes')
  }
}

const confirmDeleteTag = async (tag: Tag) => {
  const confirmed = await ElMessageBox.confirm(
    t('confirm.deleteTagMessage', { name: tag.name }),
    t('confirm.deleteTagTitle'),
    {
      confirmButtonText: t('confirm.confirmDelete'),
      cancelButtonText: t('confirm.cancel'),
      type: 'warning',
    },
  ).catch(() => false)

  if (!confirmed) {
    return
  }

  await noteStore.deleteTag(tag.id)
}

const parseDraggingItem = (value: string): DraggingItem => {
  const [kind, rawId] = value.split(':')
  const id = Number(rawId)

  if ((kind !== 'note' && kind !== 'notebook' && kind !== 'tag') || Number.isNaN(id)) {
    return null
  }

  return { kind, id }
}

const resolveDraggingItem = (event?: DragEvent): DraggingItem => {
  if (draggingItem.value) {
    return draggingItem.value
  }

  const rawValue = event?.dataTransfer?.getData('text/plain') || ''
  const resolved = parseDraggingItem(rawValue)
  if (resolved) {
    draggingItem.value = resolved
  }
  return resolved
}

const resetDragState = () => {
  draggingItem.value = null
  dragTargetNotebookId.value = null
  dragTargetMode.value = null
  trashActive.value = false
}

const canDropNotebookIntoNotebook = (targetNotebookId: number, dragItem: DraggingItem) => {
  if (dragItem?.kind !== 'notebook' || dragItem.id === targetNotebookId) {
    return false
  }

  const sourceNotebook = findNotebookById(noteStore.notebooks, dragItem.id)
  if (!sourceNotebook) {
    return false
  }

  return !containsDescendant(sourceNotebook, targetNotebookId)
}

const canDropNoteIntoNotebook = (targetNotebookId: number, dragItem: DraggingItem) => {
  if (dragItem?.kind !== 'note') {
    return false
  }

  const sourceNote = noteStore.sidebarNotes.find((item) => item.id === dragItem.id)
  return Boolean(sourceNote && sourceNote.notebookId !== targetNotebookId)
}

const startRowDrag = (event: DragEvent, row: SidebarRow) => {
  draggingItem.value = { kind: row.kind, id: row.id }
  dragTargetNotebookId.value = null
  dragTargetMode.value = null
  trashActive.value = false

  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', `${row.kind}:${row.id}`)
  }
}

const startTagDrag = (event: DragEvent, tag: Tag) => {
  draggingItem.value = { kind: 'tag', id: tag.id }
  dragTargetNotebookId.value = null
  dragTargetMode.value = null
  trashActive.value = false

  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', `tag:${tag.id}`)
  }
}

const handleDragEnd = () => {
  if (!movingItem.value) {
    resetDragState()
  }
}

const moveNotebook = async (parentId: number, dragItem: DraggingItem) => {
  if (dragItem?.kind !== 'notebook' || movingItem.value) {
    return
  }

  const sourceNotebook = findNotebookById(noteStore.notebooks, dragItem.id)
  if (!sourceNotebook) {
    resetDragState()
    return
  }

  if (parentId && !canDropNotebookIntoNotebook(parentId, dragItem)) {
    ElMessage.warning(t('messages.notebookCannotMoveToDescendant'))
    resetDragState()
    return
  }

  if (sourceNotebook.parentId === parentId) {
    resetDragState()
    return
  }

  const targetNotebook = parentId ? findNotebookById(noteStore.notebooks, parentId) : null
  const nextSortOrder = parentId ? targetNotebook?.children?.length ?? 0 : noteStore.notebooks.length

  movingItem.value = true
  try {
    await noteStore.moveNotebook(dragItem.id, parentId, nextSortOrder)
  } finally {
    movingItem.value = false
    resetDragState()
  }
}

const moveNote = async (targetNotebookId: number, dragItem: DraggingItem) => {
  if (dragItem?.kind !== 'note' || movingItem.value) {
    return
  }

  movingItem.value = true
  try {
    await noteStore.moveNote(dragItem.id, targetNotebookId)
  } finally {
    movingItem.value = false
    resetDragState()
  }
}

const deleteDraggedNote = async (dragItem: DraggingItem) => {
  if (dragItem?.kind !== 'note' || movingItem.value) {
    return
  }

  movingItem.value = true
  try {
    const deleted = await noteStore.deleteNote(dragItem.id)
    if (!deleted) {
      return
    }

    const routeNoteId = Number(router.currentRoute.value.params.noteId)
    if (!Number.isNaN(routeNoteId) && routeNoteId === dragItem.id) {
      await router.push('/app/notes')
    }
  } finally {
    movingItem.value = false
    resetDragState()
  }
}

const deleteDraggedNotebook = async (dragItem: DraggingItem) => {
  if (dragItem?.kind !== 'notebook' || movingItem.value) {
    return
  }

  const notebook = findNotebookById(noteStore.notebooks, dragItem.id)
  if (!notebook) {
    resetDragState()
    return
  }

  movingItem.value = true
  try {
    await confirmDeleteNotebook(notebook)
  } finally {
    movingItem.value = false
    resetDragState()
  }
}

const deleteDraggedTag = async (dragItem: DraggingItem) => {
  if (dragItem?.kind !== 'tag' || movingItem.value) {
    return
  }

  const tag = noteStore.tags.find((item) => item.id === dragItem.id)
  if (!tag) {
    resetDragState()
    return
  }

  movingItem.value = true
  try {
    await confirmDeleteTag(tag)
  } finally {
    movingItem.value = false
    resetDragState()
  }
}

const openRandomWalk = async () => {
  if (!noteStore.sidebarNotes.length) {
    ElMessage.info(t('messages.noRandomNotes'))
    return
  }

  const randomIndex = Math.floor(Math.random() * noteStore.sidebarNotes.length)
  await openNoteFromTree(noteStore.sidebarNotes[randomIndex])
}

const handleNotebookDragOver = (event: DragEvent, targetNotebookId: number) => {
  const dragItem = resolveDraggingItem(event)

  if (canDropNotebookIntoNotebook(targetNotebookId, dragItem)) {
    event.preventDefault()
    dragTargetNotebookId.value = targetNotebookId
    dragTargetMode.value = 'child'
    trashActive.value = false
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'move'
    }
    return
  }

  if (canDropNoteIntoNotebook(targetNotebookId, dragItem)) {
    event.preventDefault()
    dragTargetNotebookId.value = targetNotebookId
    dragTargetMode.value = 'note'
    trashActive.value = false
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'move'
    }
    return
  }

  dragTargetNotebookId.value = null
  dragTargetMode.value = null
}

const dropOnNotebook = (event: DragEvent, targetNotebookId: number) => {
  const dragItem = resolveDraggingItem(event)
  if (!dragItem) {
    return
  }

  event.preventDefault()

  if (dragItem.kind === 'notebook') {
    moveNotebook(targetNotebookId, dragItem).catch(() => undefined)
    return
  }

  moveNote(targetNotebookId, dragItem).catch(() => undefined)
}

const handleRootDragOver = (event: DragEvent) => {
  const dragItem = resolveDraggingItem(event)
  if (dragItem?.kind !== 'notebook') {
    return
  }

  event.preventDefault()
  dragTargetNotebookId.value = null
  dragTargetMode.value = 'root'
  trashActive.value = false

  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
}

const dropNotebookToRoot = (event: DragEvent) => {
  const dragItem = resolveDraggingItem(event)
  if (dragItem?.kind !== 'notebook') {
    return
  }

  event.preventDefault()
  moveNotebook(0, dragItem).catch(() => undefined)
}

const handleTrashDragOver = (event: DragEvent) => {
  const dragItem = resolveDraggingItem(event)
  if (!dragItem) {
    trashActive.value = false
    return
  }

  event.preventDefault()
  trashActive.value = true
  dragTargetNotebookId.value = null
  dragTargetMode.value = null

  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
}

const handleTrashDragLeave = (event: DragEvent) => {
  const relatedTarget = event.relatedTarget as Node | null
  if (relatedTarget && (event.currentTarget as HTMLElement | null)?.contains(relatedTarget)) {
    return
  }

  trashActive.value = false
}

const dropToTrash = (event: DragEvent) => {
  const dragItem = resolveDraggingItem(event)
  if (!dragItem) {
    resetDragState()
    return
  }

  event.preventDefault()
  if (dragItem.kind === 'notebook') {
    deleteDraggedNotebook(dragItem).catch(() => undefined)
    return
  }

  if (dragItem.kind === 'tag') {
    deleteDraggedTag(dragItem).catch(() => undefined)
    return
  }

  deleteDraggedNote(dragItem).catch(() => undefined)
}

watch(
  () => spaceStore.currentSpaceId,
  () => {
    if (spaceStore.currentSpace?.type === 'TEAM') {
      return
    }

    collapsedNotebookIds.value = []
    noteStore.loadWorkspace().catch(() => undefined)
  },
  { immediate: true },
)
</script>
