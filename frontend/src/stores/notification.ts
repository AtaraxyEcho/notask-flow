import { defineStore } from 'pinia'
import { notificationService } from '@/api/services'
import type { NotificationItem, NotificationQuery, PageResponse } from '@/types/app'

interface NotificationState {
  notificationsPage: PageResponse<NotificationItem>
  unreadCount: number
  query: NotificationQuery
}

const emptyPage = (): PageResponse<NotificationItem> => ({
  total: 0,
  pageNum: 1,
  pageSize: 20,
  list: [],
})

export const useNotificationStore = defineStore('notification', {
  state: (): NotificationState => ({
    notificationsPage: emptyPage(),
    unreadCount: 0,
    query: {
      pageNum: 1,
      pageSize: 20,
    },
  }),
  getters: {
    notifications: (state) => state.notificationsPage.list,
  },
  actions: {
    async loadNotifications() {
      this.notificationsPage = await notificationService.page(this.query)
    },
    async fetchUnreadCount() {
      this.unreadCount = await notificationService.unreadCount()
    },
    async markRead(id: number) {
      await notificationService.markRead(id)
      await Promise.all([this.loadNotifications(), this.fetchUnreadCount()])
    },
    async readAll() {
      await notificationService.readAll()
      await Promise.all([this.loadNotifications(), this.fetchUnreadCount()])
    },
    async clearRead() {
      await notificationService.clearRead()
      await Promise.all([this.loadNotifications(), this.fetchUnreadCount()])
    },
    reset() {
      this.notificationsPage = emptyPage()
      this.unreadCount = 0
      this.query = { pageNum: 1, pageSize: 20 }
    },
  },
})

