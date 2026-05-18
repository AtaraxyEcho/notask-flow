import { http } from '../http'
import type {
  Note,
  NoteQuery,
  PageResponse,
  Project,
  ProjectMember,
  ProjectQuery,
  Task,
  TaskQuery,
} from '@/types/app'

export const projectService = {
  page(spaceId: number, query: ProjectQuery) {
    return http.get<PageResponse<Project>>(`/spaces/${spaceId}/projects`, { params: query })
  },
  options(spaceId: number) {
    return http.get<Project[]>(`/spaces/${spaceId}/projects/options`)
  },
  detail(spaceId: number, projectId: number) {
    return http.get<Project>(`/spaces/${spaceId}/projects/${projectId}`)
  },
  create(
    spaceId: number,
    payload: {
      name: string
      description?: string
      coverColor?: string
      coverImageUrl?: string
      ownerUserId?: number
    },
  ) {
    return http.post<Project>(`/spaces/${spaceId}/projects`, payload)
  },
  update(
    spaceId: number,
    projectId: number,
    payload: {
      name: string
      description?: string
      coverColor?: string
      coverImageUrl?: string
      ownerUserId?: number
    },
  ) {
    return http.put<Project>(`/spaces/${spaceId}/projects/${projectId}`, payload)
  },
  archive(spaceId: number, projectId: number, archived: boolean) {
    return http.put<Project>(`/spaces/${spaceId}/projects/${projectId}/archive`, { archived })
  },
  delete(spaceId: number, projectId: number) {
    return http.delete<void>(`/spaces/${spaceId}/projects/${projectId}`)
  },
  members(spaceId: number, projectId: number) {
    return http.get<ProjectMember[]>(`/spaces/${spaceId}/projects/${projectId}/members`)
  },
  addMember(spaceId: number, projectId: number, payload: { userId: number; role?: string }) {
    return http.post<ProjectMember>(`/spaces/${spaceId}/projects/${projectId}/members`, payload)
  },
  updateMemberRole(spaceId: number, projectId: number, userId: number, role: string) {
    return http.put<ProjectMember>(`/spaces/${spaceId}/projects/${projectId}/members/${userId}`, { role })
  },
  removeMember(spaceId: number, projectId: number, userId: number) {
    return http.delete<void>(`/spaces/${spaceId}/projects/${projectId}/members/${userId}`)
  },
  tasks(spaceId: number, projectId: number, query: TaskQuery) {
    return http.get<PageResponse<Task>>(`/spaces/${spaceId}/projects/${projectId}/tasks`, { params: query })
  },
  notes(spaceId: number, projectId: number, query: NoteQuery) {
    return http.get<PageResponse<Note>>(`/spaces/${spaceId}/projects/${projectId}/notes`, { params: query })
  },
}
