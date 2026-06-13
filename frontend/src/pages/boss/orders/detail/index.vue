<template>
  <view class="page">
    <view v-if="loading" class="loading-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <view v-else-if="order" class="content">
      <view class="header">
        <view class="row-top">
          <text class="customer-name">{{ order.customerName }}</text>
          <u-tag :text="order.statusLabel" :type="statusType(order.status)" size="mini" />
        </view>
        <text class="meta">{{ order.orderNo }}</text>
        <text class="meta">{{ order.deliveryAddressShort || '—' }}</text>
        <text class="meta">{{ formatTime(order.createdAt) }}</text>
        <text v-if="order.remark" class="remark">备注：{{ order.remark }}</text>
        <text v-if="order.amount != null" class="amount">订单金额 ¥{{ Number(order.amount).toFixed(2) }}</text>
      </view>

      <view class="items">
        <text class="section-title">下单明细</text>
        <view v-for="line in order.items || []" :key="line.id" class="line-item">
          <view class="line-main">
            <text class="line-name">{{ line.productName }}</text>
            <text class="line-qty">{{ line.orderQty }}{{ line.unit }}</text>
          </view>
          <text v-if="line.dealPrice != null" class="line-price">¥{{ Number(line.dealPrice).toFixed(2) }}/{{ line.unit }}</text>
        </view>
      </view>

      <view class="actions">
        <u-button
          v-if="order.status === 'PENDING_CONFIRM'"
          type="primary"
          text="确认订单"
          @click="handleConfirm"
        />
        <u-button
          v-if="canPrice"
          type="warning"
          text="去录价"
          @click="goPricing"
        />
        <u-button
          v-if="order.status === 'PRICED'"
          type="success"
          text="推送给客户"
          :loading="publishing"
          @click="handlePublish"
        />
        <u-button
          v-if="order.status === 'PENDING_PICK' || order.status === 'PICKING'"
          type="primary"
          plain
          text="派单"
          @click="goBackToAssign"
        />
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import { confirmBossOrder, fetchBossOrderDetail, type OrderInfo } from '../../../../api/order'
import { publishOrderPricing } from '../../../../api/pricing'
import { useUserStore } from '../../../../stores/user'

const userStore = useUserStore()
const order = ref<OrderInfo | null>(null)
const loading = ref(false)
const publishing = ref(false)
const orderId = ref(0)

const canPrice = computed(() => {
  if (!order.value) return false
  return ['PENDING_CONFIRM', 'PENDING_PICK', 'PENDING_PRICE'].includes(order.value.status)
    && order.value.amount == null
})

onLoad(async (query) => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  orderId.value = Number(query?.id || 0)
  await loadOrder()
})

async function loadOrder() {
  if (!orderId.value) return
  loading.value = true
  try {
    order.value = await fetchBossOrderDetail(orderId.value)
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function handleConfirm() {
  try {
    order.value = await confirmBossOrder(orderId.value)
    uni.showToast({ title: '已确认', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '确认失败', icon: 'none' })
  }
}

function goPricing() {
  uni.navigateTo({ url: `/pages/boss/pricing/detail/index?id=${orderId.value}` })
}

async function handlePublish() {
  publishing.value = true
  try {
    order.value = await publishOrderPricing(orderId.value)
    uni.showToast({ title: '已推送给客户', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '推送失败', icon: 'none' })
  } finally {
    publishing.value = false
  }
}

function goBackToAssign() {
  uni.navigateBack()
}

function statusType(status: string) {
  if (status === 'PENDING_CONFIRM') return 'warning'
  if (status === 'PRICED') return 'success'
  if (status === 'COMPLETED') return 'success'
  return 'primary'
}

function formatTime(value?: string) {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding-bottom: 40rpx;
}

.loading-wrap {
  padding: 80rpx 0;
  text-align: center;
}

.content {
  padding: 24rpx;
}

.header,
.items {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
}

.row-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.customer-name {
  font-size: 36rpx;
  font-weight: 600;
}

.meta {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #666;
}

.remark {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #e67e22;
}

.amount {
  display: block;
  margin-top: 16rpx;
  font-size: 32rpx;
  font-weight: 600;
  color: #27ae60;
}

.section-title {
  display: block;
  margin-bottom: 20rpx;
  font-size: 30rpx;
  font-weight: 600;
}

.line-item {
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f2f3f5;
}

.line-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.line-name {
  font-size: 30rpx;
}

.line-qty {
  font-size: 28rpx;
  color: #666;
}

.line-price {
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
  color: #e67e22;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}
</style>
