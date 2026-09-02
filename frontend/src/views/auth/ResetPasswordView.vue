<template>
  <div>
    <h1 class="font-title-serif text-3xl text-on-surface">{{ t('auth.resetPasswordTitle') }}</h1>
    <p class="mt-2 text-body-secondary text-on-surface-variant">
      {{ t('auth.resetPasswordDescription') }}
    </p>

    <div
      v-if="!resetToken"
      class="mt-8 rounded-[1.5rem] border border-outline-variant/20 bg-white/55 px-5 py-6 text-body-secondary text-on-surface-variant"
    >
      {{ t('auth.missingResetToken') }}
      <div class="mt-4">
        <RouterLink class="app-primary-button inline-flex justify-center" to="/forgot-password">
          {{ t('auth.backToForgotPassword') }}
        </RouterLink>
      </div>
    </div>

    <form v-else class="mt-8 space-y-6" @submit.prevent="handleSubmit">
      <label class="block">
        <span class="mb-2 block text-label-bold text-on-surface">{{ t('auth.newPassword') }}</span>
        <input
          v-model="password"
          type="password"
          class="app-input w-full px-4 py-3"
          :placeholder="t('auth.passwordAtLeast')"
        />
      </label>

      <label class="block">
        <span class="mb-2 block text-label-bold text-on-surface">{{ t('auth.confirmNewPassword') }}</span>
        <input
          v-model="confirmPassword"
          type="password"
          class="app-input w-full px-4 py-3"
          :placeholder="t('auth.confirmNewPasswordPlaceholder')"
        />
      </label>

      <button class="app-primary-button w-full justify-center" type="submit" :disabled="loading">
        <span v-if="loading" class="material-symbols-outlined animate-spin text-base">progress_activity</span>
        {{ t('auth.confirmResetPassword') }}
      </button>
    </form>
  </div>
</template>

<script setup lang="ts">
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

const resetToken = computed(() => {
  const value = route.query.resetToken
  return Array.isArray(value) ? value[0] : value || ''
})

const handleSubmit = async () => {
  if (!resetToken.value) {
    await router.push('/forgot-password')
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
  } finally {
    loading.value = false
  }
}
</script>
