<template>
  <div class="space-y-6">
    <div class="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
      <div>
        <h1 class="font-display-serif text-5xl text-on-surface">{{ t('notifications.title') }}</h1>
        <p class="mt-2 text-body-secondary text-on-surface-variant">
          {{ t('notifications.description') }}
        </p>
      </div>
      <div class="flex flex-wrap gap-3">
        <button class="app-secondary-button" type="button" @click="notificationStore.readAll">{{ t('notifications.markAllRead') }}</button>
        <button class="app-primary-button" type="button" @click="notificationStore.clearRead">{{ t('notifications.clearRead') }}</button>
      </div>
    </div>

    <div class="flex flex-wrap gap-2 border-b border-outline-variant/30 pb-4">
      <button
        v-for="filter in filters"
        :key="filter.value"
        class="rounded-full px-4 py-2 text-label-bold"
        :class="activeFilter === filter.value ? 'bg-primary text-white' : 'bg-surface text-on-surface hover:bg-surface-container-low'"
        type="button"
        @click="applyFilter(filter.value)"
      >
        {{ filter.label }}
      </button>
    </div>

    <div v-if="notificationStore.notifications.length" class="space-y-4">
      <article
        v-for="item in notificationStore.notifications"
        :key="item.id"
        class="group rounded-[1.6rem] border p-5 transition hover:-translate-y-1 hover:shadow-md"
        :class="
          item.isRead
            ? 'border-outline-variant/30 bg-surface shadow-none'
            : 'border-primary/20 bg-primary-fixed/25 shadow-sm'
        "
      >
        <div class="flex items-start gap-4">
          <div
            class="flex h-12 w-12 items-center justify-center rounded-full"
            :class="item.isRead ? 'bg-surface-container text-on-surface-variant' : 'bg-white text-primary'"
          >
            <span class="material-symbols-outlined">{{ iconMap[item.businessType || ''] || 'notifications' }}</span>
          </div>
          <div class="min-w-0 flex-1">
            <div class="flex items-start justify-between gap-4">
              <div>
                <h2 class="font-title-serif text-2xl" :class="item.isRead ? 'text-on-surface-variant line-through' : 'text-on-surface'">
                  {{ item.title }}
                </h2>
                <p class="mt-2 text-body-main" :class="item.isRead ? 'text-on-surface-variant line-through opacity-80' : 'text-on-surface-variant'">
                  {{ item.content }}
                </p>
              </div>
              <div class="text-caption text-on-surface-variant">{{ fromNow(item.gmtCreate) }}</div>
            </div>
            <div class="mt-4 flex flex-wrap gap-3">
              <span class="app-chip">{{ item.businessType || t('notifications.system') }}</span>
              <span class="app-chip">{{ item.type || t('notifications.notice') }}</span>
            </div>

            <div class="mt-4 flex flex-wrap gap-3">
              <button
                v-if="resolveTarget(item)"
                class="app-secondary-button px-4 py-2"
                type="button"
                @click="openNotification(item)"
              >
                {{ t('common.viewDetails') }}
              </button>
              <button
                v-if="!item.isRead"
                class="app-primary-button px-4 py-2"
                type="button"
                @click="markRead(item.id)"
              >
                {{ t('notifications.markRead') }}
              </button>
            </div>
          </div>
        </div>
      </article>
    </div>

    <EmptyState
      v-else
      :title="t('notifications.emptyTitle')"
      :description="t('notifications.emptyDescription')"
      icon="notifications_paused"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import EmptyState from '@/components/common/EmptyState.vue'
import { useI18n } from '@/i18n'
import { useNotificationStore } from '@/stores/notification'
import { useSpaceStore } from '@/stores/space'
import type { NotificationItem } from '@/types/app'
import { fromNow } from '@/utils/date'

const router = useRouter()
const notificationStore = useNotificationStore()
const spaceStore = useSpaceStore()
const { t } = useI18n()
const activeFilter = ref<'all' | 'unread' | 'read'>('all')

const filters = computed(() => [
  { label: t('notifications.all'), value: 'all' as const },
  { label: t('notifications.unread'), value: 'unread' as const },
  { label: t('notifications.read'), value: 'read' as const },
])

const iconMap: Record<string, string> = {
  NOTE: 'description',
  TASK: 'task_alt',
  TODO: 'checklist',
  PROJECT: 'folder_managed',
  SPACE_JOIN_REQUEST: 'group_add',
}

const loadNotifications = async () => {
  await Promise.all([notificationStore.loadNotifications(), notificationStore.fetchUnreadCount()])
}

const applyFilter = async (filter: 'all' | 'unread' | 'read') => {
  activeFilter.value = filter
  if (filter === 'all') {
    notificationStore.query.isRead = undefined
  } else {
    notificationStore.query.isRead = filter === 'read'
  }
  await loadNotifications()
}

const markRead = async (id: number) => {
  await notificationStore.markRead(id)
}

const resolveTarget = (item: NotificationItem) => {
  if (item.businessType === 'NOTE' && item.businessId) {
    return { path: `/app/notes/${item.businessId}` }
  }
  if (item.businessType === 'TASK' && item.businessId) {
    return { path: '/app/tasks', query: { openTaskId: String(item.businessId) } }
  }
  if (item.businessType === 'SPACE_JOIN_REQUEST') {
    const targetSpaceId = item.spaceId
      || (spaceStore.currentSpace?.type === 'TEAM' ? spaceStore.currentSpaceId : undefined)
      || spaceStore.spaces.find((space) => space.type === 'TEAM')?.id
    if (targetSpaceId) {
      return { path: `/app/space/${targetSpaceId}/settings` }
    }
  }
  return null
}

const openNotification = async (item: NotificationItem) => {
  const target = resolveTarget(item)
  if (!target) {
    return
  }

  if (!item.isRead) {
    await notificationStore.markRead(item.id)
  }

  await router.push(target)
}

onMounted(() => {
  loadNotifications().catch(() => undefined)
})

watch(
  () => notificationStore.query.pageNum,
  () => {
    loadNotifications().catch(() => undefined)
  },
)
</script>
