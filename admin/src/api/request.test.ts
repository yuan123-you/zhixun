import axios, { AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import service from './request'
import { sessionStore, STORAGE_KEYS } from '@/utils/storage'

vi.mock('element-plus', () => ({ ElMessage: vi.fn(), ElMessageBox: { alert: vi.fn().mockResolvedValue(undefined) } }))
vi.mock('@/router', () => ({ default: { push: vi.fn() } }))

const originalAdapter = axios.defaults.adapter
const originalServiceAdapter = service.defaults.adapter

function response(config: InternalAxiosRequestConfig, data: unknown, status = 200): AxiosResponse {
  return { config, data, status, statusText: String(status), headers: {} }
}

function httpError(config: InternalAxiosRequestConfig, status: number) {
  return new AxiosError('Request failed', 'ERR_BAD_RESPONSE', config, undefined, response(config, { message: 'failed' }, status))
}

beforeEach(() => {
  sessionStorage.clear()
  vi.useFakeTimers()
})
afterEach(() => {
  axios.defaults.adapter = originalAdapter
  service.defaults.adapter = originalServiceAdapter
  vi.useRealTimers()
  vi.clearAllMocks()
})

describe('automatic transport retries', () => {
  it.each(['post', 'put', 'patch', 'delete'])('never replays a failed %s write', async (method) => {
    let attempts = 0
    service.defaults.adapter = async (config) => {
      attempts++
      throw httpError(config, 500)
    }
    const result = service.request({ method, url: '/articles', data: { title: 'once' } }).catch((error) => error)
    await vi.runAllTimersAsync()
    expect(await result).toBeInstanceOf(AxiosError)
    expect(attempts).toBe(1)
  })

  it('does not replay a timed-out write which may already have committed', async () => {
    let attempts = 0
    service.defaults.adapter = async (config) => {
      attempts++
      throw new AxiosError('timeout', 'ECONNABORTED', config)
    }
    const result = service.post('/comments', { content: 'once' }).catch((error) => error)
    await vi.runAllTimersAsync()
    expect(await result).toBeInstanceOf(AxiosError)
    expect(attempts).toBe(1)
  })

  it.each(['get', 'head'])('still retries a transient %s failure and returns the successful response', async (method) => {
    let attempts = 0
    service.defaults.adapter = async (config) => {
      if (++attempts < 3) throw httpError(config, 503)
      return response(config, { code: 0, data: 'loaded' })
    }
    const result = service.request({ method, url: '/articles' })
    await vi.runAllTimersAsync()
    expect(await result).toEqual({ code: 0, data: 'loaded' })
    expect(attempts).toBe(3)
  })
})

describe('bounded token refresh', () => {
  function login() {
    sessionStore.set(STORAGE_KEYS.TOKEN, 'old-access')
    sessionStore.set(STORAGE_KEYS.REFRESH_TOKEN, 'old-refresh')
  }

  it.each(['http', 'business'])('stops after the refreshed token is rejected (%s 401)', async (kind) => {
    login()
    let refreshes = 0
    let requests = 0
    axios.defaults.adapter = async (config) => {
      // A bounded provider prevents the unfixed implementation from looping forever.
      if (++refreshes > 2) throw httpError(config, 500)
      return response(config, { code: 0, data: { accessToken: 'new-access', refreshToken: 'new-refresh', expiresIn: 3600 } })
    }
    service.defaults.adapter = async (config) => {
      requests++
      if (kind === 'http') throw httpError(config, 401)
      return response(config, { code: 401, message: 'not authenticated' })
    }
    const result = await service.get('/articles').catch((error) => error)
    expect(result).toBeInstanceOf(Error)
    expect(refreshes).toBe(1)
    expect(requests).toBe(2)
    expect(sessionStore.get(STORAGE_KEYS.TOKEN)).toBeNull()
    expect(sessionStore.get(STORAGE_KEYS.REFRESH_TOKEN)).toBeNull()
  })

  it('shares one refresh across concurrent 401 responses and returns both results', async () => {
    login()
    let refreshes = 0
    axios.defaults.adapter = async (config) => {
      refreshes++
      await Promise.resolve()
      return response(config, { code: 0, data: { accessToken: 'new-access', refreshToken: 'new-refresh', expiresIn: 3600 } })
    }
    service.defaults.adapter = async (config) => {
      if (config.headers.Authorization !== 'Bearer new-access') throw httpError(config, 401)
      return response(config, { code: 0, data: config.url })
    }
    const results = await Promise.all([service.get('/first'), service.get('/second')])
    expect(results).toEqual([{ code: 0, data: '/first' }, { code: 0, data: '/second' }])
    expect(refreshes).toBe(1)
  })
})
