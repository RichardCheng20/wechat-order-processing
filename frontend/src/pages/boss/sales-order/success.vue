<template>
  <view class="page">
    <view class="success-icon">✓</view>
    <text class="success-title">提交成功</text>
    <text v-if="customerName" class="success-sub">已为 {{ customerName }} 创建订单</text>

    <button class="primary-btn" @tap="createAnother">再开一单</button>
    <button class="outline-btn" @tap="showComingSoon('添加凭证')">添加凭证</button>

    <view class="links">
      <text class="link" @tap="goHome">返回首页</text>
      <text class="divider">|</text>
      <text class="link" @tap="goDetail">查看详情</text>
    </view>

    <view class="sync-card">
      <view class="sync-icon">¥↻</view>
      <view class="sync-info">
        <text class="sync-title">更新报价单</text>
        <text class="sync-sub">同步商品价格至该客户的报价单</text>
      </view>
      <text class="sync-btn" @tap="syncQuote">一键同步</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useSalesOrderStore } from '../../../stores/salesOrder'
import { useUserStore } from '../../../stores/user'
import { syncQuoteFromOrder } from '../../../api/quote'

const userStore = useUserStore()
const salesOrder = useSalesOrderStore()
const orderId = ref(0)
const customerName = ref('')

onLoad((query) => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  orderId.value = Number(query?.id || 0)
  customerName.value = decodeURIComponent(String(query?.customer || ''))
})

function createAnother() {
  salesOrder.reset()
  uni.redirectTo({ url: '/pages/boss/sales-order/index' })
}

function goHome() {
  uni.reLaunch({ url: '/pages/boss/orders/index' })
}

function goDetail() {
  if (!orderId.value) {
    goHome()
    return
  }
  uni.redirectTo({ url: `/pages/boss/orders/detail/index?id=${orderId.value}` })
}

function showComingSoon(name: string) {
  uni.showToast({ title: `${name}即将上线`, icon: 'none' })
}

async function syncQuote() {
  if (!orderId.value) {
    uni.showToast({ title: '订单不存在', icon: 'none' })
    return
  }
  try {
    const result = await syncQuoteFromOrder(orderId.value)
    uni.showToast({ title: `已同步 ${result.syncedCount} 个商品价`, icon: 'success' })
  } catch (err) {
    uni.showToast({ title: err instanceof Error ? err.message : '同步失败', icon: 'none' })
  }
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 80rpx 48rpx;
  background: #fff;
  text-align: center;
}

.success-icon {
  width: 120rpx;
  height: 120rpx;
  margin: 0 auto 32rpx;
  border-radius: 50%;
  background: #07c160;
  color: #fff;
  font-size: 64rpx;
  line-height: 120rpx;
}

.success-title {
  display: block;
  font-size: 40rpx;
  font-weight: 700;
}

.success-sub {
  display: block;
  margin-top: 16rpx;
  font-size: 28rpx;
  color: #666;
}

.primary-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  margin-top: 64rpx;
  background: #07c160;
  color: #fff;
  font-size: 32rpx;
  border-radius: 999rpx;
  border: none;
}

.outline-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  margin-top: 24rpx;
  background: #fff;
  color: #07c160;
  font-size: 32rpx;
  border-radius: 999rpx;
  border: 2rpx solid #07c160;
}

.primary-btn::after,
.outline-btn::after {
  border: none;
}

.links {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 48rpx;
  gap: 24rpx;
}

.link {
  font-size: 28rpx;
  color: #2979ff;
}

.divider {
  color: #ddd;
}

.sync-card {
  display: flex;
  align-items: center;
  margin-top: 80rpx;
  padding: 28rpx;
  background: #fafafa;
  border-radius: 16rpx;
  text-align: left;
}

.sync-icon {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: #fff3e6;
  color: #e67e22;
  font-size: 28rpx;
  line-height: 72rpx;
  text-align: center;
  flex-shrink: 0;
}

.sync-info {
  flex: 1;
  margin: 0 20rpx;
}

.sync-title {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
}

.sync-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #999;
}

.sync-btn {
  padding: 12rpx 20rpx;
  font-size: 24rpx;
  color: #e67e22;
  border: 1rpx solid #e67e22;
  border-radius: 999rpx;
  flex-shrink: 0;
}
</style>
