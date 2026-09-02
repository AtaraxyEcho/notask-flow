<template>
  <el-drawer v-model="drawerVisible" :title="t('note.historyTitle')" size="400px">
    <div class="space-y-4">
      <div v-if="histories.length" class="rounded-3xl border border-outline-variant/30 bg-surface-container-lowest p-4 shadow-sm">
        <div class="flex items-center gap-2 text-[11px] font-bold uppercase tracking-[0.18em] text-primary">
          <span class="material-symbols-outlined text-[16px]">history</span>
          {{ t('note.historyCount', { count: histories.length }) }}
        </div>
        <div v-if="latestHistory?.gmtCreate" class="mt-2 text-caption text-on-surface-variant">
          {{ t('note.latestHistoryAt', { time: formatDateTime(latestHistory.gmtCreate) }) }}
        </div>
      </div>

      <article v-for="history in histories" :key="history.id" class="app-card w-full text-left transition hover:-translate-y-1">
        <div class="flex items-start justify-between gap-4">
          <div class="min-w-0 flex-1">
            <div class="flex flex-wrap items-center gap-2">
              <span class="font-label-bold text-on-surface">{{ t('note.version', { version: history.version }) }}</span>
              <span class="rounded-full bg-primary-fixed/30 px-2 py-0.5 text-[10px] font-bold uppercase tracking-[0.16em] text-primary">
                {{ t(`note.historySaveType.${history.saveType || 'AUTO'}`) }}
              </span>
            </div>
            <div class="mt-2 truncate text-body-secondary text-on-surface-variant">{{ history.title }}</div>
            <div class="mt-2 line-clamp-2 text-caption text-stone-500">{{ history.changeSummary || t('note.historyNoSummary') }}</div>
          </div>
          <div class="shrink-0 text-right text-caption text-on-surface-variant">{{ formatDateTime(history.gmtCreate) }}</div>
        </div>
        <div class="mt-4 flex items-center justify-between gap-3 border-t border-outline-variant/20 pt-3">
          <div class="flex items-center gap-1.5 text-[11px] font-bold uppercase tracking-[0.14em] text-primary/70">
            <span class="material-symbols-outlined text-[14px]">article</span>
            {{ t('note.characters', { count: countContentCharacters(extractPlainText(history.content || '')) }) }}
          </div>
          <button
            class="inline-flex h-8 shrink-0 items-center gap-1.5 rounded-full border border-primary/20 px-3 text-[11px] font-bold uppercase tracking-[0.12em] text-primary transition hover:border-primary hover:bg-primary-fixed/35"
            type="button"
            @click="$emit('restore', history.version)"
          >
            <span class="material-symbols-outlined text-[14px]">settings_backup_restore</span>
            {{ t('note.restoreVersion') }}
          </button>
        </div>
      </article>

      <EmptyState v-if="!histories.length" :title="t('note.noHistoryTitle')" :description="t('note.noHistoryDescription')" icon="history" />
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { useI18n } from '@/i18n'
import type { NoteHistory } from '@/types/app'
import { formatDateTime, toTimestamp } from '@/utils/date'

const props = defineProps<{
  modelValue: boolean
  histories: NoteHistory[]
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'restore', version: number): void
}>()

const { t } = useI18n()

const drawerVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const latestHistory = computed(() =>
  props.histories.reduce<NoteHistory | null>((latest, history) => {
    if (!latest) {
      return history
    }

    const latestTime = toTimestamp(latest.gmtCreate)
    const historyTime = toTimestamp(history.gmtCreate)
    return historyTime > latestTime ? history : latest
  }, null),
)

const extractPlainText = (content: string) => {
  if (!content.trim()) {
    return ''
  }

  if (typeof DOMParser !== 'undefined' && /<[^>]+>/.test(content)) {
    const document = new DOMParser().parseFromString(content, 'text/html')
    return document.body.innerText || ''
  }

  return content
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<\/(p|div|li|h[1-6]|blockquote)>/gi, '\n')
    .replace(/<[^>]*>/g, ' ')
    .replace(/&nbsp;/gi, ' ')
}

const countContentCharacters = (content: string) => Array.from(content.replace(/\s/g, '')).length
</script>
