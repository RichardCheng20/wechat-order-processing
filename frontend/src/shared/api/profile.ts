import { request } from './request'

export interface BossProfile {
  merchantName: string
  region?: string
  contactName: string
  phone?: string
}

export function fetchBossProfile() {
  return request<BossProfile>({
    url: '/api/boss/profile',
    method: 'GET',
  })
}

export function updateBossProfile(data: BossProfile) {
  return request<BossProfile>({
    url: '/api/boss/profile',
    method: 'PUT',
    data,
  })
}

export interface StallOrderQrResult {
  merchantId: number
  merchantName?: string
  loginPath: string
  miniProgramName?: string
  qrCodeBase64?: string | null
  qrErrorHint?: string | null
}

export function fetchStallOrderQrcode() {
  return request<StallOrderQrResult>({
    url: '/api/boss/merchant/order-qrcode',
    method: 'GET',
  })
}
