<template>
  <div class="space-y-6">
    <div class="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
      <div>
        <h1 class="font-display-serif text-5xl" :class="isTeam ? 'text-primary' : 'text-on-surface'">
          {{ isTeam ? t('task.titleTeam') : t('task.titlePersonal') }}
        </h1>
        <p class="mt-2 text-body-secondary text-on-surface-variant">
          {{ isTeam ? t('task.descriptionTeam') : t('task.descriptionPersonal') }}
        </p>
      </div>

      <div class="flex w-full flex-col gap-3 md:w-auto md:flex-row md:items-center">
        <label class="app-input flex h-11 w-full items-center gap-3 px-4 md:w-[300px]">
          <span class="material-symbols-outlined text-on-surface-variant">search</span>
          <input v-model="searchKeyword" class="w-full border-none bg-transparent p-0 outline-none focus:ring-0"
            :placeholder="t('task.searchPlaceholder')" />
        </label>

        <el-select v-if="isTeam" v-model="taskStore.query.projectId" clearable class="team-task-project-filter" :placeholder="t('task.projectFilter')"
          @change="loadTasks">
          <el-option v-for="project in projectStore.availableProjects" :key="project.id" :label="project.name"
            :value="project.id" />
        </el-select>

        <button class="app-primary-button h-11 shrink-0 px-5 text-sm" type="button" @click="openCreateDialog">
          <span class="material-symbols-outlined text-base">add</span>
          {{ t('task.newTask') }}
        </button>
      </div>
    </div>

    <TaskKanbanBoard :tasks="taskStore.tasks" :is-team="isTeam" :drag-enabled="true" :columns="kanbanColumns"
      :show-mode-chip="isTeam" :show-project-badge="true" @task-click="openTask" @task-drag-start="startTaskDrag"
      @task-drop="dropTask" />

    <el-dialog v-model="createDialogOpen" append-to-body destroy-on-close :title="t('task.createDialogTitle')" width="760px">
      <div class="grid gap-4">
        <label class="block">
          <span class="mb-2 block text-label-bold text-on-surface">{{ t('task.titleLabel') }}</span>
          <input v-model="form.title" class="app-input w-full px-4 py-3" :placeholder="t('task.titlePlaceholder')" />
        </label>

        <label class="block">
          <span class="mb-2 block text-label-bold text-on-surface">{{ t('task.descriptionLabel') }}</span>
          <textarea v-model="form.description" rows="4" class="app-input w-full resize-none px-4 py-3"
            :placeholder="t('task.descriptionPlaceholder')"></textarea>
        </label>

        <div v-if="isTeam" class="grid gap-4 md:grid-cols-2">
          <label class="block">
            <span class="mb-2 block text-label-bold text-on-surface">{{ t('task.modeLabel') }}</span>
            <el-select v-model="form.mode" class="w-full">
              <el-option :label="t('task.mode.open')" value="OPEN" />
              <el-option :label="t('task.mode.assigned')" value="ASSIGNED" />
            </el-select>
          </label>

          <label class="block">
            <span class="mb-2 block text-label-bold text-on-surface">{{ t('task.projectLabel') }}</span>
            <el-select v-model="form.projectId" clearable class="w-full" :placeholder="t('task.noProject')">
              <el-option v-for="project in projectStore.availableProjects" :key="project.id" :label="project.name"
                :value="project.id" />
            </el-select>
          </label>
        </div>

        <div class="grid gap-4 lg:grid-cols-3">
          <label class="block">
            <span class="mb-2 block text-label-bold text-on-surface">{{ t('task.priorityLabel') }}</span>
            <el-select v-model="form.priority" class="w-full">
              <el-option :label="t('task.priority.low')" value="LOW" />
              <el-option :label="t('task.priority.medium')" value="MEDIUM" />
              <el-option :label="t('task.priority.high')" value="HIGH" />
            </el-select>
          </label>

          <label class="block">
            <span class="mb-2 block text-label-bold text-on-surface">{{ t('task.deadlineLabel') }}</span>
            <el-date-picker v-model="form.deadline" class="w-full" clearable format="YYYY-MM-DD:HH:mm:ss"
              :placeholder="t('task.deadlinePlaceholder')" type="datetime" value-format="YYYY-MM-DD:HH:mm:ss" />
          </label>

          <label v-if="isTeam && form.mode === 'ASSIGNED'" class="block">
            <span class="mb-2 block text-label-bold text-on-surface">{{ t('task.assigneeLabel') }}</span>
            <el-select v-model="form.assigneeId" class="w-full" :placeholder="t('task.assigneePlaceholder')">
              <el-option v-for="member in spaceStore.members" :key="member.userId"
                :label="member.nickname || member.username" :value="member.userId" />
            </el-select>
          </label>
        </div>

        <div v-if="!isTeam"
          class="rounded-2xl border border-primary/10 bg-primary-fixed/20 px-4 py-3 text-sm text-on-surface-variant">
          {{ t('task.personalAutoAssignDetail') }}
        </div>

        <label v-if="isTeam && form.mode === 'ASSIGNED'" class="block">
          <span class="mb-2 block text-label-bold text-on-surface">{{ t('task.responsibilityLabel') }}</span>
          <input v-model="form.responsibility" class="app-input w-full px-4 py-3" :placeholder="t('task.responsibilityExample')" />
        </label>
      </div>

      <template #footer>
        <div class="flex w-full items-center justify-end gap-3 border-t border-outline-variant/20 pt-4">
          <button class="app-secondary-button" type="button" @click="createDialogOpen = false">{{ t('common.cancel') }}</button>
          <button class="app-primary-button" type="button" @click="submitCreate">{{ t('task.newTask') }}</button>
        </div>
      </template>
    </el-dialog>

    <el-drawer v-model="drawerOpen" append-to-body destroy-on-close size="760px" :title="t('task.details')">
      <div v-if="taskStore.currentTask && !taskStore.currentTaskLoading" class="space-y-6 pb-6">
        <div class="space-y-3">
          <div class="flex flex-wrap items-center gap-2">
            <span class="app-chip">{{ formatStatusLabel(taskStore.currentTask.status) }}</span>
            <span v-if="taskStore.currentTask.projectName" class="app-chip">{{ taskStore.currentTask.projectName
              }}</span>
          </div>
          <h2 class="font-display-serif text-4xl text-on-surface">{{ taskStore.currentTask.title }}</h2>
          <p class="text-body-main text-on-surface-variant">
            {{ taskStore.currentTask.description || t('task.noTaskDescription') }}
          </p>
        </div>

        <section v-if="pendingDropNotice" class="app-card border border-primary/20 bg-primary-fixed/20">
          <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
            <div>
              <h3 class="font-title-serif text-2xl text-on-surface">{{ t('task.dragConfirmTitle') }}</h3>
              <p class="mt-2 text-body-secondary text-on-surface-variant">
                {{ pendingDropNotice }}
              </p>
            </div>
            <div class="flex flex-wrap gap-2">
              <button v-if="pendingDropStatus === 'CANCELLED'" class="app-secondary-button border-error/30 text-error hover:border-error" type="button" @click="cancelCurrentTask">
                {{ t('task.cancelTask') }}
              </button>
              <button class="app-secondary-button" type="button" @click="clearPendingDrop">
                {{ t('common.cancel') }}
              </button>
            </div>
          </div>
        </section>

        <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <div class="app-card">
            <div class="text-caption uppercase tracking-[0.22em] text-on-surface-variant">{{ t('task.priorityLabel') }}</div>
            <div class="mt-3 font-title-serif text-2xl text-on-surface">{{
              formatPriorityLabel(taskStore.currentTask.priority) }}</div>
          </div>
          <div class="app-card">
            <div class="text-caption uppercase tracking-[0.22em] text-on-surface-variant">{{ t('task.modeLabel') }}</div>
            <div class="mt-3 font-title-serif text-2xl text-on-surface">{{ formatModeLabel(taskStore.currentTask.mode)
              }}
            </div>
          </div>
          <div class="app-card">
            <div class="text-caption uppercase tracking-[0.22em] text-on-surface-variant">{{ t('task.deadlineLabel') }}</div>
            <div class="mt-3 font-title-serif text-2xl text-on-surface">
              {{ taskStore.currentTask.deadline ? formatDateTime(taskStore.currentTask.deadline) : t('common.unset') }}
            </div>
          </div>
          <div class="app-card">
            <div class="text-caption uppercase tracking-[0.22em] text-on-surface-variant">{{ t('task.updatedAt') }}</div>
            <div class="mt-3 font-title-serif text-2xl text-on-surface">
              {{ taskStore.currentTask.gmtModified ? formatDateTime(taskStore.currentTask.gmtModified) : '--' }}
            </div>
          </div>
        </div>

        <section v-if="currentTaskMember || canClaimCurrentTask" class="app-card">
          <div class="mb-4 flex flex-wrap items-start justify-between gap-3">
            <div>
              <h3 class="font-title-serif text-2xl text-on-surface">{{ isTeam ? t('task.myAction') : t('task.myProgress') }}</h3>
              <p class="mt-2 text-body-secondary text-on-surface-variant">
                {{ canClaimCurrentTask ? t('task.claimFirst') : t('task.progressHelp') }}
              </p>
            </div>
            <span v-if="currentTaskMember" class="app-chip">{{ formatMemberStatusLabel(currentTaskMember.status)
              }}</span>
          </div>

          <div v-if="canClaimCurrentTask" class="space-y-4">
            <input v-model="claimResponsibility" class="app-input w-full px-4 py-3" :placeholder="t('task.claimPlaceholder')" />
            <button class="app-primary-button" type="button" @click="claimCurrentTask">{{ t('task.claimTask') }}</button>
          </div>

          <div v-else-if="currentTaskMember" class="space-y-5">
            <div class="rounded-2xl bg-surface-container-low px-4 py-3">
              <div class="text-caption uppercase tracking-[0.18em] text-on-surface-variant">{{ t('task.responsibilityLabel') }}</div>
              <div class="mt-2 text-body-main text-on-surface">{{ currentTaskMember.responsibility || t('task.noResponsibility') }}</div>
            </div>

            <div class="flex flex-wrap items-center justify-between gap-2">

              <div class="flex flex-wrap items-center gap-2">
                <button v-for="action in remarkToolbarActions" :key="action.id"
                  class="flex h-9 w-9 items-center justify-center rounded-full border border-outline-variant/30 text-on-surface-variant transition-all hover:border-primary hover:text-primary"
                  type="button" :title="action.label" @click="applyRemarkAction(action.id)">
                  <span class="material-symbols-outlined !text-[18px]">{{ action.icon }}</span>
                </button>
              </div>
              <div class="text-caption uppercase tracking-[0.22em] text-on-surface-variant">{{ t('task.remarkEditor') }}</div>
            </div>

            <!-- 左右等高 + 顶部对齐的区域 -->
            <div class="grid gap-5 lg:grid-cols-[minmax(0,1.08fr)_minmax(320px,0.92fr)]">
              <!-- 左侧：编辑区 -->
              <div class="min-w-0 space-y-3">

                <textarea ref="completionRemarkRef" v-model="completionRemark"
                  class="hide-scrollbar block w-full resize-none rounded-[1.5rem] border border-outline-variant/20 bg-surface-container-lowest px-4 py-4 font-mono text-sm leading-7 text-on-surface outline-none transition focus:border-primary focus:ring-0"
                  :placeholder="t('task.remarkPlaceholder')"></textarea>
              </div>

              <!-- 右侧：预览区 -->
              <div ref="completionPreviewRef"
                class="min-w-0 rounded-[1.5rem] border border-outline-variant/20 bg-surface-container-low/60 p-5">
                <div class="mb-3 text-caption uppercase tracking-[0.22em] text-on-surface-variant">{{ t('task.remarkPreview') }}</div>
                <div class="task-remark-preview" v-html="completionRemarkPreviewHtml"></div>
              </div>
            </div>

            <div class="space-y-3">
              <div class="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <div class="text-label-bold text-on-surface">{{ t('task.attachments') }}</div>
                  <div class="mt-1 text-body-secondary text-on-surface-variant">{{ t('task.attachmentsDescription') }}</div>
                </div>
                <div class="flex gap-2">
                  <input ref="attachmentInputRef" class="hidden" multiple type="file"
                    @change="handleAttachmentSelect" />
                  <button class="app-secondary-button" type="button" @click="triggerAttachmentPicker">{{ t('task.uploadAttachment') }}</button>
                </div>
              </div>

              <div v-if="taskStore.attachments.length" class="grid gap-3">
                <article v-for="attachment in taskStore.attachments" :key="attachment.id"
                  class="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-surface-container-low px-4 py-3">
                  <div class="min-w-0 flex-1">
                    <div class="truncate font-medium text-on-surface">{{ attachment.fileName }}</div>
                    <div class="mt-1 text-caption text-on-surface-variant">
                      {{ formatFileSize(attachment.fileSize) }} · {{ attachment.gmtCreate ?
                        formatDateTime(attachment.gmtCreate) : t('task.justUploaded') }}
                    </div>
                  </div>
                  <div class="flex gap-2">
                    <button class="app-secondary-button" type="button"
                      @click="taskStore.openAttachment(attachment.id)">{{ t('task.download') }}</button>
                    <button class="app-secondary-button" type="button"
                      @click="removeAttachment(attachment.id)">{{ t('common.remove') }}</button>
                  </div>
                </article>
              </div>
              <div v-else
                class="rounded-2xl border border-dashed border-outline-variant/30 px-4 py-5 text-sm text-on-surface-variant">
                {{ t('task.noAttachments') }}
              </div>
            </div>

            <div class="flex flex-wrap gap-3">
              <button v-if="canStartCurrentTask" class="app-primary-button" type="button"
                @click="startCurrentTask">{{ t('task.startTask') }}</button>
              <button v-if="canCompleteCurrentTask" class="app-primary-button" type="button"
                @click="completeCurrentTask">{{ t('task.completeTask') }}</button>
              <button v-if="canCancelCurrentTask" class="app-secondary-button border-error/30 text-error hover:border-error" type="button"
                @click="cancelCurrentTask">{{ t('task.cancelTask') }}</button>
              <span v-if="currentTaskMember.status === 'COMPLETED'"
                class="inline-flex items-center rounded-full bg-primary-fixed/30 px-4 py-2 text-sm font-semibold text-primary">
                {{ t('task.completeArchived') }}
              </span>
            </div>
          </div>
        </section>

        <section class="app-card">
          <div class="mb-4 text-label-bold text-on-surface">{{ t('task.assignees') }}</div>
          <div v-if="taskStore.currentTask.members?.length" class="space-y-3">
            <div v-for="member in taskStore.currentTask.members" :key="member.id"
              class="flex flex-wrap items-center justify-between gap-4 rounded-2xl bg-surface-container-low px-4 py-3">
              <div>
                <div class="font-medium text-on-surface">{{ member.username }}</div>
                <div class="mt-1 text-body-secondary text-on-surface-variant">{{ member.responsibility || t('task.noResponsibility') }}
                </div>
                <div v-if="member.completionRemark" class="mt-2 text-sm text-primary">{{ t('task.submittedRemark') }}</div>
              </div>
              <span class="app-chip">{{ formatMemberStatusLabel(member.status) }}</span>
            </div>
          </div>
          <div v-else
            class="rounded-2xl border border-dashed border-outline-variant/30 px-4 py-5 text-sm text-on-surface-variant">
            {{ t('task.membersEmpty') }}
          </div>
        </section>

        <section v-if="isTeam" class="app-card">
          <div class="mb-4 flex flex-wrap items-start justify-between gap-3">
            <div>
              <div class="text-label-bold text-on-surface">{{ t('task.teamComments') }}</div>
              <div class="mt-1 text-body-secondary text-on-surface-variant">{{ t('task.mentionHelp') }}</div>
            </div>
          </div>

          <div class="mb-4 space-y-3">
            <div v-for="comment in taskStore.comments" :key="comment.id"
              class="rounded-2xl bg-surface-container-low px-4 py-3">
              <div class="flex items-center justify-between gap-4">
                <div class="font-medium text-on-surface">{{ comment.username }}</div>
                <div class="text-caption text-on-surface-variant">{{ formatDateTime(comment.gmtCreate) }}</div>
              </div>
              <div class="mt-2 whitespace-pre-wrap text-body-secondary text-on-surface-variant">{{ comment.content }}
              </div>
            </div>

            <div v-if="!taskStore.comments.length"
              class="rounded-2xl border border-dashed border-outline-variant/30 px-4 py-5 text-sm text-on-surface-variant">
              {{ t('task.commentsEmpty') }}
            </div>
          </div>

          <div class="space-y-3">
            <el-mention v-model="commentContent" :options="mentionOptions" :rows="4" popper-class="task-mention-popper"
              resize="none" type="textarea" class="w-full" :placeholder="t('task.mentionPlaceholder')"
              @select="handleMentionSelect" />
            <div v-if="activeMentions.length" class="flex flex-wrap gap-2">
              <span v-for="mention in activeMentions" :key="mention.userId"
                class="rounded-full bg-primary-fixed/30 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.16em] text-primary">
                @{{ mention.value }}
              </span>
            </div>
            <button class="app-primary-button" type="button" @click="submitComment">{{ t('task.sendComment') }}</button>
          </div>
        </section>
      </div>

      <div v-else class="flex min-h-[320px] items-center justify-center">
        <EmptyState :title="t('task.loadingTitle')" :description="t('task.loadingDescription')" icon="task" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import type { Task, TaskMember, TaskPriority, TaskStatus } from '@/types/app'
import EmptyState from '@/components/common/EmptyState.vue'
import { useI18n } from '@/i18n'
import { useProjectStore } from '@/stores/project'
import { useSpaceStore } from '@/stores/space'
import { useTaskStore } from '@/stores/task'
import { useUserStore } from '@/stores/user'
import TaskKanbanBoard from '@/components/team/TaskKanbanBoard.vue'
import { formatDateTime } from '@/utils/date'
import { renderMarkdownLite } from '@/utils/markdown'
import { sanitizeHtml } from '@/utils/sanitize'

type RemarkToolbarAction = 'bold' | 'italic' | 'list' | 'quote' | 'code'

interface MentionItem {
  label: string
  userId: number
  value: string
}

const route = useRoute()
const taskStore = useTaskStore()
const spaceStore = useSpaceStore()
const projectStore = useProjectStore()
const userStore = useUserStore()
const { t } = useI18n()

const createDialogOpen = ref(false)
const drawerOpen = ref(false)
const searchKeyword = ref('')
const commentContent = ref('')
const claimResponsibility = ref('')
const completionRemark = ref('')
const dragTaskId = ref<number | null>(null)
const pendingDropTaskId = ref<number | null>(null)
const pendingDropStatus = ref<TaskStatus | null>(null)
const selectedMentionOptions = ref<MentionItem[]>([])
const handledRouteTaskId = ref<number | null>(null)

const attachmentInputRef = ref<HTMLInputElement | null>(null)
const completionRemarkRef = ref<HTMLTextAreaElement | null>(null)
const completionPreviewRef = ref<HTMLElement | null>(null)

let resizeObserver: ResizeObserver | null = null
let isSyncingHeight = false

const form = reactive({
  title: '',
  description: '',
  mode: 'OPEN' as 'OPEN' | 'ASSIGNED',
  priority: 'MEDIUM' as TaskPriority,
  deadline: '',
  projectId: undefined as number | undefined,
  assigneeId: undefined as number | undefined,
  responsibility: '',
})

const remarkToolbarActions = computed<Array<{ id: RemarkToolbarAction; icon: string; label: string }>>(() => [
  { id: 'bold', icon: 'format_bold', label: t('task.toolbar.bold') },
  { id: 'italic', icon: 'format_italic', label: t('task.toolbar.italic') },
  { id: 'list', icon: 'format_list_bulleted', label: t('task.toolbar.list') },
  { id: 'quote', icon: 'format_quote', label: t('task.toolbar.quote') },
  { id: 'code', icon: 'code', label: t('task.toolbar.code') },
])

const isTeam = computed(() => spaceStore.currentSpace?.type === 'TEAM')
const kanbanColumns = computed(() => [
  { status: 'PENDING' as TaskStatus, label: t('task.status.pending'), color: '#9ca3af' },
  { status: 'IN_PROGRESS' as TaskStatus, label: t('task.status.inProgress'), color: '#0077b6' },
  { status: 'COMPLETED' as TaskStatus, label: t('task.status.completed'), color: '#10b981' },
  { status: 'CANCELLED' as TaskStatus, label: t('task.status.cancelled'), color: '#ef4444' },
])
const currentTaskMember = computed<TaskMember | null>(() => {
  const userId = userStore.profile?.id
  if (!taskStore.currentTask || !userId) return null
  return taskStore.currentTask.members?.find((member) => member.userId === userId) || null
})
const canClaimCurrentTask = computed(
  () =>
    isTeam.value &&
    taskStore.currentTask?.mode === 'OPEN' &&
    !currentTaskMember.value &&
    taskStore.currentTask.status !== 'COMPLETED' &&
    taskStore.currentTask.status !== 'CANCELLED',
)
const canStartCurrentTask = computed(() => currentTaskMember.value?.status === 'PENDING')
const canCompleteCurrentTask = computed(() => currentTaskMember.value?.status === 'IN_PROGRESS')
const canCancelCurrentTask = computed(
  () =>
    Boolean(taskStore.currentTask) &&
    taskStore.currentTask?.status !== 'COMPLETED' &&
    taskStore.currentTask?.status !== 'CANCELLED' &&
    pendingDropStatus.value === 'CANCELLED',
)
const pendingDropNotice = computed(() => {
  if (!taskStore.currentTask || pendingDropTaskId.value !== taskStore.currentTask.id || !pendingDropStatus.value) {
    return ''
  }

  return t('task.dragConfirmDescription', { status: formatStatusLabel(pendingDropStatus.value) })
})
const completionRemarkPreviewHtml = computed(() => sanitizeHtml(renderMarkdownLite(completionRemark.value)))
const mentionOptions = computed<MentionItem[]>(() =>
  spaceStore.members.map((member) => ({
    label: `${member.nickname || member.username} · @${member.username}`,
    userId: member.userId,
    value: member.nickname || member.username,
  })),
)
const activeMentions = computed(() =>
  selectedMentionOptions.value.filter((item, index, list) => {
    const existsInText = commentContent.value.includes(`@${item.value}`)
    return existsInText && list.findIndex((target) => target.userId === item.userId) === index
  }),
)

let searchTimer: number | undefined

const resolveQueryNumber = (value: unknown) => {
  const resolved = Number(value)
  return Number.isFinite(resolved) && resolved > 0 ? resolved : undefined
}

const applyRouteProjectFilter = () => {
  if (!isTeam.value) {
    taskStore.query.projectId = undefined
    return
  }
  taskStore.query.projectId = resolveQueryNumber(route.query.projectId)
}

const maybeOpenTaskFromRoute = async () => {
  const openTaskId = resolveQueryNumber(route.query.openTaskId)
  if (!openTaskId) {
    handledRouteTaskId.value = null
    return
  }
  if (handledRouteTaskId.value === openTaskId && taskStore.currentTask?.id === openTaskId) return
  const task = taskStore.tasks.find((item) => item.id === openTaskId)
  if (!task) return
  handledRouteTaskId.value = openTaskId
  await openTask(task)
}

const formatModeLabel = (mode?: string) => {
  switch (mode) {
    case 'ASSIGNED': return t('task.mode.assigned')
    case 'OPEN': return t('task.mode.open')
    default: return t('task.mode.unset')
  }
}

const formatPriorityLabel = (priority?: string) => {
  switch (priority) {
    case 'LOW': return t('task.priority.low')
    case 'HIGH': return t('task.priority.high')
    default: return t('task.priority.medium')
  }
}

const formatStatusLabel = (status?: string) => {
  switch (status) {
    case 'PENDING': return t('task.status.pending')
    case 'IN_PROGRESS': return t('task.status.inProgress')
    case 'COMPLETED': return t('task.status.completed')
    case 'CANCELLED': return t('task.status.cancelled')
    default: return t('task.status.unknown')
  }
}

const formatMemberStatusLabel = (status?: string) => {
  switch (status) {
    case 'PENDING': return t('task.memberStatus.pending')
    case 'IN_PROGRESS': return t('task.memberStatus.inProgress')
    case 'COMPLETED': return t('task.memberStatus.completed')
    default: return t('task.memberStatus.unknown')
  }
}

const formatFileSize = (size?: number) => {
  if (!size) return '0 B'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / (1024 * 1024)).toFixed(1)} MB`
}

const resetForm = () => {
  form.title = ''
  form.description = ''
  form.mode = 'OPEN'
  form.priority = 'MEDIUM'
  form.deadline = ''
  form.projectId = undefined
  form.assigneeId = undefined
  form.responsibility = ''
}

const loadTasks = async () => {
  taskStore.query.keyword = searchKeyword.value.trim()
  taskStore.query.pageNum = 1
  if (!isTeam.value) taskStore.query.projectId = undefined
  await taskStore.loadTasks()

  if (isTeam.value) {
    const asyncTasks: Array<Promise<unknown>> = []
    if (!projectStore.availableProjects.length) asyncTasks.push(projectStore.loadProjectOptions())
    if (!spaceStore.members.length) asyncTasks.push(spaceStore.loadMembers())
    if (asyncTasks.length) await Promise.all(asyncTasks)
  }
  await maybeOpenTaskFromRoute()
}

const openCreateDialog = () => {
  resetForm()
  createDialogOpen.value = true
}

const submitCreate = async () => {
  const title = form.title.trim()
  if (!title) {
    ElMessage.warning(t('messages.taskTitleRequired'))
    return
  }
  if (!isTeam.value && !userStore.profile?.id) {
    ElMessage.warning(t('messages.accountNotReady'))
    return
  }
  if (isTeam.value && form.mode === 'ASSIGNED' && !form.assigneeId) {
    ElMessage.warning(t('messages.assigneeRequired'))
    return
  }
  if (isTeam.value && form.mode === 'ASSIGNED' && !form.responsibility.trim()) {
    ElMessage.warning(t('messages.responsibilityRequired'))
    return
  }

  const payload = {
    title,
    description: form.description.trim() || undefined,
    mode: isTeam.value ? form.mode : 'ASSIGNED',
    priority: form.priority,
    deadline: form.deadline || undefined,
    projectId: isTeam.value ? form.projectId : undefined,
    assignments: isTeam.value
      ? form.mode === 'ASSIGNED'
        ? [{ userId: form.assigneeId as number, responsibility: form.responsibility.trim(), isRequired: true }]
        : []
      : [{ userId: userStore.profile!.id, responsibility: form.description.trim() || t('task.personalTaskResponsibility'), isRequired: true }],
  }

  const created = await taskStore.createTask(payload)
  createDialogOpen.value = false
  resetForm()
  if (created) await openTask(created)
}

const startTaskDrag = (taskId: number) => {
  dragTaskId.value = taskId
}

const clearPendingDrop = () => {
  pendingDropTaskId.value = null
  pendingDropStatus.value = null
}

const dropTask = async (status: TaskStatus) => {
  if (!dragTaskId.value) return
  const task = taskStore.tasks.find((item) => item.id === dragTaskId.value)
  dragTaskId.value = null
  if (!task || task.status === status) return

  pendingDropTaskId.value = task.id
  pendingDropStatus.value = status
  await openTask(task, status)
  ElMessage.info(t('messages.taskDragOpenDetail'))
}

const syncTaskDetails = async (taskId: number) => {
  const requests: Array<Promise<unknown>> = [taskStore.loadTask(taskId), taskStore.loadTaskAttachments(taskId)]
  if (isTeam.value) {
    requests.push(taskStore.loadComments(taskId))
    if (!spaceStore.members.length) requests.push(spaceStore.loadMembers())
  }
  await Promise.all(requests)
}

const openTask = async (task: Task, requestedStatus?: TaskStatus) => {
  await syncTaskDetails(task.id)
  if (requestedStatus) {
    pendingDropTaskId.value = task.id
    pendingDropStatus.value = requestedStatus
  } else {
    clearPendingDrop()
  }
  claimResponsibility.value = ''
  commentContent.value = ''
  selectedMentionOptions.value = []
  completionRemark.value = currentTaskMember.value?.completionRemark || ''
  drawerOpen.value = true
}

const claimCurrentTask = async () => {
  if (!taskStore.currentTask) return
  const responsibility = claimResponsibility.value.trim()
  if (!responsibility) {
    ElMessage.warning(t('messages.claimResponsibilityRequired'))
    return
  }
  await taskStore.claimTask(taskStore.currentTask.id, responsibility, true)
  await syncTaskDetails(taskStore.currentTask.id)
  claimResponsibility.value = ''
  completionRemark.value = currentTaskMember.value?.completionRemark || ''
}

const startCurrentTask = async () => {
  if (!taskStore.currentTask || !currentTaskMember.value) return
  await taskStore.startTaskMember(taskStore.currentTask.id, currentTaskMember.value.id)
  await syncTaskDetails(taskStore.currentTask.id)
  if (pendingDropStatus.value === 'IN_PROGRESS') {
    clearPendingDrop()
  }
}

const completeCurrentTask = async () => {
  if (!taskStore.currentTask || !currentTaskMember.value) return
  await taskStore.completeTaskMember(
    taskStore.currentTask.id,
    currentTaskMember.value.id,
    completionRemark.value.trim() || undefined,
  )
  await syncTaskDetails(taskStore.currentTask.id)
  completionRemark.value = currentTaskMember.value?.completionRemark || completionRemark.value
  if (pendingDropStatus.value === 'COMPLETED') {
    clearPendingDrop()
  }
}

const cancelCurrentTask = async () => {
  if (!taskStore.currentTask) return
  await taskStore.updateTaskStatus(taskStore.currentTask.id, 'CANCELLED')
  await syncTaskDetails(taskStore.currentTask.id)
  clearPendingDrop()
}

const triggerAttachmentPicker = () => {
  attachmentInputRef.value?.click()
}

const handleAttachmentSelect = async (event: Event) => {
  if (!taskStore.currentTask) return
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  if (!files.length) return
  for (const file of files) {
    await taskStore.uploadTaskAttachment(taskStore.currentTask.id, file)
  }
  input.value = ''
}

const removeAttachment = async (attachmentId: number) => {
  if (!taskStore.currentTask) return
  await taskStore.unbindTaskAttachment(taskStore.currentTask.id, attachmentId)
}

const applyRemarkAction = (action: RemarkToolbarAction) => {
  const editor = completionRemarkRef.value
  if (!editor) return

  const start = editor.selectionStart ?? completionRemark.value.length
  const end = editor.selectionEnd ?? completionRemark.value.length
  const selected = completionRemark.value.slice(start, end)
  const prefix = completionRemark.value.slice(0, start)
  const suffix = completionRemark.value.slice(end)

  let nextValue = ''
  let selectionStart = start
  let selectionEnd = end

  switch (action) {
    case 'bold': {
      const content = selected || t('task.toolbar.boldPlaceholder')
      nextValue = `${prefix}**${content}**${suffix}`
      selectionStart = start + 2
      selectionEnd = selectionStart + content.length
      break
    }
    case 'italic': {
      const content = selected || t('task.toolbar.italicPlaceholder')
      nextValue = `${prefix}*${content}*${suffix}`
      selectionStart = start + 1
      selectionEnd = selectionStart + content.length
      break
    }
    case 'list': {
      const content = selected || t('task.toolbar.listPlaceholder')
      const lines = content.split(/\r?\n/).map((line) => `- ${line}`).join('\n')
      nextValue = `${prefix}${lines}${suffix}`
      selectionStart = start + 2
      selectionEnd = start + lines.length
      break
    }
    case 'quote': {
      const content = selected || t('task.toolbar.quotePlaceholder')
      nextValue = `${prefix}> ${content}${suffix}`
      selectionStart = start + 2
      selectionEnd = selectionStart + content.length
      break
    }
    case 'code':
    default: {
      const content = selected || t('task.toolbar.codePlaceholder')
      nextValue = `${prefix}\`${content}\`${suffix}`
      selectionStart = start + 1
      selectionEnd = selectionStart + content.length
      break
    }
  }

  completionRemark.value = nextValue
  nextTick(() => {
    editor.focus()
    editor.setSelectionRange(selectionStart, selectionEnd)
  }).catch(() => undefined)
}

const handleMentionSelect = (option: { label?: string; userId?: number; value?: string }) => {
  if (!option.userId || !option.value) return
  const normalizedOption: MentionItem = {
    label: option.label || option.value,
    userId: option.userId,
    value: option.value,
  }
  if (!selectedMentionOptions.value.some((item) => item.userId === normalizedOption.userId)) {
    selectedMentionOptions.value = [...selectedMentionOptions.value, normalizedOption]
  }
}

// 同步输入框与预览区高度，保证编辑和预览对齐。
const syncRemarkPaneHeight = async () => {
  if (isSyncingHeight) return
  const editor = completionRemarkRef.value
  const preview = completionPreviewRef.value
  if (!editor || !preview) return

  isSyncingHeight = true
  const originalEditorHeight = editor.style.height
  const originalPreviewHeight = preview.style.height
  editor.style.height = 'auto'
  preview.style.height = 'auto'

  const editorScrollHeight = editor.scrollHeight
  const previewScrollHeight = preview.scrollHeight
  const minHeight = 320
  const targetHeight = Math.max(editorScrollHeight, previewScrollHeight, minHeight)

  editor.style.height = `${targetHeight}px`
  preview.style.height = `${targetHeight}px`

  if (originalEditorHeight === editor.style.height && originalPreviewHeight === preview.style.height) {
    isSyncingHeight = false
  } else {
    await nextTick()
    isSyncingHeight = false
  }
}

const submitComment = async () => {
  if (!taskStore.currentTask || !commentContent.value.trim()) return
  await taskStore.addComment(
    taskStore.currentTask.id,
    commentContent.value.trim(),
    activeMentions.value.map((item) => item.userId),
  )
  commentContent.value = ''
  selectedMentionOptions.value = []
}

const handleWindowResize = () => {
  syncRemarkPaneHeight()
}

onMounted(() => {
  searchKeyword.value = taskStore.query.keyword || ''
  applyRouteProjectFilter()
  loadTasks().catch(() => undefined)

  // 监听两个区域的高度变化
  resizeObserver = new ResizeObserver(() => {
    syncRemarkPaneHeight()
  })
  if (completionRemarkRef.value) resizeObserver.observe(completionRemarkRef.value)
  if (completionPreviewRef.value) resizeObserver.observe(completionPreviewRef.value)

  window.addEventListener('resize', handleWindowResize)
  // 初次同步
  syncRemarkPaneHeight()
})

onBeforeUnmount(() => {
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  window.removeEventListener('resize', handleWindowResize)
})

watch(
  () => spaceStore.currentSpaceId,
  () => {
    drawerOpen.value = false
    commentContent.value = ''
    claimResponsibility.value = ''
    completionRemark.value = ''
    selectedMentionOptions.value = []
    loadTasks().catch(() => undefined)
  },
)

watch(
  () => [route.query.projectId, route.query.openTaskId],
  () => {
    applyRouteProjectFilter()
    loadTasks().catch(() => undefined)
  },
)

watch(searchKeyword, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = window.setTimeout(() => {
    loadTasks().catch(() => undefined)
  }, 250)
})

// 内容变化时同步高度
watch(completionRemark, () => {
  nextTick(() => {
    syncRemarkPaneHeight()
  })
})

watch(
  () => taskStore.currentTask?.id,
  () => {
    completionRemark.value = currentTaskMember.value?.completionRemark || ''
    nextTick(() => {
      syncRemarkPaneHeight()
    })
  },
)

// 当预览区域的 DOM 元素挂载时重新观察
watch(drawerOpen, (open) => {
  if (!open) {
    clearPendingDrop()
  }
})

watch(completionPreviewRef, (element, previousElement) => {
  if (previousElement && resizeObserver) resizeObserver.unobserve(previousElement)
  if (element && resizeObserver) resizeObserver.observe(element)
  syncRemarkPaneHeight()
})
</script>

<style scoped>
.team-task-project-filter {
  width: 240px;
}

:deep(.team-task-project-filter .el-select__wrapper) {
  min-height: 44px;
  border-radius: 999px;
  background: var(--surface-container-lowest);
  box-shadow: 0 0 0 1px rgba(221, 192, 184, 0.45);
}

.task-remark-preview :deep(.markdown-empty) {
  color: var(--on-surface-variant);
  opacity: 0.75;
}

.task-remark-preview :deep(p),
.task-remark-preview :deep(li),
.task-remark-preview :deep(blockquote) {
  color: var(--on-surface-variant);
  line-height: 1.8;
}

.task-remark-preview :deep(p) {
  margin-bottom: 1rem;
}

.task-remark-preview :deep(ul),
.task-remark-preview :deep(ol) {
  margin: 0 0 1rem;
  padding-left: 1.25rem;
}

.task-remark-preview :deep(blockquote) {
  border-left: 3px solid var(--primary);
  padding-left: 1rem;
}

.task-remark-preview :deep(code) {
  border-radius: 0.375rem;
  background: rgba(233, 225, 222, 0.6);
  padding: 0.15rem 0.4rem;
  color: var(--primary);
}
</style>
