import { request } from './request'

export interface CustomerQuoteSummary {
  customerId: number
  customerName: string
  quotedProductCount: number
}

export interface CustomerQuoteLine {
  productId: number
  productName: string
  unit: string
  categoryId?: number
  categoryName?: string
  basePrice?: number
  customerPrice?: number
  hasQuote?: boolean
}

export interface CustomerQuoteDetail {
  customerId: number
  customerName: string
  lines: CustomerQuoteLine[]
}

export function fetchCustomerQuoteSummaries(keyword?: string) {
  return request<CustomerQuoteSummary[]>({
    url: '/api/boss/customer-quotes',
    method: 'GET',
    query: { keyword: keyword || undefined },
  })
}

export function fetchCustomerQuoteDetail(
  customerId: number,
  options?: { keyword?: string; categoryId?: number; onlyQuoted?: boolean },
) {
  return request<CustomerQuoteDetail>({
    url: `/api/boss/customer-quotes/${customerId}`,
    method: 'GET',
    query: {
      keyword: options?.keyword || undefined,
      categoryId: options?.categoryId,
      onlyQuoted: options?.onlyQuoted ? true : undefined,
    },
  })
}

export function saveCustomerQuote(
  customerId: number,
  items: { productId: number; price?: number | null }[],
) {
  return request<CustomerQuoteDetail>({
    url: `/api/boss/customer-quotes/${customerId}`,
    method: 'PUT',
    data: { items },
  })
}

export function syncQuoteFromOrder(orderId: number) {
  return request<{ syncedCount: number }>({
    url: `/api/boss/customer-quotes/sync-from-order/${orderId}`,
    method: 'POST',
  })
}
