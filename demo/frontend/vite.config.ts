import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import { viteExternalsPlugin } from "vite-plugin-externals";

// https://vite.dev/config/
export default defineConfig({
  plugins: [
      viteExternalsPlugin({
        'luna': '__LUNA_COMPONENTS__',
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
      external: ['vue'],
      output: {
        entryFileNames: '[name].js',
        //chunkFileNames: '[name].js',
        exports: 'named',
        globals: {
          vue: 'Vue',
        },

      },
    },
    outDir: resolve(__dirname, '../src/main/resources/frontend'),
    emptyOutDir: true,
  },
})
