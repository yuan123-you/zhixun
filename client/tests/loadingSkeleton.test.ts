import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, type VueWrapper } from '@vue/test-utils'
import { onMounted, onUnmounted, ref } from 'vue'
import LoadingSkeleton from '@/components/LoadingSkeleton.vue'
import ArticleList from '@/components/ArticleList.vue'

let wrapper: VueWrapper | undefined
beforeEach(() => {
  for (const [name, value] of Object.entries({ ref, onMounted, onUnmounted })) vi.stubGlobal(name, value)
  vi.stubGlobal('IntersectionObserver', class { observe() {}; disconnect() {} })
})
afterEach(() => { wrapper?.unmount(); vi.unstubAllGlobals() })
const options = { global: { components: { LoadingSkeleton }, stubs: { ArticleCard: true, ErrorRetry: true, EmptyState: true } } }

describe('article skeleton counts', () => {
  it('renders one card per skeleton by default instead of multiplying caller loops', () => {
    wrapper = mount(LoadingSkeleton, { props: { type: 'article' } })
    expect(wrapper.findAll('.skeleton-card')).toHaveLength(1)
  })
  it('honors an explicit skeleton count', () => {
    wrapper = mount(LoadingSkeleton, { props: { type: 'article', count: 2 } })
    expect(wrapper.findAll('.skeleton-card')).toHaveLength(2)
  })
  it('matches the initial feed page size rather than showing 18 oversized cards', () => {
    wrapper = mount(ArticleList, { ...options, props: { articles: [], loading: true, hasMore: true } })
    expect(wrapper.findAll('.skeleton-card')).toHaveLength(10)
  })
  it('adds only two placeholders while loading the next page', () => {
    wrapper = mount(ArticleList, { ...options, props: { articles: [{ id: 1 }], loading: true, hasMore: true } })
    expect(wrapper.findAll('.skeleton-card')).toHaveLength(2)
  })
})
