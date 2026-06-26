import { useBossAlertStore } from '@common/stores/bossAlert'
import { useUserStore } from '@common/stores/user'

const NOTIFY_AUDIO = '/static/audio/order-notify.wav'

let audioCtx: UniApp.InnerAudioContext | null = null

export function playOrderNotifySound() {
  try {
    if (!audioCtx) {
      audioCtx = uni.createInnerAudioContext()
      audioCtx.src = NOTIFY_AUDIO
      audioCtx.volume = 1
    }
    audioCtx.stop()
    audioCtx.play()
    uni.vibrateShort({ type: 'medium' })
  } catch {
    uni.vibrateShort({ type: 'medium' })
  }
}

export function destroyOrderNotifySound() {
  if (audioCtx) {
    audioCtx.destroy()
    audioCtx = null
  }
}

export async function refreshBossOrderAlert(options?: { notify?: boolean }) {
  const userStore = useUserStore()
  if (!userStore.isLoggedIn || !userStore.isBoss) return 0
  const store = useBossAlertStore()
  const count = await store.refresh()
  if (options?.notify !== false) {
    store.maybeNotify()
  }
  return count
}

export function useBossOrderAlertOnShow(options?: { poll?: boolean }) {
  const userStore = useUserStore()
  const store = useBossAlertStore()

  async function onBossPageShow() {
    if (!userStore.isLoggedIn || !userStore.isBoss) return
    await store.checkOnShow()
    if (options?.poll) {
      store.startPolling()
    }
  }

  function onBossPageHide() {
    if (options?.poll) {
      store.stopPolling()
    }
  }

  return { onBossPageShow, onBossPageHide }
}
