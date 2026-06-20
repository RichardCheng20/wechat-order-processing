import type { OrderInfo } from '../api/order'

export type FlowPhase = 'pending' | 'current' | 'done' | 'cancelled' | 'danger'

export type FlowStepKey = 'confirm' | 'pick' | 'price' | 'print' | 'pay'

export interface OrderFlowStep {
  key: FlowStepKey
  label: string
  phase: FlowPhase
}

const PICK_CURRENT = new Set(['PENDING_PICK', 'PICKING'])
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

export function buildOrderFlowSteps(order: OrderInfo): OrderFlowStep[] {
  if (order.status === 'CANCELLED') {
    return [
      { key: 'confirm', label: '已取消', phase: 'cancelled' },
      { key: 'pick', label: '分拣', phase: 'cancelled' },
      { key: 'price', label: '录价', phase: 'cancelled' },
      { key: 'print', label: '对账', phase: 'cancelled' },
      { key: 'pay', label: '收款', phase: 'cancelled' },
    ]
  }

  const pickDone = isPickDone(order)
  const priced = isPriced(order)
  const paid = isPaid(order)
  const printed = !!order.printed

  const confirm: OrderFlowStep =
    order.status === 'PENDING_CONFIRM'
      ? { key: 'confirm', label: '待确认', phase: 'current' }
      : { key: 'confirm', label: '已确认', phase: 'done' }

  let pick: OrderFlowStep
  if (order.status === 'PENDING_CONFIRM') {
    pick = { key: 'pick', label: '待分拣', phase: 'pending' }
  } else if (order.status === 'PENDING_PICK') {
    pick = { key: 'pick', label: '待分拣', phase: 'current' }
  } else if (order.status === 'PICKING') {
    pick = { key: 'pick', label: '分拣中', phase: 'current' }
  } else if (pickDone) {
    pick = { key: 'pick', label: '已拣完', phase: 'done' }
  } else if (PICK_CURRENT.has(order.status)) {
    pick = { key: 'pick', label: '分拣中', phase: 'current' }
  } else {
    pick = { key: 'pick', label: '待分拣', phase: 'current' }
  }

  let price: OrderFlowStep
  if (!pickDone) {
    price = { key: 'price', label: '待录价', phase: 'pending' }
  } else if (!priced) {
    price = { key: 'price', label: '待录价', phase: 'current' }
  } else {
    price = { key: 'price', label: '已录价', phase: 'done' }
  }

  let printStep: OrderFlowStep
  if (printed) {
    printStep = { key: 'print', label: '已对账', phase: 'done' }
  } else if (priced) {
    printStep = { key: 'print', label: '待对账', phase: 'current' }
  } else {
    printStep = { key: 'print', label: '待对账', phase: 'pending' }
  }

  let pay: OrderFlowStep
  if (paid) {
    pay = { key: 'pay', label: '已收款', phase: 'done' }
  } else if (priced) {
    pay = { key: 'pay', label: '待收款', phase: 'danger' }
  } else {
    pay = { key: 'pay', label: '待收款', phase: 'pending' }
  }

  return [confirm, pick, price, printStep, pay]
}

export function connectorPhase(prev: OrderFlowStep): 'done' | 'pending' {
  return prev.phase === 'done' ? 'done' : 'pending'
}
