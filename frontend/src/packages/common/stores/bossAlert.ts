import { defineStore } from 'pinia'
import { fetchBossPendingConfirmCount } from '@common/api/order'
import { playOrderNotifySound } from '@common/utils/boss-order-alert'

const ACK_COUNT_KEY = 'boss_pending_confirm_ack_count'
const PROFILE_KEY = 'user_profile'

function isBossSession(): boolean {
  const token = uni.getStorageSync('token')
  if (!token) return false
  try {
    const raw = uni.getStorageSync(PROFILE_KEY)
    if (!raw) return false
    const data = JSON.parse(raw as string) as { role?: string }
    return data.role === 'OWNER_ADMIN' || data.role === 'STALL_MANAGER'
  } catch {
    return false
  }
}

function isPermissionError(error: unknown): boolean {
  if (!(error instanceof Error)) return false
  return error.message.includes('管理员权限') || error.message.includes('需要客户权限')
}

export const useBossAlertStore = defineStore('bossAlert', {
  state: () => ({
    pendingConfirmCount: 0,
    modalVisible: false,
    refreshing: false,
    pollTimer: null as ReturnType<typeof setInterval> | null,
  }),

  getters: {
    badgeText(state): string {
      if (state.pendingConfirmCount <= 0) return ''
      if (state.pendingConfirmCount > 99) return '99+'
      return `+${state.pendingConfirmCount}`
    },
    hasPendingConfirm(state): boolean {
      return state.pendingConfirmCount > 0
    },
  },

  actions: {
    async refresh() {
      if (!isBossSession()) {
        this.pendingConfirmCount = 0
        this.modalVisible = false
        this.stopPolling()
        return 0
      }
      if (this.refreshing) return this.pendingConfirmCount
      this.refreshing = true
      try {
        const count = await fetchBossPendingConfirmCount()
        this.pendingConfirmCount = count
        return count
      } catch (error) {
        if (isPermissionError(error)) {
          this.pendingConfirmCount = 0
          this.modalVisible = false
          this.stopPolling()
          return 0
        }
        return this.pendingConfirmCount
      } finally {
        this.refreshing = false
      }
    },

    maybeNotify() {
      if (this.pendingConfirmCount <= 0) {
        this.modalVisible = false
        return
      }
      const ack = Number(uni.getStorageSync(ACK_COUNT_KEY) || 0)
      if (this.pendingConfirmCount > ack) {
        playOrderNotifySound()
        this.modalVisible = true
      }
    },

    async checkOnShow() {
      if (!isBossSession()) {
        this.stopPolling()
        return
      }
      await this.refresh()
      this.maybeNotify()
    },

    dismissModal() {
      this.modalVisible = false
      uni.setStorageSync(ACK_COUNT_KEY, this.pendingConfirmCount)
    },

    goHandlePending() {
      this.modalVisible = false
      uni.setStorageSync(ACK_COUNT_KEY, this.pendingConfirmCount)
      uni.reLaunch({ url: '/pages/boss/orders/index?flow=confirm' })
    },

    startPolling(intervalMs = 30000) {
      if (!isBossSession()) {
        this.stopPolling()
        return
      }
      this.stopPolling()
      this.pollTimer = setInterval(() => {
        if (!isBossSession()) {
          this.stopPolling()
          this.pendingConfirmCount = 0
          this.modalVisible = false
          return
        }
        this.refresh()
          .then((count) => {
            const ack = Number(uni.getStorageSync(ACK_COUNT_KEY) || 0)
            if (count > ack) {
              this.maybeNotify()
            }
          })
          .catch(() => undefined)
      }, intervalMs)
    },

    stopPolling() {
      if (this.pollTimer != null) {
        clearInterval(this.pollTimer)
        this.pollTimer = null
      }
    },

    reset() {
      this.stopPolling()
      this.pendingConfirmCount = 0
      this.modalVisible = false
      this.refreshing = false
    },

    resetAck() {
      uni.removeStorageSync(ACK_COUNT_KEY)
    },
  },
})
