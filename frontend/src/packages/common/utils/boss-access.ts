import { useUserStore } from '@common/stores/user'

/** 数据平台页面：仅老板（OWNER_ADMIN）可访问 */
export function guardOwnerAdminPage(): boolean {
  const userStore = useUserStore()
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/packages/common/login/index' })
    return false
  }
  if (!userStore.isOwnerAdmin) {
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
  return true
}
