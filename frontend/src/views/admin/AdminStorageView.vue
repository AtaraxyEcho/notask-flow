<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Refresh, Search } from '@element-plus/icons-vue'
import { adminStorageService } from '@/api/modules/admin'
import type { AdminOrphanFile, AdminStorageRank, AdminStorageSummary, PageResponse } from '@/types/app'
import { formatDateTime } from '@/utils/date'

const loading = ref(false)
const orphanLoading = ref(false)
const summary = ref<AdminStorageSummary>({
  totalFileCount: 0,
  totalStorageBytes: 0,
  orphanFileCount: 0,
  orphanStorageBytes: 0,
  deletedFileCount: 0,
  deletedStorageBytes: 0,
})
const topUsers = ref<AdminStorageRank[]>([])
const topSpaces = ref<AdminStorageRank[]>([])
const orphanPage = ref<PageResponse<AdminOrphanFile>>({
  total: 0,
  pageNum: 1,
  pageSize: 10,
  list: [],
})
const orphanQuery = reactive({
  pageNum: 1,
  pageSize: 10,
})

const summaryCards = computed(() => [
  { label: '总文件数', value: formatNumber(summary.value.totalFileCount), hint: formatBytes(summary.value.totalStorageBytes) },
  { label: '孤立文件', value: formatNumber(summary.value.orphanFileCount), hint: formatBytes(summary.value.orphanStorageBytes) },
  { label: '回收站文件', value: formatNumber(summary.value.deletedFileCount), hint: formatBytes(summary.value.deletedStorageBytes) },
])

async function loadStorage() {
  loading.value = true
  try {
    const [summaryResponse, userResponse, spaceResponse] = await Promise.all([
      adminStorageService.summary(),
      adminStorageService.topUsers(),
      adminStorageService.topSpaces(),
    ])
    summary.value = summaryResponse
    topUsers.value = userResponse
    topSpaces.value = spaceResponse
  } finally {
    loading.value = false
  }
}

async function loadOrphans() {
  orphanLoading.value = true
  try {
    orphanPage.value = await adminStorageService.orphanFiles(orphanQuery)
  } finally {
    orphanLoading.value = false
  }
}

async function scanOrphans() {
  orphanLoading.value = true
  try {
    orphanQuery.pageNum = 1
    orphanPage.value = await adminStorageService.scanOrphanFiles(orphanQuery)
    await loadStorage()
    ElMessage.success('孤立文件扫描完成')
  } finally {
    orphanLoading.value = false
  }
}

async function cleanOrphans() {
  await ElMessageBox.confirm(
    '清理后会删除所有未被业务引用的文件对象与附件记录，此操作不可恢复。确认继续吗？',
    '清理孤立文件',
    {
      type: 'warning',
      confirmButtonText: '确认清理',
      cancelButtonText: '取消',
    },
  )
  orphanLoading.value = true
  try {
    const result = await adminStorageService.cleanOrphanFiles()
    ElMessage.success(`已清理 ${result.cleanedCount} 个文件，释放 ${formatBytes(result.cleanedBytes)}`)
    if (result.failedCount > 0) {
      ElMessage.warning(`${result.failedCount} 个文件清理失败，请稍后重试`)
    }
    await Promise.all([loadStorage(), loadOrphans()])
  } finally {
    orphanLoading.value = false
  }
}

function handlePageChange(pageNum: number) {
  orphanQuery.pageNum = pageNum
  loadOrphans().catch(() => undefined)
}

function formatBytes(bytes: number) {
  if (!bytes) {
    return '0 B'
  }
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let value = bytes
  let index = 0
  while (value >= 1024 && index < units.length - 1) {
    value /= 1024
    index += 1
  }
  return `${value.toFixed(index === 0 ? 0 : 1)} ${units[index]}`
}

function formatNumber(value: number) {
  return new Intl.NumberFormat('zh-CN').format(value || 0)
}

function formatTime(value?: string) {
  return formatDateTime(value, '-')
}

onMounted(() => {
  loadStorage().catch(() => undefined)
  loadOrphans().catch(() => undefined)
})
</script>

<template>
  <section class="storage-page">
    <div class="admin-table-header">
      <div>
        <h2>存储管理</h2>
        <p>统计文件资产占用，扫描孤立文件。管理端不提供文件预览和下载，避免触碰用户隐私内容。</p>
      </div>
      <el-button :icon="Refresh" @click="loadStorage">刷新</el-button>
    </div>

    <div v-loading="loading" class="summary-grid">
      <article v-for="item in summaryCards" :key="item.label" class="summary-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.hint }}</small>
      </article>
    </div>

    <div class="rank-grid">
      <section class="admin-panel">
        <div class="panel-title">
          <h3>用户存储 Top 10</h3>
          <p>按上传者聚合统计。</p>
        </div>
        <div class="rank-list">
          <div v-for="item in topUsers" :key="item.targetId" class="rank-item">
            <div>
              <strong>{{ item.targetName }}</strong>
              <small>{{ item.targetDescription || '无邮箱信息' }}</small>
            </div>
            <span>{{ formatBytes(item.storageBytes) }}</span>
          </div>
          <el-empty v-if="!topUsers.length" description="暂无用户存储数据" :image-size="90" />
        </div>
      </section>

      <section class="admin-panel">
        <div class="panel-title">
          <h3>团队空间 Top 10</h3>
          <p>按空间聚合统计。</p>
        </div>
        <div class="rank-list">
          <div v-for="item in topSpaces" :key="item.targetId" class="rank-item">
            <div>
              <strong>{{ item.targetName }}</strong>
              <small>{{ item.targetDescription || '未知类型' }}</small>
            </div>
            <span>{{ formatBytes(item.storageBytes) }}</span>
          </div>
          <el-empty v-if="!topSpaces.length" description="暂无空间存储数据" :image-size="90" />
        </div>
      </section>
    </div>

    <section class="admin-panel">
      <div class="panel-title orphan-title">
        <div>
          <h3>孤立文件</h3>
          <p>孤立文件指没有任何业务引用的附件，清理前请先扫描确认。</p>
        </div>
        <div class="orphan-actions">
          <el-button :icon="Search" @click="scanOrphans">扫描</el-button>
          <el-button type="danger" :icon="Delete" :disabled="!orphanPage.total" @click="cleanOrphans">清理孤立文件</el-button>
        </div>
      </div>

      <el-table v-loading="orphanLoading" :data="orphanPage.list" border>
        <el-table-column prop="fileName" label="文件名" min-width="220" show-overflow-tooltip />
        <el-table-column label="大小" width="110">
          <template #default="{ row }">{{ formatBytes(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="mimeType" label="类型" width="150" show-overflow-tooltip />
        <el-table-column prop="uploaderName" label="上传者" width="140" show-overflow-tooltip />
        <el-table-column prop="spaceName" label="空间" width="160" show-overflow-tooltip />
        <el-table-column label="上传时间" width="180">
          <template #default="{ row }">{{ formatTime(row.gmtCreate) }}</template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="orphanPage.total"
          :page-size="orphanQuery.pageSize"
          :current-page="orphanQuery.pageNum"
          @current-change="handlePageChange"
        />
      </div>
    </section>
  </section>
</template>

<style scoped>
.storage-page {
  display: grid;
  gap: 18px;
}

.admin-table-header,
.orphan-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.admin-table-header h2,
.panel-title h3 {
  margin: 0;
}

.admin-table-header h2 {
  font-size: 20px;
}

.admin-table-header p,
.panel-title p {
  margin: 4px 0 0;
  color: #6b778c;
}

.summary-grid,
.rank-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.rank-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.summary-card,
.admin-panel {
  border: 1px solid #dfe7f0;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 14px 34px rgba(31, 45, 61, 0.06);
}

.summary-card {
  display: grid;
  gap: 8px;
  padding: 18px;
}

.summary-card span,
.summary-card small {
  color: #6b778c;
}

.summary-card strong {
  color: #172033;
  font-size: 28px;
}

.admin-panel {
  padding: 20px;
}

.rank-list {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.rank-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  background: #f8fbff;
}

.rank-item strong,
.rank-item small {
  display: block;
}

.rank-item strong {
  color: #172033;
}

.rank-item small {
  margin-top: 3px;
  color: #8a98aa;
}

.rank-item span {
  flex-shrink: 0;
  color: #087db6;
  font-weight: 700;
}

.orphan-actions {
  display: flex;
  gap: 10px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 1100px) {
  .summary-grid,
  .rank-grid {
    grid-template-columns: 1fr;
  }
}
</style>
