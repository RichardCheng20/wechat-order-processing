import { useUserStore } from '@common/stores/user'
import { useDataPlatformAccessStore } from '@common/stores/dataPlatformAccess'

/** 数据平台页面：主管理员与档口老板可访问；密码仅在「我的」页验证，此处只校验会话内密码 */
export async function guardOwnerAdminPage(): Promise<boolean> {
  const userStore = useUserStore()
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  if (!userStore.canViewDataPlatform) {
    uni.showToast({ title: '仅老板可查看经营数据', icon: 'none', duration: 2500 })
    setTimeout(() => {
      uni.navigateBack({
        fail: () => {
          uni.reLaunch({ url: '/pages/boss/orders/index' })
        },
      })
    }, 600)
    return false
  }
  const accessStore = useDataPlatformAccessStore()
  if (accessStore.verifiedPassword) {
    return true
  }
  uni.showToast({ title: '请先在「我的」验证数据平台密码', icon: 'none', duration: 2500 })
  setTimeout(() => {
    uni.navigateBack({
      fail: () => {
        uni.reLaunch({ url: '/pages/boss/mine/index' })
      },
    })
  }, 600)
  return false
}

export function clearDataPlatformPassword() {
  useDataPlatformAccessStore().clearPassword()
}
