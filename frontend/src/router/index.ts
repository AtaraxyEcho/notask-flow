import { createRouter, createWebHistory } from 'vue-router'
import { useNoteStore } from '@/stores/note'
import { useNotificationStore } from '@/stores/notification'
import { useProjectStore } from '@/stores/project'
import { useSpaceStore } from '@/stores/space'
import { useTaskStore } from '@/stores/task'
import { useTodoStore } from '@/stores/todo'
import { useAdminStore } from '@/stores/admin'
import { useUserStore } from '@/stores/user'
import { normalizeAuthRedirect } from '@/utils/redirect'

const AuthLayout = () => import('@/layouts/AuthLayout.vue')
const AppLayout = () => import('@/layouts/AppLayout.vue')
const AdminLayout = () => import('@/layouts/AdminLayout.vue')
const LoginView = () => import('@/views/auth/LoginView.vue')
const RegisterView = () => import('@/views/auth/RegisterView.vue')
const ForgotPasswordView = () => import('@/views/auth/ForgotPasswordView.vue')
const ResetPasswordView = () => import('@/views/auth/ResetPasswordView.vue')
const PublicNoteView = () => import('@/views/shared/PublicNoteView.vue')
const TeamInviteView = () => import('@/views/shared/TeamInviteView.vue')
const AndroidCollabNoteView = () => import('@/views/android/AndroidCollabNoteView.vue')
const NoteView = () => import('@/views/notes/NoteView.vue')
const TaskView = () => import('@/views/tasks/TaskView.vue')
const TodoView = () => import('@/views/todos/TodoView.vue')
const FileManagementView = () => import('@/views/files/FileManagementView.vue')
const FilePreviewView = () => import('@/views/files/FilePreviewView.vue')
const StatsView = () => import('@/views/stats/StatsView.vue')
const TeamStatsView = () => import('@/views/stats/TeamStatsView.vue')
const NotificationView = () => import('@/views/notifications/NotificationView.vue')
const SettingsView = () => import('@/views/settings/SettingsView.vue')
const SpaceSettingsView = () => import('@/views/spaces/SpaceSettingsView.vue')
const ProjectListView = () => import('@/views/projects/ProjectListView.vue')
const ProjectDetailView = () => import('@/views/projects/ProjectDetailView.vue')
const NotFoundView = () => import('@/views/shared/NotFoundView.vue')
const AdminLoginView = () => import('@/views/admin/AdminLoginView.vue')
const AdminDashboardView = () => import('@/views/admin/AdminDashboardView.vue')
const AdminUserView = () => import('@/views/admin/AdminUserView.vue')
const AdminSessionView = () => import('@/views/admin/AdminSessionView.vue')
const AdminSettingView = () => import('@/views/admin/AdminSettingView.vue')
const AdminSystemNotificationView = () => import('@/views/admin/AdminSystemNotificationView.vue')
const AdminMonitorView = () => import('@/views/admin/AdminMonitorView.vue')
const AdminStorageView = () => import('@/views/admin/AdminStorageView.vue')
const AdminLogView = () => import('@/views/admin/AdminLogView.vue')
const AdminPlaceholderView = () => import('@/views/admin/AdminPlaceholderView.vue')
const appRouteLoaders = [
  NoteView,
  TaskView,
  TodoView,
  FileManagementView,
  StatsView,
  TeamStatsView,
  NotificationView,
  SettingsView,
  SpaceSettingsView,
  ProjectListView,
  ProjectDetailView,
]

let appRoutesPrefetched = false

export function prefetchAppRouteComponents() {
  if (appRoutesPrefetched) {
    return
  }

  appRoutesPrefetched = true
  appRouteLoaders.forEach((loader) => {
    loader().catch(() => undefined)
  })
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/app',
    },
    {
      path: '/login',
      component: AuthLayout,
      meta: { guestOnly: true },
      children: [
        {
          path: '',
          name: 'login',
          component: LoginView,
        },
      ],
    },
    {
      path: '/register',
      component: AuthLayout,
      meta: { guestOnly: true },
      children: [
        {
          path: '',
          name: 'register',
          component: RegisterView,
        },
      ],
    },
    {
      path: '/forgot-password',
      component: AuthLayout,
      meta: { guestOnly: true },
      children: [
        {
          path: '',
          name: 'forgot-password',
          component: ForgotPasswordView,
        },
      ],
    },
    {
      path: '/reset-password',
      component: AuthLayout,
      meta: { guestOnly: true },
      children: [
        {
          path: '',
          name: 'reset-password',
          component: ResetPasswordView,
        },
      ],
    },
    {
      path: '/public/notes/:shareCode',
      name: 'public-note',
      component: PublicNoteView,
    },
    {
      path: '/invite/:teamCode',
      name: 'team-invite',
      component: TeamInviteView,
    },
    {
      path: '/android/collab/notes/:noteId',
      name: 'android-collab-note',
      component: AndroidCollabNoteView,
      meta: { androidStandalone: true },
    },
    {
      path: '/admin/login',
      name: 'admin-login',
      component: AdminLoginView,
      meta: { adminGuestOnly: true },
    },
    {
      path: '/admin',
      component: AdminLayout,
      meta: { adminOnly: true },
      children: [
        {
          path: '',
          redirect: '/admin/dashboard',
        },
        {
          path: 'dashboard',
          name: 'admin-dashboard',
          component: AdminDashboardView,
          meta: { title: '数据大盘' },
        },
        {
          path: 'users',
          name: 'admin-users',
          component: AdminUserView,
          meta: { title: '用户管理' },
        },
        {
          path: 'sessions',
          name: 'admin-sessions',
          component: AdminSessionView,
          meta: { title: '会话管理' },
        },
        {
          path: 'settings',
          name: 'admin-settings',
          component: AdminSettingView,
          meta: { title: '系统配置' },
        },
        {
          path: 'logs',
          name: 'admin-logs',
          component: AdminLogView,
          meta: { title: '系统日志' },
        },
        {
          path: 'monitor',
          name: 'admin-monitor',
          component: AdminMonitorView,
          meta: { title: '性能监控' },
        },
        {
          path: 'storage',
          name: 'admin-storage',
          component: AdminStorageView,
          meta: { title: '存储管理' },
        },
        {
          path: 'system-notifications',
          name: 'admin-system-notifications',
          component: AdminSystemNotificationView,
          meta: { title: '系统通知' },
        },
      ],
    },
    {
      path: '/app',
      component: AppLayout,
      meta: { authOnly: true },
      children: [
        {
          path: '',
          redirect: '/app/notes',
        },
        {
          path: 'notes',
          name: 'notes',
          component: NoteView,
        },
        {
          path: 'notes/:noteId',
          name: 'note-detail',
          component: NoteView,
        },
        {
          path: 'tasks',
          name: 'tasks',
          component: TaskView,
        },
        {
          path: 'todos',
          name: 'todos',
          component: TodoView,
        },
        {
          path: 'files',
          name: 'files',
          component: FileManagementView,
        },
        {
          path: 'files/preview/:fileId',
          name: 'file-preview',
          component: FilePreviewView,
        },
        {
          path: 'stats',
          name: 'personal-stats',
          component: StatsView,
        },
        {
          path: 'spaces/:spaceId/stats',
          name: 'team-stats',
          component: TeamStatsView,
          meta: { teamOnly: true },
        },
        {
          path: 'notifications',
          name: 'notifications',
          component: NotificationView,
        },
        {
          path: 'settings',
          name: 'settings',
          component: SettingsView,
        },
        {
          path: 'space/:spaceId/settings',
          name: 'space-settings',
          component: SpaceSettingsView,
          meta: { teamOnly: true },
        },
        {
          path: 'projects',
          name: 'projects',
          component: ProjectListView,
          meta: { teamOnly: true },
        },
        {
          path: 'projects/:projectId',
          name: 'project-detail',
          component: ProjectDetailView,
          meta: { teamOnly: true },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: NotFoundView,
    },
  ],
})

router.beforeEach(async (to) => {
  const adminStore = useAdminStore()
  const userStore = useUserStore()
  const noteStore = useNoteStore()
  const spaceStore = useSpaceStore()
  const notificationStore = useNotificationStore()
  const projectStore = useProjectStore()
  const taskStore = useTaskStore()
  const todoStore = useTodoStore()

  if (to.meta.androidStandalone) {
    return true
  }

  if (to.meta.adminGuestOnly) {
    if (adminStore.isAuthenticated) {
      return '/admin/dashboard'
    }
    return true
  }

  if (to.meta.adminOnly) {
    if (!adminStore.isAuthenticated) {
      return {
        path: '/admin/login',
        query: {
          redirect: to.fullPath,
        },
      }
    }
    try {
      if (!adminStore.profile) {
        await adminStore.fetchProfile()
      }
    } catch {
      adminStore.clearSession()
      return {
        path: '/admin/login',
        query: {
          redirect: to.fullPath,
        },
      }
    }
    return true
  }

  if (to.meta.guestOnly) {
    return true
  }

  if (to.meta.authOnly && !userStore.isAuthenticated) {
    return {
      path: '/login',
      query: {
        redirect: normalizeAuthRedirect(to.fullPath),
      },
    }
  }

  if (userStore.isAuthenticated) {
    try {
      if (!userStore.profile) {
        await userStore.fetchProfile()
      }

      await spaceStore.ensureLoaded()

      if (typeof to.params.spaceId === 'string' && Number(to.params.spaceId) !== spaceStore.currentSpaceId) {
        const targetSpaceId = Number(to.params.spaceId)
        const exists = spaceStore.spaces.some((space) => space.id === targetSpaceId)
        if (exists) {
          await spaceStore.setCurrentSpace(targetSpaceId)
        }
      }

      notificationStore.fetchUnreadCount().catch(() => undefined)
    } catch (error) {
      userStore.clearSession()
      noteStore.reset()
      todoStore.reset()
      taskStore.reset()
      projectStore.reset()
      spaceStore.reset()
      notificationStore.reset()
      if (to.meta.authOnly) {
        return {
          path: '/login',
          query: {
            redirect: normalizeAuthRedirect(to.fullPath),
          },
        }
      }
    }

    if (to.meta.teamOnly && spaceStore.currentSpace?.type !== 'TEAM') {
      return '/app/notes'
    }
  }

  return true
})

export default router
