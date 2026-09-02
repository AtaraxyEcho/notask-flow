import { ElMessage } from 'element-plus'
import { defineStore } from 'pinia'
import { notebookService, noteService, tagService } from '@/api/services'
import { translate } from '@/i18n'
import type { Note, NoteHistory, NoteHistorySaveType, NoteQuery, Notebook, PageResponse, Tag } from '@/types/app'
import { useSpaceStore } from './space'

interface NoteState {
  notebooks: Notebook[]
  notesPage: PageResponse<Note>
  sidebarNotes: Note[]
  currentNote: Note | null
  activeNoteId: number | null
  currentNoteLoading: boolean
  histories: NoteHistory[]
  tags: Tag[]
  query: NoteQuery
  loading: boolean
  noteCache: Record<number, Note>
  workspaceSpaceId: number | null
}

interface WorkspacePayload {
  notebooks: Notebook[]
  notesPage: PageResponse<Note>
  sidebarNotes: Note[]
  tags: Tag[]
}

type ListPlacement = 'front' | 'preserve'

let latestSelectToken = 0
let latestWorkspaceToken = 0
let latestNotesToken = 0
let latestSidebarNotesToken = 0
const workspaceRequestMap = new Map<string, Promise<WorkspacePayload>>()
const detailRequestMap = new Map<string, Promise<Note>>()

const emptyPage = (): PageResponse<Note> => ({
  total: 0,
  pageNum: 1,
  pageSize: 10,
  list: [],
})

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

const collectNotebookIds = (nodes: Notebook[], notebookId: number): number[] => {
  for (const node of nodes) {
    if (node.id === notebookId) {
      return [node.id, ...(node.children?.flatMap((child) => collectNotebookIds([child], child.id)) || [])]
    }

    if (node.children?.length) {
      const childMatched = collectNotebookIds(node.children, notebookId)
      if (childMatched.length) {
        return childMatched
      }
    }
  }

  return []
}

const flattenNotebooks = (nodes: Notebook[]): Notebook[] =>
  nodes.flatMap((node) => [node, ...(node.children ? flattenNotebooks(node.children) : [])])

const mergeNote = (previous: Note | undefined, next: Note): Note => ({
  ...(previous || {}),
  ...next,
  content: next.content ?? previous?.content,
  contentHtml: next.contentHtml ?? previous?.contentHtml,
  tags: next.tags ?? previous?.tags ?? [],
})

const mergeNoteCache = (cache: Record<number, Note>, notes: Note[]) => {
  const nextCache = { ...cache }

  for (const note of notes) {
    nextCache[note.id] = mergeNote(nextCache[note.id], note)
  }

  return nextCache
}

const upsertNoteList = (notes: Note[], note: Note, placement: ListPlacement) => {
  const index = notes.findIndex((item) => item.id === note.id)
  if (index >= 0) {
    const nextNotes = [...notes]
    nextNotes[index] = mergeNote(nextNotes[index], note)
    return nextNotes
  }

  return placement === 'front' ? [note, ...notes] : [...notes, note]
}

const removeNoteFromList = (notes: Note[], noteId: number) => notes.filter((item) => item.id !== noteId)

const matchesQuery = (note: Note, query: NoteQuery) => {
  if (query.notebookId && note.notebookId !== query.notebookId) {
    return false
  }

  if (query.projectId && note.projectId !== query.projectId) {
    return false
  }

  if (query.tagId && !(note.tags || []).some((tag) => tag.id === query.tagId)) {
    return false
  }

  if (query.keyword) {
    const keyword = query.keyword.trim().toLowerCase()
    if (keyword) {
      const haystack = `${note.title} ${note.content || ''}`.toLowerCase()
      if (!haystack.includes(keyword)) {
        return false
      }
    }
  }

  return true
}

const patchPageWithNote = (
  page: PageResponse<Note>,
  note: Note,
  query: NoteQuery,
  placement: ListPlacement,
) => {
  const exists = page.list.some((item) => item.id === note.id)
  const matched = matchesQuery(note, query)

  if (!matched) {
    if (!exists) {
      return page
    }

    return {
      ...page,
      total: Math.max(0, page.total - 1),
      list: removeNoteFromList(page.list, note.id),
    }
  }

  const list = upsertNoteList(page.list, note, placement).slice(0, page.pageSize || page.list.length || 10)

  return {
    ...page,
    total: exists ? page.total : page.total + 1,
    list,
  }
}

const removeNoteFromPage = (page: PageResponse<Note>, noteId: number) => {
  const exists = page.list.some((item) => item.id === noteId)
  if (!exists) {
    return page
  }

  return {
    ...page,
    total: Math.max(0, page.total - 1),
    list: removeNoteFromList(page.list, noteId),
  }
}

const noteToUpdatePayload = (note: Note, notebookId = note.notebookId) => ({
  notebookId,
  title: note.title,
  content: note.content,
  contentHtml: note.contentHtml,
  projectId: note.projectId,
  tagIds: (note.tags || []).map((tag) => tag.id),
})

const buildWorkspaceRequestKey = (spaceId: number, query: NoteQuery) =>
  JSON.stringify({
    keyword: query.keyword || '',
    notebookId: query.notebookId ?? 0,
    pageNum: query.pageNum ?? 1,
    pageSize: query.pageSize ?? 10,
    projectId: query.projectId ?? 0,
    spaceId,
    tagId: query.tagId ?? 0,
  })

const createDetailRequestKey = (spaceId: number, noteId: number) => `${spaceId}:${noteId}`

export const useNoteStore = defineStore('note', {
  state: (): NoteState => ({
    notebooks: [],
    notesPage: emptyPage(),
    sidebarNotes: [],
    currentNote: null,
    activeNoteId: null,
    currentNoteLoading: false,
    histories: [],
    tags: [],
    query: {
      pageNum: 1,
      pageSize: 10,
      keyword: '',
    },
    loading: false,
    noteCache: {},
    workspaceSpaceId: null,
  }),
  getters: {
    notes: (state) => state.notesPage.list,
    firstNotebookId: (state) => state.notebooks[0]?.id,
  },
  actions: {
    patchLocalNote(note: Note, placement: ListPlacement = 'preserve') {
      const spaceId = useSpaceStore().currentSpaceId
      if (spaceId && note.spaceId !== spaceId) {
        return note
      }

      const merged = mergeNote(this.noteCache[note.id], note)
      this.noteCache = {
        ...this.noteCache,
        [merged.id]: merged,
      }
      this.sidebarNotes = upsertNoteList(this.sidebarNotes, merged, placement)
      this.notesPage = patchPageWithNote(this.notesPage, merged, this.query, placement)

      if (this.currentNote?.id === merged.id && this.currentNote.spaceId === merged.spaceId) {
        this.currentNote = merged
      }

      return merged
    },
    async loadWorkspace() {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      const workspaceToken = ++latestWorkspaceToken
      const requestKey = buildWorkspaceRequestKey(spaceId, this.query)
      this.loading = true

      try {
        let request = workspaceRequestMap.get(requestKey)

        if (!request) {
          request = Promise.all([
            notebookService.tree(spaceId),
            tagService.list(spaceId),
            noteService.page(spaceId, this.query),
            noteService.page(spaceId, {
              pageNum: 1,
              pageSize: 100,
            }),
          ]).then(([notebooks, tags, notesPage, sidebarPage]) => ({
            notebooks,
            notesPage,
            sidebarNotes: sidebarPage.list,
            tags,
          }))

          workspaceRequestMap.set(requestKey, request)
        }

        const workspace = await request

        if (workspaceRequestMap.get(requestKey) === request) {
          workspaceRequestMap.delete(requestKey)
        }

        if (workspaceToken !== latestWorkspaceToken || useSpaceStore().currentSpaceId !== spaceId) {
          return
        }

        this.notebooks = workspace.notebooks
        this.tags = workspace.tags
        this.notesPage = workspace.notesPage
        this.sidebarNotes = workspace.sidebarNotes
        this.workspaceSpaceId = spaceId
        this.noteCache = mergeNoteCache(
          mergeNoteCache(this.noteCache, workspace.notesPage.list),
          workspace.sidebarNotes,
        )
      } finally {
        if (workspaceToken === latestWorkspaceToken) {
          this.loading = false
        }
      }
    },
    async loadNotes() {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      const notesToken = ++latestNotesToken
      const page = await noteService.page(spaceId, this.query)
      if (notesToken !== latestNotesToken || useSpaceStore().currentSpaceId !== spaceId) {
        return
      }

      this.notesPage = page
      this.noteCache = mergeNoteCache(this.noteCache, this.notesPage.list)
    },
    async loadSidebarNotes() {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      const sidebarToken = ++latestSidebarNotesToken
      const sidebarPage = await noteService.page(spaceId, {
        pageNum: 1,
        pageSize: 100,
      })

      if (sidebarToken !== latestSidebarNotesToken || useSpaceStore().currentSpaceId !== spaceId) {
        return
      }

      this.sidebarNotes = sidebarPage.list
      this.noteCache = mergeNoteCache(this.noteCache, sidebarPage.list)
    },
    async selectNote(noteId: number) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return null
      }

      if (this.currentNote?.id === noteId && this.currentNote.spaceId === spaceId && !this.currentNoteLoading) {
        this.activeNoteId = noteId
        return this.currentNote
      }

      const selectToken = ++latestSelectToken
      this.activeNoteId = noteId
      this.currentNoteLoading = true

      if (this.currentNote && (this.currentNote.id !== noteId || this.currentNote.spaceId !== spaceId)) {
        this.currentNote = null
      }

      const requestKey = createDetailRequestKey(spaceId, noteId)
      let request = detailRequestMap.get(requestKey)

      if (!request) {
        request = noteService.detail(spaceId, noteId)
        detailRequestMap.set(requestKey, request)
      }

      try {
        const detail = await request
        if (selectToken !== latestSelectToken || useSpaceStore().currentSpaceId !== spaceId || detail.spaceId !== spaceId) {
          return null
        }

        const merged = this.patchLocalNote(detail, 'preserve')
        this.currentNote = merged

        return merged
      } finally {
        if (detailRequestMap.get(requestKey) === request) {
          detailRequestMap.delete(requestKey)
        }

        if (selectToken === latestSelectToken) {
          this.currentNoteLoading = false
        }
      }
    },
    async refreshNote(noteId: number) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return null
      }

      const selectToken = ++latestSelectToken
      const detail = await noteService.detail(spaceId, noteId)

      if (selectToken !== latestSelectToken || useSpaceStore().currentSpaceId !== spaceId || detail.spaceId !== spaceId) {
        return null
      }

      const merged = this.patchLocalNote(detail, 'preserve')
      if ((this.currentNote?.id === noteId && this.currentNote.spaceId === spaceId) || this.activeNoteId === noteId) {
        this.currentNote = merged
      }

      return merged
    },
    async createNotebook(name: string, parentId = 0) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return null
      }

      const normalizedName = name.trim()
      const exists = flattenNotebooks(this.notebooks).some(
        (notebook) => notebook.name.toLowerCase() === normalizedName.toLowerCase(),
      )
      if (exists) {
        ElMessage.warning(translate('messages.notebookNameExists'))
        return null
      }

      const notebook = await notebookService.create(spaceId, { name: normalizedName, parentId, sortOrder: 0 })
      await this.loadWorkspace()
      ElMessage.success(translate('messages.notebookCreated'))
      return notebook
    },
    async moveNotebook(notebookId: number, parentId: number, sortOrder = 0) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return null
      }

      const notebook = findNotebookById(this.notebooks, notebookId)
      if (!notebook) {
        return null
      }

      const updatedNotebook = await notebookService.update(spaceId, notebookId, {
        name: notebook.name,
        parentId,
        sortOrder,
      })

      await this.loadWorkspace()
      ElMessage.success(translate('messages.notebookMoved'))
      return updatedNotebook
    },
    async createTag(name: string) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return null
      }

      const tag = await tagService.create(spaceId, { name })
      this.tags = await tagService.list(spaceId)
      ElMessage.success(translate('messages.tagCreated'))
      return tag
    },
    async deleteTag(tagId: number) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return false
      }

      await tagService.delete(spaceId, tagId)

      if (this.query.tagId === tagId) {
        this.query.tagId = undefined
      }

      this.tags = this.tags.filter((tag) => tag.id !== tagId)
      this.sidebarNotes = this.sidebarNotes.map((note) => ({
        ...note,
        tags: (note.tags || []).filter((tag) => tag.id !== tagId),
      }))
      this.notesPage = {
        ...this.notesPage,
        list: this.notesPage.list.map((note) => ({
          ...note,
          tags: (note.tags || []).filter((tag) => tag.id !== tagId),
        })),
      }

      if (this.currentNote) {
        this.currentNote = {
          ...this.currentNote,
          tags: (this.currentNote.tags || []).filter((tag) => tag.id !== tagId),
        }
      }

      await Promise.all([this.loadWorkspace(), this.loadNotes()])
      ElMessage.success(translate('messages.tagDeleted'))
      return true
    },
    async createNote(payload: {
      notebookId: number
      title: string
      content?: string
      contentHtml?: string
      projectId?: number
      tagIds?: number[]
    }) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return null
      }

      const note = await noteService.create(spaceId, payload)
      if (useSpaceStore().currentSpaceId !== spaceId || note.spaceId !== spaceId) {
        return note
      }

      const merged = this.patchLocalNote(note, 'front')
      this.currentNote = merged
      this.activeNoteId = merged.id
      this.currentNoteLoading = false
      ElMessage.success(translate('messages.noteCreated'))
      return merged
    },
    async saveNote(
      noteId: number,
      payload: {
        notebookId: number
        title: string
        content?: string
        contentHtml?: string
        projectId?: number
        tagIds?: number[]
        saveType?: NoteHistorySaveType
      },
      options?: { updateCurrent?: boolean },
    ) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return null
      }

      const updatedNote = await noteService.update(spaceId, noteId, payload)
      if (useSpaceStore().currentSpaceId !== spaceId || updatedNote.spaceId !== spaceId) {
        return updatedNote
      }

      const merged = this.patchLocalNote(updatedNote, 'preserve')

      if (options?.updateCurrent !== false && this.currentNote?.id === noteId && this.currentNote.spaceId === spaceId) {
        this.currentNote = merged
      }

      return merged
    },
    async saveCurrentNote(payload: {
      notebookId: number
      title: string
      content?: string
      contentHtml?: string
      projectId?: number
      tagIds?: number[]
      saveType?: NoteHistorySaveType
    }) {
      if (!this.currentNote) {
        return null
      }

      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId || this.currentNote.spaceId !== spaceId) {
        return null
      }

      return this.saveNote(this.currentNote.id, payload, { updateCurrent: true })
    },
    async moveNote(noteId: number, notebookId: number) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return null
      }

      const sourceNote =
        this.currentNote?.id === noteId
          ? this.currentNote
          : this.noteCache[noteId] || this.sidebarNotes.find((item) => item.id === noteId) || null

      const resolvedNote = sourceNote || (await noteService.detail(spaceId, noteId))
      const updatedNote = await noteService.update(spaceId, noteId, noteToUpdatePayload(resolvedNote, notebookId))
      if (useSpaceStore().currentSpaceId !== spaceId || updatedNote.spaceId !== spaceId) {
        return updatedNote
      }

      const merged = this.patchLocalNote(updatedNote, 'preserve')

      if (this.currentNote?.id === noteId && this.currentNote.spaceId === spaceId) {
        this.currentNote = merged
      }

      ElMessage.success(translate('messages.noteMoved'))
      return merged
    },
    async deleteNote(noteId: number) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return false
      }

      await noteService.delete(spaceId, noteId)

      if (useSpaceStore().currentSpaceId !== spaceId) {
        return true
      }

      this.notesPage = removeNoteFromPage(this.notesPage, noteId)
      this.sidebarNotes = removeNoteFromList(this.sidebarNotes, noteId)

      const nextCache = { ...this.noteCache }
      delete nextCache[noteId]
      this.noteCache = nextCache

      if (this.currentNote?.id === noteId && this.currentNote.spaceId === spaceId) {
        this.currentNote = null
        this.currentNoteLoading = false
        this.histories = []
      }

      if (this.activeNoteId === noteId) {
        this.activeNoteId = null
      }

      ElMessage.success(translate('messages.noteDeleted'))
      return true
    },
    async deleteNotebook(notebookId: number) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return { deletedNoteIds: [] as number[] }
      }

      const notebookIds = collectNotebookIds(this.notebooks, notebookId)
      const deletedNotebookIds = new Set(notebookIds)
      const deletedNoteIds = Object.values(this.noteCache)
        .filter((note) => deletedNotebookIds.has(note.notebookId))
        .map((note) => note.id)

      await notebookService.delete(spaceId, notebookId)

      if (this.query.notebookId && deletedNotebookIds.has(this.query.notebookId)) {
        this.query.notebookId = undefined
      }

      if (this.currentNote && deletedNotebookIds.has(this.currentNote.notebookId)) {
        this.currentNote = null
        this.currentNoteLoading = false
        this.histories = []
      }

      if (this.activeNoteId && deletedNoteIds.includes(this.activeNoteId)) {
        this.activeNoteId = null
      }

      await this.loadWorkspace()
      await this.loadNotes()
      ElMessage.success(translate('messages.notebookDeleted'))
      return { deletedNoteIds }
    },
    async deleteCurrentNote() {
      if (!this.currentNote) {
        return false
      }

      return this.deleteNote(this.currentNote.id)
    },
    async loadHistory(noteId: number) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      const histories = await noteService.history(spaceId, noteId)
      if (useSpaceStore().currentSpaceId !== spaceId) {
        return
      }

      this.histories = histories
    },
    async restoreHistory(noteId: number, version: number) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      const restoredNote = await noteService.restore(spaceId, noteId, version)
      if (useSpaceStore().currentSpaceId !== spaceId || restoredNote.spaceId !== spaceId) {
        return
      }

      const merged = this.patchLocalNote(restoredNote, 'preserve')
      this.currentNote = merged
      this.activeNoteId = merged.id
      ElMessage.success(translate('messages.historyRestored'))
    },
    async shareCurrentNote(expireAt?: string) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId || !this.currentNote) {
        return null
      }

      const sharedNote = await noteService.share(spaceId, this.currentNote.id, expireAt)
      if (useSpaceStore().currentSpaceId !== spaceId || sharedNote.spaceId !== spaceId) {
        return sharedNote
      }

      const merged = this.patchLocalNote(sharedNote, 'preserve')
      this.currentNote = merged
      ElMessage.success(translate('messages.shareLinkUpdated'))
      return merged
    },
    reset() {
      this.resetWorkspaceState()
    },
    resetWorkspaceState() {
      this.notebooks = []
      this.notesPage = emptyPage()
      this.sidebarNotes = []
      this.currentNote = null
      this.activeNoteId = null
      this.currentNoteLoading = false
      this.histories = []
      this.tags = []
      this.query = { pageNum: 1, pageSize: 10, keyword: '' }
      this.loading = false
      this.noteCache = {}
      this.workspaceSpaceId = null
      latestSelectToken = 0
      latestWorkspaceToken = 0
      latestNotesToken = 0
      latestSidebarNotesToken = 0
      workspaceRequestMap.clear()
      detailRequestMap.clear()
    },
  },
})
