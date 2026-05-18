import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import App from './App.vue'
import { i18n } from './i18n'
import router from './router'
import { useAdminStore } from './stores/admin'
import { useUserStore } from './stores/user'
import './styles/main.css'

const app = createApp(App)

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

app.use(pinia)
app.use(router)
app.use(ElementPlus)
app.use(i18n)

window.addEventListener('notask:auth-required', (event) => {
  const redirect = event instanceof CustomEvent ? String(event.detail?.redirect || '/app/notes') : '/app/notes'
  router
    .replace({
      path: '/login',
      query: {
        redirect,
      },
    })
    .catch(() => undefined)
})

window.addEventListener('notask:admin-auth-required', (event) => {
  const redirect = event instanceof CustomEvent ? String(event.detail?.redirect || '/admin/dashboard') : '/admin/dashboard'
  router
    .replace({
      path: '/admin/login',
      query: {
        redirect,
      },
    })
    .catch(() => undefined)
})

window.addEventListener('storage', (event) => {
  if (event.key !== 'notask-flow-user') {
    return
  }

  try {
    useUserStore().syncPersistedSession(event.newValue ? JSON.parse(event.newValue) : null)
  } catch {
    useUserStore().clearSession()
  }
})

window.addEventListener('storage', (event) => {
  if (event.key !== 'notask-flow-admin') {
    return
  }

  try {
    useAdminStore().syncPersistedSession(event.newValue ? JSON.parse(event.newValue) : null)
  } catch {
    useAdminStore().clearSession()
  }
})

app.mount('#app')
