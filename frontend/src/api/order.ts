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
  pickRemark?: string
  shortageFlag?: number
}

export interface OrderSummary {
  todayTotal: number
  pendingConfirm: number
  pendingPick: number
  pendingPrice: number
  pendingPublish?: number
}

export interface OrderInfo {
  id: number
  orderNo: string
  customerId?: number
  customerName?: string
  source?: string
  sourceLabel?: string
  status: string
  statusLabel: string
  deliveryDate?: string
  deliveryAddressShort?: string
  contactName?: string
  amount?: number
  paidAmount?: number
  priceIncomplete?: boolean
  paymentStatusLabel?: string
  remark?: string
  createdAt?: string
  itemCount?: number
  pickedItemCount?: number
  items?: OrderLineItem[]
  assignedWorkerId?: number
  assignedWorkerName?: string
  printed?: boolean
}

export function createBossOrder(data: {
  customerId?: number
  customerName?: string
  items: { productId: number; orderQty: number; unit?: string; dealPrice?: number; pickRemark?: string }[]
  remark?: string
  deliveryDate?: string
}) {
  return request<OrderInfo>({
    url: '/api/boss/orders',
    method: 'POST',
    data,
  })
}

export function createCustomerOrder(data: {
  customerName?: string
  items: { productId: number; orderQty: number; unit?: string }[]
  remark?: string
}) {
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

export function fetchBossOrders(options?: {
  status?: string
  pricingPending?: boolean
  keyword?: string
  pickFilter?: 'ALL' | 'UNPICKED' | 'PICKED'
  deliveryFrom?: string
  deliveryTo?: string
}) {
  return request<OrderInfo[]>({
    url: '/api/boss/orders',
    method: 'GET',
    query: {
      status: options?.status,
      pricingPending: options?.pricingPending ? 1 : undefined,
      keyword: options?.keyword,
      pickFilter: options?.pickFilter && options.pickFilter !== 'ALL' ? options.pickFilter : undefined,
      deliveryFrom: options?.deliveryFrom,
      deliveryTo: options?.deliveryTo,
    },
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

export function updateBossOrder(id: number, data: {
  customerName?: string
  items: { productId: number; orderQty: number; unit?: string; dealPrice?: number; pickRemark?: string }[]
  remark?: string
  deliveryDate?: string
}) {
  return request<OrderInfo>({
    url: `/api/boss/orders/${id}`,
    method: 'PUT',
    data,
  })
}

export function updateBossOrderStatus(id: number, status: string) {
  return request<OrderInfo>({
    url: `/api/boss/orders/${id}/status`,
    method: 'PATCH',
    data: { status },
  })
}

export function markBossOrderPrinted(id: number) {
  return request<OrderInfo>({
    url: `/api/boss/orders/${id}/print`,
    method: 'POST',
  })
}

export const BOSS_ORDER_STATUS_OPTIONS = [
  { value: 'PENDING_CONFIRM', label: '待确认' },
  { value: 'PENDING_PRICE', label: '待录价' },
  { value: 'PRICED', label: '待推送' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' },
  { value: 'PENDING_PICK', label: '待分拣（旧流程）' },
  { value: 'PICKING', label: '分拣中（旧流程）' },
] as const
