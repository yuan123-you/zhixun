import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { resolve } from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  base: '/',
  plugins: [
    vue(),
    AutoImport({
      // 关闭 CSS 注入：main.ts 已通过 import 'element-plus/dist/index.css' 全量加载，
      // 避免 ElementPlusResolver 在每个组件使用处再次注入单独的 CSS 文件导致 ERR_ABORTED
      resolvers: [ElementPlusResolver({ importStyle: false })],
      imports: ['vue', 'vue-router', 'pinia'],
      // Auto-import custom composables (replacing Nuxt auto-imports)
      dirs: ['src/composables', 'src/stores'],
      dts: 'src/auto-imports.d.ts',
    }),
    Components({
      // 关闭 CSS 注入：原因同上，避免与 main.ts 的全量 CSS 重复加载
      resolvers: [ElementPlusResolver({ importStyle: false })],
      dts: 'src/components.d.ts',
    }),
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
      '~': resolve(__dirname, 'src'),
    },
  },
  // 端口与代理目标来源：docs/PORTS.md
  // - 本机：CLIENT_PORT 默认 3500；Backend 默认 localhost:8080
  // - Docker：CLIENT_PORT=3000（容器内），由 Nginx 代理
  server: {
    port: Number(process.env.CLIENT_PORT) || 3500,
    cors: true,
    proxy: {
      '/api': {
        // Windows 上后端 Spring Boot 默认监听 IPv6 `::` (== 任意地址)，
        // Node 的 `127.0.0.1` IPv4 直连会得到 ECONNREFUSED。
        // 因此默认走 IPv6 `[::1]`，与 Spring Boot 的双栈监听保持一致。
        // 同时支持通过 VITE_API_TARGET 覆盖到其他环境（如 Docker 内网或远程后端）。
        target: process.env.VITE_API_TARGET || 'http://[::1]:8080',
        changeOrigin: true,
      },
      // 腾讯地图 WebService API 已迁移到后端代理（/api/v1/users/ip-location-detail、
      // /api/v1/users/reverse-geocode），由服务端持有 Key + Redis 缓存 + IP 限流，
      // 前端不再需要本地 /tencent-map 代理，避免 Key 暴露和 referer 域名白名单问题。
      // MinIO 文件现在通过后端 /api/v1/files/view/{bucket}/{path} 代理访问，
      // 不再需要 Vite 代理 /minio。原先的 /minio 代理会因为 MinIO 的 307 重定向
      // （签名 URL / 控制台）触发 ERR_TOO_MANY_REDIRECTS。
      // 如需在开发环境直接访问 MinIO 控制台，可手动访问 http://localhost:9001。
    },
  },
  css: {
    // SCSS preprocessor if needed: add @/assets/css/variables.scss
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
          'vendor-utils': ['axios', 'qrcode', 'html2canvas'],
        },
      },
    },
  },
})
