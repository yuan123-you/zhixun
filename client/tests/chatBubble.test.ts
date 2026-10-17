import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, shallowMount, type VueWrapper } from '@vue/test-utils'
import ChatBubble from '@/components/chat/ChatBubble.vue'

let wrapper: VueWrapper | undefined
let fetchResource: ReturnType<typeof vi.fn>

beforeEach(() => {
  // Exercise the real cache, replacing only browser storage and networking.
  vi.stubGlobal('caches', { open: async () => ({ match: async () => undefined }) })
  fetchResource = vi.fn(async () => new Response(null, { status: 401 }))
  vi.stubGlobal('fetch', fetchResource)
})

afterEach(() => {
  wrapper?.unmount()
  wrapper = undefined
  vi.unstubAllGlobals()
})

function renderMessage(messageType: string, content: string) {
  return shallowMount(ChatBubble, { props: { messageType, content, isMine: false } })
}

describe('chat message image caching', () => {
  it.each([
    ['text', '@AI助手 hello'],
    ['text', 'hello'],
    ['ai_reply', '你好！我是知讯群组的AI助手。'],
    ['text', '微信小程序搜〖游序旅行规划〗'],
    ['file', '{"url":"/api/v1/files/view/chat/report.pdf","name":"report.pdf"}'],
    ['voice', '{"url":"/api/v1/files/view/chat/voice.webm","duration":3}'],
  ])('does not fetch %s message content as an image: %s', async (type, content) => {
    wrapper = renderMessage(type, content)
    await flushPromises()
    expect(fetchResource).not.toHaveBeenCalled()
    if (type === 'text' || type === 'ai_reply') expect(wrapper.text()).toContain(content)
  })

  it('still resolves and fetches actual image messages', async () => {
    wrapper = renderMessage('image', 'http://minio:9000/chat/photo.jpg')
    await flushPromises()
    expect(fetchResource).toHaveBeenCalledWith('/api/v1/files/view/chat/photo.jpg', {
      mode: 'cors', credentials: 'omit',
    })
    expect(wrapper.get('img').attributes('src')).toBe('/api/v1/files/view/chat/photo.jpg')
  })

  it('starts and stops image caching when the message type changes', async () => {
    wrapper = renderMessage('text', 'hello again')
    await flushPromises()
    expect(fetchResource).not.toHaveBeenCalled()
    await wrapper.setProps({ messageType: 'image', content: '/api/v1/files/view/chat/next.jpg' })
    await flushPromises()
    expect(fetchResource).toHaveBeenCalledTimes(1)
    expect(wrapper.get('img').attributes('src')).toBe('/api/v1/files/view/chat/next.jpg')
    await wrapper.setProps({ messageType: 'text', content: 'back to text' })
    await flushPromises()
    expect(fetchResource).toHaveBeenCalledTimes(1)
    expect(wrapper.find('img').exists()).toBe(false)
    expect(wrapper.text()).toContain('back to text')
  })
})

it('does not render or fetch an image when resource resolution rejects its URL', async () => {
  wrapper = renderMessage('image', '/minio/')
  await flushPromises()
  expect(fetchResource).not.toHaveBeenCalled()
  expect(wrapper.find('img').exists()).toBe(false)
})
