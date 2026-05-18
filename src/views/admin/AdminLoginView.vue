<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Lock, User } from '@element-plus/icons-vue'
import { useAdminStore } from '@/stores/admin'

const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()
const loading = ref(false)

const form = reactive({
  account: 'Administrator',
  password: '',
})

async function handleLogin() {
  loading.value = true
  try {
    await adminStore.login(form)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/admin/dashboard'
    router.replace(redirect.startsWith('/admin') ? redirect : '/admin/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="admin-login">
    <section class="admin-login-panel">
      <div class="admin-login-brand">
        <span>N</span>
        <div>
          <h1>Notask Admin</h1>
          <p>System Console</p>
        </div>
      </div>

      <el-form class="admin-login-form" @submit.prevent="handleLogin">
        <el-form-item>
          <el-input v-model="form.account" :prefix-icon="User" size="large" autocomplete="username" />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            :prefix-icon="Lock"
            size="large"
            type="password"
            autocomplete="current-password"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-button class="admin-login-button" type="primary" size="large" :loading="loading" @click="handleLogin">
          登录
        </el-button>
      </el-form>
    </section>
  </main>
</template>

<style scoped>
.admin-login {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background:
    linear-gradient(140deg, rgba(8, 125, 182, 0.18), transparent 34%),
    linear-gradient(320deg, rgba(245, 108, 68, 0.14), transparent 30%),
    #f6f8fb;
}

.admin-login-panel {
  width: min(420px, 100%);
  padding: 30px;
  border: 1px solid #d9e2ec;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 18px 55px rgba(25, 48, 71, 0.12);
}

.admin-login-brand {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 26px;
}

.admin-login-brand span {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: #087db6;
  color: #fff;
  font-size: 24px;
  font-weight: 800;
}

.admin-login-brand h1 {
  margin: 0;
  font-size: 24px;
}

.admin-login-brand p {
  margin: 3px 0 0;
  color: #6b778c;
}

.admin-login-form {
  display: grid;
  gap: 4px;
}

.admin-login-button {
  width: 100%;
  margin-top: 4px;
}
</style>
