<template>
  <div class="space-y-6">
    <div class="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
      <div>
        <h1 class="font-display-serif text-5xl text-on-surface">{{ t('stats.personalTitle') }}</h1>
        <p class="mt-2 text-body-secondary text-on-surface-variant">
          {{ t('stats.personalDescription') }}
        </p>
      </div>
    </div>

    <div class="grid gap-5 md:grid-cols-2 xl:grid-cols-4">
      <div class="app-card bg-surface-container-low">
        <div class="text-caption uppercase tracking-[0.22em] text-on-surface-variant">{{ t('stats.notes') }}</div>
        <div class="mt-4 font-display-serif text-5xl text-primary">{{ stats?.noteCount ?? 0 }}</div>
      </div>
      <div class="app-card">
        <div class="text-caption uppercase tracking-[0.22em] text-on-surface-variant">{{ t('stats.unfinishedTasks') }}</div>
        <div class="mt-4 font-display-serif text-5xl text-on-surface">{{ stats?.unfinishedTaskMemberCount ?? 0 }}</div>
      </div>
      <div class="app-card">
        <div class="text-caption uppercase tracking-[0.22em] text-on-surface-variant">{{ t('stats.completedThisMonth') }}</div>
        <div class="mt-4 font-display-serif text-5xl text-on-surface">{{ stats?.completedTaskCountThisMonth ?? 0 }}</div>
      </div>
      <div class="app-card bg-primary-fixed/30">
        <div class="text-caption uppercase tracking-[0.22em] text-on-surface-variant">{{ t('stats.trendWindow') }}</div>
        <div class="mt-4 font-display-serif text-5xl text-primary">{{ trend.length }}</div>
      </div>
    </div>

    <div class="grid gap-6 xl:grid-cols-[1.45fr_0.85fr]">
      <section class="app-shell overflow-hidden">
        <div class="mb-8 flex items-center justify-between">
          <div>
            <h2 class="font-title-serif text-3xl text-on-surface">{{ t('stats.rhythm') }}</h2>
            <p class="mt-2 text-body-secondary text-on-surface-variant">{{ t('stats.rhythmDescription') }}</p>
          </div>
          <div class="flex items-center gap-3">
            <span class="inline-flex items-center gap-2 rounded-full bg-primary-fixed/30 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.18em] text-primary">
              <span class="h-2.5 w-2.5 rounded-full bg-primary shadow-[0_0_0_3px_rgba(159,65,34,0.12)]"></span>
              {{ t('stats.new') }}
            </span>
            <span class="inline-flex items-center gap-2 rounded-full bg-surface-container-high px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.18em] text-outline">
              <span class="h-2.5 w-2.5 rounded-full bg-outline shadow-[0_0_0_3px_rgba(137,114,107,0.12)]"></span>
              {{ t('stats.update') }}
            </span>
          </div>
        </div>

        <div class="stats-chart-grid relative min-h-[320px] rounded-[1.75rem] border border-outline-variant/15 bg-surface-container-low/35 px-5 pb-6 pt-10">
          <div class="pointer-events-none absolute inset-x-5 top-10 bottom-14 flex flex-col justify-between">
            <span v-for="line in 4" :key="line" class="border-t border-dashed border-outline-variant/20"></span>
          </div>

          <div class="relative z-[1] flex min-h-[250px] items-end gap-3">
            <div v-for="item in trend" :key="item.date" class="flex flex-1 flex-col items-center gap-4">
              <div class="flex w-full items-end gap-2">
                <div class="flex w-1/2 flex-col items-center gap-2">
                  <div class="text-[10px] font-semibold uppercase tracking-[0.18em] text-primary">
                    {{ item.createdCount }}
                  </div>
                  <div
                    class="w-full rounded-t-[1.25rem]"
                    :style="createdBarStyle(item.createdCount)"
                  ></div>
                </div>
                <div class="flex w-1/2 flex-col items-center gap-2">
                  <div class="text-[10px] font-semibold uppercase tracking-[0.18em] text-on-surface-variant">
                    {{ item.updatedCount }}
                  </div>
                  <div
                    class="w-full rounded-t-[1.25rem]"
                    :style="updatedBarStyle(item.updatedCount)"
                  ></div>
                </div>
              </div>
              <div class="text-caption text-on-surface-variant">{{ formatDate(item.date).slice(5) }}</div>
            </div>
          </div>
        </div>
      </section>

      <section class="grid gap-5">
        <div class="app-card bg-surface-container-low">
          <div class="mb-4 text-label-bold text-primary">{{ t('stats.reminder') }}</div>
          <p class="font-title-serif text-2xl italic leading-relaxed text-on-surface">
            "{{ weeklyInsight.reminder }}"
          </p>
          <p class="mt-4 text-caption uppercase tracking-[0.22em] text-on-surface-variant">{{ weeklyInsight.footnote }}</p>
        </div>

        <div class="app-card">
          <div class="mb-4 text-label-bold text-on-surface">{{ t('stats.weeklyObservation') }}</div>
          <ul class="space-y-3 text-body-secondary text-on-surface-variant">
            <li v-for="observation in weeklyObservations" :key="observation">{{ observation }}</li>
          </ul>
        </div>

        <div class="grid gap-4 md:grid-cols-2">
          <div class="app-card bg-primary-fixed/25">
            <div class="text-caption uppercase tracking-[0.18em] text-on-surface-variant">{{ t('stats.todayFocus') }}</div>
            <div class="mt-3 font-title-serif text-2xl text-primary">{{ todayCreated }}</div>
            <div class="mt-2 text-sm text-on-surface-variant">{{ t('stats.todayCreated') }}</div>
          </div>
          <div class="app-card">
            <div class="text-caption uppercase tracking-[0.18em] text-on-surface-variant">{{ t('stats.reviewPace') }}</div>
            <div class="mt-3 font-title-serif text-2xl text-on-surface">{{ todayUpdated }}</div>
            <div class="mt-2 text-sm text-on-surface-variant">{{ t('stats.todayUpdated') }}</div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { statsService } from '@/api/services'
import { useI18n } from '@/i18n'
import type { PersonalNoteTrend, PersonalStats } from '@/types/app'
import { useSpaceStore } from '@/stores/space'
import { formatDate } from '@/utils/date'

const spaceStore = useSpaceStore()
const { t } = useI18n()
const stats = ref<PersonalStats | null>(null)
const trend = ref<PersonalNoteTrend[]>([])

const maxCount = computed(() => {
  const max = Math.max(...trend.value.map((item) => Math.max(item.createdCount, item.updatedCount)), 1)
  return max
})

const todayCreated = computed(() => trend.value[trend.value.length - 1]?.createdCount ?? 0)
const todayUpdated = computed(() => trend.value[trend.value.length - 1]?.updatedCount ?? 0)
const weeklyCreated = computed(() => trend.value.reduce((sum, item) => sum + item.createdCount, 0))
const weeklyUpdated = computed(() => trend.value.reduce((sum, item) => sum + item.updatedCount, 0))
const activeDays = computed(() => trend.value.filter((item) => item.createdCount > 0 || item.updatedCount > 0).length)
const weeklyActions = computed(() => weeklyCreated.value + weeklyUpdated.value)
const busiestDay = computed(() =>
  trend.value.reduce<PersonalNoteTrend | null>((current, item) => {
    if (!current) {
      return item
    }
    return item.createdCount + item.updatedCount > current.createdCount + current.updatedCount ? item : current
  }, null),
)
const weeklyInsight = computed(() => {
  if (!trend.value.length) {
    return {
      reminder: t('stats.reminderEmpty'),
      footnote: t('stats.reminderFootnoteEmpty'),
      observations: [t('stats.observationEmpty')],
    }
  }

  const observations = [
    t('stats.observationActivity', {
      activeDays: activeDays.value,
      created: weeklyCreated.value,
      updated: weeklyUpdated.value,
    }),
  ]

  if (activeDays.value <= 2) {
    observations.push(t('stats.observationLowActiveDays'))
  } else if (activeDays.value >= 5) {
    observations.push(t('stats.observationStrongActiveDays'))
  }

  if (weeklyCreated.value > weeklyUpdated.value) {
    observations.push(t('stats.observationCaptureMode'))
  } else if (weeklyUpdated.value > weeklyCreated.value) {
    observations.push(t('stats.observationReviewMode'))
  } else {
    observations.push(t('stats.observationBalancedMode'))
  }

  if (busiestDay.value && busiestDay.value.createdCount + busiestDay.value.updatedCount > 0) {
    observations.push(
      t('stats.observationBusiestDay', {
        date: formatDate(busiestDay.value.date).slice(5),
        count: busiestDay.value.createdCount + busiestDay.value.updatedCount,
      }),
    )
  } else {
    observations.push(t('stats.observationRestart'))
  }

  if (weeklyUpdated.value === 0 && weeklyCreated.value > 0) {
    observations.push(t('stats.observationNoUpdate'))
  } else if (weeklyCreated.value === 0 && weeklyUpdated.value > 0) {
    observations.push(t('stats.observationNoCreate'))
  }

  if (weeklyActions.value === 0) {
    return {
      reminder: t('stats.reminderEmpty'),
      footnote: t('stats.reminderFootnoteEmpty'),
      observations: [t('stats.observationEmpty')],
    }
  }

  if (activeDays.value <= 2) {
    return {
      reminder: t('stats.reminderLowActivity'),
      footnote: t('stats.reminderFootnoteLowActivity'),
      observations,
    }
  }

  if (weeklyCreated.value >= weeklyUpdated.value * 2 && weeklyCreated.value >= 3) {
    return {
      reminder: t('stats.reminderCaptureHeavy'),
      footnote: t('stats.reminderFootnoteCaptureHeavy'),
      observations,
    }
  }

  if (todayCreated.value + todayUpdated.value >= 3) {
    return {
      reminder: t('stats.reminderTodayActive'),
      footnote: t('stats.reminderFootnoteTodayActive'),
      observations,
    }
  }

  return {
    reminder: t('stats.reminderQuote'),
    footnote: t('stats.reminderFootnote'),
    observations,
  }
})
const weeklyObservations = computed(() => weeklyInsight.value.observations)

const normalize = (value: number, max: number) => Math.max((value / max) * 200, 24)
const createdBarStyle = (value: number) => ({
  height: `${normalize(value, maxCount.value)}px`,
  background: 'linear-gradient(180deg, var(--primary-fixed-dim), var(--primary))',
  boxShadow: '0 14px 30px color-mix(in srgb, var(--primary) 18%, transparent)',
})
const updatedBarStyle = (value: number) => ({
  height: `${normalize(value, maxCount.value)}px`,
  background: 'linear-gradient(180deg, var(--surface-container-highest), var(--outline))',
  boxShadow: '0 12px 26px color-mix(in srgb, var(--outline) 16%, transparent)',
})

const loadStats = async () => {
  if (spaceStore.currentSpace?.type === 'TEAM') {
    stats.value = null
    trend.value = []
    return
  }

  const [statsResponse, trendResponse] = await Promise.all([statsService.personal(), statsService.personalNoteTrend(7)])
  stats.value = statsResponse
  trend.value = trendResponse
}

onMounted(() => {
  loadStats().catch(() => undefined)
})

watch(
  () => spaceStore.currentSpaceId,
  () => {
    loadStats().catch(() => undefined)
  },
)
</script>

<style scoped>
.stats-chart-grid {
  background-image: radial-gradient(circle at top, rgba(255, 255, 255, 0.45), transparent 48%);
}
</style>
