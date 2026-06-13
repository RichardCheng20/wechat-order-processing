import { request } from './request'

export interface OrderLineItem {
  id?: number
  productId: number
  productName?: string
  orderQty: number
  actualQty?: number
  unit: string
  dealPrice?: number
  subtotalAmount?: number
}

export interface OrderSummary {
  todayTotal: number
  pendingConfirm: number
  pendingPick: number
  pendingPrice: number
}

export interface OrderInfo {
  id: number
  orderNo: string
  customerId?: number
  customerName?: string
  status: string
  statusLabel: string
  deliveryDate?: string
  deliveryAddressShort?: string
  contactName?: string
  amount?: number
  remark?: string
  createdAt?: string
  itemCount?: number
  items?: OrderLineItem[]
  assignedWorkerId?: number
  assignedWorkerName?: string
}

export function createCustomerOrder(data: { items: { productId: number; orderQty: number }[]; remark?: string }) {
  return request<OrderInfo>({
    url: '/api/customer/orders',
    method: 'POST',
    data,
  })
}

export function fetchCustomerOrders() {
  return request<OrderInfo[]>({
    url: '/api/customer/orders',
    method: 'GET',
  })
}

export function fetchCustomerOrderDetail(id: number) {
  return request<OrderInfo>({
    url: `/api/customer/orders/${id}`,
    method: 'GET',
  })
}

export function fetchBossOrderSummary() {
  return request<OrderSummary>({
    url: '/api/boss/orders/summary',
    method: 'GET',
  })
}

export function fetchBossOrders(status?: string) {
  return request<OrderInfo[]>({
    url: '/api/boss/orders',
    method: 'GET',
    query: { status },
  })
}

export function fetchBossOrderDetail(id: number) {
  return request<OrderInfo>({
    url: `/api/boss/orders/${id}`,
    method: 'GET',
  })
}

export function confirmBossOrder(id: number) {
  return request<OrderInfo>({
    url: `/api/boss/orders/${id}/confirm`,
    method: 'POST',
  })
}
