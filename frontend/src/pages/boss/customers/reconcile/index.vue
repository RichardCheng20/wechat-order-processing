<template>
  <view class="page">
    <view class="tabs">
      <view
        v-for="tab in tabs"
        :key="tab.value"
        class="tab-item"
        :class="{ active: paymentTab === tab.value }"
        @tap="switchTab(tab.value)"
      >{{ tab.label }}</view>
    </view>

    <view class="date-bar">
      <view class="date-dropdown" @tap="showDatePicker = true">
        <text class="date-prefix">配送：</text>
        <text class="date-range">{{ rangeLabel }}</text>
        <text class="date-arrow">▼</text>
      </view>
      <text class="month-tag" :class="{ active: isThisMonth }" @tap="applyThisMonth">本月</text>
    </view>

    <scroll-view scroll-y class="list-scroll" :show-scrollbar="false">
      <view v-if="loading" class="state-wrap">
        <u-loading-icon text="加载中" />
      </view>
      <view v-else-if="orders.length === 0" class="state-wrap">
        <u-empty mode="order" text="暂无订单" />
      </view>
      <view v-else class="list">
        <view v-for="item in orders" :key="item.id" class="card" @tap="toggleSelect(item.id)">
          <view class="card-check" @tap.stop="toggleSelect(item.id)">
            <view class="check-circle" :class="{ checked: selectedIds.includes(item.id) }">
              <text v-if="selectedIds.includes(item.id)">✓</text>
            </view>
          </view>
          <view class="card-body">
            <view class="card-head">
              <text class="delivery-text">配送: {{ formatDelivery(item.deliveryDate) }}</text>
              <text class="pay-status">{{ item.paymentStatusLabel || '待收款' }}</text>
            </view>
            <text class="order-no">订单编号 {{ item.orderNo }}</text>
            <view class="amount-row">
              <text>销售金额 {{ formatMoney(item.amount) }}元</text>
              <text v-if="item.priceIncomplete" class="muted">(未录完)</text>
              <text v-else class="muted">(已录完)</text>
            </view>
            <view v-if="getOutstanding(item) > 0" class="debt-row">
              <text>欠款金额</text>
              <text class="debt-value">{{ formatMoney(getOutstanding(item)) }}元</text>
            </view>
          </view>
        </view>
        <view class="list-end">没有更多了</view>
      </view>
    </scroll-view>

    <view class="bottom-bar">
      <view class="select-all" @tap="toggleSelectAll">
        <view class="check-circle" :class="{ checked: allSelected }">
          <text v-if="allSelected">✓</text>
        </view>
        <view class="select-text">
          <text>全选</text>
          <text class="select-sub">共{{ selectableOrders.length }}笔</text>
        </view>
      </view>
      <view class="bottom-actions">
        <view class="action-item" @tap="mergeSettle">
          <AppIcon name="batch" tone="gray" :size="22" :tile-size="48" :radius="12" />
          <text>合并结清</text>
        </view>
        <view class="action-item" @tap="shareSummary">
          <AppIcon name="invite" tone="gray" :size="22" :tile-size="48" :radius="12" />
          <text>分享</text>
        </view>
      </view>
    </view>

    <u-popup :show="showDatePicker" mode="bottom" round="16" @close="showDatePicker = false">
      <view class="date-popup">
        <text class="popup-title">选择配送日期</text>
        <view class="date-inputs">
          <picker mode="date" :value="draftFrom" @change="onFromChange">
            <view class="date-pick">{{ draftFrom || '开始日期' }}</view>
          </picker>
          <text>至</text>
          <picker mode="date" :value="draftTo" @change="onToChange">
            <view class="date-pick">{{ draftTo || '结束日期' }}</view>
          </picker>
        </view>
        <u-button type="primary" text="确定" @click="applyDateRange" />
      </view>
    </u-popup>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { fetchBossOrders, type OrderInfo } from '../../../../api/order'
import AppIcon from '../../../../components/AppIcon.vue'
import {
  formatShortDate,
  getLastMonthsRange,
  getThisMonthRange,
  isThisMonthRange,
} from '../../../../utils/date-range'
import { useUserStore } from '../../../../stores/user'

type PaymentTab = 'ALL' | 'UNPAID' | 'PAID'

const userStore = useUserStore()
const customerId = ref(0)
const customerName = ref('')
const loading = ref(false)
const orders = ref<OrderInfo[]>([])
const paymentTab = ref<PaymentTab>('ALL')
const selectedIds = ref<number[]>([])
const showDatePicker = ref(false)

const defaultRange = getLastMonthsRange(6)
const dateFrom = ref(defaultRange.from)
const dateTo = ref(defaultRange.to)
const draftFrom = ref(defaultRange.from)
const draftTo = ref(defaultRange.to)

const tabs = [
  { label: '全部订单', value: 'ALL' as const },
  { label: '未付清', value: 'UNPAID' as const },
  { label: '已付清', value: 'PAID' as const },
]

const rangeLabel = computed(() => {
  const from = formatShortDate(dateFrom.value)
  const to = formatShortDate(dateTo.value)
  if (from === to) return from
  return `${from}至${to}`
})

const isThisMonth = computed(() => isThisMonthRange(dateFrom.value, dateTo.value))

const selectableOrders = computed(() =>
  orders.value.filter((item) => getOutstanding(item) > 0),
)

const allSelected = computed(() =>
  selectableOrders.value.length > 0
  && selectableOrders.value.every((item) => selectedIds.value.includes(item.id)),
)

onLoad((query) => {
  customerId.value = Number(query?.customerId || 0)
  if (query?.customerName) {
    customerName.value = decodeURIComponent(String(query.customerName))
  }
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  await loadOrders()
})

async function loadOrders() {
  if (!customerId.value) return
  loading.value = true
  selectedIds.value = []
  try {
    orders.value = await fetchBossOrders({
      customerId: customerId.value,
      receivableOnly: true,
      paymentFilter: paymentTab.value,
      dateType: 'DELIVERY',
      dateFrom: dateFrom.value,
      dateTo: dateTo.value,
    })
  } catch (e) {
    orders.value = []
    uni.showToast({ title: e instanceof Error ? e.message : '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function switchTab(value: PaymentTab) {
  if (paymentTab.value === value) return
  paymentTab.value = value
  loadOrders()
}

function applyThisMonth() {
  const range = getThisMonthRange()
  dateFrom.value = range.from
  dateTo.value = range.to
  loadOrders()
}

function onFromChange(e: { detail: { value: string } }) {
  draftFrom.value = e.detail.value
}

function onToChange(e: { detail: { value: string } }) {
  draftTo.value = e.detail.value
}

function applyDateRange() {
  if (!draftFrom.value || !draftTo.value) {
    uni.showToast({ title: '请选择日期范围', icon: 'none' })
    return
  }
  dateFrom.value = draftFrom.value
  dateTo.value = draftTo.value
  showDatePicker.value = false
  loadOrders()
}

function getOutstanding(item: OrderInfo) {
  if (item.outstandingAmount != null) return Number(item.outstandingAmount)
  const receivable = Number(item.receivableAmount ?? item.amount ?? 0)
  const paid = Number(item.paidAmount ?? 0)
  return Math.max(receivable - paid, 0)
}

function formatMoney(value?: number) {
  return Number(value || 0).toFixed(0)
}

function formatDelivery(value?: string) {
  if (!value) return '-'
  const parts = value.split('-')
  if (parts.length < 3) return value
  return `${parts[1]}-${parts[2]} 20:30`
}

function toggleSelect(id: number) {
  const item = orders.value.find((o) => o.id === id)
  if (!item || getOutstanding(item) <= 0) return
  const idx = selectedIds.value.indexOf(id)
  if (idx >= 0) {
    selectedIds.value.splice(idx, 1)
  } else {
    selectedIds.value.push(id)
  }
}

function toggleSelectAll() {
  if (allSelected.value) {
    selectedIds.value = []
    return
  }
  selectedIds.value = selectableOrders.value.map((item) => item.id)
}

function mergeSettle() {
  const selected = orders.value.filter((item) => selectedIds.value.includes(item.id))
  if (selected.length === 0) {
    uni.showToast({ title: '请选择待结清订单', icon: 'none' })
    return
  }
  const total = selected.reduce((sum, item) => sum + getOutstanding(item), 0)
  if (total <= 0) {
    uni.showToast({ title: '所选订单无需结清', icon: 'none' })
    return
  }
  const name = encodeURIComponent(customerName.value)
  uni.navigateTo({
    url: `/pages/boss/sales-payment/index?customerId=${customerId.value}&customerName=${name}&amount=${total.toFixed(2)}`,
  })
}

function shareSummary() {
  const selected = selectedIds.value.length
    ? orders.value.filter((item) => selectedIds.value.includes(item.id))
    : orders.value.filter((item) => getOutstanding(item) > 0)
  if (selected.length === 0) {
    uni.showToast({ title: '暂无待分享内容', icon: 'none' })
    return
  }
  const total = selected.reduce((sum, item) => sum + getOutstanding(item), 0)
  const lines = selected.map((item) =>
    `${item.orderNo} 欠款${formatMoney(getOutstanding(item))}元`,
  )
  const text = `${customerName.value || '客户'}对账\n${lines.join('\n')}\n合计欠款：${formatMoney(total)}元`
  uni.setClipboardData({
    data: text,
    success: () => uni.showToast({ title: '对账摘要已复制', icon: 'success' }),
  })
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: #f5f6f7;
  display: flex;
  flex-direction: column;
  padding-bottom: calc(120rpx + env(safe-area-inset-bottom));
}

.tabs {
  display: flex;
  background: #fff;
  border-bottom: 1rpx solid #eee;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 24rpx 0;
  font-size: 28rpx;
  color: #666;
  position: relative;

  &.active {
    color: #2ecc71;
    font-weight: 600;

    &::after {
      content: '';
      position: absolute;
      left: 50%;
      bottom: 0;
      transform: translateX(-50%);
      width: 48rpx;
      height: 4rpx;
      background: #2ecc71;
      border-radius: 2rpx;
    }
  }
}

.date-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 24rpx;
  background: #fff;
  border-bottom: 1rpx solid #f0f0f0;
}

.date-dropdown {
  display: flex;
  align-items: center;
  font-size: 26rpx;
  color: #333;
}

.date-prefix {
  color: #666;
}

.date-arrow {
  margin-left: 8rpx;
  font-size: 20rpx;
  color: #999;
}

.month-tag {
  font-size: 24rpx;
  color: #666;
  padding: 8rpx 20rpx;
  border-radius: 24rpx;
  background: #f5f5f5;

  &.active {
    color: #2ecc71;
    background: #eafaf1;
  }
}

.list-scroll {
  flex: 1;
  height: 0;
}

.state-wrap {
  padding: 120rpx 0;
}

.list {
  padding: 16rpx 24rpx;
}

.card {
  display: flex;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
}

.card-check {
  padding-right: 16rpx;
  display: flex;
  align-items: flex-start;
  padding-top: 4rpx;
}

.check-circle {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  border: 2rpx solid #ccc;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  color: #fff;

  &.checked {
    background: #2ecc71;
    border-color: #2ecc71;
  }
}

.card-body {
  flex: 1;
}

.card-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12rpx;
}

.delivery-text {
  font-size: 28rpx;
  color: #333;
}

.pay-status {
  font-size: 24rpx;
  color: #999;
}

.order-no {
  display: block;
  font-size: 24rpx;
  color: #666;
  margin-bottom: 12rpx;
}

.amount-row {
  font-size: 26rpx;
  color: #333;
  margin-bottom: 8rpx;
}

.muted {
  color: #999;
  margin-left: 8rpx;
}

.debt-row {
  display: flex;
  gap: 12rpx;
  font-size: 28rpx;
  color: #333;

  .debt-value {
    color: #e74c3c;
    font-weight: 600;
  }
}

.list-end {
  text-align: center;
  color: #ccc;
  font-size: 24rpx;
  padding: 24rpx 0 40rpx;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #fff;
  border-top: 1rpx solid #eee;
  padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom));
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.select-all {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.select-text {
  display: flex;
  flex-direction: column;
  font-size: 28rpx;
  color: #333;
}

.select-sub {
  font-size: 22rpx;
  color: #999;
}

.bottom-actions {
  display: flex;
  gap: 32rpx;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 22rpx;
  color: #666;
  gap: 4rpx;
}

.date-popup {
  padding: 32rpx;
}

.popup-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  margin-bottom: 24rpx;
}

.date-inputs {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.date-pick {
  padding: 16rpx 24rpx;
  background: #f5f5f5;
  border-radius: 8rpx;
  font-size: 26rpx;
}
</style>
