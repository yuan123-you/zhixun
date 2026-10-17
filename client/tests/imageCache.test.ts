import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, ref } from 'vue'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { getCachedImageUrl, useImageCache } from '@/utils/imageCache'

let wrapper: VueWrapper | undefined
let fetchResource: ReturnType<typeof vi.fn>
let cache: { match: ReturnType<typeof vi.fn>, put: ReturnType<typeof vi.fn> }

beforeEach(() => {
  cache = { match: vi.fn(async () => undefined), put: vi.fn(async () => {}) }
  vi.stubGlobal('caches', { open: async () => cache })
  fetchResource = vi.fn()
  vi.stubGlobal('fetch', fetchResource)
  let id = 0
  vi.spyOn(URL, 'createObjectURL').mockImplementation(() => `blob:cached-${++id}`)
})
afterEach(() => {
  wrapper?.unmount()
  wrapper = undefined
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
})

const imageResponse = () => new Response('image bytes', { headers: { 'Content-Type': 'image/png' } })

function renderImage(source: ReturnType<typeof ref<string>>) {
  wrapper = mount(defineComponent({
    setup() {
      const cached = useImageCache(source)
      return () => h('img', { src: cached.value })
    },
  }))
}

describe('image cache correctness', () => {
  it('does not let an old fetch overwrite a newer image', async () => {
    let finishOld!: (response: Response) => void
    fetchResource.mockImplementationOnce(() => new Promise<Response>(resolve => { finishOld = resolve }))
      .mockResolvedValueOnce(imageResponse())
    const source = ref('/images/race-old.png')
    renderImage(source)
    await flushPromises()
    source.value = '/images/race-new.png'
    await flushPromises()
    expect(wrapper!.get('img').attributes('src')).toBe('blob:cached-1')
    finishOld(imageResponse())
    await flushPromises()
    expect(wrapper!.get('img').attributes('src')).toBe('blob:cached-1')
  })

  it('keeps an empty source empty after a pending image completes', async () => {
    let finish!: (response: Response) => void
    fetchResource.mockImplementation(() => new Promise<Response>(resolve => { finish = resolve }))
    const source = ref('/images/cleared.png')
    renderImage(source)
    await flushPromises()
    source.value = ''
    await flushPromises()
    finish(imageResponse())
    await flushPromises()
    expect(wrapper!.get('img').attributes('src')).toBe('')
  })

  it('retries a transient failed request instead of memoizing failure forever', async () => {
    fetchResource.mockResolvedValueOnce(new Response(null, { status: 503 })).mockResolvedValueOnce(imageResponse())
    expect(await getCachedImageUrl('/images/retry.png')).toBe('/images/retry.png')
    expect(await getCachedImageUrl('/images/retry.png')).toBe('blob:cached-1')
    expect(fetchResource).toHaveBeenCalledTimes(2)
  })

  it('does not cache a successful HTML fallback as an image blob', async () => {
    fetchResource.mockResolvedValue(new Response('<html>SPA fallback</html>', { headers: { 'Content-Type': 'text/html' } }))
    expect(await getCachedImageUrl('/images/not-an-image.png')).toBe('/images/not-an-image.png')
    expect(cache.put).not.toHaveBeenCalled()
  })

  it('ignores a non-image response already present in persistent storage', async () => {
    cache.match.mockResolvedValue(new Response('<html>old bad entry</html>', { headers: { 'Content-Type': 'text/html' } }))
    fetchResource.mockResolvedValue(imageResponse())
    expect(await getCachedImageUrl('/images/bad-old-cache.png')).toBe('blob:cached-1')
    expect(fetchResource).toHaveBeenCalledTimes(1)
  })

  it('deduplicates concurrent loads and reuses successful memory entries', async () => {
    fetchResource.mockResolvedValue(imageResponse())
    expect(await Promise.all([getCachedImageUrl('/images/dedupe.png'), getCachedImageUrl('/images/dedupe.png')]))
      .toEqual(['blob:cached-1', 'blob:cached-1'])
    expect(await getCachedImageUrl('/images/dedupe.png')).toBe('blob:cached-1')
    expect(fetchResource).toHaveBeenCalledTimes(1)
  })

  it('reuses valid persistent entries without fetching', async () => {
    cache.match.mockResolvedValue(imageResponse())
    expect(await getCachedImageUrl('/images/persistent.png')).toBe('blob:cached-1')
    expect(fetchResource).not.toHaveBeenCalled()
  })

  it.each(['data:image/png;base64,AA==', 'blob:existing'])('does not fetch inline image %s', async url => {
    expect(await getCachedImageUrl(url)).toBe(url)
    expect(fetchResource).not.toHaveBeenCalled()
  })
})
