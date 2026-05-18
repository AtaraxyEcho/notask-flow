<template>
  <div class="space-y-6">
    <div>
      <h1 class="font-display-serif text-5xl text-on-surface">{{ t('stats.teamTitle') }}</h1>
      <p class="mt-2 text-body-secondary text-on-surface-variant">{{ t('stats.teamDescription') }}</p>
    </div>

    <div class="grid gap-5 md:grid-cols-2 xl:grid-cols-4">
      <div class="app-card team-shell">
        <div class="text-caption uppercase tracking-[0.22em] text-primary/60">{{ t('stats.memberLoad') }}</div>
        <div class="mt-4 font-display-serif text-5xl text-primary">{{ loadData.length }}</div>
      </div>
      <div class="app-card team-shell">
        <div class="text-caption uppercase tracking-[0.22em] text-primary/60">{{ t('stats.trendPoints') }}</div>
        <div class="mt-4 font-display-serif text-5xl text-primary">{{ trend.length }}</div>
      </div>
      <div class="app-card team-shell">
        <div class="text-caption uppercase tracking-[0.22em] text-primary/60">{{ t('stats.roleStats') }}</div>
        <div class="mt-4 font-display-serif text-5xl text-primary">{{ roleCompletion.length }}</div>
      </div>
      <div class="app-card team-shell">
        <div class="text-caption uppercase tracking-[0.22em] text-primary/60">{{ t('stats.recentActivity') }}</div>
        <div class="mt-4 font-display-serif text-5xl text-primary">{{ activities.length }}</div>
      </div>
    </div>

    <div class="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
      <section class="app-shell team-shell">
        <div class="mb-5">
          <h2 class="font-title-serif text-3xl text-on-surface">{{ t('stats.taskTrend') }}</h2>
          <p class="mt-2 text-body-secondary text-on-surface-variant">{{ t('stats.taskTrendDescription') }}</p>
        </div>
        <div class="team-trend-chart">
          <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
            <div class="flex items-center gap-4 text-caption uppercase tracking-[0.18em] text-primary/65">
              <span class="inline-flex items-center gap-2">
                <span class="h-2.5 w-2.5 rounded-full bg-[#0077B6]"></span>
                {{ t('stats.new') }}
              </span>
              <span class="inline-flex items-center gap-2">
                <span class="h-2.5 w-2.5 rounded-full bg-[#50D9FE]"></span>
                {{ t('common.completed') }}
              </span>
            </div>
            <div class="rounded-full bg-white/70 px-3 py-1 text-caption text-primary/70">
              Max {{ maxTrend }}
            </div>
          </div>

          <div class="relative min-h-[320px] overflow-hidden rounded-[1.5rem] border border-white/60 bg-white/55 p-4">
            <svg class="h-[280px] w-full" :viewBox="`0 0 ${chartWidth} ${chartHeight}`" role="img" :aria-label="t('stats.taskTrend')">
              <defs>
                <linearGradient id="createdTrendFill" x1="0" x2="0" y1="0" y2="1">
                  <stop offset="0%" stop-color="#0077B6" stop-opacity="0.24" />
                  <stop offset="100%" stop-color="#0077B6" stop-opacity="0.02" />
                </linearGradient>
                <linearGradient id="completedTrendFill" x1="0" x2="0" y1="0" y2="1">
                  <stop offset="0%" stop-color="#50D9FE" stop-opacity="0.28" />
                  <stop offset="100%" stop-color="#50D9FE" stop-opacity="0.04" />
                </linearGradient>
              </defs>

              <g>
                <g v-for="tick in trendTicks" :key="tick.value">
                  <line
                    :x1="chartPaddingLeft"
                    :x2="chartWidth - chartPaddingRight"
                    :y1="tick.y"
                    :y2="tick.y"
                    stroke="rgba(0, 119, 182, 0.12)"
                    stroke-width="1"
                  />
                  <text :x="chartPaddingLeft - 12" :y="tick.y + 4" text-anchor="end" class="team-trend-axis-text">
                    {{ tick.value }}
                  </text>
                </g>
              </g>

              <path v-if="createdAreaPath" :d="createdAreaPath" fill="url(#createdTrendFill)" />
              <path v-if="completedAreaPath" :d="completedAreaPath" fill="url(#completedTrendFill)" />

              <g v-for="bar in trendBarGroups" :key="bar.date">
                <rect
                  :height="bar.createdHeight"
                  :rx="5"
                  :width="bar.barWidth"
                  :x="bar.createdX"
                  :y="bar.createdY"
                  fill="#0077B6"
                  opacity="0.72"
                />
                <rect
                  :height="bar.completedHeight"
                  :rx="5"
                  :width="bar.barWidth"
                  :x="bar.completedX"
                  :y="bar.completedY"
                  fill="#50D9FE"
                  opacity="0.78"
                />
              </g>

              <polyline
                v-if="createdLinePoints"
                :points="createdLinePoints"
                fill="none"
                stroke="#0077B6"
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="3"
              />
              <polyline
                v-if="completedLinePoints"
                :points="completedLinePoints"
                fill="none"
                stroke="#0EA5E9"
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="3"
              />

              <g v-for="point in completedTrendPoints" :key="`${point.date}-point`">
                <circle :cx="point.x" :cy="point.y" fill="#ffffff" r="4.5" stroke="#0EA5E9" stroke-width="2.5" />
              </g>

              <line
                :x1="chartPaddingLeft"
                :x2="chartWidth - chartPaddingRight"
                :y1="chartHeight - chartPaddingBottom"
                :y2="chartHeight - chartPaddingBottom"
                stroke="rgba(0, 119, 182, 0.28)"
                stroke-width="1.5"
              />
            </svg>

            <div v-if="!trendHasValues" class="pointer-events-none absolute inset-x-0 top-1/2 -translate-y-1/2 text-center">
              <span class="rounded-full bg-white/80 px-4 py-2 text-caption uppercase tracking-[0.2em] text-primary/55">
                {{ t('common.noData') }}
              </span>
            </div>
          </div>

          <div class="mt-3 grid grid-cols-7 gap-2 text-center text-caption text-primary/60">
            <div v-for="item in trend" :key="`${item.date}-label`">
              {{ formatDate(item.date).slice(5) }}
            </div>
          </div>
        </div>
      </section>

      <section class="app-shell team-shell">
        <div class="mb-5">
          <h2 class="font-title-serif text-3xl text-on-surface">{{ t('stats.memberLoad') }}</h2>
          <p class="mt-2 text-body-secondary text-on-surface-variant">{{ t('stats.memberLoadDescription') }}</p>
        </div>
        <div class="space-y-4">
          <div v-for="item in loadData" :key="item.userId" class="rounded-[1.4rem] bg-white/60 p-4">
            <div class="mb-2 flex items-center justify-between">
              <div class="font-label-bold text-on-surface">{{ item.username }}</div>
              <div class="text-caption text-primary/70">{{ item.loadCount }} / {{ item.completedCount }}</div>
            </div>
            <div class="h-2 overflow-hidden rounded-full bg-white">
              <div class="h-full rounded-full bg-primary-container" :style="{ width: `${Math.min((item.completedCount / Math.max(item.loadCount, 1)) * 100, 100)}%` }"></div>
            </div>
          </div>
        </div>
      </section>
    </div>

    <div class="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
      <section class="app-shell team-shell">
        <div class="mb-5">
          <h2 class="font-title-serif text-3xl text-on-surface">{{ t('stats.roleCompletion') }}</h2>
        </div>
        <div class="space-y-4">
          <div v-for="item in roleCompletion" :key="item.roleCode" class="rounded-[1.4rem] bg-white/60 p-4">
            <div class="flex items-center justify-between">
              <div>
                <div class="font-label-bold text-on-surface">{{ item.roleName }}</div>
                <div class="text-caption text-primary/60">{{ item.roleCode }}</div>
              </div>
              <div class="font-title-serif text-3xl text-primary">{{ item.completedCount }}</div>
            </div>
          </div>
        </div>
      </section>

      <section class="app-shell team-shell">
        <div class="mb-5">
          <h2 class="font-title-serif text-3xl text-on-surface">{{ t('stats.recentActivity') }}</h2>
        </div>
        <div class="space-y-4">
          <div v-for="item in activities" :key="`${item.time}-${item.content}`" class="rounded-[1.4rem] bg-white/60 p-4">
            <div class="flex items-center justify-between gap-4">
              <div>
                <div class="font-label-bold text-on-surface">{{ item.member }}</div>
                <div class="mt-1 text-body-secondary text-on-surface-variant">{{ item.content }}</div>
              </div>
              <div class="text-caption text-primary/60">{{ fromNow(item.time) }}</div>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { statsService } from '@/api/services'
import { SPACE_STATS_CHANGED_EVENT } from '@/composables/useSpaceRealtimeEvents'
import { useI18n } from '@/i18n'
import type { MemberTaskLoad, RoleCompletion, SpaceRealtimeEvent, StatsActivity, TaskTrend } from '@/types/app'
import { useSpaceStore } from '@/stores/space'
import { formatDate, fromNow } from '@/utils/date'

const route = useRoute()
const spaceStore = useSpaceStore()
const { t } = useI18n()

const loadData = ref<MemberTaskLoad[]>([])
const trend = ref<TaskTrend[]>([])
const roleCompletion = ref<RoleCompletion[]>([])
const activities = ref<StatsActivity[]>([])

const TREND_DAYS = 7
const resolvedSpaceId = computed(() => Number(route.params.spaceId || spaceStore.currentSpaceId || 0))
const targetSpace = computed(() => spaceStore.spaces.find((space) => space.id === resolvedSpaceId.value) || null)
const toCount = (value?: number | null) => {
  const count = Number(value)
  return Number.isFinite(count) ? count : 0
}
const maxTrend = computed(() =>
  Math.max(...trend.value.map((item) => Math.max(toCount(item.createdCount), toCount(item.completedCount))), 1),
)
const normalize = (value: number | null | undefined, max: number) => Math.max((toCount(value) / max) * 220, 18)

const toDateKey = (date: Date) => {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

const buildEmptyTrend = (days = TREND_DAYS) => {
  const today = new Date()
  const start = new Date(today)
  start.setDate(today.getDate() - days + 1)

  return Array.from({ length: days }, (_, index) => {
    const date = new Date(start)
    date.setDate(start.getDate() + index)
    return {
      date: toDateKey(date),
      createdCount: 0,
      completedCount: 0,
    }
  })
}

const normalizeTrend = (items?: TaskTrend[]) => {
  const trendMap = new Map<string, TaskTrend>()

  for (const item of items || []) {
    const date = formatDate(item.date)
    if (date !== '--') {
      trendMap.set(date, {
        date,
        createdCount: toCount(item.createdCount),
        completedCount: toCount(item.completedCount),
      })
    }
  }

  return buildEmptyTrend().map((item) => trendMap.get(item.date) || item)
}

const chartWidth = 760
const chartHeight = 280
const chartPaddingLeft = 44
const chartPaddingRight = 18
const chartPaddingTop = 18
const chartPaddingBottom = 34
const chartInnerWidth = computed(() => chartWidth - chartPaddingLeft - chartPaddingRight)
const chartInnerHeight = computed(() => chartHeight - chartPaddingTop - chartPaddingBottom)
const chartBottomY = computed(() => chartHeight - chartPaddingBottom)
const trendHasValues = computed(() =>
  trend.value.some((item) => toCount(item.createdCount) > 0 || toCount(item.completedCount) > 0),
)
const trendTicks = computed(() => {
  const max = maxTrend.value
  return Array.from({ length: 5 }, (_, index) => ({
    value: Math.round((max / 4) * (4 - index)),
    y: chartPaddingTop + (chartInnerHeight.value / 4) * index,
  }))
})
const trendPointX = (index: number) => {
  if (trend.value.length <= 1) {
    return chartPaddingLeft + chartInnerWidth.value / 2
  }

  return chartPaddingLeft + (chartInnerWidth.value / (trend.value.length - 1)) * index
}
const trendPointY = (value: number) => chartBottomY.value - (toCount(value) / maxTrend.value) * chartInnerHeight.value
const createdTrendPoints = computed(() =>
  trend.value.map((item, index) => ({
    date: item.date,
    x: trendPointX(index),
    y: trendPointY(toCount(item.createdCount)),
  })),
)
const completedTrendPoints = computed(() =>
  trend.value.map((item, index) => ({
    date: item.date,
    x: trendPointX(index),
    y: trendPointY(toCount(item.completedCount)),
  })),
)
const toPolylinePoints = (points: Array<{ x: number; y: number }>) =>
  points.map((point) => `${point.x.toFixed(1)},${point.y.toFixed(1)}`).join(' ')
const createdLinePoints = computed(() => toPolylinePoints(createdTrendPoints.value))
const completedLinePoints = computed(() => toPolylinePoints(completedTrendPoints.value))
const toAreaPath = (points: Array<{ x: number; y: number }>) => {
  if (!points.length) {
    return ''
  }

  const firstPoint = points[0]
  const lastPoint = points[points.length - 1]
  return [
    `M ${firstPoint.x.toFixed(1)} ${chartBottomY.value.toFixed(1)}`,
    ...points.map((point) => `L ${point.x.toFixed(1)} ${point.y.toFixed(1)}`),
    `L ${lastPoint.x.toFixed(1)} ${chartBottomY.value.toFixed(1)}`,
    'Z',
  ].join(' ')
}
const createdAreaPath = computed(() => toAreaPath(createdTrendPoints.value))
const completedAreaPath = computed(() => toAreaPath(completedTrendPoints.value))
const trendBarGroups = computed(() => {
  const groupWidth = chartInnerWidth.value / Math.max(trend.value.length, 1)
  const barWidth = Math.min(18, Math.max(8, groupWidth * 0.22))
  return trend.value.map((item, index) => {
    const centerX = chartPaddingLeft + groupWidth * index + groupWidth / 2
    const createdHeight = Math.max(2, chartBottomY.value - trendPointY(toCount(item.createdCount)))
    const completedHeight = Math.max(2, chartBottomY.value - trendPointY(toCount(item.completedCount)))
    return {
      date: item.date,
      barWidth,
      createdHeight,
      completedHeight,
      createdX: centerX - barWidth - 2,
      completedX: centerX + 2,
      createdY: chartBottomY.value - createdHeight,
      completedY: chartBottomY.value - completedHeight,
    }
  })
})

let dashboardRequestToken = 0
let reloadTimer: number | null = null
let delayedReloadTimer: number | null = null

const clearReloadTimers = () => {
  if (reloadTimer) {
    window.clearTimeout(reloadTimer)
    reloadTimer = null
  }
  if (delayedReloadTimer) {
    window.clearTimeout(delayedReloadTimer)
    delayedReloadTimer = null
  }
}

const loadDashboard = async () => {
  const requestToken = ++dashboardRequestToken
  const spaceId = resolvedSpaceId.value

  if (!spaceStore.spaces.length) {
    await spaceStore.ensureLoaded()
  }

  if (!spaceId || targetSpace.value?.type !== 'TEAM') {
    loadData.value = []
    trend.value = buildEmptyTrend()
    roleCompletion.value = []
    activities.value = []
    return
  }

  trend.value = normalizeTrend(trend.value.length ? trend.value : buildEmptyTrend())

  const [loadResponse, trendResponse, roleResponse, activityResponse] = await Promise.allSettled([
    statsService.load(spaceId),
    statsService.trend(spaceId, TREND_DAYS),
    statsService.roleCompletion(spaceId),
    statsService.activities(spaceId, 8),
  ])

  if (requestToken !== dashboardRequestToken || spaceId !== resolvedSpaceId.value) {
    return
  }

  loadData.value = loadResponse.status === 'fulfilled' ? loadResponse.value : []
  trend.value = trendResponse.status === 'fulfilled' ? normalizeTrend(trendResponse.value) : buildEmptyTrend()
  roleCompletion.value = roleResponse.status === 'fulfilled' ? roleResponse.value : []
  activities.value = activityResponse.status === 'fulfilled' ? activityResponse.value : []
}

const scheduleDashboardReload = () => {
  clearReloadTimers()
  reloadTimer = window.setTimeout(() => {
    reloadTimer = null
    loadDashboard().catch(() => undefined)
  }, 300)
  delayedReloadTimer = window.setTimeout(() => {
    delayedReloadTimer = null
    loadDashboard().catch(() => undefined)
  }, 1500)
}

const handleStatsChanged = (event: Event) => {
  const detail = (event as CustomEvent<SpaceRealtimeEvent>).detail
  if (!detail || detail.spaceId !== resolvedSpaceId.value) {
    return
  }
  scheduleDashboardReload()
}

onMounted(() => {
  window.addEventListener(SPACE_STATS_CHANGED_EVENT, handleStatsChanged)
  loadDashboard().catch(() => undefined)
})

onBeforeUnmount(() => {
  window.removeEventListener(SPACE_STATS_CHANGED_EVENT, handleStatsChanged)
  clearReloadTimers()
  dashboardRequestToken += 1
})

watch(
  () => [resolvedSpaceId.value, targetSpace.value?.type],
  () => {
    clearReloadTimers()
    loadDashboard().catch(() => undefined)
  },
)
</script>

<style scoped>
.team-trend-axis-text {
  fill: rgba(0, 93, 144, 0.55);
  font-size: 12px;
  font-weight: 700;
}
</style>
