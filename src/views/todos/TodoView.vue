<template>
  <div class="grid gap-6 xl:grid-cols-[minmax(0,1fr)_320px]">
    <section class="rounded-[1.75rem] bg-white/50 p-8 shadow-sm">
      <div class="mb-10 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <h3 class="font-display-serif text-display-serif text-on-surface">{{ t('todo.title') }}</h3>
          <p class="text-body-secondary text-stone-500">{{ t('todo.description') }}</p>
        </div>
        <div class="flex gap-2 rounded-full bg-surface-container-low p-1">
          <button
            v-for="filter in filters"
            :key="filter.value"
            class="rounded-full px-5 py-1.5 text-xs font-semibold transition-colors"
            :class="currentFilter === filter.value ? 'bg-[#FF8A65] text-white shadow-sm' : 'text-stone-500 hover:bg-stone-100'"
            type="button"
            @click="applyFilter(filter.value)"
          >
            {{ filter.label }}
          </button>
        </div>
      </div>

      <div v-if="composerOpen" class="mb-6 rounded-2xl border border-[#FF8A65]/20 bg-[#FDF8F5] p-5 shadow-sm">
        <div class="grid gap-4 md:grid-cols-[1fr_260px_auto]">
          <input v-model="form.title" class="app-input w-full px-4 py-3" :placeholder="t('todo.titlePlaceholder')" />
          <el-date-picker
            v-model="form.deadline"
            class="w-full"
            clearable
            format="YYYY-MM-DD:HH:mm:ss"
            :placeholder="t('todo.deadlinePlaceholder')"
            type="datetime"
            value-format="YYYY-MM-DD:HH:mm:ss"
          />
          <div class="flex gap-2">
            <button class="app-secondary-button" type="button" @click="closeComposer">{{ t('common.cancel') }}</button>
            <button class="app-primary-button" type="button" @click="submitTodo">
              {{ editingTodo ? t('todo.saveEdit') : t('todo.createTodo') }}
            </button>
          </div>
        </div>
      </div>

      <div v-if="todoStore.loading" class="space-y-4">
        <div v-for="item in 4" :key="item" class="animate-pulse rounded-2xl bg-white p-5">
          <div class="h-4 w-1/2 rounded-full bg-surface-container-high"></div>
        </div>
      </div>

      <div v-else-if="todoStore.todos.length" class="space-y-4">
        <article
          v-for="todo in todoStore.todos"
          :key="todo.id"
          class="group flex items-center justify-between rounded-2xl p-5 transition-all duration-300"
          :class="
            todo.isCompleted
              ? 'border-l-4 border-[#FF8A65] bg-[#FDF8F5] shadow-sm'
              : 'border border-[#E0E0E0] bg-white hover:border-[#FF8A65]/30 hover:shadow-md'
          "
        >
          <div class="flex items-center gap-5">
            <button
              class="flex h-6 w-6 items-center justify-center rounded-full border-2 transition-colors"
              :class="todo.isCompleted ? 'border-[#FF8A65] bg-white shadow-sm' : 'border-stone-200 hover:border-[#FF8A65]'"
              type="button"
              @click="todoStore.toggleTodo(todo)"
            >
              <span
                class="material-symbols-outlined text-[16px]"
                :class="todo.isCompleted ? 'text-[#FF8A65]' : 'text-transparent hover:text-[#FF8A65]'"
              >
                check
              </span>
            </button>

            <div>
              <h4 class="font-body-main" :class="todo.isCompleted ? 'text-stone-400 line-through' : 'text-on-surface'">
                {{ todo.title }}
              </h4>
              <div class="mt-1 flex flex-wrap items-center gap-3">
                <span class="rounded bg-stone-50 px-2 py-0.5 text-[11px] font-semibold uppercase tracking-wider text-stone-400">
                  {{ todo.taskId ? t('todo.taskSync') : t('todo.manual') }}
                </span>
                <span v-if="todo.isCompleted" class="flex items-center gap-1 text-[11px] font-medium text-[#FF8A65]">
                  <span class="material-symbols-outlined text-[14px]">check_circle</span>
                  {{ t('todo.completed') }}
                </span>
                <span v-else-if="todo.deadline" class="flex items-center gap-1 text-[11px] text-stone-400">
                  <span class="material-symbols-outlined text-[14px]">schedule</span>
                  {{ formatDateTime(todo.deadline) }}
                </span>
              </div>
            </div>
          </div>

          <div class="flex items-center gap-1 opacity-0 transition-opacity group-hover:opacity-100 focus-within:opacity-100">
            <button
              class="p-2 text-stone-400 transition-colors hover:text-primary"
              type="button"
              @click="openComposer(todo)"
            >
              <span class="material-symbols-outlined">edit</span>
            </button>
            <button
              class="p-2 text-stone-400 transition-colors hover:text-error"
              type="button"
              @click="confirmRemoveTodo(todo)"
            >
              <span class="material-symbols-outlined">delete</span>
            </button>
          </div>
        </article>
      </div>

      <EmptyState
        v-else
        :title="t('todo.emptyTitle')"
        :description="t('todo.emptyDescription')"
        icon="checklist"
      />

      <div class="mt-8">
        <button class="group flex items-center gap-3 text-stone-400 transition-colors hover:text-[#FF8A65]" type="button" @click="openComposer()">
          <span class="material-symbols-outlined text-sm">add_circle</span>
          <span class="text-xs font-semibold uppercase tracking-widest">{{ t('todo.addNewTask') }}</span>
        </button>
      </div>

      <div v-if="showPagination" class="mt-6 flex justify-end">
        <el-pagination
          v-model:current-page="todoStore.query.pageNum"
          v-model:page-size="todoStore.query.pageSize"
          background
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50, 100]"
          :total="todoStore.todosPage.total"
          @current-change="loadTodos"
          @size-change="handlePageSizeChange"
        />
      </div>
    </section>

    <aside class="space-y-6">
      <div class="rounded-[1.75rem] bg-surface-container-high p-6 shadow-sm">
        <h4 class="mb-6 font-title-serif text-lg">{{ t('todo.pulse') }}</h4>
        <div class="space-y-6">
          <div class="flex items-end justify-between">
            <div>
              <p class="mb-1 text-caption text-stone-500">{{ t('todo.totalTasks') }}</p>
              <p class="font-display-serif text-4xl text-[#FF8A65]">{{ todoStore.totalCount }}</p>
            </div>
            <div class="text-right">
              <p class="mb-1 text-caption text-stone-500">{{ t('todo.completion') }}</p>
              <p class="text-label-bold text-on-surface">{{ completionRate }}%</p>
            </div>
          </div>
          <div class="h-2 w-full overflow-hidden rounded-full bg-white/50">
            <div class="h-full rounded-full bg-[#FF8A65]" :style="{ width: `${completionRate}%` }"></div>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div class="rounded-2xl bg-white p-4">
              <span class="material-symbols-outlined mb-2 text-[#FF8A65]">pending_actions</span>
              <p class="text-caption text-stone-500">{{ t('todo.unfinished') }}</p>
              <p class="font-title-serif text-xl font-bold">{{ todoStore.unfinishedCount }}</p>
            </div>
            <div class="rounded-2xl bg-white p-4">
              <span class="material-symbols-outlined mb-2 text-primary">task_alt</span>
              <p class="text-caption text-stone-500">{{ t('todo.completed') }}</p>
              <p class="font-title-serif text-xl font-bold">{{ todoStore.completedCount }}</p>
            </div>
          </div>
        </div>
      </div>

      <div class="relative h-48 overflow-hidden rounded-[1.75rem]">
        <img
          v-if="!illustrationFailed"
          :src="todoIllustrationUrl"
          alt="Todo reflection"
          class="h-full w-full object-cover"
          @error="illustrationFailed = true"
        />
        <template v-else>
          <div class="absolute inset-0 bg-[linear-gradient(180deg,rgba(255,202,183,0.3),rgba(159,65,34,0.65))]"></div>
          <div class="absolute inset-0 bg-[radial-gradient(circle_at_top,_rgba(255,255,255,0.35),transparent_48%)]"></div>
        </template>
        <div class="absolute inset-0 bg-black/10"></div>
        <div class="absolute bottom-4 left-4 right-4">
          <p class="font-title-serif text-sm italic text-white">
            "{{ t('todo.quote') }}"
          </p>
          <p class="mt-1 text-[10px] font-semibold uppercase tracking-widest text-white/75">{{ t('todo.quoteAuthor') }}</p>
        </div>
      </div>

      <div class="rounded-[1.75rem] bg-[#F4F1F0] p-6">
        <h4 class="mb-4 text-xs font-semibold uppercase tracking-widest text-stone-500">{{ t('todo.focusAreas') }}</h4>
        <div class="flex flex-wrap gap-2">
          <span
            v-for="area in focusAreas"
            :key="area"
            class="rounded-full border px-3 py-1 text-xs font-medium"
            :class="area === focusAreas[0] ? 'border-[#FF8A65]/10 bg-white text-[#FF8A65]' : 'border-stone-200 bg-white text-stone-500'"
          >
            {{ area }}
          </span>
        </div>
      </div>
    </aside>

    <button
      class="fixed bottom-8 right-8 z-50 flex h-14 w-14 items-center justify-center rounded-full bg-[#FF8A65] text-white shadow-lg transition-all hover:scale-105 active:scale-95"
      type="button"
      @click="openComposer()"
    >
      <span class="material-symbols-outlined text-2xl">add</span>
    </button>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import type { Todo } from '@/types/app'
import todoIllustrationUrl from '@/assets/todo-view.png'
import EmptyState from '@/components/common/EmptyState.vue'
import { useI18n } from '@/i18n'
import { useSpaceStore } from '@/stores/space'
import { useTodoStore } from '@/stores/todo'
import { formatDateTime, toLocalInputDateTime } from '@/utils/date'

const todoStore = useTodoStore()
const spaceStore = useSpaceStore()
const { t } = useI18n()
const editingTodo = ref<Todo | null>(null)
const composerOpen = ref(false)
const currentFilter = ref<'all' | 'unfinished' | 'completed'>('all')
const illustrationFailed = ref(false)

const form = reactive({
  title: '',
  deadline: '',
})

const filters = computed(() => [
  { label: t('todo.filters.all'), value: 'all' as const },
  { label: t('todo.filters.unfinished'), value: 'unfinished' as const },
  { label: t('todo.filters.completed'), value: 'completed' as const },
])

const focusAreas = computed(() => [
  t('todo.areas.mindfulness'),
  t('todo.areas.reading'),
  t('todo.areas.writing'),
  t('todo.areas.health'),
  t('todo.areas.planning'),
])
const completionRate = computed(() => {
  if (!todoStore.totalCount) {
    return 0
  }

  return Math.round((todoStore.completedCount / todoStore.totalCount) * 100)
})
const showPagination = computed(() => todoStore.todosPage.total > (todoStore.query.pageSize || 20))

const loadTodos = async () => {
  await todoStore.loadTodos()
}

const applyFilter = async (filter: 'all' | 'unfinished' | 'completed') => {
  currentFilter.value = filter
  if (filter === 'all') {
    todoStore.query.isCompleted = undefined
  } else {
    todoStore.query.isCompleted = filter === 'completed'
  }
  todoStore.query.pageNum = 1
  await todoStore.loadTodos()
}

const closeComposer = () => {
  composerOpen.value = false
  editingTodo.value = null
  form.title = ''
  form.deadline = ''
}

const openComposer = (todo?: Todo) => {
  composerOpen.value = true
  editingTodo.value = todo || null
  form.title = todo?.title || ''
  form.deadline = toLocalInputDateTime(todo?.deadline)
}

const submitTodo = async () => {
  const payload = {
    title: form.title.trim(),
    deadline: form.deadline || undefined,
  }

  if (!payload.title) {
    ElMessage.warning(t('todo.titlePlaceholder'))
    return
  }

  if (editingTodo.value) {
    await todoStore.updateTodo(editingTodo.value.id, payload)
  } else {
    await todoStore.createTodo(payload)
  }

  closeComposer()
}

const handlePageSizeChange = async () => {
  todoStore.query.pageNum = 1
  await todoStore.loadTodos()
}

const confirmRemoveTodo = async (todo: Todo) => {
  try {
    await ElMessageBox.confirm(todo.title, t('common.delete'), {
      cancelButtonText: t('common.cancel'),
      confirmButtonText: t('common.delete'),
      type: 'warning',
    })
  } catch {
    return
  }

  await todoStore.removeTodo(todo.id)
}

onMounted(() => {
  loadTodos().catch(() => undefined)
})

watch(
  () => spaceStore.currentSpaceId,
  () => {
    todoStore.query.pageNum = 1
    loadTodos().catch(() => undefined)
  },
)
</script>
