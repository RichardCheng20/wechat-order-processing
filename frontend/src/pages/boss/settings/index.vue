<template>
  <view class="page">
    <view class="section">
      <text class="section-title">基础设置</text>
      <view class="cell" @tap="goProfile">
        <text>个人信息</text>
        <text class="arrow">›</text>
      </view>
      <view class="cell" @tap="showComingSoon('店铺信息')">
        <text>店铺信息</text>
        <text class="arrow">›</text>
      </view>
      <view class="cell" @tap="showComingSoon('打印设置')">
        <text>打印设置</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <view class="section">
      <text class="section-title">通用</text>
      <view class="cell" @tap="goMessages">
        <text>消息通知</text>
        <text class="arrow">›</text>
      </view>
      <view class="cell" @tap="showComingSoon('帮助中心')">
        <text>帮助中心</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <view class="section">
      <text class="section-title">关于</text>
      <view class="cell">
        <text>当前版本</text>
        <text class="value">MVP 0.1.0</text>
      </view>
      <view class="cell" @tap="showComingSoon('检查更新')">
        <text>检查更新</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <view class="logout-wrap">
      <u-button type="error" plain text="退出登录" @click="handleLogout" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@common/stores/user'

const userStore = useUserStore()

onShow(() => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/packages/common/login/index' })
  }
})

function showComingSoon(name: string) {
  uni.showToast({ title: `${name}即将完善`, icon: 'none' })
}

function goProfile() {
  uni.navigateTo({ url: '/pages/boss/profile/index' })
}

function goMessages() {
  uni.navigateTo({ url: '/pages/boss/messages/index' })
}

function handleLogout() {
  userStore.signOut()
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx;
}

.section {
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 24rpx;
  overflow: hidden;
}

.section-title {
  display: block;
  padding: 24rpx 28rpx 12rpx;
  font-size: 26rpx;
  color: #999;
}

.cell {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx;
  border-top: 1rpx solid #f2f3f5;
  font-size: 30rpx;
}

.arrow,
.value {
  color: #999;
}

.logout-wrap {
  margin-top: 40rpx;
  padding: 0 24rpx;
}
</style>
