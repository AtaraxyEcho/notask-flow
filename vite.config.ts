import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia'],
      resolvers: [ElementPlusResolver()],
      dts: 'src/auto-imports.d.ts',
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/components.d.ts',
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          const normalizedId = id.replace(/\\/g, '/')
          if (!normalizedId.includes('node_modules')) {
            return undefined
          }

          if (
            normalizedId.includes('/@vue/')
            || normalizedId.includes('/vue/')
            || normalizedId.includes('/vue-router/')
            || normalizedId.includes('/pinia/')
            || normalizedId.includes('/element-plus/')
            || normalizedId.includes('/@element-plus/')
            || normalizedId.includes('/@popperjs/')
            || normalizedId.includes('/@floating-ui/')
            || normalizedId.includes('/@ctrl/')
            || normalizedId.includes('/async-validator/')
            || normalizedId.includes('/axios/')
            || normalizedId.includes('/dayjs/')
            || normalizedId.includes('/lodash-es/')
            || normalizedId.includes('/vue-demi/')
          ) {
            return 'vendor-app'
          }

          if (
            normalizedId.includes('/@tiptap/')
            || normalizedId.includes('/prosemirror-')
            || normalizedId.includes('/y-prosemirror/')
          ) {
            return 'vendor-editor'
          }

          if (normalizedId.includes('/yjs/') || normalizedId.includes('/y-protocols/') || normalizedId.includes('/lib0/')) {
            return 'vendor-collaboration'
          }

          if (normalizedId.includes('/@vue-office/') || normalizedId.includes('/pdfjs-dist/') || normalizedId.includes('/jszip/')) {
            return 'vendor-office'
          }

          if (normalizedId.includes('/echarts/') || normalizedId.includes('/zrender/') || normalizedId.includes('/vue-echarts/')) {
            return 'vendor-charts'
          }

          if (normalizedId.includes('/three/') || normalizedId.includes('/gsap/')) {
            return 'vendor-visual'
          }

          return undefined
        },
      },
    },
  },
  server: {
    host: '0.0.0.0',
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'http://localhost:8081',
        ws: true,
        changeOrigin: true,
      },
    },
  },
})
