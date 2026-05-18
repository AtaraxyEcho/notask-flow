<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Delete, Refresh } from '@element-plus/icons-vue'
import { adminSessionService } from '@/api/modules/admin'
import type { LoginSession } from '@/types/app'

const loading = ref(false)
const sessions = ref<LoginSession[]>([])

async function loadSessions() {
  loading.value = true
  try {
    sessions.value = await adminSessionService.list()
  } finally {
    loading.value = false
  }
}

async function revokeSession(sessionId: string) {
  await adminSessionService.revoke(sessionId)
  await loadSessions()
}

onMounted(loadSessions)
</script>

<template>
  <section class="admin-session-page">
    <div class="admin-table-header">
      <h2>会话管理</h2>
      <el-button :icon="Refresh" @click="loadSessions">刷新</el-button>
    </div>

    <el-table v-loading="loading" :data="sessions" border>
      <el-table-column prop="username" label="用户" min-width="140" />
      <el-table-column prop="clientType" label="客户端" width="120" />
      <el-table-column prop="deviceName" label="设备" min-width="180" show-overflow-tooltip />
      <el-table-column prop="ip" label="IP" width="150" />
      <el-table-column prop="loginTime" label="登录时间" min-width="170" />
      <el-table-column prop="lastActiveTime" label="最后活跃" min-width="170" />
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-popconfirm title="确认踢出该会话？" @confirm="revokeSession(row.sessionId)">
            <template #reference>
              <el-button :icon="Delete" link type="danger">踢出</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<style scoped>
.admin-session-page {
  display: grid;
  gap: 14px;
}

.admin-table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.admin-table-header h2 {
  margin: 0;
  font-size: 18px;
}
</style>
