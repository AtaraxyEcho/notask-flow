<template>
  <el-dialog
    :model-value="visible"
    :title="document.title"
    width="min(680px, calc(100vw - 2rem))"
    top="6vh"
    destroy-on-close
    @update:model-value="emit('update:visible', $event)"
  >
    <div class="legal-document">
      <p class="legal-document-meta">{{ document.meta }}</p>
      <section v-for="section in document.sections" :key="section.heading">
        <h3>{{ section.heading }}</h3>
        <p v-for="(paragraph, index) in section.paragraphs" :key="index">{{ paragraph }}</p>
      </section>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { LEGAL_DOCUMENTS, type LegalDocumentType } from '@/constants/legal'

const props = defineProps<{
  visible: boolean
  type: LegalDocumentType
}>()

const emit = defineEmits<{
  (event: 'update:visible', value: boolean): void
}>()

const document = computed(() => LEGAL_DOCUMENTS[props.type])
</script>

<style scoped>
.legal-document {
  max-height: 62vh;
  overflow-y: auto;
  padding-right: 4px;
  font-size: 14px;
  line-height: 1.85;
  color: var(--el-text-color-primary);
}

.legal-document-meta {
  margin-bottom: 16px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.legal-document section + section {
  margin-top: 16px;
}

.legal-document h3 {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.legal-document p {
  margin: 0 0 6px;
}

.legal-document p:last-child {
  margin-bottom: 0;
}
</style>
