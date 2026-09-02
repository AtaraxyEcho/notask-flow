<template>
  <div class="flex flex-col items-center text-center">
    <div class="mb-6 flex h-16 w-16 items-center justify-center rounded-full bg-primary-fixed text-primary shadow-sm">
      <span class="material-symbols-outlined text-[32px]">mark_email_read</span>
    </div>

    <h1 class="font-title-serif text-3xl text-on-surface">
      {{ hasSentCode ? t('auth.verifyEmailTitle') : t('auth.forgotPasswordTitle') }}
    </h1>

    <p class="mt-4 max-w-[26rem] text-body-main leading-8 text-on-surface-variant">
      <template v-if="hasSentCode">
        {{ t('auth.codeSentPrefix') }} <span class="font-medium text-on-surface">{{ email }}</span
        >{{ t('auth.codeSentSuffix') }}
      </template>
      <template v-else>
        {{ t('auth.forgotPasswordDescription') }}
      </template>
    </p>

    <form class="mt-8 w-full space-y-8" @submit.prevent="handlePrimaryAction">
      <label v-if="!hasSentCode" class="block text-left">
        <span class="mb-2 block text-label-bold text-on-surface">{{ t('auth.emailAddress') }}</span>
        <input
          v-model="email"
          type="email"
          class="app-input w-full px-4 py-3.5"
          placeholder="you@example.com"
        />
      </label>

      <div v-else class="space-y-6">
        <div class="rounded-[1.2rem] bg-surface-container-low px-4 py-3 text-sm text-on-surface-variant">
          {{ t('auth.codeValidTip') }}
        </div>

        <div class="flex justify-between gap-2 sm:gap-3" @paste.prevent="handlePaste">
          <input
            v-for="(digit, index) in codeDigits"
            :key="index"
            :ref="(element) => setCodeInputRef(element as HTMLInputElement | null, index)"
            :value="digit"
            :aria-label="`Digit ${index + 1}`"
            class="h-14 w-11 rounded-xl border border-outline-variant bg-surface-container-low text-center font-title-serif text-2xl text-on-surface outline-none transition-all focus:border-primary focus:ring-1 focus:ring-primary sm:w-12"
            inputmode="numeric"
            maxlength="1"
            type="text"
            @input="handleDigitInput(index, $event)"
            @keydown="handleDigitKeydown(index, $event)"
          />
        </div>
      </div>

      <button class="app-primary-button w-full justify-center gap-2 py-3.5" type="submit" :disabled="primaryLoading">
        <span v-if="primaryLoading" class="material-symbols-outlined animate-spin text-base">progress_activity</span>
        <template v-else>
          {{ hasSentCode ? t('auth.verify') : t('auth.sendCode') }}
          <span v-if="hasSentCode" class="material-symbols-outlined text-[18px]">arrow_forward</span>
        </template>
      </button>
    </form>

    <div class="mt-8 text-center text-body-secondary text-on-surface-variant">
      <template v-if="hasSentCode">
        {{ t('auth.noCode') }}
        <button
          class="ml-1 font-semibold text-primary transition-colors hover:text-primary-container disabled:cursor-not-allowed disabled:text-on-surface-variant"
          type="button"
          :disabled="resendCountdown > 0 || resendLoading"
          @click="resendCode"
        >
          {{ resendCountdown > 0 ? t('auth.resendIn', { seconds: resendCountdown }) : t('auth.resendCode') }}
        </button>
        <button
          class="ml-3 text-on-surface-variant transition-colors hover:text-primary"
          type="button"
          @click="editEmail"
        >
          {{ t('auth.editEmail') }}
        </button>
      </template>
      <template v-else>
        {{ t('auth.rememberPassword') }}
        <RouterLink class="ml-1 font-semibold text-primary hover:text-primary-container" to="/login">
          {{ t('auth.backToLogin') }}
        </RouterLink>
      </template>
    </div>

    <div class="mt-8 w-full border-t border-outline-variant/20 pt-6">
      <div class="flex items-center justify-center gap-2 text-caption text-on-surface-variant">
        <span class="material-symbols-outlined text-[16px]">lock</span>
        <span>{{ t('auth.secureVerification') }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from '@/i18n'
import { useUserStore } from '@/stores/user'

const CODE_LENGTH = 6
const RESEND_INTERVAL_SECONDS = 60

const router = useRouter()
const userStore = useUserStore()
const { t } = useI18n()

const email = ref('')
const hasSentCode = ref(false)
const primaryLoading = ref(false)
const resendLoading = ref(false)
const resendCountdown = ref(0)
const codeDigits = ref(createEmptyDigits())
const codeInputRefs = ref<Array<HTMLInputElement | null>>(createEmptyDigits().map(() => null))

let resendTimer: number | undefined

const codeValue = computed(() => codeDigits.value.join(''))

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
  resendCountdown.value = RESEND_INTERVAL_SECONDS
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

const sendCode = async () => {
  await userStore.forgotPassword({ email: email.value.trim() })
  hasSentCode.value = true
  resetCodeInputs()
  startResendCountdown()
  focusCodeInput(0)
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

const handlePrimaryAction = async () => {
  primaryLoading.value = true
  try {
    if (!hasSentCode.value) {
      await sendCode()
      return
    }

    const response = await userStore.verifyResetCode({
      email: email.value.trim(),
      code: codeValue.value,
    })
    await router.push({
      path: '/reset-password',
      query: {
        resetToken: response.resetToken,
      },
    })
  } finally {
    primaryLoading.value = false
  }
}

const resendCode = async () => {
  resendLoading.value = true
  try {
    await sendCode()
  } finally {
    resendLoading.value = false
  }
}

const editEmail = () => {
  hasSentCode.value = false
  resetCodeInputs()
  clearResendTimer()
  resendCountdown.value = 0
}

onBeforeUnmount(() => {
  clearResendTimer()
})
</script>
