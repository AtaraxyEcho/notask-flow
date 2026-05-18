import { ElMessage } from 'element-plus'
import { defineStore } from 'pinia'
import { authService, userService } from '@/api/services'
import { translate } from '@/i18n'
import { resolveDeviceId, resolveDeviceName } from '@/utils/device'
import { resolveAvatarUrl } from '@/utils/avatar'
import type {
  ForgotPasswordRequest,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  ResetPasswordRequest,
  SendRegisterEmailCodeRequest,
  UserProfile,
  VerifyResetCodeRequest,
  VerifyResetCodeResponse,
} from '@/types/app'

interface UserState {
  tokenName: string
  tokenValue: string
  expireTime: number
  profile: UserProfile | null
}

function applyLoginState(state: UserState, payload: LoginResponse) {
  state.tokenName = payload.tokenName
  state.tokenValue = payload.tokenValue
  state.expireTime = payload.expireTime
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    tokenName: '',
    tokenValue: '',
    expireTime: 0,
    profile: null,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.tokenValue),
    displayName: (state) => state.profile?.nickname || state.profile?.username || translate('messages.notLoggedIn'),
  },
  actions: {
    async login(payload: LoginRequest) {
      const response = await authService.login({
        ...payload,
        clientType: 'WEB',
        deviceId: resolveDeviceId(),
        deviceName: resolveDeviceName(),
        appVersion: import.meta.env.VITE_APP_VERSION || 'web',
      })
      applyLoginState(this, response)
      await this.fetchProfile()
      ElMessage.success(translate('messages.loginSuccess'))
    },
    async sendRegisterEmailCode(payload: SendRegisterEmailCodeRequest) {
      await authService.sendRegisterEmailCode(payload)
      ElMessage.success(translate('messages.registerCodeSent'))
    },
    async register(payload: RegisterRequest) {
      await authService.register(payload)
      ElMessage.success(translate('messages.registerSuccess'))
    },
    async forgotPassword(payload: ForgotPasswordRequest) {
      await authService.forgotPassword(payload)
      ElMessage.success(translate('messages.verificationCodeSent'))
    },
    async verifyResetCode(payload: VerifyResetCodeRequest) {
      const response = await authService.verifyResetCode(payload)
      ElMessage.success(translate('messages.verificationCodePassed'))
      return response as VerifyResetCodeResponse
    },
    async resetPassword(payload: ResetPasswordRequest) {
      await authService.resetPassword(payload)
      ElMessage.success(translate('messages.passwordReset'))
    },
    async refreshToken() {
      const response = await authService.refresh()
      applyLoginState(this, response)
    },
    async fetchProfile() {
      const profile = await userService.profile()
      this.profile = profile ? { ...profile, avatarUrl: resolveAvatarUrl(profile.avatarUrl) } : profile
      return this.profile
    },
    async logout() {
      try {
        await authService.logout()
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
    syncPersistedSession(payload: Partial<UserState> | null) {
      this.tokenName = payload?.tokenName || ''
      this.tokenValue = payload?.tokenValue || ''
      this.expireTime = payload?.expireTime || 0
      this.profile = payload?.profile
        ? { ...payload.profile, avatarUrl: resolveAvatarUrl(payload.profile.avatarUrl) }
        : null
    },
  },
  persist: {
    key: 'notask-flow-user',
    storage: localStorage,
    pick: ['tokenName', 'tokenValue', 'expireTime', 'profile'],
  },
})
