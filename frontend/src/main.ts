import { createSSRApp } from 'vue'
import { createPinia } from 'pinia'
import uviewPlus from 'uview-plus'
import App from './App.vue'

function installUnhandledRejectionGuard() {
  // #ifdef MP-WEIXIN
  const isBenignNetworkError = (message: string) =>
    message.includes('timeout')
    || message.includes('连接超时')
    || message.includes('REQUEST_STALE')
    || message.includes('REQUEST_ABORTED')

  wx.onUnhandledRejection((res) => {
    const reason = res.reason as { message?: string; errMsg?: string } | string | undefined
    const message = typeof reason === 'string'
      ? reason
      : `${reason?.message || ''}${reason?.errMsg || ''}`
    if (isBenignNetworkError(message)) {
      return undefined
    }
  })

  wx.onError((message) => {
    if (typeof message === 'string' && isBenignNetworkError(message)) {
      return undefined
    }
  })
  // #endif
}

export function createApp() {
  installUnhandledRejectionGuard()
  const app = createSSRApp(App)
  const pinia = createPinia()
  app.use(pinia)
  app.use(uviewPlus, () => ({
    options: {
      iconUrl: '/static/fonts/uview-icon.ttf',
      loadFontOnce: true,
    },
  }))
  return { app }
}
