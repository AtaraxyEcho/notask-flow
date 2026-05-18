import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import { spaceService } from '@/api/services'
import { translate } from '@/i18n'
import type { Space, SpaceInvite, SpaceMember } from '@/types/app'
import { useUserStore } from './user'

interface SpaceState {
  spaces: Space[]
  currentSpaceId: number | null
  isSwitchingSpace: boolean
  permissions: string[]
  permissionsSpaceId: number | null
  members: SpaceMember[]
  membersSpaceId: number | null
  latestInvite: SpaceInvite | null
  switchingSpaceId: number | null
}

const MEMBER_PRESENCE_CLIENT_ID_KEY = 'notask-flow:member-presence-client-id'
const permissionCache = new Map<number, string[]>()
const memberCache = new Map<number, SpaceMember[]>()
const permissionRequestMap = new Map<number, Promise<string[]>>()
const memberRequestMap = new Map<number, Promise<SpaceMember[]>>()

const createRandomClientId = () => {
  if (window.crypto?.randomUUID) {
    return window.crypto.randomUUID()
  }

  return `client-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

const resolveMemberPresenceClientId = () => {
  try {
    const cachedClientId = window.sessionStorage.getItem(MEMBER_PRESENCE_CLIENT_ID_KEY)
    if (cachedClientId) {
      return cachedClientId
    }

    const clientId = createRandomClientId()
    window.sessionStorage.setItem(MEMBER_PRESENCE_CLIENT_ID_KEY, clientId)
    return clientId
  } catch (error) {
    return createRandomClientId()
  }
}

export const useSpaceStore = defineStore('space', {
  state: (): SpaceState => ({
    spaces: [],
    currentSpaceId: null,
    isSwitchingSpace: false,
    permissions: [],
    permissionsSpaceId: null,
    members: [],
    membersSpaceId: null,
    latestInvite: null,
    switchingSpaceId: null,
  }),
  getters: {
    currentSpace: (state) => state.spaces.find((space) => space.id === state.currentSpaceId) || null,
    switchingSpace: (state) => state.spaces.find((space) => space.id === state.switchingSpaceId) || null,
    isTeamSpace(): boolean {
      return this.currentSpace?.type === 'TEAM'
    },
  },
  actions: {
    beginSpaceSwitch(spaceId: number) {
      this.switchingSpaceId = spaceId
      this.isSwitchingSpace = true
    },
    finishSpaceSwitch(spaceId?: number) {
      if (spaceId && this.switchingSpaceId && this.switchingSpaceId !== spaceId) {
        return
      }

      this.isSwitchingSpace = false
      this.switchingSpaceId = null
    },
    async ensureLoaded() {
      if (!this.spaces.length) {
        this.spaces = await spaceService.list()
      }

      if (!this.currentSpaceId || !this.spaces.some((space) => space.id === this.currentSpaceId)) {
        this.currentSpaceId = this.spaces[0]?.id ?? null
      }

      this.applyCachedSpaceContext(this.currentSpaceId)
      this.preloadCurrentSpaceContext()
    },
    async refreshSpaces() {
      this.spaces = await spaceService.list()
      if (this.currentSpaceId && !this.spaces.some((space) => space.id === this.currentSpaceId)) {
        this.currentSpaceId = this.spaces[0]?.id ?? null
      }
      this.applyCachedSpaceContext(this.currentSpaceId)
      this.preloadCurrentSpaceContext()
    },
    async loadPermissions(spaceId: number) {
      let request = permissionRequestMap.get(spaceId)
      if (!request) {
        request = spaceService.permissions(spaceId)
        permissionRequestMap.set(spaceId, request)
        request.finally(() => {
          if (permissionRequestMap.get(spaceId) === request) {
            permissionRequestMap.delete(spaceId)
          }
        }).catch(() => undefined)
      }

      const permissions = await request
      permissionCache.set(spaceId, permissions)
      if (this.currentSpaceId === spaceId) {
        this.permissions = [...permissions]
        this.permissionsSpaceId = spaceId
      }
      return permissions
    },
    async setCurrentSpace(spaceId: number) {
      if (this.currentSpaceId === spaceId) {
        return
      }

      this.currentSpaceId = spaceId
      this.applyCachedSpaceContext(spaceId)
      this.preloadCurrentSpaceContext()
    },
    applyCachedSpaceContext(spaceId?: number | null) {
      if (!spaceId) {
        this.permissions = []
        this.permissionsSpaceId = null
        this.members = []
        this.membersSpaceId = null
        return
      }

      const cachedPermissions = permissionCache.get(spaceId)
      this.permissions = cachedPermissions ? [...cachedPermissions] : []
      this.permissionsSpaceId = cachedPermissions ? spaceId : null

      const targetSpace = this.spaces.find((space) => space.id === spaceId)
      if (targetSpace?.type === 'TEAM') {
        const cachedMembers = memberCache.get(spaceId)
        this.members = cachedMembers ? [...cachedMembers] : []
        this.membersSpaceId = cachedMembers ? spaceId : null
      } else {
        this.members = []
        this.membersSpaceId = spaceId
      }
    },
    preloadCurrentSpaceContext() {
      const spaceId = this.currentSpaceId
      if (!spaceId) {
        return
      }

      this.loadPermissions(spaceId).catch(() => undefined)
      if (this.currentSpace?.type === 'TEAM') {
        this.loadMembers(spaceId).catch(() => undefined)
      }
    },
    async loadMembers(spaceId?: number) {
      const targetSpaceId = spaceId ?? this.currentSpaceId
      if (!targetSpaceId) {
        return []
      }

      const targetSpace = this.spaces.find((space) => space.id === targetSpaceId)
      if (targetSpace?.type !== 'TEAM') {
        if (this.currentSpaceId === targetSpaceId) {
          this.members = []
          this.membersSpaceId = targetSpaceId
        }
        return []
      }

      let request = memberRequestMap.get(targetSpaceId)
      if (!request) {
        request = spaceService.members(targetSpaceId)
        memberRequestMap.set(targetSpaceId, request)
        request.finally(() => {
          if (memberRequestMap.get(targetSpaceId) === request) {
            memberRequestMap.delete(targetSpaceId)
          }
        }).catch(() => undefined)
      }

      const members = await request
      memberCache.set(targetSpaceId, members)
      if (this.currentSpaceId === targetSpaceId) {
        this.members = [...members]
        this.membersSpaceId = targetSpaceId
      }
      return members
    },
    async heartbeatMember(spaceId?: number) {
      const targetSpaceId = spaceId ?? this.currentSpaceId
      if (!targetSpaceId) {
        return
      }

      await spaceService.heartbeatMember(targetSpaceId, resolveMemberPresenceClientId())
    },
    async offlineMember(spaceId?: number) {
      const targetSpaceId = spaceId ?? this.currentSpaceId
      if (!targetSpaceId) {
        return
      }

      await spaceService.offlineMember(targetSpaceId, resolveMemberPresenceClientId())
    },
    sendOfflineMember(spaceId?: number) {
      const targetSpaceId = spaceId ?? this.currentSpaceId
      const tokenValue = useUserStore().tokenValue
      if (!targetSpaceId || !tokenValue) {
        return
      }

      void spaceService.sendOfflineMember(targetSpaceId, tokenValue, resolveMemberPresenceClientId())
    },
    async refreshMemberPresence(spaceId?: number) {
      const targetSpaceId = spaceId ?? this.currentSpaceId
      if (!targetSpaceId) {
        return []
      }

      this.heartbeatMember(targetSpaceId).catch(() => undefined)
      return this.loadMembers(targetSpaceId)
    },
    async createTeamSpace(name: string) {
      const space = await spaceService.create({ name })
      await this.refreshSpaces()
      await this.setCurrentSpace(space.id)
      ElMessage.success(translate('messages.teamSpaceCreated'))
      return space
    },
    async updateCurrentSpace(name: string) {
      if (!this.currentSpaceId) {
        return null
      }

      const updated = await spaceService.update(this.currentSpaceId, { name })
      await this.refreshSpaces()
      ElMessage.success(translate('messages.spaceNameUpdated'))
      return updated
    },
    async createInvite(roleCode = 'SPACE_MEMBER', expireMinutes = 1440) {
      if (!this.currentSpaceId) {
        throw new Error(translate('messages.spaceNotSelected'))
      }

      this.latestInvite = await spaceService.createInvite(this.currentSpaceId, {
        roleCode,
        expireMinutes,
      })
      ElMessage.success(translate('messages.inviteCodeGenerated'))
      return this.latestInvite
    },
    async updateMemberRole(userId: number, roleCode: string) {
      if (!this.currentSpaceId) {
        return
      }

      await spaceService.updateMemberRole(this.currentSpaceId, userId, roleCode)
      await this.loadMembers(this.currentSpaceId)
      ElMessage.success(translate('messages.memberRoleUpdated'))
    },
    async addMember(userId: number, roleCode: string) {
      if (!this.currentSpaceId) {
        return null
      }

      const member = await spaceService.addMember(this.currentSpaceId, {
        userId,
        roleCode,
      })
      await this.loadMembers(this.currentSpaceId)
      ElMessage.success(translate('messages.memberAdded'))
      return member
    },
    async removeMember(userId: number) {
      if (!this.currentSpaceId) {
        return
      }

      await spaceService.removeMember(this.currentSpaceId, userId)
      await this.loadMembers(this.currentSpaceId)
      ElMessage.success(translate('messages.memberRemoved'))
    },
    async leaveCurrentSpace() {
      if (!this.currentSpaceId) {
        return null
      }

      await spaceService.leave(this.currentSpaceId)
      await this.refreshSpaces()
      this.latestInvite = null
      ElMessage.success(translate('messages.teamSpaceLeft'))
      return this.currentSpace
    },
    async deleteCurrentSpace() {
      if (!this.currentSpaceId) {
        return null
      }

      const deletedSpaceId = this.currentSpaceId
      await spaceService.delete(deletedSpaceId)
      await this.refreshSpaces()
      this.latestInvite = null
      ElMessage.success(translate('messages.spaceDeleted'))
      return this.spaces.find((space) => space.id !== deletedSpaceId) || this.currentSpace
    },
    reset() {
      this.spaces = []
      this.currentSpaceId = null
      this.isSwitchingSpace = false
      this.permissions = []
      this.permissionsSpaceId = null
      this.members = []
      this.membersSpaceId = null
      this.latestInvite = null
      this.switchingSpaceId = null
      permissionCache.clear()
      memberCache.clear()
      permissionRequestMap.clear()
      memberRequestMap.clear()
    },
  },
  persist: {
    key: 'notask-flow-space',
    storage: localStorage,
    pick: ['currentSpaceId'],
  },
})
