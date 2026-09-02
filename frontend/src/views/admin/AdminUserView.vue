<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Delete, Key, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminSessionService, adminUserService } from '@/api/modules/admin'
import type { AdminUser, AdminUserStats, PageResponse, UserStatus } from '@/types/app'
import { formatDateTime } from '@/utils/date'

const loading = ref(false)
const statsLoading = ref(false)
const userPage = ref<PageResponse<AdminUser>>({
  total: 0,
  pageNum: 1,
  pageSize: 10,
  list: [],
})
const stats = ref<AdminUserStats>({
  totalUsers: 0,
  todayNewUsers: 0,
  disabledUsers: 0,
  onlineUsers: 0,
})
const query = reactive({
  keyword: '',
  status: '' as UserStatus | '',
  pageNum: 1,
  pageSize: 10,
})

const statCards = computed(() => [
  { label: '总用户数', value: stats.value.totalUsers },
  { label: '今日新增', value: stats.value.todayNewUsers },
  { label: '已禁用', value: stats.value.disabledUsers },
  { label: '当前在线', value: stats.value.onlineUsers },
])

async function loadStats() {
  statsLoading.value = true
  try {
    stats.value = await adminUserService.stats()
  } finally {
    statsLoading.value = false
  }
}

async function loadUsers() {
  loading.value = true
  try {
    userPage.value = await adminUserService.page({
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    })
  } finally {
    loading.value = false
  }
}

async function refreshAll() {
  await Promise.all([loadStats(), loadUsers()])
}

function handleSearch() {
  query.pageNum = 1
  loadUsers()
}

async function updateStatus(user: AdminUser) {
  const nextStatus: UserStatus = user.status === 'DISABLED' ? 'NORMAL' : 'DISABLED'
  const actionText = nextStatus === 'DISABLED' ? '禁用' : '启用'
  await ElMessageBox.confirm(`确认${actionText}用户「${user.nickname || user.username}」？`, '二次确认', {
    confirmButtonText: actionText,
    cancelButtonText: '取消',
    type: nextStatus === 'DISABLED' ? 'warning' : 'info',
  })
  await adminUserService.updateStatus(user.id, nextStatus)
  ElMessage.success(`${actionText}成功`)
  await refreshAll()
}

async function resetPassword(user: AdminUser) {
  const result = await ElMessageBox.prompt(`请输入用户「${user.nickname || user.username}」的新密码`, '重置密码', {
    confirmButtonText: '确认重置',
    cancelButtonText: '取消',
    inputType: 'password',
    inputPattern: /^.{6,64}$/,
    inputErrorMessage: '密码长度必须在6到64位之间',
  })
  await adminUserService.resetPassword(user.id, result.value)
  ElMessage.success('密码已重置，该用户已被强制下线')
}

async function deleteUser(user: AdminUser) {
  await ElMessageBox.confirm(
    `删除后用户将无法登录，账号唯一字段会被释放。确认删除「${user.nickname || user.username}」？`,
    '危险操作确认',
    {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    },
  )
  await adminUserService.remove(user.id)
  ElMessage.success('用户已删除')
  await refreshAll()
}

async function revokeUser(user: AdminUser) {
  await adminSessionService.revokeUser(user.id)
  ElMessage.success('已踢出该用户所有会话')
  await refreshAll()
}

function handlePageChange(pageNum: number) {
  query.pageNum = pageNum
  loadUsers()
}

function handlePageSizeChange(pageSize: number) {
  query.pageSize = pageSize
  query.pageNum = 1
  loadUsers()
}

onMounted(refreshAll)
</script>

<template>
  <section class="admin-user-page">
    <div v-loading="statsLoading" class="admin-user-stats">
      <article v-for="card in statCards" :key="card.label" class="stat-card">
        <span>{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
      </article>
    </div>

    <div class="admin-user-panel">
      <div class="admin-table-header">
        <h2>用户管理</h2>
        <el-button :icon="Refresh" @click="refreshAll">刷新</el-button>
      </div>

      <div class="admin-user-filters">
        <el-input
          v-model="query.keyword"
          clearable
          :prefix-icon="Search"
          placeholder="搜索昵称 / 用户名 / 邮箱"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="query.status" clearable placeholder="账号状态" @change="handleSearch">
          <el-option label="正常" value="NORMAL" />
          <el-option label="禁用" value="DISABLED" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="userPage.list" border>
        <el-table-column label="用户" min-width="220">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="34" :src="row.avatarUrl">
                {{ (row.nickname || row.username || '?').slice(0, 1).toUpperCase() }}
              </el-avatar>
              <div>
                <strong>{{ row.nickname || row.username }}</strong>
                <small>@{{ row.username }}</small>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'DISABLED' ? 'danger' : 'success'" effect="plain">
              {{ row.status === 'DISABLED' ? '禁用' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="在线" width="90">
          <template #default="{ row }">
            <span class="online-dot" :class="{ active: row.online }"></span>
            {{ row.online ? '在线' : '离线' }}
          </template>
        </el-table-column>
        <el-table-column label="注册时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.gmtCreate) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="updateStatus(row)">
              {{ row.status === 'DISABLED' ? '启用' : '禁用' }}
            </el-button>
            <el-button :icon="Key" link type="warning" @click="resetPassword(row)">重置密码</el-button>
            <el-button link @click="revokeUser(row)">踢出</el-button>
            <el-button :icon="Delete" link type="danger" @click="deleteUser(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="admin-pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :current-page="query.pageNum"
          :page-size="query.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="userPage.total"
          @current-change="handlePageChange"
          @size-change="handlePageSizeChange"
        />
      </div>
    </div>
  </section>
</template>

<style scoped>
.admin-user-page {
  display: grid;
  gap: 16px;
}

.admin-user-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.stat-card,
.admin-user-panel {
  border: 1px solid #dfe7f0;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 14px 34px rgba(31, 45, 61, 0.06);
}

.stat-card {
  padding: 18px;
  display: grid;
  gap: 8px;
}

.stat-card span {
  color: #6b778c;
  font-size: 13px;
}

.stat-card strong {
  color: #172033;
  font-size: 28px;
}

.admin-user-panel {
  padding: 16px;
}

.admin-table-header,
.admin-user-filters,
.admin-pagination {
  display: flex;
  align-items: center;
}

.admin-table-header {
  justify-content: space-between;
  margin-bottom: 14px;
}

.admin-table-header h2 {
  margin: 0;
  font-size: 18px;
}

.admin-user-filters {
  gap: 10px;
  margin-bottom: 14px;
}

.admin-user-filters .el-input {
  width: 280px;
}

.admin-user-filters .el-select {
  width: 150px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-cell strong,
.user-cell small {
  display: block;
}

.user-cell small {
  margin-top: 2px;
  color: #6b778c;
}

.online-dot {
  width: 8px;
  height: 8px;
  display: inline-block;
  margin-right: 6px;
  border-radius: 999px;
  background: #d64545;
}

.online-dot.active {
  background: #24a148;
}

.admin-pagination {
  justify-content: flex-end;
  margin-top: 14px;
}
</style>
