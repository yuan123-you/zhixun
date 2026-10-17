import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import axios, { type AxiosRequestConfig } from 'axios'
import { useApi } from '@/composables/useApi'
import Discover from '@/pages/discover/index.vue'
import EmptyState from '@/components/EmptyState.vue'

let wrapper: VueWrapper | undefined
let fail = false
let empty = false
let requests: AxiosRequestConfig[] = []
const originalAdapter = axios.defaults.adapter
const alice = { id: 10, nickname: 'Alice', bio: 'Writes about science', followerCount: 5 }
const bob = { id: 11, nickname: 'Bob', bio: 'Writes about history', followerCount: 8 }
const options = { global: {
  components: { EmptyState },
  stubs: { PullToRefresh: { template: '<div><slot /></div>' }, RouterLink: { template: '<a><slot /></a>' }, UserAvatar: true, ErrorRetry: true },
} }
function panel() { return wrapper!.findAll('.discover-card').find(card => card.text().includes('创作者推荐'))! }

beforeEach(() => {
  fail = false; empty = false; requests = []
  localStorage.clear()
  axios.defaults.adapter = async (config) => {
    if (config.url === '/users/recommend') {
      requests.push(config)
      if (fail) throw new Error('offline')
    }
    const data = config.url !== '/users/recommend' || empty ? []
      : Number(config.params?.offset || 0) >= 2 ? [] : [alice, bob]
    return { data: { code: 200, message: 'success', data }, status: 200, statusText: 'OK', headers: {}, config }
  }
  for (const [name, value] of Object.entries({ ref, computed, watch, onMounted, onUnmounted, useApi })) vi.stubGlobal(name, value)
  vi.stubGlobal('useRouter', () => ({ push: vi.fn() }))
  vi.stubGlobal('useUserStore', () => ({ isLoggedIn: false }))
  vi.stubGlobal('useRequestCache', () => ({ cachedRequest: (fn: () => Promise<unknown>) => fn(), invalidate() {} }))
  vi.stubGlobal('useHead', () => {})
})
afterEach(() => { wrapper?.unmount(); axios.defaults.adapter = originalAdapter; vi.unstubAllGlobals() })

describe('creator recommendations', () => {
  it('sends flat query params and wraps exhausted pagination back to the first eligible users', async () => {
    wrapper = mount(Discover, options)
    await flushPromises()
    expect(panel().text()).toContain('Alice')
    await panel().get('.shuffle-btn').trigger('click'); await flushPromises()
    expect(requests.map(request => request.params?.offset)).toEqual([0, 2, 0])
    expect(requests[0].params.limit).toBe(5)
    expect(panel().text()).toContain('Alice')
    await panel().get('.shuffle-btn').trigger('click'); await flushPromises()
    expect(requests[3].params.offset).toBe(2)
  })

  it('shows a retryable error instead of a false empty state on initial failure', async () => {
    fail = true
    wrapper = mount(Discover, options)
    await flushPromises()
    expect(panel().text()).toContain('推荐创作者加载失败')
    expect(panel().text()).not.toContain('暂无推荐创作者')
    fail = false
    await panel().get('.shuffle-btn').trigger('click'); await flushPromises()
    expect(panel().text()).toContain('Alice')
  })

  it('keeps the previous users visible and explains a failed refresh', async () => {
    wrapper = mount(Discover, options); await flushPromises()
    fail = true
    await panel().get('.shuffle-btn').trigger('click'); await flushPromises()
    expect(panel().text()).toContain('Alice')
    expect(panel().text()).toContain('推荐创作者加载失败')
  })

  it('uses a compact truthful empty state only after an empty successful response', async () => {
    empty = true
    wrapper = mount(Discover, options); await flushPromises()
    expect(panel().text()).toContain('暂无推荐创作者')
    expect(panel().findComponent(EmptyState).props('compact')).toBe(true)
    expect(panel().text()).not.toContain('推荐创作者加载失败')
  })

  it('matches the requested hot-tag count with chip-shaped placeholders', async () => {
    wrapper = mount(Discover, options)
    await nextTick()
    expect(wrapper.findAll('.tag-pill[aria-hidden="true"]')).toHaveLength(16)
  })

  it('uses the same five-row creator geometry while the first request is pending', async () => {
    wrapper = mount(Discover, options)
    await nextTick()
    expect(panel().findAll('.creator-row--skeleton')).toHaveLength(5)
  })
})
