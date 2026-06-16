<template>
  <view class="page">
    <view class="header">
      <text class="title">供应商管理</text>
      <u-button size="small" type="primary" text="新建" @click="showComingSoon('新建供应商')" />
    </view>
    <view class="placeholder">
      <u-empty mode="data" text="供应商模块开发中" />
      <text class="hint">后续可维护供应商档案、采购价与对账</text>
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

function showComingSoon(name: string) {
  uni.showToast({ title: `${name}即将上线`, icon: 'none' })
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.title {
  font-size: 36rpx;
  font-weight: 600;
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
