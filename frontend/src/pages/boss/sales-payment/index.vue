<template>
  <view class="page">
    <view class="header">
      <text class="title">销售记账</text>
      <text class="subtitle">记录客户收款、欠款与对账</text>
    </view>
    <view class="placeholder">
      <u-empty mode="data" text="销售记账开发中" />
      <text class="hint">后续可在此登记客户付款、冲减应收</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '../../../stores/user'

const userStore = useUserStore()

onShow(() => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
  }
})
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx;
}

.header {
  margin-bottom: 24rpx;
}

.title {
  display: block;
  font-size: 36rpx;
  font-weight: 600;
}

.subtitle {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #888;
}

.placeholder {
  background: #fff;
  border-radius: 16rpx;
  padding: 80rpx 24rpx;
  text-align: center;
}

.hint {
  display: block;
  margin-top: 16rpx;
  font-size: 24rpx;
  color: #999;
}
</style>
