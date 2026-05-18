import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiEnvelope } from '@/types/app'
import { translate } from '@/i18n'
import { useUserStore } from '@/stores/user'
import { normalizeAuthRedirect } from '@/utils/redirect'

declare module 'axios' {
  interface AxiosRequestConfig {
    silentError?: boolean
  }
}

function resolveApiBaseUrl() {
  const androidBaseUrl = window.sessionStorage.getItem('notask-flow-android-api-base-url')?.trim()
  return androidBaseUrl || import.meta.env.VITE_API_BASE_URL
}

const client = axios.create({
  baseURL: resolveApiBaseUrl(),
  timeout: 15000,
})

let lastErrorMessage = ''
let lastErrorAt = 0
let redirectingToLogin = false
const AUTH_REQUIRED_EVENT = 'notask:auth-required'
const ANDROID_COLLAB_READY_KEY = 'notask-flow-android-collab-ready'

function isAndroidStandaloneCollab() {
  return (
    window.sessionStorage.getItem(ANDROID_COLLAB_READY_KEY) === '1' ||
    window.location.pathname.startsWith('/android/collab/')
  )
}

function notifyError(message: string, type: 'error' | 'warning' = 'error') {
  const now = Date.now()
  if (message === lastErrorMessage && now - lastErrorAt < 2500) {
    return
  }

  lastErrorMessage = message
  lastErrorAt = now

  if (type === 'warning') {
    ElMessage.warning(message)
    return
  }

  ElMessage.error(message)
}

function redirectToLogin() {
  if (isAndroidStandaloneCollab()) {
    return
  }

  const current = window.location.pathname + window.location.search
  const authPagePrefixes = ['/login', '/register', '/forgot-password', '/reset-password']
  if (authPagePrefixes.some((prefix) => current.startsWith(prefix)) || redirectingToLogin) {
    return
  }

  redirectingToLogin = true
  window.dispatchEvent(
    new CustomEvent(AUTH_REQUIRED_EVENT, {
      detail: {
        redirect: normalizeAuthRedirect(current),
      },
    }),
  )
  window.setTimeout(() => {
    redirectingToLogin = false
  }, 500)
}

client.interceptors.request.use((config) => {
  const userStore = useUserStore()
  config.baseURL = resolveApiBaseUrl()
  if (userStore.tokenValue) {
    config.headers.Authorization = `Bearer ${userStore.tokenValue}`
  }
  return config
})

client.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiEnvelope<unknown>

    if (typeof payload?.code !== 'number') {
      return response.data
    }

    if (payload.code !== 200) {
      if (payload.code === 401) {
        useUserStore().clearSession()
        redirectToLogin()
      }

      if (!response.config.silentError) {
        notifyError(payload.message || translate('messages.requestFailed'))
      }
      return Promise.reject(new Error(payload.message || translate('messages.requestFailed')))
    }

    return payload.data
  },
  (error: AxiosError<{ message?: string }>) => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || translate('messages.networkError')

    if (status === 401) {
      useUserStore().clearSession()
      redirectToLogin()
    } else if (status === 403) {
      if (!error.config?.silentError) {
        notifyError(translate('messages.noPermission'), 'warning')
      }
    } else if (!error.config?.silentError) {
      notifyError(message)
    }

    return Promise.reject(error)
  },
)

export const http = {
  get<T>(url: string, config?: AxiosRequestConfig) {
    return client.get<T, T>(url, config)
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return client.post<T, T>(url, data, config)
  },
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return client.put<T, T>(url, data, config)
  },
  patch<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return client.patch<T, T>(url, data, config)
  },
  delete<T>(url: string, config?: AxiosRequestConfig) {
    return client.delete<T, T>(url, config)
  },
}
