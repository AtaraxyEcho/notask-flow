<template>
  <el-dialog v-model="dialogVisible" append-to-body :title="t('projectDetail.associateDocs')" width="620px">
    <div class="space-y-4">
      <div class="text-body-secondary text-on-surface-variant">
        {{ t('projectDetail.associateDocsDescription') }}
      </div>
      <el-select v-model="selectedIds" multiple filterable class="w-full" :placeholder="t('projectDetail.selectDocsPlaceholder')">
        <el-option v-for="note in notes" :key="note.id" :label="note.title" :value="note.id" />
      </el-select>
    </div>
    <template #footer>
      <button class="app-secondary-button" type="button" @click="dialogVisible = false">{{ t('common.cancel') }}</button>
      <button class="app-primary-button" type="button" @click="$emit('confirm')">{{ t('projectDetail.confirmAssociate') }}</button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '@/i18n'
import type { Note } from '@/types/app'

const props = defineProps<{
  modelValue: boolean
  selectedNoteIds: number[]
  notes: Note[]
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'update:selectedNoteIds', value: number[]): void
  (event: 'confirm'): void
}>()

const { t } = useI18n()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const selectedIds = computed({
  get: () => props.selectedNoteIds,
  set: (value: number[]) => emit('update:selectedNoteIds', value),
})
</script>
