import { http } from '../http'
import type { Attachment, AttachmentBusinessType } from '@/types/app'

export const attachmentService = {
  upload(spaceId: number, formData: FormData) {
    return http.post<Attachment>(`/spaces/${spaceId}/attachments`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  },
  download(spaceId: number, attachmentId: number) {
    return http.get<Attachment>(`/spaces/${spaceId}/attachments/${attachmentId}/download`)
  },
  bind(
    spaceId: number,
    payload: {
      attachmentId: number
      businessType: AttachmentBusinessType
      businessId: number
      referenceKey?: string
    },
  ) {
    return http.post<void>(`/spaces/${spaceId}/attachments/bind`, payload)
  },
  unbind(
    spaceId: number,
    attachmentId: number,
    payload: {
      businessType: AttachmentBusinessType
      businessId: number
      referenceKey?: string
    },
  ) {
    return http.delete<void>(`/spaces/${spaceId}/attachments/${attachmentId}/unbind`, {
      data: payload,
    })
  },
  taskAttachments(spaceId: number, taskId: number) {
    return http.get<Attachment[]>(`/spaces/${spaceId}/tasks/${taskId}/attachments`)
  },
}
