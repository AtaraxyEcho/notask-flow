<template>
  <el-dropdown trigger="click" @command="handleCommand">
    <button
      class="inline-flex items-center gap-1.5 rounded-full px-3 py-2 text-xs font-semibold transition-colors"
      :class="
        isTeam
          ? 'text-slate-500 hover:bg-[#0077B6]/5 hover:text-[#0077B6]'
          : 'text-on-surface-variant hover:bg-surface-container hover:text-primary'
      "
      type="button"
    >
      <span class="material-symbols-outlined text-[18px]">translate</span>
      <span>{{ localeLabel }}</span>
    </button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="zh-CN">{{ t('common.zh') }}</el-dropdown-item>
        <el-dropdown-item command="en-US">{{ t('common.english') }}</el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n, type Locale } from '@/i18n'

defineProps<{
  isTeam?: boolean
}>()

const { locale, setLocale, t } = useI18n()

const localeLabel = computed(() => (locale.value === 'zh-CN' ? t('common.zh') : 'EN'))

const handleCommand = (command: string | number | object) => {
  if (command === 'zh-CN' || command === 'en-US') {
    setLocale(command as Locale)
  }
}
</script>
