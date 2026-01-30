import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import { viteExternalsPlugin } from "vite-plugin-externals";

// https://vite.dev/config/
export default defineConfig({
  base: './',
  plugins: [
    viteExternalsPlugin({
      'luna': '__LUNA_COMPONENTS__',
      'vue': 'Vue',
      'axios': 'axios',
      'I18N': 'I18N'
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
      	resolve(__dirname, 'src/components/DemoComponent.vue')
      ],
      formats: ['es'],
    },
    rollupOptions: {
      external: ['vue', 'axios', 'I18N'],
      output: {
        entryFileNames: '[name].js',
        exports: 'named',
        globals: {
          vue: 'Vue',
          axios: 'axios',
          I18N: 'I18N',
        },
      },
    },
    outDir: resolve(__dirname, '../src/main/resources/frontend'),
    emptyOutDir: true,
  },
})
