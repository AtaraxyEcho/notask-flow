import { watch } from 'vue'
import { gsap } from 'gsap'
import { useSpaceStore } from '@/stores/space'
import { useUiStore } from '@/stores/ui'

const themePresetMap = {
  sunrise: 'warm',
  forest: 'forest',
  ocean: 'ocean',
  midnight: 'dark',
} as const

export function useTheme() {
  const uiStore = useUiStore()
  const spaceStore = useSpaceStore()

  const syncTheme = () => {
    const root = document.documentElement
    const isTeam = spaceStore.currentSpace?.type === 'TEAM'

    root.dataset.theme = themePresetMap[uiStore.personalThemePreset]
    root.dataset.spaceType = isTeam ? 'team' : 'personal'

    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    const shouldDark =
      !isTeam &&
      (uiStore.themeMode === 'dark' ||
        (uiStore.themeMode === 'system' && prefersDark) ||
        uiStore.personalThemePreset === 'midnight')

    root.classList.toggle('dark', shouldDark)
  }

  const animateTheme = () => {
    syncTheme()

    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      return
    }

    gsap.fromTo(
      '#app',
      { opacity: 0.92, y: 8 },
      { opacity: 1, y: 0, duration: 0.28, ease: 'power2.out', clearProps: 'transform' },
    )
  }

  const initTheme = () => {
    syncTheme()

    watch(
      () => [uiStore.themeMode, uiStore.personalThemePreset, spaceStore.currentSpace?.type],
      () => animateTheme(),
      { immediate: true },
    )
  }

  return {
    initTheme,
  }
}

