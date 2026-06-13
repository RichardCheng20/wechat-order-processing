<template>
  <view class="page">
    <view class="header">
      <text class="title">待录价订单</text>
      <text class="subtitle">按实际分拣数量录入成交单价，提交后自动计算订单金额</text>
    </view>

    <view v-if="loading" class="loading-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <view v-else-if="orders.length === 0" class="empty-wrap">
      <u-empty mode="order" text="暂无待录价订单" />
    </view>

    <view v-else class="list">
      <view
        v-for="item in orders"
        :key="item.id"
        class="card"
        @tap="goDetail"
        :data-id="item.id"
      >
        <view class="card-top">
          <text class="customer-name">{{ item.customerName }}</text>
          <u-tag text="待录价" type="warning" size="mini" />
        </view>
        <text class="meta">{{ item.orderNo }}</text>
        <text class="meta">{{ item.deliveryAddressShort || '—' }}</text>
        <text class="meta">{{ formatTime(item.createdAt) }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { fetchPendingPriceOrders, type PricingOrder } from '../../../api/pricing'
import { useUserStore } from '../../../stores/user'

const userStore = useUserStore()
const orders = ref<PricingOrder[]>([])
const loading = ref(false)

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  loading.value = true
  try {
    orders.value = await fetchPendingPriceOrders()
  } finally {
    loading.value = false
  }
})

function goDetail(e: { currentTarget: { dataset: { id?: string | number } } }) {
  uni.navigateTo({ url: `/pages/boss/pricing/detail/index?id=${e.currentTarget.dataset.id}` })
}

function formatTime(value?: string) {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<style scoped lang="scss">
.page {
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

.loading-wrap,
.empty-wrap {
  padding: 80rpx 0;
  text-align: center;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 16rpx;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.customer-name {
  font-size: 32rpx;
  font-weight: 600;
}

.meta {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #666;
}
</style>
