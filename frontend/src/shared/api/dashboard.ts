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

export interface SupplierReportSummary {
  paidAmount: number
  paymentCount: number
  supplierCount: number
}

export interface SupplierReportRow {
  supplierId?: number
  supplierName: string
  paidAmount: number
  paymentCount: number
}

export interface BossSupplierReport {
  summary: SupplierReportSummary
  rows: SupplierReportRow[]
}

export interface InventoryReportSummary {
  inboundQty: number
  outboundQty: number
  productCount: number
}

export interface InventoryReportRow {
  productId?: number
  categoryId?: number
  productName: string
  unit?: string
  inboundQty: number
  outboundQty: number
  stockQty: number
  availableQty: number
}

export interface BossInventoryReport {
  summary: InventoryReportSummary
  rows: InventoryReportRow[]
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

export function fetchBossSupplierReport(options: { dateFrom: string; dateTo: string }) {
  return request<BossSupplierReport>({
    url: '/api/boss/stats/supplier-report',
    method: 'GET',
    query: {
      dateFrom: options.dateFrom,
      dateTo: options.dateTo,
    },
  })
}

export function fetchBossInventoryReport(options: {
  dateFrom: string
  dateTo: string
  dateType?: 'DELIVERY' | 'ORDER'
}) {
  return request<BossInventoryReport>({
    url: '/api/boss/stats/inventory-report',
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
