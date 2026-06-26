import type { CustomerOrderTab, OrderInfo } from '@common/api/order'

export type FlowPhase = 'pending' | 'current' | 'done' | 'cancelled' | 'danger'

export type FlowStepKey = 'confirm' | 'confirmed' | 'pick' | 'price' | 'reconcile' | 'pay'

export interface OrderFlowStep {
  key: FlowStepKey
  label: string
  phase: FlowPhase
}

const FLOW_STEP_LABELS = ['待确认', '已确认', '已拣单', '已录价', '已对账', '已收款'] as const

const FLOW_STEP_KEYS: FlowStepKey[] = ['confirm', 'confirmed', 'pick', 'price', 'reconcile', 'pay']

const PICK_DONE_STATUS = new Set([
  'PICKED',
  'PENDING_PRICE',
  'PRICED',
  'DELIVERING',
  'DELIVERED',
  'COMPLETED',
])

export function isPickDone(order: OrderInfo) {
  if (PICK_DONE_STATUS.has(order.status)) return true
  const total = order.itemCount || 0
  const picked = order.pickedItemCount || 0
  return total > 0 && picked >= total
}

export function isPriced(order: OrderInfo) {
  return order.amount != null && !order.priceIncomplete
}

export function isPaid(order: OrderInfo) {
  const amount = order.amount ?? 0
  const paid = order.paidAmount ?? 0
  const outstanding = order.outstandingAmount ?? Math.max(amount - paid, 0)
  if (amount > 0 && outstanding <= 0) return true
  const label = order.paymentStatusLabel || ''
  return label.includes('结清') || label.includes('已收款') || label.includes('已支付')
}

export const CUSTOMER_ORDER_TABS: { value: CustomerOrderTab; label: string }[] = [
  { value: 'ALL', label: '全部' },
  { value: 'PROCESSING', label: '处理中' },
  { value: 'UNPAID', label: '待付款' },
  { value: 'PAID', label: '已付款' },
]

export function customerTabLabel(tab: CustomerOrderTab): string {
  return CUSTOMER_ORDER_TABS.find((item) => item.value === tab)?.label || tab
}

export function customerOrderListStatus(order: OrderInfo): string {
  const stage = resolveFlowStageIndex(order)
  if (stage < 0) return '已取消'
  if (stage === 0) return '订单待处理'
  if (stage === 4) return '待付款'
  if (stage >= 5) return '已完成'
  return '处理中'
}

export function customerOrderListStatusType(order: OrderInfo): 'success' | 'warning' | 'primary' | 'info' {
  const stage = resolveFlowStageIndex(order)
  if (stage >= 5) return 'success'
  if (stage === 4) return 'warning'
  if (stage === 0) return 'warning'
  return 'primary'
}

/** 客户视角：待付款 / 已付款（已对账后展示） */
export function customerPaymentLabel(order: OrderInfo): string | null {
  if (!order.printed || order.amount == null) return null
  if (isPaid(order)) return '已付款'
  const label = order.paymentStatusLabel || ''
  if (label.includes('部分')) return '部分付款'
  return '待付款'
}

/** 0=待确认 … 5=已收款，-1=已取消 */
export function resolveFlowStageIndex(order: OrderInfo): number {
  if (order.status === 'CANCELLED') return -1
  if (isPaid(order)) return 5
  if (order.printed) return 4
  if (isPriced(order)) return 3
  if (isPickDone(order)) return 2
  if (order.status !== 'PENDING_CONFIRM') return 1
  return 0
}

export function resolveFlowStatusLabel(order: OrderInfo): string {
  const stage = resolveFlowStageIndex(order)
  if (stage < 0) return '已取消'
  return FLOW_STEP_LABELS[stage] ?? '待确认'
}

export function buildOrderFlowSteps(order: OrderInfo): OrderFlowStep[] {
  const stage = resolveFlowStageIndex(order)

  if (stage < 0) {
    return FLOW_STEP_KEYS.map((key, index) => ({
      key,
      label: index === 0 ? '已取消' : FLOW_STEP_LABELS[index],
      phase: 'cancelled' as FlowPhase,
    }))
  }

  return FLOW_STEP_KEYS.map((key, index) => ({
    key,
    label: FLOW_STEP_LABELS[index],
    phase: index < stage ? 'done' : index === stage ? 'current' : 'pending',
  }))
}

export function connectorPhase(prev: OrderFlowStep): 'done' | 'pending' {
  return prev.phase === 'done' ? 'done' : 'pending'
}
