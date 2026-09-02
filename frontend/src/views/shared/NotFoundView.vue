<template>
  <div class="flex min-h-screen items-center justify-center bg-background px-4 text-on-background">
    <div class="max-w-xl text-center">
      <div class="mb-6 font-display-serif text-7xl text-primary">404</div>
      <h1 class="font-title-serif text-3xl text-on-surface">{{ t('shared.notFoundTitle') }}</h1>
      <p class="mt-3 text-body-secondary text-on-surface-variant">
        {{ t('shared.notFoundDescription') }}
      </p>
      <RouterLink class="app-primary-button mt-8" to="/app/notes">{{ t('shared.backHome') }}</RouterLink>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from '@/i18n'

const { t } = useI18n()
</script>
