import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest'
import { createApp } from 'vue'
import errorHandler from '@/plugins/error-handler'

const app = createApp({})
beforeAll(() => { app.use(errorHandler) })
afterEach(() => {
  document.querySelector<HTMLElement>('div[style*="z-index: 99999"]')?.click()
  document.body.innerHTML = ''
  vi.restoreAllMocks()
})

function preloadFailure() {
  const event = new Event('vite:preloadError', { cancelable: true })
  Object.assign(event, { payload: new Error('Unable to preload CSS for /zhixun/assets/ArticleGridCard.css') })
  window.dispatchEvent(event)
  return event
}

describe('asset loading recovery', () => {
  it.each([false, true])('offers explicit refresh for a preload failure (mobile=%s)', (mobile) => {
    vi.spyOn(window, 'matchMedia').mockReturnValue({ matches: mobile } as MediaQueryList)
    const event = preloadFailure()
    expect(document.body.textContent).toContain('页面资源加载失败')
    expect(document.querySelector('#__error_refresh_btn')?.textContent).toBe('刷新页面')
    expect(event.defaultPrevented).toBe(true)
    preloadFailure()
    expect(document.querySelectorAll('#__error_refresh_btn')).toHaveLength(1)
  })

  it('waits for the user to refresh rather than reloading automatically', () => {
    vi.spyOn(window, 'matchMedia').mockReturnValue({ matches: false } as MediaQueryList)
    const reload = vi.spyOn(window.location, 'reload').mockImplementation(() => {})
    preloadFailure()
    expect(reload).not.toHaveBeenCalled()
    document.querySelector<HTMLButtonElement>('#__error_refresh_btn')?.click()
    expect(reload).toHaveBeenCalledOnce()
  })

  it('handles uncaught dynamic-import rejections with the same recovery UI', () => {
    vi.spyOn(window, 'matchMedia').mockReturnValue({ matches: false } as MediaQueryList)
    const event = new Event('unhandledrejection', { cancelable: true })
    Object.assign(event, { reason: new Error('Failed to fetch dynamically imported module: /zhixun/assets/chunk.js') })
    window.dispatchEvent(event)
    expect(event.defaultPrevented).toBe(true)
    expect(document.querySelector('#__error_refresh_btn')?.textContent).toBe('刷新页面')
  })

  it('does not silently swallow a dynamic import failure reported by Vue', () => {
    vi.spyOn(window, 'matchMedia').mockReturnValue({ matches: false } as MediaQueryList)
    app.config.errorHandler?.(new Error('Failed to fetch dynamically imported module: /zhixun/assets/chunk.js'), null, 'route')
    expect(document.querySelector('#__error_refresh_btn')?.textContent).toBe('刷新页面')
  })
})
