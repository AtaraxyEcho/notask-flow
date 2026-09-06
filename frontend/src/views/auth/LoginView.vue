<template>
  <div>
    <h1 class="auth-title">{{ t('auth.loginTitle') }}</h1>
    <p class="auth-subtitle">{{ t('auth.loginDescription') }}</p>

    <div v-if="formError" class="auth-error-banner" role="alert">
      <span class="material-symbols-outlined">error</span>
      <span>{{ formError }}</span>
    </div>

    <form class="auth-form" novalidate @submit.prevent="handleSubmit">
      <div class="auth-field">
        <label class="auth-label" for="login-account">{{ t('auth.account') }}</label>
        <input
          id="login-account"
          v-model="form.account"
          :class="['auth-input', { 'auth-input--error': fieldErrors.account }]"
          type="text"
          name="username"
          autocomplete="username"
          :placeholder="t('auth.accountPlaceholder')"
          @input="fieldErrors.account = ''"
        />
        <p v-if="fieldErrors.account" class="auth-field-error">
          <span class="material-symbols-outlined">warning</span>
          <span>{{ fieldErrors.account }}</span>
        </p>
        <p v-else class="auth-helper">{{ t('auth.accountHelper') }}</p>
      </div>

      <div class="auth-field">
        <label class="auth-label" for="login-password">{{ t('auth.password') }}</label>
        <div class="auth-password-wrap">
          <input
            id="login-password"
            v-model="form.password"
            :class="['auth-input', { 'auth-input--error': fieldErrors.password }]"
            :type="showPassword ? 'text' : 'password'"
            name="password"
            autocomplete="current-password"
            placeholder="••••••••"
            @input="fieldErrors.password = ''"
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
        <p v-if="fieldErrors.password" class="auth-field-error">
          <span class="material-symbols-outlined">warning</span>
          <span>{{ fieldErrors.password }}</span>
        </p>
      </div>

      <div class="auth-form-row">
        <label class="auth-checkbox-label">
          <input v-model="rememberMe" type="checkbox" class="auth-checkbox" />
          <span>{{ t('auth.rememberMe') }}</span>
        </label>
        <RouterLink class="auth-link" to="/forgot-password">{{ t('auth.forgotPassword') }}</RouterLink>
      </div>

      <button class="auth-primary-button" type="submit" :disabled="loading">
        <span v-if="loading" class="material-symbols-outlined animate-spin">progress_activity</span>
        <span>{{ loading ? t('auth.signingIn') : t('auth.signIn') }}</span>
      </button>
    </form>

    <div class="auth-divider" role="separator"></div>

    <p class="auth-switch-line">
      <span>{{ t('auth.noAccount') }}</span>
      <RouterLink class="auth-link auth-link--strong" to="/register">{{ t('auth.createOne') }}</RouterLink>
    </p>
  </div>
</template>

<script setup lang="ts">
import { isAxiosError } from 'axios'
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from '@/i18n'
import { useSpaceStore } from '@/stores/space'
import { useUserStore } from '@/stores/user'
import { normalizeAuthRedirect } from '@/utils/redirect'
import { getLandingPath } from '@/utils/space'

const REMEMBERED_ACCOUNT_KEY = 'notask-remembered-account'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const spaceStore = useSpaceStore()
const { t } = useI18n()

const loading = ref(false)
const showPassword = ref(false)
const rememberMe = ref(false)
const formError = ref('')
const fieldErrors = reactive({
  account: '',
  password: '',
})
const form = reactive({
  account: '',
  password: '',
})

onMounted(() => {
  const rememberedAccount = window.localStorage.getItem(REMEMBERED_ACCOUNT_KEY)
  if (rememberedAccount) {
    form.account = rememberedAccount
    rememberMe.value = true
  }
})

const validateForm = () => {
  let valid = true
  if (!form.account.trim()) {
    fieldErrors.account = t('auth.accountRequired')
    valid = false
  }
  if (!form.password) {
    fieldErrors.password = t('auth.passwordRequired')
    valid = false
  }
  return valid
}

// 拦截器把后端业务错误包装为 Error(message)，可直接展示；
// 网络中断或非后端响应（网关 404/502 页面）的 AxiosError 无服务端消息，需回退本地化文案
const resolveLoginErrorMessage = (error: unknown) => {
  if (isAxiosError(error)) {
    return error.response?.data?.message || t('auth.loginFailed')
  }
  return error instanceof Error && error.message ? error.message : t('auth.loginFailed')
}

const handleSubmit = async () => {
  formError.value = ''
  form.account = form.account.trim()
  if (!validateForm()) {
    return
  }

  loading.value = true
  try {
    await userStore.login(form)
    if (rememberMe.value) {
      window.localStorage.setItem(REMEMBERED_ACCOUNT_KEY, form.account.trim())
    } else {
      window.localStorage.removeItem(REMEMBERED_ACCOUNT_KEY)
    }
    await spaceStore.ensureLoaded()
    const redirect = normalizeAuthRedirect(route.query.redirect, getLandingPath(spaceStore.currentSpace))
    await router.replace(redirect)
  } catch (error) {
    formError.value = resolveLoginErrorMessage(error)
  } finally {
    loading.value = false
  }
}
</script>
