import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import FilePreviewDialog from '@/components/chat/FilePreviewDialog.vue'

let wrapper: VueWrapper | undefined
let fetchResource: ReturnType<typeof vi.fn>
const initial = { visible: false, fileUrl: '/files/preview-first.txt', fileName: 'first.txt', fileExt: 'txt' }

beforeEach(() => {
  fetchResource = vi.fn(async () => new Response('current file'))
  vi.stubGlobal('fetch', fetchResource)
})
afterEach(() => {
  wrapper?.unmount()
  wrapper = undefined
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
})
function render(props = initial) {
  wrapper = mount(FilePreviewDialog, { props, global: { stubs: { teleport: true } } })
  return wrapper
}

describe('file preview lifecycle', () => {
  it('loads TXT and handles Escape when initially visible', async () => {
    render({ ...initial, visible: true })
    await flushPromises()
    expect(wrapper!.get('pre').text()).toBe('current file')
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    expect(wrapper!.emitted('close')).toHaveLength(1)
  })

  it('reloads when switching TXT files while the dialog stays open', async () => {
    fetchResource.mockResolvedValueOnce(new Response('first file')).mockResolvedValueOnce(new Response('second file'))
    render()
    await wrapper!.setProps({ visible: true })
    await flushPromises()
    expect(wrapper!.get('pre').text()).toBe('first file')
    await wrapper!.setProps({ fileUrl: '/files/preview-second.txt', fileName: 'second.txt' })
    await flushPromises()
    expect(wrapper!.get('pre').text()).toBe('second file')
  })

  it('does not allow a previously closed preview to overwrite a newly opened file', async () => {
    let finishOld!: (response: Response) => void
    fetchResource.mockImplementationOnce(() => new Promise<Response>(resolve => { finishOld = resolve }))
      .mockResolvedValueOnce(new Response('new file'))
    render()
    await wrapper!.setProps({ visible: true })
    await flushPromises()
    await wrapper!.setProps({ visible: false })
    await wrapper!.setProps({ visible: true, fileUrl: '/files/preview-new.txt', fileName: 'new.txt' })
    await flushPromises()
    expect(wrapper!.get('pre').text()).toBe('new file')
    finishOld(new Response('old file'))
    await flushPromises()
    expect(wrapper!.get('pre').text()).toBe('new file')
  })

  it('removes the Escape listener when unmounted while open', async () => {
    const add = vi.spyOn(window, 'addEventListener')
    const remove = vi.spyOn(window, 'removeEventListener')
    render()
    await wrapper!.setProps({ visible: true })
    await flushPromises()
    const handler = add.mock.calls.find(([event]) => event === 'keydown')![1]
    wrapper!.unmount()
    wrapper = undefined
    expect(remove).toHaveBeenCalledWith('keydown', handler)
  })

  it('does not fetch a resource rejected by the URL resolver', async () => {
    render({ ...initial, fileUrl: '/minio/' })
    await wrapper!.setProps({ visible: true })
    await flushPromises()
    expect(fetchResource).not.toHaveBeenCalled()
    expect(wrapper!.find('.file-preview-error').exists()).toBe(true)
  })

  it('shows a failed fetch without leaving the loading spinner active', async () => {
    fetchResource.mockRejectedValue(new Error('offline'))
    render()
    await wrapper!.setProps({ visible: true })
    await flushPromises()
    expect(wrapper!.find('.file-preview-error').exists()).toBe(true)
    expect(wrapper!.find('.file-preview-loading').exists()).toBe(false)
  })
})
