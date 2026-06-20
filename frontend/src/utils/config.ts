/**
 * 后端 API 地址
 * - 开发者工具模拟器：127.0.0.1
 * - 真机调试/预览：本机局域网 IP（手机与电脑需同一网络）
 *
 * 换 Wi-Fi / 热点后 IP 会变，可在登录页「开发设置」里改，或执行 `ipconfig getifaddr en0`
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

export function getApiBaseUrl() {
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
export const USE_WECHAT_LOGIN = true
export const REQUEST_TIMEOUT = 60000
