<template>
  <aside
    class="custom-scrollbar fixed left-0 top-16 z-40 hidden h-[calc(100vh-64px)] w-72 overflow-y-auto rounded-r-[40px] border-r border-white/20 bg-[#F0F9FF]/60 px-6 py-6 shadow-2xl shadow-[#0077B6]/5 backdrop-blur-3xl md:flex md:flex-col"
  >
    <div class="mb-8 flex items-center gap-4">
      <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-[#0077B6] text-white shadow-lg shadow-[#0077B6]/20">
        <span class="material-symbols-outlined">water_drop</span>
      </div>
      <div>
        <h3 class="font-title-serif text-xl font-black leading-none text-[#0077B6]">
          {{ spaceStore.currentSpace?.name || t('sidebar.teamFallback') }}
        </h3>
        <p class="mt-1 text-[10px] font-bold uppercase tracking-widest text-[#0077B6]/60">{{ t('sidebar.teamSubtitle') }}</p>
      </div>
    </div>

    <nav class="mb-8 space-y-1">
      <RouterLink
        v-for="item in navigation"
        :key="item.to"
        :to="item.to"
        class="flex items-center rounded-full px-4 py-3 text-sm font-medium transition-all duration-200 active:scale-95"
        :class="
          $route.path.startsWith(item.active)
            ? 'bg-[#0077B6] text-white shadow-lg shadow-[#0077B6]/20'
            : 'text-[#0077B6]/60 hover:bg-white/50 hover:text-[#0077B6]'
        "
      >
        <span class="material-symbols-outlined mr-3">{{ item.icon }}</span>
        <span>{{ item.label }}</span>
      </RouterLink>
    </nav>

    <section class="mb-8">
      <div class="mb-3 flex items-center justify-between px-4">
        <h4 class="text-[10px] font-black uppercase tracking-widest text-[#0077B6]/40">{{ t('sidebar.sharedDocuments') }}</h4>
        <button
          class="text-[#0077B6]/50 transition-colors hover:text-[#0077B6]"
          type="button"
          @click="startCreateNotebook"
        >
          <span class="material-symbols-outlined text-[18px]">add</span>
        </button>
      </div>

      <div v-if="notebookInputOpen" class="mb-3 px-4">
        <input
          ref="notebookInputRef"
          v-model="newNotebookName"
          class="w-full rounded-xl border border-white/40 bg-white/70 px-3 py-2 text-sm text-[#0077B6] outline-none placeholder:text-[#0077B6]/40 focus:border-[#0077B6]/40"
          :placeholder="t('sidebar.documentGroupPlaceholder')"
          type="text"
          @blur="handleNotebookBlur"
          @keydown.enter.prevent="createNotebookFromSidebar"
          @keydown.esc.prevent="cancelCreateNotebook"
        />
      </div>

      <div class="space-y-1">
        <div
          v-for="row in visibleSidebarRows"
          :key="`${row.kind}-${row.id}`"
          class="group flex items-center gap-1 rounded-xl transition-all"
          :class="rowClasses(row)"
          :draggable="true"
          @dragend="handleDragEnd"
          @dragleave="clearNotebookDropTarget"
          @dragover="row.kind === 'notebook' ? handleNotebookDragOver($event, row.notebook.id) : undefined"
          @dragstart="startRowDrag($event, row)"
          @drop="row.kind === 'notebook' ? dropOnNotebook($event, row.notebook.id) : undefined"
        >
          <button
            class="flex min-w-0 flex-1 items-center py-2 text-left text-sm"
            :style="{ paddingLeft: `${16 + row.depth * 18}px`, paddingRight: '12px' }"
            type="button"
            @click="handleRowClick(row)"
          >
            <span
              v-if="row.kind === 'notebook' && row.expandable"
              class="mr-1 flex h-5 w-5 items-center justify-center rounded-full text-[#0077B6]/45 transition group-hover:bg-white/70 group-hover:text-[#0077B6]"
              @click.stop="toggleNotebookExpanded(row.notebook.id)"
            >
              <span class="material-symbols-outlined text-[16px]">
                {{ isNotebookCollapsed(row.notebook.id) ? 'chevron_right' : 'expand_more' }}
              </span>
            </span>
            <span v-else class="mr-1 inline-block h-5 w-5"></span>

            <span class="material-symbols-outlined mr-2 shrink-0 text-[18px] text-[#0077B6]/45">
              {{
                row.kind === 'notebook'
                  ? isNotebookCollapsed(row.notebook.id)
                    ? 'folder'
                    : 'folder_open'
                  : 'description'
              }}
            </span>
            <span class="truncate">{{ row.label }}</span>
          </button>

          <button
            v-if="row.kind === 'notebook'"
            class="mr-2 hidden h-8 w-8 shrink-0 items-center justify-center rounded-full text-[#0077B6]/45 transition hover:bg-white/70 hover:text-red-500 group-hover:flex"
            type="button"
            @click.stop="confirmDeleteNotebook(row.notebook)"
          >
            <span class="material-symbols-outlined text-[18px]">delete</span>
          </button>

          <button
            v-if="row.kind === 'note'"
            class="mr-2 hidden h-8 w-8 shrink-0 items-center justify-center rounded-full text-[#0077B6]/45 transition hover:bg-white/70 hover:text-red-500 group-hover:flex"
            type="button"
            @click.stop="confirmDeleteNote(row.note)"
          >
            <span class="material-symbols-outlined text-[18px]">delete</span>
          </button>
        </div>

        <div
          v-if="draggingRow?.kind === 'notebook'"
          class="mx-1 rounded-[1.15rem] border border-dashed border-[#0077B6]/25 bg-white/45 px-3 py-2 text-xs font-bold text-[#0077B6]/65 transition hover:bg-white/70"
          @dragover="handleNotebookRootDragOver"
          @drop="dropNotebookToRoot"
        >
          {{ t('sidebar.dropHereForTopLevel') }}
        </div>

        <button
          v-if="hasMoreNotebookRoots"
          class="w-full rounded-[1.1rem] border border-white/40 bg-white/60 px-3 py-2 text-left text-sm text-[#0077B6] transition hover:bg-white"
          type="button"
          @click="notebookDialogOpen = true"
        >
          {{ t('sidebar.more') }}
        </button>
      </div>
    </section>

    <section class="mb-8">
      <div class="mb-3 flex items-center justify-between px-4">
        <h4 class="text-[10px] font-black uppercase tracking-widest text-[#0077B6]/40">{{ t('sidebar.documentTags') }}</h4>
        <button class="text-[#0077B6]/50 transition-colors hover:text-[#0077B6]" type="button" @click="tagInputOpen = !tagInputOpen">
          <span class="material-symbols-outlined text-[18px]">add</span>
        </button>
      </div>
      <div v-if="tagInputOpen" class="mb-3 px-4">
        <input
          v-model="newTagName"
          class="w-full rounded-xl border border-white/40 bg-white/70 px-3 py-2 text-sm text-[#0077B6] outline-none placeholder:text-[#0077B6]/40 focus:border-[#0077B6]/40"
          :placeholder="t('sidebar.tagPlaceholder')"
          type="text"
          @keydown.enter.prevent="createTagFromSidebar"
        />
      </div>
      <div class="flex flex-wrap gap-2 px-4">
        <div
          v-for="tag in noteStore.tags.slice(0, 6)"
          :key="tag.id"
          class="group inline-flex items-center gap-1 rounded-full bg-white/70 px-3 py-1 text-[11px] font-bold uppercase tracking-[0.16em] text-[#0077B6]/70 transition hover:bg-white hover:text-[#0077B6]"
          :draggable="true"
          @click="openTag(tag.id)"
          @dragend="handleDragEnd"
          @dragstart="startTagDrag($event, tag)"
        >
          <span>#{{ tag.name }}</span>
          <button
            class="material-symbols-outlined text-[14px] opacity-0 transition group-hover:opacity-100 hover:text-red-500"
            type="button"
            @click.stop="confirmDeleteTag(tag)"
          >
            close
          </button>
        </div>
      </div>
    </section>

    <section class="mb-8">
      <h4 class="mb-3 px-4 text-[10px] font-black uppercase tracking-widest text-[#0077B6]/40">{{ t('sidebar.recycleBin') }}</h4>
      <div
        class="mx-2 rounded-[1.5rem] border border-dashed px-4 py-4 transition-all duration-200"
        :class="
          trashActive
            ? 'border-red-300 bg-red-50/90 text-red-500 shadow-lg shadow-red-200/40'
            : 'border-white/45 bg-white/45 text-[#0077B6]/60 hover:bg-white/60'
        "
        @dragenter.prevent="handleTrashDragOver"
        @dragleave="handleTrashDragLeave"
        @dragover="handleTrashDragOver"
        @drop="dropToTrash"
      >
        <div class="flex items-center gap-3">
          <span class="material-symbols-outlined text-[20px]">delete</span>
          <div class="min-w-0">
            <div class="text-sm font-bold">{{ t('sidebar.dropHereToDelete') }}</div>
            <p class="mt-1 text-[11px] leading-relaxed opacity-75">{{ t('sidebar.dropDocsToDelete') }}</p>
          </div>
        </div>
      </div>
    </section>

    <section class="mb-8">
      <h4 class="mb-3 px-4 text-[10px] font-black uppercase tracking-widest text-[#0077B6]/40">{{ t('sidebar.memberStatus') }}</h4>
      <div class="flex flex-wrap gap-2 px-2">
        <div v-for="member in spaceStore.members.slice(0, 6)" :key="member.userId" class="relative">
          <img
            v-if="member.avatarUrl"
            :src="member.avatarUrl"
            :alt="member.username"
            class="h-8 w-8 rounded-full border-2 border-white object-cover shadow-sm"
          />
          <div
            v-else
            class="flex h-8 w-8 items-center justify-center rounded-full border-2 border-white bg-primary-fixed text-[10px] font-bold text-primary"
          >
            {{ member.username.slice(0, 1).toUpperCase() }}
          </div>
          <span
            class="absolute bottom-0 right-0 h-2.5 w-2.5 rounded-full border-2 border-white transition-colors"
            :class="member.online ? 'bg-emerald-500' : 'bg-red-500'"
            :title="member.online ? t('sidebar.online') : t('sidebar.offline')"
          ></span>
        </div>
      </div>
    </section>

    <el-dialog v-model="notebookDialogOpen" append-to-body :title="t('sidebar.fullDocumentTree')" width="560px">
      <div class="custom-scrollbar max-h-[62vh] space-y-2 overflow-y-auto pr-1">
        <div
          v-for="row in allSidebarRows"
          :key="`dialog-${row.kind}-${row.id}`"
          class="group flex items-center gap-2 rounded-[1.1rem] border border-white/40 bg-white/75 text-sm text-[#0077B6] shadow-sm transition hover:-translate-y-0.5 hover:bg-white"
        >
          <button
            class="flex min-w-0 flex-1 items-center px-4 py-3 text-left"
            :style="{ paddingLeft: `${18 + row.depth * 20}px` }"
            type="button"
            @click="handleDialogRowClick(row)"
          >
            <span class="material-symbols-outlined mr-3 shrink-0 text-[18px] text-[#0077B6]/60">
              {{
                row.kind === 'notebook'
                  ? isNotebookCollapsed(row.notebook.id)
                    ? 'folder'
                    : 'folder_open'
                  : 'description'
              }}
            </span>
            <span class="truncate">{{ row.label }}</span>
          </button>

          <button
            v-if="row.kind === 'notebook'"
            class="mr-3 flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-[#0077B6]/45 transition hover:bg-[#F0F9FF] hover:text-red-500"
            type="button"
            @click.stop="confirmDeleteNotebook(row.notebook)"
          >
            <span class="material-symbols-outlined text-[18px]">delete</span>
          </button>

          <button
            v-if="row.kind === 'note'"
            class="mr-3 flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-[#0077B6]/45 transition hover:bg-[#F0F9FF] hover:text-red-500"
            type="button"
            @click.stop="confirmDeleteNote(row.note)"
          >
            <span class="material-symbols-outlined text-[18px]">delete</span>
          </button>
        </div>
      </div>
    </el-dialog>
  </aside>
</template>

<script setup lang="ts">
import { ElMessageBox } from 'element-plus'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from '@/i18n'
import { useNoteStore } from '@/stores/note'
import { useSpaceStore } from '@/stores/space'
import { useUserStore } from '@/stores/user'
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

const MAX_VISIBLE_ROOT_NOTEBOOKS = 6
const MEMBER_PRESENCE_REFRESH_MS = 20000

const router = useRouter()
const { t } = useI18n()
const noteStore = useNoteStore()
const spaceStore = useSpaceStore()
const userStore = useUserStore()
const notebookInputOpen = ref(false)
const tagInputOpen = ref(false)
const notebookDialogOpen = ref(false)
const newNotebookName = ref('')
const newTagName = ref('')
const notebookInputRef = ref<HTMLInputElement | null>(null)
const collapsedNotebookIds = ref<number[]>([])
const draggingRow = ref<SidebarRow | null>(null)
const draggingTag = ref<Tag | null>(null)
const dropTargetNotebookId = ref<number | null>(null)
const trashActive = ref(false)
const presenceSpaceId = ref<number | null>(null)
let memberPresenceTimer: number | null = null
const tf = (key: string, fallback: string) => {
  const translated = t(key)
  return translated === key ? fallback : translated
}

const currentMember = computed(() =>
  spaceStore.members.find((member) => member.userId === userStore.profile?.id) || null,
)
const canManageFiles = computed(() =>
  ['SPACE_OWNER', 'SPACE_ADMIN', 'SPACE_MEMBER'].includes(currentMember.value?.roleCode || ''),
)

const navigation = computed(() => [
  { to: '/app/projects', active: '/app/projects', label: t('nav.projects'), icon: 'folder_managed' },
  { to: '/app/tasks', active: '/app/tasks', label: t('nav.taskKanban'), icon: 'view_kanban' },
  { to: '/app/notes', active: '/app/notes', label: t('nav.collaborationDocs'), icon: 'description' },
  ...(canManageFiles.value
    ? [{ to: '/app/files', active: '/app/files', label: tf('nav.files', '文件管理'), icon: 'folder_open' }]
    : []),
  {
    to: `/app/spaces/${spaceStore.currentSpaceId ?? ''}/stats`,
    active: '/app/spaces/',
    label: t('nav.reports'),
    icon: 'analytics',
  },
  {
    to: `/app/space/${spaceStore.currentSpaceId ?? ''}/settings`,
    active: '/app/space/',
    label: t('nav.members'),
    icon: 'group',
  },
])

const isNotebookCollapsed = (notebookId: number) => collapsedNotebookIds.value.includes(notebookId)

const noteMap = computed(() => {
  const nextMap = new Map<number, Note[]>()
  for (const note of noteStore.sidebarNotes) {
    const bucket = nextMap.get(note.notebookId) || []
    bucket.push(note)
    nextMap.set(note.notebookId, bucket)
  }
  return nextMap
})

const buildSidebarRows = (nodes: Notebook[], depth = 0): SidebarRow[] =>
  nodes.flatMap((node) => {
    const notes = noteMap.value.get(node.id) || []
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

    const childRows = node.children ? buildSidebarRows(node.children, depth + 1) : []
    const noteRows = notes.map<SidebarNoteRow>((note) => ({
      kind: 'note',
      id: note.id,
      depth: depth + 1,
      label: note.title,
      note,
    }))

    return [notebookRow, ...childRows, ...noteRows]
  })

const notebookRoots = computed(() => noteStore.notebooks)
const visibleRootNotebooks = computed(() => notebookRoots.value.slice(0, MAX_VISIBLE_ROOT_NOTEBOOKS))
const hasMoreNotebookRoots = computed(() => notebookRoots.value.length > MAX_VISIBLE_ROOT_NOTEBOOKS)
const visibleSidebarRows = computed(() => buildSidebarRows(visibleRootNotebooks.value))
const allSidebarRows = computed(() => buildSidebarRows(notebookRoots.value))
const isNotesRoute = computed(() => router.currentRoute.value.path.startsWith('/app/notes'))
const activeNoteId = computed(() => {
  if (!isNotesRoute.value) {
    return null
  }

  const routeNoteId = Number(router.currentRoute.value.params.noteId)
  return Number.isNaN(routeNoteId) ? noteStore.activeNoteId : routeNoteId
})

const rowClasses = (row: SidebarRow) => {
  if (row.kind === 'notebook' && dropTargetNotebookId.value === row.notebook.id) {
    return 'bg-white text-[#0077B6] ring-2 ring-[#0077B6]/25'
  }

  if (row.kind === 'notebook') {
    return isNotesRoute.value && noteStore.query.notebookId === row.notebook.id
      ? 'bg-white/85 text-[#0077B6] shadow-sm'
      : 'text-[#0077B6]/70 hover:bg-white/55 hover:text-[#0077B6]'
  }

  return activeNoteId.value === row.note.id
    ? 'bg-white/85 text-[#0077B6] shadow-sm'
    : 'text-[#0077B6]/60 hover:bg-white/45 hover:text-[#0077B6]'
}

const toggleNotebookExpanded = (notebookId: number) => {
  if (isNotebookCollapsed(notebookId)) {
    collapsedNotebookIds.value = collapsedNotebookIds.value.filter((id) => id !== notebookId)
    return
  }

  collapsedNotebookIds.value = [...collapsedNotebookIds.value, notebookId]
}

const loadSidebarData = async () => {
  if (spaceStore.currentSpace?.type !== 'TEAM') {
    return
  }

  presenceSpaceId.value = spaceStore.currentSpaceId
  await Promise.allSettled([spaceStore.refreshMemberPresence(), noteStore.loadWorkspace()])
}

const stopMemberPresenceTimer = () => {
  if (!memberPresenceTimer) {
    return
  }

  window.clearInterval(memberPresenceTimer)
  memberPresenceTimer = null
}

const startMemberPresenceTimer = () => {
  stopMemberPresenceTimer()

  if (spaceStore.currentSpace?.type !== 'TEAM') {
    return
  }

  memberPresenceTimer = window.setInterval(() => {
    spaceStore.refreshMemberPresence().catch(() => undefined)
  }, MEMBER_PRESENCE_REFRESH_MS)
}

const sendOfflinePresence = () => {
  if (!presenceSpaceId.value) {
    return
  }

  spaceStore.sendOfflineMember(presenceSpaceId.value)
}

const handlePageHide = () => {
  sendOfflinePresence()
}

const openNotebook = async (notebookId: number) => {
  noteStore.query.notebookId = notebookId
  noteStore.query.projectId = undefined
  noteStore.query.tagId = undefined
  noteStore.query.keyword = ''
  noteStore.query.pageNum = 1
  await noteStore.loadNotes()
  await router.push('/app/notes')
}

const openNote = async (note: Note) => {
  noteStore.query.notebookId = note.notebookId
  noteStore.query.projectId = undefined
  noteStore.query.tagId = undefined
  noteStore.query.keyword = ''
  noteStore.query.pageNum = 1
  await noteStore.loadNotes()
  await router.push(`/app/notes/${note.id}`)
}

const openTag = async (tagId: number) => {
  noteStore.query.notebookId = undefined
  noteStore.query.projectId = undefined
  noteStore.query.tagId = tagId
  noteStore.query.pageNum = 1
  await noteStore.loadNotes()
  await router.push('/app/notes')
}

const handleRowClick = async (row: SidebarRow) => {
  if (row.kind === 'notebook') {
    await openNotebook(row.notebook.id)
    return
  }

  await openNote(row.note)
}

const handleDialogRowClick = async (row: SidebarRow) => {
  notebookDialogOpen.value = false
  await handleRowClick(row)
}

const startRowDrag = (event: DragEvent, row: SidebarRow) => {
  draggingRow.value = row
  draggingTag.value = null
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', `${row.kind}:${row.id}`)
  }
}

const startTagDrag = (event: DragEvent, tag: Tag) => {
  draggingTag.value = tag
  draggingRow.value = null
  dropTargetNotebookId.value = null
  trashActive.value = false

  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', `tag:${tag.id}`)
  }
}

const handleDragEnd = () => {
  draggingRow.value = null
  draggingTag.value = null
  dropTargetNotebookId.value = null
  trashActive.value = false
}

const clearNotebookDropTarget = () => {
  dropTargetNotebookId.value = null
}

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

const containsDescendant = (notebook: Notebook | null, targetId: number): boolean => {
  if (!notebook?.children?.length) {
    return false
  }

  return notebook.children.some((child) => child.id === targetId || containsDescendant(child, targetId))
}

const canDropOnNotebook = (targetNotebookId: number) => {
  const row = draggingRow.value
  if (!row) {
    return false
  }

  if (row.kind === 'note') {
    return row.note.notebookId !== targetNotebookId
  }

  if (row.notebook.id === targetNotebookId) {
    return false
  }

  return !containsDescendant(row.notebook, targetNotebookId)
}

const handleNotebookDragOver = (event: DragEvent, targetNotebookId: number) => {
  if (!canDropOnNotebook(targetNotebookId)) {
    return
  }
  event.preventDefault()
  dropTargetNotebookId.value = targetNotebookId
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
}

const dropOnNotebook = async (event: DragEvent, targetNotebookId: number) => {
  event.preventDefault()
  const row = draggingRow.value
  if (!row || !canDropOnNotebook(targetNotebookId)) {
    handleDragEnd()
    return
  }
  handleDragEnd()

  if (row.kind === 'note') {
    await noteStore.moveNote(row.note.id, targetNotebookId)
    return
  }

  await noteStore.moveNotebook(row.notebook.id, targetNotebookId)
}

const handleNotebookRootDragOver = (event: DragEvent) => {
  if (draggingRow.value?.kind !== 'notebook') {
    return
  }
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
}

const dropNotebookToRoot = async (event: DragEvent) => {
  event.preventDefault()
  const row = draggingRow.value
  handleDragEnd()
  if (row?.kind !== 'notebook') {
    return
  }

  await noteStore.moveNotebook(row.notebook.id, 0)
}

const handleTrashDragOver = (event: DragEvent) => {
  if (!draggingRow.value && !draggingTag.value) {
    return
  }
  event.preventDefault()
  trashActive.value = true
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
}

const handleTrashDragLeave = (event: DragEvent) => {
  const currentTarget = event.currentTarget as Node | null
  const relatedTarget = event.relatedTarget as Node | null
  if (!currentTarget || !relatedTarget || !currentTarget.contains(relatedTarget)) {
    trashActive.value = false
  }
}

const dropToTrash = async (event: DragEvent) => {
  event.preventDefault()
  const row = draggingRow.value
  const tag = draggingTag.value
  handleDragEnd()
  if (tag) {
    await confirmDeleteTag(tag)
    return
  }

  if (!row) {
    return
  }
  if (row.kind === 'notebook') {
    await confirmDeleteNotebook(row.notebook)
    return
  }
  await confirmDeleteNote(row.note)
}

const startCreateNotebook = async () => {
  notebookInputOpen.value = true
  newNotebookName.value = ''
  await nextTick()
  notebookInputRef.value?.focus()
}

const cancelCreateNotebook = () => {
  notebookInputOpen.value = false
  newNotebookName.value = ''
}

const createNotebookFromSidebar = async () => {
  const name = newNotebookName.value.trim()
  if (!name) {
    cancelCreateNotebook()
    return
  }

  const notebook = await noteStore.createNotebook(name)
  cancelCreateNotebook()

  if (notebook) {
    await openNotebook(notebook.id)
  }
}

const handleNotebookBlur = () => {
  createNotebookFromSidebar().catch(() => undefined)
}

const createTagFromSidebar = async () => {
  const name = newTagName.value.trim().replace(/^#/, '')
  if (!name) {
    return
  }

  const tag = await noteStore.createTag(name)
  newTagName.value = ''
  tagInputOpen.value = false

  if (tag) {
    await openTag(tag.id)
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

const confirmDeleteNotebook = async (notebook: Notebook) => {
  const confirmed = await ElMessageBox.confirm(
    t('confirm.deleteDocumentGroupMessage', { name: notebook.name }),
    t('confirm.deleteDocumentGroupTitle'),
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

const confirmDeleteNote = async (note: Note) => {
  const confirmed = await ElMessageBox.confirm(
    t('confirm.deleteDocumentMessage', { title: note.title }),
    t('confirm.deleteDocumentTitle'),
    {
      confirmButtonText: t('confirm.confirmDelete'),
      cancelButtonText: t('confirm.cancel'),
      type: 'warning',
    },
  ).catch(() => false)

  if (!confirmed) {
    return
  }

  await noteStore.deleteNote(note.id)
  if (activeNoteId.value === note.id) {
    await router.push('/app/notes')
  }
}

watch(
  () => ({
    spaceId: spaceStore.currentSpaceId,
    type: spaceStore.currentSpace?.type,
  }),
  (current, previous) => {
    if (previous?.type === 'TEAM' && previous.spaceId && previous.spaceId !== current.spaceId) {
      spaceStore.sendOfflineMember(previous.spaceId)
    }

    loadSidebarData().catch(() => undefined)
    startMemberPresenceTimer()
  },
  { immediate: true },
)

onMounted(() => {
  window.addEventListener('pagehide', handlePageHide)
})

onBeforeUnmount(() => {
  stopMemberPresenceTimer()
  sendOfflinePresence()
  window.removeEventListener('pagehide', handlePageHide)
})
</script>
