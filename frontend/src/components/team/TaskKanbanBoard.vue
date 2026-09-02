<template>
  <div class="hide-scrollbar overflow-x-auto pb-2">
    <div class="flex min-w-[1120px] gap-5">
      <section
        v-for="column in resolvedColumns"
        :key="column.status"
        class="flex min-h-[560px] flex-1 flex-col rounded-[1.75rem] border p-4"
        :class="isTeam ? 'team-shell' : 'border-outline-variant/20 bg-surface-container-low/60'"
        @dragover.prevent="dragEnabled && $event.preventDefault()"
        @drop="handleDrop(column.status)"
      >
        <div class="mb-4 flex items-center justify-between px-1">
          <div class="flex items-center gap-3">
            <span class="h-2.5 w-2.5 rounded-full" :style="{ backgroundColor: column.color }"></span>
            <div class="text-label-bold uppercase tracking-[0.22em] text-on-surface">{{ column.label }}</div>
            <span class="text-caption text-on-surface-variant">{{ groupedTasks[column.status]?.length ?? 0 }}</span>
          </div>
        </div>

        <div class="flex flex-1 flex-col gap-4">
          <article
            v-for="task in groupedTasks[column.status]"
            :key="task.id"
            class="rounded-[1.5rem] border p-4 shadow-sm transition hover:-translate-y-1 hover:shadow-lg"
            :class="[
              clickable ? 'cursor-pointer' : '',
              isTeam
                ? 'glass-card border-white/30 bg-white/60'
                : task.status === 'COMPLETED'
                  ? 'border-primary/10 bg-primary-fixed/20 opacity-85'
                  : 'border-outline-variant/30 bg-surface',
            ]"
            :draggable="dragEnabled"
            @click="handleTaskClick(task)"
            @dragstart="handleDragStart(task.id)"
          >
            <div class="mb-3 flex items-start justify-between gap-3">
              <div class="flex flex-wrap gap-2">
                <span class="app-chip">{{ formatPriorityLabel(task.priority) }}</span>
                <span v-if="showModeChip" class="app-chip">{{ formatModeLabel(task.mode) }}</span>
                <span v-if="showProjectBadge && task.projectName" class="app-chip">{{ task.projectName }}</span>
              </div>
              <el-checkbox
                v-if="selectable"
                :model-value="selectedTaskIds.includes(task.id)"
                @click.stop
                @change="handleSelectionChange(task.id, $event)"
              />
            </div>

            <h3
              class="font-title-serif text-2xl"
              :class="task.status === 'COMPLETED' ? 'text-on-surface-variant line-through' : 'text-on-surface'"
            >
              {{ task.title }}
            </h3>
            <p class="mt-3 line-clamp-3 text-body-secondary text-on-surface-variant">
              {{ task.description || t('task.noDescription') }}
            </p>

            <div class="mt-6 flex items-end justify-between gap-3">
              <div class="flex items-center gap-3">
                <div class="flex -space-x-2">
                  <div
                    v-for="member in task.members?.slice(0, 3) || []"
                    :key="member.id"
                    class="flex h-8 w-8 items-center justify-center rounded-full border-2 border-white bg-primary-fixed text-xs font-semibold text-primary"
                  >
                    {{ resolveMemberInitial(member.username) }}
                  </div>
                </div>
                <span v-if="task.members?.length" class="text-caption text-on-surface-variant">
                  {{ t('task.assigneeCount', { count: task.members.length }) }}
                </span>
              </div>
              <span class="text-caption text-on-surface-variant">
                {{ task.deadline ? formatDateTime(task.deadline) : t('task.noDeadline') }}
              </span>
            </div>
          </article>

          <EmptyState
            v-if="!groupedTasks[column.status]?.length"
            :title="emptyTitle"
            :description="emptyDescription"
            icon="view_kanban"
          />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { useI18n } from '@/i18n'
import type { Task, TaskPriority, TaskStatus } from '@/types/app'
import { formatDateTime } from '@/utils/date'

interface TaskKanbanColumn {
  status: TaskStatus
  label: string
  color: string
}

const props = withDefaults(
  defineProps<{
    tasks: Task[]
    isTeam?: boolean
    dragEnabled?: boolean
    selectable?: boolean
    clickable?: boolean
    showModeChip?: boolean
    showProjectBadge?: boolean
    selectedTaskIds?: number[]
    columns?: TaskKanbanColumn[]
    emptyTitle?: string
    emptyDescription?: string
  }>(),
  {
    isTeam: true,
    dragEnabled: false,
    selectable: false,
    clickable: true,
    showModeChip: true,
    showProjectBadge: true,
    selectedTaskIds: () => [],
    emptyTitle: undefined,
    emptyDescription: undefined,
  },
)

const emit = defineEmits<{
  (event: 'task-click', task: Task): void
  (event: 'task-drag-start', taskId: number): void
  (event: 'task-drop', status: TaskStatus): void
  (event: 'toggle-select', payload: { taskId: number; checked: boolean }): void
}>()

const { t } = useI18n()
const defaultColumns = computed<TaskKanbanColumn[]>(() => [
  { status: 'PENDING', label: t('task.status.pending'), color: '#9ca3af' },
  { status: 'IN_PROGRESS', label: t('task.status.inProgress'), color: '#0077b6' },
  { status: 'COMPLETED', label: t('task.status.completed'), color: '#10b981' },
  { status: 'CANCELLED', label: t('task.status.cancelled'), color: '#ef4444' },
])
const emptyTitle = computed(() => props.emptyTitle || t('task.emptyTitle'))
const emptyDescription = computed(() => props.emptyDescription || t('task.emptyDescription'))
const resolvedColumns = computed(() => (props.columns?.length ? props.columns : defaultColumns.value))
const groupedTasks = computed(() =>
  resolvedColumns.value.reduce(
    (result, column) => {
      result[column.status] = props.tasks.filter((task) => task.status === column.status)
      return result
    },
    {} as Record<TaskStatus, Task[]>,
  ),
)

const formatModeLabel = (mode?: string) => {
  switch (mode) {
    case 'ASSIGNED':
      return t('task.mode.assigned')
    case 'OPEN':
      return t('task.mode.open')
    default:
      return t('task.mode.unset')
  }
}

const formatPriorityLabel = (priority?: TaskPriority) => {
  switch (priority) {
    case 'LOW':
      return t('task.priority.low')
    case 'HIGH':
      return t('task.priority.high')
    case 'MEDIUM':
    default:
      return t('task.priority.medium')
  }
}

const resolveMemberInitial = (username?: string) => (username || '?').slice(0, 1).toUpperCase()

const handleTaskClick = (task: Task) => {
  if (!props.clickable) {
    return
  }

  emit('task-click', task)
}

const handleDragStart = (taskId: number) => {
  if (!props.dragEnabled) {
    return
  }

  emit('task-drag-start', taskId)
}

const handleDrop = (status: TaskStatus) => {
  if (!props.dragEnabled) {
    return
  }

  emit('task-drop', status)
}

const handleSelectionChange = (taskId: number, checked: string | number | boolean) => {
  emit('toggle-select', { taskId, checked: Boolean(checked) })
}
</script>
