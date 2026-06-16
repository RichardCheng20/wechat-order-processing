<template>
  <view class="page">
    <view class="header">
      <text class="title">采购开单</text>
      <text class="subtitle">向供应商下单进货</text>
    </view>
    <view class="placeholder">
      <u-empty mode="data" text="采购开单开发中" />
      <text class="hint">后续可在此选择供应商、录入进货商品与数量</text>
      <u-button class="link-btn" type="primary" plain text="查看供应商" @click="goSuppliers" />
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

function goSuppliers() {
  uni.navigateTo({ url: '/pages/boss/suppliers/index' })
}
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

.link-btn {
  margin-top: 32rpx;
}
</style>
