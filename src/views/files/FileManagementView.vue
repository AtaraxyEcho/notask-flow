<template>
  <div class="space-y-5 text-[15px]">
    <div class="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
      <div>
        <p class="text-caption font-bold uppercase tracking-[0.24em] text-primary/70">
          {{ currentSpaceName }}
        </p>
        <h1 class="mt-2 font-display-serif text-4xl text-on-surface">
          {{ t('files.title') }}
        </h1>
        <p class="mt-2 max-w-3xl text-sm leading-7 text-on-surface-variant">
          {{ t('files.description') }}
        </p>
      </div>

      <div v-if="canManageFiles" class="flex flex-wrap gap-2">
        <button
          v-if="canConfigureUploadSettings"
          class="app-secondary-button px-4 py-2.5"
          type="button"
          @click="openUploadSettingsDialog"
        >
          <span class="material-symbols-outlined text-base">tune</span>
          {{ t('files.uploadSettings') }}
        </button>
        <button class="app-primary-button px-4 py-2.5" type="button" @click="triggerUpload">
          <span class="material-symbols-outlined text-base">upload_file</span>
          {{ t('files.upload') }}
        </button>
        <input ref="fileInputRef" class="hidden" type="file" :accept="uploadAccept" @change="handleFileChange" />
      </div>
    </div>

    <EmptyState
      v-if="!canManageFiles"
      :title="t('files.permissionTitle')"
      :description="t('files.permissionDescription')"
      icon="lock"
    />

    <template v-else>
      <div class="grid gap-3 md:grid-cols-3">
        <div class="file-stat-card">
          <div class="flex items-center justify-between">
            <span class="app-chip">{{ t('files.totalFiles') }}</span>
            <span class="material-symbols-outlined text-primary">folder_open</span>
          </div>
          <div class="mt-4 text-3xl font-semibold text-on-surface">{{ stats.totalCount }}</div>
        </div>
        <div class="file-stat-card">
          <div class="flex items-center justify-between">
            <span class="app-chip">{{ t('files.totalSize') }}</span>
            <span class="material-symbols-outlined text-primary">database</span>
          </div>
          <div class="mt-4 text-3xl font-semibold text-on-surface">{{ formatBytes(stats.totalSize) }}</div>
        </div>
        <div class="file-stat-card">
          <div class="flex items-center justify-between">
            <span class="app-chip">{{ t('files.trashFiles') }}</span>
            <span class="material-symbols-outlined text-red-500">delete</span>
          </div>
          <div class="mt-4 text-3xl font-semibold text-on-surface">{{ stats.trashCount }}</div>
        </div>
      </div>

      <div class="app-shell p-5">
        <div class="mb-4 flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
          <div class="flex flex-col gap-3 md:flex-row md:items-center">
            <label class="file-filter-input">
              <span class="material-symbols-outlined text-[18px] text-on-surface-variant">search</span>
              <input
                v-model="query.keyword"
                class="w-full border-none bg-transparent p-0 text-sm outline-none focus:ring-0"
                :placeholder="t('files.searchPlaceholder')"
              />
            </label>

            <el-select
              v-model="query.mimeType"
              clearable
              class="file-filter-select"
              :placeholder="t('files.mimePlaceholder')"
              @change="reloadFromFirstPage"
            >
              <el-option label="PDF" value="application/pdf" />
              <el-option label="Word" value="word" />
              <el-option label="Excel" value="sheet" />
              <el-option label="Image" value="image/" />
              <el-option label="Video" value="video/" />
              <el-option label="Audio" value="audio/" />
              <el-option label="ZIP" value="zip" />
              <el-option label="Text" value="text/" />
            </el-select>
          </div>

          <div class="flex flex-wrap gap-2">
            <button
              class="inline-flex items-center gap-2 rounded-full px-4 py-2.5 text-label-bold shadow-ambient"
              :class="query.trashed ? 'bg-primary text-on-primary' : 'border border-outline-variant bg-surface text-on-surface'"
              type="button"
              @click="toggleTrashMode"
            >
              <span class="material-symbols-outlined text-base">{{ query.trashed ? 'folder_open' : 'delete' }}</span>
              {{ query.trashed ? t('files.hideTrash') : t('files.showTrash') }}
            </button>
          </div>
        </div>

        <div class="grid gap-4 lg:grid-cols-[300px_1fr]">
          <aside class="rounded-[1.35rem] border border-outline-variant/50 bg-surface-container-low p-3">
            <div class="mb-2 flex items-center justify-between px-2">
              <h2 class="text-label-bold text-on-surface">{{ t('files.folders') }}</h2>
              <div class="flex items-center gap-1">
                <button class="app-icon-button h-8 w-8" type="button" :title="t('files.newFolder')" @click="startCreateFolder">
                  <span class="material-symbols-outlined text-[18px]">add</span>
                </button>
                <button class="app-icon-button h-8 w-8" type="button" @click="loadTreeData">
                  <span class="material-symbols-outlined text-[18px]">refresh</span>
                </button>
              </div>
            </div>

            <button
              class="file-tree-row mb-1 w-full"
              :class="!currentFolderId ? 'file-tree-row-active' : ''"
              type="button"
              @click="selectFolder(0)"
              @dragover.prevent="handleRootDragOver"
              @drop="dropToRoot"
            >
              <span class="material-symbols-outlined text-[18px]">folder_special</span>
              <span class="truncate">{{ t('files.allFiles') }}</span>
              <span class="ml-auto rounded-full bg-white/70 px-2 py-0.5 text-[11px] text-on-surface-variant">
                {{ treeFiles.length }}
              </span>
            </button>

            <div
              v-if="creatingFolder"
              class="file-tree-create-row mb-1"
              :style="{ paddingLeft: `${newFolderInputPadding}px` }"
            >
              <span class="material-symbols-outlined shrink-0 text-[18px] text-primary">create_new_folder</span>
              <input
                ref="folderInputRef"
                v-model="newFolderName"
                class="min-w-0 flex-1 border-none bg-transparent p-0 text-sm outline-none placeholder:text-on-surface-variant/50 focus:ring-0"
                :placeholder="t('files.folderNamePlaceholder')"
                type="text"
                @blur="handleFolderInputBlur"
                @keydown.enter.prevent="submitCreateFolder"
                @keydown.esc.prevent="cancelCreateFolder"
              />
            </div>

            <div v-if="sidebarRows.length" class="custom-scrollbar max-h-[58vh] space-y-1 overflow-y-auto pr-1">
              <div
                v-for="row in sidebarRows"
                :key="`${row.kind}-${row.id}`"
                class="group file-tree-row"
                :class="treeRowClasses(row)"
                :draggable="true"
                :style="{ paddingLeft: `${10 + row.depth * 18}px` }"
                @dragend="resetDragState"
                @dragover="row.kind === 'folder' ? handleFolderDragOver($event, row.folder.id) : undefined"
                @dragstart="startTreeDrag($event, row)"
                @drop="row.kind === 'folder' ? dropOnFolder($event, row.folder.id) : undefined"
              >
                <button
                  v-if="row.kind === 'folder' && row.expandable"
                  class="flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-stone-400 hover:bg-white/70 hover:text-primary"
                  type="button"
                  @click.stop="toggleFolder(row.folder.id)"
                >
                  <span class="material-symbols-outlined text-[16px]">
                    {{ isFolderCollapsed(row.folder.id) ? 'chevron_right' : 'expand_more' }}
                  </span>
                </button>
                <span v-else class="h-5 w-5 shrink-0"></span>

                <button class="flex min-w-0 flex-1 items-center gap-2 text-left" type="button" @click="handleTreeRowClick(row)">
                  <span class="material-symbols-outlined shrink-0 text-[18px]">
                    {{ row.kind === 'folder' ? (isFolderCollapsed(row.folder.id) ? 'folder' : 'folder_open') : fileIcon(row.file) }}
                  </span>
                  <span class="truncate">{{ row.label }}</span>
                </button>

                <template v-if="row.kind === 'folder'">
                  <button class="file-tree-action" type="button" @click.stop="openRenameFolderDialog(row.folder)">
                    <span class="material-symbols-outlined text-[16px]">edit</span>
                  </button>
                  <button class="file-tree-action danger" type="button" @click.stop="confirmDeleteFolder(row.folder)">
                    <span class="material-symbols-outlined text-[16px]">delete</span>
                  </button>
                </template>
              </div>
            </div>

            <p v-else-if="!creatingFolder" class="rounded-2xl bg-white/55 px-4 py-6 text-center text-sm text-on-surface-variant">
              {{ t('files.noFolders') }}
            </p>
          </aside>

          <section class="min-w-0">
            <div class="mb-3 flex flex-wrap items-center justify-between gap-3">
              <div>
                <div class="text-caption uppercase tracking-[0.22em] text-on-surface-variant">
                  {{ t('files.currentFolder') }}
                </div>
                <div class="mt-1 font-title-serif text-2xl text-on-surface">{{ currentFolderName }}</div>
              </div>
              <div class="text-caption text-on-surface-variant">
                {{ directoryEntries.length }} {{ t('files.tableName') }}
              </div>
            </div>

            <button
              v-if="showParentFolderEntry"
              class="parent-folder-row mb-3 w-full"
              type="button"
              @click="goParentFolder"
            >
              <span class="material-symbols-outlined text-[20px]">drive_folder_upload</span>
              <span class="font-semibold">{{ t('files.parentFolder') }}</span>
              <span class="ml-auto text-caption text-on-surface-variant">{{ t('files.rootFolder') }}</span>
            </button>

            <el-table
              v-loading="loading"
              :data="directoryEntries"
              class="file-table rounded-2xl text-sm"
              row-key="key"
              @row-dblclick="handleEntryDoubleClick"
            >
              <el-table-column min-width="340" :label="t('files.tableName')">
                <template #default="{ row }">
                  <div class="flex min-w-0 items-center gap-3">
                    <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-surface-container text-primary">
                      <span class="material-symbols-outlined text-[20px]">{{ entryIcon(row) }}</span>
                    </div>
                    <div class="min-w-0">
                      <div class="truncate font-semibold text-on-surface">{{ entryName(row) }}</div>
                      <div class="mt-1 truncate text-[12px] text-on-surface-variant">{{ entryDescription(row) }}</div>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column width="110" :label="t('files.tableSize')">
                <template #default="{ row }">{{ row.kind === 'folder' ? '-' : formatBytes(row.file.fileSize) }}</template>
              </el-table-column>
              <el-table-column width="120" :label="t('files.tableType')">
                <template #default="{ row }">
                  <span class="rounded-full bg-surface-container px-2.5 py-1 text-[12px] font-semibold text-on-surface-variant">
                    {{ row.kind === 'folder' ? t('files.folders') : mimeLabel(row.file) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column width="145" :label="t('files.tableCreated')">
                <template #default="{ row }">{{ formatDate(row.kind === 'folder' ? row.folder.gmtCreate : row.file.gmtCreate) }}</template>
              </el-table-column>
              <el-table-column fixed="right" width="230" :label="t('files.tableActions')">
                <template #default="{ row }">
                  <div class="file-actions" @click.stop>
                    <template v-if="row.kind === 'folder'">
                      <button class="file-icon-action primary" type="button" :title="t('files.openFolder')" @click="selectFolder(row.folder.id)">
                        <span class="material-symbols-outlined">folder_open</span>
                      </button>
                      <button class="file-icon-action" type="button" :title="t('files.rename')" @click="openRenameFolderDialog(row.folder)">
                        <span class="material-symbols-outlined">edit</span>
                      </button>
                      <button class="file-icon-action danger" type="button" :title="t('files.physicalDelete')" @click="confirmDeleteFolder(row.folder)">
                        <span class="material-symbols-outlined">delete</span>
                      </button>
                    </template>

                    <template v-else-if="!query.trashed">
                      <button class="file-icon-action primary" type="button" :title="t('files.preview')" @click="openFile(row.file)">
                        <span class="material-symbols-outlined">visibility</span>
                      </button>
                      <button class="file-icon-action" type="button" :title="t('files.download')" @click="downloadFile(row.file)">
                        <span class="material-symbols-outlined">download</span>
                      </button>
                      <button class="file-icon-action" type="button" :title="t('files.rename')" @click="openRenameFileDialog(row.file)">
                        <span class="material-symbols-outlined">edit</span>
                      </button>
                      <button class="file-icon-action" type="button" :title="t('files.move')" @click="openMoveFileDialog(row.file)">
                        <span class="material-symbols-outlined">drive_file_move</span>
                      </button>
                    </template>

                    <template v-else>
                      <button class="file-icon-action primary" type="button" :title="t('files.restore')" @click="confirmRestore(row.file)">
                        <span class="material-symbols-outlined">restore_from_trash</span>
                      </button>
                      <button class="file-icon-action danger" type="button" :title="t('files.physicalDelete')" @click="confirmPhysicalDelete(row.file)">
                        <span class="material-symbols-outlined">delete_forever</span>
                      </button>
                    </template>

                    <el-dropdown v-if="row.kind === 'file'" trigger="click">
                      <button class="file-icon-action" type="button">
                        <span class="material-symbols-outlined">more_horiz</span>
                      </button>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item @click="openReferences(row.file)">{{ t('files.references') }}</el-dropdown-item>
                          <el-dropdown-item @click="openOperationLogs(row.file)">{{ t('files.logs') }}</el-dropdown-item>
                          <el-dropdown-item v-if="!query.trashed" divided @click="confirmTrash(row.file)">
                            {{ t('files.trash') }}
                          </el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </div>
                </template>
              </el-table-column>
            </el-table>

            <EmptyState
              v-if="!loading && !directoryEntries.length"
              class="mt-5"
              :title="t('files.emptyTitle')"
              :description="t('files.emptyDescription')"
              icon="draft"
            />

            <div v-if="page.total > 0" class="mt-4 flex justify-end">
              <el-pagination
                v-model:current-page="query.pageNum"
                v-model:page-size="query.pageSize"
                background
                layout="sizes, prev, pager, next, total"
                :page-sizes="[10, 20, 50, 100]"
                :total="page.total"
                @current-change="loadFiles"
                @size-change="reloadFromFirstPage"
              />
            </div>
          </section>
        </div>
      </div>
    </template>

    <FileUploadSettingsDialog
      v-model="uploadSettingsDialogOpen"
      v-model:chunk-size-mb="uploadSettingsForm.chunkSizeMb"
      v-model:max-file-size-mb="uploadSettingsForm.maxFileSizeMb"
      v-model:multipart-threshold-mb="uploadSettingsForm.multipartThresholdMb"
      @save="submitUploadSettings"
    />

    <FileMoveDialog
      v-model="moveDialogOpen"
      v-model:target-folder-id="moveTargetFolderId"
      :folders="flatFolders"
      @confirm="submitMoveFile"
    />

    <FileReferencesDialog v-model="referencesDialogOpen" :references="references" />

    <FileOperationLogsDialog v-model="logsDialogOpen" :logs="operationLogs" />
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import EmptyState from '@/components/common/EmptyState.vue'
import FileMoveDialog from '@/components/files/FileMoveDialog.vue'
import FileOperationLogsDialog from '@/components/files/FileOperationLogsDialog.vue'
import FileReferencesDialog from '@/components/files/FileReferencesDialog.vue'
import FileUploadSettingsDialog from '@/components/files/FileUploadSettingsDialog.vue'
import { fileService } from '@/api/services'
import { SPACE_FILE_CHANGED_EVENT } from '@/composables/useSpaceRealtimeEvents'
import { useI18n } from '@/i18n'
import { useSpaceStore } from '@/stores/space'
import { useUserStore } from '@/stores/user'
import { formatDateTime } from '@/utils/date'
import type {
  FileFolder,
  FileOperationLog,
  FileReference,
  FileStats,
  FileUploadConfig,
  ManagedFile,
  ManagedFileQuery,
  PageResponse,
  SpaceRealtimeEvent,
} from '@/types/app'

interface FolderRow extends FileFolder {
  depth: number
}

type SidebarFolderRow = {
  kind: 'folder'
  id: number
  depth: number
  label: string
  folder: FileFolder
  expandable: boolean
}

type SidebarFileRow = {
  kind: 'file'
  id: number
  depth: number
  label: string
  file: ManagedFile
}

type SidebarRow = SidebarFolderRow | SidebarFileRow
type DraggingItem = { kind: 'folder' | 'file'; id: number } | null
type DirectoryFolderEntry = { kind: 'folder'; key: string; folder: FileFolder }
type DirectoryFileEntry = { kind: 'file'; key: string; file: ManagedFile }
type DirectoryEntry = DirectoryFolderEntry | DirectoryFileEntry

const TREE_FILE_LIMIT = 100
const BYTES_PER_MB = 1024 * 1024
const DEFAULT_UPLOAD_CONFIG: FileUploadConfig = {
  maxFileSize: 500 * 1024 * 1024,
  multipartThresholdSize: 50 * 1024 * 1024,
  chunkSize: 5 * 1024 * 1024,
  allowedMimeTypes: [],
}

const emptyPage = <T,>(): PageResponse<T> => ({
  total: 0,
  pageNum: 1,
  pageSize: 10,
  list: [],
})

const emptyStats = (): FileStats => ({
  totalCount: 0,
  totalSize: 0,
  trashCount: 0,
})

const { t } = useI18n()
const router = useRouter()
const spaceStore = useSpaceStore()
const userStore = useUserStore()

const loading = ref(false)
const folderTree = ref<FileFolder[]>([])
const treeFiles = ref<ManagedFile[]>([])
const currentFolderId = ref(0)
const page = ref<PageResponse<ManagedFile>>(emptyPage())
const stats = ref<FileStats>(emptyStats())
const fileInputRef = ref<HTMLInputElement | null>(null)
const folderInputRef = ref<HTMLInputElement | null>(null)
const moveDialogOpen = ref(false)
const moveFile = ref<ManagedFile | null>(null)
const moveTargetFolderId = ref(0)
const referencesDialogOpen = ref(false)
const references = ref<FileReference[]>([])
const logsDialogOpen = ref(false)
const operationLogs = ref<PageResponse<FileOperationLog>>(emptyPage())
const collapsedFolderIds = ref<number[]>([])
const draggingItem = ref<DraggingItem>(null)
const searchTimer = ref<number | undefined>()
const realtimeReloadTimer = ref<number | undefined>()
const uploadConfig = ref<FileUploadConfig>(DEFAULT_UPLOAD_CONFIG)
const uploadSettingsDialogOpen = ref(false)
const creatingFolder = ref(false)
const newFolderName = ref('')
const uploadSettingsForm = reactive({
  maxFileSizeMb: 500,
  multipartThresholdMb: 50,
  chunkSizeMb: 5,
})

const query = reactive<ManagedFileQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  trashed: false,
})

const currentSpaceName = computed(() => spaceStore.currentSpace?.name || t('spaceSwitcher.fallbackSpaceName'))
const currentMember = computed(() =>
  spaceStore.members.find((member) => member.userId === userStore.profile?.id) || null,
)
const uploadSettingRoleCodes = ['SPACE_OWNER', 'SPACE_ADMIN']
const fileManagerRoleCodes = ['SPACE_OWNER', 'SPACE_ADMIN', 'SPACE_MEMBER']
const canManageFiles = computed(() => {
  if (!spaceStore.currentSpaceId) {
    return false
  }

  if (spaceStore.currentSpace?.type !== 'TEAM') {
    return true
  }

  return fileManagerRoleCodes.includes(currentMember.value?.roleCode || '')
})
const canConfigureUploadSettings = computed(() => {
  if (!spaceStore.currentSpaceId) {
    return false
  }

  if (spaceStore.currentSpace?.type !== 'TEAM') {
    return true
  }

  return uploadSettingRoleCodes.includes(currentMember.value?.roleCode || '')
})
const flatFolders = computed(() => flattenFolders(folderTree.value))
const currentFolder = computed(() => (currentFolderId.value ? findFolderById(folderTree.value, currentFolderId.value) : null))
const hasGlobalFileSearch = computed(() => Boolean(query.keyword?.trim() || query.mimeType))
const showParentFolderEntry = computed(() => Boolean(currentFolderId.value && !query.trashed && !hasGlobalFileSearch.value))
const currentChildFolders = computed(() => {
  if (query.trashed || hasGlobalFileSearch.value) {
    return []
  }
  return currentFolderId.value ? currentFolder.value?.children || [] : folderTree.value
})
const directoryEntries = computed<DirectoryEntry[]>(() => [
  ...currentChildFolders.value.map((folder) => ({
    kind: 'folder' as const,
    key: `folder-${folder.id}`,
    folder,
  })),
  ...page.value.list.map((file) => ({
    kind: 'file' as const,
    key: `file-${file.id}`,
    file,
  })),
])
const fileBucketMap = computed(() => {
  const nextMap = new Map<number, ManagedFile[]>()
  for (const file of treeFiles.value) {
    const folderId = file.folderId || 0
    const bucket = nextMap.get(folderId) || []
    bucket.push(file)
    nextMap.set(folderId, bucket)
  }
  return nextMap
})
const sidebarRows = computed<SidebarRow[]>(() => [
  ...fileRowsForFolder(0, 0),
  ...buildSidebarRows(folderTree.value, 0),
])
const currentFolderName = computed(() => {
  if (!currentFolderId.value) {
    return t('files.allFiles')
  }

  return flatFolders.value.find((folder) => folder.id === currentFolderId.value)?.name || t('files.rootFolder')
})
const newFolderInputPadding = computed(() => {
  if (!currentFolderId.value) {
    return 10
  }

  const parentDepth = flatFolders.value.find((folder) => folder.id === currentFolderId.value)?.depth ?? 0
  return 10 + (parentDepth + 1) * 18
})
const uploadAccept = computed(() =>
  uploadConfig.value.allowedMimeTypes
    .filter((mimeType) => mimeType !== 'application/octet-stream')
    .join(','),
)

const bytesToMb = (value?: number) => Math.max(1, Math.round((value || BYTES_PER_MB) / BYTES_PER_MB))
const mbToBytes = (value: number) => Math.max(1, value) * BYTES_PER_MB

const flattenFolders = (folders: FileFolder[], depth = 0): FolderRow[] =>
  folders.flatMap((folder) => [
    {
      ...folder,
      depth,
    },
    ...flattenFolders(folder.children || [], depth + 1),
  ])

const isFolderCollapsed = (folderId: number) => collapsedFolderIds.value.includes(folderId)

const fileRowsForFolder = (folderId: number, depth: number): SidebarFileRow[] =>
  (fileBucketMap.value.get(folderId) || []).map((file) => ({
    kind: 'file',
    id: file.id,
    depth,
    label: file.displayName || file.fileName || `#${file.id}`,
    file,
  }))

const buildSidebarRows = (folders: FileFolder[], depth = 0): SidebarRow[] =>
  folders.flatMap((folder) => {
    const childFolders = folder.children || []
    const childFiles = fileRowsForFolder(folder.id, depth + 1)
    const expandable = Boolean(childFolders.length || childFiles.length)
    const folderRow: SidebarFolderRow = {
      kind: 'folder',
      id: folder.id,
      depth,
      label: folder.name,
      folder,
      expandable,
    }

    if (expandable && isFolderCollapsed(folder.id)) {
      return [folderRow]
    }

    return [folderRow, ...buildSidebarRows(childFolders, depth + 1), ...childFiles]
  })

const findFolderById = (folders: FileFolder[], folderId: number): FileFolder | null => {
  for (const folder of folders) {
    if (folder.id === folderId) {
      return folder
    }
    const child = findFolderById(folder.children || [], folderId)
    if (child) {
      return child
    }
  }
  return null
}

const containsDescendant = (folder: FileFolder | null, targetId: number): boolean => {
  if (!folder?.children?.length) {
    return false
  }
  return folder.children.some((child) => child.id === targetId || containsDescendant(child, targetId))
}

const treeRowClasses = (row: SidebarRow) => {
  if (row.kind === 'folder') {
    return currentFolderId.value === row.folder.id ? 'file-tree-row-active' : ''
  }

  return ''
}

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

const fileExtension = (file: ManagedFile) => {
  const name = file.fileName || file.displayName || ''
  const chunks = name.toLowerCase().split('.')
  return chunks.length > 1 ? chunks.pop() || '' : ''
}

const mimeLabel = (file: ManagedFile) => {
  const mimeType = file.mimeType || ''
  const extension = fileExtension(file)
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
  if (mimeType.startsWith('text/') || ['txt', 'md', 'json', 'csv'].includes(extension)) {
    return 'Text'
  }
  return mimeType.split('/').pop() || t('common.unknown')
}

const fileIcon = (file: ManagedFile) => {
  const type = mimeLabel(file)
  if (type === 'Image') {
    return 'image'
  }
  if (type === 'PDF') {
    return 'picture_as_pdf'
  }
  if (type === 'Excel') {
    return 'table_chart'
  }
  if (type === 'Word') {
    return 'article'
  }
  if (type === 'Video') {
    return 'movie'
  }
  if (type === 'Audio') {
    return 'graphic_eq'
  }
  if (type === 'Archive') {
    return 'folder_zip'
  }
  return 'draft'
}

const entryIcon = (entry: DirectoryEntry) => (entry.kind === 'folder' ? 'folder' : fileIcon(entry.file))
const entryName = (entry: DirectoryEntry) =>
  entry.kind === 'folder' ? entry.folder.name : entry.file.displayName || entry.file.fileName || `#${entry.file.id}`
const entryDescription = (entry: DirectoryEntry) => {
  if (entry.kind === 'folder') {
    const childCount = entry.folder.children?.length || 0
    return childCount ? `${childCount} ${t('files.folders')}` : t('files.folders')
  }
  return entry.file.fileName || entry.file.mimeType || '-'
}

const detectMimeType = (file: File) => {
  const extension = file.name.toLowerCase().split('.').pop() || ''
  const extensionMap: Record<string, string> = {
    md: 'text/markdown',
    markdown: 'text/markdown',
    zip: 'application/zip',
    mp3: 'audio/mpeg',
    wav: 'audio/wav',
    flac: 'audio/flac',
    m4a: 'audio/mp4',
    mp4: 'video/mp4',
    mov: 'video/quicktime',
    webm: 'video/webm',
  }
  return file.type || extensionMap[extension] || 'application/octet-stream'
}

const uploadByChunks = async (file: File) => {
  if (!spaceStore.currentSpaceId) {
    return
  }
  const mimeType = detectMimeType(file)
  const session = await fileService.initChunkUpload(spaceStore.currentSpaceId, {
    fileName: file.name,
    fileSize: file.size,
    mimeType,
    folderId: currentFolderId.value,
  })
  const chunkSize = session.chunkSize || uploadConfig.value.chunkSize
  for (let index = 0; index < session.totalChunks; index++) {
    const start = index * chunkSize
    const chunk = file.slice(start, Math.min(file.size, start + chunkSize), mimeType)
    await fileService.uploadChunk(spaceStore.currentSpaceId, session.uploadToken, index, chunk)
  }
  await fileService.completeChunkUpload(spaceStore.currentSpaceId, {
    uploadToken: session.uploadToken,
    displayName: file.name,
  })
}

const loadFolders = async () => {
  if (!spaceStore.currentSpaceId || !canManageFiles.value) {
    folderTree.value = []
    return
  }

  folderTree.value = await fileService.tree(spaceStore.currentSpaceId)
}

const loadTreeFiles = async () => {
  if (!spaceStore.currentSpaceId || !canManageFiles.value) {
    treeFiles.value = []
    return
  }

  const response = await fileService.page(spaceStore.currentSpaceId, {
    pageNum: 1,
    pageSize: TREE_FILE_LIMIT,
    trashed: false,
  })
  treeFiles.value = response.list
}

const loadTreeData = async () => {
  await Promise.all([loadFolders(), loadTreeFiles()])
}

const loadStats = async () => {
  if (!spaceStore.currentSpaceId || !canManageFiles.value) {
    stats.value = emptyStats()
    return
  }

  stats.value = await fileService.stats(spaceStore.currentSpaceId)
}

const loadUploadConfig = async () => {
  if (!spaceStore.currentSpaceId || !canManageFiles.value) {
    uploadConfig.value = DEFAULT_UPLOAD_CONFIG
    return
  }

  uploadConfig.value = await fileService.uploadConfig(spaceStore.currentSpaceId)
}

const loadFiles = async () => {
  if (!spaceStore.currentSpaceId || !canManageFiles.value) {
    page.value = emptyPage()
    return
  }

  loading.value = true
  try {
    page.value = await fileService.page(spaceStore.currentSpaceId, {
      ...query,
      keyword: query.keyword?.trim() || undefined,
      folderId: query.trashed || hasGlobalFileSearch.value ? undefined : currentFolderId.value,
    })
  } finally {
    loading.value = false
  }
}

const reloadFromFirstPage = async () => {
  query.pageNum = 1
  await loadFiles()
}

const loadAll = async () => {
  await Promise.all([loadTreeData(), loadStats(), loadUploadConfig(), loadFiles()])
}

const selectFolder = async (folderId = 0) => {
  currentFolderId.value = folderId
  await reloadFromFirstPage()
}

const goParentFolder = async () => {
  await selectFolder(currentFolder.value?.parentId || 0)
}

const toggleTrashMode = async () => {
  query.trashed = !query.trashed
  await reloadFromFirstPage()
}

const openUploadSettingsDialog = () => {
  if (!canConfigureUploadSettings.value) {
    return
  }

  uploadSettingsForm.maxFileSizeMb = bytesToMb(uploadConfig.value.maxFileSize)
  uploadSettingsForm.multipartThresholdMb = bytesToMb(uploadConfig.value.multipartThresholdSize)
  uploadSettingsForm.chunkSizeMb = bytesToMb(uploadConfig.value.chunkSize)
  uploadSettingsDialogOpen.value = true
}

const submitUploadSettings = async () => {
  if (!spaceStore.currentSpaceId || !canConfigureUploadSettings.value) {
    return
  }
  uploadConfig.value = await fileService.updateUploadConfig(spaceStore.currentSpaceId, {
    maxFileSize: mbToBytes(uploadSettingsForm.maxFileSizeMb),
    multipartThresholdSize: mbToBytes(uploadSettingsForm.multipartThresholdMb),
    chunkSize: mbToBytes(uploadSettingsForm.chunkSizeMb),
    allowedMimeTypes: uploadConfig.value.allowedMimeTypes,
  })
  ElMessage.success(t('files.uploadSettingsSaved'))
  uploadSettingsDialogOpen.value = false
}

const toggleFolder = (folderId: number) => {
  if (isFolderCollapsed(folderId)) {
    collapsedFolderIds.value = collapsedFolderIds.value.filter((id) => id !== folderId)
    return
  }
  collapsedFolderIds.value = [...collapsedFolderIds.value, folderId]
}

const triggerUpload = () => {
  fileInputRef.value?.click()
}

const handleFileChange = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const selectedFile = input.files?.[0]
  if (!selectedFile || !spaceStore.currentSpaceId) {
    return
  }

  try {
    if (selectedFile.size > uploadConfig.value.maxFileSize) {
      ElMessage.warning(`${t('files.fileTooLarge')} ${formatBytes(uploadConfig.value.maxFileSize)}`)
      return
    }

    if (selectedFile.size > uploadConfig.value.multipartThresholdSize) {
      await uploadByChunks(selectedFile)
    } else {
      await fileService.upload(spaceStore.currentSpaceId, {
        file: selectedFile,
        folderId: currentFolderId.value,
      })
    }
    ElMessage.success(t('files.uploaded'))
    await loadAll()
  } finally {
    input.value = ''
  }
}

const startTreeDrag = (event: DragEvent, row: SidebarRow) => {
  draggingItem.value = { kind: row.kind, id: row.id }
  event.dataTransfer?.setData('text/plain', `${row.kind}:${row.id}`)
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
  }
}

const parseDraggingItem = (event: DragEvent): DraggingItem => {
  if (draggingItem.value) {
    return draggingItem.value
  }
  const [kind, rawId] = (event.dataTransfer?.getData('text/plain') || '').split(':')
  const id = Number(rawId)
  if ((kind === 'folder' || kind === 'file') && !Number.isNaN(id)) {
    draggingItem.value = { kind, id }
    return draggingItem.value
  }
  return null
}

const resetDragState = () => {
  draggingItem.value = null
}

const canDropOnFolder = (targetFolderId: number, item: DraggingItem) => {
  if (!item) {
    return false
  }
  if (item.kind === 'file') {
    return true
  }
  if (item.id === targetFolderId) {
    return false
  }
  return !containsDescendant(findFolderById(folderTree.value, item.id), targetFolderId)
}

const handleFolderDragOver = (event: DragEvent, targetFolderId: number) => {
  if (!canDropOnFolder(targetFolderId, parseDraggingItem(event))) {
    return
  }
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
}

const handleRootDragOver = (event: DragEvent) => {
  if (!parseDraggingItem(event)) {
    return
  }
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
}

const dropOnFolder = async (event: DragEvent, targetFolderId: number) => {
  event.preventDefault()
  const item = parseDraggingItem(event)
  if (!item || !spaceStore.currentSpaceId || !canDropOnFolder(targetFolderId, item)) {
    resetDragState()
    return
  }

  if (item.kind === 'file') {
    await fileService.updateFile(spaceStore.currentSpaceId, item.id, { folderId: targetFolderId })
    ElMessage.success(t('files.fileMoved'))
  } else {
    const folder = findFolderById(folderTree.value, item.id)
    if (folder) {
      await fileService.updateFolder(spaceStore.currentSpaceId, folder.id, {
        name: folder.name,
        parentId: targetFolderId,
      })
      ElMessage.success(t('files.folderUpdated'))
    }
  }
  resetDragState()
  await loadAll()
}

const dropToRoot = async (event: DragEvent) => {
  event.preventDefault()
  const item = parseDraggingItem(event)
  if (!item || !spaceStore.currentSpaceId) {
    resetDragState()
    return
  }

  if (item.kind === 'file') {
    await fileService.updateFile(spaceStore.currentSpaceId, item.id, { folderId: 0 })
    ElMessage.success(t('files.fileMoved'))
  } else {
    const folder = findFolderById(folderTree.value, item.id)
    if (folder) {
      await fileService.updateFolder(spaceStore.currentSpaceId, folder.id, {
        name: folder.name,
        parentId: 0,
      })
      ElMessage.success(t('files.folderUpdated'))
    }
  }
  resetDragState()
  await loadAll()
}

const handleTreeRowClick = async (row: SidebarRow) => {
  if (row.kind === 'folder') {
    await selectFolder(row.folder.id)
    return
  }
  await locateFileInDirectory(row.file)
}

const locateFileInDirectory = async (file: ManagedFile) => {
  currentFolderId.value = file.folderId || 0
  query.trashed = false
  query.pageNum = 1
  await loadFiles()
}

const startCreateFolder = async () => {
  if (query.trashed) {
    query.trashed = false
    await reloadFromFirstPage()
  }

  creatingFolder.value = true
  newFolderName.value = ''
  if (currentFolderId.value) {
    collapsedFolderIds.value = collapsedFolderIds.value.filter((id) => id !== currentFolderId.value)
  }
  await nextTick()
  folderInputRef.value?.focus()
}

const cancelCreateFolder = () => {
  creatingFolder.value = false
  newFolderName.value = ''
}

const submitCreateFolder = async () => {
  if (!creatingFolder.value) {
    return
  }

  const name = newFolderName.value.trim()
  if (!name || !spaceStore.currentSpaceId) {
    cancelCreateFolder()
    return
  }

  const parentId = currentFolderId.value || 0
  cancelCreateFolder()
  await fileService.createFolder(spaceStore.currentSpaceId, {
    name,
    parentId,
  })
  ElMessage.success(t('files.folderCreated'))
  await loadAll()
}

const handleFolderInputBlur = () => {
  submitCreateFolder().catch(() => undefined)
}

const openRenameFolderDialog = async (folder: FileFolder) => {
  const result = await ElMessageBox.prompt(t('files.folderNamePlaceholder'), t('files.renameFolderTitle'), {
    inputValue: folder.name,
    inputPlaceholder: t('files.folderNamePlaceholder'),
    confirmButtonText: t('common.confirm'),
    cancelButtonText: t('common.cancel'),
  }).catch(() => null)

  const name = result?.value?.trim()
  if (!name || !spaceStore.currentSpaceId) {
    return
  }

  await fileService.updateFolder(spaceStore.currentSpaceId, folder.id, {
    name,
    parentId: folder.parentId || 0,
  })
  ElMessage.success(t('files.folderUpdated'))
  await loadTreeData()
}

const confirmDeleteFolder = async (folder: FileFolder) => {
  const confirmed = await ElMessageBox.confirm(
    t('files.confirmDeleteFolder', { name: folder.name }),
    t('files.physicalDelete'),
    {
      confirmButtonText: t('common.delete'),
      cancelButtonText: t('common.cancel'),
      type: 'warning',
    },
  ).catch(() => false)

  if (!confirmed || !spaceStore.currentSpaceId) {
    return
  }

  await fileService.deleteFolder(spaceStore.currentSpaceId, folder.id)
  ElMessage.success(t('files.folderDeleted'))
  if (currentFolderId.value === folder.id) {
    currentFolderId.value = 0
  }
  await loadAll()
}

const openRenameFileDialog = async (file: ManagedFile) => {
  const result = await ElMessageBox.prompt(t('files.rename'), t('files.renameFileTitle'), {
    inputValue: file.displayName || file.fileName,
    confirmButtonText: t('common.confirm'),
    cancelButtonText: t('common.cancel'),
  }).catch(() => null)

  const displayName = result?.value?.trim()
  if (!displayName || !spaceStore.currentSpaceId) {
    return
  }

  await fileService.updateFile(spaceStore.currentSpaceId, file.id, { displayName })
  ElMessage.success(t('files.fileUpdated'))
  await loadAll()
}

const openMoveFileDialog = (file: ManagedFile) => {
  moveFile.value = file
  moveTargetFolderId.value = file.folderId || 0
  moveDialogOpen.value = true
}

const submitMoveFile = async () => {
  if (!moveFile.value || !spaceStore.currentSpaceId) {
    return
  }

  await fileService.updateFile(spaceStore.currentSpaceId, moveFile.value.id, {
    folderId: moveTargetFolderId.value,
  })
  ElMessage.success(t('files.fileMoved'))
  moveDialogOpen.value = false
  await loadAll()
}

const openFile = async (file: ManagedFile) => {
  await router.push({
    name: 'file-preview',
    params: {
      fileId: file.id,
    },
  })
}

const handleEntryDoubleClick = async (entry: DirectoryEntry) => {
  if (entry.kind === 'folder') {
    await selectFolder(entry.folder.id)
    return
  }
  await openFile(entry.file)
}

const openFileUrl = (url?: string) => {
  if (!url) {
    ElMessage.warning(t('files.openFailed'))
    return
  }

  window.open(url, '_blank', 'noopener,noreferrer')
}

const downloadFile = async (file: ManagedFile) => {
  if (!spaceStore.currentSpaceId) {
    return
  }

  const nextFile = await fileService.downloadUrl(spaceStore.currentSpaceId, file.id)
  openFileUrl(nextFile.downloadUrl)
}

const confirmTrash = async (file: ManagedFile) => {
  const name = file.displayName || file.fileName || `#${file.id}`
  const confirmed = await ElMessageBox.confirm(t('files.confirmTrash', { name }), t('files.trash'), {
    confirmButtonText: t('common.confirm'),
    cancelButtonText: t('common.cancel'),
    type: 'warning',
  }).catch(() => false)

  if (!confirmed || !spaceStore.currentSpaceId) {
    return
  }

  await fileService.trash(spaceStore.currentSpaceId, file.id)
  ElMessage.success(t('files.fileTrashed'))
  await loadAll()
}

const confirmRestore = async (file: ManagedFile) => {
  const name = file.displayName || file.fileName || `#${file.id}`
  const confirmed = await ElMessageBox.confirm(t('files.confirmRestore', { name }), t('files.restore'), {
    confirmButtonText: t('common.confirm'),
    cancelButtonText: t('common.cancel'),
    type: 'warning',
  }).catch(() => false)

  if (!confirmed || !spaceStore.currentSpaceId) {
    return
  }

  await fileService.restore(spaceStore.currentSpaceId, file.id)
  ElMessage.success(t('files.fileRestored'))
  await loadAll()
}

const confirmPhysicalDelete = async (file: ManagedFile) => {
  const name = file.displayName || file.fileName || `#${file.id}`
  const confirmed = await ElMessageBox.confirm(t('files.confirmPhysicalDelete', { name }), t('files.physicalDelete'), {
    confirmButtonText: t('common.delete'),
    cancelButtonText: t('common.cancel'),
    type: 'error',
  }).catch(() => false)

  if (!confirmed || !spaceStore.currentSpaceId) {
    return
  }

  await fileService.physicalDelete(spaceStore.currentSpaceId, file.id, true)
  ElMessage.success(t('files.fileDeleted'))
  await loadAll()
}

const openReferences = async (file: ManagedFile) => {
  if (!spaceStore.currentSpaceId) {
    return
  }

  references.value = await fileService.references(spaceStore.currentSpaceId, file.id)
  referencesDialogOpen.value = true
}

const openOperationLogs = async (file: ManagedFile) => {
  if (!spaceStore.currentSpaceId) {
    return
  }

  operationLogs.value = await fileService.operationLogs(spaceStore.currentSpaceId, file.id)
  logsDialogOpen.value = true
}

const scheduleRealtimeReload = () => {
  if (realtimeReloadTimer.value) {
    window.clearTimeout(realtimeReloadTimer.value)
  }

  realtimeReloadTimer.value = window.setTimeout(() => {
    realtimeReloadTimer.value = undefined
    loadAll().catch(() => undefined)
  }, 260)
}

const handleSpaceFileChanged: EventListener = (event) => {
  const spaceEvent = (event as CustomEvent<SpaceRealtimeEvent>).detail
  if (!spaceEvent || spaceEvent.spaceId !== spaceStore.currentSpaceId || !canManageFiles.value) {
    return
  }

  const deletedFolderId = Number(spaceEvent.payload?.folderId || 0)
  if (spaceEvent.type === 'FILE_TREE_CHANGED' && spaceEvent.payload?.action === 'deleted' && deletedFolderId) {
    if (currentFolderId.value === deletedFolderId) {
      currentFolderId.value = 0
    }
  }

  scheduleRealtimeReload()
}

watch(
  () => ({
    spaceId: spaceStore.currentSpaceId,
    canManage: canManageFiles.value,
  }),
  () => {
    currentFolderId.value = 0
    query.pageNum = 1
    query.trashed = false
    loadAll().catch(() => undefined)
  },
  { immediate: true },
)

watch(
  () => query.keyword,
  () => {
    if (searchTimer.value) {
      window.clearTimeout(searchTimer.value)
    }
    searchTimer.value = window.setTimeout(() => {
      reloadFromFirstPage().catch(() => undefined)
    }, 280)
  },
)

onMounted(() => {
  window.addEventListener(SPACE_FILE_CHANGED_EVENT, handleSpaceFileChanged)
  if (spaceStore.currentSpace?.type === 'TEAM' && !spaceStore.members.length && spaceStore.currentSpaceId) {
    spaceStore.loadMembers(spaceStore.currentSpaceId).catch(() => undefined)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener(SPACE_FILE_CHANGED_EVENT, handleSpaceFileChanged)
  if (searchTimer.value) {
    window.clearTimeout(searchTimer.value)
  }
  if (realtimeReloadTimer.value) {
    window.clearTimeout(realtimeReloadTimer.value)
  }
})
</script>

<style scoped>
.file-stat-card {
  border: 1px solid var(--outline-variant);
  border-radius: 1.35rem;
  background:
    radial-gradient(circle at top left, rgba(255, 138, 101, 0.16), transparent 40%),
    var(--surface-container-lowest);
  padding: 1rem;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.04);
}

.file-filter-input {
  align-items: center;
  background: var(--surface-container-low);
  border: 1px solid var(--outline-variant);
  border-radius: 9999px;
  display: flex;
  gap: 0.6rem;
  height: 44px;
  padding: 0 0.9rem;
  transition: all 0.2s ease;
  width: clamp(240px, 32vw, 440px);
}

.file-filter-input:focus-within {
  background: var(--surface-container-lowest);
  border-color: var(--primary);
}

.file-filter-select {
  width: clamp(180px, 18vw, 220px);
}

.file-filter-select :deep(.el-select__wrapper) {
  align-items: center;
  background: var(--surface-container-low);
  border: 1px solid var(--outline-variant);
  border-radius: 9999px;
  box-shadow: none;
  min-height: 44px;
  padding: 0 0.9rem;
  transition: all 0.2s ease;
}

.file-filter-select :deep(.el-select__wrapper.is-focused) {
  background: var(--surface-container-lowest);
  border-color: var(--primary);
  box-shadow: none;
}

.parent-folder-row {
  align-items: center;
  background: var(--surface-container-low);
  border: 1px dashed var(--outline-variant);
  border-radius: 1.15rem;
  color: var(--on-surface-variant);
  display: flex;
  gap: 0.75rem;
  min-height: 52px;
  padding: 0 1rem;
  text-align: left;
  transition: all 0.2s ease;
}

.parent-folder-row:hover {
  background: var(--surface-container-lowest);
  border-color: var(--primary);
  color: var(--primary);
}

.file-tree-row {
  align-items: center;
  border-radius: 1rem;
  color: var(--on-surface-variant);
  display: flex;
  gap: 0.4rem;
  min-height: 36px;
  padding: 0.35rem 0.5rem;
  text-align: left;
  transition: all 0.18s ease;
}

.file-tree-row:hover {
  background: rgba(255, 255, 255, 0.7);
  color: var(--primary);
}

.file-tree-row-active {
  background: #fff;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.04);
  color: var(--primary);
}

.file-tree-create-row {
  align-items: center;
  background: rgba(255, 255, 255, 0.74);
  border: 1px dashed rgba(159, 65, 34, 0.26);
  border-radius: 1rem;
  color: var(--primary);
  display: flex;
  gap: 0.5rem;
  min-height: 38px;
  padding-bottom: 0.35rem;
  padding-right: 0.65rem;
  padding-top: 0.35rem;
}

.file-tree-action {
  align-items: center;
  border-radius: 9999px;
  color: #9a8b85;
  display: none;
  height: 28px;
  justify-content: center;
  width: 28px;
}

.group:hover .file-tree-action {
  display: flex;
}

.file-tree-action:hover {
  background: var(--surface-container);
  color: var(--primary);
}

.file-tree-action.danger:hover {
  color: var(--error);
}

.file-actions {
  display: flex;
  gap: 0.35rem;
}

.file-icon-action {
  align-items: center;
  background: var(--surface-container-low);
  border-radius: 9999px;
  color: var(--on-surface-variant);
  display: inline-flex;
  height: 34px;
  justify-content: center;
  width: 34px;
}

.file-icon-action .material-symbols-outlined {
  font-size: 18px;
}

.file-icon-action:hover {
  background: var(--surface-container);
  color: var(--primary);
}

.file-icon-action.primary {
  background: var(--primary-fixed);
  color: var(--primary);
}

.file-icon-action.danger:hover {
  background: var(--error-container);
  color: var(--error);
}

</style>
