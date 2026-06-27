import { request } from './request'

export interface CustomerItem {
  id: number
  customerNo?: string
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
  totalSalesAmount?: number
  lastOrderAt?: string
}

export interface InviteCodeResult {
  customerId: number
  customerName: string
  inviteCode: string
  inviteExpiredAt: string
}

export interface CustomerRegisterInviteResult {
  token: string
  expiredAt: string
  loginPath: string
  miniProgramName?: string
  qrCodeBase64?: string | null
}

export interface CustomerBindRequestItem {
  id: number
  userId: number
  shopName: string
  contactName?: string
  phone?: string
  address?: string
  addressShort?: string
  status: string
  customerId?: number
  rejectReason?: string
  reviewedAt?: string
  createdAt?: string
  applicantNickname?: string
}

export interface CustomerRegisterStatus {
  bound: boolean
  customerId?: number
  customerName?: string
  pendingReview: boolean
  pendingRequestId?: number
  lastRequestStatus?: string
  rejectReason?: string
  submittedAt?: string
}

export interface CustomerRegisterApplyPayload {
  registerToken: string
  shopName: string
  contactName?: string
  phone?: string
  address?: string
  addressShort?: string
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

export function fetchBossCustomerDetail(id: number) {
  return request<CustomerItem>({
    url: `/api/boss/customers/${id}`,
    method: 'GET',
  })
}

export function updateBossCustomer(id: number, data: Partial<CustomerCreatePayload> & { status?: number }) {
  return request<CustomerItem>({
    url: `/api/boss/customers/${id}`,
    method: 'PUT',
    data,
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

export function generateCustomerRegisterInvite() {
  return request<CustomerRegisterInviteResult>({
    url: '/api/boss/customers/register-invite',
    method: 'POST',
  })
}

export function fetchCurrentCustomerRegisterInvite() {
  return request<CustomerRegisterInviteResult | null>({
    url: '/api/boss/customers/register-invite',
    method: 'GET',
  })
}

export function fetchPendingBindRequestCount() {
  return request<{ count: number }>({
    url: '/api/boss/customer-bind-requests/pending-count',
    method: 'GET',
  })
}

export function fetchCustomerBindRequests(status?: string) {
  return request<CustomerBindRequestItem[]>({
    url: '/api/boss/customer-bind-requests',
    method: 'GET',
    query: { status },
  })
}

export function approveCustomerBindRequest(id: number) {
  return request<CustomerItem>({
    url: `/api/boss/customer-bind-requests/${id}/approve`,
    method: 'POST',
  })
}

export function rejectCustomerBindRequest(id: number, reason?: string) {
  return request<void>({
    url: `/api/boss/customer-bind-requests/${id}/reject`,
    method: 'POST',
    data: reason ? { reason } : {},
  })
}

export function fetchCustomerRegisterStatus() {
  return request<CustomerRegisterStatus>({
    url: '/api/customer/register-status',
    method: 'GET',
  })
}

export function submitCustomerRegisterApply(data: CustomerRegisterApplyPayload) {
  return request<CustomerBindRequestItem>({
    url: '/api/customer/register-apply',
    method: 'POST',
    data,
  })
}
