import { defineConfig } from 'vite'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  build: {
    emptyOutDir: false,
    lib: {
      entry: fileURLToPath(new URL('./src/android-collab-kernel/main.ts', import.meta.url)),
      fileName: () => 'editor.js',
      formats: ['iife'],
      name: 'NotaskAndroidCollabKernel',
    },
    minify: true,
    outDir: fileURLToPath(new URL('../android/app/src/main/assets/notask_collab', import.meta.url)),
    rollupOptions: {
      output: {
        inlineDynamicImports: true,
      },
    },
    sourcemap: false,
  },
})
