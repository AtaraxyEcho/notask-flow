<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { adminLogService } from '@/api/modules/admin'
import type { AdminLoginLog, AdminOperationLog, AdminSystemLog, PageResponse } from '@/types/app'
import { formatDateTime } from '@/utils/date'

type LogTab = 'login' | 'operation' | 'system'

const loading = ref(false)
const activeTab = ref<LogTab>('login')
const loginQuery = reactive({
  keyword: '',
  success: '' as boolean | '',
  pageNum: 1,
  pageSize: 10,
})
const operationQuery = reactive({
  keyword: '',
  success: '' as boolean | '',
  pageNum: 1,
  pageSize: 10,
})
const systemQuery = reactive({
  eventType: '',
  status: '',
  pageNum: 1,
  pageSize: 10,
})
const loginPage = ref<PageResponse<AdminLoginLog>>(emptyPage())
const operationPage = ref<PageResponse<AdminOperationLog>>(emptyPage())
const systemPage = ref<PageResponse<AdminSystemLog>>(emptyPage())

function emptyPage<T>(): PageResponse<T> {
  return {
    total: 0,
    pageNum: 1,
    pageSize: 10,
    list: [],
  }
}

async function loadCurrent() {
  if (activeTab.value === 'login') {
    await loadLoginLogs()
    return
  }
  if (activeTab.value === 'operation') {
    await loadOperationLogs()
    return
  }
  await loadSystemLogs()
}

async function loadLoginLogs() {
  loading.value = true
  try {
    loginPage.value = await adminLogService.loginLogs(loginQuery)
  } finally {
    loading.value = false
  }
}

async function loadOperationLogs() {
  loading.value = true
  try {
    operationPage.value = await adminLogService.operationLogs(operationQuery)
  } finally {
    loading.value = false
  }
}

async function loadSystemLogs() {
  loading.value = true
  try {
    systemPage.value = await adminLogService.systemLogs(systemQuery)
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  loadCurrent().catch(() => undefined)
}

function handleLoginSearch() {
  loginQuery.pageNum = 1
  loadLoginLogs().catch(() => undefined)
}

function handleOperationSearch() {
  operationQuery.pageNum = 1
  loadOperationLogs().catch(() => undefined)
}

function handleSystemSearch() {
  systemQuery.pageNum = 1
  loadSystemLogs().catch(() => undefined)
}

function formatTime(value?: string) {
  return formatDateTime(value, '-')
}

function formatBoolean(value: boolean) {
  return value ? '成功' : '失败'
}

function formatClient(value?: string) {
  return value || '-'
}

onMounted(() => {
  loadCurrent().catch(() => undefined)
})
</script>

<template>
  <section class="log-page">
    <div class="admin-table-header">
      <div>
        <h2>系统日志</h2>
        <p>集中查看登录、管理操作与系统事件失败日志，便于审计和排障。</p>
      </div>
      <el-button :icon="Refresh" @click="loadCurrent">刷新</el-button>
    </div>

    <section class="admin-panel">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="登录日志" name="login">
          <div class="toolbar">
            <el-input
              v-model="loginQuery.keyword"
              class="toolbar-input"
              clearable
              placeholder="搜索账号或IP"
              :prefix-icon="Search"
              @keyup.enter="handleLoginSearch"
              @clear="handleLoginSearch"
            />
            <el-select v-model="loginQuery.success" class="toolbar-select" placeholder="登录结果" clearable>
              <el-option label="成功" :value="true" />
              <el-option label="失败" :value="false" />
            </el-select>
            <el-button type="primary" @click="handleLoginSearch">查询</el-button>
          </div>
          <el-table v-loading="loading" :data="loginPage.list" border>
            <el-table-column prop="account" label="账号" min-width="150" show-overflow-tooltip />
            <el-table-column label="客户端" width="120">
              <template #default="{ row }">{{ formatClient(row.clientType) }}</template>
            </el-table-column>
            <el-table-column prop="ipAddress" label="IP" width="150" show-overflow-tooltip />
            <el-table-column label="结果" width="90">
              <template #default="{ row }">
                <el-tag :type="row.success ? 'success' : 'danger'">{{ formatBoolean(row.success) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="failReason" label="失败原因" min-width="180" show-overflow-tooltip />
            <el-table-column prop="userAgent" label="User-Agent" min-width="240" show-overflow-tooltip />
            <el-table-column label="时间" width="180">
              <template #default="{ row }">{{ formatTime(row.gmtCreate) }}</template>
            </el-table-column>
          </el-table>
          <div class="pager">
            <el-pagination
              background
              layout="prev, pager, next"
              :total="loginPage.total"
              :page-size="loginQuery.pageSize"
              :current-page="loginQuery.pageNum"
              @current-change="(page: number) => { loginQuery.pageNum = page; loadLoginLogs() }"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="管理操作" name="operation">
          <div class="toolbar">
            <el-input
              v-model="operationQuery.keyword"
              class="toolbar-input"
              clearable
              placeholder="搜索操作人、路径或操作"
              :prefix-icon="Search"
              @keyup.enter="handleOperationSearch"
              @clear="handleOperationSearch"
            />
            <el-select v-model="operationQuery.success" class="toolbar-select" placeholder="执行结果" clearable>
              <el-option label="成功" :value="true" />
              <el-option label="失败" :value="false" />
            </el-select>
            <el-button type="primary" @click="handleOperationSearch">查询</el-button>
          </div>
          <el-table v-loading="loading" :data="operationPage.list" border>
            <el-table-column prop="operator" label="操作人" width="160" show-overflow-tooltip />
            <el-table-column prop="method" label="方法" width="90" />
            <el-table-column prop="path" label="路径" min-width="260" show-overflow-tooltip />
            <el-table-column label="结果" width="90">
              <template #default="{ row }">
                <el-tag :type="row.success ? 'success' : 'danger'">{{ formatBoolean(row.success) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="errorMessage" label="错误信息" min-width="180" show-overflow-tooltip />
            <el-table-column prop="ipAddress" label="IP" width="150" show-overflow-tooltip />
            <el-table-column label="时间" width="180">
              <template #default="{ row }">{{ formatTime(row.gmtCreate) }}</template>
            </el-table-column>
          </el-table>
          <div class="pager">
            <el-pagination
              background
              layout="prev, pager, next"
              :total="operationPage.total"
              :page-size="operationQuery.pageSize"
              :current-page="operationQuery.pageNum"
              @current-change="(page: number) => { operationQuery.pageNum = page; loadOperationLogs() }"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="系统事件" name="system">
          <div class="toolbar">
            <el-input
              v-model="systemQuery.eventType"
              class="toolbar-input"
              clearable
              placeholder="搜索事件类型"
              :prefix-icon="Search"
              @keyup.enter="handleSystemSearch"
              @clear="handleSystemSearch"
            />
            <el-select v-model="systemQuery.status" class="toolbar-select" placeholder="处理状态" clearable>
              <el-option label="待处理" value="PENDING" />
              <el-option label="重试中" value="RETRYING" />
              <el-option label="成功" value="SUCCESS" />
              <el-option label="失败" value="FAILED" />
            </el-select>
            <el-button type="primary" @click="handleSystemSearch">查询</el-button>
          </div>
          <el-table v-loading="loading" :data="systemPage.list" border>
            <el-table-column prop="eventType" label="事件类型" min-width="180" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="110" />
            <el-table-column prop="retryCount" label="重试" width="80" />
            <el-table-column prop="failReason" label="失败原因" min-width="240" show-overflow-tooltip />
            <el-table-column prop="eventData" label="事件数据" min-width="260" show-overflow-tooltip />
            <el-table-column label="时间" width="180">
              <template #default="{ row }">{{ formatTime(row.gmtCreate) }}</template>
            </el-table-column>
          </el-table>
          <div class="pager">
            <el-pagination
              background
              layout="prev, pager, next"
              :total="systemPage.total"
              :page-size="systemQuery.pageSize"
              :current-page="systemQuery.pageNum"
              @current-change="(page: number) => { systemQuery.pageNum = page; loadSystemLogs() }"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>
  </section>
</template>

<style scoped>
.log-page {
  display: grid;
  gap: 18px;
}

.admin-table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.admin-table-header h2 {
  margin: 0;
  font-size: 20px;
}

.admin-table-header p {
  margin: 4px 0 0;
  color: #6b778c;
}

.admin-panel {
  padding: 20px;
  border: 1px solid #dfe7f0;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 14px 34px rgba(31, 45, 61, 0.06);
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar-input {
  max-width: 320px;
}

.toolbar-select {
  width: 150px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
