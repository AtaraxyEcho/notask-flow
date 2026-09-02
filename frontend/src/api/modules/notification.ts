import { http } from '../http'
import type { NotificationItem, NotificationQuery, PageResponse } from '@/types/app'

export const notificationService = {
  page(query: NotificationQuery) {
    return http.get<PageResponse<NotificationItem>>('/notifications', { params: query })
  },
  unreadCount() {
    return http.get<number>('/notifications/unread-count', { silentError: true })
  },
  markRead(id: number) {
    return http.put<NotificationItem>(`/notifications/${id}/read`)
  },
  readAll() {
    return http.put<void>('/notifications/read-all')
  },
  clearRead() {
    return http.delete<void>('/notifications/read')
  },
  delete(id: number) {
    return http.delete<void>(`/notifications/${id}`)
  },
}
