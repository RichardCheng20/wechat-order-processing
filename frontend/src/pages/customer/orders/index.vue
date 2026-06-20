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
          <u-tag :text="item.statusLabel" :type="listStatusType(item)" size="mini" />
        </view>
        <text class="meta">{{ formatTime(item.createdAt) }} · {{ item.itemCount || 0 }} 种商品</text>
        <text v-if="item.amount != null" class="amount">合计 ¥{{ Number(item.amount).toFixed(2) }}</text>
        <text v-else class="price-pending">金额待老板确认</text>
        <text v-if="item.paymentStatusLabel && item.printed" class="payment-label">{{ item.paymentStatusLabel }}</text>
        <text v-if="item.remark" class="remark">备注：{{ item.remark }}</text>
      </view>
    </view>

    <view v-if="detail" class="detail-panel">
      <text class="detail-title">订单明细</text>
      <view v-for="line in detail.items || []" :key="line.id" class="line-item">
        <view class="line-main">
          <text class="line-name">{{ line.productName }}</text>
          <text v-if="line.pickRemark" class="line-remark">备注：{{ line.pickRemark }}</text>
        </view>
        <view class="line-right">
          <text class="line-qty">{{ line.orderQty }}{{ line.unit }}</text>
          <text v-if="line.dealPrice != null" class="line-price">¥{{ Number(line.dealPrice).toFixed(2) }}</text>
        </view>
      </view>
      <text v-if="detail.amount != null" class="detail-total">订单总额 ¥{{ Number(detail.amount).toFixed(2) }}</text>
      <text v-else class="price-pending detail-pending">金额待老板确认后展示</text>
      <text v-if="detail.paymentStatusLabel && detail.printed" class="payment-detail">{{ detail.paymentStatusLabel }}</text>

      <view v-if="detail.statementImageUrl" class="statement-section">
        <text class="statement-title">对账单</text>
        <image
          class="statement-image"
          :src="statementImageSrc"
          mode="widthFix"
          @tap="previewStatement"
        />
        <text class="statement-tip">点击图片可放大查看</text>
      </view>

      <view v-if="canNotifyBoss" class="notify-section">
        <text class="notify-title">已确认订单明细，通知老板处理</text>
        <text class="notify-tip">转发会发到微信聊天；消息提醒需老板先在订单页开启订阅</text>
        <view class="notify-actions">
          <button class="notify-btn share" open-type="share">转发给老板</button>
          <button class="notify-btn primary" :loading="notifying" @tap="handleNotifyBoss">微信消息提醒</button>
        </view>
      </view>
    </view>

    <CustomerTabBar active="orders" />
  </view>
</template>

<script setup lang="ts">
import { onLoad, onShareAppMessage, onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import { fetchCustomerOrderDetail, fetchCustomerOrders, notifyBossOrder, type OrderInfo } from '../../../api/order'
import CustomerTabBar from '../../../components/CustomerTabBar.vue'
import { switchCustomerTab } from '../../../utils/customer-nav'
import { resolveMediaUrl } from '../../../utils/media'
import { useUserStore } from '../../../stores/user'

const userStore = useUserStore()
const orders = ref<OrderInfo[]>([])
const detail = ref<OrderInfo | null>(null)
const selectedId = ref<number | null>(null)
const loading = ref(false)
const notifying = ref(false)

const canNotifyBoss = computed(() => detail.value?.status === 'PENDING_CONFIRM')

const statementImageSrc = computed(() => resolveMediaUrl(detail.value?.statementImageUrl))

onShareAppMessage(() => buildSharePayload())

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
  if (selectedId.value) {
    await loadDetail(selectedId.value)
  }
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

function buildSharePayload() {
  const order = detail.value
  if (!order) {
    return {
      title: '蔬菜批发订单',
      path: '/pages/customer/orders/index',
    }
  }
  const customerName = order.customerName || userStore.displayName || '客户'
  const count = order.itemCount || order.items?.length || 0
  return {
    title: `${customerName}提交了订单，共${count}种商品，请确认`,
    path: `/pages/boss/orders/detail/index?id=${order.id}`,
  }
}

async function handleNotifyBoss() {
  if (!detail.value || notifying.value) return
  notifying.value = true
  try {
    const result = await notifyBossOrder(detail.value.id)
    uni.showToast({
      title: result.message,
      icon: result.notified > 0 ? 'success' : 'none',
      duration: 3000,
    })
  } catch (e) {
    uni.showToast({
      title: e instanceof Error ? e.message : '提醒失败',
      icon: 'none',
      duration: 3000,
    })
  } finally {
    notifying.value = false
  }
}

function statusType(status: string) {
  if (status === 'PENDING_CONFIRM') return 'warning'
  if (status === 'PENDING_PICK' || status === 'PICKING') return 'primary'
  if (status === 'PRICED' || status === 'PENDING_PRICE') return 'warning'
  if (status === 'COMPLETED') return 'success'
  return 'info'
}

function listStatusType(item: OrderInfo) {
  if (item.printed && item.status !== 'COMPLETED') return 'success'
  return statusType(item.status)
}

function formatTime(value?: string) {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 16)
}

function goHome() {
  switchCustomerTab('home')
}

function previewStatement() {
  const url = statementImageSrc.value
  if (!url) return
  uni.previewImage({ urls: [url], current: url })
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx;
  padding-bottom: calc(120rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
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
  align-items: flex-start;
  gap: 16rpx;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f2f3f5;
  font-size: 28rpx;
}

.line-main {
  flex: 1;
  min-width: 0;
}

.line-name {
  display: block;
}

.line-remark {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #e67e22;
  line-height: 1.4;
}

.line-right {
  text-align: right;
  flex-shrink: 0;
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

.payment-label {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #66736b;
}

.payment-detail {
  display: block;
  margin-top: 12rpx;
  text-align: right;
  font-size: 26rpx;
  color: #66736b;
}

.statement-section {
  margin-top: 28rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid #eef2ed;
}

.statement-title {
  display: block;
  margin-bottom: 16rpx;
  font-size: 28rpx;
  font-weight: 600;
  color: #17211b;
}

.statement-image {
  width: 100%;
  border-radius: 12rpx;
  background: #f7f8fa;
}

.statement-tip {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #999;
}

.notify-section {
  margin-top: 28rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid #eef2ed;
}

.notify-title {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: #17211b;
}

.notify-tip {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.5;
  color: #66736b;
}

.notify-actions {
  display: flex;
  gap: 16rpx;
  margin-top: 20rpx;
}

.notify-btn {
  flex: 1;
  height: 84rpx;
  line-height: 84rpx;
  margin: 0;
  padding: 0;
  font-size: 28rpx;
  border-radius: 12rpx;
  border: none;
}

.notify-btn::after {
  border: none;
}

.notify-btn.share {
  background: #fff;
  color: #07c160;
  border: 2rpx solid #07c160;
}

.notify-btn.primary {
  background: #07c160;
  color: #fff;
  font-weight: 600;
}
</style>
