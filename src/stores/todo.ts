import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import { todoService } from '@/api/services'
import { translate } from '@/i18n'
import type { PageResponse, Todo, TodoQuery } from '@/types/app'
import { useSpaceStore } from './space'

interface TodoSummary {
  completed: number
  total: number
  unfinished: number
}

interface TodoState {
  todosPage: PageResponse<Todo>
  summary: TodoSummary
  query: TodoQuery
  loading: boolean
  workspaceSpaceId: number | null
}

const emptyPage = (): PageResponse<Todo> => ({
  total: 0,
  pageNum: 1,
  pageSize: 10,
  list: [],
})

const emptySummary = (): TodoSummary => ({
  completed: 0,
  total: 0,
  unfinished: 0,
})

let latestTodosToken = 0

const isCurrentSpace = (spaceId: number) => useSpaceStore().currentSpaceId === spaceId

const createSummaryQuery = (query: TodoQuery, isCompleted?: boolean): TodoQuery => ({
  assigneeId: query.assigneeId,
  isCompleted,
  keyword: query.keyword,
  pageNum: 1,
  pageSize: 1,
})

const loadSummary = async (spaceId: number, query: TodoQuery): Promise<TodoSummary> => {
  const [allPage, completedPage, unfinishedPage] = await Promise.all([
    todoService.page(spaceId, createSummaryQuery(query)),
    todoService.page(spaceId, createSummaryQuery(query, true)),
    todoService.page(spaceId, createSummaryQuery(query, false)),
  ])

  return {
    completed: completedPage.total,
    total: allPage.total,
    unfinished: unfinishedPage.total,
  }
}

export const useTodoStore = defineStore('todo', {
  state: (): TodoState => ({
    todosPage: emptyPage(),
    summary: emptySummary(),
    query: {
      pageNum: 1,
      pageSize: 20,
      keyword: '',
    },
    loading: false,
    workspaceSpaceId: null,
  }),
  getters: {
    todos: (state) =>
      state.workspaceSpaceId === useSpaceStore().currentSpaceId ? state.todosPage.list : [],
    unfinishedCount: (state) =>
      state.workspaceSpaceId === useSpaceStore().currentSpaceId ? state.summary.unfinished : 0,
    completedCount: (state) =>
      state.workspaceSpaceId === useSpaceStore().currentSpaceId ? state.summary.completed : 0,
    totalCount: (state) =>
      state.workspaceSpaceId === useSpaceStore().currentSpaceId ? state.summary.total : 0,
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
    async loadTodos() {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      this.ensureWorkspace(spaceId)

      const todosToken = ++latestTodosToken
      this.loading = true
      try {
        const [page, summary] = await Promise.all([
          todoService.page(spaceId, this.query),
          loadSummary(spaceId, this.query),
        ])
        if (todosToken !== latestTodosToken || !isCurrentSpace(spaceId)) {
          return
        }

        this.todosPage = page
        this.summary = summary
      } finally {
        if (todosToken === latestTodosToken) {
          this.loading = false
        }
      }
    },
    async createTodo(payload: { title: string; deadline?: string }) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return null
      }

      const todo = await todoService.create(spaceId, payload)
      if (!isCurrentSpace(spaceId)) {
        return todo
      }

      ElMessage.success(translate('messages.todoCreated'))
      await this.loadTodos()
      return todo
    },
    async updateTodo(todoId: number, payload: { title: string; deadline?: string }) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      await todoService.update(spaceId, todoId, payload)
      if (!isCurrentSpace(spaceId)) {
        return
      }

      ElMessage.success(translate('messages.todoUpdated'))
      await this.loadTodos()
    },
    async toggleTodo(todo: Todo) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      if (todo.isCompleted) {
        await todoService.uncomplete(spaceId, todo.id)
      } else {
        await todoService.complete(spaceId, todo.id)
      }

      if (!isCurrentSpace(spaceId)) {
        return
      }

      await this.loadTodos()
    },
    async removeTodo(todoId: number) {
      const spaceId = useSpaceStore().currentSpaceId
      if (!spaceId) {
        return
      }

      await todoService.delete(spaceId, todoId)
      if (!isCurrentSpace(spaceId)) {
        return
      }

      ElMessage.success(translate('messages.todoDeleted'))
      await this.loadTodos()
    },
    reset() {
      this.resetWorkspaceState()
    },
    resetWorkspaceState() {
      this.todosPage = emptyPage()
      this.summary = emptySummary()
      this.query = { pageNum: 1, pageSize: 20, keyword: '' }
      this.loading = false
      this.workspaceSpaceId = null
      latestTodosToken += 1
    },
  },
})
