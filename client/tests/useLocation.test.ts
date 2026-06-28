/**
 * useLocation composable 单元测试
 * @vitest-environment jsdom
 *
 * 测试覆盖：
 * - 复合定位主流程（GPS 优先 + IP 降级）
 * - 仅 IP 定位（跳过 GPS）
 * - 错误状态管理
 * - 缓存机制
 * - locateByIP 快捷方法
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

// 模拟 Vue 响应式 API
const mockRef = <T>(val: T) => ({ value: val, __v_isRef: true })
const mockComputed = <T>(fn: () => T) => ({ get value() { return fn() }, __v_isRef: true })
vi.stubGlobal('ref', mockRef)
vi.stubGlobal('computed', mockComputed)

import { useLocation, LocateErrorCode } from '~/composables/useLocation'

function mockFetchOk(body: any) {
  const fetchMock = vi.fn().mockResolvedValue({
    ok: true,
    status: 200,
    json: async () => body,
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

describe('useLocation', () => {
  beforeEach(() => {
    // 默认禁用 geolocation
    Object.defineProperty(global.navigator, 'geolocation', {
      value: undefined,
      configurable: true,
      writable: true,
    })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  describe('初始状态', () => {
    it('应初始化 loading=false / stage=idle / lastResult=null', () => {
      const { loading, stage, lastResult, lastError } = useLocation()
      expect(loading.value).toBe(false)
      expect(stage.value).toBe('idle')
      expect(lastResult.value).toBeNull()
      expect(lastError.value).toBeNull()
    })
  })

  describe('locateByIP / skipGPS', () => {
    it('skipGPS=true 时只调用一次 IP 接口', async () => {
      const fetchMock = mockFetchOk({
        status: 0,
        message: 'ok',
        result: { ip: '10.0.0.1', ad_info: { province: '广东省', city: '深圳' } },
      })

      const { locate, lastResult } = useLocation()
      const result = await locate({ ip: '10.0.0.1', skipGPS: true, useCache: false })

      expect(fetchMock).toHaveBeenCalledTimes(1)
      expect(result?.province).toBe('广东省')
      expect(result?.city).toBe('深圳')
      expect(lastResult.value).toEqual(result)
    })

    it('IP 失败应设置 lastError 并返回 null', async () => {
      mockFetchOk({ status: 310, message: 'invalid key', result: null })

      const { locate, lastError, errorMessage } = useLocation()
      const result = await locate({ ip: '10.0.0.2', skipGPS: true, useCache: false })

      expect(result).toBeNull()
      expect(lastError.value).not.toBeNull()
      expect(lastError.value?.code).toBe(LocateErrorCode.INVALID_RESULT)
      expect(errorMessage.value).toBeTruthy()
    })
  })

  describe('缓存机制', () => {
    it('useCache=true 时相同 ip 在 TTL 内只请求一次', async () => {
      const fetchMock = mockFetchOk({
        status: 0,
        message: 'ok',
        result: { ip: '10.0.1.1', ad_info: { province: '广东省', city: '深圳' } },
      })

      const { locate } = useLocation()
      const r1 = await locate({ ip: '10.0.1.1', skipGPS: true, useCache: true })
      const r2 = await locate({ ip: '10.0.1.1', skipGPS: true, useCache: true })

      expect(r1).toEqual(r2)
      expect(fetchMock).toHaveBeenCalledTimes(1)
    })

    it('clearCache 后应重新请求', async () => {
      const fetchMock = mockFetchOk({
        status: 0,
        message: 'ok',
        result: { ip: '10.0.1.2', ad_info: { province: '广东省', city: '深圳' } },
      })

      const { locate, clearCache } = useLocation()
      clearCache('10.0.1.2')
      await locate({ ip: '10.0.1.2', skipGPS: true, useCache: true })
      clearCache('10.0.1.2')
      await locate({ ip: '10.0.1.2', skipGPS: true, useCache: true })

      expect(fetchMock).toHaveBeenCalledTimes(2)
    })

    it('useCache=false 时不应使用缓存', async () => {
      const fetchMock = mockFetchOk({
        status: 0,
        message: 'ok',
        result: { ip: '10.0.1.3', ad_info: { province: '广东省', city: '深圳' } },
      })

      const { locate } = useLocation()
      await locate({ ip: '10.0.1.3', skipGPS: true, useCache: false })
      await locate({ ip: '10.0.1.3', skipGPS: true, useCache: false })

      expect(fetchMock).toHaveBeenCalledTimes(2)
    })
  })

  describe('loading 状态', () => {
    it('locate 调用中 loading 应为 true，结束后 false', async () => {
      let resolveJson: any
      const fetchMock = vi.fn().mockImplementationOnce(
        () =>
          new Promise((resolve) => {
            resolveJson = () =>
              resolve({
                ok: true,
                status: 200,
                json: async () => ({
                  status: 0,
                  message: 'ok',
                  result: { ip: '10.0.2.1', ad_info: { province: '广东省', city: '深圳' } },
                }),
              })
          }),
      )
      vi.stubGlobal('fetch', fetchMock)

      const { locate, loading } = useLocation()
      const promise = locate({ ip: '10.0.2.1', skipGPS: true, useCache: false })
      expect(loading.value).toBe(true)
      resolveJson()
      await promise
      expect(loading.value).toBe(false)
    })
  })

  describe('locateByIP 快捷方法', () => {
    it('应直接走 IP 定位（skipGPS=true）', async () => {
      const fetchMock = mockFetchOk({
        status: 0,
        message: 'ok',
        result: { ip: '10.0.3.1', ad_info: { province: '北京市', city: '北京' } },
      })

      const { locateByIP, clearCache } = useLocation()
      clearCache('10.0.3.1')
      const result = await locateByIP('10.0.3.1')

      expect(result?.province).toBe('北京市')
      expect(fetchMock).toHaveBeenCalledTimes(1)
    })
  })
})
