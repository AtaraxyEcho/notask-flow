import { defineStore } from 'pinia'
import type { NotificationSetting, PersonalThemePreset, SidebarMode, ThemeMode } from '@/types/app'

interface UiState {
  themeMode: ThemeMode
  personalThemePreset: PersonalThemePreset
  sidebarMode: SidebarMode
  globalSearchOpen: boolean
  pageLoading: boolean
}

export const useUiStore = defineStore('ui', {
  state: (): UiState => ({
    themeMode: 'system',
    personalThemePreset: 'sunrise',
    sidebarMode: 'expanded',
    globalSearchOpen: false,
    pageLoading: false,
  }),
  actions: {
    setThemeMode(mode: ThemeMode) {
      this.themeMode = mode
    },
    setPersonalThemePreset(preset: PersonalThemePreset) {
      this.personalThemePreset = preset
    },
    setSidebarMode(mode: SidebarMode) {
      this.sidebarMode = mode
    },
    setGlobalSearchOpen(value: boolean) {
      this.globalSearchOpen = value
    },
    setPageLoading(value: boolean) {
      this.pageLoading = value
    },
    applySettings(settings: Partial<NotificationSetting>) {
      if (settings.themeMode) {
        this.themeMode = settings.themeMode
      }
      if (settings.personalThemePreset) {
        this.personalThemePreset = settings.personalThemePreset
      }
      if (settings.sidebarMode) {
        this.sidebarMode = settings.sidebarMode
      }
    },
  },
  persist: {
    key: 'notask-flow-ui',
    pick: ['themeMode', 'personalThemePreset', 'sidebarMode'],
  },
})
