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
  receivableAmount?: number
  outstandingAmount?: number
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
  customerId?: number
  paymentFilter?: 'ALL' | 'UNPAID' | 'PAID' | 'PENDING' | 'PARTIAL' | 'SETTLED'
  pickFilter?: 'ALL' | 'UNPICKED' | 'PICKED'
  dateType?: 'ORDER' | 'DELIVERY'
  dateFrom?: string
  dateTo?: string
  deliveryFrom?: string
  deliveryTo?: string
}) {
  const useNewDate = options?.dateFrom && options?.dateTo
  return request<OrderInfo[]>({
    url: '/api/boss/orders',
    method: 'GET',
    query: {
      status: options?.status,
      pricingPending: options?.pricingPending ? 1 : undefined,
      keyword: options?.keyword,
      customerId: options?.customerId,
      paymentFilter: options?.paymentFilter && options.paymentFilter !== 'ALL'
        ? options.paymentFilter
        : undefined,
      pickFilter: options?.pickFilter && options.pickFilter !== 'ALL' ? options.pickFilter : undefined,
      dateType: useNewDate ? options?.dateType || 'DELIVERY' : undefined,
      dateFrom: useNewDate ? options?.dateFrom : undefined,
      dateTo: useNewDate ? options?.dateTo : undefined,
      deliveryFrom: !useNewDate ? options?.deliveryFrom : undefined,
      deliveryTo: !useNewDate ? options?.deliveryTo : undefined,
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

export function markBossOrderPayment(
  id: number,
  data: {
    amount: number
    discount?: number
    method?: 'CASH' | 'WECHAT' | 'BANK_TRANSFER' | 'OTHER'
    remark?: string
  },
) {
  return request<OrderInfo>({
    url: `/api/boss/orders/${id}/payment`,
    method: 'POST',
    data,
  })
}

export const BOSS_ORDER_STATUS_OPTIONS = [
  { value: 'PENDING_CONFIRM', label: '待确认' },
  { value: 'PENDING_PRICE', label: '待录价' },
  { value: 'PRICED', label: '已录价' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' },
  { value: 'PENDING_PICK', label: '待分拣（旧流程）' },
  { value: 'PICKING', label: '分拣中（旧流程）' },
] as const
