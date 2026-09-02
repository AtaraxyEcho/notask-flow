<template>
  <div :class="['auth-page h-[100dvh] overflow-hidden text-on-background', pageBackgroundClass]">
    <template v-if="useWaterAuthShell">
      <LoginWaterBackground />
      <div class="auth-login-overlay"></div>
    </template>
    <template v-else>
      <div class="auth-brand-image"></div>
      <div class="auth-brand-glow"></div>
    </template>

    <div class="auth-language-switcher">
      <LanguageSwitcher />
    </div>

    <div class="relative z-10 grid h-full md:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
      <section class="hidden h-full items-center px-10 md:flex lg:px-16">
        <div class="max-w-[28rem]">
          <div :class="brandBadgeClass">
            <img src="/logo.svg" alt="Notask Flow" class="h-16 w-16 rounded-[1.35rem] object-cover" />
          </div>
          <h1 class="font-display-serif text-[2.45rem] leading-tight" :class="brandTitleClass">Notask Flow</h1>
          <p class="mt-4 font-title-serif text-[1.55rem] leading-snug" :class="brandSubtitleClass">
            {{ t('auth.brandSlogan') }}
          </p>
          <div class="mt-10">
            <div :class="brandAccentClass"></div>
          </div>
        </div>
      </section>

      <section class="flex min-h-0 items-center justify-center px-4 py-4 sm:px-6 sm:py-5 lg:px-10">
        <div :class="['w-full', panelMaxWidthClass]">
          <div v-if="showMobileBrand" class="mb-6 text-center md:hidden">
            <div :class="mobileBadgeClass">
              <img src="/logo.svg" alt="Notask Flow" class="h-11 w-11 rounded-2xl object-cover" />
            </div>
            <div class="font-display-serif text-2xl" :class="mobileTitleClass">Notask Flow</div>
            <p class="mt-2 text-sm" :class="mobileSubtitleClass">{{ t('auth.mobileSlogan') }}</p>
            <div class="mt-4 flex justify-center">
              <div :class="brandAccentClass"></div>
            </div>
          </div>

          <div :class="cardClass">
            <router-view />
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import LanguageSwitcher from '@/components/common/LanguageSwitcher.vue'
import LoginWaterBackground from '@/components/shared/LoginWaterBackground.vue'
import { useI18n } from '@/i18n'

const route = useRoute()
const { t } = useI18n()
const isLoginPage = computed(() => route.name === 'login')
const isRegisterPage = computed(() => route.name === 'register')
const isForgotPasswordPage = computed(() => route.name === 'forgot-password')
const isResetPasswordPage = computed(() => route.name === 'reset-password')
const useWaterAuthShell = computed(() =>
  isLoginPage.value || isRegisterPage.value || isForgotPasswordPage.value || isResetPasswordPage.value,
)
const showMobileBrand = computed(() => true)

const pageBackgroundClass = computed(() => (useWaterAuthShell.value ? 'bg-black' : 'bg-surface-container'))
const panelMaxWidthClass = computed(() => {
  if (isRegisterPage.value) {
    return 'max-w-[30rem]'
  }

  if (isLoginPage.value) {
    return 'max-w-[28.75rem]'
  }

  return 'max-w-[28rem]'
})

const cardClass = computed(() => {
  if (useWaterAuthShell.value) {
    return 'auth-card auth-card--login'
  }

  return isRegisterPage.value ? 'auth-card auth-card--solid' : 'auth-card auth-card--glass'
})

const brandBadgeClass = computed(() =>
  useWaterAuthShell.value
    ? 'mb-8 flex h-20 w-20 items-center justify-center rounded-[1.7rem] border border-[#fff0b3]/20 bg-white/5 shadow-ambient backdrop-blur-xl'
    : 'mb-8 flex h-20 w-20 items-center justify-center rounded-[1.7rem] border border-outline-variant/40 bg-surface shadow-ambient'
)
const brandTitleClass = computed(() => (useWaterAuthShell.value ? 'text-[#fff0b3]' : 'text-on-surface'))
const brandSubtitleClass = computed(() =>
  useWaterAuthShell.value ? 'text-[rgba(255,240,179,0.72)]' : 'text-on-surface-variant'
)
const brandAccentClass = computed(() =>
  useWaterAuthShell.value ? 'auth-brand-accent auth-brand-accent--login' : 'auth-brand-accent'
)

const mobileBadgeClass = computed(() =>
  useWaterAuthShell.value
    ? 'mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-[1.25rem] border border-[#fff0b3]/20 bg-white/5 shadow-ambient backdrop-blur-xl'
    : 'mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-[1.25rem] border border-outline-variant/40 bg-surface shadow-ambient'
)
const mobileTitleClass = computed(() => (useWaterAuthShell.value ? 'text-[#fff0b3]' : 'text-on-surface'))
const mobileSubtitleClass = computed(() =>
  useWaterAuthShell.value ? 'text-[rgba(255,240,179,0.72)]' : 'text-on-surface-variant'
)
</script>

<style scoped>
.auth-page {
  position: relative;
  isolation: isolate;
}

.shadow-ambient {
  box-shadow: 0 18px 35px rgba(0, 0, 0, 0.06);
}

.auth-brand-image,
.auth-brand-glow,
.auth-login-overlay {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.auth-language-switcher {
  position: absolute;
  top: clamp(0.75rem, 2vw, 1.25rem);
  right: clamp(0.75rem, 2vw, 1.5rem);
  z-index: 30;
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 9999px;
  background: rgba(255, 248, 246, 0.82);
  box-shadow: 0 18px 35px rgba(0, 0, 0, 0.12);
  backdrop-filter: blur(16px);
}

.auth-language-switcher :deep(button) {
  color: var(--primary);
}

.auth-brand-image {
  background-image: url('https://lh3.googleusercontent.com/aida-public/AB6AXuC-nohngofCNNm1nMSvB6UPugMzMsxrnQiWUTi7iKZ8FnKgDHsJXWda2LPhGI73xeOXvNHyLgaP_xrBZrM6o78cvAPa-eI6tw3DiNUqjSieoUXVrA1zFE4dztL4Lkh8bZt12jl4BF13JbiALYGrhCydB9896qs_IW0fQ7lrv-Rm3xpfaO4Puum0rCAjUQKVVRDmIN90TcUJ3GIgXglXBwH-id23f41hKqUbTftH03A8TeNWsiTB-A3u4kAX2F5c8OetXkVXNjn1sQ6u');
  background-position: center;
  background-size: cover;
  mix-blend-mode: multiply;
  opacity: 0.18;
}

.auth-brand-glow {
  background:
    radial-gradient(circle at 18% 18%, rgba(255, 181, 158, 0.34), transparent 34%),
    radial-gradient(circle at 72% 38%, rgba(255, 255, 255, 0.22), transparent 18%),
    radial-gradient(circle at 78% 74%, rgba(159, 65, 34, 0.14), transparent 28%);
}

.auth-login-overlay {
  background:
    radial-gradient(circle at 16% 20%, rgba(255, 240, 179, 0.08), transparent 18%),
    linear-gradient(90deg, rgba(0, 0, 0, 0.32) 0%, rgba(0, 0, 0, 0.16) 38%, rgba(0, 0, 0, 0.32) 100%);
}

.auth-card {
  max-height: calc(100dvh - 2rem);
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-width: none;
}

.auth-card::-webkit-scrollbar {
  display: none;
}

.auth-card--login {
  border-radius: 1.75rem;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(255, 248, 239, 0.9);
  padding: clamp(1.5rem, 2.8vw, 2.25rem);
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(18px);
}

.auth-card--glass {
  border-radius: 1.75rem;
  border: 1px solid rgba(255, 255, 255, 0.45);
  background: rgba(255, 255, 255, 0.84);
  padding: clamp(1.5rem, 2.8vw, 2.5rem);
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.08);
  backdrop-filter: blur(18px);
}

.auth-card--solid {
  border-radius: 1.25rem;
  border: 1px solid rgba(240, 230, 225, 0.92);
  background: rgba(255, 255, 255, 0.92);
  padding: clamp(1.5rem, 2.6vw, 2rem);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.08);
  backdrop-filter: blur(12px);
}

.auth-brand-accent {
  position: relative;
  width: 4.5rem;
  height: 0.3rem;
  overflow: hidden;
  border-radius: 9999px;
  background: rgba(159, 65, 34, 0.16);
}

.auth-brand-accent::before,
.auth-brand-accent::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
}

.auth-brand-accent::before {
  width: 36%;
  background: rgba(30, 27, 25, 0.5);
  animation: auth-accent-flow 3.4s cubic-bezier(0.4, 0, 0.2, 1) infinite;
}

.auth-brand-accent::after {
  width: 60%;
  background: linear-gradient(90deg, rgba(30, 27, 25, 0), rgba(30, 27, 25, 0.2), rgba(30, 27, 25, 0));
  transform: translateX(-120%);
  animation: auth-accent-glow 3.4s ease-in-out infinite;
}

.auth-brand-accent--login {
  background: rgba(255, 240, 179, 0.18);
}

.auth-brand-accent--login::before {
  background: rgba(255, 240, 179, 0.76);
}

.auth-brand-accent--login::after {
  background: linear-gradient(90deg, rgba(255, 240, 179, 0), rgba(255, 240, 179, 0.4), rgba(255, 240, 179, 0));
}

@keyframes auth-accent-flow {
  0% {
    transform: translateX(-140%);
    opacity: 0.1;
  }

  20% {
    opacity: 0.95;
  }

  100% {
    transform: translateX(320%);
    opacity: 0.08;
  }
}

@keyframes auth-accent-glow {
  0% {
    transform: translateX(-140%);
    opacity: 0;
  }

  30% {
    opacity: 0.8;
  }

  100% {
    transform: translateX(250%);
    opacity: 0;
  }
}

@media (max-height: 860px) {
  .auth-card--login,
  .auth-card--glass,
  .auth-card--solid {
    padding: 1.25rem 1.4rem;
  }
}
</style>
