import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import { noteService, projectService } from '@/api/services'
import { translate } from '@/i18n'
import type { Note, PageResponse, Project, ProjectMember, ProjectQuery, Task } from '@/types/app'
import { useSpaceStore } from './space'

interface ProjectState {
  projectsPage: PageResponse<Project>
  projectOptions: Project[]
  currentProject: Project | null
  members: ProjectMember[]
  relatedTasks: PageResponse<Task>
  relatedNotes: PageResponse<Note>
  query: ProjectQuery
  loading: boolean
  workspaceSpaceId: number | null
}

const emptyProjects = (): PageResponse<Project> => ({
  total: 0,
  pageNum: 1,
  pageSize: 12,
  list: [],
})

const emptyTasks = (): PageResponse<Task> => ({
  total: 0,
  pageNum: 1,
  pageSize: 10,
  list: [],
})

const emptyNotes = (): PageResponse<Note> => ({
  total: 0,
  pageNum: 1,
  pageSize: 10,
  list: [],
})

let latestProjectsToken = 0
let latestProjectToken = 0
let latestProjectOptionsToken = 0
let latestMembersToken = 0
let latestProjectTasksToken = 0
let latestProjectNotesToken = 0

const isCurrentSpace = (spaceId: number) => useSpaceStore().currentSpaceId === spaceId

export const useProjectStore = defineStore('project', {
  state: (): ProjectState => ({
    projectsPage: emptyProjects(),
    projectOptions: [],
    currentProject: null,
    members: [],
    relatedTasks: emptyTasks(),
    relatedNotes: emptyNotes(),
    query: {
      pageNum: 1,
      pageSize: 12,
      archived: false,
      keyword: '',
    },
    loading: false,
    workspaceSpaceId: null,
  }),
  getters: {
    projects: (state) =>
      state.workspaceSpaceId === useSpaceStore().currentSpaceId ? state.projectsPage.list : [],
    availableProjects: (state) =>
      state.workspaceSpaceId === useSpaceStore().currentSpaceId
        ? state.projectOptions.length
          ? state.projectOptions
          : state.projectsPage.list
        : [],
  },
  actions: {
    ensureWorkspace(spaceId: number) {
      if (this.workspaceSpaceId === spaceId) {
        return
      }

      const pendingQuery = { ...this.query }
      this.resetWorkspaceState()
      this.query = pendingQuery
      this.workspaceSpaceId = spaceId
    },
    async loadProjects() {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      this.ensureWorkspace(spaceId)

      const projectsToken = ++latestProjectsToken
      this.loading = true
      try {
        const page = await projectService.page(spaceId, this.query)
        if (projectsToken !== latestProjectsToken || !isCurrentSpace(spaceId)) {
          return
        }

        this.projectsPage = page
      } finally {
        if (projectsToken === latestProjectsToken) {
          this.loading = false
        }
      }
    },
    async loadProject(projectId: number) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return null
      }

      this.ensureWorkspace(spaceId)

      const projectToken = ++latestProjectToken
      if (this.currentProject?.id !== projectId || this.currentProject.spaceId !== spaceId) {
        this.currentProject = null
      }

      const project = await projectService.detail(spaceId, projectId)
      if (projectToken !== latestProjectToken || !isCurrentSpace(spaceId) || project.spaceId !== spaceId) {
        return project
      }

      this.currentProject = project
      return project
    },
    async loadProjectOptions() {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      this.ensureWorkspace(spaceId)

      const optionsToken = ++latestProjectOptionsToken
      const options = await projectService.options(spaceId)
      if (optionsToken !== latestProjectOptionsToken || !isCurrentSpace(spaceId)) {
        return
      }

      this.projectOptions = options
    },
    async createProject(payload: Parameters<typeof projectService.create>[1]) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return null
      }

      const project = await projectService.create(spaceId, payload)
      if (!isCurrentSpace(spaceId)) {
        return project
      }

      ElMessage.success(translate('messages.projectCreated'))
      await Promise.all([this.loadProjects(), this.loadProjectOptions()])
      return project
    },
    async updateProject(projectId: number, payload: Parameters<typeof projectService.update>[2]) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      const project = await projectService.update(spaceId, projectId, payload)
      if (!isCurrentSpace(spaceId) || project.spaceId !== spaceId) {
        return
      }

      this.currentProject = project
      await Promise.all([this.loadProjects(), this.loadProjectOptions()])
      ElMessage.success(translate('messages.projectUpdated'))
    },
    async archiveProject(projectId: number, archived: boolean) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      await projectService.archive(spaceId, projectId, archived)
      if (!isCurrentSpace(spaceId)) {
        return
      }

      await Promise.all([this.loadProjects(), this.loadProjectOptions()])
      ElMessage.success(archived ? translate('messages.projectArchived') : translate('messages.projectRestored'))
    },
    async loadMembers(projectId: number) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      const membersToken = ++latestMembersToken
      const members = await projectService.members(spaceId, projectId)
      if (membersToken !== latestMembersToken || !isCurrentSpace(spaceId) || this.currentProject?.id !== projectId) {
        return
      }

      this.members = members
    },
    async loadProjectTasks(projectId: number, query: Parameters<typeof projectService.tasks>[2] = { pageNum: 1, pageSize: 50 }) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      const tasksToken = ++latestProjectTasksToken
      const tasks = await projectService.tasks(spaceId, projectId, query)
      if (tasksToken !== latestProjectTasksToken || !isCurrentSpace(spaceId) || this.currentProject?.id !== projectId) {
        return
      }

      this.relatedTasks = tasks
    },
    async loadProjectNotes(projectId: number, query: Parameters<typeof projectService.notes>[2] = { pageNum: 1, pageSize: 50 }) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      const notesToken = ++latestProjectNotesToken
      const notes = await projectService.notes(spaceId, projectId, query)
      if (notesToken !== latestProjectNotesToken || !isCurrentSpace(spaceId) || this.currentProject?.id !== projectId) {
        return
      }

      this.relatedNotes = notes
    },
    async addMember(projectId: number, payload: { userId: number; role?: string }) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      await projectService.addMember(spaceId, projectId, payload)
      if (!isCurrentSpace(spaceId)) {
        return
      }

      await this.loadMembers(projectId)
      ElMessage.success(translate('messages.projectMemberAdded'))
    },
    async updateMemberRole(projectId: number, userId: number, role: string) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      await projectService.updateMemberRole(spaceId, projectId, userId, role)
      if (!isCurrentSpace(spaceId)) {
        return
      }

      await this.loadMembers(projectId)
      ElMessage.success(translate('messages.projectMemberRoleUpdated'))
    },
    async removeMember(projectId: number, userId: number) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      await projectService.removeMember(spaceId, projectId, userId)
      if (!isCurrentSpace(spaceId)) {
        return
      }

      await this.loadMembers(projectId)
      ElMessage.success(translate('messages.projectMemberRemoved'))
    },
    async deleteProject(projectId: number) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      await projectService.delete(spaceId, projectId)
      if (!isCurrentSpace(spaceId)) {
        return
      }

      await Promise.all([this.loadProjects(), this.loadProjectOptions()])
      if (this.currentProject?.id === projectId) {
        this.currentProject = null
      }
      ElMessage.success(translate('messages.projectDeleted'))
    },
    async bindNotesToProject(projectId: number, notes: Note[]) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId || !notes.length) {
        return
      }

      await Promise.all(
        notes.map((note) =>
          noteService.update(spaceId, note.id, {
            title: note.title,
            notebookId: note.notebookId,
            projectId,
            content: note.content,
            contentHtml: note.contentHtml,
            tagIds: note.tags?.map((tag) => tag.id),
          }),
        ),
      )
      if (!isCurrentSpace(spaceId)) {
        return
      }

      await this.loadProjectNotes(projectId)
      ElMessage.success(translate('messages.projectNoteAssociated'))
    },
    async unbindNoteFromProject(projectId: number, note: Note) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      await noteService.update(spaceId, note.id, {
        title: note.title,
        notebookId: note.notebookId,
        content: note.content,
        contentHtml: note.contentHtml,
        tagIds: note.tags?.map((tag) => tag.id),
      })
      if (!isCurrentSpace(spaceId)) {
        return
      }

      await this.loadProjectNotes(projectId)
      ElMessage.success(translate('messages.projectNoteUnlinked'))
    },
    reset() {
      this.resetWorkspaceState()
    },
    resetWorkspaceState() {
      this.projectsPage = emptyProjects()
      this.projectOptions = []
      this.currentProject = null
      this.members = []
      this.relatedTasks = emptyTasks()
      this.relatedNotes = emptyNotes()
      this.query = { pageNum: 1, pageSize: 12, archived: false, keyword: '' }
      this.loading = false
      this.workspaceSpaceId = null
      latestProjectsToken += 1
      latestProjectToken += 1
      latestProjectOptionsToken += 1
      latestMembersToken += 1
      latestProjectTasksToken += 1
      latestProjectNotesToken += 1
    },
  },
})
