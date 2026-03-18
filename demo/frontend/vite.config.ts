import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import pluginExternal from "vite-plugin-external";

// https://vite.dev/config/
export default defineConfig({
  base: './',
  plugins: [
    pluginExternal({
      externals: {
        'luna': '__LUNA_COMPONENTS__',
        'vue': 'Vue',
        'axios': 'axios',
        'I18N': 'I18N',
        'sortablejs': 'Sortable',
      }
    }),
    vue()
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    }
  },
  build: {
    sourcemap: true,
    minify: 'terser',
    lib: {
      entry: [
      	resolve(__dirname, 'src/views/DemoView.vue'),
        resolve(__dirname, 'src/views/TodoView.vue'),
      ],
      formats: ['es'],
    },
    rolldownOptions: {
      output: {
        entryFileNames: '[name].js',
        exports: 'named',
      },
    },
    outDir: resolve(__dirname, '../src/main/resources/frontend'),
    emptyOutDir: true,
  },
})
