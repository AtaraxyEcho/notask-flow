<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Refresh } from '@element-plus/icons-vue'
import { adminSettingService, type AdminSystemSetting } from '@/api/modules/admin'

const loading = ref(false)
const saving = ref(false)
const settings = ref<AdminSystemSetting[]>([])

async function loadSettings() {
  loading.value = true
  try {
    settings.value = await adminSettingService.list()
  } finally {
    loading.value = false
  }
}

async function saveSettings() {
  saving.value = true
  try {
    const payload = settings.value.reduce<Record<string, string>>((result, item) => {
      result[item.settingKey] = item.settingValue
      return result
    }, {})
    settings.value = await adminSettingService.update(payload)
    ElMessage.success('系统配置已保存')
  } finally {
    saving.value = false
  }
}

onMounted(loadSettings)
</script>

<template>
  <section class="admin-setting-page">
    <div class="admin-table-header">
      <h2>系统配置</h2>
      <div class="admin-actions">
        <el-button :icon="Refresh" @click="loadSettings">刷新</el-button>
        <el-button type="primary" :icon="Check" :loading="saving" @click="saveSettings">保存</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="settings" border>
      <el-table-column prop="settingKey" label="配置键" min-width="260" />
      <el-table-column label="配置值" min-width="240">
        <template #default="{ row }">
          <el-input v-model="row.settingValue" />
        </template>
      </el-table-column>
      <el-table-column prop="description" label="说明" min-width="260" show-overflow-tooltip />
    </el-table>
  </section>
</template>

<style scoped>
.admin-setting-page {
  display: grid;
  gap: 14px;
}

.admin-table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.admin-table-header h2 {
  margin: 0;
  font-size: 18px;
}

.admin-actions {
  display: flex;
  gap: 10px;
}
</style>
