import { request } from './request'

export interface SupplierItem {
  id: number
  supplierNo?: string
  name: string
  contactName?: string
  phone?: string
  remark?: string
  status?: number
  createdAt?: string
  payableAmount?: number
  paidAmount?: number
  outstandingPayable?: number
}

export interface SupplierCreatePayload {
  name: string
  contactName?: string
  phone?: string
  remark?: string
}

export interface SupplierUpdatePayload {
  name?: string
  contactName?: string
  phone?: string
  remark?: string
  status?: number
}

export function fetchBossSuppliers(keyword?: string) {
  return request<SupplierItem[]>({
    url: '/api/boss/suppliers',
    method: 'GET',
    query: { keyword },
  })
}

export function fetchBossSupplierDetail(id: number) {
  return request<SupplierItem>({
    url: `/api/boss/suppliers/${id}`,
    method: 'GET',
  })
}

export function createBossSupplier(data: SupplierCreatePayload) {
  return request<SupplierItem>({
    url: '/api/boss/suppliers',
    method: 'POST',
    data,
  })
}

export function updateBossSupplier(id: number, data: SupplierUpdatePayload) {
  return request<SupplierItem>({
    url: `/api/boss/suppliers/${id}`,
    method: 'PUT',
    data,
  })
}

export function deleteBossSupplier(id: number) {
  return request<void>({
    url: `/api/boss/suppliers/${id}`,
    method: 'DELETE',
  })
}

export interface SupplierPurchaseLine {
  id: number
  productId: number
  productName: string
  unit: string
  effectiveDate?: string
  purchasePrice: number
  purchasedQty: number
  lineAmount?: number
}

export function fetchSupplierPurchaseLines(
  supplierId: number,
  options?: { dateFrom?: string; dateTo?: string },
) {
  return request<SupplierPurchaseLine[]>({
    url: `/api/boss/suppliers/${supplierId}/purchase-lines`,
    method: 'GET',
    query: options,
  })
}
