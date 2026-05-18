<template>
  <div class="auth-register text-text-light dark:text-text-dark">
    <div class="pb-6 text-center">
      <div class="mb-6 flex items-center justify-center gap-2">
        <span class="material-symbols-outlined text-3xl text-primary">book</span>
        <span class="text-2xl font-semibold tracking-tight">Notask Flow</span>
      </div>
      <h1 class="mb-2 text-xl font-medium">{{ t('auth.registerTitle') }}</h1>
      <p class="text-sm text-text-muted-light dark:text-text-muted-dark">
        {{ t('auth.registerDescription') }}
      </p>
    </div>

    <div
      v-if="!authSettings.registrationEnabled"
      class="mb-6 rounded-xl border border-orange-100 bg-orange-50 px-4 py-3 text-sm leading-6 text-orange-700 dark:border-orange-900/40 dark:bg-orange-950/30 dark:text-orange-200"
    >
      {{ t('auth.registrationClosed') }}
    </div>

    <form v-else class="space-y-5" @submit.prevent="handleSubmit">
      <label class="block">
        <span class="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">{{ t('auth.nickname') }}</span>
        <input
          v-model="form.nickname"
          class="auth-register-input"
          :placeholder="t('auth.nicknamePlaceholder')"
          required
          type="text"
        />
      </label>

      <label class="block">
        <span class="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">{{ t('common.username') }}</span>
        <input
          v-model="form.username"
          class="auth-register-input"
          :placeholder="t('auth.usernamePlaceholder')"
          required
          type="text"
        />
      </label>

      <label class="block">
        <span class="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">{{ t('common.email') }}</span>
        <input
          v-model="form.email"
          class="auth-register-input"
          placeholder="xiaoman@example.com"
          required
          type="email"
        />
      </label>

      <div v-if="authSettings.registerEmailVerificationRequired" class="grid gap-3 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-end">
        <label class="block">
          <span class="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">{{ t('auth.emailCode') }}</span>
          <input
            v-model="form.emailCode"
            class="auth-register-input"
            inputmode="numeric"
            maxlength="6"
            :placeholder="t('auth.emailCodePlaceholder')"
            required
            type="text"
          />
        </label>
        <button
          class="auth-register-secondary-btn"
          :disabled="sendingCode || resendCountdown > 0"
          type="button"
          @click="handleSendCode"
        >
          <span v-if="sendingCode" class="material-symbols-outlined animate-spin text-base">progress_activity</span>
          <span v-else-if="resendCountdown > 0">{{ t('auth.resendIn', { seconds: resendCountdown }) }}</span>
          <span v-else>{{ t('auth.sendCode') }}</span>
        </button>
      </div>
      <div
        v-else
        class="rounded-lg border border-primary/15 bg-primary/5 px-4 py-3 text-sm leading-6 text-primary dark:border-primary/30 dark:bg-primary/10"
      >
        {{ t('auth.emailVerificationOptional') }}
      </div>

      <label class="block">
        <span class="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">{{ t('auth.password') }}</span>
        <div class="relative">
          <input
            v-model="form.password"
            :type="showPassword ? 'text' : 'password'"
            class="auth-register-input pr-11"
            :placeholder="t('auth.passwordPlaceholder')"
            required
          />
          <button
            class="absolute inset-y-0 right-0 flex items-center pr-3 text-gray-400 transition-colors hover:text-gray-600 dark:hover:text-gray-300"
            type="button"
            @click="showPassword = !showPassword"
          >
            <span class="material-symbols-outlined text-[18px]">
              {{ showPassword ? 'visibility' : 'visibility_off' }}
            </span>
          </button>
        </div>
      </label>

      <label class="block">
        <span class="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">{{ t('auth.confirmPassword') }}</span>
        <div class="relative">
          <input
            v-model="confirmPassword"
            :type="showConfirmPassword ? 'text' : 'password'"
            class="auth-register-input pr-11"
            :placeholder="t('auth.confirmPasswordPlaceholder')"
            required
          />
          <button
            class="absolute inset-y-0 right-0 flex items-center pr-3 text-gray-400 transition-colors hover:text-gray-600 dark:hover:text-gray-300"
            type="button"
            @click="showConfirmPassword = !showConfirmPassword"
          >
            <span class="material-symbols-outlined text-[18px]">
              {{ showConfirmPassword ? 'visibility' : 'visibility_off' }}
            </span>
          </button>
        </div>
      </label>

      <label class="block">
        <span class="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
          {{ t('auth.inviteCode') }}
          <span class="font-normal text-gray-400">{{ t('auth.optional') }}</span>
        </span>
        <input
          v-model="form.inviteCode"
          class="auth-register-input"
          :placeholder="t('auth.inviteCodePlaceholder')"
          type="text"
        />
      </label>

      <label class="mt-4 flex items-start">
        <input
          v-model="acceptedTerms"
          class="mt-0.5 h-4 w-4 cursor-pointer rounded border-gray-300 text-primary focus:ring-primary"
          required
          type="checkbox"
        />
        <span class="ml-2 block text-sm text-gray-600 dark:text-gray-400">
          {{ t('auth.termsPrefix') }}
          <a class="text-primary hover:underline" href="#">{{ t('auth.terms') }}</a>
          {{ t('auth.and') }}
          <a class="text-primary hover:underline" href="#">{{ t('auth.privacy') }}</a>
        </span>
      </label>

      <div class="pt-2">
        <button
          class="flex w-full justify-center rounded bg-primary px-4 py-2.5 text-sm font-medium text-white shadow-sm transition-colors duration-200 hover:bg-orange-600 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2"
          :disabled="loading"
          type="submit"
        >
          <span v-if="loading" class="material-symbols-outlined animate-spin text-base">progress_activity</span>
          <span>{{ loading ? t('auth.registering') : t('auth.registerNow') }}</span>
        </button>
      </div>
    </form>

    <div class="mt-6 text-center text-sm">
      <span class="text-gray-600 dark:text-gray-400">{{ t('auth.hasAccount') }}</span>
      <RouterLink class="ml-1 font-medium text-primary hover:underline" to="/login">{{ t('auth.loginDirectly') }}</RouterLink>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { authService } from '@/api/services'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from '@/i18n'
import { useUserStore } from '@/stores/user'
import type { AuthSystemSetting, RegisterTeamMode } from '@/types/app'

interface RegisterForm {
  username: string
  nickname: string
  email: string
  emailCode: string
  password: string
  teamMode: RegisterTeamMode
  teamName: string
  supervisorAccount: string
  teamApplyRemark: string
  inviteCode: string
}

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const { t } = useI18n()

const loading = ref(false)
const sendingCode = ref(false)
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const confirmPassword = ref('')
const acceptedTerms = ref(false)
const resendCountdown = ref(0)

let resendTimer: number | null = null

const authSettings = reactive<AuthSystemSetting>({
  registrationEnabled: true,
  registerEmailVerificationRequired: true,
  singleDeviceLoginOnly: true,
})

const form = reactive<RegisterForm>({
  username: '',
  nickname: '',
  email: '',
  emailCode: '',
  password: '',
  teamMode: 'PERSONAL_ONLY',
  teamName: '',
  supervisorAccount: '',
  teamApplyRemark: '',
  inviteCode: (route.query.inviteCode as string) || '',
})

const startResendCountdown = () => {
  resendCountdown.value = 60
  if (resendTimer) {
    window.clearInterval(resendTimer)
  }
  resendTimer = window.setInterval(() => {
    if (resendCountdown.value <= 1) {
      if (resendTimer) {
        window.clearInterval(resendTimer)
        resendTimer = null
      }
      resendCountdown.value = 0
      return
    }
    resendCountdown.value -= 1
  }, 1000)
}

const loadAuthSettings = async () => {
  try {
    const settings = await authService.settings()
    Object.assign(authSettings, settings)
  } catch {
    ElMessage.warning(t('auth.systemSettingsLoadFailed'))
  }
}

const handleSendCode = async () => {
  if (!authSettings.registrationEnabled) {
    ElMessage.warning(t('auth.registrationClosed'))
    return
  }
  if (!authSettings.registerEmailVerificationRequired) {
    return
  }

  const normalizedEmail = form.email.trim().toLowerCase()
  if (!EMAIL_PATTERN.test(normalizedEmail)) {
    ElMessage.warning(t('auth.validEmailFirst'))
    return
  }

  sendingCode.value = true
  try {
    await userStore.sendRegisterEmailCode({ email: normalizedEmail })
    form.email = normalizedEmail
    startResendCountdown()
  } finally {
    sendingCode.value = false
  }
}

const handleSubmit = async () => {
  if (!authSettings.registrationEnabled) {
    ElMessage.warning(t('auth.registrationClosed'))
    return
  }

  form.username = form.username.trim()
  form.nickname = form.nickname.trim()
  form.email = form.email.trim().toLowerCase()
  form.emailCode = form.emailCode.trim()
  form.inviteCode = form.inviteCode.trim()

  if (!EMAIL_PATTERN.test(form.email)) {
    ElMessage.warning(t('auth.validEmail'))
    return
  }

  if (authSettings.registerEmailVerificationRequired && !/^\d{6}$/.test(form.emailCode)) {
    ElMessage.warning(t('auth.validEmailCode'))
    return
  }

  if (form.password !== confirmPassword.value) {
    ElMessage.warning(t('auth.passwordMismatch'))
    return
  }

  if (!acceptedTerms.value) {
    ElMessage.warning(t('auth.acceptTermsFirst'))
    return
  }

  form.teamMode = form.inviteCode ? 'JOIN_INVITE_CODE' : 'PERSONAL_ONLY'
  form.teamName = ''
  form.supervisorAccount = ''
  form.teamApplyRemark = ''

  loading.value = true
  try {
    await userStore.register({
      ...form,
      emailCode: authSettings.registerEmailVerificationRequired ? form.emailCode : undefined,
      inviteCode: form.inviteCode,
    })
    await router.push('/login')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadAuthSettings()
})

onBeforeUnmount(() => {
  if (resendTimer) {
    window.clearInterval(resendTimer)
    resendTimer = null
  }
})
</script>

<style scoped>
.auth-register {
  font-family: 'Inter', 'Plus Jakarta Sans', sans-serif;
}

.auth-register-input {
  width: 100%;
  border-radius: 0.5rem;
  border: 1px solid #f0e6e1;
  background: #f9fafb;
  padding: 0.625rem 1rem;
  font-size: 0.875rem;
  line-height: 1.25rem;
  color: #333333;
  outline: none;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    background-color 0.2s ease;
}

.auth-register-input::placeholder {
  color: #9ca3af;
}

.auth-register-input:focus {
  border-color: #f46c44;
  box-shadow: 0 0 0 2px rgba(244, 108, 68, 0.2);
  background: #ffffff;
}

:global(.dark) .auth-register-input {
  border-color: #404040;
  background: #1f2937;
  color: #ffffff;
}

:global(.dark) .auth-register-input::placeholder {
  color: #9ca3af;
}

:global(.dark) .auth-register-input:focus {
  background: #111827;
}

.auth-register-secondary-btn {
  display: inline-flex;
  min-height: 42px;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  border-radius: 0.5rem;
  border: 1px solid rgba(244, 108, 68, 0.22);
  background: rgba(244, 108, 68, 0.08);
  padding: 0.625rem 1rem;
  font-size: 0.875rem;
  font-weight: 600;
  color: #9f4122;
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease,
    color 0.2s ease,
    opacity 0.2s ease;
}

.auth-register-secondary-btn:hover:not(:disabled) {
  background: rgba(244, 108, 68, 0.14);
  border-color: rgba(244, 108, 68, 0.34);
}

.auth-register-secondary-btn:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

:global(.dark) .auth-register-secondary-btn {
  border-color: rgba(255, 181, 158, 0.22);
  background: rgba(255, 181, 158, 0.1);
  color: #ffb59e;
}

:global(.dark) .auth-register-secondary-btn:hover:not(:disabled) {
  background: rgba(255, 181, 158, 0.18);
  border-color: rgba(255, 181, 158, 0.34);
}
</style>
