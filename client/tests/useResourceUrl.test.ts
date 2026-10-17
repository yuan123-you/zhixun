import { describe, expect, it } from 'vitest'
import { useResourceUrl } from '@/composables/useResourceUrl'

const { resolveUrl } = useResourceUrl()

describe('resource URLs', () => {
  it('keeps reachable generated cover and avatar URLs instead of blacklisting their host', () => {
    const url = 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=AI&image_size=landscape_16_9'
    expect(resolveUrl(url)).toBe(url)
  })

  it.each([
    ['http://minio:9000/zhixun/covers/a.jpg', '/api/v1/files/view/zhixun/covers/a.jpg'],
    ['/minio/zhixun/covers/a.jpg', '/api/v1/files/view/zhixun/covers/a.jpg'],
    ['https://picsum.photos/seed/art99/800/450', 'https://picsum.photos/seed/art99/800/450'],
    ['data:image/png;base64,AA==', 'data:image/png;base64,AA=='],
    [null, null],
    ['', null],
    ['/minio/', null],
    ['http://localhost:9001/bucket/image.jpg', null],
  ])('preserves existing resolution for %s', (input, expected) => {
    expect(resolveUrl(input)).toBe(expected)
  })
})
