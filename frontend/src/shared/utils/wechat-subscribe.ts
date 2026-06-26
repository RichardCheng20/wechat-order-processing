import { fetchMiniProgramConfig } from '@common/api/config'

export async function requestOrderNotifySubscribe() {
  try {
    const config = await fetchMiniProgramConfig()
    const tmplId = config.orderNotifyTemplateId
    if (!tmplId) {
      uni.showToast({ title: '未配置订阅消息模板', icon: 'none' })
      return false
    }
    // #ifdef MP-WEIXIN
    return await new Promise<boolean>((resolve) => {
      uni.requestSubscribeMessage({
        tmplIds: [tmplId],
        success: (res) => {
          const accepted = res[tmplId] === 'accept'
          uni.showToast({
            title: accepted ? '已开启订单微信提醒' : '未授权将无法收到微信提醒',
            icon: 'none',
            duration: 2500,
          })
          resolve(accepted)
        },
        fail: () => {
          uni.showToast({ title: '授权失败', icon: 'none' })
          resolve(false)
        },
      })
    })
    // #endif
    // #ifndef MP-WEIXIN
    uni.showToast({ title: '请在微信小程序中使用', icon: 'none' })
    return false
    // #endif
  } catch (e) {
    uni.showToast({
      title: e instanceof Error ? e.message : '开启提醒失败',
      icon: 'none',
    })
    return false
  }
}
