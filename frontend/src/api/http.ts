import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiEnvelope, LoginResponse } from '@/types/app'
import { translate } from '@/i18n'
import { useUserStore } from '@/stores/user'
import { normalizeAuthRedirect } from '@/utils/redirect'

declare module 'axios' {
  interface AxiosRequestConfig {
    silentError?: boolean
    /** 跳过 401 自动续期（用于刷新令牌请求自身，避免递归） */
    skipAuthRefresh?: boolean
    /** 标记该请求已经重试过一次 */
    authRetried?: boolean
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
let refreshPromise: Promise<boolean> | null = null
const AUTH_REQUIRED_EVENT = 'notask:auth-required'
const ANDROID_COLLAB_READY_KEY = 'notask-flow-android-collab-ready'
const REFRESH_TOKEN_URL = '/auth/refresh'

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

/**
 * 尝试使用当前会话静默续期访问令牌。
 *
 * 通过单飞锁（single-flight）保证并发 401 只触发一次续期请求，
 * 其余请求排队等待同一结果，避免刷新风暴。
 *
 * @returns 是否续期成功
 */
function tryRefreshSession(): Promise<boolean> {
  const userStore = useUserStore()
  if (!userStore.tokenValue) {
    return Promise.resolve(false)
  }
  if (!refreshPromise) {
    refreshPromise = client
      .post<LoginResponse, LoginResponse>(REFRESH_TOKEN_URL, undefined, {
        silentError: true,
        skipAuthRefresh: true,
      })
      .then((response) => {
        userStore.applyLoginState(response)
        return true
      })
      .catch(() => false)
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

/**
 * 处理鉴权失败：先尝试静默续期并重试原请求，续期失败才清除会话跳转登录页。
 *
 * @param config 原始请求配置
 * @returns 重试成功时返回响应数据，否则返回 null
 */
async function retryAfterRefresh(config: AxiosRequestConfig | undefined): Promise<unknown | null> {
  if (!config || config.skipAuthRefresh || config.authRetried) {
    return null
  }
  if (config.url?.includes(REFRESH_TOKEN_URL)) {
    return null
  }
  const refreshed = await tryRefreshSession()
  if (!refreshed) {
    return null
  }
  const retried = await client.request({ ...config, authRetried: true })
  return retried as unknown
}

function handleUnauthorized() {
  useUserStore().clearSession()
  redirectToLogin()
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
  async (response) => {
    const payload = response.data as ApiEnvelope<unknown>

    if (typeof payload?.code !== 'number') {
      return response.data
    }

    if (payload.code !== 200) {
      if (payload.code === 401) {
        const retried = await retryAfterRefresh(response.config)
        if (retried !== null) {
          return retried
        }
        handleUnauthorized()
      }

      if (!response.config.silentError) {
        notifyError(payload.message || translate('messages.requestFailed'))
      }
      return Promise.reject(new Error(payload.message || translate('messages.requestFailed')))
    }

    return payload.data
  },
  async (error: AxiosError<{ message?: string }>) => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || translate('messages.networkError')

    if (status === 401) {
      const retried = await retryAfterRefresh(error.config)
      if (retried !== null) {
        return retried
      }
      handleUnauthorized()
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
