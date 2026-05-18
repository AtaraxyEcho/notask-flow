<template>
  <el-dialog v-model="dialogVisible" append-to-body :title="t('files.operationLogsTitle')" width="640px">
    <div v-if="logs.list.length" class="space-y-3">
      <div v-for="log in logs.list" :key="log.id" class="rounded-2xl border border-outline-variant/40 bg-surface p-4">
        <div class="flex items-center justify-between gap-3">
          <span class="font-semibold text-on-surface">{{ operationLabel(log.operationType) }}</span>
          <span class="text-caption text-on-surface-variant">{{ formatDateTime(log.gmtCreate) }}</span>
        </div>
        <p class="mt-2 text-sm text-on-surface-variant">{{ log.detail || '-' }}</p>
      </div>
    </div>
    <p v-else class="rounded-2xl bg-surface-container-low p-8 text-center text-on-surface-variant">
      {{ t('files.noLogs') }}
    </p>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '@/i18n'
import type { FileOperationLog, PageResponse } from '@/types/app'
import { formatDateTime } from '@/utils/date'

const props = defineProps<{
  modelValue: boolean
  logs: PageResponse<FileOperationLog>
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
}>()

const { t } = useI18n()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const operationLabel = (operationType: string) => {
  const keyMap: Record<string, string> = {
    UPLOAD: 'files.operation.upload',
    UPDATE: 'files.operation.update',
    TRASH: 'files.operation.trash',
    RESTORE: 'files.operation.restore',
    PHYSICAL_DELETE: 'files.operation.physicalDelete',
  }
  return t(keyMap[operationType] || operationType)
}
</script>
