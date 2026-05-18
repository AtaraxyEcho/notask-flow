import { http } from '../http'
import type {
  AuthSystemSetting,
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

export const authService = {
  settings() {
    return http.get<AuthSystemSetting>('/auth/settings')
  },
  login(payload: LoginRequest) {
    return http.post<LoginResponse>('/auth/login', payload)
  },
  sendRegisterEmailCode(payload: SendRegisterEmailCodeRequest) {
    return http.post<void>('/auth/register/send-email-code', payload)
  },
  register(payload: RegisterRequest) {
    return http.post<UserProfile>('/auth/register', payload)
  },
  forgotPassword(payload: ForgotPasswordRequest) {
    return http.post<void>('/auth/forgot-password', payload)
  },
  verifyResetCode(payload: VerifyResetCodeRequest) {
    return http.post<VerifyResetCodeResponse>('/auth/verify-reset-code', payload)
  },
  resetPassword(payload: ResetPasswordRequest) {
    return http.post<void>('/auth/reset-password', payload)
  },
  refresh() {
    return http.post<LoginResponse>('/auth/refresh')
  },
  logout() {
    return http.post<void>('/auth/logout')
  },
}
