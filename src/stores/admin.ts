import { ElMessage } from 'element-plus'
import { defineStore } from 'pinia'
import { adminAuthService } from '@/api/modules/admin'
import { resolveDeviceId, resolveDeviceName } from '@/utils/device'
import type { LoginRequest, LoginResponse } from '@/types/app'

interface AdminProfile {
  username: string
  clientType: string
  sessionId: string
}

interface AdminState {
  tokenName: string
  tokenValue: string
  expireTime: number
  profile: AdminProfile | null
}

function applyAdminLoginState(state: AdminState, payload: LoginResponse) {
  state.tokenName = payload.tokenName
  state.tokenValue = payload.tokenValue
  state.expireTime = payload.expireTime
}

export const useAdminStore = defineStore('admin', {
  state: (): AdminState => ({
    tokenName: '',
    tokenValue: '',
    expireTime: 0,
    profile: null,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.tokenValue),
    displayName: (state) => state.profile?.username || 'Administrator',
  },
  actions: {
    async login(payload: LoginRequest) {
      const response = await adminAuthService.login({
        ...payload,
        clientType: 'ADMIN_WEB',
        deviceId: resolveDeviceId(),
        deviceName: resolveDeviceName(),
        appVersion: import.meta.env.VITE_APP_VERSION || 'admin-web',
      })
      applyAdminLoginState(this, response)
      await this.fetchProfile()
      ElMessage.success('登录成功')
    },
    async fetchProfile() {
      this.profile = await adminAuthService.me()
      return this.profile
    },
    async logout() {
      try {
        await adminAuthService.logout()
      } finally {
        this.clearSession()
      }
    },
    clearSession() {
      this.tokenName = ''
      this.tokenValue = ''
      this.expireTime = 0
      this.profile = null
    },
    syncPersistedSession(payload: Partial<AdminState> | null) {
      this.tokenName = payload?.tokenName || ''
      this.tokenValue = payload?.tokenValue || ''
      this.expireTime = payload?.expireTime || 0
      this.profile = payload?.profile || null
    },
  },
  persist: {
    key: 'notask-flow-admin',
    storage: localStorage,
    pick: ['tokenName', 'tokenValue', 'expireTime', 'profile'],
  },
})
