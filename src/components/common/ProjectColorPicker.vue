<template>
  <div class="space-y-3">
    <div
      class="flex items-center gap-3 rounded-2xl border border-outline-variant/40 bg-surface-container-lowest px-3 py-2 shadow-[0_8px_24px_rgba(159,65,34,0.06)] transition-colors focus-within:border-primary/60"
    >
      <input
        :value="safeColor"
        aria-label="Project cover color"
        class="h-10 w-12 shrink-0 cursor-pointer rounded-xl border-0 bg-transparent p-0"
        type="color"
        @input="handleNativeColorInput"
      />
      <input
        :value="safeColor"
        class="min-w-0 flex-1 bg-transparent text-sm font-semibold uppercase tracking-[0.08em] text-on-surface outline-none"
        inputmode="text"
        maxlength="7"
        placeholder="#0077B6"
        @input="handleTextColorInput"
      />
      <span class="h-9 w-9 shrink-0 rounded-full border border-white shadow-inner" :style="{ backgroundColor: safeColor }"></span>
    </div>

    <div class="flex flex-wrap gap-2">
      <button
        v-for="color in presetColors"
        :key="color"
        :aria-label="color"
        class="h-8 w-8 rounded-full border-2 transition-all hover:scale-105"
        :class="safeColor.toLowerCase() === color.toLowerCase() ? 'border-on-surface shadow-[0_0_0_4px_rgba(159,65,34,0.12)]' : 'border-white/80'"
        :style="{ backgroundColor: color }"
        type="button"
        @click="updateColor(color)"
      ></button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  modelValue?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const presetColors = ['#0077B6', '#9F4122', '#F46C44', '#10B981', '#F59E0B', '#6D5DF2', '#475569', '#0F766E']

const normalizeColor = (value?: string) => {
  const rawValue = (value || '').trim()
  if (/^#[0-9a-fA-F]{6}$/.test(rawValue)) {
    return rawValue.toUpperCase()
  }

  const withoutHash = rawValue.replace('#', '')
  if (/^[0-9a-fA-F]{6}$/.test(withoutHash)) {
    return `#${withoutHash.toUpperCase()}`
  }

  return '#0077B6'
}

const safeColor = computed(() => normalizeColor(props.modelValue))

const updateColor = (value: string) => {
  emit('update:modelValue', normalizeColor(value))
}

const handleNativeColorInput = (event: Event) => {
  updateColor((event.target as HTMLInputElement).value)
}

const handleTextColorInput = (event: Event) => {
  const inputValue = (event.target as HTMLInputElement).value
  if (/^#?[0-9a-fA-F]{6}$/.test(inputValue.trim())) {
    updateColor(inputValue)
  }
}
</script>
