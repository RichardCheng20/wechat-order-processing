<template>
  <view class="page">
    <view class="filter-bar">
      <picker :range="dateTypeOptions" range-key="label" :value="dateTypeIndex" @change="onDateTypeChange">
        <view class="metric-picker">
          <text>{{ dateTypeOptions[dateTypeIndex].label }}</text>
          <text class="arrow">▾</text>
        </view>
      </picker>
      <view class="quick-tabs">
        <text class="quick-tab" :class="{ active: presetDays === 1 }" @tap="applyPreset(1)">今天</text>
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
      <view class="table-card">
        <view class="table-head">
          <text class="col-name">商品</text>
          <text class="col-sales">销售额(元)</text>
          <text class="col-cost">
            参考成本
            <text class="info-icon" @tap.stop="showCostTip">?</text>
          </text>
        </view>
        <view v-for="row in rows" :key="`${row.productId}-${row.rank}`" class="table-row">
          <view class="col-name">
            <text class="name-text">{{ row.productName }}</text>
            <view v-if="row.rank <= 3" class="rank-badge" :class="`rank-${row.rank}`">{{ row.rank }}</view>
            <text v-else class="rank-num">{{ row.rank }}</text>
          </view>
          <text class="col-sales">{{ formatMoney(row.salesAmount) }}</text>
          <text class="col-cost">{{ formatMoney(row.purchaseCost) }}</text>
        </view>
        <view v-if="rows.length === 0" class="empty-tip">所选区间暂无销售数据</view>
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
  fetchBossProductRanking,
  type ProductRankingRow,
} from '@common/api/dashboard'
import AppIcon from '@/components/AppIcon.vue'
import OrderDateRangePicker from '@/components/OrderDateRangePicker.vue'
import { getLastNDaysRange, getTodayRange } from '@common/utils/date-range'
import { useUserStore } from '@common/stores/user'
import { guardOwnerAdminPage } from '@common/utils/boss-access'

const userStore = useUserStore()
const loading = ref(false)
const presetDays = ref(7)
const dateFrom = ref('')
const dateTo = ref('')
const dateType = ref<'ORDER' | 'DELIVERY'>('ORDER')
const rangePickerVisible = ref(false)
const rows = ref<ProductRankingRow[]>([])

const dateTypeOptions = [
  { label: '下单', value: 'ORDER' as const },
  { label: '送货', value: 'DELIVERY' as const },
]
const dateTypeIndex = ref(0)

onShow(async () => {
  if (!(await guardOwnerAdminPage())) return
  if (!dateFrom.value) {
    applyPreset(7)
    return
  }
  await loadData()
})

function onDateTypeChange(e: { detail: { value: string } }) {
  const idx = Number(e.detail.value)
  dateTypeIndex.value = idx
  dateType.value = dateTypeOptions[idx].value
  loadData()
}

function applyPreset(days: number) {
  presetDays.value = days
  const range = days === 1 ? getTodayRange() : getLastNDaysRange(days)
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
    const data = await fetchBossProductRanking({
      dateFrom: dateFrom.value,
      dateTo: dateTo.value,
      dateType: dateType.value,
    })
    rows.value = data.rows || []
  } catch {
    rows.value = []
  } finally {
    loading.value = false
  }
}

function formatMoney(value?: number) {
  return Number(value || 0).toFixed(2)
}

function showCostTip() {
  uni.showModal({
    title: '参考成本',
    content: '按出库数量 × 当日采购价估算，仅供参考。',
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

.metric-picker {
  display: flex;
  align-items: center;
  gap: 4rpx;
  font-size: 28rpx;
  color: #17211b;
  white-space: nowrap;
}

.metric-picker .arrow {
  font-size: 20rpx;
  color: #66736b;
}

.quick-tabs {
  flex: 1;
  display: flex;
  justify-content: center;
  gap: 24rpx;
}

.quick-tab {
  font-size: 28rpx;
  color: #66736b;
  padding-bottom: 8rpx;
}

.quick-tab.active {
  color: #0b7f3a;
  font-weight: 700;
  box-shadow: inset 0 -4rpx 0 #0b7f3a;
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
  display: flex;
  align-items: center;
  gap: 12rpx;
  min-width: 0;
}

.name-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.col-sales,
.col-cost {
  flex: 1;
  text-align: center;
}

.col-cost {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4rpx;
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

.rank-badge {
  flex-shrink: 0;
  width: 40rpx;
  height: 40rpx;
  line-height: 40rpx;
  text-align: center;
  border-radius: 50%;
  font-size: 22rpx;
  font-weight: 700;
  color: #fff;
}

.rank-badge.rank-1 {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}

.rank-badge.rank-2 {
  background: linear-gradient(135deg, #94a3b8, #64748b);
}

.rank-badge.rank-3 {
  background: linear-gradient(135deg, #d97706, #92400e);
}

.rank-num {
  flex-shrink: 0;
  font-size: 24rpx;
  color: #94a3b8;
}

.empty-tip {
  padding: 80rpx 24rpx;
  text-align: center;
  color: #94a3b8;
  font-size: 28rpx;
}
</style>
