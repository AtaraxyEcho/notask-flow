import { http } from '../http'
import type {
  FileFolder,
  FileOperationLog,
  FilePreviewHtml,
  FilePreviewText,
  FileReference,
  FileStats,
  FileUploadConfig,
  FileUploadConfigUpdate,
  ManagedFile,
  ManagedFileChunkUpload,
  ManagedFileQuery,
  ManagedFileUploadUrl,
  PageResponse,
} from '@/types/app'

export const fileService = {
  page(spaceId: number, query: ManagedFileQuery) {
    return http.get<PageResponse<ManagedFile>>(`/spaces/${spaceId}/files`, {
      params: query,
    })
  },
  detail(spaceId: number, fileId: number) {
    return http.get<ManagedFile>(`/spaces/${spaceId}/files/${fileId}`)
  },
  tree(spaceId: number) {
    return http.get<FileFolder[]>(`/spaces/${spaceId}/files/tree`)
  },
  uploadConfig(spaceId: number) {
    return http.get<FileUploadConfig>(`/spaces/${spaceId}/files/upload-config`)
  },
  updateUploadConfig(spaceId: number, payload: FileUploadConfigUpdate) {
    return http.put<FileUploadConfig>(`/spaces/${spaceId}/files/upload-config`, payload)
  },
  stats(spaceId: number) {
    return http.get<FileStats>(`/spaces/${spaceId}/files/stats`)
  },
  createFolder(spaceId: number, payload: { name: string; parentId?: number }) {
    return http.post<FileFolder>(`/spaces/${spaceId}/files/folders`, payload)
  },
  updateFolder(spaceId: number, folderId: number, payload: { name: string; parentId?: number }) {
    return http.put<FileFolder>(`/spaces/${spaceId}/files/folders/${folderId}`, payload)
  },
  deleteFolder(spaceId: number, folderId: number) {
    return http.delete<void>(`/spaces/${spaceId}/files/folders/${folderId}`)
  },
  upload(spaceId: number, payload: { file: File; folderId?: number }) {
    const formData = new FormData()
    formData.append('file', payload.file)
    return http.post<ManagedFile>(`/spaces/${spaceId}/files/upload`, formData, {
      params: {
        folderId: payload.folderId,
      },
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  },
  editorUpload(spaceId: number, file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return http.post<ManagedFile>(`/spaces/${spaceId}/files/editor-upload`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  },
  initChunkUpload(
    spaceId: number,
    payload: {
      fileName: string
      fileSize: number
      mimeType: string
      folderId?: number
    },
  ) {
    return http.post<ManagedFileChunkUpload>(`/spaces/${spaceId}/files/chunk-upload/init`, payload)
  },
  uploadChunk(spaceId: number, uploadToken: string, chunkIndex: number, chunk: Blob) {
    const formData = new FormData()
    formData.append('chunk', chunk)
    return http.post<void>(`/spaces/${spaceId}/files/chunk-upload/${uploadToken}/chunks`, formData, {
      params: {
        chunkIndex,
      },
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  },
  completeChunkUpload(spaceId: number, payload: { uploadToken: string; displayName?: string }) {
    return http.post<ManagedFile>(`/spaces/${spaceId}/files/chunk-upload/complete`, payload)
  },
  createUploadUrl(
    spaceId: number,
    payload: {
      fileName: string
      fileSize: number
      mimeType: string
      folderId?: number
    },
  ) {
    return http.post<ManagedFileUploadUrl>(`/spaces/${spaceId}/files/upload-url`, payload)
  },
  completeUpload(spaceId: number, payload: { uploadToken: string; displayName?: string }) {
    return http.post<ManagedFile>(`/spaces/${spaceId}/files/complete`, payload)
  },
  updateFile(spaceId: number, fileId: number, payload: { displayName?: string; folderId?: number }) {
    return http.put<ManagedFile>(`/spaces/${spaceId}/files/${fileId}`, payload)
  },
  downloadUrl(spaceId: number, fileId: number) {
    return http.get<ManagedFile>(`/spaces/${spaceId}/files/${fileId}/download-url`)
  },
  previewUrl(spaceId: number, fileId: number) {
    return http.get<ManagedFile>(`/spaces/${spaceId}/files/${fileId}/preview-url`)
  },
  previewBlob(spaceId: number, fileId: number) {
    return http.get<Blob>(`/spaces/${spaceId}/files/${fileId}/preview`, {
      responseType: 'blob',
    })
  },
  previewText(spaceId: number, fileId: number) {
    return http.get<FilePreviewText>(`/spaces/${spaceId}/files/${fileId}/preview-text`)
  },
  previewHtml(spaceId: number, fileId: number) {
    return http.get<FilePreviewHtml>(`/spaces/${spaceId}/files/${fileId}/preview-html`)
  },
  trash(spaceId: number, fileId: number) {
    return http.delete<void>(`/spaces/${spaceId}/files/${fileId}`)
  },
  restore(spaceId: number, fileId: number) {
    return http.post<ManagedFile>(`/spaces/${spaceId}/files/${fileId}/restore`)
  },
  physicalDelete(spaceId: number, fileId: number, force = false) {
    return http.delete<void>(`/spaces/${spaceId}/files/${fileId}/physical`, {
      params: {
        force,
      },
    })
  },
  references(spaceId: number, fileId: number) {
    return http.get<FileReference[]>(`/spaces/${spaceId}/files/${fileId}/references`)
  },
  operationLogs(spaceId: number, fileId: number, pageNum = 1, pageSize = 10) {
    return http.get<PageResponse<FileOperationLog>>(`/spaces/${spaceId}/files/${fileId}/operation-logs`, {
      params: {
        pageNum,
        pageSize,
      },
    })
  },
}
