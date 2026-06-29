import { ref } from 'vue'

/**
 * 跨组件共享 header 可见状态
 * AppHeader 写入，default.vue layout 读取，
 * 使 main 区域的 pt-[52px] 能跟随 header 显隐同步。
 */
const showHeaderInner = ref(true)

export function useHeaderVisibility() {
  return { showHeaderInner }
}
