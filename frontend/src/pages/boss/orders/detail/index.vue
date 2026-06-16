<template>
  <view class="page">
    <view v-if="loading" class="loading-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <scroll-view v-else-if="order" scroll-y class="scroll-body">
      <view class="header">
        <view class="row-top">
          <view class="name-wrap">
            <text class="customer-name">{{ order.customerName }}</text>
            <text v-if="isTemporaryOrder" class="temp-tag">临时</text>
          </view>
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
          <text v-if="line.pickRemark" class="line-sub">备注：{{ line.pickRemark }}</text>
          <text v-if="line.dealPrice != null" class="line-price">¥{{ Number(line.dealPrice).toFixed(2) }}/{{ line.unit }}</text>
        </view>
      </view>
    </scroll-view>

    <view v-if="order && !loading" class="boss-bottom-bar column">
      <u-button
        v-if="canEditOrder"
        type="info"
        plain
        text="修改订单"
        @click="goEdit"
      />
      <u-button
        v-if="showPickButton"
        type="primary"
        text="去分拣"
        @click="goPick"
      />
      <u-button
        v-if="order.status === 'PENDING_CONFIRM'"
        type="primary"
        text="确认已交货"
        @click="handleConfirm"
      />
      <u-button
        v-if="showPriceButton"
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
        v-if="order.status !== 'CANCELLED'"
        type="info"
        plain
        text="修改状态"
        @click="showStatusPicker = true"
      />
    </view>

    <u-action-sheet
      :show="showStatusPicker"
      :actions="statusActions"
      title="选择目标状态（用于纠错回退）"
      @close="showStatusPicker = false"
      @select="handleStatusSelect"
    />
  </view>
</template>

<script setup lang="ts">
import { onLoad, onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import {
  BOSS_ORDER_STATUS_OPTIONS,
  confirmBossOrder,
  fetchBossOrderDetail,
  updateBossOrderStatus,
  type OrderInfo,
} from '../../../../api/order'
import { publishOrderPricing } from '../../../../api/pricing'
import { useUserStore } from '../../../../stores/user'

const userStore = useUserStore()
const order = ref<OrderInfo | null>(null)
const loading = ref(false)
const publishing = ref(false)
const showStatusPicker = ref(false)
const orderId = ref(0)

const EDITABLE_STATUSES = ['PENDING_CONFIRM', 'PENDING_PICK', 'PICKING', 'PENDING_PRICE']

const isTemporaryOrder = computed(() => !order.value?.customerId)

const canEditOrder = computed(() => {
  if (!order.value) return false
  if (order.value.amount != null) return false
  return EDITABLE_STATUSES.includes(order.value.status)
})

const showPriceButton = computed(() => {
  if (!order.value) return false
  return ['PENDING_CONFIRM', 'PENDING_PICK', 'PENDING_PRICE'].includes(order.value.status)
    && order.value.amount == null
})

const showPickButton = computed(() => {
  if (!order.value) return false
  return ['PENDING_CONFIRM', 'PENDING_PICK', 'PICKING', 'PENDING_PRICE'].includes(order.value.status)
    && order.value.amount == null
})

const statusActions = computed(() =>
  BOSS_ORDER_STATUS_OPTIONS
    .filter((opt) => opt.value !== order.value?.status)
    .map((opt) => ({ name: opt.label, value: opt.value })),
)

onLoad((query) => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  orderId.value = Number(query?.id || 0)
})

onShow(async () => {
  if (!orderId.value) return
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

function goEdit() {
  uni.navigateTo({ url: `/pages/boss/sales-order/index?orderId=${orderId.value}` })
}

async function handleConfirm() {
  try {
    order.value = await confirmBossOrder(orderId.value)
    uni.showToast({ title: '已确认交货', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '确认失败', icon: 'none' })
  }
}

function goPick() {
  uni.navigateTo({ url: `/pages/boss/orders/pick/index?id=${orderId.value}` })
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

async function handleStatusSelect(item: { value?: string; name?: string }) {
  showStatusPicker.value = false
  const target = item.value
  if (!target) return
  uni.showModal({
    title: '确认修改状态',
    content: `将订单改为「${item.name || target}」？回退到待录价及之前会清空已录价格。`,
    success: async (res) => {
      if (!res.confirm) return
      try {
        order.value = await updateBossOrderStatus(orderId.value, target)
        uni.showToast({ title: '状态已更新', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: e instanceof Error ? e.message : '修改失败', icon: 'none' })
      }
    },
  })
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
@import '../../../../styles/boss-footer.scss';

.page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #f5f6f7;
  box-sizing: border-box;
}

.loading-wrap {
  padding: 80rpx 0;
  text-align: center;
}

.scroll-body {
  flex: 1;
  height: 0;
  padding: 24rpx;
  padding-bottom: calc(24rpx + 360rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
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
  align-items: flex-start;
  gap: 16rpx;
}

.name-wrap {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex: 1;
  min-width: 0;
}

.customer-name {
  font-size: 36rpx;
  font-weight: 600;
}

.temp-tag {
  flex-shrink: 0;
  padding: 4rpx 12rpx;
  font-size: 20rpx;
  color: #e67e22;
  background: #fef5ec;
  border-radius: 999rpx;
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

.line-item:last-child {
  border-bottom: none;
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

.line-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #999;
}

.line-price {
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
  color: #e67e22;
}
</style>
