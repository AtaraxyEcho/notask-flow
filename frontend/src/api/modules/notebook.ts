import { http } from '../http'
import type { Notebook } from '@/types/app'

export const notebookService = {
  tree(spaceId: number) {
    return http.get<Notebook[]>(`/spaces/${spaceId}/notebooks`)
  },
  create(spaceId: number, payload: { name: string; parentId?: number; sortOrder?: number }) {
    return http.post<Notebook>(`/spaces/${spaceId}/notebooks`, payload)
  },
  update(
    spaceId: number,
    notebookId: number,
    payload: { name: string; parentId?: number; sortOrder?: number },
  ) {
    return http.put<Notebook>(`/spaces/${spaceId}/notebooks/${notebookId}`, payload)
  },
  delete(spaceId: number, notebookId: number) {
    return http.delete<void>(`/spaces/${spaceId}/notebooks/${notebookId}`)
  },
}
