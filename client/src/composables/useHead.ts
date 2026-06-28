/**
 * 设置 document.title 与 description 的 composable（纯 SPA 工具）
 * 监听路由变化自动更新页面标题
 */
import { watch } from 'vue'
import { useRoute } from 'vue-router'

export function useHead(options: { title?: string | (() => string) }) {
  const route = useRoute()
  watch(
    () => route.path,
    () => {
      const title = typeof options.title === 'function' ? options.title() : options.title
      if (title) {
        document.title = title
      }
    },
    { immediate: true }
  )
}

export function useSeoMeta(meta: Record<string, string>) {
  if (meta.description) {
    const el = document.querySelector('meta[name="description"]')
    if (el) {
      el.setAttribute('content', meta.description)
    }
  }
}
