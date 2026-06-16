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
