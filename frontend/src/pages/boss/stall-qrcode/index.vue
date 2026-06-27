<template>
  <view class="page">
    <view v-if="loading" class="state-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <view v-else-if="qrInfo" class="content">
      <view class="mp-brand">
        <view class="mp-logo">菜</view>
        <text class="mp-name">{{ qrInfo.miniProgramName || '蔬菜批发' }}</text>
        <text class="mp-tag">档口长期下单码</text>
      </view>

      <text class="subtitle">
        {{ qrInfo.merchantName ? `${qrInfo.merchantName} · 扫码即可下单` : '贴于档口，路人或临时客户扫码下单' }}
      </text>

      <view class="qr-frame">
        <image
          v-if="qrSrc"
          class="qr-image"
          :src="qrSrc"
          mode="aspectFit"
          show-menu-by-longpress
        />
        <view v-else class="qr-placeholder">
          <text class="qr-placeholder-title">小程序码暂未生成</text>
          <text class="qr-placeholder-tip">{{ qrInfo.qrErrorHint || '请确认服务器已配置微信 AppSecret' }}</text>
          <u-button size="small" type="primary" text="重新加载" :loading="loading" @click="loadQr" />
        </view>
      </view>

      <text v-if="qrSrc" class="scan-hint">微信扫一扫 · 打开小程序直接选购下单</text>
      <text class="scan-hint secondary">长期有效，可打印张贴；长按二维码保存或转发</text>

      <view class="tips-card">
        <text class="tips-title">使用说明</text>
        <text class="tips-line">· 未绑定档案的客户可临时下单（需填写店铺名称）</text>
        <text class="tips-line">· 合作 VIP 客户请使用客户档案中的「VIP 专属码」绑定</text>
        <text class="tips-line">· 需审核的新客户请使用客户管理中的「邀请注册」</text>
      </view>

      <view class="actions">
        <u-button
          v-if="qrSrc"
          type="primary"
          text="保存二维码到相册"
          @click="saveQr"
        />
        <u-button text="重新加载" :loading="loading" @click="loadQr" />
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchStallOrderQrcode, type StallOrderQrResult } from '@common/api/profile'
import { useUserStore } from '@common/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const qrInfo = ref<StallOrderQrResult | null>(null)

const qrSrc = computed(() => {
  const b64 = qrInfo.value?.qrCodeBase64
  if (!b64) return ''
  return b64.startsWith('data:') ? b64 : `data:image/png;base64,${b64}`
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  await loadQr()
})

async function loadQr() {
  loading.value = true
  try {
    qrInfo.value = await fetchStallOrderQrcode()
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function saveQr() {
  const base64 = qrInfo.value?.qrCodeBase64
  if (!base64) {
    uni.showToast({ title: '暂无二维码', icon: 'none' })
    return
  }
  // #ifdef MP-WEIXIN
  const filePath = `${wx.env.USER_DATA_PATH}/stall-order-qr.png`
  wx.getFileSystemManager().writeFile({
    filePath,
    data: base64,
    encoding: 'base64',
    success: () => {
      uni.saveImageToPhotosAlbum({
        filePath,
        success: () => uni.showToast({ title: '已保存到相册', icon: 'success' }),
        fail: () => uni.showToast({ title: '保存失败，请长按图片保存', icon: 'none' }),
      })
    },
    fail: () => uni.showToast({ title: '保存失败，请长按图片保存', icon: 'none' }),
  })
  // #endif
  // #ifndef MP-WEIXIN
  uni.showToast({ title: '请长按图片保存', icon: 'none' })
  // #endif
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 32rpx 28rpx calc(48rpx + env(safe-area-inset-bottom));
  background: #f5f7f3;
  box-sizing: border-box;
}

.state-wrap {
  padding: 120rpx 0;
  display: flex;
  justify-content: center;
}

.content {
  background: #fff;
  border-radius: 20rpx;
  padding: 40rpx 32rpx 48rpx;
  border: 1rpx solid #dce6df;
}

.mp-brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
  margin-bottom: 16rpx;
}

.mp-logo {
  width: 88rpx;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 20rpx;
  background: linear-gradient(135deg, #07c160, #05a050);
  color: #fff;
  font-size: 40rpx;
  font-weight: 700;
  text-align: center;
}

.mp-name {
  font-size: 36rpx;
  font-weight: 600;
  color: #222;
}

.mp-tag {
  font-size: 22rpx;
  color: #0b7f3a;
  padding: 4rpx 16rpx;
  border: 1rpx solid #b7eb8f;
  border-radius: 999rpx;
  background: #f6ffed;
}

.subtitle {
  display: block;
  margin-bottom: 28rpx;
  text-align: center;
  font-size: 26rpx;
  color: #66736b;
  line-height: 1.5;
}

.qr-frame {
  padding: 24rpx;
  background: #fff;
  border: 1rpx solid #eee;
  border-radius: 16rpx;
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.06);
}

.qr-image {
  width: 480rpx;
  height: 480rpx;
  margin: 0 auto;
  display: block;
}

.qr-placeholder {
  min-height: 360rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  padding: 32rpx;
}

.qr-placeholder-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
}

.qr-placeholder-tip {
  font-size: 24rpx;
  color: #999;
  text-align: center;
  line-height: 1.5;
}

.scan-hint {
  display: block;
  margin-top: 24rpx;
  text-align: center;
  font-size: 26rpx;
  color: #333;
}

.scan-hint.secondary {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #999;
}

.tips-card {
  margin-top: 32rpx;
  padding: 24rpx;
  background: #f8faf8;
  border-radius: 12rpx;
  border: 1rpx solid #eef2ed;
}

.tips-title {
  display: block;
  margin-bottom: 12rpx;
  font-size: 26rpx;
  font-weight: 600;
  color: #17211b;
}

.tips-line {
  display: block;
  font-size: 24rpx;
  color: #66736b;
  line-height: 1.6;
}

.actions {
  margin-top: 32rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}
</style>
