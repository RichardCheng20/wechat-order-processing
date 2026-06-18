import { request } from './request'

export interface CategoryItem {
  id: number
  name: string
  sortOrder: number
  parentId?: number | null
  children?: CategoryItem[]
}

export interface ProductItem {
  id: number
  categoryId: number
  categoryName: string
  name: string
  aliases?: string
  unit: string
  saleUnits?: string[]
  spec?: string
  imageUrl?: string
  defaultPrice?: number
  referencePrice?: number
  saleStatus: string
}

export interface ProductCreatePayload {
  categoryId: number
  name: string
  aliases?: string
  unit?: string
  saleUnits?: string[]
  spec?: string
  imageUrl?: string
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

export function createBossCategory(name: string, parentId?: number) {
  return request<CategoryItem>({
    url: '/api/boss/product-categories',
    method: 'POST',
    data: { name, parentId },
  })
}

export function deleteBossCategory(id: number) {
  return request<void>({
    url: `/api/boss/product-categories/${id}`,
    method: 'DELETE',
  })
}

export function sortBossCategories(orderedIds: number[], parentId?: number) {
  return request<void>({
    url: '/api/boss/product-categories/sort',
    method: 'PUT',
    data: { parentId, orderedIds },
  })
}

export function fetchBossProducts(params?: { categoryId?: number; keyword?: string }) {
  return request<ProductItem[]>({
    url: '/api/boss/products',
    method: 'GET',
    query: params,
  })
}

export function fetchBossDailyPrices(date: string) {
  return request<Record<string, number>>({
    url: '/api/boss/products/daily-prices',
    method: 'GET',
    query: { date },
  })
}

export interface ProductUpdatePayload {
  categoryId?: number
  name?: string
  aliases?: string
  unit?: string
  saleUnits?: string[]
  spec?: string
  imageUrl?: string
  defaultPrice?: number
  saleStatus?: string
}

export function createBossProduct(data: ProductCreatePayload) {
  return request<ProductItem>({ url: '/api/boss/products', method: 'POST', data })
}

export function updateBossProduct(id: number, data: ProductUpdatePayload) {
  return request<ProductItem>({ url: `/api/boss/products/${id}`, method: 'PUT', data })
}

export function updateBossProductSaleStatus(id: number, data: { saleStatus: 'ON' | 'OFF'; saleUnits?: string[] }) {
  return request<ProductItem>({
    url: `/api/boss/products/${id}/sale-status`,
    method: 'PATCH',
    data,
  })
}

export function deleteBossProduct(id: number) {
  return request<void>({
    url: `/api/boss/products/${id}`,
    method: 'DELETE',
  })
}
