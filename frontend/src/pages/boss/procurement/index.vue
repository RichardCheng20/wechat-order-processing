<template>
  <view class="page">
    <view class="header">
      <text class="title">采购</text>
      <text class="subtitle">供应商采购、入库管理</text>
    </view>
    <view class="placeholder">
      <u-empty mode="data" text="采购模块开发中" />
      <text class="hint">后续可在此记录向供应商的进货单</text>
    </view>
    <BossTabbar active="procurement" />
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import BossTabbar from '../../../components/boss-tabbar/index.vue'
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
  padding: 24rpx 24rpx calc(140rpx + env(safe-area-inset-bottom));
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
