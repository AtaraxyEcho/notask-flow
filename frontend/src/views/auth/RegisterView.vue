<template>
  <div>
    <h1 class="auth-title">{{ t('auth.registerTitle') }}</h1>
    <p class="auth-subtitle">{{ t('auth.registerDescription') }}</p>

    <div v-if="!authSettings.registrationEnabled" class="auth-error-banner" role="alert">
      <span class="material-symbols-outlined">error</span>
      <span>{{ t('auth.registrationClosed') }}</span>
    </div>

    <form v-else class="auth-form" novalidate @submit.prevent="handleSubmit">
      <div class="auth-field">
        <div class="auth-label-row">
          <label class="auth-label" for="reg-username">{{ t('common.username') }}<span class="auth-required">*</span></label>
        </div>
        <input
          id="reg-username"
          v-model="form.username"
          class="auth-input"
          type="text"
          name="username"
          autocomplete="username"
          :placeholder="t('auth.usernamePlaceholder')"
        />
      </div>

      <div class="auth-field">
        <div class="auth-label-row">
          <label class="auth-label" for="reg-nickname">{{ t('auth.nickname') }}<span class="auth-required">*</span></label>
        </div>
        <input
          id="reg-nickname"
          v-model="form.nickname"
          class="auth-input"
          type="text"
          :placeholder="t('auth.nicknamePlaceholder')"
        />
      </div>

      <div class="auth-field">
        <div class="auth-label-row">
          <label class="auth-label" for="reg-email">{{ t('common.email') }}<span class="auth-required">*</span></label>
        </div>
        <input
          id="reg-email"
          v-model="form.email"
          class="auth-input"
          type="email"
          name="email"
          autocomplete="email"
          placeholder="xiaoman@example.com"
        />
      </div>

      <div class="auth-field">
        <div class="auth-label-row">
          <label class="auth-label" for="reg-password">{{ t('auth.setPassword') }}<span class="auth-required">*</span></label>
          <span class="auth-strength-text">{{ t('auth.passwordStrength') }}：{{ t(strengthLabelKey) }}</span>
        </div>
        <div class="auth-password-wrap">
          <input
            id="reg-password"
            v-model="form.password"
            :type="showPassword ? 'text' : 'password'"
            class="auth-input"
            name="new-password"
            autocomplete="new-password"
            :placeholder="t('auth.passwordPlaceholder')"
          />
          <button
            class="auth-password-toggle"
            type="button"
            :aria-label="showPassword ? t('auth.hidePassword') : t('auth.showPassword')"
            @click="showPassword = !showPassword"
          >
            <span class="material-symbols-outlined">{{ showPassword ? 'visibility_off' : 'visibility' }}</span>
          </button>
        </div>
        <div :class="['auth-strength', `auth-strength--${passwordStrength}`]" aria-hidden="true">
          <span
            v-for="index in 4"
            :key="index"
            :class="['auth-strength-segment', { 'auth-strength-segment--filled': index <= passwordStrength }]"
          ></span>
        </div>
      </div>

      <div class="auth-field">
        <div class="auth-label-row">
          <label class="auth-label" for="reg-confirm-password">{{ t('auth.confirmPassword') }}<span class="auth-required">*</span></label>
        </div>
        <div class="auth-password-wrap">
          <input
            id="reg-confirm-password"
            v-model="confirmPassword"
            :type="showConfirmPassword ? 'text' : 'password'"
            class="auth-input"
            name="confirm-password"
            autocomplete="new-password"
            :placeholder="t('auth.confirmPasswordPlaceholder')"
          />
          <button
            class="auth-password-toggle"
            type="button"
            :aria-label="showConfirmPassword ? t('auth.hidePassword') : t('auth.showPassword')"
            @click="showConfirmPassword = !showConfirmPassword"
          >
            <span class="material-symbols-outlined">{{ showConfirmPassword ? 'visibility_off' : 'visibility' }}</span>
          </button>
        </div>
      </div>

      <div v-if="authSettings.registerEmailVerificationRequired" class="auth-field">
        <div class="auth-label-row">
          <label class="auth-label" for="reg-email-code-0">{{ t('auth.emailCode') }}<span class="auth-required">*</span></label>
          <button
            class="auth-link-button"
            type="button"
            :disabled="sendingCode || resendCountdown > 0"
            @click="handleSendCode"
          >
            <span v-if="sendingCode" class="material-symbols-outlined animate-spin">progress_activity</span>
            <span v-else-if="resendCountdown > 0">{{ t('auth.resendIn', { seconds: resendCountdown }) }}</span>
            <span v-else>{{ t('auth.getCode') }}</span>
          </button>
        </div>
        <p class="auth-field-hint">{{ t('auth.emailCodeHint') }}</p>
        <div class="auth-otp-row" @paste.prevent="handlePaste">
          <input
            v-for="(digit, index) in codeDigits"
            :key="index"
            :ref="(element) => setCodeInputRef(element as HTMLInputElement | null, index)"
            :value="digit"
            class="auth-otp-cell"
            type="text"
            inputmode="numeric"
            maxlength="1"
            :aria-label="`Digit ${index + 1}`"
            @input="handleDigitInput(index, $event)"
            @keydown="handleDigitKeydown(index, $event)"
          />
        </div>
      </div>
      <div v-else class="auth-success-banner" role="status">
        <span class="material-symbols-outlined">check_circle</span>
        <span>{{ t('auth.emailVerificationOptional') }}</span>
      </div>

      <div class="auth-field">
        <div class="auth-label-row">
          <label class="auth-label" for="reg-invite-code">{{ t('auth.inviteCode') }}</label>
          <span class="auth-label-hint">{{ t('auth.optional') }}</span>
        </div>
        <input
          id="reg-invite-code"
          v-model="form.inviteCode"
          class="auth-input"
          type="text"
          :placeholder="t('auth.inviteCodePlaceholder')"
        />
      </div>

      <label class="auth-checkbox-label auth-terms-line">
        <input v-model="acceptedTerms" type="checkbox" class="auth-checkbox" />
        <span>
          {{ t('auth.termsPrefix') }}<button class="auth-inline-link" type="button" @click="openLegalDialog?.('terms')">{{ t('auth.terms') }}</button>{{ t('auth.and') }}<button class="auth-inline-link" type="button" @click="openLegalDialog?.('privacy')">{{ t('auth.privacy') }}</button>
        </span>
      </label>

      <button class="auth-primary-button" type="submit" :disabled="loading">
        <span v-if="loading" class="material-symbols-outlined animate-spin">progress_activity</span>
        <span>{{ loading ? t('auth.registering') : t('auth.registerNow') }}</span>
      </button>
    </form>

    <div class="auth-divider" role="separator"></div>

    <p class="auth-switch-line">
      <span>{{ t('auth.hasAccount') }}</span>
      <RouterLink class="auth-link auth-link--strong" to="/login">{{ t('auth.loginDirectly') }}</RouterLink>
    </p>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, inject, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { authService } from '@/api/services'
import { useRoute, useRouter } from 'vue-router'
import { LEGAL_DIALOG_INJECTION_KEY, type OpenLegalDialog } from '@/constants/legal'
import { useI18n } from '@/i18n'
import { useUserStore } from '@/stores/user'
import type { AuthSystemSetting, RegisterTeamMode } from '@/types/app'

const CODE_LENGTH = 6
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const { t } = useI18n()
const openLegalDialog = inject<OpenLegalDialog | undefined>(LEGAL_DIALOG_INJECTION_KEY, undefined)

const loading = ref(false)
const sendingCode = ref(false)
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const confirmPassword = ref('')
const acceptedTerms = ref(false)
const resendCountdown = ref(0)
const codeDigits = ref(createEmptyDigits())
const codeInputRefs = ref<Array<HTMLInputElement | null>>(Array.from({ length: CODE_LENGTH }, () => null))

let resendTimer: number | null = null

const authSettings = reactive<AuthSystemSetting>({
  registrationEnabled: true,
  registerEmailVerificationRequired: true,
  singleDeviceLoginOnly: true,
})

const form = reactive({
  username: '',
  nickname: '',
  email: '',
  emailCode: '',
  password: '',
  teamMode: 'PERSONAL_ONLY' as RegisterTeamMode,
  teamName: '',
  supervisorAccount: '',
  teamApplyRemark: '',
  inviteCode: (route.query.inviteCode as string) || '',
})

const codeValue = computed(() => codeDigits.value.join(''))

// 强度评分：长度满 8 位、字母数字混合、含特殊符号、长度满 12 位各计 1 分
const passwordStrength = computed(() => {
  const value = form.password
  if (!value) {
    return 0
  }

  let score = 0
  if (value.length >= 8) {
    score += 1
  }
  if (/[a-zA-Z]/.test(value) && /\d/.test(value)) {
    score += 1
  }
  if (/[^a-zA-Z0-9]/.test(value)) {
    score += 1
  }
  if (value.length >= 12) {
    score += 1
  }
  return score
})

const strengthLabelKeys = ['strengthEmpty', 'strengthWeak', 'strengthFair', 'strengthGood', 'strengthStrong'] as const
const strengthLabelKey = computed(() => `auth.${strengthLabelKeys[passwordStrength.value]}`)

function createEmptyDigits() {
  return Array.from({ length: CODE_LENGTH }, () => '')
}

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
    resetCodeInputs()
    startResendCountdown()
    focusCodeInput(0)
  } finally {
    sendingCode.value = false
  }
}

const resetCodeInputs = () => {
  codeDigits.value = createEmptyDigits()
}

const focusCodeInput = (index: number) => {
  nextTick(() => {
    codeInputRefs.value[index]?.focus()
    codeInputRefs.value[index]?.select()
  })
}

const setCodeInputRef = (element: Element | null, index: number) => {
  codeInputRefs.value[index] = element as HTMLInputElement | null
}

const fillDigits = (digits: string, startIndex = 0) => {
  const normalized = digits.replace(/\D/g, '')
  if (!normalized) {
    return
  }
  const nextDigits = [...codeDigits.value]
  normalized
    .slice(0, CODE_LENGTH - startIndex)
    .split('')
    .forEach((digit, offset) => {
      nextDigits[startIndex + offset] = digit
    })
  codeDigits.value = nextDigits
  const nextIndex = Math.min(startIndex + normalized.length, CODE_LENGTH - 1)
  focusCodeInput(nextIndex)
}

const handleDigitInput = (index: number, event: Event) => {
  const target = event.target as HTMLInputElement
  const normalized = target.value.replace(/\D/g, '')
  if (!normalized) {
    codeDigits.value[index] = ''
    return
  }
  if (normalized.length > 1) {
    fillDigits(normalized, index)
    return
  }
  codeDigits.value[index] = normalized
  if (index < CODE_LENGTH - 1) {
    focusCodeInput(index + 1)
  } else {
    target.blur()
  }
}

const handleDigitKeydown = (index: number, event: KeyboardEvent) => {
  if (event.key === 'Backspace' && !codeDigits.value[index] && index > 0) {
    codeDigits.value[index - 1] = ''
    focusCodeInput(index - 1)
    return
  }
  if (event.key === 'ArrowLeft' && index > 0) {
    event.preventDefault()
    focusCodeInput(index - 1)
    return
  }
  if (event.key === 'ArrowRight' && index < CODE_LENGTH - 1) {
    event.preventDefault()
    focusCodeInput(index + 1)
  }
}

const handlePaste = (event: ClipboardEvent) => {
  const pasted = event.clipboardData?.getData('text') || ''
  resetCodeInputs()
  fillDigits(pasted)
}

const handleSubmit = async () => {
  if (!authSettings.registrationEnabled) {
    ElMessage.warning(t('auth.registrationClosed'))
    return
  }

  form.username = form.username.trim()
  form.nickname = form.nickname.trim()
  form.email = form.email.trim().toLowerCase()
  form.emailCode = codeValue.value.trim()
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
