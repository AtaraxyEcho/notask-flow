import { http } from '../http'
import type { NotificationSetting, UserOption, UserProfile } from '@/types/app'

export const userService = {
  profile() {
    return http.get<UserProfile>('/user/profile')
  },
  search(keyword: string) {
    return http.get<UserOption[]>('/user/search', { params: { keyword } })
  },
  updateProfile(payload: Partial<UserProfile>) {
    return http.put<UserProfile>('/user/profile', payload)
  },
  updatePassword(payload: { oldPassword: string; newPassword: string }) {
    return http.put<void>('/user/password', payload)
  },
  sendEmailChangeCode(payload: { newEmail: string }) {
    return http.post<void>('/user/email/code', payload)
  },
  changeEmail(payload: { newEmail: string; code: string }) {
    return http.put<UserProfile>('/user/email', payload)
  },
  notificationSettings() {
    return http.get<NotificationSetting>('/user/notification-settings')
  },
  updateNotificationSettings(payload: Partial<NotificationSetting>) {
    return http.put<NotificationSetting>('/user/notification-settings', payload)
  },
  uploadAvatar(formData: FormData) {
    return http.post<UserProfile>('/user/avatar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  },
}
