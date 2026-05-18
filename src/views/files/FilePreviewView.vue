<template>
  <div class="space-y-5">
    <div class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
      <div class="min-w-0">
        <button class="mb-3 inline-flex items-center gap-2 text-sm font-semibold text-primary" type="button" @click="goBack">
          <span class="material-symbols-outlined text-[18px]">arrow_back</span>
          {{ t('files.backToFiles') }}
        </button>
        <p class="text-caption font-bold uppercase tracking-[0.24em] text-primary/70">
          {{ t('files.previewPageTitle') }}
        </p>
        <h1 class="mt-2 truncate font-display-serif text-4xl text-on-surface">
          {{ file?.displayName || file?.fileName || '-' }}
        </h1>
        <div class="mt-3 flex flex-wrap gap-2 text-caption text-on-surface-variant">
          <span class="app-chip">{{ fileTypeLabel }}</span>
          <span class="app-chip">{{ formatBytes(file?.fileSize) }}</span>
          <span class="app-chip">{{ formatDate(file?.gmtCreate) }}</span>
        </div>
      </div>

      <button class="app-primary-button px-4 py-2.5" type="button" @click="downloadFile">
        <span class="material-symbols-outlined text-base">download</span>
        {{ t('files.download') }}
      </button>
    </div>

    <div class="preview-shell">
      <div v-if="loading" class="flex min-h-[56vh] items-center justify-center text-on-surface-variant">
        {{ t('common.loading') }}
      </div>

      <template v-else>
        <img v-if="previewKind === 'image' && objectUrl" :src="objectUrl" class="mx-auto max-h-[70vh] rounded-3xl object-contain shadow-ambient" />

        <div v-else-if="previewKind === 'pdf' && officeSource" class="office-preview-frame custom-scrollbar">
          <VueOfficePdf :src="officeSource" class="office-preview-document" />
        </div>

        <video
          v-else-if="previewKind === 'video' && objectUrl"
          :src="objectUrl"
          class="mx-auto max-h-[72vh] w-full rounded-3xl bg-black shadow-ambient"
          controls
        ></video>

        <div v-else-if="previewKind === 'audio' && objectUrl" class="mx-auto flex min-h-[48vh] max-w-2xl flex-col items-center justify-center">
          <div class="mb-6 flex h-24 w-24 items-center justify-center rounded-[2rem] bg-primary-fixed text-primary">
            <span class="material-symbols-outlined text-[48px]">graphic_eq</span>
          </div>
          <audio :src="objectUrl" class="w-full" controls></audio>
        </div>

        <div
          v-else-if="previewKind === 'markdown'"
          class="markdown-preview custom-scrollbar max-h-[72vh] overflow-auto rounded-3xl bg-white/80 p-8"
          v-html="markdownHtml"
        ></div>

        <div v-else-if="previewKind === 'csv'" class="custom-scrollbar max-h-[72vh] overflow-auto rounded-3xl bg-white/80 p-5">
          <table class="w-full min-w-[640px] border-separate border-spacing-0 text-left text-sm">
            <tbody>
              <tr v-for="(row, rowIndex) in csvRows" :key="rowIndex">
                <td
                  v-for="(cell, cellIndex) in row"
                  :key="`${rowIndex}-${cellIndex}`"
                  class="border-b border-outline-variant/30 px-4 py-3"
                  :class="rowIndex === 0 ? 'bg-surface-container font-bold text-on-surface' : 'text-on-surface-variant'"
                >
                  {{ cell }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <pre
          v-else-if="previewKind === 'text'"
          class="custom-scrollbar max-h-[72vh] overflow-auto whitespace-pre-wrap rounded-3xl bg-[#241f1d] p-6 text-sm leading-7 text-white"
        >{{ textContent }}</pre>

        <div v-else-if="previewKind === 'docx' && officeSource && !officeFallback" class="office-preview-frame custom-scrollbar">
          <VueOfficeDocx :src="officeSource" class="office-preview-document" @error="loadOfficeFallback" />
        </div>

        <div v-else-if="previewKind === 'excel' && officeSource && !officeFallback" class="office-preview-frame custom-scrollbar">
          <VueOfficeExcel :src="officeSource" class="office-preview-document" @error="loadOfficeFallback" />
        </div>

        <div v-else-if="previewKind === 'office' || officeFallback" class="office-preview-frame custom-scrollbar">
          <iframe class="office-preview-iframe" sandbox="" :srcdoc="officePreviewDocument" title="office-preview"></iframe>
        </div>

        <div v-else class="mx-auto flex min-h-[48vh] max-w-xl flex-col items-center justify-center text-center">
          <div class="mb-5 flex h-20 w-20 items-center justify-center rounded-[2rem] bg-primary-fixed text-primary">
            <span class="material-symbols-outlined text-[42px]">{{ unsupportedIcon }}</span>
          </div>
          <h2 class="font-title-serif text-3xl text-on-surface">{{ fileTypeLabel }}</h2>
          <p class="mt-3 text-sm leading-7 text-on-surface-variant">
            {{ t('files.unsupportedPreview') }}
          </p>
          <button class="app-primary-button mt-6" type="button" @click="downloadFile">
            <span class="material-symbols-outlined text-base">download</span>
            {{ t('files.download') }}
          </button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import '@vue-office/docx/lib/index.css'
import '@vue-office/excel/lib/index.css'
import { computed, defineAsyncComponent, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fileService } from '@/api/services'
import { useI18n } from '@/i18n'
import { useSpaceStore } from '@/stores/space'
import type { ManagedFile } from '@/types/app'
import { formatDateTime } from '@/utils/date'
import { renderMarkdownLite } from '@/utils/markdown'
import { sanitizeHtml } from '@/utils/sanitize'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const spaceStore = useSpaceStore()
const VueOfficeDocx = defineAsyncComponent(() => import('@vue-office/docx'))
const VueOfficeExcel = defineAsyncComponent(() => import('@vue-office/excel'))
const VueOfficePdf = defineAsyncComponent(() => import('@vue-office/pdf'))

const file = ref<ManagedFile | null>(null)
const objectUrl = ref('')
const objectUrlIsBlob = ref(false)
const officeSource = ref('')
const officeSourceIsBlob = ref(false)
const officeFallback = ref(false)
const textContent = ref('')
const htmlContent = ref('')
const loading = ref(false)

const fileId = computed(() => Number(route.params.fileId))

const fileExtension = computed(() => {
  const name = file.value?.fileName || file.value?.displayName || ''
  const chunks = name.toLowerCase().split('.')
  return chunks.length > 1 ? chunks.pop() || '' : ''
})

const fileTypeLabel = computed(() => {
  const mimeType = file.value?.mimeType || ''
  const extension = fileExtension.value
  if (['xls', 'xlsx', 'csv'].includes(extension) || mimeType.includes('spreadsheet') || mimeType.includes('sheet') || mimeType.includes('excel')) {
    return 'Excel'
  }
  if (['doc', 'docx'].includes(extension) || mimeType.includes('word')) {
    return 'Word'
  }
  if (extension === 'pdf' || mimeType.includes('pdf')) {
    return 'PDF'
  }
  if (mimeType.startsWith('image/')) {
    return 'Image'
  }
  if (mimeType.startsWith('video/')) {
    return 'Video'
  }
  if (mimeType.startsWith('audio/')) {
    return 'Audio'
  }
  if (['zip', 'rar', '7z'].includes(extension) || mimeType.includes('zip') || mimeType.includes('compressed')) {
    return 'Archive'
  }
  if (mimeType.startsWith('text/') || ['txt', 'md', 'json', 'csv', 'log'].includes(extension)) {
    return 'Text'
  }
  return mimeType.split('/').pop() || t('common.unknown')
})

const previewKind = computed<'image' | 'pdf' | 'video' | 'audio' | 'markdown' | 'csv' | 'text' | 'docx' | 'excel' | 'office' | 'unsupported'>(() => {
  const mimeType = file.value?.mimeType || ''
  const extension = fileExtension.value
  if (mimeType.startsWith('image/')) {
    return 'image'
  }
  if (mimeType.startsWith('video/') || ['mp4', 'webm', 'ogg', 'mov'].includes(extension)) {
    return 'video'
  }
  if (mimeType.startsWith('audio/') || ['mp3', 'wav', 'flac', 'm4a', 'aac'].includes(extension)) {
    return 'audio'
  }
  if (mimeType.includes('pdf') || extension === 'pdf') {
    return 'pdf'
  }
  if (['md', 'markdown'].includes(extension) || mimeType.includes('markdown')) {
    return 'markdown'
  }
  if (extension === 'csv' || mimeType.includes('csv')) {
    return 'csv'
  }
  if (extension === 'docx' || mimeType.includes('wordprocessingml')) {
    return 'docx'
  }
  if (['xls', 'xlsx'].includes(extension) || mimeType.includes('spreadsheet') || mimeType.includes('sheet') || mimeType.includes('excel')) {
    return 'excel'
  }
  if (['doc', 'ppt', 'pptx', 'rtf', 'odt', 'ods', 'odp'].includes(extension)) {
    return 'office'
  }
  if (mimeType.startsWith('text/') || ['txt', 'json', 'log', 'xml', 'yaml', 'yml'].includes(extension)) {
    return 'text'
  }
  return 'unsupported'
})

const markdownHtml = computed(() => sanitizeHtml(renderMarkdownLite(textContent.value)))
const officePreviewDocument = computed(() => `<!doctype html>
<html>
  <head>
    <meta charset="utf-8" />
    <style>
      html, body {
        margin: 0;
        min-height: 100%;
        background: #ffffff;
        color: #1e1b19;
        font-family: "Times New Roman", "Noto Serif", serif;
      }
      body {
        padding: 40px;
      }
      table {
        border-collapse: collapse;
        max-width: 100%;
      }
      td, th {
        border: 1px solid #d9d2cf;
        padding: 6px 8px;
        vertical-align: top;
      }
      img {
        max-width: 100%;
        height: auto;
      }
      p {
        margin: 0 0 0.75rem;
      }
    </style>
  </head>
  <body>${sanitizeHtml(htmlContent.value || `<p>${t('files.noPreviewText')}</p>`)}</body>
</html>`)
const csvRows = computed(() =>
  textContent.value
    .split(/\r?\n/)
    .filter((line) => line.trim())
    .slice(0, 200)
    .map((line) => line.split(',').map((cell) => cell.trim())),
)

const unsupportedIcon = computed(() => {
  if (fileTypeLabel.value === 'Excel') {
    return 'table_chart'
  }
  if (fileTypeLabel.value === 'Word') {
    return 'article'
  }
  if (fileTypeLabel.value === 'Video') {
    return 'movie'
  }
  if (fileTypeLabel.value === 'Audio') {
    return 'graphic_eq'
  }
  if (fileTypeLabel.value === 'Archive') {
    return 'folder_zip'
  }
  return 'draft'
})

const formatBytes = (size?: number) => {
  const value = size || 0
  if (value < 1024) {
    return `${value} B`
  }
  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(1)} KB`
  }
  if (value < 1024 * 1024 * 1024) {
    return `${(value / 1024 / 1024).toFixed(1)} MB`
  }
  return `${(value / 1024 / 1024 / 1024).toFixed(1)} GB`
}

const formatDate = (value?: string) => {
  return formatDateTime(value, '-')
}

const revokeUrl = (url: string, isBlobUrl: boolean) => {
  if (url && isBlobUrl) {
    URL.revokeObjectURL(url)
  }
}

const revokeObjectUrl = () => {
  revokeUrl(objectUrl.value, objectUrlIsBlob.value)
  objectUrl.value = ''
  objectUrlIsBlob.value = false
}

const revokeOfficeSource = () => {
  revokeUrl(officeSource.value, officeSourceIsBlob.value)
  officeSource.value = ''
  officeSourceIsBlob.value = false
}

const loadPreviewUrl = async () => {
  if (!spaceStore.currentSpaceId) {
    return ''
  }

  const nextFile = await fileService.previewUrl(spaceStore.currentSpaceId, fileId.value)
  return nextFile.downloadUrl || ''
}

const loadObjectPreviewSource = async () => {
  const previewUrl = await loadPreviewUrl()
  if (previewUrl) {
    objectUrl.value = previewUrl
    objectUrlIsBlob.value = false
    return
  }

  const blob = await fileService.previewBlob(spaceStore.currentSpaceId!, fileId.value)
  objectUrl.value = URL.createObjectURL(blob)
  objectUrlIsBlob.value = true
}

const loadOfficePreviewSource = async () => {
  const previewUrl = await loadPreviewUrl()
  if (previewUrl) {
    officeSource.value = previewUrl
    officeSourceIsBlob.value = false
    return
  }

  const blob = await fileService.previewBlob(spaceStore.currentSpaceId!, fileId.value)
  officeSource.value = URL.createObjectURL(blob)
  officeSourceIsBlob.value = true
}

const loadOfficeFallback = async () => {
  if (!spaceStore.currentSpaceId || !file.value) {
    return
  }

  const preview = await fileService.previewHtml(spaceStore.currentSpaceId, file.value.id)
  htmlContent.value = sanitizeHtml(preview.htmlContent || '')
  officeFallback.value = true
}

const loadPreview = async () => {
  if (!spaceStore.currentSpaceId || Number.isNaN(fileId.value)) {
    return
  }

  loading.value = true
  revokeObjectUrl()
  revokeOfficeSource()
  textContent.value = ''
  htmlContent.value = ''
  officeFallback.value = false
  try {
    file.value = await fileService.detail(spaceStore.currentSpaceId, fileId.value)
    if (['pdf', 'docx', 'excel'].includes(previewKind.value)) {
      await loadOfficePreviewSource()
      return
    }
    if (previewKind.value === 'office') {
      await loadOfficeFallback()
      return
    }
    if (['text', 'markdown', 'csv'].includes(previewKind.value)) {
      const blob = await fileService.previewBlob(spaceStore.currentSpaceId, fileId.value)
      textContent.value = await blob.text()
      return
    }
    if (['image', 'video', 'audio'].includes(previewKind.value)) {
      await loadObjectPreviewSource()
      return
    }
  } finally {
    loading.value = false
  }
}

const downloadFile = async () => {
  if (!spaceStore.currentSpaceId || !file.value) {
    return
  }
  const nextFile = await fileService.downloadUrl(spaceStore.currentSpaceId, file.value.id)
  if (nextFile.downloadUrl) {
    window.open(nextFile.downloadUrl, '_blank', 'noopener,noreferrer')
  }
}

const goBack = () => {
  router.push({ name: 'files' })
}

watch(
  () => ({
    spaceId: spaceStore.currentSpaceId,
    fileId: fileId.value,
  }),
  () => {
    loadPreview().catch(() => undefined)
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  revokeObjectUrl()
  revokeOfficeSource()
})
</script>

<style scoped>
.preview-shell {
  background:
    radial-gradient(circle at top left, rgba(255, 138, 101, 0.14), transparent 34%),
    radial-gradient(circle at bottom right, rgba(159, 65, 34, 0.1), transparent 32%),
    var(--surface-container-lowest);
  border: 1px solid var(--outline-variant);
  border-radius: 2rem;
  min-height: 60vh;
  padding: 1.25rem;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.05);
}

.office-preview-frame {
  background: #ffffff;
  border: 1px solid rgba(221, 192, 184, 0.7);
  border-radius: 1.5rem;
  height: 72vh;
  overflow: auto;
  width: 100%;
}

.office-preview-document {
  min-height: 100%;
  width: 100%;
}

.office-preview-iframe {
  border: 0;
  height: 100%;
  min-height: 72vh;
  width: 100%;
}

.office-preview-frame :deep(.vue-office-docx),
.office-preview-frame :deep(.vue-office-excel),
.office-preview-frame :deep(.vue-office-pdf) {
  min-height: 100%;
}
</style>
