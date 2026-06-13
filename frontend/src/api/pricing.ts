import { request } from './request'

export interface PricingLineItem {
  id: number
  productId: number
  productName: string
  orderQty: number
  actualQty?: number
  unit: string
  referencePrice?: number
  dealPrice?: number
  subtotalAmount?: number
  shortageFlag?: number
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
  items?: PricingLineItem[]
}

export function fetchPendingPriceOrders() {
  return request<PricingOrder[]>({
    url: '/api/boss/orders/pending-price',
    method: 'GET',
  })
}

export function fetchPricingDetail(orderId: number) {
  return request<PricingOrder>({
    url: `/api/boss/orders/${orderId}/pricing`,
    method: 'GET',
  })
}

export function submitOrderPricing(orderId: number, items: { itemId: number; dealPrice: number }[]) {
  return request<PricingOrder>({
    url: `/api/boss/orders/${orderId}/pricing`,
    method: 'POST',
    data: { items },
  })
}
