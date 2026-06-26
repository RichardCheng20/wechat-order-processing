import { getApiBaseUrl } from '@common/utils/config'
import { request } from './request'

export interface PaymentCreatePayload {
  customerId: number
  amount: number
  paidAt?: string
  method?: 'CASH' | 'WECHAT' | 'BANK_TRANSFER' | 'OTHER'
  remark?: string
  voucherUrls?: string[]
}

export interface PaymentInfo {
  id: number
  customerId: number
  customerName: string
  orderId?: number
  amount: number
  method: string
  paidAt: string
  remark?: string
  voucherUrls?: string[]
  createdAt?: string
}

export function createBossPayment(data: PaymentCreatePayload) {
  return request<PaymentInfo>({
    url: '/api/boss/payments',
    method: 'POST',
    data,
  })
}

export function fetchBossPayments(customerId?: number) {
  return request<PaymentInfo[]>({
    url: '/api/boss/payments',
    method: 'GET',
    query: customerId ? { customerId } : undefined,
  })
}

export interface PurchasePaymentCreatePayload {
  supplierId?: number
  supplierName?: string
  amount: number
  paidAt?: string
  method?: 'CASH' | 'WECHAT' | 'BANK_TRANSFER' | 'OTHER'
  remark?: string
  voucherUrls?: string[]
}

export interface PurchasePaymentInfo {
  id: number
  supplierId: number
  supplierName: string
  amount: number
  method: string
  paidAt: string
  remark?: string
  voucherUrls?: string[]
  createdAt?: string
}

export function createBossPurchasePayment(data: PurchasePaymentCreatePayload) {
  return request<PurchasePaymentInfo>({
    url: '/api/boss/purchase-payments',
    method: 'POST',
    data,
  })
}

export function uploadPaymentVoucher(filePath: string): Promise<string> {
  const token = uni.getStorageSync('token') as string
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${getApiBaseUrl()}/api/boss/files/upload`,
      filePath,
      name: 'file',
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (res) => {
        if (!res.statusCode || res.statusCode >= 500) {
          reject(new Error('服务器内部错误，请确认后端已启动'))
          return
        }
        try {
          const body = JSON.parse(res.data as string) as { code: number; message: string; data?: { url: string } }
          if (body.code === 0 && body.data?.url) {
            resolve(body.data.url)
            return
          }
          reject(new Error(body.message || '上传失败'))
        } catch {
          reject(new Error('上传响应解析失败'))
        }
      },
      fail: (err) => reject(new Error(err.errMsg || '上传失败')),
    })
  })
}
