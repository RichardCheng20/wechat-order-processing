import { request } from './request'

export interface ProcurementTaskItem {
  productId: number
  productName: string
  categoryId?: number
  unit: string
  demandQty: number
  stockQty: number
  needQty: number
  purchasePrice?: number
  totalAmount?: number
  orderCount?: number
  customerCount?: number
  priced?: boolean
}

export interface ProcurementTask {
  receiveDate: string
  totalNeedQty: number
  totalAmount: number
  productCount: number
  items: ProcurementTaskItem[]
}

export interface ProcurementCustomerLine {
  customerId?: number
  customerName: string
  totalQty: number
  orderCount?: number
}

export interface ProcurementProductDetail {
  productId: number
  productName: string
  unit: string
  receiveDate?: string
  demandQty: number
  stockQty: number
  needQty: number
  purchasePrice?: number
  purchasedQtyToday?: number
  referencePurchasePrice?: number
  priced?: boolean
  customerLines: ProcurementCustomerLine[]
}

export interface ProcurementQuery {
  receiveDate?: string
  keyword?: string
  categoryId?: number
}

export function fetchProcurementTasks(params?: ProcurementQuery) {
  return request<ProcurementTask>({
    url: '/api/boss/procurement/tasks',
    method: 'GET',
    query: params,
  })
}

export function fetchProcurementProductDetail(productId: number, receiveDate?: string) {
  return request<ProcurementProductDetail>({
    url: `/api/boss/procurement/products/${productId}`,
    method: 'GET',
    query: { receiveDate },
  })
}

export function fetchProcurementReferencePrice(productId: number, receiveDate?: string) {
  return request<ProcurementProductDetail>({
    url: `/api/boss/procurement/products/${productId}/fetch-prices`,
    method: 'POST',
    query: { receiveDate },
  })
}

export function submitProcurementPurchasePrice(
  productId: number,
  purchasePrice: number,
  receiveDate?: string,
  purchasedQty?: number,
) {
  return request<ProcurementProductDetail>({
    url: `/api/boss/procurement/products/${productId}/submit-price`,
    method: 'POST',
    query: { receiveDate },
    data: { purchasePrice, purchasedQty },
  })
}

export function updateProcurementStock(productId: number, stockQty: number, receiveDate?: string) {
  return request<ProcurementProductDetail>({
    url: `/api/boss/procurement/products/${productId}/stock`,
    method: 'PATCH',
    query: { receiveDate },
    data: { stockQty },
  })
}
