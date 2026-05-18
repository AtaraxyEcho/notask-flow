<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { use } from 'echarts/core'
import VChart from 'vue-echarts'
import { adminDashboardService } from '@/api/modules/admin'
import type { AdminDashboard } from '@/types/app'

use([CanvasRenderer, BarChart, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent])

type GradientTone = 'blue' | 'orange' | 'green' | 'gold' | 'purple' | 'cyan'

const loading = ref(false)
const dashboard = ref<AdminDashboard>({
  totalUsers: 0,
  totalTeamSpaces: 0,
  totalNotes: 0,
  totalTasks: 0,
  totalTodos: 0,
  totalFiles: 0,
  totalStorageBytes: 0,
  todayNewUsers: 0,
  todayNewNotes: 0,
  todayNewTasks: 0,
  todayNewTodos: 0,
  todayNewTeamSpaces: 0,
  trends: [],
})

const trendDates = computed(() => dashboard.value.trends.map((item) => item.date.slice(5)))

const overviewCards = computed(() => [
  {
    label: '用户总数',
    value: formatNumber(dashboard.value.totalUsers),
    hint: `今日新增 ${formatNumber(dashboard.value.todayNewUsers)}`,
    tone: 'blue',
  },
  {
    label: '团队空间',
    value: formatNumber(dashboard.value.totalTeamSpaces),
    hint: `今日新增 ${formatNumber(dashboard.value.todayNewTeamSpaces)}`,
    tone: 'green',
  },
  {
    label: '内容资产',
    value: formatNumber(dashboard.value.totalNotes + dashboard.value.totalTasks + dashboard.value.totalTodos),
    hint: `笔记 ${formatNumber(dashboard.value.totalNotes)} / 任务 ${formatNumber(dashboard.value.totalTasks)}`,
    tone: 'orange',
  },
  {
    label: '文件存储',
    value: formatBytes(dashboard.value.totalStorageBytes),
    hint: `文件 ${formatNumber(dashboard.value.totalFiles)} 个`,
    tone: 'purple',
  },
])

const userTrendOption = computed(() => ({
  tooltip: axisTooltip(),
  grid: chartGrid(20, 22, 42, 44),
  xAxis: axisCategory(),
  yAxis: axisValue(),
  series: [
    {
      name: '新增用户',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      showSymbol: false,
      lineStyle: {
        width: 4,
        color: '#087db6',
        shadowColor: 'rgba(8, 125, 182, 0.28)',
        shadowBlur: 12,
      },
      itemStyle: {
        color: '#087db6',
        borderColor: '#ffffff',
        borderWidth: 2,
      },
      areaStyle: {
        color: linearGradient('rgba(8, 125, 182, 0.28)', 'rgba(8, 125, 182, 0.02)'),
      },
      data: dashboard.value.trends.map((item) => item.newUsers),
    },
  ],
}))

const contentTrendOption = computed(() => ({
  tooltip: axisTooltip(),
  legend: legendStyle(),
  grid: chartGrid(42, 18, 40, 42),
  xAxis: axisCategory(),
  yAxis: axisValue(),
  series: [
    gradientBarSeries('笔记', dashboard.value.trends.map((item) => item.newNotes), 'orange', 'content'),
    gradientBarSeries('任务', dashboard.value.trends.map((item) => item.newTasks), 'green', 'content'),
    gradientBarSeries('待办', dashboard.value.trends.map((item) => item.newTodos), 'gold', 'content'),
  ],
}))

const storageTrendOption = computed(() => ({
  tooltip: {
    ...axisTooltip(),
    valueFormatter: (value: number) => formatBytes(value),
  },
  grid: chartGrid(20, 18, 44, 44),
  xAxis: axisCategory(),
  yAxis: {
    ...axisValue(),
    axisLabel: { color: '#8a98aa', formatter: (value: number) => compactBytes(value) },
  },
  series: [
    {
      name: '上传量',
      type: 'bar',
      barWidth: 15,
      itemStyle: {
        borderRadius: [10, 10, 3, 3],
        color: linearGradient('#c9bfff', '#6f63c6'),
      },
      emphasis: {
        itemStyle: {
          color: linearGradient('#ded8ff', '#5f52bd'),
        },
      },
      data: dashboard.value.trends.map((item) => item.uploadedBytes),
    },
  ],
}))

const teamSpaceOption = computed(() => ({
  tooltip: axisTooltip(),
  grid: chartGrid(16, 12, 30, 34),
  xAxis: axisCategory(false),
  yAxis: axisValue(false),
  series: [
    {
      name: '新增团队空间',
      type: 'line',
      smooth: true,
      showSymbol: false,
      lineStyle: {
        width: 3,
        color: '#3b82c4',
      },
      areaStyle: {
        color: linearGradient('rgba(59, 130, 196, 0.24)', 'rgba(59, 130, 196, 0.02)'),
      },
      data: dashboard.value.trends.map((item) => item.newTeamSpaces),
    },
  ],
}))

const contentCompositionOption = computed(() => ({
  color: ['#087db6', '#f46c44', '#4f8f6d', '#d49b24'],
  tooltip: { trigger: 'item' },
  legend: {
    bottom: 0,
    left: 'center',
    itemWidth: 10,
    itemHeight: 10,
    textStyle: { color: '#627286' },
  },
  series: [
    {
      name: '资产占比',
      type: 'pie',
      radius: ['50%', '72%'],
      center: ['50%', '42%'],
      avoidLabelOverlap: true,
      padAngle: 3,
      itemStyle: {
        borderColor: '#ffffff',
        borderWidth: 3,
        borderRadius: 8,
      },
      label: { color: '#4a5b70', formatter: '{b}\n{d}%' },
      data: [
        { value: dashboard.value.totalNotes, name: '笔记' },
        { value: dashboard.value.totalTasks, name: '任务' },
        { value: dashboard.value.totalTodos, name: '待办' },
        { value: dashboard.value.totalFiles, name: '文件' },
      ],
    },
  ],
}))

const activitySummary = computed(() => [
  { label: '今日新增用户', value: formatNumber(dashboard.value.todayNewUsers), tone: 'blue' },
  { label: '今日新增内容', value: formatNumber(todayContentCount.value), tone: 'orange' },
  { label: '今日新增团队', value: formatNumber(dashboard.value.todayNewTeamSpaces), tone: 'green' },
])

const todayContentCount = computed(() => dashboard.value.todayNewNotes + dashboard.value.todayNewTasks + dashboard.value.todayNewTodos)

const dateRangeText = computed(() => {
  const trends = dashboard.value.trends
  if (!trends.length) {
    return '暂无趋势数据'
  }
  return `${trends[0].date} 至 ${trends[trends.length - 1].date}`
})

async function loadDashboard() {
  loading.value = true
  try {
    dashboard.value = await adminDashboardService.overview()
  } finally {
    loading.value = false
  }
}

function gradientBarSeries(name: string, data: number[], tone: GradientTone, stack?: string) {
  return {
    name,
    type: 'bar',
    stack,
    barWidth: stack ? 16 : 14,
    itemStyle: {
      borderRadius: stack ? [7, 7, 2, 2] : [10, 10, 3, 3],
      color: gradientColor(tone),
    },
    emphasis: {
      itemStyle: {
        color: gradientColor(tone, true),
      },
    },
    data,
  }
}

function gradientColor(tone: GradientTone, active = false) {
  const gradients: Record<GradientTone, [string, string]> = {
    blue: active ? ['#6bd2ff', '#087db6'] : ['#aee9ff', '#087db6'],
    orange: active ? ['#ffbd9e', '#f46c44'] : ['#ffd3c1', '#f46c44'],
    green: active ? ['#b9dfc5', '#4f8f6d'] : ['#d5f1dd', '#4f8f6d'],
    gold: active ? ['#ffe098', '#d49b24'] : ['#fff0bd', '#d49b24'],
    purple: active ? ['#dcd4ff', '#6f63c6'] : ['#eee9ff', '#6f63c6'],
    cyan: active ? ['#a6eff4', '#1f9aaa'] : ['#d3fbff', '#1f9aaa'],
  }
  return linearGradient(gradients[tone][0], gradients[tone][1])
}

function linearGradient(from: string, to: string) {
  return {
    type: 'linear',
    x: 0,
    y: 0,
    x2: 0,
    y2: 1,
    colorStops: [
      { offset: 0, color: from },
      { offset: 1, color: to },
    ],
  }
}

function chartGrid(top = 22, right = 18, bottom = 42, left = 42) {
  return { top, left, right, bottom }
}

function axisTooltip() {
  return {
    trigger: 'axis',
    backgroundColor: 'rgba(23, 32, 51, 0.92)',
    borderWidth: 0,
    padding: [10, 12],
    textStyle: { color: '#ffffff' },
    axisPointer: {
      type: 'line',
      lineStyle: { color: 'rgba(8, 125, 182, 0.32)', width: 2 },
    },
  }
}

function legendStyle() {
  return {
    top: 0,
    right: 0,
    itemWidth: 10,
    itemHeight: 10,
    textStyle: { color: '#627286' },
  }
}

function axisCategory(showLabel = true) {
  return {
    type: 'category',
    data: trendDates.value,
    boundaryGap: true,
    axisLine: { lineStyle: { color: '#d9e3ef' } },
    axisTick: { show: false },
    axisLabel: { show: showLabel, color: '#8a98aa' },
  }
}

function axisValue(showLabel = true) {
  return {
    type: 'value',
    splitLine: { lineStyle: { color: '#edf2f7', type: 'dashed' } },
    axisLabel: { show: showLabel, color: '#8a98aa' },
  }
}

function formatNumber(value: number) {
  return new Intl.NumberFormat('zh-CN').format(value || 0)
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

function compactBytes(bytes: number) {
  if (!bytes) {
    return '0'
  }
  if (bytes < 1024 * 1024) {
    return `${Math.round(bytes / 1024)}K`
  }
  if (bytes < 1024 * 1024 * 1024) {
    return `${Math.round(bytes / 1024 / 1024)}M`
  }
  return `${(bytes / 1024 / 1024 / 1024).toFixed(1)}G`
}

onMounted(loadDashboard)
</script>

<template>
  <section v-loading="loading" class="admin-page">
    <div class="dashboard-hero">
      <div>
        <p class="eyebrow">Overview</p>
        <h2>数据大盘</h2>
        <p>{{ dateRangeText }}，用更清晰的图表布局展示增长、内容、存储与资产结构。</p>
      </div>
      <el-button :icon="Refresh" @click="loadDashboard">刷新</el-button>
    </div>

    <div class="overview-grid">
      <article v-for="item in overviewCards" :key="item.label" class="overview-card" :class="item.tone">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.hint }}</small>
      </article>
    </div>

    <section class="bento-layout">
      <article class="chart-card chart-card-wide user-panel">
        <div class="chart-title">
          <div>
            <span>Growth</span>
            <h3>用户增长曲线</h3>
            <p>用面积折线突出近 30 天注册趋势和峰值变化。</p>
          </div>
          <strong>{{ formatNumber(dashboard.todayNewUsers) }}</strong>
        </div>
        <VChart class="chart-large" :option="userTrendOption" autoresize />
      </article>

      <article class="chart-card composition-panel">
        <div class="chart-title">
          <div>
            <span>Structure</span>
            <h3>资产结构</h3>
            <p>内容与文件占比。</p>
          </div>
        </div>
        <VChart class="chart-donut" :option="contentCompositionOption" autoresize />
      </article>

      <article class="chart-card chart-card-wide content-panel">
        <div class="chart-title">
          <div>
            <span>Production</span>
            <h3>内容生产堆叠</h3>
            <p>笔记、任务、待办使用渐变堆叠柱展示，便于比较每日产出结构。</p>
          </div>
          <strong>{{ formatNumber(todayContentCount) }}</strong>
        </div>
        <VChart class="chart-medium" :option="contentTrendOption" autoresize />
      </article>

      <article class="chart-card storage-panel">
        <div class="chart-title">
          <div>
            <span>Storage</span>
            <h3>存储增长</h3>
            <p>渐变柱形图强调上传峰值。</p>
          </div>
        </div>
        <VChart class="chart-small" :option="storageTrendOption" autoresize />
      </article>

      <article class="chart-card team-panel">
        <div class="chart-title">
          <div>
            <span>Team</span>
            <h3>团队空间增长</h3>
            <p>协作空间新增趋势。</p>
          </div>
        </div>
        <VChart class="chart-small" :option="teamSpaceOption" autoresize />
      </article>

      <aside class="activity-card">
        <p class="eyebrow">Today</p>
        <h3>今日活跃概览</h3>
        <div class="activity-list">
          <div v-for="item in activitySummary" :key="item.label" class="activity-row" :class="item.tone">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </aside>
    </section>
  </section>
</template>

<style scoped>
.admin-page {
  display: grid;
  gap: 18px;
}

.dashboard-hero,
.overview-card,
.chart-card,
.activity-card {
  border: 1px solid rgba(151, 170, 190, 0.24);
  background: rgba(255, 255, 255, 0.93);
  box-shadow: 0 18px 42px rgba(31, 45, 61, 0.08);
}

.dashboard-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  overflow: hidden;
  position: relative;
  padding: 26px;
  border-radius: 26px;
  background:
    radial-gradient(circle at 82% 8%, rgba(8, 125, 182, 0.18), transparent 30%),
    radial-gradient(circle at 8% 96%, rgba(244, 108, 68, 0.12), transparent 34%),
    linear-gradient(135deg, #ffffff 0%, #f5fbff 100%);
}

.dashboard-hero::after {
  content: '';
  position: absolute;
  right: 160px;
  bottom: -80px;
  width: 180px;
  height: 180px;
  border-radius: 999px;
  border: 34px solid rgba(8, 125, 182, 0.08);
}

.eyebrow,
.chart-title span {
  margin: 0 0 8px;
  color: #087db6;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.dashboard-hero h2,
.chart-title h3,
.activity-card h3 {
  margin: 0;
  color: #172033;
}

.dashboard-hero h2 {
  font-size: 24px;
}

.dashboard-hero p,
.chart-title p {
  margin: 6px 0 0;
  color: #6b778c;
  line-height: 1.7;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.overview-card {
  display: grid;
  gap: 8px;
  overflow: hidden;
  position: relative;
  padding: 18px;
  border-radius: 20px;
}

.overview-card::before {
  content: '';
  position: absolute;
  inset: 0;
  opacity: 0.08;
}

.overview-card::after {
  content: '';
  position: absolute;
  right: -42px;
  bottom: -48px;
  width: 120px;
  height: 120px;
  border-radius: 999px;
  opacity: 0.18;
}

.overview-card.blue::before,
.overview-card.blue::after {
  background: #087db6;
}

.overview-card.green::before,
.overview-card.green::after {
  background: #4f8f6d;
}

.overview-card.orange::before,
.overview-card.orange::after {
  background: #f46c44;
}

.overview-card.purple::before,
.overview-card.purple::after {
  background: #6f63c6;
}

.overview-card span,
.overview-card small {
  z-index: 1;
  color: #6b778c;
}

.overview-card strong {
  z-index: 1;
  color: #172033;
  font-size: 28px;
}

.bento-layout {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  grid-auto-rows: minmax(180px, auto);
  gap: 16px;
}

.chart-card,
.activity-card {
  display: grid;
  gap: 12px;
  min-width: 0;
  padding: 18px;
  border-radius: 24px;
}

.chart-card {
  overflow: hidden;
  position: relative;
}

.chart-card::after {
  content: '';
  position: absolute;
  right: -90px;
  top: -90px;
  width: 190px;
  height: 190px;
  border-radius: 999px;
  opacity: 0.1;
  pointer-events: none;
}

.user-panel,
.content-panel {
  grid-column: span 8;
}

.composition-panel {
  grid-column: span 4;
}

.storage-panel,
.team-panel,
.activity-card {
  grid-column: span 4;
}

.user-panel {
  background:
    radial-gradient(circle at 88% 16%, rgba(8, 125, 182, 0.08), transparent 30%),
    rgba(255, 255, 255, 0.93);
}

.user-panel::after {
  background: #087db6;
}

.content-panel::after {
  background: #f46c44;
}

.storage-panel::after {
  background: #6f63c6;
}

.team-panel::after {
  background: #3b82c4;
}

.composition-panel::after {
  background: #4f8f6d;
}

.chart-title {
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.chart-title strong {
  flex-shrink: 0;
  padding: 8px 12px;
  border-radius: 999px;
  background: #eef7fc;
  color: #087db6;
  font-size: 15px;
}

.chart-large {
  width: 100%;
  height: 330px;
}

.chart-medium {
  width: 100%;
  height: 300px;
}

.chart-small {
  width: 100%;
  height: 250px;
}

.chart-donut {
  width: 100%;
  height: 330px;
}

.activity-card {
  align-content: start;
  background:
    radial-gradient(circle at 88% 8%, rgba(244, 108, 68, 0.14), transparent 34%),
    linear-gradient(180deg, #ffffff 0%, #fffaf7 100%);
}

.activity-card h3 {
  font-size: 18px;
}

.activity-list {
  display: grid;
  gap: 12px;
  margin-top: 10px;
}

.activity-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
}

.activity-row span {
  color: #6b778c;
}

.activity-row strong {
  font-size: 24px;
}

.activity-row.blue strong {
  color: #087db6;
}

.activity-row.orange strong {
  color: #f46c44;
}

.activity-row.green strong {
  color: #4f8f6d;
}

@media (max-width: 1280px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .user-panel,
  .content-panel,
  .composition-panel,
  .storage-panel,
  .team-panel,
  .activity-card {
    grid-column: span 6;
  }
}

@media (max-width: 900px) {
  .dashboard-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }

  .user-panel,
  .content-panel,
  .composition-panel,
  .storage-panel,
  .team-panel,
  .activity-card {
    grid-column: span 12;
  }
}
</style>
