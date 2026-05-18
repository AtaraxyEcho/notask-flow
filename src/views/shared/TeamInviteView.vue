<template>
  <div class="min-h-screen bg-background px-4 py-10 text-on-background md:px-8">
    <div class="mx-auto max-w-3xl">
      <div v-if="loading" class="app-shell animate-pulse">
        <div class="mb-4 h-8 w-1/2 rounded-full bg-surface-container-high"></div>
        <div class="h-4 w-2/3 rounded-full bg-surface-container-high"></div>
      </div>

      <div v-else-if="preview" class="app-shell team-main-shell">
        <div class="mb-8 flex items-center gap-4">
          <div class="flex h-14 w-14 items-center justify-center rounded-[1.3rem] bg-primary text-white">
            <span class="material-symbols-outlined text-2xl">groups</span>
          </div>
          <div>
            <h1 class="font-display-serif text-4xl text-on-surface">{{ preview.spaceName }}</h1>
            <p class="text-body-secondary text-on-surface-variant">{{ t('shared.teamInviteFrom', { owner: preview.ownerUsername }) }}</p>
          </div>
        </div>

        <div class="grid gap-4 md:grid-cols-3">
          <div class="app-card bg-surface">
            <div class="text-caption uppercase tracking-[0.22em] text-on-surface-variant">{{ t('members.role') }}</div>
            <div class="mt-3 font-title-serif text-2xl text-on-surface">{{ preview.roleCode }}</div>
          </div>
          <div class="app-card bg-surface">
            <div class="text-caption uppercase tracking-[0.22em] text-on-surface-variant">{{ t('shared.memberCount') }}</div>
            <div class="mt-3 font-title-serif text-2xl text-on-surface">{{ preview.memberCount }}</div>
          </div>
          <div class="app-card bg-surface">
            <div class="text-caption uppercase tracking-[0.22em] text-on-surface-variant">{{ t('shared.expiresAt') }}</div>
            <div class="mt-3 font-title-serif text-2xl text-on-surface">{{ formatDateTime(preview.expiresAt) }}</div>
          </div>
        </div>

        <div class="mt-8 flex flex-wrap gap-3">
          <button class="app-primary-button" type="button" :disabled="joining" @click="joinTeam">
            <span v-if="joining" class="material-symbols-outlined animate-spin text-base">progress_activity</span>
            {{ t('shared.acceptTeamInvite') }}
          </button>
          <RouterLink class="app-secondary-button" to="/login">{{ t('shared.loginFirst') }}</RouterLink>
        </div>
      </div>

      <div v-else class="app-shell">
        <h1 class="font-title-serif text-2xl text-on-surface">{{ t('shared.inviteMissingTitle') }}</h1>
        <p class="mt-3 text-body-secondary text-on-surface-variant">{{ t('shared.inviteMissingDescription') }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { spaceService } from '@/api/services'
import { useI18n } from '@/i18n'
import type { SpaceInvitePreview } from '@/types/app'
import { useSpaceStore } from '@/stores/space'
import { useUserStore } from '@/stores/user'
import { formatDateTime } from '@/utils/date'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const spaceStore = useSpaceStore()
const { t } = useI18n()

const loading = ref(false)
const joining = ref(false)
const preview = ref<SpaceInvitePreview | null>(null)

const loadPreview = async () => {
  loading.value = true
  try {
    preview.value = await spaceService.previewInvite(route.params.teamCode as string)
  } finally {
    loading.value = false
  }
}

const joinTeam = async () => {
  if (!preview.value) {
    return
  }

  if (!userStore.isAuthenticated) {
    await router.push({
      path: '/register',
      query: {
        inviteCode: route.params.teamCode as string,
      },
    })
    return
  }

  joining.value = true
  try {
    await spaceService.joinInvite(route.params.teamCode as string)
    await spaceStore.refreshSpaces()
    await spaceStore.setCurrentSpace(preview.value.spaceId)
    await router.push('/app/projects')
  } finally {
    joining.value = false
  }
}

onMounted(() => {
  loadPreview().catch(() => undefined)
})
</script>
