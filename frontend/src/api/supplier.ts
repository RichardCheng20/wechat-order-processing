import { request } from './request'

export interface SupplierItem {
  id: number
  name: string
  contactName?: string
  phone?: string
  remark?: string
  status?: number
  createdAt?: string
  outstandingPayable?: number
}

export interface SupplierCreatePayload {
  name: string
  contactName?: string
  phone?: string
  remark?: string
}

export function fetchBossSuppliers(keyword?: string) {
  return request<SupplierItem[]>({
    url: '/api/boss/suppliers',
    method: 'GET',
    query: { keyword },
  })
}

export function createBossSupplier(data: SupplierCreatePayload) {
  return request<SupplierItem>({
    url: '/api/boss/suppliers',
    method: 'POST',
    data,
  })
}
