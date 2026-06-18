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
        <text class="date-prefix">制单：</text>
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
        <u-empty mode="order" text="暂无历史订单" />
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
              <text class="order-no">{{ item.orderNo }}</text>
              <text class="pay-status">{{ settlementLabel(item) }}</text>
            </view>
            <text class="time-text">制单: {{ formatCreated(item.createdAt) }}</text>
            <view class="amount-row">
              <text>应结 {{ formatMoney(item.receivableAmount ?? item.amount) }}元</text>
            </view>
            <view v-if="getOutstanding(item) > 0" class="debt-row">
              <text>待结</text>
              <text class="debt-value">{{ formatMoney(getOutstanding(item)) }}元</text>
            </view>
          </view>
        </view>
        <view class="list-end">没有更多了</view>
      </view>
    </scroll-view>

    <view class="bottom-bar">
      <view class="total-bar">
        <text>合计应结: {{ formatMoney(totalOutstanding) }}元</text>
      </view>
      <view class="bottom-actions">
        <view class="action-item" @tap="batchSettle">
          <AppIcon name="batch" tone="gray" :size="22" :tile-size="48" :radius="12" />
          <text>批量结账</text>
        </view>
        <view class="action-item" @tap="clearSelection">
          <AppIcon name="delete" tone="gray" :size="22" :tile-size="48" :radius="12" />
          <text>清除选择</text>
        </view>
      </view>
    </view>

    <u-popup :show="showDatePicker" mode="bottom" round="16" @close="showDatePicker = false">
      <view class="date-popup">
        <text class="popup-title">选择制单日期</text>
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

type PaymentTab = 'ALL' | 'PENDING' | 'PARTIAL' | 'SETTLED'

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
  { label: '全部', value: 'ALL' as const },
  { label: '待结款', value: 'PENDING' as const },
  { label: '部分结款', value: 'PARTIAL' as const },
  { label: '已结款', value: 'SETTLED' as const },
]

const rangeLabel = computed(() => {
  const from = formatShortDate(dateFrom.value)
  const to = formatShortDate(dateTo.value)
  if (from === to) return from
  return `${from}至${to}`
})

const isThisMonth = computed(() => isThisMonthRange(dateFrom.value, dateTo.value))

const totalOutstanding = computed(() => {
  const list = selectedIds.value.length
    ? orders.value.filter((item) => selectedIds.value.includes(item.id))
    : orders.value
  return list.reduce((sum, item) => sum + getOutstanding(item), 0)
})

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
      status: 'COMPLETED',
      paymentFilter: paymentTab.value,
      dateType: 'ORDER',
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

function settlementLabel(item: OrderInfo) {
  const outstanding = getOutstanding(item)
  const paid = Number(item.paidAmount ?? 0)
  if (outstanding <= 0) return '已结款'
  if (paid > 0) return '部分结款'
  return '待结款'
}

function formatMoney(value?: number) {
  return Number(value || 0).toFixed(0)
}

function formatCreated(value?: string) {
  if (!value) return '-'
  const d = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(d.getTime())) return value
  const m = `${d.getMonth() + 1}`.padStart(2, '0')
  const day = `${d.getDate()}`.padStart(2, '0')
  return `${m}-${day}`
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

function batchSettle() {
  const selected = orders.value.filter((item) => selectedIds.value.includes(item.id))
  const list = selected.length ? selected : orders.value.filter((item) => getOutstanding(item) > 0)
  if (list.length === 0) {
    uni.showToast({ title: '暂无待结订单', icon: 'none' })
    return
  }
  const total = list.reduce((sum, item) => sum + getOutstanding(item), 0)
  const name = encodeURIComponent(customerName.value)
  uni.navigateTo({
    url: `/pages/boss/sales-payment/index?customerId=${customerId.value}&customerName=${name}&amount=${total.toFixed(2)}`,
  })
}

function clearSelection() {
  selectedIds.value = []
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: #f5f6f7;
  display: flex;
  flex-direction: column;
  padding-bottom: calc(160rpx + env(safe-area-inset-bottom));
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
  margin-bottom: 8rpx;
}

.order-no {
  font-size: 28rpx;
  color: #333;
  font-weight: 600;
}

.pay-status {
  font-size: 24rpx;
  color: #999;
}

.time-text {
  display: block;
  font-size: 24rpx;
  color: #666;
  margin-bottom: 8rpx;
}

.amount-row {
  font-size: 26rpx;
  color: #333;
  margin-bottom: 8rpx;
}

.debt-row {
  display: flex;
  gap: 12rpx;
  font-size: 28rpx;

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
}

.total-bar {
  background: #fff3e6;
  color: #e67e22;
  font-size: 28rpx;
  font-weight: 600;
  padding: 16rpx 24rpx;
}

.bottom-actions {
  display: flex;
  justify-content: flex-end;
  gap: 48rpx;
  padding: 16rpx 32rpx calc(16rpx + env(safe-area-inset-bottom));
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
