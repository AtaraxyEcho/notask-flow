import { adminHttp } from '@/api/adminHttp'
import type {
  AdminUser,
  AdminUserStats,
  AdminDashboard,
  AdminOrphanCleanResult,
  AdminLoginLog,
  AdminOperationLog,
  AdminOrphanFile,
  AdminStorageRank,
  AdminStorageSummary,
  AdminSystemLog,
  AdminSystemMonitor,
  LoginRequest,
  LoginResponse,
  LoginSession,
  NotificationItem,
  PageResponse,
  UserStatus,
} from '@/types/app'

export interface AdminMe {
  username: string
  clientType: string
  sessionId: string
}

export const adminAuthService = {
  login(payload: LoginRequest) {
    return adminHttp.post<LoginResponse>('/admin/auth/login', payload)
  },
  logout() {
    return adminHttp.post<void>('/admin/auth/logout')
  },
  me() {
    return adminHttp.get<AdminMe>('/admin/auth/me')
  },
}

export const adminSessionService = {
  list() {
    return adminHttp.get<LoginSession[]>('/admin/sessions')
  },
  revoke(sessionId: string) {
    return adminHttp.delete<void>(`/admin/sessions/${sessionId}`)
  },
  revokeUser(userId: number) {
    return adminHttp.delete<void>(`/admin/users/${userId}/sessions`)
  },
}

export const adminDashboardService = {
  overview() {
    return adminHttp.get<AdminDashboard>('/admin/dashboard')
  },
}

export const adminMonitorService = {
  snapshot() {
    return adminHttp.get<AdminSystemMonitor>('/admin/monitor')
  },
}

export const adminStorageService = {
  summary() {
    return adminHttp.get<AdminStorageSummary>('/admin/storage/summary')
  },
  topUsers() {
    return adminHttp.get<AdminStorageRank[]>('/admin/storage/top-users')
  },
  topSpaces() {
    return adminHttp.get<AdminStorageRank[]>('/admin/storage/top-spaces')
  },
  orphanFiles(params = { pageNum: 1, pageSize: 10 }) {
    return adminHttp.get<PageResponse<AdminOrphanFile>>('/admin/storage/orphan-files', { params })
  },
  scanOrphanFiles(params = { pageNum: 1, pageSize: 10 }) {
    return adminHttp.post<PageResponse<AdminOrphanFile>>('/admin/storage/orphan-files/scan', undefined, { params })
  },
  cleanOrphanFiles() {
    return adminHttp.post<AdminOrphanCleanResult>('/admin/storage/orphan-files/clean')
  },
}

export interface AdminLogQuery {
  keyword?: string
  success?: boolean | ''
  eventType?: string
  status?: string
  pageNum?: number
  pageSize?: number
}

const cleanAdminLogParams = (params: AdminLogQuery) =>
  Object.fromEntries(Object.entries(params).filter(([, value]) => value !== '' && value !== undefined && value !== null))

export const adminLogService = {
  loginLogs(params: AdminLogQuery) {
    return adminHttp.get<PageResponse<AdminLoginLog>>('/admin/logs/login', { params: cleanAdminLogParams(params) })
  },
  operationLogs(params: AdminLogQuery) {
    return adminHttp.get<PageResponse<AdminOperationLog>>('/admin/logs/operations', {
      params: cleanAdminLogParams(params),
    })
  },
  systemLogs(params: AdminLogQuery) {
    return adminHttp.get<PageResponse<AdminSystemLog>>('/admin/logs/system', { params: cleanAdminLogParams(params) })
  },
}

export interface AdminUserQuery {
  keyword?: string
  status?: UserStatus | ''
  pageNum?: number
  pageSize?: number
}

export const adminUserService = {
  page(params: AdminUserQuery) {
    return adminHttp.get<PageResponse<AdminUser>>('/admin/users', { params })
  },
  stats() {
    return adminHttp.get<AdminUserStats>('/admin/users/stats')
  },
  updateStatus(userId: number, status: UserStatus) {
    return adminHttp.put<AdminUser>(`/admin/users/${userId}/status`, { status })
  },
  resetPassword(userId: number, newPassword: string) {
    return adminHttp.put<void>(`/admin/users/${userId}/password`, { newPassword })
  },
  remove(userId: number) {
    return adminHttp.delete<void>(`/admin/users/${userId}`)
  },
}

export interface AdminSystemNotificationRequest {
  title: string
  content: string
}

export const adminSystemNotificationService = {
  send(payload: AdminSystemNotificationRequest) {
    return adminHttp.post<void>('/admin/system-notifications', payload)
  },
  history(params = { pageNum: 1, pageSize: 10 }) {
    return adminHttp.get<PageResponse<NotificationItem>>('/admin/system-notifications/history', { params })
  },
}

export interface AdminSystemSetting {
  settingKey: string
  settingValue: string
  description?: string
}

export const adminSettingService = {
  list() {
    return adminHttp.get<AdminSystemSetting[]>('/admin/settings')
  },
  update(settings: Record<string, string>) {
    return adminHttp.put<AdminSystemSetting[]>('/admin/settings', { settings })
  },
}
