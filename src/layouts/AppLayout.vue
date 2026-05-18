<template>
  <div class="min-h-screen bg-background text-on-background">
    <AppTopbar />

    <PersonalSidebar v-if="spaceStore.currentSpace?.type !== 'TEAM'" />
    <TeamSidebar v-else />

    <main
      class="min-h-screen pt-16 transition-all"
      :class="spaceStore.currentSpace?.type === 'TEAM' ? 'md:ml-72 team-main-shell' : 'md:ml-64'"
    >
      <div class="min-h-[calc(100vh-4rem)] p-4 md:p-6">
        <router-view v-slot="{ Component, route }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" :key="route.fullPath" />
          </transition>
        </router-view>
      </div>
    </main>

    <transition name="space-switch-fade">
      <div
        v-if="spaceStore.isSwitchingSpace"
        class="fixed inset-0 z-[80] flex items-center justify-center bg-background/70 backdrop-blur-md"
      >
        <div class="relative overflow-hidden rounded-[2rem] border border-outline-variant/40 bg-surface-container-lowest/95 px-8 py-7 text-center shadow-[0_24px_70px_rgba(30,27,25,0.14)]">
          <div class="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-full bg-primary-fixed/50">
            <div class="space-switch-loader"></div>
          </div>
          <p class="text-caption font-bold uppercase tracking-[0.26em] text-primary/70">
            {{ t('spaceSwitcher.switchingSpace') }}
          </p>
          <h2 class="mt-2 max-w-[320px] truncate font-title-serif text-2xl text-on-surface">
            {{ spaceStore.switchingSpace?.name || spaceStore.currentSpace?.name || t('spaceSwitcher.fallbackSpaceName') }}
          </h2>
          <p class="mt-3 text-sm text-on-surface-variant">
            {{ t('spaceSwitcher.switchingDescription') }}
          </p>
          <div class="mt-6 h-1.5 overflow-hidden rounded-full bg-surface-container-high">
            <div class="space-switch-progress h-full rounded-full bg-primary"></div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import AppTopbar from '@/components/common/AppTopbar.vue'
import PersonalSidebar from '@/components/personal/PersonalSidebar.vue'
import TeamSidebar from '@/components/team/TeamSidebar.vue'
import { useSpaceRealtimeEvents } from '@/composables/useSpaceRealtimeEvents'
import { useI18n } from '@/i18n'
import { prefetchAppRouteComponents } from '@/router'
import { useSpaceStore } from '@/stores/space'

const spaceStore = useSpaceStore()
const { t } = useI18n()
useSpaceRealtimeEvents()

onMounted(() => {
  const schedulePrefetch =
    window.requestIdleCallback ||
    ((callback: IdleRequestCallback) => window.setTimeout(() => callback({ didTimeout: false, timeRemaining: () => 0 }), 600))

  schedulePrefetch(() => {
    prefetchAppRouteComponents()
  })
})
</script>

<style scoped>
.space-switch-fade-enter-active,
.space-switch-fade-leave-active {
  transition:
    opacity 0.22s ease,
    transform 0.22s ease;
}

.space-switch-fade-enter-from,
.space-switch-fade-leave-to {
  opacity: 0;
}

.space-switch-loader {
  animation: space-switch-spin 1.1s linear infinite;
  border: 3px solid rgba(159, 65, 34, 0.16);
  border-radius: 999px;
  border-top-color: var(--primary);
  height: 38px;
  position: relative;
  width: 38px;
}

.space-switch-loader::after {
  animation: space-switch-pulse 1.4s ease-in-out infinite;
  background: var(--primary-container);
  border-radius: 999px;
  content: '';
  height: 10px;
  left: 50%;
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 10px;
}

.space-switch-progress {
  animation: space-switch-progress 1.15s ease-in-out infinite;
  transform-origin: left;
}

@keyframes space-switch-spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}

@keyframes space-switch-pulse {
  0%,
  100% {
    opacity: 0.45;
    transform: translate(-50%, -50%) scale(0.82);
  }

  50% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1.25);
  }
}

@keyframes space-switch-progress {
  0% {
    transform: translateX(-100%) scaleX(0.35);
  }

  50% {
    transform: translateX(0%) scaleX(0.92);
  }

  100% {
    transform: translateX(220%) scaleX(0.35);
  }
}
</style>
