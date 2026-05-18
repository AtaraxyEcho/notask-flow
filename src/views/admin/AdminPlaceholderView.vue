<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const title = computed(() => String(route.meta.title || '管理模块'))
</script>

<template>
  <section class="admin-placeholder">
    <h2>{{ title }}</h2>
    <p>基础路由已接入，后续接口与页面会按实施阶段继续补全。</p>
  </section>
</template>

<style scoped>
.admin-placeholder {
  padding: 24px;
  border: 1px solid #d9e2ec;
  border-radius: 8px;
  background: #fff;
}

.admin-placeholder h2 {
  margin: 0 0 8px;
  font-size: 18px;
}

.admin-placeholder p {
  margin: 0;
  color: #6b778c;
}
</style>
