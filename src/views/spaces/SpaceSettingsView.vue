<template>
  <div class="space-y-6">
    <div>
      <h1 class="font-display-serif text-5xl text-on-surface">{{ t('members.title') }}</h1>
      <p class="mt-2 text-body-secondary text-on-surface-variant">
        {{ t('members.description') }}
      </p>
    </div>

    <section class="app-shell team-shell space-y-5">
      <div class="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
        <div>
          <h2 class="font-title-serif text-3xl text-on-surface">{{ t('members.basicInfo') }}</h2>
          <p class="mt-2 text-body-secondary text-on-surface-variant">{{ t('members.basicInfoDescription') }}</p>
        </div>
        <div class="flex flex-wrap gap-3">
          <button class="app-primary-button" type="button" :disabled="!canManageMembers" @click="saveSpaceName">
            {{ t('common.saveChanges') }}
          </button>
          <button class="app-secondary-button" type="button" :disabled="!isSpaceOwner" @click="confirmDeleteSpace">
            {{ t('members.dissolveSpace') }}
          </button>
        </div>
      </div>

      <div class="grid gap-5 lg:grid-cols-[1.2fr_0.8fr_0.8fr]">
        <label class="block">
          <span class="mb-2 block text-label-bold text-on-surface">{{ t('members.teamName') }}</span>
          <input v-model="spaceName" class="app-input w-full px-4 py-3" :disabled="!canManageMembers" />
        </label>

        <div class="rounded-[1.5rem] bg-white/60 px-5 py-4">
          <div class="text-caption uppercase tracking-[0.2em] text-on-surface-variant">{{ t('members.owner') }}</div>
          <div class="mt-3 text-lg font-medium text-on-surface">{{ ownerDisplayName }}</div>
        </div>

        <div class="rounded-[1.5rem] bg-white/60 px-5 py-4">
          <div class="text-caption uppercase tracking-[0.2em] text-on-surface-variant">{{ t('members.createdAt') }}</div>
          <div class="mt-3 text-lg font-medium text-on-surface">{{ createdAtDisplay }}</div>
        </div>
      </div>
    </section>

    <div class="grid gap-6">
      <section class="app-shell team-shell space-y-5">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h2 class="font-title-serif text-3xl text-on-surface">{{ t('members.memberManagement') }}</h2>
            <p class="mt-2 text-body-secondary text-on-surface-variant">
              {{ t('members.memberManagementDescription') }}
            </p>
          </div>

          <div class="flex flex-col gap-3 md:flex-row md:items-center">
            <label class="app-input flex min-w-[280px] items-center gap-3 px-4 py-3.5">
              <span class="material-symbols-outlined text-on-surface-variant">search</span>
              <input
                v-model="memberSearchKeyword"
                class="w-full border-none bg-transparent p-0 outline-none focus:ring-0"
                :placeholder="t('members.searchMemberPlaceholder')"
              />
            </label>

            <el-select v-model="memberRoleFilter" clearable class="min-w-[180px]" :placeholder="t('members.roleFilter')">
              <el-option v-for="option in roleOptions" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>

            <button
              class="app-primary-button min-w-[132px] justify-center px-6 py-3.5"
              type="button"
              :disabled="!canManageMembers"
              @click="inviteDialogOpen = true"
            >
              {{ t('members.inviteMember') }}
            </button>
          </div>
        </div>

        <div class="overflow-hidden rounded-[1.5rem] border border-outline-variant/20 bg-white/55">
          <div
            class="hidden items-center gap-4 border-b border-outline-variant/15 px-5 py-4 text-[11px] font-bold uppercase tracking-[0.18em] text-on-surface-variant lg:grid"
            style="grid-template-columns: 80px minmax(0,1.1fr) minmax(0,1.1fr) 180px 160px 120px;"
          >
            <span>{{ t('members.avatar') }}</span>
            <span>{{ t('members.name') }}</span>
            <span>{{ t('common.email') }}</span>
            <span>{{ t('members.role') }}</span>
            <span>{{ t('members.joinedAt') }}</span>
            <span>{{ t('members.actions') }}</span>
          </div>

          <div v-if="filteredMembers.length" class="divide-y divide-outline-variant/10">
            <article
              v-for="member in filteredMembers"
              :key="member.userId"
              class="grid gap-4 px-5 py-4 lg:items-center"
              style="grid-template-columns: 1fr;"
            >
              <div class="flex items-center gap-4 lg:hidden">
                <div class="shrink-0">
                  <img
                    v-if="member.avatarUrl"
                    :src="member.avatarUrl"
                    :alt="member.username"
                    class="h-12 w-12 rounded-full border border-white object-cover shadow-sm"
                  />
                  <div
                    v-else
                    class="flex h-12 w-12 items-center justify-center rounded-full border border-white bg-primary-fixed text-sm font-bold text-primary shadow-sm"
                  >
                    {{ resolveInitial(member.nickname || member.username) }}
                  </div>
                </div>
                <div class="min-w-0 flex-1">
                  <div class="font-medium text-on-surface">{{ member.nickname || member.username }}</div>
                  <div class="truncate text-body-secondary text-on-surface-variant">{{ member.email || t('members.noEmail') }}</div>
                </div>
              </div>

              <div
                class="hidden items-center gap-4 lg:grid"
                style="grid-template-columns: 80px minmax(0,1.1fr) minmax(0,1.1fr) 180px 160px 120px;"
              >
                <div>
                  <img
                    v-if="member.avatarUrl"
                    :src="member.avatarUrl"
                    :alt="member.username"
                    class="h-11 w-11 rounded-full border border-white object-cover shadow-sm"
                  />
                  <div
                    v-else
                    class="flex h-11 w-11 items-center justify-center rounded-full border border-white bg-primary-fixed text-sm font-bold text-primary shadow-sm"
                  >
                    {{ resolveInitial(member.nickname || member.username) }}
                  </div>
                </div>
                <div class="min-w-0 font-medium text-on-surface">{{ member.nickname || member.username }}</div>
                <div class="min-w-0 truncate text-body-secondary text-on-surface-variant">{{ member.email || t('members.noEmail') }}</div>
                <el-select
                  :model-value="member.roleCode"
                  class="w-full"
                  :disabled="!canManageMembers || member.roleCode === 'SPACE_OWNER'"
                  @change="changeMemberRole(member, $event)"
                >
                  <el-option v-for="option in editableRoleOptions" :key="option.value" :label="option.label" :value="option.value" />
                </el-select>
                <div class="text-body-secondary text-on-surface-variant">{{ formatDateTime(member.gmtJoined) }}</div>
                <div>
                  <button
                    class="app-secondary-button px-4 py-2"
                    type="button"
                    :disabled="!canManageMembers || member.roleCode === 'SPACE_OWNER'"
                    @click="confirmRemoveMember(member)"
                  >
                    {{ t('common.remove') }}
                  </button>
                </div>
              </div>

              <div class="grid gap-3 lg:hidden">
                <div class="text-body-secondary text-on-surface-variant">{{ t('members.joinedAtInline', { time: formatDateTime(member.gmtJoined) }) }}</div>
                <el-select
                  :model-value="member.roleCode"
                  class="w-full"
                  :disabled="!canManageMembers || member.roleCode === 'SPACE_OWNER'"
                  @change="changeMemberRole(member, $event)"
                >
                  <el-option v-for="option in editableRoleOptions" :key="option.value" :label="option.label" :value="option.value" />
                </el-select>
                <button
                  class="app-secondary-button"
                  type="button"
                  :disabled="!canManageMembers || member.roleCode === 'SPACE_OWNER'"
                  @click="confirmRemoveMember(member)"
                >
                  {{ t('common.remove') }}
                </button>
              </div>
            </article>
          </div>

          <div v-else class="flex min-h-[220px] items-center justify-center px-5 py-10 text-center text-sm text-on-surface-variant">
            {{ t('members.noMembers') }}
          </div>
        </div>
      </section>

      <section class="app-shell team-shell space-y-5">
        <div>
          <h2 class="font-title-serif text-3xl text-on-surface">{{ t('members.pendingRequests') }}</h2>
          <p class="mt-2 text-body-secondary text-on-surface-variant">
            {{ t('members.pendingRequestsDescription') }}
          </p>
        </div>

        <div class="overflow-hidden rounded-[1.5rem] border border-outline-variant/20 bg-white/55">
          <div
            class="hidden items-center gap-4 border-b border-outline-variant/15 px-5 py-4 text-[11px] font-bold uppercase tracking-[0.18em] text-on-surface-variant lg:grid"
            style="grid-template-columns: 80px minmax(0,1fr) minmax(0,1fr) 160px minmax(0,1.2fr) 170px;"
          >
            <span>{{ t('members.avatar') }}</span>
            <span>{{ t('members.applicant') }}</span>
            <span>{{ t('common.email') }}</span>
            <span>{{ t('members.appliedAt') }}</span>
            <span>{{ t('members.reason') }}</span>
            <span>{{ t('members.actions') }}</span>
          </div>

          <div v-if="visiblePendingRequests.length" class="divide-y divide-outline-variant/10">
            <article
              v-for="request in visiblePendingRequests"
              :key="request.id"
              class="grid gap-4 px-5 py-4 lg:items-center"
              style="grid-template-columns: 1fr;"
            >
              <div class="flex items-center gap-4 lg:hidden">
                <div class="flex h-12 w-12 items-center justify-center rounded-full border border-white bg-primary-fixed text-sm font-bold text-primary shadow-sm">
                  {{ resolveInitial(request.applicantUsername) }}
                </div>
                <div class="min-w-0 flex-1">
                  <div class="font-medium text-on-surface">{{ request.applicantUsername }}</div>
                  <div class="truncate text-body-secondary text-on-surface-variant">{{ request.applicantEmail || t('members.noEmail') }}</div>
                </div>
              </div>

              <div
                class="hidden items-center gap-4 lg:grid"
                style="grid-template-columns: 80px minmax(0,1fr) minmax(0,1fr) 160px minmax(0,1.2fr) 170px;"
              >
                <div class="flex h-11 w-11 items-center justify-center rounded-full border border-white bg-primary-fixed text-sm font-bold text-primary shadow-sm">
                  {{ resolveInitial(request.applicantUsername) }}
                </div>
                <div class="min-w-0 font-medium text-on-surface">{{ request.applicantUsername }}</div>
                <div class="min-w-0 truncate text-body-secondary text-on-surface-variant">{{ request.applicantEmail || t('members.noEmail') }}</div>
                <div class="text-body-secondary text-on-surface-variant">{{ formatDateTime(request.gmtCreate) }}</div>
                <div class="min-w-0 text-body-secondary text-on-surface-variant">
                  {{ request.remark || t('members.noReason') }}
                </div>
                <div class="flex flex-wrap gap-2">
                  <button class="app-primary-button px-4 py-2" type="button" @click="approveRequest(request)">{{ t('common.approve') }}</button>
                  <button class="app-secondary-button px-4 py-2" type="button" @click="rejectRequest(request)">{{ t('common.reject') }}</button>
                </div>
              </div>

              <div class="grid gap-3 lg:hidden">
                <div class="text-body-secondary text-on-surface-variant">{{ t('members.appliedAtInline', { time: formatDateTime(request.gmtCreate) }) }}</div>
                <div class="rounded-[1.2rem] bg-surface px-4 py-3 text-body-secondary text-on-surface-variant">
                  {{ request.remark || t('members.noReason') }}
                </div>
                <div class="flex gap-2">
                  <button class="app-primary-button flex-1 justify-center" type="button" @click="approveRequest(request)">{{ t('common.approve') }}</button>
                  <button class="app-secondary-button flex-1 justify-center" type="button" @click="rejectRequest(request)">{{ t('common.reject') }}</button>
                </div>
              </div>
            </article>
          </div>

          <div class="flex min-h-[220px] items-center justify-center px-5 py-10 text-center text-sm text-on-surface-variant" v-else>
            {{ t('members.noPendingRequests') }}
          </div>
        </div>
      </section>

      <section class="app-shell team-shell space-y-5">
        <div class="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
          <div>
            <h2 class="font-title-serif text-3xl text-on-surface">{{ t('members.advancedSettings') }}</h2>
            <p class="mt-2 text-body-secondary text-on-surface-variant">
              {{ t('members.advancedDescription') }}
            </p>
          </div>
          <button class="app-primary-button" type="button" :disabled="!canManageMembers" @click="saveAdvancedSettings">
            {{ t('common.saveChanges') }}
          </button>
        </div>

        <div class="grid gap-5 lg:grid-cols-2">
          <div class="rounded-[1.5rem] bg-white/60 px-5 py-5">
            <div class="flex items-center justify-between gap-4">
              <div>
                <div class="font-medium text-on-surface">{{ t('members.joinApprovalRequired') }}</div>
                <div class="mt-2 text-body-secondary text-on-surface-variant">
                  {{ t('members.joinApprovalRequiredHint') }}
                </div>
              </div>
              <el-switch v-model="advancedSettings.joinApprovalRequired" :disabled="!canManageMembers" />
            </div>
          </div>

          <div class="rounded-[1.5rem] bg-white/60 px-5 py-5">
            <div class="flex items-center justify-between gap-4">
              <div>
                <div class="font-medium text-on-surface">{{ t('members.memberInviteEnabled') }}</div>
                <div class="mt-2 text-body-secondary text-on-surface-variant">
                  {{ t('members.memberInviteEnabledHint') }}
                </div>
              </div>
              <el-switch v-model="advancedSettings.memberInviteEnabled" :disabled="!canManageMembers" />
            </div>
          </div>
        </div>

        <div class="border-t border-outline-variant/20 pt-4">
          <div class="flex items-start gap-3 rounded-[1.25rem] bg-surface px-4 py-4 text-body-secondary text-on-surface-variant">
            <span class="material-symbols-outlined mt-0.5 text-[18px] text-primary">info</span>
            <span>{{ t('members.advancedWarning') }}</span>
          </div>
        </div>
      </section>
    </div>

    <el-dialog v-model="inviteDialogOpen" append-to-body :title="t('members.inviteDialogTitle')" width="520px">
      <div class="space-y-4">
        <label class="block">
          <span class="mb-2 block text-label-bold text-on-surface">{{ t('members.searchUser') }}</span>
          <el-select
            v-model="inviteForm.userId"
            filterable
            remote
            reserve-keyword
            class="w-full"
            :placeholder="t('members.searchUserPlaceholder')"
            :loading="inviteLoading"
            :remote-method="searchInviteCandidates"
          >
            <el-option
              v-for="user in inviteCandidates"
              :key="user.id"
              :label="user.nickname || user.username"
              :value="user.id"
            >
              <div class="flex flex-col">
                <span>{{ user.nickname || user.username }}</span>
                <span class="text-xs text-on-surface-variant">{{ user.email || user.username }}</span>
              </div>
            </el-option>
          </el-select>
        </label>

        <label class="block">
          <span class="mb-2 block text-label-bold text-on-surface">{{ t('members.role') }}</span>
          <el-select v-model="inviteForm.roleCode" class="w-full">
            <el-option v-for="option in editableRoleOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </label>
      </div>

      <template #footer>
        <button class="app-secondary-button" type="button" @click="inviteDialogOpen = false">{{ t('common.cancel') }}</button>
        <button class="app-primary-button" type="button" @click="submitInviteMember">{{ t('members.inviteMember') }}</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { teamApplicationService, userService } from '@/api/services'
import { useI18n } from '@/i18n'
import { useSpaceStore } from '@/stores/space'
import { useUserStore } from '@/stores/user'
import type { SpaceJoinRequest, SpaceMember, UserOption } from '@/types/app'
import { formatDateTime } from '@/utils/date'
import { getLandingPath } from '@/utils/space'

interface AdvancedSettingsState {
  joinApprovalRequired: boolean
  memberInviteEnabled: boolean
}

const ADVANCED_SETTINGS_STORAGE_KEY = 'notask-flow-team-advanced-settings'

const router = useRouter()
const spaceStore = useSpaceStore()
const userStore = useUserStore()
const { t } = useI18n()

const spaceName = ref('')
const memberSearchKeyword = ref('')
const memberRoleFilter = ref<string>()
const pendingRequests = ref<SpaceJoinRequest[]>([])
const inviteDialogOpen = ref(false)
const inviteLoading = ref(false)
const inviteCandidates = ref<UserOption[]>([])
const inviteForm = reactive({
  userId: undefined as number | undefined,
  roleCode: 'SPACE_MEMBER',
})
const advancedSettings = reactive<AdvancedSettingsState>({
  joinApprovalRequired: true,
  memberInviteEnabled: false,
})

const roleOptions = computed(() => [
  { label: t('members.roles.admin'), value: 'SPACE_ADMIN' },
  { label: t('members.roles.member'), value: 'SPACE_MEMBER' },
  { label: t('members.roles.guest'), value: 'SPACE_GUEST' },
])

const editableRoleOptions = computed(() => [
  { label: t('members.roles.admin'), value: 'SPACE_ADMIN' },
  { label: t('members.roles.member'), value: 'SPACE_MEMBER' },
  { label: t('members.roles.guest'), value: 'SPACE_GUEST' },
])

const canManageMembers = computed(() => spaceStore.permissions.includes('space:member:manage'))
const isSpaceOwner = computed(() => spaceStore.currentSpace?.ownerUserId === userStore.profile?.id)
const ownerDisplayName = computed(() => {
  const ownerId = spaceStore.currentSpace?.ownerUserId
  const owner = spaceStore.members.find((member) => member.userId === ownerId)
  return owner?.nickname || owner?.username || t('common.notRecorded')
})
const createdAtDisplay = computed(() =>
  spaceStore.currentSpace?.gmtCreate ? formatDateTime(spaceStore.currentSpace.gmtCreate) : t('common.notRecorded'),
)

const filteredMembers = computed(() => {
  const keyword = memberSearchKeyword.value.trim().toLowerCase()

  return spaceStore.members.filter((member) => {
    const matchesKeyword = !keyword
      || `${member.nickname || ''} ${member.username || ''} ${member.email || ''}`.toLowerCase().includes(keyword)
    const matchesRole = !memberRoleFilter.value || member.roleCode === memberRoleFilter.value
    return matchesKeyword && matchesRole
  })
})

const visiblePendingRequests = computed(() => {
  const currentSpaceId = spaceStore.currentSpaceId
  const currentSpaceName = spaceStore.currentSpace?.name?.trim()
  return pendingRequests.value.filter((request) => {
    if (request.targetSpaceId) {
      return request.targetSpaceId === currentSpaceId
    }
    if (!request.teamName || !currentSpaceName) {
      return true
    }
    return request.teamName.trim() === currentSpaceName
  })
})

const loadAdvancedSettings = () => {
  const spaceId = spaceStore.currentSpaceId
  if (!spaceId) {
    return
  }

  const rawValue = window.localStorage.getItem(`${ADVANCED_SETTINGS_STORAGE_KEY}:${spaceId}`)
  if (!rawValue) {
    advancedSettings.joinApprovalRequired = true
    advancedSettings.memberInviteEnabled = false
    return
  }

  try {
    const parsed = JSON.parse(rawValue) as Partial<AdvancedSettingsState>
    advancedSettings.joinApprovalRequired = parsed.joinApprovalRequired ?? true
    advancedSettings.memberInviteEnabled = parsed.memberInviteEnabled ?? false
  } catch {
    advancedSettings.joinApprovalRequired = true
    advancedSettings.memberInviteEnabled = false
  }
}

const hydrate = async () => {
  spaceName.value = spaceStore.currentSpace?.name || ''
  pendingRequests.value = await teamApplicationService.pending()
  await spaceStore.loadMembers()
  loadAdvancedSettings()
}

const saveSpaceName = async () => {
  await spaceStore.updateCurrentSpace(spaceName.value.trim())
}

const saveAdvancedSettings = async () => {
  if (!spaceStore.currentSpaceId) {
    return
  }

  window.localStorage.setItem(
    `${ADVANCED_SETTINGS_STORAGE_KEY}:${spaceStore.currentSpaceId}`,
    JSON.stringify({
      joinApprovalRequired: advancedSettings.joinApprovalRequired,
      memberInviteEnabled: advancedSettings.memberInviteEnabled,
    }),
  )
  ElMessage.success(t('messages.advancedSettingsSaved'))
}

const resolveInitial = (value?: string) => (value || '?').slice(0, 1).toUpperCase()

const changeMemberRole = async (member: SpaceMember, roleCode: string | number | boolean) => {
  const nextRoleCode = String(roleCode)
  if (nextRoleCode === member.roleCode) {
    return
  }

  const roleLabel = roleOptions.value.find((option) => option.value === nextRoleCode)?.label || nextRoleCode
  const confirmed = await ElMessageBox.confirm(
    t('confirm.changeMemberRoleMessage', { name: member.nickname || member.username, role: roleLabel }),
    t('confirm.changeMemberRoleTitle'),
    {
      confirmButtonText: t('confirm.confirmChange'),
      cancelButtonText: t('confirm.cancel'),
      type: 'warning',
    },
  ).catch(() => false)

  if (!confirmed) {
    return
  }

  await spaceStore.updateMemberRole(member.userId, nextRoleCode)
}

const confirmRemoveMember = async (member: SpaceMember) => {
  const confirmed = await ElMessageBox.confirm(
    t('confirm.removeMemberMessage', { name: member.nickname || member.username }),
    t('confirm.removeMemberTitle'),
    {
      confirmButtonText: t('confirm.confirmRemove'),
      cancelButtonText: t('confirm.cancel'),
      type: 'warning',
    },
  ).catch(() => false)

  if (!confirmed) {
    return
  }

  await spaceStore.removeMember(member.userId)
}

const approveRequest = async (request: SpaceJoinRequest) => {
  if (!spaceStore.currentSpaceId) {
    return
  }

  const confirmed = await ElMessageBox.confirm(
    t('confirm.approveRequestMessage', { name: request.applicantUsername }),
    t('confirm.approveRequestTitle'),
    {
      confirmButtonText: t('confirm.confirmApprove'),
      cancelButtonText: t('confirm.cancel'),
      type: 'warning',
    },
  ).catch(() => false)

  if (!confirmed) {
    return
  }

  await teamApplicationService.approve(request.id, {
    spaceId: spaceStore.currentSpaceId,
    roleCode: 'SPACE_MEMBER',
  })
  await hydrate()
}

const rejectRequest = async (request: SpaceJoinRequest) => {
  const confirmed = await ElMessageBox.confirm(
    t('confirm.rejectRequestMessage', { name: request.applicantUsername }),
    t('confirm.rejectRequestTitle'),
    {
      confirmButtonText: t('confirm.confirmReject'),
      cancelButtonText: t('confirm.cancel'),
      type: 'warning',
    },
  ).catch(() => false)

  if (!confirmed) {
    return
  }

  await teamApplicationService.reject(request.id, {})
  await hydrate()
}

const searchInviteCandidates = async (keyword: string) => {
  const normalizedKeyword = keyword.trim()
  if (!normalizedKeyword) {
    inviteCandidates.value = []
    return
  }

  inviteLoading.value = true
  try {
    const members = await userService.search(normalizedKeyword)
    const existingMemberIds = new Set(spaceStore.members.map((member) => member.userId))
    inviteCandidates.value = members.filter((member) => !existingMemberIds.has(member.id))
  } finally {
    inviteLoading.value = false
  }
}

const submitInviteMember = async () => {
  if (!inviteForm.userId) {
    return
  }

  await spaceStore.addMember(inviteForm.userId, inviteForm.roleCode)
  inviteDialogOpen.value = false
  inviteForm.userId = undefined
  inviteForm.roleCode = 'SPACE_MEMBER'
  inviteCandidates.value = []
  await hydrate()
}

const confirmDeleteSpace = async () => {
  if (!spaceStore.currentSpaceId) {
    return
  }

  const confirmed = await ElMessageBox.confirm(
    t('confirm.deleteSpaceMessage'),
    t('confirm.deleteSpaceTitle'),
    {
      confirmButtonText: t('confirm.confirmDissolve'),
      cancelButtonText: t('confirm.cancel'),
      type: 'warning',
    },
  ).catch(() => false)

  if (!confirmed) {
    return
  }

  const nextSpace = await spaceStore.deleteCurrentSpace()
  await router.push(getLandingPath(nextSpace))
}

watch(
  () => spaceStore.currentSpaceId,
  () => {
    hydrate().catch(() => undefined)
  },
  { immediate: true },
)
</script>
