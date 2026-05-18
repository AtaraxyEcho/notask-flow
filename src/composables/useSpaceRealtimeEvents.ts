import { onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { spaceService } from '@/api/services'
import { SpaceEventProvider } from '@/collab/SpaceEventProvider'
import { useNoteStore } from '@/stores/note'
import { useProjectStore } from '@/stores/project'
import { useSpaceStore } from '@/stores/space'
import { useTaskStore } from '@/stores/task'
import { useTodoStore } from '@/stores/todo'
import type { SpaceRealtimeEvent } from '@/types/app'
import { resolveCollabWsUrl } from '@/utils/collabWs'

const RELOAD_DEBOUNCE_MS = 300
export const SPACE_FILE_CHANGED_EVENT = 'notask:space-file-changed'
export const SPACE_STATS_CHANGED_EVENT = 'notask:space-stats-changed'

const resolveWsUrl = () => {
  return resolveCollabWsUrl(import.meta.env.VITE_COLLAB_WS_URL)
}

export function useSpaceRealtimeEvents() {
  const noteStore = useNoteStore()
  const projectStore = useProjectStore()
  const router = useRouter()
  const spaceStore = useSpaceStore()
  const taskStore = useTaskStore()
  const todoStore = useTodoStore()

  let provider: SpaceEventProvider | null = null
  const timers = new Map<string, number>()

  const runDebounced = (key: string, runner: () => Promise<unknown>) => {
    const existingTimer = timers.get(key)
    if (existingTimer) {
      window.clearTimeout(existingTimer)
    }

    const timer = window.setTimeout(() => {
      timers.delete(key)
      runner().catch(() => undefined)
    }, RELOAD_DEBOUNCE_MS)
    timers.set(key, timer)
  }

  const handleDocumentDeleted = (event: SpaceRealtimeEvent) => {
    const deletedNoteId = Number(event.payload?.noteId || 0)
    if (!deletedNoteId || noteStore.currentNote?.id !== deletedNoteId) {
      return
    }

    noteStore.currentNote = null
    noteStore.activeNoteId = null
    router.push('/app/notes').catch(() => undefined)
  }

  const handleDocumentTreeChanged = (event: SpaceRealtimeEvent) => {
    if (event.payload?.action !== 'deleted') {
      return
    }

    const deletedNoteIds = Array.isArray(event.payload?.deletedNoteIds)
      ? event.payload.deletedNoteIds.map((noteId) => Number(noteId)).filter(Boolean)
      : []
    const currentRouteNoteId = Number(router.currentRoute.value.params.noteId || 0)
    const deletedCurrentNote =
      Boolean(noteStore.currentNote?.id && deletedNoteIds.includes(noteStore.currentNote.id)) ||
      Boolean(currentRouteNoteId && deletedNoteIds.includes(currentRouteNoteId))

    if (deletedCurrentNote) {
      noteStore.currentNote = null
      noteStore.activeNoteId = null
      noteStore.histories = []
      router.push('/app/notes').catch(() => undefined)
    }

    const deletedNotebookId = Number(event.payload?.notebookId || 0)
    if (deletedNotebookId && noteStore.query.notebookId === deletedNotebookId) {
      noteStore.query.notebookId = undefined
      noteStore.query.pageNum = 1
    }
  }

  const syncCurrentDocumentMetadata = async (event: SpaceRealtimeEvent) => {
    const changedNoteId = Number(event.payload?.noteId || 0)
    if (!changedNoteId || noteStore.currentNote?.id !== changedNoteId) {
      return
    }

    if (event.type.startsWith('TAG_') || event.payload?.changedField === 'tags') {
      await noteStore.refreshNote(changedNoteId)
      return
    }

    noteStore.patchLocalNote({
      ...noteStore.currentNote,
      notebookId: Number(event.payload?.notebookId || noteStore.currentNote.notebookId),
      projectId: event.payload?.projectId === undefined ? noteStore.currentNote.projectId : Number(event.payload.projectId || 0),
      title: String(event.payload?.title || noteStore.currentNote.title),
    })
  }

  const syncCurrentTaskIfNeeded = async (event: SpaceRealtimeEvent) => {
    const changedTaskId = Number(event.payload?.taskId || 0)
    if (changedTaskId && taskStore.currentTask?.id === changedTaskId) {
      await taskStore.loadTask(changedTaskId)
    }
  }

  const activeProjectId = () => {
    const routeProjectId = Number(router.currentRoute.value.params.projectId || 0)
    return projectStore.currentProject?.id || routeProjectId || 0
  }

  const reloadActiveProjectNotes = async () => {
    const projectId = activeProjectId()
    if (projectId) {
      await projectStore.loadProjectNotes(projectId)
    }
  }

  const reloadActiveProjectTasks = async () => {
    const projectId = activeProjectId()
    if (projectId) {
      await projectStore.loadProjectTasks(projectId)
    }
  }

  const reloadActiveProjectMembers = async (event: SpaceRealtimeEvent) => {
    const projectId = activeProjectId()
    const changedProjectId = Number(event.payload?.projectId || 0)
    if (projectId && (!changedProjectId || changedProjectId === projectId)) {
      await projectStore.loadMembers(projectId)
    }
  }

  const handleProjectDeleted = (event: SpaceRealtimeEvent) => {
    const deletedProjectId = Number(event.payload?.projectId || 0)
    if (!deletedProjectId || activeProjectId() !== deletedProjectId) {
      return
    }

    projectStore.currentProject = null
    router.push('/app/projects').catch(() => undefined)
  }

  const handleEvent = (event: SpaceRealtimeEvent) => {
    if (event.spaceId !== spaceStore.currentSpaceId) {
      return
    }

    if (event.type === 'DOCUMENT_DELETED') {
      handleDocumentDeleted(event)
    }

    if (event.type === 'DOCUMENT_TREE_CHANGED') {
      handleDocumentTreeChanged(event)
    }

    if (
      event.type.startsWith('DOCUMENT_') ||
      event.type === 'TAG_CREATED' ||
      event.type === 'TAG_UPDATED' ||
      event.type === 'TAG_DELETED'
    ) {
      runDebounced('notes', async () => {
        await Promise.all([noteStore.loadWorkspace(), reloadActiveProjectNotes(), syncCurrentDocumentMetadata(event)])
      })
    }

    if (event.type.startsWith('TASK_')) {
      runDebounced('tasks', async () => {
        await Promise.all([taskStore.loadTasks(), reloadActiveProjectTasks(), syncCurrentTaskIfNeeded(event)])
      })
      runDebounced('stats', async () => {
        window.dispatchEvent(new CustomEvent<SpaceRealtimeEvent>(SPACE_STATS_CHANGED_EVENT, { detail: event }))
      })
    }

    if (event.type.startsWith('PROJECT_')) {
      if (event.type === 'PROJECT_DELETED') {
        handleProjectDeleted(event)
      }

      runDebounced('projects', async () => {
        const projectId = activeProjectId()
        await Promise.all([
          projectStore.loadProjects(),
          projectStore.loadProjectOptions(),
          projectId && event.type !== 'PROJECT_DELETED' ? projectStore.loadProject(projectId) : Promise.resolve(null),
          event.type === 'PROJECT_MEMBER_CHANGED' ? reloadActiveProjectMembers(event) : Promise.resolve(null),
        ])
      })
    }

    if (event.type.startsWith('TODO_')) {
      runDebounced('todos', () => todoStore.loadTodos())
    }

    if (event.type.startsWith('FILE_')) {
      runDebounced('files', async () => {
        window.dispatchEvent(new CustomEvent<SpaceRealtimeEvent>(SPACE_FILE_CHANGED_EVENT, { detail: event }))
      })
    }

    if (
      event.type === 'SPACE_MEMBER_CHANGED' ||
      event.type === 'MEMBER_ONLINE' ||
      event.type === 'MEMBER_OFFLINE'
    ) {
      runDebounced('members', () => spaceStore.loadMembers())
    }
  }

  const stop = () => {
    provider?.destroy()
    provider = null
    timers.forEach((timer) => window.clearTimeout(timer))
    timers.clear()
  }

  const start = (spaceId: number) => {
    stop()
    provider = new SpaceEventProvider({
      getTicket: async () => {
        const response = await spaceService.createSpaceEventTicket(spaceId)
        return response.ticket
      },
      onEvent: handleEvent,
      wsUrl: resolveWsUrl(),
    })
    provider.connect()
  }

  watch(
    () => ({
      id: spaceStore.currentSpaceId,
      type: spaceStore.currentSpace?.type,
    }),
    (space) => {
      if (!space.id || space.type !== 'TEAM') {
        stop()
        return
      }
      start(space.id)
    },
    { immediate: true },
  )

  onBeforeUnmount(stop)
}
