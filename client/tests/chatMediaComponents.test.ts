import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, shallowMount, type VueWrapper } from '@vue/test-utils'
import VoiceMessage from '@/components/VoiceMessage.vue'
import FileCard from '@/components/chat/FileCard.vue'
import { useResourceUrl } from '@/composables/useResourceUrl'

let wrapper: VueWrapper | undefined
beforeEach(() => {
  vi.stubGlobal('useResourceUrl', useResourceUrl)
  vi.spyOn(HTMLMediaElement.prototype, 'pause').mockImplementation(() => {})
})
afterEach(() => {
  wrapper?.unmount()
  wrapper = undefined
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
})

describe('voice resource rendering', () => {
  it.each(['/minio/', 'https://storage:9001/bucket/voice.webm', '{"url":42}', '{"url":null}', '{broken'])
    ('does not create audio for an invalid URL: %s', url => {
      wrapper = mount(VoiceMessage, { props: { url } })
      expect(wrapper.find('audio').exists()).toBe(false)
    })
  it.each([
    ['http://minio:9000/chat/voice.webm', '/api/v1/files/view/chat/voice.webm'],
    ['{"url":"/minio/chat/voice.webm"}', '/api/v1/files/view/chat/voice.webm'],
    ['blob:recorded-voice', 'blob:recorded-voice'],
  ])('renders valid voice resources: %s', (url, expected) => {
    wrapper = mount(VoiceMessage, { props: { url } })
    expect(wrapper.get('audio').attributes('src')).toBe(expected)
  })
})

describe('file resource actions', () => {
  it('does not navigate or open a preview for invalid metadata', async () => {
    const navigate = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    wrapper = shallowMount(FileCard, { props: { content: '{"name":"broken.zip"}' } })
    await wrapper.get('.chat-file-card').trigger('click')
    expect(navigate).not.toHaveBeenCalled()
    expect(wrapper.emitted('preview')).toBeUndefined()
  })
  it('emits the resolved PDF URL for valid metadata', async () => {
    wrapper = shallowMount(FileCard, {
      props: { content: '{"url":"http://minio:9000/chat/report.pdf","name":"report.pdf","size":1024}' },
    })
    await wrapper.get('.chat-file-card').trigger('click')
    expect(wrapper.emitted('preview')).toEqual([['/api/v1/files/view/chat/report.pdf', 'report.pdf', 'pdf']])
  })
})
