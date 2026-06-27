<template>
  <view class="page">
    <view class="profile-card">
      <view class="avatar">{{ avatarText }}</view>
      <view class="profile-info">
        <text class="name">{{ userStore.displayName }}</text>
        <text class="sub">{{ bindStatusText }}</text>
      </view>
    </view>

    <view class="menu-card">
      <view v-if="!userStore.customerId" class="menu-item" @tap="goBind">
        <text class="menu-label">成为 VIP 客户</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="goOrders">
        <text class="menu-label">我的订单</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="goHome">
        <text class="menu-label">去下单</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <view class="logout-wrap">
      <button class="logout-btn" @tap="handleLogout">退出登录</button>
    </view>

    <CustomerTabBar active="mine" />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchBindStatus } from '@common/api/customer'
import CustomerTabBar from '@/components/CustomerTabBar.vue'
import { switchCustomerTab } from '@common/utils/customer-nav'
import { useUserStore } from '@common/stores/user'

const userStore = useUserStore()
const bound = ref(false)

const avatarText = computed(() => {
  const name = userStore.displayName || '客'
  return name.slice(0, 1)
})

const bindStatusText = computed(() => {
  if (userStore.customerId) {
    return `已绑定：${userStore.customerName || userStore.displayName}`
  }
  if (bound.value) {
    return '已绑定客户档案'
  }
  return '临时客户 · 下单时需填写店铺名称'
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isCustomer) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  try {
    const status = await fetchBindStatus()
    bound.value = status.bound
    if (status.bound && status.customerId) {
      userStore.applyCustomerBind(status.customerId, status.customerName)
    } else if (userStore.customerId) {
      userStore.clearCustomerBind()
    }
  } catch {
    // ignore
  }
})

function goBind() {
  uni.navigateTo({ url: '/pages/customer/bind/index' })
}

function goOrders() {
  switchCustomerTab('orders')
}

function goHome() {
  switchCustomerTab('home')
}

function handleLogout() {
  uni.showModal({
    title: '退出登录',
    content: '确定要退出当前账号吗？',
    success: (res) => {
      if (res.confirm) {
        userStore.signOut()
      }
    },
  })
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx;
  padding-bottom: calc(120rpx + env(safe-area-inset-bottom));
  background: #f5f7f3;
  box-sizing: border-box;
}

.profile-card {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 36rpx 28rpx;
  background: #0b7f3a;
  border-radius: 18rpx;
  color: #fff;
}

.avatar {
  width: 96rpx;
  height: 96rpx;
  line-height: 96rpx;
  text-align: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  font-size: 40rpx;
  font-weight: 700;
  flex-shrink: 0;
}

.profile-info {
  flex: 1;
  min-width: 0;
}

.name {
  display: block;
  font-size: 36rpx;
  font-weight: 700;
}

.sub {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  opacity: 0.92;
  line-height: 1.45;
}

.menu-card {
  margin-top: 24rpx;
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
  border: 1rpx solid #dce6df;
}

.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx 28rpx;
  border-bottom: 1rpx solid #eef2ed;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-label {
  font-size: 30rpx;
  color: #17211b;
}

.menu-arrow {
  font-size: 32rpx;
  color: #ccc;
}

.logout-wrap {
  margin-top: 48rpx;
}

.logout-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  margin: 0;
  padding: 0;
  background: #fff;
  color: #e74c3c;
  font-size: 30rpx;
  font-weight: 600;
  border-radius: 16rpx;
  border: 1rpx solid #dce6df;
}

.logout-btn::after {
  border: none;
}
</style>
