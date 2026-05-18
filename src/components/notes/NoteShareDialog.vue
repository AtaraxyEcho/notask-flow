<template>
  <el-dialog v-model="dialogVisible" append-to-body :title="t('note.shareTitle')" width="440px">
    <div class="space-y-4">
      <label class="block">
        <span class="mb-2 block text-label-bold text-on-surface">{{ t('note.expireAt') }}</span>
        <el-date-picker
          v-model="expireValue"
          class="w-full"
          clearable
          format="YYYY-MM-DD:HH:mm:ss"
          :placeholder="t('note.expireAtPlaceholder')"
          type="datetime"
          value-format="YYYY-MM-DD:HH:mm:ss"
        />
      </label>
      <div v-if="shareCode" class="app-card bg-primary-fixed/30">
        <div class="text-label-bold text-on-surface">{{ t('note.currentShareCode') }}</div>
        <div class="mt-2 break-all text-body-main text-primary">{{ shareCode }}</div>
        <div class="mt-4 text-label-bold text-on-surface">{{ t('note.readLink') }}</div>
        <div class="mt-2 break-all text-body-secondary text-on-surface-variant">{{ shareLink }}</div>
      </div>
    </div>
    <template #footer>
      <button class="app-secondary-button" type="button" @click="dialogVisible = false">{{ t('common.close') }}</button>
      <button class="app-primary-button" type="button" @click="$emit('share')">{{ t('note.generateOrUpdate') }}</button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '@/i18n'

const props = defineProps<{
  modelValue: boolean
  expire: string
  shareCode?: string
  shareLink: string
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'update:expire', value: string): void
  (event: 'share'): void
}>()

const { t } = useI18n()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const expireValue = computed({
  get: () => props.expire,
  set: (value: string) => emit('update:expire', value),
})
</script>
