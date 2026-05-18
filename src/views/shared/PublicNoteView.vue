<template>
  <div class="min-h-screen bg-background px-4 py-10 text-on-background md:px-8">
    <div class="mx-auto max-w-4xl">
      <RouterLink class="mb-6 inline-flex items-center gap-2 text-primary" to="/login">
        <span class="material-symbols-outlined text-base">arrow_back</span>
        {{ t('shared.backToApp') }}
      </RouterLink>

      <div v-if="loading" class="app-shell animate-pulse">
        <div class="mb-4 h-10 w-1/2 rounded-full bg-surface-container-high"></div>
        <div class="h-4 w-1/3 rounded-full bg-surface-container-high"></div>
      </div>

      <article v-else-if="note" class="app-shell">
        <div class="mb-6 flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 class="font-display-serif text-5xl leading-tight text-on-surface">{{ note.title }}</h1>
            <p class="mt-3 text-body-secondary text-on-surface-variant">
              {{ t('shared.publicNoteMeta', { views: note.viewCount ?? 0, time: formatDateTime(note.gmtModified) }) }}
            </p>
          </div>
          <div class="flex flex-wrap gap-2">
            <span v-for="tag in note.tags || []" :key="tag.id" class="app-chip">#{{ tag.name }}</span>
          </div>
        </div>

        <div class="markdown-preview" v-html="renderedHtml"></div>
      </article>

      <div v-else class="app-shell">
        <p class="text-body-main text-on-surface-variant">{{ t('shared.publicNoteMissing') }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { noteService } from '@/api/services'
import { useI18n } from '@/i18n'
import type { Note } from '@/types/app'
import { formatDateTime } from '@/utils/date'
import { renderMarkdownLite } from '@/utils/markdown'
import { sanitizeHtml } from '@/utils/sanitize'

const route = useRoute()
const { t } = useI18n()
const loading = ref(false)
const note = ref<Note | null>(null)

const renderedHtml = computed(() => sanitizeHtml(note.value?.contentHtml || renderMarkdownLite(note.value?.content)))

onMounted(async () => {
  loading.value = true
  try {
    note.value = await noteService.publicNote(route.params.shareCode as string)
  } finally {
    loading.value = false
  }
})
</script>
