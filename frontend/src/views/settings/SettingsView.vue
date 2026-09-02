<template>
  <div class="grid gap-6 xl:grid-cols-[300px_1fr]">
    <aside class="app-shell space-y-2">
      <h1 class="font-title-serif text-3xl text-on-surface">{{ t('settings.workspace') }}</h1>
      <button
        v-for="tab in tabs"
        :key="tab.value"
        class="flex w-full items-center justify-between rounded-[1.4rem] px-4 py-4 text-left transition"
        :class="activeTab === tab.value ? 'bg-surface-container-high text-primary' : 'text-on-surface-variant hover:bg-surface-container-low'"
        type="button"
        @click="activeTab = tab.value"
      >
        <div class="flex items-center gap-3">
          <span class="material-symbols-outlined text-lg">{{ tab.icon }}</span>
          <span class="font-label-bold">{{ tab.label }}</span>
        </div>
        <span class="material-symbols-outlined text-base">chevron_right</span>
      </button>
    </aside>

    <section class="app-shell">
      <div v-if="activeTab === 'profile'" class="space-y-6">
        <div>
          <h2 class="font-display-serif text-4xl text-on-surface">{{ t('settings.profileTitle') }}</h2>
          <p class="mt-2 text-body-secondary text-on-surface-variant">{{ t('settings.profileDescription') }}</p>
        </div>

        <div class="flex flex-col gap-6 md:flex-row md:items-end">
          <div class="flex h-24 w-24 items-center justify-center overflow-hidden rounded-full border-4 border-surface shadow-md">
            <img
              v-if="userStore.profile?.avatarUrl"
              :src="userStore.profile.avatarUrl"
              alt="avatar"
              class="h-full w-full object-cover"
            />
            <span v-else class="material-symbols-outlined text-4xl text-on-surface-variant">person</span>
          </div>
          <label class="app-secondary-button cursor-pointer">
            <span class="material-symbols-outlined text-base">upload</span>
            {{ t('settings.uploadAvatar') }}
            <input type="file" class="hidden" accept="image/*" @change="uploadAvatar" />
          </label>
        </div>

        <div class="grid gap-4 md:grid-cols-2">
          <label class="block">
            <span class="mb-2 block text-label-bold text-on-surface">{{ t('settings.nickname') }}</span>
            <input v-model="profileForm.nickname" class="app-input w-full px-4 py-3" />
          </label>
          <label class="block">
            <span class="mb-2 block text-label-bold text-on-surface">{{ t('common.username') }}</span>
            <input :value="userStore.profile?.username" readonly class="app-input w-full px-4 py-3 opacity-70" />
          </label>
        </div>

        <div class="space-y-4 rounded-[1.4rem] border border-outline-variant/30 bg-surface-container-low/40 p-4">
          <div>
            <span class="mb-2 block text-label-bold text-on-surface">{{ t('common.email') }}</span>
            <input :value="profileForm.email" type="email" class="app-input w-full px-4 py-3 opacity-70" readonly />
            <p class="mt-2 text-caption text-on-surface-variant">修改邮箱前会向当前邮箱发送验证码，确认是本人操作后再生效。</p>
          </div>
          <div class="grid gap-4 md:grid-cols-[minmax(0,1fr)_160px]">
            <label class="block">
              <span class="mb-2 block text-label-bold text-on-surface">新邮箱</span>
              <input v-model="emailChangeForm.newEmail" type="email" class="app-input w-full px-4 py-3" placeholder="请输入新的邮箱地址" />
            </label>
            <div class="flex items-end">
              <button
                class="app-secondary-button w-full justify-center"
                :disabled="emailCodeSending"
                type="button"
                @click="sendEmailChangeCode"
              >
                {{ emailCodeSending ? '发送中' : '发送验证码' }}
              </button>
            </div>
          </div>
          <div class="grid gap-4 md:grid-cols-[minmax(0,1fr)_160px]">
            <label class="block">
              <span class="mb-2 block text-label-bold text-on-surface">旧邮箱验证码</span>
              <input v-model="emailChangeForm.code" class="app-input w-full px-4 py-3" inputmode="numeric" maxlength="6" placeholder="6 位验证码" />
            </label>
            <div class="flex items-end">
              <button
                class="app-primary-button w-full justify-center"
                :disabled="emailChanging"
                type="button"
                @click="changeEmail"
              >
                {{ emailChanging ? '修改中' : '确认修改' }}
              </button>
            </div>
          </div>
        </div>

        <div class="flex justify-end">
          <button class="app-primary-button" type="button" @click="saveProfile">{{ t('common.save') }}</button>
        </div>
      </div>

      <div v-else-if="activeTab === 'security'" class="space-y-6">
        <div>
          <h2 class="font-display-serif text-4xl text-on-surface">{{ t('settings.accountSecurity') }}</h2>
          <p class="mt-2 text-body-secondary text-on-surface-variant">{{ t('settings.securityDescription') }}</p>
        </div>

        <div class="grid gap-4">
          <label class="block">
            <span class="mb-2 block text-label-bold text-on-surface">{{ t('settings.oldPassword') }}</span>
            <input v-model="passwordForm.oldPassword" type="password" class="app-input w-full px-4 py-3" />
          </label>
          <label class="block">
            <span class="mb-2 block text-label-bold text-on-surface">{{ t('settings.newPassword') }}</span>
            <input v-model="passwordForm.newPassword" type="password" class="app-input w-full px-4 py-3" />
          </label>
        </div>

        <div class="flex justify-end">
          <button class="app-primary-button" type="button" @click="savePassword">{{ t('settings.updatePassword') }}</button>
        </div>
      </div>

      <div v-else-if="activeTab === 'theme'" class="space-y-6">
        <div>
          <h2 class="font-display-serif text-4xl text-on-surface">{{ t('settings.themeTitle') }}</h2>
          <p class="mt-2 text-body-secondary text-on-surface-variant">{{ t('settings.themeDescription') }}</p>
        </div>

        <div class="grid gap-5 md:grid-cols-2">
          <label class="block">
            <span class="mb-2 block text-label-bold text-on-surface">{{ t('settings.themeMode') }}</span>
            <el-select v-model="settingsForm.themeMode" class="w-full">
              <el-option :label="t('settings.system')" value="system" />
              <el-option :label="t('settings.light')" value="light" />
              <el-option :label="t('settings.dark')" value="dark" />
            </el-select>
          </label>
          <label class="block">
            <span class="mb-2 block text-label-bold text-on-surface">{{ t('settings.personalThemePreset') }}</span>
            <el-select v-model="settingsForm.personalThemePreset" class="w-full">
              <el-option label="Sunrise" value="sunrise" />
              <el-option label="Forest" value="forest" />
              <el-option label="Ocean" value="ocean" />
              <el-option label="Midnight" value="midnight" />
            </el-select>
          </label>
        </div>

        <div class="flex justify-end">
          <button class="app-primary-button" type="button" @click="saveSettings">{{ t('common.saveSettings') }}</button>
        </div>
      </div>

      <div v-else class="space-y-6">
        <div>
          <h2 class="font-display-serif text-4xl text-on-surface">{{ t('settings.notifications') }}</h2>
          <p class="mt-2 text-body-secondary text-on-surface-variant">{{ t('settings.notificationsDescription') }}</p>
        </div>

        <div class="grid gap-4 md:grid-cols-2">
          <label
            v-for="option in notificationOptions"
            :key="option.key"
            class="app-card flex items-center justify-between"
          >
            <span class="text-body-main text-on-surface">{{ option.label }}</span>
            <input
              v-model="settingsForm[option.key]"
              type="checkbox"
              class="rounded border-outline-variant text-primary focus:ring-primary/20"
            />
          </label>
          <label class="block">
            <span class="mb-2 block text-label-bold text-on-surface">{{ t('settings.quietStart') }}</span>
            <el-time-select
              v-model="settingsForm.quietStartTime"
              class="w-full"
              clearable
              end="23:30"
              :placeholder="t('settings.selectStartTime')"
              start="00:00"
              step="00:30"
            />
          </label>
          <label class="block">
            <span class="mb-2 block text-label-bold text-on-surface">{{ t('settings.quietEnd') }}</span>
            <el-time-select
              v-model="settingsForm.quietEndTime"
              class="w-full"
              clearable
              end="23:30"
              :placeholder="t('settings.selectEndTime')"
              start="00:00"
              step="00:30"
            />
          </label>
        </div>

        <div class="flex justify-end">
          <button class="app-primary-button" type="button" @click="saveSettings">{{ t('common.saveSettings') }}</button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { userService } from '@/api/services'
import { useI18n } from '@/i18n'
import { useSpaceStore } from '@/stores/space'
import { useUiStore } from '@/stores/ui'
import { useUserStore } from '@/stores/user'
import type { NotificationSetting } from '@/types/app'

const spaceStore = useSpaceStore()
const userStore = useUserStore()
const uiStore = useUiStore()
const { t } = useI18n()
const activeTab = ref<'profile' | 'security' | 'theme' | 'notifications'>('profile')

const tabs = computed(() => {
  const baseTabs = [
    { label: t('settings.profile'), value: 'profile' as const, icon: 'person' },
    { label: t('settings.security'), value: 'security' as const, icon: 'security' },
    { label: t('settings.theme'), value: 'theme' as const, icon: 'palette' },
    { label: t('settings.notifications'), value: 'notifications' as const, icon: 'notifications' },
  ]

  if (spaceStore.currentSpace?.type === 'TEAM') {
    if (activeTab.value === 'theme') {
      activeTab.value = 'profile'
    }

    return baseTabs.filter((tab) => tab.value !== 'theme')
  }

  return baseTabs
})

const profileForm = reactive({
  nickname: '',
  email: '',
})

const emailChangeForm = reactive({
  newEmail: '',
  code: '',
})

const emailCodeSending = ref(false)
const emailChanging = ref(false)
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
})

const settingsForm = reactive<NotificationSetting>({
  themeMode: 'system',
  personalThemePreset: 'sunrise',
  sidebarMode: 'expanded',
  taskNoticeEnabled: true,
  noteNoticeEnabled: true,
  mentionNoticeEnabled: true,
  systemNoticeEnabled: true,
  emailEnabled: false,
  taskEmailEnabled: false,
  todoEmailEnabled: false,
  mentionEmailEnabled: false,
  quietEnabled: false,
  quietStartTime: '22:00',
  quietEndTime: '08:00',
})

const notificationOptions = computed(() => [
  { key: 'taskNoticeEnabled' as const, label: t('settings.options.taskNotice') },
  { key: 'noteNoticeEnabled' as const, label: t('settings.options.noteNotice') },
  { key: 'mentionNoticeEnabled' as const, label: t('settings.options.mentionNotice') },
  { key: 'systemNoticeEnabled' as const, label: t('settings.options.systemNotice') },
  { key: 'emailEnabled' as const, label: t('settings.options.email') },
  { key: 'taskEmailEnabled' as const, label: t('settings.options.taskEmail') },
  { key: 'todoEmailEnabled' as const, label: t('settings.options.todoEmail') },
  { key: 'mentionEmailEnabled' as const, label: t('settings.options.mentionEmail') },
  { key: 'quietEnabled' as const, label: t('settings.options.quiet') },
])

const hydrate = async () => {
  if (!userStore.profile) {
    await userStore.fetchProfile()
  }

  profileForm.nickname = userStore.profile?.nickname || ''
  profileForm.email = userStore.profile?.email || ''
  emailChangeForm.newEmail = ''
  emailChangeForm.code = ''

  const settings = await userService.notificationSettings()
  Object.assign(settingsForm, settings)
  uiStore.applySettings(settings)
}

const saveProfile = async () => {
  await userService.updateProfile({
    nickname: profileForm.nickname,
  })
  await userStore.fetchProfile()
  ElMessage.success(t('messages.profileSaved'))
}

const normalizedNewEmail = () => emailChangeForm.newEmail.trim().toLowerCase()

const validateNewEmail = () => {
  const email = normalizedNewEmail()
  if (!EMAIL_PATTERN.test(email)) {
    ElMessage.warning('请输入正确的新邮箱地址')
    return ''
  }
  if (email === profileForm.email.trim().toLowerCase()) {
    ElMessage.warning('新邮箱不能与当前邮箱相同')
    return ''
  }
  return email
}

const sendEmailChangeCode = async () => {
  const newEmail = validateNewEmail()
  if (!newEmail) {
    return
  }

  emailCodeSending.value = true
  try {
    await userService.sendEmailChangeCode({ newEmail })
    ElMessage.success('验证码已发送到当前邮箱')
  } finally {
    emailCodeSending.value = false
  }
}

const changeEmail = async () => {
  const newEmail = validateNewEmail()
  if (!newEmail) {
    return
  }
  if (!/^\d{6}$/.test(emailChangeForm.code.trim())) {
    ElMessage.warning('请输入 6 位验证码')
    return
  }

  emailChanging.value = true
  try {
    await userService.changeEmail({
      newEmail,
      code: emailChangeForm.code.trim(),
    })
    await userStore.fetchProfile()
    profileForm.email = userStore.profile?.email || ''
    emailChangeForm.newEmail = ''
    emailChangeForm.code = ''
    ElMessage.success('邮箱已修改')
  } finally {
    emailChanging.value = false
  }
}

const savePassword = async () => {
  await userService.updatePassword(passwordForm)
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  ElMessage.success(t('messages.passwordUpdated'))
}

const saveSettings = async () => {
  const settings = await userService.updateNotificationSettings(settingsForm)
  Object.assign(settingsForm, settings)
  uiStore.applySettings(settings)
  ElMessage.success(t('messages.settingsSaved'))
}

const uploadAvatar = async (event: Event) => {
  const files = (event.target as HTMLInputElement).files
  if (!files?.length) {
    return
  }

  const formData = new FormData()
  formData.append('file', files[0])
  await userService.uploadAvatar(formData)
  await userStore.fetchProfile()
  ElMessage.success(t('messages.avatarUpdated'))
}

onMounted(() => {
  hydrate().catch(() => undefined)
})
</script>
