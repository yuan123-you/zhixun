import { afterEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import UserAvatar from '@/components/UserAvatar.vue'
import * as avatarCache from '@/utils/avatarCache'

let wrapper: VueWrapper | undefined
afterEach(() => { wrapper?.unmount(); vi.restoreAllMocks() })

describe('avatar source changes', () => {
  it('waits for the native image load before revealing an uncached external avatar', async () => {
    const url = 'https://example.com/not-yet-loaded.jpg'
    vi.spyOn(avatarCache, 'getLocalCachedAvatar').mockResolvedValue(null)
    vi.spyOn(avatarCache, 'preloadAndCacheAvatar').mockResolvedValue(url)
    wrapper = mount(UserAvatar, { props: { src: url } })
    await flushPromises()
    expect(wrapper.get('img').classes()).not.toContain('avatar-image-loaded')
    expect(wrapper.find('.avatar-fallback').exists()).toBe(true)
    await wrapper.get('img').trigger('load')
    expect(wrapper.get('img').classes()).toContain('avatar-image-loaded')
  })

  it('does not let a slow previous cache lookup overwrite the current avatar', async () => {
    let finishOld!: (value: string) => void
    const pending = new Promise<string>(resolve => { finishOld = resolve })
    vi.spyOn(avatarCache, 'getLocalCachedAvatar').mockImplementation(url => url?.includes('old.jpg')
      ? pending : Promise.resolve('data:image/png;base64,current'))
    vi.spyOn(avatarCache, 'preloadAndCacheAvatar').mockResolvedValue(null)
    wrapper = mount(UserAvatar, { props: { src: 'https://example.com/old.jpg' } })
    await wrapper.setProps({ src: 'https://example.com/current.jpg' })
    await flushPromises()
    expect(wrapper.get('img').attributes('src')).toBe('data:image/png;base64,current')
    finishOld('data:image/png;base64,old')
    await flushPromises()
    expect(wrapper.get('img').attributes('src')).toBe('data:image/png;base64,current')
  })
})
