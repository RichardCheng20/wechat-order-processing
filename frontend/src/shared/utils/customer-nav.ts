export type CustomerTab = 'home' | 'orders' | 'mine'

export const CUSTOMER_TAB_HEIGHT_RPX = 100

export const CUSTOMER_TAB_PATHS: Record<CustomerTab, string> = {
  home: '/pages/customer/home/index',
  orders: '/pages/customer/orders/index',
  mine: '/pages/customer/mine/index',
}

export function switchCustomerTab(tab: CustomerTab) {
  const pages = getCurrentPages()
  const currentRoute = pages[pages.length - 1]?.route || ''
  const targetRoute = CUSTOMER_TAB_PATHS[tab].replace(/^\//, '')
  if (currentRoute === targetRoute) return
  uni.redirectTo({ url: CUSTOMER_TAB_PATHS[tab] })
}

export function customerTabBarHeightPx() {
  return uni.upx2px(CUSTOMER_TAB_HEIGHT_RPX) + (uni.getSystemInfoSync().safeAreaInsets?.bottom || 0)
}
