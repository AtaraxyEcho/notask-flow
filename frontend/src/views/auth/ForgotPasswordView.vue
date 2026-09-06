<template>
  <div>
    <div class="auth-steps">
      <div :class="['auth-step', { 'auth-step--active': !hasSentCode }]">
        <span class="auth-step-dot">1</span>
        <span class="auth-step-label">{{ t('auth.stepEmail') }}</span>
      </div>
      <div class="auth-steps-line"></div>
      <div :class="['auth-step', { 'auth-step--active': hasSentCode }]">
        <span class="auth-step-dot">2</span>
        <span class="auth-step-label">{{ t('auth.stepCode') }}</span>
      </div>
    </div>

    <h1 class="auth-title">{{ hasSentCode ? t('auth.verifyEmailTitle') : t('auth.forgotPasswordTitle') }}</h1>
    <p class="auth-subtitle">
      <template v-if="hasSentCode">
        {{ t('auth.codeSentPrefix') }}<span class="auth-code-target">{{ email }}</span>{{ t('auth.codeSentSuffix') }}
      </template>
      <template v-else>
        {{ t('auth.forgotPasswordDescription') }}
      </template>
    </p>

    <div v-if="formError" class="auth-error-banner" role="alert">
      <span class="material-symbols-outlined">error</span>
      <span>{{ formError }}</span>
    </div>

    <form class="auth-form" novalidate @submit.prevent="handlePrimaryAction">
      <template v-if="!hasSentCode">
        <div class="auth-field">
          <div class="auth-label-row">
            <label class="auth-label" for="forgot-email">{{ t('auth.emailAddress') }}<span class="auth-required">*</span></label>
          </div>
          <div class="auth-input-affix">
            <input
              id="forgot-email"
              v-model="email"
              class="auth-input"
              type="email"
              name="email"
              autocomplete="email"
              placeholder="you@example.com"
              @input="fieldError = ''"
            />
            <span class="material-symbols-outlined">mail</span>
          </div>
          <p v-if="fieldError" class="auth-field-error">
            <span class="material-symbols-outlined">warning</span>
            <span>{{ fieldError }}</span>
          </p>
        </div>

        <button class="auth-primary-button" type="submit" :disabled="primaryLoading">
          <span v-if="primaryLoading" class="material-symbols-outlined animate-spin">progress_activity</span>
          <span>{{ primaryLoading ? '' : t('auth.sendCode') }}</span>
        </button>
      </template>

      <template v-else>
        <p class="auth-field-hint">{{ t('auth.codeValidTip') }}</p>

        <div class="auth-field">
          <div class="auth-label-row">
            <span class="auth-label">{{ t('auth.emailCode') }}<span class="auth-required">*</span></span>
          </div>
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
          <p v-if="fieldError" class="auth-field-error">
            <span class="material-symbols-outlined">warning</span>
            <span>{{ fieldError }}</span>
          </p>
        </div>

        <button class="auth-primary-button" type="submit" :disabled="primaryLoading">
          <span v-if="primaryLoading" class="material-symbols-outlined animate-spin">progress_activity</span>
          <template v-else>
            <span>{{ t('auth.verify') }}</span>
            <span class="material-symbols-outlined">arrow_forward</span>
          </template>
        </button>
      </template>
    </form>

    <div class="auth-divider" role="separator"></div>

    <p class="auth-switch-line">
      <template v-if="hasSentCode">
        <span>{{ t('auth.noCode') }}</span>
        <button
          class="auth-link-button"
          type="button"
          :disabled="resendCountdown > 0 || resendLoading"
          @click="resendCode"
        >
          {{ resendCountdown > 0 ? t('auth.resendIn', { seconds: resendCountdown }) : t('auth.resendCode') }}
        </button>
        <button class="auth-link-button" type="button" @click="editEmail">
          {{ t('auth.editEmail') }}
        </button>
      </template>
      <template v-else>
        <span>{{ t('auth.rememberPassword') }}</span>
        <RouterLink class="auth-link auth-link--strong" to="/login">{{ t('auth.backToLogin') }}</RouterLink>
      </template>
    </p>
  </div>
</template>

<script setup lang="ts">
import { isAxiosError } from 'axios'
import { nextTick, onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from '@/i18n'
import { useUserStore } from '@/stores/user'

const CODE_LENGTH = 6
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const router = useRouter()
const userStore = useUserStore()
const { t } = useI18n()

const email = ref('')
const hasSentCode = ref(false)
const primaryLoading = ref(false)
const resendLoading = ref(false)
const resendCountdown = ref(0)
const formError = ref('')
const fieldError = ref('')
const codeDigits = ref(createEmptyDigits())
const codeInputRefs = ref<Array<HTMLInputElement | null>>(Array.from({ length: CODE_LENGTH }, () => null))

let resendTimer: number | undefined

const codeValue = () => codeDigits.value.join('')

// 拦截器把后端业务错误包装为 Error(message)，可直接展示；
// 网络中断或非后端响应的 AxiosError 无服务端消息，需回退本地化文案
const resolveErrorMessage = (error: unknown) => {
  if (isAxiosError(error)) {
    return error.response?.data?.message || t('messages.requestFailed')
  }
  return error instanceof Error && error.message ? error.message : t('messages.requestFailed')
}

function createEmptyDigits() {
  return Array.from({ length: CODE_LENGTH }, () => '')
}

const clearResendTimer = () => {
  if (resendTimer) {
    window.clearInterval(resendTimer)
    resendTimer = undefined
  }
}

const startResendCountdown = () => {
  clearResendTimer()
  resendCountdown.value = 60
  resendTimer = window.setInterval(() => {
    if (resendCountdown.value <= 1) {
      clearResendTimer()
      resendCountdown.value = 0
      return
    }
    resendCountdown.value -= 1
  }, 1000)
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
  fieldError.value = ''
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

const handlePrimaryAction = async () => {
  formError.value = ''

  if (!hasSentCode.value) {
    const normalizedEmail = email.value.trim().toLowerCase()
    if (!EMAIL_PATTERN.test(normalizedEmail)) {
      fieldError.value = t('auth.validEmail')
      return
    }

    primaryLoading.value = true
    try {
      await userStore.forgotPassword({ email: normalizedEmail })
      email.value = normalizedEmail
      hasSentCode.value = true
      resetCodeInputs()
      startResendCountdown()
      focusCodeInput(0)
    } catch (error) {
      formError.value = resolveErrorMessage(error)
    } finally {
      primaryLoading.value = false
    }
    return
  }

  if (!/^\d{6}$/.test(codeValue())) {
    fieldError.value = t('auth.validEmailCode')
    return
  }

  primaryLoading.value = true
  try {
    const response = await userStore.verifyResetCode({
      email: email.value.trim(),
      code: codeValue(),
    })
    await router.push({
      path: '/reset-password',
      query: {
        resetToken: response.resetToken,
      },
    })
  } catch (error) {
    formError.value = resolveErrorMessage(error)
  } finally {
    primaryLoading.value = false
  }
}

const resendCode = async () => {
  resendLoading.value = true
  formError.value = ''
  try {
    await userStore.forgotPassword({ email: email.value.trim() })
    resetCodeInputs()
    startResendCountdown()
    focusCodeInput(0)
  } catch (error) {
    formError.value = resolveErrorMessage(error)
  } finally {
    resendLoading.value = false
  }
}

const editEmail = () => {
  hasSentCode.value = false
  fieldError.value = ''
  resetCodeInputs()
  clearResendTimer()
  resendCountdown.value = 0
}

onBeforeUnmount(() => {
  clearResendTimer()
})
</script>
