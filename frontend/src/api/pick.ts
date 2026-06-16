import { request } from './request'
import type { OrderInfo } from './order'

export function fetchBossPickDetail(orderId: number) {
  return request<OrderInfo>({
    url: `/api/boss/orders/${orderId}/pick`,
    method: 'GET',
  })
}

export function startBossPick(orderId: number) {
  return request<OrderInfo>({
    url: `/api/boss/orders/${orderId}/pick/start`,
    method: 'POST',
  })
}

export function updateBossPickItem(
  orderId: number,
  itemId: number,
  data: { actualQty?: number; dealPrice?: number; shortageFlag?: number },
) {
  return request<OrderInfo>({
    url: `/api/boss/orders/${orderId}/pick/items/${itemId}`,
    method: 'PATCH',
    data,
  })
}

export function fillBossPickQty(orderId: number) {
  return request<OrderInfo>({
    url: `/api/boss/orders/${orderId}/pick/fill-qty`,
    method: 'POST',
  })
}

export function fetchBossPickPrices(orderId: number) {
  return request<OrderInfo>({
    url: `/api/boss/orders/${orderId}/pick/fetch-prices`,
    method: 'POST',
  })
}

export function completeBossPick(orderId: number) {
  return request<OrderInfo>({
    url: `/api/boss/orders/${orderId}/pick/complete`,
    method: 'POST',
  })
}
