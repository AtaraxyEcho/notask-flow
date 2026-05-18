<template>
  <header
    v-if="!isTeam"
    class="fixed top-0 z-40 flex h-16 w-full items-center justify-between border-b border-outline-variant/40 bg-surface/90 px-6 shadow-sm backdrop-blur-xl"
  >
    <div class="flex items-center gap-8">
      <RouterLink class="flex items-center gap-2 font-title-serif text-xl font-bold text-primary" to="/app/notes">
        <img src="/logo.svg" alt="Notask Flow" class="h-10 w-10 rounded-2xl object-cover shadow-md" />
        <span>Notask Flow</span>
      </RouterLink>
      <button
        class="hidden w-64 items-center rounded-full border border-outline-variant/30 bg-surface-container-low px-4 py-2 md:flex"
        type="button"
        @click="uiStore.setGlobalSearchOpen(true)"
      >
        <span class="material-symbols-outlined mr-2 text-sm text-on-surface-variant">search</span>
        <span class="flex-1 text-left text-body-secondary text-on-surface-variant">{{ t('common.searchFocus') }}</span>
      </button>
    </div>

    <div class="flex items-center gap-4">
      <LanguageSwitcher :is-team="isTeam" />
      <SpaceSwitcher />
      <RouterLink
        class="relative rounded-full p-2 text-on-surface-variant transition-colors hover:bg-surface-container"
        to="/app/notifications"
      >
        <span class="material-symbols-outlined">notifications</span>
        <span
          v-if="notificationStore.unreadCount"
          class="absolute right-2 top-2 h-2 w-2 rounded-full border-2 border-surface bg-primary"
        ></span>
      </RouterLink>
      <button
        class="rounded-full p-2 text-on-surface-variant transition-colors hover:bg-surface-container"
        type="button"
        @click="router.push('/app/settings')"
      >
        <span class="material-symbols-outlined">grid_view</span>
      </button>
      <el-dropdown>
        <button class="rounded-full">
          <img
            v-if="userStore.profile?.avatarUrl"
            :src="userStore.profile.avatarUrl"
            alt="avatar"
            class="h-8 w-8 rounded-full border-2 border-primary-fixed-dim object-cover"
          />
          <div
            v-else
            class="flex h-8 w-8 items-center justify-center rounded-full border-2 border-primary-fixed-dim bg-primary-fixed text-sm font-semibold text-primary"
          >
            {{ userInitials }}
          </div>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="router.push('/app/settings')">{{ t('common.personalSettings') }}</el-dropdown-item>
            <el-dropdown-item divided @click="handleLogout">{{ t('common.logout') }}</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>

  <header
    v-else
    class="fixed top-0 z-40 flex h-16 w-full items-center justify-between border-b border-white/20 bg-white/50 px-10 shadow-[0_20px_40px_rgba(0,119,182,0.05)] backdrop-blur-2xl"
  >
    <div class="flex items-center gap-6">
      <RouterLink class="flex items-center gap-2 font-title-serif text-2xl font-bold text-[#0077B6]" to="/app/projects">
        <img src="/logo.svg" alt="Notask Flow" class="h-10 w-10 rounded-2xl object-cover shadow-md" />
        <span>Notask Flow Team</span>
      </RouterLink>
      <button
        class="hidden items-center rounded-full border border-white/30 bg-white/50 px-4 py-2 md:flex"
        type="button"
        @click="uiStore.setGlobalSearchOpen(true)"
      >
        <span class="material-symbols-outlined text-outline text-[20px]">search</span>
        <span class="ml-2 w-64 text-left text-label-bold text-on-surface-variant">{{ t('common.searchCollaboration') }}</span>
      </button>
    </div>

    <div class="flex items-center gap-4">
      <LanguageSwitcher :is-team="isTeam" />
      <SpaceSwitcher />
      <RouterLink class="relative rounded-full p-2 text-slate-500 transition-colors hover:bg-[#0077B6]/5" to="/app/notifications">
        <span class="material-symbols-outlined">notifications</span>
        <span
          v-if="notificationStore.unreadCount"
          class="absolute right-2 top-2 h-2 w-2 rounded-full border-2 border-white bg-[#0077B6]"
        ></span>
      </RouterLink>
      <button
        class="rounded-full p-2 text-slate-500 transition-colors hover:bg-[#0077B6]/5"
        type="button"
        @click="openTeamSettings"
      >
        <span class="material-symbols-outlined">grid_view</span>
      </button>
      <el-dropdown>
        <button class="ml-2 h-8 w-8 overflow-hidden rounded-full bg-primary-container">
          <img
            v-if="userStore.profile?.avatarUrl"
            :src="userStore.profile.avatarUrl"
            alt="avatar"
            class="h-full w-full object-cover"
          />
          <div v-else class="flex h-full w-full items-center justify-center text-sm font-semibold text-white">
            {{ userInitials }}
          </div>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="openTeamSettings">{{ t('common.teamSettings') }}</el-dropdown-item>
            <el-dropdown-item @click="router.push('/app/settings')">{{ t('common.personalSettings') }}</el-dropdown-item>
            <el-dropdown-item divided @click="handleLogout">{{ t('common.logout') }}</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>

  <GlobalSearchDialog />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import GlobalSearchDialog from './GlobalSearchDialog.vue'
import LanguageSwitcher from './LanguageSwitcher.vue'
import SpaceSwitcher from './SpaceSwitcher.vue'
import { useI18n } from '@/i18n'
import { useNoteStore } from '@/stores/note'
import { useNotificationStore } from '@/stores/notification'
import { useProjectStore } from '@/stores/project'
import { useSpaceStore } from '@/stores/space'
import { useTaskStore } from '@/stores/task'
import { useTodoStore } from '@/stores/todo'
import { useUiStore } from '@/stores/ui'
import { useUserStore } from '@/stores/user'
import { resolveAvatarUrl } from '@/utils/avatar'

const notificationStore = useNotificationStore()
const noteStore = useNoteStore()
const projectStore = useProjectStore()
const spaceStore = useSpaceStore()
const taskStore = useTaskStore()
const todoStore = useTodoStore()
const uiStore = useUiStore()
const userStore = useUserStore()
const router = useRouter()
const { t } = useI18n()

const isTeam = computed(() => spaceStore.currentSpace?.type === 'TEAM')
const userInitials = computed(() => {
  const value = userStore.displayName.trim()
  return value.slice(0, 1).toUpperCase()
})

const openTeamSettings = async () => {
  if (spaceStore.currentSpaceId && isTeam.value) {
    await router.push(`/app/space/${spaceStore.currentSpaceId}/settings`)
    return
  }

  await router.push('/app/settings')
}

const handleLogout = async () => {
  await spaceStore.offlineMember().catch(() => undefined)
  await userStore.logout()
  spaceStore.reset()
  notificationStore.reset()
  noteStore.reset()
  todoStore.reset()
  taskStore.reset()
  projectStore.reset()
  await router.push('/login')
}
</script>
