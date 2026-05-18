import { http } from '../http'
import type { Tag } from '@/types/app'

export const tagService = {
  list(spaceId: number) {
    return http.get<Tag[]>(`/spaces/${spaceId}/tags`)
  },
  create(spaceId: number, payload: { name: string }) {
    return http.post<Tag>(`/spaces/${spaceId}/tags`, payload)
  },
  update(spaceId: number, tagId: number, payload: { name: string }) {
    return http.put<Tag>(`/spaces/${spaceId}/tags/${tagId}`, payload)
  },
  delete(spaceId: number, tagId: number) {
    return http.delete<void>(`/spaces/${spaceId}/tags/${tagId}`)
  },
  bindNoteTags(spaceId: number, noteId: number, tagIds: number[]) {
    return http.post<void>(`/spaces/${spaceId}/notes/${noteId}/tags`, { tagIds })
  },
}
