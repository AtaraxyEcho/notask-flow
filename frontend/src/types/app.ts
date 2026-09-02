export type ThemeMode = 'light' | 'dark' | 'system'
export type PersonalThemePreset = 'sunrise' | 'forest' | 'ocean' | 'midnight'
export type SidebarMode = 'expanded' | 'auto' | 'collapsed'

export type SpaceType = 'PERSONAL' | 'TEAM'
export type AttachmentBusinessType = 'NOTE' | 'TASK' | 'TODO'
export type FileReferenceBusinessType = 'NOTE' | 'TASK' | 'TODO' | 'PROJECT' | 'AVATAR' | 'COMMENT'
export type JoinRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED'
export type TaskMode = 'ASSIGNED' | 'OPEN'
export type TaskStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH'
export type TaskMemberStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED'
export type AssignmentType = 'ASSIGNED' | 'CLAIMED'
export type ProjectMemberRole = 'OWNER' | 'MEMBER'
export type RegisterTeamMode =
  | 'PERSONAL_ONLY'
  | 'CREATE_TEAM'
  | 'APPLY_SUPERVISOR'
  | 'JOIN_INVITE_CODE'

export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

export interface PageResponse<T> {
  total: number
  pageNum: number
  pageSize: number
  list: T[]
}

export interface LoginRequest {
  account: string
  password: string
  clientType?: ClientType
  deviceId?: string
  deviceName?: string
  appVersion?: string
}

export interface LoginResponse {
  userId: number
  tokenName: string
  tokenValue: string
  expireTime: number
  sessionId?: string
  clientType?: ClientType
  refreshToken?: string
}

export type ClientType = 'WEB' | 'ADMIN_WEB' | 'ANDROID' | 'IOS'

export type UserStatus = 'NORMAL' | 'DISABLED'

export interface LoginSession {
  sessionId: string
  userId?: number
  username?: string
  clientType: ClientType
  deviceId?: string
  deviceName?: string
  appVersion?: string
  ip?: string
  userAgent?: string
  loginTime?: string
  lastActiveTime?: string
  expireSeconds?: number
}

export interface AdminUser {
  id: number
  username: string
  nickname?: string
  email: string
  avatarUrl?: string
  status: UserStatus
  online: boolean
  gmtCreate?: string
  gmtModified?: string
}

export interface AdminUserStats {
  totalUsers: number
  todayNewUsers: number
  disabledUsers: number
  onlineUsers: number
}

export interface AdminDashboardTrendPoint {
  date: string
  newUsers: number
  newNotes: number
  newTasks: number
  newTodos: number
  newTeamSpaces: number
  uploadedBytes: number
}

export interface AdminDashboard {
  totalUsers: number
  totalTeamSpaces: number
  totalNotes: number
  totalTasks: number
  totalTodos: number
  totalFiles: number
  totalStorageBytes: number
  todayNewUsers: number
  todayNewNotes: number
  todayNewTasks: number
  todayNewTodos: number
  todayNewTeamSpaces: number
  trends: AdminDashboardTrendPoint[]
}

export interface AdminSystemMonitor {
  osName: string
  osVersion: string
  osArch: string
  javaVersion: string
  cpuUsage: number
  processCpuUsage: number
  systemLoadAverage: number
  cpuCoreCount: number
  physicalMemoryUsedBytes: number
  physicalMemoryTotalBytes: number
  physicalMemoryFreeBytes: number
  physicalMemoryUsage: number
  jvmHeapUsedBytes: number
  jvmHeapMaxBytes: number
  jvmHeapUsage: number
  diskUsedBytes: number
  diskTotalBytes: number
  diskFreeBytes: number
  diskUsage: number
  threadCount: number
  daemonThreadCount: number
  gcCount: number
  gcTimeMillis: number
  uptimeMillis: number
  redisKeyCount: number
  redisUsedMemoryBytes: number
  redisMaxMemoryBytes: number
  redisMemoryUsage: number
  redisHitRate: number
  redisKeyspaceHits: number
  redisKeyspaceMisses: number
  redisConnectedClients: number
  redisOpsPerSecond: number
  networkReceivedBytes: number
  networkTransmittedBytes: number
  networkInterfaceCount: number
  networkActiveInterfaceCount: number
  networkTrafficSupported: boolean
  mysqlQueriesPerSecond: number
  mysqlQuestionCount: number
  mysqlSlowQueryCount: number
  mysqlUptimeSeconds: number
  mysqlThreadsConnected: number
  mysqlThreadsRunning: number
  timestamp: string
}

export interface AdminStorageSummary {
  totalFileCount: number
  totalStorageBytes: number
  orphanFileCount: number
  orphanStorageBytes: number
  deletedFileCount: number
  deletedStorageBytes: number
}

export interface AdminStorageRank {
  targetId: number
  targetName: string
  targetDescription?: string
  fileCount: number
  storageBytes: number
}

export interface AdminOrphanFile {
  id: number
  fileName: string
  fileSize: number
  mimeType?: string
  uploaderId: number
  uploaderName: string
  uploaderEmail?: string
  spaceId: number
  spaceName: string
  gmtCreate: string
}

export interface AdminOrphanCleanResult {
  cleanedCount: number
  cleanedBytes: number
  failedCount: number
}

export interface AdminLoginLog {
  id: number
  userId?: number
  account: string
  clientType?: string
  deviceId?: string
  ipAddress?: string
  userAgent?: string
  success: boolean
  failReason?: string
  gmtCreate: string
}

export interface AdminOperationLog {
  id: number
  operator: string
  method: string
  path: string
  operationName?: string
  ipAddress?: string
  userAgent?: string
  success: boolean
  errorMessage?: string
  gmtCreate: string
}

export interface AdminSystemLog {
  id: number
  eventType: string
  eventData: string
  failReason?: string
  retryCount: number
  status: string
  gmtCreate: string
  gmtModified?: string
}

export interface RegisterRequest {
  username: string
  nickname?: string
  email: string
  password: string
  emailCode?: string
  teamMode?: RegisterTeamMode
  teamName?: string
  supervisorAccount?: string
  teamApplyRemark?: string
  inviteCode?: string
}

export interface AuthSystemSetting {
  registrationEnabled: boolean
  registerEmailVerificationRequired: boolean
  singleDeviceLoginOnly: boolean
}

export interface SendRegisterEmailCodeRequest {
  email: string
}

export interface ForgotPasswordRequest {
  email: string
}

export interface VerifyResetCodeRequest {
  email: string
  code: string
}

export interface VerifyResetCodeResponse {
  resetToken: string
  expireSeconds: number
}

export interface ResetPasswordRequest {
  resetToken: string
  newPassword: string
  confirmPassword: string
}

export interface UserProfile {
  id: number
  username: string
  nickname?: string
  email: string
  avatarUrl?: string
  gmtCreate?: string
}

export interface UserOption {
  id: number
  username: string
  nickname?: string
  email?: string
  avatarUrl?: string
}

export interface NotificationSetting {
  themeMode: ThemeMode
  personalThemePreset: PersonalThemePreset
  sidebarMode: SidebarMode
  taskNoticeEnabled: boolean
  noteNoticeEnabled: boolean
  mentionNoticeEnabled: boolean
  systemNoticeEnabled: boolean
  emailEnabled: boolean
  taskEmailEnabled: boolean
  todoEmailEnabled: boolean
  mentionEmailEnabled: boolean
  quietEnabled: boolean
  quietStartTime?: string
  quietEndTime?: string
}

export interface Space {
  id: number
  name: string
  type: SpaceType
  ownerUserId: number
  memberCount?: number
  unreadCount?: number
  joinApprovalRequired?: boolean
  gmtCreate?: string
}

export interface SpaceMember {
  spaceId: number
  userId: number
  username: string
  nickname?: string
  email?: string
  avatarUrl?: string
  roleId?: number
  roleCode?: string
  roleName?: string
  gmtJoined?: string
  online?: boolean
}

export interface SpaceInvitePreview {
  code: string
  spaceId: number
  spaceName: string
  ownerUsername: string
  roleCode: string
  memberCount: number
  expiresAt?: string
}

export interface SpaceInvite {
  code: string
  spaceId: number
  roleCode: string
  expiresAt?: string
}

export interface SpaceJoinRequest {
  id: number
  applicantUserId: number
  applicantUsername: string
  applicantEmail?: string
  supervisorUserId?: number
  supervisorUsername?: string
  targetSpaceId?: number
  targetSpaceName?: string
  teamName?: string
  status: JoinRequestStatus
  remark?: string
  rejectReason?: string
  reviewerUserId?: number
  reviewedAt?: string
  gmtCreate?: string
}

export interface Tag {
  id: number
  name: string
  spaceId: number
  gmtCreate?: string
}

export interface Attachment {
  id: number
  fileName: string
  fileSize: number
  mimeType?: string
  uploaderId?: number
  spaceId: number
  downloadUrl?: string
  gmtCreate?: string
}

export interface FileFolder {
  id: number
  spaceId: number
  parentId: number
  name: string
  sortOrder?: number
  createdBy?: number
  gmtCreate?: string
  children?: FileFolder[]
}

export interface ManagedFile {
  id: number
  attachmentId: number
  spaceId: number
  folderId?: number
  displayName: string
  fileName?: string
  fileSize: number
  mimeType?: string
  uploaderId?: number
  createdBy?: number
  trashed?: boolean
  deletedAt?: string
  downloadUrl?: string
  previewUrl?: string
  gmtCreate?: string
}

export interface FileStats {
  totalCount: number
  totalSize: number
  trashCount: number
}

export interface FileReference {
  id: number
  attachmentId: number
  businessType: FileReferenceBusinessType
  businessId: number
  referenceKey?: string
  gmtCreate?: string
}

export interface FileOperationLog {
  id: number
  fileId: number
  spaceId: number
  operatorId: number
  operationType: string
  detail?: string
  gmtCreate?: string
}

export interface ManagedFileUploadUrl {
  uploadToken: string
  uploadUrl: string
  method: string
  expiresIn: number
}

export interface FileUploadConfig {
  maxFileSize: number
  multipartThresholdSize: number
  chunkSize: number
  allowedMimeTypes: string[]
}

export interface FileUploadConfigUpdate {
  maxFileSize: number
  multipartThresholdSize: number
  chunkSize: number
  allowedMimeTypes?: string[]
}

export interface ManagedFileChunkUpload {
  uploadToken: string
  chunkSize: number
  totalChunks: number
  expiresIn: number
}

export interface FilePreviewText {
  fileName: string
  mimeType?: string
  textContent: string
}

export interface FilePreviewHtml {
  fileName: string
  mimeType?: string
  htmlContent: string
}

export interface Notebook {
  id: number
  spaceId: number
  parentId: number
  path?: string
  name: string
  sortOrder?: number
  gmtCreate?: string
  children?: Notebook[]
}

export interface NoteHistory {
  id: number
  noteId: number
  title: string
  content?: string
  version: number
  changeSummary?: string
  saveType?: NoteHistorySaveType
  gmtCreate?: string
}

export type NoteHistorySaveType = 'AUTO' | 'MANUAL' | 'CHECKPOINT'

export interface Note {
  id: number
  spaceId: number
  notebookId: number
  projectId?: number
  projectName?: string
  userId: number
  title: string
  content?: string
  contentHtml?: string
  canEdit?: boolean
  collabEnabled?: boolean
  isPublic?: boolean
  shareCode?: string
  shareExpire?: string
  viewCount?: number
  gmtCreate?: string
  gmtModified?: string
  tags?: Tag[]
}

export interface Todo {
  id: number
  spaceId: number
  userId: number
  taskMemberId?: number
  taskId?: number
  taskMode?: TaskMode
  taskStatus?: TaskStatus
  taskMemberStatus?: TaskMemberStatus
  title: string
  isCompleted: boolean
  deadline?: string
  completedAt?: string
  gmtCreate?: string
}

export interface TaskMember {
  id: number
  taskId: number
  userId: number
  username: string
  responsibility: string
  assignmentType?: AssignmentType
  status: TaskMemberStatus
  isRequired?: boolean
  startedAt?: string
  completedAt?: string
  completionRemark?: string
  version?: number
}

export interface TaskComment {
  id: number
  taskId: number
  userId: number
  username: string
  parentCommentId?: number
  content: string
  gmtCreate?: string
  mentionUserIds?: number[]
}

export interface Task {
  id: number
  spaceId: number
  projectId?: number
  projectName?: string
  title: string
  description?: string
  creatorId?: number
  mode: TaskMode
  status: TaskStatus
  priority: TaskPriority
  deadline?: string
  completedAt?: string
  gmtCreate?: string
  gmtModified?: string
  members?: TaskMember[]
}

export interface ProjectMember {
  projectId: number
  userId: number
  username: string
  nickname?: string
  email?: string
  avatarUrl?: string
  role: ProjectMemberRole
  joinedAt?: string
}

export interface Project {
  id: number
  spaceId: number
  name: string
  description?: string
  coverColor?: string
  coverImageUrl?: string
  archived?: boolean
  ownerUserId?: number
  taskCount?: number
  completedTaskCount?: number
  overdueTaskCount?: number
  documentCount?: number
  completionRate?: number
  gmtCreate?: string
  gmtModified?: string
  members?: ProjectMember[]
}

export interface NotificationItem {
  id: number
  userId: number
  spaceId?: number
  type?: string
  businessType?: string
  businessId?: number
  title: string
  content: string
  isRead: boolean
  gmtCreate?: string
}

export interface PersonalStats {
  noteCount: number
  unfinishedTaskMemberCount: number
  completedTaskCountThisMonth: number
}

export interface PersonalNoteTrend {
  date: string
  createdCount: number
  updatedCount: number
}

export interface MemberTaskLoad {
  userId: number
  username: string
  loadCount: number
  completedCount: number
}

export interface TaskTrend {
  date: string
  createdCount: number
  completedCount: number
}

export interface RoleCompletion {
  roleId?: number
  roleCode: string
  roleName: string
  completedCount: number
}

export interface StatsActivity {
  time: string
  memberUserId?: number
  member: string
  type: string
  content: string
  impact?: string
}

export interface NoteQuery {
  pageNum?: number
  pageSize?: number
  notebookId?: number
  tagId?: number
  keyword?: string
  projectId?: number
}

export interface TodoQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  isCompleted?: boolean
  assigneeId?: number
}

export interface TaskQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  status?: TaskStatus
  mode?: TaskMode
  assigneeId?: number
  projectId?: number
}

export interface ProjectQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  archived?: boolean
}

export interface NotificationQuery {
  pageNum?: number
  pageSize?: number
  isRead?: boolean
}

export interface CollabTicketResponse {
  ticket: string
  expiresIn: number
}

export interface SpaceEventTicketResponse {
  ticket: string
  expiresIn: number
}

export type SpaceRealtimeEventType =
  | 'DOCUMENT_CREATED'
  | 'DOCUMENT_UPDATED'
  | 'DOCUMENT_DELETED'
  | 'DOCUMENT_TREE_CHANGED'
  | 'TAG_CREATED'
  | 'TAG_UPDATED'
  | 'TAG_DELETED'
  | 'FILE_CREATED'
  | 'FILE_UPDATED'
  | 'FILE_DELETED'
  | 'FILE_TREE_CHANGED'
  | 'FILE_UPLOAD_CONFIG_UPDATED'
  | 'TASK_CREATED'
  | 'TASK_UPDATED'
  | 'TASK_DELETED'
  | 'PROJECT_CREATED'
  | 'PROJECT_UPDATED'
  | 'PROJECT_DELETED'
  | 'PROJECT_MEMBER_CHANGED'
  | 'TODO_CREATED'
  | 'TODO_UPDATED'
  | 'TODO_DELETED'
  | 'MEMBER_ONLINE'
  | 'MEMBER_OFFLINE'
  | 'SPACE_MEMBER_CHANGED'

export interface SpaceRealtimeEvent {
  actorUserId?: number
  eventId: string
  occurredAt?: string
  payload?: Record<string, unknown>
  spaceId: number
  type: SpaceRealtimeEventType
}

export interface ManagedFileQuery {
  pageNum?: number
  pageSize?: number
  folderId?: number
  keyword?: string
  mimeType?: string
  uploaderId?: number
  trashed?: boolean
}
