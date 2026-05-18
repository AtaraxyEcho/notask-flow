<template>
  <el-dialog
    v-model="uiStore.globalSearchOpen"
    append-to-body
    width="min(900px, 92vw)"
    :show-close="false"
    class="global-search-dialog"
    align-center
  >
    <template #header>
      <div class="flex items-center justify-between gap-4">
        <div>
          <h2 class="font-title-serif text-2xl text-on-surface">{{ t('search.title') }}</h2>
          <p class="text-body-secondary text-on-surface-variant">{{ t('search.description') }}</p>
        </div>
        <button class="app-icon-button" type="button" @click="uiStore.setGlobalSearchOpen(false)">
          <span class="material-symbols-outlined text-lg">close</span>
        </button>
      </div>
    </template>

    <div class="space-y-5">
      <label class="app-input flex items-center gap-3 px-4 py-3">
        <span class="material-symbols-outlined text-on-surface-variant">search</span>
        <input
          v-model="keyword"
          class="w-full border-none bg-transparent p-0 text-body-main outline-none focus:ring-0"
          :placeholder="t('search.placeholder')"
        />
      </label>

      <el-tabs v-model="activeTab" class="search-tabs">
        <el-tab-pane :label="t('search.notes')" name="notes" />
        <el-tab-pane :label="t('search.tasks')" name="tasks" />
        <el-tab-pane :label="t('search.projects')" name="projects" />
      </el-tabs>

      <div v-if="loading" class="grid gap-3">
        <div v-for="item in 4" :key="item" class="app-card animate-pulse p-4">
          <div class="mb-3 h-4 w-1/2 rounded-full bg-surface-container-high"></div>
          <div class="h-3 w-full rounded-full bg-surface-container-high"></div>
        </div>
      </div>

      <div v-else-if="activeTab === 'notes'" class="grid gap-3">
        <button
          v-for="note in noteResults"
          :key="note.id"
          class="app-card text-left transition-transform hover:-translate-y-1"
          type="button"
          @click="goTo(`/app/notes/${note.id}`)"
        >
          <div class="flex items-start justify-between gap-4">
            <div>
              <div class="font-title-serif text-lg text-on-surface">{{ note.title }}</div>
              <p class="mt-2 line-clamp-2 text-body-secondary text-on-surface-variant">
                {{ note.content || t('search.emptyNoteContent') }}
              </p>
            </div>
            <span class="app-chip">{{ t('search.noteChip') }}</span>
          </div>
        </button>

        <EmptyState
          v-if="!noteResults.length"
          :title="t('search.noNotesTitle')"
          :description="t('search.noNotesDescription')"
          icon="note_stack"
        />
      </div>

      <div v-else-if="activeTab === 'tasks'" class="grid gap-3">
        <button
          v-for="task in taskResults"
          :key="task.id"
          class="app-card text-left transition-transform hover:-translate-y-1"
          type="button"
          @click="goTo('/app/tasks')"
        >
          <div class="flex items-center justify-between gap-4">
            <div>
              <div class="font-title-serif text-lg text-on-surface">{{ task.title }}</div>
              <p class="mt-2 text-body-secondary text-on-surface-variant">{{ task.description || t('search.emptyTaskDescription') }}</p>
            </div>
            <span class="app-chip">{{ task.status }}</span>
          </div>
        </button>

        <EmptyState
          v-if="!taskResults.length"
          :title="t('search.noTasksTitle')"
          :description="t('search.noTasksDescription')"
          icon="check_circle"
        />
      </div>

      <div v-else class="grid gap-3">
        <button
          v-for="project in projectResults"
          :key="project.id"
          class="app-card text-left transition-transform hover:-translate-y-1"
          type="button"
          @click="goTo(`/app/projects/${project.id}`)"
        >
          <div class="flex items-center justify-between gap-4">
            <div>
              <div class="font-title-serif text-lg text-on-surface">{{ project.name }}</div>
              <p class="mt-2 text-body-secondary text-on-surface-variant">
                {{ project.description || t('search.emptyProjectDescription') }}
              </p>
            </div>
            <span class="app-chip">{{ project.completionRate ?? 0 }}%</span>
          </div>
        </button>

        <EmptyState
          v-if="!projectResults.length"
          :title="t('search.noProjectsTitle')"
          :description="t('search.noProjectsDescription')"
          icon="folder_managed"
        />
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { noteService, projectService, taskService } from '@/api/services'
import { useI18n } from '@/i18n'
import type { Note, PageResponse, Project, Task } from '@/types/app'
import { useSpaceStore } from '@/stores/space'
import { useUiStore } from '@/stores/ui'
import EmptyState from './EmptyState.vue'

const uiStore = useUiStore()
const spaceStore = useSpaceStore()
const router = useRouter()
const { t } = useI18n()

const keyword = ref('')
const activeTab = ref<'notes' | 'tasks' | 'projects'>('notes')
const loading = ref(false)
const noteResults = ref<Note[]>([])
const taskResults = ref<Task[]>([])
const projectResults = ref<Project[]>([])

let timer: number | undefined
const emptyProjectPage = (): PageResponse<Project> => ({
  total: 0,
  pageNum: 1,
  pageSize: 6,
  list: [],
})

const runSearch = async () => {
  const spaceId = spaceStore.currentSpaceId
  if (!spaceId || keyword.value.trim().length < 2) {
    noteResults.value = []
    taskResults.value = []
    projectResults.value = []
    return
  }

  loading.value = true
  try {
    const projectRequest =
      spaceStore.currentSpace?.type === 'TEAM'
        ? projectService.page(spaceId, {
            pageNum: 1,
            pageSize: 6,
            keyword: keyword.value.trim(),
            archived: false,
          })
        : Promise.resolve(emptyProjectPage())

    const [notes, tasks, projects] = await Promise.all([
      noteService.search(spaceId, keyword.value.trim()),
      taskService.page(spaceId, { pageNum: 1, pageSize: 6, keyword: keyword.value.trim() }),
      projectRequest,
    ])

    noteResults.value = notes
    taskResults.value = tasks.list
    projectResults.value = projects.list
  } finally {
    loading.value = false
  }
}

const goTo = async (path: string) => {
  uiStore.setGlobalSearchOpen(false)
  keyword.value = ''
  await router.push(path)
}

watch(keyword, () => {
  if (timer) {
    window.clearTimeout(timer)
  }

  timer = window.setTimeout(() => {
    runSearch().catch(() => undefined)
  }, 250)
})

watch(
  () => uiStore.globalSearchOpen,
  (open) => {
    if (!open) {
      keyword.value = ''
      noteResults.value = []
      taskResults.value = []
      projectResults.value = []
    }
  },
)

watch(
  () => spaceStore.currentSpaceId,
  () => {
    noteResults.value = []
    taskResults.value = []
    projectResults.value = []

    if (uiStore.globalSearchOpen && keyword.value.trim().length >= 2) {
      runSearch().catch(() => undefined)
    }
  },
)

const onKeydown = (event: KeyboardEvent) => {
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
    event.preventDefault()
    uiStore.setGlobalSearchOpen(!uiStore.globalSearchOpen)
  }
}

onMounted(() => window.addEventListener('keydown', onKeydown))
onUnmounted(() => window.removeEventListener('keydown', onKeydown))
</script>
