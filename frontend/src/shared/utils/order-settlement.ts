import type { OrderInfo } from '@common/api/order'
import { formatShortDate } from './date-range'

export function getOrderReceivable(item: OrderInfo): number {
  return Number(item.receivableAmount ?? item.amount ?? 0)
}

export function getOrderOutstanding(item: OrderInfo): number {
  if (item.outstandingAmount != null) return Number(item.outstandingAmount)
  const receivable = getOrderReceivable(item)
  const paid = Number(item.paidAmount ?? 0)
  return Math.max(receivable - paid, 0)
}

export function formatOrderMoney(value?: number): string {
  return Number(value || 0).toFixed(2)
}

/** 对账/结款列表统一结款状态 */
export function orderSettlementLabel(item: OrderInfo): string {
  const outstanding = getOrderOutstanding(item)
  const paid = Number(item.paidAmount ?? 0)
  if (outstanding <= 0) return '已结清'
  if (paid > 0) return '部分结款'
  return '待结款'
}

export function formatDeliveryDate(value?: string): string {
  if (!value) return '-'
  return formatShortDate(value)
}
