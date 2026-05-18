import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import { attachmentService, taskService } from '@/api/services'
import { translate } from '@/i18n'
import type { Attachment, PageResponse, Task, TaskComment, TaskQuery, TaskStatus } from '@/types/app'
import { useSpaceStore } from './space'

interface TaskState {
  tasksPage: PageResponse<Task>
  currentTask: Task | null
  currentTaskLoading: boolean
  comments: TaskComment[]
  attachments: Attachment[]
  query: TaskQuery
  loading: boolean
  workspaceSpaceId: number | null
}

const emptyPage = (): PageResponse<Task> => ({
  total: 0,
  pageNum: 1,
  pageSize: 20,
  list: [],
})

let latestTasksToken = 0
let latestTaskToken = 0
let latestCommentsToken = 0
let latestAttachmentsToken = 0

const isCurrentSpace = (spaceId: number) => useSpaceStore().currentSpaceId === spaceId

export const useTaskStore = defineStore('task', {
  state: (): TaskState => ({
    tasksPage: emptyPage(),
    currentTask: null,
    currentTaskLoading: false,
    comments: [],
    attachments: [],
    query: {
      pageNum: 1,
      pageSize: 20,
    },
    loading: false,
    workspaceSpaceId: null,
  }),
  getters: {
    tasks: (state) =>
      state.workspaceSpaceId === useSpaceStore().currentSpaceId ? state.tasksPage.list : [],
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
    async loadTasks() {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      this.ensureWorkspace(spaceId)

      const tasksToken = ++latestTasksToken
      this.loading = true
      try {
        const page = await taskService.page(spaceId, this.query)
        if (tasksToken !== latestTasksToken || !isCurrentSpace(spaceId)) {
          return
        }

        this.tasksPage = page
      } finally {
        if (tasksToken === latestTasksToken) {
          this.loading = false
        }
      }
    },
    async loadTask(taskId: number) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return null
      }

      this.ensureWorkspace(spaceId)

      const taskToken = ++latestTaskToken
      this.currentTaskLoading = true
      if (this.currentTask?.id !== taskId || this.currentTask.spaceId !== spaceId) {
        this.currentTask = null
      }
      try {
        const task = await taskService.detail(spaceId, taskId)
        if (taskToken !== latestTaskToken || !isCurrentSpace(spaceId) || task.spaceId !== spaceId) {
          return task
        }

        this.currentTask = task
        return task
      } finally {
        if (taskToken === latestTaskToken) {
          this.currentTaskLoading = false
        }
      }
    },
    async createTask(payload: Parameters<typeof taskService.create>[1]) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return null
      }

      const task = await taskService.create(spaceId, payload)
      if (!isCurrentSpace(spaceId)) {
        return task
      }

      await this.loadTasks()
      ElMessage.success(translate('messages.taskCreated'))
      return task
    },
    async updateTaskStatus(taskId: number, status: TaskStatus) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      await taskService.updateStatus(spaceId, taskId, status)
      if (!isCurrentSpace(spaceId)) {
        return
      }

      await this.loadTasks()
      if (this.currentTask?.id === taskId && this.currentTask.spaceId === spaceId) {
        await this.loadTask(taskId)
      }
    },
    async claimTask(taskId: number, responsibility: string, isRequired = true) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      await taskService.claim(spaceId, taskId, { responsibility, isRequired })
      if (!isCurrentSpace(spaceId)) {
        return
      }

      await Promise.all([this.loadTasks(), this.currentTask?.id === taskId ? this.loadTask(taskId) : Promise.resolve(null)])
      ElMessage.success(translate('messages.taskClaimed'))
    },
    async startTaskMember(taskId: number, memberId: number) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      await taskService.startMember(spaceId, taskId, memberId)
      if (!isCurrentSpace(spaceId)) {
        return
      }

      await Promise.all([this.loadTasks(), this.currentTask?.id === taskId ? this.loadTask(taskId) : Promise.resolve(null)])
      ElMessage.success(translate('messages.taskStarted'))
    },
    async completeTaskMember(taskId: number, memberId: number, completionRemark?: string) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      await taskService.completeMember(spaceId, taskId, memberId, completionRemark)
      if (!isCurrentSpace(spaceId)) {
        return
      }

      await Promise.all([this.loadTasks(), this.currentTask?.id === taskId ? this.loadTask(taskId) : Promise.resolve(null)])
      ElMessage.success(translate('messages.taskCompleted'))
    },
    async loadComments(taskId: number) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      const commentsToken = ++latestCommentsToken
      const comments = await taskService.comments(spaceId, taskId)
      if (commentsToken !== latestCommentsToken || !isCurrentSpace(spaceId)) {
        return
      }

      this.comments = comments
    },
    async addComment(taskId: number, content: string, mentionUserIds: number[] = []) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      await taskService.addComment(spaceId, taskId, { content, mentionUserIds })
      if (!isCurrentSpace(spaceId)) {
        return
      }

      await this.loadComments(taskId)
      ElMessage.success(translate('messages.commentSent'))
    },
    async loadTaskAttachments(taskId: number) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      const attachmentsToken = ++latestAttachmentsToken
      const attachments = await attachmentService.taskAttachments(spaceId, taskId)
      if (attachmentsToken !== latestAttachmentsToken || !isCurrentSpace(spaceId)) {
        return
      }

      this.attachments = attachments
    },
    async uploadTaskAttachment(taskId: number, file: File, referenceKey = 'task-detail') {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return null
      }

      const formData = new FormData()
      formData.append('file', file)

      const attachment = await attachmentService.upload(spaceId, formData)
      await attachmentService.bind(spaceId, {
        attachmentId: attachment.id,
        businessType: 'TASK',
        businessId: taskId,
        referenceKey,
      })

      if (!isCurrentSpace(spaceId)) {
        return attachment
      }

      if (this.currentTask?.id === taskId) {
        await this.loadTaskAttachments(taskId)
      }

      ElMessage.success(translate('messages.attachmentUploaded'))
      return attachment
    },
    async unbindTaskAttachment(taskId: number, attachmentId: number, referenceKey = 'task-detail') {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      await attachmentService.unbind(spaceId, attachmentId, {
        businessType: 'TASK',
        businessId: taskId,
        referenceKey,
      })

      if (!isCurrentSpace(spaceId)) {
        return
      }

      if (this.currentTask?.id === taskId) {
        await this.loadTaskAttachments(taskId)
      }

      ElMessage.success(translate('messages.attachmentRemoved'))
    },
    async openAttachment(attachmentId: number) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      const attachment = await attachmentService.download(spaceId, attachmentId)
      if (!isCurrentSpace(spaceId)) {
        return
      }

      if (attachment.downloadUrl) {
        window.open(attachment.downloadUrl, '_blank', 'noopener,noreferrer')
      }
    },
    reset() {
      this.resetWorkspaceState()
    },
    resetWorkspaceState() {
      this.tasksPage = emptyPage()
      this.currentTask = null
      this.currentTaskLoading = false
      this.comments = []
      this.attachments = []
      this.query = { pageNum: 1, pageSize: 20 }
      this.loading = false
      this.workspaceSpaceId = null
      latestTasksToken += 1
      latestTaskToken += 1
      latestCommentsToken += 1
      latestAttachmentsToken += 1
    },
  },
})
