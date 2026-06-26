import { request } from './request'

export interface PricingLineItem {
  id: number
  productId: number
  productName: string
  customItem?: boolean
  customName?: string
  orderQty: number
  actualQty?: number
  unit: string
  referencePrice?: number
  costPrice?: number
  dealPrice?: number
  subtotalAmount?: number
  shortageFlag?: number
  pickRemark?: string
}

export interface PricingOrder {
  id: number
  orderNo: string
  customerId?: number
  customerName?: string
  status: string
  statusLabel: string
  deliveryDate?: string
  deliveryAddressShort?: string
  amount?: number
  remark?: string
  createdAt?: string
  customItemCount?: number
  items?: PricingLineItem[]
}

export interface PricingProductSummary {
  productId: number
  productName: string
  customItem?: boolean
  customName?: string
  unit: string
  pendingCount: number
  totalCount: number
  totalQty: number
  allPriced?: boolean
}

export interface PricingProductLine {
  itemId: number
  orderId: number
  orderNo: string
  customerId?: number
  customerName?: string
  deliveryDate?: string
  orderRemark?: string
  pickRemark?: string
  quantity: number
  unit: string
  customItem?: boolean
  customName?: string
  referencePrice?: number
  costPrice?: number
  dealPrice?: number
  priced?: boolean
}

export interface PricingProductDetail {
  productId: number
  productName: string
  customItem?: boolean
  customName?: string
  unit: string
  orderCount: number
  totalQty: number
  lines: PricingProductLine[]
}

export interface PricingSubmitItem {
  itemId: number
  dealPrice: number
  costPrice?: number
}

export interface PricingQuery {
  keyword?: string
  priceFilter?: 'ALL' | 'UNPRICED' | 'PRICED'
  deliveryFrom?: string
  deliveryTo?: string
  customName?: string
}

function pricingQuery(query?: PricingQuery) {
  return {
    keyword: query?.keyword,
    priceFilter: query?.priceFilter && query.priceFilter !== 'ALL' ? query.priceFilter : undefined,
    deliveryFrom: query?.deliveryFrom,
    deliveryTo: query?.deliveryTo,
    customName: query?.customName,
  }
}

export function fetchPendingPriceOrders() {
  return request<PricingOrder[]>({
    url: '/api/boss/orders/pending-price',
    method: 'GET',
  })
}

export function fetchPricingProducts(query?: PricingQuery) {
  return request<PricingProductSummary[]>({
    url: '/api/boss/pricing/products',
    method: 'GET',
    query: pricingQuery(query),
  })
}

export function fetchProductPricingDetail(productId: number, query?: PricingQuery) {
  return request<PricingProductDetail>({
    url: `/api/boss/pricing/products/${productId}`,
    method: 'GET',
    query: pricingQuery(query),
  })
}

export function fetchProductReferencePrices(productId: number, query?: PricingQuery) {
  return request<PricingProductDetail>({
    url: `/api/boss/pricing/products/${productId}/fetch-prices`,
    method: 'POST',
    query: pricingQuery(query),
  })
}

export function submitProductPricing(
  productId: number,
  items: PricingSubmitItem[],
  query?: PricingQuery,
) {
  return request<PricingProductDetail>({
    url: `/api/boss/pricing/products/${productId}/submit`,
    method: 'POST',
    query: pricingQuery(query),
    data: { items },
  })
}

export function fetchPricingDetail(orderId: number) {
  return request<PricingOrder>({
    url: `/api/boss/orders/${orderId}/pricing`,
    method: 'GET',
  })
}

export function submitOrderPricing(orderId: number, items: PricingSubmitItem[]) {
  return request<PricingOrder>({
    url: `/api/boss/orders/${orderId}/pricing`,
    method: 'POST',
    data: { items },
  })
}

export function publishOrderPricing(orderId: number) {
  return request<PricingOrder>({
    url: `/api/boss/orders/${orderId}/publish`,
    method: 'POST',
  })
}

export function displayLineRemark(line: Pick<PricingProductLine, 'pickRemark' | 'orderRemark'>) {
  const parts = [line.pickRemark?.trim(), line.orderRemark?.trim()].filter(Boolean)
  return parts.length ? parts.join('；') : '—'
}

export function productSummaryKey(item: Pick<PricingProductSummary, 'productId' | 'customItem' | 'customName'>) {
  return item.customItem && item.customName ? `c:${item.customName}` : `p:${item.productId}`
}
