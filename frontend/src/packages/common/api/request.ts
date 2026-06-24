import { getApiBaseUrl, REQUEST_TIMEOUT } from '@common/utils/config'

export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}

function appendQuery(url: string, params?: Record<string, string | number | undefined | null>) {
  if (!params) return url
  const query = Object.entries(params)
    .filter(([, value]) => value !== undefined && value !== null && value !== '')
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
    .join('&')
  if (!query) return url
  return `${url}${url.includes('?') ? '&' : '?'}${query}`
}

function normalizeFailError(err: UniApp.GeneralCallbackResult) {
  const base = getApiBaseUrl()
  const raw = `${err.errMsg || ''}`
  if (raw.includes('timeout')) {
    return new Error(`连接超时(${base})`)
  }
  return new Error(`网络错误(${base})`)
}

function doRequest<T>(options: UniApp.RequestOptions): Promise<T> {
  const token = uni.getStorageSync('token') as string

  return new Promise((resolve, reject) => {
    uni.request({
      ...options,
      timeout: REQUEST_TIMEOUT,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.header,
      },
      success: (res) => {
        if (!res.statusCode || res.statusCode >= 500) {
          reject(new Error('服务器内部错误，请确认后端已启动'))
          return
        }
        const body = res.data as ApiResult<T>
        if (body && body.code === 0) {
          resolve(body.data as T)
          return
        }
        reject(new Error(body?.message || '请求失败'))
      },
      fail: (err) => {
        reject(normalizeFailError(err))
      },
    })
  })
}

export function request<T>(options: UniApp.RequestOptions & {
  query?: Record<string, string | number | undefined | null>
  retry?: number
}) {
  const { query, data, retry = 0, ...rest } = options
  const url = appendQuery(`${getApiBaseUrl()}${options.url}`, query)
  const payload = {
    ...rest,
    url,
    data: rest.method === 'GET' ? undefined : data,
  }

  const run = (left: number): Promise<T> =>
    doRequest<T>(payload).catch((error) => {
      if (left > 0 && error instanceof Error && error.message.includes('连接超时')) {
        return run(left - 1)
      }
      throw error
    })

  return run(retry)
}
