<template>
  <el-popover placement="bottom-end" :width="340" trigger="click">
    <template #reference>
      <button
        class="flex items-center rounded-full border px-4 py-1.5 transition-colors"
        :class="
          isTeam
            ? 'border-[#0077B6]/15 bg-white/55 text-[#0077B6] hover:bg-white/75'
            : 'border-primary-container/20 bg-primary-container/10 text-primary hover:bg-primary-container/20'
        "
        type="button"
      >
        <span class="mr-2 text-label-bold">{{ spaceStore.currentSpace?.name || t('spaceSwitcher.fallbackSpaceName') }}</span>
        <span class="material-symbols-outlined text-sm">unfold_more</span>
      </button>
    </template>

    <div class="space-y-3">
      <div>
        <div class="mb-2 px-1 text-caption uppercase tracking-[0.24em] text-on-surface-variant">{{ t('spaceSwitcher.mySpaces') }}</div>
        <button
          v-for="space in spaceStore.spaces"
          :key="space.id"
          class="flex w-full items-center justify-between rounded-2xl px-3 py-3 text-left transition hover:bg-surface-container-low"
          :class="space.id === spaceStore.currentSpaceId ? 'bg-surface-container-low' : ''"
          type="button"
          @click="selectSpace(space.id)"
        >
          <div class="flex min-w-0 items-center gap-3">
            <span
              class="material-symbols-outlined shrink-0 text-base"
              :class="space.type === 'TEAM' ? 'text-primary' : 'text-[#ff8a65]'"
            >
              {{ space.type === 'TEAM' ? 'groups' : 'book_ribbon' }}
            </span>
            <div class="min-w-0">
              <div class="truncate font-label-bold text-on-surface">{{ space.name }}</div>
              <div class="text-caption text-on-surface-variant">
                {{ space.type === 'TEAM' ? t('spaceSwitcher.memberCount', { count: space.memberCount ?? 0 }) : t('spaceSwitcher.personalSpace') }}
              </div>
            </div>
          </div>
          <span v-if="space.id === spaceStore.currentSpaceId" class="material-symbols-outlined text-primary">check</span>
        </button>
      </div>

      <div class="grid gap-3">
        <div class="rounded-2xl border border-dashed border-outline-variant/50 p-3">
          <div class="font-label-bold text-on-surface">{{ t('spaceSwitcher.createTeamTitle') }}</div>
          <p class="mt-1 text-body-secondary text-on-surface-variant">{{ t('spaceSwitcher.createTeamDescription') }}</p>
          <button class="app-primary-button mt-3 w-full justify-center" type="button" @click="createDialogOpen = true">
            <span class="material-symbols-outlined text-base">add</span>
            {{ t('spaceSwitcher.createTeamButton') }}
          </button>
        </div>

        <div class="rounded-2xl border border-dashed border-outline-variant/50 p-3">
          <div class="font-label-bold text-on-surface">{{ t('spaceSwitcher.joinTeamTitle') }}</div>
          <p class="mt-1 text-body-secondary text-on-surface-variant">{{ t('spaceSwitcher.joinTeamDescription') }}</p>
          <button class="app-secondary-button mt-3 w-full justify-center" type="button" @click="joinDialogOpen = true">
            <span class="material-symbols-outlined text-base">group_add</span>
            {{ t('spaceSwitcher.joinTeamButton') }}
          </button>
        </div>
      </div>
    </div>
  </el-popover>

  <el-dialog v-model="createDialogOpen" append-to-body :title="t('spaceSwitcher.createDialogTitle')" width="420px">
    <div class="space-y-4">
      <label class="block">
        <span class="mb-2 block text-label-bold text-on-surface">{{ t('spaceSwitcher.spaceName') }}</span>
        <input v-model="teamName" class="app-input w-full px-4 py-3" :placeholder="t('spaceSwitcher.spaceNamePlaceholder')" />
      </label>
    </div>
    <template #footer>
      <button class="app-secondary-button" type="button" @click="createDialogOpen = false">{{ t('common.cancel') }}</button>
      <button class="app-primary-button" type="button" @click="createSpace">{{ t('common.create') }}</button>
    </template>
  </el-dialog>

  <el-dialog v-model="joinDialogOpen" append-to-body :title="t('spaceSwitcher.joinDialogTitle')" width="460px">
    <div class="space-y-5">
      <div class="grid grid-cols-2 gap-2 rounded-2xl bg-surface-container-low p-1">
        <button
          class="rounded-xl px-4 py-2 text-sm font-semibold transition-all"
          :class="joinMode === 'invite' ? 'bg-white text-primary shadow-sm' : 'text-on-surface-variant hover:text-primary'"
          type="button"
          @click="joinMode = 'invite'"
        >
          {{ t('spaceSwitcher.inviteJoin') }}
        </button>
        <button
          class="rounded-xl px-4 py-2 text-sm font-semibold transition-all"
          :class="joinMode === 'apply' ? 'bg-white text-primary shadow-sm' : 'text-on-surface-variant hover:text-primary'"
          type="button"
          @click="joinMode = 'apply'"
        >
          {{ t('spaceSwitcher.applyJoin') }}
        </button>
      </div>

      <template v-if="joinMode === 'invite'">
        <label class="block">
          <span class="mb-2 block text-label-bold text-on-surface">{{ t('spaceSwitcher.inviteCode') }}</span>
          <input
            v-model="joinInviteCode"
            class="app-input w-full px-4 py-3"
            :placeholder="t('spaceSwitcher.inviteCodePlaceholder')"
          />
        </label>
        <p class="text-body-secondary text-on-surface-variant">
          {{ t('spaceSwitcher.inviteHelp') }}
        </p>
      </template>

      <template v-else>
        <label class="block">
          <span class="mb-2 block text-label-bold text-on-surface">{{ t('spaceSwitcher.supervisorAccount') }}</span>
          <input
            v-model="joinForm.supervisorAccount"
            class="app-input w-full px-4 py-3"
            :placeholder="t('spaceSwitcher.supervisorAccountPlaceholder')"
          />
        </label>

        <label class="block">
          <span class="mb-2 block text-label-bold text-on-surface">{{ t('spaceSwitcher.teamName') }}</span>
          <input
            v-model="joinForm.teamName"
            class="app-input w-full px-4 py-3"
            :placeholder="t('spaceSwitcher.teamNamePlaceholder')"
          />
        </label>

        <label class="block">
          <span class="mb-2 block text-label-bold text-on-surface">{{ t('spaceSwitcher.applicationRemark') }}</span>
          <textarea
            v-model="joinForm.remark"
            rows="4"
            class="app-input w-full resize-none px-4 py-3"
            :placeholder="t('spaceSwitcher.applicationRemarkPlaceholder')"
          ></textarea>
        </label>
      </template>
    </div>

    <template #footer>
      <button class="app-secondary-button" type="button" @click="closeJoinDialog">{{ t('common.cancel') }}</button>
      <button class="app-primary-button" type="button" :disabled="joinLoading" @click="submitJoinTeam">
        {{ joinMode === 'invite' ? t('spaceSwitcher.goPreview') : t('spaceSwitcher.submitApplication') }}
      </button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, nextTick, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { teamApplicationService } from '@/api/services'
import { useI18n } from '@/i18n'
import { useSpaceStore } from '@/stores/space'
import { getLandingPath, needsTeamRedirect } from '@/utils/space'

const spaceStore = useSpaceStore()
const router = useRouter()
const route = useRoute()
const { t } = useI18n()

const createDialogOpen = ref(false)
const joinDialogOpen = ref(false)
const joinMode = ref<'invite' | 'apply'>('invite')
const joinLoading = ref(false)
const teamName = ref('')
const joinInviteCode = ref('')
const joinForm = reactive({
  supervisorAccount: '',
  teamName: '',
  remark: '',
})

const isTeam = computed(() => spaceStore.currentSpace?.type === 'TEAM')

const resetJoinForm = () => {
  joinMode.value = 'invite'
  joinInviteCode.value = ''
  joinForm.supervisorAccount = ''
  joinForm.teamName = ''
  joinForm.remark = ''
  joinLoading.value = false
}

const closeJoinDialog = () => {
  joinDialogOpen.value = false
  resetJoinForm()
}

const selectSpace = async (spaceId: number) => {
  if (spaceStore.currentSpaceId === spaceId) {
    return
  }

  spaceStore.beginSpaceSwitch(spaceId)
  try {
    await spaceStore.setCurrentSpace(spaceId)
    const currentSpace = spaceStore.currentSpace

    if (!currentSpace) {
      return
    }

    if (route.path !== getLandingPath(currentSpace) || needsTeamRedirect(route, currentSpace)) {
      await router.push(getLandingPath(currentSpace))
    }
    await nextTick()
    await new Promise((resolve) => {
      window.setTimeout(resolve, 450)
    })
  } finally {
    spaceStore.finishSpaceSwitch(spaceId)
  }
}

const createSpace = async () => {
  const name = teamName.value.trim()
  if (!name) {
    ElMessage.warning(t('messages.fillSpaceName'))
    return
  }

  await spaceStore.createTeamSpace(name)
  teamName.value = ''
  createDialogOpen.value = false
  await router.push(getLandingPath(spaceStore.currentSpace))
}

const submitJoinTeam = async () => {
  if (joinLoading.value) {
    return
  }

  if (joinMode.value === 'invite') {
    const inviteCode = joinInviteCode.value.trim()
    if (!inviteCode) {
      ElMessage.warning(t('messages.fillInviteCode'))
      return
    }

    closeJoinDialog()
    await router.push(`/invite/${inviteCode}`)
    return
  }

  const supervisorAccount = joinForm.supervisorAccount.trim()
  if (!supervisorAccount) {
    ElMessage.warning(t('messages.fillSupervisor'))
    return
  }

  joinLoading.value = true
  try {
    await teamApplicationService.apply({
      supervisorAccount,
      teamName: joinForm.teamName.trim() || undefined,
      remark: joinForm.remark.trim() || undefined,
    })
    ElMessage.success(t('messages.joinApplicationSubmitted'))
    closeJoinDialog()
  } finally {
    joinLoading.value = false
  }
}
</script>
