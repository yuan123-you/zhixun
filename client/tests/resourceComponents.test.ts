import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { shallowMount, type VueWrapper } from '@vue/test-utils'
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useResourceUrl } from '@/composables/useResourceUrl'
import ArticleCard from '@/components/ArticleCard.vue'
import ArticleGridCard from '@/components/ArticleGridCard.vue'
import Register from '@/pages/register.vue'
import ForgotPassword from '@/pages/forgot-password.vue'

let wrapper: VueWrapper | undefined
const article = {
  id: 99, title: 'AI', summary: 'Summary', content: '',
  coverImage: 'https://example.com/cover.jpg', authorId: 4,
  authorName: 'Author', createdAt: '2026-03-15', likeCount: 1,
  commentCount: 0, shareCount: 0, tags: [], categoryId: 1, categoryName: 'AI',
}
const stubs = {
  RouterLink: { template: '<a><slot /></a>' },
  ElIcon: { template: '<span><slot /></span>' },
  UserAvatar: true, TopicBadge: true, ElTag: true,
}

beforeEach(() => {
  // The application auto-imports these APIs; expose the same real Vue APIs here.
  for (const [name, value] of Object.entries({ computed, onMounted, onUnmounted, reactive, ref, watch, useResourceUrl })) {
    vi.stubGlobal(name, value)
  }
  vi.stubGlobal('useRouter', () => ({ push: vi.fn() }))
  vi.stubGlobal('useUserStore', () => ({ isLoggedIn: false }))
  vi.stubGlobal('useCacheInvalidation', () => ({ invalidateArticle: vi.fn() }))
  vi.stubGlobal('useLocalSettings', () => ({ autoLoadImages: ref(true) }))
  vi.stubGlobal('useHead', () => {})
  vi.stubGlobal('useAuth', () => ({
    getGraphCaptcha: async () => ({ captchaKey: 'test', image: 'data:image/png;base64,AA==' }),
    register: vi.fn(), forgotPassword: vi.fn(), sendCode: vi.fn(),
  }))
})

afterEach(() => {
  wrapper?.unmount()
  wrapper = undefined
  vi.unstubAllGlobals()
})

for (const [name, component, selector] of [
  ['list', ArticleCard, '.cover-image'],
  ['grid', ArticleGridCard, '.grid-card-img'],
] as const) {
  describe(`${name} article cover`, () => {
    it('renders a reachable generated image URL', () => {
      const coverImage = 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=AI'
      wrapper = shallowMount(component, { props: { article: { ...article, coverImage } }, global: { stubs } })
      expect(wrapper.get(selector).attributes('src')).toBe(coverImage)
    })

    it('falls back after a failed image and retries when the source changes', async () => {
      wrapper = shallowMount(component, { props: { article: { ...article } }, global: { stubs } })
      await wrapper.get(selector).trigger('error')
      expect(wrapper.find(selector).exists()).toBe(false)
      await wrapper.setProps({ article: { ...article, coverImage: 'https://example.com/new.jpg' } })
      expect(wrapper.get(selector).attributes('src')).toBe('https://example.com/new.jpg')
    })

    it('never creates an image with an empty src for an invalid resource', () => {
      wrapper = shallowMount(component, { props: { article: { ...article, coverImage: '/minio/' } }, global: { stubs } })
      expect(wrapper.find(selector).exists()).toBe(false)
    })
  })
}

describe('list image preference', () => {
  it('keeps automatic image loading disabled when the user opts out', () => {
    vi.stubGlobal('useLocalSettings', () => ({ autoLoadImages: ref(false) }))
    wrapper = shallowMount(ArticleCard, { props: { article: { ...article } }, global: { stubs } })
    expect(wrapper.find('.cover-image').exists()).toBe(false)
    expect(wrapper.find('.cover-placeholder').exists()).toBe(true)
  })
})

for (const [name, component] of [['register', Register], ['forgot password', ForgotPassword]] as const) {
  describe(`${name} form autofill`, () => {
    it('marks the verification code as a numeric one-time code', () => {
      wrapper = shallowMount(component, { global: { stubs } })
      const code = wrapper.get('input[placeholder="6位验证码"]')
      expect(code.attributes('autocomplete')).toBe('one-time-code')
      expect(code.attributes('inputmode')).toBe('numeric')
      expect(wrapper.get('input[placeholder="计算结果"]').attributes('autocomplete')).toBe('off')
      const identity = wrapper.find('input[type="email"]').exists()
        ? wrapper.get('input[type="email"]') : wrapper.get('input[placeholder="请输入QQ号"]')
      expect(identity.attributes('autocomplete')).toBe('username')
    })
  })
}
