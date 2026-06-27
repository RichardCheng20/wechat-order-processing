const MERCHANT_KEY = 'entry_merchant_id'
const ACTIVATION_KEY = 'entry_activation_token'
const INVITE_KEY = 'entry_invite_code'
const REGISTER_KEY = 'entry_register_token'

export interface EntryContext {
  merchantId?: number
  activationToken?: string
  inviteCode?: string
  registerToken?: string
}

function storeRegisterToken(value?: string) {
  if (value) {
    uni.setStorageSync(REGISTER_KEY, String(value).trim().toUpperCase())
  }
}

function parseScenePairs(scene: string) {
  const decoded = decodeURIComponent(scene)
  decoded.split(/[,&]/).forEach((part) => {
    const idx = part.indexOf('=')
    if (idx <= 0) return
    const key = part.slice(0, idx).trim()
    const value = part.slice(idx + 1).trim()
    if (key === 'm') {
      const id = Number(value)
      if (!Number.isNaN(id) && id > 0) {
        uni.setStorageSync(MERCHANT_KEY, id)
      }
    } else if (key === 'r' || key === 'reg') {
      storeRegisterToken(value)
    } else if (key === 'act') {
      uni.setStorageSync(ACTIVATION_KEY, value)
    } else if (key === 'code') {
      uni.setStorageSync(INVITE_KEY, value.toUpperCase())
    }
  })
}

export function applyEntryQuery(query?: Record<string, string | undefined>) {
  if (!query) return
  if (query.scene) {
    parseScenePairs(String(query.scene))
  }
  if (query.m) {
    const id = Number(query.m)
    if (!Number.isNaN(id) && id > 0) {
      uni.setStorageSync(MERCHANT_KEY, id)
    }
  }
  if (query.act) {
    uni.setStorageSync(ACTIVATION_KEY, String(query.act).trim())
  }
  if (query.code) {
    uni.setStorageSync(INVITE_KEY, String(query.code).trim().toUpperCase())
  }
  if (query.r || query.reg) {
    storeRegisterToken(String(query.r || query.reg))
  }
}

export function captureLaunchEntry(options?: { query?: Record<string, string> }) {
  applyEntryQuery(options?.query)
}

export function getEntryContext(): EntryContext {
  const merchantRaw = uni.getStorageSync(MERCHANT_KEY)
  const merchantId = merchantRaw ? Number(merchantRaw) : undefined
  const activationToken = (uni.getStorageSync(ACTIVATION_KEY) as string) || undefined
  const inviteCode = (uni.getStorageSync(INVITE_KEY) as string) || undefined
  const registerToken = (uni.getStorageSync(REGISTER_KEY) as string) || undefined
  return {
    merchantId: merchantId && !Number.isNaN(merchantId) ? merchantId : undefined,
    activationToken,
    inviteCode,
    registerToken,
  }
}

export function clearEntryActivation() {
  uni.removeStorageSync(ACTIVATION_KEY)
}

export function clearEntryInvite() {
  uni.removeStorageSync(INVITE_KEY)
}

export function clearEntryRegister() {
  uni.removeStorageSync(REGISTER_KEY)
}

export function setEntryMerchantId(merchantId: number) {
  uni.setStorageSync(MERCHANT_KEY, merchantId)
}
