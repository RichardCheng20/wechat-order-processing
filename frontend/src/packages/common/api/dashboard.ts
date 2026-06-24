import { request } from './request'

export interface BossDashboard {
  todaySales: number
  todayProfit: number
  todayPurchaseCost?: number
  totalReceivable: number
  totalReceived: number
  totalPayable: number
}

export interface RevenueDailyRow {
  date: string
  salesAmount: number
  purchaseCost: number
  profit: number
}

export interface BossRevenueStats {
  totalSales: number
  totalProfit: number
  totalPurchaseCost: number
  rows: RevenueDailyRow[]
}

export interface PaymentDailyRow {
  date: string
  amount: number
}

export interface BossPaymentStats {
  totalReceived: number
  rows: PaymentDailyRow[]
}

export interface CustomerRankingRow {
  customerId?: number
  customerName: string
  rank: number
  salesAmount: number
  purchaseCost: number
  profit: number
}

export interface ProductRankingRow {
  productId: number
  productName: string
  rank: number
  salesAmount: number
  purchaseCost: number
  profit: number
}

export interface BossCustomerRanking {
  rows: CustomerRankingRow[]
}

export interface CustomerReportSummary {
  receivableAmount: number
  receivedAmount: number
  discountAmount: number
  outstandingAmount: number
  refundAmount: number
}

export interface CustomerReportRow {
  customerId?: number
  customerName: string
  receivableAmount: number
  receivedAmount: number
}

export interface BossCustomerReport {
  summary: CustomerReportSummary
  rows: CustomerReportRow[]
}

export interface BossProductRanking {
  rows: ProductRankingRow[]
}

export function fetchBossDashboard() {
  return request<BossDashboard>({
    url: '/api/boss/dashboard',
    method: 'GET',
  })
}

export function fetchBossRevenueStats(options: {
  dateFrom: string
  dateTo: string
  dateType?: 'DELIVERY' | 'ORDER'
}) {
  return request<BossRevenueStats>({
    url: '/api/boss/stats/revenue',
    method: 'GET',
    query: {
      dateFrom: options.dateFrom,
      dateTo: options.dateTo,
      dateType: options.dateType || 'DELIVERY',
    },
  })
}

export function fetchBossPaymentStats(options: { dateFrom: string; dateTo: string }) {
  return request<BossPaymentStats>({
    url: '/api/boss/stats/payments',
    method: 'GET',
    query: {
      dateFrom: options.dateFrom,
      dateTo: options.dateTo,
    },
  })
}

export function fetchBossCustomerRanking(options: {
  dateFrom: string
  dateTo: string
  dateType?: 'DELIVERY' | 'ORDER'
}) {
  return request<BossCustomerRanking>({
    url: '/api/boss/stats/customer-ranking',
    method: 'GET',
    query: {
      dateFrom: options.dateFrom,
      dateTo: options.dateTo,
      dateType: options.dateType || 'ORDER',
    },
  })
}

export function fetchBossCustomerReport(options: {
  dateFrom: string
  dateTo: string
  dateType?: 'DELIVERY' | 'ORDER'
}) {
  return request<BossCustomerReport>({
    url: '/api/boss/stats/customer-report',
    method: 'GET',
    query: {
      dateFrom: options.dateFrom,
      dateTo: options.dateTo,
      dateType: options.dateType || 'DELIVERY',
    },
  })
}

export function fetchBossProductRanking(options: {
  dateFrom: string
  dateTo: string
  dateType?: 'DELIVERY' | 'ORDER'
}) {
  return request<BossProductRanking>({
    url: '/api/boss/stats/product-ranking',
    method: 'GET',
    query: {
      dateFrom: options.dateFrom,
      dateTo: options.dateTo,
      dateType: options.dateType || 'ORDER',
    },
  })
}
