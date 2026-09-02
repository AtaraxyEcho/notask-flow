import { http } from '../http'
import type { PageResponse, Todo, TodoQuery } from '@/types/app'

export const todoService = {
  page(spaceId: number, query: TodoQuery) {
    return http.get<PageResponse<Todo>>(`/spaces/${spaceId}/todos`, { params: query })
  },
  create(spaceId: number, payload: { title: string; deadline?: string }) {
    return http.post<Todo>(`/spaces/${spaceId}/todos`, payload)
  },
  update(spaceId: number, todoId: number, payload: { title: string; deadline?: string }) {
    return http.put<Todo>(`/spaces/${spaceId}/todos/${todoId}`, payload)
  },
  complete(spaceId: number, todoId: number) {
    return http.put<Todo>(`/spaces/${spaceId}/todos/${todoId}/complete`)
  },
  uncomplete(spaceId: number, todoId: number) {
    return http.put<Todo>(`/spaces/${spaceId}/todos/${todoId}/uncomplete`)
  },
  delete(spaceId: number, todoId: number) {
    return http.delete<void>(`/spaces/${spaceId}/todos/${todoId}`)
  },
}
