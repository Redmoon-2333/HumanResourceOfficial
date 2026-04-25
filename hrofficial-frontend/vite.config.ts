import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import vueDevTools from 'vite-plugin-vue-devtools'

export default defineConfig({
  plugins: [
    vue(),
    vueJsx(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
        // Why: 流式 SSE 请求需要较长的超时时间，默认代理会截断
        proxyTimeout: 600000,  // 10分钟代理超时
        timeout: 600000        // 10分钟请求超时
      }
    }
  },
  build: {
    target: 'es2020',
    cssMinify: true,
    chunkSizeWarningLimit: 1000,
    reportCompressedSize: true,
    rollupOptions: {
      output: {
        manualChunks(id: string) {
          // Vue core libraries
          if (id.includes('node_modules/vue/') || 
              id.includes('node_modules/vue-router/') || 
              id.includes('node_modules/pinia/')) {
            return 'vue-vendor'
          }
          // Element Plus UI
          if (id.includes('node_modules/element-plus/') || 
              id.includes('node_modules/@element-plus/')) {
            return 'vendor-element-plus'
          }
          // Markdown rendering - split into separate chunks
          if (id.includes('node_modules/markdown-it/')) {
            return 'vendor-markdown-it'
          }
          if (id.includes('node_modules/markstream-vue/')) {
            return 'vendor-markstream'
          }
          if (id.includes('node_modules/markdown-it-task-lists') ||
              id.includes('node_modules/markdown-it-container')) {
            return 'vendor-markdown-plugins'
          }
          // Security utilities
          if (id.includes('node_modules/dompurify/')) {
            return 'vendor-security'
          }
        },
        // Better asset organization
        entryFileNames: 'assets/js/[name]-[hash].js',
        chunkFileNames: 'assets/js/[name]-[hash].js',
        assetFileNames: (chunkInfo: any) => {
          const name = chunkInfo.name || ''
          if (name.endsWith('.css')) {
            return 'assets/css/[name]-[hash].css'
          }
          return 'assets/[ext]/[name]-[hash].[ext]'
        }
      }
    }
  },
  // Build analysis: run `npx vite build --report` to generate bundle analysis report
  optimizeDeps: {
    include: ['vue', 'vue-router', 'pinia', 'element-plus'],
    exclude: []
  }
})
