import type { App, ComputedRef, InjectionKey } from 'vue'
import { computed, inject, reactive } from 'vue'
import enUS from './locales/en-US'
import zhCN from './locales/zh-CN'

export type Locale = 'zh-CN' | 'en-US'

type MessageValue = string | MessageTree

interface MessageTree {
  [key: string]: MessageValue
}

interface TranslateParams {
  [key: string]: string | number
}

interface I18nContext {
  locale: ComputedRef<Locale>
  setLocale: (locale: Locale) => void
  toggleLocale: () => void
  t: (key: string, params?: TranslateParams) => string
}

const STORAGE_KEY = 'notask-flow-locale'

const messages: Record<Locale, MessageTree> = {
  'zh-CN': zhCN,
  'en-US': enUS,
}

const resolveInitialLocale = (): Locale => {
  const savedLocale = window.localStorage.getItem(STORAGE_KEY)
  if (savedLocale === 'zh-CN' || savedLocale === 'en-US') {
    return savedLocale
  }
  return window.navigator.language.toLowerCase().startsWith('zh') ? 'zh-CN' : 'en-US'
}

const state = reactive({
  locale: resolveInitialLocale(),
})

const readMessage = (tree: MessageTree, key: string): string | null => {
  const value = key.split('.').reduce<MessageValue | undefined>((current, segment) => {
    if (!current || typeof current === 'string') {
      return undefined
    }
    return current[segment]
  }, tree)

  return typeof value === 'string' ? value : null
}

const interpolate = (template: string, params?: TranslateParams) => {
  if (!params) {
    return template
  }
  return Object.entries(params).reduce(
    (result, [key, value]) => result.split(`{${key}}`).join(String(value)),
    template,
  )
}

const setLocale = (locale: Locale) => {
  state.locale = locale
  window.localStorage.setItem(STORAGE_KEY, locale)
}

const t = (key: string, params?: TranslateParams) => {
  const message = readMessage(messages[state.locale], key) || readMessage(messages['zh-CN'], key) || key
  return interpolate(message, params)
}

export const translate = t

const i18nContext: I18nContext = {
  locale: computed(() => state.locale),
  setLocale,
  toggleLocale: () => setLocale(state.locale === 'zh-CN' ? 'en-US' : 'zh-CN'),
  t,
}

export const i18nKey: InjectionKey<I18nContext> = Symbol('notask-flow-i18n')

export const i18n = {
  install(app: App) {
    app.provide(i18nKey, i18nContext)
  },
}

export const useI18n = () => inject(i18nKey, i18nContext)
