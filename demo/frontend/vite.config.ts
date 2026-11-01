import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';
import { lunaExternalGlobals } from "./luna-external-globals.ts";

// https://vite.dev/config/
export default defineConfig({
  base: './',
  plugins: [
    lunaExternalGlobals(),
    vue()
  ],
  resolve: {
    alias: {
      '@': resolve(import.meta.dirname, 'src'),
    }
  },
  build: {
    sourcemap: true,
    minify: 'terser',
    lib: {
      entry: [
      	resolve(import.meta.dirname, 'src/views/DemoView.vue'),
        resolve(import.meta.dirname, 'src/views/TodoView.vue'),
      ],
      formats: ['es'],
    },
    rolldownOptions: {
      output: {
        entryFileNames: '[name].js',
        exports: 'named',
      },
    },
    outDir: resolve(import.meta.dirname, '../src/main/resources/frontend'),
    emptyOutDir: true,
  },
})
