import { request } from './request'

export interface ProcurementTaskItem {
  productId: number
  productName: string
  customItem?: boolean
  customName?: string
  categoryId?: number
  unit: string
  demandQty: number
  /** 可用库存 */
  stockQty: number
  physicalStockQty?: number
  reservedQty?: number
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

export interface ProcurementSupplierOption {
  id: number
  supplierNo?: string
  name: string
}

export interface ProcurementSupplierOrderLine {
  id: number
  supplierId: number
  supplierName: string
  supplierNo?: string
  purchasePrice?: number | null
  purchasedQty: number
  lineAmount?: number | null
}

export interface ProcurementProductDetail {
  productId: number
  productName: string
  customItem?: boolean
  customName?: string
  unit: string
  receiveDate?: string
  demandQty: number
  /** 可用库存 */
  stockQty: number
  physicalStockQty?: number
  reservedQty?: number
  needQty: number
  purchasePrice?: number
  purchasedQtyToday?: number
  referencePurchasePrice?: number
  priced?: boolean
  recordedAtPricing?: boolean
  customerLines: ProcurementCustomerLine[]
  supplierId?: number
  supplierName?: string
  supplierNo?: string
  supplierOptions?: ProcurementSupplierOption[]
  supplierOrders?: ProcurementSupplierOrderLine[]
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

export function fetchCustomProcurementDetail(customName: string, receiveDate?: string) {
  return request<ProcurementProductDetail>({
    url: '/api/boss/procurement/custom-items',
    method: 'GET',
    query: { customName, receiveDate },
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
  purchasePrice?: number | null,
  receiveDate?: string,
  purchasedQty?: number,
  supplierId?: number,
) {
  return request<ProcurementProductDetail>({
    url: `/api/boss/procurement/products/${productId}/submit-price`,
    method: 'POST',
    query: { receiveDate },
    data: {
      purchasePrice: purchasePrice ?? null,
      purchasedQty,
      supplierId,
    },
  })
}

export function deleteProcurementSupplierOrder(
  productId: number,
  lineId: number,
  receiveDate?: string,
) {
  return request<ProcurementProductDetail>({
    url: `/api/boss/procurement/products/${productId}/supplier-orders/${lineId}`,
    method: 'DELETE',
    query: { receiveDate },
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
