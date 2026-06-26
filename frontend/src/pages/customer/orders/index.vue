<template>
  <view class="page">
    <view class="toolbar">
      <view class="status-tabs">
        <text
          v-for="tab in CUSTOMER_ORDER_TABS"
          :key="tab.value"
          class="status-tab"
          :class="{ active: activeTab === tab.value }"
          @tap="switchTab(tab.value)"
        >
          {{ tab.label }}
        </text>
      </view>

      <view class="date-bar">
        <view class="quick-tabs">
          <text class="quick-tab" :class="{ active: presetDays === 1 }" @tap="applyPreset(1)">今天</text>
          <text class="quick-tab" :class="{ active: presetDays === 7 }" @tap="applyPreset(7)">近7日</text>
          <text class="quick-tab" :class="{ active: presetDays === 30 }" @tap="applyPreset(30)">近30日</text>
        </view>
        <view class="calendar-btn" @tap="openRangePicker">
          <AppIcon name="calendar" tone="gray" :size="16" :tile="false" />
          <text v-if="rangeLabel" class="range-label">{{ rangeLabel }}</text>
        </view>
      </view>
    </view>

    <view v-if="loading" class="loading-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <view v-else-if="orders.length === 0" class="empty-wrap">
      <u-empty mode="order" :text="emptyText" />
      <u-button v-if="activeTab === 'PROCESSING' || activeTab === 'ALL'" type="primary" plain text="去下单" @click="goHome" />
    </view>

    <view v-else class="list">
      <view
        v-for="item in orders"
        :key="item.id"
        class="card"
        :class="{ active: selectedId === item.id }"
        @tap="selectOrder(item)"
      >
        <view class="row-top">
          <text class="order-no">{{ item.orderNo }}</text>
          <u-tag :text="listStatusText(item)" :type="listStatusType(item)" size="mini" />
        </view>
        <text class="meta">{{ formatTime(item.createdAt) }} · {{ item.itemCount || 0 }} 种商品</text>
        <text v-if="item.amount != null" class="amount">合计 ¥{{ Number(item.amount).toFixed(2) }}</text>
        <text v-else class="price-pending">订单正在处理请耐心等待</text>
        <text v-if="showPaymentLabel(item)" class="payment-label">{{ customerPaymentLabel(item) }}</text>
        <text v-if="item.remark" class="remark">备注：{{ item.remark }}</text>
      </view>
    </view>

    <view v-if="detail" id="detail-panel" class="detail-panel">
      <text class="detail-title">订单明细</text>
      <view v-if="isImagePendingDetail" class="image-pending-tip">
        您已提交图片订单，老板将对照原图录入商品明细
      </view>
      <view v-if="!(detail.items || []).length && !isImagePendingDetail" class="empty-detail">
        暂无商品明细
      </view>
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
      <text v-if="detail && customerPaymentLabel(detail)" class="payment-detail">{{ customerPaymentLabel(detail) }}</text>

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

      <view v-if="detail.sourceImageUrl" class="statement-section">
        <text class="statement-title">原始下单图片</text>
        <image
          class="statement-image"
          :src="sourceImageSrc"
          mode="widthFix"
          @tap="previewSourceImage"
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

    <OrderDateRangePicker
      :show="rangePickerVisible"
      :start-date="dateFrom"
      :end-date="dateTo"
      @close="rangePickerVisible = false"
      @confirm="onRangeConfirm"
    />

    <CustomerTabBar active="orders" />
  </view>
</template>

<script setup lang="ts">
import { onLoad, onShareAppMessage, onShow } from '@dcloudio/uni-app'
import { computed, nextTick, ref } from 'vue'
import {
  fetchCustomerOrderDetail,
  fetchCustomerOrders,
  notifyBossOrder,
  type CustomerOrderTab,
  type OrderInfo,
} from '@common/api/order'
import AppIcon from '@/components/AppIcon.vue'
import CustomerTabBar from '@/components/CustomerTabBar.vue'
import OrderDateRangePicker from '@/components/OrderDateRangePicker.vue'
import { switchCustomerTab } from '@common/utils/customer-nav'
import { formatShortDate, getLastNDaysRange, getTodayRange } from '@common/utils/date-range'
import { resolveMediaUrl } from '@common/utils/media'
import { CUSTOMER_ORDER_TABS, customerOrderListStatus, customerOrderListStatusType, customerPaymentLabel, customerTabLabel } from '@common/utils/order-flow'
import { useUserStore } from '@common/stores/user'

const userStore = useUserStore()
const orders = ref<OrderInfo[]>([])
const detail = ref<OrderInfo | null>(null)
const selectedId = ref<number | null>(null)
const loading = ref(false)
const notifying = ref(false)
const activeTab = ref<CustomerOrderTab>('PROCESSING')
const dateFrom = ref('')
const dateTo = ref('')
const presetDays = ref(7)
const rangePickerVisible = ref(false)

const canNotifyBoss = computed(() => detail.value?.status === 'PENDING_CONFIRM')

const statementImageSrc = computed(() => resolveMediaUrl(detail.value?.statementImageUrl))
const sourceImageSrc = computed(() => resolveMediaUrl(detail.value?.sourceImageUrl))
const isImagePendingDetail = computed(() => {
  const d = detail.value
  if (!d) return false
  return Boolean(d.sourceImageUrl) && !(d.items || []).length
})

const rangeLabel = computed(() => {
  if (!dateFrom.value || presetDays.value > 0) return ''
  const from = formatShortDate(dateFrom.value)
  const to = formatShortDate(dateTo.value)
  return from === to ? from : `${from}~${to}`
})

const emptyText = computed(() => {
  if (activeTab.value === 'ALL') return '所选日期内暂无订单'
  const tab = customerTabLabel(activeTab.value)
  return `所选日期内暂无${tab}订单`
})

onShareAppMessage(() => buildSharePayload())

onLoad(async (query) => {
  if (!userStore.isLoggedIn || !userStore.isCustomer) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  applyPreset(7, false)
  const id = query?.id ? Number(query.id) : null
  if (id) {
    selectedId.value = id
    await loadDetail(id)
    scrollToDetail()
  }
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isCustomer) {
    return
  }
  if (!dateFrom.value) {
    applyPreset(7, false)
    return
  }
  await loadOrders()
  if (selectedId.value) {
    await loadDetail(selectedId.value)
  }
})

let loadingOrders = false

function applyPreset(days: number, reload = true) {
  presetDays.value = days
  const range = days === 1 ? getTodayRange() : getLastNDaysRange(days)
  dateFrom.value = range.from
  dateTo.value = range.to
  if (reload) {
    loadOrders()
  }
}

function openRangePicker() {
  rangePickerVisible.value = true
}

function onRangeConfirm(payload: { from: string; to: string }) {
  rangePickerVisible.value = false
  dateFrom.value = payload.from
  dateTo.value = payload.to
  presetDays.value = 0
  loadOrders()
}

function switchTab(tab: CustomerOrderTab) {
  if (activeTab.value === tab) return
  activeTab.value = tab
  selectedId.value = null
  detail.value = null
  loadOrders()
}

async function loadOrders() {
  if (loadingOrders || !dateFrom.value || !dateTo.value) return
  loadingOrders = true
  loading.value = true
  try {
    orders.value = await fetchCustomerOrders({
      dateFrom: dateFrom.value,
      dateTo: dateTo.value,
      tabFilter: activeTab.value,
    })
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

async function selectOrder(item: OrderInfo) {
  selectedId.value = item.id
  await loadDetail(item.id)
  scrollToDetail()
}

function scrollToDetail() {
  void nextTick(() => {
    setTimeout(() => {
      uni.pageScrollTo({
        selector: '#detail-panel',
        duration: 300,
      })
    }, 80)
  })
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

function listStatusText(item: OrderInfo) {
  return customerOrderListStatus(item)
}

function listStatusType(item: OrderInfo) {
  return customerOrderListStatusType(item)
}

function showPaymentLabel(item: OrderInfo) {
  const payment = customerPaymentLabel(item)
  if (!payment) return false
  return customerOrderListStatus(item) !== payment
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

function previewSourceImage() {
  const url = sourceImageSrc.value
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

.toolbar {
  margin-bottom: 20rpx;
}

.status-tabs {
  display: flex;
  gap: 12rpx;
  margin-bottom: 16rpx;
}

.status-tab {
  flex: 1;
  text-align: center;
  padding: 18rpx 0;
  font-size: 28rpx;
  color: #66736b;
  background: #fff;
  border-radius: 12rpx;
  border: 2rpx solid transparent;
}

.status-tab.active {
  color: #07c160;
  font-weight: 600;
  border-color: #07c160;
  background: #f0faf4;
}

.date-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.quick-tabs {
  display: flex;
  gap: 8rpx;
  flex: 1;
}

.quick-tab {
  padding: 10rpx 20rpx;
  font-size: 24rpx;
  color: #66736b;
  background: #fff;
  border-radius: 999rpx;
}

.quick-tab.active {
  color: #07c160;
  background: #e8f8ef;
  font-weight: 600;
}

.calendar-btn {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 10rpx 16rpx;
  background: #fff;
  border-radius: 999rpx;
  flex-shrink: 0;
}

.range-label {
  font-size: 22rpx;
  color: #66736b;
  max-width: 160rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.card:active {
  opacity: 0.92;
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

.image-pending-tip {
  display: block;
  margin-bottom: 16rpx;
  padding: 20rpx;
  font-size: 26rpx;
  line-height: 1.6;
  color: #b45309;
  background: #fff7e6;
  border-radius: 12rpx;
}

.empty-detail {
  padding: 8rpx 0 16rpx;
  font-size: 26rpx;
  color: #999;
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
