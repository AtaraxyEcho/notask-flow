<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Bell, Refresh, Promotion } from '@element-plus/icons-vue'
import { adminSystemNotificationService } from '@/api/modules/admin'
import type { NotificationItem, PageResponse } from '@/types/app'
import { formatDateTime } from '@/utils/date'

const loading = ref(false)
const sending = ref(false)
const history = ref<PageResponse<NotificationItem>>({
  total: 0,
  pageNum: 1,
  pageSize: 10,
  list: [],
})

const form = reactive({
  title: '',
  content: '',
})

const canSubmit = computed(() => Boolean(form.title.trim() && form.content.trim()))

async function loadHistory() {
  loading.value = true
  try {
    history.value = await adminSystemNotificationService.history({
      pageNum: history.value.pageNum,
      pageSize: history.value.pageSize,
    })
  } finally {
    loading.value = false
  }
}

async function sendNotification() {
  if (!canSubmit.value) {
    ElMessage.warning('请先填写通知标题和内容')
    return
  }

  sending.value = true
  try {
    await adminSystemNotificationService.send({
      title: form.title.trim(),
      content: form.content.trim(),
    })
    form.title = ''
    form.content = ''
    ElMessage.success('系统通知已发送')
    await loadHistory()
  } finally {
    sending.value = false
  }
}

onMounted(loadHistory)
</script>

<template>
  <section class="admin-notice-page">
    <section class="notice-compose-card">
      <div class="notice-hero">
        <div class="notice-icon">
          <el-icon><Bell /></el-icon>
        </div>
        <div>
          <p class="eyebrow">Broadcast</p>
          <h2>系统通知</h2>
          <p>通知会直接推送到所有用户的通知中心，适合发布维护、升级和重要提醒。</p>
        </div>
      </div>

      <el-form class="notice-form" label-position="top" @submit.prevent="sendNotification">
        <el-form-item label="通知标题">
          <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="例如：今晚 23:00 进行短暂维护" />
        </el-form-item>
        <el-form-item label="通知内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            maxlength="1000"
            show-word-limit
            resize="none"
            placeholder="写清楚影响范围、预计时间和用户需要注意的事项。"
          />
        </el-form-item>
        <div class="notice-actions">
          <span>发送后会同步进入通知历史。</span>
          <el-button type="primary" :icon="Promotion" :disabled="!canSubmit" :loading="sending" @click="sendNotification">
            发送通知
          </el-button>
        </div>
      </el-form>
    </section>

    <section class="notice-history-card">
      <div class="admin-table-header">
        <div>
          <h2>发送历史</h2>
          <p>最近发送的系统通知会按用户分发记录展示。</p>
        </div>
        <el-button :icon="Refresh" @click="loadHistory">刷新</el-button>
      </div>

      <div v-loading="loading" class="notice-list">
        <article v-for="item in history.list" :key="item.id" class="notice-item" :class="{ unread: !item.isRead }">
          <div class="notice-item-main">
            <div class="notice-badge">
              <el-icon><Bell /></el-icon>
            </div>
            <div>
              <h3>{{ item.title }}</h3>
              <p>{{ item.content }}</p>
            </div>
          </div>
          <div class="notice-meta">
            <span>用户 #{{ item.userId }}</span>
            <time>{{ formatDateTime(item.gmtCreate) }}</time>
          </div>
        </article>

        <el-empty v-if="!history.list.length && !loading" description="暂无系统通知记录" />
      </div>
    </section>
  </section>
</template>

<style scoped>
.admin-notice-page {
  display: grid;
  grid-template-columns: minmax(340px, 460px) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.notice-compose-card,
.notice-history-card {
  border: 1px solid rgba(151, 170, 190, 0.28);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 16px 36px rgba(31, 45, 61, 0.07);
}

.notice-compose-card {
  overflow: hidden;
}

.notice-hero {
  display: flex;
  gap: 14px;
  padding: 24px;
  background:
    radial-gradient(circle at 88% 0%, rgba(244, 108, 68, 0.18), transparent 32%),
    linear-gradient(135deg, #ffffff 0%, #f7fbff 100%);
}

.notice-icon,
.notice-badge {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  border-radius: 16px;
  background: #eef7fc;
  color: #087db6;
}

.notice-icon {
  width: 52px;
  height: 52px;
  font-size: 24px;
}

.notice-badge {
  width: 40px;
  height: 40px;
  font-size: 18px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #087db6;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.notice-hero h2,
.admin-table-header h2,
.notice-item h3 {
  margin: 0;
  color: #172033;
}

.notice-hero h2 {
  font-size: 22px;
}

.notice-hero p,
.admin-table-header p,
.notice-actions span,
.notice-item p,
.notice-meta {
  color: #6b778c;
}

.notice-hero p,
.admin-table-header p {
  margin: 6px 0 0;
  line-height: 1.7;
}

.notice-form {
  padding: 22px 24px 24px;
}

.notice-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-top: 6px;
}

.notice-actions span {
  font-size: 13px;
}

.notice-history-card {
  padding: 20px;
}

.admin-table-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.notice-list {
  display: grid;
  gap: 12px;
  min-height: 220px;
}

.notice-item {
  display: grid;
  gap: 12px;
  padding: 16px;
  border: 1px solid #e4ebf3;
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.notice-item.unread {
  border-color: rgba(8, 125, 182, 0.24);
  background:
    radial-gradient(circle at top right, rgba(8, 125, 182, 0.08), transparent 30%),
    #ffffff;
}

.notice-item-main {
  display: flex;
  gap: 12px;
  min-width: 0;
}

.notice-item h3 {
  font-size: 15px;
}

.notice-item p {
  display: -webkit-box;
  overflow: hidden;
  margin: 6px 0 0;
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.notice-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-left: 52px;
  font-size: 12px;
}

@media (max-width: 1100px) {
  .admin-notice-page {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .notice-actions,
  .admin-table-header,
  .notice-meta {
    align-items: flex-start;
    flex-direction: column;
  }

  .notice-meta {
    padding-left: 0;
  }
}
</style>
