<template>
  <el-dialog v-model="dialogVisible" append-to-body :title="t('files.moveFileTitle')" width="420px">
    <label class="block">
      <span class="mb-2 block text-label-bold text-on-surface">{{ t('files.targetFolder') }}</span>
      <el-select v-model="targetFolderValue" class="w-full">
        <el-option :label="t('files.rootFolder')" :value="0" />
        <el-option
          v-for="folder in folders"
          :key="folder.id"
          :label="`${'· '.repeat(folder.depth)}${folder.name}`"
          :value="folder.id"
        />
      </el-select>
    </label>
    <template #footer>
      <button class="app-secondary-button" type="button" @click="dialogVisible = false">{{ t('common.cancel') }}</button>
      <button class="app-primary-button" type="button" @click="$emit('confirm')">{{ t('common.confirm') }}</button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '@/i18n'

type MoveFolderOption = {
  id: number
  name: string
  depth: number
}

const props = defineProps<{
  modelValue: boolean
  targetFolderId: number
  folders: MoveFolderOption[]
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'update:targetFolderId', value: number): void
  (event: 'confirm'): void
}>()

const { t } = useI18n()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const targetFolderValue = computed({
  get: () => props.targetFolderId,
  set: (value: number) => emit('update:targetFolderId', value),
})
</script>
