<template>
  <div class="auth-page flex flex-col">
    <header class="auth-topbar">
      <div class="auth-topbar-inner">
        <RouterLink class="auth-brand" to="/login">
          <img src="/logo-horizontal-ui.webp" alt="Notask Flow" class="auth-brand-logo" />
        </RouterLink>
        <div class="auth-topbar-actions">
          <LanguageSwitcher />
          <button
            type="button"
            class="auth-help-link"
            :aria-label="t('auth.help')"
            :title="t('auth.help')"
            @click="helpDialogVisible = true"
          >
            <span class="material-symbols-outlined">help</span>
          </button>
        </div>
      </div>
    </header>

    <main class="auth-main">
      <div :class="['auth-card', { 'auth-card--wide': isRegisterPage }]">
        <router-view />
      </div>
    </main>

    <footer class="auth-footer">
      <span>© {{ currentYear }} Notask Flow</span>
      <nav class="auth-footer-links">
        <button type="button" @click="openLegalDialog('terms')">{{ t('auth.terms') }}</button>
        <button type="button" @click="openLegalDialog('privacy')">{{ t('auth.privacy') }}</button>
      </nav>
    </footer>

    <LegalDocumentDialog v-model:visible="legalDialogVisible" :type="legalDialogType" />

    <el-dialog v-model="helpDialogVisible" :title="t('auth.helpTitle')" width="480px">
      <p class="text-sm text-on-surface-variant">{{ t('auth.helpIntro') }}</p>
      <ul class="mt-4 space-y-2 text-sm text-on-surface-variant">
        <li class="flex gap-2"><span>·</span><span>{{ t('auth.helpItemAccount') }}</span></li>
        <li class="flex gap-2"><span>·</span><span>{{ t('auth.helpItemPassword') }}</span></li>
        <li class="flex gap-2"><span>·</span><span>{{ t('auth.helpItemDevice') }}</span></li>
        <li class="flex gap-2"><span>·</span><span>{{ t('auth.helpItemContact') }}</span></li>
      </ul>
      <template #footer>
        <el-button type="primary" @click="helpDialogVisible = false">{{ t('auth.helpClose') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, provide, ref } from 'vue'
import { useRoute } from 'vue-router'
import LanguageSwitcher from '@/components/common/LanguageSwitcher.vue'
import LegalDocumentDialog from '@/components/shared/LegalDocumentDialog.vue'
import { LEGAL_DIALOG_INJECTION_KEY, type LegalDocumentType, type OpenLegalDialog } from '@/constants/legal'
import { useI18n } from '@/i18n'

const route = useRoute()
const { t } = useI18n()
const isRegisterPage = computed(() => route.name === 'register')
const currentYear = new Date().getFullYear()

const helpDialogVisible = ref(false)
const legalDialogVisible = ref(false)
const legalDialogType = ref<LegalDocumentType>('terms')

const openLegalDialog: OpenLegalDialog = (type) => {
  legalDialogType.value = type
  legalDialogVisible.value = true
}

provide(LEGAL_DIALOG_INJECTION_KEY, openLegalDialog)
</script>
