import { request } from './request'

export interface InventoryProduct {
  id: number
  categoryId?: number
  categoryName?: string
  name: string
  unit: string
  physicalStockQty: number
  reservedQty?: number
  availableStockQty?: number
}

export function fetchInventoryProducts(params?: { categoryId?: number; keyword?: string }) {
  return request<InventoryProduct[]>({
    url: '/api/boss/inventory/products',
    method: 'GET',
    query: params,
  })
}

export function updateInventoryStock(productId: number, stockQty: number) {
  return request<InventoryProduct>({
    url: `/api/boss/inventory/products/${productId}/stock`,
    method: 'PATCH',
    data: { stockQty },
  })
}
