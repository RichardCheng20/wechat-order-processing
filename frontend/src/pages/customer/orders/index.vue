<template>
  <view class="page">
    <view v-if="loading" class="loading-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <view v-else-if="orders.length === 0" class="empty-wrap">
      <u-empty mode="order" text="暂无订单" />
      <u-button type="primary" plain text="去下单" @click="goHome" />
    </view>

    <view v-else class="list">
      <view
        v-for="item in orders"
        :key="item.id"
        class="card"
        :class="{ active: selectedId === item.id }"
        @tap="selectOrder"
        :data-id="item.id"
      >
        <view class="row-top">
          <text class="order-no">{{ item.orderNo }}</text>
          <u-tag :text="item.statusLabel" :type="statusType(item.status)" size="mini" />
        </view>
        <text class="meta">{{ formatTime(item.createdAt) }} · {{ item.itemCount || 0 }} 种商品</text>
        <text v-if="item.amount != null" class="amount">合计 ¥{{ Number(item.amount).toFixed(2) }}</text>
        <text v-else class="price-pending">金额待老板确认</text>
        <text v-if="item.remark" class="remark">备注：{{ item.remark }}</text>
      </view>
    </view>

    <view v-if="detail" class="detail-panel">
      <text class="detail-title">订单明细</text>
      <view v-for="line in detail.items || []" :key="line.id" class="line-item">
        <text class="line-name">{{ line.productName }}</text>
        <view class="line-right">
          <text class="line-qty">{{ line.orderQty }}{{ line.unit }}</text>
          <text v-if="line.dealPrice != null" class="line-price">¥{{ Number(line.dealPrice).toFixed(2) }}</text>
        </view>
      </view>
      <text v-if="detail.amount != null" class="detail-total">订单总额 ¥{{ Number(detail.amount).toFixed(2) }}</text>
      <text v-else class="price-pending detail-pending">金额待老板确认后展示</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onLoad, onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { fetchCustomerOrderDetail, fetchCustomerOrders, type OrderInfo } from '../../../api/order'
import { useUserStore } from '../../../stores/user'

const userStore = useUserStore()
const orders = ref<OrderInfo[]>([])
const detail = ref<OrderInfo | null>(null)
const selectedId = ref<number | null>(null)
const loading = ref(false)

onLoad(async (query) => {
  if (!userStore.isLoggedIn || !userStore.isCustomer) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  const id = query?.id ? Number(query.id) : null
  if (id) {
    selectedId.value = id
    await loadDetail(id)
  }
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isCustomer) {
    return
  }
  await loadOrders()
})

let loadingOrders = false

async function loadOrders() {
  if (loadingOrders) return
  loadingOrders = true
  loading.value = true
  try {
    orders.value = await fetchCustomerOrders()
  } catch {
    orders.value = []
  } finally {
    loading.value = false
    loadingOrders = false
  }
}

async function loadDetail(id: number) {
  try {
    detail.value = await fetchCustomerOrderDetail(id)
  } catch {
    detail.value = null
  }
}

function selectOrder(e: { currentTarget: { dataset: { id?: string | number } } }) {
  const id = Number(e.currentTarget.dataset.id)
  selectedId.value = id
  loadDetail(id)
}

function statusType(status: string) {
  if (status === 'PENDING_CONFIRM') return 'warning'
  if (status === 'PENDING_PICK' || status === 'PICKING') return 'primary'
  if (status === 'PRICED' || status === 'PENDING_PRICE') return 'warning'
  if (status === 'COMPLETED') return 'success'
  return 'info'
}

function formatTime(value?: string) {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 16)
}

function goHome() {
  uni.redirectTo({ url: '/pages/customer/home/index' })
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx;
}

.loading-wrap,
.empty-wrap {
  padding: 80rpx 0;
  text-align: center;
}

.amount {
  display: block;
  margin-top: 8rpx;
  font-size: 28rpx;
  color: #e67e22;
  font-weight: 600;
}

.price-pending {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #999;
}

.detail-pending {
  margin-top: 20rpx;
  text-align: right;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 16rpx;
  border: 2rpx solid transparent;
}

.card.active {
  border-color: #27ae60;
}

.row-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-no {
  font-size: 30rpx;
  font-weight: 600;
}

.meta {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #999;
}

.remark {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #666;
}

.detail-panel {
  margin-top: 24rpx;
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
}

.detail-title {
  display: block;
  margin-bottom: 20rpx;
  font-size: 30rpx;
  font-weight: 600;
}

.line-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f2f3f5;
  font-size: 28rpx;
}

.line-right {
  text-align: right;
}

.line-qty {
  display: block;
  color: #666;
}

.line-price {
  display: block;
  margin-top: 4rpx;
  color: #e67e22;
}

.detail-total {
  display: block;
  margin-top: 20rpx;
  text-align: right;
  font-size: 30rpx;
  font-weight: 600;
  color: #27ae60;
}
</style>
