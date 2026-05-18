<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Bell,
  DataAnalysis,
  Files,
  HomeFilled,
  Monitor,
  Odometer,
  Setting,
  SwitchButton,
  User,
} from '@element-plus/icons-vue'
import { useAdminStore } from '@/stores/admin'

const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()

const navItems = [
  { path: '/admin/dashboard', label: '数据大盘', icon: HomeFilled },
  { path: '/admin/users', label: '用户管理', icon: User },
  { path: '/admin/sessions', label: '会话管理', icon: Monitor },
  { path: '/admin/settings', label: '系统配置', icon: Setting },
  { path: '/admin/logs', label: '系统日志', icon: Odometer },
  { path: '/admin/monitor', label: '性能监控', icon: DataAnalysis },
  { path: '/admin/storage', label: '存储管理', icon: Files },
  { path: '/admin/system-notifications', label: '系统通知', icon: Bell },
]

const activePath = computed(() => route.path)

async function handleLogout() {
  await adminStore.logout()
  router.replace('/admin/login')
}
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="admin-brand">
        <img src="/logo.svg" alt="Notask Flow" class="admin-brand-logo" />
        <div>
          <strong>Notask Admin</strong>
          <small>Administrator</small>
        </div>
      </div>

      <nav class="admin-nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="admin-nav-item"
          :class="{ active: activePath.startsWith(item.path) }"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>
    </aside>

    <main class="admin-main">
      <header class="admin-header">
        <div>
          <h1>系统管理平台</h1>
          <p>Notask Flow operational console</p>
        </div>
        <div class="admin-header-actions">
          <span>{{ adminStore.displayName }}</span>
          <el-button :icon="SwitchButton" circle @click="handleLogout" />
        </div>
      </header>

      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.admin-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 248px minmax(0, 1fr);
  background:
    linear-gradient(135deg, rgba(0, 121, 191, 0.08), transparent 30%),
    #f6f8fb;
  color: #172033;
}

.admin-sidebar {
  border-right: 1px solid #d9e2ec;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(18px);
  padding: 24px 18px;
}

.admin-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;
}

.admin-brand-logo {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  object-fit: cover;
  box-shadow: 0 10px 24px rgba(8, 125, 182, 0.16);
}

.admin-brand strong,
.admin-brand small {
  display: block;
}

.admin-brand small {
  color: #6b778c;
  margin-top: 2px;
}

.admin-nav {
  display: grid;
  gap: 8px;
}

.admin-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 42px;
  padding: 0 12px;
  border-radius: 8px;
  color: #4a5b70;
  text-decoration: none;
  font-weight: 600;
}

.admin-nav-item.active,
.admin-nav-item:hover {
  background: #e7f3fb;
  color: #087db6;
}

.admin-main {
  min-width: 0;
  padding: 24px;
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.admin-header h1 {
  margin: 0;
  font-size: 22px;
}

.admin-header p {
  margin: 4px 0 0;
  color: #6b778c;
}

.admin-header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
