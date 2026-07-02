import { defineConfig, type Plugin } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { resolve } from 'path'

/**
 * Cloudflare 兼容插件：自动给所有 <script> 标签添加 data-cfasync="false"
 *
 * 背景：
 *   Cloudflare 的 Rocket Loader 会改写页面中的 <script> 标签，
 *   用 CF 自己的异步加载包装器包裹原始脚本。对于 ES module 来说，
 *   这种改写会破坏 import / export 语法与模块依赖顺序，
 *   导致 Vue Router、Pinia 在移动端浏览器执行失败，全局 errorHandler
 *   抛出"操作失败"提示。
 *
 * 解决方案：
 *   在构建阶段给 index.html 中所有 <script> 标签加上 data-cfasync="false"，
 *   这是 Cloudflare 官方支持的"豁免"标记，被标记的脚本不会被 Rocket Loader 改写。
 *
 * 参考：https://developers.cloudflare.com/speed/optimization/content/rocket-loader/
 */
function cloudflareRocketLoaderSafe(): Plugin {
  return {
    name: 'cloudflare-rocket-loader-safe',
    transformIndexHtml: {
      order: 'pre',
      handler(html) {
        return html.replace(
          /<script(\s[^>]*)?>/g,
          (match, attrs = '') => {
            if (/\bdata-cfasync\s*=/.test(attrs)) return match
            return `<script${attrs} data-cfasync="false">`
          }
        )
      },
    },
  }
}

// https://vitejs.dev/config/
export default defineConfig({
  base: '/',
  plugins: [
    vue(),
    cloudflareRocketLoaderSafe(),
    AutoImport({
      // 关闭 CSS 注入：main.ts 已通过 import 'element-plus/dist/index.css' 全量加载，
      // 避免 ElementPlusResolver 在每个组件使用处再次注入单独的 CSS 文件导致 ERR_ABORTED
      resolvers: [ElementPlusResolver({ importStyle: false })],
      imports: ['vue', 'vue-router', 'pinia'],
      // Auto-import custom composables and stores
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
  // - Docker：CLIENT_PORT=3500（容器映射端口），由 Nginx 代理
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
        // AI 接口后端 read timeout 120s + 重试延迟，代理超时须大于前端 axios timeout(130s)
        timeout: 135_000,
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
