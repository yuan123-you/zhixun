import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

beforeEach(() => {
  vi.resetModules()
  localStorage.clear()
  vi.stubGlobal('Image', class {
    crossOrigin = ''
    naturalWidth = 128
    onload: (() => void) | null = null
    onerror: (() => void) | null = null
    set src(_url: string) {
      // The supplied image host rejects requests with Origin headers.
      queueMicrotask(() => this.crossOrigin === 'anonymous' && _url.startsWith('https://trae-api-cn.')
        ? this.onerror?.() : this.onload?.())
    }
  })
  vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue({ drawImage() {} } as unknown as CanvasRenderingContext2D)
  vi.spyOn(HTMLCanvasElement.prototype, 'toDataURL').mockReturnValue('data:image/jpeg;base64,cached-avatar')
})
afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks() })

describe('avatar cache origin handling', () => {
  it('keeps external avatars displayable without issuing a CORS canvas preload', async () => {
    const { preloadAndCacheAvatar } = await import('@/utils/avatarCache')
    const url = 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=food&image_size=square_hd'
    expect(await preloadAndCacheAvatar(url)).toBe(url)
    expect(localStorage.length).toBe(0)
  })

  it('preserves same-origin canvas caching for uploaded avatars', async () => {
    const { preloadAndCacheAvatar, getMemoryCachedAvatar } = await import('@/utils/avatarCache')
    const url = '/api/v1/files/view/avatars/uploaded.jpg'
    expect(await preloadAndCacheAvatar(url)).toBe('data:image/jpeg;base64,cached-avatar')
    expect(getMemoryCachedAvatar(url)).toBe('data:image/jpeg;base64,cached-avatar')
  })
})
