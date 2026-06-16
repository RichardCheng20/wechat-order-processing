import { defineStore } from 'pinia'
import { devLogin, logout, wechatLogin, type LoginResult } from '../api/auth'

export type UserRole = 'CUSTOMER' | 'OWNER_ADMIN' | 'PARTNER_ADMIN' | 'WORKER'

interface UserState {
  token: string
  userId: number | null
  role: UserRole | ''
  nickname: string
  merchantId: number | null
  customerId: number | null
  customerName: string
  openid: string
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
      customerId: data.customerId ?? null,
      customerName: profile.customerName || '',
      openid: data.openid || '',
    }
  } catch {
    return {}
  }
}

function homeByRole(role: UserRole, customerId?: number | null) {
  switch (role) {
    case 'OWNER_ADMIN':
    case 'PARTNER_ADMIN':
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
      customerId: saved.customerId ?? null,
      customerName: saved.customerName ?? '',
      openid: saved.openid ?? '',
    }
  },

  getters: {
    displayName: (state) => state.customerName || state.nickname || '客户',
    isLoggedIn: (state) => !!state.token && !!state.role,
    isBoss: (state) => state.role === 'OWNER_ADMIN' || state.role === 'PARTNER_ADMIN',
    isCustomer: (state) => state.role === 'CUSTOMER',
    isCustomerBound: (state) => state.role !== 'CUSTOMER' || !!state.customerId,
  },

  actions: {
    applyLogin(data: LoginResult) {
      this.token = data.token
      this.userId = data.userId
      this.role = data.role as UserRole
      this.nickname = data.nickname
      this.merchantId = data.merchantId
      this.customerId = data.customerId ?? null
      this.customerName = (data as LoginResult & { customerName?: string }).customerName || ''
      this.openid = data.openid || ''
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
      this.customerId = saved.customerId ?? null
      this.customerName = saved.customerName ?? ''
      this.openid = saved.openid ?? ''
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
      const data = await wechatLogin(loginRes.code)
      this.applyLogin(data)
      return data
    },

    async loginWithDev(openid: string, nickname: string, role: UserRole) {
      const data = await devLogin({ openid, nickname, role })
      this.applyLogin(data)
      return data
    },

    async signOut() {
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

    applyCustomerBind(customerId: number, customerName?: string) {
      this.customerId = customerId
      if (customerName) {
        this.customerName = customerName
      }
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

    navigateHome() {
      if (!this.role) {
        uni.reLaunch({ url: '/pages/login/index' })
        return Promise.resolve()
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
