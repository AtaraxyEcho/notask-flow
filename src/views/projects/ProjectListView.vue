<template>
  <div class="space-y-[64px]">
    <header class="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
      <div>
        <h1 class="font-display-serif text-[48px] leading-[1.1] tracking-[-0.02em] text-on-surface">{{ t('project.title') }}</h1>
        <p class="mt-2 max-w-md text-[18px] leading-[1.6] text-slate-500">
          {{ t('project.description') }}
        </p>
      </div>
      <div class="flex gap-4">
        <button class="rounded-full border border-primary-container/20 px-6 py-3 text-[13px] font-semibold uppercase tracking-[0.05em] text-primary-container transition-colors hover:bg-indigo-50" type="button" @click="toggleArchiveView">
          {{ projectStore.query.archived ? t('project.viewActive') : t('project.viewArchive') }}
        </button>
        <button class="flex items-center gap-2 rounded-full bg-primary-container px-6 py-3 text-[13px] font-semibold uppercase tracking-[0.05em] text-white shadow-lg shadow-[#0077B6]/20 transition-all active:scale-95" type="button" @click="createCardOpen = !createCardOpen">
          <span class="material-symbols-outlined">create_new_folder</span>
          {{ t('project.newProject') }}
        </button>
      </div>
    </header>

    <div class="grid grid-cols-1 gap-6 lg:grid-cols-2 xl:grid-cols-3">
      <article
        v-for="project in projectStore.projects"
        :key="project.id"
        class="glass-card rounded-lg p-6 shadow-[0_10px_40px_-10px_rgba(0,119,182,0.08)] transition-all duration-300 hover:-translate-y-1"
      >
        <div class="mb-6 flex items-start justify-between">
          <div class="flex items-center gap-3">
            <div
              class="flex h-12 w-12 items-center justify-center rounded-2xl text-white"
              :style="{ background: project.coverColor || '#0077b6' }"
            >
              <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1">water_drop</span>
            </div>
            <div>
              <h3 class="font-title-serif text-h3 text-on-surface">{{ project.name }}</h3>
              <p class="text-[13px] uppercase tracking-[0.05em] text-slate-400">{{ project.archived ? t('project.archive') : t('project.activeProject') }}</p>
            </div>
          </div>
          <button class="text-slate-400 transition-colors hover:text-primary" type="button" @click="router.push(`/app/projects/${project.id}`)">
            <span class="material-symbols-outlined">more_horiz</span>
          </button>
        </div>

        <p class="mb-8 line-clamp-2 text-body-md text-slate-600">
          {{ project.description || t('project.fallbackDescription') }}
        </p>

        <div class="mb-2 flex items-center justify-between">
          <span class="text-[13px] uppercase tracking-[0.05em] text-slate-500">{{ t('project.progress') }}</span>
          <span class="text-[13px] font-bold uppercase tracking-[0.05em] text-primary">{{ t('project.tasksCount', { done: project.completedTaskCount ?? 0, total: project.taskCount ?? 0 }) }}</span>
        </div>
        <div class="mb-8 h-2 w-full rounded-full bg-indigo-50">
          <div class="h-full rounded-full bg-primary-container" :style="{ width: `${project.completionRate ?? 0}%` }"></div>
        </div>

        <div class="flex items-center justify-between">
          <div class="flex -space-x-3">
            <div
              v-for="member in project.members?.slice(0, 3) || []"
              :key="member.userId"
              class="flex h-8 w-8 items-center justify-center rounded-full border-2 border-white bg-primary-fixed text-[10px] font-bold text-primary"
            >
              {{ (member.nickname || member.username).slice(0, 1).toUpperCase() }}
            </div>
          </div>
          <div class="flex items-center gap-2 rounded-full bg-secondary-fixed/30 px-3 py-1 text-[12px] font-bold text-primary">
            <span class="h-1.5 w-1.5 rounded-full bg-primary"></span>
            {{ project.archived ? t('common.archived') : t('common.active') }}
          </div>
        </div>
      </article>

      <article
        v-if="createCardOpen"
        class="glass-card rounded-lg p-6 shadow-[0_10px_40px_-10px_rgba(0,119,182,0.08)]"
      >
        <div class="mb-6 flex items-center gap-3">
          <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary-fixed text-primary">
            <span class="material-symbols-outlined">edit_square</span>
          </div>
          <div>
            <h3 class="font-title-serif text-h3 text-on-surface">{{ t('project.initiateProject') }}</h3>
            <p class="text-[13px] uppercase tracking-[0.05em] text-slate-400">{{ t('project.inlineCreation') }}</p>
          </div>
        </div>

        <div class="space-y-4">
          <input v-model="form.name" class="app-input w-full px-4 py-3" :placeholder="t('project.namePlaceholder')" />
          <textarea v-model="form.description" rows="4" class="app-input w-full px-4 py-3" :placeholder="t('project.descriptionPlaceholder')"></textarea>
          <label class="block">
            <span class="mb-2 block text-[13px] font-semibold uppercase tracking-[0.05em] text-slate-500">{{ t('project.owner') }}</span>
            <el-select v-model="form.ownerUserId" clearable class="w-full" :placeholder="t('project.ownerPlaceholder')">
              <el-option
                v-for="member in spaceStore.members"
                :key="member.userId"
                :label="member.nickname || member.username"
                :value="member.userId"
              />
            </el-select>
          </label>
          <label class="block">
            <span class="mb-2 block text-[13px] font-semibold uppercase tracking-[0.05em] text-slate-500">{{ t('project.coverColor') }}</span>
            <ProjectColorPicker v-model="form.coverColor" />
          </label>
        </div>

        <div class="mt-6 flex justify-end gap-3">
          <button class="app-secondary-button" type="button" @click="closeCreateCard">{{ t('common.cancel') }}</button>
          <button class="app-primary-button" type="button" @click="submitCreate">{{ t('project.createProject') }}</button>
        </div>
      </article>

      <button
        v-else
        class="flex min-h-[300px] flex-col items-center justify-center rounded-lg border-2 border-dashed border-indigo-100 p-6 text-center transition-all hover:border-primary-container/30 hover:bg-indigo-50/20"
        type="button"
        @click="createCardOpen = true"
      >
        <div class="mb-4 flex h-12 w-12 items-center justify-center rounded-full border border-indigo-50 bg-white text-slate-400 transition-all hover:scale-110 hover:text-primary-container">
          <span class="material-symbols-outlined">add</span>
        </div>
        <h3 class="font-title-serif text-h3 text-slate-400">{{ t('project.initiateProject') }}</h3>
        <p class="max-w-[200px] text-body-md text-slate-400">{{ t('project.launchDescription') }}</p>
      </button>
    </div>

    <section class="grid grid-cols-12 gap-6">
      <div class="glass-card col-span-12 rounded-xl p-8 shadow-[0_10px_40px_-10px_rgba(0,119,182,0.08)] lg:col-span-8">
        <div class="mb-8 flex items-center justify-between">
          <div>
            <h2 class="font-title-serif text-h2 text-on-surface">{{ t('project.projectSnapshot') }}</h2>
            <p class="mt-2 text-body-secondary text-on-surface-variant">{{ t('project.projectSnapshotDescription') }}</p>
          </div>
          <div class="rounded-full bg-indigo-50/50 px-4 py-2 text-[13px] font-medium text-primary">{{ t('project.currentSnapshot') }}</div>
        </div>

        <div class="grid gap-4 md:grid-cols-2">
          <article
            v-for="stat in projectSnapshotStats"
            :key="stat.label"
            class="rounded-[1.5rem] border border-outline-variant/20 bg-surface-container-low/70 px-5 py-5"
          >
            <div class="flex items-start justify-between gap-4">
              <div>
                <div class="text-caption uppercase tracking-[0.22em] text-on-surface-variant">{{ stat.label }}</div>
                <div class="mt-3 font-display-serif text-4xl text-primary">{{ stat.value }}</div>
              </div>
              <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary-fixed text-primary">
                <span class="material-symbols-outlined">{{ stat.icon }}</span>
              </div>
            </div>
            <div class="mt-4 h-2 rounded-full bg-surface-container-high">
              <div class="h-full rounded-full bg-primary" :style="{ width: `${stat.percent}%` }"></div>
            </div>
          </article>
        </div>
      </div>

      <div class="col-span-12 flex flex-col gap-6 lg:col-span-4">
        <div class="glass-card flex flex-1 flex-col items-center justify-center rounded-xl p-6 text-center shadow-[0_10px_40px_-10px_rgba(0,119,182,0.08)]">
          <div class="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-secondary-fixed/50 text-primary">
            <span class="material-symbols-outlined text-3xl">task_alt</span>
          </div>
          <h4 class="font-title-serif text-h3 text-on-surface">{{ t('project.completedTasks', { count: totalCompletedTasks }) }}</h4>
          <p class="text-[13px] uppercase tracking-[0.05em] text-slate-500">{{ t('project.completedInView') }}</p>
        </div>

        <div class="glass-card flex flex-1 flex-col items-center justify-center rounded-xl p-6 text-center shadow-[0_10px_40px_-10px_rgba(0,119,182,0.08)]">
          <div class="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-tertiary-fixed/50 text-tertiary">
            <span class="material-symbols-outlined text-3xl">timer</span>
          </div>
          <h4 class="font-title-serif text-h3 text-on-surface">{{ averageCompletion }}%</h4>
          <p class="text-[13px] uppercase tracking-[0.05em] text-slate-500">{{ t('project.averageCompletion') }}</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import ProjectColorPicker from '@/components/common/ProjectColorPicker.vue'
import { useI18n } from '@/i18n'
import { useProjectStore } from '@/stores/project'
import { useSpaceStore } from '@/stores/space'

const router = useRouter()
const projectStore = useProjectStore()
const spaceStore = useSpaceStore()
const { t } = useI18n()

const createCardOpen = ref(false)
const searchKeyword = ref('')
const form = reactive({
  name: '',
  description: '',
  coverColor: '#0077b6',
  ownerUserId: undefined as number | undefined,
})

const totalCompletedTasks = computed(() =>
  projectStore.projects.reduce((sum, project) => sum + (project.completedTaskCount ?? 0), 0),
)
const averageCompletion = computed(() => {
  if (!projectStore.projects.length) {
    return 0
  }

  const total = projectStore.projects.reduce((sum, project) => sum + (project.completionRate ?? 0), 0)
  return Math.round(total / projectStore.projects.length)
})
const activeProjectCount = computed(() => projectStore.projects.filter((project) => !project.archived).length)
const archivedProjectCount = computed(() => projectStore.projects.filter((project) => project.archived).length)
const totalDocumentCount = computed(() => projectStore.projects.reduce((sum, project) => sum + (project.documentCount ?? 0), 0))
const projectSnapshotStats = computed(() => {
  const projectTotal = Math.max(projectStore.projects.length, 1)
  const taskTotal = Math.max(
    projectStore.projects.reduce((sum, project) => sum + (project.taskCount ?? 0), 0),
    1,
  )

  return [
    {
      icon: 'rocket_launch',
      label: t('project.activeProjects'),
      percent: Math.round((activeProjectCount.value / projectTotal) * 100),
      value: activeProjectCount.value,
    },
    {
      icon: 'inventory_2',
      label: t('project.archivedProjects'),
      percent: Math.round((archivedProjectCount.value / projectTotal) * 100),
      value: archivedProjectCount.value,
    },
    {
      icon: 'description',
      label: t('project.totalDocuments'),
      percent: Math.min(100, totalDocumentCount.value * 12),
      value: totalDocumentCount.value,
    },
    {
      icon: 'groups',
      label: t('project.taskCoverage'),
      percent: Math.round((totalCompletedTasks.value / taskTotal) * 100),
      value: `${Math.round((totalCompletedTasks.value / taskTotal) * 100)}%`,
    },
  ]
})

let timer: number | undefined

const loadProjects = async () => {
  if (spaceStore.currentSpace?.type !== 'TEAM') {
    return
  }

  projectStore.query.keyword = searchKeyword.value.trim()
  await Promise.all([projectStore.loadProjects(), spaceStore.loadMembers()])
}

const closeCreateCard = () => {
  createCardOpen.value = false
  form.name = ''
  form.description = ''
  form.coverColor = '#0077b6'
  form.ownerUserId = undefined
}

const submitCreate = async () => {
  const name = form.name.trim()
  if (!name) {
    return
  }

  const project = await projectStore.createProject({
    name,
    description: form.description.trim(),
    coverColor: form.coverColor,
    ownerUserId: form.ownerUserId,
  })

  closeCreateCard()

  if (project) {
    await router.push(`/app/projects/${project.id}`)
  }
}

const toggleArchiveView = async () => {
  projectStore.query.archived = !projectStore.query.archived
  await loadProjects()
}

onMounted(() => {
  loadProjects().catch(() => undefined)
})

watch(
  () => spaceStore.currentSpaceId,
  () => {
    loadProjects().catch(() => undefined)
  },
)

watch(searchKeyword, () => {
  if (timer) {
    window.clearTimeout(timer)
  }
  timer = window.setTimeout(() => {
    loadProjects().catch(() => undefined)
  }, 250)
})
</script>
