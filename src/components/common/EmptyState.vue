<template>
  <div class="app-card border-dashed border-outline-variant/60 bg-surface-container-low/60 p-8 text-center">
    <div class="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-surface">
      <span class="material-symbols-outlined text-2xl text-primary">{{ icon }}</span>
    </div>
    <h3 class="font-title-serif text-xl text-on-surface">{{ title }}</h3>
    <p class="mx-auto mt-2 max-w-md text-body-secondary text-on-surface-variant">{{ description }}</p>
    <div v-if="$slots.default" class="mt-5">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    description: string
    icon?: string
  }>(),
  {
    icon: 'inbox',
  },
)
</script>

