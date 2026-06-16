import { request } from './request'

export interface CustomerItem {
  id: number
  name: string
  contactName?: string
  phone?: string
  address?: string
  addressShort?: string
  defaultDeliveryTime?: string
  settlementType?: string
  priceLevel?: string
  autoConfirmOrder?: boolean
  bindUserId?: number
  bindStatus: string
  inviteCode?: string
  inviteExpiredAt?: string
  remark?: string
  status?: number
  createdAt?: string
  outstandingAmount?: number
  lastOrderAt?: string
}

export interface InviteCodeResult {
  customerId: number
  customerName: string
  inviteCode: string
  inviteExpiredAt: string
}

export interface CustomerCreatePayload {
  name: string
  contactName?: string
  phone?: string
  address?: string
  addressShort?: string
  defaultDeliveryTime?: string
  settlementType?: string
  priceLevel?: string
  autoConfirmOrder?: boolean
  remark?: string
}

export function fetchBossCustomers(keyword?: string) {
  return request<CustomerItem[]>({
    url: '/api/boss/customers',
    method: 'GET',
    query: { keyword },
  })
}

export function createBossCustomer(data: CustomerCreatePayload) {
  return request<CustomerItem>({
    url: '/api/boss/customers',
    method: 'POST',
    data,
  })
}

export function deleteBossCustomer(id: number) {
  return request<void>({
    url: `/api/boss/customers/${id}`,
    method: 'DELETE',
  })
}

export function generateCustomerInvite(id: number) {
  return request<InviteCodeResult>({
    url: `/api/boss/customers/${id}/invite`,
    method: 'POST',
  })
}

export function fetchBindStatus() {
  return request<{ bound: boolean; customerId: number; customerName?: string }>({
    url: '/api/customer/bind-status',
    method: 'GET',
  })
}

export function bindCustomerByInvite(inviteCode: string) {
  return request<CustomerItem>({
    url: '/api/customer/bind',
    method: 'POST',
    data: { inviteCode },
  })
}
