const MERCHANT_KEY = 'entry_merchant_id'
const ACTIVATION_KEY = 'entry_activation_token'
const INVITE_KEY = 'entry_invite_code'

export interface EntryContext {
  merchantId?: number
  activationToken?: string
  inviteCode?: string
}

export function applyEntryQuery(query?: Record<string, string | undefined>) {
  if (!query) return
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
}

export function captureLaunchEntry(options?: { query?: Record<string, string> }) {
  applyEntryQuery(options?.query)
}

export function getEntryContext(): EntryContext {
  const merchantRaw = uni.getStorageSync(MERCHANT_KEY)
  const merchantId = merchantRaw ? Number(merchantRaw) : undefined
  const activationToken = (uni.getStorageSync(ACTIVATION_KEY) as string) || undefined
  const inviteCode = (uni.getStorageSync(INVITE_KEY) as string) || undefined
  return {
    merchantId: merchantId && !Number.isNaN(merchantId) ? merchantId : undefined,
    activationToken,
    inviteCode,
  }
}

export function clearEntryActivation() {
  uni.removeStorageSync(ACTIVATION_KEY)
}

export function clearEntryInvite() {
  uni.removeStorageSync(INVITE_KEY)
}

export function setEntryMerchantId(merchantId: number) {
  uni.setStorageSync(MERCHANT_KEY, merchantId)
}
