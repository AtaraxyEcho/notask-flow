<template>
  <el-dialog v-model="dialogVisible" append-to-body :title="t('files.referencesTitle')" width="620px">
    <div class="space-y-4">
      <label class="file-reference-search">
        <span class="material-symbols-outlined text-[18px] text-on-surface-variant">search</span>
        <input
          v-model="keywordValue"
          class="w-full border-none bg-transparent p-0 text-sm outline-none focus:ring-0"
          :placeholder="t('files.searchPlaceholder')"
          type="text"
          @keydown.enter.prevent="$emit('search')"
        />
      </label>
      <div v-loading="loading" class="custom-scrollbar max-h-[48vh] space-y-2 overflow-y-auto pr-1">
        <button
          v-for="file in files"
          :key="file.id"
          class="w-full rounded-2xl border border-outline-variant/40 bg-surface-container-lowest p-4 text-left transition hover:-translate-y-0.5 hover:border-primary/40 hover:shadow-sm"
          type="button"
          @click="$emit('select', file)"
        >
          <div class="flex items-center gap-3">
            <span class="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-primary-fixed/40 text-primary">
              <span class="material-symbols-outlined text-[20px]">attach_file</span>
            </span>
            <div class="min-w-0 flex-1">
              <div class="truncate font-semibold text-on-surface">{{ file.displayName || file.fileName }}</div>
              <div class="mt-1 truncate text-caption text-on-surface-variant">{{ file.fileName || file.mimeType || '-' }}</div>
            </div>
          </div>
        </button>
        <p v-if="!loading && !files.length" class="rounded-2xl bg-surface-container-low p-8 text-center text-on-surface-variant">
          {{ t('files.emptyTitle') }}
        </p>
      </div>
    </div>
    <template #footer>
      <button class="app-secondary-button" type="button" @click="dialogVisible = false">{{ t('common.close') }}</button>
      <button class="app-primary-button" type="button" @click="$emit('search')">{{ t('common.search') }}</button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '@/i18n'
import type { ManagedFile } from '@/types/app'

const props = defineProps<{
  modelValue: boolean
  keyword: string
  loading: boolean
  files: ManagedFile[]
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'update:keyword', value: string): void
  (event: 'search'): void
  (event: 'select', file: ManagedFile): void
}>()

const { t } = useI18n()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const keywordValue = computed({
  get: () => props.keyword,
  set: (value: string) => emit('update:keyword', value),
})
</script>

<style scoped>
.file-reference-search {
  align-items: center;
  background: var(--surface-container-low);
  border: 1px solid rgba(221, 192, 184, 0.55);
  border-radius: 1rem;
  display: flex;
  gap: 0.65rem;
  padding: 0.75rem 0.95rem;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, background-color 0.18s ease;
}

.file-reference-search:focus-within {
  background: var(--surface-container-lowest);
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(159, 65, 34, 0.12);
}
</style>
