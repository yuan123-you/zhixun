import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { resolve } from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  base: '/admin/',
  plugins: [
    vue(),
    // Element Plus 自动导入
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: ['vue', 'vue-router', 'pinia'],
      dts: 'src/auto-imports.d.ts',
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/components.d.ts',
    }),
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 3001,
    cors: true,
    // 代理配置，将 /api 请求转发到后端服务
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@use "@/styles/variables.scss" as *;`,
      },
    },
  },
  build: {
    chunkSizeWarningLimit: 1000,
    cssCodeSplit: true,
    minify: 'esbuild',
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor-element-plus': ['element-plus', '@element-plus/icons-vue'],
          'vendor-vue': ['vue', 'vue-router', 'pinia'],
          'vendor-axios': ['axios'],
        },
      },
    },
    // 如需生产环境移除 console，建议安装 terser 并将 minify 改为 'terser'，然后添加：
    // terserOptions: {
    //   compress: {
    //     drop_console: true,
    //   },
    // },
  },
  // 建议安装 vite-plugin-compression 以启用 gzip 压缩：
  // npm install -D vite-plugin-compression
  // 安装后取消以下注释：
  // plugins 中添加: viteCompression({ algorithm: 'gzip', threshold: 10240 }),
})
