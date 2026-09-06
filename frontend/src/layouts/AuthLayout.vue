<template>
  <div class="auth-page flex flex-col">
    <header class="auth-topbar">
      <div class="auth-topbar-inner">
        <RouterLink class="auth-brand" to="/login">
          <img src="/logo.svg" alt="Notask Flow" class="auth-brand-logo" />
          <span class="auth-brand-name">Notask Flow</span>
        </RouterLink>
        <div class="auth-topbar-actions">
          <LanguageSwitcher />
          <a class="auth-help-link" href="#" :aria-label="t('auth.help')" :title="t('auth.help')">
            <span class="material-symbols-outlined">help</span>
          </a>
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

const legalDialogVisible = ref(false)
const legalDialogType = ref<LegalDocumentType>('terms')

const openLegalDialog: OpenLegalDialog = (type) => {
  legalDialogType.value = type
  legalDialogVisible.value = true
}

provide(LEGAL_DIALOG_INJECTION_KEY, openLegalDialog)
</script>
