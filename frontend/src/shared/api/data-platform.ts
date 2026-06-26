import { request } from './request'

export interface DataPlatformPasswordStatus {
  passwordEnabled: boolean
}

export function fetchDataPlatformPasswordStatus() {
  return request<DataPlatformPasswordStatus>({
    url: '/api/boss/data-platform/password/status',
    method: 'GET',
  })
}

export function verifyDataPlatformPassword(password: string) {
  return request<void>({
    url: '/api/boss/data-platform/password/verify',
    method: 'POST',
    data: { password },
  })
}

export function setDataPlatformPassword(password: string, oldPassword?: string) {
  return request<void>({
    url: '/api/boss/data-platform/password',
    method: 'PUT',
    data: { password, oldPassword },
  })
}

export function disableDataPlatformPassword(password: string) {
  return request<void>({
    url: '/api/boss/data-platform/password',
    method: 'DELETE',
    data: { password },
  })
}

export function resetDataPlatformPassword(password: string) {
  return request<void>({
    url: '/api/boss/data-platform/password/reset',
    method: 'POST',
    data: { password },
  })
}
