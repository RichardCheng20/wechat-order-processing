import { request } from './request'

export interface LoginResult {
  token: string
  userId: number
  role: string
  nickname: string
  merchantId: number
  customerId?: number
  openid?: string
}

export function wechatLogin(code: string) {
  return request<LoginResult>({
    url: '/api/auth/wechat-login',
    method: 'POST',
    data: { code },
  })
}

export function devLogin(data: { openid: string; nickname?: string; role?: string }) {
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
