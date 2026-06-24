<template>
  <view class="page">
    <view class="filter-bar">
      <view class="quick-tabs">
        <text class="quick-tab" :class="{ active: presetDays === 7 }" @tap="applyPreset(7)">近7日</text>
        <text class="quick-tab" :class="{ active: presetDays === 30 }" @tap="applyPreset(30)">近30日</text>
      </view>
      <view class="calendar-btn" @tap="openRangePicker">
        <AppIcon name="order" tone="gray" :size="18" :tile="false" />
      </view>
    </view>

    <view v-if="loading" class="state-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <scroll-view v-else scroll-y class="body-scroll" :show-scrollbar="false">
      <view class="summary-card">
        <view class="summary-main">
          <text class="summary-label">应收金额</text>
          <text class="summary-total">¥ {{ formatMoney(summary?.receivableAmount) }}</text>
        </view>
        <view class="summary-breakdown">
          <view class="breakdown-item">
            <text class="tag tag-received">已收</text>
            <text class="breakdown-value">{{ formatMoney(summary?.receivedAmount) }}</text>
          </view>
          <view class="breakdown-item">
            <text class="tag tag-discount">折让</text>
            <text class="breakdown-value">{{ formatMoney(summary?.discountAmount) }}</text>
          </view>
          <view class="breakdown-item">
            <text class="tag tag-outstanding">未收</text>
            <view class="breakdown-value-wrap">
              <text class="breakdown-value">{{ formatMoney(summary?.outstandingAmount) }}</text>
              <text class="info-icon" @tap.stop="showOutstandingTip">?</text>
            </view>
          </view>
          <view class="breakdown-item">
            <text class="tag tag-refund">退款</text>
            <text class="breakdown-value">{{ formatMoney(summary?.refundAmount) }}</text>
          </view>
        </view>
      </view>

      <view class="table-card">
        <view class="table-head">
          <text class="col-name">客户</text>
          <text class="col-receivable">应收金额</text>
          <text class="col-received">已收金额</text>
        </view>
        <view
          v-for="row in rows"
          :key="`${row.customerId || row.customerName}`"
          class="table-row"
        >
          <text class="col-name">{{ row.customerName }}</text>
          <text class="col-receivable">{{ formatMoney(row.receivableAmount) }}</text>
          <text class="col-received">{{ formatMoney(row.receivedAmount) }}</text>
        </view>
        <view v-if="rows.length === 0" class="empty-tip">所选区间暂无客户数据</view>
      </view>
    </scroll-view>

    <OrderDateRangePicker
      :show="rangePickerVisible"
      :start-date="dateFrom"
      :end-date="dateTo"
      @close="rangePickerVisible = false"
      @confirm="onRangeConfirm"
    />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import {
  fetchBossCustomerReport,
  type CustomerReportRow,
  type CustomerReportSummary,
} from '@common/api/dashboard'
import AppIcon from '@/components/AppIcon.vue'
import OrderDateRangePicker from '@/components/OrderDateRangePicker.vue'
import { getLastNDaysRange } from '@common/utils/date-range'
import { useUserStore } from '@common/stores/user'
import { guardOwnerAdminPage } from '@common/utils/boss-access'

const userStore = useUserStore()
const loading = ref(false)
const presetDays = ref(7)
const dateFrom = ref('')
const dateTo = ref('')
const rangePickerVisible = ref(false)
const summary = ref<CustomerReportSummary | null>(null)
const rows = ref<CustomerReportRow[]>([])

onShow(async () => {
  if (!guardOwnerAdminPage()) return
  if (!dateFrom.value) {
    applyPreset(7)
    return
  }
  await loadData()
})

function applyPreset(days: number) {
  presetDays.value = days
  const range = getLastNDaysRange(days)
  dateFrom.value = range.from
  dateTo.value = range.to
  loadData()
}

function openRangePicker() {
  rangePickerVisible.value = true
}

function onRangeConfirm(payload: { from: string; to: string }) {
  rangePickerVisible.value = false
  dateFrom.value = payload.from
  dateTo.value = payload.to
  presetDays.value = 0
  loadData()
}

async function loadData() {
  if (!dateFrom.value || !dateTo.value) return
  loading.value = true
  try {
    const data = await fetchBossCustomerReport({
      dateFrom: dateFrom.value,
      dateTo: dateTo.value,
      dateType: 'DELIVERY',
    })
    summary.value = data.summary
    rows.value = data.rows || []
  } catch {
    summary.value = null
    rows.value = []
  } finally {
    loading.value = false
  }
}

function formatMoney(value?: number) {
  return Number(value || 0).toFixed(2)
}

function showOutstandingTip() {
  uni.showModal({
    title: '未收金额',
    content: '应收金额减去已收金额，小于 0 时按 0 计算。',
    showCancel: false,
  })
}
</script>

<style scoped lang="scss">
.page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7f3;
  overflow: hidden;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 24rpx;
  background: #fff;
  border-bottom: 1rpx solid #f0f0f0;
  flex-shrink: 0;
}

.quick-tabs {
  flex: 1;
  display: flex;
  justify-content: center;
  gap: 48rpx;
}

.quick-tab {
  font-size: 28rpx;
  color: #66736b;
  padding-bottom: 8rpx;
}

.quick-tab.active {
  color: #07c160;
  font-weight: 700;
  box-shadow: inset 0 -4rpx 0 #07c160;
}

.calendar-btn {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f6f7;
  border-radius: 12rpx;
  flex-shrink: 0;
}

.body-scroll {
  flex: 1;
  height: 0;
  padding: 20rpx 24rpx calc(24rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.state-wrap {
  padding: 80rpx 0;
  text-align: center;
}

.summary-card {
  display: flex;
  align-items: stretch;
  gap: 24rpx;
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx 28rpx;
  margin-bottom: 20rpx;
  border: 1rpx solid #ececec;
}

.summary-main {
  flex: 1;
  min-width: 0;
}

.summary-label {
  display: block;
  font-size: 26rpx;
  color: #66736b;
}

.summary-total {
  display: block;
  margin-top: 16rpx;
  font-size: 48rpx;
  font-weight: 700;
  color: #07c160;
}

.summary-breakdown {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  justify-content: center;
}

.breakdown-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  min-width: 220rpx;
}

.tag {
  flex-shrink: 0;
  padding: 4rpx 12rpx;
  border-radius: 6rpx;
  font-size: 22rpx;
  line-height: 1.4;
}

.tag-received {
  background: #dcfce7;
  color: #16a34a;
}

.tag-discount {
  background: #ecfdf5;
  color: #059669;
}

.tag-outstanding {
  background: #f3f4f6;
  color: #66736b;
}

.tag-refund {
  background: #f3f4f6;
  color: #66736b;
}

.breakdown-value-wrap {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.breakdown-value {
  font-size: 26rpx;
  color: #17211b;
}

.info-icon {
  width: 28rpx;
  height: 28rpx;
  line-height: 28rpx;
  text-align: center;
  border-radius: 50%;
  background: #e5e7eb;
  color: #66736b;
  font-size: 20rpx;
}

.table-card {
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
  border: 1rpx solid #ececec;
}

.table-head,
.table-row {
  display: flex;
  align-items: center;
  padding: 24rpx 20rpx;
}

.table-head {
  background: #f3f4f6;
  font-size: 24rpx;
  color: #66736b;
}

.table-row {
  border-top: 1rpx solid #f0f0f0;
  font-size: 28rpx;
  color: #17211b;
}

.col-name {
  flex: 1.2;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.col-receivable,
.col-received {
  flex: 1;
  text-align: center;
}

.empty-tip {
  padding: 80rpx 24rpx;
  text-align: center;
  color: #94a3b8;
  font-size: 28rpx;
}
</style>
