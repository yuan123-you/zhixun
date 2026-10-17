import { describe, expect, it } from 'vitest'
import { useChatMedia } from '@/composables/chat/useChatMedia'

const { getVoiceUrl, getFileUrl, resolveMsgUrl, getVoiceDuration } = useChatMedia()

for (const [name, parse] of [['voice', getVoiceUrl], ['file', getFileUrl]] as const) {
  describe(`${name} resource parsing`, () => {
    it.each(['/minio/', 'http://storage:9001/bucket/image.png', 'https://storage:9001/bucket/image.png'])
      ('does not resurrect a rejected resource: %s', input => {
        expect(parse(input)).toBe('')
        expect(parse(JSON.stringify({ url: input }))).toBe('')
      })
    it.each(['{}', 'null', '42', '[]', '{"url":42}', '{"url":null}', '{"duration":3}', '{"url":'])
      ('does not turn malformed metadata into a URL: %s', input => {
        expect(parse(input)).toBe('')
      })
    it.each([
      ['http://minio:9000/chat/media.webm', '/api/v1/files/view/chat/media.webm'],
      ['{"url":"/minio/chat/media.webm","duration":3}', '/api/v1/files/view/chat/media.webm'],
      ['https://example.com/media.webm', 'https://example.com/media.webm'],
      ['files/view/chat/relative.webm', '/api/v1/files/view/chat/relative.webm'],
      ['blob:local-media', 'blob:local-media'],
    ])('preserves a valid resource %s', (input, expected) => {
      expect(parse(input)).toBe(expected)
    })
  })
}

it('keeps rejected image URLs empty', () => {
  expect(resolveMsgUrl('/minio/')).toBe('')
})

it('keeps the duration of valid voice metadata', () => {
  expect(getVoiceDuration('{"url":"/minio/chat/voice.webm","duration":3}')).toBe(3)
  expect(getVoiceDuration('not json')).toBe(0)
})
