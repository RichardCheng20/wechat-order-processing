/**
 * 后端 API 地址
 * - 微信小程序开发者工具：127.0.0.1
 * - 微信小程序真机：局域网 IP（登录页可改）
 * - H5 本地 dev：空字符串，走 Vite 代理 /api → 8080
 * - H5 生产：构建时设置 VITE_API_BASE_URL，或 Nginx 同域反代留空
 */
export const DEV_API_HOST_STORAGE_KEY = 'dev_api_host'
const DEV_LAN_HOST = '172.20.10.2'
const DEV_API_PORT = 8080

export function getDevApiHostOverride(): string {
  try {
    const stored = uni.getStorageSync(DEV_API_HOST_STORAGE_KEY) as string
    if (stored?.trim()) return stored.trim()
  } catch {
    // ignore
  }
  return ''
}

export function setDevApiHostOverride(host: string) {
  const trimmed = host.trim()
  if (!trimmed) {
    uni.removeStorageSync(DEV_API_HOST_STORAGE_KEY)
    return
  }
  uni.setStorageSync(DEV_API_HOST_STORAGE_KEY, trimmed)
}

function envApiBaseUrl(): string {
  const raw = import.meta.env.VITE_API_BASE_URL
  return typeof raw === 'string' ? raw.trim().replace(/\/$/, '') : ''
}

export function getApiBaseUrl() {
  const envUrl = envApiBaseUrl()
  if (envUrl) {
    return envUrl
  }

  // #ifdef H5
  const h5Override = getDevApiHostOverride()
  if (h5Override) {
    return h5Override.replace(/\/$/, '')
  }
  return ''
  // #endif

  // #ifdef MP-WEIXIN
  try {
    const sys = uni.getSystemInfoSync()
    if (sys.platform === 'devtools') {
      return `http://127.0.0.1:${DEV_API_PORT}`
    }
  } catch {
    // ignore
  }
  // #endif

  const host = getDevApiHostOverride() || DEV_LAN_HOST
  return `http://${host}:${DEV_API_PORT}`
}

/** @deprecated 请用 getApiBaseUrl()，真机调试时地址可能被用户覆盖 */
export const API_BASE_URL = getApiBaseUrl()

export const SHOW_DEV_LOGIN = true
export const USE_WECHAT_LOGIN = import.meta.env.UNI_PLATFORM !== 'h5'

export const REQUEST_TIMEOUT = 60000
