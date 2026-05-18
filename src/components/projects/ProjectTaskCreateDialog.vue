<template>
  <el-dialog v-model="dialogVisible" append-to-body destroy-on-close :title="t('projectDetail.newProjectTask')" width="760px">
    <div class="grid gap-4">
      <label class="block">
        <span class="mb-2 block text-label-bold text-on-surface">{{ t('task.titleLabel') }}</span>
        <input v-model="form.title" class="app-input w-full px-4 py-3" :placeholder="t('task.titlePlaceholder')" />
      </label>

      <label class="block">
        <span class="mb-2 block text-label-bold text-on-surface">{{ t('task.descriptionLabel') }}</span>
        <textarea
          v-model="form.description"
          rows="4"
          class="app-input w-full resize-none px-4 py-3"
          :placeholder="t('task.descriptionPlaceholder')"
        ></textarea>
      </label>

      <div class="grid gap-4 lg:grid-cols-3">
        <label class="block">
          <span class="mb-2 block text-label-bold text-on-surface">{{ t('task.assigneeLabel') }}</span>
          <el-select v-model="form.assigneeId" clearable class="w-full" :placeholder="t('projectDetail.optionalAssigneePlaceholder')">
            <el-option
              v-for="member in assignableMembers"
              :key="member.userId"
              :label="member.nickname || member.username"
              :value="member.userId"
            />
          </el-select>
        </label>

        <label class="block">
          <span class="mb-2 block text-label-bold text-on-surface">{{ t('task.priorityLabel') }}</span>
          <el-select v-model="form.priority" class="w-full">
            <el-option :label="t('task.priority.low')" value="LOW" />
            <el-option :label="t('task.priority.medium')" value="MEDIUM" />
            <el-option :label="t('task.priority.high')" value="HIGH" />
          </el-select>
        </label>

        <label class="block">
          <span class="mb-2 block text-label-bold text-on-surface">{{ t('task.deadlineLabel') }}</span>
          <el-date-picker
            v-model="form.deadline"
            class="w-full"
            clearable
            format="YYYY-MM-DD:HH:mm:ss"
            :placeholder="t('task.deadlinePlaceholder')"
            type="datetime"
            value-format="YYYY-MM-DD:HH:mm:ss"
          />
        </label>
      </div>

      <label class="block">
        <span class="mb-2 block text-label-bold text-on-surface">{{ t('task.responsibilityLabel') }}</span>
        <input v-model="form.responsibility" class="app-input w-full px-4 py-3" :placeholder="t('projectDetail.responsibilityPlaceholder')" />
      </label>
    </div>

    <template #footer>
      <div class="flex w-full items-center justify-end gap-3 border-t border-outline-variant/20 pt-4">
        <button class="app-secondary-button" type="button" @click="dialogVisible = false">{{ t('common.cancel') }}</button>
        <button class="app-primary-button !px-6 !py-2.5" type="button" @click="$emit('submit')">{{ t('task.newTask') }}</button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '@/i18n'
import type { ProjectMember, SpaceMember, TaskPriority } from '@/types/app'

type ProjectTaskCreateForm = {
  title: string
  description: string
  priority: TaskPriority
  deadline: string
  assigneeId?: number
  responsibility: string
}

type AssignableMember = Pick<SpaceMember, 'userId' | 'username' | 'nickname'> | Pick<ProjectMember, 'userId' | 'username' | 'nickname'>

const props = defineProps<{
  modelValue: boolean
  form: ProjectTaskCreateForm
  assignableMembers: AssignableMember[]
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'submit'): void
}>()

const { t } = useI18n()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})
</script>
