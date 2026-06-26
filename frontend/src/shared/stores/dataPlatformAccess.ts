import { defineStore } from 'pinia'
import {
  fetchDataPlatformPasswordStatus,
  verifyDataPlatformPassword,
  type DataPlatformPasswordStatus,
} from '@common/api/data-platform'

export const DATA_PLATFORM_PASSWORD_HEADER = 'X-Data-Platform-Password'

export const useDataPlatformAccessStore = defineStore('dataPlatformAccess', {
  state: () => ({
    status: null as DataPlatformPasswordStatus | null,
    verifiedPassword: '' as string,
    popupVisible: false,
    popupHint: '',
    pendingResolve: null as ((ok: boolean) => void) | null,
    passwordDraft: '',
    verifying: false,
    accessPending: false,
  }),

  actions: {
    clearPassword() {
      this.verifiedPassword = ''
    },

    async refreshStatus() {
      this.status = await fetchDataPlatformPasswordStatus()
      return this.status
    },

    async ensureAccess(): Promise<boolean> {
      if (this.accessPending) {
        return false
      }
      this.accessPending = true
      try {
        const status = await this.refreshStatus()
        if (!status.passwordEnabled) {
          await this.promptSetupPassword()
          return false
        }
        return await this.promptVerifyPassword()
      } catch (e) {
        uni.showToast({
          title: e instanceof Error ? e.message : '密码验证失败',
          icon: 'none',
        })
        return false
      } finally {
        this.accessPending = false
      }
    },

    promptSetupPassword() {
      return new Promise<boolean>((resolve) => {
        uni.showModal({
          title: '尚未设置密码',
          content: '请先在「设置 → 数据平台密码」中设置 6 位数字密码。',
          confirmText: '去设置',
          cancelText: '取消',
          success: (res) => {
            if (res.confirm) {
              uni.navigateTo({ url: '/pages/boss/settings/data-platform-password/index' })
            }
            resolve(false)
          },
          fail: () => resolve(false),
        })
      })
    },

    promptVerifyPassword() {
      if (this.popupVisible && this.pendingResolve) {
        return new Promise<boolean>((resolve) => {
          const prev = this.pendingResolve
          this.pendingResolve = (ok) => {
            prev?.(ok)
            resolve(ok)
          }
        })
      }
      this.popupHint = ''
      this.passwordDraft = ''
      this.popupVisible = true
      return new Promise<boolean>((resolve) => {
        this.pendingResolve = resolve
      })
    },

    cancelPopup() {
      if (!this.popupVisible) return
      this.popupVisible = false
      this.passwordDraft = ''
      this.pendingResolve?.(false)
      this.pendingResolve = null
    },

    async submitPopup() {
      const password = this.passwordDraft.trim()
      if (!/^\d{6}$/.test(password)) {
        this.popupHint = '请输入 6 位数字密码'
        return
      }
      this.verifying = true
      try {
        await verifyDataPlatformPassword(password)
        this.verifiedPassword = password
        this.popupVisible = false
        this.passwordDraft = ''
        this.popupHint = ''
        this.pendingResolve?.(true)
        this.pendingResolve = null
      } catch (e) {
        this.popupHint = e instanceof Error ? e.message : '密码错误'
      } finally {
        this.verifying = false
      }
    },
  },
})
