import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiEnvelope } from '@/types/app'
import { useAdminStore } from '@/stores/admin'

const adminClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000,
})

let redirectingToAdminLogin = false

function redirectToAdminLogin() {
  const current = window.location.pathname + window.location.search
  if (current.startsWith('/admin/login') || redirectingToAdminLogin) {
    return
  }

  redirectingToAdminLogin = true
  window.dispatchEvent(
    new CustomEvent('notask:admin-auth-required', {
      detail: {
        redirect: current,
      },
    }),
  )
  window.setTimeout(() => {
    redirectingToAdminLogin = false
  }, 500)
}

adminClient.interceptors.request.use((config) => {
  const adminStore = useAdminStore()
  if (adminStore.tokenValue) {
    config.headers.Authorization = `Bearer ${adminStore.tokenValue}`
  }
  return config
})

adminClient.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiEnvelope<unknown>
    if (typeof payload?.code !== 'number') {
      return response.data
    }
    if (payload.code !== 200) {
      if (payload.code === 401) {
        useAdminStore().clearSession()
        redirectToAdminLogin()
      }
      if (!response.config.silentError) {
        ElMessage.error(payload.message || '请求失败')
      }
      return Promise.reject(new Error(payload.message || '请求失败'))
    }
    return payload.data
  },
  (error: AxiosError<{ message?: string }>) => {
    if (error.response?.status === 401) {
      useAdminStore().clearSession()
      redirectToAdminLogin()
    } else if (!error.config?.silentError) {
      ElMessage.error(error.response?.data?.message || error.message || '网络异常')
    }
    return Promise.reject(error)
  },
)

export const adminHttp = {
  get<T>(url: string, config?: AxiosRequestConfig) {
    return adminClient.get<T, T>(url, config)
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return adminClient.post<T, T>(url, data, config)
  },
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return adminClient.put<T, T>(url, data, config)
  },
  patch<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return adminClient.patch<T, T>(url, data, config)
  },
  delete<T>(url: string, config?: AxiosRequestConfig) {
    return adminClient.delete<T, T>(url, config)
  },
}
