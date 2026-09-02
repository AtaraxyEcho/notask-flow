<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { adminMonitorService } from '@/api/modules/admin'
import type { AdminSystemMonitor } from '@/types/app'
import { formatDateTime } from '@/utils/date'

interface MetricCard {
  label: string
  value: string
  hint: string
  ratio: number
  tone: string
}

interface DetailCard {
  label: string
  value: string
  hint: string
}

const loading = ref(false)
const monitor = ref<AdminSystemMonitor | null>(null)
let refreshTimer: number | undefined

const mainMetrics = computed<MetricCard[]>(() => [
  {
    label: 'CPU 使用率',
    value: formatPercent(monitor.value?.cpuUsage || 0),
    hint: `进程 ${formatPercent(monitor.value?.processCpuUsage || 0)} / Load ${formatDecimal(monitor.value?.systemLoadAverage || 0)}`,
    ratio: monitor.value?.cpuUsage || 0,
    tone: 'cpu',
  },
  {
    label: '物理内存',
    value: formatPercent(monitor.value?.physicalMemoryUsage || 0),
    hint: `${formatBytes(monitor.value?.physicalMemoryUsedBytes || 0)} / ${formatBytes(monitor.value?.physicalMemoryTotalBytes || 0)}`,
    ratio: monitor.value?.physicalMemoryUsage || 0,
    tone: 'physical',
  },
  {
    label: 'JVM 堆内存',
    value: formatPercent(monitor.value?.jvmHeapUsage || 0),
    hint: `${formatBytes(monitor.value?.jvmHeapUsedBytes || 0)} / ${formatBytes(monitor.value?.jvmHeapMaxBytes || 0)}`,
    ratio: monitor.value?.jvmHeapUsage || 0,
    tone: 'memory',
  },
  {
    label: '磁盘使用',
    value: formatPercent(monitor.value?.diskUsage || 0),
    hint: `${formatBytes(monitor.value?.diskUsedBytes || 0)} / ${formatBytes(monitor.value?.diskTotalBytes || 0)}`,
    ratio: monitor.value?.diskUsage || 0,
    tone: 'disk',
  },
])

const redisCards = computed<DetailCard[]>(() => [
  {
    label: 'Redis 命中率',
    value: formatPercent(monitor.value?.redisHitRate || 0),
    hint: `命中 ${formatNumber(monitor.value?.redisKeyspaceHits || 0)} / 未命中 ${formatNumber(monitor.value?.redisKeyspaceMisses || 0)}`,
  },
  {
    label: 'Redis 内存',
    value: formatBytes(monitor.value?.redisUsedMemoryBytes || 0),
    hint: redisMemoryHint.value,
  },
  {
    label: 'Redis Key 数',
    value: formatNumber(monitor.value?.redisKeyCount || 0),
    hint: `客户端 ${formatNumber(monitor.value?.redisConnectedClients || 0)} / OPS ${formatNumber(monitor.value?.redisOpsPerSecond || 0)}`,
  },
])

const mysqlCards = computed<DetailCard[]>(() => [
  {
    label: 'MySQL QPS',
    value: formatDecimal(monitor.value?.mysqlQueriesPerSecond || 0),
    hint: `累计请求 ${formatNumber(monitor.value?.mysqlQuestionCount || 0)}`,
  },
  {
    label: 'MySQL 慢查询',
    value: formatNumber(monitor.value?.mysqlSlowQueryCount || 0),
    hint: `运行 ${formatDuration((monitor.value?.mysqlUptimeSeconds || 0) * 1000)}`,
  },
  {
    label: 'MySQL 线程',
    value: formatNumber(monitor.value?.mysqlThreadsConnected || 0),
    hint: `运行中 ${formatNumber(monitor.value?.mysqlThreadsRunning || 0)}`,
  },
])

const runtimeCards = computed<DetailCard[]>(() => [
  { label: '活动线程', value: formatNumber(monitor.value?.threadCount || 0), hint: `守护线程 ${formatNumber(monitor.value?.daemonThreadCount || 0)}` },
  { label: 'GC 次数', value: formatNumber(monitor.value?.gcCount || 0), hint: `累计耗时 ${formatDuration(monitor.value?.gcTimeMillis || 0)}` },
  { label: '运行时长', value: formatDuration(monitor.value?.uptimeMillis || 0), hint: '当前后端进程 uptime' },
  { label: '可用磁盘', value: formatBytes(monitor.value?.diskFreeBytes || 0), hint: `总容量 ${formatBytes(monitor.value?.diskTotalBytes || 0)}` },
])

const networkCards = computed<DetailCard[]>(() => [
  {
    label: '接收流量',
    value: monitor.value?.networkTrafficSupported ? formatBytes(monitor.value?.networkReceivedBytes || 0) : '未开放',
    hint: monitor.value?.networkTrafficSupported ? 'Linux /proc/net/dev 累计接收' : '当前系统未暴露网卡流量',
  },
  {
    label: '发送流量',
    value: monitor.value?.networkTrafficSupported ? formatBytes(monitor.value?.networkTransmittedBytes || 0) : '未开放',
    hint: monitor.value?.networkTrafficSupported ? 'Linux /proc/net/dev 累计发送' : 'Windows 环境默认仅显示网卡状态',
  },
  {
    label: '网络接口',
    value: formatNumber(monitor.value?.networkActiveInterfaceCount || 0),
    hint: `总接口 ${formatNumber(monitor.value?.networkInterfaceCount || 0)}`,
  },
])

const systemInfo = computed(() => {
  if (!monitor.value) {
    return []
  }
  return [
    { label: '系统', value: `${monitor.value.osName} ${monitor.value.osVersion}` },
    { label: '架构', value: monitor.value.osArch },
    { label: 'Java', value: monitor.value.javaVersion },
    { label: 'CPU 核心', value: `${monitor.value.cpuCoreCount || 0} Core` },
  ]
})

const redisMemoryHint = computed(() => {
  const maxMemory = monitor.value?.redisMaxMemoryBytes || 0
  if (maxMemory <= 0) {
    return 'Redis 未设置 maxmemory 上限'
  }
  return `${formatPercent(monitor.value?.redisMemoryUsage || 0)} / ${formatBytes(maxMemory)}`
})

async function loadMonitor() {
  loading.value = true
  try {
    monitor.value = await adminMonitorService.snapshot()
  } finally {
    loading.value = false
  }
}

function scheduleRefresh() {
  window.clearInterval(refreshTimer)
  refreshTimer = window.setInterval(() => {
    loadMonitor().catch(() => undefined)
  }, 10000)
}

function progressWidth(value: number) {
  return `${Math.max(3, Math.round((value || 0) * 100))}%`
}

function formatPercent(value: number) {
  return `${Math.round((value || 0) * 100)}%`
}

function formatDecimal(value: number) {
  return (value || 0).toFixed(2)
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

function formatDuration(milliseconds: number) {
  const seconds = Math.floor(milliseconds / 1000)
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (days > 0) {
    return `${days}天 ${hours}小时`
  }
  if (hours > 0) {
    return `${hours}小时 ${minutes}分钟`
  }
  return `${Math.max(1, minutes)}分钟`
}

onMounted(() => {
  loadMonitor().catch(() => undefined)
  scheduleRefresh()
})

onUnmounted(() => {
  window.clearInterval(refreshTimer)
})
</script>

<template>
  <section v-loading="loading" class="monitor-page">
    <div class="monitor-hero">
      <div>
        <p class="eyebrow">System Monitor</p>
        <h2>性能监控</h2>
        <p>覆盖系统、JVM、Redis、网络、MySQL 与磁盘快照，每 10 秒自动刷新。</p>
      </div>
      <div class="monitor-actions">
        <span>更新时间：{{ formatDateTime(monitor?.timestamp, '尚未刷新') }}</span>
        <el-button :icon="Refresh" @click="loadMonitor">刷新</el-button>
      </div>
    </div>

    <div class="system-strip">
      <article v-for="item in systemInfo" :key="item.label">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </div>

    <div class="monitor-grid">
      <article v-for="item in mainMetrics" :key="item.label" class="monitor-card">
        <div class="monitor-card-header">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
        <div class="monitor-progress">
          <i :class="item.tone" :style="{ width: progressWidth(item.ratio) }"></i>
        </div>
        <small>{{ item.hint }}</small>
      </article>
    </div>

    <div class="monitor-sections">
      <section class="monitor-panel redis-panel">
        <div class="panel-title">
          <h3>Redis</h3>
          <p>缓存命中、Key 数量、内存和 OPS，便于判断缓存是否真正发挥作用。</p>
        </div>
        <div class="detail-grid three">
          <article v-for="item in redisCards" :key="item.label" class="detail-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.hint }}</small>
          </article>
        </div>
      </section>

      <section class="monitor-panel mysql-panel">
        <div class="panel-title">
          <h3>MySQL</h3>
          <p>基于全局状态变量统计平均 QPS、慢查询和连接线程。</p>
        </div>
        <div class="detail-grid three">
          <article v-for="item in mysqlCards" :key="item.label" class="detail-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.hint }}</small>
          </article>
        </div>
      </section>
    </div>

    <div class="monitor-sections">
      <section class="monitor-panel">
        <div class="panel-title">
          <h3>网络</h3>
          <p>Linux 环境展示网卡累计流量，Windows 环境展示网卡数量与活跃状态。</p>
        </div>
        <div class="detail-grid three">
          <article v-for="item in networkCards" :key="item.label" class="detail-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.hint }}</small>
          </article>
        </div>
      </section>

      <section class="monitor-panel">
        <div class="panel-title">
          <h3>运行细节</h3>
          <p>线程、GC、Uptime 与磁盘余量，用于快速判断后端运行健康度。</p>
        </div>
        <div class="detail-grid two">
          <article v-for="item in runtimeCards" :key="item.label" class="detail-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.hint }}</small>
          </article>
        </div>
      </section>
    </div>
  </section>
</template>

<style scoped>
.monitor-page {
  display: grid;
  gap: 18px;
}

.monitor-hero,
.monitor-card,
.monitor-panel,
.detail-card,
.system-strip article {
  border: 1px solid rgba(151, 170, 190, 0.28);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 16px 36px rgba(31, 45, 61, 0.07);
}

.monitor-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  overflow: hidden;
  position: relative;
  padding: 24px;
  border-radius: 22px;
  background:
    radial-gradient(circle at 88% 18%, rgba(244, 108, 68, 0.18), transparent 30%),
    linear-gradient(135deg, #ffffff 0%, #f4f9fd 100%);
}

.monitor-hero::after {
  content: '';
  position: absolute;
  right: -70px;
  bottom: -90px;
  width: 220px;
  height: 220px;
  border-radius: 999px;
  background: rgba(8, 125, 182, 0.1);
}

.eyebrow {
  margin: 0 0 8px;
  color: #087db6;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.monitor-hero h2,
.panel-title h3 {
  margin: 0;
}

.monitor-hero h2 {
  color: #172033;
  font-size: 24px;
}

.monitor-hero p,
.panel-title p {
  margin: 6px 0 0;
  color: #6b778c;
  line-height: 1.7;
}

.monitor-actions {
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #6b778c;
  font-size: 13px;
}

.system-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.system-strip article {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 16px;
}

.system-strip span,
.monitor-card small,
.detail-card span,
.detail-card small {
  color: #6b778c;
}

.system-strip strong {
  overflow: hidden;
  color: #172033;
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.monitor-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.monitor-card {
  display: grid;
  gap: 14px;
  padding: 18px;
  border-radius: 18px;
}

.monitor-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.monitor-card-header strong {
  color: #172033;
  font-size: 28px;
}

.monitor-progress {
  height: 10px;
  overflow: hidden;
  border-radius: 999px;
  background: #eef5fb;
}

.monitor-progress i {
  display: block;
  height: 100%;
  min-width: 4px;
  border-radius: inherit;
}

.monitor-progress .cpu {
  background: linear-gradient(90deg, #087db6, #5ab7dd);
}

.monitor-progress .physical {
  background: linear-gradient(90deg, #6f63c6, #b8abff);
}

.monitor-progress .memory {
  background: linear-gradient(90deg, #f46c44, #ffaf91);
}

.monitor-progress .disk {
  background: linear-gradient(90deg, #4f8f6d, #9ac8a9);
}

.monitor-sections {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.monitor-panel {
  padding: 20px;
  border-radius: 20px;
}

.redis-panel {
  background:
    radial-gradient(circle at top right, rgba(244, 108, 68, 0.1), transparent 34%),
    rgba(255, 255, 255, 0.92);
}

.mysql-panel {
  background:
    radial-gradient(circle at top right, rgba(8, 125, 182, 0.1), transparent 34%),
    rgba(255, 255, 255, 0.92);
}

.detail-grid {
  display: grid;
  gap: 14px;
  margin-top: 16px;
}

.detail-grid.three {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.detail-grid.two {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.detail-card {
  display: grid;
  gap: 8px;
  padding: 16px;
  border-radius: 16px;
}

.detail-card strong {
  color: #172033;
  font-size: 22px;
}

@media (max-width: 1280px) {
  .monitor-grid,
  .system-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .monitor-sections {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .monitor-hero,
  .monitor-actions {
    align-items: flex-start;
    flex-direction: column;
  }

  .monitor-grid,
  .system-strip,
  .detail-grid.three,
  .detail-grid.two {
    grid-template-columns: 1fr;
  }
}
</style>
