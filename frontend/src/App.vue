<template>
  <el-config-provider :locale="elementLocale">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import en from 'element-plus/es/locale/lang/en'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { useTheme } from '@/composables/useTheme'
import { useI18n } from '@/i18n'

const { initTheme } = useTheme()
const { locale } = useI18n()
const elementLocale = computed(() => (locale.value === 'zh-CN' ? zhCn : en))

initTheme()
</script>
