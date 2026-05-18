<template>
  <div v-if="projectStore.currentProject" class="space-y-6">
    <section class="app-shell team-main-shell">
      <div class="flex flex-col gap-5 md:flex-row md:items-end md:justify-between">
        <div>
          <div class="mb-2 inline-flex items-center gap-2 rounded-full bg-primary-fixed px-4 py-2 text-caption uppercase tracking-[0.22em] text-primary">
            {{ t('projectDetail.workspace') }}
          </div>
          <h1 class="font-display-serif text-5xl text-on-surface">{{ projectStore.currentProject.name }}</h1>
          <p class="mt-3 max-w-3xl text-body-main text-on-surface-variant">
            {{ projectStore.currentProject.description || t('projectDetail.defaultDescription') }}
          </p>
        </div>
        <div class="flex flex-wrap gap-3">
          <button class="app-secondary-button" type="button" @click="settingsDrawerOpen = true">{{ t('projectDetail.settings') }}</button>
          <button class="app-secondary-button" type="button" @click="toggleArchive">
            {{ projectStore.currentProject.archived ? t('projectDetail.restoreProject') : t('projectDetail.archiveProject') }}
          </button>
          <button class="app-primary-button" type="button" @click="router.push('/app/projects')">{{ t('projectDetail.backToProjects') }}</button>
        </div>
      </div>
    </section>

    <div class="flex flex-wrap gap-2">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        class="rounded-full px-4 py-2 text-label-bold"
        :class="activeTab === tab.value ? 'bg-primary text-white' : 'bg-surface text-on-surface hover:bg-surface-container-low'"
        type="button"
        @click="activeTab = tab.value"
      >
        {{ tab.label }}
      </button>
    </div>

    <section v-if="activeTab === 'overview'" class="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
      <div class="space-y-5">
        <div class="grid gap-5 md:grid-cols-2 xl:grid-cols-4">
          <div class="app-card team-shell">
            <div class="text-caption uppercase tracking-[0.22em] text-primary/60">{{ t('projectDetail.totalTasks') }}</div>
            <div class="mt-4 font-display-serif text-5xl text-primary">{{ projectStore.currentProject.taskCount ?? 0 }}</div>
          </div>
          <div class="app-card team-shell">
            <div class="text-caption uppercase tracking-[0.22em] text-primary/60">{{ t('projectDetail.completed') }}</div>
            <div class="mt-4 font-display-serif text-5xl text-primary">{{ projectStore.currentProject.completedTaskCount ?? 0 }}</div>
          </div>
          <div class="app-card team-shell">
            <div class="text-caption uppercase tracking-[0.22em] text-primary/60">{{ t('projectDetail.documents') }}</div>
            <div class="mt-4 font-display-serif text-5xl text-primary">{{ projectStore.currentProject.documentCount ?? 0 }}</div>
          </div>
          <div class="app-card team-shell">
            <div class="text-caption uppercase tracking-[0.22em] text-primary/60">{{ t('projectDetail.members') }}</div>
            <div class="mt-4 font-display-serif text-5xl text-primary">{{ projectStore.members.length }}</div>
          </div>
        </div>

        <div class="app-shell">
          <div class="mb-5 flex items-center justify-between">
            <div>
              <h2 class="font-title-serif text-3xl text-on-surface">{{ t('projectDetail.recentActivity') }}</h2>
              <p class="mt-2 text-body-secondary text-on-surface-variant">
                {{ t('projectDetail.recentActivityDescription') }}
              </p>
            </div>
          </div>

          <div v-if="recentActivities.length" class="space-y-4">
            <article
              v-for="activity in recentActivities"
              :key="activity.id"
              class="flex items-start gap-4 rounded-[1.5rem] bg-surface-container-low/70 px-5 py-4"
            >
              <div class="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-primary-fixed text-primary">
                <span class="material-symbols-outlined">
                  {{ activity.type === 'task' ? 'task_alt' : 'description' }}
                </span>
              </div>
              <div class="min-w-0 flex-1">
                <div class="flex flex-wrap items-center justify-between gap-3">
                  <div class="font-medium text-on-surface">{{ activity.title }}</div>
                  <div class="text-caption text-on-surface-variant">{{ formatDateTime(activity.time) }}</div>
                </div>
                <div class="mt-2 text-body-secondary text-on-surface-variant">{{ activity.description }}</div>
              </div>
            </article>
          </div>
          <div v-else class="rounded-[1.5rem] border border-dashed border-outline-variant/30 px-5 py-8 text-sm text-on-surface-variant">
            {{ t('projectDetail.noRecentActivity') }}
          </div>
        </div>
      </div>

      <div class="space-y-5">
        <div class="app-shell overflow-hidden">
          <div class="mb-4 flex items-center justify-between">
            <div>
              <h2 class="font-title-serif text-3xl text-on-surface">{{ t('projectDetail.progress') }}</h2>
              <p class="mt-2 text-body-secondary text-on-surface-variant">
                {{ t('projectDetail.progressDescription') }}
              </p>
            </div>
          </div>

          <div class="flex flex-col items-center gap-6 py-6">
            <div class="flex h-52 w-52 items-center justify-center rounded-full" :style="completionRingStyle">
              <div class="flex h-36 w-36 flex-col items-center justify-center rounded-full bg-surface">
                <div class="font-display-serif text-4xl text-primary">{{ completionRate }}%</div>
                <div class="mt-2 text-caption uppercase tracking-[0.22em] text-on-surface-variant">{{ t('projectDetail.completed') }}</div>
              </div>
            </div>

            <div class="grid w-full gap-4 md:grid-cols-2">
              <div class="rounded-[1.4rem] bg-surface-container-low px-4 py-4">
                <div class="text-caption uppercase tracking-[0.2em] text-on-surface-variant">{{ t('projectDetail.owner') }}</div>
                <div class="mt-2 text-lg font-medium text-on-surface">{{ ownerDisplayName }}</div>
              </div>
              <div class="rounded-[1.4rem] bg-surface-container-low px-4 py-4">
                <div class="text-caption uppercase tracking-[0.2em] text-on-surface-variant">{{ t('projectDetail.archiveStatus') }}</div>
                <div class="mt-2 text-lg font-medium text-on-surface">{{ projectStore.currentProject.archived ? t('common.archived') : t('common.active') }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section v-else-if="activeTab === 'tasks'" class="app-shell space-y-6">
      <div class="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <h2 class="font-title-serif text-3xl text-on-surface">{{ t('projectDetail.projectTasks') }}</h2>
          <p class="mt-2 text-body-secondary text-on-surface-variant">
            {{ t('projectDetail.projectTasksDescription') }}
          </p>
        </div>

        <div class="flex flex-wrap items-center gap-3 text-sm text-on-surface-variant">
          <span class="app-chip">{{ projectStore.currentProject?.name }}</span>
          <span>{{ t('projectDetail.taskCount', { count: projectStore.relatedTasks.list.length }) }}</span>
          <button class="app-primary-button !px-5 !py-2.5 text-sm" type="button" @click="openTaskCreateDialog">
            <span class="material-symbols-outlined text-base">add</span>
            {{ t('task.newTask') }}
          </button>
        </div>
      </div>

      <TaskKanbanBoard
        :tasks="projectTasksForKanban"
        :is-team="true"
        :drag-enabled="false"
        :show-mode-chip="false"
        :show-project-badge="false"
        :columns="projectTaskColumns"
        :empty-title="t('projectDetail.noTasksTitle')"
        :empty-description="t('projectDetail.noTasksDescription')"
        @task-click="openProjectTaskBoard"
      />
    </section>

    <section v-else-if="activeTab === 'docs'" class="app-shell">
      <div class="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <h2 class="font-title-serif text-3xl text-on-surface">{{ t('projectDetail.projectDocs') }}</h2>
          <p class="mt-2 text-body-secondary text-on-surface-variant">
            {{ t('projectDetail.projectDocsDescription') }}
          </p>
        </div>

        <div class="flex flex-wrap gap-3">
          <button class="app-secondary-button" type="button" @click="openAssociateNotesDialog">{{ t('projectDetail.associateDocs') }}</button>
          <button class="app-primary-button" type="button" @click="createProjectDoc">{{ t('projectDetail.newProjectDoc') }}</button>
        </div>
      </div>

      <div v-if="projectStore.relatedNotes.list.length" class="mt-6 divide-y divide-outline-variant/30 rounded-[1.5rem] bg-surface-container-low/60">
        <article v-for="note in projectStore.relatedNotes.list" :key="note.id" class="px-5 py-5 first:pt-5 last:pb-5">
          <div class="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
            <div class="min-w-0 flex-1">
              <div class="font-title-serif text-2xl text-on-surface">{{ note.title }}</div>
              <div class="mt-2 line-clamp-2 text-body-secondary text-on-surface-variant">
                {{ note.content || t('projectDetail.emptyDocContent') }}
              </div>
              <div class="mt-3 text-caption text-on-surface-variant">
                {{ t('projectDetail.recentlyUpdated', { time: formatDateTime(note.gmtModified || note.gmtCreate) }) }}
              </div>
              <div class="mt-1 text-caption text-on-surface-variant">
                {{ t('projectDetail.lastEditor', { name: resolveMemberName(note.userId) }) }}
              </div>
            </div>
            <div class="flex flex-wrap gap-2">
              <button class="app-secondary-button" type="button" @click="router.push(`/app/notes/${note.id}`)">{{ t('projectDetail.openDoc') }}</button>
              <button class="app-secondary-button" type="button" @click="unbindNote(note)">{{ t('projectDetail.unlink') }}</button>
            </div>
          </div>
        </article>
      </div>
      <div v-else class="mt-6 rounded-[1.5rem] border border-dashed border-outline-variant/30 px-5 py-10 text-sm text-on-surface-variant">
        {{ t('projectDetail.noDocs') }}
      </div>
    </section>

    <section v-else class="grid gap-6 xl:grid-cols-[1fr_320px]">
      <div class="app-shell">
        <div class="mb-5 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <div>
            <h2 class="font-title-serif text-3xl text-on-surface">{{ t('projectDetail.projectMembers') }}</h2>
            <p class="mt-2 text-body-secondary text-on-surface-variant">
              {{ t('projectDetail.projectMembersDescription') }}
            </p>
          </div>

          <div class="flex flex-wrap gap-3">
            <el-select v-model="memberForm.userId" clearable class="w-[280px]" :placeholder="t('projectDetail.memberPlaceholder')">
              <el-option
                v-for="member in availableMembers"
                :key="member.userId"
                :label="member.nickname || member.username"
                :value="member.userId"
              />
            </el-select>
            <el-select v-model="memberForm.role" class="min-w-[160px]">
              <el-option :label="t('projectDetail.roleOwner')" value="OWNER" />
              <el-option :label="t('projectDetail.roleMember')" value="MEMBER" />
            </el-select>
            <button class="app-primary-button" type="button" @click="addMember">{{ t('projectDetail.addMember') }}</button>
          </div>
        </div>

        <div v-if="projectStore.members.length" class="space-y-4">
          <article
            v-for="member in projectStore.members"
            :key="member.userId"
            class="rounded-[1.5rem] bg-surface-container-low/60 px-5 py-5"
          >
            <div class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
              <div class="flex min-w-0 items-center gap-4">
                <div class="flex h-12 w-12 shrink-0 items-center justify-center overflow-hidden rounded-full bg-primary-fixed text-primary">
                  <img v-if="member.avatarUrl" :src="member.avatarUrl" :alt="member.nickname || member.username" class="h-full w-full object-cover" />
                  <span v-else class="text-sm font-semibold">
                    {{ (member.nickname || member.username || '?').slice(0, 1).toUpperCase() }}
                  </span>
                </div>
                <div class="min-w-0">
                  <div class="truncate text-lg font-medium text-on-surface">{{ member.nickname || member.username }}</div>
                  <div class="mt-1 text-body-secondary text-on-surface-variant">
                    {{ member.email || t('members.noEmail') }}
                  </div>
                  <div class="mt-1 text-caption text-on-surface-variant">
                    {{ t('members.joinedAtInline', { time: member.joinedAt ? formatDateTime(member.joinedAt) : t('common.notRecorded') }) }}
                  </div>
                </div>
              </div>

              <div class="flex flex-wrap items-center gap-3">
                <el-select
                  :model-value="member.role"
                  class="w-[150px] project-member-role-readonly"
                  disabled
                >
                  <el-option :label="t('projectDetail.roleOwner')" value="OWNER" />
                  <el-option :label="t('projectDetail.roleMember')" value="MEMBER" />
                </el-select>
                <button class="app-secondary-button" type="button" @click="removeMember(member.userId)">{{ t('projectDetail.removeMember') }}</button>
              </div>
            </div>
          </article>
        </div>
        <div v-else class="rounded-[1.5rem] border border-dashed border-outline-variant/30 px-5 py-8 text-sm text-on-surface-variant">
          {{ t('projectDetail.noMembers') }}
        </div>
      </div>

      <aside class="app-shell h-fit">
        <h3 class="font-title-serif text-2xl text-on-surface">{{ t('projectDetail.memberGuide') }}</h3>
        <div class="mt-4 space-y-3 text-body-secondary text-on-surface-variant">
          <p>{{ t('projectDetail.memberGuideOwner') }}</p>
          <p>{{ t('projectDetail.memberGuideMember') }}</p>
          <p>{{ t('projectDetail.memberGuideCaution') }}</p>
        </div>
      </aside>
    </section>

    <ProjectTaskCreateDialog
      v-model="taskCreateDialogOpen"
      :assignable-members="assignableMembers"
      :form="taskCreateForm"
      @submit="submitProjectTask"
    />

    <ProjectAssociateNotesDialog
      v-model="associateNotesDialogOpen"
      v-model:selected-note-ids="selectedNoteIds"
      :notes="selectableNotes"
      @confirm="associateSelectedNotes"
    />

    <el-drawer v-model="settingsDrawerOpen" append-to-body size="460px" :title="t('projectDetail.settings')">
      <div class="space-y-5">
        <label class="block">
          <span class="mb-2 block text-label-bold text-on-surface">{{ t('projectDetail.projectName') }}</span>
          <input v-model="settingsForm.name" class="app-input w-full px-4 py-3" />
        </label>

        <label class="block">
          <span class="mb-2 block text-label-bold text-on-surface">{{ t('projectDetail.projectDescription') }}</span>
          <textarea v-model="settingsForm.description" rows="5" class="app-input w-full resize-none px-4 py-3"></textarea>
        </label>

        <label class="block">
          <span class="mb-2 block text-label-bold text-on-surface">{{ t('projectDetail.owner') }}</span>
          <el-select v-model="settingsForm.ownerUserId" clearable class="w-full" :placeholder="t('projectDetail.newOwnerPlaceholder')">
            <el-option
              v-for="member in spaceStore.members"
              :key="member.userId"
              :label="member.nickname || member.username"
              :value="member.userId"
            />
          </el-select>
        </label>

        <label class="block">
          <span class="mb-2 block text-label-bold text-on-surface">{{ t('projectDetail.coverColor') }}</span>
          <ProjectColorPicker v-model="settingsForm.coverColor" />
        </label>

        <label class="block">
          <span class="mb-2 block text-label-bold text-on-surface">{{ t('projectDetail.coverImage') }}</span>
          <input v-model="settingsForm.coverImageUrl" class="app-input w-full px-4 py-3" :placeholder="t('projectDetail.coverImagePlaceholder')" />
        </label>

        <div class="flex flex-wrap gap-3">
          <button class="app-primary-button" type="button" @click="saveProjectSettings">{{ t('common.saveChanges') }}</button>
          <button class="app-secondary-button" type="button" @click="copyProjectLink">{{ t('projectDetail.copyProjectLink') }}</button>
        </div>

        <div class="rounded-[1.5rem] border border-error/20 bg-error-container/40 p-5">
          <div class="mb-4">
            <h3 class="font-title-serif text-2xl text-on-surface">{{ t('projectDetail.deleteProject') }}</h3>
            <p class="mt-2 text-body-secondary text-on-surface-variant">
              {{ t('projectDetail.deleteProjectDescription') }}
            </p>
          </div>

          <div class="space-y-4">
            <label class="block">
              <span class="mb-2 block text-label-bold text-on-surface">{{ t('projectDetail.taskStrategy') }}</span>
              <el-select v-model="deleteOptions.taskStrategy" class="w-full">
                <el-option :label="t('projectDetail.transferToOtherProject')" value="transfer" />
                <el-option :label="t('projectDetail.unlinkProject')" value="unlink" />
                <el-option :label="t('projectDetail.deleteTasksDirectly')" value="delete" />
              </el-select>
            </label>

            <label v-if="deleteOptions.taskStrategy === 'transfer'" class="block">
              <span class="mb-2 block text-label-bold text-on-surface">{{ t('projectDetail.taskTargetProject') }}</span>
              <el-select v-model="deleteOptions.taskTargetProjectId" class="w-full" :placeholder="t('projectDetail.taskTargetPlaceholder')">
                <el-option
                  v-for="project in moveTargetProjects"
                  :key="project.id"
                  :label="project.name"
                  :value="project.id"
                />
              </el-select>
            </label>

            <label class="block">
              <span class="mb-2 block text-label-bold text-on-surface">{{ t('projectDetail.docStrategy') }}</span>
              <el-select v-model="deleteOptions.docStrategy" class="w-full">
                <el-option :label="t('projectDetail.transferToOtherProject')" value="transfer" />
                <el-option :label="t('projectDetail.unlinkProject')" value="unlink" />
                <el-option :label="t('projectDetail.deleteDocsDirectly')" value="delete" />
              </el-select>
            </label>

            <label v-if="deleteOptions.docStrategy === 'transfer'" class="block">
              <span class="mb-2 block text-label-bold text-on-surface">{{ t('projectDetail.docTargetProject') }}</span>
              <el-select v-model="deleteOptions.docTargetProjectId" class="w-full" :placeholder="t('projectDetail.docTargetPlaceholder')">
                <el-option
                  v-for="project in moveTargetProjects"
                  :key="project.id"
                  :label="project.name"
                  :value="project.id"
                />
              </el-select>
            </label>

            <div class="rounded-[1.25rem] bg-white/70 px-4 py-3 text-sm text-on-surface-variant">
              {{ t('projectDetail.deleteSummary', { tasks: projectStore.currentProject?.taskCount ?? 0, docs: projectStore.currentProject?.documentCount ?? 0 }) }}
            </div>

            <button
              class="inline-flex items-center justify-center rounded-full bg-error px-5 py-3 text-sm font-semibold text-white transition hover:brightness-95 disabled:cursor-not-allowed disabled:opacity-60"
              type="button"
              :disabled="deletingProject"
              @click="deleteProjectWithRelations"
            >
              {{ deletingProject ? t('projectDetail.deletingProject') : t('projectDetail.deleteProject') }}
            </button>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>

  <div v-else-if="projectPageLoading" class="app-shell">
    <div class="space-y-5">
      <div class="h-8 w-40 animate-pulse rounded-full bg-primary-fixed/40"></div>
      <div class="h-14 w-2/3 animate-pulse rounded-3xl bg-surface-container-high"></div>
      <div class="h-5 w-1/2 animate-pulse rounded-full bg-surface-container-high"></div>
      <div class="grid gap-4 md:grid-cols-3">
        <div class="h-36 animate-pulse rounded-[1.5rem] bg-surface-container-high"></div>
        <div class="h-36 animate-pulse rounded-[1.5rem] bg-surface-container-high"></div>
        <div class="h-36 animate-pulse rounded-[1.5rem] bg-surface-container-high"></div>
      </div>
    </div>
  </div>

  <div v-else class="app-shell">
    <div class="text-body-main text-on-surface-variant">{{ t('projectDetail.notFound') }}</div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ProjectColorPicker from '@/components/common/ProjectColorPicker.vue'
import ProjectAssociateNotesDialog from '@/components/projects/ProjectAssociateNotesDialog.vue'
import ProjectTaskCreateDialog from '@/components/projects/ProjectTaskCreateDialog.vue'
import TaskKanbanBoard from '@/components/team/TaskKanbanBoard.vue'
import { notebookService, noteService, projectService, taskService } from '@/api/services'
import { useI18n } from '@/i18n'
import { useNoteStore } from '@/stores/note'
import { useProjectStore } from '@/stores/project'
import { useSpaceStore } from '@/stores/space'
import type { Note, Notebook, ProjectMember, SpaceMember, Task, TaskPriority, TaskStatus } from '@/types/app'
import { formatDateTime, toTimestamp } from '@/utils/date'
import { renderMarkdownLite } from '@/utils/markdown'

type ProjectTaskCreateForm = {
  title: string
  description: string
  priority: TaskPriority
  deadline: string
  assigneeId?: number
  responsibility: string
}

type AssignableMember = Pick<SpaceMember, 'userId' | 'username' | 'nickname'> | Pick<ProjectMember, 'userId' | 'username' | 'nickname'>

const route = useRoute()
const router = useRouter()
const noteStore = useNoteStore()
const projectStore = useProjectStore()
const spaceStore = useSpaceStore()
const { t } = useI18n()

const activeTab = ref<'overview' | 'tasks' | 'docs' | 'members'>('overview')
const tabs = computed(() => [
  { label: t('projectDetail.tabs.overview'), value: 'overview' as const },
  { label: t('projectDetail.tabs.tasks'), value: 'tasks' as const },
  { label: t('projectDetail.tabs.docs'), value: 'docs' as const },
  { label: t('projectDetail.tabs.members'), value: 'members' as const },
])

const projectTaskColumns = computed(() => [
  { status: 'PENDING' as TaskStatus, label: t('task.status.pending'), color: '#9ca3af' },
  { status: 'IN_PROGRESS' as TaskStatus, label: t('task.status.inProgress'), color: '#0077b6' },
  { status: 'COMPLETED' as TaskStatus, label: t('task.status.completed'), color: '#10b981' },
  { status: 'CANCELLED' as TaskStatus, label: t('task.status.cancelled'), color: '#ef4444' },
])

const settingsDrawerOpen = ref(false)
const associateNotesDialogOpen = ref(false)
const taskCreateDialogOpen = ref(false)
const selectedNoteIds = ref<number[]>([])
const selectableNotes = ref<Note[]>([])
const deletingProject = ref(false)
const projectPageLoading = ref(false)

const memberForm = reactive({
  userId: undefined as number | undefined,
  role: 'MEMBER' as 'OWNER' | 'MEMBER',
})

const taskCreateForm = reactive<ProjectTaskCreateForm>({
  title: '',
  description: '',
  priority: 'MEDIUM',
  deadline: '',
  assigneeId: undefined,
  responsibility: '',
})

const settingsForm = reactive({
  name: '',
  description: '',
  coverColor: '#0077b6',
  coverImageUrl: '',
  ownerUserId: undefined as number | undefined,
})

const deleteOptions = reactive({
  taskStrategy: 'transfer' as 'transfer' | 'unlink' | 'delete',
  taskTargetProjectId: undefined as number | undefined,
  docStrategy: 'transfer' as 'transfer' | 'unlink' | 'delete',
  docTargetProjectId: undefined as number | undefined,
})

const completionRate = computed(() => projectStore.currentProject?.completionRate ?? 0)
const completionRingStyle = computed(() => ({
  background: `conic-gradient(#9f4122 0deg ${Math.max(1, completionRate.value) * 3.6}deg, rgba(233, 225, 222, 0.8) ${Math.max(1, completionRate.value) * 3.6}deg 360deg)`,
}))

const ownerDisplayName = computed(() => {
  const ownerId = projectStore.currentProject?.ownerUserId
  return (
    spaceStore.members.find((member) => member.userId === ownerId)?.nickname ||
    spaceStore.members.find((member) => member.userId === ownerId)?.username ||
    t('projectDetail.noOwner')
  )
})

const recentActivities = computed(() =>
  [
    ...projectStore.relatedTasks.list.map((task) => ({
      id: `task-${task.id}`,
      time: task.completedAt || task.gmtModified || task.gmtCreate || '',
      title: task.title,
      description:
        task.status === 'COMPLETED'
          ? t('projectDetail.taskCompletedActivity')
          : t('projectDetail.taskStatusUpdatedActivity', { status: formatTaskStatusLabel(task.status) }),
      type: 'task' as const,
    })),
    ...projectStore.relatedNotes.list.map((note) => ({
      id: `note-${note.id}`,
      time: note.gmtModified || note.gmtCreate || '',
      title: note.title,
      description: t('projectDetail.noteUpdatedActivity'),
      type: 'note' as const,
    })),
  ]
    .filter((item) => item.time)
    .sort((left, right) => toTimestamp(right.time) - toTimestamp(left.time))
    .slice(0, 8),
)

const projectTasksForKanban = computed<Task[]>(() => projectStore.relatedTasks.list)

const availableMembers = computed<SpaceMember[]>(() => {
  const existingIds = new Set(projectStore.members.map((member) => member.userId))
  return spaceStore.members.filter((member) => !existingIds.has(member.userId))
})

const assignableMembers = computed<AssignableMember[]>(() =>
  projectStore.members.length
    ? projectStore.members.map((member) => ({
        userId: member.userId,
        username: member.username,
        nickname: member.nickname,
      }))
    : spaceStore.members.map((member) => ({
        userId: member.userId,
        username: member.username,
        nickname: member.nickname,
      })),
)

const moveTargetProjects = computed(() =>
  projectStore.availableProjects.filter((project) => project.id !== projectStore.currentProject?.id),
)

const projectId = computed(() => Number(route.params.projectId) || 0)

const findRootNotebookByName = (notebooks: Notebook[], name: string) =>
  notebooks.find((notebook) => notebook.parentId === 0 && notebook.name.trim().toLowerCase() === name.trim().toLowerCase()) || null

const formatTaskStatusLabel = (status?: TaskStatus) => {
  switch (status) {
    case 'PENDING':
      return t('task.status.pending')
    case 'IN_PROGRESS':
      return t('task.status.inProgress')
    case 'COMPLETED':
      return t('task.status.completed')
    case 'CANCELLED':
      return t('task.status.cancelled')
    default:
      return t('common.unset')
  }
}

const resetTaskCreateForm = () => {
  taskCreateForm.title = ''
  taskCreateForm.description = ''
  taskCreateForm.priority = 'MEDIUM'
  taskCreateForm.deadline = ''
  taskCreateForm.assigneeId = undefined
  taskCreateForm.responsibility = ''
}

const hydrateSettings = () => {
  settingsForm.name = projectStore.currentProject?.name || ''
  settingsForm.description = projectStore.currentProject?.description || ''
  settingsForm.coverColor = projectStore.currentProject?.coverColor || '#0077b6'
  settingsForm.coverImageUrl = projectStore.currentProject?.coverImageUrl || ''
  settingsForm.ownerUserId = projectStore.currentProject?.ownerUserId

  const fallbackProjectId = moveTargetProjects.value[0]?.id
  if (!fallbackProjectId) {
    if (deleteOptions.taskStrategy === 'transfer') {
      deleteOptions.taskStrategy = 'unlink'
    }
    if (deleteOptions.docStrategy === 'transfer') {
      deleteOptions.docStrategy = 'unlink'
    }
  }
  deleteOptions.taskTargetProjectId = moveTargetProjects.value.some((project) => project.id === deleteOptions.taskTargetProjectId)
    ? deleteOptions.taskTargetProjectId
    : fallbackProjectId
  deleteOptions.docTargetProjectId = moveTargetProjects.value.some((project) => project.id === deleteOptions.docTargetProjectId)
    ? deleteOptions.docTargetProjectId
    : fallbackProjectId
}

const loadProjectTasks = async () => {
  if (!projectId.value) {
    return
  }

  await projectStore.loadProjectTasks(projectId.value, {
    pageNum: 1,
    pageSize: 100,
  })
}

const loadProject = async () => {
  if (!projectId.value) {
    return
  }

  projectPageLoading.value = true
  try {
    const project = await projectStore.loadProject(projectId.value)
    if (!project) {
      return
    }

    await Promise.allSettled([
      projectStore.loadProjectOptions(),
      projectStore.loadMembers(projectId.value),
      loadProjectTasks(),
      projectStore.loadProjectNotes(projectId.value, { pageNum: 1, pageSize: 100 }),
      spaceStore.loadMembers(),
    ])

    hydrateSettings()
  } finally {
    projectPageLoading.value = false
  }
}

const toggleArchive = async () => {
  if (!projectStore.currentProject) {
    return
  }

  await projectStore.archiveProject(projectStore.currentProject.id, !projectStore.currentProject.archived)
  await loadProject()
}

const openTaskCreateDialog = async () => {
  resetTaskCreateForm()
  if (!spaceStore.members.length) {
    await spaceStore.loadMembers()
  }
  if (!projectStore.members.length && projectStore.currentProject) {
    await projectStore.loadMembers(projectStore.currentProject.id)
  }
  taskCreateDialogOpen.value = true
}

const submitProjectTask = async () => {
  const spaceId = spaceStore.currentSpaceId
  const currentProject = projectStore.currentProject
  const title = taskCreateForm.title.trim()
  if (!spaceId || !currentProject) {
    return
  }
  if (!title) {
    ElMessage.warning(t('messages.taskTitleRequired'))
    return
  }

  const responsibility = taskCreateForm.responsibility.trim() || t('projectDetail.defaultTaskResponsibility')
  const assigneeId = taskCreateForm.assigneeId
  await taskService.create(spaceId, {
    title,
    description: taskCreateForm.description.trim() || undefined,
    mode: assigneeId ? 'ASSIGNED' : 'OPEN',
    priority: taskCreateForm.priority,
    deadline: taskCreateForm.deadline || undefined,
    projectId: currentProject.id,
    assignments: assigneeId
      ? [{ userId: assigneeId, responsibility, isRequired: true }]
      : [],
  })

  taskCreateDialogOpen.value = false
  resetTaskCreateForm()
  await Promise.all([loadProjectTasks(), projectStore.loadProject(projectId.value)])
  ElMessage.success(t('projectDetail.projectTaskCreated'))
}

const createProjectDoc = async () => {
  const spaceId = spaceStore.currentSpaceId
  if (!spaceId || !projectStore.currentProject) {
    return
  }

  const projectNotebookName = projectStore.currentProject.name.trim()
  const notebooks = await notebookService.tree(spaceId)
  let projectNotebook = findRootNotebookByName(notebooks, projectNotebookName)

  if (!projectNotebook) {
    projectNotebook = await notebookService.create(spaceId, {
      name: projectNotebookName,
      parentId: 0,
      sortOrder: notebooks.length,
    })
  }

  const note = await noteService.create(spaceId, {
    notebookId: projectNotebook.id,
    title: projectStore.currentProject.name,
    projectId: projectStore.currentProject.id,
    content: '',
    contentHtml: renderMarkdownLite(''),
  })

  await Promise.all([
    projectStore.loadProjectNotes(projectStore.currentProject.id, { pageNum: 1, pageSize: 50 }),
    noteStore.loadWorkspace(),
  ])
  await router.push(`/app/notes/${note.id}`)
}

const openAssociateNotesDialog = async () => {
  const spaceId = spaceStore.currentSpaceId
  if (!spaceId || !projectStore.currentProject) {
    return
  }

  const notesPage = await noteService.page(spaceId, {
    pageNum: 1,
    pageSize: 100,
  })
  selectableNotes.value = notesPage.list.filter((note) => note.projectId !== projectStore.currentProject?.id)
  selectedNoteIds.value = []
  associateNotesDialogOpen.value = true
}

const associateSelectedNotes = async () => {
  if (!projectStore.currentProject) {
    return
  }

  const selectedNotes = selectableNotes.value.filter((note) => selectedNoteIds.value.includes(note.id))
  if (!selectedNotes.length) {
    ElMessage.warning(t('projectDetail.selectAtLeastOneDoc'))
    return
  }

  await projectStore.bindNotesToProject(projectStore.currentProject.id, selectedNotes)
  associateNotesDialogOpen.value = false
  selectedNoteIds.value = []
}

const unbindNote = async (note: Note) => {
  if (!projectStore.currentProject) {
    return
  }

  await projectStore.unbindNoteFromProject(projectStore.currentProject.id, note)
}

const resolveMemberName = (userId?: number) => {
  if (!userId) {
    return t('projectDetail.unassigned')
  }

  const member = spaceStore.members.find((item) => item.userId === userId)
  return member?.nickname || member?.username || t('projectDetail.memberNumber', { id: userId })
}

const openProjectTaskBoard = async (task: Task) => {
  await router.push({
    path: '/app/tasks',
    query: {
      projectId: String(projectId.value),
      openTaskId: String(task.id),
    },
  })
}

const addMember = async () => {
  if (!projectStore.currentProject || !memberForm.userId) {
    ElMessage.warning(t('projectDetail.selectMemberFirst'))
    return
  }

  await projectStore.addMember(projectStore.currentProject.id, {
    userId: memberForm.userId,
    role: memberForm.role,
  })
  memberForm.userId = undefined
  memberForm.role = 'MEMBER'
  await loadProject()
}

const updateMemberRole = async (userId: number, role: string | number | boolean) => {
  if (!projectStore.currentProject) {
    return
  }

  await projectStore.updateMemberRole(projectStore.currentProject.id, userId, String(role))
  await loadProject()
}

const removeMember = async (userId: number) => {
  if (!projectStore.currentProject) {
    return
  }

  await projectStore.removeMember(projectStore.currentProject.id, userId)
  await loadProject()
}

const saveProjectSettings = async () => {
  if (!projectStore.currentProject) {
    return
  }

  await projectStore.updateProject(projectStore.currentProject.id, {
    name: settingsForm.name.trim(),
    description: settingsForm.description.trim(),
    coverColor: settingsForm.coverColor,
    coverImageUrl: settingsForm.coverImageUrl.trim() || undefined,
    ownerUserId: settingsForm.ownerUserId,
  })
  await loadProject()
  settingsDrawerOpen.value = false
}

const copyProjectLink = async () => {
  await navigator.clipboard.writeText(window.location.href)
  ElMessage.success(t('projectDetail.projectLinkCopied'))
}

const fetchAllProjectTasks = async () => {
  const spaceId = spaceStore.currentSpaceId
  if (!spaceId || !projectId.value) {
    return [] as Task[]
  }

  const tasks: Task[] = []
  let pageNum = 1
  const pageSize = 100

  while (true) {
    const page = await projectService.tasks(spaceId, projectId.value, { pageNum, pageSize })
    tasks.push(...page.list)
    if (tasks.length >= page.total || page.list.length < pageSize) {
      break
    }
    pageNum += 1
  }

  return tasks
}

const fetchAllProjectNotes = async () => {
  const spaceId = spaceStore.currentSpaceId
  if (!spaceId || !projectId.value) {
    return [] as Note[]
  }

  const notes: Note[] = []
  let pageNum = 1
  const pageSize = 100

  while (true) {
    const page = await projectService.notes(spaceId, projectId.value, { pageNum, pageSize })
    notes.push(...page.list)
    if (notes.length >= page.total || page.list.length < pageSize) {
      break
    }
    pageNum += 1
  }

  return notes
}

const validateDeleteOptions = () => {
  if (deleteOptions.taskStrategy === 'transfer' && !deleteOptions.taskTargetProjectId) {
    ElMessage.warning(t('projectDetail.selectTaskTargetProject'))
    return false
  }

  if (deleteOptions.docStrategy === 'transfer' && !deleteOptions.docTargetProjectId) {
    ElMessage.warning(t('projectDetail.selectDocTargetProject'))
    return false
  }

  return true
}

const deleteProjectWithRelations = async () => {
  if (!projectStore.currentProject || !spaceStore.currentSpaceId || deletingProject.value) {
    return
  }

  if (!validateDeleteOptions()) {
    return
  }

  const [tasks, notes] = await Promise.all([fetchAllProjectTasks(), fetchAllProjectNotes()])
  const confirmed = await ElMessageBox.confirm(
    t('projectDetail.deleteConfirmMessage', { tasks: tasks.length, docs: notes.length, name: projectStore.currentProject.name }),
    t('projectDetail.deleteProject'),
    {
      confirmButtonText: t('confirm.confirmDelete'),
      cancelButtonText: t('common.cancel'),
      type: 'warning',
    },
  ).catch(() => false)

  if (!confirmed) {
    return
  }

  deletingProject.value = true
  try {
    await Promise.all(
      tasks.map((task) => {
        if (deleteOptions.taskStrategy === 'delete') {
          return taskService.delete(spaceStore.currentSpaceId as number, task.id)
        }

        return taskService.update(spaceStore.currentSpaceId as number, task.id, {
          title: task.title,
          description: task.description,
          priority: task.priority,
          deadline: task.deadline,
          projectId: deleteOptions.taskStrategy === 'transfer' ? deleteOptions.taskTargetProjectId : undefined,
        })
      }),
    )

    await Promise.all(
      notes.map((note) => {
        if (deleteOptions.docStrategy === 'delete') {
          return noteService.delete(spaceStore.currentSpaceId as number, note.id)
        }

        return noteService.update(spaceStore.currentSpaceId as number, note.id, {
          title: note.title,
          notebookId: note.notebookId,
          projectId: deleteOptions.docStrategy === 'transfer' ? deleteOptions.docTargetProjectId : undefined,
          content: note.content,
          contentHtml: note.contentHtml,
          tagIds: note.tags?.map((tag) => tag.id),
        })
      }),
    )

    await projectStore.deleteProject(projectStore.currentProject.id)
    settingsDrawerOpen.value = false
    await router.push('/app/projects')
  } finally {
    deletingProject.value = false
  }
}

onMounted(() => {
  loadProject().catch(() => undefined)
})

watch(
  () => route.params.projectId,
  () => {
    loadProject().catch(() => undefined)
  },
)
</script>
