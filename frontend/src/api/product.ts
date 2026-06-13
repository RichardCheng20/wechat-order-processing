import { request } from './request'

export interface CategoryItem {
  id: number
  name: string
  sortOrder: number
}

export interface ProductItem {
  id: number
  categoryId: number
  categoryName: string
  name: string
  aliases?: string
  unit: string
  spec?: string
  defaultPrice?: number
  referencePrice?: number
  saleStatus: string
}

export interface ProductCreatePayload {
  categoryId: number
  name: string
  aliases?: string
  unit?: string
  spec?: string
  defaultPrice?: number
  saleStatus?: string
}

export function fetchCustomerCategories() {
  return request<CategoryItem[]>({ url: '/api/customer/product-categories', method: 'GET' })
}

export function fetchCustomerProducts(params?: { categoryId?: number; keyword?: string }) {
  return request<ProductItem[]>({
    url: '/api/customer/products',
    method: 'GET',
    query: params,
  })
}

export function fetchBossCategories() {
  return request<CategoryItem[]>({ url: '/api/boss/product-categories', method: 'GET' })
}

export function fetchBossProducts(params?: { categoryId?: number; keyword?: string }) {
  return request<ProductItem[]>({
    url: '/api/boss/products',
    method: 'GET',
    query: params,
  })
}

export function createBossProduct(data: ProductCreatePayload) {
  return request<ProductItem>({ url: '/api/boss/products', method: 'POST', data })
}

export function updateBossProductSaleStatus(id: number, saleStatus: 'ON' | 'OFF') {
  return request<ProductItem>({
    url: `/api/boss/products/${id}/sale-status`,
    method: 'PATCH',
    data: { saleStatus },
  })
}
