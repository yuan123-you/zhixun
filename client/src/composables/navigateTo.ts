/** 全局导航函数 —— 纯 SPA 版（封装 router.push） */
import type { Router, RouteLocationRaw } from 'vue-router'

let _router: Router | null = null

export function setRouter(router: Router) {
  _router = router
}

export function navigateTo(to: RouteLocationRaw) {
  if (!_router) {
    console.error('[navigateTo] Router not initialized')
    return
  }
  return _router.push(to)
}
