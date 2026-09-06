<template>
  <div>
    <h1 class="auth-title">{{ t('auth.resetPasswordTitle') }}</h1>
    <p class="auth-subtitle">{{ t('auth.resetPasswordDescription') }}</p>

    <div v-if="formError" class="auth-error-banner" role="alert">
      <span class="material-symbols-outlined">error</span>
      <span>{{ formError }}</span>
    </div>

    <div v-if="!resetToken">
      <div class="auth-error-banner" role="alert">
        <span class="material-symbols-outlined">error</span>
        <span>{{ t('auth.missingResetToken') }}</span>
      </div>
      <RouterLink class="auth-secondary-button auth-back-button" to="/forgot-password">
        {{ t('auth.backToForgotPassword') }}
      </RouterLink>
    </div>

    <form v-else class="auth-form" novalidate @submit.prevent="handleSubmit">
      <div class="auth-field">
        <div class="auth-label-row">
          <label class="auth-label" for="reset-password">{{ t('auth.newPassword') }}<span class="auth-required">*</span></label>
        </div>
        <div class="auth-password-wrap">
          <input
            id="reset-password"
            v-model="password"
            :type="showPassword ? 'text' : 'password'"
            class="auth-input"
            name="new-password"
            autocomplete="new-password"
            :placeholder="t('auth.passwordAtLeast')"
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
      </div>

      <div class="auth-field">
        <div class="auth-label-row">
          <label class="auth-label" for="reset-confirm-password">{{ t('auth.confirmNewPassword') }}<span class="auth-required">*</span></label>
        </div>
        <div class="auth-password-wrap">
          <input
            id="reset-confirm-password"
            v-model="confirmPassword"
            :type="showConfirmPassword ? 'text' : 'password'"
            class="auth-input"
            name="confirm-password"
            autocomplete="new-password"
            :placeholder="t('auth.confirmNewPasswordPlaceholder')"
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

      <button class="auth-primary-button" type="submit" :disabled="loading">
        <span v-if="loading" class="material-symbols-outlined animate-spin">progress_activity</span>
        <span>{{ t('auth.confirmResetPassword') }}</span>
      </button>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { isAxiosError } from 'axios'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from '@/i18n'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { t } = useI18n()

const password = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const formError = ref('')

const resetToken = computed(() => {
  const value = route.query.resetToken
  return Array.isArray(value) ? value[0] : value || ''
})

// 拦截器把后端业务错误包装为 Error(message)，可直接展示；
// 网络中断或非后端响应的 AxiosError 无服务端消息，需回退本地化文案
const resolveErrorMessage = (error: unknown) => {
  if (isAxiosError(error)) {
    return error.response?.data?.message || t('messages.requestFailed')
  }
  return error instanceof Error && error.message ? error.message : t('messages.requestFailed')
}

const handleSubmit = async () => {
  if (!resetToken.value) {
    await router.push('/forgot-password')
    return
  }

  formError.value = ''
  if (!password.value) {
    ElMessage.warning(t('auth.passwordRequired'))
    return
  }
  if (password.value.length < 8) {
    ElMessage.warning(t('auth.passwordTooShort'))
    return
  }
  if (!confirmPassword.value) {
    ElMessage.warning(t('auth.confirmPasswordRequired'))
    return
  }
  if (password.value !== confirmPassword.value) {
    ElMessage.warning(t('auth.passwordMismatch'))
    return
  }

  loading.value = true
  try {
    await userStore.resetPassword({
      resetToken: resetToken.value,
      newPassword: password.value,
      confirmPassword: confirmPassword.value,
    })
    await router.push('/login')
  } catch (error) {
    formError.value = resolveErrorMessage(error)
  } finally {
    loading.value = false
  }
}
</script>
