import { request } from './request'

export interface LoginResult {
  token: string
  userId: number
  role: string
  nickname: string
  merchantId: number
  merchantName?: string
  customerId?: number
  customerName?: string
  openid?: string
  workerId?: number
  workerCode?: string
}

export interface LoginEntryPreview {
  merchantId: number
  merchantName: string
  entryType: 'MERCHANT_ONBOARD' | 'STAFF' | 'CUSTOMER' | 'NONE'
  entryHint: string
  activationRole?: string
  workerName?: string
}

export interface ActivationTokenResult {
  token: string
  targetRole: string
  targetRoleLabel: string
  workerId?: number
  workerName?: string
  expiredAt?: string
  loginPath?: string
}

export function fetchLoginEntryPreview(options?: {
  merchantId?: number
  activationToken?: string
}) {
  return request<LoginEntryPreview>({
    url: '/api/auth/entry-preview',
    method: 'GET',
    query: {
      merchantId: options?.merchantId,
      activationToken: options?.activationToken,
    },
  })
}

export function wechatLogin(data: {
  code: string
  merchantId?: number
  activationToken?: string
  inviteCode?: string
}) {
  return request<LoginResult>({
    url: '/api/auth/wechat-login',
    method: 'POST',
    data,
  })
}

export function devLogin(data: {
  openid: string
  nickname?: string
  role?: string
  merchantId?: number
  activationToken?: string
  inviteCode?: string
}) {
  return request<LoginResult>({
    url: '/api/auth/dev-login',
    method: 'POST',
    data,
  })
}

export function fetchMe() {
  return request<LoginResult>({
    url: '/api/auth/me',
    method: 'GET',
  })
}

export function logout() {
  return request<void>({
    url: '/api/auth/logout',
    method: 'POST',
  })
}

export function generatePersonnelActivationToken(personnelId: number) {
  return request<ActivationTokenResult>({
    url: `/api/boss/personnel/${personnelId}/activation-token`,
    method: 'POST',
  })
}
