<template>
  <el-dialog v-model="dialogVisible" append-to-body :title="t('files.referencesTitle')" width="560px">
    <div v-if="references.length" class="space-y-3">
      <div v-for="reference in references" :key="reference.id" class="rounded-2xl border border-outline-variant/40 bg-surface p-4">
        <div class="flex items-center justify-between gap-3">
          <span class="app-chip">{{ reference.businessType }}</span>
          <span class="text-caption text-on-surface-variant">#{{ reference.businessId }}</span>
        </div>
        <p class="mt-2 text-sm text-on-surface-variant">{{ reference.referenceKey || '-' }}</p>
      </div>
    </div>
    <p v-else class="rounded-2xl bg-surface-container-low p-8 text-center text-on-surface-variant">
      {{ t('files.noReferences') }}
    </p>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '@/i18n'
import type { FileReference } from '@/types/app'

const props = defineProps<{
  modelValue: boolean
  references: FileReference[]
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
}>()

const { t } = useI18n()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})
</script>
