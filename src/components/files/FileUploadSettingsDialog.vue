<template>
  <el-dialog v-model="dialogVisible" append-to-body :title="t('files.uploadSettingsTitle')" width="460px">
    <div class="space-y-4">
      <label class="block">
        <span class="mb-2 block text-label-bold text-on-surface">{{ t('files.maxFileSizeMb') }}</span>
        <el-input-number v-model="maxFileSizeValue" class="w-full" :min="1" :step="10" />
      </label>
      <label class="block">
        <span class="mb-2 block text-label-bold text-on-surface">{{ t('files.multipartThresholdMb') }}</span>
        <el-input-number v-model="multipartThresholdValue" class="w-full" :min="1" :step="5" />
      </label>
      <label class="block">
        <span class="mb-2 block text-label-bold text-on-surface">{{ t('files.chunkSizeMb') }}</span>
        <el-input-number v-model="chunkSizeValue" class="w-full" :min="1" :step="1" />
      </label>
      <p class="rounded-2xl bg-surface-container-low px-4 py-3 text-sm leading-6 text-on-surface-variant">
        {{ t('files.uploadSettingsHint') }}
      </p>
    </div>
    <template #footer>
      <button class="app-secondary-button" type="button" @click="dialogVisible = false">{{ t('common.cancel') }}</button>
      <button class="app-primary-button" type="button" @click="$emit('save')">{{ t('common.save') }}</button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '@/i18n'

const props = defineProps<{
  modelValue: boolean
  maxFileSizeMb: number
  multipartThresholdMb: number
  chunkSizeMb: number
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'update:maxFileSizeMb', value: number): void
  (event: 'update:multipartThresholdMb', value: number): void
  (event: 'update:chunkSizeMb', value: number): void
  (event: 'save'): void
}>()

const { t } = useI18n()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const maxFileSizeValue = computed({
  get: () => props.maxFileSizeMb,
  set: (value: number) => emit('update:maxFileSizeMb', value),
})

const multipartThresholdValue = computed({
  get: () => props.multipartThresholdMb,
  set: (value: number) => emit('update:multipartThresholdMb', value),
})

const chunkSizeValue = computed({
  get: () => props.chunkSizeMb,
  set: (value: number) => emit('update:chunkSizeMb', value),
})
</script>
