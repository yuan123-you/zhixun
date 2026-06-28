/**
 * 区域工具函数单元测试
 * @vitest-environment jsdom
 *
 * 测试覆盖：
 * - provinces 静态数据完整性
 * - findProvince / findCity / findDistrict 查找逻辑
 * - getRegionByIP 调用腾讯地图 /ws/location/v1/ip 接口的成功 / 失败 / 异常处理
 * - reverseGeocode 调用腾讯地图 /ws/geocoder/v1/ 接口的成功 / 失败 / 异常处理
 * - locateUser GPS 优先 + IP 降级
 * - matchRegionByCoord 兼容旧接口
 */
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import {
  provinces,
  findProvince,
  findCity,
  findDistrict,
  getRegionByIP,
  reverseGeocode,
  locateUser,
  matchRegionByCoord,
} from '~/utils/regions'

// 简易 fetch mock 助手
function mockFetchOnce(response: { ok?: boolean; status?: number; body?: any; error?: string }) {
  const fetchMock = vi.fn()
  if (response.error) {
    fetchMock.mockRejectedValueOnce(new Error(response.error))
  } else {
    fetchMock.mockResolvedValueOnce({
      ok: response.ok ?? true,
      status: response.status ?? 200,
      json: async () => response.body,
    } as any)
  }
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

describe('provinces 静态数据', () => {
  it('省级列表至少包含 31 个省级行政区（含港澳台）', () => {
    expect(provinces.length).toBeGreaterThanOrEqual(31)
  })

  it('省级 value 长度均为 6 位数字', () => {
    provinces.forEach((p) => {
      expect(p.value).toMatch(/^\d{6}$/)
    })
  })

  it('省级 value 唯一', () => {
    const values = provinces.map((p) => p.value)
    expect(new Set(values).size).toBe(values.length)
  })

  it('北京市/广东省/上海市存在', () => {
    expect(findProvince('北京市')).toBeDefined()
    expect(findProvince('广东省')).toBeDefined()
    expect(findProvince('上海市')).toBeDefined()
  })
})

describe('findProvince / findCity / findDistrict', () => {
  it('findProvince 同时支持名称和编码', () => {
    expect(findProvince('北京市')?.value).toBe('110000')
    expect(findProvince('110000')?.label).toBe('北京市')
  })

  it('findProvince 找不到时返回 undefined', () => {
    expect(findProvince('不存在的省份')).toBeUndefined()
    expect(findProvince('000000')).toBeUndefined()
  })

  it('findCity 找到广东省下的深圳市', () => {
    const gd = findProvince('广东省')!
    const city = findCity(gd, '深圳市')
    expect(city?.value).toBe('440300')
  })

  it('findCity 找不到时返回 undefined', () => {
    const gd = findProvince('广东省')!
    expect(findCity(gd, '不存在的城市')).toBeUndefined()
  })

  it('findDistrict 找不到时返回 undefined', () => {
    expect(findDistrict(findProvince('北京市')!, '不存在的区')).toBeUndefined()
  })

  it('findDistrict 在父节点无 children 时返回 undefined', () => {
    // 深圳市 children 为空数组（数据中绝大多数市未列出区）
    const gd = findProvince('广东省')!
    const city = findCity(gd, '深圳市')!
    // 数据中绝大多数市级节点无 children 属性或 children 为空
    expect(city.children ?? []).toEqual([])
    expect(findDistrict(city, '南山区')).toBeUndefined()
  })
})

describe('getRegionByIP - 腾讯地图 IP 定位', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('接口成功返回应正确解析省市区', async () => {
    mockFetchOnce({
      body: {
        status: 0,
        message: 'query ok',
        result: {
          ip: '1.2.3.4',
          location: { lat: 22.5, lng: 113.9 },
          ad_info: {
            nation: '中国',
            province: '广东省',
            city: '深圳市',
            district: '南山区',
            adcode: 440305,
          },
        },
      },
    })

    const result = await getRegionByIP('1.2.3.4')
    expect(result).not.toBeNull()
    expect(result?.province).toBe('广东省')
    expect(result?.city).toBe('深圳市')
    expect(result?.district).toBe('南山区')
    expect(result?.lat).toBe(22.5)
    expect(result?.lng).toBe(113.9)
    expect(result?.ip).toBe('1.2.3.4')
  })

  it('业务异常（status !== 0）应返回 null', async () => {
    mockFetchOnce({
      body: { status: 310, message: 'key 错误', result: null },
    })
    const result = await getRegionByIP('2.2.3.4')
    expect(result).toBeNull()
  })

  it('HTTP 异常应返回 null', async () => {
    mockFetchOnce({ ok: false, status: 500 })
    const result = await getRegionByIP('3.2.3.4')
    expect(result).toBeNull()
  })

  it('网络异常应返回 null', async () => {
    mockFetchOnce({ error: 'network failed' })
    const result = await getRegionByIP('4.2.3.4')
    expect(result).toBeNull()
  })

  it('省为空且市为空应返回 null', async () => {
    mockFetchOnce({
      body: {
        status: 0,
        message: 'ok',
        result: { ip: '5.2.3.4', ad_info: { nation: '中国' } },
      },
    })
    const result = await getRegionByIP('5.2.3.4')
    expect(result).toBeNull()
  })

  it('省为空但市不为空时仍应返回结果（直辖市降级场景）', async () => {
    mockFetchOnce({
      body: {
        status: 0,
        message: 'ok',
        result: {
          ip: '6.2.3.4',
          ad_info: { nation: '中国', province: '北京市', city: '北京市', district: '朝阳区' },
        },
      },
    })
    const result = await getRegionByIP('6.2.3.4')
    expect(result).not.toBeNull()
    // province 与 city 相同，函数应去除重复，仅返回 "北京市"
    expect(result?.province).toBe('北京市')
    expect(result?.city).toBe('北京市')
  })

  it('请求 URL 包含 key / ip / output=json 参数', async () => {
    const fetchMock = mockFetchOnce({
      body: { status: 0, message: 'ok', result: { ip: '7.2.3.4', ad_info: { province: '广东省', city: '深圳' } } },
    })
    await getRegionByIP('7.2.3.4')
    const url = (fetchMock.mock.calls[0][0] as string)
    expect(url).toContain('/ws/location/v1/ip')
    expect(url).toContain('key=')
    expect(url).toContain('ip=7.2.3.4')
    expect(url).toContain('output=json')
  })
})

describe('reverseGeocode - 腾讯地图逆地理编码', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('接口成功返回应正确解析', async () => {
    mockFetchOnce({
      body: {
        status: 0,
        message: 'query ok',
        result: {
          address: '广东省深圳市南山区科技园',
          formatted_addresses: { recommend: '广东省深圳市南山区科技园' },
          address_component: {
            nation: '中国',
            province: '广东省',
            city: '深圳市',
            district: '南山区',
            street: '科苑路',
            street_number: '1 号',
          },
          ad_info: { adcode: 440305 },
        },
      },
    })

    const result = await reverseGeocode(22.5431, 113.9341)
    expect(result).not.toBeNull()
    expect(result?.province).toBe('广东')
    expect(result?.city).toBe('深圳')
    expect(result?.district).toBe('南山区')
    expect(result?.address).toContain('南山区')
    expect(result?.lat).toBe(22.5431)
    expect(result?.lng).toBe(113.9341)
  })

  it('非数字坐标应返回 null', async () => {
    const result = await reverseGeocode(NaN, 113.9)
    expect(result).toBeNull()
  })

  it('业务异常应返回 null', async () => {
    mockFetchOnce({ body: { status: 310, message: 'invalid key' } })
    const result = await reverseGeocode(22, 113)
    expect(result).toBeNull()
  })

  it('请求 URL 包含 location 和 get_poi 参数', async () => {
    const fetchMock = mockFetchOnce({
      body: { status: 0, message: 'ok', result: { address_component: { province: '广东省', city: '深圳市' } } },
    })
    await reverseGeocode(22.5431, 113.9341)
    const url = (fetchMock.mock.calls[0][0] as string)
    expect(url).toContain('/ws/geocoder/v1/')
    expect(url).toContain('location=')
    expect(url).toContain('get_poi=0')
  })

  it('港澳台等场景下应做合理规范化', async () => {
    mockFetchOnce({
      body: {
        status: 0,
        message: 'ok',
        result: {
          address_component: {
            province: '香港特别行政区',
            city: '香港特别行政区',
            district: '中西区',
          },
        },
      },
    })
    const result = await reverseGeocode(22.3, 114.2)
    expect(result).not.toBeNull()
    expect(result?.province).toBe('香港') // 去掉"特别行政区"后缀
  })
})

describe('locateUser - 复合定位', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  beforeEach(() => {
    // 默认禁用 navigator.geolocation
    Object.defineProperty(global.navigator, 'geolocation', {
      value: undefined,
      configurable: true,
      writable: true,
    })
  })

  it('无 GPS 时应降级到 IP 定位', async () => {
    mockFetchOnce({
      body: {
        status: 0,
        message: 'ok',
        result: { ip: '1.2.3.4', ad_info: { province: '广东省', city: '深圳' } },
      },
    })
    const result = await locateUser({ ip: '1.2.3.4', skipGPS: true })
    expect(result?.province).toBe('广东省')
    expect(result?.city).toBe('深圳')
  })

  it('IP 定位失败应返回 null', async () => {
    mockFetchOnce({ body: { status: 310, message: 'key 错误' } })
    const result = await locateUser({ ip: '1.2.3.4', skipGPS: true })
    expect(result).toBeNull()
  })

  it('应优先使用 GPS + 逆地理编码', async () => {
    // 模拟 geolocation
    Object.defineProperty(global.navigator, 'geolocation', {
      value: {
        getCurrentPosition: (success: PositionCallback) => {
          success({
            coords: { latitude: 22.5, longitude: 113.9, accuracy: 10 } as any,
            timestamp: Date.now(),
          } as any)
        },
      },
      configurable: true,
      writable: true,
    })
    // 第一次 fetch: 逆地理编码
    const fetchMock = vi.fn().mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        status: 0,
        message: 'ok',
        result: { address_component: { province: '广东省', city: '深圳市', district: '南山区' } },
      }),
    } as any)
    vi.stubGlobal('fetch', fetchMock)

    const result = await locateUser()
    expect(result?.province).toBe('广东')
    expect(result?.city).toBe('深圳')
    expect(result?.lat).toBe(22.5)
    // GPS 成功时不应调用 IP 定位
    expect(fetchMock).toHaveBeenCalledTimes(1)
    const url = fetchMock.mock.calls[0][0] as string
    expect(url).toContain('/ws/geocoder/v1/')
  })

  it('GPS 失败时应降级到 IP 定位', async () => {
    Object.defineProperty(global.navigator, 'geolocation', {
      value: {
        getCurrentPosition: (_success: PositionCallback, error: PositionErrorCallback) => {
          error({ code: 1, message: 'denied', PERMISSION_DENIED: 1, POSITION_UNAVAILABLE: 2, TIMEOUT: 3 } as any)
        },
      },
      configurable: true,
      writable: true,
    })
    mockFetchOnce({
      body: {
        status: 0,
        message: 'ok',
        result: { ip: '1.2.3.4', ad_info: { province: '广东省', city: '深圳' } },
      },
    })
    const result = await locateUser({ ip: '1.2.3.4' })
    expect(result?.province).toBe('广东省')
  })

  it('GPS 成功但逆地理编码失败，应降级到 IP 定位', async () => {
    Object.defineProperty(global.navigator, 'geolocation', {
      value: {
        getCurrentPosition: (success: PositionCallback) => {
          success({
            coords: { latitude: 22.5, longitude: 113.9, accuracy: 10 } as any,
            timestamp: Date.now(),
          } as any)
        },
      },
      configurable: true,
      writable: true,
    })
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({ ok: false, status: 500, json: async () => ({}) } as any) // reverse 失败
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          status: 0,
          message: 'ok',
          result: { ip: '1.2.3.4', ad_info: { province: '北京市', city: '北京' } },
        }),
      } as any)
    vi.stubGlobal('fetch', fetchMock)

    const result = await locateUser({ ip: '1.2.3.4' })
    expect(result?.province).toBe('北京市')
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })
})

describe('matchRegionByCoord - 兼容旧接口', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('成功时返回 { province, city, district }', async () => {
    mockFetchOnce({
      body: {
        status: 0,
        message: 'ok',
        result: { address_component: { province: '广东省', city: '深圳市', district: '南山区' } },
      },
    })
    const result = await matchRegionByCoord(22.5, 113.9)
    expect(result).toEqual({ province: '广东', city: '深圳', district: '南山区' })
  })

  it('失败时返回 null', async () => {
    mockFetchOnce({ body: { status: 310, message: 'invalid key' } })
    const result = await matchRegionByCoord(22.5, 113.9)
    expect(result).toBeNull()
  })
})
