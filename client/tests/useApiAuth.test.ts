import axios, { AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useApi } from '@/composables/useApi'
import { navigateTo } from '@/composables/navigateTo'

vi.mock('@/composables/navigateTo', () => ({ navigateTo: vi.fn().mockResolvedValue(undefined) }))

const originalAdapter = axios.defaults.adapter
const api = useApi()
const originalInstanceAdapter = api.instance.defaults.adapter

function response(config: InternalAxiosRequestConfig, data: unknown, status = 200): AxiosResponse {
  return { config, data, status, statusText: String(status), headers: {} }
}
function unauthorized(config: InternalAxiosRequestConfig) {
  return new AxiosError('Not authenticated', 'ERR_BAD_RESPONSE', config, undefined, response(config, { code: 401 }, 401))
}
function login() {
  localStorage.setItem('zhixun_accessToken', JSON.stringify({ value: 'old-access' }))
  localStorage.setItem('zhixun_refreshToken', JSON.stringify({ value: 'old-refresh' }))
}

beforeEach(() => {
  localStorage.clear()
  vi.useFakeTimers()
  login()
})
afterEach(() => {
  axios.defaults.adapter = originalAdapter
  api.instance.defaults.adapter = originalInstanceAdapter
  vi.runOnlyPendingTimers()
  vi.useRealTimers()
  vi.clearAllMocks()
})

describe('bounded client authentication recovery', () => {
  it.each(['http', 'business'])('does not loop when the refreshed token also receives a %s 401', async (kind) => {
    let refreshes = 0
    let requests = 0
    axios.defaults.adapter = async (config) => {
      if (++refreshes > 2) throw new AxiosError('refresh rejected', 'ERR_NETWORK', config)
      return response(config, { code: 0, data: { accessToken: 'new-access', refreshToken: 'new-refresh', expiresIn: 3600 } })
    }
    api.instance.defaults.adapter = async (config) => {
      requests++
      if (kind === 'http') throw unauthorized(config)
      return response(config, { code: 401, message: 'Not authenticated' })
    }
    const result = await api.get('/articles').catch((error) => error)
    expect(result).toBeInstanceOf(Error)
    expect(refreshes).toBe(1)
    expect(requests).toBe(2)
    expect(localStorage.getItem('zhixun_accessToken')).toBeNull()
    expect(localStorage.getItem('zhixun_refreshToken')).toBeNull()
    expect(navigateTo).toHaveBeenCalledTimes(1)
  })

  it('shares a successful refresh and resolves both concurrent requests', async () => {
    let refreshes = 0
    axios.defaults.adapter = async (config) => {
      refreshes++
      await Promise.resolve()
      return response(config, { code: 0, data: { accessToken: 'new-access', refreshToken: 'new-refresh', expiresIn: 3600 } })
    }
    api.instance.defaults.adapter = async (config) => {
      if (config.headers.Authorization !== 'Bearer new-access') throw unauthorized(config)
      return response(config, { code: 0, data: config.url })
    }
    const results = await Promise.all([api.get('/first'), api.get('/second')])
    expect(results.map((item) => item.data.data)).toEqual(['/first', '/second'])
    expect(refreshes).toBe(1)
  })

  it('does not refresh or redirect for an already expired logout', async () => {
    let refreshes = 0
    axios.defaults.adapter = async (config) => { refreshes++; throw unauthorized(config) }
    api.instance.defaults.adapter = async (config) => { throw unauthorized(config) }
    await expect(api.post('/auth/logout')).rejects.toThrow('已退出登录')
    expect(refreshes).toBe(0)
    expect(navigateTo).not.toHaveBeenCalled()
  })
})
