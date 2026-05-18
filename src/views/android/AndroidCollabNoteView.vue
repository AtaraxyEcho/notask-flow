<template>
  <main class="android-collab-page">
    <section v-if="loading" class="android-collab-state">
      <span class="material-symbols-outlined">sync</span>
      <strong>正在打开协作文档</strong>
    </section>

    <section v-else-if="errorMessage" class="android-collab-state">
      <span class="material-symbols-outlined">error</span>
      <strong>协作文档暂时无法打开</strong>
      <p>{{ errorMessage }}</p>
    </section>

    <template v-else-if="note">
      <header class="android-collab-header">
        <div>
          <p>团队协作</p>
          <h1>{{ note.title || '未命名文档' }}</h1>
        </div>
        <span :class="['android-collab-status', statusClass]">{{ statusMessage }}</span>
      </header>

      <CollaborativeTipTapEditor
        v-model="content"
        v-model:html-value="contentHtml"
        class="android-collab-editor"
        :can-edit="canEditNote"
        :note-id="note.id"
        :space-id="note.spaceId"
        placeholder="开始协作编辑"
        @persisted="handlePersisted"
        @status-change="handleStatusChange"
      />
    </template>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { noteService } from '@/api/services'
import CollaborativeTipTapEditor from '@/components/team/CollaborativeTipTapEditor.vue'
import { useUserStore } from '@/stores/user'
import type { Note } from '@/types/app'

type AndroidCollabBootstrapState = {
  apiBaseUrl?: string
  collabWsUrl?: string
  currentSpaceId?: number
  token?: string
}

const route = useRoute()
const userStore = useUserStore()
const loading = ref(true)
const errorMessage = ref('')
const note = ref<Note | null>(null)
const content = ref('')
const contentHtml = ref('')
const statusMessage = ref('正在连接')
const connectionStatus = ref('connecting')
const bootstrapState = ref<AndroidCollabBootstrapState | null>(null)

const statusClass = computed(() => {
  if (connectionStatus.value === 'error' || connectionStatus.value === 'disconnected') {
    return 'is-error'
  }
  if (connectionStatus.value === 'synced' || connectionStatus.value === 'connected') {
    return 'is-ready'
  }
  return 'is-loading'
})
const canEditNote = computed(() => note.value?.canEdit !== false)

const resolveNumericParam = (value: unknown) => {
  const raw = Array.isArray(value) ? value[0] : value
  const parsed = Number(raw)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null
}

const readJsonStorage = (key: string) => {
  const raw = window.sessionStorage.getItem(key) || window.localStorage.getItem(key)
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw) as Record<string, unknown>
  } catch {
    return null
  }
}

const readAndroidHashState = () => {
  const rawHash = window.location.hash.replace(/^#/, '')
  if (!rawHash) {
    return null
  }

  const rawState = new URLSearchParams(rawHash).get('androidState')
  if (!rawState) {
    return null
  }

  try {
    return JSON.parse(rawState) as AndroidCollabBootstrapState
  } catch {
    return null
  }
}

const persistAndroidHashState = (state: AndroidCollabBootstrapState) => {
  const token = state.token?.trim()
  const currentSpaceId = Number(state.currentSpaceId)
  const userState = {
    tokenName: 'Authorization',
    tokenValue: token || '',
    expireTime: 0,
    profile: null,
  }
  const spaceState = {
    currentSpaceId: Number.isFinite(currentSpaceId) && currentSpaceId > 0 ? currentSpaceId : null,
  }

  if (token) {
    const userStateText = JSON.stringify(userState)
    window.localStorage.setItem('notask-flow-user', userStateText)
    window.sessionStorage.setItem('notask-flow-user', userStateText)
  }

  if (spaceState.currentSpaceId) {
    const spaceStateText = JSON.stringify(spaceState)
    window.localStorage.setItem('notask-flow-space', spaceStateText)
    window.sessionStorage.setItem('notask-flow-space', spaceStateText)
  }

  window.sessionStorage.setItem('notask-flow-android-collab-ready', '1')
  if (state.apiBaseUrl?.trim()) {
    window.sessionStorage.setItem('notask-flow-android-api-base-url', state.apiBaseUrl.trim())
  }
  if (state.collabWsUrl?.trim()) {
    window.sessionStorage.setItem('notask-flow-android-collab-ws-url', state.collabWsUrl.trim())
  }

  userStore.syncPersistedSession(userState)
}

const hydrateAndroidSession = () => {
  const androidState = readAndroidHashState()
  if (androidState) {
    bootstrapState.value = androidState
    persistAndroidHashState(androidState)
    window.history.replaceState(null, document.title, window.location.pathname + window.location.search)
    console.info('[NotaskCollab] Android bootstrap state applied', {
      apiBaseUrl: androidState.apiBaseUrl,
      collabWsUrl: androidState.collabWsUrl,
      hasToken: Boolean(androidState.token),
      spaceId: androidState.currentSpaceId,
    })
  }

  const userState = readJsonStorage('notask-flow-user')
  if (userState) {
    userStore.syncPersistedSession(userState as Parameters<typeof userStore.syncPersistedSession>[0])
  }
}

const resolvePersistedSpaceId = () => {
  const stateSpaceId = Number(bootstrapState.value?.currentSpaceId)
  if (Number.isFinite(stateSpaceId) && stateSpaceId > 0) {
    return stateSpaceId
  }

  const spaceState = readJsonStorage('notask-flow-space')
  const currentSpaceId = Number(spaceState?.currentSpaceId)
  return Number.isFinite(currentSpaceId) && currentSpaceId > 0 ? currentSpaceId : null
}

const loadNote = async () => {
  loading.value = true
  errorMessage.value = ''
  hydrateAndroidSession()
  const noteId = resolveNumericParam(route.params.noteId)
  const querySpaceId = resolveNumericParam(route.query.spaceId)
  const spaceId = querySpaceId || resolvePersistedSpaceId()

  if (!noteId || !spaceId) {
    errorMessage.value = '缺少协作文档参数'
    loading.value = false
    return
  }

  if (!userStore.tokenValue) {
    errorMessage.value = '登录态未注入，请重新从 Android 打开协作文档'
    loading.value = false
    return
  }

  try {
    if (!userStore.profile) {
      await userStore.fetchProfile().catch(() => undefined)
    }
    console.info('[NotaskCollab] loading note', { noteId, spaceId })
    const detail = await noteService.detail(spaceId, noteId)
    note.value = detail
    content.value = detail.content || ''
    contentHtml.value = detail.contentHtml || ''
    console.info('[NotaskCollab] note loaded', {
      canEdit: detail.canEdit,
      collabEnabled: detail.collabEnabled,
      hasHtml: Boolean(detail.contentHtml),
      noteId: detail.id,
      spaceId: detail.spaceId,
    })
  } catch (error) {
    console.error('[NotaskCollab] note load failed', error)
    errorMessage.value = error instanceof Error ? error.message : '加载协作文档失败'
  } finally {
    loading.value = false
  }
}

const handlePersisted = (nextNote: Note) => {
  note.value = nextNote
}

const handleStatusChange = (payload: { message: string; status: string }) => {
  statusMessage.value = payload.message
  connectionStatus.value = payload.status
}

onMounted(() => {
  loadNote()
})
</script>

<style scoped>
.android-collab-page {
  background: #f9f7f5;
  color: #1d1b20;
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-height: 0;
  overflow: hidden;
}

.android-collab-header {
  align-items: center;
  background: rgba(255, 251, 255, 0.94);
  border-bottom: 1px solid rgba(121, 116, 126, 0.16);
  display: flex;
  flex-shrink: 0;
  gap: 16px;
  justify-content: space-between;
  padding: 14px 16px;
}

.android-collab-header p {
  color: #7a6a61;
  font-size: 12px;
  font-weight: 700;
  margin: 0 0 3px;
}

.android-collab-header h1 {
  font-size: 18px;
  font-weight: 760;
  line-height: 1.25;
  margin: 0;
}

.android-collab-status {
  border-radius: 999px;
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 700;
  padding: 6px 10px;
}

.android-collab-status.is-ready {
  background: #d9eadf;
  color: #23643d;
}

.android-collab-status.is-loading {
  background: #fff1d6;
  color: #7c4f00;
}

.android-collab-status.is-error {
  background: #ffe0dc;
  color: #9b1b12;
}

.android-collab-editor {
  border: 0;
  border-radius: 0;
  flex: 1;
  min-height: 0;
}

.android-collab-state {
  align-items: center;
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 10px;
  justify-content: center;
  padding: 24px;
  text-align: center;
}

.android-collab-state .material-symbols-outlined {
  color: #9f4122;
  font-size: 40px;
}

.android-collab-state p {
  color: #6f635c;
  margin: 0;
}
</style>
