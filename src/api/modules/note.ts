import { http } from '../http'
import type { CollabTicketResponse, Note, NoteHistory, NoteHistorySaveType, NoteQuery, PageResponse } from '@/types/app'

export const noteService = {
  page(spaceId: number, query: NoteQuery) {
    return http.get<PageResponse<Note>>(`/spaces/${spaceId}/notes`, { params: query })
  },
  search(spaceId: number, keyword: string) {
    return http.get<Note[]>(`/spaces/${spaceId}/notes/search`, { params: { keyword } })
  },
  detail(spaceId: number, noteId: number) {
    return http.get<Note>(`/spaces/${spaceId}/notes/${noteId}`)
  },
  createCollabTicket(spaceId: number, noteId: number) {
    return http.post<CollabTicketResponse>(`/spaces/${spaceId}/notes/${noteId}/collab-ticket`, undefined, {
      silentError: true,
    })
  },
  create(
    spaceId: number,
    payload: {
      notebookId: number
      title: string
      projectId?: number
      content?: string
      contentHtml?: string
      isPublic?: boolean
      tagIds?: number[]
      saveType?: NoteHistorySaveType
    },
  ) {
    return http.post<Note>(`/spaces/${spaceId}/notes`, payload)
  },
  update(
    spaceId: number,
    noteId: number,
    payload: {
      notebookId: number
      title: string
      projectId?: number
      content?: string
      contentHtml?: string
      isPublic?: boolean
      tagIds?: number[]
    },
  ) {
    return http.put<Note>(`/spaces/${spaceId}/notes/${noteId}`, payload)
  },
  saveCollabContent(
    spaceId: number,
    noteId: number,
    payload: {
      content?: string
      contentHtml?: string
    },
  ) {
    return http.put<Note>(`/spaces/${spaceId}/notes/${noteId}/collab-content`, payload, {
      silentError: true,
    })
  },
  createCheckpoint(
    spaceId: number,
    noteId: number,
    payload: {
      content?: string
      contentHtml?: string
    },
  ) {
    return http.post<Note>(`/spaces/${spaceId}/notes/${noteId}/checkpoints`, payload)
  },
  delete(spaceId: number, noteId: number) {
    return http.delete<void>(`/spaces/${spaceId}/notes/${noteId}`)
  },
  history(spaceId: number, noteId: number) {
    return http.get<NoteHistory[]>(`/spaces/${spaceId}/notes/${noteId}/history`)
  },
  restore(spaceId: number, noteId: number, version: number) {
    return http.post<Note>(`/spaces/${spaceId}/notes/${noteId}/history/${version}/restore`)
  },
  share(spaceId: number, noteId: number, expireAt?: string) {
    return http.post<Note>(`/spaces/${spaceId}/notes/${noteId}/share`, { expireAt })
  },
  exportNote(spaceId: number, noteId: number, format: 'pdf' | 'word' | 'image') {
    return http.get<Blob>(`/spaces/${spaceId}/notes/${noteId}/export`, {
      params: { format },
      responseType: 'blob',
    })
  },
  publicNote(shareCode: string) {
    return http.get<Note>(`/public/notes/${shareCode}`)
  },
}
