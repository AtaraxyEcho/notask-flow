<template>
  <div>
    <h2 class="font-title-serif text-title-serif text-on-surface">{{ t('auth.loginTitle') }}</h2>
    <p class="mt-2 text-body-secondary text-on-surface-variant">
      {{ t('auth.loginDescription') }}
    </p>

    <form class="mt-8 space-y-5" @submit.prevent="handleSubmit">
      <label class="block">
        <span class="mb-2 block text-label-bold text-on-surface">{{ t('auth.account') }}</span>
        <div class="app-input flex items-center gap-3 px-4 py-3">
          <span class="material-symbols-outlined text-on-surface-variant">person</span>
          <input
            v-model="form.account"
            class="w-full border-none bg-transparent p-0 outline-none focus:ring-0"
            :placeholder="t('auth.accountPlaceholder')"
          />
        </div>
      </label>

      <label class="block">
        <span class="mb-2 block text-label-bold text-on-surface">{{ t('auth.password') }}</span>
        <div class="app-input flex items-center gap-3 px-4 py-3">
          <span class="material-symbols-outlined text-on-surface-variant">lock</span>
          <input
            v-model="form.password"
            type="password"
            class="w-full border-none bg-transparent p-0 outline-none focus:ring-0"
            placeholder="••••••••"
          />
        </div>
      </label>

      <div class="mt-4 flex items-center justify-between text-body-secondary text-on-surface-variant">
        <label class="flex items-center gap-3">
          <input
            v-model="rememberMe"
            type="checkbox"
            class="h-4 w-4 rounded border-outline-variant bg-surface-container text-primary focus:ring-2 focus:ring-primary/20"
          />
          <span>{{ t('auth.rememberMe') }}</span>
        </label>

        <RouterLink class="text-primary hover:text-on-primary-container" to="/forgot-password">
          {{ t('auth.forgotPassword') }}
        </RouterLink>
      </div>

      <div class="pt-4">
        <button
          class="flex w-full justify-center rounded-full border border-transparent bg-primary px-4 py-3.5 text-label-bold text-on-primary shadow-sm transition-colors duration-200 hover:bg-on-primary-container"
          type="submit"
          :disabled="loading"
        >
          <span v-if="loading" class="material-symbols-outlined animate-spin text-base">progress_activity</span>
          <span>{{ loading ? t('auth.signingIn') : t('auth.signIn') }}</span>
        </button>
      </div>
    </form>

    <p class="mt-8 text-center text-body-secondary text-on-surface-variant">
      {{ t('auth.noAccount') }}
      <RouterLink class="ml-1 text-primary hover:text-on-primary-container" to="/register">
        {{ t('auth.createOne') }}
      </RouterLink>
    </p>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from '@/i18n'
import { useSpaceStore } from '@/stores/space'
import { useUserStore } from '@/stores/user'
import { normalizeAuthRedirect } from '@/utils/redirect'
import { getLandingPath } from '@/utils/space'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const spaceStore = useSpaceStore()
const { t } = useI18n()

const loading = ref(false)
const rememberMe = ref(true)
const form = reactive({
  account: '',
  password: '',
})

const handleSubmit = async () => {
  loading.value = true
  try {
    await userStore.login(form)
    await spaceStore.ensureLoaded()
    const redirect = normalizeAuthRedirect(route.query.redirect, getLandingPath(spaceStore.currentSpace))
    await router.replace(redirect)
  } finally {
    loading.value = false
  }
}
</script>
