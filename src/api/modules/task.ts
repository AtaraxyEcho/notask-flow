import { http } from '../http'
import type {
  PageResponse,
  Task,
  TaskComment,
  TaskMember,
  TaskMode,
  TaskPriority,
  TaskQuery,
  TaskStatus,
} from '@/types/app'

export const taskService = {
  page(spaceId: number, query: TaskQuery) {
    return http.get<PageResponse<Task>>(`/spaces/${spaceId}/tasks`, { params: query })
  },
  detail(spaceId: number, taskId: number) {
    return http.get<Task>(`/spaces/${spaceId}/tasks/${taskId}`)
  },
  create(
    spaceId: number,
    payload: {
      title: string
      description?: string
      mode: TaskMode
      priority?: TaskPriority
      deadline?: string
      projectId?: number
      assignments?: Array<{ userId: number; responsibility: string; isRequired?: boolean }>
    },
  ) {
    return http.post<Task>(`/spaces/${spaceId}/tasks`, payload)
  },
  update(
    spaceId: number,
    taskId: number,
    payload: {
      title: string
      description?: string
      priority?: TaskPriority
      deadline?: string
      projectId?: number
    },
  ) {
    return http.put<Task>(`/spaces/${spaceId}/tasks/${taskId}`, payload)
  },
  updateStatus(spaceId: number, taskId: number, status: TaskStatus) {
    return http.patch<Task>(`/spaces/${spaceId}/tasks/${taskId}/status`, { status })
  },
  delete(spaceId: number, taskId: number) {
    return http.delete<void>(`/spaces/${spaceId}/tasks/${taskId}`)
  },
  comments(spaceId: number, taskId: number) {
    return http.get<TaskComment[]>(`/spaces/${spaceId}/tasks/${taskId}/comments`)
  },
  addComment(spaceId: number, taskId: number, payload: { content: string; mentionUserIds?: number[] }) {
    return http.post<TaskComment>(`/spaces/${spaceId}/tasks/${taskId}/comments`, payload)
  },
  assign(
    spaceId: number,
    taskId: number,
    payload: { userId: number; responsibility: string; isRequired?: boolean },
  ) {
    return http.post<TaskMember>(`/spaces/${spaceId}/tasks/${taskId}/assign`, payload)
  },
  claim(spaceId: number, taskId: number, payload: { responsibility: string; isRequired?: boolean }) {
    return http.post<TaskMember>(`/spaces/${spaceId}/tasks/${taskId}/claim`, payload)
  },
  startMember(spaceId: number, taskId: number, memberId: number) {
    return http.post<TaskMember>(`/spaces/${spaceId}/tasks/${taskId}/members/${memberId}/start`)
  },
  completeMember(spaceId: number, taskId: number, memberId: number, completionRemark?: string) {
    return http.post<TaskMember>(`/spaces/${spaceId}/tasks/${taskId}/members/${memberId}/complete`, {
      completionRemark,
    })
  },
}
