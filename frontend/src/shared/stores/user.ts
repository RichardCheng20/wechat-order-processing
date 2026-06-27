import { defineStore } from 'pinia'
import { devLogin, logout, wechatLogin, type LoginResult } from '@common/api/auth'
import { fetchCustomerRegisterStatus } from '@common/api/customer'
import { clearEntryActivation, clearEntryInvite, clearEntryRegister, getEntryContext } from '@common/utils/tenant'
import { useBossAlertStore } from './bossAlert'

export type UserRole = 'CUSTOMER' | 'OWNER_ADMIN' | 'STALL_OWNER' | 'STALL_MANAGER' | 'WORKER' | 'PARTNER_ADMIN'

interface UserState {
  token: string
  userId: number | null
  role: UserRole | ''
  nickname: string
  merchantId: number | null
  merchantName: string
  customerId: number | null
  customerName: string
  openid: string
  workerId: number | null
  workerCode: string
}

const PROFILE_KEY = 'user_profile'

function loadSavedProfile(): Partial<UserState> {
  try {
    const raw = uni.getStorageSync(PROFILE_KEY)
    if (!raw) return {}
    const data = JSON.parse(raw as string) as LoginResult
    const profile = data as LoginResult & { customerName?: string }
    return {
      userId: data.userId ?? null,
      role: (data.role as UserRole) || '',
      nickname: data.nickname || '',
      merchantId: data.merchantId ?? null,
      merchantName: data.merchantName || '',
      customerId: data.customerId ?? null,
      customerName: profile.customerName || '',
      openid: data.openid || '',
      workerId: data.workerId ?? null,
      workerCode: data.workerCode || '',
    }
  } catch {
    return {}
  }
}

function homeByRole(role: UserRole, customerId?: number | null) {
  switch (role) {
    case 'OWNER_ADMIN':
    case 'STALL_OWNER':
    case 'STALL_MANAGER':
      return '/pages/boss/orders/index'
    case 'WORKER':
      return '/pages/worker/tasks/index'
    case 'CUSTOMER':
      return '/pages/customer/home/index'
    default:
      return '/pages/customer/home/index'
  }
}

export const useUserStore = defineStore('user', {
  state: (): UserState => {
    const saved = loadSavedProfile()
    return {
      token: uni.getStorageSync('token') || '',
      userId: saved.userId ?? null,
      role: saved.role || '',
      nickname: saved.nickname || '',
      merchantId: saved.merchantId ?? null,
      merchantName: saved.merchantName ?? '',
      customerId: saved.customerId ?? null,
      customerName: saved.customerName ?? '',
      openid: saved.openid ?? '',
      workerId: saved.workerId ?? null,
      workerCode: saved.workerCode ?? '',
    }
  },

  getters: {
    displayName: (state) => state.customerName || state.nickname || '客户',
    isLoggedIn: (state) => !!state.token && !!state.role,
    isBoss: (state) =>
      state.role === 'OWNER_ADMIN'
      || state.role === 'STALL_OWNER'
      || state.role === 'STALL_MANAGER'
      || state.role === 'PARTNER_ADMIN',
    isOwnerAdmin: (state) => state.role === 'OWNER_ADMIN',
    canViewDataPlatform: (state) => state.role === 'OWNER_ADMIN' || state.role === 'STALL_OWNER',
    isCustomer: (state) => state.role === 'CUSTOMER',
    isCustomerBound: (state) => state.role !== 'CUSTOMER' || !!state.customerId,
  },

  actions: {
    applyLogin(data: LoginResult) {
      useBossAlertStore().reset()
      this.token = data.token
      this.userId = data.userId
      this.role = data.role as UserRole
      this.nickname = data.nickname
      this.merchantId = data.merchantId
      this.merchantName = data.merchantName || ''
      this.customerId = data.customerId ?? null
      this.customerName = (data as LoginResult & { customerName?: string }).customerName || ''
      this.openid = data.openid || ''
      this.workerId = data.workerId ?? null
      this.workerCode = data.workerCode || ''
      uni.setStorageSync('token', data.token)
      uni.setStorageSync(PROFILE_KEY, JSON.stringify(data))
    },

    hydrateFromStorage() {
      const token = uni.getStorageSync('token') as string
      if (!token) {
        this.$reset()
        return
      }
      const saved = loadSavedProfile()
      this.token = token
      this.userId = saved.userId ?? null
      this.role = saved.role || ''
      this.nickname = saved.nickname || ''
      this.merchantId = saved.merchantId ?? null
      this.merchantName = saved.merchantName ?? ''
      this.customerId = saved.customerId ?? null
      this.customerName = saved.customerName ?? ''
      this.openid = saved.openid ?? ''
      this.workerId = saved.workerId ?? null
      this.workerCode = saved.workerCode ?? ''
    },

    buildLoginPayload(roleOverride?: UserRole) {
      const ctx = getEntryContext()
      return {
        merchantId: ctx.merchantId,
        activationToken: ctx.activationToken,
        inviteCode: ctx.inviteCode,
        roleOverride,
      }
    },

    async loginWithWechat() {
      const loginRes = await new Promise<UniApp.LoginRes>((resolve, reject) => {
        uni.login({
          provider: 'weixin',
          success: resolve,
          fail: (err) => reject(new Error(err.errMsg || '微信登录失败')),
        })
      })
      if (!loginRes.code) {
        throw new Error('获取微信 code 失败')
      }
      const payload = this.buildLoginPayload()
      const data = await wechatLogin({
        code: loginRes.code,
        merchantId: payload.merchantId,
        activationToken: payload.activationToken,
        inviteCode: payload.inviteCode,
      })
      clearEntryActivation()
      if (data.customerId) {
        clearEntryInvite()
      }
      this.applyLogin(data)
      return data
    },

    async loginWithDev(openid: string, nickname: string, role: UserRole) {
      const payload = this.buildLoginPayload(role)
      const data = await devLogin({
        openid,
        nickname,
        role: payload.roleOverride,
        merchantId: payload.merchantId,
        activationToken: payload.activationToken,
        inviteCode: payload.inviteCode,
      })
      clearEntryActivation()
      if (data.customerId) {
        clearEntryInvite()
      }
      this.applyLogin(data)
      return data
    },

    async signOut() {
      useBossAlertStore().reset()
      logout().catch(() => undefined)
      this.$reset()
      uni.removeStorageSync('token')
      uni.removeStorageSync(PROFILE_KEY)
      uni.reLaunch({ url: '/pages/login/index' })
    },

    updateLocalNickname(nickname: string) {
      this.nickname = nickname
      const raw = uni.getStorageSync(PROFILE_KEY)
      if (raw) {
        const data = JSON.parse(raw as string) as LoginResult
        data.nickname = nickname
        uni.setStorageSync(PROFILE_KEY, JSON.stringify(data))
      }
    },

    updateLocalMerchantName(merchantName: string) {
      this.merchantName = merchantName
      const raw = uni.getStorageSync(PROFILE_KEY)
      if (raw) {
        const data = JSON.parse(raw as string) as LoginResult
        data.merchantName = merchantName
        uni.setStorageSync(PROFILE_KEY, JSON.stringify(data))
      }
    },

    syncLocalProfile(profile: { contactName?: string; merchantName?: string }) {
      if (profile.contactName) {
        this.updateLocalNickname(profile.contactName)
      }
      if (profile.merchantName) {
        this.updateLocalMerchantName(profile.merchantName)
      }
    },

    applyCustomerBind(customerId: number, customerName?: string) {
      this.customerId = customerId
      if (customerName) {
        this.customerName = customerName
      }
      clearEntryInvite()
      clearEntryRegister()
      const raw = uni.getStorageSync(PROFILE_KEY)
      if (raw) {
        const data = JSON.parse(raw as string) as LoginResult & { customerName?: string }
        data.customerId = customerId
        if (customerName) {
          data.customerName = customerName
        }
        uni.setStorageSync(PROFILE_KEY, JSON.stringify(data))
      }
    },

    async resolveCustomerHome(): Promise<string> {
      if (this.customerId) {
        return '/pages/customer/home/index'
      }
      const ctx = getEntryContext()
      if (ctx.registerToken) {
        return '/pages/customer/register/index'
      }
      if (ctx.inviteCode) {
        return '/pages/customer/bind/index'
      }
      try {
        const status = await fetchCustomerRegisterStatus()
        if (status.bound && status.customerId) {
          this.applyCustomerBind(status.customerId, status.customerName)
          return '/pages/customer/home/index'
        }
        if (status.pendingReview || status.lastRequestStatus === 'REJECTED') {
          return '/pages/customer/register/index'
        }
      } catch {
        // ignore
      }
      return '/pages/customer/home/index'
    },

    navigateHome() {
      if (!this.role) {
        uni.reLaunch({ url: '/pages/login/index' })
        return Promise.resolve()
      }
      if (this.role === 'CUSTOMER') {
        return this.resolveCustomerHome().then((url) => new Promise<void>((resolve, reject) => {
          uni.redirectTo({
            url,
            success: () => resolve(),
            fail: (err) => reject(new Error(err.errMsg || '页面跳转失败')),
          })
        }))
      }
      return new Promise<void>((resolve, reject) => {
        uni.redirectTo({
          url: homeByRole(this.role, this.customerId),
          success: () => resolve(),
          fail: (err) => reject(new Error(err.errMsg || '页面跳转失败')),
        })
      })
    },
  },
})
