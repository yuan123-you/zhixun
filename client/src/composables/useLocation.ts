/**
 * 自动定位 composable
 *
 * 统一封装基于腾讯地图 IP 定位 / 浏览器 GPS 逆地理编码的自动定位流程，
 * 供 editor、user/edit、share 等需要地理位置的模块复用。
 *
 * 主要能力：
 *  - locate()：复合定位（GPS 优先 + IP 降级），返回标准 LocationResult；
 *  - locateByIP(ip?)：仅走腾讯地图 IP 定位；
 *  - 统一的 loading / error 文案与回退策略；
 *  - 通过 store 缓存最近一次的定位结果，避免短时间内重复请求。
 */
import { ref, computed } from 'vue'
import { getRegionByIP, reverseGeocode, type LocationResult } from '@/utils/regions'

/** 定位步骤（用于 UI 展示更细粒度的提示） */
export type LocateStage = 'gps' | 'reverse' | 'ip' | 'idle'

/** 定位错误码 */
export enum LocateErrorCode {
  /** 用户拒绝授权 */
  PERMISSION_DENIED = 'PERMISSION_DENIED',
  /** 浏览器不支持 */
  NOT_SUPPORTED = 'NOT_SUPPORTED',
  /** 浏览器/GPS 定位超时 */
  TIMEOUT = 'TIMEOUT',
  /** 网络异常或接口返回错误 */
  NETWORK = 'NETWORK',
  /** 解析结果不可用（省市区都为空） */
  INVALID_RESULT = 'INVALID_RESULT',
  /** 未知错误 */
  UNKNOWN = 'UNKNOWN',
}

export interface LocateError {
  code: LocateErrorCode
  message: string
}

const LOCATE_TIMEOUT = 10000

/** 简易结果缓存：相同 ip 在 TTL 内直接复用 */
const CACHE_TTL = 5 * 60 * 1000
const cache = new Map<string, { value: LocationResult | null; expire: number }>()

function cacheKey(ip?: string) {
  return ip ? `ip:${ip}` : 'caller'
}

function readCache(ip?: string): LocationResult | null | undefined {
  const k = cacheKey(ip)
  const hit = cache.get(k)
  if (!hit) return undefined
  if (hit.expire < Date.now()) {
    cache.delete(k)
    return undefined
  }
  return hit.value
}

function writeCache(value: LocationResult | null, ip?: string) {
  cache.set(cacheKey(ip), { value, expire: Date.now() + CACHE_TTL })
}

/**
 * 错误码到用户友好文案的映射。
 * 同一份定位错误，前端展示时不要把"PERMISSION_DENIED"这类技术术语
 * 直接甩给用户，要给出他们看得懂的解释和下一步操作。
 */
const FRIENDLY_MESSAGES: Record<LocateErrorCode, string> = {
  [LocateErrorCode.PERMISSION_DENIED]: '已拒绝位置权限授权，无法自动定位，请在下方手动选择',
  [LocateErrorCode.NOT_SUPPORTED]: '当前浏览器不支持自动定位，请在下方手动选择',
  [LocateErrorCode.TIMEOUT]: '定位耗时过长，请稍后重试或手动选择',
  [LocateErrorCode.NETWORK]: '定位服务连接失败，请检查网络后重试',
  [LocateErrorCode.INVALID_RESULT]: '定位服务暂未返回有效结果，请手动选择发布位置',
  [LocateErrorCode.UNKNOWN]: '定位失败，请手动选择发布位置',
}

function friendlyMessage(code: LocateErrorCode, fallback: string): string {
  return FRIENDLY_MESSAGES[code] || fallback || FRIENDLY_MESSAGES[LocateErrorCode.UNKNOWN]
}

/**
 * 通过浏览器 Geolocation API 获取经纬度。
 * 失败抛出 LocateError，由调用方决定是否降级。
 */
function fetchGPSCoords(): Promise<{ lat: number; lng: number }> {
  return new Promise((resolve, reject) => {
    if (typeof navigator === 'undefined' || !('geolocation' in navigator)) {
      reject({ code: LocateErrorCode.NOT_SUPPORTED, message: friendlyMessage(LocateErrorCode.NOT_SUPPORTED, '浏览器不支持定位') } as LocateError)
      return
    }
    navigator.geolocation.getCurrentPosition(
      pos => resolve({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
      err => {
        let code: LocateErrorCode = LocateErrorCode.UNKNOWN
        switch (err.code) {
          case err.PERMISSION_DENIED:
            code = LocateErrorCode.PERMISSION_DENIED
            break
          case err.POSITION_UNAVAILABLE:
            code = LocateErrorCode.NETWORK
            break
          case err.TIMEOUT:
            code = LocateErrorCode.TIMEOUT
            break
        }
        reject({ code, message: friendlyMessage(code, '获取位置失败') } as LocateError)
      },
      { enableHighAccuracy: true, timeout: LOCATE_TIMEOUT, maximumAge: 120000 },
    )
  })
}

export function useLocation() {
  const loading = ref(false)
  const stage = ref<LocateStage>('idle')
  const lastError = ref<LocateError | null>(null)
  const lastResult = ref<LocationResult | null>(null)

  const errorMessage = computed(() => lastError.value?.message || '')

  function setStage(s: LocateStage) {
    stage.value = s
  }

  function setError(err: LocateError | null) {
    lastError.value = err
  }

  /**
   * 复合定位：GPS + 逆地理编码（首选） → 腾讯 IP 定位（降级）。
   *  - options.ip：可选，若提供则在 IP 定位阶段显式传入；
   *  - options.skipGPS=true：跳过浏览器定位步骤；
   *  - options.useCache=true：先查缓存再请求；
   *  返回 null 表示完全失败，UI 端需引导用户手动选择。
   */
  async function locate(options: { ip?: string; skipGPS?: boolean; useCache?: boolean } = {}): Promise<LocationResult | null> {
    loading.value = true
    setError(null)
    setStage(options.skipGPS ? 'ip' : 'gps')

    const cacheEnabled = options.useCache !== false
    if (cacheEnabled) {
      const cached = readCache(options.ip)
      if (cached !== undefined) {
        lastResult.value = cached
        loading.value = false
        setStage('idle')
        return cached
      }
    }

    try {
      // 1) 浏览器 GPS + 逆地理编码
      if (!options.skipGPS) {
        try {
          const { lat, lng } = await fetchGPSCoords()
          setStage('reverse')
          const geo = await reverseGeocode(lat, lng)
          if (geo?.province) {
            const result: LocationResult = {
              province: geo.province,
              city: geo.city,
              district: geo.district,
              lat,
              lng,
            }
            lastResult.value = result
            writeCache(result, options.ip)
            return result
          }
        } catch (err) {
          // 仅记录，不直接报错，继续走 IP 定位降级
          if (process.env.NODE_ENV !== 'production') {
            console.warn('[useLocation] GPS 定位失败，降级到 IP 定位:', err)
          }
        }
      }

      // 2) 腾讯地图 IP 定位降级
      setStage('ip')
      const ipResult = await getRegionByIP(options.ip)
      if (!ipResult || (!ipResult.province && !ipResult.city)) {
        setError({ code: LocateErrorCode.INVALID_RESULT, message: friendlyMessage(LocateErrorCode.INVALID_RESULT, '定位服务暂未返回有效结果') })
        writeCache(null, options.ip)
        return null
      }
      lastResult.value = ipResult
      writeCache(ipResult, options.ip)
      return ipResult
    } catch (err: any) {
      setError({ code: LocateErrorCode.NETWORK, message: friendlyMessage(LocateErrorCode.NETWORK, err?.message || '定位失败，请稍后重试') })
      return null
    } finally {
      loading.value = false
      setStage('idle')
    }
  }

  /** 仅走腾讯地图 IP 定位，跳过 GPS。适合已登录但需快速展示 IP 属地的场景。 */
  async function locateByIP(ip?: string): Promise<LocationResult | null> {
    return locate({ ip, skipGPS: true, useCache: true })
  }

  /** 清除缓存（用户主动切换地区后调用，避免缓存覆盖新选择） */
  function clearCache(ip?: string) {
    if (ip) cache.delete(cacheKey(ip))
    else cache.clear()
  }

  return {
    loading,
    stage,
    lastError,
    lastResult,
    errorMessage,
    locate,
    locateByIP,
    clearCache,
  }
}
