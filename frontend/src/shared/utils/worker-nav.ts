export type WorkerTab = 'pending' | 'picked' | 'mine'

export const WORKER_TAB_HEIGHT_RPX = 100

export const WORKER_TAB_PATHS: Record<WorkerTab, string> = {
  pending: '/pages/worker/tasks/index',
  picked: '/pages/worker/picked/index',
  mine: '/pages/worker/mine/index',
}

export function switchWorkerTab(tab: WorkerTab) {
  const pages = getCurrentPages()
  const currentRoute = pages[pages.length - 1]?.route || ''
  const targetRoute = WORKER_TAB_PATHS[tab].replace(/^\//, '')
  if (currentRoute === targetRoute) return
  uni.redirectTo({ url: WORKER_TAB_PATHS[tab] })
}

export function workerTabBarHeightPx() {
  return uni.upx2px(WORKER_TAB_HEIGHT_RPX) + (uni.getSystemInfoSync().safeAreaInsets?.bottom || 0)
}
